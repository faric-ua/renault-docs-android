> **Нова сесія:** спершу `CLAUDE.md` (правила) і `STATUS.md` (поточний стан). Цей файл нижче — історичний архів.

## CURRENT — v0.5.100/build116 SIGNED QA PRERELEASE PUBLISHED; MULTI-SELECT PHONE QA NEXT — 2026-10-12

- Owner provided v0.5.99 Megane II screenshot listing **22 tomes** (prior previously prepared .rdproject had **21**). They requested checkbox multi-selection of ANY subset instead of only one/all. No automatic export when merely checking items.
- **PR #174 MERGED**: Home preview has «Вибрати томи» and explicit «Усі томи». Checkbox chooser displays full NT/date/filename, counts `Вибрано X із Y`, supports `Вибрати всі / Очистити`, keeps `Вибрати папку` disabled on zero checks, and holds exact opaque IDs on rotation. Confirmation revalidates exact set and SAF remains user-driven; service emits only selected .rdpkg files, reads back SHA256, no overwrite, emits JSON report with expected_volumes equal to selected size. Cancel/back no write. Added Kotlin 3-of-4+duplicate tests and Python UX/static tests; version v0.5.100/build116.
- Exact PR checks **Python Tests #681 PASS**, **Android PR Check #521 PASS**. Exact source main commit **9ca382000c4bc0c151cce24f1b1f15b51907eb1d**; main **Python Tests #682 PASS**, stable-signer **Android Debug APK #164 PASS** (run ID **38178013797**, artifact ID **11717305399**, bundle digest sha256:7cbef87bd41d22397fcc61ad99a1a96453f33455b4c2731948b1831d045f679a). Package com.saney.renaultdocs; versionCode116; versionName 0.5.100. Verified APK SHA-256 **d99574294d3cd92b98c449ad161e22d5d8211aa624e2e131ca72582893ea7491**.
- **PR #175 MERGED**, verified publisher workflow **#18 PASS**. Debug prerelease: https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.100-debug ; exact APK: https://github.com/faric-ua/renault-docs-android/releases/download/v0.5.100-debug/Renault-Docs-v0.5.100-debug.apk ; published asset ID **631901022**. **PHONE QA STILL PENDING, v0.5.100 not confirmed installed**.
- **NEXT USER PHONE ACTION:** install v0.5.100 ON TOP OF v0.5.99 without clearing/uninstalling; confirm 22 installed Megane II tomes. Open Megane II → ⋮ → Експортувати томи окремо → Вибрати томи. Inspect checkboxes and choose e.g. 3 separate NTs; rotation should keep exact 3. Back/cancel test without export. Only when comfortable choose a **NEW EMPTY PRIVATE SAF test folder**, then verify exactly 3 packages + one JSON (expected_volumes=3); no silent full-22 write, no public Drive catalog. Do not use populated Drive folder for QA.
- **CRITICAL separate BUG #173 unresolved:** before v0.5.99, owner added 22nd tome and historical 21-tome app-private prepared .rdproject menu entries vanished. Confirmed code path `ProjectStore.upsertVolume -> invalidatePreparedProject -> PreparedShareStore.deleteProject` can physically delete old prepared snapshot. Do not claim v0.5.100 fixed this: it did **NOT** change .rdproject lifecycle, does not restore locally missing snapshot. Historical 21-tome cloud archive preserved by Google Drive metadata: `Megane-II_X84-B84-C84-S84-E84-L84-K84.rdproject`, 1,262,821,306 bytes, Drive file ID **1Cz4UxZxEIQSRv5ueeANCou_jY-nsYgE3**, content integrity still unverified. Avoid repack/delete/overwrite/downgrade and correct #173 separately with read-only local diagnosis, immutable historical snapshots, no silent deletion. #162/#147 OPEN; #133 BLOCKED.

---

## CURRENT — v0.5.99/build115 SIGNED DEBUG QA PRERELEASE PUBLISHED; SINGLE-TOME PHONE QA NEXT — 2026-10-11

- User verified on installed **v0.5.98** the Megane II menu and preflight: **21 in project, 21 ready**, with legacy one-file .rdproject still available. They approved implementing safe **Один том / Усі томи** choice rather than blindly exporting all 21.
- Stage 1b **PR #170 MERGED** to main source **4a42414735694aeffadab05e8388b9dc896ed867**. Home preview shows **Один том** (scrollable full-NT/file-name selector) vs **Усі томи**; Back is no-op. Exact selected opaque volume IDs flow through SAF picker, survive Activity recreation, revalidate in service, and are saved to folder-export run store; selected subset drives one .rdpkg and JSON `expected_volumes`. Stale, duplicate, empty IDs fail closed, no silent full export. Existing `.part`/SHA256 readback, never-overwrite and no Drive catalog publication remain unchanged. Kotlin unit regressions cover distinct NT8340A and NT8342A and invalid selections.
- Exact-head CI **Python Tests #678 PASS**, **Android PR Check #519 PASS**; main **Python Tests #679 PASS** and **Android Debug APK #163 PASS** with repository stable signer verified. APK source version 0.5.99/build115; main run ID **38166740107**; artifact ID **11712502018**; immutable bundle digest `sha256:95d8f3f31af171565b2efbd39e7ba922ee73ce355c9cff2889678e23fed58923`.
- **Signed QA prerelease published** via release metadata/promotion PR **#171 MERGED**, publisher workflow **#17 PASS**. Release https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.99-debug ; APK https://github.com/faric-ua/renault-docs-android/releases/download/v0.5.99-debug/Renault-Docs-v0.5.99-debug.apk ; exact **APK SHA-256 656bf95d047b070a12faad768ab98e88555791ba36d3f6da76f4932130764f19**, asset ID 631572701. **PHONE QA PENDING**; DO NOT label v0.5.99 installed or core stable accepted.
- **NEXT PHONE STEP:** install v0.5.99 **over** v0.5.98 (NO uninstall/data clear), verify existing Megane II and 21 tomes intact. Open Megane II project `⋮` → export separate .rdpkg → inspect preview with **Один том / Усі томи**. Tap **Один том**, inspect 21-item selector, choose ONE distinct NT, select a **NEW EMPTY PRIVATE Google Drive SAF test folder**, verify exactly **one** `.rdpkg` and small inventory JSON, hash/read-back result, no unexpected whole-project export. Test rotation/cancel/background and provider `.part` rename separately before Stage K phone PASS. Never use populated Drive catalog folder or delete prior uploads. No remote catalog mutation. #162/#147 OPEN, #133 auth/payment blocked, old 1.26GiB `.rdproject` intact; legacy one-file re-import unproven.

---

## CURRENT — v0.5.98 / build 114 SIGNED QA PRERELEASE PUBLISHED — 2026-10-11

- Stage 1 per-volume SAF export PR #165 MERGED, docs PR #166 MERGED. Owner asked to continue. Version-only PR #167 MERGED; exact source d92d1b41ae722581f93b1170323eee0e97c929a8; Android version 0.5.98 (114). PR Tests #674 PASS, Android PR Check #516 PASS, main Tests #675 PASS and stable-signed Android Debug APK #162 PASS, run ID 38164855836, artifact ID 11712105452.
- QA prerelease v0.5.98-debug PUBLISHED via PR #168 and Publish workflow #16 PASS: https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.98-debug . Original APK: https://github.com/faric-ua/renault-docs-android/releases/download/v0.5.98-debug/Renault-Docs-v0.5.98-debug.apk . APK SHA-256 bcb88aa7a89db6cabd0933dd3a93bf26d038931b8ce5d59e9a7b4da888270f62; release APK asset 631517800. Phone QA PENDING, not Core Stable accepted.
- NEXT: install v0.5.98 over existing v0.5.97 (never uninstall or clear data). Confirm Megane II project, existing 21 tomes and project menu primary independent .rdpkg export. Test on NEW EMPTY PRIVATE SAF test folder, especially Google Drive provider. Verify .part rename, SHA readback, inventory JSON, no overwrite, rotation/lock/background/interruption. Do NOT export all 21 to populated Drive before controlled test. #162 and #147 remain OPEN, Stage 2 exact Drive IDs/catalog not published; #133 login/paywall BLOCKED. Original files, uploaded 1.26GiB .rdproject and installed tomes unchanged; one-file .rdproject restore on second phone not proven.

---

## CURRENT — #162 MAIN PER-VOLUME SAF FOLDER EXPORT MERGED / SOURCE CI PASS — 2026-10-11

- Owner clarified architecture: **keep existing one-file `.rdproject` export as secondary offline/USB transfer**, but new default for distribution is **separate individually downloadable `.rdpkg` packages** with lightweight metadata-only project catalog. Do not remove old .rdproject file/producer. **Important:** Android .rdproject **re-import/restore was NOT established** by export code audit; verify/implement separately before promising one-file import on a second phone. Existing 1.26GiB user Google Drive export remains untouched/unverified due connector limit.
- **New core Android source feature Stage 1 #162 MERGED**: PR **#165** main SHA `7ef099ddb8280f2d190e14a222b167ce8d8931e2`. Exact PR Python Tests **#672 PASS**, Android PR Check **#515 PASS** (Kotlin `testDebugUnitTest`, debug APK built/signed ephemeral PR key, verified), main Python Tests **#673 PASS**, main Android Debug APK **#161 PASS**. No new stable signed user release, **phone still installed v0.5.97/build113**, DO NOT ask to install ephemeral CI builds as in-place upgrade.
- Home `⋮` now has primary `Експортувати томи окремо (.rdpkg)` preflight + SAF `ACTION_OPEN_DOCUMENT_TREE` picker, and explicitly optional old `Поділитися проєктом одним файлом (.rdproject) · офлайн`. `ProjectVolumeExportPlan` rejects non-managed legacy volumes, same-case/variant filename collisions and duplicate IDs, preserves distinct NT8340A vs NT8342A. `ProjectFolderExportService` foreground job writes **one .rdpkg per installed volume** directly to user-selected folder, verifies SHA-256 by reading back each SAF file, then renames ONLY its own .part; existing matching basename is `EXISTING_UNVERIFIED`, never overwritten or assumed identical. Writes small `RenaultDocs-<project>-<timestamp>-export.json` inventory `ready_for_public_catalog=false`; **does not obtain/invent exact Drive file IDs or write public catalog**. `ProjectFolderExportRunStore` + MainActivity status across rotation; FGS timeout stops as failure. Details `docs/architecture/PROJECT_FOLDER_PER_VOLUME_EXPORT.md`.
- **Still PENDING phone QA & next core implementation:** Google Drive SAF provider may reject temporary rename or append an extension; test first on **new empty private QA folder**, verify per-volume SHA/readback and absence of double uploads, existing-file skip, progress/rotation/background/lock/interruption (#159/#40), actual service pause/resume and report. Do not claim 21 phone volumes PASS or release #162 closed. Stage 2 remote exact Drive IDs + SHA / collision #143 review + approved catalog publication; support retries/cancel and safe provider recovery; consumer per-volume import already exists. #147 CORE STABLE open; licensing/payment security #133 blocked until separate later release.
- No original Windows ZIPs, Drive files, 21 installed Megane tomes, 10 Laguna catalog packages, or previously uploaded monolithic .rdproject were modified, moved, replaced or deleted.

---

## CHECKPOINT — PROJECT AS MANIFEST, NOT MONOLITHIC ZIP / #162 ARCHITECTURE MERGED — 2026-10-11

- **Owner decision:** Default cloud sharing is per-volume independent .rdpkg objects, with lightweight project metadata referencing exact remote file ID, package ID and immutable full NT/date/version/region/vehicle identity; one consumer can choose one tome without acquiring a whole model. Catalog `renault-docs-catalog.json` and current Android per-volume downloader ALREADY support project→volume grouping. `RdprojectExporter` currently packs all phone tomes again and nests them into a 1+GiB outer `.rdproject`; **do not use it as target cloud distribution**. Keep old uploaded 1.26GiB 21-volume .rdproject backup intact (its internal contents still NOT verified due Drive connector 256MiB limit), preserve 21 installed tomes and prepared 5 Megane/10 Laguna catalog references, all original sources and unresolved same-basename Drive objects #143. Managed on-phone .rdpkg content is expanded and individual ZIP export can still require one packaging pass for missing remote copies; do not claim compressed originals are permanently retained.
- Architecture ADR `docs/architecture/PROJECT_MANIFEST_INDEPENDENT_VOLUMES.md`, [issue #162](https://github.com/faric-ua/renault-docs-android/issues/162) OPEN. Reviewed actual `build_drive_catalog.py`, `DriveCatalog.kt`/`Activity`/`Client`, `CatalogImportService`, `ProjectStore`, `RdprojectExporter`; **no Android code/APK changes implemented yet**. Test `tests/test_project_manifest_independent_volumes.py` confirms existing metadata-only catalog groups 21 synthetic Megane + 10 synthetic Laguna independent refs into compact JSON without any project ZIP and keeps NT8340A/NT8342A distinct. PR #163 merged main **`8a5412553017f5fb37f633677459f112f30ccfd6`**; exact-head Python Tests **#666 PASS**, main Python Tests **#667 PASS**.
- **Next implementation issue #162 still OPEN:** non-mutating preview → reuse already validated exact remote ID+hash, otherwise stream one missing .rdpkg at a time for user-selected Drive upload, verify, update small catalog atomically with user/rights approval, handle failure/rotation/lock/retry and keep legacy .rdproject as optional offline bundle. Current catalog Python builder input is flat; support nested staging only if explicitly needed. This work belongs to core before signed phone-accepted Core Stable #147, NOT to #133 Google login/license/paywall which remains blocked until later separate release. Do not mark actual 21 tome archive ZIP CRC/PDF/Modern PASS from this synthetic test.

---

## CURRENT — OWNER ARCHITECTURE DECISION #162: PROJECT = METADATA REFERENCES + INDEPENDENT .RDPKG VOLUMES — 2026-10-11

- Owner explicitly rejects monolithic project ZIP as default transfer/distribution. **One Renault volume = one portable `.rdpkg`**, project Megane II/Laguna II/Kangoo II = lightweight logical manifest listing each volume's complete distinct NT identity, package ID, exact remote Drive file ID, size/digest and metadata. Users must be able to download **only one selected tome**; selecting whole project means **sequential individually retriable** tome operations, not 1+ GB bundle. Preserve all original archives, existing installed tomes, 10 Laguna + 5 Megane currently cataloged prepared volumes, #143 same-named Drive copies, and the already uploaded 1.26GB 21-volume `.rdproject` snapshot **without deletion**.
- **Important current code analysis:** This is *already built into cloud consumption* via `tools/build_drive_catalog.py` → small `renault-docs-catalog.json` grouped by project_id and independent .rdpkg `drive_file_id`; Android `DriveCatalogActivity` selects volume refs and `CatalogImportService` downloads one `.rdpkg` each. `ProjectStore` separately tracks associations. **Missing/undesired default = `RdprojectExporter`** repacks every expanded installed volume to temp .rdpkg and then nests all in huge .rdproject, high storage/network cost. Existing Android v0.5.97/build113 behavior UNCHANGED; do not represent target manifest export as already implemented. Phone package storage remains expanded under noBackupFilesDir, so export of an individually missing remote package may still require ZIP compression once. Actual 21-volume .rdproject Drive content not externally verified due 256MiB connector cap, Stage J PENDING, independent 5 catalog Megane real audits still PASS in ledger.
- New architecture issue **#162 OPEN** and decision `docs/architecture/PROJECT_MANIFEST_INDEPENDENT_VOLUMES.md`. Proposed new default: preview volumes/status → verify/reuse exact existing remote ID+hash when possible → export/upload missing .rdpkg **one-at-a-time** → validate → atomically publish small catalog manifest. Default must not create multi-tome ZIP; keep old .rdproject optional as backup, migration/legacy compatibility explicitly tested. Directory grouping is optional; current catalog builder's input is flat. Prototype regression `tests/test_project_manifest_independent_volumes.py` synthetic 21 Megane + 10 Laguna catalog without any project-archive file, metadata-only JSON with 31 refs. **PR CI and actual Android feature implementation PENDING**, user did not authorize deleting or overwriting anything.
- Core Stable gate #147 remains OPEN, licensing/Google friend access/billing/security #133 BLOCKED until core independently complete and signed phone-accepted. Project distribution redesign is core functionality and separate branch/release QA, not monetization. New architecture must respect #159 live status rotation, background lifecycle #40, metadata collision #143 and real-audit workflow #30.

---

## CURRENT — QA COMPLETION LEDGER ACCEPTED STAGES A–I — 2026-10-11

- **Canonical status board:** `docs/assistant-kit/VERIFICATION_LEDGER.md`. Completed checkboxes for Stage A/B/C/D/E/F/G/H/I **within each explicitly documented scope**; real-data rows for all 5 current Megane II catalog NT volumes NT8340A, NT8341A, NT8342A, NT8344, NT8393 show raw original/package equality, PDF structural read, static HTML link scan and Modern/Runtime/Fast Pack asset parity, all PASS.
- Stable evidence: Stage H PR #155 Python Tests #660 and main #661 PASS; Stage I PR #156 Python Tests #662 and main #663 PASS; Stage B/G/F etc CI already committed and retained in `tests/`. **45,312 original file matches, 9,243 PDF structural opens, 256,806 resolved static targets, 1,726 Runtime section/shard pairs, 33,801 Fast Pack byte matches** across exactly 5 matching catalog NT volumes. Ten `.HTM` dynamic suffix markers classified separately; no observed literal link failures.
- **Do not ask owner to redo these PASS checks** solely due to moving chats or next steps. Regression CI automatically reruns affected tests when code or actual sources change; prior source-specific acceptance remains until invalidated by changed contents. Keep all distinct full NT codes separate.
- Still OPEN: #30 full source/catalog pipeline for other NTs/Laguna/Kangoo, real Android interactive UI/visual PDF QA, licensing/rights, #147 signed/phone-accepted core stable, separately #40/#51/#85/#102/#123 disposition. #133 login/friends/Play billing/security remains BLOCKED until Core Stable released and owner accepts. No APK/source package/Drive data changes caused by this ledger.
  
---

## CURRENT — #30 STAGES H+I REAL 5/5 MEGANE CATALOG VOLUME AUDIT PASS / MAIN CI VERIFIED — 2026-10-11

- User explicitly asked assistant to verify large volumes of Renault documentation autonomously; do not delegate manual page-by-page work to user. User prefers brief visible progress updates during complex work and final response after a finished milestone. Respect strict #147 CORE STABLE first, #133 sign-in/friend allowlist/payments/security in **separate future release only**.
- **REAL PRIVATELY STAGED OWNER FILES INSPECTED, NO DRIVE MODIFICATIONS**: original Windows ZIP and exact current v4 catalog-referenced prepared `.rdpkg` pairs for all five distinct Megane II NT8340A / NT8341A / NT8342A / NT8344 / NT8393 (not copies/duplicates across different NTs). Independent read-only ZIP CRC and all-file path/content byte audit **45,312 / 45,312 original files identical**, zero missing/extra/modified. All **9,243 PDF files** structurally opened using PyMuPDF, **11,195 PDF pages** counted, 0 PDF parse-open failures (NOT visual comparison of every page). Static HTML links **256,806** exact-case valid targets and **10** actual dynamic JavaScript suffix `.HTM` fragments (5 each NT8344/NT8393), zero genuinely unresolved literal refs. Proof scope = current 5 Megane Drive catalog pairs only, not 24 original ZIPs / 21 on-phone tomes / Laguna / Kangoo.
- **Stage H PR #155 merged** app-unchanged source commit `9f684d02d73abb8d0d116271d01a2e7e62c3f258`; exact PR Python Tests #660 PASS, main Tests #661 PASS. New reproducible read-only offline `tools/audit_original_zip_rdpkg.py`, synthetic regressions `tests/test_original_zip_rdpkg_audit.py`, docs `docs/architecture/ORIGINAL_ZIP_RDPKG_REAL_AUDIT.md`. Only raw bytes unchanged or exact `core.convert_paths.patch_text` allowed HTML/JS changes qualify, cross-NT/wrong date/changed PDF/removed/extra files FAIL; unresolved wrong-case links REVIEW_REQUIRED. Earlier PR Tests #659 failed on test-only regexp `.HTM` handling; fixed and passed #660 **before** merge, no real document fault.
- **Stage I real Modern/Runtime/Fast Pack audit** for SAME five `.rdpkg`: **1,726** Modern section codes/HTML entrypoints matched 1,726 Runtime section shard JSON parsed with exact NT/count/Classic paths, 0 missing/bad; nested Fast Pack digest and CRC pass and all **33,801** Fast Pack entries byte-identical to outer same-named `.rdpkg` assets, zero missing/different; compiler `unsupported_action_count=0`, `warning_count=0` in 5 real manifests. **PR #156 merged** at main `9a0fb71f4fefbc0f26942194c557f44ea733a74b`; PR Python Tests #662 PASS, main Tests #663 PASS. New `tools/audit_rdpkg_native_graph.py` with negative tests and `docs/architecture/RDPKG_NATIVE_GRAPH_REAL_AUDIT.md`. **No copyrighted source files, contents, private Drive IDs or URLs committed; no user files changed or deleted.**
- **Evidence limits:** no actual Android click-flow, JavaScript execution or all-PDF pixel-rendered UI QA; other 19 direct Windows Megane ZIPs may not have a current matching prepared catalog entry, Laguna II multi-volume original and Kangoo not yet paired with catalog packages; no file publishing / legal rights inferred. #30 and #147 Core Stable remain OPEN; #143 within-NT same-name Drive file copies untouched (do NOT delete/rename). #40 Android quota emulator not tested, #51 archive edge QA, #85 picker suppressed prepared .rdpkg negative branch, #102 notifications and #123 per-volume next real operation for release triage. Installed phone v0.5.97/build113 unchanged; no APK update necessary. #133 access/license features BLOCKED until final independent signed accepted CORE STABLE release.
- **Next work:** prioritize remaining matched source-to-package real corpus (Laguna multi-volume split discovery and Kangoo), then Modern/Runtime graph validation for additional volumes and phone interactive QAs where feasible; use reusable auditors, not manually reviewing thousands of PDFs, and never claim entire library PASS from this limited five-volume sample.

---

## CURRENT — #30 STAGE G PREPARED VOLUME ↔ RDPKG SHA-256 PARITY MERGED / MAIN CI PASS — 2026-10-11

- User asked to continue core Renault Docs without requiring mic follow-up, with visible stage previews and final answer only after a completed unit. Stage G **PR #153 merged** to main source commit `268c99e9fbf728a3c3a10851d06383a3685f3183`; exact PR Python Tests **#657 PASS**, main Tests **#658 PASS**. Adds offline read-only `tools/compare_prepared_volume_to_rdpkg.py` + `tests/test_prepared_volume_rdpkg_parity.py` with tiny **real `core.rdpkg.build_rdpkg`-generated** multi-NT prepared HTML/PDF packages + `docs/architecture/PREPARED_VOLUME_RDPKG_PARITY.md`.
- Tool selects **one specific volume** (NT code/id/source folder/title), validates source folder bounded and without symlinks, checks Stage F whole .rdpkg internal CRC/manifests, then streams SHA-256 for every HTML/PDF/other file under the already **prepared/normalized** source volume and matching `.rdpkg` ZIP member. FAIL for wrong project/full NT/date/volume ID/source folder/entrypoint, missing/extra/changed bytes; source and package never extracted, written, moved, renamed, deleted or uploaded. NT8340A and NT8342A remain distinct full Renault document identities. Files outside selected volume like generated `_renault/` native/runtime assets are Stage F CRC-checked but NOT asserted original source parity.
- **Actual owner data NOT tested by Stage G:** this is Python synthetic CI, not Drive original ZIP→normalized HTML semantic parity, real user's 21 installed Megane tomes, Drive remote bytes or legal distribution rights. #30 remains OPEN for original archive normalization/asset preservation & actual approved real-file read-only checks; #143 same-name Drive object collisions inside each NT review-only (no cleanup). Next stage H: original Windows ZIP/7Z/RAR → prepared normalization preservation ledger, allowed HTML transformation vs untouched assets/PDF, multi-volume discovery, then explicit real source sample when owner data are safely accessible; do NOT assume original↔prepared HTML byte match or publish.
- **No Android changes:** phone still Renault Docs v0.5.97/build113; no new APK/reinstall. CORE STABLE gate #147 OPEN and auth/friends/Google Play subscription/licensing/entitlement #133 BLOCKED until fully committed, tested, signed and phone-accepted independent core release. Other outstanding release disposition: #40 shortened emulator Android 15+ FGS quota not run, #51 archive negative/cancel cases, #85 app-side prepared .rdpkg rejection phone blocked by SAF, #102 notification recovery, #123 per-tome reporting natural flows. Record blockers or justified deferrals honestly before Core Stable.

---

## CURRENT — #30 STAGE F FULL RDPKG PAYLOAD CRC INTEGRITY VERIFIED MAIN CI PASS — 2026-10-11

- User requested continued work and periodic short preview/status during lengthy tool activity, but contact only at end of a completed development unit; no automation/unattended background process implied. Core-first hard gate #147 remains OPEN and #133 Google friends/license/billing/security stays BLOCKED until independent signed, tested and owner PHONE-accepted CORE STABLE release; phone stays Renault Docs v0.5.97/build113.
- **Completed Stage F:** PR **#151 MERGED** at main `4da8454d49bdda6d2fea6087762d471239108d53`; exact-head Python Tests **#655 PASS**, main Tests **#656 PASS**. Added standalone read-only `tools/validate_rdpkg_payload.py`, synthetic `tests/test_rdpkg_payload_integrity.py` (13 scenarios), fixes Stage E test fixtures to use accurate manifest counters, adds prepublish integration negative test and documents `docs/architecture/RDPKG_PAYLOAD_INTEGRITY_PREFLIGHT.md`. `tools/preflight_drive_publish_plan.py` now requires streaming every embedded ZIP payload through CRC, safe path/no symlink/no dup case names/no encryption, bounded manifests, exact payload count/bytes, matching project/volume/entrypoint, and BLOCKS `local_package_payload_integrity_failed` even if Drive ID/name/size metadata matches. Absolutely no disk extraction, source ZIP modification, Drive download/mutation, catalog rewrite, deletion, signed APK rebuild or auth/billing features.
- **Limit of evidence:** Stage F synthetic Python CI PASS only; actual pre-existing user .rdpkg corpus and real Windows-original→prepared volume content parity not run. #30 full source corpus staged conversion, remote byte/hash, rights and actual catalog publish still OPEN. No automatic identical/superseded NT interpretation: NT8340A and NT8342A distinct Renault documentation, while #143 two same-name physical Drive copies per NT require separate review and NEVER auto-delete.
- Future core priority: read-only real package corpus/content parity and release blockers #51 other archive edge cases, #40 Android 15+ 6h dataSync shortened emulator quota still untested, #85 app-level bad .rdpkg picker guard not invoked in phone, #102 notification recovery, #123 natural per-volume reports. Decide blocker vs deferred before Core Stable #147, then exact Android and phone QA; #133 access-security later separate version.

---

## CURRENT — #30 STAGE E EXACT-DRIVE-ID PREPUBLISH PREFLIGHT / MAIN CI PASS — 2026-10-11

- Owner expects visible stage previews while working and a response only after a completed concrete unit; no automation or persistent background work unless separately scheduled. The hard release order stands: **finish/commit/CI/phone-accept signed standalone CORE STABLE first (#147 OPEN), only THEN separate Google account/free-friends/Play purchases/licenses/encrypted-document release (#133 BLOCKED)**. Core phone remains v0.5.97/build113; no Android code/API/auth/security/billing changes made this unit.
- Verified Stage E core tooling PR **#149 merged** on main `0dbafe0ce555fa78f0ed892c93fc5cad33ec0442`. Exact-head PR Python Tests **#653 PASS**, main Tests **#654 PASS**. Added `tools/preflight_drive_publish_plan.py`, synthetic real-format .rdpkg tests and `docs/architecture/PREPUBLISH_DRIVE_ID_PREFLIGHT.md`. It compares locally validated canonical .rdpkg to selected exact Drive file IDs from a pre-existing publish plan and a privately supplied metadata snapshot, checks filename/size/optional SHA256, flags same-name ID collision **within each volume**, never fallback to basename; prints ID/path-free JSON without changing any files/catalog. Statuses BLOCKED / REVIEW_REQUIRED / METADATA_MATCH_CONTENT_UNVERIFIED / METADATA_AND_HASH_MATCH, with `can_autopublish=false` always. 9 new test cases cover correct/wrong IDs, name, size/hash, collisions, distinct NT, duplicate package identity and no local writes.
- #30 remains OPEN for actual Drive source/archive validation, large nested ZIP volume discovery and content/hash parity, source rights, destination approval and catalog publication acceptance. No original Windows ZIPs or prepared Drive items downloaded/moved/deleted/replaced. #143 prepared same-filename two-copy collision remains REVIEW; NT8340A and NT8342A are **distinct** Renault document codes. #40 emulator system quota not run, #51 format/cancel QA, #85 app-side .rdpkg disallowed handler, #102 completed notifications, #123 per-volume future natural import still pending release triage.
- **Next work**: complete core functionality and release checklist safely, preferably source→prepared content/manifest parity and legal distribution gate; no forced phone import or core APK rebuild for Python tooling. Treat metadata-only SHA absence as UNVERIFIED, not PASS. #133 access monetization never introduced during core stage.

---

## CURRENT — OWNER'S CORE-THEN-ACCESS RELEASE GATE + #30 STAGE D MERGED / MAIN CI PASS — 2026-10-11

- **STRICT TWO SEPARATE RELEASES:** finish all agreed Renault Docs core functionality, merge/commit, exact-head CI and signed APK, verify stable in-place device operation, record actual PHONE acceptance, tag/publish standalone CORE STABLE, and owner accepts. Only THEN can #133 Google identity/friend grants/one-time Google Play billing/subscription/Play Integrity/licensed encrypted docs begin in an entirely separate branch/version/release. No entitlement/access/billing/security logic in core. Gate contract `docs/architecture/CORE_BEFORE_ACCESS_RELEASE_GATE.md` and issue **#147 OPEN**; #133 explicitly BLOCKED. v0.5.97/build113 is **not** claimed as final stable.
- #30 Stage D: PR **#146** merged at main SHA `66c63dc30b82092d1b60fa0a512a8cce249cee70`. New offline/no-write exact-allowlist source metadata tool `tools/audit_drive_source_metadata.py` + tests/docs; PR Python Tests **#651 PASS**, main Tests **#652 PASS**. No Drive network, no payload download, no original ZIP/RAR/7Z edits, no prepared catalog uploads, no APK rebuild. Private provider IDs/URLs excluded from report. Strong cross-model flags versus unverified filename and distinct complete Renault NT identities; never conflate NT8340A and NT8342A.
- Real metadata Stage C scopes: Kangoo II original 1 ZIP, Megane II schematics 24 ZIP, Laguna II schematics one large multi-volume candidate; folder pagination/deeper trees not validated. #143 older same-name/different-size .rdpkg variants remain untouched pending independent data audit, not automatic deletion. #30 full conversion/validation/catalog pipeline remains OPEN, as do #51 more format cases, #40 emulator FGS QA, #85 untriggered in-app negative guard, #102/#123 natural-operation QA; all release blockers/deferred items must be resolved/dispositioned honestly in #147.
- Next core development: continue #30 source provenance/catalog pipeline and remaining release blockers without bringing forward #133. Phone currently v0.5.97/build113; no user action/APK update required for tooling-only milestone.

---

## CURRENT — #30 STAGE C BOUNDED GOOGLE DRIVE SOURCE INVENTORY (METADATA ONLY) — 2026-10-11

- After Stage B PR #142 (Drive prepared catalog offline audit merged / PR Python Tests #649 PASS / main Tests #650 PASS), read-only connected Drive folder listings inspected only known source trees: Kangoo II / MEGANE II / laguna2; **no archive downloads, unpacking, conversion, uploads, deletion or Drive edits**.
- Kangoo II source folder: 1 ZIP (NT8486, 67,981,490 bytes). MEGANE II → «Схеми»: 24 direct ZIP archives, with names including NT8445/NT8298A/NT8340A/NT8343. Laguna II → «Схемы»: 1 large `laguna 2 2001-2006.zip` (~537,664,080 bytes) that may contain multiple tomes; do not equate this to a one-volume .rdpkg. These are **26 source ZIP filenames**, NOT 26 validated / nonduplicated volumes.
- Also enumerated 11 direct subfolders under model trees: 25 ZIP and 27 further nested folders. Deeper folders not scanned; shared root contains unrelated software/media/PDF items; no completeness claim or root-wide recursion. Known prepared `Renault Docs Projects` still 17 .rdpkg + catalog JSON, catalog v4 references 15 volumes including 5 Megane II. The 24 original Megane ZIP filenames should **not** be automatically uploaded as 19 missing prepared docs: installed-phone coverage (21 Megane tomes), source NT and package identity are independent, and content/rights have not been verified.
- Separate #143 OPEN: two older larger same-name unreferenced prepared .rdpkg files (NT8340A/NT8342A); catalog points to newer smaller exact IDs. No automatic cleanup or selection by basename. #30 remains OPEN, next phase is **source metadata manifest / allowlist** with privacy-safe provenance and bounded depth; owner-approved local staging and actual package/hash validation later. Publishing requires explicit content-rights review.
- Android code unchanged; phone remains Renault Docs **v0.5.97/build113**. #40 emulator OS quota deferred, #85 inner forbidden file handler not phone exercised, #133 licensing/protected docs future TODO only.

---

## CURRENT — #30 STAGE B DRIVE CATALOG METADATA AUDIT MAIN CI PASS — 2026-10-11

- PR #142 merged with new **read-only/offline** `tools/audit_drive_catalog_metadata.py` and synthetic tests, no Android source or APK changes. Commit `e6eaad6ab3a47c95615e38bfe9850d60c1dd6986`; exact PR Tests #649 PASS, main Tests #650 PASS. Compared provider-IDs + names + size in already acquired local JSON snapshots; no downloader/Drive modification/output file, no credential or Drive ID emitted in reports. Missing IDs from partial listing stay UNVERIFIED; SHA still unverified without content read.
- Real connected Drive read-only snapshot: separate original Kangoo II, MEGANE II and laguna2 source trees (some nested, root contains unrelated files); prepared `Renault Docs Projects` folder lists **17 .rdpkg and 1 catalog JSON**; catalog version 4 has **15 volume references**. Two distinct same-name prepared files for Megane II NT8340A and NT8342A have older larger unreferenced copies; catalog uses newer smaller Drive IDs. Neither copy's payload rehashed. No delete/overwrite/move/rename/upload; independent **#143** is OPEN and blocks any filename-only 'cleanup' or publishing.
- User's installed Renault Docs remains **v0.5.97/build113**. Stage #30 remains OPEN: next stage C is strictly folder-allowlisted metadata inventory of original Windows archives with bounded depth / completeness; no conversion or publication until source rights + owner-approved scope. #40 emulator FGS QA deferred; #85 in-app .rdpkg negative handler still not exercised by picker; #51/#102/#123 natural-operation QA; #133 Google licensing/protected docs separate future TODO.

---

## CURRENT — #30 STAGE A SOURCE INVENTORY MERGED / MAIN CI PASS — 2026-10-11

- Project Megane II orientation PHONE PASS: user confirmed Android status/navigation bars hide in landscape and return in portrait on installed Renault Docs v0.5.97/build113. Issue #23 remains OPEN for app-wide screens; no code change/new APK.
- Next functional #30 foundation landed via PR #140, main commit `e515108e6ef21c43b747e191a62cd191afed21e4`: `tools/inventory_windows_archives.py` scans ONLY local owner-selected Windows ZIP/7Z/RAR and indicates prepared/corrupt entries, filename-only inferred NT/date/Visu, possible non-identical NT/date duplicates, optional SHA/recursive, deterministic JSON to stdout. No output writes, conversion, extraction, deletion or uploads. Existing `tools/build_rdpkg.py` and `tools/build_drive_catalog.py` not changed. PR Tests #646 PASS; main Tests #647 PASS. **Tooling-only; no APK rebuild or phone installation required.**
- Read-only connected Drive **metadata** verified that known Kangoo II NT8486 original ZIP (67,981,490 bytes) is in a **separate original-source folder**, not the `Renault Docs Projects` prepared-catalog folder. No bytes downloaded, full folder listing confirmed, permission changed, Drive ID published, source data modified, or data uploaded.
- Next #30 stage B: design remote original-source inventory / metadata mapping, no automatic Google Drive writes or public catalog upload until explicit owner confirmation of rights/destinations. Preserve original files and distinguish potential duplicates by metadata only. #30 remains OPEN. #40 emulator quota deferred, #51 more archive/cancel cases, #85 inner invalid-file guard untested on phone, #102/#123 wait for natural operations, #133 licensing/security future TODO.

---

## CURRENT — v0.5.97/build113 VERIFIED PUBLIC SIGNED APK — PHONE QA NEXT — 2026-10-11

- Scope: PR #134 RAW_TREE explicit Kangoo/Megane model mismatch safety, both pre-destination picker and independent native worker. No model guessing from NT/X61; other archive flow unchanged. Main app source `a52a395bd4b61a178d5b821c2afee240dd4c5f15` (v0.5.97/build113).
- Exact-head PR Python Tests #644 and Android PR Check #510 PASS; main Tests #645 PASS; trusted stable-signed APK #160/run `38089732429` PASS. Original APK SHA256 `d4a410811c95a2f4a981ec2195fa7809eb98efb15e320ffae7a00e50aaea3c74`.
- PR #135 reviewed docs-only immutable promotion merged; verified publisher run `38090006087` PASS, original APK asset `629103188` + checksum asset `629103189` published at https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.97-debug. Release target exact signed app source; no rebuild/re-sign, no archives or user data touched.
- **PHONE v0.5.97 = PENDING**. User's existing installed v0.5.96/build112 passed install/retention/open document smoke. Next on phone: use compatible Renault Menu install-over to v0.5.97; confirm registered tomes and one document opens. Only if safe existing wrong-model RAW folder is available, select it from Megane raw picker and observe rejection *before* destination picker, without starting conversion. Never delete/reimport old files for QA.
- #82/#85 remain OPEN for negative phone test, #83 historical SAF path unknown. #40 emulator timeout still not run (no computer). #51 broader archive QA, #102 notification update, #123 per-volume reports only next genuine batch. #133 Google licensing/protected docs future TODO only.

---

## Latest Renault Docs v0.5.97 — 2026-10-11

Main app SHA `a52a395bd4b61a178d5b821c2afee240dd4c5f15`: explicit model conflicts blocked for manual RAW_TREE folders at both UI/picker and native worker boundaries; original archive guards untouched. PR #134 merged, Tests PR #644 / Android PR #510 / main Tests #645 / trusted signed APK #160 ALL PASS. Signed run `38089732429`, artifact `11682944077`, APK SHA `d4a410811c95a2f4a981ec2195fa7809eb98efb15e320ffae7a00e50aaea3c74`. Verified release promotion in separate docs-only branch PENDING until publisher evidence. Phone v0.5.96 install/retention/open document PASS; v0.5.97 device QA PENDING. Do not delete/reimport existing tomes. #40 emulator timeout test deferred; #133 license TODO later. Canonical `docs/assistant-kit/CURRENT_PLAN.md` and `docs/v.0.5.97/`.

---

## Latest Renault Docs v0.5.96 — 2026-10-10

Published original stable-signed debug APK v0.5.96/build112 https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.96-debug, app SHA `8f1d76dbb77881a59767156a0ee6996366f4dabb`, APK SHA `52086108bb68fd735f78e986076f789e948d9d32821b9b58063d57bbe312968b`. All 7 dataSync FGS now have onTimeout guard; source/Android CI PASS but real Android emulator shortened quota callback **NOT RUN**. Previous NT8445 ordinary 2-min lock/background phone QA PASS, issue #40 remains open. Docs/v.0.5.96/qa/EMULATOR_TIMEOUT_PROTOCOL.md. Never force-stop or reimport tomes just for testing.

---

## Latest Renault Docs v0.5.95 debug release — 2026-10-10

Verified signed public APK v0.5.95/build111 https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.95-debug, SHA256 `c6cfc4b30c040eb81d3efdae5b1e27ce39e9e8f12e84ea86928d96164c548f5e`, main source `2a25b1b5ae885584262e8fb8b152f72356213a3c`. Fixes #123 per-tome batch diagnostics for **future** runs, old successful 3-tome batch explicitly missing original per-tome data. Phone QA pending. Next user install in place, check same 20 tomes and legacy report note (no reimport). #40/#51/#102 still open. Start docs/assistant-kit/CURRENT_PLAN.md and docs/v.0.5.95/qa/PHONE_TEST.md.

---

## PHONE QA ACCEPTED — v0.5.94/build110 nested ZIP batch — 2026-10-10

- Real user's original `Documents/Renault/Megane II/Megane IIx.zip` (147,316,000 bytes; outer ZIP containing 3 compressed child ZIPs, each folder/index.html) was processed **successfully**.
- Phone screenshots: chooser found 3 distinct sources NT8228A · 2003-11-17, NT8266, NT8274; running "Том 1/3 · Готую дані..." sections 46/330; green terminal "Готово · створено томів: 3" with three ✓; Megane II registered tomes **17 → 20**.
- Read-only native report `COMPLETE`, `ARCHIVE_FILE`, source original SAF URI in Documents/Renault/Megane II, destination `Documents/Renault/packages/rdpkg`, started 2026-10-10 16:24:33 and finished 16:30:25 (~5m52s). No user files were altered by QA assistant.
- Scoped nested ZIP bug **#118 CLOSED / PHONE PASS**. Broader archive-intake #51 stays OPEN for ZIP/7Z/RAR combinations, duplicate/cancel/partial-failure QA; background lock #40 stays OPEN (the user did not show locked-screen progress). Notification upgrade history #102 also remains OPEN until future upgrade test.
- New reporting issue **#123 OPEN**: batch `completeArchiveBatch` intentionally records `processed.last().imported.packageId` as sole scalar ID, uses `sha256=""` when `processed.size>1` and prints `"3 томів"`. This yields misleading last-only `Package ID: megane-ii-nt8274`, empty SHA-256 and ungrammatical count in read-only report, *without implying corruption of the 3 successful packages*. Next code work should persist/display a bounded ordered list of each completed package ID/checksum and correct wording, backward-compatible and isolated from native pipeline. Do **not** reimport/delete tomes to test.
- Public signed release remains v0.5.94/build110 https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.94-debug. Don't start any automatic new work just to verify this.

---

## Latest Renault Docs v0.5.94 — 2026-10-10

Stable-signed public debug v0.5.94/build110 https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.94-debug; exact main app SHA `86fd713ba03f307579f86204f5b525faf0658827`, APK SHA `23cb2ffef2e178606674ec279711825e2614688d7762014d07d37c7bd1bcacd2`. Supports one-level outer ZIP containing three child Renault ZIPs in private staging and same chooser; phone QA PENDING for original Megane IIx.zip. #118/#51/#40 open, #102 future upgrade-notification recovery open. See `docs/v.0.5.94/qa/PHONE_TEST.md` and `docs/assistant-kit/CURRENT_PLAN.md`. Never uninstall/clear/reimport installed tomes to force tests.

---

## Renault Docs resume — 2026-10-10

Latest stable-signed public debug release v0.5.92/build108: https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.92-debug. app SHA `3e5d60aea828a7a586044a4ff197245a8b97e91d`, APK sha256 `1c9d71639aeeedc518eb27910b16416428a39359ebb7b8eb1af51c3696859d17`. Batch success notifications and cancel in native IMPORTING improved. **Phone QA for multiple raw roots ZIP/7Z/RAR and background lock remains pending**; issues #51/#40 open. Start `docs/assistant-kit/CURRENT_PLAN.md` and `docs/v.0.5.92/qa/PHONE_TEST.md`. Never uninstall/clear user data or duplicate imports solely to test.

---

## Renault Docs current batch/background work — 2026-10-10

After user-confirmed successful v0.5.91 progress and 2 retained result notifications, next stage is #51 multi-volume ZIP/7Z/RAR and #40 screen lock/background lifecycle. Branch `feat/v0.5.92-batch-background-safety` contains safer native late cancel and early per-volume batch success notifications, with tests and QA plan. No checked APK or phone acceptance yet. Start at `docs/assistant-kit/CURRENT_PLAN.md` and `docs/v.0.5.92/`. Do not delete/reimport installed tomes.

---

## Renault Docs handoff — 2026-10-09

Latest public verified debug APK v0.5.91/build107 (trusted stable signed run #154, SHA `1649e9da4dae3c03d2a86936568c7cf97f966389eaa6e0e61a066b10bbbf84a6`): https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.91-debug. Updated stage text line height and terminal SHA, atomic removal of finished foreground notification. Phone QA PENDING, issues #100/#102 OPEN. Review `docs/assistant-kit/CURRENT_PLAN.md` and `docs/v.0.5.91/` before resuming. No uninstall, clear data, or duplicate reimport for testing.

---

## Newest Renault Docs v0.5.91 working branch — 2026-10-09

User's v0.5.90 phone QA revealed stage text baseline bottom clipped, long SHA, and stale `Перевірка .rdpkg` notification after successful NT8299A native packaging (Megane II 15 tomes). Branch `fix/v0.5.91-terminal-text-notification-cleanup` includes font-safe status row/visual SHA wrapping and foreground lifecycle cleanup + accent, pending CI/release/phone PASS. See `docs/assistant-kit/CURRENT_PLAN.md` and `docs/v.0.5.91/`. Do not touch files or delete data.

---

## Latest Renault Docs resume — 2026-10-09

v0.5.90/build106 is public trusted stable-signed debug release from app source `eded18d7...`, run #153 and publisher `37979276172` PASS; https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.90-debug. Real phone QA is PENDING for progress stage termination/green busy/notification results; start at `docs/assistant-kit/CURRENT_PLAN.md`, `CURRENT_HANDOFF.md` and `docs/v.0.5.90/`. No uninstall/clear/forced duplicate imports. #100/#102 OPEN.

---
## Current repo head handoff — 2026-10-09

v0.5.89/build105 shared progress + last-ten notification history have merged to main: lower PR #101 merge `25e4c7b6...`, upper PR #103 merge `bfdc3a7e...`. Exact-head PR Python/Android CI PASS, but trusted main-signed APK and phone QA unverified. Do NOT install ephemeral PR APK; see `docs/assistant-kit/CURRENT_PLAN.md`. Pending issues #100 and #102; earlier v0.5.87 NT8298A archive regression phone PASS unaffected.

---

## Stacked feature in progress — 2026-10-09

- v0.5.89/build105 notification result history, issue #102, branch `feat/v0.5.89-completed-notification-history`. Stacked on v0.5.88 PR #101 (shared progress). This is source development, **not merged/signed/phone QA accepted**. Resume at `docs/assistant-kit/CURRENT_PLAN.md` and `docs/v.0.5.89/`; do not discard lower PR or promise prior notification recovery.

---

## Work in progress — v0.5.88 unified progress (2026-10-09)

Feature branch: `feat/v0.5.88-unified-progress-ui`. The user approved moving #100 from record-only to implementation across all progress surfaces. Consult `docs/assistant-kit/CURRENT_PLAN.md` and `docs/v.0.5.88/`; source changes are **not yet CI/phone accepted**. Installed baseline v0.5.87 NT8298A test was successful.

---

# Renault Docs — Assistant Context Index

Це канонічна карта контексту для нової сесії/асистента.

Мета: проєкт повинен відновлюватися з репозиторію без залежності від старого чату.

## Поточний checkpoint — 2026-10-08 v0.5.80

Найперше прочитати `docs/assistant-kit/SESSION_CHECKPOINT_2026-10-08_v0.5.80.md`: новий archive source guard / all-project NT lookup; MAIN CI PASS, signed APK #143, phone QA NEXT; не закривати реліз достроково.

## Останній checkpoint / відновлення — 2026-10-08

Почніть із `docs/assistant-kit/SESSION_CHECKPOINT_2026-10-08_v0.5.77.md` — v0.5.77 / build 93 PHONE PASS 6/6 / CLOSED.
Далі читайте канонічний `docs/assistant-kit/CURRENT_PLAN.md` та `CURRENT_HANDOFF.md`.
На паузі: наступний етап — **обговорення** вікна статусу; не починайте зміни коду до вимог.

## Перед змінами коду

1. Прочитати `docs/assistant-kit/CURRENT_PLAN.md` і взяти його **Поточний наступний крок** як default resume point.
2. Прочитати `CURRENT_HANDOFF.md` для ширшого історичного контексту.
3. Прочитати цей файл.
4. Прочитати всі шляхи з `docs/assistant-kit/CONTEXT_FILES.txt`.
5. Перевірити живу Git-гілку та HEAD.
6. Подивитися точні файли, яких стосується задача.
7. Запустити релевантні тести/аудити перед merge.

## Джерела істини

Коли джерела суперечать одне одному:

1. реальна поведінка на телефоні;
2. перевірений signed build / CI evidence;
3. поточний код GitHub;
4. поточні contracts/status docs;
5. історичні release docs;
6. пам'ять старого чату.

## Основні контракти

- `docs/assistant-kit/SYSTEM_BEHAVIOR_CONTRACT.md`
- `docs/assistant-kit/UI_CONTRACT.md`
- `docs/assistant-kit/APK_BUILD_CONTRACT.md`
- `docs/assistant-kit/RELEASE_DOCUMENTATION_CONTRACT.md`
- `docs/assistant-kit/TEST_DIAGRAM_STANDARD.md`
- `docs/assistant-kit/RESPONSE_CONTRACT.md`

## Dataset / library

- `docs/architecture.md`
- `docs/ua/LIBRARY.md`
- `core/dataset_manifest.py`
- `core/library.py`

## Головний принцип

Конвертер і datasets є окремими від Android UI.

Android-застосунок — універсальна бібліотека/viewer, а не Laguna-specific програма.


## Живий план / crash recovery

`docs/assistant-kit/CURRENT_PLAN.md` — єдиний canonical mutable TODO/checkpoint.

`ACTIVE_PLAN.md` у корені — лише compatibility pointer і не має дублювати checklist.

- перед багатокроковою feature/release/research/QA роботою оновити CURRENT_PLAN;
- після кожного успішного кроку одразу відмітити лише evidence-backed checkbox;
- після phone PASS/FAIL разом оновити CURRENT_PLAN + QA/finding/handoff;
- `Поточний наступний крок` — єдина default точка продовження після втрати чату;
- не відмічати крок завершеним лише через code/CI без потрібного phone evidence;
- при зміні scope переписати майбутні невиконані кроки, а не залишати застарілий план.

Чисті консультаційні відповіді без зміни стану проєкту не потребують окремого commit.
