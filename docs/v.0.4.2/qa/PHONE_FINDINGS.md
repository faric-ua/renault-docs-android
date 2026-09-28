# v0.4.1 → v0.4.2 Phone Findings

Date: 2026-09-24

## Confirmed working in v0.4.1

- current-page WebView text search works;
- search highlighting and match navigation work;
- Technical Blue top header actions are visible;
- Classic → Modern bridge is available.

## Phone findings

### UI-SEARCH-001 — search field too narrow

Observed:
search query text is difficult to read because query, match counter and navigation/close controls share one horizontal row.

v0.4.2 fix:
- search UI becomes two rows;
- first row gives the query field substantially more width;
- second row contains match/navigation controls.

### CLASSIC-ORDER-001 — Classic catalog not chronological

Observed:
existing Classic START.html begins with 2005/2006 volumes and then returns to 2001.

Cause:
the existing generated START.html predates chronological package ordering.

v0.4.2 fix:
- Android sorts Classic START.html cards by normalized date at runtime;
- future generated Classic catalogs also use chronological ordering;
- existing dataset does not require full reconversion only for this fix.

### PDF-FIT-001 — Fit-width arrow visually off-center

Observed:
the Unicode horizontal arrow in the Fit width control is not visually centered even though the button uses centered layout.

Cause:
font glyph metrics/baseline, not container alignment.

v0.4.2 fix:
- replace Unicode arrow with an inline vector icon;
- center icon + label as a single flex group.

## v0.4.2 CI

- source: `90ef691d862fd28f24ca0b8ed088c6a0c81d2929`
- Tests: `35940255443` — PASS
- Android Debug APK: `35940255455` — PASS
- artifact: `Renault-Docs-v0.4.2-Debug`

## Subsequent phone verification

Confirmed on phone:
- two-row Search UI works;
- current-page text search works;
- Classic catalog tiles are chronologically sorted on the existing dataset.

New follow-up:
- reopening Search with an unchanged preserved query does not reactivate WebView matches until the input changes;
- Backup Settings presentation needs clearer user-facing path/semantics.

These follow-ups are handled by v0.4.3.

PDF Fit width visual alignment still requires explicit phone confirmation after the vector-icon change.
