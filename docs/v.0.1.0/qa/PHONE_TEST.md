# Phone Test — Renault Docs v0.1.0

Status: NOT TESTED

## Device

To be filled during real-phone QA.

## Routes

### Route A — empty library

Install APK → launch → Library.

Expected:
- no crash;
- Add dataset button visible;
- no storage permission popup outside SAF.

### Route B — add Laguna dataset

Library → Add dataset → Android folder picker → choose:
`laguna 2 2001-2006_android`

Expected:
- app reads `renault-dataset.json`;
- tile shows Laguna II / 2001–2006 / X74;
- selection is persisted.

### Route C — cancel SAF

Library → Add dataset → Cancel.

Expected:
- return to same Library;
- no fake success;
- no broken tile.

### Route D — recreation

With Laguna tile visible:
- rotate portrait ↔ landscape;
- kill app from recents and reopen.

Expected:
- tile remains registered;
- no picker opens automatically;
- no duplicate tile appears.

### Route E — viewer placeholder

Tap Laguna tile.

Expected:
- Viewer screen opens with dataset title;
- Back returns to Library;
- rotation does not reopen picker or duplicate any action.
