# Renault Docs — Browser PDF phone test

Status: **PARTIAL PHONE PASS — continuous-scroll retest required**

## Build/context

Feature: local PDF.js browser viewer  
Dataset: Renault Laguna II 2001–2006 / X74  
Expected entrypoint: `INDEX.HTM`

## Уже підтверджено на реальному Android

2026-09-23:

- PDF interception працює;
- PDF відображається без зовнішньої кнопки Chrome «Відкрити»;
- PDF рендериться прямо всередині legacy Renault frame;
- перевірено багатосторінковий документ — UI показував `4 / 18`;
- основний rendering виглядав коректно.

### UX finding PDF-001

Початкова реалізація показувала лише одну сторінку PDF за раз.

Це незручно для технічної документації:
- користувач не бачить природного продовження документа;
- потрібно вручну натискати next page;
- неочевидно, що нижче є інші сторінки.

Рішення:
- continuous vertical page flow;
- current page indicator оновлюється під час scroll;
- ↑/↓ лишаються як optional quick navigation;
- lazy canvas rendering + eviction далеких сторінок.

### Finding PDF-002

Після переходу на continuous mode реальний Android screenshot показав темно-червону напівпрозору заливку поверх кожної PDF-сторінки.

Причина підтверджена у viewer CSS: `.page-error` мав `display: grid`, тому hidden error-layer лишався видимим поверх canvas у Chrome.

Fix:
- `.page-error[hidden] { display: none !important; }`;
- error overlay з'являється тільки при реальній помилці рендерингу.

## Setup для retest

```bash
reno-code
git pull --ff-only origin main
reno-stop
reno-docs
```

Повторно запускати `reno-pdf-setup` не потрібно, якщо PDF.js уже встановлений і `--check` PASS.

Перевірити:

```bash
python tools/install_pdfjs.py --check
```

## Test 1 — continuous scroll

Відкрити той самий багатосторінковий PDF.

PASS:
- сторінка 1 переходить у сторінку 2 природним scroll вниз;
- далі 3, 4, ... без ручного page switch;
- між сторінками видно невеликий проміжок і label `Сторінка N з M`;
- немає blank page під час нормального повільного scroll;
- PDF має нормальні оригінальні кольори без червоної/темної error-заливки.

## Test 2 — current page tracking

Прокручувати документ вниз.

PASS:
- поле номера сторінки автоматично змінюється;
- `/ M` показує загальну кількість;
- ↑/↓ прокручують до сусідньої сторінки;
- ручне введення номера прокручує до потрібної сторінки.

## Test 3 — memory/lazy rendering

Швидко пройти кілька сторінок вниз і назад.

PASS:
- browser не вилітає;
- старі сторінки повторно рендеряться при поверненні;
- немає постійного накопичення десятків full-resolution canvas.

## Test 4 — zoom

Перевірити:
- +;
- −;
- По ширині.

PASS:
- continuous document лишається continuous;
- поточна сторінка лишається приблизно тією самою;
- сусідні сторінки перерендерюються без переходу в single-page mode.

## Test 5 — legacy viewrect

Відкрити link:

```text
.PDF#viewrect=...
```

PASS:
- PDF renders;
- fragment remains in URL;
- початковий zoom/position застосовується;
- після цього можна звичайно гортати наступні сторінки вниз.

Якщо crop неточний, це окремий `viewrect semantics` finding, а не загальний PDF failure.

## Test 6 — rotation

While PDF is visible:
1. portrait → landscape;
2. landscape → portrait.

PASS:
- той самий PDF лишається відкритий;
- continuous flow зберігається;
- viewer перераховує fit width;
- controls usable.

## Test 7 — Back

PASS:
- Back повертає у правильний Renault HTML/frame context;
- dataset не перезавантажується;
- ніяка зовнішня PDF action не запускається.

## Test 8 — raw fallback

Tap `PDF`.

PASS:
- raw local PDF endpoint доступний як fallback;
- Renault source file не змінюється.

## Evidence rule

Static/CI PASS не означає full phone PASS.

Для finding достатньо:
- exact PDF path, якщо він відомий;
- кількість сторінок;
- portrait/landscape;
- що саме сталося;
- screenshot лише коли він реально додає інформацію.
