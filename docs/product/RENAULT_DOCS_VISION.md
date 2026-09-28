# Renault Docs — Product Vision and Roadmap

Status: living project document.

This file records product decisions that must not live only in chat.

## 1. PDF viewer

Current direction:
- manual zoom field;
- quick zoom presets: 85%, 100%, 120%, 150%, 200%;
- +/- zoom controls;
- dedicated fit-to-width action;
- PDF Save keeps exporting the original PDF bytes;
- Fast Pack remains independent from PDF storage.

Planned settings:
- default PDF zoom;
- remember last zoom;
- remember last page;
- configurable +/- step;
- fit-width as default option.

## 2. Conversion and original-source lifecycle

The converter must use a safe staged workflow.

### Required sequence

1. Select source and destination.
2. Validate source and write permissions.
3. Convert into a staging/output location.
4. Build Modern index and Fast Pack.
5. Validate the resulting dataset.
6. Create a backup archive of the original source.
7. Write backup metadata and SHA-256.
8. Verify that the backup archive can be read.
9. Only after successful verification, offer removal of the original source.
10. Register the converted dataset in the library.

### Critical safety rule

The original source folder must never be deleted when:
- conversion failed;
- output validation failed;
- backup creation failed;
- backup verification failed;
- user cancelled final deletion confirmation.

### Default option

A conversion option should be enabled by default:

> Create verified backup of the original source and remove the old source after successful conversion.

The deletion itself still requires a final explicit confirmation after the verified backup exists.

### Confirmation wording

Suggested message:

> The original Renault folder is no longer required by Renault Docs. The converted dataset uses normalized paths, Modern index and Fast Pack. The old Windows-oriented structure may behave incorrectly on Android/Linux.
>
> A verified backup archive has already been created.
>
> Delete the original source folder?

Actions:
- Keep original
- Delete original

## 3. Backup Center

Default local backup root:

`Documents/Renault/backups/`

Each backup should contain:
- original archive;
- SHA-256 file;
- backup metadata JSON;
- source dataset name;
- original size;
- archive size;
- backup date/time;
- Renault Docs app version;
- converter version/schema;
- restore compatibility information.

Backup Center actions:
- Verify;
- Restore;
- Show location;
- Delete backup.

## 4. Settings

Add a dedicated Settings screen from the main library.

Suggested sections:

### General
- interface language;
- theme;
- default Modern/Classic mode;
- startup behavior.

### PDF
- default zoom;
- remember zoom;
- remember page;
- +/- zoom step;
- fit-width behavior.

### Storage and cache
- dataset folder;
- backup folder;
- Fast Pack cache size;
- clear cache;
- auto-build/refresh Fast Pack;
- archive original after conversion.

### Cloud
- account status;
- backup over Wi-Fi only;
- manual backup now;
- restore from cloud;
- cloud cache policy.

### About
- app purpose;
- version name;
- version code/build;
- project/repository;
- dataset schema;
- Fast Pack schema;
- licenses.

## 5. Localization

UI localization should use Android resources rather than hard-coded language conditions.

Initial target languages:
- Ukrainian;
- English;
- Czech;
- System language.

Renault source documentation itself remains in its original language. Only the app UI is localized.

## 6. Google account and Google Drive

Cloud support must not regress performance by issuing thousands of individual remote requests.

### Architecture rule

Renault Docs should access data through a storage abstraction instead of directly assuming local SAF.

Suggested providers:
- Local SAF;
- Google Drive;
- future providers if needed.

### Google Drive behavior

For a Drive-backed dataset:
- manifest is cached locally;
- Modern index is cached locally;
- Fast Pack is cached locally;
- PDFs can be downloaded on demand and cached;
- large archives/images are not automatically uploaded without explicit user choice.

### Cloud backup scope

Small state:
- settings;
- library metadata;
- favorites;
- last opened pages;
- last zoom;
- history.

Large state:
- datasets;
- original backup archives;
- disc images/ISO files;
- optional large assets.

Large data backup must be opt-in and show estimated size.

## 7. Library improvements

Future library features:
- global search across all datasets;
- recent documents;
- favorites;
- dataset integrity check;
- cache management;
- per-dataset overflow menu;
- remove from library without deleting files;
- restore state on a new phone;
- import/export Renault Docs app state.

## 8. Roadmap

### v0.3.x — PDF and performance polish
- Fast Pack performance;
- chronological volume ordering;
- PDF zoom;
- presets;
- fit width;
- PDF Save.

### v0.4.x — Settings foundation
- Settings activity;
- About;
- localization foundation;
- PDF defaults;
- storage/cache preferences;
- storage-provider abstraction foundation.

### v0.5.x — Converter 2.0 and Backup Center
- actual Android conversion writer;
- staging;
- validation;
- archive original;
- SHA-256;
- verified backup;
- final deletion confirmation;
- restore flow.

### v0.6.x — Cloud backup
- Google sign-in;
- settings/state backup;
- backup archives;
- optional dataset backup.

### v0.7.x — Cloud library
- Google Drive-backed datasets;
- local manifest/index/Fast Pack cache;
- PDF on-demand caching;
- offline-aware behavior.

## 9. Project artifact rule

Important product and technical decisions must be written into the repository.

Chat is not the source of truth.

Persist:
- architecture decisions;
- release findings;
- phone QA results;
- UX contracts;
- lifecycle contracts;
- roadmaps;
- design diagrams;
- backup/restore contracts;
- cloud/storage contracts.

Prefer editable source formats for diagrams:
- SVG;
- Mermaid;
- PlantUML;
- HTML.

Raster PNG/JPEG should be treated as derived preview assets, not as the canonical source when text accuracy matters.
