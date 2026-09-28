# Renault Docs — Session checkpoint — 2026-09-24

Цей файл фіксує повний робочий контекст після довгої сесії перенесення Renault legacy UI у Modern Android UI.

## 1. Поточна точка

Актуальний напрямок: не ламати робочий legacy Renault runtime, але поступово прибирати його з видимого UI.

Остання встановлена/перевірена хвиля:
- v0.5.3 — функціонально успішна: controls/combo працюють через повний оригінальний frameset;
- v0.5.4 — спроба приховати Classic runtime в Modern shell;
- реальний phone result v0.5.4: FAIL по презентації — програма показала:
  `Modern shell: Classic runtime не вдалося повністю сховати.`
  і відкрила Classic/Visu Schema splash замість чистого Modern content surface.

Останній app merge для v0.5.4:
`5a3718dacd37932d9a869b994e2a6dcba5c80dd6`

Документаційний main до цього checkpoint:
`b8033d07fee0ddd06f5da12a2a18948086b4413e`

## 2. Ключове архітектурне рішення

НЕ повертатися до прямого відкриття child HTML fragment як основного підходу.

Це вже перевірено і не працює стабільно:
- v0.5.0–v0.5.2 відкривали direct child section entrypoint;
- частина сторінок була неповною;
- CMP101:
  - 1–6 не працювали;
  - combo/select взагалі був відсутній;
- причина: child HTML залежить від sibling frames + parent/top frame context.

Правильна база:
- native Modern screen вибирає 101/103/105/...;
- всередині має існувати повний legacy runtime/frameset;
- оригінальні Renault cross-frame scripts повинні залишитися живими;
- Classic runtime має стати implementation detail, а не видимим UI.

v0.5.3 довів, що саме цей runtime підхід функціонально правильний:
- старі controls працюють;
- combo/select працює;
- PDF та внутрішні переходи працюють.

Проблема тепер НЕ у функціональності runtime, а у відділенні runtime від Modern presentation layer.

## 3. Останній phone finding v0.5.4

На реальному телефоні після Modern → volume → section:

Верхній Android header:
- title: `Visu Schema`;
- кнопка `Modern`;
- Home;
- Search;
- Settings.

Під header:
`Modern shell: Classic runtime не вдалося повністю сховати.`

Після timeout/fallback видимий Classic Renault splash:
- жовтий фон;
- `Laguna 2`;
- Renault logo;
- `NT : 8183A`;
- `22.01.2001`;
- legacy language flag.

Висновок:
- fallback спрацював коректно з точки зору fail-safe;
- hybrid selector/hide contract не завершився;
- Modern UI фактично знову показав Classic root.

Цей результат треба вважати:
`PHONE_FAIL_MODERN_PRESENTATION_RUNTIME_OK`.

## 4. Що вже стабільно працює

### Dataset / package

Dataset:
`Renault Laguna II 2001–2006`

10 volumes:
- NT8183A · 2001-01-22
- NT8218A · 2002-05-01
- NT8236A · 2002-11-18
- NT8240A · 2003-11-17
- NT8254A · 2004-06-21
- NT8282A · 2005-04-22
- NT8283A · 2005-08-29
- NT8307A · 2005-12-12
- NT8327A · 2006-02-06
- NT8328A · 2006-05-09

Chronological sorting is required in both Modern and Classic volume menus.

Package contains:
- `renault-dataset.json`
- `_renault/START.html`
- `_renault/README_UA.html`
- `_renault/volumes.json`
- `_renault/modern-index.json`
- `_renault/modern-sections.json`
- `_renault/fast-content-*.zip`

Fast Pack is the performance path; do not reconvert ~61k files unless actually required.

Termux:
- `Renault → 9 — Оновити Fast/Modern package` updates indexes/package;
- `Renault → 8 — Download latest APK` / download helper gets CI APK;
- when only APK code changes, point 9 is NOT required.

### Modern volume list

Working:
- default Modern start;
- list of 10 volumes;
- fast search;
- chronological order;
- Classic fallback;
- Home / Search / Settings header.

Search UX fixes already done:
- search field readable;
- control buttons on lower row;
- previous query retained;
- reopening search reactivates result matching without requiring manual onChange.

### Native section list

v0.5.0 added:
`_renault/modern-sections.json`

Modern volume opens native one-column Technical Blue section list:
- codes 101 / 103 / 105 / ...;
- native search by code/title;
- Classic fallback;
- Modern bridge back to same volume section list.

Keep this screen. It is the correct user-facing section chooser.

### PDF viewer

Working:
- original PDF render;
- original PDF export/save via Android document picker;
- no broad storage permission;
- zoom;
- manual percent input;
- quick preset dropdown:
  - 85%
  - 100%
  - 120%
  - 150%
  - 200%
- +/- configurable zoom step;
- Fit width;
- dropdown popup clipping bug fixed in v0.4.5;
- Fit width arrow/text alignment fixed;
- PDF toolbar is responsive/horizontally scrollable where needed.

PDF save exports original source PDF, not screenshots.

### Settings

Existing:
- PDF +/- zoom step setting;
- Backup folder chooser;
- backup path presentation was improved from raw SAF URI to readable path;
- reset folder action moved into logical backup card area.

Future settings already desired:
- language;
- app/about/version;
- skins/themes;
- Google authorization;
- Google Drive backup/sync;
- data/settings/disk-image backup options.

### Converter / backup direction

Desired converter contract:
- after successful conversion, originals are no longer needed for Android runtime;
- default flow should protect original source before deletion;
- preferred design:
  1. convert;
  2. verify output;
  3. archive original source;
  4. move archive into Backup;
  5. only then delete original folder;
- destructive step must have explicit confirmation;
- explanation should state that old source files are not needed for the converted Android package and are kept in backup for recovery.

Do not implement destructive cleanup without verified backup/archive success.

## 5. UI / design direction

Selected visual family:
`Technical Blue`

Assets/contracts already stored under:
`docs/design/skins/technical-blue/`

Modern target header:
- Back
- context title
- Modern/Classic mode switch where appropriate
- Home
- Search
- Settings
- optional overflow later

Modern target content:
- compact one-column lists;
- native section list instead of old left frames;
- native toolbar/control surfaces;
- legacy content only where not migrated yet.

User explicitly wants all themes/skins/design concepts saved in project, not discarded.

## 6. Legacy content findings by release

### v0.5.0
Direct native section → child legacy entrypoint.

Findings:
- some content works;
- PDF works;
- some legacy dropdown/image pages work;
- some pages require frame context;
- CMP141 had black text on dark app background.

### v0.5.1
Added standalone legacy compatibility:
- white canvas for transparent pages;
- missing named target fallback;
- `window.open` fallback.

Useful but not enough for frame-dependent section shells.

### v0.5.2
Added frame-JS/select/click emulation.

Real phone result CMP101:
`1- 2- 3- 4- 5- 6- combo absent`

Conclusion:
direct fragment architecture is insufficient.

### v0.5.3
Changed to full original frameset runtime and auto-select requested section.

Phone result:
- runtime controls work;
- combo/select works;
- architecture functionally correct;
- but Modern visibly launches Classic runtime.

This is the known-good functional baseline.

### v0.5.4
Tried:
- hide WebView during warmup;
- auto-select section;
- collapse menu frame(s);
- reveal after hybrid-ready signal.

Phone result:
- timeout/fallback;
- message says Classic runtime could not be fully hidden;
- Classic Visu Schema splash visible.

Do not treat this as runtime regression.
Treat it as failure to identify/collapse/project the correct visible frame hierarchy.

## 7. Recommended next step — do diagnostics before another hide algorithm

Do NOT guess another frameset-collapse heuristic immediately.

Next implementation should first capture the real legacy frame tree on the phone after a successful v0.5.3-style runtime load.

Recommended v0.5.5 diagnostic wave:

Add a temporary/dev diagnostic action that exports or displays a structured frame report after section selection.

For every reachable same-origin frame/window collect:
- depth;
- index within parent;
- `window.name`;
- document title;
- current relative URL/src;
- parent frameset rows/cols;
- frame element name/id;
- frame element width/height;
- bounding rect if available;
- whether visible;
- number of 3-digit section codes;
- presence/count of `select`;
- presence/count of PDF links;
- presence/count of images/buttons;
- body background;
- short text fingerprint.

Also record:
- which frame contains the old 101/103/... menu;
- which frame contains the working combo;
- which frame becomes the main working content after selecting 101;
- whether top splash is a separate frame or the initial document before section selection;
- exact frameset nesting before and after original Renault handler runs.

Preferred output:
`_renault/debug-frame-tree.json` or a copyable on-screen JSON/text report.

Then use that evidence to define an explicit projection/collapse contract for this Renault generation instead of heuristic `menuScore >= N`.

## 8. Likely next architecture after diagnostics

Goal:
`Modern shell + hidden full legacy runtime + visible working content only`

Potential implementation direction after frame tree is known:
- keep full frameset alive;
- identify exact navigation/menu/splash frame(s);
- identify exact content frame(s);
- set frameset rows/cols deterministically rather than by generic scoring;
- preserve hidden frames at 0px rather than deleting them;
- reveal only after target content frame reports expected state;
- do not change original Renault JS functions unless necessary.

If deterministic frame hiding is impossible for some discs, consider a stronger split:
- offscreen engine WebView with complete frameset;
- visible Modern content surface fed by the active legacy content frame;
but this is a second choice because synchronizing DOM/navigation/PDF between two WebViews is more complex.

## 9. Product backlog captured in this conversation

Keep these ideas in project backlog:

- fully native replacement of old 101/103/... frame menu;
- native inner Renault top toolbar/icons;
- native engine/image selector;
- native combo/select where practical;
- page text search;
- Home / Search / Settings main header;
- language support;
- themes/skins;
- About/version;
- Google sign-in;
- Google Drive backup;
- settings/data/disk-image backup;
- possible operation directly from Google Drive;
- converter originals archival + confirmed cleanup;
- Backup Center;
- restore flow;
- preserve original legacy source archive for recovery;
- keep Classic as explicit fallback/debug path, not default Modern UI.

## 10. Do-not-regress contracts

1. Never trade away working legacy runtime controls merely to make UI look Modern.
2. Native section list stays.
3. Full frameset runtime from v0.5.3 is the current functional truth for frame-dependent content.
4. PDF export must always save original PDF bytes.
5. Fast Pack performance must remain.
6. Volume order must remain chronological.
7. Search state/behavior fixes must remain.
8. Modern button from inner content should return to the same volume's native section list.
9. Classic remains available as fallback.
10. No destructive original-file cleanup without verified backup + confirmation.
11. Every significant phone finding and release decision must be saved under docs + CURRENT_HANDOFF.

## 11. Resume point for the next session

Start by reading:
1. `CURRENT_HANDOFF.md`
2. this file
3. `docs/v.0.5.3/RELEASE.md`
4. `docs/v.0.5.4/RELEASE.md`
5. `docs/v.0.5.4/qa/V0_5_3_PHONE_FINDING.md`
6. `docs/design/skins/technical-blue/UX_CONTRACT.md`

Then:
- mark v0.5.4 phone result as presentation fail;
- build frame-tree diagnostics first;
- do not create v0.5.5 UI hiding logic until actual frame tree evidence exists.

Session stop reason:
user ended work for the night after confirming v0.5.4 still opens Classic.


## 12. Session resumed — v0.5.5 diagnostic build

Work resumed on 2026-09-24.

Implemented the planned diagnostic wave before any new frame-hiding logic.

v0.5.5 adds a temporary `DBG` action to hybrid section viewer and captures a copyable real-device frame-tree report.

The next decision is blocked on phone evidence, intentionally.

Required evidence:
- one report from the v0.5.4-style Classic/Visu Schema fallback state;
- ideally one report after manually navigating to the working CMP101 controls/combo state.

Do not start v0.5.6 projection/hiding until those reports are analyzed.


## 13. v0.5.5 phone evidence and v0.5.6 implementation

The planned diagnostic gate is complete.

Two real-device frame-tree reports were captured for NT8183A / requested section 101:
- initial Classic/Visu Schema state;
- working 101 state after manual interaction.

Deterministic findings:
- top document settles on `RUS/HTM/ENTREE.HTM`;
- frames: `titre`, `org`, `menu`, `nav`, `doc`;
- `titre + org` are the left Classic navigation branch;
- `org` contains the 101/103/... list;
- `menu + nav + doc` are the working right-side runtime;
- selected 101 is observable as `MENU/101.HTM` and `PC/101.HTM`;
- the old hybrid state was absent on the final ENTREE page, which explains the v0.5.4 timeout.

v0.5.6 replaces generic scoring/hiding with the named-frame contract:
- inject on final legacy HTML;
- trigger requested code in `org`;
- verify `menu/nav` activation;
- collapse the whole common `titre+org` branch at the outer frameset boundary;
- preserve `menu/nav/doc`;
- lock projection with MutationObserver;
- retain DBG through phone validation.

Exact app source:
`a965413234d4d49c4a97714588096f91bb24708e`

CI:
- Tests `36005382508` — PASS;
- Android Debug APK `36005382468` — PASS;
- artifact `Renault-Docs-v0.5.6-Debug`;
- APK SHA-256 `9bfafa025d2207ad7303c408a9311ffeca4d1179904f331449a2ca3f63cfd757`.

Next action is phone validation of v0.5.6 on Modern → NT8183A → 101. No point 9 is required.


## 14. v0.5.6 phone validation — projection works; next focus is navigation + startup cost

Phone validation confirmed the deterministic projection contract:
- `phase: projected`;
- `attempts: 1`;
- `triggerAttempts: 1`;
- projection `cols: 0,*`;
- original outer value `216,747`;
- hidden branch is the common `titre + org` left column;
- `menu/nav/doc` stay alive.

The frame diagnostics also now detect 214 section codes in `org`.

New user requirement:
- section browsing must remain available while inside a document;
- next UI should prefer a Modern searchable section drawer/overlay instead of permanently restoring the old Classic left column;
- section switching should happen inside the current loaded runtime, avoiding full ViewerActivity/frameset restart.

New performance finding:
- first load remains slow even though section selection/projection succeeds on the first attempt;
- therefore the selector loop is not the source of the delay;
- instrument runtime/PDF/Fast Pack timing before optimizing.

Converter direction:
- current package already has one array-based `_renault/modern-sections.json`;
- evaluate schema v2 / runtime manifest that normalizes legacy navigation at conversion time and drives one generic shell.


## 15. v0.5.7 — live Modern section navigation

v0.5.7 is implemented, merged and CI-green.

Exact app source:
`4ae04f1a2eda42fa56553fd31668ceb13e6c750e`

Main CI:
- Tests `36009167466` — PASS;
- Android Debug APK `36009167695` — PASS;
- artifact `Renault-Docs-v0.5.7-Debug`;
- APK SHA-256 `df15e2931690395318c79b1a4ea0e9ea369a322bf2fb0f77651a94f2660ae3c0`.

Behavior:
- hybrid viewer has `Розділи` instead of forcing a round-trip to ModernVolumeActivity;
- searchable native section overlay uses existing modern-sections index;
- switching uses the currently loaded `org/menu/nav/doc` runtime and does not load a new top-level Viewer;
- active section is persisted across rotation;
- DBG now includes initial runtime/Fast Pack/navigation timing and latest live-switch timing.

Next action:
phone-test 101 → 103 → 105 → 101, then provide two DBG reports:
1. after the first slow volume open;
2. after a live section switch.

Do not start runtime-tree/schema-v2 work until this phone result is seen; it will show which layer actually dominates latency.

## 16. Architecture pivot — Classic preserved, Modern compiled to JSON IR

Decision accepted:
- Classic is already the working compatibility/reference implementation and stays.
- Modern should ultimately stop booting the full Classic frameset just to hide `titre/org`.
- Converter/package step becomes a compiler: it reads legacy HTML/JS once and produces normalized JSON for our own native navigation/runtime.
- Slow one-time conversion is acceptable in exchange for fast repeated runtime.
- The final Modern UI and skins are independent from Classic HTML layout.

Phase 1 implementation is on branch `feat/runtime-ir-v1`:
- `core/runtime_ir.py`;
- output `_renault/runtime-tree.json`;
- schema `renault-runtime-ir` v1;
- Classic shell/frame topology + named frames;
- Modern section catalog + legacy section entrypoints;
- placeholders for controls/actions/documents and explicit pending compiler stages;
- package manifest integration;
- tests in `tests/test_runtime_ir.py`.

Canonical plan:
`docs/architecture/MODERN_NATIVE_IR_PLAN.md`.

After merge, refresh the existing converted dataset with Renault Menu point 9 to generate a real `runtime-tree.json` from the phone dataset. Then use the real 101 subtree/files as the Phase 2 compiler target.

## 17. Runtime IR Phase 1 merged

Merged source:
`81503baf5dd819f31017f4888a59928757b6978f`

Main test run:
`36015407081` — PASS.

The converter/package pipeline now emits:
`_renault/runtime-tree.json`

The manifest exposes:
`runtime_tree: "_renault/runtime-tree.json"`

This is the first concrete implementation of the canonical architecture:
- Classic preserved;
- Modern data compiled to JSON;
- native UI/skin will consume our model rather than permanently relying on hidden legacy frames.

Next step requires the real phone dataset: regenerate package with menu point 9, inspect the NT8183A/101 IR, then implement Phase 2 control/action/document compilation around real 101 data.



## 18. Runtime IR Phase 1 real dataset PASS

User uploaded the real phone-generated `runtime-tree.json`.

Confirmed:
- schema v1;
- format `renault-runtime-ir`;
- 10 volumes;
- 2174 normalized sections total;
- NT8183A contains 214 sections;
- named-frame topology matches the real phone runtime;
- section 101 is correctly normalized as `ПРИКУРИВАТЕЛЬ` → `RUS/HTM/MENU/101.HTM`;
- compiler pending list correctly identifies controls/actions/document-routing/assets as Phase 2 work.

A focused Phase 2 source exporter is being added:
- `tools/export_runtime_section_bundle.py`;
- Renault Menu item 10;
- default target NT8183A / 101;
- output `packages/Runtime-IR-NT8183A-101-source.zip`.

Next step is to inspect the real 101 HTML/JS dependency bundle and design the concrete controls/actions/documents IR from evidence, not guesses.

## 19. Runtime IR Phase 2 source-bundle gate merged

Merged source:
`a933455ef66575911f3d4737e79409c5806238d2`

Tests:
`36020872886` — PASS.

Renault Menu item 10 exports a focused source bundle for one section.

Default:
NT8183A / 101.

Output:
`packages/Runtime-IR-NT8183A-101-source.zip`.

Next action is to upload that ZIP and design/implement the concrete controls/actions/documents IR from the real 101 source graph.

## 20. Runtime IR Phase 2 compiler merged

Real source bundle:
`Runtime-IR-NT8183A-101-source.zip`

Finding:
- 14 text files;
- 30 binary refs;
- 61 dependency edges;
- 101 legacy JS behavior is static/simple enough to normalize;
- SCH is a select → schematic PDF mapping;
- NM is a select → composite PDF + details table mapping;
- PC is a document card;
- GENERAL is a select → PDF mapping;
- MENU is a static action bar.

Compiler merge:
`e83992791cf79ae5b2b1ec49a14eaf866cfc8bd7`

Tests:
`36022346261` — PASS.

Runtime IR is now schema v2 / `section-ir-v2`.

Per-section IR includes:
`panels, controls, actions, documents, assets, source_files`.

Small compiled-section exporter:
merge `e96cf8af30f927c2ff3ab6e3d2467050f2fe6aa1`,
tests `36022523957` — PASS.

Renault Menu item 11 exports:
`packages/Runtime-IR-NT8183A-101-section.json`.

Next step:
refresh project, run point 9, run point 11 with default NT8183A/101, upload the small JSON and validate the real emitted v2 IR before starting the Android native renderer.

## 21. v0.5.8 native Runtime IR preview + cross-year menu coverage

User clarified a critical compatibility requirement:
newer Renault volumes/years contain additional/different menu items, so NT8183A/2001 must not become a hard-coded menu contract.

Canonical rule:
- compiler discovers menu/actions from each source file;
- Runtime IR stores arbitrary menu items;
- Android renders action-bar items from JSON;
- unsupported/dynamic legacy JavaScript remains explicit and falls back to Classic;
- full native support is claimed only after coverage across all 10 volumes/years is reviewed.

Implemented and merged:
`187513adfbb2a183e167eae4ab6a7f8edf38ef69`

v0.5.8 / versionCode 24.

New package output:
`_renault/runtime-ir-coverage.json`

Coverage tracks:
- menu labels by volume/year;
- panel/control/action/route/document types;
- compile states;
- unsupported legacy-javascript actions;
- compiler warnings.

Renault Menu item 12 exports:
`packages/Runtime-IR-Coverage.json`.

Android:
- `RuntimeIrReader` reads Runtime IR schema v2;
- `NativeSectionActivity` is the first generic native renderer;
- Modern section tap enters native Runtime IR preview;
- action-bar/select/document-list controls are data-driven;
- PDF routes reuse Android PDF viewer;
- structured HTML tables/headings render natively;
- composite nomenclature documents expose drawing PDF + native details;
- unknown actions retain Classic fallback.

CI evidence:
- PR Android APK run `36029132889` — PASS (app source identical; later commits were test-contract fixes only);
- PR artifact `Renault-Docs-v0.5.8-Debug`, id `10819859277`;
- APK SHA-256 `30ed5c6405f2995d80a0d50afe6405d9cf5e67ef1a772205eb2422af603984bc`;
- main Tests run `36029452241` — PASS;
- main Android run `36029452533` was still running when this checkpoint entry was written.

Phone/data gate:
1. Renault Menu → 5;
2. Renault Menu → 9;
3. Renault Menu → 12 and upload `Runtime-IR-Coverage.json`;
4. install v0.5.8 when main APK is available;
5. test Modern → NT8183A → 101;
6. compare native SCH/NM/PC/GENE/direct PDF/CRITERE behavior with Classic.



## 22. v0.5.8 final CI

Merged source:
`187513adfbb2a183e167eae4ab6a7f8edf38ef69`

Main Tests:
`36029452241` — PASS.

Main Android Debug:
`36029452533` — PASS.

Artifact:
`Renault-Docs-v0.5.8-Debug`
id `10820474101`

APK SHA-256:
`30ed5c6405f2995d80a0d50afe6405d9cf5e67ef1a772205eb2422af603984bc`

Next:
Menu 5 → 9 → 12, upload Runtime-IR-Coverage.json, then Menu 8 and phone-test native NT8183A/101.

## 23. Real Runtime IR coverage PASS

Real phone-generated coverage report is now available.

Laguna II 2001–2006:
- 10 volumes;
- 2174 sections;
- 2174/2174 `section-ir-v2`;
- 0 unsupported actions;
- 0 warnings.

Observed top-level menu labels across all current volumes:
`AIDE, GENE, PLATFUSI, SCH, PC, NM, blank`.

Important correction:
the later Laguna II volumes in the current dataset do not add extra top-level menu labels. The native renderer must still stay data-driven because item availability varies by section and a future dataset may differ.

Potential Laguna III:
if documentation is found, onboard it as a new dataset family through the same Classic-preserved → Runtime IR → coverage gate. Do not assume identical Classic topology before coverage evidence.

## 24. v0.5.8 native reader OOM / v0.5.9 shards

Real phone failure captured:
`Failed to allocate a 268501000 byte allocation ... growth limit 268435456`.

v0.5.8 was loading the complete dataset-level `runtime-tree.json` into one Android String/JSONObject.

v0.5.9 changes runtime delivery to:
- small `runtime-ir-index.json`;
- one per-section JSON shard;
- bounded reads;
- full tree kept only for converter/debug;
- Runtime IR JSON excluded from Fast Pack.

Merged app source:
`2ac19a941ce2f29d82d58e649cf332d343ab5a1d`.

Dataset point 9 is mandatory before the phone retest.

v0.5.9 final CI:
- Tests `36033113768` — PASS;
- Android Debug `36033113767` — PASS;
- artifact id `10823058032`;
- APK SHA-256 `36f5b1af5458006d11543fe768275a8402053e8c8441c8366b11eb23f85203fa`.

Next phone action remains 5 → 9 → 8, then Modern → NT8183A → 101.

## 25. v0.5.10 phone UX pass

v0.5.9 real phone PASS for Runtime IR sharding:
101 opens natively and NM composite/table rendering works.

Merged source:
`ee850788ce8481df94cb88a3ef3b8b9a29c621cc`

v0.5.10 changes:
- SCH → Схема;
- NM → Розʼєм;
- PC → Положення на авто;
- GENE/PLATFUSI/AIDE → grouped Документація;
- blank hidden;
- CRITERE → Критерії / скорочення;
- clickable rows prefixed with ›;
- unresolved items visibly marked `немає посилання`;
- Classic button now pure Classic (no old hybrid target flow);
- custom adaptive Renault Docs launcher icon added;
- versionCode 26 / versionName 0.5.10.

Play Protect warning seen on phone is separate from the app icon: sideloaded development APK / unverified Google Play developer path. Do not attempt to solve it by disabling Play Protect globally.

No point 9 needed if v0.5.9 shards are already present. Next phone test should be point 5 → point 8, then recheck 101 and Classic.



v0.5.10 final CI:
- Tests `36037676395` — PASS;
- Android Debug `36037676490` — PASS;
- artifact id `10824924034`;
- APK SHA-256 `269a093bba30292ee1da63ffe62b8e20d753f34287316e5dfbd3a9507ba4078c`.

Next phone action: point 5 → point 8, then test 101 and pure Classic fallback. Point 9 is not required if v0.5.9 Runtime IR shards are already present.

## 25. v0.5.10 native UX + launcher PASS in CI

Merged app source:
`ee850788ce8481df94cb88a3ef3b8b9a29c621cc`

Phone feedback addressed:
- custom launcher icon;
- SCH/NM/PC → Схема/Розʼєм/Положення на авто;
- GENE/PLATFUSI/AIDE → Documentation group;
- blank hidden;
- CRITERE localized;
- actionable vs unavailable link state exposed;
- Classic button now opens pure Classic instead of hybrid target-section logic.

Play Protect warning is tracked as a sideload/developer-verification distribution concern, not as a Runtime IR failure.

Main CI:
- Tests `36037676395` — PASS;
- Android `36037676490` — PASS;
- artifact id `10824924034`;
- APK SHA-256 `269a093bba30292ee1da63ffe62b8e20d753f34287316e5dfbd3a9507ba4078c`.

Next phone step:
Menu 5 → 8. No point 9 required when v0.5.9 shards already exist.

## 26. v0.5.11 menu rows / NM parity / Classic contrast

Phone parity finding:
Classic nomenclature preserves two child documents:
`dessin` PDF + `alveoles` pin/contact description HTML.

Runtime IR already kept both. The Modern renderer was flattening the second document immediately.

v0.5.11:
- row 1: Схеми + Розʼєм;
- row 2: Положення на авто;
- row 3: Документація;
- NM variant opens two explicit document actions:
  - Схема розʼєму;
  - Опис контактів;
- contact rows are static text, not button cards;
- Classic top-level INDEX gets a white WebView canvas so transparent pin-description frames remain readable;
- versionName 0.5.11 / versionCode 27;
- no point 9 required.

Findings are tracked in `docs/assistant-kit/OPEN_FINDINGS.md`.

## 27. v0.5.11 CI PASS

Merged source:
`93c722270a5670eeab6f8343d48ab51cb067f19b`

Main CI:
- Tests `36041709175` — PASS;
- Android Debug `36041708827` — PASS;
- artifact id `10826734430`;
- APK SHA-256 `a172ad458e5c91d3e981d39da3ae86a7a0c11a4e062e488d3b509b9c4417eb4f`.

Phone gate:
Menu 5 → 8; no point 9.
Validate stable menu rows, separate NM drawing/contact documents, and Classic pin-description contrast.

## 28. v0.5.12 density / contact table / PDF toolbar

Phone UI requirements implemented:
- top title one line;
- compact Розділи/Classic actions;
- CMP heading replaced by section code/title;
- menu density reduced while keeping Schematics+Connector / Position / Documentation grouping;
- contact/pin table rows stay on one line and pan horizontally;
- PDF toolbar follows zoomed document width;
- PDF plus button moved immediately after zoom dropdown;
- version 0.5.12 / code 28;
- APK-only, no dataset regeneration.

Phone test doc:
`docs/v.0.5.12/qa/PHONE_TEST.md`.

## 29. v0.5.12 CI PASS

Merged source:
`fa6963d05412def8ff917184e9a30537e06a2300`

Main CI:
- Tests `36051860932` — PASS;
- Android Debug `36051860854` — PASS;
- artifact id `10831137351`;
- APK SHA-256 `f892238a66d5aab02392fc3036d7e7ba212072ad6e93e7ac8e91f25745cf8c33`.

Phone gate:
Menu 5 → 8; no point 9.
Validate compact Modern layout, horizontal contact rows, and PDF toolbar expansion at 150/200%.

## 30. Classic pin-description phone PASS

Real phone screenshot confirms the Classic `alveoles` / connector pin description is readable after the white-canvas fix.

BUG-002: PHONE PASS.

Use this Classic compact table as the visual/parity reference for Modern `Опис контактів`: one row per line, compact spacing, visible columns, horizontal access for long rows.

## 31. v0.5.13 native section tiles

Implemented requested section-screen redesign:
- top global row: back + Home + Search + Settings;
- context row: section code only + Modern/Classic segmented switch;
- primary tiles always visible (Scheme / Connector / Position / Documentation);
- absent action = disabled tile, not disappearing layout;
- select/group blocks rendered as rounded tiles;
- section search can be opened directly from NativeSectionActivity;
- version 0.5.13 / code 29;
- APK-only.

BUG-004 catalog completeness is intentionally not mixed into this UI release.

## 32. v0.5.13 CI PASS

Merged source:
`c6ab899042f016dd0e8bf6d7f7e463de8d2365e4`

Main CI:
- Tests `36057491536` — PASS;
- Android Debug `36057491412` — PASS;
- artifact id `10832474678`;
- APK SHA-256 `1f8adeca1a1c0695c337caafa624d7913b6355db393815fa07e00edb9afbfc4d`.

Next:
Menu 5 → 8; no point 9.
Phone-test global section chrome, persistent disabled menu tiles, and tiled selector blocks.

## 33. v0.5.13 phone layout PASS / new converter evidence

Phone screenshots confirm:
- 101/107 persistent primary tiles behave correctly;
- 107 Connector remains visible and disabled;
- grouped Scheme tiles are accepted visually;
- global navigation + Modern/Classic switch is a clear improvement.

New converter evidence:
catalog identifiers include numeric, R-prefixed, and alphabetic forms:
`101, R15, R325, MAH, MYH, MA, MB, ME, MG, MH, ML, MQ, MT, MW, NA, NC, NH, NT, NU`.

Do not model these as integers or a 3-digit regex. Preserve the source identifier as a string.

New BUG-005:
Classic abbreviation documents are semantic two-column glossaries. Modern generic one-line structured-table rendering is wrong for them. Add a dedicated glossary renderer while keeping connector pin tables on the separate one-line/horizontal contract.

## 34. v0.5.14 unified tables / PDF export

Implemented latest Classic-parity request:
- one native table renderer for abbreviations and connector pin data;
- cell borders and column-aware widths;
- long descriptions wrap within their right-hand cell;
- connector structured document header uses section code/title + criteria;
- legacy CMP heading hidden;
- connector/location metadata grouped separately;
- table can be saved as a real paginated PDF through Android document picker;
- version 0.5.14 / code 30;
- APK-only.

BUG-004 remains the next converter-level task.

## 35. v0.5.14 CI PASS

Merged source:
`66702baec93d3747e8d2b70808177a3746b82ca2`

Main CI:
- Tests `36065777800` — PASS;
- Android Debug `36065777851` — PASS;
- artifact id `10836740287`;
- APK SHA-256 `1c3177fed7ce7e5c086a2b9f39cb2df78c19787959dea06fc21a559764308788`.

Next:
Menu 5 → 8; no point 9.
Phone-test unified abbreviations/pin tables, connector header hierarchy, and save-table-as-PDF.

## 36. v0.5.14 phone evidence / v0.5.15 implementation

Phone evidence:
- generated table PDF saved and opened successfully;
- Cyrillic is readable;
- native glossary table works but fixed column proportions waste width;
- multi-line description rows need full-height centered short cells;
- connector menu needs combined `Схема + піни розʼєма`;
- generic `Renault_101_table.pdf` filename is rejected as the long-term naming contract.

v0.5.15 implementation:
- shared content-aware table fractions;
- portrait PDF for <=2 columns, landscape for wider tables;
- vertically centered cells/text blocks for wrapped rows;
- combined connector composite action;
- source-aware PDF names such as `101_1(pins).pdf`;
- version 0.5.15 / code 31;
- APK-only.

## 37. v0.5.15 CI PASS

Merged source:
`cd4d0cbcc78d2623488f8d50367c91b94c0e041b`

Main CI:
- Tests `36072358387` — PASS;
- Android Debug `36072358369` — PASS;
- artifact id `10838243576`;
- APK SHA-256 `7e2ca52f7e37cfa5844133e331fec0f58561f33538146b45e4b9096b423841b9`;
- artifact ZIP SHA-256 `419916d1914c4b62d35ccd6e949d319baae086c283d4adb5a17f1f88f788fe9b`.

Phone gate:
Menu 5 → 8; no point 9.
Validate adaptive table widths, centered wrapped rows, combined connector view and source-aware PDF filename.

