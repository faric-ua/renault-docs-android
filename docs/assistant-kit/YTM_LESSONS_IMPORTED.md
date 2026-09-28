# Що перенесено з YTM Importer

Джерело: portable skeleton та system audits у гілці `feat/v1.4.49-updater` репозиторію `faric-ua/YTM`.

## Перенесено як принципи

- repository-first assistant recovery;
- source-of-truth precedence;
- Activity recreation contract;
- modal ownership;
- first-frame dialog stabilization;
- navigation origin/Back ownership;
- long-operation ownership;
- progress/result separation;
- SAF persisted URI contract;
- generic Tile contract;
- adaptive actions;
- safe viewport/insets;
- semantic theme palette;
- unit/static/build/phone evidence separation;
- release documentation skeleton;
- signed APK verification and SHA-256 handoff.

## Не переноситься автоматично

- YTM business logic;
- package/application id;
- OAuth/API code;
- versionName/versionCode;
- YTM-specific permissions;
- colors/brand;
- exact dependency versions;
- historical YTM bugs as Renault bugs.

## Важливий урок з permissions

У YTM різні release waves мали різні filesystem/updater потреби.

Renault viewer/library починаємо з SAF + persistable URI і не додаємо broad filesystem permission без окремої підтвердженої причини.

## Важливий урок з UI

Проблеми, які вже ловили на real phone:
- dialog geometry jump після першого frame;
- Help/result dialog loss on rotation;
- неправильний Back destination після delegated flow;
- fixed controls, що зникають у довгому списку;
- action labels, що wrap і ламають висоту;
- landscape content, який не scroll;
- text glyph замість нормальної Back icon;
- theme colors, які перестають розрізняти semantic states.

Для Renault ці класи проблем вважаються відомими заздалегідь і закриваються контрактами до реалізації.
