# Renault Docs v0.5.18 — pin-table semantic headers + compact token layout

Date: 2026-09-25

## Trigger

Real-phone v0.5.17 test in section `103`, connector variant `103_5` found:
- generated `103_5(pines).pdf` still had no visible column-name header;
- native pin table wrapped wire cross-section `0.6` into two lines.

## Root cause

v0.5.17 repeated table rows already marked as headers, but some legacy Renault connector-contact pages do not encode their visual header as semantic `th` cells. Current Runtime IR can therefore contain body rows without a usable header.

The converter also previously dropped empty non-header cells, which could collapse legacy image/header column positions.

## Converter / Runtime IR fix

For structured documents whose source filename starts with `T_`:
- preserve empty cells while compiling table rows;
- for standard 4-column pin/contact tables, expose a semantic Modern header:
  - `№`;
  - `мм²`;
  - `Код`;
  - `Опис`;
- if the first row is actual pin data, prepend the header;
- if the first row is a legacy visual/header row, replace that visual row with the semantic header;
- unrelated structured documents do not receive a synthetic pin header.

## Android / PDF fix

For 4-column pin tables:
- second compact column gets more width;
- first three compact fields are single-line in native UI;
- first three compact fields are not split by the PDF text wrapper;
- long description column still wraps normally.

Existing contracts retained:
- opaque Renault connector IDs;
- `(pines)` filename naming;
- combined scheme + pins view;
- adaptive table widths;
- repeated PDF table headers after pagination;
- Classic fallback.

## Packaging

This release changes converter / Runtime IR output.

After installing the APK:
- Renault Menu → 5;
- Renault Menu → 8;
- Renault Menu → 9 **IS REQUIRED** before phone validation.

## Version

- versionName: `0.5.18`
- versionCode: `34`
- branch: `fix/v0.5.18-pin-table-semantics`
