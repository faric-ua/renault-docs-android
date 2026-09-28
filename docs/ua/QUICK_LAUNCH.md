# Швидкий запуск на Android

## Termux:Widget

У самому віджеті показуються лише два проєкти:

```text
Renault
YTM Importer
```

Termux:Widget не має розкривних папок усередині самого віджета. Тому вкладені shortcut-файли не використовуються як меню.

Натисни **Renault** — відкриється Renault Menu:

```text
1 — Відкрити браузерну документацію
2 — Перейти в код Renault
3 — Статус локального сервера
4 — Зупинити локальний сервер
5 — Оновити проєкт з GitHub
6 — Показати основні папки Renault
7 — Build + Download APK
8 — Download latest APK
9 — Оновити Fast/Modern package
0 — Вийти
```

Натисни **YTM Importer** — відкриється YTM Menu:

```text
1 — Відкрити код YTM
2 — Build APK
3 — Download APK
0 — Вийти
```

## Оновити ярлики

```bash
reno-code
git pull --ff-only origin main
bash tools/install_termux_aliases.sh
source ~/.bashrc
bash tools/install_termux_widget.sh
```

Після цього `reno-code` і Renault Widget відкривають `$HOME/renault-docs-android`. Натисни refresh у Termux:Widget.

Інсталятор прибирає попередні `Renault/...`, `YTM Importer/...` і старі плоскі Renault shortcuts, а потім створює тільки два project buttons.


## Renault APK — як у YTM

Для APK не потрібно вручну завантажувати GitHub artifact ZIP.

У Renault Menu:

```text
7 — Build + Download APK
```

Цей пункт:
1. перевіряє, що локальна Git-гілка чиста і збігається з GitHub;
2. запускає `android-debug.yml`;
3. чекає завершення GitHub Actions;
4. завантажує artifact через `gh run download`;
5. artifact автоматично розпаковується самим GitHub CLI;
6. перевіряє SHA-256;
7. копіює тільки APK + checksum у стабільну папку Renault/packages.

Результат:

```text
/storage/emulated/0/Documents/Renault/packages/Renault-Docs-vX.Y.Z-build/
├── Renault-Docs-vX.Y.Z-debug.apk
└── Renault-Docs-vX.Y.Z-debug.apk.sha256
```

Якщо build уже існує:

```text
8 — Download latest APK
```

Короткі команди:

```bash
reno-apk
reno-apk-latest
```

Після handoff користувачу потрібен лише APK із цієї папки. Actions ZIP вручну розпаковувати не потрібно.


## Modern index для готового dataset

Після оновлення проєкту можна перебудувати package metadata без повторного копіювання всієї Renault документації:

```text
Renault Menu
→ 9 — Оновити Modern index dataset
```

або короткою командою:

```bash
reno-modern
```

Команда оновлює `renault-dataset.json`, каталог, `volumes.json`, `_renault/modern-index.json` і створює `_renault/fast-content-*.zip`. Fast Pack містить web-ресурси HTM/JS/GIF/ICO/CSS/JSON, але не дублює PDF. Оригінальні файли не переписуються.


### Для швидкості Android

Після появи Fast Pack застосунок один раз копіює цей компактний web-пакет у свій локальний cache. Далі legacy HTML/JS/GIF читаються з локального ZIP, а не тисячами SAF-запитів до окремих файлів.

Коротка команда:

```bash
reno-fast
```
