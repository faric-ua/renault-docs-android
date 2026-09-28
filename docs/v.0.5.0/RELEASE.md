# Renault Docs v0.5.0 — Native section navigation wave 1

## Purpose

Replace the first major piece of legacy Renault frame navigation with Technical Blue native Android UI.

The volume list was already native. v0.5.0 adds the next level:
`volume → native sections → legacy content`.

## Dataset package

A new generated artifact is added:

`_renault/modern-sections.json`

It is built by the existing fast package update flow:

`Renault → 9 — Оновити Fast/Modern package`

This does not reconvert the normalized dataset.

The section index builder:
- starts from each volume INDEX.HTM;
- follows legacy frame/iframe sources;
- extracts rows/links whose visible text begins with a three-digit Renault section code;
- supports table rows where code and label live in separate cells;
- supports ordinary anchors and image-map areas;
- handles common JavaScript navigation href/onclick patterns;
- handles declared legacy encodings including Windows-1251;
- falls back to ranked navigation-like HTML candidates when the frameset path is not enough;
- deduplicates by section code;
- sorts sections numerically.

Example output:
- 101 · ПРИКУРИВАТЕЛЬ
- 103 · ГЕНЕРАТОР
- 105 · ...
- 107 · АККУМУЛЯТОР

## Android Modern volume screen

Tapping a volume card in Modern now opens a native `ModernVolumeActivity`.

The screen contains:
- Back;
- volume title;
- Home;
- Search;
- Settings;
- Classic fallback;
- native section count;
- compact one-column Technical Blue section rows.

Each section row contains:
- prominent three-digit code;
- legacy human-readable label;
- navigation chevron.

Search filters the native section list by code/title.

## Legacy content boundary

v0.5.0 deliberately does not rewrite the actual Renault document content yet.

Selecting a native section opens its legacy HTML entrypoint in the existing safe viewer.

Therefore this wave removes the old frame menu from the navigation path while keeping legacy content rendering available.

## Modern bridge

When a section is open in the viewer, the `Modern` button now returns to the native section list for that same volume.

Classic fallback remains available.

## Fallback contract

If:
- `modern-sections.json` is missing;
- the selected volume is absent from the index;
- no sections could be discovered;

the app does not fail.

It shows a native fallback card and offers:
`Відкрити Classic`.

## Required dataset refresh

After installing v0.5.0, run once:

`Renault → 9 — Оновити Fast/Modern package`

This creates `_renault/modern-sections.json` and rebuilds Fast Pack with the new small JSON file.

It is not a full conversion.
