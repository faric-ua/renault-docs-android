# v0.5.22 Phone test

## Install

1. Renault Menu → 5.
2. Renault Menu → 8.
3. Install v0.5.22.
4. Do NOT run point 9.

## Gate A — native 108 table

Open the same 108 pin/contact screen.

Expected:
- header visible and bold: `№ | мм² | Код | Опис`;
- compact columns fit their real content;
- no clipping of `2.0`, `2.5`, `3CV`, `3N`;
- description receives remaining width.

For the upper 2-column source-information table:
- first column fits values such as `F5R700`;
- first-column designations are bold.

## Gate B — abbreviations

Open/export an abbreviations table.

Expected:
- first-column abbreviations/designations are bold;
- header is bold;
- first column remains compact/content-sized.

## Gate C — rotation audit

Execute `docs/v.0.5.22/LIFECYCLE_AUDIT.md`.

Critical checks:
- connector child screen survives rotation;
- pin/contact child survives rotation;
- Documentation submenu survives rotation;
- Settings choosers survive rotation;
- Viewer section navigator + its search query survive rotation;
- Frame debug survives rotation;
- current PDF does not return to parent;
- table-PDF save flow still completes if Activity recreation occurs.

## Regression

- no action autostarts because of rotation;
- Back semantics unchanged;
- Classic unchanged;
- exact opaque Renault connector IDs unchanged;
- v0.5.21 PDF composition/margins/icons unchanged.
