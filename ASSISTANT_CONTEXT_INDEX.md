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
