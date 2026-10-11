# #30 — Windows-source archive inventory (read-only local foundation)

Status: **tooling candidate, CI/merge pending**. This is NOT a new Android APK, Google Drive ingestion, or catalog publication.

## Why

Renault Docs already builds one `.rdpkg` per volume with `tools/build_rdpkg.py` and validates/catalogs completed packages with `tools/build_drive_catalog.py`. Before conversion, owner-managed original Windows ZIP/7Z/RAR archives need an **immutable source inventory** so that originals, prepared packages, and possible duplicated Renault NT+date labels are never silently mixed.

## Command (developer / Termux, optional)

From the existing Renault Docs repository:

```bash
python tools/inventory_windows_archives.py "/path/to/your/local/Windows archive folder"
```

The command **only reads** top-level files and prints deterministic UTF-8 JSON to stdout. It does **not** write a report file or an archive, install anything, copy to Google Drive, or update the user's Renault projects.

Optional flags:

- `--recursive`: read nested directories as well (not the default; symlinks skipped).
- `--sha256`: explicitly stream/read every inspected archive to compute SHA-256 (not the default; may be slow for very large files).

No automatic cleanup, checkpoint, backup or rollback is created. No need to run it on the user's phone solely for testing.

## JSON contracts

`schema_version = 1`; `source_folder` is just the **leaf directory name**, never an absolute private SAF/Drive URI. Each archive has `relative_path`, filename/size, declared format, `identity_source = filename_only_unverified`, inferred NT/date/vehicle codes/document type/version/region and strongly explicit model words (Kangoo/Megane/Laguna) when present.

`status` is one of:

- `candidate_unverified`: ZIP central directory could be opened, or 7Z/RAR header matches. **Not** content-ready.
- `needs_model_review`: filename explicitly names conflicting model families.
- `prepared_package_not_source`: `.rdpkg` extension, never raw input.
- `prepared_payload_not_source`: ZIP has root `rdpkg.json` or `renault-dataset.json` (already prepared, not a Windows original).
- `invalid_header` / `invalid_zip_structure` / `unreadable`: file must not be passed to converter without resolution.

Possible duplicates by equal **NT and date** are listed as `possible_duplicate_paths`, not treated as byte identity and never silently skipped. Different Renault models or regions can legitimately use similar NT codes. For 7Z/RAR the tool only checks the signature, **not full internal validity**. ZIP metadata is read without extracting any file.

## Explicitly outside this step

1. Reading private Google Drive or downloading any original Windows files.
2. Parsing entire archive content or checking every entrypoint inside 7Z/RAR.
3. Running converter/building a new `.rdpkg` or changing original ZIP.
4. Publishing to any Google Drive catalog or public URL.
5. Claiming any PDF or documentation can legally be redistributed.

Those are independent future #30 acceptance gates and must preserve originals and user data.

## QA

Synthetic temporary files only: expected identity/duplicate warnings, .rdpkg/masquerading ZIP denial, bogus headers, no implicit hashing, recursive opt-in, skipped symlinks, deterministic stdout, unchanged source bytes and filenames. Python CI before merge. No new signed APK/phone install required for tooling-only milestone.
