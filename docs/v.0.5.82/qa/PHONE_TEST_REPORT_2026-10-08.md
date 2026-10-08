# Renault Docs v0.5.82 — Phone QA report — 2026-10-08

## Environment and build
- User reports testing installed Renault Docs v0.5.82 / build98 after signed APK #145 was provided.
- Runtime source SHA: `f17b319c4e17d8f7005ae20221dd7bd642cb172b`; APK #145 main CI PASS.
- User confirmation: **«Пасс. Взагалі все чудово.»**
- Evidence type: user-reported phone acceptance following explicit Home portrait→landscape→portrait test instructions. No new screenshot submitted in the acceptance message.

## Accepted core scenario (PASS)
- Home has the collapsible/pinnable Add panel and My Renault beneath it.
- Add expanded and portrait 📌 pinned; in landscape system status/navigation bars hide and Add collapses; on return to portrait system bars and previous pin state return.
- No defects were reported for this tested scenario.

## Extended phone acceptance — test 2 (2026-10-08)
- User: «Все добре, пас, поїхали далі.» after verifying expanded Home Add contains `Новий том`, `Новий проєкт`, `Готові проєкти`, `Конвертер`, `Legacy` and that Add contents and `Мої Renault` list scroll independently.
- **PASS:** visual action availability and independent scrolling. **Not tested:** actual action execution, status during active work, data changes.

## Extended phone acceptance — test 3 Home Help (2026-10-08)
- User replied «Пасс» after checking Home `Додати` → `?` Help window across portrait→landscape→portrait. Window remains in place, system bars hide/restore, Close returns Home and does not start operations. **PASS for this specific Help modal.**

## Extended phone acceptance — test 4 Settings (2026-10-08)
- User replied «Пасс» following Settings portrait↔landscape bar hide/restore, a non-destructive choice dialog surviving rotation, and closing without saving changes. **PASS** for Settings + that selection window.

## Test 6 — Native Modern section orientation (2026-10-09)

Three new phone screenshots and explicit user acceptance: Megane II / NT8340A / section `120 — ЭБУ СИСТ. ВПР.`. In portrait, full category controls `Схеми`, `Роз’єм`, `Положення на авто`, `Документація` are visible. In landscape only the currently selected `Схеми` content/list occupies the view. Portrait restores additional navigation while retaining selected category and section. The user confirms this is the intended behavior. **PHONE PASS: category-aware adaptive orientation and section continuity.** Not evidence of all per-category native data actions.

## Not independently evidenced
- Other app-owned dialogs/transient swipe and precise post-fullscreen system bar restoration; native Modern section orientation and Classic Viewer PDF fullscreen are now accepted.
- Actual Ready Projects/Drive, Legacy and Tools launch results; active operation progress/terminal status interactions (UI presence and independent scrolling already PASS).
- Cross-app system SAF picker behavior and IME; destructive/project import/export operations; volumes count after install and data-preservation checks.
- Original v0.5.81 Viewer `Розділи` dialog is *hybrid-mode-only*, not accessible from ordinary Modern→Classic. Do not treat test as failed or passed; the test route was wrong.

## Result
**CORE PHONE PASS / EXTENDED QA OPEN**. Do not close the whole release or unrelated issue set yet. Next isolated test: Viewer-specific `Розділи · ...` AlertDialog portrait→landscape→portrait, close returns same Viewer without navigation; then PDF fullscreen and active status scenarios.

## Viewer attempted test 5 — route corrected (2026-10-08)

Four user screenshots show NT8340 Classic `Visu Schema`, text search `120` at `0/0` in start page and `1/3` after opening the legacy document, plus inline PDF. No `Розділи · …` modal is pictured. Code audit shows this modal requires `hybridSectionMode`, whereas section `Classic` opens untouched Viewer with no hybrid extras. **INVALID QA INSTRUCTION, NO APP REGRESSION CONFIRMED**. Replacement test 5A **PASS** based on user screenshots: original Classic `⛶` fullscreen in portrait and landscape displaying the same NT8340/120 PDF; do not rework native Classic. Remaining QA: independent operation-status behavior, genuine action routing and any unrelated legacy outstanding tests.

## Test 5A — Classic PDF fullscreen accepted (2026-10-08)

Three user screenshots: portrait fullscreen (app toolbar hidden), portrait ordinary mode (app toolbar present), landscape fullscreen (app toolbar hidden); all show NT8340 / code 120 / `SE2416-P` inline PDF at `176%`. User clarifies this is **original Renault Classic** preserved with only Android compatibility. **PHONE PASS for Classic fullscreen/orientation and continuity of displayed content.** Keep original frames, drawings, controls and document flow unchanged. Precise restored Android bar state outside fullscreen and exact scroll position after rotation are not separately evidenced and stay outside this PASS.

## Next isolated QA (no source mutations)

Observe only an already-existing terminal operation/status on Home if visible while Add is collapsed and expanded. Do not start a new import, conversion, or share just to create a status. If no status currently exists, record N/A and choose another non-destructive test.
