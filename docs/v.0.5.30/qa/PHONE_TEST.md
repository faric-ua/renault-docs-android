# v0.5.30 Phone test — PDF 300–400% scrolling

## Prepare

1. Renault Menu → 5.
2. Renault Menu → 8.
3. Install v0.5.30.
4. If point 9 was already completed for v0.5.29, do NOT run it again.

Use the same 13-page PDF that reproduced the blank-page problem.

## Gate A — current-page tracking at 400%

Set zoom to 400% and scroll slowly through several page boundaries.

Expected:
- the counter follows the page actually under the middle of the viewport;
- it must not remain `13 / 13` while page 11 or 12 is visible;
- no page should stay as a white/loading placeholder merely because zoom is high.

## Gate B — neighbor preload

At 400%, move from one page into the next.

Expected:
- next/previous page already has a visible lightweight render;
- after scrolling stops, the current page becomes sharper;
- there may be a short quality upgrade, but not a blank-page deadlock.

## Gate C — reverse scrolling

Scroll forward 3–4 pages and then back.

Expected:
- previous pages reload automatically;
- no need to reduce zoom below 200%;
- no crash/OOM.

## Gate D — fullscreen / rotation regression

At 300–400%:
- enter fullscreen;
- scroll;
- exit fullscreen;
- rotate portrait ↔ landscape.

Expected:
- fullscreen button state remains correct;
- current page, zoom and approximate position remain stable;
- high-zoom pages continue loading.

## Closeout

v0.5.30 is PHONE PASS when 300–400% scrolling never requires lowering zoom to make pages render.
