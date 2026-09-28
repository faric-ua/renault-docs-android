from __future__ import annotations

import html
import json
import re
from html.parser import HTMLParser
from pathlib import Path
from typing import Any, Callable
from urllib.parse import unquote, urlsplit


MODERN_SECTIONS_SCHEMA_VERSION = 2
MODERN_SECTIONS_FILENAME = "modern-sections.json"

_HTML_SUFFIXES = {".htm", ".html"}
_NAV_NAME_HINTS = (
    "menu",
    "nav",
    "navi",
    "sommaire",
    "summary",
    "index",
    "left",
    "tree",
    "toc",
    "list",
    "rubrique",
    "fonction",
)
_SECTION_RE = re.compile(
    r"^\s*([A-Za-z0-9][A-Za-z0-9_-]{1,15})\s*(?:[-–—:.;]+\s*)?(.*?)\s*$"
)
_SECTION_ID_RE = re.compile(
    r"^(?:\d{3,4}|[A-Za-z]{2,3}|[A-Za-z]{1,3}\d{1,4})$"
)
_JS_HTML_RE = re.compile(
    r"""["']([^"']+?\.(?:html?|HTML?))(?:[?#][^"']*)?["']"""
)
_CHARSET_RE = re.compile(
    br"charset\s*=\s*[\"']?\s*([A-Za-z0-9._-]+)",
    re.IGNORECASE,
)

ProgressCallback = Callable[[str], None]


class _LegacyNavParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.frames: list[str] = []
        self.anchors: list[tuple[str, str]] = []
        self.rows: list[tuple[str, str]] = []
        self.entries: list[tuple[int, str, str]] = []

        self._event_order = 0
        self._anchor_order = 0
        self._anchor_ref: str | None = None
        self._anchor_text: list[str] = []

        self._row_depth = 0
        self._row_order = 0
        self._row_text: list[str] = []
        self._row_refs: list[str] = []

    def handle_starttag(
        self,
        tag: str,
        attrs: list[tuple[str, str | None]],
    ) -> None:
        self._event_order += 1

        attrs_map = {
            key.lower(): value or ""
            for key, value in attrs
        }

        if tag in {"frame", "iframe"}:
            src = attrs_map.get("src", "").strip()
            if src:
                self.frames.append(src)

        if tag == "tr":
            if self._row_depth == 0:
                self._row_order = self._event_order
                self._row_text = []
                self._row_refs = []
            self._row_depth += 1

        if tag == "a":
            self._anchor_order = self._event_order
            ref = _link_reference(
                href=attrs_map.get("href", ""),
                onclick=attrs_map.get("onclick", ""),
            )
            self._anchor_ref = ref
            self._anchor_text = []

            if (
                ref
                and self._row_depth > 0
            ):
                self._row_refs.append(ref)

        if tag == "area":
            ref = _link_reference(
                href=attrs_map.get("href", ""),
                onclick=attrs_map.get("onclick", ""),
            )
            label = _clean_text(
                attrs_map.get("alt", "")
                or attrs_map.get("title", "")
            )
            if ref and label:
                self.anchors.append(
                    (ref, label)
                )
                self.entries.append(
                    (
                        self._event_order,
                        ref,
                        label,
                    )
                )

    def handle_data(
        self,
        data: str,
    ) -> None:
        if self._anchor_ref is not None:
            self._anchor_text.append(data)

        if self._row_depth > 0:
            self._row_text.append(data)

    def handle_endtag(
        self,
        tag: str,
    ) -> None:
        if tag == "a":
            if self._anchor_ref:
                text = _clean_text(
                    " ".join(self._anchor_text)
                )
                if text:
                    self.anchors.append(
                        (
                            self._anchor_ref,
                            text,
                        )
                    )
                    self.entries.append(
                        (
                            self._anchor_order,
                            self._anchor_ref,
                            text,
                        )
                    )

            self._anchor_ref = None
            self._anchor_text = []

        if (
            tag == "tr"
            and self._row_depth > 0
        ):
            self._row_depth -= 1

            if self._row_depth == 0:
                text = _clean_text(
                    " ".join(self._row_text)
                )
                if (
                    text
                    and self._row_refs
                ):
                    self.rows.append(
                        (
                            self._row_refs[0],
                            text,
                        )
                    )
                    self.entries.append(
                        (
                            self._row_order,
                            self._row_refs[0],
                            text,
                        )
                    )

                self._row_text = []
                self._row_refs = []


def build_modern_sections_index(
    output_root: Path,
    volumes: list[dict[str, Any]],
    progress: ProgressCallback | None = None,
) -> dict[str, Any]:
    output_root = output_root.resolve()
    indexed_volumes: list[dict[str, Any]] = []
    total_sections = 0

    for index, volume in enumerate(
        volumes,
        start=1,
    ):
        result = discover_volume_sections(
            output_root=output_root,
            volume=volume,
        )

        indexed_volumes.append(result)
        total_sections += len(
            result["sections"]
        )

        if progress is not None:
            code = (
                volume.get("document_code")
                or volume.get("title")
                or f"volume-{index}"
            )
            progress(
                "Sections: "
                f"{index}/{len(volumes)} · "
                f"{code} · "
                f"{len(result['sections'])} розділів"
            )

    return {
        "schema_version":
            MODERN_SECTIONS_SCHEMA_VERSION,
        "source":
            "legacy-html-navigation",
        "volume_count":
            len(indexed_volumes),
        "section_count":
            total_sections,
        "volumes":
            indexed_volumes,
    }


def write_modern_sections_index(
    output_root: Path,
    volumes: list[dict[str, Any]],
    package_root: Path,
    progress: ProgressCallback | None = None,
) -> Path:
    data = build_modern_sections_index(
        output_root=output_root,
        volumes=volumes,
        progress=progress,
    )

    target = (
        package_root
        / MODERN_SECTIONS_FILENAME
    )
    target.write_text(
        json.dumps(
            data,
            ensure_ascii=False,
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )
    return target


def discover_volume_sections(
    output_root: Path,
    volume: dict[str, Any],
) -> dict[str, Any]:
    output_root = output_root.resolve()
    entrypoint = str(
        volume.get("entrypoint")
        or ""
    ).strip()

    if not entrypoint:
        return _volume_result(
            volume=volume,
            sections=[],
            source_file=None,
        )

    entry_file = (
        output_root
        / Path(entrypoint)
    ).resolve()

    if not _is_within(
        entry_file,
        output_root,
    ):
        return _volume_result(
            volume=volume,
            sections=[],
            source_file=None,
        )

    volume_root = _volume_root(
        output_root=output_root,
        volume=volume,
        entry_file=entry_file,
    )

    sections: list[dict[str, str]] = []
    section_positions: dict[
        tuple[str, str],
        int,
    ] = {}
    section_source: Path | None = None

    queue: list[Path] = [entry_file]
    visited: set[Path] = set()

    while (
        queue
        and len(visited) < 64
    ):
        current = queue.pop(0)

        if current in visited:
            continue

        visited.add(current)
        parsed = _parse_html_file(current)

        if parsed is None:
            continue

        found = _extract_sections(
            parsed=parsed,
            current_file=current,
            output_root=output_root,
        )

        if found:
            if section_source is None:
                section_source = current

            _merge_sections(
                destination=sections,
                positions=section_positions,
                incoming=found,
            )

        for frame_ref in parsed.frames:
            target = _resolve_reference(
                current_file=current,
                reference=frame_ref,
                output_root=output_root,
            )

            if (
                target is not None
                and target not in visited
                and _is_html(target)
            ):
                queue.append(target)

    if len(sections) < 4:
        for current in _fallback_candidates(
            volume_root=volume_root,
            already_seen=visited,
        ):
            parsed = _parse_html_file(
                current,
            )
            if parsed is None:
                continue

            found = _extract_sections(
                parsed=parsed,
                current_file=current,
                output_root=output_root,
            )

            if found:
                if section_source is None:
                    section_source = current

                _merge_sections(
                    destination=sections,
                    positions=section_positions,
                    incoming=found,
                )

            if len(sections) >= 8:
                break

    source_relative = None
    if section_source is not None:
        source_relative = (
            section_source
            .relative_to(output_root)
            .as_posix()
        )

    return _volume_result(
        volume=volume,
        sections=sections,
        source_file=source_relative,
    )


def _volume_result(
    volume: dict[str, Any],
    sections: list[dict[str, str]],
    source_file: str | None,
) -> dict[str, Any]:
    return {
        key: value
        for key, value in {
            "id": volume.get("id"),
            "title": volume.get("title"),
            "document_code":
                volume.get("document_code"),
            "date": volume.get("date"),
            "entrypoint":
                volume.get("entrypoint"),
            "source_file": source_file,
            "sections": sections,
        }.items()
        if value is not None
    }


def _extract_sections(
    parsed: _LegacyNavParser,
    current_file: Path,
    output_root: Path,
) -> list[dict[str, str]]:
    candidates = sorted(
        parsed.entries,
        key=lambda item: item[0],
    )
    result: list[dict[str, str]] = []

    for _, reference, text in candidates:
        target = _resolve_reference(
            current_file=current_file,
            reference=reference,
            output_root=output_root,
        )

        if (
            target is None
            or not target.is_file()
            or not _is_html(target)
        ):
            continue

        parsed_label = _parse_section_label(
            text=text,
            target=target,
        )
        if parsed_label is None:
            continue

        code, title = parsed_label

        result.append(
            {
                "code": code,
                "title": title,
                "entrypoint":
                    target
                    .relative_to(
                        output_root
                    )
                    .as_posix(),
            }
        )

    return result


def _parse_section_label(
    text: str,
    target: Path,
) -> tuple[str, str] | None:
    cleaned = _clean_text(text)
    match = _SECTION_RE.match(cleaned)

    if match is not None:
        code = match.group(1).upper()
        if _looks_like_section_id(code):
            title = _clean_text(
                match.group(2)
            )
            return (
                code,
                title or f"Розділ {code}",
            )

    stem = target.stem.upper()
    if not _looks_like_section_id(stem):
        return None

    title = cleaned
    prefix = re.compile(
        rf"^\s*{re.escape(stem)}\s*(?:[-–—:.;]+\s*)?",
        re.IGNORECASE,
    )
    title = _clean_text(
        prefix.sub("", title)
    )

    return (
        stem,
        title or f"Розділ {stem}",
    )


def _looks_like_section_id(
    value: str,
) -> bool:
    return _SECTION_ID_RE.fullmatch(
        value.strip()
    ) is not None


def _merge_sections(
    destination: list[dict[str, str]],
    positions: dict[tuple[str, str], int],
    incoming: list[dict[str, str]],
) -> None:
    for section in incoming:
        key = (
            section["code"].casefold(),
            section["entrypoint"].casefold(),
        )
        position = positions.get(key)

        if position is None:
            positions[key] = len(destination)
            destination.append(section)
            continue

        previous = destination[position]
        code = section["code"]
        previous_is_generic = (
            previous["title"]
            == f"Розділ {code}"
        )
        incoming_is_better = (
            not section["title"]
            .startswith("Розділ ")
            and (
                previous_is_generic
                or len(section["title"])
                > len(previous["title"])
            )
        )

        if incoming_is_better:
            destination[position] = section


def _fallback_candidates(
    volume_root: Path,
    already_seen: set[Path],
) -> list[Path]:
    if not volume_root.is_dir():
        return []

    candidates = [
        path
        for path in volume_root.rglob("*")
        if (
            path.is_file()
            and _is_html(path)
            and path not in already_seen
        )
    ]

    def score(
        path: Path,
    ) -> tuple[int, int, int, str]:
        relative = path.relative_to(
            volume_root,
        )
        name = path.name.casefold()
        hinted = any(
            hint in name
            for hint in _NAV_NAME_HINTS
        )
        depth = len(relative.parts)
        size = _safe_size(path)

        return (
            0 if hinted else 1,
            depth,
            size,
            relative.as_posix().casefold(),
        )

    candidates.sort(key=score)
    return candidates[:300]


def _parse_html_file(
    path: Path,
) -> _LegacyNavParser | None:
    if (
        not path.is_file()
        or not _is_html(path)
        or _safe_size(path) > 2_000_000
    ):
        return None

    try:
        raw = path.read_bytes()
    except OSError:
        return None

    text = _decode_html(raw)
    parser = _LegacyNavParser()

    try:
        parser.feed(text)
        parser.close()
    except Exception:
        return None

    return parser


def _decode_html(
    raw: bytes,
) -> str:
    head = raw[:8192]
    match = _CHARSET_RE.search(head)

    encodings: list[str] = []

    if match is not None:
        declared = (
            match.group(1)
            .decode(
                "ascii",
                errors="ignore",
            )
            .strip()
        )
        if declared:
            encodings.append(declared)

    encodings.extend(
        [
            "utf-8",
            "cp1251",
            "cp1252",
            "latin-1",
        ]
    )

    seen: set[str] = set()

    for encoding in encodings:
        key = encoding.casefold()
        if key in seen:
            continue
        seen.add(key)

        try:
            return raw.decode(encoding)
        except (
            LookupError,
            UnicodeDecodeError,
        ):
            continue

    return raw.decode(
        "utf-8",
        errors="replace",
    )


def _link_reference(
    href: str,
    onclick: str,
) -> str | None:
    href = html.unescape(
        href or ""
    ).strip()

    if href and href != "#":
        if not href.casefold().startswith(
            "javascript:"
        ):
            return href

        js_match = _JS_HTML_RE.search(href)
        if js_match is not None:
            return js_match.group(1)

    onclick = html.unescape(
        onclick or ""
    )

    js_match = _JS_HTML_RE.search(
        onclick
    )
    if js_match is not None:
        return js_match.group(1)

    return None


def _resolve_reference(
    current_file: Path,
    reference: str,
    output_root: Path,
) -> Path | None:
    cleaned = html.unescape(
        reference
    ).strip()

    if not cleaned:
        return None

    lowered = cleaned.casefold()

    if lowered.startswith(
        (
            "http:",
            "https:",
            "mailto:",
            "data:",
        )
    ):
        return None

    if lowered.startswith(
        "javascript:"
    ):
        match = _JS_HTML_RE.search(
            cleaned
        )
        if match is None:
            return None
        cleaned = match.group(1)

    try:
        parsed = urlsplit(
            cleaned.replace(
                "\\",
                "/",
            )
        )
    except ValueError:
        return None

    path_text = unquote(
        parsed.path
    ).strip()

    if not path_text:
        return None

    if path_text.startswith("/"):
        target = (
            output_root
            / path_text.lstrip("/")
        ).resolve()
    else:
        target = (
            current_file.parent
            / path_text
        ).resolve()

    if not _is_within(
        target,
        output_root,
    ):
        return None

    return target


def _volume_root(
    output_root: Path,
    volume: dict[str, Any],
    entry_file: Path,
) -> Path:
    source_folder = str(
        volume.get("source_folder")
        or ""
    ).strip()

    if source_folder:
        candidate = (
            output_root
            / source_folder
        ).resolve()

        if (
            candidate.is_dir()
            and _is_within(
                candidate,
                output_root,
            )
        ):
            return candidate

    return entry_file.parent


def _is_html(
    path: Path,
) -> bool:
    return (
        path.suffix.casefold()
        in _HTML_SUFFIXES
    )


def _is_within(
    path: Path,
    root: Path,
) -> bool:
    try:
        path.relative_to(root)
    except ValueError:
        return False
    return True


def _safe_size(
    path: Path,
) -> int:
    try:
        return path.stat().st_size
    except OSError:
        return 2_000_001


def _clean_text(
    value: str,
) -> str:
    return " ".join(
        html.unescape(value)
        .replace("\xa0", " ")
        .split()
    )
