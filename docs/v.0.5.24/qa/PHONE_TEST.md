# v0.5.24 Phone test — PDF viewer quality/performance

## Install

1. Renault Menu → 5.
2. Renault Menu → 8.
3. Install v0.5.24.
4. Do NOT run point 9.

Use the same PDF that previously looked softer/slower so the comparison is meaningful.

## Gate A — zoom quality

Open the PDF and compare:
- 100%;
- 150%;
- 200%.

Expected:
- fine text/line edges are visibly sharper than v0.5.23;
- no JPEG halo/block artifacts around black technical text/lines;
- zoom remains stable and continuous-scroll behavior remains.

If practical, compare the same page against an external PDF viewer.

## Gate B — rotation latency

While the current page is fully rendered:
1. portrait → landscape;
2. wait until stable;
3. landscape → portrait.

Expected:
- same PDF remains open;
- current page appears sooner than before;
- old rendered bitmap may remain visible while a sharper orientation-specific render finishes;
- no long white/loading blank between orientations;
- controls stay usable.

Run the rotation a second time.

Expected:
- repeat rotation is especially fast because shared cached page variants can be reused.

## Gate C — page navigation

Scroll at least 4–5 pages down and back.

Expected:
- current visible page loads before distant neighbors;
- no missing pages;
- no persistent white pages;
- continuous scroll remains natural.

## Gate D — zoom transition

From 100% → 150% → 200% → 100%.

Expected:
- current page does not blank while replacement render loads;
- new resolution eventually becomes sharp;
- nearby pages populate after the current page.

## Gate E — memory/stability

Use a multi-page PDF for several minutes:
- scroll;
- zoom;
- rotate;
- scroll back.

Expected:
- no crash/OOM;
- no permanent stale low-resolution page;
- viewer remains responsive.

## Regression

- save PDF still works;
- Back returns to correct Renault context;
- PDF rotation lifecycle PASS from BUG-009 remains;
- no point 9 required.
