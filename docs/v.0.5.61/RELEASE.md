# Renault Docs v0.5.61 — shared live progress

Status: **MERGED / FINAL MAIN CI PASS / PHONE PASS / CLOSED — 2026-10-05**

## Final source

- PR #28: `v0.5.61 — shared live progress`
- merge commit: `c36e10ddb5fb1a24e9a37d2dc21321a577ed69ed`
- versionName: `0.5.61`
- versionCode: `77`

## Final CI

- Tests #443 — PASS
- Android Debug APK #116 — PASS
- final main artifact run: `37347713735`

## Phone acceptance

Final `main` APK was installed over the existing app without uninstall/data reset.

Confirmed after install:
- app reports `v0.5.61`;
- Megane II remains at 2 volumes;
- Laguna II remains at 10 volumes;
- Kangoo II remains at 1 volume;
- existing data/projects are preserved.

Accepted UX/runtime behavior:
- shared thin live-progress presentation;
- visible file counters;
- smoother measured progress with balanced file/byte weighting;
- durable lifecycle-safe progress for long operations;
- named project-volume stages such as `Пакую том 1/2 - NT8340A · 2006-04-18…`;
- canonical target filename shown while preparing `.rdpkg` / `.rdproject`;
- prepared project reuse on normal Share;
- explicit repack action when rebuild is desired;
- project/volume deletion flows aligned to the same dialog sequence.

## Scope completed

- native raw → .rdpkg preparation;
- .rdpkg import;
- .rdpkg export/share;
- .rdproject preparation/share;
- converter progress presentation;
- shared persistent progress model and lifecycle reattachment.

Issue #25 is complete.
