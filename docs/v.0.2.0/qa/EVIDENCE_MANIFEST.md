# v0.2.0 Evidence Manifest

## CI

Exact tested app source:
`47a536027b2c35b69a8e0dedf17f6e1ec140cd49`

GitHub Actions:
- Android Debug APK run `35908968101` — PASS;
- Python Tests run `35908968049` — PASS.

Android CI proved:
- Kotlin/JVM unit tests pass;
- debug APK builds;
- APK passes apksigner verification;
- APK passes zipalign verification;
- package/sdk metadata can be read by aapt;
- SHA-256 file is generated;
- artifact upload succeeded.

This is build evidence only, not real-phone PASS.

## Phone

Pending.

Expected phone evidence:
- Library with enabled Converter button;
- Converter screen before selection;
- source selected;
- source + destination selected;
- same state after rotation;
- Cancel from SAF;
- Back to Library.
