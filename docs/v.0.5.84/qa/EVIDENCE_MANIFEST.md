# v0.5.84 evidence

Source audit against current `main` before modifications: all seven services declared FGS dataSync + stopWithTask=false, use PARTIAL_WAKE_LOCK or BackgroundWorkWakeLock and START_REDELIVER_INTENT. ConversionRunStore.begin reset risk, native cancel-reset risk and potential stale redelivery identified from code. No actual phone reproduction yet.

Keep PR, CI, stable signer, source SHA and future phone QA independently tracked. Signed build not available when this skeleton is created.
