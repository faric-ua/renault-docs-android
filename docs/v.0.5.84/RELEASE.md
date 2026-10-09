# Renault Docs v0.5.84 / build 100 — background lifecycle hardening

Status: **PR #92 MERGED / MAIN CI PASS / STABLE-SIGNED APK #147 PASS / VERIFIED PUBLIC DEBUG PRERELEASE PUBLISHED / PHONE QA PENDING**.

## Build and publication

- Source commit `4890963f0dc0a268f803913c0e3e7da7193ecd7a`, PR #92 merged.
- PR Python Tests #584 PASS / Android PR Check #463 PASS; main Tests #585 PASS / stable-signed Android Debug APK #147 PASS, run ID 37866966922.
- Original APK published through verified signed release promotion #2 (GitHub Actions ID 37867195007): https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.84-debug
- Actual APK SHA-256 `90f4ee81e46340d4f6fd4e9f56632598bad68d3e52a944e9490727e53a1a7010`, separate .sha256 asset provided. The Actions bundle digest `sha256:c5d7bc4f711a9986f7092367f1f00b97a4defb3638bf7234901aa9995107a24c` is **not** the APK checksum.
- **PHONE QA PENDING / paused by user**. This is a public debug prerelease, not a declared production-stable release. No user file operations/installations occurred.

## Why

Issue [#40](https://github.com/faric-ua/renault-docs-android/issues/40): all seven services already have `dataSync` foreground notifications, partial CPU wakelocks, persisted run state and redelivery semantics. However, a newly redelivered `ConversionService` launch unconditionally calls `ConversionRunStore.begin()`, losing progress, original start time and cancel intent. A stale redelivered native preparation command can also restart a finished run, and native resume clears a pending cancellation.

## Scope

- Prevent completed/cancelled jobs from being restarted by stale framework redelivery.
- Preserve matching active conversion state and cancellation on process restart, reject mismatched active operations.
- Preserve pending native archive/raw cancellation when Android restarts worker.
- Mitigate false interrupted-run state on ConversionActivity: delay reconciliation until service restart had time to run; cancel reconciliation when UI is backgrounded and protect late callbacks with run identity checks.
- Verify all seven existing services keep FGS/wakelock/redelivery and that Activity screens do not require always-on display.
- Do not change Classic, imported volume data, source archives, or SAF permissions.

## Safety

Android force-stop/reboot remain outside automatic replay guarantees; durable startup and exact safe resumption need explicit on-device acceptance. **Never claim reboot or process-death stability proven from static unit tests alone.** Preserve pending phone-QA pause.
