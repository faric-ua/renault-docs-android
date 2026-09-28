# Renault Docs — Rotation / lifecycle audit

Date: 2026-09-25
Release: v0.5.22

## Contract

Rotating the phone must not silently move the user to a parent screen or close an app-owned window.

For every app-owned screen/window:
- the same logical content remains selected after Activity recreation;
- an open app-owned dialog reopens over the same parent screen;
- search/query state remains;
- no action is executed automatically merely because rotation occurred;
- system file/folder pickers remain system-owned and return to the recreated Activity through the existing request contract.

## Audit matrix

### MainActivity

Type: top-level Library screen.

Custom transient windows:
- none.

External/system windows:
- Android SAF dataset-folder picker.

Rotation contract:
- Library is rebuilt from persistent DatasetStore on resume;
- system picker result continues through the Activity result contract.

Status: structurally covered; phone rotation regression required.

### ConversionActivity

Type: converter setup screen.

Custom transient windows:
- none.

External/system windows:
- source folder SAF picker;
- destination folder SAF picker.

Rotation contract:
- source/destination draft is persisted in ConversionDraftStore;
- picker remains system-owned.

Status: structurally covered; phone rotation regression required.

### ModernDatasetActivity

Type: Modern volume catalog.

State:
- search query stored in `STATE_QUERY`.

Rotation contract:
- same dataset;
- same search query/filter;
- no automatic navigation.

Status: existing lifecycle contract retained.

### ModernVolumeActivity

Type: Modern section catalog.

State:
- search query stored in `STATE_QUERY`;
- search can be explicitly opened from intent.

Rotation contract:
- same volume;
- same query/filter;
- no automatic section opening caused by rotation.

Status: existing lifecycle contract retained.

### NativeSectionActivity

Type: native Modern section/detail renderer.

Previously vulnerable states:
- open panel;
- Documentation submenu;
- composite connector document;
- structured/pin/contact document;
- table-PDF save flow.

v0.5.22 state:
- `VIEW_PANEL` + panel id;
- `VIEW_DOCUMENTATION`;
- `VIEW_DOCUMENT` + document id;
- pending `NativeTablePdfData` serialized into saved instance state while Android document picker is open.

Rotation contract:
- restore the same inline child content after Runtime IR reload;
- do not fall back to the first parent panel;
- PDF save continues with the exact pre-rotation export payload.

Status: fixed in v0.5.22; phone gate required.

### SettingsActivity

App-owned dialogs:
- default open mode chooser;
- PDF default zoom chooser;
- PDF zoom-step chooser.

v0.5.22 state:
- dialog kind stored in `STATE_DIALOG_KIND`;
- the same chooser is reopened after recreation;
- dismissing it clears the saved transient state.

System window:
- backup folder SAF picker.

Status: fixed in v0.5.22; phone gate required.

### ViewerActivity

Persistent state already present:
- WebView history/state;
- page-search visibility/query;
- pending original-PDF save path;
- active Modern section code.

App-owned dialogs added to lifecycle contract:
- Modern section navigator;
- Frame debug dialog.

v0.5.22:
- active dialog kind survives rotation;
- section navigator search query survives rotation;
- frame-debug dialog is regenerated from the restored WebView instead of placing a potentially large report into the Android Bundle.

Status: fixed in v0.5.22; phone gate required.

### Android PDF/content viewer

The PDF document itself is opened through ViewerActivity/AndroidPdfLayer and remains under the Viewer lifecycle owner.

Phone gate must verify:
- current PDF remains open after rotation;
- it does not return to the parent section;
- toolbar remains usable after returning to portrait/landscape.

## Phone audit sequence

Use representative paths rather than only rotating top-level screens.

1. Library → rotate twice.
2. Modern dataset catalog with non-empty search → rotate.
3. Modern volume catalog with non-empty search → rotate.
4. Native section:
   - open `Розʼєм`;
   - rotate;
   - remain on connector window.
5. Open `Опис контактів`;
   - rotate;
   - remain on the same pin/contact document.
6. Open `Документація`;
   - rotate;
   - remain in Documentation, not the first parent panel.
7. Settings:
   - open each of the three choosers;
   - rotate with chooser open;
   - chooser must remain open.
8. Viewer:
   - open `Розділи`;
   - enter search query;
   - rotate;
   - same dialog/query must remain.
9. Viewer Frame debug:
   - open;
   - rotate;
   - dialog must reopen with regenerated report.
10. PDF:
   - open a connector/PDF document;
   - rotate and rotate back;
   - same document remains open.
11. Table export:
   - press Save table PDF;
   - rotate while Android Save dialog is open if device permits;
   - complete save;
   - output must use the same table/export payload.

Do not mark the lifecycle audit PASS until these phone gates are completed.


## Phone progress — 2026-09-25

Current real-phone session reports PASS for the NativeSection rotation checks exercised so far:
- active native child content remains selected through rotation;
- the tested connector/contact/documentation flow does not collapse back to its parent screen;
- portrait/landscape restoration is working in the tested path.

This is a partial phone PASS only. The audit remains open until Settings, Viewer, PDF and export/system-picker gates are also completed.


### Settings phone gate — PASS (2026-09-25)

Real-phone result:
- default open mode chooser survives rotation;
- PDF default zoom chooser survives rotation;
- PDF zoom-step chooser survives rotation.

Result: 3/3 PASS for Settings app-owned dialogs.


### Viewer section navigator + search — PASS (2026-09-25)

Real-phone result:
- `Розділи` dialog remains open through rotation;
- non-empty search query remains present;
- filtered navigator state remains;
- no fallback to the parent/PDF screen.

Result: PASS.


### Frame debug phone gate — NOT REACHABLE FROM CURRENT NORMAL UI (2026-09-25)

Code audit of current main/v0.5.23:
- `DBG` is rendered only when `ViewerActivity.hybridSectionMode == true`;
- that requires a non-empty `modernSectionCode`, a non-empty `modernVolumeEntrypoint`, and Viewer `entrypoint == modernVolumeEntrypoint`;
- current Modern section navigation opens `NativeSectionActivity`, not Viewer hybrid mode;
- `ModernVolumeActivity.openClassicVolume()` opens Viewer without `modernSectionCode`;
- `NativeSectionActivity.openPath()` also opens Viewer without `modernSectionCode`.

Therefore there is no ordinary user navigation path in the current app that exposes the `DBG` button.

Lifecycle gate classification: N/A for current reachable UI. Keep the code-level restoration contract, but do not block BUG-009 closeout on a phone test for an unreachable debug-only mode.


### PDF viewer rotation — PASS (2026-09-25)

Real-phone result:
- current PDF remains open through landscape/portrait rotation;
- no fallback to the parent section was observed.

Result: PASS.

### Table-PDF save / system picker handoff — PASS (2026-09-25)

Real-phone result:
- Save table PDF flow survives rotation while the Android document picker owns the foreground;
- returning to the recreated Activity does not lose the pending export payload;
- save completes successfully.

Result: PASS.

## BUG-009 closeout

All reachable app-owned child-window gates that reproduced or covered the reported rotation problem are now phone-PASS:
- NativeSection child content;
- Settings choosers;
- Viewer section navigator + search;
- current PDF;
- table-PDF save handoff.

Frame debug is not reachable from the current normal UI and is classified N/A for phone closeout.

BUG-009: CLOSED on phone for the current reachable UI. Continue ordinary regression testing of Library/catalog/system-pickers when those flows are touched, but they no longer block this bug closeout.
