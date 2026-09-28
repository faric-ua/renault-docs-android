# v0.5.14 Phone test

## Install

APK-only:

1. Renault Menu → 5.
2. Renault Menu → 8.
3. Point 9 is not required.

## Gate A — abbreviations

Open section 101 → `Критерії / скорочення`.

Expected:
- no `CMP101` heading;
- top heading is `101 — ПРИКУРИВАТЕЛЬ`;
- abbreviation data is a real two-column table;
- left column contains values such as `B74`, `K74`, `DD`, `DG`, `E2`, `SSNAV`;
- right column contains the full description and wraps inside its cell instead of being clipped off-screen.

## Gate B — connector pin description

Open:

```text
101
→ Розʼєм
→ variant
→ Опис контактів
```

Expected:
- header uses `101 — ПРИКУРИВАТЕЛЬ`;
- criteria line such as `DG/E2/...` is immediately below it;
- connector metadata such as `ЭЛПРОВ. САЛОНА` / `B74,K74` is grouped clearly;
- pin data is a bordered native table;
- each logical source row remains one table row;
- long description cells can wrap, like the useful Classic table behavior.

## Gate C — PDF export

On either the abbreviations table or connector pin table tap:

```text
Зберегти таблицю PDF
```

Expected:
- Android asks where to create the PDF;
- save succeeds;
- open the saved PDF;
- title, criteria/metadata and table are readable;
- long tables continue onto additional PDF pages.

## Regression

- v0.5.13 persistent top tiles still work;
- 107 Connector remains visible but disabled;
- Classic runtime is unchanged;
- ordinary PDF viewer remains unchanged.

## Separate known issue

BUG-004 remains open: Classic catalog identifiers such as `1405`, `R325`, `MAH`, `MYH` are still not all present in Modern until converter discovery is redesigned.
