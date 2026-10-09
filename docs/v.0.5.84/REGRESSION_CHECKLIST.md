# v0.5.84 background lifecycle regression

- [x] Source/static contract: 7 services manifest dataSync + stopWithTask=false + foreground execution, CPU wakelock and redelivery (Python #584 and #585 PASS).
- [x] Source contract: conversion resumes matching plan without resetting cancel/start time.
- [x] Source contract: redelivered terminal conversion does not start duplicate.
- [x] Source contract: native preparation preserves cancel.
- [x] Source contract: native completed/waiting stale command ignored.
- [x] Source contract: Activity defers 30 seconds, cancels onStop and rechecks identity.
- [x] Source contract: other five services guard persisted running state; actual restart idempotency still phone pending.
- [ ] Process shutdown cleanup and Activity state reattachment remain consistent.
- [x] Python static contracts PR #584/main #585 PASS.
- [x] Android PR Check #463 PASS (unit + compile).
- [x] Main CI #585 and stable-signed APK #147 PASS; GitHub debug prerelease published.
- [ ] Phone lock/unlock/background/rotation across each long operation — **DEFERRED BY USER**.
