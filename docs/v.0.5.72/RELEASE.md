# Renault Docs v0.5.72 — archive-root raw duplicate fast-path

Status: **DEVELOPMENT**

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
