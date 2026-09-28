# Renault Docs v0.4.0 — Settings foundation

## Purpose

Introduce the first working Settings screen and make selected preferences affect real application behavior.

## Implemented

### General
- Settings entry from the main Library screen;
- default dataset opening mode:
  - Modern;
  - Classic.

The selected mode is used when a dataset tile is opened.

### PDF
- default PDF zoom:
  - 85%;
  - fit width / 100%;
  - 120%;
  - 150%;
  - 200%;
- configurable +/- zoom step:
  - 5%;
  - 10%;
  - 20%.

The values are applied to newly opened PDF viewers.

### Backup preparation
- user can select and persist a writable SAF folder for future backups;
- the selected backup folder is stored in app settings;
- setting can be cleared.

This is infrastructure for Converter 2.0 / Backup Center. v0.4.0 does not delete source files or create original-source archives yet.

### About
- app name;
- version/build;
- current storage/runtime model;
- PDF runtime summary.

## Not included yet

- UI localization switch;
- Google authentication;
- Google Drive;
- automatic backup;
- source-folder deletion;
- remember last PDF page/zoom;
- theme switching.

Those remain later roadmap items.
