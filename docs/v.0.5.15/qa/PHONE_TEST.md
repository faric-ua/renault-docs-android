# v0.5.15 Phone test

## Install

1. Renault Menu → 5.
2. Renault Menu → 8.
3. Renault Menu → 9 is **not required**.

## Gate A — abbreviations table

Open:

```text
Modern
→ NT8183A
→ 101
→ Критерії / скорочення
```

Expected:
- two-column table;
- left code column is sized from actual codes, not from the long header label;
- right description column receives most of the width;
- E2/E3 and similar rows do not look like two unrelated stacked cells;
- if description wraps, the short left code is vertically centered in the same tall row.

## Gate B — abbreviation PDF

Tap:

```text
Зберегти таблицю PDF
```

Expected:
- suggested name: `101(abbreviations).pdf`;
- exported page is portrait;
- code column is compact;
- description column is wide;
- text is readable in the system PDF viewer;
- Cyrillic is correct.

## Gate C — connector menu

Open:

```text
101
→ Розʼєм
→ connector variant
```

Expected connector document menu:

```text
Схема + піни розʼєма
Схема розʼєму
Опис контактів
```

Combined action should open the original connector composition showing drawing and pin/contact information together, similar to the readable Classic layout.

## Gate D — pin table + filename

Open `Опис контактів`.

Expected:
- full bordered table;
- compact № / mm² / code columns;
- wide description column;
- wrapped descriptions keep code cells vertically centered.

Save PDF.

Expected filename:
- `101(pins).pdf`, or
- `101_1(pins).pdf` when the underlying source carries a variant such as `101_1`.

## Gate E — pagination

Find a longer structured table and export it.

Expected:
- multiple pages are created when required;
- no row is silently dropped;
- header/title information remains readable.

## Regression

- 101/107 tile shell still works;
- 107 Connector remains disabled but visible;
- Classic fallback still opens pure Classic;
- ordinary PDF viewer unchanged.

## Separate pending converter work

BUG-004 remains open and is not part of v0.5.15.
