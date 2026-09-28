# Renault Docs v0.5.35 — preserve ViewerActivity across rotation

Date: 2026-09-26

## Trigger

Real-phone testing showed that fullscreen state could still be lost after device rotation.

This happens with:
- one PDF in fullscreen;
- two-document Companion mode in fullscreen.

Observed:
- after rotation, app chrome/toolbars can reappear;
- PDF fullscreen button loses active state;
- user must press fullscreen again.

This proves the remaining problem is not Companion-specific.

## Root cause

ViewerActivity was being recreated on orientation changes.

Although pdfFullscreen was saved/restored in the Activity state, the real device lifecycle still allowed the new Activity/WebView presentation to race with fullscreen restoration.

The result could be a recreated viewer whose visible presentation no longer matched the fullscreen state expected by the user.

## Fix strategy

ViewerActivity now handles:
- orientation;
- screenSize

through `onConfigurationChanged()` instead of Activity recreation.

Manifest:
`android:configChanges="orientation|screenSize"`.

Consequences:
- main WebView remains alive;
- Companion WebView remains alive;
- pdfFullscreen variable remains in memory;
- page/zoom/scroll state is not rebuilt just because the phone rotates;
- split ratio remains in the same Activity instance.

## Configuration-change reconciliation

On orientation change:
1. recompute split ratio layout when Companion is open;
2. immediately reconcile fullscreen presentation;
3. post the same reconciliation after layout;
4. repeat fullscreen reconciliation after 180 ms because Android may re-show system bars during the orientation transition.

The same path applies to:
- single-document fullscreen;
- two-document fullscreen.

## Existing state persistence

SavedInstanceState support remains for actual Activity/process recreation.

The v0.5.34 reconciliation logic remains as a fallback for those cases.

## Packaging

APK/runtime UI only.

No Runtime IR/package changes.
Point 9 is not required.

## Version

- versionName: `0.5.35`
- versionCode: `51`
- branch: `fix/v0.5.35-viewer-rotation-preserve`

## Status

Implementation complete on feature branch.
CI and phone validation pending.


## Merge / CI

PR #111 merged to main as:
`1eb744ce3959d1a56cb2d9059b5069dd1b207b9c`.

Green tested source:
`14994bc4a48356d6547da07883d7367f460f0054`.

ViewerActivity / manifest / version blobs were verified identical between tested source and merged main.

CI:
- Tests `36206184286` — PASS;
- Android Debug `36206184390` — PASS;
- artifact `Renault-Docs-v0.5.35-Debug`;
- artifact id `10894415207`;
- APK SHA-256 `d565d8e0f0c9f4d1787f2ee1a3397e15502fd5766081b5ace0681ebeaa80b0ed`;
- artifact ZIP SHA-256 `1c79214b4f0a6f8257e24978098ce3a8b07e4074ad286625902f9fb02e1a8f2b`.

Status: CI PASS; phone validation pending.


## Phone result — 2026-09-26

Video evidence confirmed that v0.5.35 preserves the Activity-level fullscreen presentation across rotation, but the PDF toolbar button can still lose its active visual state.

Observed single-PDF sequence:
- fullscreen active before rotation;
- after rotation the app remains fullscreen (main app toolbar stays hidden);
- PDF fullscreen button renders inactive;
- one tap exits fullscreen and restores app toolbar.

Conclusion:
- ViewerActivity/fullscreen state itself survives;
- remaining BUG-006 is specifically WebView/PDF control state synchronization after rotation.

v0.5.36 targets this final synchronization layer with acknowledgement/retry instead of timing-only reconciliation.

Status: **PHONE FAIL — BUTTON STATE DESYNC**.
