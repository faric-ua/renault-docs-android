# Renault Docs v0.5.13 — Modern section chrome and tile layout

Date: 2026-09-24

## Scope

APK-only UI update. Runtime IR regeneration is **not** required.

This release does **not** address BUG-004 (missing non-3-digit Classic catalog entries). That converter/catalog fix remains separate and will require Renault Menu → 9 when implemented.

## Global section navigation

The native Modern section screen now uses two levels of navigation.

Top/global row:
- back;
- Home;
- section search;
- Settings.

Section context row:
- only the current section identifier, for example `101`;
- a compact segmented mode switch:
  - `Modern` active;
  - `Classic` available as fallback/reference.

The section title is not repeated in the top row because the native content already shows:

```text
101 — ПРИКУРИВАТЕЛЬ
```

## Persistent primary menu

The four primary Modern actions keep a stable position and never disappear:

```text
[ Схеми ] [ Розʼєм ]
[ Положення на авто ]
[ Документація ]
```

If the current section has no corresponding Runtime IR action, the tile remains visible but is disabled/dimmed.

This avoids layout jumping between sections such as 101 and 107.

Unknown/future menu actions remain data-driven and render below the fixed primary group.

## Tile presentation

Primary actions now use rounded technical tiles rather than default Android gray buttons.

Native select/group blocks are also rendered as cards:
- group label/header inside the card;
- one or more actionable choices inside;
- unavailable actions are visibly disabled;
- prompt labels such as `ВЫБЕРИТЕ СХЕМУ` remain lightweight text outside the cards.

This keeps the interface dense while making the information hierarchy clearer.

## Search behavior

The search icon on the native section screen reopens the current Modern volume with its section search field already visible.

## Version

- versionName: `0.5.13`
- versionCode: `29`
