# Web viewer

Браузерна оболонка для normalized Renault documentation.

## Навіщо потрібен локальний сервер

Ми навмисно не використовуємо `file://...` як еталонний спосіб запуску. Через HTTP поведінка ближча до тієї, яку потім отримає Android WebView через controlled local origin.

## PDF layer

Android Chrome не дає нам стабільний embedded PDF runtime для старої Renault документації. Тому server перехоплює локальні URL, що закінчуються на `.pdf/.PDF`, і замість browser/plugin поведінки показує наш локальний PDF.js viewer.

Схема:

```text
legacy HTML link
      ↓
.../SCH/0207_A3.PDF#viewrect=...
      ↓
Renault local server intercept
      ↓
local PDF.js viewer
      ↓
raw PDF endpoint
```

Fragment `#viewrect=...` залишається у browser URL і viewer використовує його як початковий zoom/scroll region.

## Continuous pages

PDF читається як одна вертикальна стрічка:

```text
Сторінка 1
    ↓
Сторінка 2
    ↓
Сторінка 3
    ↓
...
```

Кнопки ↑/↓ і поле номера сторінки лишаються лише як швидка навігація. Вони прокручують до сторінки, а не замінюють єдиний canvas.

Щоб великий документ не з'їдав пам'ять телефона, viewer:
- створює lightweight placeholders для всіх сторінок;
- рендерить поточну сторінку та сусідні;
- звільняє canvas далеких сторінок;
- повторно рендерить їх, коли користувач до них доходить.

Тобто документ візуально безперервний, але не тримає сотні full-resolution canvas одночасно.

## Одноразове встановлення PDF.js

PDF.js навмисно не комітиться у цей репозиторій. Runtime встановлюється локально з офіційного Mozilla release:

```bash
python tools/install_pdfjs.py
```

Або:

```bash
reno-pdf-setup
```

Перевірка:

```bash
python tools/install_pdfjs.py --check
```

Поточна pinned версія задається у `web/pdf_support.py`.

## Запуск

```bash
reno-docs
```

Або вручну:

```bash
python web/serve.py "/storage/emulated/0/Documents/Renault/laguna 2 2001-2006_android" --host 0.0.0.0 --port 8080
```

## PDF viewer controls

Browser viewer має:
- природне вертикальне прокручування всіх сторінок;
- автоматичний індикатор поточної сторінки;
- ↑/↓ для швидкого переходу;
- поле номера сторінки;
- zoom in/out;
- fit width;
- Back;
- raw PDF fallback;
- rotation/resize;
- початкову обробку legacy `#viewrect`.

## Phone evidence

На реальному Android підтверджено:
- PDF interception працює;
- PDF рендериться всередині legacy Renault frame;
- зовнішня кнопка Chrome «Відкрити» більше не потрібна;
- багатосторінковий PDF відкривається.

Після першого тесту виявлено UX finding: single-page mode приховував природну багатосторінковість документа. Це і стало причиною переходу на continuous vertical scroll.

Повний acceptance: `docs/qa/PDF_BROWSER_PHONE_TEST.md`.
