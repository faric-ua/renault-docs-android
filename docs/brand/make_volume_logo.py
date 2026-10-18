#!/usr/bin/env python3
"""Build docs/brand/volume-logo.gif from the owner's emblem №1 (TODO №26).

History:
  * 2026-10-18 00:56 — first version: emblem on a solid yellow background.
    GIF has only 256 colours, and quantisation turned the page yellow
    (255, 255, 0) into (252, 252, 0). On the phone (Renodocs, Classic start
    page of Laguna II, 02:55) a slightly different yellow square was visible
    around the emblem.
  * 2026-10-18 03:05 — this script: the background is *transparent*, so the
    page shows through and there is no square at all. Semi-transparent glow
    pixels are blended onto the page yellow (GIF transparency is on/off only),
    so the edge still looks right on the yellow page.

Usage: python3 docs/brand/make_volume_logo.py [out.gif]
"""

from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
SOURCE = HERE / "emblems" / "emblem-1.png"
OUT = HERE / "volume-logo.gif"
SIZE = 400
PAGE_YELLOW = (255, 255, 0)  # start page of Laguna II volumes
ALPHA_CUTOFF = 24  # below this the pixel becomes fully transparent


def build(source: Path = SOURCE) -> Image.Image:
    emblem = Image.open(source).convert("RGBA").resize((SIZE, SIZE), Image.LANCZOS)
    alpha = emblem.getchannel("A")
    matte = Image.new("RGBA", emblem.size, PAGE_YELLOW + (255,))
    matte.alpha_composite(emblem)
    rgb = matte.convert("RGB")
    # 255 colours for the picture, index 255 reserved for "transparent".
    picture = rgb.quantize(colors=255, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
    palette = picture.getpalette()[: 255 * 3]
    palette += [0, 0, 0] * (256 - len(palette) // 3)
    picture.putpalette(palette)
    mask = alpha.point(lambda a: 255 if a < ALPHA_CUTOFF else 0)
    picture.paste(255, mask=mask)
    return picture


def main(argv: list[str]) -> int:
    out = Path(argv[1]) if len(argv) > 1 else OUT
    image = build()
    image.save(out, format="GIF", transparency=255, optimize=False)
    print(f"{out}: {out.stat().st_size} bytes")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
