# Open findings

## BUG-001 — PDF toolbar/control frame does not expand with PDF viewport

Status: **FIXED IN v0.5.12 · PHONE TEST PENDING**  
Found: 2026-09-24  
Source: real phone test

### Observed

In the Android PDF viewer, the upper control frame containing the zoom and view controls (for example `200%`, the zoom dropdown, and the `↔ По ...` fit-mode control) does not stretch to the same usable width as the PDF area.

The PDF page itself occupies the available content width, while the toolbar/control frame ends noticeably earlier and the right-side control is clipped.

### Expected

The PDF toolbar/control frame should:
- use the same available viewport width as the PDF area;
- adapt to device width/orientation;
- keep all controls reachable without visually truncating the toolbar because of a fixed/narrow container.

### Fix

The PDF toolbar now receives the same zoom-derived width as the displayed PDF page. At 200% it expands with the document instead of remaining viewport-sized and clipping controls. The `+` zoom button is also moved directly after the zoom dropdown, before `По ширині`.

Screenshot evidence was supplied in the 2026-09-24 phone tests.

## UX-001 — Stable three-row Modern section menu

Status: **SUPERSEDED BY UX-004 / v0.5.13**  
Found: 2026-09-24  
Source: real phone test

### Required layout

For every section/album when the corresponding actions exist:

1. row 1: `Схеми` + `Розʼєм`;
2. row 2: `Положення на авто`;
3. row 3: `Документація`.

Historical v0.5.11 rule: the renderer remained data-driven and absent actions could disappear.

Superseded in v0.5.13: the four primary positions are now stable; unavailable primary actions remain visible but disabled/dimmed. Unknown future actions still render after the known group.

## BUG-002 — Classic connector pin-description frame is unreadable on dark canvas

Status: **PHONE PASS in v0.5.12**  
Found: 2026-09-24  
Source: real phone screenshot

### Observed

In Classic nomenclature, the upper `dessin` PDF is readable, but the lower `alveoles`/pin-description HTML frame is composited over the dark app/WebView canvas. The legacy page uses dark text and becomes almost unreadable.

### Fix

When a non-hybrid Classic volume opens through its top-level `INDEX.HTM`, the WebView canvas is white. Explicit legacy frame backgrounds remain intact, while transparent detail pages get the light background expected by the old documentation.

### Phone result

PASS. The Classic connector pin/contact description is now clearly readable. The original compact table layout is also accepted as the visual reference for the Modern `Опис контактів` renderer: one logical row per line, visible column structure, dense spacing, and horizontal access to long content.

## BUG-003 — Modern NM flattened the connector drawing and contact description

Status: **FIXED IN v0.5.11 · PHONE TEST PENDING**  
Found: 2026-09-24  
Source: parity comparison with Classic

### Observed

Classic `LIENNM/...HTM` keeps two distinct child documents:
- `dessin` → connector drawing PDF;
- `alveoles` → contact/pin description HTML.

Runtime IR already preserves these as separate nested documents, but the first Modern renderer immediately appended the structured contact table under the PDF action.

### Expected / fix

Modern now presents two explicit actions:
- `Схема розʼєму`;
- `Опис контактів`.

The contact description is rendered only after the user opens it. Static pin rows are rendered as plain data rather than button-like cards.

## UX-002 — Compact Modern header and menu density

Status: **SUPERSEDED BY UX-004 / v0.5.13**  
Found: 2026-09-24  
Source: real phone screenshots

### Required

Historical v0.5.12 requirement:
- top section title stayed on one line;
- `Розділи` and `Classic` shared the top bar;
- `CMP 101` was replaced by `101 — <section title>`;
- menu density was reduced.

Superseded in v0.5.13 by the global Back/Home/Search/Settings row plus a separate code-only `Modern | Classic` context row.

## UX-003 — Connector contact table must be one-line and horizontally pannable

Status: **SUPERSEDED BY UX-005 / v0.5.14**  
Found: 2026-09-24  
Source: real phone screenshot

### Observed

The native contact/pin description wraps long rows over several lines, which makes the table difficult to scan.

### Historical v0.5.12 fix

Each source row was temporarily rendered as one non-wrapping monospace line inside a horizontal scroller.

### Superseded contract

Phone comparison with Classic led to v0.5.14/UX-005: pins and abbreviations now use a shared bordered native table. Long descriptions wrap inside their cells while source row/column boundaries are preserved. v0.5.15 further adds content-aware widths and vertically centered compact cells.

## BUG-004 — Modern volume catalog drops non-3-digit Classic entries

Status: **IMPLEMENTED IN v0.5.26 · CI / PHONE PARITY PENDING**  
Found: 2026-09-24  
Source: real phone Classic vs Modern comparison + source inspection

### Observed

Classic volume navigation contains many entries that do not appear in the Modern volume catalog.

Real phone examples visible in Classic include:
- ordinary numeric identifiers such as `101`;
- four-digit identifiers such as `1405`, `1406`, `1407`, `1412`;
- `R`-prefixed identifiers such as `R15`, `R21`, `R24`, `R262`, `R265`, `R325`;
- alphabetic identifiers such as `MA`, `MAH`, `MYH`, `MB`, `ME`, `MG`, `MH`, `ML`, `MQ`, `MT`, `MW`, `NA`, `NC`, `NH`, `NT`, `NU`.

Modern currently shows only the subset discovered as three-digit section codes.

### Confirmed root cause

`core/sections.py` currently uses:

```python
_SECTION_RE = re.compile(
    r"^\s*(\d{3})(?!\d)\s*(?:[-–—:.;]+\s*)?(.*?)\s*$"
)
```

Therefore the converter can only index exactly three decimal digits.

Entries like `1405` or `R262` cannot enter `modern-sections.json` / Runtime IR through this discovery path.

There is a second parity risk: the current converter/Android path sorts and deduplicates primarily by `code`, while Classic navigation order is source-defined and may contain identifiers outside a numeric-only model.

### Required fix

The Modern catalog must be generated from the complete Classic navigation contract, not a 3-digit regex.

Required behavior:
- treat the Classic identifier as an opaque string, not a number;
- accept numeric, mixed alphanumeric, and alphabetic identifiers (examples above);
- preserve Classic source order;
- keep only entries whose legacy link resolves to a real local target;
- do not silently merge distinct entries just because their display code matches;
- compile Runtime IR for every valid catalog target;
- retain search by code/title in Modern.

This is a converter/data-contract change and will require Renault Menu → 9 after implementation.

## UX-004 — Native section chrome + persistent primary tiles

Status: **PHONE LAYOUT PASS in v0.5.13 · SEARCH ACTION STILL PENDING**  
Found: 2026-09-24  
Source: real phone UI review

### Required

- global app controls (Home/Search/Settings) are visible above the section context row;
- section context row shows only the section identifier plus a compact Modern/Classic switch;
- the repeated section title is removed from the top row because the content heading already shows `<code> — <title>`;
- primary actions `Схеми`, `Розʼєм`, `Положення на авто`, `Документація` keep stable positions;
- if an action is unavailable, its tile remains visible but disabled/dimmed;
- section selector groups are presented as compact rounded tiles/cards rather than a loose stack of labels and default buttons.

This is an APK-only UI contract. BUG-004 catalog completeness remains separate.

## BUG-005 — Modern renders abbreviation glossary as a generic one-line table

Status: **FIXED IN v0.5.14 · PHONE TEST PENDING**  
Found: 2026-09-25  
Source: Classic/Modern phone comparison

### Observed

Classic contains a dedicated abbreviations/glossary document with two semantic columns:
- `СОКРАЩЕНИЯ`;
- `ПОЛНЫЕ НАИМЕНОВАНИЯ`.

Examples visible on phone include `DG`, `DD`, `K74`, `B74`, `E2`, `SSNAV`, `SRUNLI`, `NINAV3`, `DIESEL`, `ESS`, engine codes and gearbox codes.

The current Modern generic structured-table renderer flattens this into long monospace one-line rows. On phone the right side is clipped and the glossary is substantially harder to read than Classic.

### Required renderer contract

Do not use the connector-pin one-line/horizontal-table presentation for every structured table.

Detect/retain table semantics from Runtime IR and render a glossary as a real two-column native table:
- left column = abbreviation/code, compact fixed/minimum width;
- right column = full description, uses the remaining width and can wrap;
- each source row remains a distinct row;
- table can still pan horizontally only when genuinely wider than the viewport;
- preserve the Classic order.

This was the initial BUG-005 proposal. It was superseded by the later user decision recorded in UX-005: glossary and connector pin data share one adaptive native grid, with column-count-specific proportions.

### Converter implication

Runtime IR must preserve enough structured-table metadata (headers and row cells) so Android can distinguish a glossary from a pin/contact table instead of rendering both with one generic policy.

## UX-005 — Structured tables share one native grid + PDF export

Status: **PHONE PARTIAL PASS in v0.5.14 · LAYOUT REFINEMENT IN v0.5.15**  
Found: 2026-09-25  
Source: real phone comparison with Classic

### Contract

Abbreviation/glossary tables and connector pin/contact descriptions use the same native table component:
- bordered grid;
- column-aware proportions;
- source row boundaries preserved;
- long text wraps inside cells;
- no generic pipe-separated monospace stream.

Connector documents also use a Classic-inspired information hierarchy:
- `<code> — <section title>`;
- criteria/configuration line below;
- connector/location metadata grouped separately;
- table below.

Every structured native table document offers `Зберегти таблицю PDF` and exports title + criteria + metadata + table rows using Android's standard create-document flow.

### v0.5.14 phone evidence

PASS:
- Android create-document flow opens;
- PDF is created successfully;
- saved PDF opens in the system PDF viewer;
- Cyrillic text is readable.

Needs refinement:
- column widths are too fixed instead of adapting to actual content;
- short code cells in rows with wrapped descriptions should occupy the full row height and center their text vertically;
- exported filename `Renault_101_table.pdf` is too generic.

## UX-006 — Adaptive table columns + vertically centered multi-line rows

Status: **PHONE PASS* IN v0.5.16**  
Found: 2026-09-25  
Source: real phone screenshots of native and exported tables

### Required

For both native table rendering and generated PDF:
- compact/code columns should size from actual body content, not a hard-coded 25/75 or 8/12/14/66 split;
- header labels must not force an unnecessarily wide code column;
- the description column receives the remaining width;
- two-column glossary exports use portrait page geometry for better on-phone readability;
- wider pin/contact tables may use landscape geometry;
- when the description wraps to two or more lines, the short code cell (for example `E2` / `E3`) spans the same row height and its text is vertically centered;
- header cells are centered.

Implementation uses shared `NativeTableLayout.columnFractions(...)` in both Android native rendering and PDF export.

## UX-007 — Combined connector view: scheme + pins

Status: **PHONE PASS IN v0.5.16**  
Found: 2026-09-25  
Source: comparison of Modern connector menu with Classic combined connector view

### Required

For a connector composite that contains both:
- drawing PDF (`dessin`);
- pin/contact document (`alveoles`);

Modern must offer three choices:
- `Схема + піни розʼєма`;
- `Схема розʼєму`;
- `Опис контактів`.

The combined action opens the original connector composite document itself inside the app viewer. This intentionally reuses the proven legacy connector frameset only for this single combined document, without returning to the full Classic volume navigation.

## UX-008 — Source-aware structured-table PDF filenames

Status: **SUPERSEDED BY UX-009 / v0.5.16**  
Found: 2026-09-25  
Source: real saved-file screenshot

### Observed

v0.5.14 suggested the generic name:

```text
Renault_101_table.pdf
```

### Required

Use the source connector/document identity when available.

Examples:
- `101(pins).pdf`;
- `101_1(pins).pdf`;
- `101(abbreviations).pdf`.

For connector pin documents, if the legacy source filename contains a section-derived variant such as `101_1`, preserve that variant in the suggested filename. Fall back to the current section code when no stronger source identifier exists.



## UX-009 — Opaque Renault connector ID in pin-PDF filenames

Status: **PHONE PASS IN v0.5.16**  
Found: 2026-09-25  
Source: Runtime IR inspection + user clarification of Renault connector semantics

### Clarified source semantics

Connector/source suffixes such as `_1`, `_2`, `_3` are part of the Renault source identifier. They are **not** file-collision counters and do not have one universal semantic meaning.

Examples:
- section `101`: `101_1` and `101_2` select different applicability variants; observed criteria include `DD`, `DG`, `E2`, `E3`, navigation and equipment criteria;
- section `120` (ECU): source IDs may identify separate physical connector/pin documents. Real-phone evidence now includes `120_18`, so suffix values are not limited to `_1/_2/_3`.

Therefore Android/converter/runtime/export code must treat the complete source identifier as opaque and must not infer meaning from the numeric suffix.

### Export contract

For pin/contact PDF export:
- `T_101_1.HTM` → `101_1(pines).pdf`;
- `T_101_2.HTM` → `101_2(pines).pdf`;
- `T_120_1.HTM` → `120_1(pines).pdf`;
- `T_120_2.HTM` → `120_2(pines).pdf`;
- `T_120_3.HTM` → `120_3(pines).pdf`.

The `T_` prefix is only a legacy technical wrapper around the contact-table source filename and is stripped. The remaining source stem is preserved.

If Android's document provider reports that an exact filename already exists, normal system save/replace behavior applies. The app must **not** manufacture `_1`, `_2`, ... as duplicate counters, because those suffixes may already be real Renault connector IDs.

## BUG-006 — Generated table PDF must show and repeat the column header

Status: **v0.5.17 PHONE FAIL · ROOT CAUSE EXTENDED TO RUNTIME IR · v0.5.18 IMPLEMENTED / CI PENDING**  
Found: 2026-09-25  
Source: real phone test of v0.5.16 + exporter code inspection

### Observed

The native structured tables show their column names correctly, but the generated table PDF was reported without the expected table header / column-name row.

Separately, code inspection confirmed a pagination defect: when a table continued onto a new PDF page, `NativeTablePdfExporter` started a new page and redrew the document title/criteria/metadata, but did not redraw the table's leading header row before continuing body rows.

### Required

For every structured-table PDF:
- preserve the leading source rows marked as table headers;
- draw the table header before the first body row;
- repeat the same table header after every page break;
- keep source column names unchanged;
- keep the v0.5.15 adaptive widths and vertical text alignment;
- do not invent a header for a source table that has no leading header rows.

### v0.5.17 implementation

`NativeTablePdfExporter.drawTable(...)` now separates:
- leading `tableHeaderRows = table.rows.takeWhile { it.header }`;
- remaining body rows.

The leading header block is rendered before the body and again after each PDF page break.

This is APK-only. Runtime IR regeneration is not required.

### Additional phone evidence for UX-009

Observed on phone:
- original/source PDF: `120_18.PDF`;
- exported pin PDF: `120_18(pines).pdf`.

This confirms preservation of a non-trivial opaque source suffix (`_18`). The `*` means coverage is representative, not exhaustive: the exact observed `120_18` path is confirmed, but all possible 120 variants were not enumerated.


## BUG-007 — Compact pin values such as 0.6 wrap inside narrow columns

Status: **PHONE PASS FOR 0.6 IN v0.5.18 · WIDTH POLICY REFINED IN v0.5.19**  
Found: 2026-09-25  
Source: real phone screenshot of section 103 / connector variant 103_5

### Observed

In the native pin/contact table, wire cross-section `0.6` is rendered as:

```text
0.
6
```

The value is one atomic technical token and must stay on one line.

### Related BUG-006 phone evidence

The same real-phone example `103_5(pines).pdf` confirms that v0.5.17 still has no visible table column header on page 1.

The v0.5.17 PDF renderer can only repeat rows already marked as headers. Some legacy Renault contact pages use ordinary `td` cells and/or visual image-based header cells, so Runtime IR may contain no explicit header row.

### v0.5.18 fix

Converter / Runtime IR:
- for legacy connector-contact documents whose filename begins with `T_`;
- preserve empty source cells so column positions are not collapsed;
- for a 4-column pin table, normalize the first visual/header row into the Modern semantic header:
  - `№`;
  - `мм²`;
  - `Код`;
  - `Опис`;
- if the first row is already actual pin data, prepend that semantic header instead of replacing data;
- do not synthesize this header for unrelated structured HTML documents.

Android / PDF layout:
- widen the second compact pin column;
- keep the first three 4-column pin-table fields on one line;
- PDF exporter also treats the first three compact fields as non-wrapping tokens.

Because Runtime IR generation changed, v0.5.18 requires Renault Menu → 9 after installing/updating the app.


## BUG-008 — Pin-table header detection must be structural, not filename-based

Status: **IMPLEMENTED IN v0.5.19 · CI / PHONE TEST PENDING**  
Found: 2026-09-25  
Source: real-phone v0.5.18 screenshots for 103_5 / 108

### Observed

After v0.5.18 + package regeneration:
- compact value `0.6` correctly remains on one line;
- the generated/native pin table still has no semantic column header;
- therefore the previous converter assumption that relevant contact tables can be recognized from a `T_*.HTM` filename is incomplete.

### Required

Recognize a Renault pin/contact table from its data shape, not from the wrapper filename:
- column 1: pin/contact number;
- column 2: wire cross-section;
- column 3: wire/color/code token;
- column 4: description.

If the first detected body row is row 1, prepend the semantic header:
`№ | мм² | Код | Опис`.

If one or more visual-only legacy rows appear before the first real body row, replace that visual header block with the semantic header.

Unrelated 4-column structured tables must remain unchanged.

## UX-010 — Compact columns sized by content/header; last column fills remainder

Status: **IMPLEMENTED IN v0.5.19 · CI / PHONE TEST PENDING**  
Found: 2026-09-25  
Source: real-phone v0.5.18 screenshots / user review

### Required

For structured tables:
- every column before the last is a compact technical column;
- its width must account for both the longest body value and the header label;
- compact columns must stay on one line;
- the last description column receives all remaining width;
- in 2-column glossary tables, the first column must be wide enough for labels such as `СОКРАЩЕНИЯ` without wrapping;
- in pin tables, compact columns such as `1`, `0.6`, `2A` should not consume excessive width;
- native UI and generated PDF must use the same width contract.


## UX-011 — Composed pines PDF from connector card + section identity + pin table

Status: **IMPLEMENTED IN v0.5.20 · CI / PHONE TEST PENDING**  
Found: 2026-09-25  
Source: real-phone screenshots + comparison with original Renault connector/pin PDF composition

### Required document structure

A `(...pines).pdf` export is not a raw dump of the current native table screen. It is a dedicated technical document composed from three logical blocks:

1. Connector/application card:
   - vector connector icon on the left;
   - source zone/type on the first right row (for example `ЭЛПРОВ. САЛОНА`, `ЭЛПРОВ. ДВИГ.`);
   - source applicability/configuration on the second right row (for example `B74,K74`, `F5R700`, `DD`);
   - centered compact card, not full-page width.

2. Section identity block:
   - section code;
   - section title;
   - criteria/applicability line;
   - centered;
   - additional metadata preserved between connector card and identity block when present.

3. Pin/contact table:
   - portrait page;
   - text header plus vector icons;
   - semantic labels: `№ | мм² | Код | Опис`;
   - first three columns sized from actual text/header measurements;
   - final description column receives the remaining width;
   - compact values centered;
   - description left-aligned and vertically centered.

### Header icon contract

The PDF renderer draws simple local vector icons so the document does not depend on external bitmap assets:
- connector card: connector housing + socket dots;
- `№`: contact/pin icon;
- `мм²`: wire cross-section icon;
- `Код`: wire/zig-zag icon;
- `Опис`: Renault-like directional arrow/dots icon.

The textual header remains visible together with the icons.

### Pagination

Page 1:
- connector card;
- section identity/applicability;
- pin header;
- body.

Continuation pages:
- compact section identity;
- repeated pin header;
- remaining body rows.

The large connector card is not repeated on continuation pages.

### v0.5.20 implementation

`NativeTablePdfExporter` now detects `(pines)` exports and uses a dedicated portrait composition path.

This is APK-only. No converter/Runtime-IR changes are introduced by v0.5.20 itself. If the phone already regenerated the package with v0.5.19, Renault Menu → 9 is not required again for v0.5.20.


## UX-012 — v0.5.21 table/PDF visual polish after v0.5.20 phone pass

Status: **IMPLEMENTED IN v0.5.21 · CI / PHONE TEST PENDING**  
Found: 2026-09-25  
Source: real-phone screenshots of v0.5.20

### Phone observations

The v0.5.20 composed pines PDF works functionally, but visual polish is still needed:
- native 4-column pin/contact view may still show body rows without the semantic text header;
- the third compact column (`Код`) is wider than its real content requires;
- PDF content starts too close to the top edge;
- pines header icons work but should look more modern and use restrained color accents;
- generic abbreviations PDF title should be centered;
- generic PDF table should sit farther below the title;
- PDF page margins should be approximately 2 cm;
- the first column of a 2-column glossary should be sized from its actual header/body content rather than taking a large fixed share.

### v0.5.21 contract

Native pin/contact table:
- when the current export/document is a `(pines)` document and the 4-column source rows do not begin with a semantic header, the Android renderer prepends:
  `№ | мм² | Код | Опис`;
- compact columns are tightened; description keeps the remainder.

PDF:
- page margins use approximately 2 cm (`57 pt`);
- generic title/criteria are centered;
- extra spacing separates title/header metadata from the first table;
- pines compact columns are measured more tightly;
- the final description column keeps the remainder;
- technical icons use restrained color accents with soft background badges;
- connector/application metadata remains preserved between the connector card and the section/table composition.

This is APK-only. No converter/Runtime-IR regeneration is required by v0.5.21 itself.


## BUG-009 — Rotation can collapse child windows back to parent content

Status: **CLOSED · PHONE PASS on current reachable UI**  
Found: 2026-09-25  
Source: real-phone user report

### Observed

Several app-owned child windows/content states disappear after device rotation. The recreated Activity may show a parent/default window instead of the exact child the user had open.

Confirmed code-level vulnerable areas before v0.5.22:
- NativeSectionActivity inline panel/document/Documentation state;
- SettingsActivity chooser dialogs;
- ViewerActivity section navigator dialog;
- ViewerActivity Frame debug dialog;
- NativeSectionActivity pending table-PDF save payload during Activity recreation.

### v0.5.22 lifecycle contract

Rotation must preserve the same logical window. It must not:
- jump to the first parent panel;
- close an app-owned chooser;
- lose the section navigator query;
- auto-run an action.

Implemented:
- NativeSection view kind + id restoration;
- Settings active chooser restoration;
- Viewer dialog-kind + navigator-query restoration;
- Frame debug regeneration after rotation;
- serialized pending NativeTablePdfData during Save-document handoff.

Full audit matrix:
`docs/v.0.5.22/LIFECYCLE_AUDIT.md`

Do not close BUG-009 until the complete phone rotation audit passes.

## UX-013 — Native compact technical columns must fit measured content

Status: **PHONE PASS on tested 108 screen in v0.5.22**  
Found: 2026-09-25  
Source: real-phone v0.5.21 screenshot

### Observed

The v0.5.21 native 108 table contains all expected fields and a semantic header, but compact columns remain narrower than their real text:
- upper source-info column clips `F5R700`;
- pin table can clip `2.0`, `2.5`, `3CV`, `3N`;
- percentage/fraction heuristics do not reliably represent rendered Android text width.

### v0.5.22 fix

For native tables:
- measure compact column text with Android Paint in pixels;
- include header text and bold font in measurement;
- assign fixed measured width to every column before the final column;
- let final description column consume the remainder;
- make 2-column abbreviations/designations bold;
- use high-contrast bold headers.

For generic 2-column generated PDFs:
- first-column abbreviations/designations are also bold.


## UX-014 — Focused native landscape should show only the active top-level mode

Status: **IMPLEMENTED IN v0.5.23 · CI / PHONE TEST PENDING**  
Found: 2026-09-25  
Source: real-phone v0.5.22 landscape screenshot

### Required

When a native section is inside a selected top-level mode and the phone rotates to landscape:
- hide Back/Home/Search/Settings;
- hide section header + Modern/Classic switch;
- hide all other top-level mode buttons;
- keep only the selected top-level button full-width at the top;
- keep the same child content open.

The rule applies to:
- Схеми;
- Розʼєм;
- Положення на авто;
- Документація.

Returning to portrait restores the full chrome and all top-level buttons.

v0.5.23 persists the active top-level menu label/action together with the existing child view state.

## UX-015 — Pines PDF hierarchy: bold technical columns and larger headers

Status: **IMPLEMENTED IN v0.5.23 · CI / PHONE TEST PENDING**  
Found: 2026-09-25  
Source: real-phone v0.5.22 exported 108_1(pines).pdf

### Required

For pines PDF:
- body/description text stays 10 pt regular;
- columns 1-3 use 10 pt bold;
- table header grows by 4 pt: 10 → 14 pt bold;
- upper connector-info table uses the same 14 pt bold size as the pin-table header;
- existing title/identity sizes remain unchanged;
- existing icons/margins/composition remain unchanged.

Renderer font family remains Android `Typeface.SANS_SERIF`.


## UX-016 — Built-in PDF viewer raster quality and rotation re-render latency

Status: **ACCEPTED IN v0.5.25 · KNOWN HIGH-ZOOM LIMITATION**  
Found: 2026-09-25  
Source: real-phone observation after v0.5.23

### Observed

In the built-in PDF viewer:
- zoomed PDF pages can look softer than the same original PDF in a dedicated external PDF viewer;
- rotating the phone can make the PDF take noticeable time to appear again.

### Current renderer behavior

`AndroidPdfLayer` does not display the PDF page as live vector content. It:
- opens the PDF through Android `PdfRenderer`;
- renders each page to an `ARGB_8888` bitmap;
- compresses the rendered bitmap to JPEG at quality 92;
- serves that JPEG to the WebView;
- caps render width at 1800 px;
- caps effective DPR contribution at 2;
- renders nearby pages around the current page;
- serializes PDF page renders through one render lock;
- stores page JPEGs in a per-`AndroidPdfLayer` 16 MiB LRU cache.

On Activity recreation after rotation, `ViewerActivity` restores WebView state, but `SafDatasetWebViewClient` and its `AndroidPdfLayer` instance are recreated, so the in-memory rendered-page cache is not retained.

### Consequences

Quality:
- vector PDF text/lines become raster pixels before display;
- JPEG compression can soften fine text/line edges;
- zooming beyond the rendered bitmap's effective resolution enlarges pixels instead of re-vectorizing them;
- the 1800 px render cap and DPR cap can become visible on high-density screens.

Rotation latency:
- Activity/WebView/client recreation loses the per-instance PDF image cache;
- visible/nearby pages may be rendered again;
- rendering is CPU/memory intensive and currently serialized;
- JPEG encoding adds another cost after `PdfRenderer`.

### Important distinction

The exported PDF file itself can still contain vector text/lines. This finding concerns the app's built-in PDF viewing pipeline, not necessarily the intrinsic quality of the saved PDF.

### Candidate follow-up

Treat quality and rotation speed together:
- keep a higher-quality/adaptive render target for zoom;
- avoid JPEG for technical line/text pages where practical, or use a higher-fidelity strategy;
- retain/reuse rendered page cache across Activity recreation;
- prioritize current visible page after rotation before pre-rendering neighbors;
- avoid unnecessary re-render when only layout/orientation changes;
- measure memory before raising the maximum render width.


### BUG-009 phone progress — 2026-09-25

Real-phone follow-up reports the currently exercised NativeSection rotation path as PASS. Child content no longer drops back to the parent screen in the tested flow.

BUG-009 remains open until the rest of the lifecycle matrix is completed, especially Settings choosers, Viewer dialogs/search, PDF rotation, and save/export handoffs.


### BUG-009 Settings gate — PASS (2026-09-25)

Real-phone result:
- Modern/Classic default-mode chooser survives rotation;
- PDF default zoom chooser survives rotation;
- PDF zoom-step chooser survives rotation.

Settings portion of the lifecycle audit is now PASS.


### BUG-009 Viewer navigator/search gate — PASS (2026-09-25)

Real-phone result:
- Viewer `Розділи` survives rotation;
- its search query/filter survives rotation;
- no parent-screen fallback observed.

Remaining lifecycle gates still include Frame debug, current PDF rotation, and save/export/system-picker handoffs.


### BUG-009 final phone closeout — PASS (2026-09-25)

Final reachable gates:
- current PDF rotation — PASS;
- table-PDF save/system-picker handoff — PASS.

Together with the earlier NativeSection, Settings, and Viewer navigator/search passes, the reported child-window-on-rotation bug is closed for the current reachable UI.

Frame debug remains N/A because the current normal navigation does not expose hybrid Viewer DBG mode.


### UX-016 v0.5.24 implementation

Implemented:
- lossless PNG page encoding replaces JPEG quality 92;
- DPR cap raised to 2.5;
- max render width raised to 2400 px;
- width variants grouped into 200 px buckets;
- 32 MiB process-level shared compressed-page cache;
- cache key namespaced by dataset tree URI + PDF path + page + width;
- current visible page requested first;
- neighbor prefetch delayed by 60 ms;
- previous bitmap kept visible during resolution replacement;
- lazy root margin reduced from 1200 px to 700 px;
- resize/orientation resolution upgrade debounced by 120 ms.

The viewer remains raster-based; v0.5.24 improves the raster path rather than replacing it with a native vector PDF engine.

Do not close UX-016 until phone comparison covers zoom quality, first/second rotation latency and memory stability.


### UX-016 v0.5.24 phone result — 2026-09-25

Real-phone result:
- quality: `++`;
- first rotation: `+`;
- second/repeated rotation: `++`;
- 100% multi-page scroll: acceptable;
- 200% multi-page scroll on a document around 20 pages: not smooth enough.

Interpretation:
- lossless/higher-resolution path is a clear quality PASS;
- shared rotation cache is a PASS and improves repeat rotation;
- the remaining problem is decoded-image/render pressure during long high-zoom continuous scroll.

v0.5.25 addresses that remaining scroll issue without reducing the v0.5.24 quality target.

## UX-017 — PDF controls must be independent from the scrollable/zoomed document

Status: **ACCEPTED IN v0.5.25**  
Found: 2026-09-25  
Source: real-phone v0.5.24 feedback

Required:
- PDF control menu must not move/hide when document content is vertically scrolled or horizontally panned at zoom;
- portrait controls use two rows;
- landscape controls use one row;
- document scrolling occurs in a separate viewport below the controls;
- rotation must retain the current PDF reading state.

v0.5.25 implementation:
- fixed control area + independent `#pdfViewport`;
- portrait: two explicit toolbar rows;
- landscape: one horizontal toolbar row;
- high zoom >=150% prefetch/decoded-page radius reduced;
- far decoded page images evicted from DOM;
- current page/zoom/nested scroll state persisted for 5 minutes per dataset+PDF path.


### v0.5.25 merged / CI PASS

Source:
`6e962b5bac1bdee93ba232f55ee594f318ab2f8b`

CI:
- Tests `36167218686` — PASS;
- Android Debug `36167218706` — PASS.

UX-016/UX-017 remain open only for the v0.5.25 real-phone scroll/control validation.


### UX-016 / UX-017 phone closeout — 2026-09-25

User accepted the v0.5.25 PDF viewer as good enough to move on.

Accepted state:
- v0.5.24/v0.5.25 quality improvement is kept;
- rotation cache improvement is kept;
- PDF controls are separated from document scrolling;
- portrait/landscape responsive toolbar behavior is kept;
- 200% long-document scrolling is improved but remains a known performance ceiling of the current raster/WebView architecture.

Do not spend another release on this unless a concrete new regression appears or the viewer architecture is replaced.


### BUG-004 v0.5.26 implementation

Implemented converter/runtime contract:
- section identifiers are opaque Renault IDs rather than exactly three digits;
- representative accepted families include `1405`, `R325`, `MAH`, `NT`, `NU`;
- Classic source order is preserved end-to-end;
- unresolved/missing local targets are rejected;
- dedup identity is `code + resolved entrypoint`, not code alone;
- duplicate display codes with different targets are retained;
- Runtime IR shard index adds ordered `section_entries`;
- Android resolves duplicate display codes by code + legacy entrypoint;
- old code→path shard map remains a compatibility fallback.

`modern-sections.json` schema is now 2.

Dataset regeneration with Renault Menu → 9 is required before phone testing.


### BUG-004 v0.5.26 phone closeout — 2026-09-25

Status: **PHONE PASS (REPRESENTATIVE) · CLOSED**

After the required v0.5.26 package regeneration, the user reported that the Classic↔Modern parity check looked normal.

Accepted phone evidence:
- no problem was reported for representative non-3-digit identifiers;
- source-order comparison did not expose a mismatch;
- ordinary 101/108 regression behavior remained acceptable.

The duplicate-display-code real-phone gate was not explicitly demonstrated with a convenient visible duplicate. The automated duplicate-routing contract remains the evidence for that collision case.

BUG-004 is closed for the current Laguna II dataset contract.


## UX-018 — Live PDF pinch zoom with fixed toolbar and deferred quality render

Status: **CI PASS IN v0.5.27 · PHONE TEST PENDING**
Found/requested: 2026-09-25

User requested a concrete extension to the accepted v0.5.25 viewer:
- pinch should resize the document immediately;
- the visible zoom percentage must update during the gesture;
- the PDF toolbar must stay at fixed UI scale;
- expensive high-resolution rerendering should happen only after zoom motion settles.

v0.5.27 implementation:
- browser/WebView page zoom is disabled on the generated PDF viewer page;
- two-finger pinch is handled inside `#pdfViewport` only;
- CSS page width changes immediately while the existing bitmap stays visible;
- `zoomInput` updates live in integer percent;
- pinch anchors to the touched page region using page-relative X/Y ratios;
- quality rerender is debounced by ~180 ms;
- current page is rendered first, then existing neighbor policy applies;
- lazy/scroll/prefetch render work is suppressed or cancelled during active pinch;
- portrait two-row and landscape one-row toolbar contracts remain unchanged;
- zoom remains 50–200%.

This is APK-only. Renault Menu point 9 is not required.


### UX-018 v0.5.27 merged / CI PASS

Merged main source:
`7660416ad3f00a7fdf3d7556b6fd11a0bfd1186f`.

Green tested feature source:
`46ba0c0df464d144158cd5a696611f2366198d75`.

The runtime code blobs used by the artifact were verified identical to merged main for:
- `AndroidPdfLayer.kt`;
- `android/app/build.gradle.kts`.

CI:
- Tests `36176557305` — PASS;
- Android Debug `36176557288` — PASS;
- artifact `Renault-Docs-v0.5.27-Debug`;
- artifact id `10882358464`;
- APK SHA-256 `03dca9deb5fda72e4fa5d72eae18f94285c451e04bd3b78157bf97fd19109ac8`;
- ZIP SHA-256 `b0f79a703f60fd1255fb90a9724bce8e39196bbcf694579c83a0e2416e5406ae`.

Status: **PHONE PASS · CLOSED**.


## UX-019 — PDF zoom to 400% + explicit fullscreen

Status: **CI PASS IN v0.5.28 · PHONE TEST PENDING**
Requested: 2026-09-25

User accepted v0.5.27 pinch behavior and requested:
- upper zoom limit 400%;
- app/global chrome not to consume landscape document space;
- explicit entry/exit control for fullscreen.

v0.5.28:
- extends shared zoom state to 50–400%;
- adds 300%/400% quick presets;
- increases full render cap to 3000 px;
- >=250% keeps decoded current page only;
- keeps active-scroll tier <=1600 px;
- adds PDF-toolbar fullscreen toggle routed to ViewerActivity;
- fullscreen hides app toolbar + Android status/navigation bars while retaining PDF toolbar;
- Android Back exits fullscreen first;
- fullscreen state survives recreation.

APK-only; point 9 is not required.


## ARCH-007 — Hoist common documentation from section IR to volume IR

Status: **IMPLEMENTED IN v0.5.29 · CI/PHONE TEST PENDING**

Observed contract:
- Modern groups `GENE / PLATFUSI / AIDE` under `Документація`;
- these routes are currently compiled into each section shard;
- in Renault volumes where the documentation set is identical across sections, the semantic owner is the volume rather than the section.

v0.5.29 implementation:
- converter detects the documentation route-set independently inside each volume;
- resolved GENE / PLATFUSI / AIDE signatures must agree before hoisting;
- conflicting targets disable hoisting for that volume;
- one volume-level documentation shard is emitted under runtime-ir/documentation/;
- runtime index exposes documentation_path for that volume only;
- Android loads the shard lazily on first Documentation tap and caches it in the current Activity;
- documentation IDs are remapped to a vdoc-* namespace to avoid collisions with section-local IDs;
- section-local documentation remains as compatibility fallback during this migration wave.

Important: Fast Pack already stores the underlying physical HTML/PDF resources only once, so the expected gain is smaller Runtime IR, less repeated JSON parsing/routing, and cleaner/stabler navigation rather than large file-size savings.

This changes converter/package data and therefore v0.5.29 requires point 9.

Hard boundary: documentation is NEVER deduplicated across different Renault volumes/configurations. Each volume owns its own files and its own documentation shard.


### UX-019 v0.5.28 merged / CI PASS

Merged main source:
`1104db917f5964db7efb41a9282eb88ccde733ee`.

Green tested feature source:
`330c4d7168712236453d76fe8fef92e17710b35c`.

Runtime implementation/version blobs were verified identical between tested feature source and merged main for AndroidPdfLayer, SafDatasetWebViewClient, ViewerActivity and build.gradle.kts.

CI:
- Tests `36179103365` — PASS;
- Android Debug `36179103296` — PASS;
- artifact `Renault-Docs-v0.5.28-Debug`;
- artifact id `10884005657`;
- APK SHA-256 `615fe8bb939b41a92ac999472bad0015c9a5e2b7dfaf204cb344bba778479fd2`;
- ZIP SHA-256 `9016bd4ef4b3dc03d28a6b50e9037f4eb89731e875db186b69c1ee8c052b780d`.

Status: **CI PASS · PHONE TEST PENDING**.


### UX-019 v0.5.28 phone closeout — 2026-09-25

Status: **PHONE PASS WITH UX FOLLOW-UP · CLOSED**

User confirmed 400% zoom/fullscreen works better and accepted continuation.
Remaining visual issue: fullscreen button did not indicate whether the mode was active.

v0.5.29 fixes that with synchronized `aria-pressed` + accent/pressed styling.


## BUG-005 — Blank PDF pages while scrolling at 250–400%

Status: **PHONE PASS IN v0.5.30 · CLOSED**
Found: 2026-09-25

Real-phone symptom:
- at 400% a newly visible page could remain as a blank/loading placeholder;
- lowering zoom to around/below 200% made it render;
- counter could remain on a stale page (example: 13/13 while page 11 was visible).

Root cause:
- current page used IntersectionObserver thresholds starting at 25%;
- a very tall 400% page can never occupy 25% of the viewport-relative page area required for that callback;
- high-zoom retain radius 0 then evicted the actually visible page while repeatedly loading the stale current page.

v0.5.30:
- tracks current page from the center of the nested PDF viewport during scroll;
- keeps ±1 page retained/preloaded;
- high-zoom neighbors use lightweight <=1200 px renders;
- current page uses <=1600 px during active scroll and upgrades to <=3000 px after idle.


## ARCH-008 — Dedicated volume documentation screen

Status: **PHONE PASS IN v0.5.31 · CLOSED**

v0.5.29 correctly moved documentation DATA ownership to the volume, but the UI is still hosted inside NativeSectionActivity.

Recommended next refactor:
- add one dedicated `VolumeDocumentationActivity`;
- expose `Документація` directly on ModernVolumeActivity;
- section-level `Документація` becomes only a shortcut to that same volume-owned activity;
- activity receives volume identity/context, never section identity;
- load exactly that volume's `documentation_path`;
- preserve nested documentation navigation/rotation independently from section state;
- after phone parity, remove verified duplicate documentation menu/actions/documents from section shards.

Hard boundary: different Renault volumes/configurations always keep separate documentation shards/files.


### BUG-005 v0.5.30 merged / CI PASS

Merged main source:
`2a342aa505378b651baaeef9fe44b555e6836896`.

Green tested source:
`89160047ff9a4208b547e00bd0982b22b91a45df`.

CI:
- Tests `36186156147` — PASS;
- Android Debug `36186156169` — PASS;
- artifact `Renault-Docs-v0.5.30-Debug`;
- artifact id `10885104783`;
- APK SHA-256 `06a3a032361ba18f6f925949b1eb959d9fa69320aba51d7c974cd177426f62ab`.

Status: **CI PASS · PHONE TEST PENDING**.


## UX-020 — PDF Companion documentation pane / cross-reference workspace

Status: **PHONE CORE PASS IN v0.5.32 · UX FOLLOW-UP IN v0.5.33**
Requested: 2026-09-26

User clarified why documentation must remain quickly accessible while reading wiring diagrams:
- schematic PDFs contain block/component/fuse identifiers that are not practical to know exhaustively;
- the user needs to look those identifiers up manually in the SAME volume's documentation without closing or losing the schematic context.

Important consequence for ARCH-008:
- removing duplicated `GENE / PLATFUSI / AIDE` graph data from section shards must never remove documentation access;
- documentation remains volume-owned and must remain reachable from section and PDF contexts.

Proposed PDF Companion workflow:
- add a new control near the PDF fullscreen control;
- keep the current schematic PDF open as the main frame;
- open a second documentation frame below it;
- second frame resolves only the current volume's documentation shard/entrypoint;
- allow lookup of fuses, electrical equipment/block lists, connections, abbreviations, subject index and other volume-owned reference material;
- main PDF page/zoom/scroll must remain unchanged;
- companion navigation/scroll/PDF state must be independent;
- fullscreen should preserve the split workspace;
- rotation should restore both panes and their states.

Performance note:
- treat this as one shared two-pane render budget;
- main schematic remains primary quality target;
- companion PDF should retain only conservative visible-page state;
- do not run two unrestricted high-resolution preload policies.

This is a manual cross-reference feature first. Automatic block-number recognition/linking is explicitly NOT required for the first implementation.

Detailed design:
`docs/design/PDF_COMPANION_DOCUMENTATION.md`.


### BUG-005 v0.5.30 phone closeout — 2026-09-26

Status: **PHONE PASS · CLOSED**

User confirmed that PDF files now scroll/load correctly at 400% without lowering zoom.

Terminology clarification for documentation architecture:
- canonical ownership scope is the Renault `volume` / «том»;
- one volume owns one documentation set;
- all sections inside that volume reuse the same `Загальна документація / Запобіжники / Довідка` set;
- do not label the architecture boundary as only «комплектація», «рік» or «модифікація», because those may be attributes of a volume rather than the stable owner identity.


### ARCH-008 v0.5.31 implementation

Implemented UI ownership matching the v0.5.29 data model:
- new `VolumeDocumentationActivity`;
- direct `Документація` entry on `ModernVolumeActivity`;
- section-level Documentation button routes to the same volume Activity when `documentation_path` exists;
- dedicated Activity resolves documentation directly from Runtime IR index by volume entrypoint;
- no section code/title/entrypoint is required by the dedicated screen;
- nested documentation panel stack survives Activity recreation;
- final HTML/PDF targets reuse `ViewerActivity` with current volume context;
- section-local documentation renderer remains compatibility fallback.

No section graph deletion yet. Remove duplicate section-local documentation metadata only after phone parity.


### ARCH-008 v0.5.31 merged / CI PASS

Merged main source:
`6fae1c82b37e12a10938e428383e08aff3d8135c`.

Green tested source:
`b73d0a4893318233e2b222711a4bbe82740dd5fb`.

CI:
- Tests `36193148657` — PASS;
- Android Debug `36193148624` — PASS;
- artifact `Renault-Docs-v0.5.31-Debug`;
- artifact id `10888463798`;
- APK SHA-256 `c1318bf7fff75e793f0f8270aa1a4f7587deb1b7b2c12f57410160b839779cd5`.

Status: **CI PASS · PHONE TEST PENDING**.


### ARCH-008 v0.5.31 phone evidence — 2026-09-26

Status: **PARTIAL PHONE PASS**

After Renault Menu → 9 regenerated the package, direct volume Documentation for:
`NT8236A · 2002-11-18`
opened successfully.

Observed root menu:
- Загальна документація;
- Запобіжники;
- Довідка.

This confirms:
- Runtime IR index now exposes this volume's `documentation_path`;
- VolumeDocumentationActivity resolves the current volume correctly;
- direct volume-level Documentation entry works after package refresh.

Remaining phone gates before closeout:
- open child items under all three root categories;
- verify section 103/105 Documentation opens the same dedicated screen;
- verify rotation/back hierarchy;
- verify another volume remains isolated.


### ARCH-008 v0.5.31 phone closeout — 2026-09-26

Status: **PHONE PASS · CLOSED**

User confirmed the remaining v0.5.31 gates:
- child documentation navigation;
- section shortcut parity;
- another-volume isolation;
- rotation/back hierarchy.

The dedicated volume documentation screen is now the accepted canonical documentation UI owner.


### UX-020 v0.5.32 implementation / CI PASS

Implemented the first PDF Companion split workspace:
- new Companion control beside fullscreen in the main PDF toolbar;
- main schematic WebView remains alive and keeps its state;
- lower companion pane uses the same volume documentation identity;
- nested documentation navigation stays in the lower pane;
- final HTML/PDF targets open in the lower pane;
- main and companion PDF state scopes are independent;
- companion PDF uses a compact render budget;
- companion visibility and WebView history survive Activity recreation;
- fullscreen keeps both panes visible;
- companion does not expose a recursive Companion button.

Merged main source:
`41dcede6ebea2f2220ba23e363e15be6c5ea79c8`.

CI:
- Tests `36201064184` — PASS;
- Android Debug `36201064135` — PASS;
- artifact `Renault-Docs-v0.5.32-Debug`;
- artifact id `10891713605`;
- APK SHA-256 `574c7184dda4267f84d688b8e748fa0aea18b65c2e0ee5078646db3acb4880f6`.

Status: **CI PASS · PHONE TEST PENDING**.


### UX-021 — Split fullscreen chrome + adjustable divider

Status: **CI PASS IN v0.5.33 · PHONE TEST PENDING**

Real-phone v0.5.32 screenshots showed that dual-document fullscreen works but permanent controls consume too much document area, especially in landscape.

v0.5.33 follow-up:
- fullscreen + Companion enters document-first focus mode;
- PDF toolbars become overlay toolbars and are hidden by default;
- companion Android header is hidden in focus mode;
- double tap either pane reveals controls temporarily;
- controls auto-hide after ~3.2s;
- floating Android Back/Close/fullscreen actions appear with temporary controls;
- 12dp draggable divider changes main/companion share from 20–80%;
- default ratio 55/45;
- split ratio and transient control visibility persist through recreation;
- fullscreen and both WebView/PDF states continue to persist.

Landscape intentionally remains vertical split for this wave. Side-by-side stays a later option after phone validation.


### UX-021 v0.5.33 merged / CI PASS

Merged main source:
`250b2a26a799bc88b117b82d26005f98550681fd`.

Green tested source:
`031d5b75a608fcd5a6dc7c427311f5b6476b8330`.

CI:
- Tests `36203053082` — PASS;
- Android Debug `36203052992` — PASS;
- artifact `Renault-Docs-v0.5.33-Debug`;
- artifact id `10893250571`;
- APK SHA-256 `44ff64b86907148ea589f3415679944859e283cf92a78baa029bf7ec3baaf5d2`.

Status: **CI PASS · PHONE TEST PENDING**.


## BUG-006 — Fullscreen state desync after Companion close + rotation

Status: **v0.5.34 INCOMPLETE · SUPERSEDED BY v0.5.35 PHONE TEST PENDING**

Real-phone reproduction from v0.5.33:
1. open main PDF;
2. open Companion;
3. enter fullscreen split mode;
4. close Companion;
5. rotate to portrait.

Observed:
- app/system chrome may remain in fullscreen presentation;
- PDF fullscreen button may render inactive;
- main app toolbar remains hidden;
- user needs an extra fullscreen toggle to restore a consistent UI.

Root cause:
- Android `pdfFullscreen` state and PDF JavaScript button state were synchronized at different lifecycle times;
- fullscreen was applied before restored WebView content finished replacing the page;
- Companion close did not force a complete single-document fullscreen reconciliation.

v0.5.34 fix:
- Android `pdfFullscreen` is the sole source of truth;
- reconciliation runs after main WebView restore/load setup;
- a posted reconciliation runs after the WebView event queue;
- WebChrome title callbacks reconcile once PDF JS exists;
- window focus reconciles both true and false states;
- closing Companion reconciles the remaining main viewer without toggling fullscreen.

Required invariant:
- fullscreen active => button active + app toolbar hidden;
- fullscreen inactive => button inactive + app toolbar visible;
- no mixed state.


### BUG-006 v0.5.34 merged / CI PASS

Merged main source:
`adb226f605c3730037e945fe0e6ce95275981710`.

CI:
- Tests `36204766057` — PASS;
- Android Debug `36204766049` — PASS;
- artifact `Renault-Docs-v0.5.34-Debug`;
- artifact id `10893043078`;
- APK SHA-256 `f45ebd66dd5d4c292f4808f63f0aac446d833798205db16d97f36fe5fe23896f`.

Status: **CI PASS · PHONE TEST PENDING**.


### BUG-006 clarification from v0.5.34 phone test

The remaining failure is NOT Companion-specific.

User confirmed fullscreen is also lost after rotation with a single PDF.

Therefore the actual root condition is ViewerActivity orientation recreation, not only Companion-close reconciliation.

### BUG-006 v0.5.35 structural fix / CI PASS

ViewerActivity now handles `orientation|screenSize` itself and is not recreated just because the device rotates.

On configuration change:
- same main WebView survives;
- same Companion WebView survives;
- `pdfFullscreen` remains live in the same Activity;
- split ratio is reapplied;
- fullscreen presentation is reconciled immediately;
- reconciliation runs again after layout and after 180 ms to counter system-bar reappearance.

This path covers both:
- one-document fullscreen;
- two-document fullscreen.

Merged main:
`1eb744ce3959d1a56cb2d9059b5069dd1b207b9c`.

CI:
- Tests `36206184286` — PASS;
- Android Debug `36206184390` — PASS;
- artifact `Renault-Docs-v0.5.35-Debug`;
- artifact id `10894415207`;
- APK SHA-256 `d565d8e0f0c9f4d1787f2ee1a3397e15502fd5766081b5ace0681ebeaa80b0ed`.

Status: **CI PASS · PHONE TEST PENDING**.


### BUG-006 v0.5.35 phone video result

Video `225102.mp4` confirmed:
- Activity-level fullscreen remains active after rotation (main app toolbar stays hidden);
- PDF fullscreen button can still fall back to inactive visual state;
- one tap then exits fullscreen normally.

This isolates the remaining defect to Android→WebView fullscreen-control synchronization.

### BUG-006 v0.5.36 acknowledgement fix / CI PASS

v0.5.36 changes:
- PDF JS setter returns acknowledgement that `aria-pressed` actually matches requested state;
- PDF remembers native expected fullscreen state and reapplies it on resize/pageshow/visibilitychange;
- ViewerActivity retries Android→WebView sync until acknowledgement or retry limit;
- stale retries are guarded by generation id and expected state;
- v0.5.35 no-recreation rotation handling remains enabled.

Merged main:
`3b6580d5a2b8d2a012a978cd828c220035dbef79`.

CI:
- Tests `36206923878` — PASS;
- Android Debug `36206923894` — PASS;
- artifact `Renault-Docs-v0.5.36-Debug`;
- artifact id `10894740860`;
- APK SHA-256 `cf89e1273d550fb1c052c9d3c5a419fed5dde17609deada8ff5a81e413d46ec9`.

Status: **CI PASS · PHONE TEST PENDING**.


### BUG-006 v0.5.36 phone closeout — 2026-09-26

Status: **PHONE PASS · CLOSED**

User confirmed the acknowledgement/retry synchronization works on phone:
- fullscreen survives rotation;
- fullscreen button remains visually active when Android fullscreen is active;
- one tap exits fullscreen normally;
- behavior is correct for one and two documents.


## ARCH-009 — Converter 2.0 in-app execution

Status: **WAVE 1 CI PASS IN v0.5.37 · PHONE TEST PENDING**

Long-standing product target:
the end user must be able to select an original Renault documentation folder and produce a usable Android dataset without Termux/Python.

v0.5.37 implements the first real execution layer:
- foreground data-sync conversion service;
- source/destination SAF permissions;
- persistent run state;
- recursive source scan;
- full exact-path map before patching;
- staged recursive copy;
- existing Kotlin case/path normalizer applied to HTM/HTML/JS;
- base manifest/catalog/volume package;
- output validation;
- staging -> final rename only after validation;
- cancel cleanup;
- completed output can be registered into Library.

Safety:
- source is read-only;
- source is never deleted in v0.5.37;
- existing final output is never overwritten;
- destination inside source is rejected;
- failed/cancelled pre-finalization staging is removed.

Important boundary:
v0.5.37 does NOT yet port the Python Modern package compiler. The resulting output is normalized/Classic-ready with a volume-level modern-index only.

Remaining Converter 2.0 waves:
1. Android Modern package compiler parity:
   - full Classic catalog discovery;
   - modern-sections;
   - Runtime IR v2 section shards/index;
   - per-volume documentation shards;
   - coverage;
   - Fast Pack.
2. output parity validation against the Python/package oracle.
3. verified original-source backup:
   - ZIP;
   - metadata;
   - SHA-256;
   - archive read/verification.
4. explicit final source deletion confirmation only after verified backup.
5. Backup Center restore/verify/delete flows.

Critical invariant remains:
source deletion is impossible when conversion, package compilation, validation, backup, verification or final confirmation has not succeeded.


### ARCH-009 v0.5.37 merged / CI PASS

Merged main:
`51c2d77771a7c5676e5793ee38de2f180eaf7bb5`.

Green tested source:
`de28d445749c752d5d0d1ecbf31959cab92b8d3f`.

CI:
- Tests `36209095449` — PASS;
- Android Debug `36209095432` — PASS;
- artifact `Renault-Docs-v0.5.37-Debug`;
- artifact id `10894143612`;
- APK SHA-256 `ac7b0e6dfe0227c86b536f1811249690e8e6fe625f5364354c9ce81ccc590e6e`.

Next gate: real-phone staged conversion/cancel/output validation. Full Modern compiler parity is still the next converter wave, not claimed by v0.5.37.


## PERF-003 — Converter source scan too slow on SAF

Status: **FIX IMPLEMENTED IN v0.5.38 · CI/PHONE TEST PENDING**

Found on real phone while converting Megane II.

v0.5.37 source discovery used recursive `DocumentFile.listFiles()` and per-file metadata access. On Android's external-storage DocumentsProvider this causes many provider calls and makes large Renault trees noticeably slow to scan.

v0.5.38:
- primary scanner uses `DocumentsContract.buildChildDocumentsUriUsingTree()` + one `ContentResolver.query()` per directory;
- obtains document id / display name / MIME / size from the cursor;
- avoids per-file DocumentFile metadata calls;
- keeps full exact-path map before normalization;
- retains DocumentFile scanner as compatibility fallback for providers that do not support the fast query path.


## BUG-007 — Converted Renault images appear as Gallery albums

Status: **FIX IMPLEMENTED IN v0.5.38 · CI/PHONE TEST PENDING**

Real-phone v0.5.37 output caused Android Gallery to expose documentation asset folders as albums, including PM, PC, ICONES and DRAPEAUX.

Root cause:
the converted dataset lives under normal shared storage and had no `.nomedia` marker.

v0.5.38:
- creates `.nomedia` in staging before source media files are copied;
- marker survives staging -> final rename;
- registering an existing converted output also ensures `.nomedia` exists.

Note:
already indexed Gallery entries from an older output can remain cached until Android/Gallery rescans or the old output is removed. The fix prevents new converter output from being treated as user photo albums.


## UX-022 — Raw SAF URI shown in converter/settings folder UI

Status: **FIX IMPLEMENTED IN v0.5.38 · CI/PHONE TEST PENDING**

v0.5.37 converter cards showed values such as:
`content://com.android.externalstorage.documents/tree/primary%3ADocuments%2FRenault...`

v0.5.38 introduces shared `SafDisplayPath`:
- primary storage -> readable path such as `Documents/Renault/Megane II`;
- secondary storage -> `<storage-id>/<path>`;
- raw content URI is no longer shown when a friendly tree path can be resolved;
- Backup Settings and Converter use the same formatter.

Scan progress UX also changes:
- SCANNING is indeterminate;
- UI says `Знайдено файлів: N` instead of misleading `0 / N`.


## BUG-008 — Termux point 8 can download an older APK after merge

Status: **FIX IMPLEMENTED IN v0.5.38 · CI/PHONE TEST PENDING**

Observed:
user expected v0.5.36 but Renault Menu point 8 downloaded v0.5.35.

Root cause:
`reno-download-apk.sh` selected the latest successful Android workflow run for the CURRENT Git branch. After `5` the repo was on `main`, while the exact release APK had been built on its feature branch before merge. The newest successful main build could therefore be older.

v0.5.38 fix:
- current code version is parsed from Gradle;
- exact artifact `Renault-Docs-v<version>-Debug` is looked up through GitHub Actions artifact API regardless of branch;
- only that exact artifact is downloaded;
- old-version fallback is forbidden;
- missing exact artifact produces an explicit error instead.

Required invariant:
Menu 8 must either deliver the APK matching current code `versionName` or fail. It must never silently install an older version.


## DATA-001 — Laguna prepared dataset volume parity mismatch

Status: **OPEN · DIAGNOSIS IN PROGRESS**

Real-phone dataset link checker baseline on 2026-09-30:

- active build root: `laguna 2 2001-2006_android`;
- scanned HTML/CSS files: `14417`;
- checked local references: `77449`;
- missing local targets: `14`;
- all 14 missing targets come from root `INDEX.HTM`;
- the 14 entries form exactly 7 pairs: missing top-level volume `INDEX.HTM` plus missing `FLAG-RUS.GIF` beneath the same top-level volume folder;
- current Runtime IR coverage documents only 10 Laguna II volumes / 2174 sections;
- original archive audit historically recorded 61759 files, while the current build root audit records 19836 files.

Affected stale root entries:
- NT8183A · 2001-01-22;
- NT8218A · 2002-05-01;
- NT8240A · 2003-11-17;
- NT8254A · 2004-06-21;
- NT8307A · 2005-12-12;
- NT8327A · 2006-02-06;
- NT8328A · 2006-05-09.

The root `INDEX.HTM` is still a real Classic/browser entrypoint, so these are not automatically harmless warnings.

Next diagnostic gate:
compare `source_root` and `build_root` top-level discovered volumes. If the 7 folders exist in source but not build, prepared Laguna is incomplete and must be repaired/rebuilt. If they are absent from both, root catalog is stale and should be regenerated/pruned from actual volume inventory.
