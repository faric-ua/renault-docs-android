#!/usr/bin/env python3
"""Replace the defaced Laguna II start-page logo inside .rdpkg / .rdproject files.

Owner request 2026-10-18 (TODO №26): in the Laguna II volumes `ICONES/LOGO.GIF`
was replaced in 2023 by an obscene picture (84 696 bytes, SHA-256 below). This
tool writes *new* package files with that exact picture replaced by our own
logo. Originals are never modified.

Where the picture lives inside one volume package (.rdpkg):
  * the volume tree (what `index` in the volume root links to);
  * the Fast Pack `fast-content-<sha16>.zip` — the viewer serves files from it
    *first*, so it must be fixed too. Its SHA-256 / size / name are recorded in
    `renault-dataset.json` (`fast_pack`), and `rdpkg.json` keeps
    `payload_bytes` — both are rewritten to match.
A project file (.rdproject) holds several .rdpkg — each is fixed the same way.

Matching is by exact fingerprint (size + SHA-256), never by name, so nothing
else is touched. The result is checked before it is kept.

Exit codes: 0 — ok (dry run or applied), 2 — some file failed.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import shutil
import sys
import tempfile
from dataclasses import dataclass, field
from pathlib import Path
from zipfile import ZIP_DEFLATED, BadZipFile, ZipFile, ZipInfo

BAD_SIZE = 84_696
BAD_SHA256 = "26ba49e731c292c27b7d28a2e14e1478196f2906a6cd77d7d3a09032adc39f1a"

RDPKG_MANIFEST = "rdpkg.json"
FAST_PREFIX = "fast-content-"
OUTPUT_FOLDER = "logo-fixed"
PACKAGE_SUFFIXES = (".rdpkg", ".rdproject")

DEFAULT_ROOTS = (
    "/storage/emulated/0/Download",
    "/storage/emulated/0/Documents/Renault/packages",
    "/storage/emulated/0/Documents/Renault Docs",
)

CHUNK = 1024 * 1024


class FixError(Exception):
    pass


@dataclass
class Report:
    replaced: list[str] = field(default_factory=list)  # entry paths, nested as "a.zip!b"

    @property
    def count(self) -> int:
        return len(self.replaced)


# ---------------------------------------------------------------- helpers

def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(CHUNK), b""):
            digest.update(chunk)
    return digest.hexdigest()


def is_bad_entry(zip_file: ZipFile, info: ZipInfo) -> bool:
    if info.is_dir() or info.file_size != BAD_SIZE:
        return False
    return hashlib.sha256(zip_file.read(info)).hexdigest() == BAD_SHA256


def is_nested_zip(name: str) -> bool:
    base = name.rsplit("/", 1)[-1].lower()
    return (base.startswith(FAST_PREFIX) and base.endswith(".zip")) or base.endswith(".rdpkg")


def copy_info(info: ZipInfo, name: str | None = None) -> ZipInfo:
    out = ZipInfo(filename=name or info.filename, date_time=info.date_time)
    out.compress_type = info.compress_type
    out.external_attr = info.external_attr
    out.create_system = info.create_system
    out.comment = info.comment
    out.extra = b""
    return out


def dump_json_like(original: str, value: object) -> str:
    """Keep the original layout style (indented or compact)."""
    stripped = original.lstrip()
    if stripped.startswith("{\n") or "\n" in stripped[:200]:
        indent_line = next((l for l in original.splitlines()[1:] if l.strip()), "  ")
        indent = len(indent_line) - len(indent_line.lstrip()) or 2
        text = json.dumps(value, ensure_ascii=False, indent=indent)
        return text + ("\n" if original.endswith("\n") else "")
    return json.dumps(value, ensure_ascii=False, separators=(",", ":"))


# ---------------------------------------------------------------- scanning

def count_bad(path: Path, _depth: int = 0) -> int:
    """How many places hold the bad picture (nested archives included)."""
    total = 0
    with ZipFile(path) as zf:
        for info in zf.infolist():
            if is_bad_entry(zf, info):
                total += 1
            elif _depth < 3 and not info.is_dir() and is_nested_zip(info.filename):
                with tempfile.TemporaryDirectory(dir=_scratch_dir(path)) as tmp:
                    nested = Path(tmp) / "n.zip"
                    with zf.open(info) as src, nested.open("wb") as dst:
                        shutil.copyfileobj(src, dst, CHUNK)
                    try:
                        total += count_bad(nested, _depth + 1)
                    except BadZipFile:
                        pass
    return total


def _scratch_dir(path: Path) -> str | None:
    env = os.environ.get("FIX_LOGO_TMP")
    return env or None


def find_packages(roots: list[Path]) -> list[Path]:
    found: list[Path] = []
    seen: set[Path] = set()
    for root in roots:
        if not root.is_dir():
            continue
        for current, dirs, files in os.walk(root):
            dirs[:] = [d for d in dirs if d != OUTPUT_FOLDER and not d.startswith(".")]
            for name in sorted(files):
                if name.lower().endswith(PACKAGE_SUFFIXES) and not name.startswith("."):
                    path = Path(current) / name
                    real = path.resolve()
                    if real not in seen:
                        seen.add(real)
                        found.append(path)
    return found


# ---------------------------------------------------------------- rewriting

def rewrite_zip(source: Path, target: Path, logo: bytes, work: Path, report: Report, prefix: str = "") -> bool:
    """Rewrite one archive. Returns True when something was replaced."""
    with ZipFile(source) as zf:
        infos = zf.infolist()
        names = {info.filename for info in infos}
        is_rdpkg = RDPKG_MANIFEST in names
        staged: dict[str, Path | bytes] = {}
        renamed: dict[str, str] = {}
        fast_info: dict[str, dict] = {}
        changed = False

        for index, info in enumerate(infos):
            if info.is_dir():
                continue
            name = info.filename
            if is_bad_entry(zf, info):
                staged[name] = logo
                report.replaced.append(prefix + name)
                changed = True
            elif is_nested_zip(name):
                nested_in = work / f"in-{index}.zip"
                nested_out = work / f"out-{index}.zip"
                with zf.open(info) as src, nested_in.open("wb") as dst:
                    shutil.copyfileobj(src, dst, CHUNK)
                nested_work = work / f"w-{index}"
                nested_work.mkdir()
                try:
                    nested_changed = rewrite_zip(nested_in, nested_out, logo, nested_work, report, prefix + name + "!")
                except BadZipFile:
                    nested_changed = False
                shutil.rmtree(nested_work, ignore_errors=True)
                if nested_changed:
                    changed = True
                    staged[name] = nested_out
                    base = name.rsplit("/", 1)[-1]
                    if base.lower().startswith(FAST_PREFIX) and base.lower().endswith(".zip"):
                        digest = sha256_file(nested_out)
                        new_base = FAST_PREFIX + digest[:16] + ".zip"
                        new_name = name[: len(name) - len(base)] + new_base
                        if new_name != name and new_name in names:
                            raise FixError(f"name clash for {new_name}")
                        renamed[name] = new_name
                        fast_info[base] = {"new_base": new_base, "sha256": digest, "bytes": nested_out.stat().st_size}
                nested_in.unlink(missing_ok=True)

        if not changed:
            return False

        manifest_name = None
        manifest_obj = None
        if is_rdpkg:
            manifest_text = zf.read(RDPKG_MANIFEST).decode("utf-8")
            manifest_obj = json.loads(manifest_text)
            manifest_name = manifest_obj.get("dataset_manifest", "renault-dataset.json")

        # Fast Pack descriptor in the dataset manifest (any renault-dataset.json in this archive).
        if fast_info:
            updated = 0
            for info in infos:
                if info.is_dir() or not info.filename.rsplit("/", 1)[-1] == "renault-dataset.json":
                    continue
                text = zf.read(info).decode("utf-8")
                data = json.loads(text)
                fast = data.get("fast_pack") if isinstance(data, dict) else None
                if not isinstance(fast, dict):
                    continue
                path_value = str(fast.get("path", ""))
                old_base = path_value.rsplit("/", 1)[-1]
                if old_base in fast_info:
                    new = fast_info[old_base]
                    fast["path"] = path_value[: len(path_value) - len(old_base)] + new["new_base"]
                    fast["sha256"] = new["sha256"]
                    fast["bytes"] = new["bytes"]
                    staged[info.filename] = dump_json_like(text, data).encode("utf-8")
                    updated += 1
            if updated == 0:
                raise FixError("Fast Pack changed but its descriptor was not found in renault-dataset.json")

        def size_of(info: ZipInfo) -> int:
            value = staged.get(info.filename)
            if value is None:
                return info.file_size
            if isinstance(value, bytes):
                return len(value)
            return value.stat().st_size

        if is_rdpkg and manifest_obj is not None:
            payload = [i for i in infos if not i.is_dir() and i.filename != RDPKG_MANIFEST]
            manifest_obj["payload_bytes"] = sum(size_of(i) for i in payload)
            manifest_obj["payload_file_count"] = len(payload)
            staged[RDPKG_MANIFEST] = dump_json_like(zf.read(RDPKG_MANIFEST).decode("utf-8"), manifest_obj).encode("utf-8")

        target.parent.mkdir(parents=True, exist_ok=True)
        with ZipFile(target, "w", allowZip64=True) as out:
            for info in infos:
                new_name = renamed.get(info.filename, info.filename)
                if info.is_dir():
                    out.writestr(copy_info(info, new_name), b"")
                    continue
                value = staged.get(info.filename)
                out_info = copy_info(info, new_name)
                if value is None:
                    with zf.open(info) as src, out.open(out_info, "w", force_zip64=True) as dst:
                        shutil.copyfileobj(src, dst, CHUNK)
                elif isinstance(value, bytes):
                    out.writestr(out_info, value)
                else:
                    with value.open("rb") as src, out.open(out_info, "w", force_zip64=True) as dst:
                        shutil.copyfileobj(src, dst, CHUNK)
        for value in staged.values():
            if isinstance(value, Path):
                value.unlink(missing_ok=True)
        return True


# ---------------------------------------------------------------- checking

def verify(source: Path, result: Path) -> None:
    with ZipFile(source) as a, ZipFile(result) as b:
        if b.testzip() is not None:
            raise FixError("CRC check failed")
        names_a = [i.filename for i in a.infolist()]
        names_b = [i.filename for i in b.infolist()]
        if len(names_a) != len(names_b):
            raise FixError("entry count differs")
        if names_a and names_a[0] != names_b[0]:
            raise FixError("first entry changed")
        _verify_rdpkgs(b)
    if count_bad(result) != 0:
        raise FixError("bad picture still present")


def _verify_rdpkgs(zf: ZipFile) -> None:
    names = {i.filename for i in zf.infolist()}
    if RDPKG_MANIFEST in names:
        _verify_one_rdpkg(zf)
    for info in zf.infolist():
        if info.filename.lower().endswith(".rdpkg"):
            with tempfile.TemporaryDirectory(dir=_scratch_dir(Path("."))) as tmp:
                nested = Path(tmp) / "v.rdpkg"
                with zf.open(info) as src, nested.open("wb") as dst:
                    shutil.copyfileobj(src, dst, CHUNK)
                with ZipFile(nested) as inner:
                    _verify_one_rdpkg(inner)


def _verify_one_rdpkg(zf: ZipFile) -> None:
    infos = [i for i in zf.infolist() if not i.is_dir()]
    manifest = json.loads(zf.read(RDPKG_MANIFEST))
    payload = [i for i in infos if i.filename != RDPKG_MANIFEST]
    if manifest.get("payload_bytes") != sum(i.file_size for i in payload):
        raise FixError("payload_bytes mismatch")
    if manifest.get("payload_file_count") != len(payload):
        raise FixError("payload_file_count mismatch")
    dataset_name = manifest.get("dataset_manifest", "renault-dataset.json")
    dataset = json.loads(zf.read(dataset_name))
    fast = dataset.get("fast_pack")
    if isinstance(fast, dict) and fast.get("path"):
        by_name = {i.filename: i for i in infos}
        candidates = [n for n in by_name if n == fast["path"] or n.endswith("/" + fast["path"])]
        if not candidates:
            raise FixError("Fast Pack file named in renault-dataset.json is missing")
        info = by_name[candidates[0]]
        digest = hashlib.sha256()
        with zf.open(info) as src:
            for chunk in iter(lambda: src.read(CHUNK), b""):
                digest.update(chunk)
        if digest.hexdigest() != fast.get("sha256") or info.file_size != fast.get("bytes"):
            raise FixError("Fast Pack SHA-256 / size do not match renault-dataset.json")


# ---------------------------------------------------------------- main

def fix_file(path: Path, logo: bytes) -> tuple[Path, Report]:
    out_dir = path.parent / OUTPUT_FOLDER
    out_dir.mkdir(exist_ok=True)
    target = out_dir / path.name
    report = Report()
    with tempfile.TemporaryDirectory(dir=os.environ.get("FIX_LOGO_TMP") or out_dir, prefix=".fix-") as tmp:
        work = Path(tmp)
        partial = work / ("result" + path.suffix)
        if not rewrite_zip(path, partial, logo, work, report):
            raise FixError("no defaced logo inside")
        verify(path, partial)
        shutil.move(str(partial), str(target))
    return target, report


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--directory", action="append", help="folder to scan (repeatable)")
    parser.add_argument("--logo", required=True, help="new logo file")
    parser.add_argument("--apply", action="store_true", help="write fixed copies")
    args = parser.parse_args(argv)

    logo_path = Path(args.logo)
    logo = logo_path.read_bytes()
    if hashlib.sha256(logo).hexdigest() == BAD_SHA256:
        print("Нове лого збігається зі зіпсованим — зупинено.")
        return 2

    roots = [Path(p) for p in (args.directory or DEFAULT_ROOTS)]
    print("Шукаю файли томів у:")
    for root in roots:
        print(f"  {root}" + ("" if root.is_dir() else "  (немає)"))
    print()

    affected: list[tuple[Path, int]] = []
    for path in find_packages(roots):
        try:
            places = count_bad(path)
        except (BadZipFile, OSError) as error:
            print(f"  ? {path.name} — не читається ({error})")
            continue
        if places:
            affected.append((path, places))

    if not affected:
        print("Файлів із зіпсованим лого не знайдено.")
        print("Поклади файли томів Laguna II (.rdpkg) або проєкт (.rdproject) у папку Download і запусти ще раз.")
        return 0

    print(f"Знайдено файлів із зіпсованим лого: {len(affected)}")
    for path, places in affected:
        print(f"  • {path.name}  (місць: {places})  — {path.parent}")
    print()

    if not args.apply:
        print("Це лише перевірка — нічого не змінено.")
        return 0

    failed = 0
    for number, (path, _places) in enumerate(affected, start=1):
        print(f"[{number}/{len(affected)}] {path.name} …", flush=True)
        previous = (path.parent / OUTPUT_FOLDER / path.name).exists()
        try:
            target, report = fix_file(path, logo)
        except (FixError, BadZipFile, OSError, ValueError, KeyError) as error:
            failed += 1
            print(f"    ✗ не вийшло: {error}. Оригінал не змінено.")
            continue
        print(f"    ✓ замінено місць: {report.count}, перевірено → {target}")
        if previous:
            # 2026-10-18: the logo was redone (transparent background) and the
            # owner runs item 26 again — our own earlier copy is replaced.
            print("    (попередню копію в «logo-fixed» замінено новою; оригінал не змінено)")

    print()
    if failed:
        print(f"Готово з помилками: {failed} з {len(affected)}. Оригінали не змінено.")
        return 2
    print(f"Готово: {len(affected)} файл(ів). Нові файли — у папці «{OUTPUT_FOLDER}» поруч з оригіналами.")
    print("Оригінали не змінено.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
