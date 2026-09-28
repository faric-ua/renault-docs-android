# Renault Docs — System Behavior Contract

## Activity recreation не є дією користувача

Rotation/configuration change/recreation може відновити UI, але не має права автоматично:

- повторно імпортувати dataset;
- повторно конвертувати папку;
- видаляти dataset;
- запускати другу remote/local write operation;
- повторно відкривати installer;
- повторно виконувати destructive action.

Правило:

`recreate UI → observe/restore existing state`

а не:

`recreate UI → execute action again`.

## Semantic state

По можливості переживають recreation:

- поточний screen;
- вибраний dataset;
- відкритий документ/сторінка;
- search/filter;
- scroll position, якщо він важливий;
- відкритий Help;
- action menu;
- confirmation;
- result window;
- progress state;
- unsaved form draft.

## Modal lifecycle

Кожне вікно має мати чіткого owner.

Якщо dialog/modal був відкритий під час rotation:
- він відновлюється над тим самим semantic parent;
- positive action не запускається автоматично;
- Cancel/Close не змінює underlying data;
- result modal і completed status screen є різними станами.

Закриття result modal не повинно автоматично знищувати completed screen.

## Navigation ownership

Back destination визначається не лише класом Activity, а маршрутом походження.

Окремо тестувати:
- toolbar Back;
- Android system Back;
- Cancel;
- Close;
- rotation before Back;
- rotation while modal is open;
- child screen, відкритий з різних parent routes.

## Long-running operations

Конвертація, імпорт, індексація, download/update та інші довгі операції не повинні належати лише transient Activity.

Recreated UI повинна під'єднатися до існуючої operation state.

## Progress/result

Явні стани:
- idle;
- preparing;
- running;
- completed;
- failed;
- cancelled.

Completed state лишається доступним, доки користувач явно не піде з екрана.

## Forms / IME

Unsaved draft переживає recreation.

Клавіатура не повинна:
- перекривати обов'язкові кнопки;
- скидати форму;
- міняти screen;
- submit-ити автоматично.

## Lists / library

Для бібліотеки datasets:
- filter/search переживає recreation;
- tile primary action — відкриття dataset;
- secondary actions окремі;
- destructive action потребує confirmation;
- unavailable dataset не видаляється автоматично: спочатку пропонується reconnect/remove.

## SAF / system UI

Android system picker — окрема ownership boundary.

Cancel із system picker:
- повертає у правильний parent;
- не означає success;
- не стирає локальний state.

Persisted SAF permission — capability, яку треба реально перевіряти.

## Themes / skins

Theme змінює appearance, але не semantics.

Success/warning/danger/disabled мають залишатися відмінними у будь-якій темі.

Theme recreation не повторює domain actions.

## PDF viewer

PDF open/render state є viewer state.

Rotation:
- не повинна повторно «відкривати» PDF як нову дію;
- по можливості зберігає сторінку/zoom/позицію;
- legacy fragment на кшталт `#viewrect` не втрачається без явної причини.

## Updater

Check update — read.
Download — resumable operation.
Install — explicit Android/system action.

Перед install:
- verify version;
- verify expected source;
- verify SHA-256;
- use platform installer;
- не обіцяти silent install там, де Android вимагає confirmation.

## Evidence

- static audit → source contract;
- unit test → deterministic logic;
- CI build → compilation/signing;
- тільки real-phone test → реальна mobile UI/lifecycle поведінка.
