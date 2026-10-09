# v0.5.84 background lifecycle regression

- [ ] Validate 7 services manifest dataSync + stopWithTask=false + foreground execution, CPU wakelock and redelivery.
- [ ] Conversion Service restores same plan state without resetting cancel or start time.
- [ ] Stale redelivered completed conversion does not start a duplicate operation.
- [ ] Native .rdpkg preparing resume preserves user cancellation.
- [ ] Stale completed native command cannot restart work.
- [ ] Activity interrupted reconciliation defers 30 seconds, is canceled onStop and rechecks run identity before terminal mutation.
- [ ] Other 5 service run stores guard duplicate restarts.
- [ ] Process shutdown cleanup and Activity state reattachment remain consistent.
- [ ] Python static contracts PASS.
- [ ] Android JVM/unit tests and Debug APK PR Check PASS.
- [ ] Main CI and stable-signed APK PASS.
- [ ] Phone lock/unlock/background/rotation across each long operation — **DEFERRED BY USER**.
