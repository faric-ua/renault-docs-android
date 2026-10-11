# Issue #30 — Stage D: exact allowlisted Drive source metadata snapshots

**Status:** source candidate, CI PENDING. This is *not* a Google Drive crawler, authorization system, installer or package publisher. Access/security features are strictly postponed until the separately accepted core-stable release, per `CORE_BEFORE_ACCESS_RELEASE_GATE.md` and issue #133.

## Goal and trust boundaries

Inventory only selected original Windows Renault ZIP/7Z/RAR files from *explicit* folder scopes. The owner or a future read-only connector retrieves provider listing metadata separately; this script processes already obtained local JSON files offline. The shared drive root contains unrelated files; never recursively crawl it by default.

Each folder scope must specify:
- a private `folder_id` used only to match a supplied folder listing;
- opaque public `alias`, e.g. `megane-schemes` (not a folder URI);
- explicit `project_id`: one of `megane-ii`, `kangoo-ii`, `laguna-ii`.

The target folder must be allowlisted **before** its listing can be processed. Unknown folder IDs, duplicate aliases, duplicate IDs inside one folder, or unsupported project IDs are rejected. Individual list entries have provider `id`, `title`, `size`, optionally `file_or_folder`. Child folders are **never traversed** by the tool.

Example private input files (synthetic placeholder IDs; do not commit real Drive IDs):

```json
// allowlist.json — remove this explanatory comment in the actual JSON file
{
  "schema_version": 1,
  "scopes": [
    {"alias": "megane-schemes", "folder_id": "PRIVATE_FOLDER_ID", "project_id": "megane-ii"}
  ]
}
```

```json
{
  "schema_version": 1,
  "listings": [
    {
      "folder_id": "PRIVATE_FOLDER_ID",
      "complete": true,
      "files": [
        {"id": "PRIVATE_FILE_ID", "title": "Megane II E,L,K 84_NT8340A_Visu v3.0_2006.04.18.zip", "size": "67773534", "file_or_folder": "file"}
      ]
    }
  ]
}
```

`complete=true` is **an assertion from the snapshot collector**, not something this local script can verify; use `false` for paginated, truncated or uncertain listings. A missing allowlisted folder yields a warning and never a false zero-files claim.

Usage from the existing Renault Docs repository:

```bash
python tools/audit_drive_source_metadata.py \
  --allowlist /private/allowlist.json \
  --snapshots /private/source-listings.json
```

The tool produces deterministic UTF-8 JSON **on stdout only**, never files. It excludes unrelated PDFs/app installers/other folders; reports all ZIP/7Z/RAR candidates without opening any payload. The `file_name`, `size_bytes`, **filename-only** inferred NT/date/vehicle codes/doc type/version/region, and status are included. Provider IDs, URLs and folder IDs are intentionally **absent**. No copy, upload, delete, rename, download, extraction, conversion or package building occurs.

## Renault documentation identity and application safety

The source `project_id` comes from allowlist, *not* from inferred filename code such as X61/X74. Strong explicit Kangoo/Megane/Laguna disagreement produces `explicit_cross_model_conflict`. No explicit model in a filename means `model_not_verified`; a name with matching model receives `archive_metadata_only`. None of these means valid package, verified content, or permission to convert.

**Every full NT code is independent.** `NT8340A` and `NT8342A` are two distinct Renault documents; they must never be collapsed, treated as duplicates or automatically superseded simply due to numeric proximity, year or different size. Same NT/date across filenames also does not prove content equality.

A prepared .rdpkg catalog and raw original sources are separate. No authenticated or monetized access/encryption features are implemented in this step.

## Tests and future gates

Synthetic tests cover a cross-model source rejection, two distinct NT volumes, incomplete/missing listings, strict folder allowlist/no recursion, private provider IDs absent from report, bad metadata, deterministic result and unchanged input bytes. Exact-head Python CI and main CI required for a merged tooling milestone.

Future work before any real source conversion:
1. Establish a user-approved source-folder allowlist and a privacy-safe real metadata snapshot with honest pagination/completeness.
2. Verify the original ZIP/7Z/RAR structure and content offline without modifying originals. Large multi-volume ZIP requires separate discovery, not presumed one `.rdpkg`.
3. Review existing catalog + installed tomes before creating copies; no automatic duplicate cleanup (#143).
4. Explicitly approve destination and independent redistribution rights before any public Drive catalog upload.

This document is not a release of a new Android APK.
