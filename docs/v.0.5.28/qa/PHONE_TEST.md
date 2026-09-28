# v0.5.28 Phone test — 400% zoom + fullscreen

## Prepare

1. Renault Menu → 5.
2. Renault Menu → 8.
3. Install v0.5.28.
4. Do NOT run point 9.

Use the same representative multi-page PDF used for v0.5.27.

## Gate A — 400% zoom

Start at 100%.

Expected:
- pinch can continue beyond 200%;
- live field can reach values up to 400%;
- quick menu contains 300% and 400%;
- +/- continue from the current value;
- typed 350% / 400% are accepted;
- values above 400% clamp to 400%.

## Gate B — high-zoom quality and memory

At 300–400%:
- inspect a schematic/detail;
- stop moving;
- current page should sharpen after the existing short debounce;
- scroll to the next page and back.

Expected:
- no crash/OOM;
- no chain of several huge decoded neighboring pages;
- current page may need a short reload when moving between pages at this zoom.

## Gate C — fullscreen

Rotate to landscape and press the fullscreen icon in the PDF toolbar.

Expected:
- app-level top toolbar disappears;
- Android status/navigation bars disappear;
- PDF toolbar remains visible;
- document occupies the extra space;
- zoom/scroll position does not reset.

Press the fullscreen icon again.

Expected:
- app toolbar and system bars return.

## Gate D — Back and rotation

While fullscreen:
- press Android Back once;
- enter fullscreen again;
- rotate landscape ↔ portrait ↔ landscape.

Expected:
- first Back exits fullscreen instead of leaving the document;
- fullscreen state survives recreation when active;
- PDF page/zoom/relative position remain approximately stable.

## Closeout

v0.5.28 is PHONE PASS when 400% zoom remains stable and fullscreen entry/exit is predictable.
