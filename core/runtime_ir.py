from __future__ import annotations

import html
import json
import re
from html.parser import HTMLParser
from pathlib import Path
from typing import Any, Callable
from urllib.parse import unquote, urlsplit

from core.sections import discover_volume_sections
from core.section_ir import SectionIrCompiler
from core.runtime_ir_shards import write_runtime_ir_shards


RUNTIME_TREE_SCHEMA_VERSION = 2
RUNTIME_TREE_FILENAME = "runtime-tree.json"

_HTML_SUFFIXES = {".htm", ".html"}
_CHARSET_RE = re.compile(
    br"charset\s*=\s*[\"']?\s*([A-Za-z0-9._-]+)",
    re.IGNORECASE,
)
_QUOTED_HTML_RE = re.compile(
    r"""["']([^"'<>]+?\.(?:html?|HTML?))(?:[?#][^"']*)?["']"""
)
_META_REFRESH_URL_RE = re.compile(
    r"url\s*=\s*['\"]?([^'\";]+)",
    re.IGNORECASE,
)

ProgressCallback = Callable[[str], None]


class _RuntimeHtmlParser(HTMLParser):
    """Capture the shell/frame topology without executing legacy JavaScript."""

    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.title_parts: list[str] = []
        self._in_title = False

        self.frame_roots: list[dict[str, Any]] = []
        self._frameset_stack: list[dict[str, Any]] = []
        self.frame_refs: list[str] = []
        self.redirect_refs: list[str] = []

    def handle_starttag(
        self,
        tag: str,
        attrs: list[tuple[str, str | None]],
    ) -> None:
        attrs_map = {
            key.lower(): value or ""
            for key, value in attrs
        }

        if tag == "title":
            self._in_title = True
            return

        if tag == "frameset":
            node: dict[str, Any] = {
                "type": "frameset",
                "children": [],
            }

            rows = attrs_map.get("rows", "").strip()
            cols = attrs_map.get("cols", "").strip()

            if rows:
                node["rows"] = rows
            if cols:
                node["cols"] = cols

            if self._frameset_stack:
                self._frameset_stack[-1]["children"].append(node)
            else:
                self.frame_roots.append(node)

            self._frameset_stack.append(node)
            return

        if tag in {"frame", "iframe"}:
            src = attrs_map.get("src", "").strip()
            node = {
                "type": tag,
                "name": attrs_map.get("name", "").strip(),
                "id": attrs_map.get("id", "").strip(),
                "src": src,
            }

            scrolling = attrs_map.get("scrolling", "").strip()
            if scrolling:
                node["scrolling"] = scrolling

            if self._frameset_stack:
                self._frameset_stack[-1]["children"].append(node)
            else:
                self.frame_roots.append(node)

            if src:
                self.frame_refs.append(src)
            return

        if tag == "meta":
            http_equiv = attrs_map.get("http-equiv", "").casefold()
            content = attrs_map.get("content", "")

            if http_equiv == "refresh" and content:
                match = _META_REFRESH_URL_RE.search(content)
                if match is not None:
                    self.redirect_refs.append(
                        match.group(1).strip()
                    )

    def handle_endtag(self, tag: str) -> None:
        if tag == "title":
            self._in_title = False
            return

        if tag == "frameset" and self._frameset_stack:
            self._frameset_stack.pop()

    def handle_data(self, data: str) -> None:
        if self._in_title:
            self.title_parts.append(data)

    @property
    def title(self) -> str:
        return " ".join(
            html.unescape(part)
            .replace("\xa0", " ")
            .strip()
            for part in self.title_parts
            if part.strip()
        )


def build_runtime_tree(
    output_root: Path,
    volumes: list[dict[str, Any]],
    progress: ProgressCallback | None = None,
) -> dict[str, Any]:
    """
    Compile the legacy Renault shell/navigation into a normalized IR.

    Classic files stay untouched. The generated IR is additive and intended
    to become the data contract for the native Modern renderer.
    """
    output_root = output_root.resolve()
    compiled_volumes: list[dict[str, Any]] = []
    total_sections = 0

    for index, volume in enumerate(volumes, start=1):
        compiled = compile_volume_runtime(
            output_root=output_root,
            volume=volume,
        )
        compiled_volumes.append(compiled)

        section_count = int(
            compiled["modern"]["section_count"]
        )
        total_sections += section_count

        if progress is not None:
            label = (
                volume.get("document_code")
                or volume.get("title")
                or f"volume-{index}"
            )
            progress(
                "Runtime IR: "
                f"{index}/{len(volumes)} · "
                f"{label} · "
                f"{section_count} розділів"
            )

    return {
        "schema_version": RUNTIME_TREE_SCHEMA_VERSION,
        "format": "renault-runtime-ir",
        "source": "legacy-html-compiler",
        "classic_preserved": True,
        "modern_data_contract": "normalized-json",
        "compiler_phase": "section-ir-v2",
        "volume_count": len(compiled_volumes),
        "section_count": total_sections,
        "volumes": compiled_volumes,
    }


def write_runtime_tree(
    output_root: Path,
    volumes: list[dict[str, Any]],
    package_root: Path,
    progress: ProgressCallback | None = None,
) -> Path:
    data = build_runtime_tree(
        output_root=output_root,
        volumes=volumes,
        progress=progress,
    )

    target = package_root / RUNTIME_TREE_FILENAME
    target.write_text(
        json.dumps(
            data,
            ensure_ascii=False,
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )

    write_runtime_ir_shards(
        runtime_tree=data,
        package_root=package_root,
    )

    return target


def compile_volume_runtime(
    output_root: Path,
    volume: dict[str, Any],
) -> dict[str, Any]:
    output_root = output_root.resolve()

    entrypoint = str(
        volume.get("entrypoint")
        or ""
    ).strip()

    sections_result = discover_volume_sections(
        output_root=output_root,
        volume=volume,
    )

    classic = _compile_classic_shell(
        output_root=output_root,
        entrypoint=entrypoint,
    )

    section_compiler = SectionIrCompiler(
        output_root=output_root,
        volume=volume,
    )

    sections: list[dict[str, Any]] = []

    for section in sections_result["sections"]:
        compiled_section = (
            section_compiler.compile_section(
                section
            )
        )

        sections.append(
            {
                "code": section["code"],
                "title": section["title"],
                "legacy_entrypoint":
                    section["entrypoint"],
                **compiled_section,
            }
        )

    volume_documentation = (
        _compile_volume_documentation(
            sections
        )
    )

    modern: dict[str, Any] = {
        "section_source":
            sections_result.get(
                "source_file"
            ),
        "section_count":
            len(sections),
        "sections": sections,
    }

    if volume_documentation is not None:
        modern["documentation"] = (
            volume_documentation
        )

    return {
        key: value
        for key, value in {
            "id": volume.get("id"),
            "title": volume.get("title"),
            "document_code":
                volume.get("document_code"),
            "date": volume.get("date"),
            "kind": volume.get("kind"),
            "source_folder":
                volume.get("source_folder"),
            "classic": classic,
            "modern": modern,
            "compiler": {
                "phase":
                    "section-ir-v2",
                "completed": [
                    "classic-shell-topology",
                    "section-catalog",
                    "section-static-controls",
                    "section-static-routing",
                    "section-document-graph",
                    "section-asset-index",
                ],
                "pending": [
                    "native-renderer-parity",
                    "dynamic-js-patterns",
                    "cross-section-deduplication",
                ],
            },
        }.items()
        if value is not None
    }


_DOCUMENTATION_CODES = {
    "GENE",
    "PLATFUSI",
    "AIDE",
}


def _compile_volume_documentation(
    sections: list[dict[str, Any]],
) -> dict[str, Any] | None:
    """
    Hoist one documentation set per Renault volume.

    Renault volumes may contain many section menus, but GENE / PLATFUSI /
    AIDE normally point to the same resolved documentation within one volume.
    Hoisting is allowed only when every non-empty documentation signature in
    that volume is identical. Conflicting route sets stay section-local.
    """
    candidates: list[
        tuple[
            tuple[tuple[str, str, str, str], ...],
            dict[str, Any],
        ]
    ] = []

    for section in sections:
        items = _documentation_menu_items(
            section
        )
        if not items:
            continue

        signature = _documentation_signature(
            section=section,
            items=items,
        )
        if not signature:
            continue

        candidates.append(
            (
                signature,
                section,
            )
        )

    if not candidates:
        return None

    expected = candidates[0][0]

    if any(
        signature != expected
        for signature, _ in candidates[1:]
    ):
        return None

    bundle = _documentation_bundle(
        section=candidates[0][1],
    )

    if bundle is None:
        return None

    bundle["scope"] = "volume"
    bundle["verified_section_count"] = (
        len(candidates)
    )
    bundle["signature"] = [
        {
            "code": code,
            "type": action_type,
            "route_type": route_type,
            "target": target,
        }
        for (
            code,
            action_type,
            route_type,
            target,
        ) in expected
    ]
    return bundle


def _documentation_menu_items(
    section: dict[str, Any],
) -> list[dict[str, Any]]:
    controls = section.get(
        "controls",
        [],
    )

    toolbar = next(
        (
            control
            for control in controls
            if control.get("id") ==
            "menu-toolbar"
        ),
        None,
    )

    if not isinstance(toolbar, dict):
        return []

    result: list[dict[str, Any]] = []

    for item in toolbar.get(
        "items",
        [],
    ):
        label = str(
            item.get("label")
            or ""
        ).strip()

        if label.upper() in _DOCUMENTATION_CODES:
            result.append(item)

    return result


def _documentation_signature(
    section: dict[str, Any],
    items: list[dict[str, Any]],
) -> tuple[
    tuple[str, str, str, str],
    ...,
]:
    actions = {
        str(action.get("id") or ""):
            action
        for action in section.get(
            "actions",
            [],
        )
    }

    signature: list[
        tuple[str, str, str, str]
    ] = []

    for item in items:
        action_id = str(
            item.get("action_id")
            or ""
        )
        action = actions.get(action_id)

        if not isinstance(action, dict):
            return ()

        code = str(
            item.get("label")
            or ""
        ).strip().upper()

        signature.append(
            (
                code,
                str(
                    action.get("type")
                    or ""
                ),
                str(
                    action.get("route_type")
                    or ""
                ),
                str(
                    action.get("target")
                    or ""
                )
                .replace("\\", "/")
                .casefold(),
            )
        )

    return tuple(
        sorted(signature)
    )


def _documentation_bundle(
    section: dict[str, Any],
) -> dict[str, Any] | None:
    menu_items = _documentation_menu_items(
        section
    )

    if not menu_items:
        return None

    arrays = {
        key: {
            str(item.get("id") or ""):
                item
            for item in section.get(
                key,
                [],
            )
            if isinstance(item, dict)
            and str(
                item.get("id")
                or ""
            )
        }
        for key in (
            "actions",
            "panels",
            "controls",
            "documents",
        )
    }

    wanted: dict[str, set[str]] = {
        key: set()
        for key in arrays
    }

    action_queue = [
        str(item.get("action_id") or "")
        for item in menu_items
        if str(
            item.get("action_id")
            or ""
        )
    ]
    panel_queue: list[str] = []
    control_queue: list[str] = []
    document_queue: list[str] = []

    def queue_action(value: Any) -> None:
        action_id = str(value or "")
        if (
            action_id
            and action_id not in
            wanted["actions"]
        ):
            action_queue.append(
                action_id
            )

    while (
        action_queue
        or panel_queue
        or control_queue
        or document_queue
    ):
        while action_queue:
            action_id = action_queue.pop(0)
            if (
                not action_id
                or action_id in
                wanted["actions"]
            ):
                continue

            action = arrays["actions"].get(
                action_id
            )
            if action is None:
                continue

            wanted["actions"].add(
                action_id
            )

            panel_id = str(
                action.get("panel_id")
                or ""
            )
            if (
                panel_id
                and panel_id not in
                wanted["panels"]
            ):
                panel_queue.append(
                    panel_id
                )

            document_id = str(
                action.get("document_id")
                or ""
            )
            if (
                document_id
                and document_id not in
                wanted["documents"]
            ):
                document_queue.append(
                    document_id
                )

            for nested in action.get(
                "side_effect_action_ids",
                [],
            ):
                queue_action(nested)

        while panel_queue:
            panel_id = panel_queue.pop(0)
            if (
                not panel_id
                or panel_id in
                wanted["panels"]
            ):
                continue

            panel = arrays["panels"].get(
                panel_id
            )
            if panel is None:
                continue

            wanted["panels"].add(
                panel_id
            )

            for control_id in panel.get(
                "control_ids",
                [],
            ):
                control_id = str(
                    control_id or ""
                )
                if (
                    control_id
                    and control_id not in
                    wanted["controls"]
                ):
                    control_queue.append(
                        control_id
                    )

            for key in (
                "action_ids",
                "init_action_ids",
            ):
                for action_id in panel.get(
                    key,
                    [],
                ):
                    queue_action(action_id)

        while control_queue:
            control_id = (
                control_queue.pop(0)
            )
            if (
                not control_id
                or control_id in
                wanted["controls"]
            ):
                continue

            control = arrays["controls"].get(
                control_id
            )
            if control is None:
                continue

            wanted["controls"].add(
                control_id
            )

            for item in control.get(
                "items",
                [],
            ):
                if isinstance(item, dict):
                    queue_action(
                        item.get(
                            "action_id"
                        )
                    )

            for option in control.get(
                "options",
                [],
            ):
                if isinstance(
                    option,
                    dict,
                ):
                    queue_action(
                        option.get(
                            "action_id"
                        )
                    )

            for action_id in control.get(
                "action_ids",
                [],
            ):
                queue_action(action_id)

        while document_queue:
            document_id = (
                document_queue.pop(0)
            )
            if (
                not document_id
                or document_id in
                wanted["documents"]
            ):
                continue

            document = (
                arrays["documents"]
                .get(
                    document_id
                )
            )
            if document is None:
                continue

            wanted["documents"].add(
                document_id
            )

            for part in document.get(
                "parts",
                [],
            ):
                if not isinstance(
                    part,
                    dict,
                ):
                    continue
                nested_id = str(
                    part.get(
                        "document_id"
                    )
                    or ""
                )
                if (
                    nested_id
                    and nested_id not in
                    wanted["documents"]
                ):
                    document_queue.append(
                        nested_id
                    )

    selected = {
        key: [
            item
            for item in section.get(
                key,
                [],
            )
            if str(
                item.get("id")
                or ""
            ) in wanted[key]
        ]
        for key in arrays
    }

    if not selected["actions"]:
        return None

    import copy

    bundle = {
        "menu_items":
            copy.deepcopy(
                menu_items
            ),
        **{
            key:
                copy.deepcopy(value)
            for key, value in
            selected.items()
        },
    }

    id_map: dict[str, str] = {}

    for key, prefix in (
        ("actions", "action"),
        ("panels", "panel"),
        ("controls", "control"),
        ("documents", "document"),
    ):
        for item in bundle[key]:
            old = str(
                item.get("id")
                or ""
            )
            if old:
                id_map[old] = (
                    "vdoc-" +
                    prefix +
                    "-" +
                    old
                )

    _remap_documentation_ids(
        bundle,
        id_map,
    )

    return bundle


def _remap_documentation_ids(
    value: Any,
    id_map: dict[str, str],
    parent_key: str = "",
) -> Any:
    singular_keys = {
        "id",
        "action_id",
        "source_panel_id",
        "panel_id",
        "document_id",
    }
    plural_keys = {
        "action_ids",
        "side_effect_action_ids",
        "init_action_ids",
        "control_ids",
    }

    if isinstance(value, dict):
        for key, child in list(
            value.items()
        ):
            if (
                key in singular_keys
                and isinstance(
                    child,
                    str,
                )
            ):
                value[key] = id_map.get(
                    child,
                    child,
                )
                continue

            if (
                key in plural_keys
                and isinstance(
                    child,
                    list,
                )
            ):
                value[key] = [
                    id_map.get(
                        str(item),
                        str(item),
                    )
                    for item in child
                ]
                continue

            _remap_documentation_ids(
                child,
                id_map,
                key,
            )

    elif isinstance(value, list):
        for child in value:
            _remap_documentation_ids(
                child,
                id_map,
                parent_key,
            )

    return value


def _compile_classic_shell(
    output_root: Path,
    entrypoint: str,
) -> dict[str, Any]:
    if not entrypoint:
        return {
            "entrypoint": "",
            "pages": [],
            "named_frames": {},
        }

    entry_file = (
        output_root / Path(entrypoint)
    ).resolve()

    if (
        not _is_within(entry_file, output_root)
        or not entry_file.is_file()
    ):
        return {
            "entrypoint": entrypoint,
            "pages": [],
            "named_frames": {},
        }

    queue: list[Path] = [entry_file]
    visited: set[Path] = set()
    pages: list[dict[str, Any]] = []
    named_frames: dict[
        str,
        list[dict[str, str]],
    ] = {}

    # We only need the shell/topology here, not the whole document corpus.
    while queue and len(visited) < 96:
        current = queue.pop(0)

        if current in visited:
            continue
        visited.add(current)

        parsed = _parse_runtime_html(current)
        if parsed is None:
            continue

        relative_path = (
            current.relative_to(output_root)
            .as_posix()
        )

        resolved_tree = [
            _resolve_frame_node(
                node=node,
                current_file=current,
                output_root=output_root,
                queue=queue,
                named_frames=named_frames,
            )
            for node in parsed.frame_roots
        ]

        redirect_candidates: list[str] = []

        raw = _safe_read_text(current)
        raw_refs = (
            _QUOTED_HTML_RE.findall(raw)
            if raw is not None
            else []
        )

        for reference in [
            *parsed.redirect_refs,
            *raw_refs,
        ]:
            target = _resolve_reference(
                current_file=current,
                reference=reference,
                output_root=output_root,
            )

            if (
                target is None
                or not target.is_file()
                or not _is_html(target)
            ):
                continue

            target_relative = (
                target.relative_to(output_root)
                .as_posix()
            )

            if target_relative not in redirect_candidates:
                redirect_candidates.append(
                    target_relative
                )

            # Script/meta entry transitions are useful until the shell frame
            # names are discovered. Avoid recursively crawling every section.
            if (
                len(named_frames) < 3
                and target not in visited
                and target not in queue
            ):
                queue.append(target)

        pages.append(
            {
                "path": relative_path,
                "title": parsed.title,
                "frame_tree": resolved_tree,
                "redirect_candidates":
                    redirect_candidates,
            }
        )

        # Once the canonical shell is found, its direct frame children have
        # already been queued. No need to follow every arbitrary JS reference.
        canonical = {
            "titre",
            "org",
            "menu",
            "nav",
            "doc",
        }
        if canonical.issubset(
            named_frames.keys()
        ):
            queue = [
                path
                for path in queue
                if _was_frame_target(
                    path=path,
                    pages=pages,
                    output_root=output_root,
                )
            ]

    return {
        "entrypoint": entrypoint,
        "pages": pages,
        "named_frames": named_frames,
    }


def _resolve_frame_node(
    node: dict[str, Any],
    current_file: Path,
    output_root: Path,
    queue: list[Path],
    named_frames: dict[
        str,
        list[dict[str, str]],
    ],
) -> dict[str, Any]:
    resolved = dict(node)

    if node.get("type") == "frameset":
        resolved["children"] = [
            _resolve_frame_node(
                child,
                current_file,
                output_root,
                queue,
                named_frames,
            )
            for child in node.get(
                "children",
                [],
            )
        ]
        return resolved

    src = str(
        node.get("src")
        or ""
    ).strip()

    target = _resolve_reference(
        current_file=current_file,
        reference=src,
        output_root=output_root,
    )

    if target is not None and target.is_file():
        target_relative = (
            target.relative_to(output_root)
            .as_posix()
        )
        resolved["target"] = target_relative

        if (
            _is_html(target)
            and target not in queue
        ):
            queue.append(target)

        name = str(
            node.get("name")
            or ""
        ).strip()

        if name:
            entry = {
                "page":
                    current_file
                    .relative_to(output_root)
                    .as_posix(),
                "target": target_relative,
            }
            bucket = named_frames.setdefault(
                name,
                [],
            )
            if entry not in bucket:
                bucket.append(entry)

    return resolved


def _was_frame_target(
    path: Path,
    pages: list[dict[str, Any]],
    output_root: Path,
) -> bool:
    relative = path.relative_to(
        output_root
    ).as_posix()

    def walk(
        nodes: list[dict[str, Any]],
    ) -> bool:
        for node in nodes:
            if node.get("target") == relative:
                return True
            if walk(
                node.get("children", [])
            ):
                return True
        return False

    return any(
        walk(page.get("frame_tree", []))
        for page in pages
    )


def _parse_runtime_html(
    path: Path,
) -> _RuntimeHtmlParser | None:
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

    parser = _RuntimeHtmlParser()

    try:
        parser.feed(_decode_html(raw))
        parser.close()
    except Exception:
        return None

    return parser


def _safe_read_text(
    path: Path,
) -> str | None:
    try:
        return _decode_html(
            path.read_bytes()
        )
    except OSError:
        return None


def _decode_html(raw: bytes) -> str:
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


def _resolve_reference(
    current_file: Path,
    reference: str,
    output_root: Path,
) -> Path | None:
    cleaned = html.unescape(
        reference or ""
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
            "javascript:",
        )
    ):
        return None

    try:
        parsed = urlsplit(
            cleaned.replace("\\", "/")
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


def _is_html(path: Path) -> bool:
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


def _safe_size(path: Path) -> int:
    try:
        return path.stat().st_size
    except OSError:
        return 2_000_001
