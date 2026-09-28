# Як ділитися конвертером і готовою документацією

## Головна ціль

Для людей, які не хочуть ставити Termux/Python/Git, фінальний формат — **один Android APK**.

Сценарій:

```text
Встановити Renault Docs.apk
        ↓
Додати документацію
        ↓
Вибрати папку Renault
        ↓
Вибрати папку результату
        ↓
Конвертувати
        ↓
Готовий dataset
        ↓
Додати в бібліотеку / Відкрити
```

## Що буде створювати конвертер

Після конвертації вихідна папка матиме не лише старі HTM/PDF/GIF, а й службовий шар:

```text
converted-dataset/
├── renault-dataset.json
├── _renault/
│   ├── START.html
│   ├── README_UA.html
│   └── volumes.json
├── <оригінальні Renault папки/томи>
├── INDEX.HTM
└── ...
```

### renault-dataset.json

Описує:
- модель;
- роки;
- platform;
- entrypoint;
- catalog entrypoint;
- список внутрішніх томів;
- тип документації.

### _renault/START.html

Людський каталог із плитками всіх знайдених внутрішніх томів/редакцій.

### _renault/README_UA.html

Коротка інструкція прямо всередині готової папки.

### _renault/volumes.json

Машинозчитуваний список усіх внутрішніх томів для Android app.

## Внутрішні томи

Один архів Renault може містити кілька незалежних редакцій документації.

Конвертер не повинен вважати кожну таку папку окремим автомобілем у глобальній бібліотеці.

Правильна модель:

```text
Library
  └── Renault Laguna II 2001–2006
        ├── NT8183A · 2001-01-22
        ├── NT8218A · 2002-05-01
        ├── NT8236A · 2002-11-18
        ├── ...
        └── NT8328A · 2006-05-09
```

Тобто одна плитка автомобіля може містити багато томів/редакцій.

## Що з ярликами

На Android звичайні Windows `.lnk`/BAT-style ярлики не є переносимим рішенням.

Тому маємо два рівні:

1. **Всередині dataset** — `START.html` з плитками/посиланнями.
2. **У Renault Docs APK** — плитка dataset у бібліотеці.

Пізніше APK може також створювати Android launcher shortcut для конкретного dataset, але це буде функція застосунку, а не файл усередині папки.

## APK без додаткового ПЗ

Щоб друг отримав буквально один файл:

```text
Renault-Docs-vX.Y.Z.apk
```

в APK мають бути:
- converter core;
- folder picker через SAF;
- progress/result screen;
- manifest/package generator;
- library;
- viewer;
- PDF layer;
- help.

Python/Termux на його телефоні тоді не потрібні.

## Поточний developer workflow

Поки APK-конвертер ще не готовий, той самий package contract уже можна перевіряти через:

```bash
reno-code
git pull --ff-only origin main
python tools/package_dataset.py
```

Це не конвертує PDF/HTML повторно. Воно лише додає/оновлює manifest, каталог, інструкцію й volume inventory для вже готової папки.


## Поточний APK milestone

Реалізація вже почалася у `android/`.

v0.1.0 поки вирішує лише безпечне підключення **готового dataset**:

```text
APK
 ↓
Library
 ↓
Вибрати готову папку
 ↓
SAF
 ↓
manifest validation
 ↓
плитка
```

Чому не вставляємо converter одразу: спочатку треба довести на реальному телефоні, що persistable folder access, rotation, restart і Library state стабільні. Після цього converter буде доданий як окрема long-running operation, а не як код, прив'язаний до Activity.
