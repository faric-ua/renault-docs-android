#!/data/data/com.termux/files/usr/bin/bash
set -u

PD_VERSION="2026-10-29.2"
PD_REPO="faric-ua/renault-docs-android"
PD_SOURCE_PATH="tools/termux/phone-diagnostics-menu.sh"
BASE_DIR="/storage/emulated/0/Documents/PhoneDiagnostics"
# Пункти 21–22: реклама MIUI і службові програми, які не потрібні.
# Вимикаються (не видаляються) і повертаються одним пунктом.
# Пункти 23–24: рідко вживані застосунки бабусиного Redmi (звіт 17, кошик 40).
# Не працюють у фоні, поки їх не відкрити; відкриваються як звичайно.
SLEEP_PACKAGES="cn.wps.xiaomi.abroad.lite com.cubleopard.pocketlifeworld com.einnovation.temu com.google.android.apps.bard com.google.android.apps.podcasts com.google.android.videos com.google.android.apps.youtube.music com.zhiliaoapp.musically com.voe.superbill"
# 2026-10-20 (чат Track Catcher, T85; faric): M30s пише музику з ривками 10–20 мс — бракує пам'яті.
# Для Samsung — свої списки; пункти 21–24 беруть список за виробником підключеного телефона.
# Не чіпати: Track Catcher, Audio Converter, Suno, Poweramp, AIMP, месенджери, Quick Share, Samsung Cloud.
# Galaxy Wearable — у списку «вимкнути»: годинника в faric нема (faric 2026-10-20 01:13–01:15).
SAMSUNG_SLEEP_PACKAGES="com.zhiliaoapp.musically com.ss.android.ugc.trill com.zhiliaoapp.musically.go com.estrongs.android.pop com.moonshot.kimichat com.android.vending com.google.android.googlequicksearchbox"
SAMSUNG_BLOAT_PACKAGES="com.samsung.android.app.watchmanager2 com.facebook.appmanager com.facebook.services com.facebook.system com.samsung.android.dqagent com.sec.android.diagmonagent com.samsung.android.dynamiclock com.samsung.android.game.gos"
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

  if [ -n "$own" ]; then
    echo "Мій IP: $own" >&2
  else
    echo "Мій IP: не вдалось дізнатись — підказка з останньої адреси, перевір мережу." >&2
  fi
  if [ -n "$last" ]; then
    echo "Лише Enter (нічого не дописуючи) = $last (останній)" >&2
  fi
  echo "Допиши останнє число або зітри й напиши своє:" >&2
  # 2026-10-18 (faric): поле було невидиме — `read -e` пише підказку й текст
  # у stderr, а `2>/dev/null` його ховав: стирав і писав наосліп. Тепер stderr
  # не глушимо; редагована підказка — лише якщо bash її вміє (4+).
  if [ "${BASH_VERSINFO[0]:-0}" -ge 4 ] && [ -t 0 ]; then
    read -e -i "$prefill" -r -p "IP телефону: " answer
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
  # Завжди показати, яку адресу взято (2026-10-18).
  [ -n "$TARGET_IP" ] && echo "Адреса: $TARGET_IP" >&2
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
38. Пункт 33 — Renault Docs: скільки часу йде на відкриття
    томів (з карти і з телефона). Спершу відкрий том і 2–3
    документи, потім цей пункт. НІЧОГО НЕ ЗМІНЮЄ.
39. Пункт 34 — чому застосунок закрився сам: журнал падінь
    Android і причина закриття. Запускати одразу після
    закриття (до перезавантаження). НІЧОГО НЕ ЗМІНЮЄ.

ЗАСТОСУНКИ, ЯКИХ БІЛЬШЕ НЕМА В МАГАЗИНІ
34. Пункт 26 — зберегти застосунок: з ЦЬОГО телефона
    (Enter) або з підключеного пунктом 4 (вибір 2).
    УСІ частини APK (base + split_*) у
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
    # 2026-10-20 (чат Track Catcher, T85): чи увімкнені ефекти звуку Samsung (Dolby Atmos,
    # SoundAlive, Adapt Sound, UHQ) — вони обробляють звук в audioserver і вантажать M30s.
    # Лише читає; вимикати — у Налаштуваннях телефона.
    echo
    echo "=== sound effects settings (system/global/secure) ==="
    for ns in system global secure; do
      adb -s "$ADB_TARGET" shell settings list "$ns" 2>/dev/null \
        | grep -i -E "dolby|atmos|soundalive|sound_alive|adapt_sound|uhq|upscal|equalizer|surround|sound_effect|tube_amp|concert_hall" \
        | sed "s/^/$ns: /"
    done
    echo
    echo "=== effect packages ==="
    adb -s "$ADB_TARGET" shell pm list packages 2>/dev/null | grep -i -E "dolby|soundalive|audio.*effect|sec.android.app.soundalive" || true
    echo
    echo "=== audio_flinger effects (media.audio_flinger) ==="
    adb -s "$ADB_TARGET" shell dumpsys media.audio_flinger 2>/dev/null \
      | grep -i -E "effect|session|dolby|soundalive|name:" | head -n 80 || true
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
    echo
    # 2026-10-20 (faric: «що можна відключити, щоб полегшити життя старенькому Samsung»):
    # повний список вбудованих програм, які зараз увімкнені, — з нього складаємо
    # список «вимкнути» для Samsung (пункти 21/22 вимикають, не видаляють).
    echo "=== SYSTEM PACKAGES ENABLED (pm list packages -s -e) ==="
    adb -s "$ADB_TARGET" shell pm list packages -s -e 2>/dev/null | sed 's/^package://' | sort || true
    echo
    echo "=== USER APPS ENABLED (pm list packages -3 -e) ==="
    adb -s "$ADB_TARGET" shell pm list packages -3 -e 2>/dev/null | sed 's/^package://' | sort || true
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
    com.ss.android.ugc.trill) echo "TikTok (азійська версія)" ;;
    com.zhiliaoapp.musically.go) echo "TikTok Lite" ;;
    com.estrongs.android.pop) echo "ES Провідник" ;;
    com.moonshot.kimichat) echo "Kimi" ;;
    com.android.vending) echo "Play Маркет (у фоні; оновлення — вручну)" ;;
    com.google.android.googlequicksearchbox) echo "Google (пошук і стрічка)" ;;
    com.samsung.android.dqagent) echo "Samsung: збір даних про якість (службова)" ;;
    com.sec.android.diagmonagent) echo "Samsung: діагностика (службова)" ;;
    com.samsung.android.dynamiclock) echo "Samsung: динамічні шпалери екрана блокування" ;;
    com.samsung.android.game.gos) echo "Samsung: Game Optimizing Service" ;;
    com.samsung.android.app.watchmanager2) echo "Galaxy Wearable (годинник)" ;;
    com.samsung.android.app.sharelive) echo "Quick Share" ;;
    com.samsung.android.scloud) echo "Samsung Cloud" ;;
    com.sec.android.daemonapp) echo "Погода Samsung" ;;
    com.samsung.android.bio.face.service) echo "Розпізнавання обличчя" ;;
    com.samsung.android.smartface) echo "Розумне очікування (камера дивиться на тебе)" ;;
    com.google.android.projection.gearhead) echo "Android Auto" ;;
    com.samsung.android.mdx.kit) echo "Зв'язок з Windows" ;;
    com.microsoft.skydrive) echo "OneDrive" ;;
    com.netflix.partner.activation) echo "Netflix (службова)" ;;
    com.android.chrome) echo "Chrome" ;;
    com.google.android.gm) echo "Gmail" ;;
    com.google.android.youtube) echo "YouTube" ;;
    com.openai.chatgpt) echo "ChatGPT" ;;
    ai.qwenlm.chat.android) echo "Qwen" ;;
    kling.ai.video.chat) echo "Kling AI" ;;
    com.brave.browser) echo "Brave" ;;
    com.anthropic.claude) echo "Claude" ;;
    ua.faric.doramaland.video.dev) echo "Dorama (dev)" ;;
    ua.faric.doramaland.video.debug) echo "Dorama (debug)" ;;
    com.saney.renaultdocs) echo "Renault Docs" ;;
    com.sec.android.app.myfiles) echo "Мої файли" ;;
    com.android.settings) echo "Налаштування" ;;
    com.sec.android.gallery3d) echo "Галерея" ;;
    com.samsung.android.video) echo "Samsung Відео" ;;
    com.suno.android) echo "Suno" ;;
    com.maxmpz.audioplayer) echo "Poweramp" ;;
    com.aimp.player) echo "AIMP" ;;
    com.aimp.cutter) echo "AIMP Cutter" ;;
    com.voe.superbill) echo "voe.superbill" ;;
    *) echo "$1" ;;
  esac
}

# Список за виробником підключеного телефона: Samsung — свій, решта — бабусин Redmi.
target_is_samsung() {
  adb -s "$ADB_TARGET" shell getprop ro.product.manufacturer 2>/dev/null | tr -d '\r' | grep -qi samsung
}

bloat_list() {
  if target_is_samsung; then printf '%s' "$SAMSUNG_BLOAT_PACKAGES"; else printf '%s' "$BLOAT_PACKAGES"; fi
}

sleep_list() {
  if target_is_samsung; then printf '%s' "$SAMSUNG_SLEEP_PACKAGES"; else printf '%s' "$SLEEP_PACKAGES"; fi
}

set_bloat_enabled() {
  local mode="$1"
  local item="$2"
  local title="$3"
  clear
  if ! require_connection; then pause_menu; return; fi

  local pkg answer disabled_list ok=0 skipped=0 failed=0 list
  list="$(bloat_list)"
  echo "$title"
  echo "Телефон: $(adb -s "$ADB_TARGET" shell getprop ro.product.model)"
  echo
  for pkg in $list; do
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
  for pkg in $list; do
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

  local pkg answer op bucket ok=0 skipped=0 failed=0 list
  list="$(sleep_list)"
  echo "$title"
  echo "Телефон: $(adb -s "$ADB_TARGET" shell getprop ro.product.model)"
  echo
  for pkg in $list; do
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
  for pkg in $list; do
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

  local pattern list pkg answer n i version dir paths path saved=0 here
  here="$(getprop ro.product.model 2>/dev/null)"
  echo "Звідки взяти застосунок?"
  echo "  1) Цей телефон — ${here:-цей}   (Enter)"
  echo "  2) Підключений по Wi-Fi (M30s чи інший; спершу пункт 4)"
  printf "Номер (Enter = 1): "
  read -r answer
  case "${answer:-1}" in
    1) SRC="local" ;;
    2) SRC="adb"
       if ! require_connection; then pause_menu; return; fi ;;
    *) echo "Скасовано."; pause_menu; return ;;
  esac

  echo
  echo "Зберегти застосунок з телефона: $(src_model)"
  echo
  printf "Що шукати в назві пакета (Enter = bms|lith): "
  read -r pattern
  pattern="${pattern:-bms|lith}"

  local all total
  all="$(src_run "pm list packages -3" | tr -d '\r' | sed -n 's/^package://p' | sort)"
  list="$(printf '%s\n' "$all" | grep -i -E "$pattern")"
  if [ -z "$list" ]; then
    total="$(printf '%s\n' "$all" | grep -c .)"
    echo "Нічого не знайдено за «$pattern»."
    if [ "$total" -eq 0 ]; then
      if [ "$SRC" = "local" ]; then
        echo "Android не дав Termux список застосунків."
        echo "Тоді з'єднай цей телефон сам із собою (пункти 3 і 4) і вибери 2."
      fi
      pause_menu
      return
    fi
    # 2026-10-16: назва пакета буває зовсім не схожа на назву застосунку
    # (Aolithium за «aolit» не знайшовся) — показати весь список.
    echo "Назва пакета буває не схожа на назву застосунку."
    printf "Показати всі встановлені застосунки (%s)? Enter = так, 0 = ні: " "$total"
    read -r answer
    case "$answer" in 0) echo "Скасовано."; pause_menu; return ;; esac
    list="$all"
  fi
  echo
  i=0
  while IFS= read -r pkg; do
    i=$((i + 1))
    if [ "$(printf '%s\n' "$list" | grep -c .)" -le 15 ]; then
      version="$(src_version "$pkg")"
      echo "  $i) $pkg  (версія ${version:-?})"
    else
      echo "  $i) $pkg"
    fi
  done <<EOF2
$list
EOF2
  echo
  printf "Номер застосунку (0 = скасувати): "
  read -r n
  case "$n" in ''|*[!0-9]*|0) echo "Скасовано."; pause_menu; return ;; esac
  pkg="$(printf '%s\n' "$list" | sed -n "${n}p")"
  if [ -z "$pkg" ]; then echo "Нема такого номера."; pause_menu; return; fi

  version="$(src_version "$pkg")"
  dir="$APK_DIR/${pkg}_v${version:-unknown}"
  mkdir -p "$dir"
  new_log "26" "save-apk" "Зберегти застосунок $pkg"

  paths="$(src_run "pm path $pkg" | tr -d '\r' | sed 's/^package://')"
  {
    echo "Пакет: $pkg"
    echo "Версія: ${version:-?}"
    echo "Телефон: $(src_model)"
    echo "Android: $(src_run 'getprop ro.build.version.release' | tr -d '\r')"
    echo "Частини:"
    printf '%s\n' "$paths"
  } | tee -a "$CURRENT_LOG" > "$dir/info.txt"

  while IFS= read -r path; do
    [ -n "$path" ] || continue
    if [ "$SRC" = "local" ]; then
      cp "$path" "$dir/$(basename "$path")" >> "$CURRENT_LOG" 2>&1
    else
      adb -s "$ADB_TARGET" pull "$path" "$dir/$(basename "$path")" >> "$CURRENT_LOG" 2>&1
    fi
    if [ $? -eq 0 ]; then
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
    if [ "$SRC" = "local" ]; then
      echo "Спробуй через Wi-Fi: з'єднай цей телефон сам із собою (пункти 3 і 4) і вибери 2." | tee -a "$CURRENT_LOG"
    fi
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

# 2026-10-16 (faric): «за замовчуванням — цей телефон (A26), а якщо що — можна
# вибрати M30s». Aolithium стоїть і на A26, де працює саме меню, тож для нього
# не треба з'єднувати A26 «сам із собою» по Wi-Fi: Termux бачить список
# застосунків (pm) і може прочитати їхні APK напряму.
# SRC = local (цей телефон) або adb (підключений пунктом 4).
src_run() {
  if [ "$SRC" = "local" ]; then
    sh -c "$1" 2>/dev/null
  else
    adb -s "$ADB_TARGET" shell "$1" 2>/dev/null
  fi
}

src_model() {
  if [ "$SRC" = "local" ]; then
    getprop ro.product.model 2>/dev/null
  else
    adb -s "$ADB_TARGET" shell getprop ro.product.model 2>/dev/null | tr -d '\r'
  fi
}

# Версія: dumpsys (лише через ADB); на цьому телефоні — номер збірки з pm.
src_version() {
  local v code
  v="$(src_run "dumpsys package $1" | tr -d '\r' | sed -n 's/^ *versionName=//p' | head -n 1)"
  if [ -z "$v" ]; then
    code="$(src_run "pm list packages --show-versioncode $1" | tr -d '\r' \
      | grep "^package:$1 " | sed -n 's/.*versionCode://p' | head -n 1)"
    [ -n "$code" ] && v="build$code"
  fi
  printf '%s' "$v"
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

pick_faric_app() {
  # 2026-10-16 (faric, лог 096): друкувати назву пакета на телефоні незручно —
  # Enter залишав Audio Converter, хоча треба було Track Catcher. Тепер номер.
  local choice
  echo "Який застосунок?" >&2
  echo "  1 — Audio Converter" >&2
  echo "  2 — Track Catcher" >&2
  echo "  або напиши назву пакета (напр. com.saney.renaultdocs)" >&2
  printf "Вибір (Enter = 1): " >&2
  read -r choice
  case "$choice" in
    ""|1) echo "ua.faric.audioconverter" ;;
    2) echo "ua.faric.trackcatcher" ;;
    *) echo "$choice" ;;
  esac
}

app_threads_report() {
  # 2026-10-16 (faric, M30s): Audio Converter кодує MP3 ~4 с на 1 % смуги
  # (≈ пісня за реальний час), хоча процес бере 2+ ядра. Після перезавантаження
  # (вільно 250 МБ замість 80) — так само. Цей пункт лише читає: які потоки
  # застосунку крутяться, з яким пріоритетом, у якій групі ядер (великі / малі).
  clear
  if ! require_connection; then pause_menu; return; fi
  local pkg
  pkg="$(pick_faric_app)"
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

renault_read_timing() {
  # 2026-10-16 (Renault Docs, TODO №16, етап 0 плану «читати том з одного .rdpkg»):
  # томи на карті M30s відкриваються дуже довго, у пам'яті телефона — швидко.
  # Renault Docs з v0.5.137 пише в logcat (тег RenaultRead), скільки переліків папок
  # і відкриттів файлів тому йде і скільки мілісекунд, окремо для карти й телефона.
  # Пункт лише читає журнал. Нічого не змінює.
  clear
  if ! require_connection; then pause_menu; return; fi
  new_log "33" "renault-read" "Renault Docs: заміри читання томів"
  echo "Спершу в Renault Docs відкрий том з карти і 2–3 документи (і для порівняння — том з телефона)."
  echo "Потім цей пункт. Нічого не змінюю."
  echo
  {
    echo "=== Renault Docs ==="
    adb -s "$ADB_TARGET" shell dumpsys package com.saney.renaultdocs 2>/dev/null \
      | grep -m 1 "versionName" || echo "versionName: ?"
    echo
    echo "=== Заміри (logcat, тег RenaultRead) ==="
    echo "LIST — перелік папки; OPEN — повільне відкриття файлу; SUM — підсумок тому; FAST — перше відкриття тому;"
    echo "PDF відкрито — відкриття PDF; СТОРІНКА — малювання і стиснення сторінки (з v0.5.139)."
    echo "ARCHIVE — том читається з архіву (.rdpkg); ARCHIVE-PDF — PDF скопійовано з архіву в кеш (з v0.5.141)."
    adb -s "$ADB_TARGET" logcat -d -v time -s RenaultRead:I 2>&1 | tail -n 400
  } 2>&1 | tee -a "$CURRENT_LOG"
  echo
  echo "Готово. Надішли цей файл у чат (шлях — нижче)."
  pause_menu
}
# renault_read_timing end

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
  pkg="$(pick_faric_app)"
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

app_crash_report() {
  # 2026-10-16 12:29 (faric, M30s, Audio Converter 0.6.0): після «Стоп» застосунок
  # закривається сам, хоча в списку недавніх лишається. У коді немає finish() —
  # схоже на падіння. Журнал падінь Android (logcat -b crash) живе до перезавантаження.
  # Лише читає.
  clear
  if ! require_connection; then pause_menu; return; fi
  local pkg pid
  pkg="$(pick_faric_app)"
  new_log "34" "app-crash" "Чому закрився застосунок: $pkg"
  echo "Збираю журнал падінь. Нічого не змінюю."
  echo
  pid="$(adb -s "$ADB_TARGET" shell pidof "$pkg" 2>/dev/null | tr -d '\r' | awk '{print $1}')"
  {
    echo "Пакет: $pkg"
    echo "Зараз запущений: ${pid:-ні}"
    echo "Версія: $(adb -s "$ADB_TARGET" shell dumpsys package "$pkg" 2>/dev/null | tr -d '\r' | sed -n 's/^ *versionName=//p' | head -n 1)"
    echo
    echo "=== Журнал падінь (crash) — останнє падіння цього застосунку, повністю ==="
    # 2026-10-16 (лог 102): «grep -A 40 | tail» обрізав верх стеку — саме те місце,
    # де видно, що викликало падіння. Тепер — від заголовка останнього падіння.
    local crash_text line_no
    crash_text="$(adb -s "$ADB_TARGET" logcat -d -b crash -v time 2>&1)"
    line_no="$(printf '%s\n' "$crash_text" | grep -n -E ">>> $pkg <<<|Process: $pkg" | tail -n 1 | cut -d: -f1)"
    if [ -n "$line_no" ]; then
      # Java-падіння: «FATAL EXCEPTION» за 2 рядки вище; нативне: «*** *** ***» — за ~6.
      printf '%s\n' "$crash_text" | sed -n "$(( line_no > 8 ? line_no - 8 : 1 )),$(( line_no + 130 ))p"
      echo
      echo "Падінь цього застосунку в журналі: $(printf '%s\n' "$crash_text" | grep -c -E ">>> $pkg <<<|Process: $pkg")"
    else
      echo "(падінь цього застосунку в журналі немає)"
    fi
    echo
    echo "=== Помилки Android про цей застосунок (ANR, «зупинено», вбито) ==="
    adb -s "$ADB_TARGET" logcat -d -b main,system -v time -t 20000 2>&1 \
      | grep -E "$pkg" \
      | grep -i -E "FATAL|ANR|crash|died|Force stopping|Killing|kill|has died|Exception|finish|lowmemory|onTrimMemory" \
      | tail -n 80
    echo
    echo "=== Останні записи самого застосунку ==="
    if [ -n "$pid" ]; then
      adb -s "$ADB_TARGET" logcat -d -v time --pid="$pid" -t 3000 2>&1 | tail -n 60
    else
      echo "(застосунок зараз не запущений — дивись блоки вище)"
    fi
    echo
    echo "=== Закриття з причиною (Android 11+) ==="
    adb -s "$ADB_TARGET" shell dumpsys activity exit-info "$pkg" 2>/dev/null | head -n 60
  } 2>&1 | tee -a "$CURRENT_LOG"
  echo
  echo "Готово. Надішли цей файл у чат (шлях — нижче)."
  pause_menu
}
# app_crash_report end

# junk_cleanup start
# 35 — «Прибрати сміття в проєктах» (faric 2026-10-18 15:36: «пройшов по всім
# репозиторіям і сказав, що от такий об'єм файлів нам не потрібний»).
# Лише ЦЕЙ телефон (у чужий Termux ADB не заглядає). Репо шукає сам — працює на
# будь-якому телефоні, де стоїть це меню.
# Спершу ПОКАЗУЄ групи з розмірами; видаляє тільки вибрані групи й лише після «так».
# Не чіпає ніколи: код і історію репо (.git), ключі підпису й токени, Android SDK,
# Gradle і його кеш поточної версії, тома/записи/музику в Documents, застосунки,
# збережені пунктом 26, файли в Download (лише показує).
# Корені можна підмінити (JC_HOME, JC_DOCS, JC_PREFIX) — так це меню перевіряють тести.

jc_kb() {
  [ "$#" -gt 0 ] || { echo 0; return; }
  du -sk -- "$@" 2>/dev/null | awk '{s += $1} END {print s + 0}'
}

jc_human() {
  awk -v k="${1:-0}" 'BEGIN {
    if (k >= 1048576) printf "%.1f ГБ", k / 1048576;
    else if (k >= 1024) printf "%d МБ", k / 1024;
    else printf "%d КБ", k }'
}

# Короткий шлях для екрана: ~/… і Documents/…
jc_short() {
  local p="$1"
  case "$p" in
    "$JC_HOME"/*) p="~/${p#"$JC_HOME"/}" ;;
    "$JC_DOCS"/*) p="Documents/${p#"$JC_DOCS"/}" ;;
    "$JC_PREFIX"/*) p="\$PREFIX/${p#"$JC_PREFIX"/}" ;;
  esac
  printf '%s' "$p"
}

# Остання перевірка перед rm: лише всередині дозволених коренів і ніколи —
# ключі, токени, історія git.
jc_safe_path() {
  local p="$1"
  case "$p" in
    ""|/|"$JC_HOME"|"$JC_DOCS"|"$JC_PREFIX") return 1 ;;
    *"/../"*|*"/.."|*"/.android"|*"/.android/"*|*"/.git"|*"/.git/"*) return 1 ;;
    *"-secrets"*|*"/.config"|*"/.config/"*|*"/.ssh"|*"/.ssh/"*) return 1 ;;
    *"/PhoneDiagnostics/apk/"*) return 1 ;;
    "$JC_HOME"/?*|"$JC_DOCS"/?*|"$JC_PREFIX"/tmp/?*|"$JC_PREFIX"/var/cache/apt/?*) return 0 ;;
  esac
  return 1
}

# Збірка зараз іде? (клієнт Gradle живе лише під час збірки; фоновий демон — ні)
jc_build_running() {
  ps -ef 2>/dev/null | grep -E 'org\.gradle\.launcher\.GradleMain|org\.gradle\.wrapper\.GradleWrapperMain' | grep -v grep >/dev/null
}

# Репо на цьому телефоні: у домашній папці Termux (і на рівень глибше) та в Documents.
jc_repos() {
  local d
  for d in "$JC_HOME"/*/ "$JC_HOME"/*/*/ "$JC_DOCS"/*/; do
    [ -d "${d}.git" ] && printf '%s\n' "${d%/}"
  done
}

# Група 1: результати збірки — папка build/ поруч із build.gradle(.kts) і .gradle/
# поруч із settings.gradle(.kts). Лише якщо в git там нічого не записано.
jc_list_build() {
  local repo f dir rel tracked sub
  while IFS= read -r repo; do
    while IFS= read -r -d '' f; do
      dir="$(dirname -- "$f")"
      for sub in build .gradle; do
        [ -d "$dir/$sub" ] || continue
        case "$sub:$(basename -- "$f")" in
          build:build.gradle*|.gradle:settings.gradle*) ;;
          *) continue ;;
        esac
        rel="${dir#"$repo"}"; rel="${rel#/}"
        # git не відповів — не ризикуємо, пропускаємо.
        tracked="$(git -c safe.directory='*' -C "$repo" ls-files -- "${rel:+$rel/}$sub" 2>/dev/null)" || continue
        [ -z "$tracked" ] && printf '%s\n' "$dir/$sub"
      done
    done < <(find "$repo" -maxdepth 4 \( -name .git -o -name build -o -name .gradle -o -name node_modules \) -prune \
              -o -type f \( -name build.gradle -o -name build.gradle.kts -o -name settings.gradle -o -name settings.gradle.kts \) -print0 2>/dev/null)
  done < <(jc_repos)
}

# Група 2: старі APK у Documents (2 найновіші кожного застосунку лишаються).
# Папки: <застосунок>/apk, <застосунок>/artifacts/apk, <застосунок>/packages
# (у packages — лише *-v<версія>*.apk, щоб не зачепити тома Renault).
JC_KEEP_APK=2
jc_list_old_apk() {
  local app name
  for app in "$JC_DOCS"/*/; do
    app="${app%/}"
    name="$(basename -- "$app")"
    [ "$name" = "PhoneDiagnostics" ] && continue
    {
      [ -d "$app/apk" ] && find "$app/apk" -type f -name '*.apk' -printf '%T@\t%p\n' 2>/dev/null
      [ -d "$app/artifacts/apk" ] && find "$app/artifacts/apk" -type f -name '*.apk' -printf '%T@\t%p\n' 2>/dev/null
      [ -d "$app/packages" ] && find "$app/packages" -type f -name '*-v[0-9]*.apk' -printf '%T@\t%p\n' 2>/dev/null
    } | sort -rn | awk -F '\t' -v keep="$JC_KEEP_APK" 'NR > keep {print $2}'
  done
}

# Група 3: скачане й тимчасове в Termux.
jc_list_temp() {
  local p p_ver
  for p in "$JC_HOME/.cache/track-catcher" "$JC_HOME/.cache/audio-converter" \
           "$JC_HOME/renault-apk-artifact" "$JC_HOME/renault-release-apk"; do
    [ -e "$p" ] && printf '%s\n' "$p"
  done
  # Інсталятори, які вже розпаковано.
  [ -f "$JC_HOME/commandlinetools-linux.zip" ] && [ -d "$JC_HOME/android-sdk/cmdline-tools" ] &&
    printf '%s\n' "$JC_HOME/commandlinetools-linux.zip"
  for p in "$JC_HOME"/gradle-*-bin.zip; do
    [ -f "$p" ] || continue
    p_ver="$(basename -- "$p" -bin.zip)"
    [ -d "$JC_HOME/gradle/$p_ver" ] && printf '%s\n' "$p"
  done
  # Тимчасові файли Termux старші за добу (свіже може бути потрібне зараз).
  if [ -d "$JC_PREFIX/tmp" ]; then
    while IFS= read -r -d '' p; do
      [ -S "$p" ] && continue
      [ -n "$(find "$p" -mmin -1440 -print -quit 2>/dev/null)" ] && continue
      printf '%s\n' "$p"
    done < <(find "$JC_PREFIX/tmp" -mindepth 1 -maxdepth 1 -print0 2>/dev/null)
  fi
  # Старі журнали фонового Gradle і скачані пакети pkg.
  find "$JC_HOME/.gradle/daemon" -type f -name '*.log' -mmin +1440 2>/dev/null
  find "$JC_PREFIX/var/cache/apt/archives" -maxdepth 1 -type f -name '*.deb' 2>/dev/null
}

# Версії Gradle, якими збирають на цьому телефоні.
jc_gradle_versions() {
  local d
  for d in "$JC_HOME"/gradle/gradle-*/; do
    [ -d "$d" ] && basename -- "$d" | sed 's/^gradle-//'
  done
  for d in "$JC_HOME"/.gradle/wrapper/dists/gradle-*/; do
    [ -d "$d" ] && basename -- "$d" | sed 's/^gradle-//; s/-\(bin\|all\)$//'
  done
  while IFS= read -r d; do
    grep -h 'distributionUrl' "$d/gradle/wrapper/gradle-wrapper.properties" "$d"/*/gradle/wrapper/gradle-wrapper.properties 2>/dev/null |
      sed -n 's/.*gradle-\([0-9][0-9.]*\)-\(bin\|all\)\.zip.*/\1/p'
  done < <(jc_repos)
}

# Група 4: кеш старих версій Gradle (~/.gradle/caches/<версія>, ~/.gradle/daemon/<версія>).
# Якщо жодної своєї версії не знайдено — не пропонуємо нічого (не вгадуємо).
jc_list_old_gradle() {
  local used d v
  used="$(jc_gradle_versions | sort -u)"
  [ -n "$used" ] || return 0
  for d in "$JC_HOME"/.gradle/caches/*/ "$JC_HOME"/.gradle/daemon/*/; do
    [ -d "$d" ] || continue
    v="$(basename -- "$d")"
    printf '%s' "$v" | grep -Eq '^[0-9]+\.[0-9]+(\.[0-9]+)?$' || continue
    printf '%s\n' "$used" | grep -Fxq "$v" && continue
    printf '%s\n' "${d%/}"
  done
}

JC_TITLES=(
  ""
  "Результати збірки в проєктах (build/, .gradle/) — перебудуються самі, наступна збірка довша на кілька хвилин"
  "Старі APK у Documents — лишаю по $JC_KEEP_APK найновіші кожного застосунку"
  "Скачане й тимчасове в Termux (завантажені APK, старі тимчасові файли, пакети pkg)"
  "Кеш старих версій Gradle — зараз ними не збирають"
)

jc_list_group() {
  case "$1" in
    1) jc_list_build ;;
    2) jc_list_old_apk ;;
    3) jc_list_temp ;;
    4) jc_list_old_gradle ;;
  esac | sort -u
}

junk_cleanup() {
  clear
  JC_HOME="${JC_HOME:-$HOME}"
  JC_DOCS="${JC_DOCS:-/storage/emulated/0/Documents}"
  JC_PREFIX="${JC_PREFIX:-${PREFIX:-/data/data/com.termux/files/usr}}"
  new_log "35" "junk-cleanup" "Прибрати сміття в проєктах"

  local g n k total=0 answer chosen="" p sel_k=0 deleted=0 failed=0 shown
  local -a lists sizes counts

  if jc_build_running; then
    echo "Зараз іде збірка APK. Дочекайся кінця й відкрий цей пункт знову." | tee -a "$CURRENT_LOG"
    echo "Нічого не видалено." | tee -a "$CURRENT_LOG"
    pause_menu
    return
  fi

  {
    echo "Прибрати сміття в проєктах — цей телефон: $(getprop ro.product.model 2>/dev/null)"
    echo "Вільно зараз: $(df -h "$JC_HOME" 2>/dev/null | awk 'NR==2 {print $4}')"
    echo
    echo "Знайдені проєкти (репо):"
    jc_repos | while IFS= read -r p; do echo "  $(jc_short "$p")"; done
    echo
    echo "Рахую, що можна прибрати (1–2 хвилини)…"
  } | tee -a "$CURRENT_LOG"

  for g in 1 2 3 4; do
    lists[$g]="$(jc_list_group "$g")"
    if [ -n "${lists[$g]}" ]; then
      mapfile -t _jc_paths <<<"${lists[$g]}"
      counts[$g]="${#_jc_paths[@]}"
      sizes[$g]="$(jc_kb "${_jc_paths[@]}")"
    else
      counts[$g]=0
      sizes[$g]=0
    fi
    total=$((total + sizes[g]))
  done

  {
    echo
    echo "Що можна прибрати:"
    for g in 1 2 3 4; do
      printf '  %s) %-8s %s\n' "$g" "$(jc_human "${sizes[$g]}")" "${JC_TITLES[$g]}"
      [ "${counts[$g]}" -gt 0 ] || echo "              (нічого)"
    done
    echo "  Разом: $(jc_human "$total")"
    echo
    echo "Не чіпаю (лише для відома):"
    k=0
    while IFS= read -r p; do k=$((k + $(jc_kb "$p/.git"))); done < <(jc_repos)
    printf '  %-8s %s\n' "$(jc_human "$k")" "історія git у проєктах"
    [ -d "$JC_HOME/.gradle/caches" ] && printf '  %-8s %s\n' "$(jc_human "$(jc_kb "$JC_HOME/.gradle/caches")")" "кеш Gradle (з ним збірка швидка; старі версії — у групі 4)"
    [ -d "$JC_HOME/android-sdk" ] && printf '  %-8s %s\n' "$(jc_human "$(jc_kb "$JC_HOME/android-sdk")")" "Android SDK (потрібен для збірки)"
    [ -d "$JC_DOCS/PhoneDiagnostics/apk" ] && printf '  %-8s %s\n' "$(jc_human "$(jc_kb "$JC_DOCS/PhoneDiagnostics/apk")")" "застосунки, збережені пунктом 26"
    n="$(find "$JC_DOCS/../Download" -maxdepth 2 -type f \( -name '*.apk' -o -name '*.apks' \) 2>/dev/null | wc -l)"
    if [ "${n:-0}" -gt 0 ]; then
      mapfile -t _jc_paths < <(find "$JC_DOCS/../Download" -maxdepth 2 -type f \( -name '*.apk' -o -name '*.apks' \) 2>/dev/null)
      printf '  %-8s %s\n' "$(jc_human "$(jc_kb "${_jc_paths[@]}")")" "APK у Download: $n шт. — переглянь сам у «Моїх файлах»"
    fi
    echo "  ключі підпису, токени, тома Renault, записи й музика — ніколи"
    echo
  } | tee -a "$CURRENT_LOG"

  if [ "$total" -eq 0 ]; then
    echo "Прибирати нічого. Нічого не видалено." | tee -a "$CURRENT_LOG"
    pause_menu
    return
  fi

  printf "Які групи видалити? Номери через пробіл (напр. 1 3), «все» — усі, Enter — нічого: "
  read -r answer
  case "$answer" in
    все|всі|Все|Всі) answer="1 2 3 4" ;;
  esac
  for n in $answer; do
    case "$n" in
      1|2|3|4) [ "${counts[$n]}" -gt 0 ] && case " $chosen " in *" $n "*) ;; *) chosen="$chosen $n" ;; esac ;;
      *) echo "Нема групи «$n» — пропускаю." ;;
    esac
  done
  chosen="${chosen# }"
  if [ -z "$chosen" ]; then
    echo "Скасовано. Нічого не видалено." | tee -a "$CURRENT_LOG"
    pause_menu
    return
  fi

  {
    echo
    echo "Буде видалено (групи: $chosen):"
  } | tee -a "$CURRENT_LOG"
  shown=0
  for g in $chosen; do
    sel_k=$((sel_k + sizes[g]))
    while IFS= read -r p; do
      echo "  [$g] $(jc_short "$p")" >> "$CURRENT_LOG"
      if [ "$shown" -lt 15 ]; then echo "  [$g] $(jc_short "$p")"; fi
      shown=$((shown + 1))
    done <<<"${lists[$g]}"
  done
  [ "$shown" -gt 15 ] && echo "  … і ще $((shown - 15)) (повний список — у лозі)"
  echo "Звільниться приблизно: $(jc_human "$sel_k")" | tee -a "$CURRENT_LOG"
  echo
  printf "Видалити? Введи «так» і Enter (будь-що інше — скасувати): "
  read -r answer
  if [ "$answer" != "так" ]; then
    echo "Скасовано. Нічого не видалено." | tee -a "$CURRENT_LOG"
    pause_menu
    return
  fi

  echo | tee -a "$CURRENT_LOG"
  for g in $chosen; do
    while IFS= read -r p; do
      [ -n "$p" ] || continue
      if ! jc_safe_path "$p"; then
        echo "  ПРОПУЩЕНО (захищене місце): $(jc_short "$p")" | tee -a "$CURRENT_LOG"
        continue
      fi
      if rm -rf -- "$p" 2>/dev/null && [ ! -e "$p" ]; then
        deleted=$((deleted + 1))
        echo "  видалено: $(jc_short "$p")" >> "$CURRENT_LOG"
        # Порожня папка версії APK після видалення — теж геть (лише порожня).
        [ "$g" = "2" ] && rmdir -- "$(dirname -- "$p")" 2>/dev/null
      else
        failed=$((failed + 1))
        echo "  НЕ ВИЙШЛО: $(jc_short "$p")" | tee -a "$CURRENT_LOG"
      fi
    done <<<"${lists[$g]}"
  done

  {
    echo "ГОТОВО: видалено $deleted, не вийшло $failed."
    echo "Вільно тепер: $(df -h "$JC_HOME" 2>/dev/null | awk 'NR==2 {print $4}')"
  } | tee -a "$CURRENT_LOG"
  pause_menu
}
# junk_cleanup end

# record_mode start
# 36 / 37 — «Режим запису» (faric 2026-10-20 01:05: «перед записом все виключив, а після
# запису все назад включив … зайшов, тикнув — все відключилось; вранці тикнув — все
# запустилось, як і було»). Для T85 Track Catcher: на M30s ривки 10–20 мс від нестачі пам'яті.
# 36 запам'ятовує, як було, і лише тоді міняє; 37 повертає рівно те, що змінив 36.
# Нічого не видаляє: закриває (як змахнути з «Недавніх»), приспає, тимчасово вимикає
# службове (pm disable-user), вмикає «Не турбувати». Дані застосунків не чіпає.
# Програми запису й плеєр, з якого пишемо, не чіпає ніколи.

# Закрити й приспати на час запису (якщо встановлені).
REC_SLEEP_PACKAGES="com.google.android.gm com.zhiliaoapp.musically com.zhiliaoapp.musically.go com.ss.android.ugc.trill com.estrongs.android.pop com.moonshot.kimichat com.android.vending com.google.android.googlequicksearchbox com.android.chrome com.google.android.youtube com.openai.chatgpt ai.qwenlm.chat.android kling.ai.video.chat com.brave.browser"
# Лише закрити (не присипляти): потрібні вдень, але не під час запису.
REC_STOP_PACKAGES="com.anthropic.claude ua.faric.doramaland.video.dev ua.faric.doramaland.video.debug com.saney.renaultdocs com.sec.android.app.myfiles com.android.settings com.sec.android.gallery3d com.samsung.android.video"
# Тимчасово вимкнути (повертає 37): годинник, Quick Share, Samsung Cloud, погода,
# обличчя, Android Auto, Windows-зв'язок, OneDrive і службові з пункту 21.
REC_DISABLE_PACKAGES="com.samsung.android.app.watchmanager2 com.samsung.android.app.sharelive com.samsung.android.scloud com.sec.android.daemonapp com.samsung.android.bio.face.service com.samsung.android.smartface com.google.android.projection.gearhead com.samsung.android.mdx.kit com.microsoft.skydrive com.netflix.partner.activation com.facebook.appmanager com.facebook.services com.facebook.system com.samsung.android.dqagent com.sec.android.diagmonagent com.samsung.android.dynamiclock com.samsung.android.game.gos"
# Плеєри: закривається той, з якого НЕ пишемо.
REC_PLAYERS="com.suno.android com.maxmpz.audioplayer com.aimp.player com.aimp.cutter"
# Ніколи не чіпати.
REC_KEEP="ua.faric.trackcatcher com.termux com.termux.widget"

# adb без stdin: інакше `adb shell` читає те, що йде на вхід (файл стану чи клавіатуру).
rec_adb() {
  adb -s "$ADB_TARGET" "$@" </dev/null
}

rec_state_file() {
  local serial
  serial="$(rec_adb shell getprop ro.serialno 2>/dev/null | tr -d '\r')"
  [ -n "$serial" ] || serial="$(printf '%s' "$ADB_TARGET" | tr -c 'A-Za-z0-9' '_')"
  printf '%s/record-mode-%s.state' "$STATE_DIR" "$serial"
}

rec_has() {
  rec_adb shell pm path "$1" >/dev/null 2>&1
}

rec_kept() {
  case " $REC_KEEP $REC_PLAYER_KEEP $REC_LOCAL_KEEP " in *" $1 "*) return 0 ;; esac
  return 1
}

rec_appops() {
  local v
  v="$(rec_adb shell cmd appops get "$1" RUN_ANY_IN_BACKGROUND 2>/dev/null | tr -d '\r')"
  case "$v" in
    *ignore*) echo ignore ;;
    *deny*) echo deny ;;
    *allow*) echo allow ;;
    *) echo default ;;
  esac
}

rec_bucket_name() {
  case "$1" in
    10) echo active ;; 20) echo working_set ;; 30) echo frequent ;;
    40|50) echo rare ;; 45) echo restricted ;; *) echo "" ;;
  esac
}

# Журнал телефона (logcat): розмір кожного буфера, напр. «main 256K». Звичайно 256 КБ —
# це ~1,5 години; 20.10 пункт 38 через це не побачив ночі (faric 14:58: «збільшити журнал»).
REC_LOG_BUFFERS="main system events crash"
REC_LOG_SIZE="16M"
rec_logbuf_sizes() {
  local b
  for b in $REC_LOG_BUFFERS; do
    rec_adb logcat -b "$b" -g 2>/dev/null | tr -d '\r' \
      | sed -n "s/^$b: ring buffer is \([0-9][0-9]*\) *\([KMG]\).*/$b \1\2/p" | head -n 1
  done
}

rec_zen_name() {
  case "$1" in 1) echo priority ;; 2) echo none ;; 3) echo alarms ;; *) echo off ;; esac
}

record_mode_on() {
  clear
  if ! require_connection; then pause_menu; return; fi
  local state answer pkg n=0 en_list was bucket zen here there
  state="$(rec_state_file)"
  if [ -f "$state" ]; then
    echo "Режим запису вже увімкнено ($(sed -n 's/^time //p' "$state"))."
    echo "Спершу поверни все пунктом 37 — інакше загубиться, як було."
    pause_menu; return
  fi

  echo "РЕЖИМ ЗАПИСУ — телефон: $(rec_adb shell getprop ro.product.model | tr -d '\r')"
  echo
  echo "З якого плеєра пишеш? Його не чіпаю, інші плеєри закрию."
  echo "  1) Suno   (Enter)"
  echo "  2) Poweramp"
  echo "  3) AIMP"
  echo "  4) Інший — плеєри не закривати"
  printf "Номер: "
  read -r answer
  case "${answer:-1}" in
    1) REC_PLAYER_KEEP="com.suno.android" ;;
    2) REC_PLAYER_KEEP="com.maxmpz.audioplayer com.maxmpz.equalizer" ;;
    3) REC_PLAYER_KEEP="com.aimp.player com.aimp.cutter" ;;
    4) REC_PLAYER_KEEP="$REC_PLAYERS" ;;
    *) echo "Скасовано. Нічого не змінено."; pause_menu; return ;;
  esac

  # Меню запущене на тому самому телефоні (A26 сам до себе) — Claude не закривати.
  here="$(getprop ro.product.model 2>/dev/null)"
  there="$(rec_adb shell getprop ro.product.model 2>/dev/null | tr -d '\r')"
  REC_LOCAL_KEEP=""
  [ -n "$here" ] && [ "$here" = "$there" ] && REC_LOCAL_KEEP="com.anthropic.claude"

  echo
  echo "Що зроблю (усе повертає пункт 37):"
  echo "  • закрию важкі застосунки й інші плеєри (Claude, Gmail, ES, TikTok, Play, Google, Chrome…)"
  echo "  • приспю їх до ранку, щоб самі не запускались"
  echo "  • тимчасово вимкну: годинник, Quick Share, Samsung Cloud, погоду, обличчя, Android Auto…"
  echo "  • «Не турбувати»: лише будильники — звуки сповіщень не потраплять у запис"
  echo "  • журнал телефона більший на ніч — пункт 38 потім побачить усю ніч"
  echo "Не чіпаю: Track Catcher, плеєр запису, Termux, дзвінки, месенджери."
  printf "Увімкнути режим запису? так / ні: "
  read -r answer
  case "$answer" in
    так|Так|ТАК|y|Y|yes|д|Д) ;;
    *) echo "Скасовано. Нічого не змінено."; pause_menu; return ;;
  esac

  new_log "36" "record-mode-on" "Режим запису: вимкнути зайве"
  {
    echo "target $ADB_TARGET"
    echo "model $there"
    echo "time $(date '+%Y-%m-%d %H:%M')"
  } > "$state"

  # 1) «Не турбувати» — запам'ятати, як було.
  zen="$(rec_adb shell settings get global zen_mode 2>/dev/null | tr -d '\r')"
  case "$zen" in 0|1|2|3) ;; *) zen=0 ;; esac
  echo "zen $zen" >> "$state"
  if rec_adb shell cmd notification set_dnd alarms >> "$CURRENT_LOG" 2>&1; then
    echo "ГОТОВО: «Не турбувати» — лише будильники (було: $(rec_zen_name "$zen"))" | tee -a "$CURRENT_LOG"
  else
    echo "НЕ ВИЙШЛО: «Не турбувати» — увімкни вручну зі шторки" | tee -a "$CURRENT_LOG"
  fi

  # 1б) Більший журнал телефона на ніч — щоб пункт 38 бачив усю ніч, а не останні ~1,5 год.
  local logbuf_before logbuf_after
  logbuf_before="$(rec_logbuf_sizes)"
  if [ -n "$logbuf_before" ]; then
    for pkg in $REC_LOG_BUFFERS; do
      rec_adb logcat -b "$pkg" -G "$REC_LOG_SIZE" >> "$CURRENT_LOG" 2>&1
    done
    logbuf_after="$(rec_logbuf_sizes)"
    if [ "$logbuf_after" != "$logbuf_before" ]; then
      printf '%s\n' "$logbuf_before" | sed 's/^/logbuf /' >> "$state"
      echo "ГОТОВО: журнал телефона більший на ніч (було: $(printf '%s' "$logbuf_before" | tr '\n' ' '))" | tee -a "$CURRENT_LOG"
      n=$((n + 1))
    else
      echo "НЕ ВИЙШЛО: телефон не дав збільшити журнал (пункт 38 побачить лише останні години)" | tee -a "$CURRENT_LOG"
    fi
  else
    echo "НЕ ВИЙШЛО: не прочитав розмір журналу телефона" | tee -a "$CURRENT_LOG"
  fi

  # 2) Тимчасово вимкнути службове — лише те, що зараз увімкнено.
  en_list="$(rec_adb shell pm list packages -e 2>/dev/null | tr -d '\r')"
  for pkg in $REC_DISABLE_PACKAGES; do
    rec_kept "$pkg" && continue
    printf '%s\n' "$en_list" | grep -qx "package:$pkg" || continue
    rec_adb shell pm disable-user --user 0 "$pkg" >> "$CURRENT_LOG" 2>&1
    # Перевіряємо, що справді вимкнено (Samsung деякі службові не дає — 20.10, GOS).
    if rec_adb shell pm list packages -d 2>/dev/null | tr -d '\r' | grep -qx "package:$pkg"; then
      echo "disabled $pkg" >> "$state"
      echo "вимкнено: $(package_label "$pkg")" | tee -a "$CURRENT_LOG"; n=$((n + 1))
    else
      echo "НЕ ВИЙШЛО (телефон не дав вимкнути): $(package_label "$pkg")" | tee -a "$CURRENT_LOG"
    fi
  done

  # 3) Закрити й приспати важкі; інші плеєри — лише закрити.
  for pkg in $REC_SLEEP_PACKAGES; do
    rec_kept "$pkg" && continue
    rec_has "$pkg" || continue
    was="$(rec_appops "$pkg")"
    bucket="$(rec_adb shell am get-standby-bucket "$pkg" 2>/dev/null | tr -d '\r')"
    echo "appops $pkg $was" >> "$state"
    echo "bucket $pkg $bucket" >> "$state"
    rec_adb shell am force-stop "$pkg" >> "$CURRENT_LOG" 2>&1
    rec_adb shell cmd appops set "$pkg" RUN_ANY_IN_BACKGROUND ignore >> "$CURRENT_LOG" 2>&1
    rec_adb shell am set-standby-bucket "$pkg" restricted >> "$CURRENT_LOG" 2>&1
    echo "закрито й приспано: $(package_label "$pkg")" | tee -a "$CURRENT_LOG"; n=$((n + 1))
  done
  for pkg in $REC_STOP_PACKAGES $REC_PLAYERS; do
    rec_kept "$pkg" && continue
    rec_has "$pkg" || continue
    if [ "$pkg" = "ua.faric.audioconverter" ]; then continue; fi
    rec_adb shell am force-stop "$pkg" >> "$CURRENT_LOG" 2>&1
    echo "закрито: $(package_label "$pkg")" | tee -a "$CURRENT_LOG"; n=$((n + 1))
  done

  # Audio Converter не закриваю: може саме конвертувати. Лише нагадую.
  if [ -n "$REC_LOCAL_KEEP" ]; then
    echo "УВАГА: меню працює на цьому ж телефоні — Claude не закриваю. Перед записом закрий його сам." | tee -a "$CURRENT_LOG"
  fi
  if rec_adb shell pidof ua.faric.audioconverter >/dev/null 2>&1; then
    echo "УВАГА: Audio Converter відкритий. Якщо не конвертує — закрий його сам (змахни з «Недавніх»)." | tee -a "$CURRENT_LOG"
  fi

  {
    echo
    echo "ГОТОВО: режим запису увімкнено, змін: $n."
    echo "Запиши: відкрий плеєр і Track Catcher."
    echo "Після запису: СПЕРШУ пункт 38 (що було за ніч), ПОТІМ 37 — 37 зменшить журнал, і ніч зітреться."
    echo "Що саме змінено (для 37): $state"
  } | tee -a "$CURRENT_LOG"
  pause_menu
}

# Повернути все з одного файлу стану. Файл читаємо через дескриптор 3, а adb — з </dev/null:
# інакше `adb shell` з'їдає решту файлу зі stdin. Так 20.10 о 03:51 пункт 37 повернув лише
# «Не турбувати» (перший рядок з adb), а 11 вимкнених програм і 12 приспаних лишились як були.
rec_apply_file() {
  local file="$1" line kind pkg val name
  while IFS= read -r line <&3; do
    kind="${line%% *}"; line="${line#* }"
    case "$kind" in
      disabled)
        pkg="$line"
        if rec_adb shell pm enable --user 0 "$pkg" >> "$CURRENT_LOG" 2>&1; then
          echo "увімкнено: $(package_label "$pkg")" | tee -a "$CURRENT_LOG"; REC_OK=$((REC_OK + 1))
        else
          echo "НЕ ВИЙШЛО увімкнути: $(package_label "$pkg")" | tee -a "$CURRENT_LOG"; REC_FAIL=$((REC_FAIL + 1))
        fi ;;
      appops)
        pkg="${line%% *}"; val="${line#* }"
        if rec_adb shell cmd appops set "$pkg" RUN_ANY_IN_BACKGROUND "$val" >> "$CURRENT_LOG" 2>&1; then
          REC_OK=$((REC_OK + 1)); else REC_FAIL=$((REC_FAIL + 1)); fi ;;
      bucket)
        pkg="${line%% *}"; val="${line#* }"; name="$(rec_bucket_name "$val")"
        if [ -n "$name" ]; then
          if rec_adb shell am set-standby-bucket "$pkg" "$name" >> "$CURRENT_LOG" 2>&1; then
            REC_OK=$((REC_OK + 1)); else REC_FAIL=$((REC_FAIL + 1)); fi
        fi
        echo "розбуджено: $(package_label "$pkg")" | tee -a "$CURRENT_LOG" ;;
      logbuf)
        pkg="${line%% *}"; val="${line#* }"
        if rec_adb logcat -b "$pkg" -G "$val" >> "$CURRENT_LOG" 2>&1; then
          echo "журнал телефона ($pkg) — як був: $val" | tee -a "$CURRENT_LOG"; REC_OK=$((REC_OK + 1))
        else
          echo "НЕ ВИЙШЛО: журнал ($pkg) — лишився більшим до перезапуску телефона" | tee -a "$CURRENT_LOG"; REC_FAIL=$((REC_FAIL + 1))
        fi ;;
      zen)
        name="$(rec_zen_name "$line")"
        if rec_adb shell cmd notification set_dnd "$name" >> "$CURRENT_LOG" 2>&1; then
          echo "«Не турбувати» — як було ($name)" | tee -a "$CURRENT_LOG"; REC_OK=$((REC_OK + 1))
        else
          echo "НЕ ВИЙШЛО: «Не турбувати» — перевір зі шторки" | tee -a "$CURRENT_LOG"; REC_FAIL=$((REC_FAIL + 1))
        fi ;;
    esac
  done 3< "$file"
}

record_mode_off() {
  clear
  if ! require_connection; then pause_menu; return; fi
  local state base f todo="" before
  state="$(rec_state_file)"
  base="${state%.state}"
  # Що повертати: поточний стан (новіший) і старі файли, які 37 ще не повернув до кінця
  # (без позначки restored-ok) — від новішого до найстарішого, щоб наприкінці стало як було спочатку.
  [ -f "$state" ] && todo="$state"
  for f in $(ls -1 "$base".done-* 2>/dev/null | sort -r); do
    grep -q '^restored-ok' "$f" 2>/dev/null || todo="$todo $f"
  done
  if [ -z "$todo" ]; then
    echo "Режим запису на цьому телефоні не вмикався (пункт 36) — повертати нічого."
    pause_menu; return
  fi
  new_log "37" "record-mode-off" "Режим запису: повернути як було"
  REC_OK=0; REC_FAIL=0
  for f in $todo; do
    before="$REC_FAIL"
    echo "Повертаю, як було до $(sed -n 's/^time //p' "$f")..." | tee -a "$CURRENT_LOG"
    rec_apply_file "$f"
    if [ "$REC_FAIL" -eq "$before" ]; then
      echo "restored-ok $(date '+%Y-%m-%d %H:%M')" >> "$f"
      # Файл стану не видаляю — лишаю як історію з позначкою часу.
      [ "$f" = "$state" ] && mv "$state" "$base.done-$(date '+%Y%m%d-%H%M%S')"
    fi
  done

  if [ "$REC_FAIL" -eq 0 ]; then
    echo "ГОТОВО: усе повернуто ($REC_OK)." | tee -a "$CURRENT_LOG"
  else
    echo "Повернуто $REC_OK, не вийшло $REC_FAIL. Запусти пункт 37 ще раз (після пункту 4, якщо зв'язок пропав)." | tee -a "$CURRENT_LOG"
  fi
  pause_menu
}
# record_mode end

# night_report start
# 38 — «Що було за ніч» (faric 2026-10-20 14:42: «що вночі було … хто міг перегружати
# процесор чи пам'ять … пісні взагалі погано записались»). Знімки 12 показують лише
# останні хвилини; тут — те, що Android пам'ятає годинами: чому закривались програми
# (exit-info живе й після закриття), тиск пам'яті за години (procstats), вбивства
# через нестачу пам'яті (журнал подій), нагрів, обриви звуку (underrun). Лише читає.
NIGHT_PACKAGES="com.suno.android ua.faric.trackcatcher com.maxmpz.audioplayer com.aimp.player com.anthropic.claude"

night_report() {
  clear
  if ! require_connection; then pause_menu; return; fi
  local hours pkg
  echo "ЩО БУЛО ЗА НІЧ — лише читаю, нічого не змінюю."
  printf "За скільки годин? (Enter — 12): "
  read -r hours
  case "${hours:-12}" in *[!0-9]*|'') hours=12 ;; *) hours="${hours:-12}" ;; esac
  new_log "38" "night-report" "Що було за ніч (${hours} год)"
  echo "Збираю (~1 хв)..."
  {
    echo "=== Телефон ==="
    echo "Модель: $(rec_adb shell getprop ro.product.model | tr -d '\r')"
    echo "Зараз: $(rec_adb shell date | tr -d '\r')"
    echo "Увімкнений: $(rec_adb shell uptime | tr -d '\r')"
    echo
    echo "=== Чому закривались програми запису (Android пам'ятає й після закриття) ==="
    echo "(reason: LOW_MEMORY — забрала система через пам'ять; USER_REQUESTED / SIGNALED — закрили; CRASH — падіння)"
    for pkg in $NIGHT_PACKAGES; do
      rec_has "$pkg" || continue
      echo "--- $pkg (зараз запущений: $(rec_adb shell pidof "$pkg" 2>/dev/null | tr -d '\r' | awk '{print $1}' | grep . || echo ні))"
      rec_adb shell dumpsys activity exit-info "$pkg" 2>/dev/null | tr -d '\r' \
        | grep -E "ApplicationExitInfo|timestamp=|reason=|description=|importance=|pss=|rss=" | head -n 40
    done
    echo
    echo "=== Скільки годин пам'ятає журнал телефона ==="
    for pkg in main events; do
      echo "$pkg: з $(rec_adb shell "logcat -d -b $pkg -v time | head -n 3" 2>/dev/null | tr -d '\r' | grep -v '^-' | head -n 1 | cut -c1-18) (розмір: $(rec_adb logcat -b "$pkg" -g 2>/dev/null | tr -d '\r' | head -n 1))"
    done
    echo
    echo "=== Вбивства через нестачу пам'яті й закриття (журнал подій) ==="
    rec_adb shell logcat -d -b events -v time 2>/dev/null | tr -d '\r' \
      | grep -E "am_kill|am_proc_died|am_low_memory|am_anr|am_crash|am_wtf" | tail -n 150
    echo
    # 21.10: tail -n 150 вище показав лише 11:57–13:50, а нічні рядки програм запису відрізав.
    echo "=== Програми запису в журналі подій — за весь час, що він пам'ятає ==="
    rec_adb shell logcat -d -b events -v time 2>/dev/null | tr -d '\r' \
      | grep -E "am_kill|am_proc_died|am_proc_start|am_anr|am_crash" \
      | grep -E "com.suno.android|ua.faric.trackcatcher|com.maxmpz.audioplayer|com.aimp.player" | tail -n 200
    echo
    echo "--- Скільки програм закрила система по годинах (am_kill) ---"
    rec_adb shell logcat -d -b events -v time 2>/dev/null | tr -d '\r' \
      | grep "am_kill" | cut -c1-8 | uniq -c
    echo
    echo "=== Тиск пам'яті за ${hours} год (procstats: Normal / Moderate / Low / Critical) ==="
    rec_adb shell dumpsys procstats --hours "$hours" 2>/dev/null | tr -d '\r' \
      | sed -n '/Memory usage:/,/^$/p;/Run time Stats:/,/^$/p;/System memory usage:/,/^$/p' | head -n 80
    echo
    echo "--- Програми запису в procstats (скільки були живі, пам'ять) ---"
    for pkg in $NIGHT_PACKAGES; do
      rec_adb shell dumpsys procstats --hours "$hours" "$pkg" 2>/dev/null | tr -d '\r' \
        | grep -E "^ *\* |TOTAL|Top|Fgs|Service|Cached|Imp Bg" | head -n 12
    done
    echo
    echo "=== Найбільше енергії/процесора з останньої зарядки (batterystats) ==="
    rec_adb shell dumpsys batterystats 2>/dev/null | tr -d '\r' \
      | sed -n '/Estimated power use/,/^ *$/p' | head -n 45
    echo
    echo "=== Нагрів і зарядка ==="
    rec_adb shell dumpsys thermalservice 2>/dev/null | tr -d '\r' \
      | grep -E "Thermal Status|IsStatusOverride|Temperature\{" | head -n 25
    rec_adb shell dumpsys battery 2>/dev/null | tr -d '\r' \
      | grep -E "AC powered|USB powered|level|temperature|status" | head -n 8
    echo
    echo "=== Обриви звуку (underrun — програвач не встиг дати звук) ==="
    rec_adb shell dumpsys media.audio_flinger 2>/dev/null | tr -d '\r' \
      | grep -i -E "Output thread|Input thread|underrun|overrun|Standby|Sample rate|Frames written|Write blocked|Delayed write" \
      | head -n 80
    echo
    echo "=== Хто грав і хто писав звук (історія) ==="
    rec_adb shell dumpsys audio 2>/dev/null | tr -d '\r' \
      | grep -E "requestAudioFocus|abandonAudioFocus|AudioPlaybackConfiguration|recording|capture" | tail -n 40
    echo
    echo "=== Wi-Fi: обриви з'єднання (Suno грає з інтернету) ==="
    rec_adb shell logcat -d -b events,main,system -v time 2>/dev/null | tr -d '\r' \
      | grep -i -E "CTRL-EVENT-(DIS)?CONNECTED|CTRL-EVENT-SSID|reason=[0-9]|onLost|NetworkAgent.*(lost|disconnected)|ADB wifi device disconnected|wifi.*(disconnected|roam)" \
      | grep -v -i "capabilities" | tail -n 80
    echo
    echo "=== Track Catcher про себе (останнє в журналі) ==="
    rec_adb shell logcat -d -v time 2>/dev/null | tr -d '\r' \
      | grep -i -E "trackcatcher|TrackCatcher|SegmentFinisher|FinisherQueue|gap|underrun" | tail -n 80
  } 2>&1 | tee -a "$CURRENT_LOG"
  echo
  echo "Готово. Надішли цей файл у чат (шлях — нижче)."
  pause_menu
}
# night_report end

# Шапка меню: велика назва між смужками (faric 2026-10-16 — «щоб одразу видно, яке це меню»).
# Букви — напівблоки ▀▄█ (шрифт 3×5, згенеровано скриптом menu_banner.py), ширина 45 знаків.
# screen_record start
# Пункт 39 — запис екрана шматками (faric 2026-10-29, з чату Track Catcher).
# Запис екрана Samsung і Track Catcher не працюють разом: Android дає дозвіл
# «Запис екрана» (MediaProjection) лише одному застосунку. `adb shell screenrecord`
# цього дозволу не просить — нікому не заважає. Пише без звуку, до 3 хв за раз,
# тому пишемо шматками поспіль _001, _002… — потім склеїти. Звук — окремо.
SREC_DIR="/sdcard/Movies/PhoneDiagnostics"
SREC_SECONDS="${PD_SREC_SECONDS:-180}"
SREC_BITRATE="6000000"
SREC_MIN_FREE_KB=1048576
SREC_STOP="$STATE_DIR/screen-record.stop"
SREC_COUNT="$STATE_DIR/screen-record.count"

srec_free_kb() {
  rec_adb shell df /sdcard 2>/dev/null | tr -d '\r' | awk 'NR==2 {print $4}'
}

srec_worker() {
  local prefix="$1" n=0 name start took fails=0 free
  while [ ! -f "$SREC_STOP" ]; do
    n=$((n + 1))
    name="$(printf '%s_%03d.mp4' "$prefix" "$n")"
    echo "$n" > "$SREC_COUNT"
    echo "$(date '+%H:%M:%S') шматок $n почато: $SREC_DIR/$name" >> "$CURRENT_LOG"
    start="$(date +%s)"
    rec_adb shell screenrecord --bit-rate "$SREC_BITRATE" --time-limit "$SREC_SECONDS" "$SREC_DIR/$name" >> "$CURRENT_LOG" 2>&1
    took=$(( $(date +%s) - start ))
    echo "$(date '+%H:%M:%S') шматок $n закінчено (${took} с)" >> "$CURRENT_LOG"
    [ -f "$SREC_STOP" ] && break
    if [ "$(adb -s "$ADB_TARGET" get-state 2>/dev/null)" != "device" ]; then
      echo "Зв'язок з телефоном пропав — зупиняю." >> "$CURRENT_LOG"
      echo "lost" > "$SREC_STOP"; break
    fi
    if [ "$took" -lt 3 ]; then
      fails=$((fails + 1))
      if [ "$fails" -ge 3 ]; then
        echo "screenrecord тричі поспіль не почав писати — зупиняю." >> "$CURRENT_LOG"
        echo "fail" > "$SREC_STOP"; break
      fi
    else
      fails=0
    fi
    free="$(srec_free_kb)"
    case "$free" in
      ''|*[!0-9]*) ;;
      *) if [ "$free" -lt "$SREC_MIN_FREE_KB" ]; then
           echo "Місця менше 1 ГБ — зупиняю." >> "$CURRENT_LOG"
           echo "space" > "$SREC_STOP"; break
         fi ;;
    esac
  done
}

screen_record_chunks() {
  clear
  if ! require_connection; then pause_menu; return; fi
  local model free answer prefix pid started el count why files f
  model="$(rec_adb shell getprop ro.product.model 2>/dev/null | tr -d '\r')"
  echo "ЗАПИС ЕКРАНА шматками по $((SREC_SECONDS / 60)) хв — без звуку"
  echo
  echo "Телефон: ${model:-?} ($ADB_TARGET)"
  echo "  Інший телефон — спершу пункт 4 (A26 сам до себе або M30s)."
  echo "Файли: Movies/PhoneDiagnostics на тому телефоні, по порядку _001, _002…"
  echo "Дозвіл «Запис екрана» не потрібен — Track Catcher і візуалізатору не заважає."
  echo "Звук сюди не пишеться — візьми пісню з Track Catcher."
  free="$(srec_free_kb)"
  case "$free" in
    ''|*[!0-9]*) ;;
    *) echo "Вільно на тому телефоні: $((free / 1024)) МБ (3 хв ≈ 135 МБ)."
       if [ "$free" -lt "$SREC_MIN_FREE_KB" ]; then
         echo "Замало місця (менше 1 ГБ) — не починаю."
         pause_menu; return
       fi ;;
  esac
  echo
  printf "Почати? (так — y, ні — Enter): "
  read -r answer
  case "$answer" in y|Y|т|Т|так|Так) ;; *) echo "Не починаю."; pause_menu; return ;; esac

  prefix="screen-$(date '+%Y%m%d-%H%M')"
  new_log "39" "screen-record" "Запис екрана шматками (${model:-?})"
  rm -f "$SREC_STOP" "$SREC_COUNT"
  rec_adb shell mkdir -p "$SREC_DIR" >/dev/null 2>&1
  command -v termux-wake-lock >/dev/null 2>&1 && termux-wake-lock 2>/dev/null
  srec_worker "$prefix" &
  pid=$!
  started="$(date +%s)"
  echo
  echo "ПИШУ. Можна вийти з Termux і вмикати пісню."
  echo "Щоб зупинити — повернись сюди і натисни Enter."
  echo
  while kill -0 "$pid" 2>/dev/null; do
    if read -r -t 5 _; then break; fi
    el=$(( $(date +%s) - started ))
    count="$(cat "$SREC_COUNT" 2>/dev/null)"
    printf "\r  Іде %02d:%02d, шматок %s   " $((el / 60)) $((el % 60)) "${count:-1}"
  done
  echo
  [ -f "$SREC_STOP" ] || echo "stop" > "$SREC_STOP"
  echo "Зупиняю..."
  rec_adb shell 'kill -INT $(pidof screenrecord) 2>/dev/null' >/dev/null 2>&1
  wait "$pid" 2>/dev/null
  command -v termux-wake-unlock >/dev/null 2>&1 && termux-wake-unlock 2>/dev/null
  why="$(cat "$SREC_STOP" 2>/dev/null)"
  rm -f "$SREC_STOP" "$SREC_COUNT"

  files="$(rec_adb shell ls "$SREC_DIR/" 2>/dev/null | tr -d '\r' | grep "^${prefix}_[0-9]*\.mp4$")"
  # Щоб Галерея побачила файли одразу.
  rec_adb shell content call --method scan_volume --uri content://media --arg external_primary >/dev/null 2>&1
  for f in $files; do
    rec_adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d "file://$SREC_DIR/$f" >/dev/null 2>&1
  done
  {
    echo "Зупинено: ${why:-stop}"
    echo "Файли:"
    rec_adb shell ls -l "$SREC_DIR/" 2>/dev/null | tr -d '\r' | grep " ${prefix}_"
  } >> "$CURRENT_LOG"

  echo
  case "$why" in
    lost) echo "⚠ Зв'язок з телефоном пропав — запис зупинився сам." ;;
    fail) echo "⚠ Телефон не дав почати запис (screenrecord). Подробиці — у лозі." ;;
    space) echo "⚠ Закінчилось місце — запис зупинився сам." ;;
  esac
  echo "Готово. Шматків: $(printf '%s\n' "$files" | grep -c .)"
  echo "Папка на телефоні: Movies/PhoneDiagnostics"
  for f in $files; do echo "  $f"; done
  echo "Склеювати по порядку номерів. Між шматками може бути пропуск ~1 с."
  pause_menu
}
# screen_record end

# screen_glue start
# Пункт 40 — склеїти шматки пункту 39 в одне відео і додати звук з Track Catcher
# (faric 2026-10-29 15:04). Звук підганяється за часом: кожен шматок і кожна пісня
# мають час кінця (час файлу на тому телефоні) і тривалість → знаємо, коли почались.
# Для кожного шматка беремо саме його відрізок звуку — пропуски між шматками не зсувають звук.
SGLUE_LOCAL="${PD_LOCAL_MOVIES:-/storage/emulated/0/Movies/PhoneDiagnostics}"
SGLUE_TC_DIR="/sdcard/Music/Track Catcher"

sglue_dur() {
  ffprobe -v error -show_entries format=duration -of csv=p=0 "$1" 2>/dev/null | head -n 1
}

# «час|розмір|шлях» для файлів за шаблоном на тому телефоні
sglue_stat() {
  rec_adb shell "stat -c '%Y|%s|%n' $1 2>/dev/null" | tr -d '\r' | grep '|' | sort -t'|' -k3
}

# Взяти файл з телефона: якщо це той самий телефон і файл уже тут — без копії.
sglue_fetch() {
  local remote="$1" size="$2" work="$3" local_try base
  base="${remote##*/}"
  case "$remote" in
    /sdcard/Movies/PhoneDiagnostics/*) local_try="$SGLUE_LOCAL/$base" ;;
    *) local_try="" ;;
  esac
  if [ -n "$local_try" ] && [ -f "$local_try" ] && [ "$(stat -c %s "$local_try" 2>/dev/null)" = "$size" ]; then
    printf '%s\n' "$local_try"; return 0
  fi
  adb -s "$ADB_TARGET" pull "$remote" "$work/$base" </dev/null >/dev/null 2>&1 || return 1
  printf '%s\n' "$work/$base"
}

screen_glue() {
  clear
  if ! require_connection; then pause_menu; return; fi
  local answer sessions prefix i line mt sz path f d st t0="" tend="" work out total
  local chunk_files="" songs="" n=0 k inputs filt mixin off part list
  echo "СКЛЕЇТИ ЗАПИС ЕКРАНА + ЗВУК З TRACK CATCHER"
  echo
  if ! command -v ffmpeg >/dev/null 2>&1 || ! command -v ffprobe >/dev/null 2>&1; then
    echo "Потрібна програма ffmpeg (≈ 30–60 МБ)."
    printf "Встановити зараз? (так — y, ні — Enter): "
    read -r answer
    case "$answer" in y|Y|т|Т|так|Так) pkg install -y ffmpeg ;; *) pause_menu; return ;; esac
    hash -r
    command -v ffmpeg >/dev/null 2>&1 || { echo "ffmpeg не встановився."; pause_menu; return; }
  fi

  sessions="$(rec_adb shell ls "$SREC_DIR/" 2>/dev/null | tr -d '\r' \
    | sed -n 's/^\(screen-[0-9]\{8\}-[0-9]\{4\}\)_[0-9][0-9]*\.mp4$/\1/p' | sort -u -r)"
  if [ -z "$sessions" ]; then
    echo "На телефоні нема записів пункту 39 (Movies/PhoneDiagnostics)."
    pause_menu; return
  fi
  echo "Записи на телефоні (новіші зверху):"
  i=0
  for f in $sessions; do i=$((i + 1)); echo "  $i — $f"; done
  printf "Який склеїти? (Enter — 1): "
  read -r answer
  case "${answer:-1}" in *[!0-9]*) answer=1 ;; esac
  prefix="$(printf '%s\n' $sessions | sed -n "${answer:-1}p")"
  [ -n "$prefix" ] || { echo "Нема такого номера."; pause_menu; return; }

  new_log "40" "screen-glue" "Склеїти $prefix + звук"
  work="$HOME/.cache/pd-glue/$prefix"
  rm -rf "$work"; mkdir -p "$work"
  echo "Беру шматки..."
  while IFS='|' read -r mt sz path; do
    [ -n "$path" ] || continue
    f="$(sglue_fetch "$path" "$sz" "$work")" || { echo "Не вдалось взяти $path"; continue; }
    d="$(sglue_dur "$f")"
    [ -n "$d" ] || { echo "Пошкоджений шматок: ${path##*/} — пропускаю" | tee -a "$CURRENT_LOG"; continue; }
    st="$(awk -v m="$mt" -v d="$d" 'BEGIN{printf "%.3f", m-d}')"
    [ -n "$t0" ] || t0="$st"
    tend="$mt"
    n=$((n + 1))
    chunk_files="$chunk_files$f|$st|$d
"
    echo "  ${path##*/}: $(awk -v d="$d" 'BEGIN{printf "%d:%02d", d/60, d%60}')" | tee -a "$CURRENT_LOG"
  done <<EOF
$(sglue_stat "$SREC_DIR/${prefix}_*.mp4")
EOF
  [ "$n" -gt 0 ] || { echo "Жодного цілого шматка."; rm -rf "$work"; pause_menu; return; }
  total="$(awk -v a="$t0" -v b="$tend" 'BEGIN{printf "%.3f", b-a}')"

  echo "Шукаю пісні Track Catcher за цей час..."
  k=0
  while IFS='|' read -r mt sz path; do
    [ -n "$path" ] || continue
    # Кінець пісні має бути після початку відео і не пізніше ніж 20 хв після його кінця.
    awk -v m="$mt" -v a="$t0" -v b="$tend" 'BEGIN{exit !(m > a && m < b + 1200)}' || continue
    f="$(sglue_fetch "$path" "$sz" "$work")" || continue
    d="$(sglue_dur "$f")"
    [ -n "$d" ] || continue
    st="$(awk -v m="$mt" -v d="$d" 'BEGIN{printf "%.3f", m-d}')"
    awk -v s="$st" -v b="$tend" 'BEGIN{exit !(s < b)}' || continue
    k=$((k + 1))
    songs="$songs$f|$st
"
    echo "  ♪ ${path##*/} — з $(awk -v s="$st" -v a="$t0" 'BEGIN{o=s-a; sg=(o<0)?"-":""; if(o<0)o=-o; printf "%s%d:%02d", sg, o/60, o%60}') від початку відео" | tee -a "$CURRENT_LOG"
  done <<EOF
$(sglue_stat "'$SGLUE_TC_DIR'/*")
EOF
  if [ "$k" -eq 0 ]; then
    echo "Пісень Track Catcher за цей час не знайшов (Music/Track Catcher)."
    printf "Склеїти без звуку? (так — y, ні — Enter): "
  else
    printf "Склеїти з цим звуком? (так — y, ні — Enter): "
  fi
  read -r answer
  case "$answer" in y|Y|т|Т|так|Так) ;; *) echo "Не склеюю."; rm -rf "$work"; pause_menu; return ;; esac

  echo "Складаю звук..."
  local -a args
  args=(-nostdin -y -v error -f lavfi -t "$total" -i anullsrc=r=48000:cl=stereo)
  filt=""; mixin="[0:a]"; i=0
  while IFS='|' read -r f st; do
    [ -n "$f" ] || continue
    i=$((i + 1))
    off="$(awk -v s="$st" -v a="$t0" 'BEGIN{printf "%.3f", s-a}')"
    if awk -v o="$off" 'BEGIN{exit !(o < 0)}'; then
      args+=(-ss "$(awk -v o="$off" 'BEGIN{printf "%.3f", -o}')" -i "$f")
      filt="$filt[$i:a]aresample=48000,aformat=channel_layouts=stereo[s$i];"
    else
      args+=(-i "$f")
      filt="$filt[$i:a]aresample=48000,aformat=channel_layouts=stereo,adelay=$(awk -v o="$off" 'BEGIN{printf "%d", o*1000}'):all=1[s$i];"
    fi
    mixin="$mixin[s$i]"
  done <<EOF
$songs
EOF
  args+=(-filter_complex "${filt}${mixin}amix=inputs=$((i + 1)):normalize=0:duration=first[out]" -map "[out]" -c:a pcm_s16le "$work/timeline.wav")
  ffmpeg "${args[@]}" >>"$CURRENT_LOG" 2>&1
  [ -s "$work/timeline.wav" ] || { echo "Не вдалось скласти звук — подробиці в лозі."; rm -rf "$work"; pause_menu; return; }

  echo "Склеюю відео..."
  list="$work/list.txt"; : > "$list"; i=0
  while IFS='|' read -r f st d; do
    [ -n "$f" ] || continue
    i=$((i + 1))
    part="$work/part_$(printf '%03d' "$i").mp4"
    off="$(awk -v s="$st" -v a="$t0" 'BEGIN{o=s-a; if(o<0)o=0; printf "%.3f", o}')"
    ffmpeg -nostdin -y -v error -i "$f" -ss "$off" -t "$d" -i "$work/timeline.wav" \
      -map 0:v:0 -map 1:a:0 -c:v copy -c:a aac -b:a 192k -shortest "$part" >>"$CURRENT_LOG" 2>&1
    [ -s "$part" ] && echo "file '$part'" >> "$list"
  done <<EOF
$chunk_files
EOF
  mkdir -p "$SGLUE_LOCAL"
  out="$SGLUE_LOCAL/${prefix}_zvuk.mp4"
  [ "$k" -eq 0 ] && out="$SGLUE_LOCAL/${prefix}_razom.mp4"
  ffmpeg -nostdin -y -v error -f concat -safe 0 -i "$list" -c copy -movflags +faststart "$out" >>"$CURRENT_LOG" 2>&1
  rm -rf "$work"
  if [ -s "$out" ]; then
    command -v termux-media-scan >/dev/null 2>&1 && termux-media-scan "$out" >/dev/null 2>&1
    echo "Готово: $out" | tee -a "$CURRENT_LOG"
    echo "Довжина: $(awk -v d="$(sglue_dur "$out")" 'BEGIN{printf "%d:%02d", d/60, d%60}')"
    echo "Лежить на ЦЬОМУ телефоні: Movies/PhoneDiagnostics/${out##*/}"
  else
    echo "Не вийшло склеїти — подробиці в лозі."
  fi
  pause_menu
}
# screen_glue end

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
  echo "10 — Audio diagnostics → файл (і ефекти звуку: Dolby Atmos)"
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
  echo "21 — Вимкнути зайве службове (свій список для Redmi і Samsung)"
  echo "22 — Повернути програми з пункту 21"
  echo "23 — Приспати рідкі й важкі застосунки (свій список для Redmi і Samsung)"
  echo "24 — Розбудити застосунки з пункту 23"
  echo "25 — Що надсилає «Відкрити за допомогою» → файл"
  echo "26 — Зберегти застосунок (усі частини APK): цей телефон або підключений"
  echo "27 — Поставити збережений застосунок на телефон"
  echo "28 — Що робить сканер медіа → файл (нічого не змінює)"
  echo "29 — Галерея: прибрати папки з .nomedia (файли не чіпає)"
  echo "30 — Ціни криптовалют для тренажера → файл (лише читає Binance)"
  echo "31 — Що роблять потоки застосунку (ядра, пріоритет) → файл"
  echo "32 — Прискорити застосунок (машинний код; звук і дані ті самі)"
  echo "33 — Renault Docs: заміри читання томів → файл (нічого не змінює)"
  echo "34 — Чому закрився застосунок (журнал падінь) → файл"
  echo "35 — Прибрати сміття в проєктах (спершу покаже; видаляє лише після «так»)"
  echo "36 — Перед записом: усе зайве вимкнути (режим запису)"
  echo "37 — Після запису: усе повернути, як було"
  echo "38 — Що було за ніч: хто закрив, пам'ять, нагрів, обриви звуку → файл (лише читає)"
  echo "39 — Запис екрана шматками по 3 хв (без звуку; Track Catcher не заважає)"
  echo "40 — Склеїти запис 39 в одне відео + звук пісні з Track Catcher"
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
    33) renault_read_timing ;;
    34) app_crash_report ;;
    35) junk_cleanup ;;
    36) record_mode_on ;;
    37) record_mode_off ;;
    38) night_report ;;
    39) screen_record_chunks ;;
    40) screen_glue ;;
    0) clear; exit 0 ;;
    *) echo; echo "Невідомий пункт: $choice"; sleep 1 ;;
  esac
done
