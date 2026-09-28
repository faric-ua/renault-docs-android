# v0.2.0 Phone Test

## A — Open converter

1. Launch Renault Docs.
2. Tap `Конвертувати стару папку`.
3. Expected: Converter screen opens. No system picker opens automatically.

## B — Source picker

1. Tap source action.
2. Cancel once.
3. Expected: return to Converter, no fake selection.
4. Open again and select the old Renault source folder.
5. Expected: selected folder/URI summary appears.

## C — Rotation after source

1. Rotate portrait → landscape → portrait.
2. Expected: source selection remains.
3. Expected: source picker does not reopen.

## D — Destination picker

1. Tap destination action.
2. Cancel once.
3. Expected: return to Converter.
4. Select destination parent folder.
5. Expected: destination selection remains visible.

## E — Rotation with complete draft

1. Rotate twice.
2. Expected: both selections remain.
3. Expected: no picker opens and no conversion starts.

## F — Validation

1. If practical, choose the same tree as source and destination.
2. Tap plan validation.
3. Expected: clear local error; no write/copy starts.

## G — Navigation

1. Press Android system Back.
2. Expected: Library.
3. Reopen Converter.
4. Expected: saved draft is restored; no picker opens automatically.

## PASS gate

Only mark PASS after all tested routes above are confirmed on the real phone.
