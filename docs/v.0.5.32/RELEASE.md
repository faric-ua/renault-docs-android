# Renault Docs v0.5.32 — PDF Companion split view

Date: 2026-09-26

## Goal

Allow the user to keep a wiring-diagram PDF open while looking up the SAME volume's documentation in a second frame.

This implements the first usable PDF Companion workflow recorded in:
`docs/design/PDF_COMPANION_DOCUMENTATION.md`.

## Main PDF toolbar

When ViewerActivity has valid Modern volume context, the built-in PDF toolbar gets a new Companion control next to fullscreen.

Behavior:
- inactive: normal toolbar style;
- active: accent/pressed style;
- press once: open documentation pane below the main PDF;
- press again: close the pane without destroying its current state.

The Companion control is not shown inside the companion PDF itself, preventing recursive split panes.

## Split workspace

ViewerActivity now owns a vertical workspace:
- top: existing main WebView/PDF schematic;
- bottom: companion container;
- companion uses about 45% of the available workspace when visible.

Opening Companion does NOT recreate the main WebView.
The main PDF keeps its current document/page/zoom/scroll state and is only resized.

Companion header contains:
- Back;
- current companion title;
- Close.

Android Back behavior:
- fullscreen still exits first;
- while Companion is open, Back navigates companion history first;
- at companion root, Back closes the companion pane;
- only after that does normal main-viewer Back navigation run.

## Companion documentation root

The companion pane loads the SAME volume documentation identity already used by:
- ModernVolumeActivity → Documentation;
- NativeSectionActivity → Documentation shortcut.

It resolves:
`RuntimeIrReader.readVolumeDocumentationForVolume(... modernVolumeEntrypoint ...)`.

The embedded root renders:
- Загальна документація;
- Запобіжники;
- Довідка;
- any other items actually present in that volume.

Nested panels/controls are rendered inside the companion pane.

Final documentation targets open inside the companion WebView itself, not in the main schematic WebView.

## Companion HTML/PDF rendering

The companion uses its own:
- WebView;
- SafDatasetWebViewClient;
- AndroidPdfLayer instance;
- browser history;
- PDF state scope.

Main PDF state key scope:
`main`.

Companion PDF state key scope:
`companion`.

Therefore the same PDF path can have independent page/zoom/scroll state in each pane.

## Shared memory/render budget

Two unrestricted PDF renderers would be unsafe.

Main viewer keeps the existing quality budget:
- idle full-quality render up to 3000 px;
- active scroll tier up to 1600 px;
- normal neighbor policy.

Companion renderer uses a compact profile:
- full-quality render cap: 1800 px;
- active scroll cap: 1200 px;
- neighbor render cap: 900 px;
- decoded neighbor radius: 0 at high zoom;
- decoded retain radius: 0 at high zoom.

Both AndroidPdfLayer instances continue to share the global compressed image cache where render keys match.

The main schematic remains the primary quality target.

## Fullscreen

Fullscreen applies to the whole ViewerActivity workspace.

When Companion is open:
- app/system chrome hides as before;
- main PDF + companion pane remain visible;
- exiting fullscreen restores the same split layout.

Fullscreen state is synchronized into both PDF WebViews.

## Lifecycle

v0.5.32 persists:
- whether Companion is open;
- companion WebView history/state;
- main PDF state through the existing mechanism;
- independent companion PDF state through the `companion` state scope.

On rotation/recreation, the companion pane restores without returning automatically to documentation root when WebView state is available.

## Non-goals for this first version

Not implemented yet:
- draggable divider;
- side-by-side landscape layout;
- automatic recognition of block/fuse numbers;
- automatic linking from a schematic number to a documentation entry;
- cross-volume documentation lookup.

## Packaging

APK/runtime UI change only.

Uses the existing per-volume documentation shard from v0.5.29+.

If Renault Menu point 9 has already been completed for v0.5.29 or later, point 9 is NOT required again.

## Version

- versionName: `0.5.32`
- versionCode: `48`
- branch: `feat/v0.5.32-pdf-companion`

## Status

Implementation complete on feature branch.
CI and phone validation pending.


## Merge / CI

PR #108 merged to main as:
`41dcede6ebea2f2220ba23e363e15be6c5ea79c8`.

Green tested source:
`f22fa09949225d9f483e860165446163c32966fc`.

Key runtime/version blobs were verified identical between tested feature source and merged main:
- ViewerActivity.kt;
- AndroidPdfLayer.kt;
- SafDatasetWebViewClient.kt;
- VolumeDocumentationWebPage.kt;
- android/app/build.gradle.kts.

CI:
- Tests `36201064184` — PASS;
- Android Debug `36201064135` — PASS;
- artifact `Renault-Docs-v0.5.32-Debug`;
- artifact id `10891713605`;
- APK SHA-256 `574c7184dda4267f84d688b8e748fa0aea18b65c2e0ee5078646db3acb4880f6`;
- artifact ZIP SHA-256 `07ea758777485670cea36be5ee09a05cba73490650427a2a360e5a2f80a53112`.

Status: CI PASS; phone validation pending.


## Phone feedback — 2026-09-26

Real-phone screenshots confirmed the core Companion concept works in both portrait and landscape:
- main schematic PDF stays visible;
- companion PDF opens below;
- both PDFs have independent page/zoom state;
- fullscreen + two-document workspace is functional.

UX issue discovered:
- in dual-document fullscreen, two PDF toolbars plus the companion header consume too much vertical space, especially in landscape;
- fixed split ratio is not ideal for different lookup tasks.

Follow-up accepted for v0.5.33:
- hide document chrome by default in fullscreen split mode;
- double tap temporarily reveals controls over the documents;
- controls auto-hide again;
- add draggable divider;
- preserve fullscreen, companion state and divider ratio across rotation.

Status: **PHONE CORE PASS · UX FOLLOW-UP IN v0.5.33**.
