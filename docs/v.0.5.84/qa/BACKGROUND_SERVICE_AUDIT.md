# Issue #40 — seven-operation lifecycle audit — 2026-10-09

## Baseline (v0.5.83)

Each service below already existed and runs as a non-exported `dataSync` foreground service with `android:stopWithTask=false`. Android manifest grants `FOREGROUND_SERVICE_DATA_SYNC` and `WAKE_LOCK`. Each has `START_REDELIVER_INTENT`, a persisted run store, a worker guard and a CPU partial wakelock. The source-level audit **does not** itself prove lock/unlock device survival or battery policy behavior.

| Workflow | Owner | Persisted state | CPU lock | Restart / latest assessment |
|---|---|---|---|---|
| Legacy conversion | `ConversionService` | `ConversionRunStore` | native `PARTIAL_WAKE_LOCK` | **Fixed**: only matching active run resumes, preserving cancel and start time; terminal redelivery is no-op |
| Raw → RDPKG + archive intake | `NativeRdpkgPreparationService` | `NativeRdpkgRunStore` | native `PARTIAL_WAKE_LOCK` (bounded) | **Fixed**: pending cancel preserved and terminal/waiting selection redelivery ignored |
| Catalog import | `CatalogImportService` | `CatalogImportRunStore` | `BackgroundWorkWakeLock` | Existing persisted `isRunning` guard; idempotent replay **still requires runtime acceptance** |
| Prepared RDPKG import | `RdpkgImportService` | `RdpkgImportRunStore` | `BackgroundWorkWakeLock` | Existing persisted project/package identity and `isRunning` guard; partial file outcomes to QA |
| RDPKG export | `RdpkgExportService` | `RdpkgExportRunStore` | `BackgroundWorkWakeLock` | Existing project/volume/destination identity and `isRunning` guard; partial output to QA |
| RDPKG share preparation | `RdpkgShareService` | `RdpkgShareRunStore` | `BackgroundWorkWakeLock` | Existing project/volume identity and `isRunning` guard; prepared file replacement to QA |
| RDPROJECT share preparation | `RdprojectShareService` | `RdprojectShareRunStore` | `BackgroundWorkWakeLock` | Existing project identity and `isRunning` guard; prepared file replacement to QA |

## Fixes in v0.5.84/build100

1. Legacy conversion begin committed durably before worker; redelivery with a completed run no longer triggers a second conversion. A matching still-running plan reuses run-state without resetting progress or cancellation. Mismatched active plan refused.
2. Native .rdpkg resume preserves user cancel request, avoiding reversal of intent after process death. Stale completed/waiting-selection ACTION_START ignored.
3. ConversionActivity lifecycle reconciliation only scheduled while visible, with a 30-second recovery grace rather than 1.5 seconds; before overwriting status after asynchronous filesystem validation it rechecks active service and matching unchanged run identity. Original successful-output recovery behavior stays available after confirmed interruption.

## Limitations / evidence boundaries

- **Android phone QA is paused at user's explicit request.** Compilation and static contract tests are not real process-death/lock/battery optimization proof.
- Ordinary lock/background ≠ Android force-stop/reboot. User force-stop and reboot are hard boundaries and may strand an operation; automatic reboot replay is not promised.
- On Android 15+/targetSdk 36, the OS limits `dataSync` FGS background execution quota (not unlimited uptime). The existing timed wakelocks also must not be described as infinitely renewable. Need follow-up Android 15/16 timeout-service behavior audit before claiming multi-hour stability.
- Source currently has no comprehensive isolated-instrumentation tests for partial-output idempotency across all five other services. Runtime status attach and operation cancellation need device evidence before #40 can be fully CLOSED.
- No source ZIP/.rdpkg deletion, user data clearing, forced re-import or APK installation performed for this audit.

## Next gate

1. Automated Python/static tests and Android PR compile.
2. Merge only with CI passing and signed main APK built.
3. Keep #40 **IMPLEMENTATION HARDENED / EXTENDED PHONE QA PAUSED**, not falsely CLOSED.
4. Only when user asks to resume: device lock/unlock, background return and interrupted output recovery per service with small safe test inputs.
