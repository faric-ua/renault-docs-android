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
