# v0.5.13 Phone test

## Install

APK-only:

1. Renault Menu → 5.
2. Renault Menu → 8.
3. Renault Menu → 9 is **not required** for this release.

## Gate A — top navigation

Open:

```text
Modern
→ NT8183A
→ 101 · ПРИКУРИВАТЕЛЬ
```

Expected:

Top row:
- back;
- Home;
- Search;
- Settings.

Second row:
- only `101` at the left;
- compact `Modern | Classic` switch at the right;
- no repeated `ПРИКУРИВАТЕЛЬ` title in this row;
- no old `Розділи` button.

Below, the content heading may show:

```text
101 — ПРИКУРИВАТЕЛЬ
```

## Gate B — persistent menu

On 101 expected:

```text
[ Схеми ] [ Розʼєм ]
[ Положення на авто ]
[ Документація ]
```

Then open 107, where connector data was previously absent.

Expected:
- all four tiles remain in exactly the same positions;
- unavailable `Розʼєм` stays visible but dimmed/disabled;
- no vertical layout jump caused by a disappearing button.

## Gate C — tiles

Open `Схеми`.

Expected:
- prompt text remains lightweight;
- groups such as `МАССА`, `КОММУТАЦ. БЛОК...`, etc. are grouped into separate rounded cards/tiles;
- routes inside a card look actionable;
- unavailable entries look disabled rather than like valid buttons.

## Gate D — search

From section 101 tap Search in the top global row.

Expected:
- current volume section list opens;
- search input is already visible;
- Home and Settings remain reachable from the volume header.

## Regression

Check:
- `Classic` still opens the real Classic runtime;
- NM connector/pin flow from v0.5.11/v0.5.12 still works;
- PDF toolbar changes from v0.5.12 are unaffected.

## Separate known issue

BUG-004 remains open: Modern volume catalog still misses Classic entries such as `1405` and `R262` until the converter/catalog discovery is redesigned.
