## v0.5.83/build99 — #79 Home New Volume fixed in code — 2026-10-09

PR #89 merged, app SHA `b5f1c22d73642d7341943e4320b5fb469cbe9bec`. ProjectChooserActivity now calls ProjectActivity.intent(showAddPanel=true) instead of openPicker=true; ProjectActivity expands existing Add choices on fresh navigation, preserves saved pin and rotational expansion, and leaves explicit direct-picker route available. Python Tests #573 PR / #574 main PASS, Android PR #457 and stable-signed APK #146 PASS. Signed artifact ID 11582942367, bundle SHA256 `b65dd8100cb4412c7a79933ad2c12bc4456859f37ff1a4268298933f3e01d1ef`. **Phone QA PAUSED**, issue #79 remains OPEN until phone acceptance; do not start additional QA or mutate archives. For details see `docs/assistant-kit/SESSION_CHECKPOINT_2026-10-09_v0.5.83.md`.

---

## v0.5.82 — CORE HOME/ORIENTATION PHONE PASS — 2026-10-08

After signed build98 (APK #145) user responded «Пасс. Взагалі все чудово.» to the Home portrait→landscape→portrait test: collapsible/pinnable `Додати`, independent `Мої Renault` layout, system bars landscape hide / portrait restore, portrait 📌 state. Record **core phone PASS**; other QA (Tools/Legacy/Drive, scroll/status, Help/Dialog/Viewer/Settings/SAF/IME) still open. Source unchanged `f17b319c4e17d8f7005ae20221dd7bd642cb172b`. No automatic imports or cleanup; see `docs/v.0.5.82/qa/PHONE_TEST_REPORT_2026-10-08.md`.

---

## v0.5.82 — Home Add parity / global immersive landscape — 2026-10-08

User-requested new UI: on Home outside expandable `Додати` only `Мої Renault` project list (Megane II, Laguna II, Kangoo II). Inside Add: New Volume, New Project, Ready Projects/Drive, project-vs-volume explanation, Tools (Converter/Legacy), legacy dataset records. Home own 📌 portrait pin setting separate from Project, temporary landscape expansion, fixed operations status above independently scrollable My Renault; bounded internal action scroll. Landscape hide status/navigation bars in app-owned Activity/dialog windows, show on portrait; exclude Android system keyboard/SAF; PDF fullscreen takes precedence. PR #88 merged, app SHA `f17b319c4e17d8f7005ae20221dd7bd642cb172b`; PR Tests #571 and Android Check #456 PASS, main Tests #572 and signed APK #145 PASS; artifact ID 11563839781, SHA256 `94cadb5caf5b1af93a2a959e674819c31f0571b479c66f7b9a492af6ae6586be`. **Phone QA pending, not closed**. Reusable contracts updated. Preserve data; no original ZIP/RDPKG deletion/movement. v0.5.81 screenshots show Modern search and archive dialog but not confirmed Viewer `Розділи` modal; do not mark that gate PASS.

---

## v0.5.81 dialog consistency — CODE MERGED / MAIN CI PASS / PHONE QA PENDING — 2026-10-08

User requested one visual standard for warning, confirmation and action windows; real-phone v0.5.80 archive preview is too verbose. Audit: 20 app-owned Android AlertDialogs in 5 owners, 18 already styled; Viewer 2 now use DialogUi. Archive preflight now compact by default, technical provider/Document ID + full registered-project NT candidates expandable in same modal; toggled state restored across rotation. Preserves existing Cancel/Continue/selected source URI and no automatic source mutations. PR #87 merged to main source `309f068bbf92c65ca06c0cc2e38887d6a4664837`; Tests #568 PASS, Android Debug APK #144 PASS; stable artifact 11560786817 sha256 `5711b9b6181edd4edaa53a575bc23fa13186a68aea925544de71b2b78b1afe79`. Full audit/phone gates: `docs/v.0.5.81/DIALOG_AUDIT.md`, `docs/v.0.5.81/qa/PHONE_TEST.md`. **Not phone-accepted yet**. v0.5.80 own screenshot evidence: disabled prepared packages PASS, cross-model block PASS, current-project NT8341A preview match, rotation PASS; explicit Cancel/no-run and other-project match remain pending. Keyboard visible behind prior dialog is observation, not confirmed new regression. No automatic reimport, cleanup or original file relocation.

---

## v0.5.77 — landscape pinned Add — PHONE PASS 6/6 / CLOSED — 2026-10-08

Phone acceptance received 2026-10-08 from user: PASS 6/6, comment «Все норм». Checked:
1. Portrait: Add expanded, pin red — PASS.
2. Landscape rotation: Add auto-collapses, pin grayscale — PASS.
3. Landscape: volume list scrolls freely — PASS.
4. Landscape: manual expand/collapse — PASS.
5. Return portrait: pinned red emoji and expanded Add return — PASS.
6. Reopen Megane II: persistent pinned state — PASS.

Scope: six explicitly reported gates only; separate unpinned rotation, repeated rotations, no auto-action and status-card behavior were not individually asserted in this six-item result.

- Implementation: PR #77 merged, source `87ab5c2dc20fa14dd7e88ac8cfb81d746c262169`.
- Tests #555 PASS; Android Debug APK #140 PASS, artifact `Renault-Docs-v0.5.77-Debug` id `11523858486`.
- v0.5.77 release accepted without new runtime changes.
- Next: status-card review as independent work. Issue #68 kept open for the Home-panel follow-up.

---

# Renault Docs — Master Project Ledger

## Issue #30 — deterministic Catalog publish pipeline — 2026-10-06

PR #32 merged:
`f1def74bc7a2c5748e4ec886bf00beb0dc23c179`.

CI:
- PR Tests #456 — PASS;
- main Tests #457 — PASS.

Durable data contract:
- Windows/source Drive roots remain read-only and separate;
- public `Renault Docs Projects` contains runtime-ready packages + catalog metadata only;
- production Catalog is generated from canonical validated .rdpkg files plus explicit Drive IDs;
- generator computes actual file size and SHA-256;
- non-canonical filenames, missing packages and duplicate Drive IDs fail closed;
- no Android runtime/version change was required.

Read-only Drive roots confirmed:
- MEGANE II source: `1LLDp8bvgS8UGRoQvc1kjAdgsYEG70H1n`;
- external source folder `laguna2`: `1j8Rm2f1Abk_0hFHI6bsTbq26rbF-rjKo`; keep this external folder name unchanged, while Renault Docs uses `Laguna II` as the project/display identity;
- Renault Docs Projects: `1UyN4UIgaNMrpG-5mLuDBd9laFEbwmb4Y`.

Laguna II is the first publish batch because all 10 packages already have accepted conversion/package evidence.

Next gate:
upload the exact 10 canonical Laguna II .rdpkg files, capture Drive IDs, build catalog v3, verify SHA evidence, then Catalog-import smoke the oldest and newest volumes.


## v0.5.62 — Drive Catalog v1 — CLOSED 2026-10-06

Final status: **PHONE PASS / MAIN VERIFIED / CLOSED**.

Exact accepted runtime head:
`e215ed9a29ccf7cd7e36d083e2579e24bd2a9c3f`.

Final main runtime merge:
`2fe05f18618718e5ef16521ad0411734fb1b89f5`.

PR:
`#31 — v0.5.62 — Drive Catalog v1` — MERGED.

Issue:
`#29 — Drive Catalog v1 + guided Renault Docs import` — COMPLETE.

CI:
- Tests #451 — PASS;
- Android PR Check #369 — PASS;
- PR closeout Tests #454 — PASS;
- PR closeout Android PR Check #372 — PASS;
- final main Tests #455 — PASS;
- final main Android Debug APK #123 — PASS.

Final main APK:
- `Renault-Docs-v0.5.62-Debug`;
- SHA-256 `939f69b3dd70ab5043304286f132b6c9151f694133d3de7169fc82079f609de9`;
- installed over existing app with data preserved.

Accepted product/runtime contracts:
- Google Drive is backend storage; Renault Docs owns the user-facing catalog UI;
- normal users see model/project + volume metadata, not long physical package filenames;
- public catalog manifest is `renault-docs-catalog.json`;
- current catalog groups Megane II / E84 · L84 · K84 and lists NT8340A + NT8342A;
- installed state is computed locally and shown as `✓ Встановлено`;
- selected `.rdpkg` packages download from Drive and reuse the validated/atomic RdpkgImporter path;
- successful import upserts the volume into the matching Renault project;
- catalog selection survives configuration change;
- terminal import result survives rotation;
- loaded catalog state is retained during rotation to avoid a visible network reload delay;
- absent optional metadata is omitted instead of displayed as `null`.

Final phone evidence:
- v0.5.62 / build 78 installed from final main build;
- in-place update preserved project data;
- Megane II = 2 volumes;
- NT8340A · 2006-04-18 opens successfully;
- NT8342A remains present.

Data/source separation:
- original Windows-only archives stay on separate source storage;
- Android/runtime-ready `.rdpkg/.rdproject` and catalog metadata live separately in `Renault Docs Projects`;
- issue #30 owns the Windows source → conversion → validation → publish pipeline.

Next gate:
issue #30 — expand the source/conversion/publish pipeline and Catalog. Do not reopen #29 unless a regression is found.

## v0.5.61 — shared live progress — CLOSED 2026-10-05

Final app source:
`c36e10ddb5fb1a24e9a37d2dc21321a577ed69ed`.

Final main CI:
- Tests #443 — PASS;
- Android Debug APK #116 — PASS.

Phone acceptance:
- final `main` APK installed over the existing app;
- version `v0.5.61` confirmed;
- Megane II 2 / Laguna II 10 / Kangoo II 1 retained;
- no project data loss.

Durable contracts accepted:
- one shared thin progress component for long operations;
- visible file counters;
- progress uses measured work with file/byte weighting;
- no bouncing indeterminate bar before real totals are known;
- .rdproject preparation is service-owned and lifecycle durable;
- prepared project is reused for normal Share unless explicitly repacked;
- project progress names the current volume and output filename;
- project/volume deletion flows use the same actions → confirm → inline-result sequence.

Issue #25: complete.


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
## 21. Kotlin-native raw → .rdpkg preparation contract — 2026-09-27

After v0.5.50 closed the managed-package Android export/import round-trip, the next converter architecture is fixed:

```text
raw SAF source
   → one metadata scan
   → one normalized app-private staging copy
   → Kotlin compiler/Runtime IR
   → Fast Pack
   → manifests
   → streamed .rdpkg
```

The primary user flow must not create a public `*_android` intermediate directory. Private staging is allowed because compiler stages need random access to normalized source files; it is temporary implementation storage, not a user-facing dataset.

Performance rules:
- each raw source file should cross SAF once for the preparation copy;
- path normalization happens during that copy;
- compiler work runs on local File I/O;
- Fast Pack reads only selected web assets;
- outer `.rdpkg` reads prepared files once;
- SHA-256 is calculated during archive writing, not with a second full package read.

v0.5.51 foundation:
- `RdpkgZipWriter` owns deterministic outer-package streaming;
- text/metadata use `Deflater.BEST_SPEED`;
- binary/already-compressed payload uses `Deflater.NO_COMPRESSION`;
- `NativeFastPackWriter` mirrors Python Fast Pack selection semantics and hashes while writing;
- existing managed `.rdpkg` export delegates to the shared writer.

Python/Termux remain the reference implementation and parity oracle until Kotlin compilation is verified against the same source volume. ZIP bytes/hashes do not need to match Python when compression policy intentionally differs; semantic Runtime IR/package parity is the acceptance criterion.

Canonical design:
`docs/architecture/KOTLIN_NATIVE_RAW_TO_RDPKG.md`.
### v0.5.51 native preparation foundation checkpoint

Development branch `feat/v0.5.51-native-preparation-foundation` has a green CI checkpoint at `51c833fbdbc23f6f2dbdf649cba79c09d3eb7968`.

Implemented foundations:
- raw SAF → app-private staging with one metadata scan and one normalized copy;
- Kotlin volume discovery / Modern index;
- Kotlin section discovery preserving opaque section identifiers and source order;
- Kotlin Fast Pack writer;
- shared outer `.rdpkg` streaming writer with SHA-256 during write and speed-oriented compression.

CI:
- Tests `36342715326` — PASS;
- Android Debug APK `36342715337` — PASS.

This is not a v0.5.51 release and not a phone-accepted converter yet. Full Section IR / Runtime IR parity and end-user flow wiring remain pending.
### v0.5.51 live boundary update — 2026-09-28

The native converter branch has advanced beyond the earlier Section IR checkpoint. Current head `cc07797bc9f25bc6543da8d5386928a8cd37997a` has green Tests (`36345630397`) and Android Debug APK (`36345630361`).

The Kotlin-native core path is now assembled through:
raw SAF → private normalized staging → volume/index → section index → Section IR v2 → Runtime IR shards/index/coverage → Fast Pack → manifests → streamed `.rdpkg`.

The remaining boundary is user-facing/lifecycle integration plus real NT8340A parity on phone. Do not claim v0.5.51 release/PHONE PASS before that test.


### v0.5.51 lifecycle-safe native .rdpkg integration — GREEN

Checkpoint:
`83cfda448e20ad946704c6b05efb5adce1a38817`.

The Kotlin-native raw-folder engine is now reachable from ProjectActivity through a lifecycle-safe foreground service. Run state is persisted, rotation/recreate reattaches, PREPARING can be cancelled, generated packages are validated/imported automatically, and successful imports are upserted into the originating Renault project.

CI:
- Tests `36357699673` — PASS;
- Android Debug APK `36357699651` — PASS.

The only remaining release gate is the v0.5.51 candidate build plus real-phone NT8340A parity test.


### v0.5.51 phone candidate frozen

Candidate version `0.5.51` / code `67`.

Validated combination:
- runtime source `2adab113bbfd48607446310a2f2daa4ce5f3d354`;
- branch validation source `912fd52ef32613d5d2d3628382baae4994c38272`;
- Tests `36358146811` PASS;
- Android Debug APK `36357861552` PASS;
- artifact id `10944810990`;
- APK SHA-256 `c0b2ec950f6313d1befbc53b9bd6d4878737af5baf787f615aed4355fb1e3d4c`.

Only tests/docs differ between runtime source and validation source.

Status remains `CI_PASS_PHONE_PENDING`; the next evidence must come from raw NT8340A on the real phone.


### Durable chat-recovery workflow

`docs/assistant-kit/CURRENT_PLAN.md` is the first recovery file for active work after a chat/session interruption.

Rule:
- write the current implementation plan there before/while starting a multi-step stage;
- immediately mark completed items `[x]`;
- attach commit/CI/phone evidence when available;
- keep exactly one concrete “current next step”;
- do not rely on chat-only state for active implementation progress.

`CURRENT_HANDOFF.md` remains the broader historical/state handoff; `CURRENT_PLAN.md` is the short live TODO/checkpoint.


### v0.5.51 real-phone native NT8340A evidence — 2026-09-28

The Android/Kotlin raw-folder pipeline successfully produced and imported a real NT8340A package on phone.

Accepted evidence:
- `NT8340A · 2006-04-18`;
- 347 native sections;
- Fast Pack 6138 files;
- outer package 8012 files;
- SHA-256 `7d6cc758b329adcd1ef46680de4d9c6815ff48b053f4e3550b06dd92b229b77e`;
- project volume count stayed at 2, so existing NT8340A was updated rather than duplicated;
- reopened Modern screen shows `Розділів: 347 · native`;
- no compatibility fallback;
- Classic remains an explicit alternate mode.

Full v0.5.51 PHONE PASS is not closed yet because the lifecycle stale-request replay fix still requires phone verification and the no-public-`*_android` storage check remains open.


### v0.5.51 replay-fix phone rerun

The one-shot native request-id repair was exercised on a full NT8340A phone conversion.

Observed single-run path:
- source files copied: `7653`;
- Runtime IR sections: `347`;
- Fast Pack files: `6138`;
- outer package files: `8012`;
- importer completed;
- SHA-256:
  `7d6cc758b329adcd1ef46680de4d9c6815ff48b053f4e3550b06dd92b229b77e`.

The submitted screenshot sequence has no stale second start. The SHA matches the previous successful run exactly, giving repeatability/determinism evidence for the generated package.

Formal lifecycle PASS awaits user confirmation that rotation/background/window handoffs were performed during this exact run. Public `*_android` absence also remains to be verified.


### v0.5.51 post-completion replay regression closed

Real-phone verification on the request-id guarded candidate confirms that after successful completion, rotation/app exit/window switching no longer replays the old native conversion request.

This closes the specific stale-result replay bug observed earlier. Active-run lifecycle continuity remains a separate final phone sub-gate.


### v0.5.51 cancellation and storage-root observation

Real phone:
- cancel during active native PREPARING succeeded;
- raw source remained unchanged;
- private staging cleanup succeeded.

`Documents/Renault` still contains historical public `*_android` datasets from older workflows. The Kotlin-native NT8340A flow did not visibly create a new NT8340A-specific `*_android` folder. Storage hygiene/legacy cleanup should be audited separately and must not delete datasets without verifying they are no longer referenced.


### Planned post-v0.5.51 UX/storage cleanup

Two follow-ups were accepted during phone validation:

1. Terminal native-run status on Project screen should be dismissible.
   - right-side close icon for COMPLETE/CANCELLED/FAILED only;
   - never during active PREPARING/IMPORTING;
   - dismisses presentation state only, not package/project/source data;
   - dismissal should remain stable across Activity recreation.

2. Historical `Documents/Renault/*_android` folders need a read-only provenance/reference audit before cleanup.
   - classify `KEEP / LEGACY / SAFE TO REMOVE`;
   - prefer a Termux menu audit workflow;
   - no blind deletion.

Also fix Ukrainian singular progress wording: `1 файл`, not `1 файлів`.


### v0.5.51 PHONE PASS / raw-to-rdpkg closed

Real-phone acceptance is complete for the Android/Kotlin raw Renault volume → `.rdpkg` path.

Reference:
`NT8340A · 2006-04-18`.

Accepted evidence:
- source files `7653`;
- Runtime IR sections `347`;
- Fast Pack files `6138`;
- outer package files `8012`;
- stable SHA-256 across successful reruns:
  `7d6cc758b329adcd1ef46680de4d9c6815ff48b053f4e3550b06dd92b229b77e`;
- generated package validates/imports automatically;
- existing volume is updated without duplication;
- reopened Modern is `347 · native`;
- no compatibility fallback;
- active and post-completion lifecycle handoffs no longer replay/restart conversion;
- PREPARING cancellation leaves raw source unchanged and cleans private staging;
- no new public `*_android` intermediate is produced by the accepted Kotlin-native flow.

Historical `Documents/Renault/*_android` folders remain outside this acceptance result and require separate provenance/reference audit before deletion.

Status:
**v0.5.51 PHONE PASS — 2026-09-28.**


### v0.5.51 merged to public main

Public PR #1 was merged as:
`921e87f728a222e8f388a01ad989b082a7bd8894`.

The release remains PHONE PASS for the Kotlin-native raw NT8340A → `.rdpkg` behavior.

Post-merge distribution work is limited to development signer continuity:
- accepted cert SHA-256:
  `dd588fbb3093a047f81c24397fc6d2ab7f5a32040425a21dd24506cce59a9802`;
- verify/restore the same secure signer for public-main builds;
- then prove a public-main v0.5.51 APK updates the existing installed app without uninstall/data reset.

Do not conflate this signing/distribution check with app runtime QA, which is already PHONE PASS.


### Renault menu project/build status

Termux Renault menu item `19 — Статус проєкту / build` is the user-facing read-only status check.

It shows:
- current Renault version / versionCode;
- branch and local commit;
- canonical CURRENT_PLAN project status and next step;
- RELEASE_META QA/distribution status and expected signer;
- latest Android Debug APK run;
- latest Tests run;
- whether Android/build files changed after the latest Android build.

Merged as `b0073f723e28c17622efbd3ad29c0a05d0193d2b`.


### Accepted signer restore workflow

Renault Menu now exposes:
`20 — Відновити accepted signer + build`.

The helper uses the private historical archive as the only source of the previously accepted development signer, verifies both keystore and certificate identity, updates public GitHub Actions Secrets without printing secret values, and then triggers an exact public-main build.

Merged helper:
`239663c4577c2e4d279a4052f1b0ac460b19bf9d`.

This replaces manual key handling for the migration checkpoint.


### v0.5.51 signer continuity verified

Public-main signing continuity was restored successfully.

Evidence:
- trusted build run `36603132862` PASS;
- public-main commit `fc7ffc6a7deda7074e5d6b2e631380fd923401c0`;
- signer SHA-1 `4102350e2787fd538bbf58a219293a132235e618`;
- accepted signer SHA-256 verified: `dd588fbb3093a047f81c24397fc6d2ab7f5a32040425a21dd24506cce59a9802`;
- APK SHA-256 `7b51c30cdd0be50de48ff8ddc21c6ee85f33c00eaa9c93adacf22fec4ef5ab48`, exactly matching the phone-accepted v0.5.51 candidate;
- artifact id `11050306597`.

Only the final in-place installation/data-preservation phone check remains for distribution closeout.


### v0.5.51 distribution closeout complete

The signer-continuity migration is complete and verified on phone.

Final acceptance:
- exact trusted public-main APK installed in-place over the existing app;
- no uninstall/data reset;
- app reopened as `v0.5.51`;
- Megane II remained present with `Томів: 2`;
- existing project state was preserved.

Status:
**v0.5.51 READY / CLOSED — 2026-09-29**.


### v0.5.52 terminal status / live progress phone acceptance

Final accepted source:
`8a364ef9e50d0bd81273e9d419321284abdfed03`.

Real-phone acceptance on 2026-09-30:
- candidate installed in-place over the existing app with data preserved;
- app opened as v0.5.52;
- Megane II remained at 2 volumes; Laguna II and Kangoo II remained present;
- terminal COMPLETE/CANCELLED/FAILED presentation has a right-side `×`;
- terminal dismissal survives repeated rotation and leaving/reopening the project;
- active PREPARING has no terminal `×`;
- PREPARING cancellation returns to terminal CANCELLED state;
- the open “Підготовка .rdpkg виконується” dialog now follows real persisted progress live instead of showing a frozen snapshot;
- Ukrainian file-count forms are covered by JVM/contract tests.

Final candidate validation:
- Tests `36639849368` — PASS;
- Android PR Check `36639849222` — PASS;
- trusted candidate build `36640408798` — PASS;
- APK SHA-256 `2a2fc9c952ac24c11b3022786b1f6e1eccd98c5a595532317f15f080d01797aa`;
- stable signer SHA-1 `4102350e2787fd538bbf58a219293a132235e618`.

Status:
**v0.5.52 PHONE PASS; public-main distribution verification remains.**

Next product work after release closeout:
read-only provenance/reference audit of historical `Documents/Renault/*_android` folders before any deletion.


### v0.5.52 public-main distribution closeout

PR #6 was squash-merged to public `main` as:
`31c26e9c24abab6fb491f8aa9290c5395112a2ca`.

Public-main validation:
- Tests `36642605889` — PASS;
- Android Debug `36642605895` — PASS;
- artifact id `11067202339`;
- stable signer SHA-1 `4102350e2787fd538bbf58a219293a132235e618`;
- accepted signer certificate SHA-256 verified by the trusted workflow;
- APK SHA-256 `2a2fc9c952ac24c11b3022786b1f6e1eccd98c5a595532317f15f080d01797aa`.

The public-main APK is byte-for-byte identical to the final candidate already installed and phone-accepted on 2026-09-30. A separate reinstall was intentionally skipped because it would not exercise a different binary.

Status:
**v0.5.52 READY / CLOSED — 2026-09-30.**

Next work:
read-only provenance/reference audit of historical public `Documents/Renault/*_android` folders. Delete nothing automatically.


### Read-only legacy `*_android` audit tooling

A dedicated Termux helper was added for the next storage-hygiene stage.

Contract:
- scan only top-level `Documents/Renault/*_android` folders;
- report path, size, file count, modified date, dataset/.nomedia markers and repository references;
- configured active `build_root` is `KEEP`;
- all other unknown folders default conservatively to `LEGACY`;
- `LEGACY` explicitly does not mean `SAFE TO REMOVE`;
- no automatic deletion/move/rename;
- Termux cannot by itself prove absence of app-private/SAF references, so actual deletion decisions require a second review of the real phone output.

User-facing entry:
`21 — Аудит legacy *_android (read-only)`.

Validation:
Tests run `36643121485` — PASS.

Next gate:
run item 21 on the real phone and classify each returned folder. Delete nothing before that review.


### Real-phone legacy `*_android` audit result

Read-only Termux item 21 was run on the real phone.

Observed:
- `Megane II_NT8342A_android`: 405M, 7855 files, dataset manifest + `.nomedia`, historical prepared NT8342A source;
- `Megane II_android`: 400M, 7664 files, dataset manifest + `.nomedia`, historical v0.5.37 single-volume Megane output used for the legacy NT8340A path;
- `laguna 2 2001-2006_android`: 356M, 19836 files, dataset manifest, configured active `build_root`.

Current classification:
- Laguna output: **KEEP**;
- NT8342A prepared folder: **KEEP until NT8342A is migrated to app-private .rdpkg**;
- old Megane II output: **legacy / safe-to-remove candidate**, because current NT8340A was later upserted from a successful `.rdpkg` import.

Code contract relevant to cleanup:
`RdpkgImporter.install()` extracts the package into app-private `noBackupFilesDir/rdpkg/<packageId>`, exposes it through `LocalDatasetDocumentsProvider`, and the project volume record stores that app-owned tree URI. A successfully upserted package volume therefore no longer needs its historical public `*_android` folder for runtime access.

Next:
build/import NT8342A as `.rdpkg`, verify both Megane volumes reopen, then use reversible quarantine before any permanent deletion.


### Megane II package migration completed

Real-phone result:
- NT8342A single-volume package created as `Megane-II_NT8342A_2006-10-09.rdpkg`;
- package SHA-256: `d76e55a8c7da29244ac01517526fea3e3b9fb118f0f45249a400e708fddf69aa`;
- package payload files: `8204`;
- import updated the existing NT8342A volume instead of adding a duplicate;
- Megane II remained at exactly 2 volumes;
- NT8340A reopen PASS;
- NT8342A reopen PASS.

Both Megane project volumes are now verified after app-private package migration. The next cleanup gate is reversible quarantine of the historical public Megane `*_android` folders, one at a time, with reopen verification after each rename. Permanent deletion remains blocked until quarantine PASS.


### Megane II first quarantine gate PASS

Real-phone reversible quarantine test:
- `Megane II_android` was renamed out of its original public-storage path;
- NT8340A reopen PASS;
- NT8342A reopen PASS.

This proves the current project no longer requires `Megane II_android` at runtime. Permanent deletion is still deferred until the second Megane folder also passes quarantine.

Next:
keep the first folder quarantined, quarantine `Megane II_NT8342A_android`, then re-open both volumes.


### Megane II second quarantine gate PASS

Real-phone reversible quarantine test:
- `Megane II_android` remained renamed out of its historical path;
- `Megane II_NT8342A_android` was also renamed out of its historical path;
- NT8340A reopen PASS;
- NT8342A reopen PASS.

Conclusion:
both historical public Megane `*_android` prepared folders are no longer required by the current runtime and are now classified **SAFE TO REMOVE**.

Keep:
`laguna 2 2001-2006_android`, because it is still the configured active `build_root`.

Expected storage recovered by deleting the two quarantined Megane folders is roughly 805 MB based on the read-only audit sizes (400M + 405M).


### Legacy Megane public-storage cleanup closed

Final real-phone audit after permanent cleanup:
- only `laguna 2 2001-2006_android` remains under top-level `Documents/Renault/*_android`;
- Laguna is the configured active `build_root` and remains **KEEP**;
- final audit summary: `KEEP 1 / LEGACY 0 / SAFE TO REMOVE 0`;
- both old Megane prepared folders were removed only after app-private package migration plus two-step reversible quarantine/reopen PASS;
- approximate recovered storage: 805 MB.

Status:
**legacy Megane storage cleanup CLOSED — 2026-09-30**.


### Dataset link integrity checker foundation

A read-only post-conversion checker was added before enabling any blocking converter gate.

Scope:
- HTML local references: href/src/background/action/data/poster;
- CSS url(...) and quoted @import;
- selected path-bearing dataset manifest fields;
- query/fragment stripping before filesystem resolution;
- external/data/javascript/mail/tel references ignored;
- JSON report plus nonzero exit code when missing local targets are found;
- no dataset mutation.

User-facing entry:
`22 — Перевірити посилання dataset (read-only)`.

Validation:
Tests `36651746426` — PASS.

Next gate:
run item 22 against the real active Laguna dataset and inspect the baseline before deciding which legacy references are true failures and whether the checker can become a blocking post-conversion gate.


### Laguna link-check baseline found 7 absent volume targets

First real-phone run of Termux item 22:
- scanned 14417 HTML/CSS files;
- checked 77449 local references;
- found 14 missing targets;
- 0 outside-root skips.

The 14 misses are not random:
each of 7 root-catalog volume entries contributes exactly two missing targets:
the volume's `INDEX.HTM` and its Russian flag GIF.

The affected volume folders correspond to:
NT8183A, NT8218A, NT8240A, NT8254A, NT8307A, NT8327A and NT8328A.

Current Laguna Runtime IR coverage is based on 10 volumes / 2174 sections, while the historical original archive audit had 61759 files and the current build-root storage audit shows only 19836 files.

Because root `INDEX.HTM` remains a Classic/browser entrypoint, this is treated as a real data-integrity finding rather than an ignored warning.

Next:
item 22 will compare top-level volume inventory between configured `source_root` and `build_root`. No dataset mutation until that comparison is known.


### Laguna partial-build root cause confirmed

Real-phone source/build parity:
- configured source has 10 top-level volumes;
- current prepared build has 3;
- exactly 7 source volumes are missing in build.

Historical v0.5.19 instructions explicitly allowed temporarily moving 7 converted volume folders outside `laguna 2 2001-2006_android`, leaving 3 test volumes for faster package regeneration, with a mandatory later restore of all volumes.

That restore was not completed. This exactly explains the present state.

The current 14 missing root-catalog links are seven pairs:
- missing volume `INDEX.HTM`;
- missing flag asset under the same absent volume root.

Therefore the primary problem is whole-folder absence, not flag naming or language cleanup.

Repair is non-destructive fast-converter `--merge`: copy only missing source volume roots, normalize only those new roots, rebuild package metadata/Runtime IR/Fast Pack, then rerun the read-only link/parity gate.


### Laguna hold-folder location confirmed

User confirmed the seven prepared Classic volume folders removed for the reduced v0.5.19 test were not deleted and do not need reconversion.

They are preserved under:
`/storage/emulated/0/Documents/Renault/_volumes_hold`

Repair direction is therefore corrected:
1. verify the hold folder contains the exact 7 volumes reported missing by source/build parity;
2. move those already-prepared volume folders back into `laguna 2 2001-2006_android`;
3. rerun read-only item 22;
4. only rebuild package metadata/Runtime IR if the restored physical Classic tree and generated package metadata prove inconsistent afterward.

Do not run fast-converter MERGE unless the preserved hold copy is incomplete or invalid.


### Laguna dataset integrity restored

The seven prepared Classic volume folders preserved under `_volumes_hold` were restored into the Laguna build root.

Final real-phone item 22:
- 48307 HTML/CSS files scanned;
- 264779 local references checked;
- 0 missing targets;
- 0 outside-root skips;
- source volumes 10;
- build volumes 10;
- missing volumes 0;
- extra volumes 0;
- link integrity PASS;
- volume parity PASS.

DATA-001 is CLOSED.

No residual flag/language-cleanup issue remains in the prepared Laguna dataset.


### Laguna II all-volume .rdpkg batch

The full Laguna II prepared dataset is now link-clean and volume-parity PASS at 10/10 volumes.

Packaging direction:
- keep the established distribution contract `one prepared volume = one .rdpkg`;
- create 10 separate portable packages, not one multi-volume archive.

Termux item 15 is extended with:
`A — Усі томи окремими .rdpkg`.

The batch path reuses the existing validated single-volume builder for every volume and produces a JSON batch report with per-package SHA-256, size and payload count.


Batch implementation closeout:
- PR #11 merged as `3c5a7d6d75ef3cb357b3d11602977678dd66dabd`;
- Tests `36656945889` — PASS;
- phone action: update main, open item 15, choose Laguna dataset, then `A — Усі томи окремими .rdpkg`;
- expected output: 10 independent packages plus one JSON batch report.


### Laguna II 10-volume .rdpkg batch PASS

Real-phone batch packaging completed successfully.

Output:
- package count: 10;
- output directory: `/storage/emulated/0/Documents/Renault/packages/rdpkg`;
- batch report: `renault-laguna-ii-x74-2001-2006-rdpkg-batch.json`.

Packages:
- NT8183A · 2001-01-22 · SHA-256 `decd71b01d017d01f6cd45968585482874114b65c00e2231ba88941afb0046e3`;
- NT8218A · 2002-05-01 · SHA-256 `1177d44f3b46c2d00b40a7a6a20bdb694be621a6a668f9100245bce5a84d10a4`;
- NT8236A · 2002-11-18 · SHA-256 `d87f1d862ad8289fdf7ba40306c6826b6446726fab5b980154f017704251aff5`;
- NT8240A · 2003-11-17 · SHA-256 `ed1362814e17422984d3ce6268b62f997da119269383da8cabe029e03977107a`;
- NT8254A · 2004-06-21 · SHA-256 `485316cd6f7902c421e3efbb88c3cc75143193752a9c01e58684c67503624cd3`;
- NT8282A · 2005-04-22 · SHA-256 `54e1559cf7467b86355e25b744dda0f53ed2f0117c0615a17e108968355ae8f9`;
- NT8283A · 2005-08-29 · SHA-256 `f77d6e603b388a6b09dabcf1b9589b82138be2ac627fd26b49ccb9aefccea008`;
- NT8307A · 2005-12-12 · SHA-256 `4cdbbcc17764960e58fcec0609a8a19286aea1a72fef29fbb5400f2b31eb8def`;
- NT8327A · 2006-02-06 · SHA-256 `58bc20dee56e327563c4be0fd602af25e6583bdf10c8a0db874a5e9ab6d1e2a5`;
- NT8328A · 2006-05-09 · SHA-256 `c647cbe29784afc3cfd77e408cc350ab3e13561763248012aba19fc900711c14`.

Status:
**Laguna II portable package preparation PASS — 10/10 volumes ready.**

### v0.5.53 natural Modern section display order

Real-phone finding from imported Laguna II NT8183A:
- package import/open PASS;
- volume opens as `NT8183A · 2001-01-22` / `383 · native`;
- Modern section tiles are difficult to scan because the accepted BUG-004 data contract preserves raw Classic source order.

v0.5.53 keeps that source-order contract intact and adds UI-only stable natural sorting in `ModernVolumeActivity`.

Expected display behavior:
- numeric runs compare numerically;
- suffix variants remain grouped;
- opaque alphanumeric IDs remain supported;
- duplicate codes remain distinct and keep relative source order;
- no `.rdpkg` regeneration required.

### v0.5.53 phone sort PASS

Final refined candidate phone result:
- Laguna II NT8183A remains available and opens in native mode;
- Modern list ordering accepted by user;
- accepted order is numeric-leading section IDs first, then `R...` connector IDs, then remaining alphabetic IDs;
- numeric natural ordering remains correct;
- no `.rdpkg` regeneration required.

CI on final PR head:
- Tests `36660615811` — PASS;
- Android PR Check `36660615926` — PASS.

v0.5.53 merge:
- PR #12 merged as `68323e66c67f07df0e770d99ba7f7b2aa3d95b0f`;
- final PR-head Tests `36661693096` — PASS;
- final PR-head Android PR Check `36661693082` — PASS;
- phone ordering PASS before merge.

### Laguna II edge package import PASS

Real-phone package validation:
- oldest batch volume `NT8183A · 2001-01-22` — PASS;
- newest batch volume `NT8328A · 2006-05-09` — PASS.

Both packages imported into Laguna II, opened in native mode, and representative section navigation worked.

Conclusion:
the 10-volume batch has representative compatibility coverage at both ends of the Laguna II date range; no `.rdpkg` rebuild is required before importing the remaining eight packages.


### Laguna II remaining package import

User reports the remaining eight Laguna II `.rdpkg` packages were added after the edge-package checks.

Final exact 10-volume / no-duplicate screen sanity remains part of the next phone gate.

### v0.5.54 UI/lifecycle audit implementation

User-requested UX:
- Home add subtitle becomes `До проєкту`;
- empty project becomes `Порожній · додай том`;
- Project add actions become one `Додати` tile with `Авто` and `Вручну`.

Lifecycle work:
- shared `LifecycleHelpDialogController`;
- contextual Help IDs survive rotation and reopen above the same Activity;
- Help restoration never starts the underlying action;
- CreateProject typed name survives rotation;
- Project volume action/removal/multi-volume/mismatch dialogs restore presentation state without automatic mutation;
- pending manual import mode survives recreation.

Help surfaces:
Home, Project, Add, raw builder, Converter, Modern volume, Native section, Volume documentation.

Audit finding kept open:
`RISK-LIFE-001` — direct `.rdpkg` import uses an Activity-owned Thread and should move to a durable service/run-store lifecycle.


Final v0.5.54 PR-head CI:
- Tests `36664711140` — PASS;
- Android PR Check `36664711278` — PASS;
- PR #13 remains open for phone acceptance.


### Overnight checkpoint — v0.5.54 phone QA pending

Session stopped at 2026-09-30 06:42 +03:00.

State:
- PR #13 is open and mergeable;
- v0.5.54 implementation complete for the planned UX/Help/lifecycle scope;
- final code-bearing PR head before checkpoint docs: `5660b6f29f24d91c62abd16d1824a96b6b121c36`;
- Tests `36664899562` PASS;
- Android PR Check `36664899564` PASS;
- no merge yet;
- phone candidate not installed/accepted yet.

Resume:
Menu 5 → Menu 16 → PR #13 → install candidate → visual gate → Help/dialog rotation gate → merge only after phone PASS.


v0.5.54 visual refinement after first phone screenshots:
- unified Add tile kept;
- `Авто` now shows `.rdpkg · один том`;
- `Вручну` now shows `Папка / SAF`;
- Help dialogs use Renault Docs dark surface/border/accent styling instead of default system gray.


v0.5.54 phone typography refinement:
- introduced shared compact action subtitle token `Ui.actionSubtitleSp = 11sp`;
- applied to Home and Project action-card secondary labels;
- avoids forced/manual wrapping of `.rdpkg · один том` while keeping titles unchanged.


Settings typography refinement from phone review:
- descriptions → 12sp shared secondary token;
- selected values → 13sp;
- small labels/warnings → 11sp;
- Backup buttons → 14sp;
- `Backup` section localized to `Резервні копії`.


Settings backup button refinement from phone review:
- Backup action button text reduced to 12sp;
- kept a practical tap height while reducing visual weight;
- replaced default gray Android Button background with Renault Docs themed surfaces;
- primary folder action uses accent border/text;
- reset action uses neutral border/text.


### Full v0.5.54 app-dialog consolidation

After phone screenshots exposed remaining system-gray dialogs, all app-owned AlertDialogs were audited.

Inventory:
- Help controller: 1;
- Settings: 3;
- Project: 5;
- total: 9.

All 9 now use shared `DialogUi` with explicit roles (HELP / CHOICE / CONFIRM / DANGER / PROGRESS).

The Settings bug where styling had been applied repeatedly to only one chooser is removed; each of the three Settings choosers now receives the shared theme exactly once.

Home tool cards also replace the folder word with `ic_folder`:
- Converter: `+ стару Renault` + icon;
- Legacy: `+ готова` + icon.

External Android SAF/DocumentsUI is not app-owned and cannot be themed by Renault Docs.


### v0.5.54 final visual hierarchy pass

Final phone-review package:
- Home Add converted to one parent tile with New Volume / New Project child actions;
- Home Tools converted to one parent tile with Converter / Legacy child actions;
- folder icon enlarged and right-aligned inside tool child actions;
- entity titles across project/volume/dataset/native surfaces use shared `Ui.entityTitle`;
- shared `Ui.actionButton` replaces audited gray app-owned buttons;
- Modern dataset/volume search fields receive accent outline for readability;
- Modern active mode is visually highlighted;
- Create Project form and action are themed;
- all changes remain in PR #13 pending one final phone regression.


### v0.5.54 header and mode polish

Phone-review follow-up:
- tool folder icons moved to bottom-right overlay;
- tool titles no longer lose width to the icon;
- one shared `Ui.modeButton` now styles Classic/Modern controls;
- ModernDataset, ModernVolume, NativeSection and Viewer use the shared mode control;
- Viewer toolbar titles reduced to 16sp and use entity-title color for long titles;
- legacy `Як користуватись` pages explicitly warn that their volume count belongs to the opened Classic dataset, not the current project.


### v0.5.54 converter + stale legacy phone findings

Phone screenshots found:
- Converter actions still rendered as default gray Android Buttons;
- stale `Старі бібліотеки` records could enter Modern after the old SAF folder had been moved/deleted, producing raw `renault-dataset.json` read failure.

Resolution:
- Converter action buttons use shared Renault Docs styling;
- stale legacy records are validated before navigation;
- inaccessible records stay on Home with a clear re-add-via-Legacy message;
- Modern direct stale-path failure now uses the same clear explanation rather than exposing the raw manifest error.


### New-chat handoff 2026-10-01

Checkpoint saved for v0.5.54 / PR #13.

- PR remains open and must not be merged yet.
- Android PR Check `36790097812` passed.
- Tests `36790097841` failed on one stale contract assertion in `test_v0513_modern_section_tiles_contract.py` after the shared `Ui.modeButton` refactor; runtime/phone failure is not indicated by this test.
- Latest phone-driven fixes before handoff: Converter button theming, stale Legacy SAF pre-open validation, clearer stale Modern failure, bottom-right Home tool folder icons, shared Classic/Modern switch styling, legacy help-page relevance note.
- Resume by fixing only that stale test expectation, rerunning CI, then installing one fresh PR #13 candidate for the full phone regression.


### v0.5.54 stale contract-test recovery — 2026-10-01

The remaining PR #13 CI failure was confirmed as a stale source-format assertion, not a runtime defect.

Change:
- `tests/test_v0513_modern_section_tiles_contract.py` now validates the shared `Ui.modeButton` contract with whitespace-tolerant regexes for Modern(active) and Classic(inactive);
- no Android runtime file changed in the fix commit.

Evidence:
- fix commit `c3073d44f381c8160829abdc6cee707353711b32`;
- Tests `36794236208` PASS;
- Android PR Check `36794236168` PASS.

Release state:
PR #13 remains open/not merged. One fresh candidate and the full v0.5.54 phone regression are still required before merge.


### v0.5.54 Classic help-page mobile layout — 2026-10-01

Real-phone screenshot showed that legacy `README_UA.html` is visually oversized and inefficient on a narrow screen.

Resolution:
- existing package HTML is adapted at runtime in `SafDatasetWebViewClient`, so already imported Laguna II data changes presentation without package regeneration;
- Viewer toolbar uses dataset identity on info/help pages and places a trailing `YYYY–YYYY` range on line 2;
- the Classic scope note is shortened;
- the help page uses smaller headings, a compact catalog action and a wrapping two-column `Файл | Призначення` table;
- stale `Android` explanatory copy is shortened to current Renault Docs behavior;
- both Python and Android package writers emit the compact layout for future datasets.

Implementation commit:
`24a99325157b1871b9e3d8dd53e7253f046bc237`.

CI:
Tests `36797661478` PASS; Android PR Check `36797661499` PASS.

This is presentation-only. Dataset schema, Runtime IR and current Laguna II `.rdpkg` payloads are unchanged.


### Classic help page first-attempt failure and deterministic replacement — 2026-10-01

The first v0.5.54 mobile-help implementation was rejected by real-phone evidence.

Observed:
- app-owned Viewer toolbar reflected new code;
- `_renault/README_UA.html` still rendered the legacy body unchanged;
- post-load JavaScript adaptation was therefore not reliable enough for this screen.

Final implementation contract:
- README requests are intercepted before generic Fast Pack delivery;
- already-imported legacy README files are replaced by an app-rendered compact HTML response;
- no package rebuild/reimport is needed;
- the response contains smaller typography, compact catalog action, two-column file table, current Renault Docs copy and concise warning;
- toolbar gets its identity from the README page title and formats `Laguna II` / `2001–2006`;
- `rdhelp=compact-v2` prevents the previous 24-hour Fast Pack/WebView cache entry from masking the fix.

Commits:
`874648c5ea851926f5f5443e15688948a6c87ac8` + `d36dd3b8343d514733ec88b14235c7f38a956968`.

CI:
Tests `36811029640` PASS; Android PR Check `36811029609` PASS.

Status:
phone recheck required; PR #13 remains unmerged.


### Laguna dataset metadata parity final PASS — 2026-10-01

After the seven preserved prepared volumes had been restored, the physical build root contained all 10 Laguna II volumes, but the old package metadata still described only 3. That stale metadata explained why `Старі бібліотеки` continued to display `Томів: 3`.

Package generation was rerun against the restored build root. Final real-phone integrity evidence:

- HTML/CSS scanned: 48307;
- local references checked: 264793;
- missing: 0;
- live build volumes: 10;
- manifest volumes: 10 / PASS;
- `_renault/volumes.json`: 10 / PASS;
- `_renault/modern-index.json`: 10 / PASS;
- source/build parity: 10/10;
- missing/extra: 0/0;
- overall: **PASS**.

Report:
`/storage/emulated/0/Documents/Renault/reports/dataset-links-20261001-074659.json`.

The integrity checker was hardened in commit
`82491c108f5a75352edd6466d27c0f9f0b475114`
so a future state such as “10 live folders but 3 manifest volumes” is a FAIL rather than a false PASS.

CI:
Tests `36813567420` PASS;
Android PR Check `36813567450` PASS.

Architecture clarification recorded from phone investigation:
legacy `*_android` datasets and Project-installed `.rdpkg` volumes are independent storage flows. Project Classic reads the app-private installed package copy via `LocalDatasetDocumentsProvider`; Legacy Classic reads the SAF-linked public dataset folder.


### v0.5.57 Project/Legacy separation and quarantine — 2026-10-04

Decision:
- ProjectStore + app-private installed .rdpkg volumes are canonical.
- DatasetStore remains compatibility metadata only.
- A populated current project suppresses the matching Legacy dataset tile on Home without deleting the legacy record.
- Public top-level *_android outputs move to Documents/Renault/legacy-quarantine rather than being deleted.
- Termux item 23 provides plan / MOVE / RESTORE and records manifest.tsv.
- Laguna build_root follows the quarantined prepared dataset path; raw Laguna source stays in place.

Code evidence before docs update:
- exact HEAD 5598e96d818058370b0e7fed3af3afcee81a34c4;
- Tests 37214251297 PASS;
- Android PR Check 37214251232 PASS.

Phone quarantine and post-move reopen verification remain pending.


## 2026-10-07 — v0.5.68 baseline and Archive Intake handoff

Recorded baseline:
- v0.5.68 / build 84;
- main SHA `41e6801b985a344922169b8e8e2b60b535f7b60e`;
- main Tests #481 PASS;
- signed Android Debug APK #131 PASS.
- branded foreground notification icon is installed for phone visual QA.

New development item:
- issue #51: direct archive intake for old Renault documentation;
- target UX: ZIP / 7Z / RAR → safe private extraction → Renault raw detection → existing native .rdpkg builder/importer;
- source archives remain untouched;
- archive extraction must be traversal-safe and lifecycle-safe;
- long-running archive work must follow the v0.5.65+ foreground/background contract.

This is the canonical continuation after v0.5.68. Catalog rolling issue #30 remains separate and open.


## 2026-10-07 — v0.5.69 archive intake sleep checkpoint

Feature work is preserved on draft PR #53 / `feat/v0.5.69-archive-intake`, head `8de99f551ad56e6ec61b9a2372514217dc08cb7c`.

Validated at checkpoint:
- Tests #497 PASS;
- Android PR Check #395 PASS;
- archive formats ZIP/7Z/RAR;
- safe private staging/extraction;
- reuse of native raw→.rdpkg engine;
- persisted archive source kind;
- installed-volume duplicate preflight before conversion;
- terminal `ALREADY_PRESENT` result for exact duplicates.

Release remains blocked on explicit multi-volume chooser, feature-branch sync with latest main/Termux fixes, lifecycle/cleanup acceptance, and real-device archive QA.

Normal phone delivery remains `5 → 19 → 8 → 13`.

## 2026-10-07 — v0.5.69 Archive Intake merged; phone gate starts with ZIP

PR #53 merged to main as `53f4208b4b2e4d27e4bb55094c855eaf13fb2442`.

Release evidence:
- v0.5.69 / build 85;
- feature head `f9f11e4a3770b4d098a134217f32c036cfe21613`;
- PR Tests #525 PASS;
- Android PR Check #423 PASS;
- main Tests #526 PASS;
- main Android Debug APK #132 PASS;
- artifact `Renault-Docs-v0.5.69-Debug`, id `11489564391`;
- artifact digest `sha256:328d897a8c484931d08c6e6fa3e00fd718034cc0c7514581a23394d9f2bdcdba`.

Architecture now implemented in code/CI:
- direct SAF archive intake for ZIP/7Z/RAR;
- private safe staging, traversal protection and bounded extraction;
- pre-extraction root inspection and exact-duplicate fast path;
- persisted multi-volume `WAITING_SELECTION` state;
- installed candidates disabled by default;
- explicit one-or-many new-volume selection;
- resume from extracted staging;
- foreground service / wake lock / cancel / process-redelivery;
- stale partial destination cleanup;
- canonical per-volume .rdpkg output into explicit user destination;
- source archive never mutated;
- private staging cleaned on terminal success/cancel/failure.

Phone evidence is still required. First real-device gate is a ZIP archive; issue #51 remains open until phone PASS.

## 2026-10-07 — Collapsible Add panel UX decision

Future UX direction recorded from phone use; **not part of the current v0.5.69 implementation/QA wave**.

Project screen:
- consolidate `Авто`, `Вручну`, raw→.rdpkg and archive→.rdpkg under one compact `Додати` panel;
- default compact presentation is a collapsed `Додати` header with Help `?`;
- tap toggles expansion;
- expanded state can be pinned so the user can choose persistent expanded vs compact/collapsed presentation;
- move the current tap/long-press instructional paragraph into Add Help/panel content to reclaim vertical space;
- keep all existing workflows/semantics unchanged.

Home:
- later evaluate the same collapsible/pinnable panel pattern for the main project/action area.

Priority rule:
finish v0.5.69 Archive Intake phone acceptance first. The immediate phone test is an archive whose volume is already installed, to verify exact-duplicate rejection/skip without creating a second volume.

## 2026-10-07 — Archive duplicate fast-path + NT list ordering decision

Real-phone v0.5.69 evidence:
- duplicate archive for `NT8266A · 2004-06-28` eventually ended in `Том уже є · Конвертацію пропущено`;
- however the app visibly extracted the ZIP first (`3732 / 4378` files observed).

Follow-up contract:
- improve Archive Intake preflight so a unique Renault NT identity can be discovered from archive filename/path and, where required, a bounded direct read of small index/metadata entries without extracting the full archive;
- full extraction remains the safe fallback when identity is not provable;
- do not trade correctness for speed on ambiguous archives.

Project-volume presentation decision:
- support natural NT-number sorting;
- smaller NT first is the preferred default, with reverse order available;
- sorting is UI-only and does not change opaque Renault/package identity;
- add display toggles so date, vehicle codes and document type/version can be hidden when a more compact list is desired.

## 2026-10-07 — Assistant response / Renault Menu communication contract

Project-wide communication rule:
- assistant replies contain the substantive answer first;
- every actionable reply ends with a short explicit `Від тебе зараз` block;
- phone-side user work should be routed through the Renault Termux menu whenever a menu action exists;
- raw shell/Git commands are reserved for diagnostics or missing menu capabilities;
- phone QA replies should record evidence/checkpoints, then give the next exact phone action instead of burying it in long prose.

Canonical contract:
`docs/assistant-kit/RESPONSE_CONTRACT.md`.

## 2026-10-07 — v0.5.70 archive duplicate fast-path merged

Real-phone v0.5.69 finding PERF-ARCHIVE-001 led to v0.5.70 / build 86.

Merged runtime:
`b9c763237b7aceede742c6aa559f53abf4c83444`.

CI:
- PR #63 merged;
- Tests #530 PASS;
- Android Debug APK #133 PASS;
- artifact `Renault-Docs-v0.5.70-Debug`, id `11508378680`;
- digest `sha256:20b4a7ac2b519a6a701ba688e962a8b51238c535d486eae3621e0db566882c2b`.

Contract:
- ZIP duplicate preflight may directly read only a bounded 64 KiB prefix of the raw entrypoint HTML inside the archive;
- root path / single-volume source filename / entrypoint content are strong NT identity hints;
- generic entry-path NT hints are fallback only;
- only an unambiguous unique installed NT match may skip extraction;
- ambiguous identity keeps the old full safe-extraction fallback.

Phone acceptance pending: repeat the same NT8266A ZIP and verify no `Розпаковую ZIP...` phase occurs.

## 2026-10-07 — v0.5.71 top-level archive root fast-path

v0.5.70 phone re-test still showed full extraction for the duplicate NT8266A ZIP.

Root cause:
`RawRootHint.leafName` used `substringAfterLast('/', "")`; a top-level raw root had no slash in its parent path, so the leaf became empty and the hint was discarded.

v0.5.71 / build 87:
- PR #65 merged;
- runtime source `131b4dcef2ed95e35198a8cb03a0ce4322fda64c`;
- Tests #532 PASS;
- Android Debug APK #134 PASS;
- artifact `Renault-Docs-v0.5.71-Debug`, id `11510676847`;
- digest `sha256:47ff40990048cd7e275bdd6677c438996693843b409dae6b65101e3c63230e37`.

Phone re-test pending with the exact same NT8266A ZIP.

UX queue remains unchanged: collapsible/pinnable `Додати` panel is saved and is the next UI follow-up after Archive Intake acceptance.

## 2026-10-07 — Add panel detailed UX contract locked

Real-phone screenshots confirm the current Project add/create area consumes excessive vertical space.

Locked next-UI contract:
- one full-width `Додати` header tile replaces the always-expanded cluster;
- pin + expand/collapse + overall Help live in the header;
- unpinned default is collapsed;
- pin keeps the panel expanded persistently;
- expanded body keeps Auto, Manual, raw→.rdpkg, archive→.rdpkg and every existing per-action Help control;
- tap/long-press volume guidance moves into Add expanded/help content;
- operation status remains outside the collapsible body so running work stays visible;
- rotation preserves transient expansion state; pin persists across reopen;
- Home may later reuse the same pattern after Project-screen acceptance.

Implementation is queued immediately after the current Archive Intake duplicate fast-path phone gate.

## 2026-10-07 — v0.5.72 archive-root duplicate fast-path merged

Input phone evidence:
- `Розпаковую ZIP… 261 / 4378`;
- later `894 / 4378`;
- previous fast-path therefore remained FAIL.

Additional root case fixed:
when INDEX/ACCUEIL is directly at archive root, the relative raw-root path is empty. The preflight now keeps that root hint, derives a safe fallback leaf, preserves bounded NT identity from the entrypoint and may match an installed volume before extraction.

Merged runtime:
`eacbbc1f914d4f35564724501a3edb126fd71963`.

CI:
- Tests #535 PASS;
- Android Debug APK #135 PASS;
- artifact `Renault-Docs-v0.5.72-Debug`, id `11511521715`;
- digest `sha256:c670621c76309294d15c6a1f8335a2e5578fd8c9425314cf592d033a07378c62`.

Phone re-test is required with the exact same NT8266A ZIP.

The compact/pinnable Add panel is now tracked as GitHub issue #68 and remains the next UI task after this archive gate.

## 2026-10-08 — v0.5.72 duplicate fast-path phone PASS

Real-phone video acceptance:
- same duplicate NT8266A archive used for the previous failing runs;
- archive copy/inspection is visible;
- no `Розпаковую ZIP...` counter appears;
- flow ends in `Том уже є`;
- project remains at 9 volumes.

Result:
PERF-ARCHIVE-001 is CLOSED for v0.5.72.

Next runtime task:
issue #68 — compact collapsible/pinnable Project `Додати` panel, target v0.5.73 / build 89.

## 2026-10-08 — v0.5.73 compact Add panel merged

Issue #68 implementation:
- one full-width `Додати` header;
- unpinned default collapsed;
- persistent pin keeps it expanded across reopen;
- transient expanded/collapsed state survives rotation;
- Auto / Manual / raw→.rdpkg / archive→.rdpkg remain inside the expanded body with existing Help semantics;
- volume interaction guidance moved inside Add;
- operation/progress status stays outside the collapsible body.

Merged runtime:
`871d93c6f0416f9b98ea78edbab06a9ddcce8eba`.

CI:
- Tests #538 PASS;
- Android Debug APK #136 PASS;
- artifact `Renault-Docs-v0.5.73-Debug`, id `11517210871`;
- digest `sha256:027f0603e84868e883cc826f8706394f3c40106135bbaa87c97e32a63094f36b`.

Phone acceptance pending.

## 2026-10-08 — v0.5.74 Add panel refinement decision

v0.5.73 real-phone screenshots confirm the compact Add panel concept works in both collapsed and expanded states.

Refinement requested before final acceptance:
- loose transient status text must not sit between Add and volume cards;
- transient add/import status moves inside Add as a subtle warm card and disappears when blank;
- volume tap/long-press guidance is removed;
- inactive pin becomes neutral monochrome; pinned pin becomes red;
- chevron becomes heavier using ▲ / ▼;
- active operation/progress stays outside the collapsible body.

Target: v0.5.74 / build 90.

## 2026-10-08 — v0.5.74 Add panel refinement merged

Phone-requested refinement implemented:
- transient add/import messages render inside a subtle warm status card at the bottom of expanded Add;
- blank status is hidden;
- permanent volume interaction guidance removed;
- emoji pin replaced with monochrome vector;
- unpinned pin = neutral gray, pinned pin = red;
- expand/collapse control uses heavier ▲ / ▼;
- active operation/progress remains outside Add.

Merged runtime:
`8dc7a3d0c6d22c50df343a36456f6bd4716b3a5a`.

CI:
- Tests #542 PASS;
- Android Debug APK #137 PASS;
- artifact `Renault-Docs-v0.5.74-Debug`, id `11518058878`;
- digest `sha256:a391f4c16a0ce7f3c7b79a9fd9383e1e0e386f07e7d5268e1b4938eaf657f230`.

Phone acceptance pending.

## 2026-10-08 — v0.5.75 sticky Add panel decision

Real-phone feedback after v0.5.74:
- compact Add is accepted;
- Add should remain fixed while volume cards scroll independently;
- a small gap is needed between Add and first volume card;
- restore the previous diagonal pin form;
- use the same pin silhouette with neutral light tint when unpinned and red tint when pinned;
- status-card appearance will be reviewed later, not in this change.

Target release: v0.5.75 / build 91.

## 2026-10-08 — v0.5.75 sticky Add panel merged

Implemented:
- Add/count area remains fixed;
- only volume cards are inside the Project ScrollView;
- 10dp gap before the first volume card;
- previous diagonal push-pin silhouette restored;
- unpinned tint = light monochrome;
- pinned tint = red;
- Help copy no longer uses a misleading colored emoji;
- status-card visual review remains deferred.

Merged runtime:
`b25efbee254562d7b3e9f6917772e8d2e6370eb3`.

CI:
- Tests #547 PASS;
- Android Debug APK #138 PASS;
- artifact `Renault-Docs-v0.5.75-Debug`, id `11520145404`;
- digest `sha256:59888f89ef694512ffa2a4895a75cb93b21c99687ccaa24270bfa9a06f948f00`.

Phone acceptance pending.

