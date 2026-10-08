# v0.5.79 / build95 — Read-only last .rdpkg source

Status: MAIN CI PASS / SIGNED APK READY / PHONE DIAGNOSTICS PENDING

- PR #84 merged; source `b6fdfa1efc90f0c4db9be2fc30898ddb8dfac58a`.
- Main Tests #561 PASS; signed Android Debug APK #142 PASS; artifact `Renault-Docs-v0.5.79-Debug` id `11553367269`, digest sha256 `a0ed0807dc2b43d2eb92c83014e426bb0a8ea7c521d1cc7f079c927ceb4da695`.

- Megane II Project Add button opens read-only report of persisted NativeRdpkgRunStore state.
- Report shows stored source URI/name, project ID, source provider document ID/name/size if available, destination, result SHA, timestamps.
- Copy and Close; neither starts/resumes operation nor mutates/deletes source, staging, destination, or saved state.
- Stable-signer install over existing v0.5.78 required to preserve original private SharedPreferences.
- No change to pin/sticky/landscape, ongoing progress UI, file selection or conversion.
- Related source issue #83, model-mismatch #82, immutable originals #81 and Home-picker #79 remain open.
