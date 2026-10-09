# Renault Docs v0.5.86 / build 102 — Archive multi-volume isolation

Status: **PR #96 MERGED / PYTHON + ANDROID CI PASS / SIGNED APK #149 PASS / VERIFIED PUBLIC DEBUG PRERELEASE PUBLISHED / PHONE QA PAUSED.**

## Issue #51

A Renault archive may place a valid raw volume's `INDEX.HTM` at extraction root and a separate valid raw Renault volume in a nested folder. Although v0.5.85 correctly restored the archive-root candidate's identity and chooser selection, `NativePreparationStager.scanLocalSource()` walked every descendant and copied nested independently selectable volumes into the parent's private staging and generated .rdpkg.

This release fixes the **source contamination** without deleting or rewriting the archive:

- `ArchiveRawVolumeIsolation.excludedDescendantRoots()` resolves each persisted archive chooser candidate via `ArchiveIntake.resolveRawRoot()` and computes only strict descendants of the currently selected volume. It includes *all* other nested raw candidates, whether selected or not.
- `walkSelectedRoot()` prunes nested candidate roots before any file read/copy into private native source staging; parent assets not in separately detected volumes remain. Child volumes can be selected independently in the same batch.
- The exclusion plan is passed explicitly from batch selection → service → native preparation engine → native stager. SAF raw-tree conversion and a single-root archive retain the previous behavior (default: no exclusions).
- Validate current raw source/project model using the real archive name, not the private folder `extracted`.
- Add Kotlin JUnit tests for parent+two nested volumes, parent/child/grandchild, source preservation and traversal/invalid boundaries, with Python source regression contract.

## Safety and release gates

No production user file is modified; original ZIP/7Z/RAR is SAF read-only, existing volumes and Classic unaffected. Do **not** claim full #51 closed on static tests alone. Real mixed archive import, per-volume result, cancellation and Android lifecycle remain phone QA (paused at user's request). #40 background Android-specific acceptance remains separately open.


## Verified build and release — 2026-10-09

- PR #96 merged at Android app source SHA `f145dee0c545d5ee1953a8584cebb3a60bfd5acf`.
- Python PR Tests #588 PASS, Android PR Check #465 PASS (including new `ArchiveRawVolumeIsolationTest`); main Tests #589 PASS and stable-signed Android Debug APK #149 PASS (run `37872426595`).
- Original signed Actions artifact `Renault-Docs-v0.5.86-Debug`, ID `11590399784`, bundle SHA-256 `20c5727c7b000f56fb7af61348eaab77254573694f204e6dd9b7a5100abd153b` (**artifact bundle**, not inner APK).
- PR #97 explicit verified promotion; publisher run `37872667304` PASS. [v0.5.86-debug](https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.86-debug) includes unchanged original signed APK + `.apk.sha256`. Original APK SHA256: `72d78292c8197a026bddaba4ecd32ab5f433556bab7ca6622b9d246d109b5ec3`.
- Public **developer-signed debug prerelease**, not a phone-approved production release. No user source archives or installed files were touched.
