# v0.5.86 QA/evidence

Source-level audit of v0.5.85: `NativePreparationStager.scanLocalSource()` traversed full selected folder with walkTopDown, including independently detected nested Renault raw roots from archive chooser. New `ArchiveRawVolumeIsolation` computes selected candidate subtree exclusions constrained to extracted app-private staging and returns a pruned `FileTreeWalk`; service/engine/stager pass excludes with defaults to preserve raw source behavior. Three JUnit isolation/containment tests plus Python contract.

CI and signed artifact pending at this checkpoint; device/phone QA paused, #51 OPEN. No original Renault archives or registered data touched.

## 2026-10-09 initial CI evidence

PR #96 MERGED app source `f145dee0c545d5ee1953a8584cebb3a60bfd5acf`. PR Python Tests #588 PASS, Android PR Check #465 PASS including JUnit and ephemeral PR compile; main Python Tests #589 PASS. Main stable signer APK #149 (run ID `37872426595`) still processing as of this record. No device test, no source ZIP/RDPKG touched. Next: verify full signed APK run before publishing.


## Signed debug build / public prerelease closeout — 2026-10-09

PR #96 merged app source `f145dee0c545d5ee1953a8584cebb3a60bfd5acf`. PR Python #588 / Android PR #465 PASS; main Tests #589 PASS, stable-signed APK #149 PASS (run ID `37872426595`, artifact ID `11590399784`, artifact bundle SHA256 `20c5727c7b000f56fb7af61348eaab77254573694f204e6dd9b7a5100abd153b`). Reviewed PR #97 published QA-pending debug prerelease via run `37872667304` PASS: https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.86-debug. Assets: original signed APK SHA256 `72d78292c8197a026bddaba4ecd32ab5f433556bab7ca6622b9d246d109b5ec3` and independent `.apk.sha256`. Phone QA not performed; #51 remains open.
