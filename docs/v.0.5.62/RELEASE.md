# Renault Docs v0.5.62 — Drive Catalog v1

Status: **PHONE PASS / CI PASS / CLOSEOUT PENDING**

## Goal

Replace raw Google Drive browsing with an in-app Renault Docs catalog that shows human-readable projects and volumes and can import selected Android-ready .rdpkg packages directly.

## Initial scope

- public catalog manifest `renault-docs-catalog.json`;
- in-app catalog grouped by Renault project/model;
- vehicle codes + documentation year range;
- per-volume code/date/type/version/region;
- installed-state detection;
- checkbox selection;
- foreground download/import flow;
- reuse existing `.rdpkg` validation/install path;
- add imported volume to the correct Renault project.

## Storage contract

- Windows-only originals remain on a separate source drive;
- Renault Docs Projects contains only Android/runtime-ready artifacts + catalog metadata;
- app users do not browse physical package filenames in normal catalog UX.

## First live catalog

Megane II:
- NT8340A · 2006-04-18 · Visu v3.0
- NT8342A · 2006-10-09 · Visu v3.0 · Europe

The first phone test will remove one existing volume from the project and import it back from the public catalog.


## Accepted phone result — 2026-10-06

Exact accepted runtime head:
`e215ed9a29ccf7cd7e36d083e2579e24bd2a9c3f`

CI:
- Tests #451 — PASS;
- Android PR Check #369 — PASS.

Accepted:
- native Catalog discovery;
- installed-state rendering;
- NT8340A round-trip import from public Google Drive;
- existing .rdpkg validation/install path reused;
- Megane II restored to 2 volumes;
- NT8340A reopened successfully;
- optional null metadata omitted;
- selection + terminal state survive rotation;
- already-loaded catalog is retained during configuration change so project cards/checkboxes restore promptly.

Next:
close out PR #31. Do not repeat the accepted phone tests unless runtime code changes.
