## CURRENT — v0.5.90 BUILD106 MEASURED PHASE PROGRESS CANDIDATE — 2026-10-09

- After installing/testing v0.5.89, user sent real Megane II landscape screenshots for NT8275A · 2005-01-03 · 333 native, volume count 12 → 13. Running status shows ZIP extraction `Файлів: 3432/5357`, while other phases lose counters/freeze at zero; terminal SHA-256 runs off screen. User asks real phase-specific counts and green active/success, red errors.
- **Implementation on `fix/v0.5.90-phase-progress-and-status-colors`, NOT MERGED/PHONE ACCEPTED.** New v0.5.90/build106 candidate uses real green indeterminate movement for unknown denominators (no fake %), real Runtime IR section progress, real Fast Pack file counts, and lossless soft-wrap terminal SHA display. Stops raw text-only callbacks overwriting structured progress; introduces error red and cancelled amber. Converter uses same state colors.
- Preserve current native ZIP extraction progress and archive copy percentages, stable status layout and foreground/background ownership. Real counts provided when knowable. No pretending index enumeration has known total until discovered; index shard writing has explicit unmeasured busy phase. Installed archives/tomes untouched.
- NEXT: open PR; Python Tests + Android PR Check on exact head; fix any failures; only then merge and obtain stable signed main APK. Phone QA must verify no frozen zero/no stale stage and new counters; issues #100 and #102 remain open until real v0.5.89/90 phone evidence. #51/#40 separate.
- v0.5.89 original stable-signed public prerelease remains last **verified downloadable APK**, published from main run #152. Do not claim user installed v0.5.90.

Canonical: `docs/v.0.5.90/` and `docs/assistant-kit/UI_CONTRACT.md`.

---

## CURRENT — VERIFIED v0.5.89/debug RELEASE PUBLISHED / PHONE QA NEXT — 2026-10-09

- v0.5.89/build105 application code from main `bfdc3a7e74220f19dbc90eefa596c166a6432f75` contains shared progress layout PR #101 and last-ten notifications PR #103. PR Python + Android PASS; main Tests #601 PASS.
- Trusted stable-signed main **Android Debug APK run #152 / ID 37972943494 PASS**, signer certificate, zipalign, manifest and SHA-256 verified. APK SHA-256 `46a2edc531725ca142e5f2a8afd2f5ff93ddb4e3061c919ede40e769097236db`. Artifact ID 11636907894, bundle digest `sha256:d802de296b5074a878e6a8ca887729e171da58cdfb1477910c925db822b4d40d`.
- Reviewed promotion PR #105 MERGED `f49cc6a8f8ba4482798b0ecb0596514424f00531`. Verified publisher **run 37976168925 PASS**; original APK + checksum published at https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.89-debug. Public debug prerelease, no proprietary archives.
- **User phone QA remains PENDING**. #100 and #102 reopened because automatic PR-linked closure was premature. Do not claim progress/card UX or grouped notification history phone PASS. No uninstall/clear data. Previously overwritten Android notification entries cannot be recovered.
- Safe next step: user updates local Termux checkout to `main` and downloads *same exact signed code* through existing Renault menu. Install APK over existing app; verify file-count row stability, current progress vs terminal result, two sequential distinct completed tome notifications, grouping/individual tap; no duplicate conversions. Then tackle #51 archive variants and #40 background/lock QA without destructive tests.
- APK promotion done; **no further feature-code work until device regression findings are reported**.

Canonical: `docs/v.0.5.89/RELEASE_META.json`, `docs/v.0.5.89/qa/PHONE_TEST.md`, #100, #102.

---

## CURRENT — v0.5.89/build105 MERGED TO MAIN / SIGNED MAIN AND PHONE QA PENDING — 2026-10-09

- Lower progress UI PR #101 (v0.5.88/build104) passed exact-head Python Tests and Android PR Check, merged to main as `25e4c7b6a2608a788765fbd466f8ead57275ff38`.
- Bounded Android notification history PR #103 (v0.5.89/build105) passed exact-head Python Tests #37972543520 and Android PR Check #37972543480, was retargeted to main, merged as `bfdc3a7e74220f19dbc90eefa596c166a6432f75`.
- User screenshot: previous native completed notification overwritten by last `NT8339A · 2006-04-18 · 325 native`; issue #102. New candidate keeps one live notification and separately groups a bounded last ten completed tomes, including multi-volume batches and regular .rdpkg import.
- **Main stable-signed Android Debug APK run/inner APK hash NOT VERIFIED YET. Public promotion and phone acceptance PENDING.** Do not install ephemeral PR APK, make up a main Actions run ID, claim release published, or close #100/#102 until real-device QA. Past overwritten notifications are unrecoverable.
- NEXT: verify trusted main signed Android Debug APK workflow for app commit `bfdc3a7e...` (may be an ancestor if docs-only main changes). Record run ID/artifact checksum/signature. Then reviewed promotion manifest, publisher and installer over existing app. Validate stage/count stability and Android notification grouping. No user file deletion.
- #51 other archive formats and #40 extended background stress are independent open work.

Canonical docs: `docs/v.0.5.88/`, `docs/v.0.5.89/`, issues #100/#102.

---

## CURRENT — v0.5.89 BUILD105 NOTIFICATION HISTORY STACKED DEVELOPMENT — 2026-10-09

- User screenshot proves native finished `NT8339A · 2006-04-18 · 325 native` replaced prior result in Android shade. Root cause: fixed foreground ID 3702 reused for terminal notifications; analogous import ID 3703. User wants the last ~10 added tomes visible as expandable notification list.
- Open UX issue #102. New stacked branch `feat/v0.5.89-completed-notification-history` based on lower draft PR #101 (v0.5.88 progress UI). Result notifications now use 10 persisted rotating IDs + group summary, duplicate event guard, per-result PendingIntent; foreground progress stays fixed; batch results each notify independently; import included. No user files touched.
- **CI, main merge, signed APK and phone QA all PENDING.** This is not installed and cannot restore earlier overwritten notifications. Historical issue #100 progress UI checks passed on exact PR #101 head but #101 not merged/phone-accepted.
- NEXT: open stacked PR for #102, run and fix tests/Android CI, merge lower #101 only after review, then retarget this PR to main and apply verified release workflow. No phone test before signed build. Test grouped entries and real archive independence.
- #51 and #40 remain open; NT8298A v0.5.87 prior phone PASS unchanged.

Canonical: `docs/v.0.5.89/`, #102, lower PR #101.

---

## CURRENT — v0.5.88/build104 UNIFIED PROGRESS CANDIDATE — 2026-10-09

- **User explicitly prioritized implementing** stable shared progress for all operations after real v0.5.87 unpacking video. This supersedes the old #100 record-only decision (retained below as historical context).
- Feature branch `feat/v0.5.88-unified-progress-ui` based on v0.5.87 main; code candidate adds shared thin progress bar/normalization, fixed stage and file-counter slots in `OperationStatusView` used by Home/Project/Drive, and shared bar/compact status for Converter.
- Removes redundant raw/structured per-file updates during ZIP unpack, native staging and final packaging; native message and measurement are persisted atomically. Discards obsolete hidden Home bar. No changes to actual archive contents, conversion outputs, package data, Classic, deletion, sharing semantics.
- **Tests / Android CI / signed APK / device QA: PENDING.** Feature branch source changes are **not** a released or phone-tested candidate. Do not claim issue #100 closed, do not auto-merge or install on phone before exact PR checks PASS.
- Next: open/review PR, await Python Tests and Android PR Check on exact head; fix any failures, then follow regular main/signed-build process; phone QA must verify stage/counter alignment, converter, all status menus, rotation, and preservation of user projects.
- #51 remaining archive variants and #40 background stress remain open. v0.5.87 NT8298A prior PHONE PASS stands.

Canonical: `docs/v.0.5.88/` and issue #100.

---

## NT8298A real ZIP PHONE PASS / UX #100 DEFERRED / DELETE AUDIT READ-ONLY — 2026-10-09

- User supplied actual v0.5.87 archive-to-RDPKG final status and read-only native diagnostics: `COMPLETE`, project `Megane II`, `ARCHIVE_FILE`, source 72,693,952-byte NT8298A ZIP, volume `NT8298A · 2005-11-28`, **319 native sections**, package ID `megane-ii-nt8298a-2005-11-28`, resulting .rdpkg SHA256 `e7fdbea2d3363af3ea3710eda22dcee518e36d08963b3483f0610a55602f6603`, duration 119 s. **Original nested ZIP regression PHONE PASS**. Previous v0.5.86 FAIL resolved for this specific case. Post-run GUI tome count and individual section openings not separately reported.
- Source `NativeRdpkgPreparationService.processPreparedSource()` installs generated package via `RdpkgImporter.install()` and upserts project volume before setting COMPLETE. Source ZIP full SAF URI/Document ID deliberately omitted from public records.
- User reported **jumpy/live status text** (file counters alternating with plain counts/stage, layout shifting) during successful operation. New open UX issue **#100**, **record only, user requested no changes yet**. Potential dual progress callbacks and dynamic 1/2-line height; not confirmed root cause. Do not start code/CI for this finding.
- User asked whether deleting similar Megane II/Kangoo II volume data/files might remove one or two files. Read-only source audit `docs/v.0.5.87/qa/DELETE_BEHAVIOR_READONLY.md`: `Видалити з проєкту` removes association for one project-volume pair, **not original ZIP/exported RDPKG nor installed package**; `Видалити підготовлений .rdpkg` deletes only private prepared-share cache, possibly both canonical + legacy cache paths; `Clear storage/data` in Android is global for all app-private installed packages and project records. Newly generated package IDs and canonical filenames prefix project identity/model, but **actual Kangoo vs Megane physical files cannot be compared without read-only device inventory**. No delete action should be used for investigation.
- Issue **#51 remains OPEN** for untested other archive formats/batch/cancellation, although its NT8298A regression is PASS. Issue #40 background runtime QA remains OPEN. No new APK, builds, source mutations, cleanup, deletion or user file modifications in this turn. Preserve Classic and installed data.

Canonical: `docs/v.0.5.87/qa/PHONE_TEST.md`, `docs/v.0.5.87/qa/DELETE_BEHAVIOR_READONLY.md`, `docs/v.0.5.87/RELEASE_META.json` and #100.

---

## v0.5.87/build103 — verified native ZIP root recovery; final phone action required — 2026-10-09

PR #98 merged app source `937ffa06080c3dd6a63a87d4e9c5d209ff4b8755`; Python #591, Android #467, main Python #592, stable-signed APK #150 PASS. PR #99 public verified prerelease https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.87-debug, original signed APK SHA256 `60795a057ad77b96df0f914a8c9341222d9684fc0087e89d9389c558761286f5`. User previously confirmed v0.5.86 installation but original NT8298A ZIP failed: ARCHIVE_FILE, FAILED, no output, nested folder screenshot includes INDEX/COMMUN/RUS. Last-mile native now automatically resolves only one safe nested root; refuses multiple. Original ZIP still needs single phone retry. Next user action: over-install v0.5.87 via Termux 5→19→8→13 and retry same ZIP once, report result, no deletions. Do not claim #51 closed. See `docs/assistant-kit/SESSION_CHECKPOINT_2026-10-09_v0.5.87.md`.

---

## Renault Docs v0.5.86/build102 — #51 source fix published, phone QA on hold — 2026-10-09

PR #96 merged source `f145dee0c545d5ee1953a8584cebb3a60bfd5acf`; selected mixed-archive raw volume excludes nested separate candidate folders in native stager, preserving source. Python #588/Android #465/main Python #589/signed APK #149 PASS. Reviewed PR #97 published QA-pending public debug prerelease https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.86-debug, original signed APK SHA256 `72d78292c8197a026bddaba4ecd32ab5f433556bab7ca6622b9d246d109b5ec3`. #51 real mixed ZIP/7Z/RAR/cancel/lock QA OPEN, user paused manual tests; #40 runtime background acceptance OPEN. No v0.5.86 device install; previous v0.5.84 install/retention PHONE PASS. Next #30 Windows source/catalog direction. Resume `docs/assistant-kit/SESSION_CHECKPOINT_2026-10-09_v0.5.86.md`.

---

## v0.5.85/build101 — #51 archive safety source merged, signed release available — 2026-10-09

PR #94 merged app SHA `5b27f30ce92aed00e5b1e0e233d9a312a064bfb6`; core fixes root-level multi-volume chooser path, archive filename/NT identity propagation, ZIP/7Z/RAR member collision validation. PR Python #586, Android PR #464, main Tests #587 and signed debug APK #148 PASS. PR #95 public verified developer debug prerelease v0.5.85-debug published, original APK SHA256 `dde09bde484d0f3d7392505a999faa8f8036a2ce38778c654fcbeac676baacb6`: https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.85-debug. **User paused on-device testing and has not confirmed installation.** #51 OPEN for mixed-root nested isolation, actual ZIP/7Z/RAR, cancel/lock/staging. #40 stays open for real Android background lifecycle. See `docs/assistant-kit/SESSION_CHECKPOINT_2026-10-09_v0.5.85.md`.

---

## v0.5.84 — #40 background hardening signed release, device QA paused — 2026-10-09

PR #92 merged, source 4890963f0dc0a268f803913c0e3e7da7193ecd7a; 7 services audited; Python PR #584, Android PR #463, main Tests #585, signed Android APK #147 PASS. Verified public debug prerelease https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.84-debug, inner APK SHA256 90f4ee81e46340d4f6fd4e9f56632598bad68d3e52a944e9490727e53a1a7010. Issue #40 remains OPEN for platform 6h dataSync and real phone process-death/lock acceptance; user paused manual QA. No input/source archive changes or app install. Next #51 and #30. See docs/assistant-kit/SESSION_CHECKPOINT_2026-10-09_v0.5.84.md.

---

## v0.5.83/build99 — CODE MERGED / SIGNED CI PASS / PHONE QA ON HOLD — 2026-10-09

PR #89 merged (source SHA `b5f1c22d73642d7341943e4320b5fb469cbe9bec`). #79 New Volume now opens chosen Project Add expanded without automatic SAF; Auto/Manual/raw/archive remain explicit and existing direct picker works by its own intent. PR Tests #573 PASS / Android PR Check #457 PASS, main Tests #574 PASS / Android signed APK #146 PASS. Artifact ID 11582942367; digest `sha256:b65dd8100cb4412c7a79933ad2c12bc4456859f37ff1a4268298933f3e01d1ef`. No project data was modified by the navigation change. **User-requested QA pause continues**: do not request install/testing. Details `docs/assistant-kit/SESSION_CHECKPOINT_2026-10-09_v0.5.83.md`.

---

## v0.5.82 / build98 — CORE PHONE PASS / EXTENDED QA OPEN — 2026-10-08

User acceptance: «Пасс. Взагалі все чудово.» after Home Add 📌 portrait→landscape→portrait and Android system bars hide/restore test. **Core on-device scenario PASS**, see `docs/v.0.5.82/qa/PHONE_TEST_REPORT_2026-10-08.md`. Runtime source `f17b319c4e17d8f7005ae20221dd7bd642cb172b`, signed APK #145, main Tests #572 PASS. No reinstall needed. **Next**: non-destructive extended QA of Home Add contents (New Volume/Project, Ready/Drive, Tools/Legacy), independent scrolling and status, app-owned Help/dialog orientation and Viewer/Settings. Do not claim untested steps PASS or full release CLOSED.

---

## v0.5.82 / build98 — MAIN CI PASS / STABLE SIGNED APK / PHONE QA PENDING — 2026-10-08

PR #88 merged. Runtime `f17b319c4e17d8f7005ae20221dd7bd642cb172b`; Tests #572 PASS, Android APK #145 PASS; signed artifact ID 11563839781, SHA256 `94cadb5caf5b1af93a2a959e674819c31f0571b479c66f7b9a492af6ae6586be`. Version 0.5.82/build98. Global landscape Activity+AlertDialog hide system status/nav with transient swipe, portrait restore; exclude IME/SAF, retain PDF fullscreen. Home Add ported from Project fixed pinnable/collapsible panel with separate Home pref, legacy/ready/tools/new volume/new project inside; status remains accessible independent of collapse; My Renault list scroll independently. **Next: install over via Renault menu 5 → 19 → 8 → 13 and verify portrait→landscape→portrait, pin, content/accessibility/scroll and counters. No cleanup/backup/migrations.** Canonical checkpoint `docs/assistant-kit/SESSION_CHECKPOINT_2026-10-08_v0.5.82.md`, phone checklist `docs/v.0.5.82/qa/PHONE_TEST.md`. Earlier v0.5.81 Viewer modal phone QA still pending, regardless of screenshots of Modern native search.

---

## v0.5.81 / build97 — MAIN CI PASS / SIGNED APK READY / PHONE QA PENDING — 2026-10-08

PR #87 merged. Runtime source `309f068bbf92c65ca06c0cc2e38887d6a4664837`. Main Tests #568 PASS / Android Debug APK #144 PASS. Signed debug artifact ID 11560786817 sha256 `5711b9b6181edd4edaa53a575bc23fa13186a68aea925544de71b2b78b1afe79`, expiry 2026-10-11 UTC. 20 app-owned AlertDialogs now share DialogUi; archive preflight short plus `Технічні деталі` with state saved on rotation. Full audit `docs/v.0.5.81/DIALOG_AUDIT.md`. **Next: install over** via Renault Menu `5 → 19 → 8 → 13` and perform v0.5.81 phone QA; no uninstall, data clearing, archive move/delete or unnecessary conversion. Previous v0.5.80 test 1 (disabled .rdpkg), test 2 (wrong Kangoo ZIP), test 4 (rotation) PASS; test 3 current Megane NT match observed, cross-project still unverified; Cancel no-run unconfirmed. Keep #85/#82 phone gate, #83 picker history uncertainty, #81 immutability and #79 Home routing separate. Canonical checkpoint `docs/assistant-kit/SESSION_CHECKPOINT_2026-10-08_v0.5.81.md`.

---

## v0.5.80 / build96 — MAIN CI PASS / PHONE QA NEXT — 2026-10-08

Read `docs/assistant-kit/SESSION_CHECKPOINT_2026-10-08_v0.5.80.md` first. PR #86 merged. Runtime source `5a6ddc060edb4ff452eb1e3e96c1423cc042ddf9`. Main Tests #563 PASS, Android Debug APK #143 PASS; signed artifact `Renault-Docs-v0.5.80-Debug` ID `11557433214` (sha256 `0da82672e308148e5a2178f9fb147705b5ab8f2b96c39da15220e09dde6223b6`). Phone QA PENDING. Preserve source ZIP/.rdpkg and installed volumes; no migration/cleanup. Next: install via Renault Menu 5 → 19 → 8 → 13, test source guard, strong model mismatch, SAF source confirmation, global registered-project NT preview, Cancel/rotation. Issues #85/#82 candidate not closed, #83 picker history unresolved, #81 source preservation, #79 Home picker separate.

---

## PROVENANCE RESOLVED FOR CONTENT / PICKER NAVIGATION STILL OPEN — 2026-10-08

**Confirmed by actual v0.5.79/build95 phone diagnostic, after no intervening native runs:**
- Open + saved project: `Megane II / megane-ii`. Phase `COMPLETE`, source kind `ARCHIVE_FILE`.
- **Original selected source as persisted by application:** `Kangoo-II_X61_NT8486_Visu-v5.0_2009-08-31.rdpkg`, size `94965991` bytes, current name still resolves.
- **Source URI** `content://com.android.externalstorage.documents/document/primary%3ADocuments%2FRenault%2Fpackages%2Frdpkg%2FKangoo-II_X61_NT8486_Visu-v5.0_2009-08-31.rdpkg`.
- **Document ID** `primary:Documents/Renault/packages/rdpkg/Kangoo-II_X61_NT8486_Visu-v5.0_2009-08-31.rdpkg`.
- Output target tree `primary:Documents/Renault/packages/rdpkg`; result package ID `megane-ii-nt8486-2009-08-31`; result SHA256 `4ed38ab4a3ea8dcf41c24cf86d25820d820f896652062b682eab7f1ef0de53f2`; started local `2026-10-08 15:13:32`, completed `15:15:50`.
- **Conclusion:** wrong Kangoo content in the produced Megane RDPKG is explained by **existing Kangoo RDPKG as actual persisted input**. It was **not original Windows ZIP**. The user still specifically recalls navigating *Megane Sources*; the saved single URI **does not reveal picker navigation history** or whether app callback handled the expected selection. Do not assume user error or claim picker sequence is proved.
- Code: `ProjectActivity.startNativeArchiveRdpkgFlow()` uses `ACTION_OPEN_DOCUMENT`, `type="*/*"`, MIME includes `application/octet-stream`; callback accepts any returned name/URI as ARCHIVE_FILE; staging copies even unrecognized extension, `ArchiveIntake.detectFormat` detects by header ZIP magic, so ZIP-compatible `.rdpkg` can be treated as archive. Converter stamps target model `project.model` without source cross-model validation.
- **Issue #85 OPEN:** deny `.rdpkg` as archive source + show verified source/path + explicit preflight; **#82 OPEN** model consistency gate; **#83 OPEN** unknown picker navigation/selection vs persisted URI; **#81** source immutability, original Windows ZIP found separately on Drive/phone, no confirmed original deletion.
- **Next:** request user approval for a scoped safeguard release (extension/container validation + source identity preflight/model mismatch), not another live original-archive repro. Do not delete/move generated packages or original archives. Retain v0.5.79 evidence report / don't overwrite before note reviewed.
- v0.5.79 / build95 signed APK #142 CI PASS, diagnostic report successfully received; full previous v0.5.78 progress phone QA still separate/pending; #79 Home chooser later.

---

## CI verified — v0.5.79/build95 signed provenance diagnostic — 2026-10-08

- PR #84 MERGED; app source `b6fdfa1efc90f0c4db9be2fc30898ddb8dfac58a`.
- PR Tests #560 PASS / Android PR Check #448 PASS.
- Main Tests #561 PASS / Android Debug APK #142 PASS.
- Signed debug artifact `Renault-Docs-v0.5.79-Debug` ID `11553367269`, SHA256 `a0ed0807dc2b43d2eb92c83014e426bb0a8ea7c521d1cc7f079c927ceb4da695`. Artifact expected to expire 2026-10-11.
- Phone evidence gate: **PENDING**. Do not claim source URI known yet.
- Install **over** existing app using Renault Menu `5 → 19 → 8 → 13`, no uninstall, data clearing, or extra native .rdpkg preparation.
- Megane II → expand Add → «Джерело останньої .rdpkg · діагностика» → «Копіювати звіт» → paste in chat.
- Read-only diagnostic (NativeRdpkgRunStore.load & provider metadata only); issue #83 root cause UNKNOWN.
- After report compare `sourceUri` / Document ID / sourceName with actual user-selected Megane II source folder. No destructive fixes before evidence.

---

# v0.5.79 / build95 — READ-ONLY last native source diagnostics — 2026-10-08

**Status: MERGED / MAIN CI PASS / SIGNED APK READY / PHONE SOURCE REPORT PENDING.**

User confirms **NO new native .rdpkg operation since Megane-II_X61_NT8486**. Evidence in NativeRdpkgRunStore likely preserved after COMPLETE (not yet read from device). Diagnostic option in Project → expand Add → `Джерело останньої .rdpkg · діагностика` reads last persisted state via `load()`, queries only provider metadata (document name, size, Document ID), and copies source/destination/project/result/time report. **No conversion, service restart, delete, clearFinished or archive stream read.**

New investigation build is v0.5.79 / build95. Install it *over the existing app* using stable signer, never uninstall or clear data. After install open Megane II → expand Add → diagnostics → copy into chat. Do not run any additional .rdpkg creation until report collected. If stored source info absent, stop: do not reproduce with original archives.

Issue #83 provenance origin UNKNOWN; issue #82 model conflict separate, issue #81 source immutability separate, issue #79 Home picker separate. v0.5.78 phone QA still pending. No run state touched by GitHub work.

---

## URGENT: preserve original archive provenance — issue #83 — 2026-10-08

**User clarified explicitly:** problematic conversion was initiated by selecting an archive within phone's **Megane II Sources folder**, which the user asserts contains only Megane archives. Yet the generated `Megane-II_X61_NT8486_...` package displays real **Kangoo II X61 PDF content** (not merely filename). Do NOT reinterpret this as simply user selecting a Kangoo ZIP in the Kangoo folder. Root cause remains UNKNOWN.

**Key newly verified source-state fact:** `NativeRdpkgRunStore.begin()` persists `sourceUri`, `sourceName`, `projectId`, `destinationUri`; `complete()` preserves them. But next `clearFinished()` will clear the whole previous run. **Stop new native conversions, app-data resets, cleanup/uninstall until read-only last-run provenance is captured.**

**Normal path** in `ProjectActivity.handleNativeRdpkgArchiveSourceResult()`: Android SAF `data.data` becomes source URI → saved as pendingNativeSourceUri → `NativeRdpkgPreparationService` → `ArchiveIntakeStager` deletes old private staging and copies fresh input bytes via `openInputStream(sourceUri)`. Thus ordinary reuse of a stale private source copy is NOT supported by this path, but resume/batch and SAF selection/lifecycle still need audit.

**Next highest priority:** issue [#83](https://github.com/faric-ua/renault-docs-android/issues/83), read-only diagnostic exposing persisted last-run source URI/name/project/target and provider-resolvable path/size (no side effects, no action restart); compare selected Megane folder vs actual URI/file bytes. Issue #82 remains model-consistency guard, #81 source preservation. Don't claim proven cause. No runtime modifications so far.

Current app v0.5.78/build 94, CI PASS, phone QA PENDING; issue #79 Home chooser also open. **Do not overwrite last-run evidence through additional phone QA imports.**

---

## POSITIVE SOURCE FIND — NT8486 confirmed in user's connected Google Drive — 2026-10-08

- Direct folder listing (connected **Eset** account) reveals exactly one ZIP in **My Drive → Kangoo II**: `KangooII X61_NT8486_Visu v5.0_2009.08.31(RUS).zip`.
- Source file ID `1pnTYhIUPxoHk1yq83PV4pN8HHY1M-Nbs`; folder ID `1zzBcaXIzK8BFB2KbQM7v2S7SPiche6tM`.
- Source size `67981490` bytes (64.83 MiB), Drive created `2025-06-05T20:31:26.958Z`, modified `2025-06-05T20:27:04Z`.
- File link: https://drive.google.com/file/d/1pnTYhIUPxoHk1yq83PV4pN8HHY1M-Nbs/view
- Exact ZIP also appears in user's Android-wide file-manager search. **Original not proven lost**. Generic Drive search failed to index the name, but direct Kangoo II folder listing and metadata gave an unequivocal match.
- Separate issue #82: current-project model is used for output `.rdpkg` naming and dataset metadata, explaining possible Kangoo X61 source packaged as `Megane-II_X61_NT8486...` without model conflict preflight.
- Issue #81 remains open only for source-preservation regression safeguards / confirmation of local parent folder; no deletion incident proven. No files moved, deleted, copied or modified on Drive.
- Issue #79 (Home «Новий том» navigation) separate; v0.5.78 / build 94 CI PASS, phone QA pending. **No runtime code changes.**

---

## NT8486 search evidence + model mismatch — 2026-10-08

**Observed in user screenshot (file manager search `NT8486`):**
- Original `KangooII X61_NT8486_Visu v5.0_2009.08.31(RUS).zip` (64.83 MB) *is present in search results*, so global disappearance from phone is **not established**. Exact folder and hashes are unknown.
- Two `.rdpkg`s: `Kangoo-II_X61_NT8486_Visu-v5.0_2009-08-31.rdpkg` and `Megane-II_X61_NT8486_Visu-v5.0_2009-08-31.rdpkg`, both display ~90.57 MB. Displayed size alone does not prove identical bytes.
- Likely cross-model labeling pathway verified in code: `ProjectActivity` passes selected `project.model`; `NativeRdpkgPreparationService.createArchiveDestination()` constructs output name from `request.model` plus raw-root identity without explicit source vehicle-model mismatch guard; `NativeRdpkgPreparationEngine` also uses requested model for dataset metadata.
- **Issue #82 OPEN** for wrong-project source model preflight/validation. **Issue #81 OPEN** only pending actual ZIP source directory/immutability verification, not confirmed deletion. Do not delete or move originals, wrong-project packages or installed volumes.
- Next: ask whether NT8486 conversion was launched from **Megane II**; inspect safe read-only manifests if required. Avoid file cleanup until user confirms.
- v0.5.78/build 94: app source `948d5688646728d53b47327cb56f222d316b6ca5`; CI PASS, phone QA incomplete. Issue #79 separate.
- Preserve user workflow: **Sources → manual user transfer to Backup**; app must not move source files.

---

## Sources / Backup correction from two phone videos — 2026-10-08

- The user **manually** moves successfully processed original Windows ZIP archives from their source folder into Backup. Renault Docs should never do this automatically.
- Video source breadcrumb: `A26 faric → Documents → Renault → Megane II`; user calls the input location Sources. Backup is `A26 faric → Documents → Renault → Megane II → Backup`. Don't assume a literal visible folder called `Sources`.
- Previous claim of a missing file arose amid checking the wrong folder; app deletion is **not confirmed**.
- Earlier notification concerns processed `NT8486 · 2009-08-31`; exact original ZIP whereabouts in source or Backup are still unknown.
- Issue #81 updated to evidence-gathering. Confirm whether original NT8486 ZIP is truly missing from **both** folders; refrain from code changes/reimports/destructive actions until clarified.
- Main v0.5.78/build 94 remains CI PASS, phone QA PENDING; issue #79 unchanged.

---

## DATA SAFETY — Windows archive source reportedly missing — OPEN / issue #81 — 2026-10-08

- User transferred original Windows Renault archive and used native archive → .rdpkg flow; now cannot locate original at source location. Exact file name (possibly NT84…/84-96), extension and source folder/provider still require confirmation. A screenshot is not proof of deletion by the app.
- Reviewed main v0.5.78 source: `ProjectActivity` opens original archive with `ACTION_OPEN_DOCUMENT` + read flags; `ArchiveIntakeStager` reads source via `openInputStream` and copies to `noBackupFilesDir/archive-intake/`; `ArchiveIntake` extracts there.
- Cleanup normally deletes private staging and incomplete output `.rdpkg`, not original archive. Potential edge case: incomplete output cleanup uses `deleteDestination()`; audit source/output URI separation and alias/collision safety instead of assuming it can never affect source.
- **DO NOT** re-download over missing file, run cleanup, delete source folders, or test destructively with a real original. Gather precise storage path, filename, source provider and before/after evidence. Only use disposable archive for reproduction.
- **Priority:** issue #81 investigation before asking user to run more archive-import QA; keep v0.5.78 phone-QA status PENDING. No runtime fix claimed, no new APK yet.
- Existing Home chooser issue #79 remains separate. Planned Home pin/collapse/status redesign remains deferred.

**Поточний наступний крок:** з'ясувати точну папку первинного Windows-архіву (локальна пам'ять / SD / Drive / Windows / синхронізована), знайти файл read-only пошуком, перевірити SAF input/output semantics і потім обережно вирішити, чи потрібен патч захисту.

---

## MAIN CI PASS — v0.5.78 / build 94 — 2026-10-08

- PR #80 MERGED; app source `948d5688646728d53b47327cb56f222d316b6ca5`.
- PR Tests #556 PASS / Android PR Check #445 PASS.
- Main Tests #557 PASS / Android Debug APK #141 PASS.
- Signed Debug artifact `Renault-Docs-v0.5.78-Debug`, id `11548219781`, digest `sha256:3978ba9494c4860370eea4de40abbc96153a61e064e7515d76e1561123ba7d8c`.
- Next phone QA: install via Renault Menu `5 → 19 → 8 → 13`. Inspect collapsed Add + visible progress, status expand/copy, Cancel, × and orientation persistence; verify Home screen unchanged.
- User-approved separate future Home refactor: reuse volume panel's collapse/expand/pin/landscape/status pattern after volume phone acceptance.
- UX issue #79: Home New volume auto-opens .rdpkg picker due `ProjectChooserActivity` `openPicker=true`; pending separate fix.
- Do not close v0.5.78 without phone QA. Do not change code simply for documentation.

---

# v0.5.78 — Project volume progress inside fixed Add panel — 2026-10-08

Status: **IMPLEMENTED / PR CI NEXT / PHONE QA PENDING**.

- Scope only ProjectActivity (inside Megane II/volumes), not Home.
- Keep compact operation/progress/status view inside the fixed Add card, as a *sibling* of collapsed action body. Progress/Cancel/terminal × remain visible even while collapsed in landscape.
- Independent ▾/▴ to show status details and ⧉ to copy full status; detail state survives Activity rotation.
- No change to original 3D 📌, portrait pin, landscape auto-collapse, scroll list or 10dp gap.
- Home/Projects action panel to reuse accepted pattern in a later release.
- Found Home «Новий том» auto-opens ready .rdpkg via openPicker=true without choice; logged **issue #79**, separate fix later (do not bundle).
- Target v0.5.78 / build 94. Gate: PR CI → main signed APK → real-phone QA for status running/terminal/cancel/rotation.

---

## Пауза / session checkpoint — 2026-10-08

**Стан:** Renault Docs v0.5.77 / build 93 — **PHONE PASS 6/6, CLOSED**. Користувач: «Все норм». Код стабільний, нового APK не потрібно.

**Поточний наступний крок:** наступної сесії обговорити вікно/панель статусу та прогресу, попередньо переглянувши UI й запитавши уточнення. **Нічого зараз не реалізовувати.**

Канонічний докладний checkpoint: `docs/assistant-kit/SESSION_CHECKPOINT_2026-10-08_v0.5.77.md`.
App runtime: `87ab5c2dc20fa14dd7e88ac8cfb81d746c262169`; main Tests #555 PASS; Debug APK #140 PASS. Issue #68 Home-panel follow-up лишається відкритою.

---

## ACCEPTED CLOSEOUT — v0.5.77 / build 93

Phone acceptance received 2026-10-08 from user: PASS 6/6, comment «Все норм». Checked:
1. Portrait: Add expanded, pin red — PASS.
2. Landscape rotation: Add auto-collapses, pin grayscale — PASS.
3. Landscape: volume list scrolls freely — PASS.
4. Landscape: manual expand/collapse — PASS.
5. Return portrait: pinned red emoji and expanded Add return — PASS.
6. Reopen Megane II: persistent pinned state — PASS.

Scope: six explicitly reported gates only; separate unpinned rotation, repeated rotations, no auto-action and status-card behavior were not individually asserted in this six-item result.

- Release source: `87ab5c2dc20fa14dd7e88ac8cfb81d746c262169`.
- CI: Tests #555 PASS; Android Debug APK #140 PASS.
- Artifact: `Renault-Docs-v0.5.77-Debug` / `11523858486`.
- Next: status-window review (not part of this closeout).

---

# v0.5.77 — landscape Add auto-collapse — 2026-10-08

Status: **PHONE PASS 6/6 / RELEASE CLOSED — 2026-10-08**.

- v0.5.76 build 92: **PHONE PASS 7/7** (original 3D emoji, grayscale unpinned, fixed Add, 10dp gap, reopening, rotation state, Help).
- User-submitted rotation video demonstrates pinned Add fills the landscape viewport and hides the volume cards.
- v0.5.77 build 93, PR #77 MERGED, runtime source `87ab5c2dc20fa14dd7e88ac8cfb81d746c262169`.
- Tests #552 PASS / Android PR Check #442 PASS / main Tests #555 PASS / Android Debug APK #140 PASS.
- Signed Debug APK artifact `Renault-Docs-v0.5.77-Debug` id `11523858486`; digest `sha256:ea2412820e0111797db40ae42b96e40e007fa3d0071ffec96348a991a9078694`.
- Landscape: pin appears grayscale/inactive; Add collapses; volumes scroll. User may expand Add temporarily with the header/chevron.
- Portrait pin/expanded preferences persist unchanged; returning to portrait restores red 📌 and expanded Add.
- No changes to status-card visuals, original emoji or existing 10dp gap.
- Duplicate competing PR #78 closed unmerged.
- Phone gate completed: PASS 6/6; user comment «Все норм».
- Release closed for the six reported gates. Unchecked supplemental regression scenarios remain outside the six-item acceptance.
- Next UX work: review operation/status-card appearance separately; do not make further v0.5.77 runtime changes.
- Issue #68 remains open for the separate Home-panel follow-up.

---

# v0.5.76 — restore original 3D emoji push-pin — 2026-10-08

Status: **MERGED / MAIN CI PASS / PHONE QA NEXT**.

- PR #76 MERGED; main runtime `9969f38cb8f12683e9140381d4b67fcea8409daf`.
- Tests #551 PASS; Android Debug APK #139 PASS.
- Signed debug artifact: `Renault-Docs-v0.5.76-Debug`, id `11523497323`.
- SHA-256 `a5e1e94cd4d19f390d91ae08138b75ceac6b0c39a5af81a0a65a9da17a3473b8`.
- Phone gate: Renault Menu `5 → 19 → 8 → 13` → verify original colored 📌 pinned / same emoji monochrome unpinned; sticky scroll, 10dp gap, reopen, rotation, Help; status-card review deferred.

User's image confirms the original Android system emoji 📌 (red dimensional body, white needle), not a rotated vector. Restore the v0.5.73 emoji-based TextView. Active/pinned state retains original colored emoji; inactive uses a zero-saturation hardware layer paint on the same glyph. Preserve the v0.5.75 sticky Add panel, 10dp gap, pin persistence, and current status-card behavior.

Phone acceptance: inspect the actual emoji, both color states, pin persistence, scrolling, Help and rotation. Do not consider the color result accepted until real-phone evidence.

---
# v0.5.75 — sticky Add panel + diagonal pin — 2026-10-08

Status: **MERGED / MAIN CI PASS / PHONE QA NEXT**.

Phone feedback after v0.5.74:
- Add layout is generally accepted;
- user wants the **Add panel fixed at the top** while volume cards alone scroll;
- add a small visual gap before the first volume card;
- restore the previous diagonal push-pin form;
- inactive pin = light monochrome, pinned pin = red;
- status-card appearance is deferred for a later visual pass.

Target:
- v0.5.75 / build 91;
- title/count/Add remain outside the ScrollView;
- only volume cards scroll;
- 10dp gap before the volume list;
- same diagonal pin vector used for both states via tint.

Merged / CI:
- PR #74 — MERGED;
- runtime source: `b25efbee254562d7b3e9f6917772e8d2e6370eb3`;
- Tests #547 — PASS;
- Android Debug APK #138 — PASS;
- artifact: `Renault-Docs-v0.5.75-Debug`;
- artifact id: `11520145404`;
- digest: `sha256:59888f89ef694512ffa2a4895a75cb93b21c99687ccaa24270bfa9a06f948f00`.

Next:
phone QA of sticky scrolling, spacing and pin states.

---

# v0.5.74 — Add panel status/pin/chevron refinement — 2026-10-08

Status: **MERGED / MAIN CI PASS / PHONE QA NEXT**.

v0.5.73 phone visual evidence:
- collapsed Add layout: PASS;
- expanded Add layout: PASS;
- new UX finding: bare transient status text sits between Add and the volume list;
- new UX finding: emoji pin does not communicate inactive vs pinned state cleanly;
- new UX finding: expand/collapse chevron is too light;
- guidance line inside Add should be removed.

Target:
- transient status moves to a subtle warm status card at the bottom of expanded Add;
- blank status is hidden;
- guidance line is removed;
- inactive pin = neutral monochrome, pinned pin = red;
- chevron = heavier `▲ / ▼`;
- active operation/progress remains outside Add.

Target release: **v0.5.74 / build 90**.

Merged / CI:
- PR #72 — MERGED;
- runtime source: `8dc7a3d0c6d22c50df343a36456f6bd4716b3a5a`;
- Tests #542 — PASS;
- Android Debug APK #137 — PASS;
- artifact: `Renault-Docs-v0.5.74-Debug`;
- artifact id: `11518058878`;
- digest: `sha256:a391f4c16a0ce7f3c7b79a9fd9383e1e0e386f07e7d5268e1b4938eaf657f230`.

Next:
phone QA of status-card placement, pin colors, chevron weight, pin persistence and rotation.

---

# v0.5.73 — compact collapsible/pinnable Add panel — 2026-10-08

Status: **MERGED / MAIN CI PASS / PHONE QA NEXT**.

Prerequisite phone gate:
- v0.5.72 duplicate NT8266A fast-path: **PASS**;
- video evidence shows archive copy/inspection followed directly by `Том уже є`;
- no `Розпаковую ZIP...` counter appears;
- Megane II remains at 9 volumes.

Current implementation:
- issue #68;
- target v0.5.73 / build 89;
- one full-width `Додати` header;
- unpinned default collapsed;
- pin persists expanded state;
- rotation preserves transient expanded/collapsed state;
- Auto / Manual / raw / archive actions stay unchanged inside expanded body;
- volume tap/long-press guidance moved inside Add;
- operation/progress status remains outside and visible.

Merged / CI:
- PR #70 — MERGED;
- runtime source: `871d93c6f0416f9b98ea78edbab06a9ddcce8eba`;
- Tests #538 — PASS;
- Android Debug APK #136 — PASS;
- artifact: `Renault-Docs-v0.5.73-Debug`;
- artifact id: `11517210871`;
- digest: `sha256:027f0603e84868e883cc826f8706394f3c40106135bbaa87c97e32a63094f36b`.

Next:
phone QA of collapse / expand / pin / reopen / rotation / unchanged actions.

---

# v0.5.72 — archive-root duplicate fast-path — MAIN CI PASS / PHONE RE-TEST NEXT — 2026-10-07

Phone video evidence before v0.5.72:
- duplicate NT8266A flow still showed `Розпаковую ZIP… · 261 / 4378`;
- later `Розпаковую ZIP… · 894 / 4378`;
- therefore the preceding fast-path attempt was **not accepted**.

v0.5.72 fix:
- preserve raw-root hints when INDEX/ACCUEIL is directly at archive root;
- keep the bounded root INDEX identity probe result;
- allow unique NT match against installed project volumes before extraction;
- regression test covers root-level `INDEX.HTM` with NT8266A.

Merged / CI:
- runtime source: `eacbbc1f914d4f35564724501a3edb126fd71963`;
- Tests #535 — PASS;
- Android Debug APK #135 — PASS;
- artifact: `Renault-Docs-v0.5.72-Debug`;
- artifact id: `11511521715`;
- digest: `sha256:c670621c76309294d15c6a1f8335a2e5578fd8c9425314cf592d033a07378c62`.

Next phone gate:
`5 → 19 → 8 → 13` → install v0.5.72 → repeat the exact same NT8266A ZIP.

Expected:
- copy / “Перевіряю склад архіву…” may appear;
- **no `Розпаковую ZIP...` counter**;
- terminal `Том уже є`;
- Megane II stays at 9 volumes.

Next UI after this gate:
Issue #68 — compact collapsible/pinnable `Додати` panel.

---

# v0.5.71 — top-level archive raw-root fast-path fix — 2026-10-07

Status: **MERGED / MAIN CI PASS / PHONE RE-TEST NEXT**.

Phone evidence:
- v0.5.70 / build 86 is installed;
- the exact same duplicate ZIP still entered `Розпаковую ZIP...` and extracted files before reporting `Том уже є`;
- therefore v0.5.70 phone re-test = **FAIL for early duplicate fast-path**; final duplicate prevention still works.

Root cause found in code:
- archive inspection computed `leafName` with `parent.substringAfterLast('/', "")`;
- when a Renault raw root is a direct top-level folder in the archive, e.g. `NT8266A.../INDEX.HTM`, `parent` contains no `/`;
- the explicit missing-delimiter fallback `""` made `leafName` empty;
- the hint was then filtered out before duplicate matching;
- after full extraction the real folder name was available again, so the later duplicate check succeeded.

Fix target:
- preserve the whole parent string as `leafName` when no slash exists;
- add a regression test for a top-level `NT8266A.../INDEX.HTM` ZIP;
- target **v0.5.71 / build 87**.

UX note:
- the collapsible/pinnable `Додати` panel is already recorded in the plan;
- it has **not been implemented yet** because it was intentionally deferred during Archive Intake phone QA;
- keep it as the next UI follow-up after the archive duplicate path is accepted.

Implementation / CI:
- PR #65 — MERGED;
- runtime source: `131b4dcef2ed95e35198a8cb03a0ce4322fda64c`;
- Tests #532 — PASS;
- Android Debug APK #134 — PASS;
- artifact: `Renault-Docs-v0.5.71-Debug`;
- artifact id: `11510676847`;
- artifact digest: `sha256:47ff40990048cd7e275bdd6677c438996693843b409dae6b65101e3c63230e37`.

Current next step:
install v0.5.71 through Renault Menu `5 → 19 → 8 → 13` and repeat the exact same NT8266A duplicate ZIP. Expected: no `Розпаковую ZIP...` stage.

After this archive gate passes, the next planned UI follow-up is the already-recorded collapsible/pinnable `Додати` panel.

---

## NEXT UI — compact collapsible/pinnable `Додати` panel

Status: **SPEC LOCKED / IMPLEMENT AFTER CURRENT ARCHIVE FAST-PATH PHONE GATE**.

Project screen target from real-phone layout:
- replace the current tall add/create cluster with **one full-width compact header tile** named `Додати`;
- collapsed header contains:
  - `Додати` title;
  - pin control;
  - expand/collapse chevron;
  - the existing overall Help `?`;
- default unpinned presentation is compact/collapsed to reclaim vertical space;
- tapping the header/chevron expands the panel;
- expanded body contains the existing controls **without changing their workflows**:
  - `Авто` (.rdpkg · один том);
  - `Вручну` (Папка / SAF);
  - `Створити .rdpkg з raw`;
  - `Створити .rdpkg з архіву`;
- existing per-action Help `?` buttons stay with their actions inside the expanded body;
- current instruction text `Натисни на том… / Утримуй том…` moves inside this expanded/help area so it disappears when the panel is collapsed;
- pin means **keep this panel expanded persistently** across activity recreation/reopen;
- unpin returns it to normal collapsible behavior; after unpin the user can collapse it;
- rotation must preserve current expanded/collapsed state;
- persistent pin state must survive app/page reopen;
- active operation status cards remain outside the collapsed body so an in-progress import/conversion is never hidden.

Later Home follow-up:
- evaluate the same full-width collapsible + pin pattern for large Home action/project panels after the Project screen version is accepted.

This is a layout compaction only; no add/import/raw/archive semantics are to be rewritten.


# v0.5.70 — Archive duplicate identity fast-path — 2026-10-07

Status: **MERGED / MAIN CI PASS / PHONE RE-TEST NEXT**.

Trigger:
- real-phone v0.5.69 duplicate ZIP test for `NT8266A · 2004-06-28`;
- duplicate prevention was functionally correct, but the app still extracted thousands of files before proving the volume was already installed.

Goal:
- identify an unambiguous Renault `NT...` document code before full archive extraction whenever possible;
- use archive filename/path metadata first;
- for ZIP, use bounded direct reads of small index/metadata entries without materializing the whole archive;
- if exactly one installed project volume matches the detected NT code, terminate as `ALREADY_PRESENT` before extraction;
- preserve conservative fallback to full extraction when identity is ambiguous.

Release:
- target: **v0.5.70 / build 86**;
- branch: `fix/v0.5.70-archive-duplicate-fastpath`.

Acceptance:
- same NT8266A duplicate ZIP no longer reaches `Розпаковую ZIP...`;
- terminal result remains `Том уже є`;
- project count remains unchanged;
- source ZIP remains unchanged;
- non-duplicate and ambiguous archives still fall back safely.

Implementation / CI:
- PR #63 merged to main;
- main runtime source: `b9c763237b7aceede742c6aa559f53abf4c83444`;
- Tests #530 — PASS;
- Android Debug APK #133 — PASS;
- artifact: `Renault-Docs-v0.5.70-Debug`;
- artifact id: `11508378680`;
- artifact digest: `sha256:20b4a7ac2b519a6a701ba688e962a8b51238c535d486eae3621e0db566882c2b`.

Current next step:
install v0.5.70 through Renault Menu `5 → 19 → 8 → 13`, then re-run the **same NT8266A duplicate ZIP**. It must reach `Том уже є` without entering `Розпаковую ZIP...`.

---

# v0.5.69 Archive Intake — MERGED / PHONE QA NEXT — 2026-10-07

Status: **MAIN CI PASS / PHONE ACCEPTANCE PENDING**.

Authoritative code state:
- release: **v0.5.69 / build 85**;
- issue: **#51 — Archive Intake** remains open until phone PASS;
- PR: **#53 — v0.5.69 — direct ZIP / 7Z / RAR archive intake** — MERGED;
- feature head before merge: `f9f11e4a3770b4d098a134217f32c036cfe21613`;
- main merge: `53f4208b4b2e4d27e4bb55094c855eaf13fb2442`;
- PR CI: Tests #525 PASS; Android PR Check #423 PASS;
- main CI: Tests #526 PASS; Android Debug APK #132 PASS;
- artifact: `Renault-Docs-v0.5.69-Debug`, id `11489564391`;
- artifact digest: `sha256:328d897a8c484931d08c6e6fa3e00fd718034cc0c7514581a23394d9f2bdcdba`.

Implemented and merged:
- ZIP / 7Z / RAR SAF intake;
- source archive is read-only and never renamed/deleted;
- app-private staging + safe extraction containment;
- file-count / expanded-size / free-space guards;
- Renault raw-root discovery;
- pre-extraction archive listing inspection;
- exact duplicate preflight and skip;
- persisted multi-volume `WAITING_SELECTION` chooser;
- installed volumes disabled/unselected; possible duplicates warned, not silently skipped;
- one or more selected new volumes supported;
- selected volumes resume from already-extracted staging without re-extraction;
- foreground notification + partial wake lock + cancellation;
- process-redelivery/idempotent duplicate checks;
- stale partial output cleanup after process loss;
- canonical .rdpkg written to the user-selected destination;
- private staging cleanup on success/cancel/failure.

Current next step — **phone QA, starting with a real ZIP archive**:
1. phone Renault Menu: `5 → 19 → 8 → 13`;
2. install v0.5.69 over v0.5.68 without clearing app data;
3. smoke app startup/project data;
4. run `Створити .rdpkg з архіву` with a real ZIP;
5. then cover duplicate, multi-volume chooser, rotation, background/lock, cancel, source-unchanged and cleanup gates.

Do not reopen implementation work unless phone evidence finds a regression. Catalog issue #30 remains separate.


## FUTURE UX — collapsible Add panel

Do **not** implement during the current v0.5.69 Archive Intake phone QA.

Project screen follow-up:
- replace the current tall cluster of `Додати`, `Авто`, `Вручну`, `Створити .rdpkg з raw`, and `Створити .rdpkg з архіву` with one compact collapsible `Додати` panel;
- collapsed state shows only the `Додати` header plus its Help `?`;
- tapping `Додати` expands/collapses the actions;
- expanded panel offers a pin control so the user can keep it permanently expanded; pinned/collapsed preference should persist;
- the explanatory text currently shown below the Add actions (`Натисни на том...`, long-press guidance, etc.) should move into this Add/Help area so the normal project screen does not waste vertical space;
- Help `?` explains all available add/create paths without expanding the panel;
- preserve the existing actions and semantics; this is a layout/UX consolidation, not a workflow rewrite.

Home follow-up:
- evaluate the same collapsible + optional pinned-panel pattern for the large Home project/tools/action area where it reduces vertical space without hiding core navigation.

This remains backlog until v0.5.69 ZIP/duplicate/multi-volume/lifecycle phone QA is complete.


---

# Renault Docs — CURRENT PLAN

## Issue #30 — Windows-source → publish pipeline

Status: **TOOLING MERGED / MAIN TESTS PASS / LAGUNA UPLOAD PENDING — 2026-10-06**.

Merged foundation:
- PR #32 — deterministic Drive catalog publisher;
- main merge: `f1def74bc7a2c5748e4ec886bf00beb0dc23c179`;
- Tests #456 — PASS;
- final main Tests #457 — PASS;
- no Android runtime/version change.

Drive boundary:
- `MEGANE II` and external source folder `laguna2` are Windows/source archives; `laguna2` is preserved as the source-folder name, while the Renault Docs project/display identity is `Laguna II`;
- `Renault Docs Projects` is runtime-ready output only;
- raw source remains read-only and is never copied directly into Catalog.

Publisher:
- `tools/build_drive_catalog.py`;
- canonical .rdpkg metadata → size/SHA-256 → stable Catalog JSON;
- explicit Drive IDs supplied via publish-plan;
- production Catalog is not hand-edited in the normal workflow.

First publish batch:
Laguna II 10 accepted canonical packages.

Next:
1. phone repo → current `main`;
2. upload the exact 10 canonical Laguna II .rdpkg files from `Documents/Renault/packages/rdpkg` into `Renault Docs Projects`;
3. capture Drive IDs;
4. generate catalog v3 and verify hashes;
5. phone-smoke NT8183A + NT8328A through Catalog.


Останнє оновлення: 2026-10-06.

## v0.5.62 — Drive Catalog v1 / issue #29

Status: **CLOSED / PHONE PASS / MAIN VERIFIED — 2026-10-06**.

Final main runtime merge:
`2fe05f18618718e5ef16521ad0411734fb1b89f5`.

Exact accepted runtime head:
`e215ed9a29ccf7cd7e36d083e2579e24bd2a9c3f`.

CI:
- accepted runtime: Tests #451 — PASS; Android PR Check #369 — PASS;
- final main: Tests #455 — PASS; Android Debug APK #123 — PASS.

Final phone install-over-existing:
- `v0.5.62 / build 78` confirmed;
- project data preserved;
- Megane II = 2 volumes;
- NT8340A · 2006-04-18 opens;
- NT8342A remains present.

Accepted:
- native Catalog opens from Home;
- Drive round-trip import uses existing validated `.rdpkg` path;
- selection and terminal state survive rotation;
- loaded catalog is retained across rotation for immediate card restore;
- optional JSON null values are omitted.

Issue #29: complete.

Next:
1. continue issue #30;
2. inventory Windows-source documentation;
3. convert → validate → publish Android-ready packages;
4. regenerate and expand the Renault Docs Catalog.

Останнє оновлення: 2026-10-05.

## v0.5.61 — shared live progress / issue #25

Status: **MERGED / FINAL MAIN CI PASS / PHONE PASS / CLOSED — 2026-10-05**.

Final app source:
`c36e10ddb5fb1a24e9a37d2dc21321a577ed69ed`.

Final CI:
- Tests #443 — PASS;
- Android Debug APK #116 — PASS.

Final phone install-over-existing:
- v0.5.61 visible in app;
- Megane II = 2 volumes;
- Laguna II = 10 volumes;
- Kangoo II = 1 volume;
- project data preserved.

Accepted:
- shared thin live progress with visible file counters;
- smoother balanced progress;
- lifecycle-safe .rdpkg/.rdproject operations;
- named current-volume progress;
- canonical output filename shown during preparation;
- prepared-project reuse;
- unified project/volume deletion dialog sequence.

Next:
- select the next independent Renault Docs issue from current `main`; do not reopen #25 unless a regression is found.

## v0.5.60 — canonical project/package identity

Status: **MERGED / FINAL MAIN CI PASS / PHONE PASS / CLOSED — 2026-10-05**.

Final main commit:
`1c546f15b8e037e81a705b880e3a2f9364fc2bd8`.

Accepted:
- canonical .rdpkg and .rdproject naming;
- rich volume identity in rdproject metadata;
- local public .rdpkg rename migration applied 13/13 without conflicts/skips;
- public Drive catalog cleaned to canonical Megane II packages only;
- final in-place v0.5.60 install preserved Megane II 2 / Laguna II 10 / Kangoo II 1.

Final main CI:
- Tests #395 — PASS;
- Android Debug APK #105 — PASS.

## v0.5.57 — Project-only Home + reversible Legacy quarantine

Status: **IMPLEMENTED / CODE CI PASS / PHONE MOVE PENDING — 2026-10-04**.

Architecture decision:
- current Renault projects are `ProjectStore` metadata + per-volume records;
- imported `.rdpkg` payloads live in app-private `noBackupFilesDir/rdpkg/<packageId>`;
- legacy `DatasetStore` SAF datasets are a separate compatibility layer and are not the canonical project model;
- Home no longer shows a Legacy dataset when the same model already has a populated current project;
- legacy records are retained internally for recovery/compatibility and are not auto-deleted.

Filesystem cleanup:
- new reversible quarantine root: `/storage/emulated/0/Documents/Renault/legacy-quarantine`;
- menu item `23 — Legacy quarantine *_android (move/restore, без видалення)`;
- move is guarded by explicit `MOVE` confirmation, verifies file count + byte size, and writes `manifest.tsv`;
- restore is available with explicit `RESTORE` confirmation;
- no `rm`, `rmdir`, `find -delete` or automatic deletion path exists;
- Laguna legacy `build_root` now points to `.../legacy-quarantine/laguna 2 2001-2006_android`;
- raw source remains `/storage/emulated/0/Documents/Renault/laguna 2 2001-2006`.

Code CI on exact HEAD `5598e96d818058370b0e7fed3af3afcee81a34c4`:
- Tests `37214251297` — PASS;
- Android PR Check `37214251232` — PASS.

Next phone gate:
1. install fresh candidate;
2. confirm Home shows only current Project cards (no duplicate migrated Megane/Laguna Legacy tiles);
3. run menu item 23 and move top-level `*_android` into quarantine;
4. reopen Megane II, Laguna II and Kangoo II current projects and verify their installed volumes still open;
5. run menu item 21 and confirm archived/KEEP state.


## v0.5.57 — durable .rdpkg export/share lifecycle

Status: **PHONE PASS — 2026-10-04**.

Accepted exact phone candidate: `5d2e39e238114991e052a01a662d882b26a3fbc8`.

Accepted phone evidence:
- durable Share preparation shows live progress and survives Activity lifecycle;
- completion opens Android Share Sheet once;
- terminal card `Підготовка тому завершена` remains visible underneath the Share Sheet and is still present after the Share Sheet is cancelled/closed;
- terminal status is dismissed only by explicit `×`;
- already prepared volume package is reused instead of preparing the same volume again;
- Megane II phone evidence used NT8340A / NT8342A project screen; source project/volumes remained intact.

CI on accepted exact HEAD:
- Tests run `37166273230` — PASS;
- Android PR Check run `37166273228` — PASS.

PR: #16 `v0.5.57 — durable .rdpkg export/share lifecycle`.

# Renault Docs — CURRENT PLAN

Останнє оновлення: 2026-10-04.

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
- [x] PREPARING Cancel reaches CANCELLED and does not remain stuck — PHONE PASS 2026-10-03: live scan (`500 files · 6 folders`) → Cancel → terminal `Створення .rdpkg скасовано`; source unchanged, private staging cleaned; Kangoo II remains 1 volume / NT8486.
- [x] Valid inner raw source creates/imports a real native .rdpkg — PHONE PASS 2026-10-03: NT8486 · 2009-08-31 · 276 native; Kangoo II 0 → 1 volume; terminal COMPLETE shown on the same status card with SHA-256.
- [x] COMPLETE rotation + landscape reachability — PHONE PASS 2026-10-03 on fixed build `1d5e76cd212bac2c1daeba7e8870a57664b26df0`: after rotation the Project screen scrolls to the same COMPLETE card; NT8486 remains exactly one volume and no rerun/duplicate import is observed.

Important earlier phone finding now fixed in code:
- an invalid/mixed raw folder could show a FAILED terminal card while the running `Створення .rdpkg` card remained orphaned;
- the v0.5.56 contract is one native-run status surface: `Running → Complete / Failed / Cancelled`.

Phone evidence for invalid/mixed source:\n- selected outer `Kangoo II` folder containing both ZIP and nested extracted volume;\n- scanner entered running state;\n- validation correctly rejected it as parent/mixed source;\n- the same status card transitioned to `Створення .rdpkg · помилка`;\n- no second/orphan running card remained.\n\nNext phone gate:\n- close the FAILED card with `×` and verify it disappears cleanly and does not return immediately.


Phone finding 2026-10-03 — COMPLETE rotation lifecycle (corrected after frame/code review):\n- NT8486 remained installed exactly once (`Kangoo II · томів: 1`);\n- no visible rerun or duplicate import occurred;\n- landscape viewport only shows the upper content through the raw-create tile; the operation status sits lower in the scroll content;\n- returning to portrait shows the COMPLETE card;\n- do not classify card persistence as PASS/FAIL until landscape is scrolled down.

- Follow-up UX audit requested from phone QA: apply/verify the same full-page vertical scrolling contract on Home, especially landscape/small-height layouts.

- [x] Home full-page landscape scrolling — PHONE PASS 2026-10-03 on v0.5.56: landscape can scroll through the project library to the bottom version label; lower project cards remain reachable.


## v0.5.59 — cleanup + catalog + support

Status: **IMPLEMENTED / CI + PHONE QA PENDING**.

Scope:
- BUG #18: prepared-project `↑` indicator refreshes immediately after deleting the prepared `.rdproject`;
- UX #19: app-owned Help copy is user-facing and no longer explains Kotlin/Python/Termux/`*_android`/rotation/lifecycle implementation details;
- FEAT #20: Home gets `Готові проєкти` → Google Drive catalog;
- FEAT #21: Settings → `Про програму` gets provider-independent `Підтримати Renault Docs` action.

Project catalog:
- Google Drive folder: `Renault Docs Projects`;
- URL: `https://drive.google.com/drive/folders/1UyN4UIgaNMrpG-5mLuDBd9laFEbwmb4Y`;
- the folder still needs owner-side public sharing (`Anyone with the link` / viewer) before it is useful to other users.

Support:
- UI/action infrastructure is present;
- `SUPPORT_URL` intentionally remains empty until the owner chooses the final donation/support destination;
- support must stay voluntary and must not unlock app features/content.

Release:
- versionName `0.5.59`;
- versionCode `75`.

Phone gate:
1. delete prepared project artifact and verify `↑` disappears immediately, after Home re-render, rotation and cold start;
2. open Help surfaces and verify implementation jargon is gone;
3. Home → `Готові проєкти` opens the configured Drive folder;
4. Settings → `Підтримати Renault Docs` shows the not-configured message until SUPPORT_URL is supplied.


## v0.5.59 closeout — 2026-10-05

Status: **PASS / MERGED / INSTALLED FROM MAIN**.

- PR #22 merged to `main`;
- merge commit: `464beaa1fd030ac18bc563e75dccad811529912e`;
- Tests PASS;
- Android Debug APK PASS;
- final main APK installed on the phone;
- phone QA PASS, including PDF Viewer landscape fullscreen;
- Google Drive catalog folder is public read-only.

Follow-ups moved out of v0.5.59:
- #25 shared real-time progress for long operations;
- #26 canonical package/project naming and rich project metadata.


## v0.5.60 — canonical project/package identity

Status: **IMPLEMENTATION IN PROGRESS**.

Branch:
- `feat/v0.5.60-canonical-project-packages`.

Scope:
- keep individual `.rdpkg` export naming on the canonical rich identity path;
- use one resolved metadata function for both package filenames and project manifests;
- whole-project bundle filename becomes model + aggregate Renault vehicle codes, e.g.:
  - `Megane-II_E84-L84-K84.rdproject`;
  - `Laguna-II_X74.rdproject`;
  - `Kangoo-II_X61.rdproject`;
- `rdproject.json` now carries per-volume:
  - `vehicle_codes`;
  - `document_code`;
  - `date`;
  - `document_type`;
  - `document_version`;
  - `region`;
- project-level manifest also carries aggregate `vehicle_codes`;
- old prepared `<project-id>.rdproject` is detected and migrated to the canonical name in app-private prepared-share storage;
- arbitrary user files under public `Documents/Renault` are not silently renamed.

Public catalog:
- existing Drive archives still need replacement/rebuild from the canonical exporter;
- do not treat a Drive metadata-only rename as proof that internal package metadata is current.

Release:
- versionName `0.5.60`;
- versionCode `76`.

Phone gate after CI:
1. existing prepared project from v0.5.59 is still detected and migrates without losing the share indicator;
2. prepare/share Megane II project and verify canonical `.rdproject` filename;
3. inspect one exported `.rdpkg` filename for the rich E84/L84/K84 or X74/X61 identity;
4. verify project/volume UI state is unchanged and no duplicate project/volume is created.

## Catalog v3 current gate — 2026-10-06

Status: TOOLING MERGED / DRIVE PACKAGES UPLOADED / PHONE CATALOG BUILD PENDING.

1. Phone: Renault menu → 5, update main.
2. Phone: Renault menu → 25, build public Catalog v3 from local canonical packages.
3. Require Laguna II 10 + Megane II 2 + SHA-256 12/12.
4. Only after PASS: replace existing public `renault-docs-catalog.json` in place, preserving Drive file ID `1mH0YJo1ts_GzpXz9Y4bx7Aj9DKBSgwwA`.
5. Verify Catalog shows Laguna II and smoke early/late volumes.

## Catalog v3 phone build result — PASS

- Phone build from local canonical packages passed: 2 projects / 12 volumes.
- Laguna II 10 + Megane II 2.
- SHA-256 present for all 12 package entries.
- Catalog SHA-256: `b071a45a7302c55d933fedd89a4d7a76312e521e634e6d2b7bf0c7d9c91d9dd4`.
- Public Drive catalog has NOT been replaced yet.
- Next: validate the generated JSON file itself, replace the existing Drive catalog in place, then verify Catalog phone behavior.

## Catalog v3 blocker — reconcile Megane II packages

Current generated v3 candidate is valid JSON and Laguna II is fully aligned 10/10, but the two Megane II local packages differ in byte size from the package files currently served by their Drive IDs. Public catalog replacement is blocked until these package bytes are reconciled. Do not publish the current catalog candidate unchanged.

## Catalog v4 current gate — 15 packages

Status: PLAN MERGED / DRIVE PACKAGES UPLOADED / PHONE CATALOG BUILD PENDING.

1. Phone Renault menu → 5, update main.
2. Phone Renault menu → 25, build public Catalog v4.
3. Require Laguna II 10 + Megane II 5 + vehicle_codes 15/15 + SHA-256 15/15.
4. Inspect generated JSON.
5. Replace existing public catalog in place only after PASS.


## v0.5.68 checkpoint / v0.5.69 Archive Intake — 2026-10-07

Status: **v0.5.68 MAIN BUILD PASS / ICON PHONE QA IN PROGRESS / ARCHIVE INTAKE NEXT**.

Baseline:
- v0.5.68 / build 84;
- main `41e6801b985a344922169b8e8e2b60b535f7b60e`;
- Tests #481 PASS;
- signed Android Debug APK #131 PASS.

Next feature: **#51 Archive Intake**.

Target release: **v0.5.69 / build 85**.

Plan:
- [ ] add SAF archive-file intake without changing existing raw-folder flow;
- [ ] app-private staging with explicit cleanup;
- [ ] safe ZIP extraction + traversal protection;
- [ ] Renault raw-root discovery after extraction;
- [ ] one valid root → existing native raw→.rdpkg handoff;
- [ ] multiple roots → explicit chooser/batch plan;
- [ ] 7Z backend under the same extraction interface;
- [ ] RAR backend under the same extraction interface;
- [ ] clear encrypted/corrupt/unsupported errors;
- [ ] progress + foreground notification + wake-lock lifecycle;
- [ ] cancellation/restart/recreation contracts;
- [ ] real-device archive phone QA including screen lock.

Acceptance: user can choose a supported old Renault archive and receive the canonical installed .rdpkg without manually unpacking the archive; source archive remains unchanged.

## FUTURE UX — NT volume sorting / display controls

Do **not** interrupt the current v0.5.69 Archive Intake phone QA to implement this.

Project volume-list follow-up:
- add explicit sort control for Renault document number (`NT...`);
- natural NT key: numeric part first, suffix second, e.g. `NT8266A < NT8340A < NT8344 < NT8393`;
- provide ascending and descending modes;
- preferred default: **smaller NT first**;
- NT sorting is a presentation order and must not rewrite package/runtime identity;
- volumes without a parseable NT code use a deterministic fallback after coded volumes.

Add a compact `Вигляд томів` / display menu:
- show/hide date;
- show/hide vehicle codes;
- show/hide document type/version (e.g. Visu v2.2);
- sort NT ascending/descending;
- later consider date-based sort as an alternate mode, with NT fallback for missing dates.

Current code renders `ProjectStore.volumes()` in stored order, so this is a UI-only sorting/display layer.

## FOLLOW-UP — archive duplicate fast path from NT/index metadata

Real-phone v0.5.69 finding:
a duplicate `NT8266A · 2004-06-28` ZIP was correctly rejected, but only after ZIP extraction had progressed through thousands of entries.

Target:
- prove an installed duplicate before extraction whenever archive filename/path/index metadata exposes a unique NT code;
- use bounded direct archive-entry reads, not full extraction;
- retain conservative fallback when the identity is ambiguous.

This is an optimization/follow-up after the current QA sequence; the existing final duplicate result is functionally correct.

