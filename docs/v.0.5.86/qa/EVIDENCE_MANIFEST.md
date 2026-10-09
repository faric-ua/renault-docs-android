# v0.5.86 QA/evidence

Source-level audit of v0.5.85: `NativePreparationStager.scanLocalSource()` traversed full selected folder with walkTopDown, including independently detected nested Renault raw roots from archive chooser. New `ArchiveRawVolumeIsolation` computes selected candidate subtree exclusions constrained to extracted app-private staging and returns a pruned `FileTreeWalk`; service/engine/stager pass excludes with defaults to preserve raw source behavior. Three JUnit isolation/containment tests plus Python contract.

CI and signed artifact pending at this checkpoint; device/phone QA paused, #51 OPEN. No original Renault archives or registered data touched.

## 2026-10-09 initial CI evidence

PR #96 MERGED app source `f145dee0c545d5ee1953a8584cebb3a60bfd5acf`. PR Python Tests #588 PASS, Android PR Check #465 PASS including JUnit and ephemeral PR compile; main Python Tests #589 PASS. Main stable signer APK #149 (run ID `37872426595`) still processing as of this record. No device test, no source ZIP/RDPKG touched. Next: verify full signed APK run before publishing.
