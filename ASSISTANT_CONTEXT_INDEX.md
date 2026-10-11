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
