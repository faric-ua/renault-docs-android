Renault Browser Transfer Kit
============================

Призначення
-----------
Перенесення браузерної версії Renault Docs на інший Android-телефон.

На новому телефоні потрібні лише:
1. Termux з F-Droid.
2. Termux:Widget з F-Droid.
3. Будь-який браузер Android.

Сам Renault dataset (HTM/PDF/GIF) у цей ZIP НЕ входить.
Його потрібно окремо скопіювати у:
  /storage/emulated/0/Documents/Renault/

Пакет не потребує Git clone для запуску browser runtime.

Одна команда після завантаження ZIP у Download
----------------------------------------------

pkg update -y && pkg install -y unzip && ZIP="$(find /storage/emulated/0/Download /storage/emulated/0/Downloads -maxdepth 1 -type f -name 'Renault-Browser-Transfer-Kit*.zip' 2>/dev/null | head -n 1)" && test -n "$ZIP" && TMP="$HOME/renault-browser-transfer-kit" && rm -rf "$TMP" && mkdir -p "$TMP" && unzip -oq "$ZIP" -d "$TMP" && bash "$TMP/Renault-Browser-Transfer-Kit/install.sh"

Що робить installer
-------------------
- встановлює python/git/gh/unzip у Termux;
- запускає termux-setup-storage, якщо потрібно;
- кладе browser runtime у:
    /storage/emulated/0/Documents/Renault/application
- шукає готовий dataset з renault-dataset.json;
- якщо dataset один — автоматично прив'язує його в config/current-device.json;
- встановлює pinned PDF.js із перевіркою SHA-256;
- ставить aliases;
- ставить Termux:Widget shortcuts.

Після install
-------------
У Termux:
  source ~/.bashrc
  reno-browser

У Termux:Widget:
  Renault
  YTM Importer

Примітка
--------
Це runtime snapshot для перенесення browser version, а не Git working tree.
Для активної розробки приватний Git repository підключається окремо.
