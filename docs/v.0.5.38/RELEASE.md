# Renault Docs v0.5.38 — Converter foundation fix

Date: 2026-09-26

## Trigger

Real-phone v0.5.37 Megane II conversion completed successfully for 7657 files, but exposed three foundation issues before the full Modern compiler wave:

1. recursive SAF scan is too slow;
2. documentation images appear in Android Gallery as photo albums;
3. source/destination cards show raw content:// URIs.

## Fast SAF scanner

Primary scan path now uses DocumentsContract + ContentResolver directly.

Per directory:
- query child document rows once;
- read document id;
- display name;
- MIME type;
- size;
- recurse only into directory rows.

This avoids DocumentFile metadata calls for every child.

Compatibility:
- if direct DocumentsProvider query fails, converter falls back to the v0.5.37 DocumentFile scanner;
- cancellation remains active in both scanners;
- complete exact-path map is still built before any normalization/copy.

## Scan progress UX

During SCANNING:
- progress bar is indeterminate;
- UI shows `Знайдено файлів: N`;
- no misleading `0 / N` copy-style counter.

Fast scanner status also reports directory count in the phase message.

## Gallery isolation

Every new staging dataset receives an empty root `.nomedia` marker before source media files are copied.

This prevents Android MediaStore/Gallery from treating Renault documentation image folders as user albums.

The marker remains after staging is renamed to final output.

Registering an already-created converter output also ensures the marker exists.

Already-indexed albums from older output may remain cached until the Gallery/MediaStore refreshes or the old output is removed.

## Friendly SAF paths

New shared `SafDisplayPath`.

Examples:
- `primary:Documents/Renault/Megane II`
  -> `Documents/Renault/Megane II`;
- `primary:Documents/Renault`
  -> `Documents/Renault`.

Converter source/destination cards no longer expose raw content URIs when a friendly path is available.

Backup Settings now uses the same formatter for consistent display.

## Scope

No Modern Runtime IR/Fast Pack compiler yet.

v0.5.38 is a foundation cleanup before the full compiler parity wave.

## Version

- versionName: `0.5.38`
- versionCode: `54`
- branch: `fix/v0.5.38-converter-foundation`

## Status

Implementation complete on feature branch.
CI and phone validation pending.


## Termux APK handoff fix

Renault Menu point 8 previously searched the latest successful Android build for the current Git branch.

That is unsafe after merge because:
- code can already be on `main`;
- the exact release APK may have been built on the feature branch before merge;
- the latest successful `main` build can therefore belong to an older app version.

v0.5.38 changes point 8:
- read current `versionName` from `android/app/build.gradle.kts`;
- require exact artifact name `Renault-Docs-v<version>-Debug`;
- find the matching non-expired GitHub Actions artifact regardless of build branch;
- download only that exact named artifact;
- verify expected APK filename and SHA-256;
- if the exact artifact does not exist, STOP instead of silently downloading an older APK.

After v0.5.38 merge, normal phone workflow returns to:
`5 → 8 → install`.


## Merge / CI

PR #114 merged to main as:
`d93e56de43a5ec00120e62f99e77bf9875b0b4dc`.

Green tested source:
`c9faf5bf704ff45e62f226bb39f163e562597a5b`.

Key converter/UI/Termux/version blobs were verified identical between tested source and merged main.

CI:
- Tests `36240225061` — PASS;
- Android Debug `36240225052` — PASS;
- artifact `Renault-Docs-v0.5.38-Debug`;
- artifact id `10905865135`;
- APK SHA-256 `caf6ecb0426306b8953c97048e578f4459b20d5a2510491a2e1e9ffe7d45a90d`;
- artifact ZIP SHA-256 `fa05aa143634aac5dc4ee172b9a7aa78e79515e2d680851fcae4800cdcaaa2d8`.

Status: CI PASS; phone validation pending.
