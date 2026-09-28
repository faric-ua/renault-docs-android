# Renault Docs v0.5.37 — Converter Writer Wave 1

Date: 2026-09-26

## Goal

Turn the old v0.2.0 ConversionActivity foundation into a real long-running Android SAF conversion writer.

This is the first execution wave of Converter 2.0.

## Implemented

### Real conversion execution

ConversionActivity now has:
- source SAF tree picker;
- destination-parent SAF tree picker;
- plan validation;
- real `Почати конвертацію`;
- persistent progress;
- cancel request;
- completed-output registration into Library.

Conversion continues in a foreground data-sync service, so leaving the Activity or rotating the device does not intentionally restart the operation.

### Safe staging contract

The writer creates:

`.<source>_android.renault-staging`

inside the selected destination parent.

Sequence:
1. scan source;
2. build the complete exact-path map;
3. create staging tree;
4. copy every source directory/file;
5. normalize confirmed case-sensitive legacy references while copying HTM/HTML/JS;
6. write conversion report;
7. write a base dataset package;
8. validate output;
9. only then rename staging to `<source>_android`.

On failure/cancel before finalization:
- staging is deleted;
- original source is untouched.

If final output already exists, conversion refuses to start rather than overwrite it.

Destination inside source is rejected.

### Path normalization

Uses the already-tested Kotlin `ConverterPathNormalizer`.

Text handling preserves the Python converter contract:
- HTM / HTML / JS are decoded as ISO-8859-1;
- only confirmed reference/path changes are rewritten;
- original 8-bit Renault text bytes are otherwise preserved;
- legacy VISU.JS dynamic print suffix normalization remains shared with existing tests.

### Base package

Wave 1 creates:
- `renault-dataset.json`;
- `_renault/START.html`;
- `_renault/README_UA.html`;
- `_renault/volumes.json`;
- `_renault/modern-index.json`;
- `conversion-report.json`.

The package discovers:
- root INDEX/ACCUEIL;
- top-level Renault volumes;
- NT code/date when present;
- wiring-diagram vs technical-documentation volume type.

The completed output can be registered in the app Library.

### Important limitation

v0.5.37 does NOT yet compile the full current Modern package:
- no `modern-sections.json`;
- no Runtime IR v2 shards/index;
- no volume documentation shards;
- no Runtime IR coverage;
- no Fast Pack.

Therefore v0.5.37 output is **normalized / Classic-ready with volume-level Modern catalog only**.

The next Converter 2.0 wave ports the current package/Runtime-IR/Fast-Pack compiler into Android so the in-app converter produces the same full Modern result currently produced by Renault Menu point 9.

### Backup / deletion

Not implemented in this wave.

The original source is NEVER deleted.

Verified backup + SHA-256 + restore + explicit deletion confirmation remain the next safety wave after full package compilation.

## Foreground service

New `ConversionService`:
- foreground type: `dataSync`;
- persistent run state stored in `ConversionRunStore`;
- UI polls/reconnects to that state after Activity recreation;
- progress persistence is throttled;
- notification updates are throttled;
- cancel is a persistent flag checked throughout scan/copy/validation.

## Version

- versionName: `0.5.37`
- versionCode: `53`
- branch: `feat/v0.5.37-converter-writer`

## Status

Implementation complete on feature branch.
CI pending.


## Merge / CI

PR #113 merged to main as:
`51c2d77771a7c5676e5793ee38de2f180eaf7bb5`.

Green tested source:
`de28d445749c752d5d0d1ecbf31959cab92b8d3f`.

Key converter/runtime blobs were verified identical between tested source and merged main.

CI:
- Tests `36209095449` — PASS;
- Android Debug `36209095432` — PASS;
- artifact `Renault-Docs-v0.5.37-Debug`;
- artifact id `10894143612`;
- APK SHA-256 `ac7b0e6dfe0227c86b536f1811249690e8e6fe625f5364354c9ce81ccc590e6e`;
- artifact ZIP SHA-256 `bed2177788448146fd3ac96f0f955a69922256bc3fd11c948463221d661f9739`.

Status: CI PASS; phone validation pending.


## Phone evidence — 2026-09-26

Real-phone conversion of a Megane II source completed:
- files: 7657 / 7657;
- changed files: 0;
- path fixes: 0;
- discovered volumes: 1;
- output: `Megane II_android`;
- source remained unchanged.

This is a meaningful Wave-1 writer success, but v0.5.37 is not yet full PHONE PASS because cancel/reconnect/Library registration were not all closed out.

New findings from this run:
- source scanning through DocumentFile is noticeably slow;
- scan UI displays confusing `0 / N` while only discovery is happening;
- copied documentation images are indexed by Android Gallery as albums (examples: PM, PC, ICONES, DRAPEAUX);
- converter source/destination cards expose raw `content://...` SAF URIs.

These are addressed in v0.5.38 foundation fix before the full Modern compiler wave.
