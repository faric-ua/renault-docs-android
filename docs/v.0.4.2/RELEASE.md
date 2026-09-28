# Renault Docs v0.4.2 — Search layout, Classic order, Fit width alignment

## Purpose

Phone QA follow-up for v0.4.1.

## Fixes

### Search UI
The current-page search panel is now two rows.

Row 1:
- wide search field;
- current match / total match count.

Row 2:
- previous match;
- next match;
- close.

This keeps entered text readable on portrait phones.

### Classic package catalog sorting
Existing converted datasets may contain an older `_renault/START.html` whose cards were written in non-chronological order.

v0.4.2 fixes this at runtime:
- when Classic opens `_renault/START.html`;
- Android injects a small local JavaScript sorter;
- cards are reordered by normalized YYYY-MM-DD date ascending;
- undated cards go last.

Important:
- no dataset rebuild is required;
- Fast Pack does not need to be regenerated;
- future package generation is already chronological as well.

### PDF Fit width
The Fit width control no longer relies on the font-rendered Unicode `↔` glyph.

It now uses:
- an inline vector/SVG double-arrow;
- separate label text;
- flex centering for icon + label as one unit.

This removes baseline/font-dependent visual misalignment.

## Unchanged

- Fast Pack;
- original PDF save/export;
- Modern chronological sorting;
- Classic → Modern bridge;
- Settings;
- current-page HTML search behavior.
