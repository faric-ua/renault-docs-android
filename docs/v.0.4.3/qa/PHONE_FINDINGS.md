# v0.4.2 Phone Findings → v0.4.3

Date: 2026-09-24

## Confirmed

- Two-row page search is functional.
- Classic tile chronology works on the existing dataset.

## SEARCH-RESUME-001

Observed:
After closing page search and opening it again, the previous query remains in the input, but the search is inactive until the input value changes.

Root cause:
`closePageSearch()` clears WebView matches. `showPageSearch()` previously only showed/focused the search UI.

v0.4.3:
`showPageSearch()` reruns `findAllAsync(existingQuery)` whenever the current query is non-empty.

## BACKUP-UX-001

Observed:
Settings displays the full SAF `content://` URI, which is implementation detail and difficult to read.

v0.4.3:
Convert the tree document ID into a friendly display such as:
`Внутрішня пам’ять/Documents/Renault/backups`.

## BACKUP-UX-002

Observed:
Button text `Очистити папку backup` can be understood as deleting backup files.

Actual behavior:
Only the stored folder selection is cleared.

v0.4.3:
Rename to `Скинути вибір папки` and explicitly state that files were not deleted.

## BACKUP-SAFETY-001

Observed:
A backup folder can be selected inside a dataset folder.

Risk:
Future dataset cleanup/removal could also remove or complicate access to backups stored inside that dataset.

v0.4.3:
Recommend a separate `Documents/Renault/backups` folder and warn when the selected path looks like it is inside an `_android` dataset tree.
