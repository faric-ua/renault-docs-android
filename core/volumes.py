from __future__ import annotations

import re
import unicodedata
from pathlib import Path
from typing import Any

from core.dataset_manifest import discover_entrypoint


DATE_RE = re.compile(r"(?P<year>20\d{2})[._-](?P<month>\d{2})[._-](?P<day>\d{2})")
NT_RE = re.compile(r"(?:^|[^A-Z0-9])NT(?P<number>\d{4}[A-Z]?)(?=$|[^A-Z0-9])", re.IGNORECASE)
GROUPED_VEHICLE_CODE_RE = re.compile(
    r"(?<![A-Z0-9])(?P<letters>[A-Z](?:\s*[,/]\s*[A-Z])+)[ ]*(?P<series>\d{2})(?!\d)",
    re.IGNORECASE,
)
DIRECT_VEHICLE_CODE_RE = re.compile(
    r"(?<![A-Z0-9])(?P<prefix>[A-Z])(?P<series>\d{2})(?![A-Z0-9])",
    re.IGNORECASE,
)
VISU_RE = re.compile(
    r"(?<![A-Z0-9])(?P<type>VISU)\s*[- ]?\s*[Vv]?\s*(?P<version>\d+(?:\.\d+)*)(?!\d)",
    re.IGNORECASE,
)
EUROPE_RE = re.compile(r"(?<![A-Z0-9])EUROPE(?![A-Z0-9])", re.IGNORECASE)


def infer_volume_identity(
    *values: object,
) -> dict[str, Any]:
    source = " ".join(
        str(value)
        for value in values
        if value is not None
        and str(value).strip()
    )

    document_type, document_version = (
        _extract_document_identity(
            source,
        )
    )

    identity = {
        "document_code": _extract_nt_code(
            source,
        ),
        "date": _extract_date(
            source,
        ),
        "vehicle_codes": _extract_vehicle_codes(
            source,
        ),
        "document_type": document_type,
        "document_version": document_version,
        "region": _extract_region(
            source,
        ),
    }

    return {
        key: value
        for key, value in identity.items()
        if value not in (
            None,
            "",
            [],
        )
    }


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
        vehicle_codes = _extract_vehicle_codes(folder_name)
        document_type, document_version = _extract_document_identity(folder_name)
        if document_type is None:
            document_type = _infer_document_type_from_entrypoint(
                child / entrypoint,
            )
        region = _extract_region(folder_name)
        is_visu = document_type == "Visu" or "visu" in folder_name.casefold()

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
            "vehicle_codes": vehicle_codes or None,
            "document_type": document_type,
            "document_version": document_version,
            "region": region,
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



def _extract_vehicle_codes(name: str) -> list[str]:
    codes: list[str] = []

    for match in GROUPED_VEHICLE_CODE_RE.finditer(name):
        series = match.group("series")
        for prefix in re.split(r"\s*[,/]\s*", match.group("letters")):
            clean = prefix.strip().upper()
            if len(clean) == 1 and clean.isalpha():
                code = clean + series
                if code not in codes:
                    codes.append(code)

    for match in DIRECT_VEHICLE_CODE_RE.finditer(name):
        code = match.group("prefix").upper() + match.group("series")
        if code not in codes:
            codes.append(code)

    return codes


def _extract_document_identity(name: str) -> tuple[str | None, str | None]:
    match = VISU_RE.search(name)
    if not match:
        return None, None

    return "Visu", match.group("version")


def _infer_document_type_from_entrypoint(path: Path) -> str | None:
    try:
        raw = path.read_bytes()[: 64 * 1024]
    except OSError:
        return None

    text = raw.decode("latin-1", errors="ignore")
    if re.search(
        r"<title[^>]*>\s*Visu\s+Schema\b",
        text,
        re.IGNORECASE | re.DOTALL,
    ):
        return "Visu"

    return None


def _extract_region(name: str) -> str | None:
    if EUROPE_RE.search(name):
        return "Europe"
    return None

def _slugify(value: str) -> str:
    normalized = unicodedata.normalize("NFKD", value)
    ascii_value = normalized.encode("ascii", "ignore").decode("ascii")
    slug = re.sub(r"[^a-zA-Z0-9]+", "-", ascii_value).strip("-").lower()
    return slug or "volume"
