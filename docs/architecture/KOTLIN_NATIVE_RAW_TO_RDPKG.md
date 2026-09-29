# Kotlin-native raw Renault → .rdpkg pipeline

Date: 2026-09-27

Status: **v0.5.51 development foundation**

## Goal

The end-user Android path must not require Python, Termux, or a user-visible intermediate `*_android` folder.

Target:

```text
raw Renault folder (SAF)
        ↓
one source scan
        ↓
one copy/patch into app-private staging
        ↓
Kotlin compiler metadata
        ↓
native sections + Runtime IR shards/index + coverage
        ↓
Fast Pack
        ↓
rdpkg.json + renault-dataset.json
        ↓
stream outer .rdpkg
        ↓
SHA-256 while writing
```

Python/Termux remain the reference implementation and developer parity oracle.

## Why private staging still exists

The compiler needs random access to the normalized files while it discovers sections, parses legacy HTML/JS, builds Runtime IR, writes shards and packages web assets.

Trying to compile directly while reading the raw SAF tree would cause repeated provider reads.

Therefore the optimized contract is:

```text
raw SAF source
  → exactly one normalized private staging copy
  → all compiler passes operate on local File I/O
  → final .rdpkg is streamed once
```

The private staging directory is temporary implementation storage, not a user-facing converted dataset.

This avoids the old expensive shape:

```text
raw SAF
  → public *_android copy
  → rescan public copy
  → package
  → .rdpkg
```

## Reusable Android foundation

Already present and reusable:

- fast SAF scan using `DocumentsContract + ContentResolver.query()`;
- `ConverterPathNormalizer` parity with `core/convert_paths.py`;
- text patching while bytes are copied;
- cancellation/progress lifecycle foundation;
- volume identity/date/entrypoint discovery in `AndroidDatasetPackageWriter`;
- managed `.rdpkg` import and validation;
- Android-native managed package export.

The new pipeline should reuse these rules instead of embedding Python.

## Python-only compiler work still to port

The full Modern compiler remains in Python today:

- `core/volumes.py`;
- `core/sections.py`;
- `core/section_ir.py`;
- `core/runtime_ir.py`;
- `core/runtime_ir_shards.py`;
- `core/runtime_ir_coverage.py`;
- `core/modern_index.py`;
- `core/dataset_package.py`.

This is the actual parity work for the next implementation waves.

## v0.5.51 foundation added

### Shared outer .rdpkg writer

`RdpkgZipWriter` is now the common streaming ZIP writer.

Contract:

- deterministic file order;
- fixed ZIP timestamps;
- SHA-256 calculated through `DigestOutputStream` while the archive is written;
- no second full-file hashing pass;
- text/web metadata uses `Deflater.BEST_SPEED` (level 1);
- binary/already-compressed payload uses `Deflater.NO_COMPRESSION` (level 0);
- level 0 still uses standard DEFLATED ZIP entries, avoiding the CRC/size pre-pass required by STORED entries.

The existing v0.5.50 managed-volume exporter is refactored to use this writer, so the optimization is immediately regression-testable before the raw converter is complete.

### Kotlin Fast Pack writer

`NativeFastPackWriter` is the File-based counterpart of `core/fast_pack.py`.

Contract:

- same inclusion/exclusion semantics as the Python Fast Pack;
- deterministic entry order and timestamps;
- `Deflater.BEST_SPEED`;
- SHA-256 calculated during ZIP creation;
- final `fast-content-<sha16>.zip` name is selected after the streaming digest is known;
- no separate SHA-256 reread.

It is intentionally File-based because the future raw pipeline will compile from private local staging after the single SAF copy.

## Performance contract

For the new raw converter, do not reintroduce repeated full corpus byte passes unless a compiler stage genuinely needs file contents.

Desired expensive I/O:

1. metadata scan of SAF tree;
2. one SAF read / one private staging write per source file;
3. compiler reads only the HTML/JS/metadata files it needs from local staging;
4. Fast Pack reads selected web assets once;
5. outer `.rdpkg` reads prepared staging once and hashes while writing.

No public `*_android` output is required for the primary user flow.

## Parity policy

Kotlin output does not have to be byte-identical to Python ZIP output because compression settings may intentionally differ for speed.

Parity means semantic equality:

- volume identity;
- document code/date;
- entrypoint;
- section count/order/IDs;
- section titles and legacy entrypoints;
- Runtime IR action/control/document semantics;
- shard/index references;
- coverage counters and unsupported-action reporting;
- Fast Pack selected file set and file count;
- dataset manifest semantics;
- successful Android native reopen without compatibility fallback.

Python remains the oracle until those checks pass on the same source volume.

## Implementation order after this foundation

1. private File staging writer fed by the existing SAF scan + `ConverterPathNormalizer`;
2. Kotlin volume + Modern index writer parity;
3. Kotlin section discovery parity;
4. Kotlin Section IR compiler parity;
5. Runtime IR shards/index + coverage;
6. `NativeFastPackWriter`;
7. `rdpkg.json` + dataset manifest finalization;
8. `RdpkgZipWriter` to the user-selected destination;
9. validate package and reopen/install through the existing Android managed-package runtime.

The first reference volume should remain NT8340A because the accepted package is known to reopen as:

`NT8340A · 2006-04-18 → 347 · native`.

That gives a stable parity target before broader dataset-family testing.
## Implementation progress

The private staging step is now implemented as `NativePreparationStager`.

It performs the required one-time SAF metadata scan and one source-byte copy into `noBackupFilesDir/native-preparation`, applying `ConverterPathNormalizer` during the copy. It retains the proven direct `DocumentsContract` scanner with a DocumentFile compatibility fallback and deletes incomplete staging on failure/cancel.

Therefore the next unimplemented boundary is no longer source I/O. It is compiler parity on local files:
1. volume discovery + Modern index;
2. native section discovery;
3. Section IR;
4. Runtime IR shards/index + coverage.
### Local compiler progress

`NativeVolumeCompiler` now covers the first compiler parity layer on app-private File staging:
- Python `core/volumes.py` volume discovery semantics;
- Python `core/modern_index.py` dataset/navigation shape;
- `volumes.json` and `modern-index.json` output.

The next parity target is `core/sections.py`, because section identity/order/entrypoint must be correct before Section IR can be compiled safely.
### Native section discovery progress

`NativeSectionCompiler` now covers the `core/sections.py` parity layer and writes `modern-sections.json` schema v2.

Implementation uses jsoup as the tolerant HTML parser after explicit legacy-byte decoding. The compiler does not execute Renault JavaScript; it extracts local navigation targets from frames, anchors, table rows and quoted HTML targets in JavaScript attributes.

The opaque section-ID contract is preserved. No numeric-only or three-digit assumption is permitted.

The next major parity boundary is `core/section_ir.py`.
