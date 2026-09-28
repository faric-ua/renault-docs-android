# Renault Docs v0.5.10 — Native menu UX + launcher icon

Date: 2026-09-24

## Phone feedback addressed

Native Runtime IR now works on the real NT8183A/101 dataset after the v0.5.9 sharding fix.

This release focuses on presentation/semantics:

- replace legacy abbreviations in the primary native menu:
  - `SCH` → `Схема`;
  - `NM` → `Розʼєм`;
  - `PC` → `Положення на авто`;
- group constant documentation actions:
  - `GENE`;
  - `PLATFUSI`;
  - `AIDE`;
  under one `Документація` entry;
- hide the legacy `blank` placeholder;
- rename `CRITERE` to `Критерії / скорочення`;
- visually distinguish actionable rows (`› ...`) from non-link/group rows;
- show `немає посилання` for unresolved actions instead of silently pretending they work.

## Classic button

The native screen's `Classic` button now opens pure untouched Classic runtime.

It no longer starts the old hybrid target/projection flow. The phone DBG for section 105 showed that hybrid state could remain at:

```text
phase: waiting
attempts: 48
hasMenuWindow: false
root: INDEX.HTM
```

That is not the desired behavior for a button explicitly named Classic.

## Launcher icon

The app now declares a real adaptive launcher icon instead of falling back to the generic Android application icon.

Concept:
- dark technical background;
- blue technical-document outline;
- white schematic traces;
- yellow connector node.

The icon is intentionally an original Renault Docs technical symbol, not a copy of the Renault trademark.

## Google Play Protect

The launcher icon change does not remove Play Protect's sideload warning.

The development APK is installed outside Google Play and the developer/app has not been verified through Google Play distribution. On some Android/Play Protect configurations Google therefore shows an unknown/unverified developer warning.

This is separate from the app icon and separate from the Runtime IR implementation.

For development, the tester may explicitly choose the platform's `Усе одно встановити` path.

For public distribution without that class of warning, the app should later use the normal Google Play developer verification / signing / testing or release channel.

## Dataset

No Runtime IR regeneration is required specifically for these UI changes if the phone already has the v0.5.9 sharded package.

v0.5.9+ dataset requirement still applies:
`runtime-ir-index.json` and per-section shards must exist.
