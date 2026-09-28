# v0.3.0 Phone Test

## A — In-place update

1. Install v0.3.0 over v0.2.2.
2. Expected: no uninstall required.
3. Existing Laguna dataset remains registered.

## B — Modern mode

1. Tap Laguna tile.
2. Expected: native Android Modern screen opens immediately.
3. Confirm 10 volumes.
4. Type `8236` in search.
5. Expected: list filters to NT8236.
6. Clear search.
7. Tap NT8236.
8. Expected: legacy internal documentation opens.
9. Back returns to Modern screen.

## C — Classic fallback

1. Tap `Classic`.
2. Expected: old generated `_renault/START.html` opens.
3. Confirm old catalog still works.

## D — PDF save

1. Open S20.PDF in NT8236.
2. Tap the Save PDF action.
3. Cancel once.
4. Expected: return to same PDF, nothing saved.
5. Tap Save again.
6. Save as `S20-test.pdf` to a user-selected folder.
7. Expected: success message.
8. Open the exported PDF from Android Files.
9. Expected: valid PDF with same document content.

## E — rotation/back

1. Rotate Modern screen.
2. Rotate legacy volume.
3. Rotate PDF.
4. Expected: no picker relaunch and no duplicate action.

Do not mark PASS until tested on the real phone.
