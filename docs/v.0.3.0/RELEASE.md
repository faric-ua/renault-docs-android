# Renault Docs v0.3.0 — Modern mode + PDF export

## Goal

Move the app away from loading every navigation screen through old Renault frames while preserving a reliable Classic fallback.

## Modern mode v1

- dataset tile opens a native Android catalog by default;
- native catalog reads the dataset manifest/modern index directly;
- native volume cards avoid loading the generated HTML catalog;
- search/filter works without WebView;
- tapping a volume still opens its legacy internal documentation for now;
- a visible `Classic` action opens the old generated `_renault/START.html` catalog.

This is the first step toward a fully native hierarchy:
`vehicle → volume → section → node → PDF/image`.

## Converter/package change

Every packaged dataset gains:

`_renault/modern-index.json`

Schema v1 contains stable dataset/volume navigation metadata and is designed to grow with sections/nodes later without changing the old Renault files.

Existing already-converted datasets remain compatible: the app can fall back to `renault-dataset.json.volumes` when `modern-index.json` is absent.

## PDF export

The native PDF viewer gains a Save action.

Flow:
- user taps Save inside PDF toolbar;
- Android `ACTION_CREATE_DOCUMENT` opens;
- user chooses destination/name;
- the original source PDF bytes are copied from the SAF dataset;
- no broad storage permission is required;
- cancelling the system picker returns to the same viewer and does not export anything.

## Non-goals

- v0.3.0 does not yet parse every legacy internal menu into native section/node rows;
- Classic mode remains available;
- exact Adobe `#viewrect` parity is still not claimed.
