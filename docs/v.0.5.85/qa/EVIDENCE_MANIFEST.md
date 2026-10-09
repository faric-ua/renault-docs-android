# v0.5.85 evidence ledger

Code-only findings: `NativeRdpkgPreparationService.buildArchiveCandidates` previously rejected empty relative paths; resumed chooser rejected canonical extraction root; local staging used `rawRoot.name = extracted`. `ArchiveIntake` previously allowed overwrite by duplicate normalized members. Patched and added `ArchiveIntakeTest` cases + `tests/test_v0585_archive_root_safety_contract.py`.

At creation: CI, signed APK, published prerelease and phone acceptance NOT YET CLAIMED. User-requested pause on extended phone QA continues; preserve all user source archives and volumes.

## Signed CI + public prerelease evidence

PR #94 merged app source `5b27f30ce92aed00e5b1e0e233d9a312a064bfb6`. Python PR Tests #586 / Android PR Check #464 / main Tests #587 / signed APK #148 all PASS. Signed source run ID `37871352918`, artifact ID `11590002768`, artifact ZIP digest `sha256:c0c1d8c73c04eed467c83ad1e0c9b59e0c5b3df70437a99c4ac0abb978cbf394`. Published exact original signed APK through PR #95 verified publisher run ID `37871568678` PASS: https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.85-debug, original APK SHA-256 `dde09bde484d0f3d7392505a999faa8f8036a2ce38778c654fcbeac676baacb6`, separate `.sha256`. No new device evidence for #51, phone QA paused.
