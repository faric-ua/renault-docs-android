# Renault Docs — UI Contract

## Home — Add panel parity (v0.5.82)

- Home mirrors Project `Додати`: fixed header with original colored 📌 when portrait-pinned, grayscale/inactive pin in landscape, ▲/▼, and Help.
- Home pin is a distinct persisted setting: do not change a per-project pinned preference.
- Landscape starts with actions collapsed, but lets the user expand them temporarily without modifying the portrait pin or expanded state; returning portrait restores portrait state.
- The Home Add action body has its own bounded scroll surface, while `Мої Renault` is an independent project-list scroll below it.
- New volume, new project, Ready Projects/Drive, project-vs-volume explanation, Tools (Converter/Legacy), and any legacy library records live **inside** expanded Add.
- Running/terminal operation status lives inside Home Add **outside** the collapsible action body, so progress/cancel/result remains accessible even with action buttons hidden.
- Do not introduce automatic archive import or delete/move original sources during UI changes.

## Library screen

Головний екран Android-застосунку — бібліотека datasets.

Dataset відображається як **плитка**.

Плитка може містити:
- model;
- years;
- platform code;
- content type;
- preview;
- availability/status;
- last opened state;
- secondary actions.

Tap body → primary action: відкрити dataset.

`⋮` → повне action menu.

Long press може відкрити те саме menu, але ніколи не виконує destructive action напряму.

## Touch targets

Критичні icon/button touch targets — орієнтовно **48dp або більше**.

Back icon — нормальний vector asset, не текстовий символ. Візуальний glyph може бути близько 24dp, але touch target лишається приблизно 48×48dp.

## Screen structure

Для utility/full-screen screens:
- стабільний top bar;
- нормальна Back-кнопка;
- scrollable middle content;
- fixed footer/header лише коли вони не ховають content;
- safe system insets;
- portrait і landscape тестуються окремо.

Довгий список не має прокручувати разом із критичними `Cancel / Add / Confirm`, якщо через це actions можуть зникнути за межами viewport.

## Dialogs / Help / confirmations

Усі app-owned dialogs повинні йти через спільний UI layer.

Не використовувати випадкові native builders у різних Activities як паралельні UI-системи.

First-frame contract описаний окремо в `WINDOW_LIFECYCLE_CONTRACT.md`:
- Window configure before `show()`;
- content/decor не показується до стабільних insets/geometry;
- жодного видимого center→top або size snap.

Вікно:
- має чіткий title hierarchy;
- не обрізається safe area;
- довгий content scrollable;
- actions мають стабільну ієрархію;
- danger action семантично відмінний;
- rotation відновлює dialog без виконання дії.

## Action hierarchy

Primary/Accent — основна позитивна дія.
Normal — secondary/cancel/navigation.
Danger — delete/remove/reset.

Для 2–3 кнопок horizontal row дозволений лише коли ширини вистачає.

Орієнтир із перевіреного YTM рішення: рішення про horizontal/vertical layout приймається за реальною `screenWidthDp` і мінімальною шириною action, а не просто за orientation.

Якщо label wrap-иться або кнопки різної висоти:
- vertical layout;
- або коротший однозначний label.

## Result windows

Result modal не є власником operation state.

```text
operation
   ↓
completed status screen
   ↓
result modal
   ↓ Close
completed status screen
```

Close закриває тільки modal, якщо інше явно не визначено product contract.

## Modern native adaptive category view — v0.5.82 device acceptance (2026-10-09)

- In **portrait**, display full Modern section navigation: `Схеми`, `Роз’єм`, `Положення на авто`, `Документація` and the active category content.
- In **landscape**, prioritize only the currently active category content, with other tabs hidden to maximize browsing space; this is **not** a loss of data or a Classic fallback.
- Returning portrait restores full navigation without changing the active section/category or discarding displayed list/content.
- Choosing a different category in portrait (e.g. `Роз’єм`) should apply the same landscape focus to that category. Preserve this invariant through future UI changes.
- Phone QA for Megane II NT8340A section 120 `Схеми`: user screenshots + explicit acceptance PASS; don't infer every category's underlying dataset actions individually tested.

## Classic vs Modern preservation — v0.5.82 phone acceptance

- **Classic is the authentic Renault Visu Schema / legacy HTML and frames**, not a redesigned Android screen. Its original controls, visual arrangement, page content, link semantics, PDF/schematic handling and full-screen feature are preserved as-is; Android shell adaptation may handle system bars, safe insets, lifecycle, file permissions and crash fixes only when necessary.
- **Modern is the Android-native interface** where layout, search, menus and project actions may be expanded. Do not copy Modern-specific `Розділи` dialogs into Classic simply to pass an invalid QA scenario.
- Section-origin hybrid Viewer and untouched original Classic are distinct routes. Do not assume `Modern → Classic` opens the legacy hybrid navigator.
- User device screenshots 2026-10-08 accepted authentic Classic NT8340/120 PDF `SE2416-P` at 176% in ordinary portrait, fullscreen portrait and fullscreen landscape.

## Viewer screen

Viewer має:
- dataset title/context;
- WebView/content region;
- predictable Back;
- PDF interception;
- loading/error states;
- можливість повернутися в Library без втрати dataset registration.

Legacy HTML не повинен диктувати navigation самого Android app.

## Theme

Перший release може мати одну тему, але компоненти не повинні hardcode-ити випадкові кольори.

Закласти semantic palette:
- background;
- surface;
- surfaceAlt;
- border;
- text;
- muted;
- accent;
- success;
- warning;
- danger;
- disabled.

Semantic state важливіший за конкретний колір: тема може змінити palette, але success/warning/danger/disabled мають залишатися легко відмінними.


## Modern / Classic dataset navigation

Primary dataset tap opens **Modern mode**.

Modern mode is app-owned native UI and must not depend on legacy Renault frames for top-level catalog/navigation.

Initial hierarchy:

```text
Library
  ↓
Modern dataset catalog
  ↓
native volume list/search
  ↓
legacy internal volume content
```

The hierarchy may later move section/node levels into native UI as the modern index is enriched.

Classic mode remains an explicit fallback action and opens the generated legacy catalog. Modern and Classic must point at the same dataset; Classic never creates or mutates a second copy.

## PDF export

PDF viewer exposes an explicit Save action.

Save:
- opens Android `ACTION_CREATE_DOCUMENT`;
- writes the original PDF bytes, not a rendered screenshot;
- does not request broad storage permission;
- Cancel returns to the same PDF;
- rotation must not relaunch the system picker automatically.

## Native structured tables

Runtime IR structured documents use the shared native table presentation.

Rules:
- preserve source rows and columns;
- choose compact-column widths from actual body content;
- do not let a long header alone force a wide code column;
- use remaining width for the description column;
- wrap long descriptions inside their cell;
- all cells in a row share the row height;
- short code/value cells are vertically centered when another cell wraps;
- header cells are centered;
- table rows remain selectable/readable data, not button-like actions.

Generated table PDF must use the same column-layout logic.

Page geometry:
- 1–2 columns: portrait;
- 3+ columns: landscape unless a later semantic table contract overrides it.

## Connector document UI

When Runtime IR exposes both connector child documents:
- drawing PDF (`dessin`);
- contact/pin data (`alveoles`);

Modern shows:
- `Схема + піни розʼєма`;
- `Схема розʼєму`;
- `Опис контактів`.

The combined action may open the original connector composite source inside the app viewer. This is an intentional document-level compatibility bridge and must not switch the whole dataset into Classic navigation.

Structured-table save filenames should preserve source identity when available:
- `101(pins).pdf`;
- `101_1(pins).pdf`;
- `101(abbreviations).pdf`.

Generic `Renault_<code>_table.pdf` naming is deprecated.

