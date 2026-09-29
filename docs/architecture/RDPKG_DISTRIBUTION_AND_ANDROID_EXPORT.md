# RDPKG distribution and Android-native export

## Status

- `.rdpkg v1` is the canonical one-volume distribution unit.
- Real-phone E2E evidence for NT8340A: build → import/update → Modern open → `347 · native` — PASS.
- v0.5.50 adds Android-native export of an already installed managed `.rdpkg` volume.
- Python/Termux remain developer/reference tools, not a required end-user runtime.

## Product contract

One prepared Renault documentation volume is distributed as one file:

```text
NT8340A · 2006-04-18
        ↓
Megane-II_NT8340A_2006-04-18.rdpkg
        ↓
Renault Docs
        ↓
Megane II project
```

The user should not need to understand `*_android` folders, package internals, Python, or Termux.

## RDPKG v1

Current package identity:

```text
schema_version = 1
format = renault-volume-package-v1
```

Required top-level records:

```text
rdpkg.json
renault-dataset.json
<single-volume payload>
```

The dataset manifest must describe exactly one real volume and its entrypoint must exist inside the package.

## Android-native fast export

### Goal

Export an already installed managed `.rdpkg` volume without rebuilding Modern/Runtime/Fast Pack.

### Algorithm

```text
Project volume
   ↓
verify treeUri belongs to Renault Docs managed package provider
   ↓
resolve private managed package directory
   ↓
validate rdpkg.json
   ↓
validate renault-dataset.json and exactly one volume
   ↓
ACTION_CREATE_DOCUMENT
   ↓
ZipOutputStream over existing prepared files
   ↓
MessageDigest SHA-256 while writing
   ↓
result .rdpkg
```

The export is a repack operation, not a conversion. Existing prepared artifacts are reused:
- native sections index;
- Runtime IR;
- Fast Pack;
- dataset manifest;
- original prepared volume files.

This keeps export fast and avoids duplicating the Python converter inside the app.

## Storage contract

The user chooses the destination with Android Storage Access Framework via `ACTION_CREATE_DOCUMENT`.

The app writes only to the returned URI. No broad external-storage permission is required.

Managed imported packages remain inside app-private `noBackupFilesDir/rdpkg/<packageId>`.

## Python policy

Do not embed Python into the production APK for `.rdpkg` export.

Standard Android/JVM APIs are sufficient:
- Kotlin/Java file APIs;
- `ZipOutputStream`;
- `MessageDigest`;
- `ContentResolver`;
- `org.json`;
- Storage Access Framework.

Python stays useful as:
1. the current desktop/Termux converter;
2. a reference implementation;
3. a parity oracle while Kotlin conversion logic is ported.

## Future full on-device converter

Fast export and full conversion are separate stages.

Future Kotlin-native converter:

```text
raw Renault folder
   ↓
volume discovery
   ↓
normalized dataset
   ↓
native sections index
   ↓
Runtime IR
   ↓
Fast Pack
   ↓
validation
   ↓
.rdpkg
```

Port converter stages incrementally. For each stage, compare Kotlin output against the Python reference using the same source volume.

Parity checks should include:
- volume identity;
- document code/date;
- entrypoint;
- section count and IDs;
- Runtime IR structure;
- Fast Pack file count;
- manifest semantics;
- package validation.

## Failure contract

Export must fail without corrupting the installed managed package.

The installed source is read-only for export. Only the destination URI is written.

If package validation fails, no export starts.

If the destination write fails or is cancelled, the installed source remains untouched.

## Scope boundary for v0.5.50

v0.5.50 fast export supports volumes installed into Renault Docs managed storage from `.rdpkg`.

Legacy/reference SAF folder volumes remain readable, but converting those folders into a fresh `.rdpkg` is a later Kotlin-converter stage.
## v0.5.51 Kotlin-native preparation foundation

The next stage is no longer described only as a future converter. The canonical design is now:

```text
raw Renault SAF folder
   ↓
single metadata scan
   ↓
single copy/patch into app-private staging
   ↓
Kotlin Modern compiler
   ↓
native sections + Runtime IR shards/index + coverage
   ↓
Fast Pack
   ↓
rdpkg.json + renault-dataset.json
   ↓
stream .rdpkg + SHA-256 during write
```

The primary end-user flow must not create a public `*_android` directory. Temporary staging is private implementation storage used to avoid repeated SAF/provider reads.

v0.5.51 foundation introduces:
- `RdpkgZipWriter` — shared deterministic streaming outer-package writer;
- SHA-256 through `DigestOutputStream`, without a second full package pass;
- text/metadata entries at `Deflater.BEST_SPEED`;
- binary/already-compressed entries at `Deflater.NO_COMPRESSION`;
- `NativeFastPackWriter` — Kotlin File-based Fast Pack writer with streaming SHA-256 and Python-equivalent file-selection rules.

The full raw-folder compiler is not claimed complete by this foundation. The remaining parity work is the Kotlin port of section discovery, Section IR, Runtime IR shards/index, and coverage.

Canonical detailed design:
- `docs/architecture/KOTLIN_NATIVE_RAW_TO_RDPKG.md`.
