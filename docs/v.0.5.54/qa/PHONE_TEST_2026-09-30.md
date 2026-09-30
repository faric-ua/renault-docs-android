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

- [ ] Home Help opens in Renault Docs dark theme, not the system-gray dialog.
- [ ] Home Help survives rotation.
- [ ] Closing Help returns to Home with no action launched.
- [ ] Project Add Help opens.
- [ ] Project Add Help survives rotation.
- [ ] No picker/import starts automatically after Help rotation.
- [ ] Converter Help survives rotation.
- [ ] Modern volume Help survives rotation.
- [ ] Native section Help survives rotation.
- [ ] Volume documentation Help survives rotation and panel context stays intact.

## Other lifecycle

- [ ] New Project typed text survives rotation.
- [ ] Volume actions dialog survives rotation.
- [ ] Remove confirmation survives rotation.
- [ ] Rotation does not export/remove/add automatically.

## Regression

- [ ] Existing projects/data preserved.
- [ ] Laguna II remains populated.
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
