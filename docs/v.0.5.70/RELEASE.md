# Renault Docs v0.5.70 — Archive duplicate identity fast-path

Status: **MERGED / MAIN CI PASS / PHONE RE-TEST PENDING**

Goal: reject obvious already-installed archive volumes before full extraction.

Target behavior:
- inspect archive names/paths first;
- for ZIP, directly read only bounded small index/metadata content when needed;
- derive an unambiguous Renault NT code;
- if exactly one installed volume matches that NT code, return `ALREADY_PRESENT` without extraction;
- preserve full-extraction fallback for ambiguous archives.

No source archive mutation is allowed.


## Implementation / CI

- PR #63 merged to main;
- runtime source: `b9c763237b7aceede742c6aa559f53abf4c83444`;
- Tests #530 PASS;
- Android Debug APK #133 PASS;
- artifact `Renault-Docs-v0.5.70-Debug`, id `11508378680`.

Implemented:
- optional NT identity on archive raw-root hints;
- bounded ZIP entrypoint read, maximum 64 KiB;
- unique installed-volume match by NT code before extraction;
- conservative ambiguous fallback remains unchanged.

Phone re-test with the exact NT8266A duplicate archive is still required.
