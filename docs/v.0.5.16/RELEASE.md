# Renault Docs v0.5.16 — opaque connector ID export naming

Date: 2026-09-25

## Scope

APK-only correction on top of v0.5.15. Runtime IR regeneration is **not required**.

v0.5.15 already implemented:
- content-aware table columns;
- vertically centered short cells in wrapped rows;
- portrait 2-column table PDFs;
- combined `Схема + піни розʼєма` view.

v0.5.16 corrects the connector identity/export contract after inspection of real Runtime IR and Renault source naming.

## Renault connector identity rule

Suffixes such as `_1`, `_2`, `_3` are part of the Renault source identifier and must not be treated as duplicate counters.

They also do not have one universal semantic meaning:
- `101_1` / `101_2` are linked to different applicability criteria (DD/DG/E2/E3 and other criteria);
- `120_1` / `120_2` / `120_3` may represent separate physical connectors of one ECU.

Android must therefore preserve the full source ID without interpreting the suffix.

## Source extraction

Pin/contact HTML commonly uses names such as:

```text
T_101_1.HTM
T_101_2.HTM
T_120_1.HTM
```

For pin-table export, v0.5.16 strips only the technical `T_` prefix and preserves the rest of the source stem.

The old v0.5.15 code reconstructed an ID from the current section code plus an optional numeric suffix. That happened to work for `101_1` / `101_2`, but encoded an assumption that is not valid as a general Renault contract.

## PDF filename contract

Pin/contact exports use:

```text
101(pines).pdf
101_1(pines).pdf
101_2(pines).pdf
120_1(pines).pdf
120_2(pines).pdf
120_3(pines).pdf
```

The application must not generate `_1`, `_2`, `_3` merely because a file with the same name already exists. File conflicts are handled by the Android document provider.

Abbreviation export remains, for example:

```text
101(abbreviations).pdf
```

## Version

- versionName: `0.5.16`
- versionCode: `32`
- branch: `fix/v0.5.16-connector-id-export`


## Merge / CI

Merged source:
`b66ecb1a5f0e35250470b4e30ae580062e710121`

Main CI:
- Tests run `36078619569` — PASS;
- Android Debug APK run `36078619634` — PASS;
- artifact `Renault-Docs-v0.5.16-Debug`;
- artifact id `10841531621`;
- APK SHA-256 `cf5c2cff2e66fcd3844ee837b302484e27219b3e0686ce803b713b8fe2234134`;
- artifact ZIP SHA-256 `6695b817fb280d80e0176a021c59920116b6f95fa4eb24b70934e38dfbccee9f`.

Status: CI PASS; real-phone validation pending.
