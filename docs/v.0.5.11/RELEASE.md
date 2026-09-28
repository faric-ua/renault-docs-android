# Renault Docs v0.5.11 — Modern menu layout + NM parity + Classic contrast

Date: 2026-09-24

## Scope

APK-only update. Runtime IR dataset regeneration is **not required**.

## Modern section menu layout

The section menu now uses a stable three-row layout whenever those actions exist:

1. `Схема` + `Розʼєм`;
2. `Положення на авто`;
3. `Документація`.

This is presentation only. The underlying action availability still comes from Runtime IR, so missing actions are not invented and unknown future actions remain data-driven.

## Nomenclature parity

Classic nomenclature `LIENNM/...HTM` contains two child documents:

- `dessin` — connector drawing PDF;
- `alveoles` — pin/contact description HTML.

Runtime IR already preserved both documents separately. The first native renderer incorrectly flattened the contact table directly into the NM result.

v0.5.11 keeps the documents separate in Modern:

- `Схема розʼєму`;
- `Опис контактів`.

The pin/contact table opens only when `Опис контактів` is selected.

Static pin rows are rendered as plain data rather than button/card UI, making links/actions visually distinct from non-clickable information.

## Classic nomenclature contrast

A real phone screenshot showed the lower Classic pin-description frame almost unreadable: legacy black text was drawn over the app's dark WebView canvas.

For a non-hybrid top-level Classic `INDEX.HTM` volume, the WebView canvas is now white.

Legacy pages/frames with explicit colors keep those colors; transparent old detail pages receive the light canvas expected by the original documentation.

## Existing open bug

BUG-001 (PDF toolbar/control frame width) remains open and is not changed in v0.5.11.
