# v0.5.12 Phone test

## Install

APK-only update:

1. Renault Menu → 5.
2. Renault Menu → 8.
3. Renault Menu → 9 is not required.

## Gate A — Modern header/menu density

Open:

```text
Modern
→ NT8183A
→ 101 · ПРИКУРИВАТЕЛЬ
```

Expected:
- top title stays on one line when space allows;
- `Розділи` + `Classic` are visibly more compact;
- section heading is `101 — ПРИКУРИВАТЕЛЬ`, not `CMP 101`;
- menu rows keep:
  - Схеми + Розʼєм;
  - Положення на авто;
  - Документація;
- the three rows occupy less vertical space than v0.5.11.

Also open a section without NM (for example the previously tested 107) and confirm the remaining menu stays compact without an empty connector button.

## Gate B — contact description

Open:

```text
101
→ Розʼєм
→ variant
→ Опис контактів
```

Expected:
- every logical pin/contact row stays on one line;
- long rows do not wrap;
- drag horizontally to read the rest of the row;
- vertical scrolling between rows still works;
- rows do not look clickable.

## Gate C — PDF toolbar

Open a PDF.

At 100%:
- `+` is immediately after the zoom dropdown;
- `По ширині` follows the `+` button.

Set 150% / 200%.

Expected:
- the PDF becomes wider;
- the upper control strip expands with the document;
- its background/border no longer ends at the old viewport width;
- `По ширині` is not visually cut off merely because the document was zoomed;
- horizontal panning still works.

## Regression

Classic contrast fix from v0.5.11 remains expected to work.
