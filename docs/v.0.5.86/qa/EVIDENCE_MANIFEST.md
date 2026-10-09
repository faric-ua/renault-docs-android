# v0.5.86 QA/evidence

Source-level audit of v0.5.85: `NativePreparationStager.scanLocalSource()` traversed full selected folder with walkTopDown, including independently detected nested Renault raw roots from archive chooser. New `ArchiveRawVolumeIsolation` computes selected candidate subtree exclusions constrained to extracted app-private staging and returns a pruned `FileTreeWalk`; service/engine/stager pass excludes with defaults to preserve raw source behavior. Three JUnit isolation/containment tests plus Python contract.

CI and signed artifact pending at this checkpoint; device/phone QA paused, #51 OPEN. No original Renault archives or registered data touched.
