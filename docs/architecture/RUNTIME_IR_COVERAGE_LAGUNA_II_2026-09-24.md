# Runtime IR coverage — Laguna II 2001–2006

Date: 2026-09-24

Source:
real phone-generated `Runtime-IR-Coverage.json`.

## Result

The current Laguna II dataset contains:

- 10 volumes;
- 2174 sections;
- all 2174 sections compiled as `section-ir-v2`;
- 0 unsupported actions;
- 0 compiler warnings.

This is a strong cross-year validation of Runtime IR v2 for the current Laguna II corpus.

## Menu vocabulary across all 10 volumes

The complete discovered top-level menu label set is:

- `AIDE`;
- `GENE`;
- `PLATFUSI`;
- `SCH`;
- `PC`;
- `NM`;
- `blank` placeholders.

Important correction to the earlier assumption:
the newer Laguna II volumes in this dataset do **not** introduce additional top-level menu labels.

Availability still varies by section:
not every section has SCH, PC or NM, and blank slots vary.

Therefore the UI must remain data-driven even though the current Laguna II corpus is structurally very consistent.

## Coverage totals

Panels:
- general: 2174;
- menu: 2174;
- schematic: 2110;
- pc: 1790;
- nomenclature: 1393.

Controls:
- select: 5677;
- action-bar: 2174;
- document-list: 1790.

Actions:
- route: 62528;
- set-surface-location: 16163.

Routes:
- open-document: 55061;
- open-panel: 7467.

Documents:
- pdf: 47222;
- structured-html: 8345;
- composite-document: 4842.

## Migration conclusion

For the existing Laguna II 2001–2006 corpus:

```text
Runtime IR v2 static compiler coverage
= 2174 / 2174 sections
= 100% compile_state coverage
= 0 unsupported legacy actions
= 0 warnings
```

This does not yet prove phone UI parity for every section, but it means the converter found no unsupported action class in the current corpus.

The next risk is therefore native-renderer parity, not missing compiler semantics.

## Possible Laguna III extension

If Laguna III documentation is found later, treat it as a **new dataset/model family**, not as an assumption that its Classic runtime is identical to Laguna II.

Recommended import gate:

1. preserve the original Classic source;
2. run volume discovery;
3. generate Runtime IR;
4. generate `runtime-ir-coverage.json`;
5. compare:
   - frame topology;
   - panel kinds;
   - menu labels;
   - control types;
   - action types;
   - route types;
   - document types;
   - unsupported JS/warnings;
6. only then enable native Modern rendering for that dataset.

If Laguna III uses the same Renault VISU generation, it may compile with little or no additional work.

If it uses a newer/different shell, the coverage report should expose the new patterns rather than silently forcing them into the Laguna II contract.

## Product architecture implication

The app should evolve from:

```text
Laguna II-specific renderer
```

toward:

```text
Renault Docs
├── Dataset: Laguna II
│   └── Runtime IR v2
├── Dataset: Laguna III (future, if available)
│   └── Runtime IR capability detection
└── Classic fallback per dataset
```

No Laguna III source is currently part of the validated dataset.
