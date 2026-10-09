# Renault Docs session handoff — v0.5.86 / build102 — 2026-10-09

## Version state / issue #51

- Last **confirmed installed** version on user's device: **v0.5.84/build100**, phone PASS for over-install, app launch and preservation of project/volume data (no individual counts provided). **No v0.5.85 or v0.5.86 phone installation/QA** was performed.
- Issue [#51](https://github.com/faric-ua/renault-docs-android/issues/51) archive intake: ZIP/7Z/RAR, read-only SAF source staging, private extraction, duplicate preflight, multi-volume chooser, native .rdpkg package build/import already exist; do not rewrite working functionality.
- In v0.5.85, archive-root candidate path/NT naming and safe member collisions fixed but a parent raw volume that **contained another raw volume folder** could still absorb nested files because native local scanner used an unrestricted recursive walk.
- **v0.5.86/build102 PR #96 MERGED**, exact Android app source SHA `f145dee0c545d5ee1953a8584cebb3a60bfd5acf`. Added `ArchiveRawVolumeIsolation`:
  - compute exclusions from *all* persisted chooser candidate roots, validate candidate paths strictly within app-private extracted staging;
  - for each selected raw root, exclude its strict nested candidate subtrees (regardless selected/installed status) from native local scanner using `FileTreeWalk.onEnter`, **without deleting/moving bytes**;
  - raw SAF-tree prep remains unchanged via default empty excludes; selected child separately still includes its own files.
  - Root-level project model conflict checks now use original archive name, not `extracted`.
- Added Kotlin JUnit `ArchiveRawVolumeIsolationTest.kt` checking root/two children, multilevel nesting, unchanged original source and rejecting traversal, plus Python source contract `tests/test_v0586_archive_volume_isolation_contract.py`.
- **Automated CI PASS**: Python PR Tests #588, Android PR Check #465, main Tests #589, developer-signed Android Debug APK #149 (source run `37872426595`). Original source artifact ID `11590399784`, Actions artifact bundle SHA256 `20c5727c7b000f56fb7af61348eaab77254573694f204e6dd9b7a5100abd153b`.
- **Public developer-signed QA-pending debug prerelease published** via PR #97, verified publisher run `37872667304` PASS: https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.86-debug. Original APK SHA256 **`72d78292c8197a026bddaba4ecd32ab5f433556bab7ca6622b9d246d109b5ec3`**, with independent `.apk.sha256` release asset. Artifact bundle digest is **not** APK checksum.
- Canonical release details `docs/v.0.5.86/RELEASE.md`, `docs/v.0.5.86/RELEASE_META.json`, `docs/v.0.5.86/REGRESSION_CHECKLIST.md`, `docs/v.0.5.86/qa/PHONE_TEST.md`.

## Acceptance boundaries and remaining work

**#51 remains OPEN.** Programmatic source/CI passing does not prove source archive imported correctly on real phone. Unverified: real mixed root + nested ZIP/7Z/RAR per-volume .rdpkg isolation/identity/install counts, cancel midway and staged cleanup, encrypted/unsupported/corrupt archive messaging, long lock/unlock background lifecycle. The latter also belongs to #40 still OPEN.

All phone QA remains **PAUSED BY USER**; do not request tests or reinstall unless explicitly asked. Do not delete/reset registered Renault projects, classic viewer, original archives (.zip/.7z/.rar/.rdpkg), or Termux widget layout. No user files were modified in v0.5.86 work. Keep signed Releases as durable APK; optional normal user installation is over existing app only, not a requirement now.

Next separate product direction after #51 engineering work: issue #30 Windows source→validated prepared package and catalog pipeline; other low-priority #21/#50. For further archive engineering, use #51 outstanding runtime/error paths and sample-specific acceptance, not assumptions from synthetic tests.
