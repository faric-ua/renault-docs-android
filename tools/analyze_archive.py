#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import html
import json
import posixpath
import re
import urllib.parse
from collections import Counter, defaultdict
from pathlib import Path

TEXT_EXTENSIONS = {".htm", ".html", ".js"}

QUOTED_PATH_RE = re.compile(
    r'''["']([^"'<>\r\n]*?\.(?:htm|html|pdf|gif|ico|js)(?:#[^"'<>\r\n]*)?(?:\?[^"'<>\r\n]*)?)["']''',
    re.I,
)
UNQUOTED_ATTR_RE = re.compile(
    r'''(?:href|src|background|action|value)\s*=\s*([^\s"'<>]+)''',
    re.I,
)
PATH_SUFFIX_RE = re.compile(
    r"\.(?:htm|html|pdf|gif|ico|js)(?:[#?]|$)",
    re.I,
)

FEATURES = {
    "frameset": re.compile(r"<\s*frameset\b", re.I),
    "frame_tag": re.compile(r"<\s*frame\b", re.I),
    "target_blank": re.compile(r'''target\s*=\s*["']?_blank''', re.I),
    "window_open": re.compile(r"window\.open\s*\(", re.I),
    "navigator_appName": re.compile(r"navigator\.appName", re.I),
    "pdf_viewrect": re.compile(r"\.pdf#viewrect=", re.I),
    "onclick": re.compile(r"\bonclick\s*=", re.I),
    "onchange": re.compile(r"\bonchange\s*=", re.I),
}

WINDOWS_PATTERNS = {
    "file_uri": re.compile(r"file\s*://", re.I),
    "drive_abs": re.compile(r'''\b[A-Za-z]:[\\/][^"'<>\s]*'''),
    "backslash_path": re.compile(
        r'''(?:\.\.?\\|[A-Za-z0-9_.-]+\\)[^"'<>\r\n]*'''
    ),
    "windows_api": re.compile(
        r"(ActiveXObject|WScript|Shell\.Application|"
        r"Scripting\.FileSystemObject|window\.external|showModalDialog)",
        re.I,
    ),
}


def extract_references(text: str) -> set[str]:
    text = html.unescape(text)
    found = {m.group(1).strip() for m in QUOTED_PATH_RE.finditer(text)}

    for match in UNQUOTED_ATTR_RE.finditer(text):
        value = match.group(1).strip()
        if PATH_SUFFIX_RE.search(value):
            found.add(value)

    return found


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("root", type=Path)
    parser.add_argument("--out", type=Path, default=Path("analysis-out"))
    args = parser.parse_args()

    root = args.root.resolve()
    out = args.out.resolve()
    out.mkdir(parents=True, exist_ok=True)

    files = [p for p in root.rglob("*") if p.is_file()]
    text_files = [
        p for p in files
        if p.suffix.lower() in TEXT_EXTENSIONS
    ]

    relative_files = [
        p.relative_to(root).as_posix()
        for p in files
    ]
    exact_files = set(relative_files)

    lower_map: dict[str, list[str]] = defaultdict(list)
    for relative in relative_files:
        lower_map[relative.lower()].append(relative)

    extension_counts = Counter(
        p.suffix.lower() or "<noext>"
        for p in files
    )
    extension_bytes = Counter()

    for path in files:
        extension_bytes[path.suffix.lower() or "<noext>"] += path.stat().st_size

    feature_counts = Counter()
    windows_counts = Counter()
    references: list[dict] = []

    for path in text_files:
        source = path.relative_to(root).as_posix()
        text = path.read_text(
            encoding="utf-8",
            errors="replace",
        )

        for name, pattern in FEATURES.items():
            if pattern.search(text):
                feature_counts[name] += 1

        for name, pattern in WINDOWS_PATTERNS.items():
            if pattern.search(text):
                windows_counts[name] += 1

        for raw in extract_references(text):
            lower = raw.lower()

            if lower.startswith(
                ("http://", "https://", "mailto:", "javascript:", "data:")
            ):
                references.append({
                    "source": source,
                    "reference": raw,
                    "status": "external",
                    "resolved": None,
                    "actual": None,
                })
                continue

            path_part = raw.split("#", 1)[0].split("?", 1)[0]
            path_part = urllib.parse.unquote(path_part).replace("\\", "/")

            if path_part.startswith("/"):
                target = posixpath.normpath(path_part.lstrip("/"))
            else:
                target = posixpath.normpath(
                    posixpath.join(
                        posixpath.dirname(source),
                        path_part,
                    )
                )

            if target.startswith("../") or target == "..":
                status = "outside"
                actual = None
            elif target in exact_files:
                status = "exact"
                actual = target
            else:
                matches = lower_map.get(target.lower(), [])

                if len(matches) == 1:
                    status = "case_mismatch"
                    actual = matches[0]
                elif len(matches) > 1:
                    status = "ambiguous_case"
                    actual = ";".join(matches)
                else:
                    status = "missing"
                    actual = None

            # These strings in VISU.JS are comparison/index markers,
            # not paths relative to the JS file itself.
            if (
                path.suffix.lower() == ".js"
                and raw in {".PDF", "../ERREUR.HTM"}
            ):
                status = "dynamic_marker"

            references.append({
                "source": source,
                "reference": raw,
                "status": status,
                "resolved": target,
                "actual": actual,
            })

    case_rows = [
        row for row in references
        if row["status"] == "case_mismatch"
    ]
    problem_sources = sorted({
        row["source"]
        for row in case_rows
    })
    files_with_references = sorted({
        row["source"]
        for row in references
    })

    summary = {
        "root": root.name,
        "files_total": len(files),
        "bytes_total": sum(
            p.stat().st_size
            for p in files
        ),
        "text_files_scanned": len(text_files),
        "files_with_static_references": len(files_with_references),
        "unique_source_reference_pairs": len(references),
        "reference_statuses": dict(
            Counter(
                row["status"]
                for row in references
            )
        ),
        "case_mismatch_source_files": len(problem_sources),
        "case_mismatch_references": len(case_rows),
        "extension_counts": dict(extension_counts),
        "extension_bytes": dict(extension_bytes),
        "features": dict(feature_counts),
        "windows_path_findings": dict(windows_counts),
    }

    (out / "summary.json").write_text(
        json.dumps(
            summary,
            ensure_ascii=False,
            indent=2,
        ),
        encoding="utf-8",
    )

    (out / "files-with-references.txt").write_text(
        "\n".join(files_with_references) + "\n",
        encoding="utf-8",
    )

    (out / "case-mismatch-source-files.txt").write_text(
        "\n".join(problem_sources) + "\n",
        encoding="utf-8",
    )

    with (out / "case-mismatches.csv").open(
        "w",
        encoding="utf-8",
        newline="",
    ) as handle:
        writer = csv.DictWriter(
            handle,
            fieldnames=[
                "source",
                "reference",
                "resolved",
                "actual",
                "status",
            ],
        )
        writer.writeheader()
        writer.writerows(case_rows)

    with (out / "references.jsonl").open(
        "w",
        encoding="utf-8",
    ) as handle:
        for row in references:
            handle.write(
                json.dumps(
                    row,
                    ensure_ascii=False,
                )
                + "\n"
            )

    print(
        json.dumps(
            summary,
            ensure_ascii=False,
            indent=2,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
