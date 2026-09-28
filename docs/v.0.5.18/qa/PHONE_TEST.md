# v0.5.18 Phone test

## Install / regenerate

1. Renault Menu → 5.
2. Renault Menu → 8.
3. Install v0.5.18.
4. Renault Menu → 9 — REQUIRED.
5. Re-open the refreshed dataset.

## Gate A — 103_5 native pin table

Open the same path that produced `103_5(pines)`.

Expected:
- visible 4-column header:
  - `№`;
  - `мм²`;
  - `Код`;
  - `Опис`;
- value `0.6` stays on one line;
- pin number and wire code also stay on one line;
- description may wrap.

## Gate B — 103_5 pin PDF

Export the pin table.

Expected:
- filename remains `103_5(pines).pdf`;
- first row contains the 4-column semantic header;
- `0.6` is not split;
- description is readable;
- Cyrillic/Ukrainian header text renders correctly.

## Gate C — 101 regression

Re-check:
- `101(abbreviations).pdf`;
- one `101_1/101_2(pines).pdf`.

Expected:
- glossary header remains correct;
- pin header appears;
- opaque connector filename remains exact.

## Gate D — pagination

If a long pin/structured table is available:
- export page 2+;
- header repeats on each continuation page;
- no body row is lost or duplicated.

## Regression

- combined connector view works;
- Classic works;
- ordinary PDF viewer works;
- connector IDs such as `120_18` remain opaque;
- no unrelated 4-column structured document receives a pin header.
