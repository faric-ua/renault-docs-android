# Renault Docs v0.5.17 — structured-table PDF headers

Date: 2026-09-25

## Scope

APK-only PDF-export correction on top of v0.5.16. Runtime IR regeneration is **not required**.

## Phone evidence from v0.5.16

PASS:
- adaptive native two-column glossary layout;
- vertical centering of short codes in wrapped rows;
- connector menu with combined / scheme / contacts actions;
- combined connector view;
- exact source-derived filenames `101_1(pines).pdf` and `101_2(pines).pdf`.

Additional connector-ID evidence after the initial v0.5.16 gate:
- `120_18.PDF` → `120_18(pines).pdf` is phone-confirmed;
- connector-ID preservation is PASS* (representative, not exhaustive across every possible 120 variant).

New issue:
- generated structured-table PDF does not reliably expose the table's column-name header;
- exporter pagination also failed to repeat the table header on new pages.

## Fix

`NativeTablePdfExporter.drawTable(...)` now separates leading source header rows from body rows:

```text
tableHeaderRows = leading rows marked header
bodyRows        = remaining rows
```

Behavior:
- draw source header rows before the first body row;
- after a page break, draw document metadata and then the same table header rows;
- continue body rows after the repeated header;
- do not invent headers for source tables without a leading header row.

Existing behavior retained:
- adaptive column widths;
- portrait for <=2 columns;
- landscape for wider pin tables;
- vertically centered cell text;
- opaque Renault connector IDs;
- `(pines)` filename suffix.

## Version

- versionName: `0.5.17`
- versionCode: `33`
- branch: `fix/v0.5.17-pdf-table-headers`


## Merge / CI

Merged app source:
`b56b29b64cddd0cdf11531e7a1e543c45c5593e9`

Main CI:
- Tests run `36081274501` — PASS;
- Android Debug APK run `36081274495` — PASS;
- artifact `Renault-Docs-v0.5.17-Debug`;
- artifact id `10841149488`;
- APK SHA-256 `a72784d9c5519e0830c30ec2f213c20f7c622a33f5112d2c9ae2f5d46b4237ed`;
- artifact ZIP SHA-256 `5fd8223e9a70b6cbf65e519640140923ea7197181018a69276d2bd84d9b671d7`.

Status: CI PASS; phone test of BUG-006 is pending.
