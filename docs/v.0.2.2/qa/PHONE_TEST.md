# v0.2.2 Phone Test

## A — Update

1. Install v0.2.2 over stable-signed v0.2.1.
2. Expected: normal update, no uninstall required.
3. Existing dataset registration should remain.

## B — Performance

1. Open Laguna dataset.
2. Open NT8236.
3. Navigate through 3–5 menu entries.
4. Return to a previously visited entry.
5. Expected: repeated navigation is visibly more responsive; no multi-second repeated directory lookup stalls.

## C — PDF

1. Open the PDF that previously showed the placeholder:
   `Laguna X74 NT8236A 2002_11_18/COMMUN/PDF/PC/S20.PDF`.
2. Expected: first PDF page renders inside the existing Renault frame.
3. Scroll down through several pages.
4. Expected: continuous vertical pages, lazy loading near viewport.
5. Use previous/next.
6. Use zoom + and -.
7. Expected: no crash; visible pages rerender.

## D — lifecycle/navigation

1. Rotate inside HTML page.
2. Rotate inside PDF page.
3. Press Back.
4. Expected: no picker relaunch; navigation remains owned by viewer/history.

## PASS gate

Do not mark performance or PDF PASS until tested on the real phone.
