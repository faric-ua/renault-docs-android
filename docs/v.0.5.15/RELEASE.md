# Renault Docs v0.5.15 — adaptive tables + combined connector view

Date: 2026-09-25

## Scope

APK-only UI/renderer update. Runtime IR regeneration is **not required**.

BUG-004 (full Modern catalog parity for identifiers such as `1405`, `R325`, `MAH`, `MYH`) remains a separate converter task.

## Phone feedback addressed

Real v0.5.14 phone screenshots confirmed that table PDF export works, but exposed four follow-up issues:

1. native/PDF table columns still use proportions that are too rigid;
2. rows whose description wraps should keep one full-height code cell with vertically centered short text;
3. connector menu should provide a combined scheme+pins view similar to the readable Classic connector composition;
4. exported filenames such as `Renault_101_table.pdf` are too generic.

## Adaptive table columns

Native and generated PDF tables now share `NativeTableLayout.columnFractions(...)`.

The algorithm derives compact-column proportions from body content and leaves the remaining width to the description column.

Important behavior:
- header labels do not force compact code columns to become excessively wide;
- 2-column glossaries get a narrower code column and wider description column;
- 4-column connector tables keep compact № / mm² / signal columns and a wide description column.

## Multi-line row alignment

If a description wraps onto multiple lines:
- every cell occupies the same row height;
- compact code cells are vertically centered;
- table headers are centered;
- PDF text blocks are vertically centered inside the row as well.

This specifically targets rows such as `E2` / `E3` where the right-hand description can wrap.

## PDF page geometry

Generated table PDFs now choose page orientation by table shape:
- up to 2 columns → portrait;
- wider tables → landscape.

This improves phone readability for glossary/abbreviation tables.

## Combined connector view

For a connector composite that contains both:
- `dessin` PDF;
- `alveoles` pin/contact HTML;

Modern now offers:

```text
Схема + піни розʼєма
Схема розʼєму
Опис контактів
```

The combined action opens the original connector composite source directly inside the app viewer. This preserves the readable scheme+pins arrangement from Classic without returning to the full Classic volume navigation.

## Structured PDF filenames

Suggested filenames now use source identity when possible.

Examples:

```text
101(pins).pdf
101_1(pins).pdf
101(abbreviations).pdf
```

For pin documents, a legacy source variant such as `101_1` is preserved if it can be extracted from the structured source filename.

Fallback is the current section identifier.

## Version

- versionName: `0.5.15`
- versionCode: `31`
