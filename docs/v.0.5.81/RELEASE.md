# Renault Docs v0.5.81 / build 97 — unified dialog candidate

Status: **CANDIDATE / NOT MERGED / DEVICE QA PENDING**.

Scope:
- All 20 identified app-owned Android `AlertDialog` instances use existing `DialogUi` canonical colors, rounded border, title/message and button styling. Two previously unstyled Viewer dialogs now use it.
- Archive source preflight is compact by default, with full SAF provider, Document ID and registered-project candidate duplicates behind `Технічні деталі` (expand/collapse, state survives rotation).
- Identical Yes/No/Cancel semantics, same selected URI identity guard and source preservation as v0.5.80. No auto-creation and no deleted/moved files.
- Pure audit and contract tests: `docs/v.0.5.81/DIALOG_AUDIT.md`, `tests/test_v0581_unified_dialog_contract.py`.

Do **not** mark v0.5.80 phone-QA complete because of this candidate. Original `NT8341A` cancel/no-run, cross-project duplicate cases and raw/ZIP content guard still need their own evidence.

Gates: Tests + Android PR Check PASS -> controlled main merge and stable signer APK -> real-device visual and lifecycle QA (see `qa/PHONE_TEST.md`). Never reinstall from scratch or wipe app data.
