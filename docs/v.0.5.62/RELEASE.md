# Renault Docs v0.5.62 — Drive Catalog v1

Status: **DEVELOPMENT**

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
