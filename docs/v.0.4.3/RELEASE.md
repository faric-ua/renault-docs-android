# Renault Docs v0.4.3 — Search resume and Backup UX

## Purpose

Follow-up to real-phone testing of v0.4.2.

## Search reopen fix

Observed on phone:
- closing Search preserves the query text;
- reopening Search shows the previous query;
- however, highlights/match navigation do not resume until the query changes.

Cause:
closing Search clears WebView matches, while reopening only restores focus/visibility.

Fix:
- reopening Search now calls WebView `findAllAsync()` immediately when the saved query is non-empty;
- no character edit is required;
- preserved query continues to work as an active search.

## Backup folder UX

Observed on phone:
- raw Android `content://...` URI is shown in Settings;
- button text “Очистити папку backup” sounds like it may delete files, but it only clears the selected folder preference;
- selecting a Backup folder inside a specific dataset is possible but not ideal for future restore safety.

Fix:
- Settings displays a human-friendly SAF location instead of raw content URI when possible;
- example: `Внутрішня пам’ять/Documents/Renault/backups`;
- recommendation is shown: use a separate `Documents/Renault/backups` folder;
- button renamed to `Скинути вибір папки`;
- clearing the preference explicitly states that no files were deleted;
- if the selected path appears to be inside an `_android` dataset folder, Settings shows a warning recommending an external backup folder.

## Confirmed from v0.4.2 phone QA

- page search itself works;
- two-row search layout is usable;
- Classic volume tiles are chronologically sorted on the existing dataset.

## Unchanged

- no Fast Pack rebuild required;
- no dataset schema change;
- no backup archive creation yet;
- no files are deleted by Backup settings;
- Converter 2.0 / Backup Center remain future implementation work.
