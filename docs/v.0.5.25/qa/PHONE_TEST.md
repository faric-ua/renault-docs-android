# v0.5.25 Phone test — PDF controls + 200% scroll

## Install

1. Renault Menu → 5.
2. Renault Menu → 8.
3. Install v0.5.25.
4. Do NOT run point 9.

Use the same ~20-page PDF that was tested in v0.5.24.

## Gate A — portrait controls

Portrait expected:
- PDF controls stay visible while the document scrolls vertically;
- PDF controls do not move when the PDF is panned horizontally at 150–200%;
- controls are arranged in two rows;
- row 1: ↑ / page counter / ↓ / save;
- row 2: − / zoom / ＋ / fit width.

## Gate B — landscape controls

Rotate to landscape.

Expected:
- controls become one horizontal row;
- same PDF/page/zoom state is retained;
- document scroll remains independent under the controls.

Rotate back to portrait.

Expected:
- controls return to two rows;
- reading position remains approximately the same.

## Gate C — 200% multi-page scroll

Set 200%.

Scroll through at least 8–10 pages of a ~20-page document, then back several pages.

Expected:
- visibly smoother than v0.5.24;
- while moving quickly, a page may briefly use the 1600 px interactive tier and sharpen after scrolling stops;
- no accumulating slowdown as more pages are visited;
- nearby next/previous pages appear without long blank periods;
- far pages may briefly show their loading placeholder when revisited, but should reload from cache quickly;
- no OOM/crash.

## Gate D — 100% regression

At 100%:
- continuous scroll remains at least as good as v0.5.24;
- page counter updates correctly;
- no unexpected page eviction while casually scrolling.

## Gate E — quality/rotation regression

At 150–200%:
- v0.5.24 sharpness remains;
- first rotation remains acceptable;
- repeat rotation remains fast;
- nested document viewport does not reset to page 1 after rotation.

## Regression

- save PDF works;
- Back works;
- zoom picker opens above the fixed control area;
- pinch behavior is not intentionally disabled;
- no converter refresh / point 9.
