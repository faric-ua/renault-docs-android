# v0.5.23 Phone test

## Install

1. Renault Menu → 5.
2. Renault Menu → 8.
3. Install v0.5.23.
4. Do NOT run point 9.

## Gate A — portrait regression

Open section 108 → connector/contact table.

Expected:
- portrait layout remains like accepted v0.5.22;
- measured compact columns remain readable;
- no clipping regression.

## Gate B — landscape focus

For each top-level native mode:
- `Схеми`;
- `Розʼєм`;
- `Положення на авто`;
- `Документація`.

Open the mode in portrait and rotate to landscape.

Expected in landscape:
- only the active mode button remains at the top, full width;
- Back/Home/Search/Settings are hidden;
- section number/title chrome is hidden;
- Modern/Classic switch is hidden;
- all other top-level mode buttons are hidden;
- current child content stays open.

Rotate back to portrait.

Expected:
- full toolbar returns;
- Modern/Classic switch returns;
- all top-level menu buttons return;
- the same child content remains open.

## Gate C — pines PDF typography

Export `108_1(pines).pdf`.

Expected:
- first 3 data columns use bold 10 pt text;
- description column uses regular 10 pt;
- header labels are bold 14 pt;
- upper connector-info table text is bold 14 pt;
- icons remain in the header;
- page composition/margins remain unchanged.

## Lifecycle continuation

Continue the v0.5.22 rotation audit afterwards. Any child window that disappears or falls back to its parent remains a lifecycle bug.
