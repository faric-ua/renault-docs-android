# Long-running operations and acceptance gates

| Workload | Worker | State | Lock | Current gate |
|---|---|---|---|---|
| Legacy conversion | ConversionService | ConversionRunStore | native PARTIAL_WAKE_LOCK | lock, interruption, output idempotency |
| Native raw/archive batch | NativeRdpkgPreparationService | NativeRdpkgRunStore | native PARTIAL_WAKE_LOCK | batch cancel/import, lock/resume, partial installed results |
| Catalog import | CatalogImportService | CatalogImportRunStore | BackgroundWorkWakeLock | lock, queue interruption |
| RDPKG import | RdpkgImportService | RdpkgImportRunStore | BackgroundWorkWakeLock | lock/unknown source length, partial output |
| RDPKG export | RdpkgExportService | RdpkgExportRunStore | BackgroundWorkWakeLock | lock, partial SAF output |
| RDPKG share | RdpkgShareService | RdpkgShareRunStore | BackgroundWorkWakeLock | lock, prepared share idempotency |
| RDPROJECT share | RdprojectShareService | RdprojectShareRunStore | BackgroundWorkWakeLock | lock, source volume identity |

All seven have FGS/redelivery/locks. Test source contracts in PR; phone acceptance for background must be observed and logged for each workload independently. Android force-stop and device reboot are NOT auto-resume guarantees. 6h CPU lock is bounded and OS background dataSync quota also applies. Do not alter battery optimization settings to fabricate a PASS.
