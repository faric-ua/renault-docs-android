# Renault Docs v0.5.73 — compact Add panel

Status: **MERGED / MAIN CI PASS / PHONE QA PENDING**

## Goal

Reclaim Project-screen vertical space without changing any add/import workflow.

## UX contract

- one full-width `Додати` header tile;
- unpinned default: collapsed;
- header controls: pin, expand/collapse, overall Help;
- expanded body contains the existing Auto, Manual, raw→.rdpkg and archive→.rdpkg actions;
- existing raw/archive Help buttons remain with their actions;
- pin persists expanded state across page reopen;
- rotation preserves transient expanded/collapsed state;
- volume tap/long-press guidance lives inside expanded Add content;
- active operation/progress status remains outside the collapsible body and stays visible.

GitHub issue: #68.


## CI evidence

- PR #70 merged.
- Runtime source: `871d93c6f0416f9b98ea78edbab06a9ddcce8eba`.
- Tests #538: PASS.
- Android Debug APK #136: PASS.
- Artifact: `Renault-Docs-v0.5.73-Debug`.
- Artifact ID: `11517210871`.
- Digest: `sha256:027f0603e84868e883cc826f8706394f3c40106135bbaa87c97e32a63094f36b`.

Phone QA remains required.
