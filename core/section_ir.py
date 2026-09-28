from __future__ import annotations

import hashlib
import html
import re
from html.parser import HTMLParser
from pathlib import Path
from typing import Any
from urllib.parse import unquote, urlsplit


_TEXT_SUFFIXES = {
    ".htm",
    ".html",
}
_PDF_SUFFIXES = {".pdf"}
_IMAGE_SUFFIXES = {
    ".gif",
    ".png",
    ".jpg",
    ".jpeg",
    ".bmp",
    ".svg",
    ".ico",
}
_CHARSET_RE = re.compile(
    br"charset\s*=\s*[\"']?\s*([A-Za-z0-9._-]+)",
    re.IGNORECASE,
)
_PARENT_LOCATION_RE = re.compile(
    r"parent\.(nav|doc|menu|org)\.location(?:\.href)?\s*=\s*[\"']([^\"']+)[\"']",
    re.IGNORECASE,
)
_PARENT_TARGET_RE = re.compile(
    r"parent\.(nav|doc|menu|org)",
    re.IGNORECASE,
)
_HANDLER_RE = re.compile(
    r"([A-Za-z_$][\w$]*)\s*\(",
)


def _clean_text(parts: list[str]) -> str:
    value = " ".join(parts)
    value = html.unescape(value)
    value = value.replace("\xa0", " ")
    return re.sub(r"\s+", " ", value).strip()


class _SectionHtmlParser(HTMLParser):
    """Parse legacy Renault section HTML into static UI primitives."""

    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)

        self.title_parts: list[str] = []
        self._in_title = False

        self.body_attrs: dict[str, str] = {}

        self.anchors: list[dict[str, Any]] = []
        self._anchor_stack: list[dict[str, Any]] = []

        self.selects: list[dict[str, Any]] = []
        self._select: dict[str, Any] | None = None
        self._option: dict[str, Any] | None = None

        self.images: list[dict[str, str]] = []

        self.frame_roots: list[dict[str, Any]] = []
        self._frameset_stack: list[dict[str, Any]] = []

        self.headings: list[dict[str, Any]] = []
        self._heading: dict[str, Any] | None = None

        self.tables: list[dict[str, Any]] = []
        self._table_stack: list[dict[str, Any]] = []
        self._row: list[dict[str, Any]] | None = None
        self._cell: dict[str, Any] | None = None

    def handle_starttag(
        self,
        tag: str,
        attrs: list[tuple[str, str | None]],
    ) -> None:
        tag = tag.casefold()
        attrs_map = {
            key.casefold(): value or ""
            for key, value in attrs
        }

        if tag == "title":
            self._in_title = True
            return

        if tag == "body":
            self.body_attrs = attrs_map
            return

        if tag == "a":
            anchor = {
                "href": attrs_map.get("href", "").strip(),
                "target": attrs_map.get("target", "").strip(),
                "onclick": attrs_map.get("onclick", "").strip(),
                "text_parts": [],
                "images": [],
            }
            self.anchors.append(anchor)
            self._anchor_stack.append(anchor)
            return

        if tag == "img":
            image = {
                "src": attrs_map.get("src", "").strip(),
                "alt": attrs_map.get("alt", "").strip(),
                "width": attrs_map.get("width", "").strip(),
                "height": attrs_map.get("height", "").strip(),
            }
            self.images.append(image)
            if self._anchor_stack:
                self._anchor_stack[-1]["images"].append(image)
            return

        if tag == "select":
            select = {
                "name": attrs_map.get("name", "").strip(),
                "id": attrs_map.get("id", "").strip(),
                "onchange": attrs_map.get("onchange", "").strip(),
                "options": [],
            }
            self.selects.append(select)
            self._select = select
            return

        if tag == "option" and self._select is not None:
            option = {
                "value": attrs_map.get("value", "").strip(),
                "text_parts": [],
            }
            self._select["options"].append(option)
            self._option = option
            return

        if tag == "frameset":
            node: dict[str, Any] = {
                "type": "frameset",
                "rows": attrs_map.get("rows", "").strip(),
                "cols": attrs_map.get("cols", "").strip(),
                "children": [],
            }

            if self._frameset_stack:
                self._frameset_stack[-1]["children"].append(node)
            else:
                self.frame_roots.append(node)

            self._frameset_stack.append(node)
            return

        if tag in {"frame", "iframe"}:
            node = {
                "type": tag,
                "name": attrs_map.get("name", "").strip(),
                "src": attrs_map.get("src", "").strip(),
                "scrolling": attrs_map.get("scrolling", "").strip(),
            }

            if self._frameset_stack:
                self._frameset_stack[-1]["children"].append(node)
            else:
                self.frame_roots.append(node)
            return

        if tag in {"h1", "h2", "h3", "h4", "h5", "h6"}:
            heading = {
                "level": int(tag[1]),
                "text_parts": [],
            }
            self.headings.append(heading)
            self._heading = heading
            return

        if tag == "table":
            table = {
                "rows": [],
            }
            self.tables.append(table)
            self._table_stack.append(table)
            return

        if tag == "tr" and self._table_stack:
            row: list[dict[str, Any]] = []
            self._table_stack[-1]["rows"].append(row)
            self._row = row
            return

        if tag in {"td", "th"} and self._row is not None:
            cell = {
                "header": tag == "th",
                "text_parts": [],
            }
            self._row.append(cell)
            self._cell = cell

    def handle_endtag(self, tag: str) -> None:
        tag = tag.casefold()

        if tag == "title":
            self._in_title = False
        elif tag == "a" and self._anchor_stack:
            self._anchor_stack.pop()
        elif tag == "option":
            self._option = None
        elif tag == "select":
            self._select = None
            self._option = None
        elif tag == "frameset" and self._frameset_stack:
            self._frameset_stack.pop()
        elif tag in {"h1", "h2", "h3", "h4", "h5", "h6"}:
            self._heading = None
        elif tag in {"td", "th"}:
            self._cell = None
        elif tag == "tr":
            self._row = None
        elif tag == "table" and self._table_stack:
            self._table_stack.pop()
            self._row = None
            self._cell = None

    def handle_data(self, data: str) -> None:
        if self._in_title:
            self.title_parts.append(data)

        if self._anchor_stack:
            self._anchor_stack[-1]["text_parts"].append(data)

        if self._option is not None:
            self._option["text_parts"].append(data)

        if self._heading is not None:
            self._heading["text_parts"].append(data)

        if self._cell is not None:
            self._cell["text_parts"].append(data)

    @property
    def title(self) -> str:
        return _clean_text(self.title_parts)


class SectionIrCompiler:
    """
    Compile one legacy Renault section into declarative Modern JSON IR.

    The compiler is deliberately static: it does not execute old JavaScript.
    It extracts controls, routes and documents from the HTML contract.
    """

    def __init__(
        self,
        output_root: Path,
        volume: dict[str, Any],
    ) -> None:
        self.output_root = output_root.resolve()

        source_folder = str(
            volume.get("source_folder")
            or ""
        ).strip()

        self.volume_root = (
            self.output_root / source_folder
        ).resolve()

        self._page_cache: dict[
            Path,
            _SectionHtmlParser | None,
        ] = {}

    def compile_section(
        self,
        section: dict[str, Any],
    ) -> dict[str, Any]:
        entrypoint = str(
            section.get("entrypoint")
            or section.get("legacy_entrypoint")
            or ""
        ).strip()

        menu_file = (
            self.output_root / entrypoint
        ).resolve()

        result: dict[str, Any] = {
            "panels": [],
            "controls": [],
            "actions": [],
            "documents": [],
            "assets": [],
            "source_files": [],
            "warnings": [],
            "compile_state":
                "section-ir-v2",
        }

        self._seen_panels: set[str] = set()
        self._seen_documents: set[str] = set()
        self._seen_assets: set[str] = set()
        self._seen_sources: set[str] = set()
        self._action_counter = 0

        if not self._is_valid_file(menu_file):
            result["compile_state"] = (
                "navigation-indexed"
            )
            result["warnings"].append(
                "legacy-entrypoint-missing"
            )
            return result

        self._compile_panel(
            path=menu_file,
            result=result,
            kind="menu",
            panel_id="menu",
        )

        if not result["warnings"]:
            result.pop("warnings")

        return result

    def _compile_panel(
        self,
        path: Path,
        result: dict[str, Any],
        kind: str | None = None,
        panel_id: str | None = None,
    ) -> str | None:
        if not self._is_valid_file(path):
            return None

        relative = self._relative(path)
        panel_id = panel_id or self._panel_id(path)

        if panel_id in self._seen_panels:
            return panel_id

        parsed = self._parse(path)
        if parsed is None:
            result["warnings"].append(
                f"panel-parse-failed:{relative}"
            )
            return None

        self._seen_panels.add(panel_id)
        self._record_source(path, result)
        self._record_images(
            parsed=parsed,
            current_file=path,
            result=result,
        )

        panel: dict[str, Any] = {
            "id": panel_id,
            "kind": kind or self._panel_kind(path),
            "title": parsed.title,
            "source": relative,
            "control_ids": [],
            "action_ids": [],
        }

        init_actions = self._compile_inline_routes(
            script=parsed.body_attrs.get(
                "onload",
                "",
            ),
            current_file=path,
            result=result,
            source_panel_id=panel_id,
            event="load",
        )
        if init_actions:
            panel["init_action_ids"] = init_actions

        if parsed.selects:
            for index, select in enumerate(
                parsed.selects,
                start=1,
            ):
                control = self._compile_select(
                    parsed_select=select,
                    current_file=path,
                    result=result,
                    panel_id=panel_id,
                    index=index,
                )
                result["controls"].append(
                    control
                )
                panel["control_ids"].append(
                    control["id"]
                )

        anchor_action_ids: list[str] = []

        for anchor_index, anchor in enumerate(
            parsed.anchors,
            start=1,
        ):
            action_id = self._compile_anchor(
                anchor=anchor,
                current_file=path,
                result=result,
                source_panel_id=panel_id,
                index=anchor_index,
            )
            if action_id is not None:
                anchor_action_ids.append(
                    action_id
                )

        panel["action_ids"].extend(
            anchor_action_ids
        )

        if (
            panel["kind"] == "menu"
            and parsed.anchors
        ):
            items: list[dict[str, Any]] = []

            for anchor, action_id in zip(
                parsed.anchors,
                [
                    self._action_for_anchor(
                        anchor=anchor,
                        current_file=path,
                        result=result,
                        source_panel_id=panel_id,
                    )
                    for anchor in parsed.anchors
                ],
            ):
                if action_id is None:
                    continue
                items.append(
                    {
                        "label":
                            self._anchor_label(
                                anchor
                            ),
                        "action_id":
                            action_id,
                    }
                )

            linked_images = {
                image.get("src", "")
                for anchor in parsed.anchors
                for image in anchor.get(
                    "images",
                    [],
                )
            }

            for image in parsed.images:
                src = image.get("src", "")
                if (
                    not src
                    or src in linked_images
                ):
                    continue

                label = self._image_label(src)
                if label.casefold() == "blank":
                    items.append(
                        {
                            "label": "blank",
                            "enabled": False,
                        }
                    )

            if items:
                control_id = (
                    panel_id
                    + "-toolbar"
                )
                result["controls"].append(
                    {
                        "id": control_id,
                        "type": "action-bar",
                        "panel_id": panel_id,
                        "items": items,
                    }
                )
                panel["control_ids"].insert(
                    0,
                    control_id,
                )

        elif (
            not parsed.selects
            and anchor_action_ids
            and panel["kind"] in {
                "pc",
                "general",
            }
        ):
            control_id = (
                panel_id
                + "-documents"
            )
            result["controls"].append(
                {
                    "id": control_id,
                    "type": "document-list",
                    "panel_id": panel_id,
                    "action_ids":
                        anchor_action_ids,
                }
            )
            panel["control_ids"].append(
                control_id
            )

        result["panels"].append(panel)
        return panel_id

    def _compile_select(
        self,
        parsed_select: dict[str, Any],
        current_file: Path,
        result: dict[str, Any],
        panel_id: str,
        index: int,
    ) -> dict[str, Any]:
        name = (
            parsed_select.get("name")
            or parsed_select.get("id")
            or f"select-{index}"
        )
        control_id = (
            panel_id
            + "-"
            + self._slug(str(name))
        )

        onchange = str(
            parsed_select.get("onchange")
            or ""
        )
        handler = self._handler_name(
            onchange
        )
        target_surface = (
            self._surface_from_script(
                onchange
            )
            or "doc"
        )

        options: list[dict[str, Any]] = []

        for option_index, option in enumerate(
            parsed_select.get(
                "options",
                [],
            ),
            start=1,
        ):
            label = _clean_text(
                option.get(
                    "text_parts",
                    [],
                )
            )
            value = str(
                option.get("value")
                or ""
            ).strip()

            entry: dict[str, Any] = {
                "label": label,
                "legacy_value": value,
            }

            if self._is_non_document_option(
                value=value,
                label=label,
            ):
                entry["kind"] = (
                    self._non_document_kind(
                        label
                    )
                )
                entry["enabled"] = False
                options.append(entry)
                continue

            target, fragment = (
                self._resolve_with_fragment(
                    current_file=current_file,
                    reference=value,
                )
            )

            if target is None:
                entry["kind"] = "unknown"
                entry["enabled"] = False
                options.append(entry)
                continue

            action_id = self._add_route_action(
                current_file=current_file,
                result=result,
                source_panel_id=panel_id,
                target=target,
                target_surface=target_surface,
                label=label,
                legacy_handler=handler,
                legacy_fragment=fragment,
                event="select",
            )

            entry["kind"] = "route"
            entry["action_id"] = action_id
            entry["enabled"] = True
            options.append(entry)

        return {
            "id": control_id,
            "type": "select",
            "panel_id": panel_id,
            "name": str(name),
            "legacy_handler": handler,
            "target_surface":
                target_surface,
            "options": options,
        }

    def _compile_anchor(
        self,
        anchor: dict[str, Any],
        current_file: Path,
        result: dict[str, Any],
        source_panel_id: str,
        index: int,
    ) -> str | None:
        href = str(
            anchor.get("href")
            or ""
        ).strip()

        if not href:
            return None

        if href.casefold().startswith(
            "javascript:"
        ):
            script = href.split(
                ":",
                1,
            )[1]
            handler = self._handler_name(
                script
            )
            if handler.casefold() == "imprimer":
                return self._append_action(
                    result,
                    {
                        "type": "print",
                        "source_panel_id":
                            source_panel_id,
                        "label":
                            self._anchor_label(
                                anchor
                            )
                            or "print",
                        "legacy_handler":
                            handler,
                    },
                )
            return self._append_action(
                result,
                {
                    "type":
                        "legacy-javascript",
                    "source_panel_id":
                        source_panel_id,
                    "label":
                        self._anchor_label(
                            anchor
                        ),
                    "script": script,
                },
            )

        target, fragment = (
            self._resolve_with_fragment(
                current_file=current_file,
                reference=href,
            )
        )
        if target is None:
            return None

        target_surface = (
            str(
                anchor.get("target")
                or ""
            ).strip()
            or "self"
        )

        action_id = self._add_route_action(
            current_file=current_file,
            result=result,
            source_panel_id=source_panel_id,
            target=target,
            target_surface=target_surface,
            label=self._anchor_label(
                anchor
            )
            or f"action-{index}",
            legacy_fragment=fragment,
            event="click",
        )

        side_effect_ids = (
            self._compile_inline_routes(
                script=str(
                    anchor.get("onclick")
                    or ""
                ),
                current_file=current_file,
                result=result,
                source_panel_id=
                    source_panel_id,
                event="click-side-effect",
            )
        )

        if side_effect_ids:
            for action in result["actions"]:
                if action["id"] == action_id:
                    action["side_effect_action_ids"] = (
                        side_effect_ids
                    )
                    break

        return action_id

    def _action_for_anchor(
        self,
        anchor: dict[str, Any],
        current_file: Path,
        result: dict[str, Any],
        source_panel_id: str,
    ) -> str | None:
        href = str(
            anchor.get("href")
            or ""
        ).strip()

        label = self._anchor_label(anchor)

        for action in result["actions"]:
            if (
                action.get("source_panel_id")
                == source_panel_id
                and action.get("label")
                == label
            ):
                target = action.get(
                    "legacy_target"
                )
                if target == href:
                    return str(
                        action["id"]
                    )

        # Fallback for action labels generated from icons.
        target, _ = self._resolve_with_fragment(
            current_file=current_file,
            reference=href,
        )
        if target is None:
            return None

        relative = self._relative(target)
        for action in result["actions"]:
            if (
                action.get("source_panel_id")
                == source_panel_id
                and action.get("target")
                == relative
            ):
                return str(action["id"])

        return None

    def _add_route_action(
        self,
        current_file: Path,
        result: dict[str, Any],
        source_panel_id: str,
        target: Path,
        target_surface: str,
        label: str,
        legacy_handler: str = "",
        legacy_fragment: str = "",
        event: str = "",
    ) -> str:
        suffix = target.suffix.casefold()
        relative = self._relative(target)

        action: dict[str, Any] = {
            "type": "route",
            "source_panel_id":
                source_panel_id,
            "label": label,
            "event": event,
            "target_surface":
                target_surface,
            "target": relative,
        }

        if legacy_handler:
            action["legacy_handler"] = (
                legacy_handler
            )
        if legacy_fragment:
            action["legacy_fragment"] = (
                legacy_fragment
            )

        if (
            target_surface.casefold()
            == "nav"
            and suffix in _TEXT_SUFFIXES
        ):
            panel_id = self._compile_panel(
                path=target,
                result=result,
            )
            if panel_id:
                action["route_type"] = (
                    "open-panel"
                )
                action["panel_id"] = panel_id
            else:
                action["route_type"] = (
                    "legacy-page"
                )

        elif suffix in (
            _PDF_SUFFIXES
            | _TEXT_SUFFIXES
        ):
            document_id = (
                self._compile_document(
                    path=target,
                    result=result,
                )
            )
            action["route_type"] = (
                "open-document"
            )
            if document_id:
                action["document_id"] = (
                    document_id
                )

        else:
            action["route_type"] = (
                "resource"
            )

        return self._append_action(
            result,
            action,
        )

    def _compile_inline_routes(
        self,
        script: str,
        current_file: Path,
        result: dict[str, Any],
        source_panel_id: str,
        event: str,
    ) -> list[str]:
        action_ids: list[str] = []

        for match in _PARENT_LOCATION_RE.finditer(
            script or ""
        ):
            surface = match.group(1)
            reference = match.group(2)

            target, fragment = (
                self._resolve_with_fragment(
                    current_file=current_file,
                    reference=reference,
                )
            )
            if target is None:
                continue

            relative = self._relative(
                target
            )

            action: dict[str, Any] = {
                "type":
                    "set-surface-location",
                "source_panel_id":
                    source_panel_id,
                "event": event,
                "target_surface":
                    surface,
                "target": relative,
            }
            if fragment:
                action["legacy_fragment"] = (
                    fragment
                )

            if self._is_blank_path(
                relative
            ):
                action["semantic"] = (
                    "clear-surface"
                )

            action_ids.append(
                self._append_action(
                    result,
                    action,
                )
            )

        return action_ids

    def _compile_document(
        self,
        path: Path,
        result: dict[str, Any],
    ) -> str | None:
        if not self._is_valid_file(path):
            return None

        document_id = (
            "doc-"
            + self._stable_id(
                self._relative(path)
            )
        )

        if document_id in self._seen_documents:
            return document_id

        self._seen_documents.add(
            document_id
        )

        relative = self._relative(path)
        suffix = path.suffix.casefold()

        if suffix in _PDF_SUFFIXES:
            result["documents"].append(
                {
                    "id": document_id,
                    "type": "pdf",
                    "path": relative,
                }
            )
            return document_id

        parsed = self._parse(path)
        if parsed is None:
            return None

        self._record_source(path, result)
        self._record_images(
            parsed=parsed,
            current_file=path,
            result=result,
        )

        if parsed.frame_roots:
            parts: list[dict[str, Any]] = []

            def walk(
                nodes: list[
                    dict[str, Any]
                ],
            ) -> None:
                for node in nodes:
                    if (
                        node.get("type")
                        == "frameset"
                    ):
                        walk(
                            node.get(
                                "children",
                                [],
                            )
                        )
                        continue

                    reference = str(
                        node.get("src")
                        or ""
                    )
                    target, fragment = (
                        self._resolve_with_fragment(
                            current_file=path,
                            reference=reference,
                        )
                    )
                    if target is None:
                        continue

                    nested_id = (
                        self._compile_document(
                            target,
                            result,
                        )
                    )

                    part: dict[str, Any] = {
                        "role":
                            str(
                                node.get("name")
                                or ""
                            )
                            or "content",
                        "target":
                            self._relative(
                                target
                            ),
                    }
                    if nested_id:
                        part["document_id"] = (
                            nested_id
                        )
                    if fragment:
                        part["legacy_fragment"] = (
                            fragment
                        )
                    parts.append(part)

            walk(parsed.frame_roots)

            layout = self._frame_layout(
                parsed.frame_roots
            )

            document: dict[str, Any] = {
                "id": document_id,
                "type":
                    "composite-document",
                "path": relative,
                "title": parsed.title,
                "parts": parts,
            }
            if layout:
                document["layout"] = layout

            result["documents"].append(
                document
            )
            return document_id

        document = {
            "id": document_id,
            "type": "structured-html",
            "path": relative,
            "title": parsed.title,
            "headings": [
                {
                    "level":
                        heading["level"],
                    "text":
                        _clean_text(
                            heading[
                                "text_parts"
                            ]
                        ),
                }
                for heading in parsed.headings
                if _clean_text(
                    heading["text_parts"]
                )
            ],
            "tables":
                self._structured_tables(
                    parsed.tables,
                    source_path=path,
                ),
        }

        print_actions = []
        for anchor in parsed.anchors:
            href = str(
                anchor.get("href")
                or ""
            )
            if href.casefold().startswith(
                "javascript:imprimer"
            ):
                print_actions.append(
                    {
                        "type": "print",
                    }
                )

        if print_actions:
            document["native_actions"] = (
                print_actions
            )

        result["documents"].append(
            document
        )
        return document_id

    def _structured_tables(
        self,
        tables: list[dict[str, Any]],
        source_path: Path | None = None,
    ) -> list[dict[str, Any]]:
        result: list[dict[str, Any]] = []

        for table in tables:
            rows: list[list[dict[str, Any]]] = []
            for row in table.get(
                "rows",
                [],
            ):
                cells = []
                for cell in row:
                    text = _clean_text(
                        cell.get(
                            "text_parts",
                            [],
                        )
                    )
                    cells.append(
                        {
                            "text": text,
                            "header": bool(
                                cell.get(
                                    "header"
                                )
                            ),
                        }
                    )

                if (
                    cells
                    and any(
                        cell["text"]
                        or cell["header"]
                        for cell in cells
                    )
                ):
                    rows.append(cells)

            if rows:
                rows = (
                    self._normalize_pin_table_rows(
                        rows
                    )
                )
                result.append(
                    {
                        "rows": rows,
                    }
                )

        return result

    def _normalize_pin_table_rows(
        self,
        rows: list[list[dict[str, Any]]],
    ) -> list[list[dict[str, Any]]]:
        column_count = max(
            (
                len(row)
                for row in rows
            ),
            default=0,
        )

        if column_count != 4:
            return rows

        normalized = [
            row
            + [
                {
                    "text": "",
                    "header": False,
                }
                for _ in range(
                    column_count - len(row)
                )
            ]
            for row in rows
        ]

        first_row = normalized[0]
        if any(
            cell["header"]
            for cell in first_row
        ):
            return normalized

        # Do not rely on Renault file naming here. Real source families may
        # expose the same pin/contact table under T_*.HTM, <id>.HTM, or
        # another wrapper. Identify the table from its body shape instead:
        # pin number / wire cross-section / wire code / description.
        body_index = next(
            (
                index
                for index, row in enumerate(
                    normalized[:3]
                )
                if self._looks_like_pin_body_row(
                    row
                )
            ),
            None,
        )

        if body_index is None:
            return normalized

        semantic_header = [
            {
                "text": "№",
                "header": True,
            },
            {
                "text": "мм²",
                "header": True,
            },
            {
                "text": "Код",
                "header": True,
            },
            {
                "text": "Опис",
                "header": True,
            },
        ]

        if body_index == 0:
            return [
                semantic_header,
                *normalized,
            ]

        # Rows before the first detected body row are legacy visual headers
        # (often ordinary <td> cells and/or image-backed labels). Replace
        # that visual-only block with one stable semantic header.
        return [
            semantic_header,
            *normalized[body_index:],
        ]

    def _looks_like_pin_body_row(
        self,
        row: list[dict[str, Any]],
    ) -> bool:
        if len(row) < 4:
            return False

        values = [
            str(
                cell.get("text")
                or ""
            ).strip()
            for cell in row[:4]
        ]

        first_is_pin = bool(
            re.fullmatch(
                r"[A-Za-z]?\d+[A-Za-z0-9.-]*",
                values[0],
            )
        )
        second_is_cross_section = bool(
            re.fullmatch(
                r"\d+(?:[.,]\d+)?",
                values[1],
            )
        )
        third_is_wire_code = (
            values[2] == ""
            or bool(
                re.fullmatch(
                    r"[A-Za-z0-9.+/_-]{1,16}",
                    values[2],
                )
            )
        )

        return (
            values[3] != ""
            and first_is_pin
            and second_is_cross_section
            and third_is_wire_code
        )

    def _record_images(
        self,
        parsed: _SectionHtmlParser,
        current_file: Path,
        result: dict[str, Any],
    ) -> None:
        for image in parsed.images:
            reference = str(
                image.get("src")
                or ""
            )
            target, _ = (
                self._resolve_with_fragment(
                    current_file=current_file,
                    reference=reference,
                )
            )
            if target is None:
                continue

            relative = self._relative(
                target
            )
            if relative in self._seen_assets:
                continue
            self._seen_assets.add(relative)

            upper = relative.upper()
            if "/BOUTONS/" in upper:
                role = "button-icon"
            elif "/VIGNETTE/" in upper:
                role = "thumbnail"
            elif "/ICONES/" in upper:
                role = "content-icon"
            else:
                role = "image"

            result["assets"].append(
                {
                    "type": "image",
                    "role": role,
                    "path": relative,
                }
            )

    def _record_source(
        self,
        path: Path,
        result: dict[str, Any],
    ) -> None:
        relative = self._relative(path)
        if relative in self._seen_sources:
            return
        self._seen_sources.add(relative)
        result["source_files"].append(
            relative
        )

    def _parse(
        self,
        path: Path,
    ) -> _SectionHtmlParser | None:
        if path in self._page_cache:
            return self._page_cache[path]

        if (
            not self._is_valid_file(path)
            or path.suffix.casefold()
            not in _TEXT_SUFFIXES
        ):
            self._page_cache[path] = None
            return None

        try:
            raw = path.read_bytes()
        except OSError:
            self._page_cache[path] = None
            return None

        parser = _SectionHtmlParser()
        try:
            parser.feed(
                self._decode_html(raw)
            )
            parser.close()
        except Exception:
            self._page_cache[path] = None
            return None

        self._page_cache[path] = parser
        return parser

    def _resolve_with_fragment(
        self,
        current_file: Path,
        reference: str,
    ) -> tuple[
        Path | None,
        str,
    ]:
        cleaned = html.unescape(
            reference or ""
        ).strip()

        if not cleaned:
            return None, ""

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
            return None, ""

        try:
            parsed = urlsplit(
                cleaned.replace(
                    "\\",
                    "/",
                )
            )
        except ValueError:
            return None, ""

        path_text = unquote(
            parsed.path
        ).strip()
        if not path_text:
            return None, parsed.fragment

        if path_text.startswith("/"):
            target = (
                self.output_root
                / path_text.lstrip("/")
            ).resolve()
        else:
            target = (
                current_file.parent
                / path_text
            ).resolve()

        if not self._is_within(
            target,
            self.output_root,
        ):
            return None, parsed.fragment

        return target, parsed.fragment

    def _panel_kind(
        self,
        path: Path,
    ) -> str:
        upper_parts = [
            part.upper()
            for part in path.parts
        ]

        if "SCH" in upper_parts:
            return "schematic"
        if "NM" in upper_parts:
            return "nomenclature"
        if "PC" in upper_parts:
            return "pc"
        if path.stem.upper() == "GENERAL":
            return "general"
        return "legacy-panel"

    def _panel_id(
        self,
        path: Path,
    ) -> str:
        kind = self._panel_kind(path)
        if kind != "legacy-panel":
            return kind

        return (
            "panel-"
            + self._stable_id(
                self._relative(path)
            )
        )

    def _append_action(
        self,
        result: dict[str, Any],
        action: dict[str, Any],
    ) -> str:
        self._action_counter += 1
        action_id = (
            "action-"
            + str(self._action_counter)
        )
        action["id"] = action_id

        result["actions"].append(
            action
        )
        return action_id

    def _anchor_label(
        self,
        anchor: dict[str, Any],
    ) -> str:
        text = _clean_text(
            anchor.get(
                "text_parts",
                [],
            )
        )
        if text:
            return text

        images = anchor.get(
            "images",
            [],
        )
        if images:
            src = str(
                images[0].get("src")
                or ""
            )
            if src:
                return self._image_label(
                    src
                )

        return ""

    def _image_label(
        self,
        src: str,
    ) -> str:
        return Path(
            src.replace("\\", "/")
        ).stem

    def _handler_name(
        self,
        script: str,
    ) -> str:
        match = _HANDLER_RE.search(
            script or ""
        )
        return (
            match.group(1)
            if match
            else ""
        )

    def _surface_from_script(
        self,
        script: str,
    ) -> str:
        match = _PARENT_TARGET_RE.search(
            script or ""
        )
        return (
            match.group(1)
            if match
            else ""
        )

    def _is_non_document_option(
        self,
        value: str,
        label: str,
    ) -> bool:
        normalized = value.casefold()
        if (
            "erreur.htm" in normalized
            or "blank.htm" in normalized
        ):
            return True

        if not value.strip():
            return True

        return False

    def _non_document_kind(
        self,
        label: str,
    ) -> str:
        stripped = label.strip()
        if (
            stripped
            and set(stripped) <= {"-"}
        ):
            return "separator"

        upper = stripped.upper()
        if upper.startswith(
            (
                "ВЫБЕРИТЕ",
                "SELECT",
                "CHOISISSEZ",
            )
        ):
            return "prompt"

        return "group"

    def _is_blank_path(
        self,
        relative: str,
    ) -> bool:
        return relative.casefold().endswith(
            "/blank.htm"
        )

    def _frame_layout(
        self,
        roots: list[dict[str, Any]],
    ) -> dict[str, str] | None:
        if len(roots) != 1:
            return None

        root = roots[0]
        if root.get("type") != "frameset":
            return None

        result: dict[str, str] = {}
        rows = str(
            root.get("rows")
            or ""
        ).strip()
        cols = str(
            root.get("cols")
            or ""
        ).strip()

        if rows:
            result["rows"] = rows
        if cols:
            result["cols"] = cols

        return result or None

    def _relative(
        self,
        path: Path,
    ) -> str:
        return path.resolve().relative_to(
            self.output_root
        ).as_posix()

    def _is_valid_file(
        self,
        path: Path,
    ) -> bool:
        try:
            path.resolve().relative_to(
                self.output_root
            )
        except ValueError:
            return False
        return path.is_file()

    def _is_within(
        self,
        path: Path,
        root: Path,
    ) -> bool:
        try:
            path.relative_to(root)
        except ValueError:
            return False
        return True

    def _stable_id(
        self,
        value: str,
    ) -> str:
        return hashlib.sha1(
            value.encode("utf-8")
        ).hexdigest()[:12]

    def _slug(
        self,
        value: str,
    ) -> str:
        cleaned = re.sub(
            r"[^A-Za-z0-9_-]+",
            "-",
            value.strip(),
        ).strip("-")
        return cleaned or "control"

    def _decode_html(
        self,
        raw: bytes,
    ) -> str:
        match = _CHARSET_RE.search(
            raw[:8192]
        )

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
                encodings.append(
                    declared
                )

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
                return raw.decode(
                    encoding
                )
            except (
                LookupError,
                UnicodeDecodeError,
            ):
                continue

        return raw.decode(
            "utf-8",
            errors="replace",
        )
