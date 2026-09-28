# Renault Docs — Permissions and Storage Contract

## Принцип мінімальних permissions

Не копіювати permissions YTM Importer механічно.

Для Renault Docs за замовчуванням **не використовуємо**:

- `MANAGE_EXTERNAL_STORAGE`;
- legacy broad storage permissions;
- `REQUEST_INSTALL_PACKAGES`, доки updater реально не реалізований.

## Dataset access

Фінальний Android app підключає dataset через Storage Access Framework.

Потік:

```text
Library
  ↓ Add dataset
ACTION_OPEN_DOCUMENT_TREE
  ↓ user selects folder
takePersistableUriPermission
  ↓
store content:// URI + metadata
```

Не зберігати тільки абсолютний `/storage/emulated/0/...` як канонічне посилання.

## Persisted access

Перед відкриттям dataset перевіряти, що persisted URI permission ще існує.

Якщо доступ втрачено:
- tile лишається у Library;
- показати unavailable/reconnect state;
- користувач може вибрати папку повторно;
- не видаляти запис автоматично.

## Reference mode

Перший Android release використовує Reference mode:
- data лишаються у папці користувача;
- app зберігає URI та metadata;
- сотні мегабайт не дублюються.

## Managed mode

Можливий пізніше:
- app копіює dataset у своє storage;
- окремий explicit import;
- progress + cancel + failure cleanup;
- recreation не стартує копіювання вдруге.

## WebView origin

Не покладатися на raw `file://` як основну архітектуру.

Пріоритет:
- AndroidX WebKit / WebViewAssetLoader або еквівалентний controlled origin;
- content resolver bridge для SAF-backed files;
- loopback HTTP лише якщо compatibility test доведе необхідність.

## Network

Основна документація offline.

`INTERNET` permission додається лише якщо є реальна функція, яка його потребує, наприклад updater або remote metadata.

## Updater

`REQUEST_INSTALL_PACKAGES` не потрібен першій версії viewer/library.

Якщо updater з'явиться:
- окремий release/system contract;
- download/hash/version validation;
- explicit platform installer;
- окремий phone QA.
