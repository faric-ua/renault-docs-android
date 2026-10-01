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
