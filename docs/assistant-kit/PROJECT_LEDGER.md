# Renault Docs — Master Project Ledger

Last consolidated: 2026-09-25

This file is the canonical human-readable project ledger.

Its purpose is to prevent architectural decisions, phone findings, completed work, open bugs, QA gates, and future work from being lost between sessions.

Use together with:
- `CURRENT_HANDOFF.md` — chronological handoff/history;
- `docs/assistant-kit/OPEN_FINDINGS.md` — active and recently resolved findings;
- per-release `docs/v.X.Y.Z/` documentation;
- `docs/assistant-kit/SESSION_CHECKPOINT_2026-09-24.md` — detailed implementation/session chronology.

## 1. Product direction

Renault Docs has two intentionally separate modes.

### Classic

Classic is the preserved legacy Renault documentation runtime.

It remains available as a fidelity/reference fallback and must not be destroyed while Modern is being developed.

Classic may continue to use:
- legacy `INDEX.HTM`;
- `ENTREE.HTM`;
- legacy framesets;
- `CODE.HTM`;
- `MENU/<code>.HTM`;
- `nav` / PC pages;
- legacy PDF/HTML documents;
- original cross-frame JavaScript.

Classic is not the UI target for the new application. It is the compatibility reference.

### Modern

Modern is app-owned navigation and presentation built from data extracted during conversion.

Target architecture:

```text
source Renault documentation
        ↓
converter / package generator
        ↓
Classic package (preserved)
        +
Modern data model / Runtime IR
        ↓
native Android navigation/renderers
        ↓
PDF / diagrams / structured tables / documents
```

Modern must gradually stop depending on the legacy `INDEX → ENTREE → CODE` frameset for navigation.

The converter is allowed to do expensive one-time analysis so phone runtime stays fast.

## 2. Conversion/data architecture

Current generated package includes Classic content plus Modern indexes/Runtime IR.

Important generated files include:
- `_renault/START.html`;
- `_renault/README_UA.html`;
- `_renault/volumes.json`;
- `_renault/modern-index.json`;
- `_renault/modern-sections.json`;
- `_renault/runtime-tree.json`;
- `_renault/runtime-ir-index.json`;
- per-section Runtime IR shards under `_renault/runtime-ir/sections/...`;
- Fast Pack archives/cache metadata.

### Runtime IR memory rule

v0.5.8 proved that Android must never load the complete `runtime-tree.json` into memory for normal navigation.

The full file remains compiler/debug data.

Runtime reads use:
- small Runtime IR index;
- one selected section shard.

This was fixed in v0.5.9 after a real phone OOM attempting a ~268 MB allocation.

## 3. Known legacy runtime architecture

For the tested Laguna II NT8183A volume, Classic boot/runtime is approximately:

```text
INDEX.HTM
   ↓
ENTREE.HTM
   ↓
outer frameset
   ├─ titre → CTITRE.HTM
   ├─ org   → CODE.HTM
   ├─ menu  → RUS/HTM/MENU/<section>.HTM
   ├─ nav   → optional navigation/PC page
   └─ doc   → PDF/HTML/document
```

Semantics discovered on phone:
- `org/CODE.HTM` is the authoritative legacy section/catalog navigation;
- selecting an `org` item changes `menu`;
- `menu` may then populate `nav` and/or `doc`;
- `nav` and `doc` may legitimately stay blank until a menu action is selected;
- frame element `src` is not authoritative after navigation; actual frame document URL must be read from the live frame;
- legacy pages may depend on `parent.frames.*`, therefore direct extraction of one child HTML without context can break behavior.

This is why early direct-fragment Modern attempts failed.

## 4. Modern navigation/UI contract

Current accepted phone direction after v0.5.13:

Top global row:
- Back;
- Home;
- Search;
- Settings.

Section context row:
- section identifier only;
- compact `Modern | Classic` switch.

Primary section menu has stable positions:

```text
[ Схеми ] [ Розʼєм ]
[ Положення на авто ]
[ Документація ]
```

Rules:
- primary tiles do not disappear;
- unavailable action stays in place but disabled/dimmed;
- unknown/future actions may render below the fixed group;
- content groups/selectors use compact rounded cards/tiles;
- duplicate section title is not needed in the top context row;
- content can show `<code> — <section title>`.

Real phone result:
- 101 tile layout accepted;
- 107 correctly keeps disabled `Розʼєм`;
- Modern/Classic segmented switch accepted;
- grouped scheme choices accepted.

Search action still requires explicit phone confirmation after v0.5.13.

## 5. Friendly menu naming

Current Modern mapping:
- `SCH` → `Схеми`;
- `NM` → `Розʼєм`;
- `PC` → `Положення на авто`;
- `CRITERE` → `Критерії / скорочення`;
- `GENE`, `PLATFUSI`, `AIDE` are documentation-type actions and belong under `Документація`;
- `blank` is a legacy placeholder and should not be shown as a Modern user action.

Do not assume this list is exhaustive. New legacy menu types found in later volumes must be preserved and classified, not silently dropped.

## 6. Connector / nomenclature contract

Classic NM proved that connector content can contain two distinct documents:
- `dessin` → connector drawing PDF;
- `alveoles` → connector pin/contact description HTML.

Modern must preserve that split.

### Opaque connector/source identity

A Renault connector/source identifier must be preserved as an opaque source ID.

The numeric suffix has no universal meaning:
- for section `101`, `101_1` / `101_2` are tied to different applicability criteria (for example `DD` right-hand drive, `DG` left-hand drive, `E2` / `E3` equipment levels and other criteria);
- for section `120` (ECU), suffix IDs may correspond to separate physical connector/pin documents on the same control unit. Real-phone evidence includes `120_18`, so no code may assume a small or contiguous `_1/_2/_3` range.

Therefore:
- never treat `_1`, `_2`, `_3` as duplicate-file counters;
- never derive connector semantics from the suffix itself;
- preserve the full Renault source identifier in Runtime IR / Android UI / export naming;
- determine applicability or physical role from surrounding source metadata and linked documents.

Current Modern actions:
- `Схема + піни розʼєма` when both child documents exist;
- `Схема розʼєму`;
- `Опис контактів`.

The combined action intentionally opens the original connector composite document (`dessin + alveoles`) inside the app viewer so the proven Classic-style scheme+pins layout remains available without switching the entire volume to Classic.

Do not flatten the contact table automatically below the PDF action.

### Connector document information hierarchy

Accepted target based on Classic phone reference:

```text
connector/location metadata
(e.g. ЭЛПРОВ. САЛОНА, B74,K74)

101 — ПРИКУРИВАТЕЛЬ
DG/E2/SSNAV/SRUNLI ...

native structured table
```

Legacy `CMP101` is not the desired Modern heading.

## 7. Structured tables contract

As of v0.5.14, abbreviations and connector pin descriptions use one native table component.

v0.5.15 refines that component so native and exported PDF tables use the same content-aware column model rather than only fixed ratios.

Examples:

Abbreviation/glossary:
```text
┌────────┬──────────────────────────────┐
│ CODE   │ FULL DESCRIPTION             │
├────────┼──────────────────────────────┤
│ DG     │ ...                          │
│ K74    │ ...                          │
└────────┴──────────────────────────────┘
```

Connector pins:
```text
┌───┬──────┬─────┬──────────────────────┐
│ № │ mm²  │ код │ опис                 │
├───┼──────┼─────┼──────────────────────┤
│ 1 │ 0.35 │ LPG │ ...                  │
└───┴──────┴─────┴──────────────────────┘
```

Rules:
- preserve source rows;
- preserve source columns;
- use content-aware column widths derived primarily from body cells;
- header text must not over-expand a compact code column;
- allow long right-hand descriptions to wrap inside the cell;
- when one cell wraps, shorter cells occupy the same full row height and are vertically centered;
- center header cells;
- do not reduce semantic tables to a pipe-separated monospace string;
- the same native component may adapt by column count.

Classic pin description on the light canvas is accepted as a readability reference.

## 8. Structured table PDF export

v0.5.14 adds `Зберегти таблицю PDF`.

v0.5.14 phone evidence confirms that the save picker works, the PDF is created, the file opens, and Cyrillic text is readable.

v0.5.15 adds:
- content-aware exported column widths;
- portrait pages for 1–2-column tables and landscape for wider tables;
- vertically centered multi-line row content;
- source-aware suggested filenames such as `101(pines).pdf`, `101_1(pines).pdf`, and `101(abbreviations).pdf`.

v0.5.16 tightens connector filename identity: pin-table source stems are preserved as opaque Renault IDs after stripping only the legacy `T_` wrapper. Examples include `101_1(pines).pdf` and the phone-confirmed `120_18(pines).pdf`. The app must not invent or interpret numeric suffixes for duplicate files; suffix values can be non-trivial and must be copied from the actual Renault source ID.

Contract:
- use Android `ACTION_CREATE_DOCUMENT`;
- output a real PDF, not screenshot capture;
- include section title;
- include criteria/configuration text;
- include connector/document metadata;
- include all table rows;
- paginate long tables;
- do not require broad storage permission.

Phone validation is still required for:
- save picker;
- saved PDF open;
- multi-page pagination;
- Cyrillic rendering/encoding;
- long-cell wrapping.

## 9. PDF viewer contract

Existing PDF viewer supports:
- continuous vertical document viewing;
- zoom;
- zoom step settings;
- fit-width;
- save original PDF.

v0.5.12 changed the toolbar:
- `+` moved immediately after the zoom dropdown;
- toolbar width follows zoomed document width rather than clipping at old viewport width.

BUG-001 remains implemented but phone confirmation should still be recorded explicitly when re-tested.

## 10. Classic canvas/readability

Classic connector pin description previously rendered dark text over a dark app/WebView canvas.

v0.5.11 sets a light/white canvas for non-hybrid Classic top-level runtime while preserving explicit legacy frame backgrounds.

Phone result: PASS.

Do not regress this behavior.

## 11. Modern catalog completeness — BUG-004 closed

BUG-004 was caused by the old assumption that Renault section identifiers were exactly three decimal digits.

v0.5.26 replaced that assumption with the durable contract:
- identifier is an opaque string;
- source order from Classic is preserved;
- numeric/alphanumeric/alphabetic codes are supported;
- only resolvable local targets are catalogued;
- identity is code + resolved entrypoint, not visible code alone;
- duplicate display codes with different targets remain separate;
- Runtime IR routing uses code + legacy entrypoint.

Representative identifier families include:
- normal numeric: `101`;
- 4-digit: `1405`, `1406`, `1407`, `1412`;
- R-prefixed: `R15`, `R21`, `R24`, `R262`, `R265`, `R325`;
- alphabetic: `MA`, `MAH`, `MYH`, `MB`, `ME`, `MG`, `MH`, `ML`, `MQ`, `MT`, `MW`, `NA`, `NC`, `NH`, `NT`, `NU`.

v0.5.26 required Renault Menu → 9 because converter/package data changed.

Real-phone closeout on 2026-09-25: representative Classic↔Modern parity was reported normal by the user. Duplicate-code routing was not explicitly exercised with a convenient phone example, but is covered by automated contract tests.

Status: **PHONE PASS (REPRESENTATIVE) · CLOSED**.

## 12. Fast Pack / performance findings

Real timing diagnostics showed:
- cached Fast Pack prepare ~261 ms;
- top ENTREE DOM load ~414 ms;
- hybrid projection only a few ms;
- live section trigger around ~100 ms in tested case.

Do not interpret `clientAgeMs` as exact startup duration; it includes user time before DBG capture.

Fast Pack cache is not the demonstrated main cause of the earlier long subjective wait.

For future performance investigation, instrument explicit milestones:
- Activity created;
- Fast Pack ready;
- WebView start/finish;
- projection start/done;
- section acknowledged;
- menu URL changed;
- native viewer ready;
- nav first nonblank;
- doc first nonblank/PDF.

## 13. Historical hybrid Modern work

v0.5.3 restored full Classic frameset after direct fragment approaches failed.

v0.5.4 attempted to hide legacy presentation during warmup but did not reliably hide Classic.

v0.5.5 added frame diagnostics.

v0.5.6 implemented deterministic frame projection:
- find named `titre/org/menu/nav/doc`;
- collapse the whole left `titre+org` outer branch;
- preserve right working frames;
- reapply projection with MutationObserver.

v0.5.7 added live section switching through hidden `org` and native section navigator.

Important lesson:
Modern should not keep growing as an elaborate controller around hidden Classic frames forever.

Runtime IR/native navigation is the preferred architecture.

Classic stays as the fallback/reference path.

## 14. Runtime IR / converter strategy

Desired staged architecture:

```text
Classic source
   ↓
converter
   ├─ preserved Classic package
   └─ Modern Runtime IR
         ↓
native catalog
         ↓
native section tiles
         ↓
native structured renderers
         ↓
legacy PDF/HTML only where not yet normalized
```

Future Runtime IR should be able to represent:
- catalog identifier;
- title;
- source order;
- stable target identity;
- primary menu actions;
- action availability;
- selector groups/options;
- document relationships;
- connector drawing + contact description relation;
- table headers/cells;
- PDF/document targets;
- metadata/criteria;
- unresolved legacy actions with explicit fallback semantics.

Do not delete source information during conversion merely because current Android UI does not use it yet.

## 15. Launcher icon / Play Protect

A custom adaptive launcher icon was added in v0.5.10.

Concept:
- dark technical background;
- blue document outline;
- white schematic traces;
- yellow connector node;
- original project symbol, not Renault trademark.

Play Protect warning seen on phone is associated with sideloaded/unverified development APK distribution.

Do not disable Play Protect globally.

Public-warning reduction requires normal verified developer/distribution/signing workflow.

## 16. APK phone workflow

Termux Renault menu currently includes:
- point 5: update project;
- point 8: download latest APK;
- point 9: regenerate/update Fast/Modern package when converter/data changes;
- point 13: open the newest APK build folder.

After point 8:
- APK is downloaded/verified;
- the exact APK folder should open automatically when Android allows it.

Fallbacks:
- DocumentsUI directory VIEW;
- `OPEN_DOCUMENT_TREE` positioned at the folder;
- finally open the APK itself.

Rule:
- APK/UI-only change → point 9 is not needed;
- converter/Runtime IR/data-package change → point 9 is required.

## 17. Release state

Latest CI-confirmed build:
- versionName: `0.5.17`;
- versionCode: `33`;
- merged app source: `b56b29b64cddd0cdf11531e7a1e543c45c5593e9`;
- main Tests: `36081274501` PASS;
- main Android Debug: `36081274495` PASS;
- artifact: `Renault-Docs-v0.5.17-Debug`;
- artifact id: `10841149488`;
- APK SHA-256: `a72784d9c5519e0830c30ec2f213c20f7c622a33f5112d2c9ae2f5d46b4237ed`;
- artifact ZIP SHA-256: `5fd8223e9a70b6cbf65e519640140923ea7197181018a69276d2bd84d9b671d7`.

v0.5.17 is merged and CI-confirmed. Phone validation of BUG-006 (structured-table PDF header on page 1 and repeated after pagination) is pending. The preceding v0.5.16 connector/menu/table behavior is phone-confirmed, including exact source-derived `101_1(pines).pdf`, `101_2(pines).pdf`, and representative `120_18.PDF` → `120_18(pines).pdf`. Connector-ID preservation is PASS* (representative real variants confirmed, not exhaustive enumeration of all 120 variants).

v0.5.14 phone validation result is partial:
- PDF save/open and Cyrillic rendering: PASS;
- table layout: refinement requested and implemented in v0.5.15;
- connector combined view: added in v0.5.15;
- export filename: corrected in v0.5.15;
- long multi-page pagination still needs explicit phone evidence.

### Canonical restart checkpoint

Latest session stop point:
`docs/assistant-kit/SESSION_CHECKPOINT_2026-09-25.md`

Tomorrow's first action is v0.5.17 phone QA for BUG-006. If that passes, resume BUG-004 converter/catalog parity.

## 18. Open / pending work

Priority order is not automatic; it can be changed by phone findings.

Current known work:

1. v0.5.18 phone QA — regenerate Runtime IR/package (Renault Menu → 9), then verify `103_5` pin header and that `0.6` stays on one line in native/PDF output.
2. Re-test `101(abbreviations).pdf` header and a `101_x(pines).pdf` header after regeneration.
3. Explicitly test multi-page structured-table PDF pagination with repeated header.
4. BUG-004 — CLOSED in v0.5.26 after representative phone parity; duplicate-code collision remains automated-test evidence.
4. Explicitly close/confirm BUG-001 PDF toolbar fix on phone.
5. Confirm v0.5.13 Search action opens the current volume with search field visible.
6. Continue parity audit of all Classic menu/document types across all Laguna II volumes, especially newer-year files and menu actions not yet classified.
7. Ensure unavailable/non-linked items remain visible but clearly disabled where stable layout is desired.
8. Continue replacing raw legacy abbreviations with user-friendly Ukrainian labels only where semantics are known; preserve unknown raw code instead of guessing.
9. Audit PDF/export lifecycle under rotation and cancel/return flows.
10. Expand Runtime IR/table metadata if future sources require semantic table type or richer layout.
11. Consider Laguna III as an additional dataset if source documentation is found. Converter/app architecture must remain dataset/model agnostic and must not hardcode Laguna II-only assumptions.
12. Continue visual polish after functional parity: spacing, typography, compactness, disabled-state contrast, dark-theme readability.
13. Public distribution/Play Protect strategy remains future release/distribution work, separate from local technical QA.

### Structured-table PDF header rule

Generated structured-table PDFs must preserve the source table's leading header rows. The column-name header is drawn before the first body row and repeated after every page break. For known legacy connector-contact documents (`T_*.HTM`) whose visual header is not represented as semantic HTML headers, converter v0.5.18 normalizes the standard 4-column pin table to `№ | мм² | Код | Опис` in Runtime IR. Unrelated headerless tables must not receive invented headers.

## 19. Non-negotiable preservation rules

- Do not remove Classic while Modern is incomplete.
- Do not silently drop legacy catalog entries.
- Do not assume section identifiers are numeric.
- Do not invent action semantics when source behavior is unknown.
- Preserve source order where it carries navigation meaning.
- Preserve separate source documents instead of flattening them without reason.
- Prefer converter-time work over expensive runtime parsing when possible.
- Avoid loading giant aggregate JSON files into Android heap.
- Every real-phone finding must be documented before it can be forgotten.
- Every release implementation must have release docs and phone gate.
- Every bug status change (OPEN/FIXED/PASS) must be recorded with evidence level.
- APK-only and converter/package changes must be clearly distinguished.

## 20. Documentation rule for all future work

For every future change, update the project documentation in the same work package.

At minimum:
- implementation/release notes;
- `CURRENT_HANDOFF.md`;
- `OPEN_FINDINGS.md` when a finding is created/changed/closed;
- this `PROJECT_LEDGER.md` when architecture, accepted behavior, pending work, or product direction changes;
- phone QA plan/result;
- CI/source/artifact/checksum metadata when a build is produced.

Do not rely on chat history as the only source of project state.


### v0.5.18 pin-table semantics

Real-phone `103_5` evidence exposed two remaining issues:
- no pin-table column header in generated PDF because some legacy `T_*.HTM` pages do not encode the visual header as semantic `th` cells;
- compact numeric tokens such as wire cross-section `0.6` can wrap in the narrow second native column.

v0.5.18 moves the missing-header correction to the converter/Runtime-IR layer and keeps compact pin fields non-wrapping in Android/PDF. This is a converter/data-package change, so point 9 is required after installing the APK.


### v0.5.19 structural pin detection + content/header-aware widths

Phone v0.5.18 confirms the `0.6` single-line fix, but pin headers remain absent in real `103_5` / `108` paths. The remaining root cause is that contact-table detection cannot depend on a `T_` filename wrapper.

v0.5.19 rules:
- identify 4-column pin/contact tables by row semantics, not filename;
- normalize to `№ | мм² | Код | Опис`;
- compact columns use the longest body/header value for sizing;
- every compact column remains single-line;
- the final description column receives the remaining width;
- 2-column glossary first column includes header width so `СОКРАЩЕНИЯ` does not wrap;
- native and PDF exporters share `NativeTableLayout`.

### Temporary reduced-volume package workflow

For faster phone iterations of Renault Menu → 9, it is valid to temporarily keep only the few test volumes inside the converted dataset build root.

Current build root:
`/storage/emulated/0/Documents/Renault/laguna 2 2001-2006_android`

Safe temporary workflow:
- move complete unwanted **converted volume folders** to a sibling holding directory outside the build root, for example:
  `/storage/emulated/0/Documents/Renault/_volumes_hold/`;
- do NOT move the original source dataset;
- do NOT place the hold directory inside `...2001-2006_android`, because Fast Pack recursively scans the whole build root;
- run point 9; package discovery and Fast Pack then see only the remaining volumes;
- after testing, restore all volume folders and run point 9 again to rebuild the complete catalog/package.

While volumes are held outside the build root, the generated Modern/catalog package intentionally contains only the remaining test volumes.


### v0.5.19 merged / CI PASS

Merged source:
`930b20d240fff0a2e0f7dd5e60769654241a23c9`

CI:
- Tests `36133711994` — PASS;
- Android Debug `36133712023` — PASS;
- artifact `Renault-Docs-v0.5.19-Debug`;
- artifact id `10862578973`;
- APK SHA-256 `03eb2a9f1edc9f5d0dcbe2aabad4090e433462ffcb866bcc38e610bedd682ab0`;
- ZIP SHA-256 `0e797cdfb03765ffb0115a8bb38bb4c7b6cc3018f836dfb0e25cd560189d150b`.

Immediate phone gate:
1. point 5;
2. point 8;
3. install v0.5.19;
4. point 9 (required);
5. validate 103_5 and 108 semantic headers;
6. validate compact width policy and `0.6 / 1.4 / 3CV / 3N` single-line behavior;
7. export 103_5(pines).pdf and confirm first three columns are compact while description fills the rest;
8. re-check 101 glossary header width.


### v0.5.20 composed pines PDF layout

Accepted pines export direction:
- `(...pines).pdf` is a dedicated composed technical document, not a generic structured-table dump;
- page geometry is portrait;
- top connector/application card reconstructs the useful Renault visual hierarchy from the non-pin structured table;
- connector icon is drawn locally as a vector primitive;
- section code/title/criteria remain a distinct centered block;
- pin header contains both vector pictograms and explicit text labels `№ | мм² | Код | Опис`;
- compact columns use measured content/header width and remain centered;
- final description column gets all remaining width and stays left-aligned;
- continuation pages use compact section identity + repeated table header, without repeating the large connector card;
- opaque source-derived filename contract remains unchanged.

v0.5.20 changes only Android PDF rendering. It does not change converter/Runtime IR. When testing immediately after a valid v0.5.19 point-9 regeneration, point 9 is not required again.


### v0.5.20 merged / CI PASS

Merged source:
`094698411f6af56a62dcf306c4c4c24b49e6af8c`

CI:
- Tests `36142685738` — PASS;
- Android Debug `36142685726` — PASS;
- artifact `Renault-Docs-v0.5.20-Debug`;
- artifact id `10868375871`;
- APK SHA-256 `d3a898085c6b2dc3c3e19c58e82b3fdc27de75fefff5ea5a5e10f32bd47016fc`;
- ZIP SHA-256 `d480d5ff7717e633af0b76655567a660c2004bb7db7f32eaa5f10d3deecc4a5b`.

Immediate phone gate:
1. point 5;
2. point 8;
3. install v0.5.20;
4. do not run point 9 if the dataset is already the v0.5.19-regenerated package;
5. export a 105 pines PDF and `108_1(pines).pdf`;
6. verify connector card → centered section identity/criteria → icon+text pin table;
7. verify connector/application values survive;
8. verify portrait layout and measured compact columns.


### v0.5.21 table/PDF visual polish

Phone review of v0.5.20 accepted the new composed pines document structure and requested a polish pass.

Accepted v0.5.21 rules:
- PDF page margins are ~2 cm (`57 pt`);
- generic structured PDF titles are centered;
- first table begins with a visible gap below title/criteria/metadata;
- 2-column glossary first column is compact and content/header-aware; description receives the remainder;
- native pines tables guarantee a visible semantic header `№ | мм² | Код | Опис` even when the active Runtime IR payload lacks a leading header row;
- native pines columns `№ / мм² / Код` are tightened and remain single-line;
- pines PDF first three columns are measured tightly from actual content/header; description gets the remainder;
- pines technical icons use restrained blue/teal/green/orange/purple accents with soft badges;
- connector/application information remains present between the source blocks;
- v0.5.21 is APK-only and does not require point 9 when testing on the already-regenerated v0.5.19+ dataset.


### v0.5.21 merged / CI PASS

Merged source:
`ad9d69cb05697e960f2fc2800f28f7a95f99a4e8`

CI:
- Tests `36146178858` — PASS;
- Android Debug `36146178924` — PASS;
- artifact `Renault-Docs-v0.5.21-Debug`;
- artifact id `10869810678`;
- APK SHA-256 `934f9791804b65725f75f4b9db7be6e8701f394e646d4ee854fda79adbaf58ea`;
- ZIP SHA-256 `ae29db796261f2e3b7db26f4f290f414bb8d964c6222cf7e76f7053deec87ef2`.

Immediate phone gate:
1. Renault Menu → 5;
2. Renault Menu → 8;
3. install v0.5.21;
4. point 9 is NOT required;
5. check native 108/105 pines table header and tighter Код width;
6. export `108_1(pines).pdf` and inspect ~2 cm margins, lower placement, colored icons and tighter compact columns;
7. export `105(abbreviations).pdf` and inspect centered title, title/table gap and compact first column.


### v0.5.22 measured native columns + lifecycle audit

Phone v0.5.21 feedback:
- semantic pin header is now visible;
- pines PDF visual composition is accepted;
- native compact columns still clip because fraction heuristics are not equivalent to real Android text width;
- first-column abbreviations/designations and table headers should be bold;
- multiple child windows disappear on phone rotation and return to parent/default content.

v0.5.22 table rule:
- native columns before the final description column are measured from actual rendered text using Android Paint;
- header text participates in the maximum;
- 2-column column 1 abbreviations/designations are bold;
- table headers are bold and high contrast;
- final column gets the remaining width;
- generic 2-column PDF abbreviations/designations are bold.

v0.5.22 lifecycle rule:
- every app-owned child window has a logical state owner;
- NativeSection preserves panel / Documentation / composite document / structured document;
- Settings preserves the currently open chooser dialog;
- Viewer preserves section navigator and its query, and regenerates Frame debug after rotation;
- WebView/history/page search and Viewer pending PDF path remain under their existing saved-state contract;
- NativeSection serializes pending table-PDF export data while Android's create-document picker owns the foreground;
- no rotation may auto-run a user action.

Full lifecycle audit:
`docs/v.0.5.22/LIFECYCLE_AUDIT.md`

v0.5.22 is APK-only. Point 9 is not required.


### v0.5.22 merged / CI PASS

Merged source:
`b5e685d0b6c6d635ce64132685d00ccacc431007`

CI:
- Tests `36150581053` — PASS;
- Android Debug `36150581047` — PASS;
- artifact `Renault-Docs-v0.5.22-Debug`;
- artifact id `10871432429`;
- APK SHA-256 `f7619833cae34fa552cda3626e5770a9f3ae93bafe40adb38d0fa855258ce9a4`;
- ZIP SHA-256 `a7a6e0af3d69f7ce1cf16de117def9fd51091b20edbd6e565e4f12e4f1dff13d`.

Immediate phone gate:
1. Renault Menu → 5.
2. Renault Menu → 8.
3. Install v0.5.22.
4. Do not run point 9.
5. Validate measured native widths and bold header/designations on 108.
6. Execute the full rotation matrix in `docs/v.0.5.22/LIFECYCLE_AUDIT.md`.
7. Keep BUG-009 open until the phone lifecycle audit is actually PASS.


### v0.5.23 landscape focus + pines PDF typography

Real-phone v0.5.22 feedback:
- portrait 108 native table is accepted;
- focused landscape still wastes vertical space on global chrome and unrelated top-level buttons;
- pines PDF needs bolder technical values and stronger table hierarchy.

v0.5.23 landscape rule:
- store active top-level menu label/action;
- in landscape with an active mode, hide global toolbar, section context row, Modern/Classic and unrelated top-level buttons;
- render only the active top-level mode as a full-width button;
- keep the same child panel/document below;
- portrait rebuilds the full navigation chrome.

v0.5.23 PDF typography:
- `Typeface.SANS_SERIF`;
- body/description = 10 pt regular;
- technical columns 1-3 = 10 pt bold;
- table headers = 14 pt bold;
- upper connector-info table = 14 pt bold;
- connector identity remains 15 pt / 17 pt bold;
- criteria remains 10.5 pt bold.

Version: 0.5.23 / code 39.
APK-only; point 9 is not required.


### v0.5.24 PDF viewer quality + rotation cache

UX-016 implementation:
- keep Android PdfRenderer architecture, but remove lossy JPEG from page display;
- render pages to lossless PNG;
- adaptive target uses DPR up to 2.5 and width up to 2400 px;
- backend groups requests into 200 px width buckets;
- compressed rendered pages live in a 32 MiB process-level LRU cache;
- cache key includes dataset tree URI to prevent cross-dataset collisions;
- cache survives ViewerActivity/WebViewClient recreation during rotation;
- visible page is requested before neighbors;
- neighbor prefetch waits 60 ms;
- old bitmap remains visible while a higher-resolution replacement loads;
- resize/orientation rendering is debounced and only upgrades when target width changes materially;
- lazy prefetch window reduced.

Memory constraint:
- full ARGB bitmap is not retained in the cache;
- one render at a time remains protected by the shared render lock;
- phone OOM/stability gate is mandatory before closing.

Version: v0.5.24 / code 40.
APK-only; point 9 is not required.


### v0.5.24 merged / CI PASS

Merged source:
`c8decdb903c9b48325bdeb55e2c1e1413b8844a1`

CI:
- Tests `36163857309` — PASS;
- Android Debug `36163857157` — PASS;
- artifact `Renault-Docs-v0.5.24-Debug`;
- artifact id `10876995104`;
- APK SHA-256 `2df8bef3156341710d8c4c12e93fcc9eccd5d6758834f48f881db5ef35d7cb57`;
- ZIP SHA-256 `e6c9e23d7eb6fc939681bdcfa39486c671cdf3d323ab4f27d22b06d4379a995c`.

Immediate phone gate:
1. point 5;
2. point 8;
3. install v0.5.24;
4. do not run point 9;
5. compare the same PDF at 100/150/200%;
6. rotate portrait↔landscape twice and compare first/repeat latency;
7. scroll 4–5 pages and back;
8. stress zoom + rotation and watch for OOM/stale pages.


### v0.5.24 phone result / v0.5.25 PDF viewport follow-up

v0.5.24 real phone:
- quality ++;
- first rotation +;
- repeat rotation ++;
- 100% multi-page scroll acceptable;
- 200% scrolling through ~20 pages remains too heavy.

v0.5.25 contract:
- toolbar and PDF document are separate layout regions;
- PDF content scrolls/pans only inside `#pdfViewport`;
- portrait toolbar = two rows;
- landscape toolbar = one row;
- toolbar width is no longer tied to zoomed document width;
- >=150%: active scrolling uses a temporary <=1600 px render tier, then upgrades to full quality after 180 ms idle;
- >=150%: prefetch only current ±1 and retain decoded current ±1;
- <150%: prefetch current ±2 and retain decoded current ±4;
- far pages keep geometry but release decoded image src;
- v0.5.24 shared compressed cache remains the reload source;
- nested PDF viewport page/zoom/scroll position gets a short-lived 5-minute restore state so rotation does not regress.

Version: v0.5.25 / code 41.
APK-only; point 9 is not required.


### v0.5.25 merged / CI PASS

Merged source:
`6e962b5bac1bdee93ba232f55ee594f318ab2f8b`

CI:
- Tests `36167218686` — PASS;
- Android Debug `36167218706` — PASS;
- artifact `Renault-Docs-v0.5.25-Debug`;
- artifact id `10878087586`;
- APK SHA-256 `205f031bb889826f0b3b7ae3d76318627c203ed0cb3829b60ac611b0380c625b`;
- ZIP SHA-256 `34b5508b7b7006f4c3e35f0e2839de8729a660bba16109caad70bef91194976e`.

Immediate phone gate:
1. 5 → 8 → install v0.5.25;
2. do not run point 9;
3. confirm portrait PDF controls are two rows and remain fixed;
4. confirm landscape PDF controls are one row;
5. at 200%, scroll 8–10 pages down/back in a ~20-page PDF;
6. confirm temporary interactive rendering sharpens after scroll idle;
7. rotate while mid-document and verify page-relative position persists.


### v0.5.26 — full Classic catalog parity

BUG-004 dependency is implemented before building the final in-app converter execution path.

Catalog rules:
- Renault section/entry code is an opaque string;
- numeric 3/4-digit, R/mixed and short alphabetic IDs are supported;
- Classic navigation order is preserved;
- only real resolved local HTML targets enter Modern;
- exact same code+target may deduplicate;
- same display code + different target must remain separate.

Runtime routing:
- shard index includes ordered `section_entries[] = {code, entrypoint, path}`;
- legacy `sections[code]` remains first-occurrence compatibility only;
- Android section load uses code + legacy entrypoint.

This explicitly prevents duplicate-code collisions and avoids numeric assumptions that would block other Renault datasets such as Megane II / future Laguna III.

Version: v0.5.26 / code 42.
Converter/package change: point 9 REQUIRED.


### v0.5.27 live PDF pinch zoom

User requested a targeted PDF UX improvement after accepting the v0.5.25 raster viewer.

Contract:
- toolbar stays outside `#pdfViewport` and never scales with document pinch;
- generated PDF page disables browser-level zoom;
- two-finger pinch changes only PDF document width;
- zoom percentage updates live during the gesture;
- touched page region is used as the focal anchor;
- current decoded bitmap remains visible while zooming;
- high-resolution `PdfRenderer` refresh is deferred by ~180 ms until interaction settles;
- current page is prioritized, then bounded neighbor prefetch follows;
- queued scroll/prefetch render work is cancelled/suppressed while pinch is active;
- zoom range stays 50–200%;
- +/- / typed zoom / fit-width remain on the same shared zoom state.

Version: v0.5.27 / code 43.
APK-only: point 9 is NOT required.


### v0.5.27 merged / CI PASS

Merged main source:
`7660416ad3f00a7fdf3d7556b6fd11a0bfd1186f`.

Tested feature source:
`46ba0c0df464d144158cd5a696611f2366198d75`.

Runtime blobs for PDF implementation/version are identical between tested feature source and merged main.

CI:
- Tests `36176557305` — PASS;
- Android Debug `36176557288` — PASS;
- artifact `Renault-Docs-v0.5.27-Debug`;
- artifact id `10882358464`;
- APK SHA-256 `03dca9deb5fda72e4fa5d72eae18f94285c451e04bd3b78157bf97fd19109ac8`;
- artifact ZIP SHA-256 `b0f79a703f60fd1255fb90a9724bce8e39196bbcf694579c83a0e2416e5406ae`.

Immediate phone gate:
1. Renault Menu → 5;
2. Renault Menu → 8;
3. install v0.5.27;
4. do NOT run point 9;
5. validate toolbar isolation during pinch;
6. validate live zoom percentage;
7. validate focal-point stability;
8. validate post-pinch sharpness upgrade;
9. regression-check long-document 180–200% scroll and rotation.


### v0.5.28 400% zoom + fullscreen

User accepted v0.5.27 pinch zoom and requested additional document focus controls.

Contract:
- PDF zoom range: 50–400%;
- quick presets include 300% and 400%;
- full render cap: 3000 px;
- active-scroll render tier remains <=1600 px;
- >=250% prefetch/decoded retain radius becomes 0 so only current page is decoded;
- PDF toolbar stays fixed and contains fullscreen toggle;
- fullscreen hides ViewerActivity global toolbar and Android system bars, not the PDF toolbar;
- Back exits fullscreen before normal navigation;
- fullscreen state persists across Activity recreation.

Version: v0.5.28 / code 44.
APK-only: point 9 is NOT required.

### Planned volume-level documentation IR

`GENE / PLATFUSI / AIDE` are currently recognized as documentation-type actions and grouped by every section screen.

Where a Renault volume exposes the same documentation set across all/most sections, future converter work should:
1. identify the common route-set at conversion time;
2. create one volume-level documentation shard/catalog;
3. store only a stable reference from each section;
4. cache/read that catalog once in Android;
5. retain section-local documentation only when a section genuinely differs.

Do not assume all future Renault datasets share exactly the same documentation. Converter must verify equality of resolved targets before hoisting.

This is a Runtime IR/package optimization and will require point 9 when implemented.


### v0.5.28 merged / CI PASS

Merged main source:
`1104db917f5964db7efb41a9282eb88ccde733ee`.

Tested feature source:
`330c4d7168712236453d76fe8fef92e17710b35c`.

CI:
- Tests `36179103365` — PASS;
- Android Debug `36179103296` — PASS;
- artifact `Renault-Docs-v0.5.28-Debug`;
- artifact id `10884005657`;
- APK SHA-256 `615fe8bb939b41a92ac999472bad0015c9a5e2b7dfaf204cb344bba778479fd2`;
- artifact ZIP SHA-256 `9016bd4ef4b3dc03d28a6b50e9037f4eb89731e875db186b69c1ee8c052b780d`.

Immediate phone gate:
1. Renault Menu → 5;
2. Renault Menu → 8;
3. install v0.5.28;
4. do NOT run point 9;
5. verify 300–400% pinch/typed/preset zoom;
6. verify current-page-only behavior remains stable at >=250%;
7. verify fullscreen entry/exit in landscape;
8. verify Back exits fullscreen first;
9. verify rotation keeps PDF state.


### v0.5.29 per-volume documentation ownership

User clarified the durable Renault data model:

- every volume/configuration has its own documentation files;
- sections inside that SAME volume reuse that one documentation set;
- documentation from different volumes must never be combined even when labels look identical.

Implemented contract:
- converter derives documentation signatures independently per volume;
- signature uses resolved top-level GENE / PLATFUSI / AIDE routes;
- all non-empty signatures in a volume must match before hoisting;
- conflicts keep documentation section-local;
- one `runtime-ir/documentation/<volume-key>.json` shard is emitted for a qualifying volume;
- runtime index stores that volume's `documentation_path`;
- documentation graph gets a `vdoc-*` ID namespace;
- Android section startup reads only the section shard and path metadata;
- the volume documentation shard is loaded lazily only when the user opens `Документація`;
- section-local graph remains a fallback in v0.5.29.

No cross-volume deduplication is permitted.

Because Runtime IR/package output changes, point 9 is mandatory for v0.5.29.

### Fullscreen pressed-state follow-up

v0.5.29 also makes the PDF fullscreen button reflect actual mode:
- inactive = normal toolbar style;
- active = accent/pressed style;
- `aria-pressed` is synchronized from ViewerActivity;
- Back/rotation/focus updates the visual state.


### v0.5.30 high-zoom PDF page tracking

Real phone exposed a flaw in the v0.5.28 memory optimization: at 400% a page can be taller than the viewport by enough that the 25% IntersectionObserver threshold never promotes it to currentPage. Since retain radius was 0, the visible page could then be evicted while the stale page was repeatedly loaded.

v0.5.30 contract:
- current page is recalculated from nested viewport center on every document scroll;
- high zoom retains/preloads ±1;
- neighboring pages use <=1200 px;
- scrolling current page uses <=1600 px;
- idle current page upgrades to <=3000 px;
- no zoom reduction should be required to make a page render.

### Planned documentation UI refactor

The per-volume documentation shard from v0.5.29 is the correct data model. The next cleanup should also make UI ownership volume-level:
- ModernVolumeActivity exposes Documentation;
- a dedicated VolumeDocumentationActivity renders the shard;
- NativeSectionActivity keeps only a shortcut;
- once verified, section-local documentation graph copies can be removed.

Do not merge or cache documentation across different volumes/configurations.


### Planned PDF cross-reference workspace

A new product-level requirement was clarified on 2026-09-26:

While reading a wiring-diagram PDF, the user should be able to open the SAME volume's documentation in a secondary frame without leaving the schematic.

Rationale:
- diagram block/fuse/component identifiers are numerous;
- manual lookup in volume documentation is a core diagnostic workflow;
- documentation access therefore remains important even after section-local documentation graph duplication is removed.

Target architecture:
- one volume-owned documentation identity/shard;
- dedicated volume documentation screen for normal navigation;
- section Documentation remains a shortcut;
- PDF toolbar gets a Companion/split control near fullscreen;
- main schematic remains open;
- secondary documentation pane opens below it;
- panes preserve independent navigation/scroll/zoom state;
- fullscreen and rotation preserve the split workspace;
- no cross-volume documentation sharing;
- conservative shared render-memory budget when both panes display PDFs.

Do not make automatic identifier recognition a dependency for this feature.

Design source:
`docs/design/PDF_COMPANION_DOCUMENTATION.md`.


### v0.5.31 dedicated volume documentation screen

ARCH-008 implementation makes UI ownership match the existing per-volume documentation shard.

New canonical flow:
- ModernVolumeActivity → Documentation → VolumeDocumentationActivity;
- NativeSectionActivity → Documentation → the same VolumeDocumentationActivity;
- future PDF Companion → the same volume documentation identity.

VolumeDocumentationActivity:
- receives volume context only;
- resolves `documentation_path` directly from Runtime IR index;
- does not need an arbitrary section to bootstrap;
- renders root/nested documentation navigation natively;
- opens final HTML/PDF targets through ViewerActivity;
- restores nested panel stack across rotation/recreation.

Migration rule:
- keep section-local documentation graph as fallback in v0.5.31;
- after phone PASS, a later converter cleanup may remove verified duplicate GENE/PLATFUSI/AIDE graph data from section shards.

v0.5.31 / code 47 is APK/runtime-reader only and consumes package data already generated by v0.5.29. Point 9 is not required if that package refresh already happened.


### v0.5.31 phone closeout

ARCH-008 dedicated volume documentation screen is PHONE PASS and closed.

Accepted behavior:
- direct volume Documentation works;
- section Documentation shortcuts to the same volume-owned screen;
- child navigation works;
- another volume remains isolated;
- rotation/back hierarchy is acceptable.

### v0.5.32 PDF Companion split workspace

First implementation of the manual cross-reference workspace is merged.

Architecture:
- main ViewerActivity WebView remains the primary schematic;
- companion is a second WebView inside the same Activity below the main view;
- both use the same SAF dataset but independent WebView/PDF state;
- volume documentation is loaded through the existing volume-level documentation identity;
- companion final HTML/PDF navigation never replaces the main schematic.

PDF state:
- main scope = `main`;
- companion scope = `companion`.

Render budget:
- main: existing <=3000 px idle / <=1600 px active scroll;
- companion: <=1800 px full / <=1200 px active / <=900 px neighbor cap;
- compact companion keeps no high-zoom decoded neighbors;
- global compressed image cache remains shared.

Lifecycle:
- companion open/closed state persists;
- companion WebView history/state persists through Activity recreation;
- fullscreen covers the entire split workspace;
- Android Back operates companion history before main navigation after leaving fullscreen.

Version: v0.5.32 / code 48.
Point 9 is NOT required if package data from v0.5.29+ is already current.


### v0.5.32 phone feedback / v0.5.33 split focus refinement

Real-phone portrait and landscape screenshots confirmed the core v0.5.32 Companion architecture:
- upper schematic and lower documentation PDF can coexist;
- independent page/zoom states work;
- fullscreen split is usable.

Observed UX cost:
- two PDF toolbars plus companion header take too much vertical space;
- fixed ratio is not flexible enough.

v0.5.33 contract:
- fullscreen + Companion = split focus mode;
- PDF toolbars are fixed overlays, hidden by default;
- companion header hidden in split focus;
- double tap either WebView reveals controls for about 3.2 seconds;
- floating Android controls provide companion Back, Close and fullscreen exit;
- divider remains visible and draggable;
- main pane ratio clamped to 20–80%, default 55%;
- ratio persists across Activity recreation;
- split/fullscreen/WebView/PDF state survives rotation;
- normal non-fullscreen Companion UI remains unchanged;
- landscape remains a vertical split until phone evidence justifies side-by-side.

Version: v0.5.33 / code 49.
APK-only; point 9 not required when v0.5.29+ package is current.


### v0.5.37 Converter Writer Wave 1

After PDF/Companion closeout, development returns to the long-planned Converter 2.0.

Existing foundation reused:
- ConversionActivity SAF source/destination draft from v0.2.0;
- Kotlin ConverterPathNormalizer parity with the Python path converter;
- current universal dataset manifest/library model.

v0.5.37 adds real Android execution:
- foreground dataSync ConversionService;
- persistent ConversionRunStore so Activity rotation/return reconnects instead of restarting;
- full source scan before writes;
- output uses hidden staging folder;
- recursive tree copy through SAF;
- HTM/HTML/JS normalized during copy using ISO-8859-1 byte-preserving semantics;
- cancellation checked during scan/copy/validation;
- base package writes renault-dataset.json, START.html, README_UA.html, volumes.json and modern-index.json;
- validation checks manifest + entrypoint + copied source file count;
- only validated staging is renamed to <source>_android;
- final output can be registered in Library;
- source is never modified/deleted.

This is intentionally Wave 1, not the final converter.

Remaining before Converter 2.0 is called complete:
- port full Modern sections + Runtime IR v2 + documentation shards + Fast Pack generation to Android;
- full-corpus parity audit against current Python package output;
- verified source ZIP backup + metadata + SHA-256 + read verification;
- explicit post-backup source deletion confirmation;
- Backup Center restore/verify/delete.

The converter must remain dataset/model agnostic. Do not introduce Laguna-II-only assumptions; Megane II is the first planned external dataset after current-corpus converter parity.
