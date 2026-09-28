# v0.5.21 Phone test

## Install

1. Renault Menu → 5.
2. Renault Menu → 8.
3. Install v0.5.21.
4. Point 9 is NOT required when using the already v0.5.19-regenerated dataset.

## Gate A — native 108 / 105 pin table

Open a pin/contact table.

Expected:
- visible header `№ | мм² | Код | Опис`;
- first three columns stay single-line;
- 3rd `Код` column is visibly narrower and follows content;
- description gets the remainder.

## Gate B — 108_1(pines).pdf

Export `108_1(pines).pdf`.

Expected:
- portrait;
- content begins about 2 cm from the top;
- left/right margins about 2 cm;
- connector card + identity + table preserved;
- modern colored technical icons;
- text labels still visible;
- first three columns compact;
- description gets most of the table width.

## Gate C — 105(abbreviations).pdf

Export `105(abbreviations).pdf`.

Expected:
- ~2 cm margins;
- title centered;
- visible gap between title and table;
- first column compact and sized from its header/body content;
- second column gets the remaining width.

## Regression

- exact opaque connector filename remains;
- `(pines)` naming remains;
- connector/application metadata remains;
- combined scheme + pins viewer unchanged;
- Classic unchanged.
