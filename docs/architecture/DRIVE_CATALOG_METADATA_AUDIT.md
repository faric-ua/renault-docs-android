# #30 Stage B — read-only Google Drive catalog metadata reconciliation

Status: **MERGED / PR Tests #649 PASS / main Tests #650 PASS**. Separate Drive collision review #143 OPEN; no data cleanup authorized.

## Ground truth and scope

The Windows originals are stored separately from **Renault Docs Projects**, which contains **prepared `.rdpkg`** and `renault-docs-catalog.json`. Do not infer that every top-level shared-Drive folder, source name, or similar Renault NT code belongs to a single vehicle.

This tool compares **two already obtained local JSON snapshots**:

- A copy of the existing `renault-docs-catalog.json` from the prepared-catalog folder (which contains chosen Drive `drive_file_id`, canonical `file_name`, and `size_bytes` for volumes).
- A provider metadata listing of the **same prepared folder**, represented as `{"files": [{"id": "...", "title": "...", "size": "..."}, ...]}`. Real Drive `list_folder` responses can be normalized to this format. No ZIP/PDF payload is needed.

A trusted owner can run from repository root:

```bash
python tools/audit_drive_catalog_metadata.py \
  --catalog /private/path/renault-docs-catalog.json \
  --listing /private/path/prepared-folder-listing.json
```

The optional `--complete-listing` must only be used if the supplied snapshot demonstrably includes **all immediate children** of the prepared folder. A capped or partial folder listing must not be declared complete. Without that flag, absence from the listing is reported as *unverified*, not necessarily missing on Drive.

**Important:** these are explicit local JSON inputs. The tool does not connect to Google Drive, acquire OAuth tokens, download files, scan the user's phone, or modify Drive permissions. It prints sanitized JSON **to stdout only**; it never writes new files or backup checkpoints. Source input files and original Windows ZIP/7Z/RAR are untouched.

## Checks

- Matching is by **immutable Drive file ID** first, not by title. Two Drive files can have the same canonical name and different content.
- Compare each referenced prepared file's `file_name` and `size_bytes` to current Drive metadata. Different names/sizes are errors.
- Warn on duplicate prepared package basenames; show size options and count of catalog-referenced copies, without exposing Drive IDs or URLs.
- Mark any unreferenced prepared `.rdpkg` for **human review**, not automatic deletion.
- Flag any raw source ZIP/7Z/RAR in the **prepared-catalog** folder as an inappropriate raw-source location.
- Do not confuse same NT/date or same basename with byte-level identity. Catalog SHA-256 is **not verified** by metadata-only scans; that requires separately authorized content hashing.
- Omit provider IDs, secret tokens, source URLs, Drive permissions and full account path from stdout/committed docs.

This is an **audit**, not a manifest publisher: it will not rewrite `renault-docs-catalog.json`, modify packages, select winners, drop duplicate entries, or change Drive permissions.

## Real metadata observation — 2026-10-11

A metadata-only inspection of the prepared Drive folder found 17 `.rdpkg` and one catalog JSON. The existing catalog has 15 volume references; two older `.rdpkg` copies have **identical basenames but different sizes** compared with newer catalog-referenced items (NT8340A and NT8342A). The catalog's references point to newer smaller files. **This is not a corruption determination or deletion authorization.** These copy pairs must remain untouched until content checks, recovery usefulness and rights are understood. The original Kangoo II NT8486 ZIP was separately verified in its source folder; no original file bytes were fetched.

## Later phases

1. Drive source-tree inventory with explicit folder allowlist and server-side pagination/completeness (no root-wide crawl).
2. Owner-approved staging/copy of one original into private workspace only, source immutability confirmed.
3. Converter parity, package metadata and actual SHA-256 validation before any catalog changes.
4. Explicit rights, distribution and hosting policy review before publishing documentation.
5. Review changes with a diff, never silently delete/replace an old original or prepared Drive item.

The technical preflight does not authorize monetization or public redistribution of third-party documentation. This work does not change the installed Android v0.5.97 APK.
