# Renault Docs v0.4.1 — Technical Blue header wave 1

## Purpose

Start implementing the approved Technical Blue viewer UX without migrating legacy Renault frame navigation yet.

## Implemented

### Viewer top actions
The legacy/Classic viewer now exposes upper-screen actions:
- Back;
- Modern quick switch when Modern routing context is available;
- Home / main Renault Docs library;
- Search on current page;
- Settings.

### Classic → Modern bridge
Classic is no longer a dead end.

The Modern action:
- returns to/open Modern for the same dataset;
- preserves the current volume entrypoint when known;
- highlights and scrolls to that volume in the Modern volume list.

It does not change the saved default Modern/Classic preference.

### Current-page search
Search opens as a compact row below the viewer header:
- query field;
- previous match;
- next match;
- match counter;
- close.

Search uses Android WebView page find for the currently loaded HTML content.

Current limitation:
PDF pages are currently rendered as images by PdfRenderer, so OCR/text-layer PDF search is not part of v0.4.1.

### PDF toolbar polish
- all controls use centered content;
- manual zoom field is slightly wider;
- Fit width now shows `↔ По ширині`;
- toolbar can horizontally scroll on very narrow screens instead of clipping controls;
- page area keeps narrow pages centered.

## Not included yet

- native 101/103/105/... section navigation;
- Technical Blue mode tabs;
- global/volume search;
- searchable PDF text layer;
- runtime-selectable skins.
