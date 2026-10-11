# Renault Docs #30 — Stage G: prepared volume ↔ .rdpkg byte parity

**Status: candidate / CI pending.** This is an offline, read-only **core documentation-data correctness** tool, not an access-control or licensing feature. Auth/payments #133 remain blocked until separately accepted CORE STABLE #147.

## Trust boundary

Three different representations must NOT be confused:

1. **Original Windows ZIP/7Z/RAR** — potentially contains many tomes and HTML that converter intentionally transforms. Not byte-identical to normalized output.
2. **Prepared/normalized `*_android` dataset folder** — existing `renault-dataset.json`, individual discovered document-volume directories and transformed assets.
3. **Portable `.rdpkg`** — single-volume snapshot built from one chosen prepared volume, plus derived `_renault/` indexes/IR/Fast Pack and `rdpkg.json` metadata.

This audit checks only **(2) ⇄ (3)** for the exact selected source-volume subtree. The previous Stage F also fully streams all `.rdpkg` members for ZIP CRC, manifest counters, volume identity and unsafe paths. This combination is more meaningful than comparing two outer ZIP sizes, or deduplicating by neighboring NT numbers.

## Command

```bash
python tools/compare_prepared_volume_to_rdpkg.py \
  --prepared-root "/private/path/Megane II_android" \
  --package "/private/path/packages/rdpkg/Megane-II-...NT8340A....rdpkg" \
  --volume "NT8340A"
```

**No automatic discovery of a 'newest' edition:** the caller must explicitly choose `--volume`, using exact source folder, document code, title or volume ID through existing `select_volume`. The tool loads the existing prepared dataset manifest, ensures selected source stays inside its root, rejects symlinks and special files, and limits the source scan to 100,000 files / 32 GiB by default.

It first validates the entire `.rdpkg` with Stage F's streaming CRC+payload safety. Then checks package project ID, selected complete NT code, date, volume ID, source folder and entrypoint. For each byte of every source file **under that selected volume folder**, computes streaming SHA-256 and compares to the corresponding ZIP member hash. Missing, extra and different-content members are FAIL; the report lists at most 25 example mismatches, with full count and truncation flag.

The `_renault/` derived indexes, Fast Pack, runtime IR, and generated dataset manifest are checked for ZIP integrity by Stage F but **are not original source-volume files** and are not claimed byte-matched to Windows originals. Generated content semantics are a separate core QA step.

Report JSON prints to **stdout only**, with no absolute filesystem paths, tokens or Drive provider IDs. Exit 0=PASS, exit 2=FAIL. No extraction, conversion, copying, report file, delete, rename, package rebuild, Drive API/network operation or original-source ZIP changes.

## Explicitly unproven

- **Original Windows archive → normalized prepared HTML/PDF exact parity:** HTML can be modified intentionally during conversion; no blanket SHA equality claim.
- Actual read-only tests against the owner's existing 21 installed Megane volumes or Drive files: not done by synthetic CI; must be authorized and run against privately staged files when available.
- Logical content/links/navigation correctness in generated native index, Classic/Modern or Fast Pack, and IP/redistribution permission.
- Identical vs non-identical prepared Drive package objects with same filenames (#143): no deletion or guessed 'winner'.

NT8340A and NT8342A are **distinct independent Renault documents**, never collapsed as duplicate volumes.

## Acceptance criteria

Synthetic end-to-end test uses the repository's real `build_rdpkg` function to produce portable packages from tiny two-volume prepared dataset (HTML+PDF). Verifies byte-match PASS, missing/extra/changed files FAIL, wrong NT FAIL, other NT correct PASS, source symlink rejection, corrupted ZIP refusal, hash/file count limits, CLI stdout and no source/package mutations.

Exact PR-head Python CI then main CI PASS required. No Android code or APK change; installed v0.5.97/build113 stays current until separate core release milestone.
