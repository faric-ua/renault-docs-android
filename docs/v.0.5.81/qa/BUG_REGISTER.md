# v0.5.81 — UI findings register

- **DIALOG-001** — Viewer section navigator and frame-debug dialogs lacked common `DialogUi` styling. **Implemented in PR #87; phone QA pending**.
- **DIALOG-002** — Archive preflight was text-heavy (full SAF provider/Document ID visible before short duplicate summary). **Implemented concise+expandable details in PR #87; phone QA pending**.
- **DIALOG-003 (observation)** — Soft keyboard was visible behind archive warning/confirmation on v0.5.80 screenshots. **Not confirmed as a functional regression; phone recheck pending**. No speculative global keyboard suppression patch.
- **DIALOG-004 (QA)** — On small/landscape screens verify native action labels fit without clipping and text body can scroll. **Not verified on phone**.

Preserve distinct popup behavior/selection, safety confirmation, no hidden automatic operations. More general UI contracts: `docs/assistant-kit/UI_CONTRACT.md`, `WINDOW_LIFECYCLE_CONTRACT.md`.
