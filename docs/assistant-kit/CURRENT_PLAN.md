# Renault Docs — CURRENT PLAN

Останнє оновлення: 2026-09-29.

Це коротка жива точка відновлення. Історія рішень і старих інцидентів лишається в `CURRENT_HANDOFF.md` та `docs/assistant-kit/PROJECT_LEDGER.md`.

## Робочі правила

- Телефонний workflow — через `reno-docs` / меню Termux; ручні Git/gh/bash команди лише для аварійної діагностики або відсутньої функції меню.
- Menu item 19 — `Статус проєкту / build` — є read-only self-check: version/branch/commit, CURRENT_PLAN status/next step, release QA/delivery, latest Android Debug and Tests runs.
- Після кожного завершеного кроку одразу оновлювати цей файл і довготривалий handoff/ledger.
- Не змінювати phone-accepted runtime v0.5.51 під час closeout. Новий UX — окремим follow-up після merge.
- Не видаляти legacy `*_android` без read-only provenance/reference audit.

## v0.5.51 — Kotlin-native raw Renault → .rdpkg

Статус: **PHONE PASS + MERGED + DISTRIBUTION VERIFIED — CLOSED 2026-09-29**.

Reference:
`NT8340A · 2006-04-18`.

Accepted pipeline:

```text
raw Renault SAF folder
→ app-private normalized staging
→ Kotlin Modern / Section IR / Runtime IR
→ Fast Pack
→ manifests
→ streamed .rdpkg
→ validation/import
→ project upsert
→ native reopen
```

### Accepted phone evidence

- [x] Android/Kotlin raw-folder conversion only; Python/Termux не входять у production flow.
- [x] Source count: `7653` files.
- [x] Runtime IR: `347` native sections.
- [x] Fast Pack: `6138` files.
- [x] Outer package: `8012` files.
- [x] Deterministic package SHA-256 across successful reruns:
  `7d6cc758b329adcd1ef46680de4d9c6815ff48b053f4e3550b06dd92b229b77e`.
- [x] Automatic `RdpkgImporter.install()` validation/import PASS.
- [x] Existing NT8340A upserted without duplicate; project stayed at 2 volumes.
- [x] Reopen: `NT8340A · 2006-04-18`.
- [x] Modern: `347 · native`.
- [x] No compatibility fallback.
- [x] Classic remains explicit alternate mode.
- [x] Post-completion stale Activity-result replay regression fixed and phone-verified.
- [x] Active PREPARING lifecycle PASS: same copy run progressed `500/7653 → 1400/7653 → 2300/7653` through rotation/background/external-app handoffs.
- [x] PREPARING Cancel PASS: source unchanged; private staging cleaned.
- [x] Current Kotlin flow created no new public `*_android` intermediate.

### Accepted candidate / CI

Phone-accepted runtime source:
`6b334eafb1fbf9dcca591fa4d1b619885f32ea4b`.

CI for that candidate:
- Tests `36370544159` — PASS;
- Android Debug APK `36370544247` — PASS;
- artifact `Renault-Docs-v0.5.51-Debug`, id `10948508448`;
- APK SHA-256 `7b51c30cdd0be50de48ff8ddc21c6ee85f33c00eaa9c93adacf22fec4ef5ab48`;
- signer cert SHA-256 `dd588fbb3093a047f81c24397fc6d2ab7f5a32040425a21dd24506cce59a9802`.

## Closeout public PR #1

- [x] PHONE PASS recorded in release docs, handoff and ledger.
- [x] Reconcile PR branch with current `main` versions of shared Termux/signing infrastructure.
- [x] Resolve merge conflicts; PR is mergeable again.
- [x] Condense active plan and phone QA so stale PENDING chronology is not the primary source of truth.
- [x] Public Tests CI on final PR head — PASS.
- [x] Public Android Debug CI on final PR head — PASS.
- [x] Public PR #1 marked ready and squash-merged.
- [x] Merged SHA: `921e87f728a222e8f388a01ad989b082a7bd8894`.
- [x] Merge state recorded in release metadata.
- [x] Restore accepted development signer in public GitHub Actions Secrets.
- [x] Verify public-main signer continuity: SHA-1 `4102350e2787fd538bbf58a219293a132235e618`, accepted cert SHA-256 `dd588fbb3093a047f81c24397fc6d2ab7f5a32040425a21dd24506cce59a9802`.
- [x] Exact public-main APK SHA-256 `7b51c30cdd0be50de48ff8ddc21c6ee85f33c00eaa9c93adacf22fec4ef5ab48` matches the previously phone-accepted v0.5.51 APK byte-for-byte.
- [x] Final in-place install check PASS: exact public-main v0.5.51 installed over existing Renault Docs without uninstall/data reset; reopen kept Megane II with `Томів: 2`.

## Follow-up після merge

These are accepted work items, but they must not reopen the already accepted v0.5.51 runtime before merge:

1. **Terminal status dismiss UX**
   - right-side `×` for COMPLETE / CANCELLED / FAILED;
   - never during PREPARING / IMPORTING;
   - hides presentation state only;
   - does not delete package, volume, source or project data;
   - dismissal survives Activity recreation.

2. **Ukrainian wording**
   - `Імпортую .rdpkg… 1 файлів` → `Імпортую .rdpkg… 1 файл`.

3. **Legacy storage audit**
   Existing historical folders include:
   - `laguna 2 2001-2006_android`;
   - `Megane II_android`;
   - `Megane II_NT8342A_android`.

   Add a read-only Termux-menu audit: path, size, modified date, likely role/reference, then classify `KEEP / LEGACY / SAFE TO REMOVE`. Delete nothing automatically.

## Legacy `*_android` storage audit

Status: **CLOSED — 2026-09-30**.

Final real-phone result:
- `Megane II_android` migrated/quarantined/reopen-tested and permanently removed;
- `Megane II_NT8342A_android` migrated/quarantined/reopen-tested and permanently removed;
- both NT8340A and NT8342A remained functional while both legacy folders were absent;
- final read-only audit shows only `laguna 2 2001-2006_android`;
- Laguna remains **KEEP** as the configured active `build_root`;
- final audit summary: `KEEP 1 / LEGACY 0 / SAFE TO REMOVE 0`;
- approximately 805 MB of obsolete Megane prepared data was removed.

## Dataset link integrity gate

Status: **REAL-PHONE PASS — 264779 refs / 0 missing / 10↔10 volumes**.

Scope:
- [x] read-only scanner for prepared dataset HTML/CSS local references;
- [x] checks HTML `href/src/background/action/data/poster`;
- [x] checks CSS `url(...)` and quoted `@import`;
- [x] strips query/fragment before filesystem resolution;
- [x] ignores external/data/javascript/mail/tel references;
- [x] validates manifest path fields such as entrypoint/modern/runtime/fast-pack references;
- [x] JSON report with source/reference/resolved path;
- [x] nonzero exit code when missing local targets are found;
- [x] Termux Menu item `22 — Перевірити посилання dataset (read-only)`;
- [x] unit tests for valid, missing, external, fragment/query and outside-root references;
- [x] checker does not mutate the dataset.

## Laguna DATA-001 repair

Status: **CLOSED / REAL-PHONE PASS — 2026-09-30**.

Final evidence:
- restored seven prepared Classic volume folders from `_volumes_hold`;
- source volumes: 10;
- build volumes: 10;
- missing volumes: 0;
- extra volumes: 0;
- HTML/CSS files scanned: 48307;
- local references checked: 264779;
- missing local targets: 0;
- volume parity: PASS;
- dataset link integrity: PASS.

This confirms there is no remaining link breakage from the user's earlier language cleanup.

## Laguna II batch .rdpkg preparation

Status: **PHONE BATCH BUILD PASS — 10/10 PACKAGES READY**.

Target:
- full Laguna dataset contains 10 validated volumes;
- package model remains `1 volume = 1 .rdpkg`;
- do not create one monolithic 10-volume package.

Implementation:
- existing menu item 15 remains the package entrypoint;
- when a dataset has multiple volumes, it now offers `A — Усі томи окремими .rdpkg`;
- batch iterates every discovered packageable volume;
- each volume uses the existing single-volume `build_rdpkg()` pipeline and its validation;
- output remains under `Documents/Renault/packages/rdpkg`;
- batch writes one JSON summary containing package paths, identities, SHA-256, byte sizes and payload file counts;
- single-volume selection remains available unchanged.

## v0.5.53 — Modern section natural display order

Status: **MERGED / PHONE PASS — 2026-09-30**.

Real-phone finding:
- Laguna II `NT8183A · 2001-01-22` imports and opens as `383 · native`;
- Modern list currently exposes preserved Classic source order such as `R70 → 1013 → 338 → 321 → 853`, which is hard to scan.

Implementation:
- Runtime/converter order remains source-defined and opaque-ID safe;
- only `ModernVolumeActivity` presentation is sorted;
- stable natural comparison handles numeric runs and suffix variants;
- display group priority is numeric-leading → `R...` connectors → other alphabetic IDs;
- duplicates preserve original relative order;
- search uses the same display order;
- version bumped to `0.5.53` / code `69`;
- existing `.rdpkg` files do not require regeneration.

## Laguna II `.rdpkg` phone validation

Status: **EDGE PACKAGE PHONE PASS**.

Validated on phone:
- `NT8183A · 2001-01-22` — import/open/native sections PASS;
- `NT8328A · 2006-05-09` — import/open/representative sections PASS.

This covers the oldest and newest Laguna II package formats from the 10-volume batch.
No package regeneration is required.

## Laguna II full package import

Status: **USER REPORTS REMAINING 8 IMPORTED / FINAL 10-VOLUME SCREEN CHECK PENDING**.

Already phone-validated:
- NT8183A early edge PASS;
- NT8328A late edge PASS;
- user then imported the remaining Laguna II packages.

Final project-level count/dedup sanity is included in the v0.5.54 phone gate.

## v0.5.54 — UI / Help / lifecycle audit

Status: **IMPLEMENTED / CI PASS / PHONE TEST PENDING**.

Implemented:
- Home `Додати том → До проєкту`;
- empty project `Порожній · додай том`;
- Project one `Додати` tile with `Авто` / `Вручну`, with subtitles `.rdpkg · один том` / `Папка / SAF`;
- secondary action subtitles standardized at 11sp across Home/Project action tiles;
- Settings secondary text/value/button hierarchy standardized (12sp / 13sp / 12sp), with compact labels/warnings at 11sp; Backup buttons also use Renault Docs themed borders instead of default gray Button styling;
- shared rotation-safe Help controller with Renault Docs dark styling;
- Help on Home / Project / Add / raw / Converter / Modern volume / Native section / Volume documentation;
- CreateProject typed name survives rotation;
- Project pending manual mode survives recreation;
- Project dialogs restore after rotation without automatically exporting/removing/adding;
- full audit in `docs/v.0.5.54/UX_AUDIT.md`.

Audit follow-up:
- `RISK-LIFE-001`: direct .rdpkg import worker remains Activity-owned.

## Session checkpoint — 2026-09-30 06:42 +03:00

Status: **STOPPED FOR THE NIGHT / SAFE CONTINUATION POINT**.

v0.5.54 implementation is complete enough for phone QA:
- PR #13 open and mergeable;
- current PR head before this docs-only checkpoint: `5660b6f29f24d91c62abd16d1824a96b6b121c36`;
- Tests `36664899562` — PASS;
- Android PR Check `36664899564` — PASS;
- no merge performed;
- no phone installation/acceptance performed yet.

Tomorrow continue from exactly this gate:

1. Renault Menu `5 — Оновити проєкт з GitHub`;
2. `16 — Тестовий candidate PR`;
3. select PR #13 and install v0.5.54 candidate over the current app;
4. first visual gate:
   - Home: `Додати том` / `До проєкту`;
   - empty project: `Порожній · додай том`;
   - Laguna II: `Томів: 10`;
   - Project add area: one `Додати` tile with `Авто` / `Вручну`;
5. then rotation/lifecycle gate for Help and Project dialogs;
6. only after phone PASS consider merging PR #13.

Known follow-up remains open:
`RISK-LIFE-001` — direct .rdpkg install worker is Activity-owned and is not part of tonight's phone gate.

## v0.5.54 final UI consolidation

Status: **IMPLEMENTED / CI PENDING AFTER FULL DIALOG AUDIT**.

Final consolidation before phone acceptance:
- audited all 9 app-owned AlertDialogs;
- all use shared `DialogUi` with explicit roles;
- Settings chooser dialogs no longer use per-dialog ad-hoc styling;
- Help / Settings / Project dialogs share one visual contract;
- external Android SAF/DocumentsUI remains OS-owned and cannot be themed by Renault Docs;
- Home tool cards replace the folder word with a dedicated folder icon to prevent wrapping and align card height.

## v0.5.54 final visual hierarchy pass

Status: **IMPLEMENTED / CI PENDING**.

Implemented from final phone review:
- Home `Додати` is one parent tile with `Новий том` + `Новий проєкт`;
- Home `Інструменти` is one parent tile with `Конвертер` + `Legacy`;
- tool folder pictogram enlarged to 38dp and positioned on the right side of each child action;
- shared `Ui.actionButton` replaces remaining gray app-owned primary/Classic actions on the audited screens;
- shared `Ui.entityTitle` gives project/volume/dataset/section identities a second readable color;
- Modern dataset: active Modern state highlighted, Classic themed, search field accent-outlined, volume titles highlighted;
- Modern volume: Classic themed, search field accent-outlined, volume/section identity hierarchy improved;
- Create Project: input and primary action themed;
- Project / Project chooser / Documentation / Native section identity titles aligned with the same hierarchy.

## v0.5.54 header/mode polish

Status: **IMPLEMENTED / CI PENDING**.

Latest phone-review fixes:
- Home tool folder icon moved to the lower-right corner of each child action;
- tool text gets the full card width; `Конвертер` is forced to one line;
- shared `Ui.modeButton` now owns Classic/Modern styling;
- Modern dataset, Modern volume, Native section and Classic Viewer all use the same mode-control visual language;
- Viewer toolbar titles use the shared entity-title color and smaller 16sp size for long titles;
- legacy `Як користуватись` pages now show an app-owned relevance note: their volume count describes only the opened Classic dataset, not the current Renault Docs project.

## v0.5.54 phone-findings follow-up

Status: **IMPLEMENTED / CI PENDING**.

Latest phone screenshots exposed two concrete defects:
1. Converter still used default gray Android buttons on its folder/actions screen.
2. A stale entry under `Старі бібліотеки` could still navigate into Modern and fail with `Не вдалося прочитати renault-dataset.json` after its old public folder had been moved/removed.

Fixes:
- Converter source/destination, validate, start, cancel, register and clear actions now use the Renault Docs button visual system.
- Old standalone library records are revalidated with `DatasetReader` before any Modern/Classic navigation.
- If the saved SAF target is gone/stale, navigation is blocked and Home explains that the folder must be re-added via Legacy.
- Modern also replaces the raw manifest error with the same user-facing stale-library explanation when entered through an old/direct path.

## Поточний наступний крок

**Wait for CI on the latest PR #13 head. If green, install one candidate and continue the full phone regression from Converter + stale Legacy library first, then the remaining UI/lifecycle checklist.**


## NEW CHAT HANDOFF — 2026-10-01

Status: **v0.5.54 / PR #13 / UI polish + stale legacy guard / NOT MERGED**.

Current branch:
- `feat/v0.5.54-ux-help-audit`

Checkpoint before switching chats:
- latest PR #13 head observed before handoff: `682d2c437213019ae69974d1617699d5e428b813`;
- Android PR Check `36790097812` = **PASS**;
- Tests `36790097841` = **FAIL** with exactly one known contract-test failure;
- failing test: `tests/test_v0513_modern_section_tiles_contract.py::test_global_nav_mode_switch_persistent_tiles_and_content_cards`;
- reason: old assertion expects literal `label = "Modern"`, while NativeSection now uses the shared formatted `Ui.modeButton(... label = "Modern" ...)` layout. This is a test-contract update, not a phone/runtime failure.

Latest implemented phone findings before handoff:
- Converter default gray Android buttons were replaced with Renault Docs action styling;
- old `Старі бібліотеки` SAF records are revalidated with `DatasetReader` before opening;
- stale/moved/deleted old dataset no longer navigates into a broken Modern screen; user gets a re-add-via-Legacy explanation;
- Modern direct stale-path error no longer exposes raw `renault-dataset.json` failure;
- folder pictograms on Home Tools are bottom-right overlays;
- Classic/Modern controls use shared `Ui.modeButton` styling across ModernDataset, ModernVolume, NativeSection, and Viewer;
- legacy `Як користуватись` page gets a scope note explaining that its volume count belongs to the opened Classic dataset, not the current Renault Docs project.

First action in the new chat:
1. update the one stale v0.5.13 contract assertion for the new shared `Ui.modeButton` formatting;
2. rerun CI and require **Tests PASS + Android PR Check PASS**;
3. build/install one fresh PR #13 candidate;
4. resume phone regression starting with **Converter** and the stale **Megane II / Старі бібліотеки** case;
5. then continue the full v0.5.54 visual/lifecycle checklist;
6. do **not merge PR #13** until the full phone gate passes.


## CI recovery checkpoint — 2026-10-01

Status: **TEST CONTRACT FIXED / CI GREEN / FRESH PHONE CANDIDATE NEXT**.

Completed after the new-chat handoff:
- stale contract test updated for shared whitespace-formatted `Ui.modeButton` calls;
- runtime/app code was not changed by this fix;
- test-only commit: `c3073d44f381c8160829abdc6cee707353711b32`;
- Tests `36794236208` — **PASS**;
- Android PR Check `36794236168` — **PASS**.

Next:
1. build/install exactly one fresh PR #13 candidate from the latest branch head;
2. start phone regression with Converter button styling and stale `Старі бібліотеки` / Megane II handling;
3. continue the full v0.5.54 visual + lifecycle checklist;
4. keep PR #13 unmerged until the full phone gate passes.


## v0.5.54 Classic help-page mobile layout

Status: **IMPLEMENTED / CI GREEN / PHONE CHECK NEXT**.

Phone screenshot finding:
- legacy `Як користуватись` / `README_UA.html` is too tall and text-heavy on a phone;
- page title and headings consume too much vertical space;
- long file descriptions are hard to scan;
- the Viewer toolbar can waste two lines on the generic `Як користуватися — …` title.

Implemented:
- Viewer shows the actual dataset identity for legacy info pages instead of the generic help-page title;
- a trailing year range is normalized to a second line, e.g. `Laguna II` / `2001–2006`;
- Classic scope note is shorter;
- existing imported `README_UA.html` pages receive an idempotent mobile layout at runtime, so Laguna II packages do not need rebuilding;
- `Відкрити каталог документації` becomes a compact `Відкрити каталог` action;
- `Що знаходиться у папці` becomes `Основні файли`;
- the old long file paragraph becomes a two-column `Файл | Призначення` table with wrapping paths;
- large help headings are reduced;
- the Android explanatory paragraph is shortened;
- Python and Android package writers generate the same compact visual family for future packages.

Code commit:
`24a99325157b1871b9e3d8dd53e7253f046bc237`.

CI:
- Tests `36797661478` — **PASS**;
- Android PR Check `36797661499` — **PASS**.

Phone gate:
open Laguna II → Classic → `Як користуватись` and verify the compact toolbar title, table readability, action row, and absence of oversized headings. No `.rdpkg` regeneration is required.


## Classic help phone correction — 2026-10-01

Status: **FIRST ATTEMPT PHONE FAIL → RESPONSE-LEVEL FIX IMPLEMENTED / CI GREEN / RECHECK REQUIRED**.

Phone evidence after the first compact-help candidate:
- Viewer toolbar changed, proving the new app code was installed;
- the actual `README_UA.html` body stayed old: oversized H1/H2, old `З чого почати`, old file paragraph, old `Android` copy;
- therefore the post-load JavaScript DOM adaptation did not satisfy the phone gate.

Correction:
- removed the post-load `applyReadmeMobileLayout()` approach;
- `SafDatasetWebViewClient` now intercepts `_renault/README_UA.html` **before** the generic Fast Pack response and serves a deterministic compact HTML response;
- old README content is used only to recover dataset identity and dataset-local volume count;
- the compact response contains the intended mobile layout and the `Файл | Призначення` table directly;
- Viewer toolbar identity is parsed from the help page title, drops the redundant `Renault` prefix and splits a trailing year range, targeting `Laguna II` / `2001–2006`;
- old cached README responses are bypassed with the versioned query `rdhelp=compact-v2`, without clearing the rest of the WebView/documentation cache.

Code:
- response-level replacement: `874648c5ea851926f5f5443e15688948a6c87ac8`;
- cache-bust: `d36dd3b8343d514733ec88b14235c7f38a956968`.

CI on `d36dd3b...`:
- Tests `36811029640` — **PASS**;
- Android PR Check `36811029609` — **PASS**.

Phone recheck:
install one fresh PR #13 candidate and reopen the same Laguna II Classic `Як користуватись` page. The old giant layout must not appear.


## SLEEP CHECKPOINT — 2026-10-01 · Laguna integrity restored

Status: **DATASET 10/10 PASS / PACKAGE METADATA 10/10 PASS / PR #13 OPEN / PHONE REGRESSION CONTINUES LATER**.

Real-phone item 22 result after rebuilding package metadata:
- scanned HTML/CSS files: `48307`;
- checked local references: `264793`;
- missing local targets: `0`;
- skipped outside-root refs: `0`;
- live build volumes: `10`;
- `renault-dataset.json` volumes: `10` — metadata parity PASS;
- `_renault/volumes.json` volumes: `10` — metadata parity PASS;
- `_renault/modern-index.json` volumes: `10` — metadata parity PASS;
- source volumes: `10`;
- build volumes: `10`;
- missing/extra volumes: `0 / 0`;
- overall dataset link/integrity check: **PASS**;
- report: `/storage/emulated/0/Documents/Renault/reports/dataset-links-20261001-074659.json`.

Checker hardening:
- commit `82491c108f5a75352edd6466d27c0f9f0b475114`;
- item 22 now fails when live build volumes disagree with manifest / volumes.json / modern-index.json;
- Tests `36813567420` PASS;
- Android PR Check `36813567450` PASS.

Important source separation:
- `Старі бібліотеки` / legacy Laguna II reads directly from the SAF-linked public `laguna 2 2001-2006_android` dataset;
- Project Laguna II volumes imported from `.rdpkg` are unpacked into app-private `noBackupFilesDir/rdpkg/<packageId>`;
- Classic opened from a Project volume reads that installed private package copy, not the old public `*_android` folder;
- public `Documents/Renault/packages/rdpkg` contains source/install archives; presence there does not by itself prove every package is installed in the Project.

Verified public Laguna II package inventory exists for all 10 volumes:
`NT8183A, NT8218A, NT8236A, NT8240A, NT8254A, NT8282A, NT8283A, NT8307A, NT8327A, NT8328A`.

Resume after sleep:
1. reopen the old `Старі бібліотеки → Renault Laguna II 2001–2006` once, return Home and verify its stored card refreshes from `Томів: 3` to `Томів: 10`;
2. open the normal Project `Laguna II` and count installed Project volumes separately;
3. if Project already has 10 — continue regression; if it has fewer, identify missing installed `.rdpkg` packages before doing any unrelated dataset rebuild;
4. then resume v0.5.54 phone regression at Converter / stale Legacy / remaining Help+rotation gates;
5. PR #13 stays **NOT MERGED** until full phone PASS.


## Legacy Laguna phone refresh — PASS 2026-10-01

Real-device screenshots confirm:
- legacy `Renault Laguna II 2001–2006` opens with `томів: 10`;
- Home legacy card refreshes to `Томів: 10 · відкриття: Classic`;
- public `*_android` contains all 10 physical volume folders;
- public `packages/rdpkg` contains all 10 Laguna II archives.

This closes the stale 3-volume legacy-card symptom.

Next:
1. open normal Project `Laguna II`;
2. count installed Project volumes separately;
3. if fewer than 10, identify missing installed packages;
4. then continue Converter + lifecycle regression.


## Project Laguna II installed-volume phone check — PASS 2026-10-01

Real-device screenshot confirms the normal Project `Laguna II` reports `томів: 10`.

Conclusion:
- all 10 Laguna II packages are already installed into the Project;
- no missing `.rdpkg` import remains;
- Legacy 10-volume state and Project 10-volume state are both independently verified.

Next regression block:
1. Converter visual/buttons;
2. stale Legacy guard;
3. remaining Help/dialog rotation lifecycle.


## v0.5.56 — shared operation status UI / phone evidence

Status: **PHONE QA IN PROGRESS — 2026-10-03**.

Candidate under test:
- branch: `feat/v0.5.56-operation-status-ui`;
- phone-installed exact source: `9de7ebc761f0cadf268870632c3c6eeb95d93422`;
- Tests `37120745185` — PASS;
- Android PR Check `37120745176` — PASS;
- Android Debug APK `37126617668` — PASS;
- artifact: `Renault-Docs-v0.5.56-Debug`.

Phone evidence:
- [x] install/update of the exact-head v0.5.56 candidate — PASS;
- [x] Home → Project `Kangoo II` opens without crash — PASS.
  - This specifically verifies the `projectScroll` initialization regression fix.
- [x] Native raw → .rdpkg UI transition on an intentionally invalid source — PHONE PASS for status-surface behavior only; package creation itself FAILED as expected because the selected folder was parent/mixed (2026-10-03).
- [x] Expected validation failure for parent/mixed source replaces running state; no orphan running card remains — UI/LIFECYCLE PHONE PASS 2026-10-03.
- [x] FAILED terminal `×` dismisses presentation state and returns to the empty-project screen — PHONE PASS 2026-10-03.
- [ ] PREPARING Cancel reaches CANCELLED and does not remain stuck.
- [x] Valid inner raw source creates/imports a real native .rdpkg — PHONE PASS 2026-10-03: NT8486 · 2009-08-31 · 276 native; Kangoo II 0 → 1 volume; terminal COMPLETE shown on the same status card with SHA-256.
- [x] COMPLETE rotation + landscape reachability — PHONE PASS 2026-10-03 on fixed build `1d5e76cd212bac2c1daeba7e8870a57664b26df0`: after rotation the Project screen scrolls to the same COMPLETE card; NT8486 remains exactly one volume and no rerun/duplicate import is observed.

Important earlier phone finding now fixed in code:
- an invalid/mixed raw folder could show a FAILED terminal card while the running `Створення .rdpkg` card remained orphaned;
- the v0.5.56 contract is one native-run status surface: `Running → Complete / Failed / Cancelled`.

Phone evidence for invalid/mixed source:\n- selected outer `Kangoo II` folder containing both ZIP and nested extracted volume;\n- scanner entered running state;\n- validation correctly rejected it as parent/mixed source;\n- the same status card transitioned to `Створення .rdpkg · помилка`;\n- no second/orphan running card remained.\n\nNext phone gate:\n- close the FAILED card with `×` and verify it disappears cleanly and does not return immediately.


Phone finding 2026-10-03 — COMPLETE rotation lifecycle (corrected after frame/code review):\n- NT8486 remained installed exactly once (`Kangoo II · томів: 1`);\n- no visible rerun or duplicate import occurred;\n- landscape viewport only shows the upper content through the raw-create tile; the operation status sits lower in the scroll content;\n- returning to portrait shows the COMPLETE card;\n- do not classify card persistence as PASS/FAIL until landscape is scrolled down.

- Follow-up UX audit requested from phone QA: apply/verify the same full-page vertical scrolling contract on Home, especially landscape/small-height layouts.

- [x] Home full-page landscape scrolling — PHONE PASS 2026-10-03 on v0.5.56: landscape can scroll through the project library to the bottom version label; lower project cards remain reachable.
