# Renault Docs — handoff v0.5.84 / build100 — 2026-10-09

## Issue #40 background lifecycle (SOURCE CODE FIXED / PHONE QA PAUSED)

- PR #92 merged, runtime source SHA `4890963f0dc0a268f803913c0e3e7da7193ecd7a`. VersionName `0.5.84`, versionCode `100`.
- PR Python Tests #584 PASS; Android PR Check #463 PASS. Main Python Tests #585 PASS; **stable-signed Android APK #147 in progress** as this checkpoint is written, run ID `37866966922`.
- All seven long-running operations have foreground dataSync service, PARTIAL_WAKE_LOCK, persisted run state and START_REDELIVER_INTENT.
- Actual fixes: conversion redelivery no longer resets cancellation/start/progress or restarts completed run; native raw/archive preparation no longer clears pending cancel on recovery or handles stale finished/waiting command as new; ConversionActivity interrupted reconciliation now waits 30s, canceled when Activity stops, rechecks store identity/service on late callback.
- Formal audit matrix `docs/v.0.5.84/qa/BACKGROUND_SERVICE_AUDIT.md`; regressed via `tests/test_v0584_background_recovery_contract.py`. FGS long-running 6h limits and real device process-death/partial write behavior remain separately open.
- No Android app data migration, ZIP/RDPKG file deletion, force-stop, reboot, or live conversion has been performed. Classic unchanged.
- User explicitly paused expanded phone QA. Do not prompt for more tests/installations without user asking. Issue #40 remains OPEN pending on-device confirmation and long FGS quota concerns.

## Prior release and next steps

- v0.5.83/debug is public GitHub Release with signed APK and sha256, URL https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.83-debug. Signed source `b5f1c22d73642d7341943e4320b5fb469cbe9bec`; user's New Volume core PHONE PASS.
- Once v0.5.84 signed run #147 succeeds, record artifact digest and original APK SHA256; publish **debug prerelease labeled PHONE QA PENDING** only through the reviewed manifest-gated GitHub Releases workflow (no automatic artifact substitution). Do not confuse Actions bundle digest with original APK digest.
- Then determine next feature scope: #51 archive intake acceptance and #30 Windows-source pipeline; optional #21/#50. Phone QA remains paused.
