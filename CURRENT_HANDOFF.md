# Renault Docs — CURRENT HANDOFF

Останнє оновлення: 2026-09-25.

## Stable foundation

Repository: `faric-ua/renault-docs-android`

Main now contains:
- deterministic converter;
- real-phone Laguna II conversion;
- universal dataset/package contract;
- exactly 10 Laguna top-level volumes confirmed on phone;
- generated `_renault/START.html`, `README_UA.html`, `volumes.json`;
- browser local server;
- PDF.js interception;
- continuous vertical PDF viewer;
- Android/system/release contracts;
- first Android v0.1.0 Library/SAF skeleton.

## Real-phone dataset/browser evidence

Dataset/package:
- `renault-dataset.json` exists;
- library scanner detects Laguna II;
- package generator detects exactly 10 volumes.

Browser/PDF:
- PDF opens inside legacy Renault frame;
- Chrome «Відкрити» prompt is no longer required;
- continuous page scroll works;
- hidden error overlay bug was fixed.

## Android v0.1.0 build evidence

PR #14 merged as:
`557e5a08b06f566f392db2371c8122d9588b5970`.

Exact tested feature source:
`0d9465acc44c82e9322f87db901fed4f703fdcb1`.

GitHub Actions:
- Android Debug APK run `35898272768` — PASS;
- Python Tests run `35898272864` — PASS.

Artifact:
- `Renault-Docs-v0.1.0-Debug`;
- artifact id `10768775075`;
- GitHub archive digest:
  `sha256:b3f2b3946d5549fc811ecbd80557bc10154a4e16614f42c9b154aae141d573df`.

Android CI verified:
- JVM unit tests;
- debug APK build;
- apksigner;
- zipalign;
- aapt package/sdk metadata;
- SHA-256 generation.

## v0.1.0 behavior

Implemented:
- Library;
- Add ready dataset via Android SAF;
- persistable read URI;
- manifest validation;
- persistent dataset registration;
- tile with model/years/platform/volume count;
- Viewer placeholder;
- proper Back ownership;
- no `MANAGE_EXTERNAL_STORAGE`.

Not implemented yet:
- old-folder conversion inside APK;
- production WebView;
- Android PDF layer;
- release signing.

## Next gate

Install the debug APK on the real phone and execute:
`docs/v.0.1.0/qa/PHONE_TEST.md`.

Do not call v0.1.0 PHONE PASS until SAF, rotation, restart persistence and Back are actually tested.


## Browser transfer kit

Merged in main before v0.2.0:
- portable source: `transfer/browser-version/`;
- generator: `tools/build_browser_transfer_kit.py`;
- default output: `/storage/emulated/0/Documents/Renault/packages/Renault-Browser-Transfer-Kit.zip`;
- target new-phone runtime only; Renault corpus is transferred separately.

## Android v0.2.0 development state

Branch: `feat/android-converter-foundation`.

Implemented and CI-tested:
- Library converter entry;
- source/destination SAF selection;
- persisted draft;
- no auto-picker on Activity recreation;
- plan validation/same-tree guard;
- Kotlin converter path normalizer + tests;
- app version `0.2.0` / code `2`.

Exact tested app source:
`47a536027b2c35b69a8e0dedf17f6e1ec140cd49`.

CI:
- Android Debug APK run `35908968101` — PASS;
- Python Tests run `35908968049` — PASS.

Phone QA for v0.1.0 was not completed before the user asked to continue development. Do not retroactively mark it PASS.

v0.2.0 phone QA is also pending. The next code wave after phone validation is the actual foreground conversion writer/staging/cleanup + Kotlin dataset package generator.


## Phone APK handoff

Renault now follows the same phone-side artifact pattern as YTM:
- `tools/termux/reno-build-apk.sh` — dispatch + watch + download;
- `tools/termux/reno-download-apk.sh` — latest/exact run download, automatic artifact extraction, SHA-256 verification;
- stable phone folder: `/storage/emulated/0/Documents/Renault/packages/Renault-Docs-vX.Y.Z-build/`;
- aliases: `reno-apk`, `reno-apk-latest`;
- Renault Menu items 7/8 expose the same flow without remembering commands.

Do not hand the user several loose files when phone-side GitHub handoff is available. Prefer the stable Renault/packages folder containing APK + checksum. Do not use Download for Renault APK handoff because YTM already owns that user-facing download pattern.


## Stable development signing

Phone finding after v0.2.1 artifact:
- Android reported `Додаток не встановлено` when trying to update v0.2.0;
- root cause: GitHub Actions default debug keystore was regenerated on each ephemeral runner, so v0.2.0 and v0.2.1 had different signer certificates.

Fix:
- debug CI restores one stable non-production development key from `.github/signing/renault-docs-dev.jks.b64`;
- keystore SHA-256:
  `7944e7d78bd2442021731a4cfd3105c06f475c13d70d4195b2378539604dfd10`;
- signer certificate SHA-256:
  `dd588fbb3093a047f81c24397fc6d2ab7f5a32040425a21dd24506cce59a9802`;
- workflow verifies both before publishing the APK.

Migration rule:
the already-installed v0.2.0 was signed by the old ephemeral key and cannot be updated in-place. It must be uninstalled once, then the stable-signed v0.2.1 installed. From that stable-signed build onward, future debug APK updates should install over the previous version as long as versionCode increases.


## v0.2.1 real-phone viewer evidence

Confirmed:
- dataset catalog renders;
- all 10 Laguna volumes are listed;
- representative NT8236 opens its legacy frame/menu/image UI;
- PDF path resolves correctly into the controlled viewer.

Not yet claimed:
- full rotation/Back matrix;
- exact PDF rendering in v0.2.1.

User also reported noticeable latency in the Android viewer.

## v0.2.2 — PDF + performance

Exact tested app source:
`2d2a5f057c5e1820cb7cd4a4cad34b32153864cb`.

CI:
- Python Tests `35919503656` — PASS;
- Android Debug APK `35919503732` — PASS.

Implemented:
- cached `DocumentsContract` SAF resolver;
- per-directory child index;
- resolved-node cache and missing-path cache;
- no repeated `DocumentFile.findFile()` in the viewer;
- WebView `LOAD_DEFAULT` instead of forced no-cache;
- Android native `PdfRenderer` layer;
- continuous vertical PDF pages;
- lazy rendering near viewport;
- previous/next and zoom controls;
- bounded rendered-page JPEG byte cache.

Phone QA for PDF/performance is still pending. Do not claim PASS until the user installs v0.2.2 and tests it.


## v0.3.0 development direction

User requested a faster modernized navigation model while preserving the working legacy mode, plus the ability to save PDFs from inside the app.

v0.3.0 implements the first Modern layer:
- Library dataset tap → native `ModernDatasetActivity`;
- native search/filter across volumes;
- Classic fallback remains visible;
- existing datasets work from manifest `volumes`;
- newly packaged datasets also get `_renault/modern-index.json` schema v1;
- volume tap still enters legacy internal content for compatibility.

This is intentionally incremental. The next Modern-index evolution should extract internal section/node navigation from representative Renault volumes and move more old frame screens into native Android UI.

PDF export contract:
- PDF toolbar `⇩ PDF`;
- custom internal WebView action is intercepted by the app;
- Android `ACTION_CREATE_DOCUMENT`;
- copy original source PDF bytes from dataset SAF;
- no broad storage permission;
- picker Cancel returns without fake success.


## v0.3.0 phone finding

Confirmed:
- native PDF rendering remains functional;
- PDF Save/export works on the real phone.

Failed performance goal:
- user reports no meaningful overall speed improvement.

Interpretation:
Modern v1 only replaced the top-level volume catalog. After a volume is opened, legacy HTM/JS/GIF still cross SAF as individual files, so the dominant latency remains.

## v0.3.1 Fast Pack

Performance strategy:
- package all non-PDF web resources into one deterministic `_renault/fast-content-<sha>.zip`;
- record path/hash/size/file count in `renault-dataset.json.fast_pack`;
- Modern screen prewarms/copies that archive once into Android app cache;
- verify SHA-256 before activation;
- WebView serves HTM/HTML/JS/CSS/GIF/ICO/PNG/JPG/SVG/JSON from local `ZipFile`;
- native PDF layer still reads PDFs separately;
- SAF remains fallback when Fast Pack is missing/unavailable;
- Classic mode remains.

Existing converted datasets do not need full reconversion. Running `tools/package_dataset.py` / Renault Menu item 9 regenerates package metadata plus Fast Pack without rewriting original normalized Renault files.


## v0.3.1 real-phone result — 2026-09-24

Confirmed on the real phone:
- Fast Pack package generation completed successfully;
- Laguna dataset Fast Pack contains 49,077 web files;
- Fast Pack archive size: 40,323,133 bytes;
- Modern screen reports Fast Pack ready;
- user reports internal legacy navigation is now fast and responsive;
- PDF save/export remains working.

PERF-002 is considered phone PASS.

New phone findings:
- Modern top-level volume tiles are not in chronological release order;
- PDF +/- controls do not visibly zoom the PDF page because only render resolution changes while CSS width stays 100%.

## v0.3.2 fix wave

Targets:
- sort volume tiles chronologically by normalized release date ascending;
- sort in Android runtime as well as package generation so the existing generated Fast Pack/index does not need to be rebuilt just for ordering;
- make PDF +/- controls change the actual displayed page width;
- keep higher-resolution rerendering at larger zoom;
- show current PDF zoom percentage;
- preserve Fast Pack, PDF save, Classic fallback, Back and rotation behavior.


## Durable project-artifact rule — 2026-09-24

Important Renault Docs decisions must not remain only in chat.

Repository is the source of truth for:
- roadmap and product decisions;
- architecture/storage/cloud contracts;
- conversion/backup/restore behavior;
- UX contracts and design diagrams;
- QA findings and release state.

Canonical text-heavy diagrams should use editable vector/source formats (SVG, Mermaid, PlantUML, HTML). AI-generated raster images may be kept as previews, but should not be the only project record.

Current product vision:
- `docs/product/RENAULT_DOCS_VISION.md`
- `docs/design/renault-docs-vision-roadmap.svg`
- `docs/PROJECT_ARTIFACT_POLICY.md`


## v0.4.0 Settings foundation — 2026-09-24

Merged working app source:
`cd10f7b3f4cd688cc18df96edfbb73eac30a0bd7`

CI:
- Tests run `35934861805` — PASS;
- Android Debug APK run `35934861828` — PASS;
- artifact: `Renault-Docs-v0.4.0-Debug`.

Implemented:
- Settings entry from Library;
- default dataset opening mode: Modern / Classic;
- default PDF zoom: 85 / fit-width 100 / 120 / 150 / 200%;
- configurable PDF +/- step: 5 / 10 / 20%;
- persisted writable SAF backup-folder selection;
- About section with app version/build/runtime summary;
- selected PDF defaults are applied to newly opened PDF viewers;
- selected Modern/Classic default is applied when opening dataset tiles.

Phone QA is pending. Do not claim v0.4.0 phone PASS until installed and tested.

## Design artifact pipeline

Project design source-of-truth:
- `docs/design/renault-docs-vision-roadmap.svg` — canonical editable vector source;
- `docs/design/renault-docs-vision-roadmap.png` — 2x phone-friendly preview;
- `docs/design/renault-docs-vision-roadmap.pdf` — vector PDF.

Automation:
- `tools/export_design_assets.py`;
- `.github/workflows/export-design-assets.yml`;
- any design SVG update on main automatically regenerates and commits matching PNG/PDF outputs.

The PDF export was render-verified; font-dependent decorative toolbar symbols in the roadmap were replaced with font-safe text labels.

## Next development target after v0.4.0 phone QA

Converter 2.0 + Backup Center:
1. staged conversion;
2. output validation;
3. original-source ZIP creation;
4. backup metadata + SHA-256;
5. archive verification;
6. explicit final deletion confirmation;
7. source deletion only after successful verified backup;
8. restore flow / Backup Center.

Critical contract:
source must never be deleted if conversion, validation, backup creation, backup verification, or user confirmation fails/cancels.


## Skin/theme system — Technical Blue approved concept — 2026-09-24

A durable skin/theme design system is now part of the repository.

Source-of-truth:
- `docs/design/skins/README.md`
- `docs/design/skins/technical-blue/SKIN.md`
- `docs/design/skins/technical-blue/TOKENS.md`
- `docs/design/skins/technical-blue/UX_CONTRACT.md`
- `docs/design/skins/technical-blue/IMPLEMENTATION.md`
- `docs/design/skins/technical-blue/CONCEPTS.md`

Technical Blue status:
`approved-concept`.

Selected portrait-phone direction:
- top app bar: Back, title, Home, Search, Settings, overflow;
- native mode row: Sections / Illustrations / PDF / Contents;
- compact one-column native section list;
- PDF controls in the upper content region;
- centered labels/glyphs in every control;
- dedicated Fit width action;
- Classic must expose a fast Modern switch;
- Modern keeps Classic fallback while legacy coverage remains;
- page/document search opens from the top UI.

Concept assets:
- `concept-selected.svg` — selected portrait layout;
- `concept-variant-b-drawer.svg` — permanent two-pane drawer reference for tablets/landscape;
- `concept-variant-c-overlay.svg` — overlay drawer reference for phone menu behavior.

The design exporter now scans `docs/design/**/*.svg` recursively, so skin SVGs automatically receive PNG + PDF derived previews on main.

Implementation sequence:
1. PDF toolbar alignment/centering polish;
2. Classic -> Modern quick switch;
3. persistent top navigation actions;
4. page/document text search;
5. migrate legacy frame menus 101/103/105/... to native one-column navigation;
6. progressively reduce legacy WebView navigation responsibility;
7. only then expose runtime-selectable skins after infrastructure and phone QA.


## v0.4.1 Technical Blue header wave 1 — CI PASS — 2026-09-24

Merged app source:
`49f49a364148e3744d8038330f78b5ab007eb7a7`

CI:
- Tests run `35938712431` — PASS;
- Android Debug APK run `35938712460` — PASS;
- artifact: `Renault-Docs-v0.4.1-Debug`.

Implemented:
- Classic/legacy viewer top header actions: Modern, Home, Search, Settings;
- Classic -> Modern bridge without forcing a Library round trip;
- when current volume entrypoint is known, Modern highlights and scrolls to that volume;
- current-page WebView text search with previous/next/match count/close;
- search query and open search row survive rotation;
- PDF toolbar button/input content centered;
- Fit width button now visibly says `↔ По ширині`;
- PDF toolbar horizontally scrolls on very narrow screens instead of clipping controls;
- wider centered zoom field;
- Home uses Library as the stable top-level destination;
- Settings opens without discarding viewer context.

Current search scope:
WebView text search for the currently loaded HTML page. PdfRenderer pages are raster images, so text inside PDFs is not searchable yet.

Phone QA is pending. Do not mark v0.4.1 phone PASS until tested on the real phone.

Next UI migration after phone QA:
- native Technical Blue one-column sections for legacy 101/103/105/... menus;
- top document mode/navigation structure;
- progressively remove legacy frame navigation while keeping Classic fallback.


## v0.4.2 phone-QA fixes — CI PASS — 2026-09-24

Merged app source:
`90ef691d862fd28f24ca0b8ed088c6a0c81d2929`

CI:
- Tests run `35940255443` — PASS;
- Android Debug APK run `35940255455` — PASS;
- artifact: `Renault-Docs-v0.4.2-Debug`.

Phone findings addressed:
1. page-search query field was too narrow because navigation controls shared the same row;
2. Classic package catalog could show old unsorted volume order from an already-generated `_renault/START.html`;
3. Unicode `↔` inside the Fit width PDF control looked vertically misaligned.

Implemented:
- page search is now two rows:
  - row 1: wide query + match counter;
  - row 2: previous / next / close;
- Classic `_renault/START.html` cards are sorted chronologically at runtime by date;
- this Classic sorting fix does not require package/Fast Pack regeneration;
- Fit width uses an inline vector SVG arrow plus separate centered label;
- icon + text use flex centering as one unit.

Phone QA is pending.
Do not mark v0.4.2 phone PASS until verified on the real device.

After v0.4.2 phone QA, continue with native Technical Blue section migration for legacy 101/103/105/... navigation.


## v0.4.1 phone findings / v0.4.2 fix wave — 2026-09-24

Real-phone v0.4.1 findings:
- current-page search works;
- search field was too narrow because query + counter + controls shared one row;
- Classic catalog tiles in the existing generated START.html were not chronological;
- Fit width Unicode arrow was visually off-center despite centered button layout.

v0.4.2 fixes:
- two-row search UI with a wide first-row query field;
- Classic START.html chronological runtime sorting for existing datasets;
- future Classic catalog generation uses chronological order;
- Fit width uses an inline vector icon centered with the label.

v0.4.2 tested source:
`90ef691d862fd28f24ca0b8ed088c6a0c81d2929`

CI:
- Tests run `35940255443` — PASS;
- Android Debug APK run `35940255455` — PASS;
- artifact: `Renault-Docs-v0.4.2-Debug`.

Dataset/Fast Pack format is unchanged.
Point 9 is not required just to get the runtime Classic sorting fix.
Phone QA of v0.4.2 is still pending.


## v0.4.2 phone follow-up / v0.4.3 — 2026-09-24

Confirmed on real phone with v0.4.2:
- two-row Search UI is usable;
- current-page text search works;
- Classic catalog tiles are chronologically sorted on the existing Laguna dataset.

New phone finding:
- after closing Search, the query remains visible on reopen but WebView matches are inactive until the query changes;
- Backup Settings expose raw SAF URI and the old “Очистити папку backup” wording is misleading because it does not delete files;
- selected Backup folder was inside the current `..._android/Backup` dataset tree, which is not the preferred long-term backup location.

v0.4.3 fixes:
- reopening Search with non-empty preserved query immediately reruns `findAllAsync(query)`;
- Backup Settings show a friendly SAF path when possible instead of raw `content://`;
- recommended backup root is `Documents/Renault/backups`;
- button renamed to “Скинути вибір папки”;
- clearing selection explicitly says files were not deleted;
- selection inside an `_android/` tree shows a warning recommending an external backup folder.

v0.4.3 tested main source:
`2bee3608c06f05d261fc768c755417a40990e288`

CI:
- Tests run `35941559116` — PASS;
- Android Debug APK run `35941559172` — build/sign/verify/upload PASS;
- artifact: `Renault-Docs-v0.4.3-Debug`.

No Fast Pack/dataset rebuild required.
Phone QA of v0.4.3 is pending.

After this polish passes phone QA, continue with the native Technical Blue section migration for legacy `101 / 103 / 105 / ...` navigation.


## v0.4.3 phone confirmation / v0.4.4 Backup card — 2026-09-24

Confirmed on real phone with v0.4.3:
- Search reopen works correctly with the preserved query;
- Fit width vector arrows are visually centered;
- Classic chronological ordering remains correct.

New UI finding:
Backup Settings were functionally correct but visually fragmented:
- selected folder lived inside the card;
- “Скинути вибір папки” was outside the card;
- folder basename `Backup` duplicated the final segment of the readable path.

v0.4.4:
- Backup is one cohesive settings card;
- label `Вибрана папка` + one readable path value;
- no separate duplicated folder basename;
- actions live inside the same card;
- no folder selected: `Вибрати папку`;
- folder selected: `Змінити папку` + `Скинути вибір`;
- reset action is hidden when no folder is selected;
- dataset-local backup warning remains inside the card.

v0.4.4 tested main source:
`4efcc1edbbd9df9092d99353943ffab919cda0a9`

CI:
- Tests run `35942756642` — PASS;
- Android Debug APK run `35942756636` — PASS;
- artifact: `Renault-Docs-v0.4.4-Debug`.

No dataset/Fast Pack rebuild required.

After v0.4.4 phone QA, proceed to native Technical Blue migration of legacy `101 / 103 / 105 / ...` navigation.


## v0.4.5 PDF zoom preset popup hotfix — 2026-09-24

Phone finding:
the small PDF zoom preset dropdown button was visible but the 85/100/120/150/200% popup did not appear.

Root cause:
the popup was absolutely positioned inside the horizontally scrollable PDF toolbar. Android WebView clipped the popup through the toolbar overflow/scroll container.

v0.4.5 fix:
- preset popup is a fixed overlay;
- popup node is moved to `document.body`, outside the scrollable toolbar;
- popup position is calculated from the dropdown button;
- horizontal position is clamped to viewport;
- popup opens above the button if there is not enough room below;
- toolbar scroll and window resize reposition the popup;
- outside tap closes the popup;
- preset selection still calls the shared `setZoomPercent()` path.

Tested main source:
`be45f7b9be93120f7eb542efbb8d21a5b3117695`

CI:
- Tests run `35943891399` — PASS;
- Android Debug APK run `35943891510` — PASS;
- artifact: `Renault-Docs-v0.4.5-Debug`.

No dataset/Fast Pack rebuild required.
Phone QA of the dropdown remains pending.


## v0.5.0 native Technical Blue sections wave 1 — 2026-09-24

Purpose:
replace the first major Renault legacy frame-navigation layer with native Android UI.

Dataset/package:
- new generated artifact: `_renault/modern-sections.json`;
- generated by existing `Renault → 9 — Оновити Fast/Modern package`;
- no full normalized-dataset reconversion is required;
- section discovery follows legacy frame/iframe navigation and extracts three-digit Renault section rows/links;
- supports table-row labels, anchors, image-map areas, common JavaScript href/onclick navigation, legacy encodings including Windows-1251, and ranked fallback scanning;
- section codes are deduplicated and numerically sorted;
- manifest now persists `modern_sections`.

Android:
- Modern volume card now opens `ModernVolumeActivity` rather than immediately opening the legacy frameset;
- native Technical Blue one-column section list;
- each row shows section code + legacy label + navigation chevron;
- native section search by code/title;
- Home / Search / Settings actions;
- explicit Classic fallback;
- selecting a native row opens only its mapped legacy content entrypoint in the existing viewer;
- Modern button from mapped legacy content returns to the same volume's native section list;
- missing/empty section index degrades to a safe Classic fallback card.

Tested main source:
`79fddfbe14336af619e989652aeddbaf9e4b8c67`

CI:
- Tests run `35945733289` — PASS;
- Android Debug APK run `35945733273` — PASS;
- artifact: `Renault-Docs-v0.5.0-Debug`.

Phone QA is pending.

Required before phone QA:
1. pull current main;
2. run `Renault → 9 — Оновити Fast/Modern package` once;
3. inspect the package output and record section counts for all 10 volumes;
4. install v0.5.0;
5. test native section list first on NT8236A and record any section whose direct legacy entrypoint depends on missing frame context.

Next decision after phone QA:
- if extracted mappings are correct, continue replacing inner legacy navigation/content shells;
- if some codes map incorrectly, improve the section parser/index mapping without abandoning the native UI layer.


## v0.5.0 phone findings / v0.5.1 standalone legacy compatibility — 2026-09-24

Real-phone v0.5.0 observations after native section routing:
- some section content works correctly;
- PDF content works;
- self-contained legacy select/dropdown navigation works;
- engine/image-choice pages render;
- some direct legacy pages still depend on old frame context;
- CMP141 exposed black legacy text on a transparent page over the app's dark WebView background.

v0.5.1 compatibility bridge:
- enabled only for legacy content opened directly from native section flow;
- transparent legacy HTML gets a white fallback canvas + light color-scheme;
- explicit Renault blue/background pages are not overwritten;
- missing named `target` values on anchors/forms/base are rewritten to `_self`;
- dynamic target-bearing elements are handled via MutationObserver;
- `window.open(url, oldFrameName)` targeting a missing named frame falls back to current-page navigation;
- Classic full-volume mode is not put into standalone compatibility mode.

Tested main source:
`08f5f61487931aa197337df88ce8c020dda74690`

CI:
- Tests run `35947884873` — PASS;
- Android Debug APK run `35947884807` — PASS;
- artifact: `Renault-Docs-v0.5.1-Debug`.

No dataset/Fast Pack refresh is required for v0.5.1.

Phone QA target:
retest the same CMP141/CMP103 paths and record any exact control that still does nothing. Remaining failures are expected to be direct `parent.frames[...]` JavaScript cases and should be fixed specifically rather than restoring the old frame menu.


## v0.5.1 phone finding / v0.5.2 legacy frame-JS bridge — 2026-09-24

Real-phone CMP101 observations:
- legacy top action 3 loads content;
- actions 1, 2 and 4 do not navigate;
- legacy combo/select changes selection but does not open the selected content;
- local icons/assets render, so this is a navigation-context problem rather than missing files.

v0.5.2:
- scans standalone legacy HTML and same-origin external scripts for named/numeric frame references;
- creates hidden same-origin bridge frames for missing `parent.frames["name"]`, `top.frames["name"]`, `frames[N]` style dependencies;
- navigation performed in a bridge frame is mirrored into the main viewer;
- select/change fallback checks selected option and inline onchange targets when normal handler did not navigate;
- click fallback checks href/onclick local HTML/PDF candidates when normal handler did not navigate;
- all fallback navigation is restricted to the local virtual origin;
- Classic full frameset behavior remains unchanged.

Tested main source:
`3bf94e6a441e8a40b246872d93f03ccded43fca3`

CI:
- Tests run `35949086107` — PASS;
- Android Debug APK run `35949086123` — PASS;
- artifact: `Renault-Docs-v0.5.2-Debug`.

No dataset/Fast Pack refresh is required.

Phone QA target:
retest CMP101 actions 1/2/3/4 and the same combo/select. If one still fails, record the exact icon number or selected item; next step is to capture/normalize that specific legacy handler pattern rather than reintroducing the old navigation frame.


## v0.5.2 phone failure / v0.5.3 hybrid section shell — 2026-09-24

v0.5.2 real-phone CMP101 result:
`1- 2- 3- 4- 5- 6- combo absent`.

Conclusion:
the indexed section entrypoint is only a child fragment of the original Renault frameset. The absence of the combo proves that missing sibling frames/UI cannot be restored reliably by patching handlers inside that fragment.

v0.5.3 architecture:
- native 101/103/... list remains the user-facing section chooser;
- section tap now loads the full original volume entrypoint/frameset;
- hybrid script waits for same-origin frames;
- scores child documents by unique three-digit Renault codes to locate the old menu frame;
- triggers the original requested section row/link inside that real navigation frame;
- then collapses the old menu frame to zero size when the frameset geometry allows it;
- sibling frames remain alive, preserving original combo boxes, selectors and cross-frame JavaScript;
- if auto-selection cannot find the code, the full original frameset remains loaded rather than falling back to the known-broken direct fragment.

No dataset/Fast Pack refresh is required.

Main application merge:
`6191e1288c034e81599f04a8aa568e4ec6876956`

Main CI:
- Tests run `35950162906` — PASS;
- Android Debug APK run `35950162849` — PASS;
- artifact: `Renault-Docs-v0.5.3-Debug`.

Phone QA target:
open CMP101 through native 101 and retest 1–6 plus combo. If controls work but the old section menu remains visible, treat that only as frame-collapse UI polish; do not revert the hybrid runtime.


## v0.5.3 phone pass / v0.5.4 Modern hybrid shell — 2026-09-24

v0.5.3 real-phone result:
- legacy runtime controls work;
- combo/select works;
- hybrid full-frameset architecture is functionally correct;
- UX issue: Modern visibly starts inside the Classic Renault frameset.

v0.5.4 keeps the working runtime and changes only the presentation boundary:
- native section launch hides the legacy WebView during warm-up;
- status shows `Відкриваю Modern · <code>…`;
- old section menu frames are collapsed more aggressively but kept alive;
- collapse is repeated to survive legacy frameset geometry rewrites;
- Android polls the hybrid selector state;
- visible content fades in only after the requested section is selected;
- timeout reveals the runtime as safe fallback rather than leaving a blank page;
- rotation/restored state does not repeat the warm-up hiding.

Application merge:
`5a3718dacd37932d9a869b994e2a6dcba5c80dd6`

Main CI:
- Tests run `35951443398` — PASS;
- Android Debug APK run `35951443423` — PASS;
- artifact: `Renault-Docs-v0.5.4-Debug`.

No dataset/Fast Pack refresh is required.

Phone QA target:
1. Modern → volume → 101;
2. runtime controls/combo must remain working;
3. old 101/103/... menu should not remain visible;
4. full Classic frameset should not flash during open;
5. Modern button must still return to the native section list.

If only some inner legacy toolbar remains visible while the old section chooser is gone, that is the next native UI migration layer, not a rollback condition.


## v0.5.4 phone result / stop point — 2026-09-24

Phone result:
- v0.5.4 did NOT keep Classic runtime hidden;
- visible status: `Modern shell: Classic runtime не вдалося повністю сховати.`;
- fallback opened visible Classic `Visu Schema` / Laguna 2 splash (NT8183A, 22.01.2001);
- therefore v0.5.4 status is `PHONE_FAIL_MODERN_PRESENTATION_RUNTIME_OK`.

Important:
- do NOT regress from the full-frameset runtime introduced in v0.5.3;
- v0.5.3 proved legacy controls + combo/select work;
- current problem is presentation/projection only.

Next session:
1. read `docs/assistant-kit/SESSION_CHECKPOINT_2026-09-24.md`;
2. do not guess another menu-collapse heuristic;
3. implement a real-device frame-tree diagnostic report first;
4. capture names/src/nesting/rows/cols/dimensions/code counts/select counts before and after selecting a section;
5. then make deterministic Modern projection while keeping the full legacy runtime alive.

User stopped work for the night at this point.


## v0.5.5 real-device frame-tree diagnostics — 2026-09-24

Decision after v0.5.4:
do not add another blind Classic-frame hiding heuristic.

v0.5.5 is diagnostic-only:
- hybrid section viewer gets a temporary `DBG` button;
- `DBG` collects the actual same-origin Renault frame/window hierarchy from the running phone;
- report includes tree path/depth/index, window name, title, URL, frame src/name/id, live rect, computed display/visibility/opacity, parent frameset rows/cols, child count, three-digit section codes, select/combo state, PDF links, image/button counts, body background and text fingerprint;
- report also includes hybrid state plus summarized menu/combo candidates;
- report opens in a selectable monospaced dialog;
- `Копіювати` copies the complete report to Android clipboard.

Important:
- v0.5.5 does NOT attempt to solve the visible Classic runtime;
- v0.5.3 full-frameset runtime remains the functional baseline;
- no dataset/Fast Pack refresh is required.

Application merge:
`68a83adfd30d8dfda2dae4409876dcc36675dfe5`

Main Tests run:
`35987490749` — PASS.

Main Android Debug run:
`35987490643` — PASS.

Artifact:
`Renault-Docs-v0.5.5-Debug`

Artifact SHA-256:
`9995c5da1ca0ffc604e08a27d8fe52fdb463255806d35d7d91abff2463d45c2a`

Next phone action after APK is ready:
1. Modern → same volume → 101;
2. let current Classic/Visu Schema fallback appear;
3. tap `DBG`;
4. tap `Копіювати`;
5. paste full report into project chat;
6. ideally capture a second report after manually reaching the working CMP101 controls/combo state.

Only after those reports should v0.5.6 define deterministic frame projection/hiding.


## v0.5.5 phone frame-tree result / v0.5.6 deterministic projection — 2026-09-24

Real-device v0.5.5 diagnostic capture for NT8183A / section 101 is complete.

Critical frame evidence:
- top runtime settles on `RUS/HTM/ENTREE.HTM`, not the configured root `INDEX.HTM`;
- named frames are exactly `titre`, `org`, `menu`, `nav`, `doc`;
- `titre + org` occupy the left Classic column;
- `org` is the 101/103/... navigation frame;
- after manual 101 selection:
  - `menu -> RUS/HTM/MENU/101.HTM`;
  - `nav -> COMMUN/HTM/PC/101.HTM`;
  - `doc -> COMMUN/PDF/PC/S7.pdf`;
- `menu + nav + doc` are the working runtime that must remain alive;
- both captures had `hybridStates: []`, confirming the old exact-root injection gate was lost when the top document became `ENTREE.HTM`;
- old menu/combo scoring returned no candidates and must not drive projection.

v0.5.6 implementation:
- hybrid injection now runs on the finished legacy HTML runtime, including `ENTREE.HTM`;
- selection targets named frame `org`;
- requested section readiness is verified from named `menu/nav` transitions before reveal;
- the common `titre+org` nested FRAMESET branch is collapsed at its outer geometry boundary;
- `menu/nav/doc` stay loaded and visible;
- MutationObserver locks the projection against legacy frameset rewrites;
- DBG remains available and now reports phase/triggerAttempts/projection.

Merged app source:
`a965413234d4d49c4a97714588096f91bb24708e`

Main CI:
- Tests run `36005382508` — PASS;
- Android Debug APK run `36005382468` — PASS;
- artifact: `Renault-Docs-v0.5.6-Debug`;
- artifact id: `10809478409`;
- APK SHA-256:
  `9bfafa025d2207ad7303c408a9311ffeca4d1179904f331449a2ca3f63cfd757`.

No dataset/Fast Pack refresh is required.

Next phone gate:
1. download/install v0.5.6;
2. Modern → NT8183A → 101;
3. do not manually touch the old section list;
4. verify 101 auto-opens and the left Classic `titre+org` column is gone;
5. verify the right runtime controls and PDF/document flow still work;
6. if anything fails, capture one DBG report in the failed state.


## v0.5.6 phone result — deterministic projection PASS / navigation UX + performance finding — 2026-09-24

Real-phone NT8183A → Modern → 101 result:
- deterministic projection works;
- left Classic branch `titre + org` is hidden;
- DBG reports `done: true`, `attempts: 1`, `triggerAttempts: 1`, `phase: projected`;
- exact outer projection is `cols = 0,*`;
- original outer geometry was `216,747`;
- preserved runtime frames are `menu`, `nav`, `doc`;
- `org` now correctly reports 214 section codes, confirming it is the authoritative 101/103/... source.

New UX requirement:
- while reading a section, the user needs fast surfing between 101/103/... blocks;
- preferred direction is a Modern section drawer/panel with search, opened from the viewer without reloading the whole volume;
- optional Classic panel reveal may be kept as a fallback/debug action, but should not be the default navigation UX;
- switching sections should reuse the already-loaded full runtime and trigger the target code inside `org`, instead of starting a new ViewerActivity/full frameset load.

New performance finding:
- initial Modern section load is still noticeably slow;
- the projection selector itself is not the bottleneck because phone DBG completed selection/projection in one attempt;
- likely cost remains full legacy runtime startup and initial document/PDF path, plus Fast Pack cache preparation when required;
- add timing diagnostics before further performance guesses.

Architecture idea to evaluate:
- extend converter/package generation from the existing `_renault/modern-sections.json` into a richer normalized runtime manifest;
- use one generic Modern/runtime shell driven by arrays/JSON rather than relying on per-volume Classic navigation files for user-facing navigation;
- preserve legacy section-specific menu/nav/doc pages only where their cross-frame JS is still required.


## v0.5.7 live Modern section navigator — CI PASS — 2026-09-24

Built on the v0.5.6 phone-confirmed deterministic projection.

Implemented:
- in hybrid viewer the previous `Modern` action becomes `Розділи`;
- `Розділи` opens a searchable Modern panel over the current document;
- section list comes from the existing `_renault/modern-sections.json`;
- current section is marked active;
- search filters by code/title;
- switching 101/103/... reuses the already-loaded full Renault runtime;
- no new ViewerActivity / top-level frameset load is required for a live switch;
- target section is triggered through named frame `org`;
- switch success waits for named `menu/nav` to expose the requested section;
- current live-selected section survives Activity recreation/rotation;
- `Повний Modern` remains available from the section panel.

Performance diagnostics added to DBG:
- `runtimeTiming.clientAgeMs`;
- `runtimeTiming.fastPackPrepareMs`;
- `runtimeTiming.fastPackCopiedToLocalCache`;
- top-document `navigationTiming`;
- initial hybrid projection `durationMs`;
- `liveSectionSwitch` state with attempts/triggerAttempts/durationMs.

Exact merged app source:
`4ae04f1a2eda42fa56553fd31668ceb13e6c750e`

Main CI:
- Tests run `36009167466` — PASS;
- Android Debug APK run `36009167695` — PASS;
- artifact: `Renault-Docs-v0.5.7-Debug`;
- artifact id: `10811083951`;
- APK SHA-256:
  `df15e2931690395318c79b1a4ea0e9ea369a322bf2fb0f77651a94f2660ae3c0`.

No dataset/Fast Pack refresh is required.

Next phone gate:
1. install v0.5.7;
2. Modern → NT8183A → 101;
3. open `Розділи`, search/select 103;
4. live-switch `103 → 105 → 101` and compare speed with the initial volume open;
5. rotate after a live switch and confirm active section is preserved;
6. capture DBG once after initial slow open and once after a live switch so startup cost can be separated from switch cost.

## Classic preserved + Modern JSON IR architecture — 2026-09-24

Canonical decision:
- Classic already works and remains untouched as compatibility/reference mode.
- We will not keep polishing "Modern = full Classic runtime + hidden frames" as the final architecture.
- Conversion/package generation becomes a compiler step.
- It may take longer once, but it should emit a normalized JSON model that lets the Modern app run fast afterwards.
- Modern UI/skin/navigation will be fully ours and independent from the legacy Renault frameset presentation.

New compiler artifact:
- `_renault/runtime-tree.json`
- schema: `renault-runtime-ir` v1;
- one dataset-level JSON with `volumes[]`;
- each volume contains:
  - preserved Classic shell topology;
  - discovered named frames;
  - normalized Modern sections;
  - placeholders for controls/actions/documents;
  - explicit compiler phase/pending stages.

Implementation:
- `core/runtime_ir.py`;
- integrated into `core/dataset_package.py`;
- dataset manifest gains `runtime_tree: "_renault/runtime-tree.json"`;
- package tool prints the generated Runtime path;
- tests added in `tests/test_runtime_ir.py`.

Phase 1 scope:
- INDEX/ENTREE/frame topology;
- named frames;
- 101/103/... section catalog;
- section title + legacy entrypoint.

Next compiler phases:
1. inspect real 101 `MENU/PC` files;
2. compile controls/options;
3. compile legacy JS actions;
4. build document/asset routing;
5. reproduce 101 fully from JSON without depending on hidden `org`;
6. only after parity, scale to all sections.

Detailed contract:
`docs/architecture/MODERN_NATIVE_IR_PLAN.md`.

Classic remains the parity oracle throughout migration.

## Runtime IR Phase 1 merged — 2026-09-24

Canonical Modern architecture is now implemented at foundation level.

Merged source:
`81503baf5dd819f31017f4888a59928757b6978f`

Main Tests:
- run `36015407081` — PASS.

Result:
- Classic remains unchanged as compatibility/reference mode;
- package/conversion now generates `_renault/runtime-tree.json`;
- dataset manifest exposes `runtime_tree`;
- runtime IR schema v1 stores Classic shell/frame topology plus normalized Modern section data;
- controls/actions/documents are intentionally placeholders for the next compiler phases.

Next real-data gate:
1. phone/Termux updates to latest `main`;
2. Renault Menu point 9 regenerates Fast/Modern package;
3. inspect the generated `_renault/runtime-tree.json` for NT8183A;
4. use real section 101 files/data as the first Phase 2 target;
5. compile 101 controls/actions/documents from legacy MENU/PC/JS into JSON;
6. reproduce 101 from our data model while keeping Classic available for parity checks.



## Runtime IR Phase 1 real dataset validation — 2026-09-24

The user supplied the real generated `runtime-tree.json` from the phone dataset.

Validated facts:
- schema v1 / `renault-runtime-ir`;
- Classic preserved = true;
- Modern contract = normalized JSON;
- 10 volumes;
- 2174 normalized sections total;
- NT8183A has 214 normalized sections;
- real shell topology matches phone runtime evidence:
  `titre/org/menu/nav/doc`;
- NT8183A section 101 is indexed as `ПРИКУРИВАТЕЛЬ` with legacy entrypoint
  `RUS/HTM/MENU/101.HTM`;
- Phase 1 intentionally leaves controls/actions/documents empty;
- pending compiler stages remain section-controls, legacy-js-actions, document-routing, asset-dependencies.

Conclusion:
Phase 1 is real-data PASS. The architecture split "Classic preserved + Modern compiled JSON IR" is validated.

Next gate:
- pull latest main after this wave;
- Renault Menu → 10;
- defaults NT8183A / 101;
- generate `packages/Runtime-IR-NT8183A-101-source.zip`;
- upload that zip for Phase 2 compiler work.

The source-bundle exporter intentionally starts from the requested section, not CODE.HTM, so it does not crawl all 214 sections.

## Runtime IR Phase 2 bundle gate merged — 2026-09-24

Merged source:
`a933455ef66575911f3d4737e79409c5806238d2`

Tests:
- run `36020872886` — PASS.

Renault Menu now has:
`10 — Експорт Runtime IR source bundle`

Defaults:
- volume `NT8183A`;
- section `101`.

Expected output:
`/storage/emulated/0/Documents/Renault/packages/Runtime-IR-NT8183A-101-source.zip`

Upload that ZIP next. It is the real evidence package for implementing Phase 2 controls/actions/document-routing compiler logic.

## Runtime IR Phase 2 compiler merged — 2026-09-24

Real NT8183A / 101 source bundle was analyzed.

Evidence:
- 14 text source files;
- 30 binary references;
- 61 dependency edges;
- no missing references;
- one shared legacy JS file (`VISU.JS`) with simple route/print/back helpers;
- 101 can be described statically as menu + panels + selects + document routes + composite nomenclature documents.

Runtime IR v2 implementation merged as:
`e83992791cf79ae5b2b1ec49a14eaf866cfc8bd7`

Tests:
- PR Tests run `36022346261` — PASS.

Runtime tree changes:
- schema version: 2;
- compiler phase: `section-ir-v2`;
- per section now compiles:
  - `panels[]`;
  - `controls[]`;
  - `actions[]`;
  - `documents[]`;
  - `assets[]`;
  - `source_files[]`;
- PDF, composite frameset documents, structured HTML tables/headings, selects and static routes are normalized;
- Classic source/runtime remains untouched.

Real 101 finding:
`docs/architecture/RUNTIME_IR_PHASE2_NT8183A_101_FINDING_2026-09-24.md`.

Small section validation exporter merged as:
`e96cf8af30f927c2ff3ab6e3d2467050f2fe6aa1`

Tests:
- run `36022523957` — PASS.

Renault Menu:
- 9 — regenerate Fast/Modern package and Runtime IR v2;
- 10 — export raw source bundle;
- 11 — export one compiled Runtime IR JSON section.

Next phone gate:
1. Renault Menu → 5;
2. Renault Menu → 9;
3. Renault Menu → 11;
4. accept defaults NT8183A / 101;
5. upload:
   `/storage/emulated/0/Documents/Renault/packages/Runtime-IR-NT8183A-101-section.json`.

After validation of that generated JSON, next implementation target is a native Android 101 proof-of-concept renderer that consumes Runtime IR v2 instead of hidden `org/menu/nav` navigation.



## v0.5.8 direction — native Runtime IR preview + all-year menu audit — 2026-09-24

New user requirement:
later/newer Renault volumes contain additional menu items; conversion/native UI must not assume that the 2001 NT8183A menu is universal.

Architecture contract:
- menu is data, not hard-coded UI;
- converter emits arbitrary `action-bar.items[]`;
- Android renders whatever menu items exist in Runtime IR;
- normalized actions run natively;
- unknown `legacy-javascript` actions fall back to Classic;
- Classic remains unchanged.

Implemented on branch `feat/v0.5.8-native-runtime-ir-preview`:
- Runtime IR cross-year coverage audit:
  `_renault/runtime-ir-coverage.json`;
- coverage tracks menu labels, panel/control/action/document types, unsupported JS and warnings per volume/year;
- Renault Menu item 12 exports:
  `packages/Runtime-IR-Coverage.json`;
- Android `RuntimeIrReader` reads schema v2;
- generic `NativeSectionActivity` renders action-bar/select/document-list from JSON;
- generic open-panel/open-document execution;
- native structured HTML/table rendering;
- composite document support for nomenclature PDF + details;
- unknown legacy actions keep Classic fallback;
- Modern section tap now enters the native preview;
- v0.5.8 / versionCode 24.

Phone gate after CI:
1. point 5;
2. point 9;
3. point 12 and upload coverage;
4. install v0.5.8;
5. test Modern → NT8183A → 101 for native parity;
6. then inspect coverage before claiming later-year support.


## v0.5.8 merged / CI PASS — 2026-09-24

Merged source:
`187513adfbb2a183e167eae4ab6a7f8edf38ef69`

Main CI:
- Tests run `36029452241` — PASS;
- Android Debug APK run `36029452533` — PASS;
- artifact: `Renault-Docs-v0.5.8-Debug`;
- artifact id: `10820474101`;
- APK SHA-256:
  `30ed5c6405f2995d80a0d50afe6405d9cf5e67ef1a772205eb2422af603984bc`;
- artifact ZIP SHA-256:
  `e19f3eb32cc970d35aee10e231840f8d0356a054e856dff9f8cbe92018dd7b6b`.

Dataset refresh IS required because v0.5.8 adds Runtime IR coverage output.

Phone/data sequence:
1. Renault Menu → 5;
2. Renault Menu → 9;
3. Renault Menu → 12 and upload `Runtime-IR-Coverage.json`;
4. Renault Menu → 8 to install v0.5.8;
5. test Modern → NT8183A → 101 native renderer;
6. compare SCH/NM/PC/GENE/direct PDFs/CRITERE/NM composite behavior against Classic.

Do not claim all-year native support until the coverage report for the 10 real volumes is reviewed.

## Runtime IR coverage real dataset PASS + Laguna III future gate — 2026-09-24

User uploaded the real `Runtime-IR-Coverage.json`.

Validated Laguna II 2001–2006 coverage:
- 10 volumes;
- 2174 sections;
- all 2174 sections are `section-ir-v2`;
- unsupported actions: 0;
- warnings: 0.

Top-level menu vocabulary across the entire current corpus is only:
`AIDE, GENE, PLATFUSI, SCH, PC, NM, blank`.

This corrects the earlier suspicion that later Laguna II years might add new top-level menu labels. They do not in the current dataset. Availability of SCH/PC/NM still varies per section, so the renderer remains data-driven.

Current totals:
- panels: general 2174, menu 2174, schematic 2110, pc 1790, nomenclature 1393;
- controls: select 5677, action-bar 2174, document-list 1790;
- actions: route 62528, set-surface-location 16163;
- routes: open-document 55061, open-panel 7467;
- documents: pdf 47222, structured-html 8345, composite-document 4842.

Interpretation:
compiler-semantic coverage for the current Laguna II corpus is very strong. The next major risk is native-renderer parity across real sections.

Possible future Laguna III:
- if source is found, add it as a new dataset/model family;
- preserve Classic;
- run the same Runtime IR + coverage pipeline;
- compare topology/menu/control/action/document types before enabling native Modern;
- never assume Laguna III uses the same shell just because Laguna II is consistent.

Detailed finding:
`docs/architecture/RUNTIME_IR_COVERAGE_LAGUNA_II_2026-09-24.md`.

## v0.5.8 phone failure → v0.5.9 Runtime IR sharding hotfix — 2026-09-24

Real phone evidence:
- Modern → NT8183A → 101 could remain on `Читаю Runtime IR v2…`;
- failure message attempted allocation:
  `268501000 bytes`;
- Android heap growth limit shown by the device:
  `268435456` bytes.

Root cause:
v0.5.8 Android read the complete `_renault/runtime-tree.json` for all 10 volumes / 2174 sections using `readText()`.

This is an architecture bug, not corrupted Renault content.

Fix merged as app source:
`2ac19a941ce2f29d82d58e649cf332d343ab5a1d`

v0.5.9 / versionCode 25:
- full `runtime-tree.json` remains compiler/debug data;
- converter generates small `_renault/runtime-ir-index.json`;
- converter generates one JSON shard per section under
  `_renault/runtime-ir/sections/<volume>/<code>.json`;
- Android reads only the index + selected section shard;
- runtime reads are bounded to prevent giant accidental allocations;
- native/compiler `_renault/*.json` are excluded from legacy Fast Pack, avoiding duplicate huge Runtime IR data.

Dataset refresh is REQUIRED.

Phone gate:
1. Renault Menu → 5;
2. Renault Menu → 9;
3. Renault Menu → 8;
4. Modern → NT8183A → 101;
5. confirm the native menu appears without OOM/stall;
6. continue parity testing SCH/NM/PC/GENE/direct documents against Classic.

Docs:
- `docs/v.0.5.9/RELEASE.md`;
- `docs/v.0.5.9/qa/PHONE_TEST.md`.

v0.5.9 main CI:
- Tests run `36033113768` — PASS;
- Android Debug APK run `36033113767` — PASS;
- artifact `Renault-Docs-v0.5.9-Debug`;
- artifact id `10823058032`;
- APK SHA-256 `36f5b1af5458006d11543fe768275a8402053e8c8441c8366b11eb23f85203fa`;
- artifact ZIP SHA-256 `f7a029fac430ac636ee94e7e570a420042c26fd11a6c1d778a86c4604de35eea`.

## v0.5.10 native menu semantics + launcher icon — 2026-09-24

Real phone v0.5.9 result:
- sharded Runtime IR native preview works for NT8183A / 101;
- SCH selector renders;
- NM composite renders PDF action + native connector table;
- OOM from v0.5.8 is closed.

New UX requirements from phone test:
- SCH should be shown as `Схема`;
- NM as `Розʼєм`;
- PC as `Положення на авто`;
- GENE / PLATFUSI / AIDE are stable documentation actions and should be grouped separately;
- legacy `blank` should not be visible in Modern;
- CRITERE should have a human-readable label;
- linked/clickable rows must look different from headings/non-links;
- unresolved items must be visibly disabled instead of looking clickable.

Also observed:
native `Classic` button produced old hybrid DBG for target 105:
`phase=waiting`, `attempts=48`, `hasMenuWindow=false`, root stayed at `INDEX.HTM`.
Conclusion: the button named Classic must open pure untouched Classic, not hybrid target/projection mode.

Implemented and merged:
`ee850788ce8481df94cb88a3ef3b8b9a29c621cc`

v0.5.10 / versionCode 26:
- friendly primary menu labels;
- GENE/PLATFUSI/AIDE grouped under `Документація`;
- `blank` hidden;
- `CRITERE` → `Критерії / скорочення`;
- actionable rows use a `›` affordance;
- unresolved items show `немає посилання`;
- Classic button opens pure Classic with no modernSectionCode/hybrid target;
- app now declares a custom adaptive Renault Docs launcher icon.

Launcher icon concept:
dark technical background + blue document outline + white schematic traces + yellow connector node.
It is an original project symbol, not the Renault trademark.

Play Protect:
the screenshot warning is a sideload/unverified-developer warning. The custom icon does not remove it. The development APK is installed outside Google Play; public-warning removal requires normal Google Play developer verification/distribution/signing workflow. Do not disable Play Protect globally for testing.

Dataset refresh:
NOT required if the v0.5.9 sharded Runtime IR package is already on the phone.

CI:
- PR Tests `36037478915` — PASS;
- PR Android Debug APK `36037479322` — PASS;
- main Tests `36037676395` — PASS;
- main Android run `36037676490` was still running when this handoff entry was written.



## v0.5.10 main CI PASS — 2026-09-24

App source:
`ee850788ce8481df94cb88a3ef3b8b9a29c621cc`

Main CI:
- Tests run `36037676395` — PASS;
- Android Debug APK run `36037676490` — PASS;
- artifact `Renault-Docs-v0.5.10-Debug`;
- artifact id `10824924034`;
- APK SHA-256 `269a093bba30292ee1da63ffe62b8e20d753f34287316e5dfbd3a9507ba4078c`;
- artifact ZIP SHA-256 `dfe236f46ec24728761ec7ef8a8435a2aa3365c81c1789bc0a2c21d30ce69e39`.

Phone sequence:
- if v0.5.9 shards already exist: Renault Menu → 5, then → 8;
- no point 9 required for v0.5.10 UI/icon-only changes;
- re-test Modern → NT8183A → 101;
- verify friendly labels, Documentation grouping, link/non-link presentation, custom launcher icon;
- verify Classic button opens pure Classic with no targetSection hybrid wait.

## v0.5.10 native menu UX + launcher icon — 2026-09-24

Real phone evidence after v0.5.9:
- Runtime IR OOM is fixed;
- NT8183A/101 native SCH/NM/PC and nomenclature data render on device;
- old raw menu abbreviations and button-like static rows need UX cleanup;
- Play Protect warns about the sideloaded/unverified developer APK;
- Classic fallback DBG for target 105 could remain at INDEX.HTM with no child frames and hybrid phase waiting.

Implemented and merged as app source:
`ee850788ce8481df94cb88a3ef3b8b9a29c621cc`

v0.5.10 / versionCode 26:
- launcher/adaptive icon now exists; generic Android placeholder is gone;
- SCH → `Схема`;
- NM → `Розʼєм`;
- PC → `Положення на авто`;
- GENE/PLATFUSI/AIDE grouped under `Документація`;
- CRITERE → `Критерії / скорочення`;
- blank legacy placeholder hidden;
- actionable rows use `›`;
- unresolved actions are disabled/marked as having no link;
- Native Modern `Classic` opens pure Classic runtime, not the old hybrid target/projection flow.

Play Protect:
this is a distribution/developer-verification warning for the sideloaded test APK, not evidence from the screenshot that Runtime IR is malicious. Do not disable Play Protect globally. For development, manually approve only the project's own CI APK. For public distribution, move to verified developer/app registration or Google Play distribution.

Dataset refresh:
NOT required if v0.5.9 shards already exist.

Main CI:
- Tests run `36037676395` — PASS;
- Android Debug APK run `36037676490` — PASS;
- artifact `Renault-Docs-v0.5.10-Debug`;
- artifact id `10824924034`;
- APK SHA-256 `269a093bba30292ee1da63ffe62b8e20d753f34287316e5dfbd3a9507ba4078c`;
- artifact ZIP SHA-256 `dfe236f46ec24728761ec7ef8a8435a2aa3365c81c1789bc0a2c21d30ce69e39`.

Phone gate:
1. Renault Menu → 5;
2. Renault Menu → 8;
3. verify new launcher icon;
4. Modern → NT8183A → 101;
5. verify friendly labels, Documentation group, clickable/non-clickable distinction;
6. tap Classic and confirm plain Classic opens without hybrid target wait.

## Open finding — BUG-001 PDF toolbar width — 2026-09-24

Phone screenshot shows the PDF page using the available viewer width, while the upper PDF control frame (`200%`, zoom dropdown, fit-mode control) remains noticeably narrower and clips the right-side control.

Expected:
PDF toolbar/control frame should follow the PDF viewport width and adapt to screen/orientation.

Status:
OPEN — recorded only. Do not implement a fix until explicitly requested.

Canonical open-findings list:
`docs/assistant-kit/OPEN_FINDINGS.md`.

## v0.5.11 menu rows + NM document parity + Classic contrast — 2026-09-24

New real-phone requirements/findings:
- section menu layout should be stable:
  - row 1: Схеми + Розʼєм;
  - row 2: Положення на авто;
  - row 3: Документація;
- Classic NM shows two distinct child documents (drawing + pin/contact description);
- first Modern renderer incorrectly flattened the contact table directly into NM;
- Classic lower pin-description frame can be nearly unreadable because transparent legacy HTML sits over the dark WebView canvas;
- linked/action items and static pin data must be visually distinct.

Implemented on branch `feat/v0.5.11-nm-layout-and-classic-contrast`:
- fixed-row Modern menu presentation;
- NM composite presents explicit `Схема розʼєму` and `Опис контактів` actions;
- pin description opens separately;
- structured pin rows use plain static text rows;
- top-level non-hybrid Classic INDEX uses white WebView canvas for transparent legacy detail frames;
- v0.5.11 / versionCode 27;
- APK-only; no Runtime IR regeneration required.

Open BUG-001 PDF toolbar width is intentionally untouched.

## v0.5.11 merged / CI PASS — 2026-09-24

Merged source:
`93c722270a5670eeab6f8343d48ab51cb067f19b`

Main CI:
- Tests run `36041709175` — PASS;
- Android Debug APK run `36041708827` — PASS;
- artifact `Renault-Docs-v0.5.11-Debug`;
- artifact id `10826734430`;
- APK SHA-256 `a172ad458e5c91d3e981d39da3ae86a7a0c11a4e062e488d3b509b9c4417eb4f`;
- artifact ZIP SHA-256 `76b6a48ea2885f70a41cb1c00a4ffbbaa4956e2ce7fb97b05575d8683195debf`.

Phone test:
1. Renault Menu → 5;
2. Renault Menu → 8;
3. point 9 is NOT required;
4. Modern → NT8183A → 101;
5. verify rows:
   - Схеми + Розʼєм;
   - Положення на авто;
   - Документація;
6. open Розʼєм → variant and verify two actions:
   - Схема розʼєму;
   - Опис контактів;
7. open Опис контактів and confirm pin rows are static readable data;
8. open Classic and confirm lower connector pin-description frame is readable on a light canvas.

BUG-001 PDF toolbar width remains open and intentionally unchanged.

## v0.5.12 compact Modern + horizontal contact table + PDF toolbar width — 2026-09-24

Latest phone feedback:
- 101 and 107 menu layouts confirmed the data-driven absence/presence of NM;
- top section title should remain on one line;
- `Розділи` / `Classic` should be denser;
- legacy `CMP 101` heading should become `101 — <section title>`;
- connector contact/pin description must keep each source row on one visual line and pan horizontally;
- PDF toolbar still did not grow with a 150–200% zoomed document;
- PDF `+` should sit directly after the zoom dropdown.

Implemented on `feat/v0.5.12-density-pdf-table-polish`:
- one-line native section header with compact top actions;
- compact three-row Modern menu;
- native panel/composite heading uses `<code> — <title>`;
- structured contact tables use one-line monospace rows inside a HorizontalScrollView;
- PDF toolbar width is driven by the same zoomed display width as the page;
- PDF control order is minus → zoom input/dropdown → plus → fit-width → save;
- v0.5.12 / versionCode 28;
- APK-only; no point 9 required.

Findings:
- BUG-001 moved to fixed-in-v0.5.12 / phone-test-pending;
- UX-002 compact Modern header/menu;
- UX-003 horizontally pannable one-line contact table.

## v0.5.12 merged / CI PASS — 2026-09-24

Merged source:
`fa6963d05412def8ff917184e9a30537e06a2300`

Main CI:
- Tests run `36051860932` — PASS;
- Android Debug APK run `36051860854` — PASS;
- artifact `Renault-Docs-v0.5.12-Debug`;
- artifact id `10831137351`;
- APK SHA-256 `f892238a66d5aab02392fc3036d7e7ba212072ad6e93e7ac8e91f25745cf8c33`;
- artifact ZIP SHA-256 `5607791d73e4d44ebc9984a7030947a6e82cee7019c0d3e39085e7637ea8bc59`.

Phone gate:
1. Renault Menu → 5;
2. Renault Menu → 8;
3. point 9 is NOT required;
4. validate compact one-line section header and reduced menu density;
5. validate `101 — <title>` instead of `CMP 101`;
6. open connector contact description and verify single-line rows + horizontal pan;
7. open PDF at 100%, then 150/200%;
8. verify `+` is immediately after the zoom dropdown and toolbar width follows the zoomed PDF.

BUG-001 PDF toolbar width is implemented in v0.5.12 and awaits phone confirmation.

## Classic connector pin description — phone PASS / Modern visual reference — 2026-09-24

Latest phone screenshot confirms the Classic nomenclature pin/contact description is now clearly readable on the light canvas.

BUG-002 is therefore PHONE PASS.

The Classic layout is now the reference target for Modern `Опис контактів`:
- one logical source row per visual line;
- compact row height;
- clear column structure;
- long content remains horizontally accessible instead of wrapping into tall cards.

No code change was made from this observation; it records the accepted visual target for the next Modern comparison.

## Latest APK folder handoff — 2026-09-24

Phone-side APK handoff now also opens the exact build folder automatically after Renault Menu item 7/8 finishes downloading and verifying the APK.

New helper:
`tools/termux/reno-open-latest-apk.sh`.

Renault Menu item:
`13 — Відкрити папку останнього APK`.

Behavior:
- point 8 downloads/verifies the APK into
  `/storage/emulated/0/Documents/Renault/packages/Renault-Docs-vX.Y.Z-build/`;
- immediately opens that folder in Android DocumentsUI/file manager when possible;
- point 13 can reopen the newest Renault Docs build folder later without searching manually;
- if folder VIEW is unavailable, falls back to OPEN_DOCUMENT_TREE at that folder, then to opening the APK itself.

## BUG-004 — Modern catalog is incomplete vs Classic — 2026-09-24

Phone comparison shows Classic navigation entries such as `1405`, `1406`, `R15`, `R21`, `R262`, etc. are absent from the Modern volume catalog.

Root cause is confirmed in `core/sections.py`:
section discovery regex accepts only exactly three digits (`\d{3}`).

This means the current Modern count is not a full fidelity copy of Classic navigation.

Required architecture correction:
- catalog identifiers are strings, not 3-digit integers;
- preserve source order;
- include any valid navigation entry with a resolvable local target;
- avoid code-only deduplication when two entries can point to different targets;
- regenerate Modern/Runtime IR package after the fix.

Tracked as BUG-004 in `docs/assistant-kit/OPEN_FINDINGS.md`.

## v0.5.13 native section chrome / persistent tiles — 2026-09-24

Latest requested Modern UX:
- topmost row should expose the main app actions: back, Home, Search, Settings;
- second row should show only the current section code plus a civilized Modern/Classic switch;
- avoid repeating the section title in that row because the content already shows `<code> — <title>`;
- primary menu positions must never jump/disappear between sections;
- unavailable actions stay visible as disabled/dimmed tiles;
- selector/content groups should use tile/card presentation.

Implemented on `feat/v0.5.13-modern-section-tiles`:
- global navigation row in NativeSectionActivity;
- section code-only context row;
- segmented Modern/Classic switch;
- search reopens the current Modern volume with search visible;
- persistent fixed primary tiles for SCH/NM/PC/Documentation;
- unknown future actions still render below the fixed group;
- grouped select choices render inside rounded content tiles;
- v0.5.13 / versionCode 29;
- APK-only; no point 9 required.

BUG-004 (Modern catalog missing non-3-digit Classic entries) remains separate and open.

## v0.5.13 merged / CI PASS — 2026-09-24

Merged source:
`c6ab899042f016dd0e8bf6d7f7e463de8d2365e4`

Main CI:
- Tests run `36057491536` — PASS;
- Android Debug APK run `36057491412` — PASS;
- artifact `Renault-Docs-v0.5.13-Debug`;
- artifact id `10832474678`;
- APK SHA-256 `1f8adeca1a1c0695c337caafa624d7913b6355db393815fa07e00edb9afbfc4d`;
- artifact ZIP SHA-256 `6bc4d0a25db7f5e1f43896010477716cafc26c1d0f3777d156153e74a06fd36c`.

Phone gate:
1. Renault Menu → 5;
2. Renault Menu → 8;
3. point 9 is NOT required;
4. open Modern → NT8183A → 101;
5. verify top global Back/Home/Search/Settings row;
6. verify second row is only section code + Modern/Classic switch;
7. verify all four primary menu tiles remain in fixed positions across 101 and 107;
8. verify unavailable Connector tile stays visible but disabled;
9. verify Scheme selector groups render as tiles/cards.

BUG-004 catalog completeness remains open and separate.

## Phone findings after v0.5.13 — 2026-09-25

v0.5.13 native layout result:
- 101 tile grouping looks good on phone;
- 107 keeps the Connector tile in the same position and correctly shows it disabled;
- Modern/Classic segmented control and the global Home/Search/Settings row are visually accepted;
- grouped Scheme choices are substantially clearer as tiles.

Converter catalog finding expanded:
Classic identifiers are not merely 3-digit or R-prefixed numbers. Real phone evidence also contains alphabetic codes such as `MAH`, `MYH`, `MA`, `MB`, `ME`, `MG`, `MH`, `ML`, `MQ`, `MT`, `MW`, `NA`, `NC`, `NH`, `NT`, `NU`, plus `R325`.
Therefore the converter must treat section/catalog identifiers as opaque source strings.

New BUG-005:
Classic abbreviations are a readable two-column glossary (`СОКРАЩЕНИЯ | ПОЛНЫЕ НАИМЕНОВАНИЯ`), while Modern currently renders the same data as a clipped generic monospace row stream.
Modern needs a dedicated two-column glossary renderer; this must remain distinct from the one-line horizontally pannable connector pin table.

BUG-004 and BUG-005 are now the next converter/renderer parity targets.

## v0.5.14 unified structured tables + PDF export — 2026-09-25

Latest phone request:
- pin descriptions and abbreviations should use the same table-style data presentation;
- use the readable Classic table behavior as the reference;
- long descriptions should wrap inside table cells instead of being clipped;
- structured tables should be saveable as PDF;
- connector description should present `101 — ПРИКУРИВАТЕЛЬ` and the criteria line similarly to the useful Classic hierarchy.

Implemented on `feat/v0.5.14-native-tables-pdf`:
- one shared native grid renderer for Runtime IR structured tables;
- 2-column glossary layout and 4-column pin/contact layout share the same component;
- legacy `CMP101` is not exposed as the Modern heading;
- section code/title and extracted criteria are rendered explicitly;
- extra connector/location headings are grouped into a compact metadata card;
- `Зберегти таблицю PDF` uses ACTION_CREATE_DOCUMENT;
- new `NativeTablePdfExporter` writes paginated Android PdfDocument output;
- v0.5.14 / versionCode 30;
- APK-only, no point 9 required.

BUG-004 converter/catalog completeness remains open and separate.

## v0.5.14 merged / CI PASS — 2026-09-25

Merged source:
`66702baec93d3747e8d2b70808177a3746b82ca2`

Main CI:
- Tests run `36065777800` — PASS;
- Android Debug APK run `36065777851` — PASS;
- artifact `Renault-Docs-v0.5.14-Debug`;
- artifact id `10836740287`;
- APK SHA-256 `1c3177fed7ce7e5c086a2b9f39cb2df78c19787959dea06fc21a559764308788`;
- artifact ZIP SHA-256 `46950313f8726ee28afed6b44b1764f6df9f73ba640f079f3c5f682795756e4d`.

Phone gate:
1. Renault Menu → 5;
2. Renault Menu → 8;
3. point 9 is NOT required;
4. 101 → Критерії / скорочення: verify native 2-column table and wrapped descriptions;
5. 101 → Розʼєм → variant → Опис контактів: verify shared table style, section heading + criteria + connector metadata;
6. tap Зберегти таблицю PDF and verify the created PDF opens and paginates correctly.

BUG-004 converter/catalog completeness remains open.

## Canonical project documentation rule — 2026-09-25

User requirement: document **all** completed changes and all known future work; do not leave project state only in chat.

Added canonical consolidated ledger:
`docs/assistant-kit/PROJECT_LEDGER.md`.

It now records:
- Classic and Modern architecture direction;
- Runtime IR / sharding / converter rules;
- accepted native tile/navigation UI;
- connector/NM and structured-table contracts;
- PDF/table export behavior;
- PDF viewer and Classic readability fixes;
- BUG-004 full-catalog converter problem;
- identifier grammar (numeric, R-prefixed, alphabetic);
- current release/build baseline;
- APK/Termux workflow including latest APK folder opener;
- Play Protect/icon notes;
- historical hybrid lessons;
- complete current pending-work list, including future Laguna III compatibility if source material is found.

Updated `RELEASE_DOCUMENTATION_CONTRACT.md` so future work must update the ledger when architecture, accepted behavior, workflow or pending work changes.

Future sessions must read `PROJECT_LEDGER.md` via `CONTEXT_FILES.txt`.

Rule: chat history, PR text and commit messages are supplementary only; repository docs are the source of project state.

## v0.5.14 phone partial PASS → v0.5.15 refinements — 2026-09-25

Real phone evidence from v0.5.14:
- structured table PDF export succeeds;
- Android save picker works;
- saved PDF opens in the system PDF viewer;
- Cyrillic table content renders;
- native two-column glossary is substantially more readable than the old pipe-separated renderer;
- connector menu still needs a combined scheme+pins action;
- table column widths are still too rigid;
- rows such as E2/E3 with a wrapped right-hand description should keep one full-height left cell and center the short code vertically;
- generic export filename `Renault_101_table.pdf` is not useful enough.

Implemented on `feat/v0.5.15-table-layout-combined-connector`:
- shared content-aware `NativeTableLayout` for native and PDF table column proportions;
- 2-column exported tables use portrait page geometry; wider tables keep landscape;
- compact cells use full row height and center text vertically when the description wraps;
- PDF table text blocks are vertically centered inside each row;
- combined connector action `Схема + піни розʼєма` opens the connector composite source directly in the app viewer;
- separate `Схема розʼєму` and `Опис контактів` actions remain;
- structured PDF suggested names derive from the source path when possible:
  - `101(pins).pdf`;
  - `101_1(pins).pdf`;
  - `101(abbreviations).pdf`;
- v0.5.15 / versionCode 31;
- APK-only; Renault Menu → 9 is not required.

BUG-004 full Modern catalog remains separate converter work.

## v0.5.15 merged / CI PASS — 2026-09-25

Merged source:
`cd4d0cbcc78d2623488f8d50367c91b94c0e041b`

Main CI:
- Tests run `36072358387` — PASS;
- Android Debug APK run `36072358369` — PASS;
- artifact `Renault-Docs-v0.5.15-Debug`;
- artifact id `10838243576`;
- APK SHA-256 `7e2ca52f7e37cfa5844133e331fec0f58561f33538146b45e4b9096b423841b9`;
- artifact ZIP SHA-256 `419916d1914c4b62d35ccd6e949d319baae086c283d4adb5a17f1f88f788fe9b`.

Phone sequence:
1. Renault Menu → 5;
2. Renault Menu → 8;
3. point 9 is NOT required;
4. validate 101 abbreviation table adaptive width + E2/E3 row alignment;
5. save abbreviation PDF and verify portrait layout + `101(abbreviations).pdf`;
6. open 101 → Розʼєм → variant and verify `Схема + піни розʼєма`;
7. verify combined connector view shows scheme + pins together;
8. open `Опис контактів`, export PDF, verify `101(pins).pdf` or source variant such as `101_1(pins).pdf`;
9. later explicitly validate a multi-page export.

BUG-004 converter/catalog completeness remains open and requires a future point-9 package regeneration.



## v0.5.16 connector identity correction — 2026-09-25

New source analysis clarified that Renault suffixes `_1`, `_2`, `_3` are real source identifiers, not duplicate-file counters, and their meaning depends on context.

Examples:
- `101_1` / `101_2`: applicability variants tied to criteria such as DD/DG/E2/E3 and other equipment criteria;
- `120_1` / `120_2` / `120_3`: may be separate physical connectors of one ECU.

Implementation on `fix/v0.5.16-connector-id-export`:
- keep the full pin-table source stem as opaque connector identity;
- strip only the legacy technical `T_` prefix from sources such as `T_101_1.HTM`;
- stop reconstructing connector IDs from `sectionCode + optional numeric suffix`;
- use the exact requested suffix `(pines)` for pin/contact PDF exports;
- examples: `101_1(pines).pdf`, `101_2(pines).pdf`, `120_1(pines).pdf`, `120_2(pines).pdf`, `120_3(pines).pdf`;
- never manufacture `_1/_2/_3` as conflict counters;
- bump candidate to v0.5.16 / versionCode 32;
- APK-only; Renault Menu → 9 is not required.

v0.5.15 adaptive table widths, vertical row centering and combined connector view remain unchanged and must be rechecked in the v0.5.16 phone gate.


## v0.5.16 merged / CI PASS — 2026-09-25

Merged source:
`b66ecb1a5f0e35250470b4e30ae580062e710121`

Main CI:
- Tests run `36078619569` — PASS;
- Android Debug APK run `36078619634` — PASS;
- artifact `Renault-Docs-v0.5.16-Debug`;
- artifact id `10841531621`;
- APK SHA-256 `cf5c2cff2e66fcd3844ee837b302484e27219b3e0686ce803b713b8fe2234134`;
- artifact ZIP SHA-256 `6695b817fb280d80e0176a021c59920116b6f95fa4eb24b70934e38dfbccee9f`.

Phone gate:
1. Renault Menu → 5;
2. Renault Menu → 8;
3. point 9 is NOT required;
4. validate adaptive abbreviation columns and E2/E3-style vertical centering;
5. validate `101(abbreviations).pdf`;
6. open 101 → Розʼєм → variant and validate `Схема + піни розʼєма`;
7. export each available 101 pin variant and verify exact source-derived names such as `101_1(pines).pdf` / `101_2(pines).pdf`;
8. if section 120 variants are available in the chosen volume, verify full IDs such as `120_1(pines).pdf`, `120_2(pines).pdf`, `120_3(pines).pdf`;
9. explicitly test one long multi-page table export.

BUG-004 remains the next major converter/data task after this phone gate.

## v0.5.16 phone result → v0.5.17 PDF-header fix — 2026-09-25

Real phone screenshots/results confirm:
- 101 Modern connector menu shows all three actions: `Схема + піни розʼєма`, `Схема розʼєму`, `Опис контактів`;
- combined connector view opens and shows scheme + pin/contact content together;
- native abbreviations table uses the adaptive two-column layout and E2/E3-style tall rows keep the short code centered;
- saved pin PDFs preserve the exact Renault IDs: `101_1(pines).pdf` and `101_2(pines).pdf`;
- section 120 variants were not found in the tested navigation, so that optional gate is NOT TESTED rather than failed.

New phone finding:
generated structured-table PDFs do not reliably show the table column-name header. Code inspection also confirmed that table headers were not repeated after a pagination break.

Implemented on `fix/v0.5.17-pdf-table-headers`:
- preserve leading source header rows separately;
- render them before table body;
- repeat them after every page break;
- do not invent headers for headerless source tables;
- keep adaptive widths, portrait/landscape policy, vertical centering and v0.5.16 opaque connector filenames unchanged;
- bump to v0.5.17 / versionCode 33;
- APK-only; Renault Menu → 9 is not required.

## Additional v0.5.16 connector-ID phone evidence — 2026-09-25

Real phone evidence now includes section 120 with the exact source-derived pair:
- `120_18.PDF` — connector drawing;
- `120_18(pines).pdf` — exported pin/contact table.

Interpretation:
- the opaque connector-ID contract is PHONE PASS with a coverage limitation (`PASS*`);
- the observed suffix is `_18`, proving the suffix is not limited to small values such as `_1/_2/_3` and must never be modeled as a duplicate counter;
- `120_18` is preserved exactly through the pin-PDF filename path;
- this does not prove exhaustive coverage of every 120 connector variant; only the observed `120_18` variant is phone-confirmed.

Future 120 tests should accept any real source ID such as `120_18`, `120_2`, etc. The test must compare the exported filename against the actual source connector ID rather than expect a fixed `_1/_2/_3` sequence.


## STOP POINT — 2026-09-25

Canonical restart file:
`docs/assistant-kit/SESSION_CHECKPOINT_2026-09-25.md`

Current APK candidate:
- v0.5.17 / versionCode 33;
- app source `b56b29b64cddd0cdf11531e7a1e543c45c5593e9`;
- main Tests `36081274501` PASS;
- main Android Debug `36081274495` PASS;
- artifact `Renault-Docs-v0.5.17-Debug`;
- artifact id `10841149488`;
- APK SHA-256 `a72784d9c5519e0830c30ec2f213c20f7c622a33f5112d2c9ae2f5d46b4237ed`;
- artifact ZIP SHA-256 `5fd8223e9a70b6cbf65e519640140923ea7197181018a69276d2bd84d9b671d7`.

Stop state:
- v0.5.16 connector/menu/native table work is phone-confirmed;
- opaque connector ID export is PASS* with real examples `101_1`, `101_2`, and `120_18`;
- BUG-006 PDF table-header fix is implemented in v0.5.17 and is the immediate next phone gate;
- point 9 is not required for v0.5.17;
- after BUG-006 PASS, resume BUG-004 full Classic catalog converter parity.


## v0.5.17 phone finding → v0.5.18 — 2026-09-25

Real phone test in section `103`, connector/contact variant `103_5`:

Observed:
- saved file name `103_5(pines)` is correct;
- generated PDF still has no visible column-name header;
- native table renders wire cross-section `0.6` as two lines (`0.` + `6`).

Interpretation:
- v0.5.17 pagination/header renderer works only when Runtime IR already marks header rows;
- this legacy pin table does not provide a usable semantic header in current Runtime IR;
- fix belongs partly in converter, not only PDF rendering.

v0.5.18 implementation:
- preserve empty table cells during conversion so columns stay aligned;
- for 4-column `T_*.HTM` connector-contact tables, normalize Runtime IR header to `№ | мм² | Код | Опис`;
- if first row is actual data, prepend header instead of replacing it;
- widen wire cross-section column;
- keep first three compact pin columns single-line in native UI and PDF;
- version `0.5.18` / code `34`;
- converter/data package changed: after APK update, Renault Menu → 9 IS REQUIRED.

Immediate phone gate after CI:
1. point 5;
2. point 8;
3. point 9;
4. re-open 103 → connector 103_5 → pin description;
5. confirm header exists and `0.6` is one line;
6. export `103_5(pines).pdf` and confirm same header + compact values;
7. re-check one 101 abbreviations PDF and one 101 pin PDF.


## v0.5.18 phone result → v0.5.19 — 2026-09-25

Real phone result:
- `103_5`: wire cross-section `0.6` now stays on one line — PASS;
- `103_5` / `108`: semantic pin-table header is still missing — FAIL;
- 2-column compact cells can still wrap values/header labels (for example B74,K74 / glossary header);
- PDF pin layout leaves too much width in the first compact columns.

v0.5.19 implementation:
- detect pin tables from row data shape instead of `T_` filename;
- semantic header: `№ | мм² | Код | Опис`;
- first real data row is never discarded;
- visual-only header rows before data are replaced by one semantic header;
- 2/3/4+ column compact widths consider both header and body content;
- all columns before the final description column are single-line;
- final column receives the remaining width;
- same width fractions are used by native UI and PDF;
- v0.5.19 / code 35;
- converter changed, so point 9 is REQUIRED after APK update.

Performance/testing note:
7 unused converted volume folders may be moved temporarily outside `laguna 2 2001-2006_android` to a sibling hold directory so point 9 processes only the remaining 3 test volumes. Restore them and run point 9 again before final full-dataset validation.


## v0.5.19 merged / CI PASS — 2026-09-25

Merged source:
`930b20d240fff0a2e0f7dd5e60769654241a23c9`

Main CI:
- Tests `36133711994` — PASS;
- Android Debug `36133712023` — PASS;
- artifact `Renault-Docs-v0.5.19-Debug`;
- artifact id `10862578973`;
- APK SHA-256 `03eb2a9f1edc9f5d0dcbe2aabad4090e433462ffcb866bcc38e610bedd682ab0`;
- artifact ZIP SHA-256 `0e797cdfb03765ffb0115a8bb38bb4c7b6cc3018f836dfb0e25cd560189d150b`.

Next phone sequence:
1. optionally move 7 unused converted volume folders OUTSIDE the `...2001-2006_android` build root, leaving 3 test volumes;
2. Renault Menu → 5;
3. Renault Menu → 8;
4. install v0.5.19;
5. Renault Menu → 9 — REQUIRED;
6. test 103_5 and 108;
7. export 103_5(pines).pdf;
8. check 101 abbreviations header;
9. after final testing restore all volumes and rerun point 9 for the complete dataset.


## v0.5.20 pines PDF composition — 2026-09-25

User-approved target based on real Renault source screenshots:
- rebuild `(...pines).pdf` as three stacked blocks;
- connector/application card first;
- section identity + criteria second;
- pin table third;
- add both text labels and vector technical icons;
- preserve connector/applicability information between the source tables.

Implementation on `feat/v0.5.20-pines-pdf-layout`:
- pines PDF forced to portrait;
- connector card reconstructed from non-pin structured table rows;
- connector pictogram drawn as vector;
- centered section code/title/criteria;
- header icons + text for `№ / мм² / Код / Опис`;
- first three columns measured from real content/header text;
- description fills the remaining width;
- first three body values centered; description left-aligned;
- continuation pages repeat compact identity + table header only;
- v0.5.20 / versionCode 36;
- APK-only.

Phone sequence after CI:
1. Renault Menu → 5;
2. Renault Menu → 8;
3. install v0.5.20;
4. if the current dataset was already regenerated with v0.5.19, DO NOT run point 9 again;
5. export `105_*(pines).pdf` and `108_1(pines).pdf`;
6. compare composition with the Renault reference: connector card → identity/criteria → pin table;
7. verify connector/application values such as `ЭЛПРОВ. САЛОНА / B74,K74`, `ЭЛПРОВ. ДВИГ. / F5R700`, `DD` remain visible;
8. verify icons + text header;
9. verify portrait proportions and compact first three columns.


## v0.5.20 merged / CI PASS — 2026-09-25

Merged source:
`094698411f6af56a62dcf306c4c4c24b49e6af8c`

Main CI:
- Tests `36142685738` — PASS;
- Android Debug `36142685726` — PASS;
- artifact `Renault-Docs-v0.5.20-Debug`;
- artifact id `10868375871`;
- APK SHA-256 `d3a898085c6b2dc3c3e19c58e82b3fdc27de75fefff5ea5a5e10f32bd47016fc`;
- artifact ZIP SHA-256 `d480d5ff7717e633af0b76655567a660c2004bb7db7f32eaa5f10d3deecc4a5b`.

Phone test now:
- Renault Menu → 5;
- Renault Menu → 8;
- install v0.5.20;
- point 9 NOT required if v0.5.19 package regeneration was already completed;
- export 105 / 108 pines PDFs and compare with the Renault reference composition.


## v0.5.21 table/PDF polish — 2026-09-25

Real phone review of v0.5.20:
- composed pines PDF is functionally accepted;
- native pin table still needs guaranteed visible header;
- 3rd compact column is too wide;
- PDF needs ~2 cm margins and lower top placement;
- icons should be cleaner/more modern and colored;
- abbreviations PDF title should be centered;
- abbreviations table should have more gap below the title;
- first abbreviations column should be content-sized.

Implementation on `feat/v0.5.21-pdf-polish`:
- semantic native pin header fallback `№ | мм² | Код | Опис`;
- tighter NativeTableLayout compact fractions;
- PDF margin `57 pt` (~2 cm);
- centered generic PDF title/criteria;
- larger title→table spacing;
- tighter measured pines columns;
- colored modern vector icon accents + soft badges;
- v0.5.21 / versionCode 37;
- APK-only; point 9 not required if current dataset was already regenerated with v0.5.19.

Phone gate after CI:
1. point 5;
2. point 8;
3. install v0.5.21;
4. do NOT run point 9;
5. check native 108/105 pin table header and 3rd-column width;
6. export 108_1(pines).pdf and inspect 2 cm margins, lower placement, colored icons, compact columns;
7. export 105(abbreviations).pdf and inspect centered title, title/table gap, ~2 cm margins, compact first column.


## v0.5.21 merged / CI PASS — 2026-09-25

Merged source:
`ad9d69cb05697e960f2fc2800f28f7a95f99a4e8`

Main CI:
- Tests `36146178858` — PASS;
- Android Debug `36146178924` — PASS;
- artifact `Renault-Docs-v0.5.21-Debug`;
- artifact id `10869810678`;
- APK SHA-256 `934f9791804b65725f75f4b9db7be6e8701f394e646d4ee854fda79adbaf58ea`;
- artifact ZIP SHA-256 `ae29db796261f2e3b7db26f4f290f414bb8d964c6222cf7e76f7053deec87ef2`.

Phone test now:
- Renault Menu → 5;
- Renault Menu → 8;
- install v0.5.21;
- point 9 NOT required;
- compare native 108/105 and exported 108_1(pines).pdf / 105(abbreviations).pdf against the latest phone feedback.


## v0.5.22 implementation — 2026-09-25

Trigger:
- v0.5.21 native table columns still clip real values despite all fields/header being present;
- first-column abbreviations/designations and table headers should be bold;
- user reports many windows disappear on phone rotation and fall back to the parent screen.

Implemented on `fix/v0.5.22-lifecycle-table-widths`:
- native compact columns measured in real Android pixels from content + header;
- final description column gets the remainder;
- 2-column designations/abbreviations bold in native and generic PDF;
- native headers bold/high contrast;
- NativeSection panel/document/Documentation state saved/restored;
- pending NativeTablePdfData survives Activity recreation during Android Save-document flow;
- Settings chooser dialogs saved/restored;
- Viewer section navigator + query saved/restored;
- Viewer Frame debug regenerated/restored;
- full audit matrix added at `docs/v.0.5.22/LIFECYCLE_AUDIT.md`;
- v0.5.22 / versionCode 38;
- APK-only; point 9 NOT required.

Phone gate after CI:
1. point 5;
2. point 8;
3. install v0.5.22;
4. do not run point 9;
5. verify native 108 widths + bold header/designations;
6. execute all rotation scenarios in LIFECYCLE_AUDIT.md.


## v0.5.22 merged / CI PASS — 2026-09-25

Merged source:
`b5e685d0b6c6d635ce64132685d00ccacc431007`

Main CI:
- Tests `36150581053` — PASS;
- Android Debug `36150581047` — PASS;
- artifact `Renault-Docs-v0.5.22-Debug`;
- artifact id `10871432429`;
- APK SHA-256 `f7619833cae34fa552cda3626e5770a9f3ae93bafe40adb38d0fa855258ce9a4`;
- artifact ZIP SHA-256 `a7a6e0af3d69f7ce1cf16de117def9fd51091b20edbd6e565e4f12e4f1dff13d`.

Phone sequence:
- Renault Menu → 5;
- Renault Menu → 8;
- install v0.5.22;
- point 9 NOT required;
- test native 108 measured widths/bold header;
- run the full rotation/lifecycle matrix from `docs/v.0.5.22/LIFECYCLE_AUDIT.md`.


## v0.5.23 implementation — 2026-09-25

Phone feedback:
- v0.5.22 portrait 108 table: looks good;
- landscape should collapse to only the active top-level mode button;
- PDF columns 1-3 should be bold;
- PDF table header should be +4 pt;
- upper connector-info table should match that header size.

Implemented on `fix/v0.5.23-landscape-focus-pdf-type`:
- active top-level native menu label/action saved across rotation;
- focused landscape hides Back/Home/Search/Settings, section chrome and Modern/Classic;
- focused landscape shows only the active mode button full-width;
- portrait restores complete chrome;
- pines PDF body stays 10 pt;
- columns 1-3 use 10 pt bold;
- table header uses 14 pt bold;
- upper connector-info table uses 14 pt bold;
- Android font family: `Typeface.SANS_SERIF`;
- v0.5.23 / versionCode 39;
- APK-only; point 9 NOT required.

Phone gate after CI:
1. 5 → 8 → install v0.5.23;
2. do not run point 9;
3. rotate each of Схеми / Розʼєм / Положення на авто / Документація;
4. verify only active top-level button remains in landscape and portrait restores all chrome;
5. export 108_1(pines).pdf and verify 10/14 pt hierarchy and bold columns 1-3;
6. continue the wider v0.5.22 lifecycle audit afterwards.


## v0.5.23 merged / CI PASS — 2026-09-25

Merged source:
`087efd7a61acfcd573af7abc4c14d99e8468aa25`

Main CI:
- Tests `36155433400` — PASS;
- Android Debug `36155433415` — PASS;
- artifact `Renault-Docs-v0.5.23-Debug`;
- artifact id `10874000361`;
- APK SHA-256 `937a6e13e4e47c53a66637140a367d7f89b773d26e22fa7afae36d3066a2abc1`;
- artifact ZIP SHA-256 `66268214f0bfe68df90a6ba1e37a0913798fe28c6b73badb987bcdd08f3126ec`.

Phone sequence:
- Renault Menu → 5;
- Renault Menu → 8;
- install v0.5.23;
- point 9 NOT required;
- verify landscape focus for all four top-level native modes;
- export 108_1(pines).pdf and verify 10 pt body / 10 pt bold technical columns / 14 pt header + upper connector-info table;
- continue the wider rotation audit afterwards.


## Lifecycle phone progress — 2026-09-25

User reports the just-tested NativeSection rotation sequence as PASS.

Interpretation for project state:
- NativeSection child-state restoration is working on the tested phone path;
- no parent-screen fallback observed in the current checks;
- BUG-009 stays open because the full matrix is not finished.

Next gate:
- Settings → test all three app-owned chooser dialogs through rotation:
  1. default open mode;
  2. PDF default zoom;
  3. PDF zoom-step.


## Lifecycle phone progress — Settings PASS

User result: `+,+,+`.

Confirmed on phone:
- default open mode chooser — PASS;
- PDF default zoom chooser — PASS;
- PDF zoom-step chooser — PASS.

Next gate:
Viewer → open `Розділи`, enter a non-empty search query, rotate, and verify the same dialog/query remain.


## Lifecycle phone progress — Viewer navigator/search PASS

User result: `+`.

Confirmed on phone:
- Viewer `Розділи` dialog survives rotation;
- entered search query survives;
- filtered state survives.

Remaining lifecycle gates:
- Viewer Frame debug;
- current PDF rotation;
- table-PDF save/system-picker handoff.


## Lifecycle audit correction — Frame debug is not user-reachable

Current main/v0.5.23 code was rechecked.

`DBG` exists only inside Viewer hybridSectionMode. No current normal navigation path supplies the required `modernSectionCode` while opening the volume-root Viewer, so the button is not reachable from the present UI.

Do not ask the phone tester to search for DBG. Treat this phone gate as N/A unless hybrid Viewer navigation is reintroduced later.

Remaining reachable lifecycle gates:
- current PDF survives rotation;
- table-PDF save/system picker handoff survives Activity recreation.


## Lifecycle closeout — BUG-009 CLOSED

User result for the final two gates: `+,+`.

Confirmed on phone:
- current PDF survives rotation — PASS;
- table-PDF save/system-picker handoff survives rotation and completes — PASS.

Combined with earlier passes:
- NativeSection child state — PASS;
- Settings choosers 3/3 — PASS;
- Viewer section navigator + query — PASS;
- Frame debug — N/A in current reachable UI.

BUG-009 is now CLOSED for the current reachable UI.

Next planned work:
1. UX-016 — built-in PDF viewer quality + rotation re-render latency;
2. converter UX/execution path;
3. BUG-004 — full Classic catalog converter parity (converter/Runtime-IR change; point 9 required when that work lands).


## v0.5.24 PDF viewer quality/performance implementation — 2026-09-25

Next work after BUG-009 closeout is UX-016.

Implemented on `fix/v0.5.24-pdf-viewer-quality-cache`:
- JPEG 92 → lossless PNG;
- DPR cap 2 → 2.5;
- max render width 1800 → 2400 px;
- minimum preferred target 700 → 800 px;
- 200 px render-width buckets;
- shared 32 MiB compressed-page LRU survives Activity recreation;
- cache namespace includes dataset tree URI;
- current page requested before neighbors;
- neighbor loading delayed 60 ms;
- old bitmap remains visible during sharper re-render;
- resize/orientation re-render debounce 120 ms;
- lazy prefetch root margin 1200 → 700 px;
- v0.5.24 / versionCode 40;
- APK-only; point 9 NOT required.

Phone comparison after CI:
1. 5 → 8 → install v0.5.24;
2. use the same PDF as before;
3. compare 100/150/200% sharpness;
4. rotate portrait↔landscape twice and compare first vs repeat latency;
5. scroll 4–5 pages, zoom and rotate for OOM/stability;
6. verify save/back/lifecycle regressions.


## v0.5.24 merged / CI PASS — 2026-09-25

Merged source:
`c8decdb903c9b48325bdeb55e2c1e1413b8844a1`

Main CI:
- Tests `36163857309` — PASS;
- Android Debug `36163857157` — PASS;
- artifact `Renault-Docs-v0.5.24-Debug`;
- artifact id `10876995104`;
- APK SHA-256 `2df8bef3156341710d8c4c12e93fcc9eccd5d6758834f48f881db5ef35d7cb57`;
- artifact ZIP SHA-256 `e6c9e23d7eb6fc939681bdcfa39486c671cdf3d323ab4f27d22b06d4379a995c`.

Phone test now:
- 5 → 8 → install v0.5.24;
- point 9 NOT required;
- use same PDF previously judged soft/slow;
- compare 100/150/200% sharpness;
- rotate twice and compare first vs repeat latency;
- scroll multiple pages and stress zoom/rotation for stability.


## v0.5.24 phone result → v0.5.25 implementation — 2026-09-25

v0.5.24 phone:
- quality ++;
- first rotation +;
- second rotation ++;
- 100% multi-page scroll acceptable;
- 200% on ~20-page PDF still not smooth enough.

User UX request:
- PDF controls separate from the document;
- controls never disappear/move during PDF scroll or horizontal zoom pan;
- portrait controls on two rows;
- landscape controls on one row.

Implemented on `fix/v0.5.25-pdf-toolbar-scroll`:
- independent fixed toolbar + nested `#pdfViewport`;
- portrait two toolbar rows;
- landscape one toolbar row;
- toolbar no longer follows zoomed document width;
- IntersectionObserver roots moved to PDF viewport;
- high zoom >=150% uses a temporary <=1600 px interactive render while scrolling, then full quality after 180 ms idle;
- high zoom >=150% prefetch/retention reduced to current ±1;
- far decoded page images evicted while page placeholders remain;
- lazy margin reduced to 300 px;
- short-lived per-dataset/PDF local state restores page/zoom/scroll after rotation;
- v0.5.25 / versionCode 41;
- APK-only; point 9 NOT required.

Phone gate after CI:
1. 5 → 8 → install v0.5.25;
2. no point 9;
3. verify portrait two-row controls stay fixed while PDF scrolls/pans;
4. rotate and verify one-row landscape controls;
5. at 200%, scroll 8–10 pages down/back in ~20-page PDF;
6. verify no accumulated slowdown/OOM and reading position survives rotation;
7. verify v0.5.24 quality remains.


## v0.5.25 merged / CI PASS — 2026-09-25

Merged source:
`6e962b5bac1bdee93ba232f55ee594f318ab2f8b`

Main CI:
- Tests `36167218686` — PASS;
- Android Debug `36167218706` — PASS;
- artifact `Renault-Docs-v0.5.25-Debug`;
- artifact id `10878087586`;
- APK SHA-256 `205f031bb889826f0b3b7ae3d76318627c203ed0cb3829b60ac611b0380c625b`;
- artifact ZIP SHA-256 `34b5508b7b7006f4c3e35f0e2839de8729a660bba16109caad70bef91194976e`.

Phone test now:
- 5 → 8 → install v0.5.25;
- point 9 NOT required;
- portrait: two fixed PDF-control rows;
- landscape: one fixed PDF-control row;
- 200%: scroll 8–10 pages down/back;
- watch whether motion stays smoother and current page sharpens after ~180 ms idle;
- rotate mid-document and verify page-relative position is retained.


## PDF viewer closeout — accepted

User decision: move on from PDF viewer work.

v0.5.25 is accepted as the current baseline. The remaining 200% long-document scroll limitation is documented and does not block further work.

Next dependency order:
1. BUG-004 — full Classic catalog converter parity;
2. converter execution/UX on top of the corrected catalog/runtime contract.

Reason for taking BUG-004 first: a new in-app conversion flow must not generate incomplete Modern catalogs for datasets such as Megane II.


## v0.5.26 BUG-004 implementation — 2026-09-25

PDF viewer work is accepted and parked.

Next dependency implemented:
BUG-004 — full Classic catalog parity.

Branch:
`fix/v0.5.26-classic-catalog-parity`

Implemented:
- opaque Renault section IDs beyond 3 digits;
- source-order catalog preservation;
- real-target validation;
- dedup only by code + entrypoint;
- duplicate display codes remain separate;
- ordered Runtime IR `section_entries`;
- Android duplicate-code resolution by legacy entrypoint;
- ModernSectionsReader no longer numeric-sorts sections;
- modern-sections schema 2;
- v0.5.26 / versionCode 42.

After merge/CI:
1. 5;
2. 8;
3. install v0.5.26;
4. 9 — REQUIRED;
5. test representative Classic-only IDs: 1405 / R325 / MAH / NT / NU where present;
6. compare source order with Classic;
7. regression-open 101 and 108.

After BUG-004 phone PASS, continue the in-app converter execution/UX flow using the corrected catalog contract.


## v0.5.26 merged / CI PASS — 2026-09-25

Merged source:
`03b0eb4a27fa0c0f076fb1cca9524a6109130272`

Main CI:
- Tests `36171369633` — PASS;
- Android Debug `36171369588` — PASS;
- artifact `Renault-Docs-v0.5.26-Debug`;
- artifact id `10880925610`;
- APK SHA-256 `3f42d50fc3cf335545380d6c7a47b114cbe92abdbba7d8d158028795f7a57413`;
- ZIP SHA-256 `2e4c62f2d10ba671ba896d31dd9a766d6788ea03b238894371ec937065c570df`.

Phone gate now:
1. 5;
2. 8;
3. install v0.5.26;
4. 9 — REQUIRED;
5. verify representative non-3-digit Classic IDs in Modern: 1405, R325, MAH, NT, NU where present;
6. compare Modern order against Classic;
7. regression-open 101 and 108;
8. duplicate display-code real-phone gate is optional/NOT TESTED if no convenient example is visible.

After representative parity PASS: close BUG-004 and proceed to in-app converter execution/UX.


## v0.5.26 phone closeout / v0.5.27 next wave — 2026-09-25

v0.5.26 representative Classic↔Modern parity was reported normal on the real phone after the required point-9 regeneration.

BUG-004 status:
- representative non-3-digit families/source order/regression behavior accepted;
- duplicate display-code real-phone example was not explicitly exercised;
- automated duplicate-routing contract remains the evidence for that special case;
- BUG-004 is closed.

User then requested a specific PDF interaction improvement without replacing the current renderer.

v0.5.27 implementation:
- live two-finger pinch inside the PDF document viewport;
- zoom field updates continuously during pinch;
- toolbar does not zoom or pan with the document;
- current decoded bitmap scales immediately;
- after ~180 ms idle, current page rerenders from the original PDF at the quality required by the final zoom;
- current page first, neighbors later;
- lazy/scroll/prefetch render churn is suppressed while fingers are actively pinching;
- touched page region is used as the focal anchor;
- v0.5.25 rotation/scroll/cache behavior is preserved.

Version: `0.5.27` / code `43`.
Branch: `feat/v0.5.27-pdf-pinch-zoom`.
This is APK-only; Renault Menu point 9 is NOT required.

Phone QA source:
`docs/v.0.5.27/qa/PHONE_TEST.md`.


## v0.5.27 merged / CI PASS — 2026-09-25

PR #103 merged to main as:
`7660416ad3f00a7fdf3d7556b6fd11a0bfd1186f`.

Green tested source:
`46ba0c0df464d144158cd5a696611f2366198d75`.

Runtime implementation/version blobs were verified identical between tested feature source and merged main.

CI:
- Tests `36176557305` — PASS;
- Android Debug `36176557288` — PASS;
- artifact `Renault-Docs-v0.5.27-Debug`;
- artifact id `10882358464`;
- APK SHA-256 `03dca9deb5fda72e4fa5d72eae18f94285c451e04bd3b78157bf97fd19109ac8`;
- artifact ZIP SHA-256 `b0f79a703f60fd1255fb90a9724bce8e39196bbcf694579c83a0e2416e5406ae`.

Next action is real-phone v0.5.27 QA from `docs/v.0.5.27/qa/PHONE_TEST.md`.

Install flow:
1. Renault Menu → 5;
2. Renault Menu → 8;
3. install v0.5.27;
4. point 9 is NOT required.


## v0.5.28 development — 2026-09-25

After v0.5.27 phone confirmation, user reported pinch zoom works better and requested:
- raise upper zoom limit from 200% to 400%;
- improve landscape document focus/fullscreen;
- investigate whether the common per-volume `Документація` can be shared instead of repeated per section.

Implemented on branch `feat/v0.5.28-pdf-400-fullscreen`:
- zoom range 50–400%;
- quick presets 300% / 400%;
- render cap 3000 px;
- >=250% decoded/prefetch radius 0;
- PDF toolbar fullscreen toggle;
- ViewerActivity fullscreen hides app toolbar + system bars;
- Back exits fullscreen first;
- fullscreen survives rotation/recreation.

v0.5.28 is APK-only. Point 9 is NOT required.

Documentation investigation:
- Fast Pack already stores physical resources once;
- repeated section-local Runtime IR routes are the actual duplication;
- planned future optimization is one verified volume-level documentation catalog/shard referenced by sections;
- do not hoist unless converter proves the resolved documentation targets are identical for the volume;
- this future change will require point 9.


## v0.5.28 merged / CI PASS — 2026-09-25

PR #104 merged to main as:
`1104db917f5964db7efb41a9282eb88ccde733ee`.

Green tested source:
`330c4d7168712236453d76fe8fef92e17710b35c`.

Runtime implementation/version blobs were verified identical between tested feature source and merged main.

CI:
- Tests `36179103365` — PASS;
- Android Debug `36179103296` — PASS;
- artifact `Renault-Docs-v0.5.28-Debug`;
- artifact id `10884005657`;
- APK SHA-256 `615fe8bb939b41a92ac999472bad0015c9a5e2b7dfaf204cb344bba778479fd2`;
- artifact ZIP SHA-256 `9016bd4ef4b3dc03d28a6b50e9037f4eb89731e875db186b69c1ee8c052b780d`.

Next action: real-phone v0.5.28 QA.
Install flow: 5 → 8 → install. Point 9 is NOT required.

After v0.5.28 phone closeout, the next candidate converter optimization is ARCH-007: verified volume-level documentation catalog/shard for common `GENE / PLATFUSI / AIDE` routes. That future change WILL require point 9.


## v0.5.29 development — 2026-09-25

User confirmed v0.5.28 works better and clarified two follow-ups.

1. Fullscreen button must visibly indicate active/inactive mode.
2. Documentation ownership is exactly one set per Renault volume/configuration. Different volumes have different files and must never be combined. Sections within one volume share that volume's set.

Branch:
`feat/v0.5.29-volume-documentation`.

Implemented:
- fullscreen button accent/pressed state + `aria-pressed`;
- ViewerActivity synchronizes button state after toggle/focus/recreation;
- converter verifies GENE / PLATFUSI / AIDE route equality inside each volume;
- one self-contained documentation shard per qualifying volume;
- no cross-volume sharing;
- vdoc-* namespace for hoisted graph IDs;
- runtime index documentation_path;
- Android lazy-loads volume documentation only on first Documentation tap;
- section-local documentation remains compatibility fallback for the migration wave.

Version: v0.5.29 / code 45.
Point 9 is REQUIRED.

Python CI on fixed head `242d6b7358232ccd4f0627c5fb8bd5dc054f5b5c`: PASS.
Android CI pending.


## v0.5.29 merged / CI PASS — 2026-09-25

PR #105 merged to main as:
`05c6833070ebbc5c2db8a3015bcdf1b030cfbcc6`.

Green tested source:
`db1078e4a45b5e2e2d5fecaf9d64b105acbded15`.

CI:
- Tests `36181651229` — PASS;
- Android Debug `36181651350` — PASS;
- artifact `Renault-Docs-v0.5.29-Debug`;
- artifact id `10884272976`;
- APK SHA-256 `761404a0380ed07c47d02508a4298a393b87e9040b3390648b7c90a4c832b1e3`;
- artifact ZIP SHA-256 `bb8a0ca782aa01de9c5231e405272de7e5aff415cf5327386af92ab2873b0047`.

Phone install flow:
1. Renault Menu → 5;
2. Renault Menu → 8;
3. install v0.5.29;
4. Renault Menu → 9 — REQUIRED.

Primary phone gates:
- fullscreen button visibly tracks active/inactive state;
- Documentation from sections inside one volume resolves to that volume's shared set;
- different volumes remain fully isolated and use their own documentation files;
- Schemes / Connector / Position / PDF remain regression-safe.


## v0.5.30 development — 2026-09-25

User found a real high-zoom PDF regression on phone:
- at 400% scrolling could show a blank/loading page;
- the page rendered only after reducing zoom;
- screenshot showed stale counter 13/13 while placeholder page 11 was visible.

Root cause:
- current-page observer required >=25% intersection;
- at 400% a page can be too tall to reach that threshold;
- high-zoom retain radius 0 then evicted the actually visible page.

Implemented on `fix/v0.5.30-pdf-high-zoom-scroll`:
- current page derived from nested viewport center during scroll;
- ±1 neighbor retained/preloaded;
- high-zoom neighbor render capped at 1200 px;
- current page <=1600 px while scrolling;
- current page upgrades to <=3000 px after idle;
- zoom range remains 50–400%.

Version: v0.5.30 / code 46.
APK-only relative to v0.5.29 package data. If point 9 was already run for v0.5.29, it is not required again.

Documentation next architecture:
- v0.5.29 per-volume documentation ownership stays;
- next candidate is dedicated VolumeDocumentationActivity;
- ModernVolumeActivity gets direct Documentation entry;
- section Documentation button becomes a shortcut;
- after parity, strip duplicated documentation graph from section shards;
- never share across different volumes/configurations.


## v0.5.30 merged / CI PASS — 2026-09-25

PR #106 merged to main as:
`2a342aa505378b651baaeef9fe44b555e6836896`.

Green tested source:
`89160047ff9a4208b547e00bd0982b22b91a45df`.

CI:
- Tests `36186156147` — PASS;
- Android Debug `36186156169` — PASS;
- artifact `Renault-Docs-v0.5.30-Debug`;
- artifact id `10885104783`;
- APK SHA-256 `06a3a032361ba18f6f925949b1eb959d9fa69320aba51d7c974cd177426f62ab`;
- artifact ZIP SHA-256 `9db989fab0602ffd52ae8a7adc291cc1bf86f8ce814dafc555f0330f15dd0267`.

Install flow:
1. Renault Menu → 5;
2. Renault Menu → 8;
3. install v0.5.30;
4. if point 9 was already completed for v0.5.29, do NOT run it again.

Primary phone gate:
- at 300–400% scroll across several page boundaries;
- counter must follow the actual page under viewport center;
- no blank/loading page may require lowering zoom;
- neighbors should appear at lightweight quality, then current page sharpens after scroll idle.

Next architecture candidate after this phone gate: ARCH-008 dedicated volume documentation screen.


## Design note — PDF Companion documentation — 2026-09-26

User requested that documentation remain rapidly accessible while diagnosing wiring diagrams because schematic block/fuse/component numbers need frequent manual lookup.

No code was changed for this request.

Recorded design:
`docs/design/PDF_COMPANION_DOCUMENTATION.md`.

Key contract:
- documentation remains one independent set per volume/configuration;
- future removal of duplicated section-local `GENE / PLATFUSI / AIDE` graph data must NOT remove access;
- future dedicated `VolumeDocumentationActivity` remains the canonical documentation owner;
- section Documentation is a shortcut to that owner;
- PDF viewer should later gain a Companion/split button near fullscreen;
- Companion opens same-volume documentation in a second frame while keeping the schematic PDF open;
- main and companion panes preserve independent state;
- fullscreen/rotation preserve the two-pane workspace;
- no cross-volume sharing;
- manual lookup works without automatic block-number recognition.

Current implementation priority remains v0.5.30 phone validation. Companion mode is design-only.


## v0.5.30 phone PASS / next stage — 2026-09-26

User confirmed 400% PDF scrolling/loading works correctly.

BUG-005 is closed.

Documentation terminology:
- canonical scope is `volume` / «том»;
- one volume owns one documentation set;
- sections inside that volume share the same General documentation / Fuses / Help data;
- model/modification/year/configuration may describe the volume, but are not the stable architectural owner key.

Next implementation stage:
ARCH-008 — dedicated volume documentation screen.


## v0.5.31 development — 2026-09-26

Next stage after v0.5.30 phone PASS: ARCH-008 dedicated volume documentation screen.

Branch:
`feat/v0.5.31-volume-documentation-screen`.

Implemented:
- `VolumeDocumentationActivity` registered in manifest;
- direct Documentation button on ModernVolumeActivity;
- section Documentation button becomes shortcut to the same volume screen when volume documentation exists;
- RuntimeIrReader can resolve/read documentation directly by volume entrypoint without any section identity;
- nested documentation panel stack persists across rotation;
- final documents reuse ViewerActivity with volume context;
- legacy section-local documentation remains fallback.

Version: v0.5.31 / code 47.
No converter/package format change relative to v0.5.29. If point 9 has already been run for v0.5.29+, it is not required again.

Next: CI, merge, then phone gates from `docs/v.0.5.31/qa/PHONE_TEST.md`.


## v0.5.31 merged / CI PASS — 2026-09-26

PR #107 merged to main as:
`6fae1c82b37e12a10938e428383e08aff3d8135c`.

Green tested source:
`b73d0a4893318233e2b222711a4bbe82740dd5fb`.

CI:
- Tests `36193148657` — PASS;
- Android Debug `36193148624` — PASS;
- artifact `Renault-Docs-v0.5.31-Debug`;
- artifact id `10888463798`;
- APK SHA-256 `c1318bf7fff75e793f0f8270aa1a4f7587deb1b7b2c12f57410160b839779cd5`;
- artifact ZIP SHA-256 `add2d7d8319eecd2a2e664f587adbc55bcb9d1a7ed4f3b22d9f158b4daaff589`.

Install flow:
1. Renault Menu → 5;
2. Renault Menu → 8;
3. install v0.5.31;
4. if point 9 was already completed for v0.5.29+, do NOT run it again.

Phone priority:
- open Documentation directly from volume screen;
- open Documentation from section 103/105 and confirm it lands on the same dedicated screen;
- verify General documentation / Fuses / Help and nested items;
- verify another volume stays isolated;
- rotate inside a nested documentation panel and verify Back hierarchy.


## v0.5.31 phone progress — 2026-09-26

After point 9 regeneration, direct volume Documentation for `NT8236A · 2002-11-18` works and shows:
- Загальна документація;
- Запобіжники;
- Довідка.

ARCH-008 status: PARTIAL PHONE PASS.

Remaining:
1. open child documents from all three categories;
2. verify section 103/105 shortcut reaches the same dedicated screen;
3. verify rotation/back hierarchy;
4. verify another volume is isolated.


## v0.5.31 phone progress — 2026-09-26

After Renault Menu → 9, volume `NT8236A · 2002-11-18` now opens the dedicated volume documentation screen successfully.

Visible root categories:
- Загальна документація;
- Запобіжники;
- Довідка.

The prior missing-documentation screen was stale-package state.

v0.5.31 status: PHONE PARTIAL PASS.

Remaining gates:
- child documentation navigation;
- section shortcut parity;
- another-volume isolation;
- rotation/back hierarchy.


## v0.5.31 phone PASS / v0.5.32 merged — 2026-09-26

v0.5.31 remaining phone gates were reported PASS.
ARCH-008 is CLOSED.

v0.5.32 / code 48 implements PDF Companion.

Merged main source:
`41dcede6ebea2f2220ba23e363e15be6c5ea79c8`.

Green tested source:
`f22fa09949225d9f483e860165446163c32966fc`.

CI:
- Tests `36201064184` — PASS;
- Android Debug `36201064135` — PASS;
- artifact `Renault-Docs-v0.5.32-Debug`;
- artifact id `10891713605`;
- APK SHA-256 `574c7184dda4267f84d688b8e748fa0aea18b65c2e0ee5078646db3acb4880f6`;
- ZIP SHA-256 `07ea758777485670cea36be5ee09a05cba73490650427a2a360e5a2f80a53112`.

Install:
1. Renault Menu → 5;
2. Renault Menu → 8;
3. install v0.5.32;
4. point 9 is NOT required if already completed for v0.5.29+.

Primary phone gate:
- open a schematic PDF;
- use new Companion control beside fullscreen;
- verify main PDF state is preserved;
- navigate same-volume Documentation in lower pane;
- open HTML/PDF in lower pane;
- verify main/companion PDF states are independent;
- test fullscreen, rotation, Back/Close and memory stability.


## v0.5.32 phone core PASS → v0.5.33 development — 2026-09-26

Real-phone screenshots verified PDF Companion works in portrait and landscape.

v0.5.32 accepted core behavior:
- two PDFs visible;
- independent page/zoom state;
- fullscreen split works.

UX follow-up requested:
- in fullscreen with two documents, hide all permanent panels/buttons;
- double tap shows controls temporarily over documents, then auto-hides;
- restore current fullscreen/split state after rotation;
- draggable divider changes document proportions.

Implemented on:
`feat/v0.5.33-split-focus-divider`.

v0.5.33 / code 49:
- split focus mode for fullscreen + Companion;
- overlay PDF toolbars;
- companion header hidden in focus mode;
- double-tap temporary controls;
- ~3.2s auto-hide;
- floating companion Back/Close/fullscreen controls;
- draggable divider 20–80%, default 55/45;
- split ratio and transient control state persisted.

Point 9 is NOT required.

Next: CI, merge, then real-phone test using the same two-document pair from v0.5.32.


## v0.5.33 merged / CI PASS — 2026-09-26

PR #109 merged to main as:
`250b2a26a799bc88b117b82d26005f98550681fd`.

Green tested source:
`031d5b75a608fcd5a6dc7c427311f5b6476b8330`.

CI:
- Tests `36203053082` — PASS;
- Android Debug `36203052992` — PASS;
- artifact `Renault-Docs-v0.5.33-Debug`;
- artifact id `10893250571`;
- APK SHA-256 `44ff64b86907148ea589f3415679944859e283cf92a78baa029bf7ec3baaf5d2`;
- ZIP SHA-256 `58f3d4b3dac95c0c6c1d9e5e420a3977dff5d756ff1f45dd6bcd0485d330d74e`.

Install:
1. Renault Menu → 5;
2. Renault Menu → 8;
3. install v0.5.33;
4. point 9 is NOT required.

Phone priority:
- drag divider in normal and fullscreen split;
- enter fullscreen with two documents: permanent PDF toolbars + companion header should hide;
- double tap either document: overlay controls appear and auto-hide after ~3.2s;
- rotation preserves fullscreen, Companion, divider ratio and both PDF states;
- stress 300–400% main + 200–300% companion.


## v0.5.33 phone feedback / v0.5.34 fix — 2026-09-26

v0.5.33 split-focus UX is reported convenient and screenshots confirm:
- hidden chrome in fullscreen split;
- temporary overlay controls;
- draggable divider;
- portrait/landscape dual-document use.

New BUG-006:
after fullscreen split → close second document → rotate portrait, PDF fullscreen button and Android app chrome can disagree. The button may look inactive while main app toolbar is still hidden.

Fix branch:
`fix/v0.5.34-fullscreen-state-reconcile`.

v0.5.34 / code 50 implementation:
- `pdfFullscreen` is canonical;
- do not apply fullscreen before restoring main WebView state;
- reconcile immediately after restore/load setup and again via `webView.post`;
- WebChrome callbacks reconcile after PDF JS becomes available;
- window focus reconciles fullscreen true AND false;
- closing Companion reconciles presentation without changing fullscreen state.

Point 9 is NOT required.
Next: CI, merge, then reproduce exact phone sequence.


## v0.5.34 merged / CI PASS — 2026-09-26

PR #110 merged to main as:
`adb226f605c3730037e945fe0e6ce95275981710`.

Green tested source:
`bf23eb88d39565559d61caa40ac9373fac8feaab`.

CI:
- Tests `36204766057` — PASS;
- Android Debug `36204766049` — PASS;
- artifact `Renault-Docs-v0.5.34-Debug`;
- artifact id `10893043078`;
- APK SHA-256 `f45ebd66dd5d4c292f4808f63f0aac446d833798205db16d97f36fe5fe23896f`;
- ZIP SHA-256 `0efb0024c516538e0a2223603fc683c13c7b720463f0c2042d4dadad7767ac6c`.

Install:
1. Renault Menu → 5;
2. Renault Menu → 8;
3. install v0.5.34;
4. point 9 is NOT required.

Primary phone reproduction:
fullscreen split → close second document → rotate portrait.

Expected invariant:
- fullscreen true => PDF button active + app toolbar hidden;
- fullscreen false => PDF button inactive + app toolbar visible;
- one fullscreen tap exits; no corrective second tap.


## v0.5.35 merged / CI PASS — 2026-09-26

User clarified that fullscreen loss on rotation also reproduces with ONE PDF, not only two-document Companion mode.

v0.5.34 therefore did not fully solve BUG-006.

Structural fix in v0.5.35:
- ViewerActivity manifest now uses `android:configChanges="orientation|screenSize"`;
- orientation change no longer recreates ViewerActivity;
- main/companion WebViews remain alive;
- `pdfFullscreen` stays in the same Activity instance;
- `onConfigurationChanged()` reapplies split ratio and fullscreen;
- fullscreen is reconciled immediately, after layout, and again after 180 ms.

PR #111 merged:
`1eb744ce3959d1a56cb2d9059b5069dd1b207b9c`.

CI:
- Tests `36206184286` — PASS;
- Android Debug `36206184390` — PASS;
- artifact `Renault-Docs-v0.5.35-Debug`;
- artifact id `10894415207`;
- APK SHA-256 `d565d8e0f0c9f4d1787f2ee1a3397e15502fd5766081b5ace0681ebeaa80b0ed`;
- ZIP SHA-256 `1c79214b4f0a6f8257e24978098ce3a8b07e4074ad286625902f9fb02e1a8f2b`.

Install:
1. Renault Menu → 5;
2. Renault Menu → 8;
3. install v0.5.35;
4. point 9 is NOT required.

Phone gate:
- single PDF fullscreen → rotate portrait/landscape repeatedly;
- two-document fullscreen → rotate repeatedly;
- fullscreen button must remain active, app toolbar must remain hidden;
- one tap after rotation must exit fullscreen normally.


## v0.5.36 merged / CI PASS — 2026-09-26

Phone video `225102.mp4` showed v0.5.35 still had one remaining BUG-006 symptom:
- fullscreen Activity state survives rotation;
- main app toolbar stays hidden;
- PDF fullscreen button becomes visually inactive.

v0.5.36 fixes the WebView synchronization layer:
- PDF setter acknowledges applied `aria-pressed`;
- native expected fullscreen state is remembered in PDF JS;
- resize/pageshow/visibilitychange reapply it;
- ViewerActivity retries sync until acknowledged;
- generation guards prevent stale retries from overwriting newer user toggles.

PR #112 merged:
`3b6580d5a2b8d2a012a978cd828c220035dbef79`.

CI:
- Tests `36206923878` — PASS;
- Android Debug `36206923894` — PASS;
- artifact `Renault-Docs-v0.5.36-Debug`;
- artifact id `10894740860`;
- APK SHA-256 `cf89e1273d550fb1c052c9d3c5a419fed5dde17609deada8ff5a81e413d46ec9`;
- ZIP SHA-256 `68dd8cb2df6983176e9865e5ed8fe5189ce99f5efacb15b61ab4069eb293bd46`.

Install:
1. Renault Menu → 5;
2. Renault Menu → 8;
3. install v0.5.36;
4. point 9 is NOT required.

Primary phone gate:
- reproduce the exact v0.5.35 video sequence;
- fullscreen button must stay active after portrait↔landscape rotation;
- app toolbar must remain hidden while fullscreen;
- one tap after rotation exits fullscreen normally;
- repeat with Companion/two-document fullscreen.


## v0.5.36 phone PASS / next stage — 2026-09-26

User confirmed v0.5.36 works correctly.
BUG-006 is CLOSED.

PDF/Companion work is accepted for now.

Next major product stage:
finish Converter UX / in-app converter execution and then run the remaining broad converter/runtime parity audit.


## v0.5.37 Converter Writer Wave 1 — 2026-09-26

Branch:
`feat/v0.5.37-converter-writer`

Version:
- v0.5.37
- versionCode 53

Implemented:
- real foreground SAF conversion;
- staging directory;
- recursive scan/copy;
- path normalization via ConverterPathNormalizer;
- persistent progress/cancel;
- base dataset package;
- validation before final rename;
- Library registration;
- source is never deleted.

Output:
`<destination>/<source>_android`.

Current limitation:
this wave is Classic-ready + volume modern-index only. Full Modern Runtime IR/Fast Pack is deliberately NOT claimed yet.

Next after CI/phone gate:
v0.5.38-class converter compiler wave — port the current Python package pipeline (modern sections, Runtime IR v2 shards/index, volume documentation, coverage, Fast Pack) into Android and compare generated output against the existing package oracle.

After package parity:
verified backup + SHA-256 + explicit source deletion confirmation + Backup Center.


## v0.5.37 merged / CI PASS — 2026-09-26

Converter Writer Wave 1 is merged.

PR #113:
`51c2d77771a7c5676e5793ee38de2f180eaf7bb5`.

Green tested source:
`de28d445749c752d5d0d1ecbf31959cab92b8d3f`.

CI:
- Tests `36209095449` — PASS;
- Android Debug `36209095432` — PASS;
- artifact `Renault-Docs-v0.5.37-Debug`;
- artifact id `10894143612`;
- APK SHA-256 `ac7b0e6dfe0227c86b536f1811249690e8e6fe625f5364354c9ce81ccc590e6e`;
- ZIP SHA-256 `bed2177788448146fd3ac96f0f955a69922256bc3fd11c948463221d661f9739`.

Do not use Renault Menu point 8 for this phone gate until its latest-artifact selection is fixed/verified.

Phone priority:
1. install the exact v0.5.37 artifact;
2. use a small copied Renault source first;
3. validate plan;
4. start conversion and rotate/leave/return;
5. cancel and verify staging disappears + source is unchanged;
6. run again to completion;
7. register output in Library;
8. confirm duplicate run refuses existing output.

After PHONE PASS, start the full Modern compiler parity wave: modern-sections + Runtime IR v2 + volume documentation + coverage + Fast Pack.

## Next-chat checkpoint — 2026-09-26

Canonical recovery file:
`docs/assistant-kit/NEXT_CHAT_HANDOFF_2026-09-26.md`

Read that file first in the next chat.

Current state in one line:
v0.5.37 Converter Writer Wave 1 is MERGED + CI PASS, PHONE TEST PENDING; next user action is the exact v0.5.37 staged conversion/cancel/output phone gate, then development moves to full Android Modern compiler parity.

Do not use Renault Menu point 8 for this gate until its artifact-selection bug is fixed/verified.


## v0.5.38 merged / CI PASS — 2026-09-26

Real-phone v0.5.37 Megane II writer completed 7657/7657 files and produced `Megane II_android`, source unchanged.

Foundation findings from that run:
- slow DocumentFile SAF scan;
- misleading SCANNING `0 / N` UI;
- Renault image folders appeared as Gallery albums;
- raw content:// folder URIs were visible;
- Termux point 8 could fetch an older APK after merge.

v0.5.38 fixes all five:
- direct DocumentsContract/ContentResolver scanner with DocumentFile fallback;
- SCANNING uses indeterminate progress + found-file count;
- root .nomedia created before media copy and ensured during existing-output registration;
- shared friendly SAF paths in Converter + Backup Settings;
- point 8 now selects exact `Renault-Docs-v<current version>-Debug` artifact regardless of branch and refuses old-version fallback.

Merged main:
`d93e56de43a5ec00120e62f99e77bf9875b0b4dc`.

CI:
- Tests `36240225061` — PASS;
- Android Debug `36240225052` — PASS;
- artifact `Renault-Docs-v0.5.38-Debug`;
- artifact id `10905865135`;
- APK SHA-256 `caf6ecb0426306b8953c97048e578f4459b20d5a2510491a2e1e9ffe7d45a90d`.

Normal APK phone flow is restored:
1. Renault Menu → 5;
2. Renault Menu → 8;
3. point 8 must download exact v0.5.38 or fail; it must never silently fetch an older version.

Phone gate:
- verify friendly source/destination paths;
- compare scan speed on same Megane II source;
- new converted output contains .nomedia and does not create new Gallery albums;
- verify point 8 reports/installs v0.5.38.


## v0.5.42–v0.5.43 project/volume library — 2026-09-26

Product direction:
- keep the Android SAF converter as a fallback;
- keep the fast direct-filesystem Termux/Linux converter for developer preparation;
- primary user model becomes project → independently imported volumes;
- default empty projects: Megane II, Laguna II, Kangoo II;
- future distribution target is one prepared volume package per document/volume, so users install only what they need.

v0.5.42:
- project store and volume records;
- default projects;
- create custom project;
- choose target project before prepared-volume import;
- manual project override flow;
- prepared manifests can carry project_id;
- fast Termux converter can stamp project_id;
- legacy dataset cards retained for compatibility.
Merged:
`fbaefc7e622b39cb565e33f481a02a644f5b63f7`.
Main Android build PASS:
run `36262686813`;
artifact `Renault-Docs-v0.5.42-Debug`;
artifact id `10912862959`;
APK SHA-256 `9f10a14f6c35de7c6da17b59497f2140843757ac325947aa24f77f29581312af`.

Phone v0.5.42 result:
- Megane II project showed 2 volumes after adding `Megane II_NT8342A_android`;
- NT8342A metadata imported correctly;
- older auto-migrated NT8340A appeared incorrectly as a generic `Megane II` card;
- home screen was visually too button-heavy.

v0.5.43 fixes:
- legacy one-volume migration re-reads the actual prepared manifest through PreparedVolumeReader;
- same-source volume records de-duplicate by treeUri as well as volume id, so the generic migrated Megane II card is replaced by real NT8340A metadata;
- home screen is project-first with compact Add Volume / New Project actions;
- converter and legacy folder import moved under compact Tools;
- project cards use cleaner active/empty styling and intentional order Megane II → Laguna II → Kangoo II;
- project screen uses compact prepared/manual actions;
- volume cards no longer repeat `NTxxxx · date` twice: document code is title, date is metadata.

PR #126 merged:
`409444fc5547c3eb93ebc185c69d2432f103693a`.
Green PR source:
`22078f4612bec1ecebaeeb0575cf8c255cb75c45`.
CI:
- Tests run `36263983260` — PASS;
- Android Debug run `36263983283` — PASS;
- PR artifact `Renault-Docs-v0.5.43-Debug`;
- artifact id `10912379627`;
- APK SHA-256 `99fddfd953b45823b518f73d3eb5ab725d9e0a3afb9b26bb39b922dea1d281b0`.

Next phone gate:
1. install v0.5.43;
2. home must show compact controls and projects in Megane II, Laguna II, Kangoo II order;
3. open Megane II;
4. expected two cards: NT8340A / 2006-04-18 and NT8342A / 2006-10-09 — no generic `Megane II` volume card and no duplicated code/date line;
5. open both volumes and verify Modern/Classic behavior is unchanged.

Next development after that phone gate:
define and implement the single-file prepared volume package/import format (`.rdpkg`) while keeping folder import + converter fallback.


## v0.5.44 project volume repair — 2026-09-26

Phone v0.5.43 findings:
- home/library polish is visually improved and accepted enough to continue;
- Megane II project still showed one generic `Megane II` volume card instead of NT8340A metadata;
- opening that generic card reopened the whole old dataset catalog instead of the selected project volume;
- old Android-converted Megane dataset incorrectly exposed `Backup` as a second Modern volume;
- NT8340A then fell back to `Сумісний режим` because that older Android conversion was Classic-ready only and does not contain a native Modern sections index.

v0.5.44 fixes:
- existing saved project-volume records with missing code/date are re-read from their saved folder via PreparedVolumeReader and repaired automatically;
- project volume taps in Modern mode now open the selected volume directly through ModernVolumeActivity instead of reopening ModernDatasetActivity;
- ModernCatalogReader filters synthetic support folders `Backup`, `_renault`, and `packages` when they have no real Renault document metadata;
- AndroidDatasetPackageWriter excludes the same support folders from future volume discovery;
- project metadata repair also runs when the project screen is opened directly.

PR #127 merged:
`b21e5c3c7c9f3d52f0cc05d225aa6fb75e7e41ce`.

Green PR source:
`5535ed6075325967e4b55d164c1de8c665bae54c`.

CI:
- Tests run `36265503749` — PASS;
- Android Debug run `36265503748` — PASS;
- artifact `Renault-Docs-v0.5.44-Debug`;
- artifact id `10913682403`;
- APK artifact SHA-256 `8288d06b777b1e668f712e107683ab4ccdbefd4d1fc61ff306f5a5340ded55ca`.

Next phone gate:
1. install v0.5.44 using Renault Menu → 5 → 8;
2. open Megane II project;
3. expected cards: NT8340A / 2006-04-18 and NT8342A / 2006-10-09; no generic `Megane II` card;
4. tap NT8340A: it must go directly to that selected volume, not the two-volume Modern dataset list;
5. `Backup` must not appear as a Renault volume;
6. NT8340A may still show `Сумісний режим` because that particular old folder has no compiled native Modern sections; this is a data/package capability issue, not the project-library bug.

After this phone gate:
continue with single-volume prepared package/import format (`.rdpkg`) and fast per-volume preparation workflow.

## Termux private-repo migration — 2026-09-27

Decision:
- live Git/code checkout moves out of Android shared storage and into Termux private storage:
  `$HOME/renault-docs-android`;
- Renault source documents, converted `*_android` datasets, converter logs and APK/packages stay under:
  `/storage/emulated/0/Documents/Renault/`;
- old shared-storage checkout:
  `/storage/emulated/0/Documents/Renault/application`
  must not be deleted until private-clone + aliases + Widget/Menu + dataset/browser/APK smoke are all verified.

Audit findings:
- old repo path was hardcoded in `menu.sh`, `config/current-device.json`, alias/widget installers, `reno-docs.sh`, APK build/download scripts, fast converter and phone workflow docs;
- `browser.sh` was already self-relative;
- server status/stop and APK-folder opener do not require a repo-root migration;
- phone GitHub auth/SSH are healthy; old checkout was exactly synchronized with then-current `origin/main`;
- old checkout had only untracked Python `__pycache__` directories, now ignored by repo rules.

Implemented in PR #131:
- operational scripts resolve the repository from their own location instead of the legacy shared path;
- `reno-code`/other Renault aliases are reinstalled from the private checkout and old alias definitions are replaced;
- Renault Termux:Widget shortcut points at the private checkout;
- `repository_root` now records `/data/data/com.termux/files/home/renault-docs-android`;
- source/build/package paths remain in shared storage;
- browser transfer-kit code installs into Termux private storage;
- Python cache files are ignored.

PR #131 source:
`4f209d575788a2f38963dce821a7c655d1db0273`.

CI:
- Tests run `36276702464` — PASS.

Merged main:
`19cb2ba195bf9344054f79ef3abf0349e5f6f8e2`.

Phone bootstrap after pulling/using this main:
1. keep the old shared checkout untouched;
2. clone `main` into `$HOME/renault-docs-android`;
3. run `tools/install_termux_aliases.sh`, source the shell rc, then run `tools/install_termux_widget.sh`;
4. verify `reno-code` resolves to `/data/data/com.termux/files/home/renault-docs-android`;
5. verify Renault Menu → paths/update/browser/APK handoff still use shared datasets/packages;
6. only after the smoke passes may the obsolete global `safe.directory` rule for the old shared checkout be removed; do not delete the old checkout until explicitly confirmed.

## v0.5.49 RDPKG v1 phone acceptance — 2026-09-27

Real-phone E2E PASS for NT8340A:
- Termux/reference builder created `Megane-II_NT8340A_2006-04-18.rdpkg`;
- source package: 8,011 payload files, 73,250,913 bytes;
- Android `.rdpkg` picker/import completed;
- existing NT8340A was updated without creating a duplicate;
- reopened volume is exactly `NT8340A · 2006-04-18`;
- Modern reports `347 · native`;
- no compatibility-mode fallback.

Conclusion: `.rdpkg v1` format + Android managed import are accepted end-to-end.

Canonical architecture:
- `docs/architecture/RDPKG_DISTRIBUTION_AND_ANDROID_EXPORT.md`.

## v0.5.50 development — Android-native .rdpkg export

Branch:
`feat/v0.5.50-native-rdpkg-export`

First-wave scope:
- no Python or Termux in the APK/user workflow;
- visible per-volume actions menu;
- `Експортувати .rdpkg`;
- only managed volumes installed from `.rdpkg` are fast-exportable in this wave;
- validate installed `rdpkg.json` + single-volume `renault-dataset.json`;
- reuse existing native sections / Runtime IR / Fast Pack;
- `ACTION_CREATE_DOCUMENT` chooses destination;
- Kotlin/JVM `ZipOutputStream` writes package;
- SHA-256 is computed while writing;
- installed source package is read-only and never mutated by export.

Future stage:
port raw-folder conversion to Kotlin incrementally and use the Python converter as a parity/reference oracle rather than embedding Python into production Android.

## v0.5.50 Android-native .rdpkg round-trip phone evidence — 2026-09-27

Real-phone fast export/import evidence for NT8340A:

- source volume: `NT8340A · 2006-04-18`;
- Android-native export progressed through the full managed package;
- exported file contained `8012` files;
- export completed with SHA-256:
  `07fab53c386ad3514a97528b9fa7dcc72f1433b41b4b4d0f2b758272d82f3696`;
- the exported `.rdpkg` was selected again through the app's `Додати том` flow;
- import progressed normally;
- existing NT8340A was updated in-place;
- project still contains exactly two volumes, so no duplicate NT8340A record was created.

This confirms the user-facing round-trip path through Android-native export and Android managed import.

Final acceptance gate — PASS:
- reopened exactly `NT8340A · 2006-04-18`;
- Modern shows `347 · native`;
- no compatibility-mode fallback;
- Classic remains available only as the explicit alternate mode.

Conclusion:
- v0.5.50 Android-native `.rdpkg` fast export/import round-trip is **PHONE PASS / CLOSED**.

Next logical stage:
- start Kotlin-native preparation/conversion from a raw Renault folder;
- keep Python/Termux only as reference/developer tooling and parity oracle;
- do not embed Python runtime into the production APK.

## v0.5.51 development — Kotlin-native raw preparation foundation — 2026-09-27

Branch:
`feat/v0.5.51-native-preparation-foundation`.

Starting point:
- v0.5.50 Android-native managed `.rdpkg` export/import round-trip is PHONE PASS / CLOSED;
- accepted NT8340A reference result remains `NT8340A · 2006-04-18` → `347 · native`.

Pipeline audit result:
- reuse the existing fast SAF scanner and `ConverterPathNormalizer`;
- do not reproduce the old user-facing `raw → *_android → package → .rdpkg` chain;
- use one temporary app-private normalized staging copy;
- run all compiler passes against local File I/O;
- Python/Termux stay reference/developer tooling and the parity oracle.

Implemented foundation on this branch:
- new shared `RdpkgZipWriter`;
- deterministic outer archive ordering/timestamps;
- streaming SHA-256 while writing;
- speed-oriented per-entry compression: text/metadata level 1, binary/already-compressed payload level 0;
- existing v0.5.50 `RdpkgExporter` refactored to use the shared writer;
- new `NativeFastPackWriter` with Python-equivalent selection rules, `BEST_SPEED`, deterministic output, and SHA-256 during write;
- JVM tests for archive compression policy and Fast Pack selection;
- canonical architecture: `docs/architecture/KOTLIN_NATIVE_RAW_TO_RDPKG.md`.

Important scope boundary:
this is the performance/storage foundation, not yet a complete raw-folder → native Modern `.rdpkg` implementation. Do not call v0.5.51 PHONE PASS yet.

Next code step:
extract/reuse the current fast SAF scan as a shared source scanner, then write exactly one normalized copy into app-private staging. After that, port volume/Modern index parity and then the Python-only section/Runtime IR compiler stages.
### v0.5.51 private staging writer added

`NativePreparationStager` now implements the next planned I/O stage:
- fast `DocumentsContract` metadata scan with DocumentFile compatibility fallback;
- complete exact-path map before writes, preserving `ConverterPathNormalizer` behavior;
- exactly one SAF read/copy per source file into `noBackupFilesDir/native-preparation/<token>.staging`;
- HTM/HTML/JS normalization during that copy;
- binary streaming with a 1 MiB buffer;
- cancellation checks during scan/copy;
- automatic private staging cleanup on failure/cancel;
- no public `*_android` output in this path.

Next implementation step is now compiler parity on the local staging tree, starting with volume discovery + Modern index, then native section discovery.
### v0.5.51 volume/index compiler foundation added

`NativeVolumeCompiler` now ports the first local compiler stage from Python:
- top-level Renault volume discovery;
- INDEX/ACCUEIL entrypoint discovery;
- NT document-code extraction;
- date extraction;
- VISU/technical-documentation kind;
- stable volume slug/title/order;
- `_renault/volumes.json`;
- `_renault/modern-index.json`.

JVM tests use an NT8340A-shaped temporary volume and verify `NT8340A · 2006-04-18` identity/entrypoint plus Modern index structure.

Next compiler boundary: port `core/sections.py` semantics to Kotlin. Do not jump directly to Section IR until catalog identifier/order/target parity is covered.
### v0.5.51 native section discovery foundation added

`NativeSectionCompiler` now ports `core/sections.py` behavior onto local staging:
- tolerant legacy HTML parsing through jsoup;
- frames/iframes traversal without executing legacy JavaScript;
- href / javascript-href / onclick local HTML target discovery;
- table-row navigation fallback;
- opaque section identifiers including numeric, 4-digit, R-prefixed and alphabetic families;
- source order preservation;
- identity by code + resolved entrypoint;
- duplicate-title improvement without collapsing different targets;
- bounded fallback scan for legacy navigation pages;
- `modern-sections.json` schema v2 output.

This preserves the BUG-004 durability rule: section identifiers are opaque strings, not exactly three decimal digits.

Next compiler boundary is `core/section_ir.py`. That is the large semantic stage (actions, controls, selectors, connector/document relations, structured tables), so it should reuse the same tolerant HTML layer rather than introduce a second parser.
### v0.5.51 foundation CI checkpoint

Green code checkpoint:
`51c833fbdbc23f6f2dbdf649cba79c09d3eb7968`.

PR:
`#138` — draft, `v0.5.51: Kotlin-native raw preparation foundation`.

CI for the green code checkpoint:
- Tests run `36342715326` — PASS;
- Android Debug APK run `36342715337` — PASS.

What is now implemented but not yet wired as the final user flow:
- one-scan / one-copy SAF → private staging;
- path normalization during that copy;
- Kotlin volume discovery + Modern index;
- Kotlin native section discovery / `modern-sections.json`;
- Kotlin Fast Pack writer;
- shared speed-oriented outer `.rdpkg` writer with SHA-256 during write.

Important: version remains v0.5.50 / code 66 on this development branch for now. No phone install/gate is requested from this checkpoint. The next implementation boundary is `core/section_ir.py` parity, followed by Runtime IR shards/index/coverage and final package-manifest wiring.
## v0.5.51 current integration boundary — 2026-09-28

Live branch head at this checkpoint:
`cc07797bc9f25bc6543da8d5386928a8cd37997a`.

Important correction to the older handoff text:
the branch has advanced beyond the earlier “next = section_ir.py” checkpoint. Section IR and Runtime IR are now implemented in Kotlin, and the native data path is assembled end-to-end in `NativeRdpkgPreparationEngine`.

Implemented on branch:
- raw SAF scan + one normalized copy into app-private staging;
- volume discovery + Modern index;
- native section discovery;
- `NativeSectionIrCompiler` (`section-ir-v2`) with static legacy HTML/JS route extraction;
- `NativeRuntimeIrCompiler` with runtime tree, section shards/index and coverage;
- Fast Pack;
- `renault-dataset.json` + `rdpkg.json`;
- streamed outer `.rdpkg` with SHA-256 during write;
- staging cleanup after completion/failure;
- parser cache bounded for phone memory;
- volume/section discovery reused across the native pipeline.

Current branch CI at `cc07797...`:
- Tests run `36345630397` — PASS;
- Android Debug APK run `36345630361` — PASS.

What is NOT done yet:
1. the engine is not wired into the user-facing Project/Conversion UI;
2. long-running native preparation needs lifecycle-safe execution (rotation/process UI reattachment) using the existing service/run-store pattern, not a raw Activity-owned thread;
3. no real NT8340A Kotlin-native parity/phone gate has been run yet;
4. v0.5.51 version bump/release/merge must wait for that gate.

Therefore the next code task is UI + lifecycle integration around `NativeRdpkgPreparationEngine`, then a real-phone NT8340A run expecting one `.rdpkg`, reopen as `NT8340A · 2006-04-18`, and `347 · native` without compatibility fallback.


### v0.5.51 lifecycle/UI integration — GREEN CI

Integration is now wired around the native core:
- `NativeRdpkgRunStore` persists native run state separately from legacy converter state;
- `NativeRdpkgPreparationService` owns long-running work as a foreground `dataSync` service with wake lock;
- ProjectActivity provides raw folder → `.rdpkg` destination two-step SAF flow;
- pending source survives Activity recreation;
- ProjectActivity reattaches to persistent progress after rotation/recreate;
- Cancel reaches the engine during PREPARING; final validation/import is intentionally atomic;
- generated package is immediately validated through `RdpkgImporter.install()`;
- validated volume is automatically upserted into the same project;
- stale RUNNING state is recovered after service/process loss;
- incomplete destination is deleted on pre-validation failure/cancel.

Green integration checkpoint:
`83cfda448e20ad946704c6b05efb5adce1a38817`.

CI:
- Tests `36357699673` — PASS;
- Android Debug APK `36357699651` — PASS.

Next step:
bump candidate to v0.5.51 / versionCode 67, rebuild/sign, then run `docs/v.0.5.51/qa/PHONE_TEST.md` on raw NT8340A.


### v0.5.51 exact phone candidate — CI PASS / PHONE PENDING

Version:
- `0.5.51`;
- versionCode `67`.

Runtime APK source:
`2adab113bbfd48607446310a2f2daa4ce5f3d354`.

Branch validation source:
`912fd52ef32613d5d2d3628382baae4994c38272`.

Compare from runtime source to validation source contains only tests/docs, so Android runtime blobs match.

CI:
- Tests run `36358146811` — PASS;
- Android Debug APK run `36357861552` — PASS.

Artifact:
- `Renault-Docs-v0.5.51-Debug`;
- artifact id `10944810990`;
- APK SHA-256 `c0b2ec950f6313d1befbc53b9bd6d4878737af5baf787f615aed4355fb1e3d4c`;
- artifact ZIP SHA-256 `c0400b072ba4fdbd4efa2126b7446e0462c27ffbd408b2c6089423d3af6d2a9f`;
- signer certificate SHA-256 `dd588fbb3093a047f81c24397fc6d2ab7f5a32040425a21dd24506cce59a9802`.

Release metadata:
`docs/v.0.5.51/RELEASE_META.json`.

Phone gate:
`docs/v.0.5.51/qa/PHONE_TEST.md`.

Do not mark v0.5.51 PHONE PASS or merge PR #138 until raw NT8340A proves Android/Kotlin generation → validation/import → `347 · native`.


### Default phone-side Termux workflow

Для Renault Docs користувацький phone-side workflow за замовчуванням ведемо через існуюче Termux-меню (`reno-docs`), а не через ручні Git/gh/bash команди.

Manual commands are fallback only for:
- дії, яких ще немає в меню;
- аварійну діагностику;
- одноразове виправлення menu state.

Якщо новий development/test flow потребує ручного branch switching, preferred fix — додати підтримку цього flow у меню.


### Termux candidate menu workflow — 2026-09-28

Default phone workflow remains menu-first.

Operational support was merged to `main`:
- PR #140 / `65b5a62a9f901c8b8d387c74b2f2caa6221ac568` — branch-aware update + candidate/main menu actions;
- PR #141 / `32cdb8887576f0146381cbdb7f2f6fd19fe45eb0` — removes external `jq` dependency.

Menu:
- 5 = update current branch;
- 16 = resolve PR #138, switch/update candidate branch, download exact APK;
- 17 = return to main.

For devices still running the old menu code, item 5 must be run once, then the menu process restarted so items 16/17 become visible.

Do not give manual branch-switch commands unless menu recovery itself fails.


### Termux candidate fetch repair — 2026-09-28

Real-phone menu item 16 resolved PR #138 but failed before switching:
`fatal: invalid reference: origin/feat/v0.5.51-native-preparation-foundation`.

Cause:
the phone clone's remote fetch configuration may not populate arbitrary remote branches on a plain fetch.

Repair:
- PR #142 merged to `main` as `5965e29e5e4f8183e8495891d209d30f484a0289`;
- `reno-candidate.sh` explicitly fetches the PR head branch into `refs/remotes/origin/<branch>`;
- then switches/tracks that exact ref;
- Tests `36361285175` — PASS;
- repair synced into the v0.5.51 branch too.

Next phone action is menu-only:
**5 — Оновити проєкт з GitHub**, then **16 — Тестовий candidate: перейти + завантажити APK**.


### Termux candidate tracking repair — 2026-09-28

After the explicit fetch fix, the phone reached the next failure:
`fatal: cannot set up tracking information; starting point 'origin/feat/v0.5.51-native-preparation-foundation' is not a branch`.

The ref existed, but the phone clone's narrow fetch configuration meant Git would not accept it as an upstream-tracking source.

Repair:
- PR #143 merged to `main` as `e2377031dd190c7f7e2667aa29ab76fbf5057ed4`;
- candidate branch creation now uses the full `refs/remotes/origin/<branch>` ref without `--track`;
- menu item 5 also uses an explicit fetch refspec and explicit remote ref;
- Tests `36362625071` — PASS;
- repair synced into the v0.5.51 branch.

Next phone action remains menu-only:
**5 — Оновити проєкт з GitHub**, then **16 — Тестовий candidate: перейти + завантажити APK**.


### Interrupted candidate checkout recovery — 2026-09-28

Real phone reached a third Termux edge case: failed `git switch --track` left the v0.5.51 candidate tree staged on the current branch. The clean-worktree guard then blocked further menu operations.

PR #144 merged to `main` as `84659fe110030eb1bc12c9b447758b32a8b3d7d3`.

The candidate script now auto-recovers only the exact safe residue case:
- no untracked files;
- no unstaged edits;
- staged tree exactly equals the fetched PR #138 candidate tree;
- current branch is not already the candidate.

Only then does it reset the interrupted checkout residue and continue. Any other dirty state still stops.

Tests `36363339053` — PASS. Fix is also synced into the v0.5.51 branch.

For the one phone already stuck on the old script, a single emergency `git reset --hard HEAD` is needed, then resume menu-only flow: item 5 → item 16.


### v0.5.51 phone gate started — 2026-09-28

User confirmed the v0.5.51 candidate APK was downloaded and installed on the real phone.

Next phone action:
Renault Docs → project Megane II → **Створити .rdpkg з raw** → choose original raw NT8340A folder.

Do not mark PHONE PASS yet; only installation is confirmed.


### v0.5.51 phone finding: Runtime IR OOM — 2026-09-28

The first real raw-folder phone run used the wrong source volume by mistake, so it is diagnostic evidence only, not the NT8340A acceptance run.

Phone evidence:
- volume: `NT8393 · 2007-06-04`;
- source files: `14228`;
- Runtime IR completed `351` sections;
- failure: Android OOM requesting `150994952` bytes with heap growth limit `268435456`;
- incomplete destination `.rdpkg` remained visible after failure.

Root cause identified in the native compiler:
`runtimeTree.toString(2)` followed by `File.writeText()` materialized the full large Runtime IR tree as a giant String/byte array on top of the already-resident JSONObject tree.

Repair on PR #138:
- buffered recursive JSON streaming writer replaces whole-document `toString()/writeText` for Runtime IR artifacts;
- incomplete SAF destination cleanup now has a `DocumentsContract.deleteDocument()` fallback and user-visible warning on cleanup failure.

NT8340A PHONE PASS remains pending. A rebuilt candidate must be installed before the next real acceptance run.


### v0.5.51 OOM-fix candidate green — 2026-09-28

The Runtime IR streaming + incomplete-SAF-destination cleanup repair is now fully green.

Candidate source:
`c0039b09ca4406dd73333a83cd9d44744dbf4f40`.

CI:
- Tests `36365007868` — PASS;
- Android Debug APK `36365007904` — PASS.

Artifact:
- `Renault-Docs-v0.5.51-Debug`;
- artifact id `10946489978`;
- APK SHA-256 `6148bd8c46d917812c0eab8dff0062416414ea871f91731afd725528de20d1d6`;
- artifact ZIP SHA-256 `03691e66dda1abba7d6df47ad71b46753af8fca7a603a04abb27afe7a9cad97d`.

Next phone action:
install this updated candidate through the Renault Termux menu, then run raw-folder conversion on the correct NT8340A source. PHONE PASS remains pending.


### Termux APK artifact lookup repair — 2026-09-28

Real phone showed menu item 16 returning silently immediately after the exact artifact search line.

Cause:
`set -euo pipefail` + `gh api ... | head -n 1`; multiple matching v0.5.51 artifacts cause SIGPIPE on `gh`.

Repair:
- PR #145 merged to `main` as `e05a3edb3c999ac70c8056e37f11f6475e2296ca`;
- Tests `36367038334` — PASS;
- feature branch has the same fix;
- no Android runtime code changed.

Next phone action:
menu item **5**, then item **16** again.


### v0.5.51 OOM-fix candidate installed — 2026-09-28

User confirmed the updated v0.5.51 candidate containing Runtime IR streaming and failed-destination cleanup fixes is installed on the real phone.

Next phone action:
Renault Docs → Megane II → **Створити .rdpkg з raw** → select the correct original raw **NT8340A** folder.

The previous NT8393 run remains diagnostic only and does not count toward NT8340A PHONE PASS.


### v0.5.51 NT8340A phone run started — 2026-09-28

User confirmed the correct raw NT8340A source is now running through the updated OOM-fix candidate.

Phone evidence:
- project: Megane II;
- existing volume remains `NT8340A · 2006-04-18`;
- native status: `Копіюю і нормалізую raw source…`;
- progress observed: `900/21880`.

Next phone gate inside this same run:
rotate the device, send app to Home/background briefly, then reopen and confirm the same operation continues/reattaches without duplicate work.


### NT8340A progress cadence observation — 2026-09-28

During the real NT8340A phone run:
- copy UI updates every 100 files by design;
- DocumentsContract scan updates every 500 files; fallback scan updates every 100;
- the selected destination can remain 0 bytes until the final outer package writer begins.

Potential lifecycle issue to resolve:
the previous phone screenshot had already reached COPY `900/21880`, while a later screenshot showed SCAN `3500 files / 27 folders`. If no manual second run was started, the operation restarted and this must be fixed before acceptance.


### v0.5.51 phone finding: parent raw folder + nested destination — 2026-09-28

The 21880-file run used the parent SAF folder `Megane II`, not one exact raw Renault volume folder. The filename `Megane-II_Megane-II.rdpkg` confirms the selected source name.

Consequences:
- recursive scan included unrelated sibling content (including the earlier NT8393 raw source);
- destination `.rdpkg` was created inside the selected source tree and could appear in the scan as a 0-byte file.

This is a source-boundary UX bug, not evidence that the converter wrote into the raw source.

Fix added on PR #138:
- require a root Renault entrypoint before any staging copy;
- reject destination inside the source tree and clean the just-created document.

The current 21880-file run must be cancelled and does not count toward PHONE PASS.


### v0.5.51 lifecycle hardening for duplicate native start — 2026-09-28

The phone showed one non-deterministic observation where UI appeared to move from copy progress back to scan progress after a window/app handoff. The user later repeated window switching and could not reproduce it.

Because a narrow pre-run persistence race existed, PR #138 now hardens start idempotence:
- `startInFlight` blocks duplicate start requests before `runStore.begin()` becomes visible;
- service refuses to start a new worker when persisted run state is already running;
- existing per-service `workerRunning` guard remains.

This is defensive lifecycle hardening; the observation itself is not yet proven as a reproducible bug. CI pending at `7655b84e66f0dc6943fc573e651a569588b3dfae`.


### Confirmed phone evidence: parent folder contained 2 volumes — 2026-09-28

The user allowed the 21880-file run to finish. It reached:
`Private staging готовий · 21880 файлів.`

Then the compiler failed with:
`Для .rdpkg потрібно вибрати одну Renault volume-папку. Знайдено томів: 2.`

This definitively confirms the selected source was the parent/mixed folder containing two Renault volumes, not the single NT8340A raw volume. The run does not count toward PHONE PASS.

PR #138 already contains an earlier source-root guard so future builds fail fast before staging this wrong selection.


### v0.5.51 boundary/lifecycle candidate green — 2026-09-28

Latest phone candidate is fully green.

Source:
`28ec4faf12c5ccea0daeb16f10d7afebc52b785a`.

CI:
- Tests `36368715992` — PASS;
- Android Debug APK `36368715769` — PASS.

Artifact:
- `Renault-Docs-v0.5.51-Debug`;
- artifact id `10948048373`;
- APK SHA-256 `39bd542fb503b12f4431e1a21cd0cfd866cb6b2f489a0bf9c09e12b06c109a1c`;
- artifact ZIP SHA-256 `87c46dd1e58a70a261a52dc8d35deca9463dd9ddf6294837471d0d7d04d607c4`.

It includes OOM streaming, failed destination cleanup, fail-fast single-volume source validation, nested-destination rejection, and duplicate-start lifecycle hardening.

Next phone action is menu-only: item 16, install over current v0.5.51, then rerun on the exact NT8340A raw folder.


### NT8340A generation succeeded; stale Activity-result replay confirmed — 2026-09-28

Correct NT8340A real-phone generation succeeded end-to-end far enough to prove the native data path:
- Runtime IR: 347 sections;
- Fast Pack: 6138 files;
- outer package: 8012 files;
- automatic import executed;
- final result: `NT8340A · 2006-04-18 · 347 native`;
- SHA-256: `7d6cc758b329adcd1ef46680de4d9c6815ff48b053f4e3550b06dd92b229b77e`;
- project remained at 2 volumes, confirming update/upsert rather than duplicate volume creation.

The same screenshot sequence proves an intermittent lifecycle replay:
after import progress, the app later showed a fresh raw scan and section compilation without a new explicit user start. This can only be a second native run.

Root cause boundary:
the earlier `startInFlight` / persisted-RUNNING / `workerRunning` guards only block duplicates while a run is starting or active. A stale Activity result delivered after completion can still start again.

Fix now on PR #138:
- picker session UUID persisted in Activity saved state;
- durable, synchronized one-shot request claim stored separately from run-state prefs;
- same request id is rejected forever on replay, while a newly initiated user run receives a new id.

CI pending at `6b334eafb1fbf9dcca591fa4d1b619885f32ea4b`.

Do not mark PHONE PASS until the fixed candidate survives the lifecycle test and the resulting NT8340A is reopened as native without compatibility fallback.


### NT8340A native reopen confirmed — 2026-09-28

Phone screenshot after generated-package import confirms the resulting volume reopens natively:
- `NT8340A · 2006-04-18`;
- `Розділів: 347 · native`;
- no compatibility / `Сумісний режим` fallback;
- `Classic` remains a separate explicit alternate button.

These acceptance sub-gates are PASS.

Do not mark full PHONE PASS yet: stale-request lifecycle replay must be retested on the fixed candidate, and public `*_android` absence still needs explicit phone verification.


### Stale-request replay candidate green — 2026-09-28

The one-shot request-id lifecycle repair is fully green.

Runtime source:
`6b334eafb1fbf9dcca591fa4d1b619885f32ea4b`.

CI:
- Tests `36370544159` — PASS;
- Android Debug APK `36370544247` — PASS.

Artifact:
- id `10948508448`;
- APK SHA-256 `7b51c30cdd0be50de48ff8ddc21c6ee85f33c00eaa9c93adacf22fec4ef5ab48`.

Immediate phone gate:
menu item 16 → install over v0.5.51 → repeat lifecycle handoff test. Full PHONE PASS remains pending.


### Replay-fix rerun is clean and deterministic — 2026-09-28

Phone screenshots from the fixed candidate show one monotonic NT8340A run:
scan → copy (`7653` source files) → section index → Runtime IR `347` → Fast Pack `6138` → outer package `8012` → automatic import → COMPLETE.

No second scan/restart appears in the submitted sequence.

Final SHA-256:
`7d6cc758b329adcd1ef46680de4d9c6815ff48b053f4e3550b06dd92b229b77e`,
identical to the previous successful NT8340A package. This is strong deterministic-output evidence.

Before closing lifecycle PASS, get explicit user confirmation that rotation/background/file-manager handoffs were performed during this exact rerun. Then only the no-public-`*_android` storage check remains.

Non-blocking UX polish found: `Імпортую .rdpkg… 1 файлів` should use singular `1 файл`.


### Post-completion stale replay phone check passed — 2026-09-28

User explicitly confirmed that after the fixed-candidate NT8340A run completed, they rotated the phone, left the app, and switched between other apps/windows. No automatic second conversion started.

Therefore the exact previously observed **post-completion stale Activity-result replay** is phone-PASS on the fixed candidate.

The only lifecycle uncertainty left is the active-PREPARING handoff matrix, because the user is not certain whether all rotation/background steps were performed before completion. This can be closed with a short disposable run and Cancel during PREPARING; no second full package build is necessary.


### PREPARING cancellation passed; Documents/Renault has legacy clutter — 2026-09-28

Phone evidence confirms cancellation during active PREPARING:
`Копіюю і нормалізую raw source… 1/7653` → Cancel → `Source не змінено, private staging очищено.`

This is PASS for the cancel sub-gate.

A root storage screenshot of `Documents/Renault` shows existing user-visible legacy `*_android` directories:
`laguna 2 2001-2006_android`, `Megane II_android`, and `Megane II_NT8342A_android`.

No new NT8340A-specific `*_android` output is visible from the Kotlin-native run. Do not conflate legacy storage clutter with current-flow behavior. Handle cleanup as a separate audited task; no blind deletion.


### Tomorrow checkpoint — 2026-09-29

Start from PR #138 / `feat/v0.5.51-native-preparation-foundation`.

Known accepted phone evidence:
- raw NT8340A → Kotlin-native package succeeds;
- source count `7653`;
- Runtime IR `347`;
- Fast Pack `6138`;
- outer package `8012`;
- SHA-256 repeatable:
  `7d6cc758b329adcd1ef46680de4d9c6815ff48b053f4e3550b06dd92b229b77e`;
- importer/upsert succeeds without duplicate volume;
- reopen is `NT8340A · 2006-04-18` / `347 · native`;
- no compatibility fallback;
- post-completion stale replay regression is PASS;
- PREPARING Cancel is PASS.

Still open:
1. one short active-PREPARING lifecycle handoff check (rotation + Home/background + external app + return, then Cancel);
2. final v0.5.51 PHONE PASS/closeout;
3. non-blocking UX: terminal COMPLETE/CANCELLED/FAILED status needs a right-side close/dismiss icon;
4. non-blocking grammar: `1 файлів` → `1 файл`;
5. audit historical public `*_android` folders before any cleanup;
6. consolidate v0.5.51 documentation after PHONE PASS so active docs stop accumulating stale/pending chronology.

Do not delete any historical `*_android` folder without a reference/use audit.


### v0.5.51 PHONE PASS — 2026-09-28

Final active-PREPARING lifecycle phone check passed on the request-id guarded candidate.

Phone sequence:
- same copy operation progressed `500/7653 → 1400/7653 → 2300/7653` across the agreed rotation/background/external-window handoffs;
- no reset to a fresh scan;
- no second/parallel conversion started;
- Cancel during PREPARING then completed cleanly with source unchanged and private staging removed.

Combined accepted v0.5.51 phone evidence:
- raw NT8340A source count `7653`;
- Runtime IR `347`;
- Fast Pack `6138`;
- outer package `8012`;
- deterministic SHA-256:
  `7d6cc758b329adcd1ef46680de4d9c6815ff48b053f4e3550b06dd92b229b77e`;
- auto validation/import PASS;
- project upsert with no duplicate volume;
- reopen exactly `NT8340A · 2006-04-18`;
- Modern `347 · native`;
- no compatibility fallback;
- Classic remains explicit alternate;
- post-completion stale replay PASS;
- active-PREPARING lifecycle PASS;
- current Kotlin-native flow created no new public `*_android` intermediate.

Conclusion:
**v0.5.51 PHONE PASS**.

Next release action:
consolidate stale chronological docs into one current PASS summary, then prepare PR #138 for merge without changing the accepted runtime.

Follow-up work after the accepted candidate is frozen:
- terminal COMPLETE/CANCELLED/FAILED status dismiss `×`;
- Ukrainian `1 файл` wording;
- read-only audit of historical public `*_android` folders before cleanup.


### Public-repo closeout correction — 2026-09-29

The active phone clone now points to public `faric-ua/renault-docs-android`. Its `main` is still v0.5.50.

A mistaken closeout instruction caused item 7 to build public main:
- commit `97a5c6e29bd939197784f9ffced09161f5876bb2`;
- run `36580219193`;
- version v0.5.50;
- build PASS, but Android installer reported `Додаток не встановлено`.

Reason: this is a lower version than the installed v0.5.51 candidate (versionCode 66 vs 67), so it is not a valid candidate install.

The phone-accepted private feature was transplanted file-for-file onto public branch:
`feat/v0.5.51-native-preparation-foundation`.
Public continuation PR: **#1**.

Termux candidate flow was hardened:
item 16 now selects/switches the public PR branch and launches `reno-build-apk.sh`, guaranteeing a fresh workflow_dispatch build with the stable signer.

Next phone action after PR CI is green:
`reno-docs` → 5 (update main) → 16 → select PR #1.


### Public signing continuity blocker — 2026-09-29

Public continuation PR #1 is mergeable and CI-green at `266840f5182f1c49a4b84673592367744f09e77e`:
Tests `36583494822` PASS; Android Debug `36583494741` PASS.

Do not merge yet.

The phone's accepted v0.5.51 build and the new public repository build use different development signing identities:
- accepted/private signer SHA-1 `4102350e2787fd538bbf58a219293a132235e618`;
- public signer SHA-1 `aa91096d3699a62c0d6e4d4306064068e67fda0d`.

The user accidentally built public `main` v0.5.50/versionCode 66 (run `36580219193`) and Android showed `Додаток не встановлено`. That attempt had both a version downgrade and signer mismatch.

Release-safe path:
preserve update/data continuity by restoring the original accepted signer through the user's secure local/private backup workflow, without placing key material in the public repository or chat. Then build PR #1 as v0.5.51/versionCode 67, verify accepted cert SHA-256 `dd588f...`, install over the existing app, and only then merge.


### v0.5.51 public merge — 2026-09-29

Active repository:
`faric-ua/renault-docs-android`.

Public PR #1 was marked ready and squash-merged after final PR-head CI was green.

Merged SHA:
`921e87f728a222e8f388a01ad989b082a7bd8894`.

Current public main:
- `versionName 0.5.51`;
- `versionCode 67`.

Important remaining distribution checkpoint:
do not ask the user to uninstall/reinstall Renault Docs yet. The phone-accepted build used the accepted development certificate SHA-256
`dd588fbb3093a047f81c24397fc6d2ab7f5a32040425a21dd24506cce59a9802`.
The public repository signing identity must be restored/verified to match before a new public-main APK is treated as an in-place update.

The earlier phone menu build `36580219193` was public `main` v0.5.50 and is not v0.5.51 evidence.

Project-separation rule:
Renault Docs and YTM importer remain independent projects. Never mix their branches, menus, artifacts, QA, or release state.


### Accepted signer restore helper — 2026-09-29

Public main now includes Renault Menu item `20 — Відновити accepted signer + build`.

Merged helper SHA:
`239663c4577c2e4d279a4052f1b0ac460b19bf9d`.

Purpose:
restore the exact phone-accepted development signing identity from the private historical archive into the public repository's GitHub Actions Secrets without exposing key/password values in chat or terminal output.

The helper verifies:
- keystore SHA-256 `7944e7d78bd2442021731a4cfd3105c06f475c13d70d4195b2378539604dfd10`;
- accepted certificate SHA-256 `dd588fbb3093a047f81c24397fc6d2ab7f5a32040425a21dd24506cce59a9802`.

Then it launches an exact public-main build.

Next user action:
`reno-docs → 5 → 20`.
Do not uninstall Renault Docs. After the build, capture the final output before attempting installation.


### Signing workflow hardened — 2026-09-29

Merged:
`5aa665756e7b44de3dbb90bff2250d4bbe30596b`.

The Android Debug signing workflow no longer has an automatic `pull_request` trigger.
Accepted signing secrets are restricted to trusted execution paths:
- main push;
- explicit manual workflow dispatch.

Separate Python/contract Tests still run on PRs.

Next user step:
`reno-docs → 5 → 20`, then return the full signer-restore/build output before installing the APK.


### Accepted signer restored and verified — 2026-09-29

Renault Menu item 20 successfully restored the accepted development signer from the private archive into public GitHub Actions Secrets and launched an exact public-main build.

Verified public-main build:
- commit `fc7ffc6a7deda7074e5d6b2e631380fd923401c0`;
- Android Debug run `36603132862` — PASS;
- artifact id `11050306597`;
- signer SHA-1 `4102350e2787fd538bbf58a219293a132235e618`;
- workflow verified accepted signer SHA-256 `dd588fbb3093a047f81c24397fc6d2ab7f5a32040425a21dd24506cce59a9802`;
- APK SHA-256 `7b51c30cdd0be50de48ff8ddc21c6ee85f33c00eaa9c93adacf22fec4ef5ab48`;
- artifact ZIP SHA-256 `ddf9eb0ee9ef53e31ce997d157855c6bb3b1d7111cc1e662686039ddc4f55bb2`.

The public-main APK SHA-256 is identical to the previously phone-accepted v0.5.51 APK, so signer continuity and APK identity are confirmed.

Final release-distribution gate:
install this exact APK over the existing Renault Docs without uninstall/data reset, reopen, and confirm the pre-install baseline still exists: v0.5.51 and Megane II with 2 volumes.


### v0.5.51 final distribution closeout — 2026-09-29

Final phone install check passed.

The exact trusted public-main APK was installed over the existing Renault Docs application without uninstall/data reset.

Post-install screenshot confirms:
- app version remains `v0.5.51`;
- `Megane II` project is preserved;
- `Томів: 2` is preserved;
- Laguna II and Kangoo II placeholders remain present.

This closes the distribution/signing continuity gate.

Final status:
**v0.5.51 READY / CLOSED**.

Next development patch:
- terminal COMPLETE/CANCELLED/FAILED dismiss `×`;
- fix `1 файлів` → `1 файл`;
- then perform read-only audit of legacy `*_android` folders.


### v0.5.54 sleep handoff — 2026-10-01

Active repository:
`faric-ua/renault-docs-android`

Branch / PR:
- `feat/v0.5.54-ux-help-audit`;
- PR #13 open, mergeable, **NOT MERGED**.

Latest code-bearing head before this checkpoint:
`82491c108f5a75352edd6466d27c0f9f0b475114`.

CI on that head:
- Tests `36813567420` PASS;
- Android PR Check `36813567450` PASS.

Accepted phone evidence this session:
- compact Classic `Як користуватись` response-level layout is visibly working;
- old giant help layout is gone;
- file info is now a wrapping `Файл | Призначення` table;
- Laguna public prepared dataset is restored to 10 physical volumes;
- final item 22 integrity result: 48307 HTML/CSS, 264793 local refs, 0 missing;
- live / manifest / volumes.json / modern-index.json all report 10 volumes with metadata parity PASS;
- source/build parity is 10/10 with 0 missing and 0 extra.

Critical architecture distinction:
1. `Старі бібліотеки` Laguna II is a SAF-linked **legacy public dataset** at the old `*_android` location. Classic opened from that record reads the public dataset directly.
2. Project `Laguna II` built from `.rdpkg` uses **installed app-private packages** under `noBackupFilesDir/rdpkg/<packageId>` exposed through `LocalDatasetDocumentsProvider`. Classic opened from a Project volume reads that private installed copy.
3. Files in `Documents/Renault/packages/rdpkg` are install archives. All 10 Laguna II archives are present, but that alone does not prove all 10 are already installed into the Project.

All 10 Laguna II `.rdpkg` archives observed:
NT8183A, NT8218A, NT8236A, NT8240A, NT8254A, NT8282A, NT8283A, NT8307A, NT8327A, NT8328A.

Resume:
1. open old `Старі бібліотеки → Renault Laguna II 2001–2006`, then return Home; verify its card refreshes to `Томів: 10`;
2. separately open Project `Laguna II` and count its installed volumes;
3. if fewer than 10, determine which `.rdpkg` archives are not installed; do **not** confuse this with the repaired legacy `*_android` dataset;
4. continue v0.5.54 regression: Converter styling/stale Legacy handling, then Help/dialog rotation/lifecycle;
5. keep `RISK-LIFE-001` as a separate follow-up;
6. do not merge PR #13 until full phone PASS.


### Legacy Laguna refresh phone PASS — 2026-10-01

Real-phone evidence after metadata repair:
- legacy Laguna II opened successfully;
- Modern/legacy dataset shows `томів: 10`;
- Home `Старі бібліотеки` card now shows `Томів: 10 · відкриття: Classic`;
- all 10 physical prepared volume folders are present in the public `*_android` dataset;
- all 10 Laguna II `.rdpkg` archives are present in the public package folder.

This closes the old `Томів: 3` legacy symptom.

Next phone step is intentionally separate:
open the normal Project `Laguna II` and count **installed Project volumes**. Presence of 10 public archives does not prove 10 packages are installed into the Project.


### Project Laguna II 10-volume phone PASS — 2026-10-01

Normal Project `Laguna II` now independently confirms:
- project opens;
- header shows `томів: 10`;
- installed volume list is populated;
- no additional Laguna II `.rdpkg` import is needed.

Both storage paths are now verified:
- Legacy public dataset: 10/10;
- Project app-private installed packages: 10/10.

Next phone regression target: Converter styling and stale-Legacy behavior, then lifecycle/rotation.


### Converter visual phone PASS — 2026-10-01

Real-device screenshot confirms:
- source/destination selectors use Renault Docs dark bordered controls;
- validate/start/clear actions no longer use default system-gray Android buttons;
- primary start action uses accent styling;
- cancelled-state status text renders normally.

Still pending:
- Cancel danger styling while an operation is actively running;
- Converter Help rotation/lifecycle.

Minor non-blocking copy polish observed: `source` / `destination` remain English inside Ukrainian UI.


### Converter Help lifecycle phone PASS — 2026-10-01

Real-device screenshot after the requested rotation cycle confirms:
- Converter Help remains open above the same Converter screen;
- Help uses Renault Docs dark dialog styling;
- no SAF picker is visible;
- no conversion/action auto-started under the dialog.

Final micro-check after closing Help:
the same Converter state should remain visible with its selected source/destination unchanged.
