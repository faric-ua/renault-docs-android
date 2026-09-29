# Renault Docs — Window, Dialog and Lifecycle Contract

Цей контракт адаптований з real-phone проблем і виправлень YTM Importer.

## Єдиний modal engine

Не розкидати по Activities випадкові `AlertDialog.Builder` та окремі стилі.

Усі app-owned dialogs / Help / confirmations / result windows мають проходити через спільний UI layer.

Це дає один контракт для:
- theme;
- safe insets;
- geometry;
- actions;
- rotation restore;
- accessibility;
- phone QA.

## First-frame rule

Користувач не повинен бачити стрибок dialog з центру вгору або зміну розміру після появи.

Базова послідовність:

```text
create Dialog
    ↓
configure Window before show()
    ↓
hide decor/content
    ↓
show()
    ↓
apply real WindowInsets + final geometry
    ↓
wait for stable pre-draw/layout
    ↓
reveal
```

Не маскувати проблему довгою animation.

## Safe viewport

Dialog/content не має заходити під:
- status bar;
- display cutout;
- navigation bar;
- IME, якщо поле редагується.

Довгий content:
- верх залишається доступним;
- body scrollable;
- actions не губляться за межами екрана.

## Geometry

Не робити vertical position залежним від нестабільної висоти тексту першого frame.

Для складних/довгих modal content пріоритет — стабільний safe top anchor.

Якщо короткий dialog візуально центрується, це не повинно призводити до center→top snap після layout.

## Actions

Semantic tones:
- NORMAL;
- ACCENT;
- DANGER.

Широкий екран може показувати 2–3 actions горизонтально лише якщо кожна має достатню ширину.

Якщо текст не вміщується:
- vertical layout;
- або коротший однозначний label.

Не дозволяти різну висоту peer buttons лише через wrap одного label.

## Rotation

Якщо dialog відкритий:
- відновити той самий тип dialog;
- над тим самим parent screen;
- відновити selection/draft;
- не виконувати positive action;
- не повторювати destructive/remote operation.

Help window після rotation залишається Help window, а не запускає дію під ним.

## Result vs completed screen

Result modal — overlay.

Completed operation screen/state існує незалежно.

```text
completed
   ↓
result modal
   ↓ Close
completed
```

## Back

Візуальна Back-кнопка:
- vector icon, не текстовий символ;
- icon приблизно 24dp;
- touch target приблизно 48×48dp;
- destination визначається route origin.

Toolbar Back і system Back мають бути протестовані окремо.

## Android system UI

SAF picker / platform installer не є app dialog.

При виході в system UI app зберігає parent state.

Cancel повертає у parent без fake-success і без втрати draft/selection.


## Native long-operation progress dialog

For the project-level native raw → `.rdpkg` flow:

- the main Project screen polls persisted `NativeRdpkgRunState` every 750 ms while the Activity is started;
- if the active progress dialog is open, its message must refresh from the same persisted state instead of freezing the text captured when the dialog opened;
- PREPARING may expose `Скасувати`; IMPORTING must not expose a cancellation action unless cancellation is explicitly supported for that phase;
- active PREPARING / IMPORTING must never show the terminal dismiss `×`;
- once the run leaves active state, the active progress dialog closes and the terminal COMPLETE / CANCELLED / FAILED presentation becomes authoritative;
- terminal `×` dismissal is presentation-only, is scoped to the exact `finishedAtMs`, and survives Activity recreation/reopen;
- starting a new run clears the prior terminal dismissal marker.

Phone-accepted in v0.5.52 on 2026-09-30, including live numeric progress updates in an already-open dialog.
