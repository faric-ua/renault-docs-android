# Runtime IR Phase 1 — real phone dataset validation

Date: 2026-09-24

Source:
real generated `_renault/runtime-tree.json` from the converted Laguna II 2001–2006 phone dataset.

## Result

Phase 1 is **validated on the real dataset**.

Generated IR reports:

- schema version: 1;
- format: `renault-runtime-ir`;
- source: `legacy-html-compiler`;
- Classic preserved: true;
- Modern data contract: `normalized-json`;
- compiler phase: `navigation-ir-v1`;
- volumes: 10;
- total normalized sections: 2174.

For NT8183A · 2001-01-22:

- Classic entrypoint:
  `Laguna X74 NT8183A 2001_01_22/INDEX.HTM`;
- INDEX redirect candidate:
  `RUS/HTM/ENTREE.HTM`;
- ENTREE outer frame geometry:
  `cols = 216,747`;
- named frames compiled:
  - `titre` → `RUS/HTM/CTITRE.HTM`;
  - `org` → `RUS/HTM/CODE.HTM`;
  - `menu` → `RUS/HTM/MENU.HTM`;
  - `nav` → `COMMUN/HTM/BLANK.HTM`;
  - `doc` → `RUS/DOCUMENT/CLAUSLEG.PDF`;
- Modern section source:
  `RUS/HTM/CODE.HTM`;
- normalized section count: 214;
- section 101:
  - code: `101`;
  - title: `ПРИКУРИВАТЕЛЬ`;
  - legacy entrypoint:
    `RUS/HTM/MENU/101.HTM`;
  - controls/actions/documents are still empty by design in Phase 1.

The compiler state correctly reports Phase 2 work still pending:

- `section-controls`;
- `legacy-js-actions`;
- `document-routing`;
- `asset-dependencies`.

## Architectural conclusion

The real dataset confirms that the chosen split is viable:

```text
converted dataset
├── Classic runtime (preserved)
└── Modern JSON IR (compiled)
```

We now have enough reliable structure to stop learning section navigation from runtime frame guessing.

The next compiler task is to inspect the real legacy source graph behind one section, starting with NT8183A / 101.

## Phase 2 source-bundle gate

A focused diagnostic/export tool is added:

```text
tools/export_runtime_section_bundle.py
```

Renault Menu gains:

```text
10 — Експорт Runtime IR source bundle
```

Default target:

- volume: `NT8183A`;
- section: `101`.

The exporter creates:

```text
/storage/emulated/0/Documents/Renault/packages/Runtime-IR-NT8183A-101-source.zip
```

The bundle contains:

- requested section HTML;
- same-section PC page when present;
- recursively referenced text dependencies (HTML/JS/CSS/etc.);
- a dependency graph;
- references to PDF/images/assets without unnecessarily embedding large binary documents;
- a runtime-tree subset for the selected volume/section.

Important:
the exporter intentionally does **not** start from `CODE.HTM`, because that file links all sections and would turn a focused 101 analysis into a crawl of the whole volume.

## Next step

Generate and inspect the NT8183A / 101 source bundle.

From that real bundle, Phase 2 will define the first concrete native IR schema for:

- controls;
- selector options;
- actions;
- document routing;
- asset dependencies.

The goal remains:
reproduce section 101 from our JSON model while Classic stays available as the parity oracle.
