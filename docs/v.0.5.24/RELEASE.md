# Renault Docs v0.5.24 — PDF viewer quality + rotation cache

Date: 2026-09-25

## Trigger

Real-phone feedback after v0.5.23:
- the built-in PDF viewer looks softer than the same original PDF in a dedicated external PDF viewer when zoomed;
- rotating the phone keeps the PDF open, but the page can take noticeably long to redraw.

## Root cause

The built-in viewer is raster-based:
- Android PdfRenderer draws a PDF page into an ARGB bitmap;
- v0.5.23 then compressed that bitmap as JPEG quality 92;
- render width was capped at 1800 px;
- devicePixelRatio contribution was capped at 2;
- the rendered-page cache belonged to one AndroidPdfLayer instance, so Activity recreation on rotation discarded it.

The saved PDF itself is not rewritten by this viewer path. The quality issue is about in-app display.

## v0.5.24 quality changes

- page images are encoded as lossless PNG instead of JPEG;
- adaptive render DPR cap: 2 → 2.5;
- maximum render width: 1800 → 2400 px;
- minimum preferred render target: 700 → 800 px;
- backend render widths are grouped into 200 px buckets to reduce duplicate variants.

This improves fine technical text and line edges at 150–200% zoom without blindly rendering every page at the maximum resolution.

## v0.5.24 rotation/performance changes

Rendered PDF pages now use a process-level shared compressed-page LRU cache:
- size: 32 MiB;
- cache key includes the dataset tree URI namespace, PDF path, page and render width;
- cache therefore survives ViewerActivity / SafDatasetWebViewClient recreation during rotation;
- cache entries from different datasets cannot collide by relative path alone.

Rendering remains serialized to keep peak bitmap memory bounded.

## Visible-page priority

The browser-side viewer now:
- requests the current visible page first;
- delays neighbor page requests by 60 ms;
- keeps the previous bitmap visible while a sharper/new-width bitmap loads;
- reduces lazy prefetch root margin from 1200 px to 700 px;
- debounces orientation/viewport resize by 120 ms;
- requests a new resolution only when the target width changes by at least 100 px.

This is intended to improve perceived rotation/zoom latency without losing continuous scroll.

## Memory reasoning

A 2400 px portrait A4-style ARGB bitmap can be roughly 30–33 MiB before compression. Only one render is performed inside the shared render lock at a time. The retained cache stores compressed PNG bytes, capped at 32 MiB, rather than full bitmaps.

Phone testing must still watch for:
- slow PNG encoding on unusually image-heavy pages;
- memory pressure on very large/complex PDFs;
- whether 2400 px is enough for the user's real zoom use.

## Packaging

APK-only.
No converter/Runtime IR changes.
Point 9 is NOT required.

## Version

- versionName: `0.5.24`
- versionCode: `40`
- branch: `fix/v0.5.24-pdf-viewer-quality-cache`


## Merge / CI

Merged source:
`c8decdb903c9b48325bdeb55e2c1e1413b8844a1`

Main CI:
- Tests `36163857309` — PASS;
- Android Debug APK `36163857157` — PASS;
- artifact `Renault-Docs-v0.5.24-Debug`;
- artifact id `10876995104`;
- APK SHA-256 `2df8bef3156341710d8c4c12e93fcc9eccd5d6758834f48f881db5ef35d7cb57`;
- artifact ZIP SHA-256 `e6c9e23d7eb6fc939681bdcfa39486c671cdf3d323ab4f27d22b06d4379a995c`.

Status: CI PASS; phone quality/performance validation pending.
