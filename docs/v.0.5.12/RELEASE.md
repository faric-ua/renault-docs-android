# Renault Docs v0.5.12 — PDF toolbar width + compact Modern layout

Date: 2026-09-24

## Scope

APK-only update. Runtime IR regeneration is **not required**.

## PDF toolbar

Real phone screenshots showed that the upper PDF control row stayed viewport-sized while the PDF page grew at 150–200% zoom.

Result:
- the PDF content became wider;
- the toolbar/control strip did not follow that width;
- controls such as `По ширині` looked clipped/truncated.

v0.5.12 ties the toolbar width to the same zoom-derived display width as the PDF page.

The `+` zoom button is also moved directly after the zoom input/dropdown:

```text
…  −  [100%][▼]  +  [По ширині]  [PDF]
```

## Modern top bar

The section title is constrained to one line.

Example:

```text
101 · ПРИКУРИВАТЕЛЬ
```

`Розділи` and `Classic` remain on the same top bar, but use smaller text/padding/height so the title does not wrap unnecessarily.

## Section heading

Legacy-looking panel titles such as:

```text
CMP 101
```

are no longer shown as the primary native section heading.

Modern uses:

```text
101 — ПРИКУРИВАТЕЛЬ
```

using the section code/title already present in Runtime IR.

## Menu density

The established row structure remains:

```text
[ Схеми ] [ Розʼєм ]
[ Положення на авто ]
[ Документація ]
```

but buttons, padding and row gaps are reduced to use less vertical space.

If a section does not have one of these actions, no fake action is added.

## Contact / pin description table

Native contact descriptions are changed from wrapped text blocks to a horizontal table-like surface:

- each source row remains on one visual line;
- rows use monospace text for easier column scanning;
- the table can be dragged horizontally;
- vertical document scrolling remains unchanged;
- rows remain static data, not fake buttons.

This applies to the structured contact/pin description opened from `Опис контактів`.
