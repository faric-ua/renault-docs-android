# Renault Docs v0.5.72 — archive-root raw duplicate fast-path

Status: **MERGED / MAIN CI PASS / PHONE RE-TEST PENDING**

## Phone evidence

The provided phone video still shows full extraction:
- `Розпаковую ZIP… · Файлів: 261 / 4378`;
- later `Розпаковую ZIP… · Файлів: 894 / 4378`.

Therefore the early duplicate fast-path is **not accepted yet**.

## Additional root case

Archive inspection previously discarded a Renault raw root when the entrypoint itself was at archive root:

`INDEX.HTM`
`data/...`

For that case the raw-root parent is an empty relative path. The inspector filtered it out even when the bounded INDEX probe had already recovered an unambiguous NT code.

## Fix

- keep archive-root raw hints;
- derive a safe display/fallback leaf from the archive filename;
- preserve the probed NT code from root INDEX/ACCUEIL content;
- allow duplicate matching by the NT code before extraction;
- keep ambiguous archives on the existing safe extraction path.

Regression coverage now includes a root-level `INDEX.HTM` with `NT8266A`.


## CI evidence

- PR #67 merged.
- Runtime source: `eacbbc1f914d4f35564724501a3edb126fd71963`.
- Tests #535: PASS.
- Android Debug APK #135: PASS.
- Artifact: `Renault-Docs-v0.5.72-Debug`.
- Artifact ID: `11511521715`.
- Digest: `sha256:c670621c76309294d15c6a1f8335a2e5578fd8c9425314cf592d033a07378c62`.

Next UI after phone acceptance: issue #68.
