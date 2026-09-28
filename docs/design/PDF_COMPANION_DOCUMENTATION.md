# PDF Companion Documentation — design note

Date: 2026-09-26
Status: FIRST IMPLEMENTATION MERGED IN v0.5.32 — PHONE VALIDATION PENDING

## Motivation

Renault wiring diagrams contain block/component/fuse identifiers that are not practical to fully know or infer automatically.

The app therefore needs a fast manual cross-reference workflow:
- keep the wiring diagram open;
- inspect an identifier on the diagram;
- open the documentation set that belongs to the SAME Renault volume/configuration;
- look up fuses, electrical equipment, connections, abbreviations, general information, etc.;
- continue reading the original diagram without losing page/zoom/scroll state.

This is not an automatic block-number recognition feature. The user remains in control of the lookup.

## Durable ownership model

Documentation remains owned by the Renault volume/configuration.

Example:

```text
Volume A
  ├─ sections 101 / 105 / 120 / ...
  └─ Documentation A
      ├─ GENE
      ├─ PLATFUSI
      └─ AIDE

Volume B
  ├─ its own sections
  └─ Documentation B
      ├─ its own GENE
      ├─ its own PLATFUSI
      └─ its own AIDE
```

Never merge or reuse documentation across different volumes/configurations.

Removing duplicated GENE / PLATFUSI / AIDE graphs from section shards in a future cleanup MUST NOT remove access to the documentation. Sections and PDF viewer should reference the one volume-level documentation owner.

## Proposed PDF Companion mode

Add a new control near the existing fullscreen control in the PDF toolbar.

Working name:
- Companion;
- Documentation pane;
- Split documentation;
- Reference pane.

Preferred concept:
- main frame remains the currently open PDF schematic;
- a second documentation frame opens below it;
- the second frame loads the same volume's documentation entrypoint/shard;
- closing the second frame returns the schematic to full available height.

The main schematic must not be recreated when the companion frame opens.

## Companion frame content

Initial state should present the same volume documentation menu, for example:
- Загальна документація;
- Запобіжники;
- Довідка.

From there the user can navigate to useful reference material such as:
- fuse numbers;
- electrical equipment/block lists;
- connection lists;
- ground connection lists;
- abbreviations;
- subject index;
- other volume-owned documentation.

The exact content remains whatever exists in that volume. Do not hardcode assumptions that every volume contains identical categories.

## Interaction contract

Main PDF frame:
- preserves current page;
- preserves zoom;
- preserves horizontal/vertical scroll;
- keeps existing 50–400% behavior;
- remains independently scrollable/zoomable.

Documentation companion frame:
- has independent navigation;
- has independent scroll state;
- if it opens a PDF, it should have its own page/zoom state;
- may render HTML or PDF depending on the documentation target;
- must never navigate the main schematic away.

A divider between the two frames should eventually be draggable/resizable.

Suggested default:
- portrait: documentation opens as a bottom pane;
- landscape: bottom pane is acceptable initially; later an adaptive side-by-side layout can be considered if it improves readability.

## Fullscreen behavior

Fullscreen should apply to the entire PDF workspace, not destroy the companion pane.

When Companion is open:
- fullscreen hides app/system chrome as today;
- main PDF + companion pane both remain visible;
- exiting fullscreen restores the same split state.

## Lifecycle contract

Rotation/recreation should restore:
- whether Companion is open;
- divider size if adjustable;
- main PDF page/zoom/scroll;
- companion navigation target;
- companion page/zoom/scroll when the companion document is PDF.

No automatic document reload should reset either pane to its root unless recovery is required.

## Memory/performance budget

Two PDF frames can double bitmap pressure, so the viewer must not simply run two unrestricted renderers.

Suggested policy:
- main schematic remains primary quality target;
- main current page gets the normal full-quality idle render;
- companion PDF keeps only its current visible page decoded at high quality;
- neighbor prefetch for companion should be conservative;
- inactive/covered pane should release far decoded bitmaps aggressively;
- shared compressed render cache may still be reused where safe;
- never preload an entire documentation PDF just because the split pane is open.

This should be treated as a two-pane render-budget problem, not two independent unlimited viewers.

## Navigation entry points

Long-term preferred access:
1. Volume screen → Documentation opens the dedicated volume documentation screen.
2. Section screen → Documentation acts as a shortcut to that same volume documentation.
3. Open PDF → Companion button opens the same volume documentation inside a secondary frame without leaving the PDF.

All three entry points resolve through the SAME volume documentation identity/path.

## Explicit non-goals for first implementation

Do not require:
- OCR of numbers on the wiring diagram;
- automatic block recognition;
- automatic linking of every schematic number to documentation;
- cross-volume search;
- merging documentation from several volumes.

Those can be explored later if reliable mappings become available.

## Future enhancement possibility

If the converter eventually discovers trustworthy mappings between schematic block identifiers and documentation entries, the companion pane could support optional targeted lookup.

That should be additive. The manual cross-reference workflow must remain usable even when no automatic mapping exists.


## v0.5.32 implementation note

The first implementation now exists:
- vertical split in ViewerActivity;
- main schematic above, documentation companion below;
- same-volume documentation root rendered in the companion;
- final HTML/PDF documents stay in the companion;
- separate PDF state scopes;
- compact companion PDF render budget;
- fullscreen/rotation state persistence.

Still deferred:
- draggable divider;
- adaptive side-by-side landscape;
- automatic identifier linking.
