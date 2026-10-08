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

## Not independently evidenced
- Other app-owned dialogs/transient swipe, Viewer/ModernVolume and explicit PDF fullscreen (Home Help + Settings selection dialogs already PASS).
- Actual Ready Projects/Drive, Legacy and Tools launch results; active operation progress/terminal status interactions (UI presence and independent scrolling already PASS).
- Cross-app system SAF picker behavior and IME; destructive/project import/export operations; volumes count after install and data-preservation checks.
- Older v0.5.81 Viewer-specific `Розділи` modal QA remains distinct.

## Result
**CORE PHONE PASS / EXTENDED QA OPEN**. Do not close the whole release or unrelated issue set yet. Next isolated test: Viewer-specific `Розділи · ...` AlertDialog portrait→landscape→portrait, close returns same Viewer without navigation; then PDF fullscreen and active status scenarios.
