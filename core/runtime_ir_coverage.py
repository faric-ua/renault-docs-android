from __future__ import annotations

import json
from collections import Counter
from pathlib import Path
from typing import Any


RUNTIME_IR_COVERAGE_FILENAME = "runtime-ir-coverage.json"


def build_runtime_ir_coverage(
    runtime_tree: dict[str, Any],
) -> dict[str, Any]:
    global_panel_kinds: Counter[str] = Counter()
    global_control_types: Counter[str] = Counter()
    global_action_types: Counter[str] = Counter()
    global_route_types: Counter[str] = Counter()
    global_document_types: Counter[str] = Counter()
    global_menu_labels: Counter[str] = Counter()
    global_compile_states: Counter[str] = Counter()

    unsupported_actions: list[dict[str, Any]] = []
    warnings: list[dict[str, Any]] = []
    volumes_out: list[dict[str, Any]] = []

    for volume in runtime_tree.get("volumes", []):
        v_panel: Counter[str] = Counter()
        v_control: Counter[str] = Counter()
        v_action: Counter[str] = Counter()
        v_route: Counter[str] = Counter()
        v_document: Counter[str] = Counter()
        v_menu_labels: Counter[str] = Counter()
        v_states: Counter[str] = Counter()
        v_unsupported = 0
        v_warnings = 0

        sections = (
            volume.get("modern", {})
            .get("sections", [])
        )

        for section in sections:
            code = str(
                section.get("code")
                or ""
            )

            state = str(
                section.get("compile_state")
                or "unknown"
            )
            global_compile_states[state] += 1
            v_states[state] += 1

            for panel in section.get(
                "panels",
                [],
            ):
                kind = str(
                    panel.get("kind")
                    or "unknown"
                )
                global_panel_kinds[kind] += 1
                v_panel[kind] += 1

            for control in section.get(
                "controls",
                [],
            ):
                control_type = str(
                    control.get("type")
                    or "unknown"
                )
                global_control_types[
                    control_type
                ] += 1
                v_control[control_type] += 1

                if control_type == "action-bar":
                    for item in control.get(
                        "items",
                        [],
                    ):
                        label = str(
                            item.get("label")
                            or ""
                        ).strip()
                        if not label:
                            continue
                        global_menu_labels[
                            label
                        ] += 1
                        v_menu_labels[label] += 1

            for action in section.get(
                "actions",
                [],
            ):
                action_type = str(
                    action.get("type")
                    or "unknown"
                )
                global_action_types[
                    action_type
                ] += 1
                v_action[action_type] += 1

                route_type = str(
                    action.get("route_type")
                    or ""
                )
                if route_type:
                    global_route_types[
                        route_type
                    ] += 1
                    v_route[route_type] += 1

                if action_type in {
                    "legacy-javascript",
                }:
                    v_unsupported += 1
                    if (
                        len(unsupported_actions)
                        < 200
                    ):
                        unsupported_actions.append(
                            {
                                "document_code":
                                    volume.get(
                                        "document_code"
                                    ),
                                "date":
                                    volume.get(
                                        "date"
                                    ),
                                "section_code":
                                    code,
                                "label":
                                    action.get(
                                        "label"
                                    ),
                                "script":
                                    action.get(
                                        "script"
                                    ),
                            }
                        )

            for document in section.get(
                "documents",
                [],
            ):
                document_type = str(
                    document.get("type")
                    or "unknown"
                )
                global_document_types[
                    document_type
                ] += 1
                v_document[document_type] += 1

            for warning in section.get(
                "warnings",
                [],
            ):
                v_warnings += 1
                if len(warnings) < 200:
                    warnings.append(
                        {
                            "document_code":
                                volume.get(
                                    "document_code"
                                ),
                            "date":
                                volume.get(
                                    "date"
                                ),
                            "section_code":
                                code,
                            "warning":
                                warning,
                        }
                    )

        volumes_out.append(
            {
                "id": volume.get("id"),
                "document_code":
                    volume.get(
                        "document_code"
                    ),
                "date": volume.get("date"),
                "section_count":
                    len(sections),
                "compile_states":
                    _sorted_counter(
                        v_states
                    ),
                "panel_kinds":
                    _sorted_counter(
                        v_panel
                    ),
                "control_types":
                    _sorted_counter(
                        v_control
                    ),
                "action_types":
                    _sorted_counter(
                        v_action
                    ),
                "route_types":
                    _sorted_counter(
                        v_route
                    ),
                "document_types":
                    _sorted_counter(
                        v_document
                    ),
                "menu_labels":
                    _sorted_counter(
                        v_menu_labels
                    ),
                "unsupported_action_count":
                    v_unsupported,
                "warning_count":
                    v_warnings,
            }
        )

    return {
        "runtime_schema_version":
            runtime_tree.get(
                "schema_version"
            ),
        "compiler_phase":
            runtime_tree.get(
                "compiler_phase"
            ),
        "volume_count":
            len(volumes_out),
        "section_count":
            runtime_tree.get(
                "section_count"
            ),
        "compile_states":
            _sorted_counter(
                global_compile_states
            ),
        "panel_kinds":
            _sorted_counter(
                global_panel_kinds
            ),
        "control_types":
            _sorted_counter(
                global_control_types
            ),
        "action_types":
            _sorted_counter(
                global_action_types
            ),
        "route_types":
            _sorted_counter(
                global_route_types
            ),
        "document_types":
            _sorted_counter(
                global_document_types
            ),
        "menu_labels":
            _sorted_counter(
                global_menu_labels
            ),
        "unsupported_action_count":
            len(unsupported_actions),
        "unsupported_action_samples":
            unsupported_actions,
        "warning_count":
            sum(
                item["warning_count"]
                for item in volumes_out
            ),
        "warning_samples":
            warnings,
        "volumes": volumes_out,
    }


def write_runtime_ir_coverage(
    runtime_tree_path: Path,
    package_root: Path,
) -> Path:
    runtime_tree = json.loads(
        runtime_tree_path.read_text(
            encoding="utf-8"
        )
    )

    target = (
        package_root
        / RUNTIME_IR_COVERAGE_FILENAME
    )
    target.write_text(
        json.dumps(
            build_runtime_ir_coverage(
                runtime_tree
            ),
            ensure_ascii=False,
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )
    return target


def _sorted_counter(
    counter: Counter[str],
) -> dict[str, int]:
    return {
        key: counter[key]
        for key in sorted(
            counter,
            key=lambda item: (
                -counter[item],
                item,
            ),
        )
    }
