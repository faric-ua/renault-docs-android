# Renault Docs — session checkpoint v0.5.81 / build 97 — 2026-10-08

## Current release
- **Merged / main CI PASS / signed APK ready / phone QA pending (NOT CLOSED).**
- PR #87: https://github.com/faric-ua/renault-docs-android/pull/87
- App source / merge SHA: `309f068bbf92c65ca06c0cc2e38887d6a4664837`.
- Main Tests **#568 PASS**; Android Debug APK **#144 PASS** (stable signing).
- Artifact `Renault-Docs-v0.5.81-Debug`, ID **11560786817**, sha256 **5711b9b6181edd4edaa53a575bc23fa13186a68aea925544de71b2b78b1afe79**; expires **2026-10-11 15:33:11 UTC**.
- Install **over existing** via Renault Termux Menu `5 → 19 → 8 → 13`. Do not uninstall, clear app data or move/delete source archives/packages/installed volumes.

## Exactly what changed
- Audited **20 app-owned Android AlertDialogs** in ProjectActivity (10), HomeProjectDialogController (4), SettingsActivity (3), LifecycleHelpDialogController (1), ViewerActivity (2). Previously Viewer 2 omitted shared styling; now all 20 use existing `DialogUi` and semantic roles. Android SAF/system dialogs are out of scope.
- Archive preflight now short by default: source file, destination project, possible NT duplicate(s) + metadata caveat. A tappable `Технічні деталі` expands full provider, Document ID and all registered-project matches without adding a third decision button. Expansion state survives rotation.
- Archive source selection/URI identity and Cancel/Continue callbacks preserved; no automatic conversion or data mutation.
- New Python contract checks and full audit docs: `docs/v.0.5.81/DIALOG_AUDIT.md`, `docs/v.0.5.81/qa/PHONE_TEST.md`.

## Phone evidence from predecessor v0.5.80 (do not treat as v0.5.81 acceptance)
1. Prepared `.rdpkg` files greyed out/unselectable in archive-source picker — PASS.
2. Original Kangoo II ZIP selected under Megane II blocked by "Неправильне джерело" — PASS.
3. NT8341A original Megane II ZIP from `Documents/Renault/Megane II/Backup` shows a current-project installed NT8341A matching metadata — PARTIAL PASS. No separately confirmed other-project match; matching NT != byte identity.
4. Source confirmation persisted through portrait/landscape rotation — PASS.
5. **Cancel/no-run after preview still not explicitly confirmed**. Continue/new conversion, user-file preservation edge cases, generated-marker ZIP guard remain pending.

## Phone QA progress — v0.5.81

- 2026-10-08: user replied `++` for short confirmation and explicitly `Пасс` when asked whether opened technical details survived rotation and Cancel launched no conversion. **PASS for rotation + Cancel/no-execution.** Do not automatically mark provenance text readability, preserved prior diagnostic, source reimport, or other app-owned dialogs PASS.
- Next isolated check: Viewer `Розділи` dialog chrome + close/restore, then Home/Settings/Help and remaining release gates. Phone QA overall **IN PROGRESS, NOT CLOSED**.

## Initial phone QA plan — v0.5.81
1. Install signed debug APK over current app; do not clear storage.
2. Megane II → create .rdpkg from raw → choose original Megane II NT8341A ZIP (read-only SAF selection) → inspect **compact duplicate summary**.
3. Tap `Технічні деталі`, verify provider + full Document ID. Rotate twice; expanded state and same archive must survive without starting conversion. Collapse.
4. Tap `Скасувати`. Confirm no destination picker, no job, previous source diagnostic unchanged.
5. Inspect other dialog chrome on Home (project delete confirmation without positive tap), Settings, Help, Viewer section navigator / frame-debug. Keep semantics; do not execute destructive actions.
6. If soft keyboard still appears beneath modal, capture screenshot as separate UX finding rather than declaring it fixed.
7. Real-phone pass or failure required before CLOSE.

## Pending issues / safety
- #85 prepared-source denial / #82 model-consistency guards candidate phone QA as in v0.5.80; do not close silently.
- #83 cannot reconstruct historical SAF picker navigation, despite persisted Kangoo `.rdpkg` URI as actual incident input; #81 original ZIP protection; #79 Home new volume route separate.
- v0.5.78 status UI full phone QA separate.
- Do not delete or move `Kangoo-II_X61_NT8486`, `Megane-II_X61_NT8486`, original ZIP files, archives or installed data without explicit user instruction.
