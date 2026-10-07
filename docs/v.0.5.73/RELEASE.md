# Renault Docs v0.5.73 — compact Add panel

Status: **DEVELOPMENT**

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
