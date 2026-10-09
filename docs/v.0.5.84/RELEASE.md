# Renault Docs v0.5.84 / build 100 — background lifecycle hardening

Status: **DEVELOPMENT — PHONE QA PAUSED**.

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
