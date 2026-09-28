# Renault Docs v0.5.8 — Native Runtime IR preview

Date: 2026-09-24

## Goal

Start rendering Modern sections from compiled Runtime IR v2 instead of booting and hiding the full Classic frameset.

Classic remains unchanged and available as the reference/fallback implementation.

## Important architecture rule

Do **not** assume that every Renault year uses the same menu as NT8183A/2001.

Later volumes may contain additional or changed menu actions.

Therefore both compiler and Android renderer are data-driven:

- menu labels come from Runtime IR `action-bar.items[]`;
- panels come from `panels[]`;
- controls come from `controls[]`;
- routes come from `actions[]`;
- documents come from `documents[]`;
- unknown legacy JavaScript actions remain explicit and fall back to Classic.

No `SCH/NM/PC/GENE` button set is hard-coded in the native renderer.

## Runtime IR coverage

Packaging now also generates:

```text
_renault/runtime-ir-coverage.json
```

The audit summarizes all converted volumes/years:

- compile-state distribution;
- panel kinds;
- control types;
- action types;
- route types;
- document types;
- every discovered menu label;
- legacy JavaScript actions that are not yet normalized;
- compiler warnings;
- per-volume statistics.

This is the gate for supporting newer Renault menu variants systematically.

Renault Menu item 12 exports a small copy:

```text
/storage/emulated/0/Documents/Renault/packages/Runtime-IR-Coverage.json
```

## Native section preview

Modern section taps now enter `NativeSectionActivity`.

The screen reads `_renault/runtime-tree.json` schema v2 and renders the selected section generically.

Supported in the first preview:

- native action-bar menu;
- native select/group/prompt/separator presentation;
- native document-list controls;
- open-panel routes;
- PDF routes through the existing Android PDF viewer;
- structured HTML documents rendered as native headings/tables;
- composite nomenclature documents rendered as native metadata/table plus a PDF drawing action;
- explicit Classic fallback for unsupported/dynamic legacy actions.

## Scope

This is a proof-of-concept migration step, not removal of Classic.

The first phone parity target remains NT8183A / 101.

After it works, the coverage report is used to identify menu/control/action variants in 2002–2006 volumes before expanding native coverage.
