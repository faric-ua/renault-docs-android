# v0.4.3 Phone Test

Install over v0.4.2.

## A. Search resume
1. Open an HTML page.
2. Open Search.
3. Search for a word with multiple matches.
4. Close Search with ×.
5. Open Search again without editing the text.
6. Expected:
   - previous query is still visible;
   - matches are highlighted immediately;
   - match counter is active;
   - ↑ / ↓ work immediately.
7. Repeat after rotation.

## B. Backup folder presentation
1. Open Settings → Backup.
2. Expected:
   - raw `content://...` URI is no longer shown as the primary user-facing location;
   - a readable path is shown when Android exposes a standard tree document ID.
3. Confirm the description recommends:
   `Documents/Renault/backups`.
4. Select a folder.
5. Reopen Settings and verify it persists.
6. Tap `Скинути вибір папки`.
7. Expected:
   - only the app preference is cleared;
   - no files are deleted;
   - status explicitly says files were not deleted.

## C. Dataset-local folder warning
1. Select a Backup folder inside a folder whose path contains `_android/`.
2. Expected:
   warning recommends keeping backups outside the dataset.

## D. Regression
- Classic tiles remain chronological.
- PDF Fit width remains functional.
- PDF Save still exports the original PDF.
- Modern / Classic bridge still works.
