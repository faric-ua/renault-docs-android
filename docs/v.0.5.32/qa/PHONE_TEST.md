# v0.5.32 Phone test — PDF Companion

## Prepare

1. Renault Menu → 5.
2. Renault Menu → 8.
3. Install v0.5.32.
4. If point 9 was already completed for v0.5.29+, do NOT run it again.

Use a wiring-diagram PDF opened from a Modern volume with working volume documentation.

## Gate A — Companion button

Open the schematic PDF.

Expected:
- a new split/documentation icon appears beside fullscreen;
- pressing it gives the button an active/accent state;
- pressing it again closes the lower pane and restores inactive state.

## Gate B — main PDF preservation

Before opening Companion:
- go to a non-first page;
- set zoom around 200–400%;
- pan/scroll to a recognizable location.

Open Companion.

Expected:
- main PDF remains the same document;
- page does not reset to page 1;
- zoom does not reset;
- location remains approximately where it was;
- no main-PDF navigation occurs when interacting with the lower pane.

## Gate C — documentation inside lower pane

In Companion root, verify the current volume's categories, where present:
- Загальна документація;
- Запобіжники;
- Довідка.

Navigate into child items.

Expected:
- nested menus remain inside the lower pane;
- opening HTML remains inside the lower pane;
- opening a documentation PDF remains inside the lower pane;
- the main schematic stays visible above.

## Gate D — independent PDF state

Open a PDF in Companion.

Expected:
- its page/zoom controls work independently;
- zooming the companion PDF does not change main PDF zoom;
- moving main PDF does not change companion page;
- Companion PDF does not show another Companion button.

## Gate E — Back / Close

Inside a child document:
- use Companion header Back;
- then Android Back;
- then Close.

Expected:
- Back walks companion history/menu first;
- at companion root Android Back closes Companion;
- Close hides Companion immediately without navigating the main schematic;
- reopening Companion restores previous WebView state when possible.

## Gate F — fullscreen / rotation

With Companion open:
- enter fullscreen;
- rotate portrait ↔ landscape;
- exit fullscreen.

Expected:
- both panes remain visible in fullscreen;
- main PDF state remains;
- companion state/history remains;
- fullscreen button state remains correct.

## Gate G — memory/stability

With main PDF around 300–400% and a PDF open in Companion:
- scroll both panes for several pages.

Expected:
- no crash/OOM;
- main PDF remains the sharper primary pane;
- companion may sharpen more conservatively;
- no permanent blank pages.

## Closeout

v0.5.32 is PHONE PASS when the user can cross-reference documentation in the lower pane without losing the open schematic context.
