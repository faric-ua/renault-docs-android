# Renault Docs v0.5.33 — split focus mode + draggable divider

Date: 2026-09-26

## Goal

Refine the v0.5.32 PDF Companion based on real-phone portrait/landscape use.

Problem observed:
- when two PDFs are open, both PDF toolbars plus the companion header consume too much usable document area;
- landscape is especially cramped;
- the fixed split ratio cannot adapt to the user's current lookup task.

## Fullscreen split focus mode

When BOTH conditions are true:
- PDF fullscreen is active;
- Companion is open;

the workspace enters split focus mode.

Default focus state:
- app/system chrome remains hidden by fullscreen;
- companion Android header is hidden;
- both PDF toolbars are hidden;
- documents keep the full available area;
- the divider remains visible and draggable.

The PDF toolbar is not removed. It is converted to a fixed overlay layer so showing/hiding it does not resize the PDF viewport.

## Double tap controls

Double tapping either document while split focus mode is active:
- shows both available PDF toolbars as overlays;
- shows a small Android floating control group for companion Back / Close / exit fullscreen;
- keeps the documents underneath;
- does not change page/zoom/scroll;
- auto-hides controls after about 3.2 seconds.

Another double tap restarts the visibility timeout.

When the companion content is HTML/menu rather than PDF, the Android floating controls still provide companion Back/Close while the HTML remains interactive.

## Draggable divider

A 12dp divider is placed between the main and companion panes.

Dragging it vertically changes the document ratio.

Allowed main-document share:
- minimum 20%;
- maximum 80%.

Default:
- main 55%;
- companion 45%.

The ratio is stored in Activity state and restored after rotation/recreation.

## Rotation / lifecycle

Persisted:
- fullscreen state;
- Companion open/closed state;
- main WebView state;
- companion WebView state;
- split ratio;
- whether transient split controls were visible at the moment of recreation.

If transient controls were visible before recreation, they are restored and then auto-hide again.

## Normal mode

Outside fullscreen split mode:
- existing PDF toolbars work exactly as before;
- companion header is visible;
- divider remains draggable while Companion is open;
- no auto-hide behavior is applied.

## Landscape

v0.5.33 intentionally keeps the same vertical split in landscape.

Reason:
- first validate focus mode + adjustable ratio;
- avoid introducing a second orientation-specific layout contract in the same change.

Adaptive side-by-side landscape remains a later option if real-phone use shows a clear benefit.

## Packaging

APK/runtime UI only.

No Runtime IR/package format changes.

If Renault Menu point 9 has already been run for v0.5.29 or later, point 9 is NOT required.

## Version

- versionName: `0.5.33`
- versionCode: `49`
- branch: `feat/v0.5.33-split-focus-divider`

## Status

Implementation complete on feature branch.
CI and phone validation pending.


## Merge / CI

PR #109 merged to main as:
`250b2a26a799bc88b117b82d26005f98550681fd`.

Green tested source:
`031d5b75a608fcd5a6dc7c427311f5b6476b8330`.

Key runtime/version blobs were verified identical between tested source and merged main.

CI:
- Tests `36203053082` — PASS;
- Android Debug `36203052992` — PASS;
- artifact `Renault-Docs-v0.5.33-Debug`;
- artifact id `10893250571`;
- APK SHA-256 `44ff64b86907148ea589f3415679944859e283cf92a78baa029bf7ec3baaf5d2`;
- artifact ZIP SHA-256 `58f3d4b3dac95c0c6c1d9e5e420a3977dff5d756ff1f45dd6bcd0485d330d74e`.

Status: CI PASS; phone validation pending.


## Phone feedback — 2026-09-26

Real-phone screenshots confirm the v0.5.33 UX refinement is useful:
- fullscreen split focus is significantly cleaner;
- temporary overlay controls work;
- draggable divider works in portrait and landscape;
- two-document workflow is reported convenient.

One lifecycle/state bug remains:

`BUG-006 — fullscreen button/app chrome desync after closing Companion + rotation`.

Observed:
- after fullscreen split mode, close the second document;
- rotate back to portrait;
- PDF fullscreen button can render inactive while the Activity still behaves fullscreen and the main app toolbar remains hidden;
- user must toggle fullscreen to recover a consistent state.

v0.5.34 is the targeted fix.

Status: **PHONE UX PASS WITH KNOWN BUG-006**.
