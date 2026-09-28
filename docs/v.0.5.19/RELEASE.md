# Renault Docs v0.5.19 — structural pin-table headers + compact column sizing

Date: 2026-09-25

## Trigger

Real-phone v0.5.18 test confirmed one fix but exposed two remaining issues:
- wire cross-section `0.6` now stays on one line;
- pin/contact tables such as `103_5` and `108` still show no semantic column header;
- compact columns in native/PDF layouts are still wider or narrower than their content warrants.

## Root cause

Header normalization in v0.5.18 depended on the source filename beginning with `T_`.

Real Renault document wrappers are not consistent enough for that assumption. The same logical 4-column contact table can appear through another wrapper/path.

## Converter fix

Pin/contact tables are now detected by row structure instead of filename.

Expected body shape:
- contact/pin number;
- numeric wire cross-section;
- compact wire/color/code token;
- description.

When detected:
- if row 1 is already body data, prepend `№ | мм² | Код | Опис`;
- if one or more visual-only header rows precede body data, replace that leading visual block with the semantic header;
- keep all body rows;
- unrelated 4-column tables remain unchanged.

## Width contract

For native UI and generated PDFs:
- every column before the last is compact;
- compact width is based on the longest value OR header label;
- compact columns stay single-line;
- the final description column receives all remaining width.

This also applies to 2-column glossary/metadata tables:
- first column accounts for body values and header text;
- labels such as `СОКРАЩЕНИЯ` and values such as `B74,K74` should not wrap merely because of an undersized fixed fraction.

## Packaging

Converter / Runtime IR changed.

After installing v0.5.19:
1. Renault Menu → 5;
2. Renault Menu → 8;
3. install APK;
4. Renault Menu → 9 — REQUIRED.

For faster iteration, unused converted volume folders may be temporarily moved outside the build root. See PROJECT_LEDGER.md.

## Version

- versionName: `0.5.19`
- versionCode: `35`
- branch: `fix/v0.5.19-table-semantics-widths`


## Merge / CI

Merged source:
`930b20d240fff0a2e0f7dd5e60769654241a23c9`

Main CI:
- Tests run `36133711994` — PASS;
- Android Debug APK run `36133712023` — PASS;
- artifact `Renault-Docs-v0.5.19-Debug`;
- artifact id `10862578973`;
- APK SHA-256 `03eb2a9f1edc9f5d0dcbe2aabad4090e433462ffcb866bcc38e610bedd682ab0`;
- artifact ZIP SHA-256 `0e797cdfb03765ffb0115a8bb38bb4c7b6cc3018f836dfb0e25cd560189d150b`.

Status: CI PASS; phone validation pending. Point 9 is required because converter/Runtime IR changed.
