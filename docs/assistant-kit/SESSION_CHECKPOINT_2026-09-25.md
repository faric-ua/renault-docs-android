# Renault Docs — Session checkpoint 2026-09-25

## Purpose

Canonical stop point for the next session. Read this file together with `PROJECT_LEDGER.md` and `CURRENT_HANDOFF.md` before changing code.

## Current release state

Current app release candidate:
- versionName: `0.5.17`;
- versionCode: `33`;
- merged app source: `b56b29b64cddd0cdf11531e7a1e543c45c5593e9`;
- main Tests run: `36081274501` — PASS;
- main Android Debug APK run: `36081274495` — PASS;
- artifact: `Renault-Docs-v0.5.17-Debug`;
- artifact id: `10841149488`;
- APK SHA-256: `a72784d9c5519e0830c30ec2f213c20f7c622a33f5112d2c9ae2f5d46b4237ed`;
- artifact ZIP SHA-256: `5fd8223e9a70b6cbf65e519640140923ea7197181018a69276d2bd84d9b671d7`;
- APK-only change; Runtime IR/package regeneration is NOT required;
- Renault Menu → 9 is NOT required for v0.5.17.

Documentation-only commits after the app build do not change the APK source.

## Phone evidence already accepted

v0.5.16 phone evidence:
- adaptive native abbreviation table: PASS;
- E2/E3-style vertically centered short cells: PASS;
- connector menu contains:
  - `Схема + піни розʼєма`;
  - `Схема розʼєму`;
  - `Опис контактів`;
- combined scheme + pins view: PASS;
- `101_1(pines).pdf`: PASS;
- `101_2(pines).pdf`: PASS;
- opaque Renault connector/source ID rule: PASS*.

Additional real connector-ID evidence:
- source drawing: `120_18.PDF`;
- exported contacts: `120_18(pines).pdf`.

Meaning of PASS*:
representative real source IDs are confirmed, including a non-trivial suffix `_18`, but the complete family of all `120_*` variants has not been enumerated.

Critical rule:
`_1`, `_2`, `_3`, `_18`, etc. are part of the opaque Renault source ID. They are not duplicate-file counters and their semantics must not be inferred from the suffix.

## Why v0.5.17 exists

Phone test found that generated structured-table PDFs did not reliably show the table column-name header.

Code inspection also found that continuation pages did not repeat table headers.

v0.5.17 changes `NativeTablePdfExporter` so:
- leading source rows marked as headers are separated from body rows;
- the source header is drawn before the first body row;
- the same header is repeated after each PDF page break;
- headerless source tables do not receive invented headers;
- adaptive column widths, vertical centering, portrait/landscape behavior, connector combined view, and opaque filename IDs remain unchanged.

Tracked as BUG-006.

## Exact next phone test

Start tomorrow with v0.5.17:

1. Renault Menu → 5.
2. Renault Menu → 8.
3. Do NOT run point 9.
4. Open `Modern → NT8183A → 101 → Критерії / скорочення`.
5. Export `101(abbreviations).pdf`.
6. Confirm the first PDF table row contains:
   - `СОКРАЩЕНИЯ`;
   - `ПОЛНЫЕ НАИМЕНОВАНИЯ`.
7. Open one 101 connector variant → `Опис контактів` → export PDF.
8. Confirm the pin/contact PDF contains its source column-name header and retains the exact filename such as `101_1(pines).pdf` or `101_2(pines).pdf`.
9. If practical, export a table long enough for page 2 and confirm the table header repeats on the continuation page with no dropped/duplicated body rows.

If the header is still missing on page 1 for a specific source table, do not keep patching the PDF renderer blindly. Inspect Runtime IR / converter output for that table: the legacy Renault HTML may be using ordinary `td` cells instead of `th`, so the converter may not mark the source row as a header. That would become converter/Runtime-IR work and would require package regeneration after the converter fix.

## Next work after v0.5.17 phone gate

If BUG-006 passes, return to the major open converter task:

BUG-004 — full Modern catalog parity with Classic.

Known Classic IDs prove identifiers must be opaque strings, including:
- `101`;
- `1405`, `1406`, ...;
- `R15`, `R21`, `R262`, `R325`;
- `MAH`, `MYH`, `MA`, `MB`, `NT`, `NU`, etc.

BUG-004 requires converter/package regeneration and therefore Renault Menu → 9 after implementation.

## Do not regress

- Classic remains available.
- Connector IDs remain opaque.
- Never manufacture `_1/_2/_3` for filename collisions.
- Preserve `101_1`, `101_2`, `120_18`, etc. exactly.
- Keep adaptive native/PDF table widths.
- Keep vertical centering for short cells in wrapped rows.
- Keep combined scheme + pins action.
- Keep `(pines)` naming.
- Keep documentation updated with every finding/change.

## Restart sentence

Next session can begin with:

> Continue Renault Docs from `SESSION_CHECKPOINT_2026-09-25.md`: install/test v0.5.17 table-PDF headers first; if PASS, proceed to BUG-004 full Classic catalog converter parity.
