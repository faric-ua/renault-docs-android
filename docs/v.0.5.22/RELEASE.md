# Renault Docs v0.5.22 — exact native compact widths + rotation lifecycle

Date: 2026-09-25

## Phone trigger

v0.5.21 phone evidence showed:
- all four native pin columns are present;
- compact columns are still too narrow/clipped instead of fitting their real contents;
- first-column abbreviations/designations should be bold;
- table headers should be visually strong;
- several app-owned child windows/dialogs disappear on phone rotation and the UI falls back to a parent window.

## Native table fix

Native structured tables no longer rely on approximate fraction formulas for every compact technical column.

For all columns before the final description column:
- Android measures the widest displayed cell text in pixels;
- measurement includes the semantic header row;
- header/bold typeface is included in measurement;
- the compact column receives a fixed measured width;
- the final column receives the remaining width.

Result:
- `F5R700`, `2.0`, `2.5`, `3CV`, `3N`, etc. are not clipped merely because a fraction was too narrow;
- 2-column abbreviations/designations use bold text in column 1;
- table headers use bold high-contrast text.

Generic 2-column PDF abbreviations/designations are also bold in column 1.

## Rotation/lifecycle fix

### NativeSectionActivity
Persists the current inline child:
- panel;
- Documentation;
- composite document;
- structured/pin document.

After Runtime IR reload, the same child is restored instead of automatically opening the default/parent panel.

The pending table-PDF export payload is serialized into saved instance state so Android's Save document flow can survive Activity recreation.

### SettingsActivity
Persists/reopens:
- Modern/Classic chooser;
- PDF default zoom chooser;
- PDF zoom-step chooser.

### ViewerActivity
Already preserved WebView/search state. v0.5.22 additionally preserves:
- open section navigator;
- section navigator search query;
- open Frame debug window.

Frame debug is regenerated after rotation rather than storing a potentially large report in the Bundle.

## Audit

Full matrix:
`docs/v.0.5.22/LIFECYCLE_AUDIT.md`

Phone PASS is required before lifecycle closeout.

## Packaging

APK-only.

No converter/Runtime-IR changes.
Point 9 is NOT required when the current package has already been regenerated with v0.5.19+.

## Version

- versionName: `0.5.22`
- versionCode: `38`
- branch: `fix/v0.5.22-lifecycle-table-widths`


## Merge / CI

Merged source:
`b5e685d0b6c6d635ce64132685d00ccacc431007`

Main CI:
- Tests run `36150581053` — PASS;
- Android Debug APK run `36150581047` — PASS;
- artifact `Renault-Docs-v0.5.22-Debug`;
- artifact id `10871432429`;
- APK SHA-256 `f7619833cae34fa552cda3626e5770a9f3ae93bafe40adb38d0fa855258ce9a4`;
- artifact ZIP SHA-256 `a7a6e0af3d69f7ce1cf16de117def9fd51091b20edbd6e565e4f12e4f1dff13d`.

Status: CI PASS; phone lifecycle/table-width validation pending. This release is APK-only and does not require point 9.
