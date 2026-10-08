# v0.5.79 / build95 — Read-only last .rdpkg source

Status: FEATURE PR CI PENDING / PHONE DIAGNOSTICS PENDING

- Megane II Project Add button opens read-only report of persisted NativeRdpkgRunStore state.
- Report shows stored source URI/name, project ID, source provider document ID/name/size if available, destination, result SHA, timestamps.
- Copy and Close; neither starts/resumes operation nor mutates/deletes source, staging, destination, or saved state.
- Stable-signer install over existing v0.5.78 required to preserve original private SharedPreferences.
- No change to pin/sticky/landscape, ongoing progress UI, file selection or conversion.
- Related source issue #83, model-mismatch #82, immutable originals #81 and Home-picker #79 remain open.
