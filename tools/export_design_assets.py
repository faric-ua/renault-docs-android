#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path

import cairosvg


ROOT = Path(__file__).resolve().parents[1]
DESIGN_DIR = ROOT / "docs" / "design"
PNG_SCALE = 2.0


def export_svg(svg_path: Path) -> None:
    svg_bytes = svg_path.read_bytes()
    png_path = svg_path.with_suffix(".png")
    pdf_path = svg_path.with_suffix(".pdf")

    cairosvg.svg2png(
        bytestring=svg_bytes,
        write_to=str(png_path),
        scale=PNG_SCALE,
    )
    cairosvg.svg2pdf(
        bytestring=svg_bytes,
        write_to=str(pdf_path),
    )

    print(
        f"{svg_path.relative_to(ROOT)} -> "
        f"{png_path.relative_to(ROOT)}, "
        f"{pdf_path.relative_to(ROOT)}"
    )


def main() -> None:
    svg_files = sorted(DESIGN_DIR.rglob("*.svg"))

    if not svg_files:
        print("No SVG design assets found.")
        return

    for svg_path in svg_files:
        export_svg(svg_path)


if __name__ == "__main__":
    main()
