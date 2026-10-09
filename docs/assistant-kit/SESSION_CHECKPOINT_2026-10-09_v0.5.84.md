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

## Signed / published closeout — 2026-10-09

- Merged PR #92, exact tested Android source 4890963f0dc0a268f803913c0e3e7da7193ecd7a. Python Tests #585 PASS, signed Android APK #147 PASS, run ID 37866966922. Artifact ID 11588687681, Actions ZIP digest sha256:c5d7bc4f711a9986f7092367f1f00b97a4defb3638bf7234901aa9995107a24c.
- PR #93 published the pinned original developer-signed APK via verified promotion run 37867195007 PASS: https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.84-debug. APK and .sha256 assets present, inner APK SHA256 90f4ee81e46340d4f6fd4e9f56632598bad68d3e52a944e9490727e53a1a7010. This is a DEBUG PRERELEASE with phone-qa-pending label.
- Seven-service audit matrix and remaining limitations: docs/v.0.5.84/qa/BACKGROUND_SERVICE_AUDIT.md. Issue #40 OPEN for phone and Android dataSync 6h quota/timeouts and safe partial-output replays. User paused phone QA; no app was installed or conversions started during this work.
- Next distinct functional scope #51 archive intake or #30 Windows source-to-published catalog. Preserve original Renault ZIP/RDPKG and Classic.

## Device installation smoke PHONE PASS — 2026-10-09

Following a direct request to install **v0.5.84/build100** via Termux Renault 5 → 19 → 8 → 13 *over the existing app* and verify app opens and all projects/volumes remain, the user replied «Пасс». Record confirmed **installation/launch/data-retention smoke PASS** for current device. Do not imply every individual volume was audited; no counts reported. **Issue #40 detailed foreground operation lock/unlock/process-death QA remains PAUSED and issue stays OPEN.** No new tests required. Next distinct user-selected feature scope #51 archive intake (or #30 Windows packaging) when requested.
