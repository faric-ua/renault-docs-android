# v0.5.10 Phone test

## Install

If v0.5.9 Runtime IR shards already exist:

1. Renault Menu → 5 — update project.
2. Renault Menu → 8 — install v0.5.10.

No point 9 is required only for the v0.5.10 UI/icon change.

If the dataset is still pre-v0.5.9, run point 9 first.

## Test NT8183A / 101

Open:
`Modern → NT8183A → 101`.

Expected primary menu:
- Схема;
- Розʼєм;
- Положення на авто;
- Документація.

Expected:
- no visible `blank`;
- Документація exposes Загальна документація / Запобіжники / Довідка;
- CRITERE appears as Критерії / скорочення;
- clickable rows have `›` affordance;
- group/prompt rows are plain text;
- unresolved rows are visibly marked `немає посилання`.

## Classic regression

Tap `Classic`.

Expected:
- pure Classic volume opens;
- no `targetSection` hybrid wait/projection state is started by the native Classic button.

## Launcher icon

Expected:
- launcher / installer surfaces use the Renault Docs technical icon instead of the generic Android placeholder.

Play Protect may still show the sideload/unverified-developer warning; that is not an icon regression.
