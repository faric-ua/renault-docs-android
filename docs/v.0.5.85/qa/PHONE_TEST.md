# v0.5.85 phone QA — PAUSED

Only on explicit future user approval. No automatic conversions, source deletion, clearing app data or new imported volumes for tests.

Future isolated acceptance: inspect a ZIP with one root-level INDEX and another nested NT volume, confirm chooser lists both with meaningful labels, select root and verify generated canonical filename/installed identity exactly once; repeat nested; cancel safely. Then test representative nonduplicate ZIP, 7Z and RAR on the device, unsupported/encrypted/corrupt handling, path-collision failure, background/lock, storage space, cancellation and staging cleanup. No claim of these PASS until observed.

Existing v0.5.72 phone evidence proves one exact duplicate ZIP fast-path, **not** complete archive support.

## Artifact readiness (2026-10-09)

Public signed debug prerelease https://github.com/faric-ua/renault-docs-android/releases/tag/v0.5.85-debug ready; stable APK #148 PASS. **Not installed/phone-tested as part of this work**. User explicitly paused extended QA. Does not prove mixed-root output isolation or encryption/CRC/cancel/lock acceptance.
