# Renault Docs v0.5.14 — unified native tables + PDF export

Date: 2026-09-25

## Scope

APK-only renderer update. Runtime IR regeneration is **not required**.

BUG-004 (full Classic catalog / non-3-digit identifiers) remains a separate converter task and still requires point 9 when implemented.

## One table renderer for abbreviations and connector pins

Structured HTML documents now use one native table presentation instead of separate ad-hoc text streams.

The same renderer is used for:
- abbreviation / glossary tables;
- connector pin/contact descriptions;
- other Runtime IR structured tables.

Column proportions adapt to the source column count.

Examples:
- 2 columns: compact code/abbreviation column + wide description column;
- 4 columns: narrow number / wire size / signal columns + wide description column.

Cells wrap naturally inside the table, matching the useful behavior seen in Classic instead of clipping long right-hand text.

## Connector description header

Connector/contact documents no longer expose legacy titles such as `CMP101`.

Modern renders:

```text
101 — ПРИКУРИВАТЕЛЬ
DG/E2/SSNAV/SRUNLI ...
```

Additional source metadata such as connector/location labels is grouped into a compact card above the table.

This is intended to preserve the useful information hierarchy from Classic without recreating its frameset skin.

## Save table as PDF

Every structured native table document now has:

```text
Зберегти таблицю PDF
```

The action opens Android's standard create-document flow and writes an actual PDF file containing:
- section title;
- criteria/configuration line;
- metadata;
- all structured table rows.

The PDF uses the same column semantics as the native table and creates additional pages when needed.

## Version

- versionName: `0.5.14`
- versionCode: `30`
