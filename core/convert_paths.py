#!/usr/bin/env python3
from __future__ import annotations

import argparse
import html
import json
import posixpath
import re
import shutil
import urllib.parse
from collections import Counter, defaultdict
from pathlib import Path

TEXT_EXTENSIONS = {'.htm', '.html', '.js'}

QUOTED_PATH_RE = re.compile(
    r'''(["'])([^"'<>\r\n]*?\.(?:htm|html|pdf|gif|ico|js)(?:#[^"'<>\r\n]*)?(?:\?[^"'<>\r\n]*)?)\1''',
    re.I,
)
UNQUOTED_ATTR_RE = re.compile(
    r'''((?:href|src|background|action|value)\s*=\s*)([^\s"'<>]+)''',
    re.I,
)
PATH_SUFFIX_RE = re.compile(r'\.(?:htm|html|pdf|gif|ico|js)(?:[#?]|$)', re.I)


def split_suffix(ref: str) -> tuple[str, str]:
    cut = len(ref)
    for ch in ('#', '?'):
        idx = ref.find(ch)
        if idx != -1:
            cut = min(cut, idx)
    return ref[:cut], ref[cut:]


def normalize_reference(
    source_rel: str,
    ref: str,
    exact_files: set[str],
    lower_map: dict[str, list[str]],
) -> tuple[str, str | None]:
    raw = html.unescape(ref)
    low = raw.lower()
    if low.startswith(('http://', 'https://', 'mailto:', 'javascript:', 'data:')):
        return ref, None

    path_part, suffix = split_suffix(raw)
    decoded = urllib.parse.unquote(path_part).replace('\\', '/')
    if not decoded:
        return ref, None

    source_dir = posixpath.dirname(source_rel)
    if decoded.startswith('/'):
        target = posixpath.normpath(decoded.lstrip('/'))
        absolute_style = True
    else:
        target = posixpath.normpath(posixpath.join(source_dir, decoded))
        absolute_style = False

    if target.startswith('../') or target == '..':
        return ref, None
    if target in exact_files:
        return ref, None

    matches = lower_map.get(target.lower(), [])
    if len(matches) != 1:
        return ref, None

    actual = matches[0]
    if absolute_style:
        corrected_path = '/' + actual
    else:
        corrected_path = posixpath.relpath(actual, source_dir or '.')

    if '%' in path_part:
        corrected_path = urllib.parse.quote(corrected_path, safe='/:._-')

    return corrected_path + suffix, actual


def patch_text(
    source_rel: str,
    text: str,
    exact_files: set[str],
    lower_map: dict[str, list[str]],
) -> tuple[str, list[dict]]:
    changes: list[dict] = []

    def quoted_repl(match: re.Match[str]) -> str:
        quote, ref = match.group(1), match.group(2)
        corrected, actual = normalize_reference(source_rel, ref, exact_files, lower_map)
        if corrected != ref:
            changes.append({
                'kind': 'static_case',
                'from': ref,
                'to': corrected,
                'actual': actual,
            })
        return quote + corrected + quote

    text = QUOTED_PATH_RE.sub(quoted_repl, text)

    def attr_repl(match: re.Match[str]) -> str:
        prefix, ref = match.group(1), match.group(2)
        if not PATH_SUFFIX_RE.search(ref):
            return match.group(0)
        corrected, actual = normalize_reference(source_rel, ref, exact_files, lower_map)
        if corrected != ref:
            changes.append({
                'kind': 'static_case',
                'from': ref,
                'to': corrected,
                'actual': actual,
            })
        return prefix + corrected

    text = UNQUOTED_ATTR_RE.sub(attr_repl, text)

    # Two older VISU.JS variants dynamically generate lowercase print-PDF suffixes.
    # Historical // comments are intentionally left untouched.
    if source_rel.upper().endswith('/COMMUN/JS/VISU.JS'):
        lines = text.splitlines(keepends=True)
        for i, line in enumerate(lines):
            if line.lstrip().startswith('//'):
                continue
            new_line = line
            new_line = re.sub(
                r'(["\'])_printNB\1',
                lambda m: m.group(1) + '_PRINTNB' + m.group(1),
                new_line,
            )
            new_line = re.sub(
                r'(["\'])_print\1',
                lambda m: m.group(1) + '_PRINT' + m.group(1),
                new_line,
            )
            if new_line != line:
                changes.append({
                    'kind': 'dynamic_print_case',
                    'from': line.rstrip('\r\n'),
                    'to': new_line.rstrip('\r\n'),
                    'line': i + 1,
                })
                lines[i] = new_line
        text = ''.join(lines)

    return text, changes


def copy_source(source: Path, output: Path) -> None:
    if output.exists():
        raise SystemExit(f'Output already exists: {output}')
    shutil.copytree(source, output)


def main() -> int:
    parser = argparse.ArgumentParser(
        description='Normalize Renault documentation references for case-sensitive filesystems.'
    )
    parser.add_argument('source', type=Path, help='Original Renault documentation root')
    parser.add_argument('--output', type=Path, help='Create a converted copy here')
    parser.add_argument('--in-place', action='store_true', help='Patch source directly (not recommended)')
    parser.add_argument('--report', type=Path, default=Path('conversion-report.json'))
    args = parser.parse_args()

    source = args.source.resolve()
    if not source.is_dir():
        raise SystemExit(f'Not a directory: {source}')
    if args.in_place == bool(args.output):
        raise SystemExit('Choose exactly one: --output DIR or --in-place')

    if args.in_place:
        work = source
    else:
        work = args.output.resolve()
        copy_source(source, work)

    files = [p for p in work.rglob('*') if p.is_file()]
    relative_files = [p.relative_to(work).as_posix() for p in files]
    exact_files = set(relative_files)

    lower_map: dict[str, list[str]] = defaultdict(list)
    for rel in relative_files:
        lower_map[rel.lower()].append(rel)

    all_changes: list[dict] = []
    changed_files = 0
    kind_counts = Counter()

    for path in files:
        if path.suffix.lower() not in TEXT_EXTENSIONS:
            continue

        rel = path.relative_to(work).as_posix()
        raw = path.read_bytes()

        # latin-1 is a lossless byte<->text mapping for legacy 8-bit HTML.
        # We only alter ASCII path strings, so original legacy text bytes are preserved.
        text = raw.decode('latin-1')
        patched, changes = patch_text(rel, text, exact_files, lower_map)

        if not changes:
            continue

        path.write_bytes(patched.encode('latin-1'))
        changed_files += 1

        for change in changes:
            change['file'] = rel
            all_changes.append(change)
            kind_counts[change['kind']] += 1

    report = {
        'source': str(source),
        'output': str(work),
        'files_total': len(files),
        'changed_files': changed_files,
        'changes_total': len(all_changes),
        'changes_by_kind': dict(kind_counts),
        'changes': all_changes,
    }

    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(
        json.dumps(report, ensure_ascii=False, indent=2),
        encoding='utf-8',
    )

    print(json.dumps(
        {k: v for k, v in report.items() if k != 'changes'},
        ensure_ascii=False,
        indent=2,
    ))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
