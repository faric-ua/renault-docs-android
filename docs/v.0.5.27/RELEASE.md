# Renault Docs v0.5.27 — live PDF pinch zoom

Date: 2026-09-25

## Goal

Keep the existing built-in raster/PdfRenderer architecture, but make zoom behave like a normal touch PDF viewer:
- pinch changes document scale immediately;
- the visible zoom percentage updates during the gesture;
- the PDF toolbar stays fixed and never participates in document zoom;
- expensive high-resolution rendering waits until the zoom interaction settles.

## Interaction contract

The PDF toolbar and document remain independent layout regions.

Pinch behavior:
1. two-finger pinch is handled only inside `#pdfViewport`;
2. WebView/browser page zoom is disabled for the generated PDF page;
3. the existing decoded page bitmap scales immediately through CSS width;
4. `zoomInput` updates live with the current integer percentage;
5. the point under the pinch center remains anchored to the same PDF page region as closely as the document layout permits;
6. after about 180 ms without further zoom movement, the current page is re-requested from `PdfRenderer` at the quality required for the final zoom;
7. current page is prioritized, then neighboring pages follow through the existing bounded prefetch policy.

The existing zoom range remains 50–200%.

## Render-load protection

While a pinch is active:
- lazy observer render requests are suppressed;
- current-page observer does not start render work;
- nested scroll render work is suppressed;
- queued neighbor prefetch and scroll-idle render timers are cancelled at pinch start.

This prevents a sequence such as 141% → 142% → 143% from launching repeated expensive page renders.

## Toolbar contract

The toolbar remains outside `#pdfViewport`.
Portrait remains two toolbar rows.
Landscape remains one toolbar row.

Pinch changes only the PDF document. It does not scale:
- toolbar buttons;
- page counter;
- zoom field;
- save control.

## Existing behavior preserved

- +/- buttons and typed zoom use the same zoom value;
- fit width returns to 100%;
- 200% high-zoom scroll optimization remains;
- lossless PNG rendering remains;
- shared compressed render cache remains;
- rotation/page-relative restore remains;
- original PDF save/export remains.

## Packaging

This is APK/UI runtime work only.
Converter, Runtime IR and dataset package formats do not change.

Renault Menu point 9 is NOT required for v0.5.27 testing when the dataset was already regenerated for v0.5.26.

## Version

- versionName: `0.5.27`
- versionCode: `43`
- branch: `feat/v0.5.27-pdf-pinch-zoom`

## Validation status

Implementation complete on feature branch.
CI and real-phone validation pending.


## Merge / CI

PR #103 merged to main as:
`7660416ad3f00a7fdf3d7556b6fd11a0bfd1186f`.

Green tested feature source:
`46ba0c0df464d144158cd5a696611f2366198d75`.

Runtime source equivalence was verified after squash merge:
- `AndroidPdfLayer.kt` blob SHA matches feature source and merged main;
- `android/app/build.gradle.kts` blob SHA matches feature source and merged main.

CI:
- Tests `36176557305` — PASS;
- Android Debug APK `36176557288` — PASS;
- artifact `Renault-Docs-v0.5.27-Debug`;
- artifact id `10882358464`;
- APK SHA-256 `03dca9deb5fda72e4fa5d72eae18f94285c451e04bd3b78157bf97fd19109ac8`;
- artifact ZIP SHA-256 `b0f79a703f60fd1255fb90a9724bce8e39196bbcf694579c83a0e2416e5406ae`.

Status: CI PASS; real-phone pinch/toolbar/quality validation pending.
