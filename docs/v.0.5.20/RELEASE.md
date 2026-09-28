# Renault Docs v0.5.20 — composed pines PDF layout

Date: 2026-09-25

## Scope

APK-only PDF composition refinement on top of v0.5.19.

The user-approved reference is the original Renault connector/pin document hierarchy:
- connector/application card;
- section code/title/criteria;
- pin table.

## Pines PDF composition

`(...pines).pdf` now uses a dedicated portrait renderer.

### Block 1 — connector/application card

Centered compact card:
- vector connector pictogram on the left;
- top-right source zone/type;
- bottom-right source applicability/configuration.

Examples from phone/source material:
- `ЭЛПРОВ. САЛОНА` + `B74,K74`;
- `ЭЛПРОВ. ДВИГ.` + `F5R700`;
- `DD`.

Additional non-pin metadata is preserved below the card if present.

### Block 2 — section identity

Centered:
- section code;
- section title;
- criteria/applicability line.

### Block 3 — pin table

Header keeps both icons and text:
- `№` + pin/contact icon;
- `мм²` + wire cross-section icon;
- `Код` + wire/zig-zag icon;
- `Опис` + arrow/dots icon.

The first three columns are measured from their real header/body content.
The description column receives all remaining width.

Body alignment:
- first three columns centered horizontally/vertically;
- description left-aligned and vertically centered.

## Pagination

First page:
- connector card;
- full section identity;
- pin header;
- body.

Continuation pages:
- compact section identity;
- repeated pin header;
- remaining body.

The large connector card is not repeated.

## Packaging

No converter/Runtime-IR changes are introduced in v0.5.20.

If the active dataset has already been regenerated with v0.5.19, Renault Menu → 9 is NOT required again.

## Version

- versionName: `0.5.20`
- versionCode: `36`
- branch: `feat/v0.5.20-pines-pdf-layout`


## Merge / CI

Merged source:
`094698411f6af56a62dcf306c4c4c24b49e6af8c`

Main CI:
- Tests run `36142685738` — PASS;
- Android Debug APK run `36142685726` — PASS;
- artifact `Renault-Docs-v0.5.20-Debug`;
- artifact id `10868375871`;
- APK SHA-256 `d3a898085c6b2dc3c3e19c58e82b3fdc27de75fefff5ea5a5e10f32bd47016fc`;
- artifact ZIP SHA-256 `d480d5ff7717e633af0b76655567a660c2004bb7db7f32eaa5f10d3deecc4a5b`.

Status: CI PASS; phone validation pending. This is APK-only on top of the v0.5.19-regenerated dataset.
