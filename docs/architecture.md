# Архітектура: datasets + universal viewer + Android library

## Головний принцип

Застосунок не прив'язаний до Laguna II.

Конвертер перетворює будь-який підтримуваний Renault-пакет у стандартний **dataset**. Dataset містить документацію та `renault-dataset.json`.

Один universal viewer відкриває всі datasets.

```text
Original Renault package
        ↓
     analyzer
        ↓
     converter
        ↓
normalized dataset + renault-dataset.json
        ↓
 ┌───────────────┬──────────────────┐
 │ Web viewer    │ Android library  │
 │ local server  │ + universal view │
 └───────────────┴──────────────────┘
```

## Dataset contract

Мінімальний контракт:

```text
dataset/
├── renault-dataset.json
├── INDEX.HTM
└── ...
```

Manifest описує:

- стабільний dataset id;
- виробника;
- модель;
- platform code;
- роки;
- тип документації;
- entrypoint;
- viewer profile;
- preview metadata;
- capabilities;
- conversion summary.

Viewer не визначає модель за назвою папки.

## Android library

Фінальний Android app має екран бібліотеки.

Користувач вибирає папку dataset через Storage Access Framework. App зберігає **persistable content URI**, читає manifest і додає плитку.

Сирі filesystem paths типу `/storage/emulated/0/...` використовуються тільки у поточному Termux development workflow.

## Reference mode

Перший режим бібліотеки — reference mode:

- dataset залишається у вибраній користувачем папці;
- app не дублює сотні мегабайт;
- локальна database зберігає URI + metadata + останню сторінку;
- якщо папка переміщена/видалена, tile переходить у стан unavailable і просить перепідключити її.

Пізніше можна додати managed import mode.

## Universal viewer

Viewer працює з manifest + dataset root.

Він відповідає за:

- HTML;
- legacy frames;
- JavaScript;
- GIF/зображення;
- back/forward state;
- PDF interception;
- PDF rendering;
- адаптацію старих `#viewrect=...`.

PDF viewer належить оболонці, а не конкретному dataset. Тому його не потрібно дублювати для Laguna/Megane/Scenic.

## Web target

Web viewer є development/reference runtime.

Поточний Termux launcher читає `build_root` з config та, якщо є manifest, відкриває його `entrypoint`.

## Android target

Android app:

1. **Library** — список підключених datasets.
2. **Dataset tile** — preview, model, years, content type.
3. **Viewer** — один універсальний HTML/WebView runtime.
4. **PDF layer** — власний renderer/handler.
5. **State** — остання сторінка, history, favorites у майбутньому.

## Стратегія гілок

- `main` — стабільний стан;
- `feat/core-...` — converter/dataset contract;
- `feat/web-...` — browser viewer;
- `feat/android-...` — Android library/viewer;
- `fix/...` — точкові виправлення.

Feature branch → tests → PR → merge.


## Детальні runtime-діаграми

Окремі Mermaid-схеми поточної архітектури:

- `docs/architecture/CLASSIC_ARCHITECTURE.md` — повний Classic runtime і серфінг між секціями;
- `docs/architecture/MODERN_CURRENT_ARCHITECTURE.md` — поточний Modern v0.5.7 поверх legacy runtime;
- `docs/architecture/CLASSIC_VS_MODERN_ARCHITECTURE.md` — пряме порівняння Classic/Modern і цільова архітектура з `runtime-tree.json`.


## Канонічний напрямок Modern runtime

Прийняте архітектурне рішення: **Classic зберігається як compatibility/reference mode, а Modern поступово переходить на нормалізований JSON IR, що генерується під час конвертації.**

Детально:
- `docs/architecture/MODERN_NATIVE_IR_PLAN.md`
- `core/runtime_ir.py`

Перший compiler output:
- `_renault/runtime-tree.json`

Ключова мета: довша одноразова конвертація допустима, якщо після неї Modern працює швидко, без запуску/маскування повного Classic FRAMESET runtime.


## RDPKG distribution / Android export

Канонічний контракт одного тому як одного `.rdpkg`, Android-native fast export без Python/Termux та план Kotlin-native full converter:

- `docs/architecture/RDPKG_DISTRIBUTION_AND_ANDROID_EXPORT.md`

## Kotlin-native raw preparation

- `docs/architecture/KOTLIN_NATIVE_RAW_TO_RDPKG.md` — canonical raw SAF → private staging → Kotlin compiler → Fast Pack → `.rdpkg` pipeline and parity/performance contract.
