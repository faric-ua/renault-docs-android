# Renault Docs v0.5.21 — table and PDF visual polish

Date: 2026-09-25

## Scope

APK-only visual polish on top of the accepted v0.5.20 pines composition.

## Native pin/contact table

For `(pines)` documents:
- guarantee a visible semantic header `№ | мм² | Код | Опис` when the source rows do not already begin with one;
- tighten the compact technical columns;
- especially reduce unnecessary width of the 3rd `Код` column;
- keep the description column as the flexible remainder.

## PDF page layout

All generated structured PDFs:
- use approximately 2 cm page margins (`57 pt`);
- start content lower on the page;
- center the generic document title;
- center criteria/metadata under the title;
- leave a clearer gap before the first table.

## Pines PDF polish

The v0.5.20 three-block composition stays intact:
1. connector/application card;
2. section identity/criteria;
3. pin/contact table.

Refinements:
- first three pin columns measured more tightly from real header/body content;
- final description column receives more of the remaining width;
- technical header icons use restrained modern color accents;
- icons have soft tinted badges;
- connector icon receives the same modern visual treatment;
- text labels remain visible with the icons;
- connector/application metadata remains preserved.

## Abbreviations PDF polish

- title centered;
- table separated from title by a visible gap;
- ~2 cm margins;
- first column sized compactly from header/body content;
- second description column receives the remainder.

## Packaging

No converter/Runtime-IR changes in v0.5.21.

If the active dataset was already regenerated with v0.5.19, Renault Menu → 9 is NOT required.

## Version

- versionName: `0.5.21`
- versionCode: `37`
- branch: `feat/v0.5.21-pdf-polish`


## Merge / CI

Merged source:
`ad9d69cb05697e960f2fc2800f28f7a95f99a4e8`

Main CI:
- Tests run `36146178858` — PASS;
- Android Debug APK run `36146178924` — PASS;
- artifact `Renault-Docs-v0.5.21-Debug`;
- artifact id `10869810678`;
- APK SHA-256 `934f9791804b65725f75f4b9db7be6e8701f394e646d4ee854fda79adbaf58ea`;
- artifact ZIP SHA-256 `ae29db796261f2e3b7db26f4f290f414bb8d964c6222cf7e76f7053deec87ef2`.

Status: CI PASS; phone validation pending. This release is APK-only and does not require point 9 on the already regenerated dataset.
