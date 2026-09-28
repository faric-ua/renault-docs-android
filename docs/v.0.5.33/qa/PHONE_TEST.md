# v0.5.33 Phone test — split focus mode + draggable divider

## Prepare

1. Renault Menu → 5.
2. Renault Menu → 8.
3. Install v0.5.33.
4. Point 9 is NOT required if already completed for v0.5.29+.

Use the same schematic + documentation PDF pair from the v0.5.32 screenshots.

## Gate A — draggable divider

Open Companion before fullscreen.

Expected:
- a visible thin divider appears between documents;
- drag it up/down;
- main/companion sizes change continuously;
- neither pane can collapse completely;
- useful range is approximately 20–80%.

## Gate B — enter fullscreen with two documents

With Companion open, enter fullscreen.

Expected:
- app header/system bars hide;
- companion header hides;
- both PDF toolbars hide;
- only documents + divider remain;
- page/zoom/scroll of both PDFs remain unchanged.

## Gate C — double tap controls

Double tap either PDF.

Expected:
- PDF controls appear OVER the documents rather than permanently consuming document height;
- floating Back/Close/fullscreen controls appear;
- after about 3 seconds they disappear again;
- document position does not jump when controls appear/disappear.

Repeat from the lower document.

## Gate D — control actions

While controls are visible:
- use companion Back;
- reopen controls and Close companion;
- reopen Companion;
- enter fullscreen and use floating fullscreen exit.

Expected:
- controls work;
- main schematic never navigates away due to companion actions;
- closing companion returns main PDF to full workspace.

## Gate E — rotation

With:
- fullscreen active;
- Companion open;
- divider moved away from 50/50;
- both PDFs at non-default zoom/pages;

rotate portrait ↔ landscape and back.

Expected:
- fullscreen returns;
- Companion remains open;
- divider ratio is restored;
- both documents restore their page/zoom/scroll state;
- focus controls stay hidden unless they were temporarily visible at rotation time.

## Gate F — landscape usability

In landscape:
- move divider to give one document more height;
- double tap to reveal controls;
- zoom/scroll both documents.

Expected:
- significantly more document area than v0.5.32;
- no permanent toolbar/header stack;
- divider remains easy to grab.

## Gate G — stability

Stress:
- main PDF 300–400%;
- companion PDF 200–300%;
- drag divider several times;
- rotate;
- show/hide controls repeatedly.

Expected:
- no crash/OOM;
- no blank page that requires lowering zoom;
- no accidental reset of either PDF.

## Closeout

v0.5.33 is PASS when fullscreen two-document use feels like a document workspace first, with controls available on demand rather than permanently occupying the screen.
