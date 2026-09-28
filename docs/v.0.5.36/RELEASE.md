# Renault Docs v0.5.36 — fullscreen button acknowledgement

Date: 2026-09-26

## Trigger

Phone video from v0.5.35 showed that Activity-level fullscreen now survives rotation, but the PDF toolbar button can still lose its active visual state.

Observed:
- fullscreen is active before rotation;
- after rotation the main app toolbar remains hidden, proving Android fullscreen state is still true;
- the PDF fullscreen button becomes visually inactive;
- tapping it once exits fullscreen and restores the main app toolbar.

Therefore the remaining issue is specifically WebView/PDF control synchronization.

## Root cause

The previous implementation sent fullscreen state from Android into WebView JavaScript but did not verify that the PDF DOM actually applied it.

During orientation transitions the WebView can briefly recreate/rebind its visual page state after Android has already sent the synchronization command.

Result:
- Android state remains correct;
- PDF button falls back to its initial aria-pressed=false presentation.

## Fix

### PDF JavaScript acknowledgement

`window.renaultSetFullscreen(enabled)` now:
- stores the expected native fullscreen state in `window.renaultNativeFullscreenExpected`;
- applies `aria-pressed`, title and aria-label;
- returns a boolean acknowledgement confirming the DOM now matches the requested state.

The PDF page also reapplies the last native fullscreen state on:
- `resize`;
- `pageshow`;
- `visibilitychange` when visible.

### Android verified synchronization

ViewerActivity no longer assumes a single JavaScript call succeeded.

For each fullscreen sync:
- capture expected Android `pdfFullscreen` state;
- call the PDF setter;
- require JavaScript to return `true`;
- if not acknowledged, retry with a short increasing delay;
- stop after 8 retries;
- cancel stale retries logically using a generation id;
- never let an old retry overwrite a newer user fullscreen toggle.

This applies independently to:
- main PDF WebView;
- companion PDF WebView.

## Existing rotation preservation

The v0.5.35 structural rotation fix remains:
- ViewerActivity handles `orientation|screenSize`;
- Activity/WebViews survive rotation;
- fullscreen state itself stays in memory.

v0.5.36 adds reliable visual/control convergence on top of that.

## Packaging

APK/runtime UI only.

No Runtime IR/package changes.
Point 9 is not required.

## Version

- versionName: `0.5.36`
- versionCode: `52`
- branch: `fix/v0.5.36-fullscreen-button-ack`

## Status

Implementation complete on feature branch.
CI and phone validation pending.


## Merge / CI

PR #112 merged to main as:
`3b6580d5a2b8d2a012a978cd828c220035dbef79`.

Green tested source:
`7d65b6e6e09362dbc43838afad279275f1522764`.

ViewerActivity / AndroidPdfLayer / version blobs were verified identical between tested source and merged main.

CI:
- Tests `36206923878` — PASS;
- Android Debug `36206923894` — PASS;
- artifact `Renault-Docs-v0.5.36-Debug`;
- artifact id `10894740860`;
- APK SHA-256 `cf89e1273d550fb1c052c9d3c5a419fed5dde17609deada8ff5a81e413d46ec9`;
- artifact ZIP SHA-256 `68dd8cb2df6983176e9865e5ed8fe5189ce99f5efacb15b61ab4069eb293bd46`.

Status: CI PASS; phone validation pending.


## Phone closeout — 2026-09-26

User confirmed v0.5.36 works correctly on phone.

Accepted:
- fullscreen button remains synchronized after rotation;
- Activity fullscreen state and PDF control state stay aligned;
- single-PDF fullscreen rotation works;
- two-document fullscreen rotation works;
- one tap exits fullscreen normally.

BUG-006 is closed.

Status: **PHONE PASS**.
