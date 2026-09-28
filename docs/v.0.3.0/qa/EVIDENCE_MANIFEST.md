# v0.3.0 Evidence Manifest

## Pre-change phone evidence

- v0.2.1: native Library + SAF WebView successfully opened Laguna and legacy volume UI.
- v0.2.2: user confirmed native PDF rendering works.
- user still reports noticeable latency in legacy navigation.

## CI

Exact tested app source:
`0bb1a98b2e5b678f2974101d3e606486d2a133c4`

- Python Tests run `35922042245` — PASS.
- Android Debug APK run `35922042240` — PASS.
- JVM tests — PASS.
- stable development signer — PASS.
- APK build/apksigner/zipalign/aapt/SHA-256/artifact upload — PASS.

This is build evidence only. Modern mode and PDF export remain PHONE NOT TESTED.

## Phone

Confirmed by user:
- PDF Save works.

Not passed:
- overall speed improvement. User reports no material speed gain.

Therefore v0.3.0 PDF export has phone evidence, while performance remains open and moves to v0.3.1 Fast Pack.
