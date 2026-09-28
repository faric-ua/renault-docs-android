# Renault Docs v0.5.34 — fullscreen state reconciliation

Date: 2026-09-26

## Trigger

Real-phone v0.5.33 testing found one lifecycle/UI desynchronization after the otherwise successful split-focus workflow.

Reproduction:
1. open main schematic PDF;
2. open Companion;
3. enter fullscreen split mode;
4. close the second document;
5. rotate back to portrait;
6. observe that Android chrome can remain fullscreen-hidden while the PDF fullscreen button renders inactive.

The user then has to toggle fullscreen again to get the app toolbar/menu back.

## Root cause

Fullscreen presentation had two observable states:
- Android Activity/window chrome controlled by `pdfFullscreen`;
- PDF toolbar button state controlled by JavaScript inside the WebView.

During Activity recreation, Android fullscreen presentation was applied before the main WebView state was restored. The restored PDF page could therefore replace the JavaScript state after Android had already synchronized the button.

Closing Companion also changed split-focus presentation without forcing a full reconciliation of the remaining single-document fullscreen UI.

Result: the Activity could still behave as fullscreen while the PDF button visually looked inactive.

## Fix

Add `reconcilePdfFullscreenPresentation()` as the canonical presentation reconciliation step.

It:
- treats Android `pdfFullscreen` as the source of truth;
- applies Android app/system chrome visibility from that state;
- synchronizes the PDF fullscreen button;
- synchronizes Companion state;
- reapplies split-focus presentation.

Lifecycle changes:
- do not apply fullscreen before main WebView restore;
- reconcile immediately after restore/load setup;
- post another reconciliation on the main WebView event queue;
- WebChrome title callbacks reconcile again after PDF JavaScript is available;
- `onWindowFocusChanged(true)` reconciles BOTH fullscreen=true and fullscreen=false;
- closing Companion reconciles the surviving main PDF without changing `pdfFullscreen`.

## Intended behavior

Closing Companion never toggles fullscreen by itself.

If fullscreen was active:
- main document stays fullscreen;
- app toolbar remains hidden;
- fullscreen button remains visibly active.

If fullscreen was not active:
- app toolbar is visible;
- fullscreen button is inactive.

Rotation preserves whichever state was actually active.

There should no longer be an intermediate state where:
- app toolbar is hidden;
- fullscreen button is inactive.

## Packaging

APK/runtime UI only.

No Runtime IR/package changes.
Point 9 is not required.

## Version

- versionName: `0.5.34`
- versionCode: `50`
- branch: `fix/v0.5.34-fullscreen-state-reconcile`

## Status

Implementation complete on feature branch.
CI and phone validation pending.


## Merge / CI

PR #110 merged to main as:
`adb226f605c3730037e945fe0e6ce95275981710`.

Green tested source:
`bf23eb88d39565559d61caa40ac9373fac8feaab`.

ViewerActivity + version blobs were verified identical between tested source and merged main.

CI:
- Tests `36204766057` — PASS;
- Android Debug `36204766049` — PASS;
- artifact `Renault-Docs-v0.5.34-Debug`;
- artifact id `10893043078`;
- APK SHA-256 `f45ebd66dd5d4c292f4808f63f0aac446d833798205db16d97f36fe5fe23896f`;
- artifact ZIP SHA-256 `0efb0024c516538e0a2223603fc683c13c7b720463f0c2042d4dadad7767ac6c`.

Status: CI PASS; phone validation pending.
