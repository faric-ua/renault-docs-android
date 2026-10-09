# v0.5.84 evidence

Source audit against current `main` before modifications: all seven services declared FGS dataSync + stopWithTask=false, use PARTIAL_WAKE_LOCK or BackgroundWorkWakeLock and START_REDELIVER_INTENT. ConversionRunStore.begin reset risk, native cancel-reset risk and potential stale redelivery identified from code. No actual phone reproduction yet.

Keep PR, CI, stable signer, source SHA and future phone QA independently tracked. Signed build not available when this skeleton is created.

## 2026-10-09 code changes awaiting Android compile

ConversionRunStore and ConversionService now preserve matching persisted run and cancel through redelivery; NativeRdpkgRunStore and PreparationService guard canceled and terminal replay; ConversionActivity postpones and scopes interrupted reconciliation. Added `tests/test_v0584_background_recovery_contract.py`. Native/runtime battery policy/device acceptance still pending. Seven-service matrix: `qa/BACKGROUND_SERVICE_AUDIT.md`.
