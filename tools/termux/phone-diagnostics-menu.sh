#!/data/data/com.termux/files/usr/bin/bash
set -u

PD_VERSION="2026-10-16.3"
PD_REPO="faric-ua/renault-docs-android"
PD_SOURCE_PATH="tools/termux/phone-diagnostics-menu.sh"
BASE_DIR="/storage/emulated/0/Documents/PhoneDiagnostics"
# Пункти 21–22: реклама MIUI і службові програми, які не потрібні.
# Вимикаються (не видаляються) і повертаються одним пунктом.
# Пункти 23–24: рідко вживані застосунки бабусиного Redmi (звіт 17, кошик 40).
# Не працюють у фоні, поки їх не відкрити; відкриваються як звичайно.
SLEEP_PACKAGES="cn.wps.xiaomi.abroad.lite com.cubleopard.pocketlifeworld com.einnovation.temu com.google.android.apps.bard com.google.android.apps.podcasts com.google.android.videos com.google.android.apps.youtube.music com.zhiliaoapp.musically com.voe.superbill"
BLOAT_PACKAGES="com.miui.msa.global com.amazon.appmanager com.ebay.carrier com.facebook.appmanager com.facebook.services com.facebook.system com.mi.globalminusscreen com.google.android.apps.magazines"
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
  # 2026-10-15 (faric): шлях до файлу — і зверху, і в кінці роботи,
  # бо після довгого виводу верхній рядок уже не видно.
  if [ -n "${CURRENT_LOG:-}" ]; then
    echo
    echo "Лог збережено:"
    echo "  $CURRENT_LOG"
    CURRENT_LOG=""
  fi
  echo
  printf "Натисни Enter, щоб повернутися в меню..."
  read -r _
}

detect_phone_ip() {
  # 2026-10-15: на A26 `ip route get` у Termux нічого не дає — пробуємо кілька
  # способів; якщо жоден не спрацював — порожньо (тоді питаємо повну адресу).
  local found
  found="$(ip route get 1.1.1.1 2>/dev/null | sed -n 's/.* src \([0-9.]*\).*/\1/p' | head -n 1)"
  [ -n "$found" ] || found="$(ip -4 addr show wlan0 2>/dev/null | sed -n 's/.*inet \([0-9.]*\).*/\1/p' | head -n 1)"
  [ -n "$found" ] || found="$(ifconfig wlan0 2>/dev/null | sed -n 's/.*inet[^0-9]*\([0-9][0-9.]*\).*/\1/p' | head -n 1)"
  if [ -z "$found" ] && command -v termux-wifi-connectioninfo >/dev/null 2>&1; then
    found="$(timeout 5 termux-wifi-connectioninfo 2>/dev/null | sed -n 's/.*"ip": *"\([0-9.]*\)".*/\1/p' | head -n 1)"
  fi
  case "$found" in
    *.*.*.*) printf '%s\n' "$found" ;;
  esac
}

# Мережа вже відома (з IP цього телефона, напр. 192.168.50.) —
# людина вводить лише останнє число. Enter без нічого = останній IP.
# Повну адресу з крапками теж можна ввести.
ask_target_ip() {
  # Підказка — лише текст у полі вводу, який можна дописати або стерти
  # (власник 2026-10-15). Береться: мережа цього телефона → мережа останньої
  # адреси → 192.168.0. За адресою підключаємось лише за тим, що в полі.
  local own prefill last answer
  own="$(detect_phone_ip)"
  last="$(load_target)"
  last="${last%%:*}"

  case "$own" in
    *.*.*.*) prefill="${own%.*}." ;;
    *)
      case "$last" in
        *.*.*.*) prefill="${last%.*}." ;;
        *) prefill="192.168.0." ;;
      esac
      ;;
  esac

  [ -n "$own" ] && echo "Мій IP: $own" >&2
  if [ -n "$last" ]; then
    echo "Лише Enter (нічого не дописуючи) = $last (останній)" >&2
  fi
  echo "Допиши останнє число або зітри й напиши своє:" >&2
  if read -e -i "$prefill" -r -p "IP телефону: " answer 2>/dev/null; then
    :
  else
    printf "IP телефону: %s" "$prefill" >&2
    read -r answer
    case "$answer" in
      *.*) ;;
      "") answer="$prefill" ;;
      *) answer="$prefill$answer" ;;
    esac
  fi

  case "$answer" in
    ""|"$prefill") TARGET_IP="$last" ;;
    *.*.*.*) TARGET_IP="$answer" ;;
    *)
      TARGET_IP=""
      echo "Адреса неповна: «$answer». Потрібно 4 числа через крапку." >&2
      ;;
  esac
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
29. Пункт 17 — звіт для чистки: які застосунки
    займають пам'ять і місце, якими давно не
    користувались, анімації. НІЧОГО НЕ ЗМІНЮЄ.

31. Пункти 19–20 — анімації швидше (0,5x) / як було (1x)
    на підключеному телефоні. Нічого не видаляється.

32. Пункт 21 — вимкнути рекламу MIUI і службові програми
    Amazon / eBay / Facebook (сам Facebook і Messenger
    лишаються). НЕ видаляє — пункт 22 повертає все назад.

33. Пункт 23 — приспати рідко вживані застосунки: не
    працюють у фоні, поки їх не відкрити. Пункт 24 — розбудити.

36. Пункт 31 — що роблять потоки застосунку: на яких ядрах
    (великих чи малих) і з яким пріоритетом. Запускати, поки
    застосунок працює. НІЧОГО НЕ ЗМІНЮЄ.
37. Пункт 32 — прискорити застосунок: Android одразу
    переводить увесь його код у машинний (те, що сам робить
    уночі на зарядці). Звук, файли й дані — ті самі. Після
    оновлення застосунку — ще раз. «2» — повернути як було.

ЗАСТОСУНКИ, ЯКИХ БІЛЬШЕ НЕМА В МАГАЗИНІ
34. Пункт 26 — зберегти застосунок з підключеного
    телефона: УСІ частини APK (base + split_*) у
    Documents/PhoneDiagnostics/apk/ + один файл .apks
    для чужого телефона (ставиться застосунком SAI
    з Play Маркету). Нічого не змінює.
35. Пункт 27 — поставити збережений застосунок на
    підключений телефон (усі частини разом).
    Один APK через Quick Share часто не працює саме
    тому, що застосунок складається з кількох частин.

ОНОВЛЕННЯ
30. Пункт 18 — завантажити нову версію цього меню
    з GitHub. Логи й підключення лишаються.

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
  pkg install -y curl 2>&1 | tee -a "$CURRENT_LOG"
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

  local ip port

  echo "PAIRING"
  echo "На іншому телефоні відкрий pairing-вікно з 6-значним кодом."
  echo
  ask_target_ip
  ip="$TARGET_IP"
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

  local ip port target

  echo "ADB CONNECT"
  echo "На іншому телефоні дивись рядок «IP-адреса й порт»"
  echo "на головному екрані Wi-Fi debugging."
  echo
  ask_target_ip
  ip="$TARGET_IP"
  printf "CONNECT PORT: "
  read -r port

  if [ -z "$ip" ] || [ -z "$port" ]; then
    echo "IP або порт не введено."
    pause_menu
    return
  fi

  target="$ip:$port"
  new_log "04" "adb-connect" "ADB connect"

  local out tmo="" ping_ok=""
  command -v timeout >/dev/null 2>&1 && tmo="timeout 15"
  {
    echo "Мій IP (цей телефон): $(detect_phone_ip)"
    echo "Підключаюсь до: $target (чекаю до 15 с)..."
    if command -v ping >/dev/null 2>&1; then
      if ping -c 2 -W 2 "$ip" >/dev/null 2>&1; then
        echo "Ping $ip: ВІДПОВІДАЄ"
        : > "$STATE_DIR/ping_ok"
      else
        echo "Ping $ip: НЕ відповідає"
      fi
    fi
  } | tee -a "$CURRENT_LOG"
  [ -f "$STATE_DIR/ping_ok" ] && ping_ok=1
  rm -f "$STATE_DIR/ping_ok"
  out="$($tmo adb connect "$target" 2>&1)"
  [ -n "$out" ] || out="timed out (15 с без відповіді)"
  echo "$out" | tee -a "$CURRENT_LOG"

  # Після зміни роутера / Wi-Fi старий ADB-сервер часто «застряє» на старих
  # адресах — перезапускаємо його і пробуємо ще раз (2026-10-15).
  if [ "$(adb -s "$target" get-state 2>/dev/null)" != "device" ]; then
    echo | tee -a "$CURRENT_LOG"
    echo "Не вийшло з першого разу — перезапускаю ADB і пробую ще раз..." | tee -a "$CURRENT_LOG"
    adb disconnect >/dev/null 2>&1 || true
    adb kill-server >/dev/null 2>&1 || true
    adb start-server >/dev/null 2>&1 || true
    out="$($tmo adb connect "$target" 2>&1)"
    [ -n "$out" ] || out="timed out (15 с без відповіді)"
    echo "$out" | tee -a "$CURRENT_LOG"
  fi

  if [ "$(adb -s "$target" get-state 2>/dev/null)" = "device" ]; then
    echo "$target" > "$TARGET_FILE"
    echo "ГОТОВО: підключено. Збережено ADB target: $target" | tee -a "$CURRENT_LOG"
    pause_menu
    return
  fi

  {
    echo
    echo "НЕ ПІДКЛЮЧИЛОСЬ: $target"
    echo
    case "$out" in
      *refused*)
        echo "Причина: «Connection refused» — неправильний ПОРТ або на тому телефоні"
        echo "вимкнене «Налагодження через Wi-Fi». Порт міняється щоразу, коли його"
        echo "вмикають. Візьми свіжий порт з рядка «IP-адреса й порт»." ;;
      *timed*|*"No route"*|*unreachable*)
        if [ -n "$ping_ok" ]; then
          echo "Причина: телефон У МЕРЕЖІ (ping є), але порт $port мовчить."
          echo "Значить, порт не той або налагодження на тому телефоні вимкнулось."
          echo "Візьми порт з ГОЛОВНОГО екрана «Налагодження через Wi-Fi» →"
          echo "«IP-адреса й порт» (не з віконця з кодом пари)."
        else
        echo "Причина: телефон НЕ досяжний — різні мережі, гостьова мережа,"
        echo "або роутер ізолює пристрої (у роутері: «ізоляція клієнтів» /"
        echo "«AP isolation» — вимкнути). Перевір: обидва в одній Wi-Fi?"
        fi ;;
      *authenticate*|*unauthorized*)
        echo "Причина: нема дозволу. Зроби пару заново — пункт 3 (код з того телефона)." ;;
      *)
        echo "Подивись рядок вище (що відповів adb) і надішли цей лог у чат." ;;
    esac
    echo
    echo "Нова мережа: на тому телефоні Android питає «Дозволити налагодження"
    echo "у цій мережі?» — треба натиснути «Дозволити»."
  } | tee -a "$CURRENT_LOG"

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

  # 2026-10-15: на M30s процес зветься com.android.providers.media.module,
  # а пакет може бути з google або без — зупиняємо той, що встановлений.
  local pkg found=0
  for pkg in com.google.android.providers.media.module com.android.providers.media.module; do
    if adb -s "$ADB_TARGET" shell pm path "$pkg" 2>/dev/null | grep -q '^package:'; then
      found=1
      echo "Зупиняю: $pkg" | tee -a "$CURRENT_LOG"
      adb -s "$ADB_TARGET" shell am force-stop "$pkg" 2>&1 | tee -a "$CURRENT_LOG"
    fi
  done
  [ "$found" = 1 ] || echo "Пакет сканера медіа не знайдено — нічого не зупинено." | tee -a "$CURRENT_LOG"
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

cleanup_report() {
  clear
  if ! require_connection; then pause_menu; return; fi
  new_log "17" "cleanup-report" "Звіт для чистки (нічого не змінює)"

  echo "Збираю дані для чистки. Нічого не змінюється."
  echo "На слабкому телефоні це 1–3 хвилини — зачекай."
  echo

  {
    echo "=== DEVICE ==="
    echo "Виробник: $(adb -s "$ADB_TARGET" shell getprop ro.product.manufacturer)"
    echo "Модель: $(adb -s "$ADB_TARGET" shell getprop ro.product.model)"
    echo "Пристрій: $(adb -s "$ADB_TARGET" shell getprop ro.product.device)"
    echo "Android: $(adb -s "$ADB_TARGET" shell getprop ro.build.version.release)"
    echo "MIUI/HyperOS: $(adb -s "$ADB_TARGET" shell getprop ro.miui.ui.version.name)"
    echo "Збірка: $(adb -s "$ADB_TARGET" shell getprop ro.build.version.incremental)"
    echo
    echo "=== UPTIME ==="
    adb -s "$ADB_TARGET" shell uptime || true
    echo
    echo "=== STORAGE /data ==="
    adb -s "$ADB_TARGET" shell df -h /data || true
    echo
    echo "=== RAM ==="
    adb -s "$ADB_TARGET" shell free -h || true
    echo
    echo "=== ANIMATIONS (1 = звичайно, 0.5 = швидше) ==="
    echo "window: $(adb -s "$ADB_TARGET" shell settings get global window_animation_scale)"
    echo "transition: $(adb -s "$ADB_TARGET" shell settings get global transition_animation_scale)"
    echo "animator: $(adb -s "$ADB_TARGET" shell settings get global animator_duration_scale)"
    echo
    echo "=== USER APPS: STANDBY BUCKET ==="
    echo "# 5 exempted · 10 active · 20 working set · 30 frequent · 40 rare · 45 restricted · 50 never"
    adb -s "$ADB_TARGET" shell 'for p in $(pm list packages -3 | cut -d: -f2 | sort); do echo "$(am get-standby-bucket "$p" 2>/dev/null) $p"; done' || true
    echo
    echo "=== DISABLED PACKAGES ==="
    adb -s "$ADB_TARGET" shell pm list packages -d || true
    echo
    echo "=== BACKGROUND RESTRICTED (RUN_ANY_IN_BACKGROUND ignore) ==="
    adb -s "$ADB_TARGET" shell cmd appops query-op RUN_ANY_IN_BACKGROUND ignore 2>&1 || true
    echo
    echo "=== ADS / ANALYTICS PACKAGES (installed) ==="
    adb -s "$ADB_TARGET" shell pm list packages | grep -i -E "msa|analytics|joyose|hybrid|bugreport|mipicks|facebook" || true
  } >> "$CURRENT_LOG" 2>&1
  echo "1/3 — застосунки й налаштування: готово"

  {
    echo
    echo "=== MEMINFO: TOTAL PSS BY PROCESS (top 60) ==="
    adb -s "$ADB_TARGET" shell dumpsys meminfo > "$STATE_DIR/meminfo.tmp" 2>&1 || true
    sed -n '/Total PSS by process/,/Total PSS by OOM/p' "$STATE_DIR/meminfo.tmp" | head -n 62
    echo
    echo "=== MEMINFO: SUMMARY ==="
    grep -E "Total RAM|Free RAM|Used RAM|Lost RAM|ZRAM|Tuning" "$STATE_DIR/meminfo.tmp" || true
    rm -f "$STATE_DIR/meminfo.tmp"
  } >> "$CURRENT_LOG" 2>&1
  echo "2/3 — пам'ять: готово"

  {
    echo
    echo "=== DISKSTATS (розміри застосунків, даних і кешу) ==="
    adb -s "$ADB_TARGET" shell dumpsys diskstats || true
  } >> "$CURRENT_LOG" 2>&1
  echo "3/3 — місце на диску: готово"

  echo
  echo "Звіт збережено. Надішли цей файл у чат:"
  echo "  $CURRENT_LOG"
  pause_menu
}

update_self() {
  clear
  local app_dir="$BASE_DIR/app"
  local menu_path="$app_dir/menu.sh"
  local tmp_path="$app_dir/menu.sh.tmp"

  echo "Оновлення Phone Diagnostics"
  echo "Зараз: версія $PD_VERSION"
  echo

  if ! command -v gh >/dev/null 2>&1; then
    echo "Не знайдено GitHub CLI (gh). Оновлення неможливе."
    pause_menu
    return
  fi

  mkdir -p "$app_dir"
  echo "Завантажую нову версію з GitHub..."
  if gh api "repos/$PD_REPO/contents/$PD_SOURCE_PATH" --jq .content \
      | tr -d '\n' | base64 -d > "$tmp_path" 2>/dev/null \
      && grep -q "Phone Diagnostics Menu" "$tmp_path"; then
    mv "$tmp_path" "$menu_path"
    chmod 700 "$menu_path" 2>/dev/null || true
    echo "Готово. Нова версія: $(sed -n 's/^PD_VERSION="\(.*\)"$/\1/p' "$menu_path" | head -n 1)"
    echo "Перезапускаю меню..."
    sleep 2
    exec bash "$menu_path"
  fi

  rm -f "$tmp_path"
  echo "Не вдалося завантажити. Стара версія лишилась без змін."
  pause_menu
}

set_animations() {
  local scale="$1"
  local item="$2"
  local title="$3"
  clear
  if ! require_connection; then pause_menu; return; fi
  new_log "$item" "animations-$scale" "$title"

  local key value failed=0
  {
    echo "Модель: $(adb -s "$ADB_TARGET" shell getprop ro.product.model)"
    echo "Ставлю масштаб анімацій: $scale"
    echo
    for key in window_animation_scale transition_animation_scale animator_duration_scale; do
      adb -s "$ADB_TARGET" shell settings put global "$key" "$scale" 2>&1
      value="$(adb -s "$ADB_TARGET" shell settings get global "$key" 2>&1)"
      echo "$key = $value"
    done
  } 2>&1 | tee -a "$CURRENT_LOG"

  for key in window_animation_scale transition_animation_scale animator_duration_scale; do
    value="$(adb -s "$ADB_TARGET" shell settings get global "$key" 2>/dev/null)"
    case "$value" in
      "$scale"|"$scale.0") ;;
      *) failed=1 ;;
    esac
  done

  echo
  if [ "$failed" -eq 0 ]; then
    echo "ГОТОВО: анімації = $scale" | tee -a "$CURRENT_LOG"
  else
    {
      echo "НЕ ВИЙШЛО: телефон не дав змінити налаштування."
      echo "На Xiaomi треба увімкнути в «Для розробників»:"
      echo "  «Налагодження USB (налаштування безпеки)»"
      echo "і повторити цей пункт."
    } | tee -a "$CURRENT_LOG"
  fi
  pause_menu
}

package_label() {
  case "$1" in
    com.miui.msa.global) echo "Реклама MIUI" ;;
    com.amazon.appmanager) echo "Amazon (службова)" ;;
    com.ebay.carrier) echo "eBay (службова)" ;;
    com.facebook.appmanager) echo "Facebook App Manager (службова)" ;;
    com.facebook.services) echo "Facebook Services (службова)" ;;
    com.facebook.system) echo "Facebook App Installer (службова)" ;;
    com.mi.globalminusscreen) echo "Екран зліва від головного (App Vault)" ;;
    com.google.android.apps.magazines) echo "Google Новини" ;;
    cn.wps.xiaomi.abroad.lite) echo "WPS Office" ;;
    com.cubleopard.pocketlifeworld) echo "Pocket Life World" ;;
    com.einnovation.temu) echo "Temu" ;;
    com.google.android.apps.bard) echo "Gemini" ;;
    com.google.android.apps.podcasts) echo "Google Подкасти" ;;
    com.google.android.videos) echo "Google TV" ;;
    com.google.android.apps.youtube.music) echo "YouTube Music" ;;
    com.zhiliaoapp.musically) echo "TikTok" ;;
    com.voe.superbill) echo "voe.superbill" ;;
    *) echo "$1" ;;
  esac
}

set_bloat_enabled() {
  local mode="$1"
  local item="$2"
  local title="$3"
  clear
  if ! require_connection; then pause_menu; return; fi

  local pkg answer disabled_list ok=0 skipped=0 failed=0
  echo "$title"
  echo "Телефон: $(adb -s "$ADB_TARGET" shell getprop ro.product.model)"
  echo
  for pkg in $BLOAT_PACKAGES; do
    echo "  • $(package_label "$pkg")"
  done
  echo
  if [ "$mode" = "disable" ]; then
    echo "Програми НЕ видаляються. Повернути: пункт 22."
  fi
  printf "Продовжити? Напиши так / ні (можна y / n): "
  read -r answer
  case "$answer" in
    так|Так|ТАК|y|Y|yes|д|Д) ;;
    *) echo "Скасовано. Нічого не змінено."; pause_menu; return ;;
  esac

  new_log "$item" "bloat-$mode" "$title"
  for pkg in $BLOAT_PACKAGES; do
    if ! adb -s "$ADB_TARGET" shell pm path "$pkg" >/dev/null 2>&1; then
      echo "— $(package_label "$pkg"): немає на телефоні" | tee -a "$CURRENT_LOG"
      skipped=$((skipped + 1))
      continue
    fi
    if [ "$mode" = "disable" ]; then
      adb -s "$ADB_TARGET" shell pm disable-user --user 0 "$pkg" >> "$CURRENT_LOG" 2>&1
    else
      adb -s "$ADB_TARGET" shell pm enable --user 0 "$pkg" >> "$CURRENT_LOG" 2>&1
    fi
    disabled_list="$(adb -s "$ADB_TARGET" shell pm list packages -d 2>/dev/null)"
    if printf '%s\n' "$disabled_list" | grep -qx "package:$pkg"; then
      if [ "$mode" = "disable" ]; then
        echo "ГОТОВО: $(package_label "$pkg") — вимкнено" | tee -a "$CURRENT_LOG"; ok=$((ok + 1))
      else
        echo "НЕ ВИЙШЛО: $(package_label "$pkg") — досі вимкнено" | tee -a "$CURRENT_LOG"; failed=$((failed + 1))
      fi
    else
      if [ "$mode" = "disable" ]; then
        echo "НЕ ВИЙШЛО: $(package_label "$pkg") — телефон не дав вимкнути" | tee -a "$CURRENT_LOG"; failed=$((failed + 1))
      else
        echo "ГОТОВО: $(package_label "$pkg") — увімкнено" | tee -a "$CURRENT_LOG"; ok=$((ok + 1))
      fi
    fi
  done

  echo | tee -a "$CURRENT_LOG"
  echo "Підсумок: готово $ok · не вийшло $failed · немає на телефоні $skipped" | tee -a "$CURRENT_LOG"
  pause_menu
}

set_apps_sleep() {
  local mode="$1"
  local item="$2"
  local title="$3"
  clear
  if ! require_connection; then pause_menu; return; fi

  local pkg answer op bucket ok=0 skipped=0 failed=0
  echo "$title"
  echo "Телефон: $(adb -s "$ADB_TARGET" shell getprop ro.product.model)"
  echo
  for pkg in $SLEEP_PACKAGES; do
    echo "  • $(package_label "$pkg")"
  done
  echo
  if [ "$mode" = "sleep" ]; then
    echo "Нічого не видаляється. Застосунок відкривається як звичайно,"
    echo "лише не працює у фоні. Розбудити всі: пункт 24."
  fi
  printf "Продовжити? Напиши так / ні (можна y / n): "
  read -r answer
  case "$answer" in
    так|Так|ТАК|y|Y|yes|д|Д) ;;
    *) echo "Скасовано. Нічого не змінено."; pause_menu; return ;;
  esac

  new_log "$item" "apps-$mode" "$title"
  for pkg in $SLEEP_PACKAGES; do
    if ! adb -s "$ADB_TARGET" shell pm path "$pkg" >/dev/null 2>&1; then
      echo "— $(package_label "$pkg"): немає на телефоні" | tee -a "$CURRENT_LOG"
      skipped=$((skipped + 1))
      continue
    fi
    if [ "$mode" = "sleep" ]; then
      adb -s "$ADB_TARGET" shell cmd appops set "$pkg" RUN_ANY_IN_BACKGROUND ignore >> "$CURRENT_LOG" 2>&1
      adb -s "$ADB_TARGET" shell am set-standby-bucket "$pkg" restricted >> "$CURRENT_LOG" 2>&1
    else
      adb -s "$ADB_TARGET" shell cmd appops set "$pkg" RUN_ANY_IN_BACKGROUND allow >> "$CURRENT_LOG" 2>&1
      adb -s "$ADB_TARGET" shell am set-standby-bucket "$pkg" active >> "$CURRENT_LOG" 2>&1
    fi
    op="$(adb -s "$ADB_TARGET" shell cmd appops get "$pkg" RUN_ANY_IN_BACKGROUND 2>&1)"
    bucket="$(adb -s "$ADB_TARGET" shell am get-standby-bucket "$pkg" 2>&1)"
    echo "  [$pkg] appops: $op · bucket: $bucket" >> "$CURRENT_LOG"
    if [ "$mode" = "sleep" ]; then
      if printf '%s' "$op" | grep -q "ignore"; then
        echo "ГОТОВО: $(package_label "$pkg") — спить" | tee -a "$CURRENT_LOG"; ok=$((ok + 1))
      else
        echo "НЕ ВИЙШЛО: $(package_label "$pkg")" | tee -a "$CURRENT_LOG"; failed=$((failed + 1))
      fi
    else
      if printf '%s' "$op" | grep -q "ignore"; then
        echo "НЕ ВИЙШЛО: $(package_label "$pkg") — досі спить" | tee -a "$CURRENT_LOG"; failed=$((failed + 1))
      else
        echo "ГОТОВО: $(package_label "$pkg") — розбуджено" | tee -a "$CURRENT_LOG"; ok=$((ok + 1))
      fi
    fi
  done

  echo | tee -a "$CURRENT_LOG"
  echo "Підсумок: готово $ok · не вийшло $failed · немає на телефоні $skipped" | tee -a "$CURRENT_LOG"
  pause_menu
}

# Пункт 25 (2026-10-14, Renault Docs): «Мої файли» Samsung не показує Renault Docs
# у «Відкрити за допомогою» для .rdproject, а ES Провідник показує. Тут видно,
# що саме файловий менеджер надіслав Android: адресу (dat=…) і тип (typ=…).
# Лише читає журнал і список вікон; нічого не змінює.
open_with_intent() {
  clear
  if ! require_connection; then pause_menu; return; fi
  new_log "25" "open-with-intent" "Що надсилає «Відкрити за допомогою»"
  adb -s "$ADB_TARGET" logcat -c >/dev/null 2>&1 || true
  echo "1) На ТОМУ телефоні (до якого підключено ADB) відкрий файловий менеджер"
  echo "   і торкнись файлу (напр. .rdproject)."
  echo "2) Коли з'явиться «Відкрити за допомогою» — НЕ закривай його."
  echo "3) Повернись сюди і натисни Enter."
  read -r _
  {
    echo "=== Вікно зверху (dumpsys activity activities) ==="
    adb -s "$ADB_TARGET" shell dumpsys activity activities \
      | grep -E "Intent \{|ResolverActivity|ChooserActivity|mResumedActivity|topResumedActivity" \
      | head -n 40 || true
    echo
    echo "=== Журнал запусків (logcat) ==="
    adb -s "$ADB_TARGET" logcat -d -v time \
      | grep -E "START u0|ResolverActivity|ChooserActivity|act=android.intent.action.(VIEW|SEND)" \
      | tail -n 40 || true
    echo
    echo "Шукай рядок з act=android.intent.action.VIEW: dat=… — адреса, typ=… — тип."
  } 2>&1 | tee -a "$CURRENT_LOG"
  echo
  echo "Тепер можна закрити «Відкрити за допомогою» на тому телефоні."
  pause_menu
}

# Пункти 26–27 (2026-10-15): Aolithium (виробник зник, у магазині нема) і стара
# робоча версія JK BMS. Сучасні застосунки — це кілька файлів (base.apk +
# split_config.*.apk); передача одного base.apk дає «не встановлюється» або
# «не працює». Тому зберігаємо й ставимо всі частини разом.
APK_DIR="$BASE_DIR/apk"

save_app_apks() {
  clear
  if ! require_connection; then pause_menu; return; fi

  local pattern list pkg answer n i version dir paths path saved=0
  echo "Зберегти застосунок з телефона: $(adb -s "$ADB_TARGET" shell getprop ro.product.model)"
  echo
  printf "Що шукати в назві пакета (Enter = bms|lith): "
  read -r pattern
  pattern="${pattern:-bms|lith}"

  list="$(adb -s "$ADB_TARGET" shell pm list packages -3 2>/dev/null | tr -d '\r' | sed 's/^package://' | grep -i -E "$pattern" | sort)"
  if [ -z "$list" ]; then
    echo "Нічого не знайдено за «$pattern»."
    pause_menu
    return
  fi

  echo
  i=0
  while IFS= read -r pkg; do
    i=$((i + 1))
    version="$(adb -s "$ADB_TARGET" shell dumpsys package "$pkg" 2>/dev/null | tr -d '\r' | sed -n 's/^ *versionName=//p' | head -n 1)"
    echo "  $i) $pkg  (версія ${version:-?})"
  done <<EOF2
$list
EOF2
  echo
  printf "Номер застосунку (0 = скасувати): "
  read -r n
  case "$n" in ''|*[!0-9]*|0) echo "Скасовано."; pause_menu; return ;; esac
  pkg="$(printf '%s\n' "$list" | sed -n "${n}p")"
  if [ -z "$pkg" ]; then echo "Нема такого номера."; pause_menu; return; fi

  version="$(adb -s "$ADB_TARGET" shell dumpsys package "$pkg" 2>/dev/null | tr -d '\r' | sed -n 's/^ *versionName=//p' | head -n 1)"
  dir="$APK_DIR/${pkg}_v${version:-unknown}"
  mkdir -p "$dir"
  new_log "26" "save-apk" "Зберегти застосунок $pkg"

  paths="$(adb -s "$ADB_TARGET" shell pm path "$pkg" 2>/dev/null | tr -d '\r' | sed 's/^package://')"
  {
    echo "Пакет: $pkg"
    echo "Версія: ${version:-?}"
    echo "Телефон: $(adb -s "$ADB_TARGET" shell getprop ro.product.model)"
    echo "Android: $(adb -s "$ADB_TARGET" shell getprop ro.build.version.release)"
    echo "Частини:"
    printf '%s\n' "$paths"
  } | tee -a "$CURRENT_LOG" > "$dir/info.txt"

  while IFS= read -r path; do
    [ -n "$path" ] || continue
    if adb -s "$ADB_TARGET" pull "$path" "$dir/$(basename "$path")" >> "$CURRENT_LOG" 2>&1; then
      saved=$((saved + 1))
      echo "ГОТОВО: $(basename "$path")" | tee -a "$CURRENT_LOG"
    else
      echo "НЕ ВИЙШЛО: $(basename "$path")" | tee -a "$CURRENT_LOG"
    fi
  done <<EOF2
$paths
EOF2

  echo | tee -a "$CURRENT_LOG"
  if [ "$saved" -eq 0 ]; then
    echo "НЕ ВИЙШЛО: телефон не віддав жодної частини APK." | tee -a "$CURRENT_LOG"
    pause_menu
    return
  fi
  echo "Збережено частин: $saved → $dir" | tee -a "$CURRENT_LOG"

  # Один файл .apks для чужого телефона: відкривається застосунком SAI
  # («Split APKs Installer» з Play Маркету), який ставить усі частини разом.
  local share="$APK_DIR/${pkg}_v${version:-unknown}.apks"
  if ! command -v zip >/dev/null 2>&1; then
    echo "Ставлю zip (один раз)..." | tee -a "$CURRENT_LOG"
    pkg install -y zip >> "$CURRENT_LOG" 2>&1 || true
  fi
  if command -v zip >/dev/null 2>&1 && (cd "$dir" && zip -q -j -X "$share.tmp" ./*.apk) && mv "$share.tmp" "$share"; then
    {
      echo
      echo "Файл для іншого телефона: $share"
      echo "Надішли його (Viber/Telegram/Quick Share). На тому телефоні:"
      echo "Play Маркет → «SAI» (Split APKs Installer) → відкрити цей файл → Встановити."
    } | tee -a "$CURRENT_LOG"
  else
    rm -f "$share.tmp"
    echo "Файл .apks не вийшов (нема zip). Частини лежать у папці вище." | tee -a "$CURRENT_LOG"
  fi
  pause_menu
}

install_saved_app() {
  clear
  if ! require_connection; then pause_menu; return; fi

  local dirs n dir answer i
  mkdir -p "$APK_DIR"
  dirs="$(ls -1 "$APK_DIR" 2>/dev/null | grep -v '\.apks$' | sort)"
  if [ -z "$dirs" ]; then
    echo "Ще нічого не збережено. Спершу пункт 26."
    pause_menu
    return
  fi

  echo "Поставити збережений застосунок на: $(adb -s "$ADB_TARGET" shell getprop ro.product.model)"
  echo
  i=0
  while IFS= read -r dir; do
    i=$((i + 1))
    echo "  $i) $dir  ($(ls -1 "$APK_DIR/$dir"/*.apk 2>/dev/null | wc -l) част.)"
  done <<EOF2
$dirs
EOF2
  echo
  printf "Номер (0 = скасувати): "
  read -r n
  case "$n" in ''|*[!0-9]*|0) echo "Скасовано."; pause_menu; return ;; esac
  dir="$(printf '%s\n' "$dirs" | sed -n "${n}p")"
  if [ -z "$dir" ]; then echo "Нема такого номера."; pause_menu; return; fi
  if ! ls "$APK_DIR/$dir"/*.apk >/dev/null 2>&1; then
    echo "У «$dir» нема APK — збережи ще раз пунктом 26."
    pause_menu
    return
  fi

  echo
  echo "Встановлюю «$dir» поверх (дані застосунку не видаляються)."
  printf "Продовжити? Напиши так / ні (можна y / n): "
  read -r answer
  case "$answer" in
    так|Так|ТАК|y|Y|yes|д|Д) ;;
    *) echo "Скасовано. Нічого не змінено."; pause_menu; return ;;
  esac

  new_log "27" "install-apk" "Поставити $dir"
  if adb -s "$ADB_TARGET" install-multiple -r "$APK_DIR/$dir"/*.apk 2>&1 | tee -a "$CURRENT_LOG" | grep -q "Success"; then
    echo "ГОТОВО: встановлено. Відкрий застосунок на тому телефоні." | tee -a "$CURRENT_LOG"
  else
    {
      echo "НЕ ВИЙШЛО. Причина — у рядку вище (INSTALL_FAILED_...)."
      echo "Часто: на телефоні вже стоїть НОВІША версія (VERSION_DOWNGRADE),"
      echo "або Xiaomi без «Встановлення через USB» не дає ставити по adb."
    } | tee -a "$CURRENT_LOG"
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

media_scan_report() {
  # 2026-10-15: на M30s сканер медіа (com.android.providers.media.module)
  # годинами тримав ~1 ядро (знімки 068, 069). Цей пункт лише читає:
  # що він робить, які файли щойно змінювались, де багато файлів.
  clear
  if ! require_connection; then pause_menu; return; fi
  new_log "28" "media-scan" "Що робить сканер медіа"
  echo "Збираю дані, до 3 хвилин. Нічого не змінюю."
  echo
  local pid
  pid="$(adb -s "$ADB_TARGET" shell pidof com.android.providers.media.module 2>/dev/null | tr -d '\r' | awk '{print $1}')"
  {
    echo "=== Пакети сканера медіа ==="
    adb -s "$ADB_TARGET" shell pm list packages -U 2>/dev/null | grep -i "providers.media" || true
    echo
    echo "PID процесу: ${pid:-не знайдено}"
    if [ -n "$pid" ]; then
      echo
      echo "=== Потоки сканера (хто з них крутиться) ==="
      adb -s "$ADB_TARGET" shell top -b -n 1 -H -p "$pid" 2>&1 | head -n 30
      echo
      echo "=== Журнал сканера (останні рядки) ==="
      adb -s "$ADB_TARGET" shell logcat -d -v time --pid="$pid" -t 4000 2>&1 | tail -n 250
    fi
    echo
    echo "=== Інші записи журналу про скан ==="
    adb -s "$ADB_TARGET" shell logcat -d -v time -t 8000 2>&1 \
      | grep -i -E "MediaScanner|ModernMediaScanner|MediaProvider|FuseDaemon|MediaService" \
      | tail -n 120 || true
    echo
    echo "=== Фонові задачі сканера ==="
    adb -s "$ADB_TARGET" shell dumpsys jobscheduler 2>/dev/null \
      | grep -i -E "providers.media" | head -n 40 || true
    echo
    echo "=== Файли, змінені за останні 30 хв (без Android/) ==="
    adb -s "$ADB_TARGET" shell "timeout 90 find /storage/emulated/0 -path '*/Android' -prune -o -type f -mmin -30 -print 2>/dev/null | head -n 80"
    echo
    echo "=== Скільки файлів у папках (верхній рівень і Documents) ==="
    adb -s "$ADB_TARGET" shell 'cd /storage/emulated/0 2>/dev/null || exit 0
      for d in */ Documents/*/ Music/*/; do
        [ -d "$d" ] || continue
        case "$d" in Android/) continue ;; esac
        n=$(timeout 20 find "$d" -type f 2>/dev/null | wc -l)
        nm=""; [ -f "$d.nomedia" ] && nm=" (.nomedia)"
        echo "$n  $d$nm"
      done | sort -rn | head -n 40'
  } 2>&1 | tee -a "$CURRENT_LOG"
  echo
  echo "Готово. Надішли цей файл у чат (шлях — нижче)."
  pause_menu
}
# media_scan_report end

gallery_count_in_index() {
  # Скільки файлів з папки $1 ще є в індексі медіа (його бачить Галерея).
  local out
  out="$(adb -s "$ADB_TARGET" shell "content query --uri content://media/external/file --projection _id --where \"_data LIKE '$1/%'\"" 2>&1)"
  case "$out" in
    *Exception*|*Error*|*rror:*) echo "?" ;;
    *) printf '%s\n' "$out" | grep -c '^Row:' ;;
  esac
}

gallery_nomedia_rescan() {
  # 2026-10-15 (faric): у деяких папках лежить .nomedia, а Галерея Samsung
  # їх досі показує — індекс медіа ще не перечитав ці папки.
  # Не чистимо дані Галереї/Медіасховища (зник би кошик Галереї й плейлисти):
  # лише просимо індекс перечитати саме ці папки. Файли не чіпаємо.
  clear
  if ! require_connection; then pause_menu; return; fi
  new_log "29" "gallery-nomedia" "Галерея: прибрати папки з .nomedia"
  echo "Шукаю папки з .nomedia (телефон і карта пам'яті), до 3 хвилин..."
  local dirs
  dirs="$(adb -s "$ADB_TARGET" shell 'for r in /storage/emulated/0 /storage/*-*; do
      [ -d "$r" ] || continue
      timeout 120 find "$r" -name .nomedia -type f 2>/dev/null
    done' | tr -d '\r' | sed 's#/\.nomedia$##' | grep -v "'" | sort -u)"
  if [ -z "$dirs" ]; then
    echo "Папок з .nomedia не знайдено." | tee -a "$CURRENT_LOG"
    pause_menu
    return
  fi

  local dir n total=0 shown=""
  {
    echo
    echo "=== Папки з .nomedia: скільки файлів з них ще бачить Галерея ==="
  } | tee -a "$CURRENT_LOG"
  while IFS= read -r dir; do
    [ -n "$dir" ] || continue
    n="$(gallery_count_in_index "$dir")"
    echo "$n  $dir" | tee -a "$CURRENT_LOG"
    case "$n" in ''|*[!0-9]*) ;; *) total=$((total + n)); [ "$n" -gt 0 ] && shown="$shown$dir
" ;; esac
  done <<EOF29
$dirs
EOF29
  echo | tee -a "$CURRENT_LOG"
  if [ -z "$shown" ]; then
    echo "Галерея вже не бачить жодної з цих папок. Нічого робити не треба." | tee -a "$CURRENT_LOG"
    pause_menu
    return
  fi

  echo "Галерея ще бачить $total файлів з цих папок."
  echo "Попрошу Android перечитати ЛИШЕ ці папки. Файли не видаляються,"
  echo "дані Галереї (кошик, альбоми) не чіпаються."
  echo "Якщо зараз Renault імпортує томи — краще дочекатись кінця імпорту."
  printf "Перечитати? Напиши так / ні (можна y / n): "
  local answer
  read -r answer
  case "$answer" in
    так|Так|ТАК|y|Y|yes|д|Д) ;;
    *) echo "Скасовано. Нічого не змінено." | tee -a "$CURRENT_LOG"; pause_menu; return ;;
  esac

  {
    echo
    echo "=== Прошу перечитати ==="
  } | tee -a "$CURRENT_LOG"
  while IFS= read -r dir; do
    [ -n "$dir" ] || continue
    echo "→ $dir" | tee -a "$CURRENT_LOG"
    adb -s "$ADB_TARGET" shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE \
      -p com.android.providers.media.module -d "\"file://$dir\"" 2>&1 | tail -n 1 | tee -a "$CURRENT_LOG"
  done <<EOF29
$shown
EOF29

  echo "Чекаю, поки Android перечитає (до 3 хвилин)..." | tee -a "$CURRENT_LOG"
  local i left
  for i in $(seq 1 18); do
    sleep 10
    left=0
    while IFS= read -r dir; do
      [ -n "$dir" ] || continue
      n="$(gallery_count_in_index "$dir")"
      case "$n" in ''|*[!0-9]*) ;; *) left=$((left + n)) ;; esac
    done <<EOF29
$shown
EOF29
    echo "  через $((i * 10)) с: ще видно $left" | tee -a "$CURRENT_LOG"
    [ "$left" -eq 0 ] && break
  done

  {
    echo
    echo "=== Підсумок ==="
    echo "Було видно: $total → стало: $left"
    if [ "$left" -eq 0 ]; then
      echo "ГОТОВО: Галерея більше не бачить ці папки. Відкрий Галерею й перевір."
    elif [ "$left" -lt "$total" ]; then
      echo "Іде, але ще не все. Запусти пункт 29 ще раз за кілька хвилин."
    else
      echo "НЕ ЗМІНИЛОСЬ: Android не перечитав. Надішли цей файл у чат."
    fi
  } | tee -a "$CURRENT_LOG"
  pause_menu
}
# gallery_nomedia_rescan end

crypto_prices() {
  # 2026-10-15 (faric): ціни монет для сторінки-тренажера «Крипто-тренажер».
  # Лише публічна історія цін Binance (свічки за день): без входу, без ключів,
  # нічого не купує й не продає. Телефон підключати не треба.
  clear
  local dir="/storage/emulated/0/Documents/Crypto"
  local coins="ETH BNB TRX XRP SOL LTC DOT XLM NEAR PEPE ALGO VTHO"
  if ! command -v curl >/dev/null 2>&1; then
    echo "Немає curl. Спершу пункт 2 — Підготувати Termux."
    pause_menu
    return
  fi
  mkdir -p "$dir" && touch "$dir/.nomedia"
  new_log "30" "crypto-prices" "Ціни криптовалют для тренажера"
  local out="$dir/binance-1d-$(date '+%Y%m%d-%H%M').json"
  local part="$out.part" first=1 c resp host ok=0 bad="" days
  echo "Завантажую історію цін (до 1000 днів) з Binance..." | tee -a "$CURRENT_LOG"
  printf '{' > "$part"
  for c in $coins; do
    resp=""
    for host in api.binance.com data-api.binance.vision; do
      resp="$(curl -sS -m 40 "https://$host/api/v3/klines?symbol=${c}USDT&interval=1d&limit=1000" 2>&1)"
      case "$resp" in \[\[*) break ;; esac
    done
    case "$resp" in
      \[\[*)
        [ "$first" = 1 ] || printf ',' >> "$part"
        printf '"%s":%s' "$c" "$resp" >> "$part"
        first=0
        ok=$((ok + 1))
        days=$(( $(printf '%s' "$resp" | grep -o '\],\[' | wc -l) + 1 ))
        echo "  $c — $days днів" | tee -a "$CURRENT_LOG"
        ;;
      *)
        bad="$bad $c"
        echo "  $c — не вийшло: $(printf '%s' "$resp" | head -c 120)" | tee -a "$CURRENT_LOG"
        ;;
    esac
  done
  printf '}' >> "$part"
  if [ "$ok" -gt 0 ]; then
    mv "$part" "$out"
    {
      echo
      echo "ГОТОВО: $ok монет. Файл з цінами:"
      echo "  $out"
      [ -n "$bad" ] && echo "Не вийшло:$bad"
      echo "У тренажері натисни «Відкрити файл з цінами» → Documents → Crypto."
    } | tee -a "$CURRENT_LOG"
  else
    rm -f "$part"
    echo "НЕ ВИЙШЛО жодної монети — перевір інтернет. Причина — у рядках вище." | tee -a "$CURRENT_LOG"
  fi
  pause_menu
}
# crypto_prices end

app_threads_report() {
  # 2026-10-16 (faric, M30s): Audio Converter кодує MP3 ~4 с на 1 % смуги
  # (≈ пісня за реальний час), хоча процес бере 2+ ядра. Після перезавантаження
  # (вільно 250 МБ замість 80) — так само. Цей пункт лише читає: які потоки
  # застосунку крутяться, з яким пріоритетом, у якій групі ядер (великі / малі).
  clear
  if ! require_connection; then pause_menu; return; fi
  local pkg
  echo "Який застосунок дивитись? Enter = Audio Converter (ua.faric.audioconverter)."
  printf "Пакет: "
  read -r pkg
  [ -n "$pkg" ] || pkg="ua.faric.audioconverter"
  new_log "31" "app-threads" "Потоки застосунку: $pkg"
  echo "Збираю дані ~15 секунд. Нічого не змінюю."
  echo "Краще запускати, поки застосунок саме працює (напр. кодує)."
  echo
  local pid
  pid="$(adb -s "$ADB_TARGET" shell pidof "$pkg" 2>/dev/null | tr -d '\r' | awk '{print $1}')"
  {
    echo "Пакет: $pkg"
    echo "PID процесу: ${pid:-не знайдено (застосунок не запущений)}"
    echo
    echo "=== Ядра процесора: які групи на яких ядрах ==="
    adb -s "$ADB_TARGET" shell 'for g in top-app foreground background system-background restricted; do
        [ -f /dev/cpuset/$g/cpus ] && echo "$g: $(cat /dev/cpuset/$g/cpus)"
      done
      for c in /sys/devices/system/cpu/cpu[0-9]*; do
        f=$(cat $c/cpufreq/cpuinfo_max_freq 2>/dev/null)
        [ -n "$f" ] && echo "${c##*/}: до $((f / 1000)) МГц"
      done' 2>&1
    if [ -n "$pid" ]; then
      echo
      echo "=== Стан процесу в Android (група планування, пріоритет) ==="
      adb -s "$ADB_TARGET" shell dumpsys activity processes "$pkg" 2>/dev/null \
        | grep -E "ProcessRecord|curSchedGroup|setSchedGroup|curProcState|setProcState|curAdj|setAdj|hasForegroundServices" \
        | head -n 20 || true
      echo
      echo "=== Потоки: група ядер (cpuset) і пріоритет (nice) ==="
      adb -s "$ADB_TARGET" shell "for t in /proc/$pid/task/*; do
          tid=\${t##*/}
          name=\$(cat \$t/comm 2>/dev/null)
          cs=\$(cat \$t/cpuset 2>/dev/null)
          rest=\$(sed 's/.*) //' \$t/stat 2>/dev/null)
          ni=\$(echo \$rest | cut -d' ' -f17)
          cpu=\$(echo \$rest | cut -d' ' -f37)
          echo \"\$tid  nice=\$ni  ядро=\$cpu  група=\$cs  \$name\"
        done" 2>&1 | head -n 80
      echo
      echo "=== Хто з потоків крутиться (два знімки з перервою 5 с; дивись другий) ==="
      adb -s "$ADB_TARGET" shell top -b -n 2 -d 5 -H -p "$pid" 2>&1 | head -n 70
    fi
  } 2>&1 | tee -a "$CURRENT_LOG"
  echo
  echo "Готово. Надішли цей файл у чат (шлях — нижче)."
  pause_menu
}
# app_threads_report end

app_compile_status() {
  # Як Android зараз виконує код застосунку: verify/quicken = інтерпретатор + JIT,
  # speed-profile = гарячі частини вже в машинному коді, speed = увесь код.
  adb -s "$ADB_TARGET" shell dumpsys package dexopt 2>/dev/null \
    | grep -A 6 "\[$1\]" | grep -E "\[$1\]|status=|reason=" | head -n 8
}

app_compile_speed() {
  # 2026-10-16 (faric, M30s, пункт 31, лог 092): Audio Converter кодує MP3
  # ~4 с на 1 %, а потік «Jit thread pool» крутиться 99 % ядра — стільки ж,
  # скільки саме кодування. Застосунок зібрано на телефоні й поставлено щойно,
  # тож Android виконує jump3r інтерпретатором і постійно докомпільовує.
  # Повна компіляція (`cmd package compile -m speed`) робить те саме, що Android
  # і так зробив би вночі на зарядці, лише зараз. Звук і файли — ті самі, дані
  # застосунку не чіпаються. Нова версія застосунку (оновлення) скидає це —
  # тоді пункт 32 ще раз.
  clear
  if ! require_connection; then pause_menu; return; fi
  local pkg answer mode
  echo "Прискорити застосунок: Android заздалегідь переводить увесь його код"
  echo "у машинний (те, що він і так робить сам уночі на зарядці)."
  echo "Звук і результат — ті самі. Дані застосунку НЕ чіпаються."
  echo "Після оновлення застосунку — повторити цей пункт."
  echo
  echo "Який застосунок? Enter = Audio Converter (ua.faric.audioconverter)."
  echo "Track Catcher — ua.faric.trackcatcher."
  printf "Пакет: "
  read -r pkg
  [ -n "$pkg" ] || pkg="ua.faric.audioconverter"
  if ! adb -s "$ADB_TARGET" shell pm path "$pkg" >/dev/null 2>&1; then
    echo "Застосунку $pkg на підключеному телефоні немає."
    pause_menu
    return
  fi
  echo
  echo "1 — прискорити (повна компіляція, 1–3 хв)"
  echo "2 — повернути як було після встановлення"
  printf "Вибір (Enter = 1): "
  read -r mode
  [ -n "$mode" ] || mode="1"
  case "$mode" in 1|2) ;; *) echo "Скасовано. Нічого не змінено."; pause_menu; return ;; esac
  echo
  echo "Якщо застосунок саме конвертує — краще дочекатись кінця файлу"
  echo "(або поставити паузу «після цього файлу»)."
  printf "Продовжити? Напиши так / ні (можна y / n): "
  read -r answer
  case "$answer" in
    так|Так|ТАК|y|Y|yes|д|Д) ;;
    *) echo "Скасовано. Нічого не змінено."; pause_menu; return ;;
  esac
  new_log "32" "app-compile" "Компіляція застосунку: $pkg (режим $mode)"
  {
    echo "Пакет: $pkg"
    echo
    echo "=== Було ==="
    app_compile_status "$pkg"
    echo
    if [ "$mode" = "1" ]; then
      echo "=== Компілюю (speed), чекай ==="
      adb -s "$ADB_TARGET" shell cmd package compile -m speed -f "$pkg" 2>&1
    else
      echo "=== Повертаю як після встановлення ==="
      adb -s "$ADB_TARGET" shell cmd package compile --reset "$pkg" 2>&1
    fi
    echo
    echo "=== Стало ==="
    app_compile_status "$pkg"
  } 2>&1 | tee -a "$CURRENT_LOG"
  echo
  echo "Готово. Відкрий застосунок заново і запусти наступний файл."
  echo "Порівняй швидкість (скільки секунд на 1 %)."
  pause_menu
}
# app_compile_speed end

# Шапка меню: велика назва між смужками (faric 2026-10-16 — «щоб одразу видно, яке це меню»).
# Букви — напівблоки ▀▄█ (шрифт 3×5, згенеровано скриптом menu_banner.py), ширина 45 знаків.
# Вужчий екран — проста шапка одним рядком.
show_banner() {
  local c="\033[1;32m" d="\033[0;32m" z="\033[0m" cols
  cols="$(stty size 2>/dev/null | cut -d' ' -f2)"
  if [ "${cols:-80}" -ge 45 ] 2>/dev/null; then
    echo -e "${d}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${z}"
    echo -e "${c}             █▀█ █ █ █▀█ █▀▄ █▀▀${z}"
    echo -e "${c}             █▀▀ █▀█ █ █ █ █ █▀${z}"
    echo -e "${c}             ▀   ▀ ▀ ▀▀▀ ▀ ▀ ▀▀▀${z}"
    echo -e "${c} █▀▄ ▀█▀ █▀█ █▀▀ █▀▄ █▀█ █▀▀ ▀█▀ ▀█▀ █▀▀ █▀▀${z}"
    echo -e "${c} █ █  █  █▀█ █ █ █ █ █ █ ▀▀█  █   █  █   ▀▀█${z}"
    echo -e "${c} ▀▀  ▀▀▀ ▀ ▀ ▀▀▀ ▀ ▀ ▀▀▀ ▀▀▀  ▀  ▀▀▀ ▀▀▀ ▀▀▀${z}"
    echo -e "${d}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${z}"
  else
    echo -e "${c}━━ PHONE DIAGNOSTICS ━━${z}"
  fi
}

while true; do
  clear
  target="$(load_target)"
  show_banner
  echo "             Phone Diagnostics Menu"
  echo "Версія: $PD_VERSION"
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
  echo "17 — Звіт для чистки → файл (нічого не змінює)"
  echo "18 — Оновити це меню з GitHub"
  echo "19 — Анімації швидше (0,5x)"
  echo "20 — Анімації як було (1x)"
  echo "21 — Вимкнути рекламу MIUI і службові Amazon/eBay/Facebook"
  echo "22 — Повернути програми з пункту 21"
  echo "23 — Приспати рідко вживані застосунки"
  echo "24 — Розбудити застосунки з пункту 23"
  echo "25 — Що надсилає «Відкрити за допомогою» → файл"
  echo "26 — Зберегти застосунок (усі частини APK) з телефона"
  echo "27 — Поставити збережений застосунок на телефон"
  echo "28 — Що робить сканер медіа → файл (нічого не змінює)"
  echo "29 — Галерея: прибрати папки з .nomedia (файли не чіпає)"
  echo "30 — Ціни криптовалют для тренажера → файл (лише читає Binance)"
  echo "31 — Що роблять потоки застосунку (ядра, пріоритет) → файл"
  echo "32 — Прискорити застосунок (машинний код; звук і дані ті самі)"
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
    17) cleanup_report ;;
    18) update_self ;;
    19) set_animations "0.5" "19" "Анімації швидше (0,5x)" ;;
    20) set_animations "1" "20" "Анімації як було (1x)" ;;
    21) set_bloat_enabled "disable" "21" "Вимкнути рекламу MIUI і службові програми" ;;
    22) set_bloat_enabled "enable" "22" "Повернути програми з пункту 21" ;;
    23) set_apps_sleep "sleep" "23" "Приспати рідко вживані застосунки" ;;
    24) set_apps_sleep "wake" "24" "Розбудити застосунки з пункту 23" ;;
    25) open_with_intent ;;
    26) save_app_apks ;;
    27) install_saved_app ;;
    28) media_scan_report ;;
    29) gallery_nomedia_rescan ;;
    30) crypto_prices ;;
    31) app_threads_report ;;
    32) app_compile_speed ;;
    0) clear; exit 0 ;;
    *) echo; echo "Невідомий пункт: $choice"; sleep 1 ;;
  esac
done
