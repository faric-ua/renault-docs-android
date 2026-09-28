# Renault Docs v0.5.25 — independent PDF controls + high-zoom multi-page scroll

Date: 2026-09-25

## Phone trigger

Real-phone v0.5.24 result:
- PDF quality: ++;
- first rotation: +;
- repeated rotation: ++;
- 100% multi-page scroll: acceptable;
- 200% multi-page scroll on ~20-page documents: still not smooth enough.

New UX request:
- PDF controls must be visually and behaviorally independent from the PDF document;
- document pan/scroll/zoom must not move or hide the controls;
- portrait PDF controls should use two rows;
- landscape PDF controls should use one row.

## Independent PDF viewport

The PDF HTML shell is now split into:
1. fixed control area;
2. independent scrollable PDF viewport.

The document viewport owns:
- vertical page scrolling;
- horizontal panning for zoomed pages;
- current-page observation.

The toolbar no longer expands to the zoomed PDF width and no longer scrolls with the document.

## Responsive controls

Portrait:
- exactly two logical rows:
  - row 1: previous page / page counter / next page / save PDF;
  - row 2: zoom out / zoom picker / zoom in / fit width.

Landscape:
- the two logical groups are placed on one horizontal row;
- toolbar may horizontally scroll only if an unusually narrow landscape viewport cannot fit every control.

## High-zoom multi-page optimization

At zoom >= 150%:
- while the user is actively scrolling, pages use a faster temporary render tier capped at 1600 px;
- after 180 ms of scroll idle, the current page upgrades to the full v0.5.24 quality target (up to 2400 px);
- an already sharper page is never intentionally downgraded;
- only one neighboring page on each side is prefetched;
- only current ±1 decoded page images are retained in the DOM;
- far page image src values are evicted while their page placeholders remain;
- returning to an evicted page reuses the shared compressed page cache when available.

Below 150%:
- up to two neighbors are prefetched;
- a wider current ±4 decoded-page window is retained.

Lazy observer root margin is reduced to 300 px and is scoped to the PDF viewport.

Goal:
- reduce WebView decoded-image pressure and render queue churn on 20+ page documents at 200%;
- preserve the v0.5.24 sharpness improvement.

## Rotation state for nested viewport

Because the document now scrolls inside its own viewport, the viewer stores a short-lived state per dataset + PDF path:
- current page;
- zoom;
- page-relative vertical position inside the current page;
- proportional horizontal pan position.

The state is restored only when newer than 5 minutes. Vertical/horizontal positions are stored proportionally, so portrait ↔ landscape geometry changes do not apply stale absolute pixels. This protects the existing rotation lifecycle behavior without making every future PDF open resume an old reading position indefinitely.

## Existing v0.5.24 quality path retained

- PdfRenderer;
- lossless PNG;
- DPR cap 2.5;
- max render width 2400 px;
- process-level 32 MiB compressed page cache.

## Packaging

APK-only.
No converter / Runtime IR changes.
Point 9 is NOT required.

## Version

- versionName: `0.5.25`
- versionCode: `41`
- branch: `fix/v0.5.25-pdf-toolbar-scroll`


## Merge / CI

Merged source:
`6e962b5bac1bdee93ba232f55ee594f318ab2f8b`

Main CI:
- Tests `36167218686` — PASS;
- Android Debug APK `36167218706` — PASS;
- artifact `Renault-Docs-v0.5.25-Debug`;
- artifact id `10878087586`;
- APK SHA-256 `205f031bb889826f0b3b7ae3d76318627c203ed0cb3829b60ac611b0380c625b`;
- artifact ZIP SHA-256 `34b5508b7b7006f4c3e35f0e2839de8729a660bba16109caad70bef91194976e`.

Status: CI PASS; phone validation pending.
