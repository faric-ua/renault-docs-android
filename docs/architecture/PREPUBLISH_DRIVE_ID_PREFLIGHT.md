# Renault Docs #30 — prepublication Drive ID and prepared package preflight

This is a **non-mutating, offline** check between locally prepared canonical `.rdpkg`, a validated explicit publish plan, and a separately supplied Google Drive metadata listing.

## Purpose

Before replacing `renault-docs-catalog.json`, ensure the owner-selected **exact** Drive file ID resolves (within a supplied snapshot) to the selected canonical filename and expected local size. When provider SHA-256 metadata is available, compare it to the locally recomputed package SHA. A matching filename alone is NEVER enough: the prepared Drive folder has historically contained two different same-named objects inside each of NT8340A and NT8342A. Those **two different NT codes are distinct Renault documents** and must remain separate.

## Command

```bash
python tools/preflight_drive_publish_plan.py \
  --packages-dir /private/path/to/prepared/rdpkg \
  --publish-plan /private/path/to/catalog-publish-plan.json \
  --drive-snapshot /private/path/to/prepared-folder-metadata.json
```

Inputs:
- `--packages-dir`: existing local, *prepared* canonical `.rdpkg` objects. No source Windows originals here.
- `--publish-plan`: existing `tools/build_drive_catalog.py` versioned publish-plan schema with `packages = [{file_name, drive_file_id}]`; the existing plan loader validates basename and uniqueness.
- `--drive-snapshot`: provider-metadata listing already obtained **separately**, `{"complete": false, "files": [{"id": "...", "title": "...", "size": "12345"}]}`. Optional per-file `sha256Checksum` or `sha256_checksum`. Do not mark `complete:true` unless the collector actually covered the entire direct folder; absence from any snapshot is an unverified selection and blocks this preflight, **not proof the file was removed from Drive**.

Report prints stdout JSON only. It intentionally **does not reveal provider file IDs or absolute local file paths**. Exit status 2 for blocking findings, 0 for a non-blocking report. **Exit 0 does not authorize publishing or prove content rights**.

## Statuses

- `BLOCKED`: missing local package, invalid manifest/canonical name, missing chosen exact Drive ID from supplied snapshot, mismatched name/size/sha, or duplicate **package identity** in selected plan. Review the named problem. Do not update catalog.
- `REVIEW_REQUIRED`: exactly selected remote item matches available metadata but there are **multiple objects sharing that same filename** in supplied Drive snapshot. Preserve all copies; owner review needed, no automatic "newer wins" or cleanup.
- `METADATA_MATCH_CONTENT_UNVERIFIED`: exact file ID/name/size match, but Drive has **no SHA-256** in listing. File content is **NOT verified**. Read-only actual package integrity/hash parity would be a separate authorized action.
- `METADATA_AND_HASH_MATCH`: SHA-256 from the provider snapshot matches local SHA-256 in addition to ID/name/size. It is still *not* proof of redistribution rights, successful app import or user acceptance.

`can_autopublish` is always false. No script herein calls Google Drive APIs, uploads/publishes, deletes, moves, renames, re-signs, rewrites catalog or touches user data. No backend, Google login or paywall is introduced before core-stable release gate #147.

## How it fits the pipeline

1. Inventory Windows originals using existing offline `inventory_windows_archives.py` / allowlisted `audit_drive_source_metadata.py` — names/size are hints, not content.
2. Privately prepare and validate real volumes, with **full distinct NT/date/applicability identities**; only intended one volume per prepared package.
3. Create canonical local `.rdpkg` and inspect its manifest and local SHA via `build_drive_catalog.inspect_package`.
4. Owner explicitly selects the already-present Drive file **ID** in a publish plan; acquire bounded **prepared-folder** metadata snapshot.
5. Run **this** non-mutating preflight; address `BLOCKED` and review collisions, independent bytes/hash check when metadata isn't sufficient.
6. Only after authorizing documentation rights, publish destination and stable core flow should a separate explicit operation update the catalog. Never silently replace or remove source or prepared files.

This tool **does not** itself perform Steps 1–4 or Step 6. Android v0.5.97/build113 remains unchanged.

## CI

Synthetic test fixtures verify real local .rdpkg format construction, exact ID with same-name collision, size/name/hash mismatches, missing ID without filename fallback, distinct NT8340A/NT8342A, duplicated package ID, invalid ZIP, CLI exit behavior and unchanged local input files. Require PR exact-head Python Tests and main Tests before marking this milestone merged.
