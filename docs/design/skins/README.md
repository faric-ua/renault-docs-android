# Renault Docs Skin System

Status: active design contract.

## Purpose

Renault Docs separates product behavior from visual presentation.

A **skin** controls visual tokens and component styling.
A **layout variant** controls arrangement of navigation/content.
A **feature contract** controls behavior and must remain stable across skins.

Skins must never change document meaning, dataset structure, navigation semantics, backup safety, or PDF correctness.

## Status lifecycle

Every skin uses one of these states:

- `concept` — exploratory visual direction;
- `approved-concept` — selected direction, not yet implemented as production UI;
- `implementation` — actively being wired into app code;
- `implemented` — available in the app;
- `deprecated` — retained for history but not offered to users.

## Folder contract

Each skin lives under:

`docs/design/skins/<skin-id>/`

Recommended contents:

- `SKIN.md` — purpose, state, scope, visual intent;
- `TOKENS.md` — colors, spacing, typography, radii, elevation;
- `UX_CONTRACT.md` — navigation and behavior that the visual design must preserve;
- `IMPLEMENTATION.md` — mapping from design tokens/components to Android code;
- `concept-*.svg` — canonical editable concept sources when available;
- `concept-*.png` — phone-friendly previews;
- `concept-*.pdf` — vector previews when generated from SVG;
- `reference-*.png` — raster references selected during design discussion.

## Naming

Skin IDs use lowercase kebab-case.

Examples:
- `technical-blue`
- `graphite`
- `light-service`

Component/token names should describe intent, not a specific screen:
- `surface-primary`
- `surface-raised`
- `accent-primary`
- `text-primary`
- `control-height`
- `radius-control`

Avoid names such as `laguna-blue-button`.

## Required invariants across all skins

Every skin must preserve:

1. readable contrast;
2. touch targets suitable for phones;
3. centered labels inside controls;
4. visible focus/selected states;
5. Back behavior;
6. Classic ↔ Modern bridge;
7. PDF controls;
8. Search access;
9. Settings access;
10. no destructive action without its existing safety contract.

## Canonical source rule

When a concept is recreated as SVG, the SVG is canonical.
PNG/PDF are derived outputs.

When only a raster concept exists, keep the raster reference in the skin folder and create a vector/source representation before implementation if exact reproducibility matters.

## Current first skin

`technical-blue` is the first approved concept family for the new Renault Docs document viewer/navigation UI.
