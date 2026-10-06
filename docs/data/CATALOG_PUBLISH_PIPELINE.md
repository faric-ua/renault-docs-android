# Renault Docs Catalog publish pipeline

This document defines the data-only publish boundary for issue #30.

## Purpose

The public `renault-docs-catalog.json` must not be hand-edited as the normal production workflow.

The catalog is generated from:

1. already prepared and validated canonical `.rdpkg` files;
2. an explicit publish plan containing the Google Drive file ID for each selected package;
3. package metadata embedded inside each `.rdpkg`.

Windows-only Renault source archives are never modified by this step.

## Storage boundary

```text
Windows source archive
    read-only
       |
       v
converter / validation
       |
       v
canonical .rdpkg
       |
       +----> publish to Renault Docs Projects
       |          |
       |          +--> Drive file IDs
       |
       v
catalog publish plan
       |
       v
tools/build_drive_catalog.py
       |
       v
renault-docs-catalog.json
```

The source archive and `Renault Docs Projects` remain separate storage domains.

## Publish plan v1

Example:

```json
{
  "schema_version": 1,
  "catalog_id": "renault-docs-public",
  "catalog_version": 3,
  "generated_at": "2026-10-06T20:00:00+03:00",
  "packages": [
    {
      "file_name": "Laguna-II_X74_NT8183A_Visu-v1.0_2001-01-22.rdpkg",
      "drive_file_id": "GOOGLE_DRIVE_FILE_ID"
    }
  ]
}
```

`generated_at` is supplied by the publish plan rather than generated implicitly so identical inputs produce identical catalog JSON.

## Generator contract

Run:

```bash
python tools/build_drive_catalog.py \
  --packages-dir /storage/emulated/0/Documents/Renault/packages/rdpkg \
  --publish-plan /path/to/catalog-publish-plan.json \
  --output /path/to/renault-docs-catalog.json
```

The generator:

- opens each selected `.rdpkg` read-only;
- validates `rdpkg.json` format/schema;
- reads the embedded dataset manifest;
- resolves rich volume identity;
- rejects non-canonical package filenames;
- computes package byte size;
- computes SHA-256 from the actual package bytes;
- groups volumes by `project_id`;
- derives documentation year range;
- emits stable project/volume ordering;
- rejects missing packages, duplicate filenames and duplicate Drive IDs.

The generator does not:

- upload files;
- modify the Windows source archive;
- rename packages;
- modify existing `.rdpkg` bytes;
- write to Google Drive.

## First issue #30 publish candidate

Laguna II is the first practical publish batch because the existing prepared dataset has already passed:

- source/build volume parity 10/10;
- local-reference integrity;
- Runtime IR coverage across all 10 volumes;
- 10/10 package batch creation;
- representative package import/open checks at both the oldest and newest volume;
- final project state with 10 installed Laguna II volumes.

Known package identities:

- NT8183A · 2001-01-22
- NT8218A · 2002-05-01
- NT8236A · 2002-11-18
- NT8240A · 2003-11-17
- NT8254A · 2004-06-21
- NT8282A · 2005-04-22
- NT8283A · 2005-08-29
- NT8307A · 2005-12-12
- NT8327A · 2006-02-06
- NT8328A · 2006-05-09

Before production catalog update:

1. verify the local 10-package directory still contains the canonical migrated filenames;
2. upload those exact package bytes to `Renault Docs Projects`;
3. capture returned Drive file IDs;
4. build catalog version 3 using the generator;
5. compare generated SHA-256 values with the accepted batch evidence;
6. replace the public catalog file only after the generated manifest passes validation;
7. phone-smoke one early and one late Laguna II volume from Catalog download/import.
