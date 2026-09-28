#!/usr/bin/env python3
from __future__ import annotations

import argparse
import html
import json
import re
import sys
import zipfile
from collections import deque
from pathlib import Path
from urllib.parse import unquote, urlsplit

REPO_ROOT = Path(__file__).resolve().parents[1]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from tools.prepare_from_config import load_config


TEXT_SUFFIXES = {
    ".htm",
    ".html",
    ".js",
    ".css",
    ".txt",
    ".xml",
    ".json",
}
BINARY_SUFFIXES = {
    ".pdf",
    ".gif",
    ".png",
    ".jpg",
    ".jpeg",
    ".bmp",
    ".svg",
    ".ico",
}
PATH_RE = re.compile(
    r"""["']([^"'<>\r\n]+?\.(?:html?|js|css|pdf|gif|png|jpe?g|bmp|svg|ico))(?:[?#][^"']*)?["']""",
    re.IGNORECASE,
)
ATTR_RE = re.compile(
    r"""(?:src|href|action)\s*=\s*["']([^"']+)["']""",
    re.IGNORECASE,
)
CHARSET_RE = re.compile(
    br"charset\s*=\s*[\"']?\s*([A-Za-z0-9._-]+)",
    re.IGNORECASE,
)


def decode_text(raw: bytes) -> str:
    match = CHARSET_RE.search(raw[:8192])
    candidates: list[str] = []
    if match is not None:
        declared = (
            match.group(1)
            .decode("ascii", errors="ignore")
            .strip()
        )
        if declared:
            candidates.append(declared)

    candidates.extend(
        [
            "utf-8",
            "cp1251",
            "cp1252",
            "latin-1",
        ]
    )

    seen: set[str] = set()
    for encoding in candidates:
        key = encoding.casefold()
        if key in seen:
            continue
        seen.add(key)
        try:
            return raw.decode(encoding)
        except (LookupError, UnicodeDecodeError):
            pass

    return raw.decode("utf-8", errors="replace")


def is_within(path: Path, root: Path) -> bool:
    try:
        path.relative_to(root)
    except ValueError:
        return False
    return True


def resolve_reference(
    dataset_root: Path,
    current_file: Path,
    reference: str,
) -> Path | None:
    cleaned = html.unescape(reference or "").strip()
    if not cleaned:
        return None

    lowered = cleaned.casefold()
    if lowered.startswith(
        (
            "http:",
            "https:",
            "mailto:",
            "javascript:",
            "data:",
        )
    ):
        return None

    try:
        parsed = urlsplit(
            cleaned.replace("\\", "/")
        )
    except ValueError:
        return None

    path_text = unquote(parsed.path).strip()
    if not path_text:
        return None

    if path_text.startswith("/"):
        target = (
            dataset_root
            / path_text.lstrip("/")
        ).resolve()
    else:
        target = (
            current_file.parent
            / path_text
        ).resolve()

    if not is_within(target, dataset_root):
        return None

    return target


def discover_references(
    dataset_root: Path,
    current_file: Path,
    text: str,
) -> list[Path]:
    values: list[str] = []

    values.extend(
        match.group(1)
        for match in ATTR_RE.finditer(text)
    )
    values.extend(
        match.group(1)
        for match in PATH_RE.finditer(text)
    )

    resolved: list[Path] = []
    seen: set[Path] = set()

    for value in values:
        target = resolve_reference(
            dataset_root=dataset_root,
            current_file=current_file,
            reference=value,
        )
        if target is None or target in seen:
            continue
        seen.add(target)
        resolved.append(target)

    return resolved


def load_runtime_tree(dataset_root: Path) -> dict:
    path = (
        dataset_root
        / "_renault"
        / "runtime-tree.json"
    )
    if not path.is_file():
        raise SystemExit(
            "runtime-tree.json не знайдено. "
            "Спочатку запусти Renault Menu → 9."
        )
    return json.loads(
        path.read_text(encoding="utf-8")
    )


def pick_volume(
    runtime_tree: dict,
    volume_query: str,
) -> dict:
    query = volume_query.casefold().strip()

    candidates = []
    for volume in runtime_tree.get(
        "volumes",
        [],
    ):
        haystack = " ".join(
            str(volume.get(key) or "")
            for key in (
                "id",
                "title",
                "document_code",
                "source_folder",
            )
        ).casefold()

        if query in haystack:
            candidates.append(volume)

    if not candidates:
        raise SystemExit(
            f"Том не знайдено: {volume_query}"
        )

    if len(candidates) > 1:
        exact = [
            volume
            for volume in candidates
            if str(
                volume.get("document_code")
                or ""
            ).casefold()
            == query
        ]
        if len(exact) == 1:
            return exact[0]

        labels = "\n".join(
            "  - "
            + str(
                item.get("document_code")
                or item.get("title")
                or item.get("id")
            )
            for item in candidates
        )
        raise SystemExit(
            "Запит неоднозначний. Варіанти:\n"
            + labels
        )

    return candidates[0]


def pick_section(
    volume: dict,
    code: str,
) -> dict:
    for section in (
        volume.get("modern", {})
        .get("sections", [])
    ):
        if str(section.get("code")) == code:
            return section

    raise SystemExit(
        f"У томі немає секції {code}"
    )


def build_bundle(
    dataset_root: Path,
    volume: dict,
    section: dict,
    output_path: Path,
    max_text_files: int = 400,
) -> dict:
    section_code = str(section["code"])
    source_folder = str(
        volume.get("source_folder")
        or ""
    )
    volume_root = (
        dataset_root / source_folder
    ).resolve()

    if not volume_root.is_dir():
        raise SystemExit(
            f"Папку тому не знайдено: {volume_root}"
        )

    entrypoint = (
        dataset_root
        / str(section["legacy_entrypoint"])
    ).resolve()

    # Start only from the requested section. Do not seed CODE.HTM
    # or the whole Classic shell here: CODE.HTM links every section and
    # would turn a focused 101 bundle into a crawl of the complete volume.
    seeds: list[Path] = [entrypoint]

    convention_candidates = [
        volume_root
        / "COMMUN"
        / "HTM"
        / "PC"
        / f"{section_code}.HTM",
        volume_root
        / "RUS"
        / "HTM"
        / "PC"
        / f"{section_code}.HTM",
        volume_root
        / "RUS"
        / "HTM"
        / "MENU"
        / f"{section_code}.HTM",
    ]
    seeds.extend(convention_candidates)

    queue = deque(
        path
        for path in seeds
        if path.is_file()
        and is_within(path, dataset_root)
    )

    visited: set[Path] = set()
    text_files: list[Path] = []
    binary_refs: set[Path] = set()
    missing_refs: set[str] = set()
    edges: list[dict[str, str]] = []

    while queue and len(text_files) < max_text_files:
        current = queue.popleft()
        if current in visited:
            continue
        visited.add(current)

        suffix = current.suffix.casefold()
        if suffix not in TEXT_SUFFIXES:
            binary_refs.add(current)
            continue

        try:
            raw = current.read_bytes()
        except OSError:
            continue

        text_files.append(current)
        decoded = decode_text(raw)

        for target in discover_references(
            dataset_root=dataset_root,
            current_file=current,
            text=decoded,
        ):
            edge = {
                "from":
                    current.relative_to(
                        dataset_root
                    ).as_posix(),
                "to":
                    target.relative_to(
                        dataset_root
                    ).as_posix(),
            }
            if edge not in edges:
                edges.append(edge)

            if not target.exists():
                missing_refs.add(edge["to"])
                continue

            suffix = target.suffix.casefold()

            if suffix in TEXT_SUFFIXES:
                if target not in visited:
                    queue.append(target)
            elif suffix in BINARY_SUFFIXES:
                binary_refs.add(target)

    manifest = {
        "format":
            "renault-section-source-bundle",
        "schema_version": 1,
        "document_code":
            volume.get("document_code"),
        "volume_id":
            volume.get("id"),
        "source_folder":
            source_folder,
        "section": {
            "code": section_code,
            "title": section.get("title"),
            "legacy_entrypoint":
                section.get("legacy_entrypoint"),
        },
        "text_file_count":
            len(text_files),
        "binary_reference_count":
            len(binary_refs),
        "dependency_edge_count":
            len(edges),
        "truncated":
            bool(queue),
        "max_text_files":
            max_text_files,
        "text_files": [
            path.relative_to(
                dataset_root
            ).as_posix()
            for path in text_files
        ],
        "binary_references": [
            path.relative_to(
                dataset_root
            ).as_posix()
            for path in sorted(binary_refs)
        ],
        "missing_references":
            sorted(missing_refs),
        "edges": edges,
    }

    runtime_subset = {
        "schema_version":
            runtime_tree_subset_schema(),
        "volume": {
            key: volume.get(key)
            for key in (
                "id",
                "title",
                "document_code",
                "date",
                "kind",
                "source_folder",
            )
        },
        "classic":
            volume.get("classic"),
        "section": section,
    }

    output_path.parent.mkdir(
        parents=True,
        exist_ok=True,
    )

    with zipfile.ZipFile(
        output_path,
        "w",
        compression=zipfile.ZIP_DEFLATED,
        compresslevel=9,
    ) as archive:
        archive.writestr(
            "BUNDLE_MANIFEST.json",
            json.dumps(
                manifest,
                ensure_ascii=False,
                indent=2,
            )
            + "\n",
        )
        archive.writestr(
            "RUNTIME_SUBSET.json",
            json.dumps(
                runtime_subset,
                ensure_ascii=False,
                indent=2,
            )
            + "\n",
        )

        for source in text_files:
            relative = source.relative_to(
                dataset_root
            ).as_posix()
            archive.write(
                source,
                arcname=
                    "sources/" + relative,
            )

    return manifest


def runtime_tree_subset_schema() -> int:
    return 1


def main() -> int:
    parser = argparse.ArgumentParser(
        description=(
            "Export one Renault section's legacy HTML/JS/CSS "
            "dependency bundle for Runtime IR compiler development."
        )
    )
    parser.add_argument(
        "--config",
        type=Path,
        default=Path(
            "config/current-device.json"
        ),
    )
    parser.add_argument(
        "--volume",
        default="NT8183A",
        help=(
            "Document code / volume id / title fragment. "
            "Default: NT8183A"
        ),
    )
    parser.add_argument(
        "--section",
        default="101",
        help="Section code. Default: 101",
    )
    parser.add_argument(
        "--output",
        type=Path,
    )
    args = parser.parse_args()

    config = load_config(
        args.config.resolve()
    )
    dataset_root = Path(
        config["build_root"]
    ).resolve()

    runtime_tree = load_runtime_tree(
        dataset_root
    )
    volume = pick_volume(
        runtime_tree,
        args.volume,
    )
    section = pick_section(
        volume,
        args.section,
    )

    output = args.output
    if output is None:
        packages = (
            dataset_root.parent
            / "packages"
        )
        document_code = str(
            volume.get("document_code")
            or "volume"
        )
        output = (
            packages
            / (
                "Runtime-IR-"
                + document_code
                + "-"
                + str(section["code"])
                + "-source.zip"
            )
        )

    manifest = build_bundle(
        dataset_root=dataset_root,
        volume=volume,
        section=section,
        output_path=output.resolve(),
    )

    print("Runtime IR source bundle готовий:")
    print(f"  {output.resolve()}")
    print()
    print(
        "Text sources: "
        f"{manifest['text_file_count']}"
    )
    print(
        "Binary refs: "
        f"{manifest['binary_reference_count']}"
    )
    print(
        "Dependency edges: "
        f"{manifest['dependency_edge_count']}"
    )
    if manifest["truncated"]:
        print(
            "УВАГА: dependency scan досяг ліміту "
            f"{manifest['max_text_files']} text files."
        )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
