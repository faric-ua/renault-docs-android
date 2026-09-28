#!/data/data/com.termux/files/usr/bin/bash
set -u

BASE_DIR="/storage/emulated/0/Documents/PhoneDiagnostics"
STATE_DIR="$BASE_DIR/state"
COUNTER_FILE="$STATE_DIR/sequence"
TARGET_FILE="$STATE_DIR/adb-target"
PRIMARY_LOG_DIR="$BASE_DIR/logs"
FALLBACK_LOG_DIR="$HOME/phone-diagnostics-logs"

mkdir -p "$STATE_DIR"
touch "$BASE_DIR/.nomedia" 2>/dev/null || true

LOG_DIR="$PRIMARY_LOG_DIR"
if ! mkdir -p "$LOG_DIR" 2>/dev/null; then
  LOG_DIR="$FALLBACK_LOG_DIR"
  mkdir -p "$LOG_DIR"
fi

pause_menu() {
  echo
  printf "Натисни Enter, щоб повернутися в меню..."
  read -r _
}

detect_phone_ip() {
  ip route get 1.1.1.1 2>/dev/null     | sed -n 's/.* src \([^ ]*\).*/\1/p'     | head -n 1
}

load_target() {
  if [ -f "$TARGET_FILE" ]; then
    cat "$TARGET_FILE"
  fi
}

next_sequence() {
  local current=0
  if [ -f "$COUNTER_FILE" ]; then
    current="$(cat "$COUNTER_FILE" 2>/dev/null || echo 0)"
  fi
  case "$current" in
    ''|*[!0-9]*) current=0 ;;
  esac
  current=$((current + 1))
  printf "%03d" "$current"
  echo "$current" > "$COUNTER_FILE"
}

new_log() {
  local item="$1"
  local slug="$2"
  local title="$3"
  local seq stamp target
  seq="$(next_sequence)"
  stamp="$(date '+%Y%m%d-%H%M%S')"
  target="$(load_target)"
  CURRENT_LOG="$LOG_DIR/${seq}_item-${item}_${slug}_${stamp}.txt"

  {
    echo "Phone Diagnostics"
    echo "Sequence: $seq"
    echo "Menu item: $item"
    echo "Task: $title"
    echo "Time: $(date '+%Y-%m-%d %H:%M:%S %z')"
    echo "ADB target: ${target:-not-set}"
    echo "Log: $CURRENT_LOG"
    echo "============================================================"
  } > "$CURRENT_LOG"

  echo
  echo "Лог:"
  echo "  $CURRENT_LOG"
  echo
}

require_adb() {
  if ! command -v adb >/dev/null 2>&1; then
    echo "ADB не встановлений."
    echo "Спочатку виконай пункт 2 — Підготувати Termux + ADB."
    return 1
  fi

  if ! adb version >/dev/null 2>&1; then
    echo "ADB встановлений, але не запускається."
    echo "Виконай пункт 2 — Підготувати Termux + ADB."
    return 1
  fi

  return 0
}

require_connection() {
  local target
  target="$(load_target)"

  if [ -z "$target" ]; then
    echo "ADB target ще не збережений."
    echo "Спочатку виконай пункт 4 — Підключитися до ADB."
    return 1
  fi

  if [ "$(adb -s "$target" get-state 2>/dev/null)" != "device" ]; then
    echo "Немає активного ADB-підключення до:"
    echo "  $target"
    echo
    echo "Виконай пункт 4 — Підключитися до ADB."
    return 1
  fi

  ADB_TARGET="$target"
  return 0
}

show_help() {
  clear
  cat <<'EOF'
============================================================
      Phone Diagnostics — Wi-Fi ADB: інструкція
============================================================

ПЕРШИЙ РАЗ: РЕЖИМ РОЗРОБНИКА
1. Налаштування → Відомості про телефон → Відомості про ПЗ.
2. 7 разів натисни «Номер збірки».
3. Введи PIN, якщо Samsung попросить.
4. Повернись у Налаштування → Параметри розробника.

УВІМКНЕННЯ WI-FI НАЛАГОДЖЕННЯ
5. Параметри розробника → «Налагодження через Wi-Fi».
6. Увімкни перемикач.
7. Для домашньої мережі підтвердь «Дозволити».

ЩОБ ВІКНО З КОДОМ НЕ ЗАКРИВАЛОСЯ
8. Відкрий Termux.
9. Відкрий Останні програми.
10. Натисни іконку Termux → «Відкрити в режимі розділеного екрана».
11. Другою програмою вибери «Налаштування».
12. У Налаштуваннях відкрий:
    Параметри розробника → Налагодження через Wi-Fi.

PAIRING
13. Натисни «Підключати пристрій за допомогою коду підключення».
14. Samsung покаже IP, PAIRING PORT і 6-значний код.
15. У цьому меню вибери пункт 3.
16. Введи pairing port, а коли ADB попросить — 6-значний код.

ВАЖЛИВО:
- Pairing port і звичайний ADB port — РІЗНІ.
- Старий pairing-код/порт може швидко стати недійсним.
- Pairing робиться не щоразу; зазвичай тільки один раз або після скидання пари.

ЗВИЧАЙНЕ ADB-ПІДКЛЮЧЕННЯ
17. На головному екрані «Налагодження через Wi-Fi» знайди
    «IP-адреса й порт».
18. Це CONNECT PORT.
19. У меню вибери пункт 4 та введи цей порт.
20. Пункт 5 має показати стан «device».

ДІАГНОСТИКА
21. Пункт 6 — CPU snapshot.
22. Пункт 7 — 30 секунд CPU/Media моніторингу.
23. Пункт 8 — TOP snapshot.
24. Пункт 9 — Media/Photo/Gallery процеси.
25. Пункт 10 — аудіопідсистема.
26. Пункт 11 — памʼять.
27. Пункт 12 — повний diagnostic bundle.
28. Пункти 13–14 — A/B тест із тимчасовою зупинкою
    Google/Samsung MediaProvider. Дані НЕ видаляються.

ЛОГИ
Кожний тест пишеться окремо в:
  /storage/emulated/0/Documents/PhoneDiagnostics/logs

Формат імені:
  001_item-06_cpu-snapshot_YYYYMMDD-HHMMSS.txt
  002_item-07_cpu-monitor-30s_YYYYMMDD-HHMMSS.txt

Перше число — загальний порядковий номер виконаної задачі.
item-XX — номер пункту меню.
EOF
  pause_menu
}

prepare_tools() {
  clear
  new_log "02" "prepare-adb" "Підготувати Termux + ADB"

  echo "Оновлюю Termux і перевстановлюю ADB..." | tee -a "$CURRENT_LOG"
  pkg update 2>&1 | tee -a "$CURRENT_LOG"
  pkg upgrade -y 2>&1 | tee -a "$CURRENT_LOG"
  pkg reinstall -y libc++ android-tools 2>&1 | tee -a "$CURRENT_LOG"
  hash -r
  {
    echo
    echo "=== adb version ==="
    adb version
  } 2>&1 | tee -a "$CURRENT_LOG"

  pause_menu
}

pair_device() {
  clear
  if ! require_adb; then
    pause_menu
    return
  fi

  local auto_ip ip port
  auto_ip="$(detect_phone_ip)"

  echo "PAIRING"
  echo "На Samsung відкрий pairing-вікно з 6-значним кодом."
  echo
  printf "IP телефону [%s]: " "${auto_ip:-192.168.x.x}"
  read -r ip
  ip="${ip:-$auto_ip}"
  printf "PAIRING PORT: "
  read -r port

  if [ -z "$ip" ] || [ -z "$port" ]; then
    echo "IP або порт не введено."
    pause_menu
    return
  fi

  new_log "03" "adb-pair" "ADB pairing"
  {
    echo "Pair target: $ip:$port"
    echo
  } | tee -a "$CURRENT_LOG"

  adb pair "$ip:$port" 2>&1 | tee -a "$CURRENT_LOG"
  pause_menu
}

connect_device() {
  clear
  if ! require_adb; then
    pause_menu
    return
  fi

  local auto_ip ip port target
  auto_ip="$(detect_phone_ip)"

  echo "ADB CONNECT"
  echo "На Samsung дивись рядок «IP-адреса й порт»"
  echo "на головному екрані Wi-Fi debugging."
  echo
  printf "IP телефону [%s]: " "${auto_ip:-192.168.x.x}"
  read -r ip
  ip="${ip:-$auto_ip}"
  printf "CONNECT PORT: "
  read -r port

  if [ -z "$ip" ] || [ -z "$port" ]; then
    echo "IP або порт не введено."
    pause_menu
    return
  fi

  target="$ip:$port"
  new_log "04" "adb-connect" "ADB connect"

  adb connect "$target" 2>&1 | tee -a "$CURRENT_LOG"
  if [ "$(adb -s "$target" get-state 2>/dev/null)" = "device" ]; then
    echo "$target" > "$TARGET_FILE"
    echo "Збережено ADB target: $target" | tee -a "$CURRENT_LOG"
  else
    echo "Стан device не підтверджено; target не збережено." | tee -a "$CURRENT_LOG"
  fi

  pause_menu
}

show_connection() {
  clear
  if ! require_adb; then
    pause_menu
    return
  fi

  new_log "05" "adb-status" "ADB status"
  {
    echo "=== adb devices -l ==="
    adb devices -l
    echo
    echo "Saved target: $(load_target)"
  } 2>&1 | tee -a "$CURRENT_LOG"
  pause_menu
}

cpu_snapshot() {
  clear
  if ! require_connection; then pause_menu; return; fi
  new_log "06" "cpu-snapshot" "CPU snapshot"
  adb -s "$ADB_TARGET" shell dumpsys cpuinfo 2>&1 | tee -a "$CURRENT_LOG"
  pause_menu
}

cpu_monitor() {
  clear
  if ! require_connection; then pause_menu; return; fi
  new_log "07" "cpu-monitor-30s" "30 секунд CPU/Media моніторингу"

  for i in $(seq 1 10); do
    {
      echo
      echo "=== SAMPLE $i / 10 · $(date '+%H:%M:%S') ==="
      adb -s "$ADB_TARGET" shell dumpsys cpuinfo         | grep -i -E "TOTAL|media|audio|smartsuggestions|system_server|surfaceflinger|chatgpt|termux"         || true
    } 2>&1 | tee -a "$CURRENT_LOG"
    sleep 3
  done

  pause_menu
}

top_snapshot() {
  clear
  if ! require_connection; then pause_menu; return; fi
  new_log "08" "top-snapshot" "TOP snapshot"
  adb -s "$ADB_TARGET" shell top -b -n 1 2>&1 | head -n 60 | tee -a "$CURRENT_LOG"
  pause_menu
}

media_processes() {
  clear
  if ! require_connection; then pause_menu; return; fi
  new_log "09" "media-processes" "Media/Photo/Gallery процеси"
  {
    echo "=== ps -A | media/photo/gallery ==="
    adb -s "$ADB_TARGET" shell ps -A       | grep -i -E "media|photo|gallery"       || true
    echo
    echo "=== cpuinfo media/photo/gallery ==="
    adb -s "$ADB_TARGET" shell dumpsys cpuinfo       | grep -i -E "media|photo|gallery"       || true
  } 2>&1 | tee -a "$CURRENT_LOG"
  pause_menu
}

audio_snapshot() {
  clear
  if ! require_connection; then pause_menu; return; fi
  new_log "10" "audio-snapshot" "Аудіопідсистема"
  {
    echo "=== media_session ==="
    adb -s "$ADB_TARGET" shell dumpsys media_session       | grep -i -E "package=|state="       || true
    echo
    echo "=== audio focus/active/mode ==="
    adb -s "$ADB_TARGET" shell dumpsys audio       | grep -i -E "focus|active|mode"       | head -n 120       || true
  } 2>&1 | tee -a "$CURRENT_LOG"
  pause_menu
}

memory_snapshot() {
  clear
  if ! require_connection; then pause_menu; return; fi
  new_log "11" "memory-snapshot" "Памʼять і swap"
  {
    echo "=== free ==="
    adb -s "$ADB_TARGET" shell free -h || true
    echo
    echo "=== meminfo ==="
    adb -s "$ADB_TARGET" shell dumpsys meminfo | head -n 100
  } 2>&1 | tee -a "$CURRENT_LOG"
  pause_menu
}

full_bundle() {
  clear
  if ! require_connection; then pause_menu; return; fi
  new_log "12" "full-diagnostic" "Повний diagnostic bundle"

  {
    echo "=== adb devices -l ==="
    adb devices -l
    echo
    echo "=== CPU INFO ==="
    adb -s "$ADB_TARGET" shell dumpsys cpuinfo
    echo
    echo "=== TOP 80 ==="
    adb -s "$ADB_TARGET" shell top -b -n 1 | head -n 80
    echo
    echo "=== MEDIA PROCESSES ==="
    adb -s "$ADB_TARGET" shell ps -A | grep -i -E "media|photo|gallery" || true
    echo
    echo "=== MEMORY ==="
    adb -s "$ADB_TARGET" shell free -h || true
    echo
    echo "=== AUDIO SUMMARY ==="
    adb -s "$ADB_TARGET" shell dumpsys audio | grep -i -E "focus|active|mode" | head -n 120 || true
  } 2>&1 | tee -a "$CURRENT_LOG"

  pause_menu
}

force_stop_google_media() {
  clear
  if ! require_connection; then pause_menu; return; fi
  new_log "13" "force-stop-google-media" "A/B: зупинити Google MediaProvider"

  {
    echo "УВАГА: це НЕ видаляє дані."
    echo "Тимчасово зупиняється com.google.android.providers.media.module."
    echo
  } | tee -a "$CURRENT_LOG"

  adb -s "$ADB_TARGET" shell am force-stop com.google.android.providers.media.module 2>&1 | tee -a "$CURRENT_LOG"
  echo "Готово. Покористуйся телефоном 20–30 секунд і порівняй плавність." | tee -a "$CURRENT_LOG"
  pause_menu
}

force_stop_samsung_media() {
  clear
  if ! require_connection; then pause_menu; return; fi
  new_log "14" "force-stop-samsung-media" "A/B: зупинити Samsung MediaProvider"

  {
    echo "УВАГА: це НЕ видаляє дані."
    echo "Тимчасово зупиняється com.samsung.android.providers.media."
    echo
  } | tee -a "$CURRENT_LOG"

  adb -s "$ADB_TARGET" shell am force-stop com.samsung.android.providers.media 2>&1 | tee -a "$CURRENT_LOG"
  echo "Готово. Покористуйся телефоном 20–30 секунд і порівняй плавність." | tee -a "$CURRENT_LOG"
  pause_menu
}

open_logs() {
  clear
  local logs_uri
  logs_uri="content://com.android.externalstorage.documents/document/primary%3ADocuments%2FPhoneDiagnostics%2Flogs"

  mkdir -p "$LOG_DIR"

  echo "Відкриваю папку логів:"
  echo "  $LOG_DIR"
  echo

  if am start \
    -a android.intent.action.VIEW \
    -d "$logs_uri" \
    -t "vnd.android.document/directory" >/dev/null 2>&1; then
    echo "Папку передано файловому менеджеру."
    pause_menu
    return
  fi

  if am start \
    -a android.intent.action.OPEN_DOCUMENT_TREE \
    --eu android.provider.extra.INITIAL_URI "$logs_uri" >/dev/null 2>&1; then
    echo "Відкрито системний вибір папки на PhoneDiagnostics/logs."
    pause_menu
    return
  fi

  if command -v termux-open >/dev/null 2>&1; then
    termux-open "$LOG_DIR" >/dev/null 2>&1 || true
    echo "Виконано резервну спробу через termux-open."
  else
    echo "Не вдалося автоматично відкрити файловий менеджер."
    echo "Відкрий вручну:"
    echo "  Documents → PhoneDiagnostics → logs"
  fi

  pause_menu
}

reset_target() {
  clear
  rm -f "$TARGET_FILE"
  adb disconnect >/dev/null 2>&1 || true
  echo "Збережений ADB target очищено."
  echo "Pairing-ключі не видалялись."
  pause_menu
}

while true; do
  clear
  target="$(load_target)"
  echo "============================================================"
  echo "             Phone Diagnostics Menu"
  echo "============================================================"
  echo "ADB target: ${target:-не заданий}"
  echo "Logs: $LOG_DIR"
  echo
  echo "1  — Інструкція: Wi-Fi debugging крок за кроком"
  echo "2  — Підготувати / полагодити Termux + ADB"
  echo "3  — Pair device за 6-значним кодом"
  echo "4  — ADB connect до звичайного Wi-Fi debugging порту"
  echo "5  — Перевірити ADB connection"
  echo "6  — CPU snapshot → файл"
  echo "7  — CPU/Media monitor 30 секунд → файл"
  echo "8  — TOP snapshot → файл"
  echo "9  — Media / Photo / Gallery процеси → файл"
  echo "10 — Audio diagnostics → файл"
  echo "11 — RAM / swap → файл"
  echo "12 — Повний diagnostic bundle → файл"
  echo "13 — A/B: force-stop Google MediaProvider"
  echo "14 — A/B: force-stop Samsung MediaProvider"
  echo "15 — Відкрити папку логів у файловому менеджері"
  echo "16 — Скинути збережений ADB target"
  echo "0  — Вийти"
  echo
  printf "Вибір: "
  read -r choice

  case "$choice" in
    1) show_help ;;
    2) prepare_tools ;;
    3) pair_device ;;
    4) connect_device ;;
    5) show_connection ;;
    6) cpu_snapshot ;;
    7) cpu_monitor ;;
    8) top_snapshot ;;
    9) media_processes ;;
    10) audio_snapshot ;;
    11) memory_snapshot ;;
    12) full_bundle ;;
    13) force_stop_google_media ;;
    14) force_stop_samsung_media ;;
    15) open_logs ;;
    16) reset_target ;;
    0) clear; exit 0 ;;
    *) echo; echo "Невідомий пункт: $choice"; sleep 1 ;;
  esac
done
