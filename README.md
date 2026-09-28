# Renault Docs Android

Проєкт для адаптації офлайн-документації Renault до Android/Linux та сучасного браузера.

## Українська документація

Якщо хочеш зрозуміти проєкт без читання коду, починай тут:

- `docs/ua/README.md` — як усе працює + Mermaid-діаграми;
- `docs/ua/LIBRARY.md` — універсальні datasets і бібліотека автомобілів;
- `docs/ua/SHARING_AND_APK.md` — як ділитися конвертером без Termux/Python;
- `docs/ua/ANDROID_BLUEPRINT.md` — майбутні екрани Android APK;
- `docs/ua/ANDROID_APP_RULES.md` — правила lifecycle/windows/SAF/APK;
- `docs/ua/PHONE_WORKFLOW.md` — робота з репозиторієм на Android;
- `docs/ua/QUICK_LAUNCH.md` — швидкий запуск документації;
- `docs/ua/CONVERTER.md` — що робить конвертер;
- `docs/architecture.md` — технічна архітектура;
- `docs/initial-analysis.md` — що знайдено в Laguna II.

Для нової assistant-сесії канонічна точка входу: `ASSISTANT_CONTEXT_INDEX.md`.

## Поточні шляхи на Android

Живий Git-репозиторій зберігається у приватному сховищі Termux, а Renault-документи, datasets і APK — у shared storage.

Репозиторій:

```text
$HOME/renault-docs-android/
```

Оригінальна документація:

```text
/storage/emulated/0/Documents/Renault/laguna 2 2001-2006/
```

Normalized build:

```text
/storage/emulated/0/Documents/Renault/laguna 2 2001-2006_android/
```

Усі значення лежать у `config/current-device.json`.

## Поточний набір

Перший проаналізований архів: **Renault Laguna II 2001–2006**.

- 61 759 файлів;
- 12 686 PDF;
- 48 305 HTM;
- основна підтверджена несумісність — case-sensitive файлові системи.

Реальна конвертація на Android уже пройдена:
- 1 507 змінених текстових файлів;
- 16 877 static-case замін;
- 4 dynamic print-case заміни;
- 16 881 змін загалом.

Browser smoke підтвердив legacy HTML/frames/navigation. Відомий наступний compatibility layer — автоматичний PDF rendering замість Android Chrome кнопки «Відкрити».

## Архітектура

Проєкт **не прив'язаний до Laguna II**.

Кожен конвертований комплект стає dataset з `renault-dataset.json`.

Один universal viewer використовує будь-який dataset:

```text
Renault source
   ↓ converter
normalized dataset
   ↓
┌──────────────┬─────────────────┐
│ Browser      │ Android Library │
│ viewer       │ + universal UI  │
└──────────────┴─────────────────┘
```

Android app у Reference mode зберігатиме persistable SAF URI + metadata й створюватиме плитку для кожного підключеного автомобіля/тому.

## Швидкий запуск зараз

Перейти в код:

```bash
reno-code
```

Запустити вже конвертовану документацію:

```bash
reno-docs
```

Додатково:

```bash
reno-status
reno-stop
```

## Конвертер

```bash
python convert.py
```

Конвертер читає шляхи й dataset metadata з `config/current-device.json`.

Для вже конвертованої Laguna II manifest можна додати без повторної конвертації:

```bash
python tools/create_dataset_manifest.py
```

Перевірити datasets:

```bash
python tools/list_library.py "/storage/emulated/0/Documents/Renault"
```

## Android engineering rules

У `docs/assistant-kit/` перенесені та адаптовані reusable rules із перевіреного YTM Importer skeleton:

- Activity recreation/lifecycle;
- modal first-frame geometry;
- Back/navigation ownership;
- generic Tiles;
- SAF/persisted folder access;
- permission minimization;
- adaptive actions;
- APK signing/verification;
- release docs + real-phone QA separation.

YTM-specific OAuth/playlist/business logic, branding, permissions та version numbers **не копіюються**.

## Поточні інструменти

- `convert.py` — простий запуск конвертера;
- `tools/analyze_archive.py` — аудит структури та посилань;
- `core/convert_paths.py` — основна логіка нормалізації;
- `core/dataset_manifest.py` — dataset contract;
- `core/library.py` — library scanner;
- `tools/create_dataset_manifest.py` — manifest для вже готового dataset;
- `tools/list_library.py` — список datasets;
- `tools/package_dataset.py` — каталог, інструкція та inventory внутрішніх томів;
- `web/serve.py` — локальний HTTP server;
- `tests/` — автоматичні тести.

Оригінальні Renault HTML/PDF/GIF/ICO та архіви у GitHub не зберігаються.


## Android APK — v0.1.0 skeleton

Перший реальний Android module знаходиться в `android/`.

Поточний milestone:

```text
Launch
  ↓
Library
  ↓
Додати готовий dataset
  ↓
Android SAF folder picker
  ↓
renault-dataset.json validation
  ↓
persistable read URI
  ↓
Laguna tile
  ↓
Viewer placeholder
```

Цей APK ще не конвертує стару Windows-папку всередині застосунку. Це наступна хвиля після real-phone PASS фундаменту.

Debug APK збирається GitHub Actions workflow:

```text
.github/workflows/android-debug.yml
```

Без `MANAGE_EXTERNAL_STORAGE` і без Termux/Python на телефоні користувача.


## Browser version — one file launch

Браузерна версія лишається окремим шаром у цьому ж репозиторії:

```text
core/      — спільна логіка
web/       — browser runtime
android/   — APK
```

Для ручного запуску з кореня:

```bash
bash browser.sh
```

А після встановлення Termux aliases:

```bash
reno-browser
```

`browser.sh` сам:
- перевіряє готовий dataset;
- при потребі генерує `renault-dataset.json` + `_renault/START.html`;
- перевіряє PDF.js і один раз встановлює його, якщо відсутній;
- запускає/reuses local server;
- відкриває browser version.


## Renault Menu

Для звичайного використання можна взагалі не пам'ятати Termux-команди.

Файл у корені:

```text
menu.sh
```

Termux:Widget показує лише два project buttons:

```text
Renault
YTM Importer
```

Кожна кнопка відкриває власне terminal menu. Renault menu містить browser/code/status/stop/update actions. YTM menu містить Code / Build APK / Download APK. Це обходить обмеження Termux:Widget, де вкладені папки відображаються як шляхи, а не як розкривні групи.


## Browser transfer kit

Для перенесення browser version на інший Android-телефон є окремий portable package source:

```text
transfer/browser-version/
```

Згенерувати один ZIP:

```bash
python tools/build_browser_transfer_kit.py
```

За замовчуванням архів створюється тут:

```text
/storage/emulated/0/Documents/Renault/packages/Renault-Browser-Transfer-Kit.zip
```

ZIP містить browser/runtime код, installer і українську інструкцію, але **не містить Renault dataset** з HTM/PDF/GIF. Dataset переноситься окремо під `/storage/emulated/0/Documents/Renault/`.

GitHub Actions workflow `.github/workflows/browser-transfer-kit.yml` може зібрати той самий ZIP як artifact.
