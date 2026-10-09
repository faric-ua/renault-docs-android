# v0.5.84 evidence

Source audit against current `main` before modifications: all seven services declared FGS dataSync + stopWithTask=false, use PARTIAL_WAKE_LOCK or BackgroundWorkWakeLock and START_REDELIVER_INTENT. ConversionRunStore.begin reset risk, native cancel-reset risk and potential stale redelivery identified from code. No actual phone reproduction yet.

Keep PR, CI, stable signer, source SHA and future phone QA independently tracked. Signed build not available when this skeleton is created.

## 2026-10-09 code changes awaiting Android compile

ConversionRunStore and ConversionService now preserve matching persisted run and cancel through redelivery; NativeRdpkgRunStore and PreparationService guard canceled and terminal replay; ConversionActivity postpones and scopes interrupted reconciliation. Added `tests/test_v0584_background_recovery_contract.py`. Native/runtime battery policy/device acceptance still pending. Seven-service matrix: `qa/BACKGROUND_SERVICE_AUDIT.md`.

## Signed and published CI evidence — 2026-10-09

PR #92 merged source SHA `4890963f0dc0a268f803913c0e3e7da7193ecd7a`; Python PR #584 / Android PR #463 PASS, main Python #585 PASS, stable Android Debug APK #147 PASS (run ID `37866966922`). Source artifact ID 11588687681, bundle SHA256 `c5d7bc4f711a9986f7092367f1f00b97a4defb3638bf7234901aa9995107a24c`. Published `v0.5.84-debug` via run `37867195007` PASS, with unchanged original signed APK SHA256 `90f4ee81e46340d4f6fd4e9f56632598bad68d3e52a944e9490727e53a1a7010` and separate .sha256. **No phone acceptance evidence for this version**; #40 remains open.
