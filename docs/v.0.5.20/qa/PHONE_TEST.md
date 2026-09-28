# v0.5.20 Phone test

## Install

1. Renault Menu → 5.
2. Renault Menu → 8.
3. Install v0.5.20.
4. If the current dataset was already regenerated with v0.5.19, point 9 is NOT required.

## Gate A — 105 pines PDF

Open a 105 connector/contact variant and export `(...pines).pdf`.

Expected order:
1. connector/application card;
2. section code/title/criteria;
3. pin table.

Connector card must preserve source values such as:
- `ЭЛПРОВ. САЛОНА`;
- `B74,K74` or the actual selected applicability.

Pin header:
- icon + `№`;
- icon + `мм²`;
- icon + `Код`;
- arrow icon + `Опис`.

## Gate B — 108_1 pines PDF

Export `108_1(pines).pdf`.

Expected:
- portrait page;
- connector card contains `ЭЛПРОВ. ДВИГ.` and `F5R700` when present in source;
- section 108 identity and criteria stay centered;
- first three pin columns are compact;
- description uses remaining width;
- `2.0`, `2.5`, `3CV`, `3N` remain readable.

## Gate C — metadata preservation

Use a variant with extra connector/application metadata such as `DD` or multiple source metadata lines.

Expected:
- source metadata is not dropped;
- extra lines appear between connector card and section identity.

## Gate D — pagination

If a pin table spans page 2:
- page 2 starts with compact section identity;
- pin header is repeated;
- connector card is not repeated;
- no body row is lost or duplicated.

## Regression

- filename remains the exact opaque source ID + `(pines).pdf`;
- native table UI unchanged by this APK-only PDF change;
- 101 abbreviations PDF unchanged;
- combined scheme + pins viewer unchanged;
- Classic unchanged.
