# Renault Docs v0.5.62 — Drive Catalog v1

Status: **CLOSED / PHONE PASS / MAIN VERIFIED**

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

## Final closeout — 2026-10-06

PR #31 was merged to `main`.

Final main runtime merge:
`2fe05f18618718e5ef16521ad0411734fb1b89f5`.

Final main CI:
- Tests #455 — PASS;
- Android Debug APK #123 — PASS.

Final main artifact:
- `Renault-Docs-v0.5.62-Debug`;
- SHA-256: `939f69b3dd70ab5043304286f132b6c9151f694133d3de7169fc82079f609de9`.

Final phone install-over-existing:
- `v0.5.62 / build 78` confirmed;
- existing data preserved;
- Megane II remains at 2 volumes;
- NT8340A · 2006-04-18 opens successfully;
- NT8342A remains present.

Issue #29 is complete.

Next:
continue issue #30 Windows-source intake/conversion and expand the catalog with newly validated Android-ready packages.
