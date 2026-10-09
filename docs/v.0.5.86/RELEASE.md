# Renault Docs v0.5.86 / build 102 — Archive multi-volume isolation

Status: CODE CANDIDATE / CI PENDING / PHONE QA PAUSED.

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
