# Вхід через Google у Renodocs — налаштування Google Cloud (0.6.5)

_Етап #133 «вхід через Google». Власник 2026-10-18 16:33: «давай, починай… можемо завжди назад відкотити»._

## Що робить вхід у 0.6.5

- Налаштування → «Акаунт Google» → «Увійти через Google» (вікно Google) → картка показує ім'я й пошту; «Вийти» спершу питає.
- **Нічого не закриває**: власні томи, імпорт, читання — як без входу. Закритий каталог для друзів — наступний етап.
- На телефоні зберігаються лише пошта й ім'я. Токен Google не зберігається.

## Що потрібно один раз від власника (Google Cloud, з телефона в Chrome)

Без цього кнопка каже «Вхід через Google ще не налаштовано».

1. `console.cloud.google.com` → увійти своїм Google-акаунтом → погодитись з умовами.
2. Угорі вибір проєкту → «New project» → назва **Renodocs** → «Create».
3. Меню ☰ → «APIs & Services» → «OAuth consent screen» (або «Google Auth Platform») → «Get started»:
   назва застосунку **Renodocs**, пошта підтримки — своя; «Audience» — **External**; контактна пошта — своя → «Create».
4. «Audience» → «Test users» → «Add users» → своя пошта (друзів — пізніше). Поки застосунок у режимі «Testing», увійти можуть лише люди з цього списку (до 100).
5. «Clients» → «Create client» → тип **Android**:
   - Package name: `com.farsan.renodocs` (остаточна назва з 0.6.5; до того було `com.saney.renodocs`)
   - SHA-1: `41:02:35:0E:27:87:FD:53:8B:BF:58:A2:19:29:3A:13:22:35:E6:18`
   → «Create».
6. «Clients» → «Create client» → тип **Web application** → назва «Renodocs Web» → «Create» → скопіювати **Client ID** (закінчується на `.apps.googleusercontent.com`) і надіслати в чат.

Client ID — не секрет (Google показує його кожному застосунку, що входить). Claude вписує його в `android/gradle.properties` (`renodocs.googleWebClientId`) → нова збірка.

## Звідки SHA-1

Відбиток сертифіката, яким підписано APK з `android-debug.yml` (стабільний ключ). Claude витяг його з опублікованого `Renault-Docs-v0.6.4-debug.apk` (блок підпису v2): SHA-256 `dd588fbb…59a9802` — той самий, що записаний у журналі для Core Stable, тож ключ правильний. Якщо колись зміниться ключ підпису (напр. Google Play), у Google Cloud треба додати ще один Android-клієнт з новим SHA-1.

## Що може піти не так (і що це означає)

| Що видно | Причина |
|---|---|
| «Вхід через Google ще не налаштовано» | Client ID ще не вписано в збірку. |
| «Не знайдено акаунта Google для входу» | На телефоні нема акаунта Google, або акаунта нема в «Test users». |
| «Не вдалося увійти: … 10 / developer error» | Не збігся package name або SHA-1 в Android-клієнті. |
| «Вхід скасовано» | Закрив вікно Google — нічого не змінено. |

## Журнал

- **2026-10-18 17:03–17:59** — власник зробив усе з телефона: проєкт RenoDocs (ID `renodocs`), Auth Platform External/Testing, test users `faric.eset`, `faric.kpi`; Android-клієнт `com.farsan.renodocs` + SHA-1; Web-клієнт «Renodocs Web» → Client ID `47038347839-tqo68lvq…tdiss.apps.googleusercontent.com` вписано в `android/gradle.properties`. Урок: після створення Android-клієнта Google теж показує «Client ID» — його в застосунок не вписують; у застосунок іде лише ID Web-клієнта (Android-клієнт Google впізнає сам за пакетом і SHA-1).
- **2026-10-18 ~17:00** — код 0.6.5 (картка «Акаунт Google», `GoogleAccount.kt`, Credential Manager 1.6.0 + googleid 1.1.1); Client ID порожній — чекаємо власника.
