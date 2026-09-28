# v0.5.31 Phone test — dedicated volume documentation screen

## Prepare

1. Renault Menu → 5.
2. Renault Menu → 8.
3. Install v0.5.31.
4. If point 9 was already completed for v0.5.29 or later, do NOT run it again.

Use a volume where `Загальна документація / Запобіжники / Довідка` are known to work.

## Gate A — direct volume entry

Open the Modern volume screen before entering any section.

Expected:
- a separate `Документація` button is visible;
- pressing it opens a dedicated documentation screen;
- screen header identifies Documentation and the current volume;
- no section number such as 101/103/105 is required to open it.

## Gate B — content parity

From the dedicated screen, verify the same volume-owned categories previously visible from a section, where available:
- Загальна документація;
- Запобіжники;
- Довідка.

Open several child items.

Expected:
- nested documentation menu/panel navigation works;
- final HTML/PDF documents open normally in the existing Viewer;
- Back from a final document returns to the documentation screen.

## Gate C — section shortcut

Open section 103 or 105 and press `Документація`.

Expected:
- it opens the SAME dedicated volume documentation screen;
- content is the same set as when opened directly from the volume screen;
- changing section does not change the documentation set inside the same volume.

## Gate D — volume isolation

Open another Renault volume and use its direct Documentation button.

Expected:
- the second volume opens only its own documentation;
- no path/document from the previous volume leaks across.

## Gate E — lifecycle

Inside Documentation:
- enter a nested panel;
- rotate portrait ↔ landscape;
- press Back.

Expected:
- nested panel is restored after rotation;
- Back returns one documentation level first;
- root Back exits to the previous screen.

## Gate F — regression

Verify:
- sections still open normally;
- Schemes / Connector / Position still work;
- a PDF still opens and 400% scrolling remains good;
- Classic fallback remains available if documentation shard is absent.

## Closeout

v0.5.31 is PHONE PASS when direct volume entry and section shortcut expose the same volume-owned documentation with stable navigation and volume isolation.
