# v0.5.11 Phone test

## Install

APK-only update:

1. Renault Menu → 5 — update project.
2. Renault Menu → 8 — download/install latest APK.
3. Renault Menu → 9 is not required if Runtime IR shards from v0.5.9+ already exist.

## Modern menu layout

Open:

```text
Modern
→ NT8183A
→ 101 · ПРИКУРИВАТЕЛЬ
```

Expected menu:

```text
[ Схеми ] [ Розʼєм ]
[ Положення на авто ]
[ Документація ]
```

Absent actions may disappear. Unknown future actions may appear below the known rows.

## NM parity

Tap:

```text
Розʼєм
→ one vehicle/criteria variant
```

Expected:

```text
Документи розʼєму

› Схема розʼєму
› Опис контактів
```

The contact/pin table must **not** appear immediately.

Tap `Опис контактів`.

Expected:
- pin/contact description opens as a separate native document;
- rows are plain data, not button-like cards;
- empty rows are skipped;
- actionable links elsewhere still use the `›` affordance.

## Classic contrast

Open plain Classic and navigate to the same nomenclature connector.

Expected:
- upper connector PDF remains normal;
- lower pin/contact description is readable;
- transparent legacy detail page should sit on a light/white canvas instead of dark background.

## Not part of this build

Do not use this test to judge BUG-001 PDF toolbar width. That bug remains recorded for later.
