# v0.5.16 Phone test

## Install

1. Renault Menu → 5.
2. Renault Menu → 8.
3. Renault Menu → 9 is **not required**.

## Gate A — v0.5.15 table regression

Open:

```text
Modern
→ NT8183A
→ 101
→ Критерії / скорочення
```

Expected:
- compact left abbreviation column;
- wide right description column;
- E2/E3-style wrapped rows keep one full-height code cell;
- short code text is vertically centered.

Export the table.

Expected:
- `101(abbreviations).pdf`;
- portrait page;
- Cyrillic readable.

## Gate B — connector combined view

Open:

```text
101
→ Розʼєм
→ one connector variant
```

Expected:
- `Схема + піни розʼєма`;
- `Схема розʼєму`;
- `Опис контактів`.

Combined action must still show drawing + pin/contact information together.

## Gate C — opaque connector ID filename

Open `Опис контактів` for each available 101 variant and save PDF.

Expected names must follow the actual source variant, for example:

```text
101_1(pines).pdf
101_2(pines).pdf
```

Important:
- `_1` / `_2` come from Renault source identity;
- they are not generated as duplicate counters.

## Gate D — ECU multi-connector example

If section 120 and its connector variants are available in the selected volume, verify each physical connector independently.

Expected examples:

```text
120_1(pines).pdf
120_2(pines).pdf
120_3(pines).pdf
```

The app must preserve the exact variant ID presented by the source.

## Gate E — pagination

Export one long structured table.

Expected:
- all rows present;
- multiple pages when required;
- no dropped row;
- readable title/metadata.

## Regression

- Modern/Classic switch works;
- 101/107 tile shell unchanged;
- ordinary PDF viewer unchanged;
- Classic fallback unchanged.
