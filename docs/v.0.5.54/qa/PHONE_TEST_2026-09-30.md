# v0.5.54 phone test — 2026-09-30

Result: **PENDING**

## Copy/layout

- [ ] Home: `Додати том` subtitle is `До проєкту`.
- [ ] Empty project card: `Порожній · додай том`.
- [ ] Project has one `Додати` tile.
- [ ] Add tile has `Авто` / `.rdpkg · один том`.
- [ ] Add tile has `Вручну` / `Папка / SAF`.
- [ ] raw → `.rdpkg` action remains separate.

## Help lifecycle

- [x] Home Help opens in Renault Docs dark theme, not the system-gray dialog. Phone PASS 2026-10-01.
- [x] Home Help survives rotation. Phone PASS 2026-10-01.
- [x] Home stale-Legacy status text and danger color survive rotation under Help.
- [x] Closing Help returns to Home with no action launched. Phone PASS 2026-10-01.
- [x] Project Add Help opens. Phone PASS 2026-10-01.
- [x] Project Add Help survives rotation. Phone PASS 2026-10-01.
- [x] No picker/import starts automatically after Help rotation. Phone PASS 2026-10-01.
- [x] Closing Project Add Help returns to the same Project with no picker/import launched.
- [x] Project list scroll position survives rotation. Phone PASS 2026-10-01 after scroll restoration fix.
- [x] Converter Help survives rotation. Phone PASS 2026-10-01: dialog restored over the same Converter.
- [x] Closing Converter Help returns to the same Converter state with source/destination preserved and no action launched.
- [x] Modern volume Help survives rotation. User-confirmed PASS 2026-10-01.
- [x] Native section Help survives rotation. Phone/video PASS 2026-10-01.
- [x] Native section `130 → Схеми → Критерії / скорочення` keeps the active Schemes panel controls visible above the table. Phone PASS 2026-10-02.
- [ ] Native section detail/table state and scroll survive rotation without rereading Runtime IR. Fix implemented, phone recheck pending.
- [ ] Native section Connector composite actions (`Схема + піни роз’єма`, `Схема роз’єму`, `Опис контактів`) use Renault Docs styling instead of default gray Android buttons. Phone FAIL 2026-10-02; fix implemented, recheck pending.
- [ ] Volume documentation Help survives rotation and panel context stays intact.

## Other lifecycle

- [ ] New Project typed text survives rotation.
- [ ] Volume actions dialog survives rotation.
- [ ] Remove confirmation survives rotation.
- [ ] Rotation does not export/remove/add automatically.

## Regression

- [ ] Existing projects/data preserved.
- [x] Laguna II remains populated. Phone PASS 2026-10-01: Project shows `Laguna II · томів: 10`.
- [ ] NT8183A opens.
- [ ] NT8328A opens.


## Dialog visual audit

- [ ] Settings → `Відкривати dataset` uses Renault Docs dark dialog theme.
- [ ] Settings → `Масштаб PDF` uses Renault Docs dark dialog theme.
- [ ] Settings → `Крок масштабу PDF` uses Renault Docs dark dialog theme.
- [ ] Help dialog uses the same visual family.
- [ ] Project → volume actions uses the same visual family.
- [ ] Project → remove confirmation uses danger styling and survives rotation.
- [ ] Project → multi-volume chooser uses the same visual family.
- [ ] Project → wrong-project confirmation uses the same visual family.
- [ ] Native raw preparation progress dialog uses the same visual family.
- [ ] No app-owned AlertDialog remains system-gray.

## Home tool cards

- [ ] Converter shows `+ стару Renault` plus folder icon on one line.
- [ ] Legacy shows `+ готова` plus folder icon on one line.
- [ ] Converter and Legacy cards are visually the same height.


## Final visual hierarchy

- [ ] Home shows one `Додати` parent tile.
- [ ] Add child actions are `Новий том / До проєкту` and `Новий проєкт / Створити модель`.
- [ ] Home shows one `Інструменти` parent tile.
- [ ] Converter / Legacy child cards have equal height and a large folder icon on the right.
- [ ] Modern dataset shows active `Modern` state clearly.
- [ ] Dataset `Classic` uses Renault Docs action styling.
- [ ] Dataset search field is clearly outlined and readable.
- [ ] Dataset/project/volume titles use the secondary identity color.
- [ ] Modern volume `Classic` uses Renault Docs action styling.
- [ ] Modern volume search field is clearly outlined.
- [ ] Create Project input and `Створити проєкт` action use Renault Docs styling.


## Header / mode polish

- [ ] Tool folder icons sit in the lower-right corner and do not force `Конвертер` to wrap.
- [ ] Modern dataset Classic/Modern switch matches Renault Docs styling.
- [ ] Modern volume Classic/Modern switch matches the same styling.
- [ ] Native section Classic/Modern switch matches the same styling.
- [ ] Classic Viewer / Visu / `Як користуватись` Modern button matches the same visual family.
- [ ] Long Viewer titles remain readable and do not collide with mode/home/search/settings controls.
- [ ] `Як користуватись` shows the relevance note that legacy volume counts describe only the opened Classic dataset.


## Converter / stale Legacy regression

- [x] Converter folder buttons use Renault Docs dark/bordered styling. Phone PASS 2026-10-01.
- [x] `Перевірити план`, `Почати конвертацію`, `Очистити вибір` are no longer default gray Android buttons. Phone PASS 2026-10-01.
- [ ] Cancel action uses danger styling when visible.
- [x] A valid old `Старі бібліотеки` record still opens normally. Phone PASS 2026-10-01: Laguna II opens with 10 volumes.
- [x] A moved/deleted old library does not enter a broken Modern screen. Phone PASS 2026-10-01: stale Megane II stays on Home.
- [x] Stale library message tells the user to re-add it through Legacy. Phone PASS 2026-10-01.


## Classic help-page readability

Phone result 2026-10-01: **PASS — compact layout accepted from real-device screenshots**.

- [x] Laguna II → Classic → `Як користуватись` opens without rebuilding/reimporting the package.
- [x] Viewer toolbar shows dataset identity, not the generic long `Як користуватися — …` page title.
- [x] Dataset identity stays readable in the narrow toolbar; years are shown separately in the page body.
- [x] Classic scope note is compact and remains readable.
- [x] `Відкрити каталог` is a compact action row and does not wrap awkwardly.
- [x] Section title is `Основні файли`, not the oversized old heading.
- [x] File information is shown as a two-column `Файл | Призначення` table.
- [x] Long paths wrap inside the first column without horizontal overflow.
- [x] Help-page H1/H2 typography is compact; no giant multi-line heading dominates the screen.
- [ ] Scroll to the bottom once and confirm the short `Renault Docs` / `Важливо` copy renders normally.


## Legacy Laguna 10-volume refresh

Phone result 2026-10-01: **PASS**.

- [x] Old `Старі бібліотеки → Renault Laguna II 2001–2006` opens successfully.
- [x] Opened legacy dataset reports `томів: 10`.
- [x] Returning to Home refreshes the stored legacy card to `Томів: 10 · відкриття: Classic`.
- [x] Public prepared `*_android` folder visibly contains all 10 Laguna II volume directories.
- [x] Public `packages/rdpkg` folder visibly contains all 10 Laguna II install archives.

Next check is separate: open the normal Project `Laguna II` and count installed Project volumes. Do not infer Project installation state from legacy/public folders.


## Project Laguna II installed-volume check

Phone result 2026-10-01: **PASS**.

- [x] Normal Project `Laguna II` opens.
- [x] Project header shows `Laguna II · томів: 10`.
- [x] Installed Project volume list is present and populated.
- [x] No additional Laguna II `.rdpkg` import is required.

This is independent from the Legacy 10-volume PASS and confirms the app-private Project package set is complete.
