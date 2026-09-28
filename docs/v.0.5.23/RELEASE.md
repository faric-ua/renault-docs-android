# Renault Docs v0.5.23 — landscape focus mode + PDF typography

Date: 2026-09-25

## Phone feedback

v0.5.22 portrait native table layout is accepted on the tested 108 connector screen.

Two follow-ups remain:
- in landscape, the native section keeps too much navigation chrome visible;
- generated pines PDF needs stronger technical typography.

## Landscape focus mode

When a native section is already inside a top-level mode and the device is landscape:
- hide Back/Home/Search/Settings toolbar;
- hide section code + Modern/Classic switch;
- hide all other top-level native menu buttons;
- keep only the active top-level menu button stretched full width at the top;
- keep the current child content under it.

Examples:
- if active mode is `Розʼєм`, landscape shows only the full-width `Розʼєм` top button;
- the same rule applies to `Схеми`, `Положення на авто`, `Документація`.

Returning to portrait restores the full toolbar, Modern/Classic switch and all top-level buttons.

The active top-level menu label/action is stored in saved instance state, so rotation does not lose which top-level mode owns the current child screen.

## Pines PDF typography

Font family:
- Android `Typeface.SANS_SERIF` (system sans-serif).

Sizes in v0.5.23:
- normal table body / description: 10 pt, regular;
- technical cells in columns 1-3: 10 pt, bold;
- table header: 14 pt, bold;
- upper connector-info table: 14 pt, bold;
- connector code/title block remains: 15 pt / 17 pt bold;
- criteria remains 10.5 pt bold.

The table header is therefore exactly +4 pt relative to the 10 pt body text.

## Packaging

APK-only.
Dataset/Runtime IR is unchanged.
Point 9 is NOT required.

## Version

- versionName: `0.5.23`
- versionCode: `39`
- branch: `fix/v0.5.23-landscape-focus-pdf-type`


## Merge / CI

Merged source:
`087efd7a61acfcd573af7abc4c14d99e8468aa25`

Main CI:
- Tests `36155433400` — PASS;
- Android Debug APK `36155433415` — PASS;
- artifact `Renault-Docs-v0.5.23-Debug`;
- artifact id `10874000361`;
- APK SHA-256 `937a6e13e4e47c53a66637140a367d7f89b773d26e22fa7afae36d3066a2abc1`;
- artifact ZIP SHA-256 `66268214f0bfe68df90a6ba1e37a0913798fe28c6b73badb987bcdd08f3126ec`.

Status: CI PASS; phone validation pending.
