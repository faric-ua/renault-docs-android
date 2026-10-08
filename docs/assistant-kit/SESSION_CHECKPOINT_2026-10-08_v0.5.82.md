# Renault Docs — session checkpoint v0.5.82 / build 98 — 2026-10-08

## Verified source / build

- **PR #88 MERGED / MAIN CI PASS / STABLE-SIGNED APK READY / CORE HOME PHONE PASS / EXTENDED QA OPEN (NOT CLOSED)**.
- PR: https://github.com/faric-ua/renault-docs-android/pull/88
- Runtime application source/merge SHA: `f17b319c4e17d8f7005ae20221dd7bd642cb172b`.
- PR Python Tests **#571 PASS**; Android PR Check **#456 PASS**.
- Main Python Tests **#572 PASS**; Android Debug APK **#145 PASS**, stable signing.
- Artifact `Renault-Docs-v0.5.82-Debug` ID **11563839781**, sha256 **94cadb5caf5b1af93a2a959e674819c31f0571b479c66f7b9a492af6ae6586be**; expires **2026-10-11 16:39:51 UTC**.
- Install **over existing** app through Renault Termux menu `5 → 19 → 8 → 13`; do NOT uninstall, clear data or move/delete ZIP, .rdpkg, installed tomes.

## Scope shipped in candidate

1. Global orientation rule for **app-owned** windows. In landscape hide `WindowInsetsCompat.Type.systemBars()` (Android status+nav) via existing `Ui.applyOrientationSystemBars`; in portrait explicitly show them. Allow transient swipe reveal. No IME/keyboard manipulation, and external SAF file picker remains Android-owned. Existing RenaultDocsApplication lifecycle handlers extended to DialogUi's tracked AlertDialog windows; Viewer and ModernVolume (configChanges) explicitly reapply. Preserve explicit PDF fullscreen.
2. Home layout parity with Project `Додати`: fixed heading, 📌 original system emoji full color when pinned in portrait / grayscale inactive in landscape, ▲/▼ and Help; separate persisted Home pin `homeAddPanelPinned` (not Project preference); collapsed-by-default landscape actions with transient manual expansion; restore portrait state when rotating back.
3. Bounded vertically scrollable Add body; Main's My Renault project tiles scroll **independently** below fixed Add. Move `Новий том`, `Новий проєкт`, `Готові проєкти` (Google Drive), explanatory project/volume message, `Інструменти` (Converter, Legacy) and any local legacy library dataset tiles into Add. Do not remove their existing navigation.
4. Project .rdproject share/progress/cancel/result `OperationStatusView` remains inside fixed Add **outside collapsible actions**, so a collapsed panel does not hide active progress.
5. Auto contract tests `tests/test_v0582_home_add_and_system_bars_contract.py`; changed old Home scroll/status test expectations because the user explicitly requested new behavior. Retained Project Add implementation unchanged.
6. Reusable system contracts updated: `docs/assistant-kit/UI_CONTRACT.md`, `docs/assistant-kit/WINDOW_LIFECYCLE_CONTRACT.md`. Full release QA `docs/v.0.5.82/qa/PHONE_TEST.md`.

## Historical screenshots / unfinished predecessor checks

- User screenshots v0.5.81 show compact and expanded duplicate preflight `NT8341A`, project library counters Megane II 10, Laguna II 10, Kangoo II 1; Modern sections catalog and keyboard in both orientations.
- v0.5.81 user explicitly phone-accepted archive preflight detail expansion+rotation and Cancel/no conversion.
- **Do not mark Viewer `Розділи` modal PASS**: the supplied screenshots show a Modern native section search/list, not necessarily the Viewer section navigator AlertDialog; no confirmed modal QA.
- V0.5.80 global duplicate across *different* project not independently observed; current-project NT8341A metadata match only. Matching NT/date != archive hash.

## Core phone evidence — 2026-10-08

User replied «Пасс. Взагалі все чудово.» to the Home/Add + 📌 portrait→landscape→portrait scenario, including automatic hiding/restoring of Android system bars. **Core feature PHONE PASS**. No v0.5.82 screenshots or explicit acceptance of all Help/Viewer/Settings/SAF/independent-scroll/legacy routes, so retain separate QA items. No need to reinstall for follow-ups.

## Original and extended phone QA checklist

1. Home portrait: only `Додати` + `Мої Renault` blocks outside Add. Confirm Megane/Laguna/Kangoo counters unchanged. Expand Add; check New Volume/Project, Ready Projects, description, Converter/Legacy.
2. Pin Home Add portrait 📌. Rotate landscape: status and nav bars hidden, pin appears disabled/desaturated, Add actions auto-collapse. Tap ▲/▼ to temporarily expand and scroll within Add without losing My Renault list. Rotate portrait: original pin and expansion back, Android bars visible again.
3. Open a Help/confirmation dialog in landscape, ensure Android status/nav hide there too, portrait restore. Avoid performing positive/destructive actions.
4. Optionally verify Settings/Viewer/ModernVolume landscape/portrait and PDF fullscreen; **do not manipulate system keyboard UI or SAF picker**.
5. Status/diagnostics: operationStatus remains reachable independent of collapsed Add. Do not launch arbitrary native conversion or delete prepared packages for QA.

Status: core Home Add + orientation phone **PASS** (2026-10-08), extended QA **OPEN**, not closed. No source/archive backup, rollback, cleanup or migration performed in v0.5.82.

## Pending unrelated

- v0.5.81 separate Viewer/dialog phone checks remain open; #79 Home `Новий том` picker routing (next separate change if still relevant), #81 original file preservation, #82/#85 safe intake candidate QA, #83 historical picker trail uncertainty, other unclosed older gates. Do not infer all issues closed from merged UI code.
