# v0.5.19 Phone test

## Fast test dataset option

To reduce point-9 time, you may temporarily leave only three required converted volume folders inside:

`/storage/emulated/0/Documents/Renault/laguna 2 2001-2006_android`

Move the other converted volume folders to a sibling path OUTSIDE that build root, for example:

`/storage/emulated/0/Documents/Renault/_volumes_hold/`

Do not move original source documentation. Restore all volumes and run point 9 again before final full-dataset validation.

## Install / regenerate

1. Renault Menu → 5.
2. Renault Menu → 8.
3. Install v0.5.19.
4. Renault Menu → 9 — REQUIRED.
5. Re-open refreshed dataset.

## Gate A — 103_5 native table

Open the same `103_5` contact/pin description.

Expected:
- semantic header visible: `№ | мм² | Код | Опис`;
- `0.6` stays on one line;
- compact columns are narrow but readable;
- description gets most of the width.

## Gate B — 108 native table

Open the 108 pin/contact table.

Expected:
- same semantic header;
- `1.4`, `3CV`, `3N` stay on one line;
- final description column gets the remaining width.

## Gate C — PDF

Export `103_5(pines).pdf`.

Expected:
- filename remains exact;
- semantic header appears on page 1;
- first three columns are content-sized, not oversized;
- last description column gets the remaining width;
- compact values do not split.

## Gate D — 2-column glossary

Open/export the 101 abbreviations table.

Expected:
- first column header `СОКРАЩЕНИЯ` does not wrap unnecessarily;
- first column width accounts for both header and body;
- second description column receives remaining width.

## Gate E — pagination

If a long table is available:
- header repeats on page 2+;
- no row is lost or duplicated.

## Regression

- opaque IDs such as `103_5`, `101_1`, `120_18` remain unchanged;
- `(pines)` naming remains;
- combined scheme + pins view remains;
- Classic remains available.
