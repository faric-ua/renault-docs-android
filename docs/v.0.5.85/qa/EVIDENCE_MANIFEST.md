# v0.5.85 evidence ledger

Code-only findings: `NativeRdpkgPreparationService.buildArchiveCandidates` previously rejected empty relative paths; resumed chooser rejected canonical extraction root; local staging used `rawRoot.name = extracted`. `ArchiveIntake` previously allowed overwrite by duplicate normalized members. Patched and added `ArchiveIntakeTest` cases + `tests/test_v0585_archive_root_safety_contract.py`.

At creation: CI, signed APK, published prerelease and phone acceptance NOT YET CLAIMED. User-requested pause on extended phone QA continues; preserve all user source archives and volumes.
