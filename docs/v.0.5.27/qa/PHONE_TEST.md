# v0.5.27 Phone test — PDF pinch zoom

## Prepare

1. Renault Menu → 5.
2. Renault Menu → 8.
3. Install v0.5.27.
4. Do NOT run point 9; this release does not change converter/Runtime IR/package data.

Use the same representative multi-page PDF that was used for v0.5.24/v0.5.25 high-zoom testing.

## Gate A — toolbar isolation

Open the PDF at 100%.

Expected:
- portrait PDF controls remain two rows;
- pinch the document with two fingers;
- only the document changes size;
- toolbar height, button size and text size remain unchanged;
- toolbar never pans horizontally with the zoomed document.

## Gate B — live zoom percentage

Start at 100% and slowly pinch in/out.

Expected:
- the zoom field changes immediately during the gesture, e.g. 103%, 117%, 146%;
- it does not wait until the fingers are released;
- +/- buttons continue from the current pinch-derived percentage.

## Gate C — focal-point stability

Pinch while looking at a recognizable detail in the middle of a page.

Expected:
- the page grows/shrinks around approximately the touched region;
- the viewer should not jump to another page or to the top-left corner.

## Gate D — deferred quality upgrade

Pinch quickly from 100% toward 180–200%.

Expected:
- movement is immediate using the already decoded bitmap;
- during movement the page may temporarily look softer;
- after the gesture stops, the current page becomes sharper after a short delay;
- there should be no white flash while the sharper render replaces the previous bitmap.

## Gate E — long-document regression

At 180–200%:
- scroll 8–10 pages down;
- go several pages back;
- pinch once more;
- rotate portrait ↔ landscape.

Expected:
- no crash/OOM;
- no repeated blank pages;
- toolbar remains fixed;
- current page/relative position survives rotation approximately as in v0.5.25.

## Closeout

v0.5.27 is PHONE PASS when A–E are acceptable on the real device.
