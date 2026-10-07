# Renault Docs v0.5.70 — Archive duplicate identity fast-path

Status: **DEVELOPMENT**

Goal: reject obvious already-installed archive volumes before full extraction.

Target behavior:
- inspect archive names/paths first;
- for ZIP, directly read only bounded small index/metadata content when needed;
- derive an unambiguous Renault NT code;
- if exactly one installed volume matches that NT code, return `ALREADY_PRESENT` without extraction;
- preserve full-extraction fallback for ambiguous archives.

No source archive mutation is allowed.
