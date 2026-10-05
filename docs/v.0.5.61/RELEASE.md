# Renault Docs v0.5.61 — shared live progress

Status: **DEVELOPMENT**

## Scope

- issue #25: shared real-progress contract for long-running operations;
- keep the existing thin progress bar visual style;
- compact human-readable stage text only;
- measured determinate progress whenever totals are known;
- indeterminate only while a real total is unavailable;
- smooth/coalesced UI updates without per-file redraw storms;
- preserve durable run-store/service lifecycle behavior.

## Initial target surfaces

- native raw → .rdpkg preparation;
- .rdpkg import;
- .rdpkg export/share;
- .rdproject preparation/share;
- converter.

## Non-goals

- no large progress dashboard;
- no filename/bytes/speed/ETA telemetry in normal UI;
- no fake progress when work cannot be measured.
