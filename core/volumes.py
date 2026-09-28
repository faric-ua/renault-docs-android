from __future__ import annotations

import re
import unicodedata
from pathlib import Path
from typing import Any

from core.dataset_manifest import discover_entrypoint


DATE_RE = re.compile(r"(?P<year>20\d{2})[._-](?P<month>\d{2})[._-](?P<day>\d{2})")
NT_RE = re.compile(r"(?:^|[^A-Z0-9])NT(?P<number>\d{4}[A-Z]?)(?=$|[^A-Z0-9])", re.IGNORECASE)


def discover_volumes(root: Path) -> list[dict[str, Any]]:
    """Discover top-level documentation volumes with their own entrypoint."""
    volumes: list[dict[str, Any]] = []

    if not root.is_dir():
        return volumes

    for child in sorted(root.iterdir(), key=lambda p: p.name.casefold()):
        if not child.is_dir():
            continue

        entrypoint = discover_entrypoint(child)
        if not entrypoint:
            continue

        folder_name = child.name
        date = _extract_date(folder_name)
        document_code = _extract_nt_code(folder_name)
        is_visu = "visu" in folder_name.casefold()

        relative_entrypoint = (child.relative_to(root) / entrypoint).as_posix()
        title_parts: list[str] = []

        if document_code:
            title_parts.append(document_code)
        elif is_visu:
            title_parts.append("VISU")
        else:
            title_parts.append(folder_name)

        if date:
            title_parts.append(date)

        volume = {
            "id": _slugify(folder_name),
            "title": " · ".join(title_parts),
            "source_folder": folder_name,
            "entrypoint": relative_entrypoint,
            "document_code": document_code,
            "date": date,
            "kind": "wiring-diagrams" if is_visu else "technical-documentation",
        }

        volumes.append({key: value for key, value in volume.items() if value is not None})

    volumes.sort(
        key=lambda volume: (
            volume.get("date") is None,
            volume.get("date") or "",
            str(
                volume.get("document_code")
                or volume.get("title")
                or ""
            ).casefold(),
        )
    )

    return volumes


def _extract_date(name: str) -> str | None:
    match = DATE_RE.search(name)
    if not match:
        return None

    return (
        f"{match.group('year')}-"
        f"{match.group('month')}-"
        f"{match.group('day')}"
    )


def _extract_nt_code(name: str) -> str | None:
    match = NT_RE.search(name)
    if not match:
        return None
    return "NT" + match.group("number").upper()


def _slugify(value: str) -> str:
    normalized = unicodedata.normalize("NFKD", value)
    ascii_value = normalized.encode("ascii", "ignore").decode("ascii")
    slug = re.sub(r"[^a-zA-Z0-9]+", "-", ascii_value).strip("-").lower()
    return slug or "volume"
