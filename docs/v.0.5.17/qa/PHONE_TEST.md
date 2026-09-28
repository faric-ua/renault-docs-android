# v0.5.17 Phone test

## Install

1. Renault Menu → 5.
2. Renault Menu → 8.
3. Renault Menu → 9 is **not required**.

## Gate A — abbreviation PDF header

Open:

```text
Modern
→ NT8183A
→ 101
→ Критерії / скорочення
→ Зберегти таблицю PDF
```

Expected:
- filename remains `101(abbreviations).pdf`;
- first table row in the PDF contains the source column names:
  - `СОКРАЩЕНИЯ`;
  - `ПОЛНЫЕ НАИМЕНОВАНИЯ`;
- portrait layout remains;
- column widths remain adaptive;
- Cyrillic remains readable.

## Gate B — pin PDF header

Open a 101 connector variant:

```text
101
→ Розʼєм
→ variant
→ Опис контактів
→ Зберегти таблицю PDF
```

Expected:
- filename stays source-derived, for example `101_1(pines).pdf` or `101_2(pines).pdf`;
- first row contains the source pin/contact column names;
- no regression in pin-table layout.

## Gate C — 120 opaque-ID regression (representative)

If a section 120 connector is available, do not expect a fixed `_1/_2/_3` sequence.

Observed real example:

```text
120_18.PDF
120_18(pines).pdf
```

Expected:
- exported pin filename preserves the exact source connector ID;
- `_18` stays `_18`;
- the app does not renumber it to `_1`, `_2`, etc.

## Gate D — pagination header repetition

Export a table long enough to create page 2.

Expected on every continuation page:
- document title/criteria/metadata remain readable;
- table column header is repeated before the first body row on that page;
- no row is dropped.

## Regression

- combined connector view still works;
- native table layout unchanged;
- `101_1/101_2(pines).pdf` naming unchanged;
- ordinary PDF viewer unchanged;
- Classic unchanged.
