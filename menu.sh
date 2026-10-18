#!/data/data/com.termux/files/usr/bin/bash
set -u

REPO="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
# shellcheck source=tools/termux/reno-github.sh
source "$REPO/tools/termux/reno-github.sh"

pause_menu() {
  echo
  printf "Натисни Enter, щоб повернутися в меню..."
  read -r _
}

# TODO №34 (faric 2026-10-18 20:09): bash тримає старий список меню в пам'яті,
# тож після оновлення нові пункти не видно, доки меню не закрити (0) і не відкрити знову.
# Тепер: якщо оновлення змінило menu.sh — меню саме відкривається заново.
menu_fingerprint() {
  cksum < "$REPO/menu.sh" 2>/dev/null || true
}

restart_menu_if_changed() {
  local before="$1" after
  after="$(menu_fingerprint)"
  if [ -n "$before" ] && [ -n "$after" ] && [ "$before" != "$after" ]; then
    echo
    echo "Меню теж оновилось — після Enter відкриється нове меню з новими пунктами."
    pause_menu
    exec bash "$REPO/menu.sh"
  fi
}

repo_dirty() {
  ! git diff --quiet ||
    ! git diff --cached --quiet ||
    [ -n "$(git ls-files --others --exclude-standard)" ]
}

menu_context() {
  local branch repo_slug
  branch="$(git -C "$REPO" branch --show-current 2>/dev/null || true)"
  repo_slug="$(cd "$REPO" && reno_github_repo 2>/dev/null || true)"

  [ -n "$branch" ] || branch="detached"
  [ -n "$repo_slug" ] || repo_slug="GitHub remote: невідомий"

  printf '%s · %s\n' "$branch" "$repo_slug"
}

run_browser() {
  clear
  echo "Запускаю браузерну версію..."
  echo
  bash "$REPO/browser.sh"
}

open_code() {
  clear
  cd "$REPO" || exit 1
  echo "Код Renault:"
  pwd
  echo
  echo "Відкриваю окремий shell у папці проєкту."
  echo "Щоб повернутися в Renault Menu, введи: exit"
  echo

  "${SHELL:-$PREFIX/bin/bash}" -i
}

show_status() {
  clear
  bash "$REPO/tools/termux/reno-status.sh"
  pause_menu
}

stop_server() {
  clear
  bash "$REPO/tools/termux/reno-stop.sh"
  pause_menu
}

update_project() {
  clear
  echo "Оновлюю Renault з GitHub..."
  echo
  cd "$REPO" || exit 1

  if repo_dirty; then
    echo "У репозиторії є локальні або нові файли."
    echo "Автоматичне оновлення скасовано, щоб нічого не перезаписати."
    echo
    git status --short
    pause_menu
    return
  fi

  local branch
  branch="$(git branch --show-current)"

  if [ -z "$branch" ]; then
    echo "Не вдалося визначити поточну Git-гілку."
    pause_menu
    return
  fi

  echo "Поточна гілка: $branch"
  echo

  local menu_before
  menu_before="$(menu_fingerprint)"

  if git fetch --prune origin \
       "+refs/heads/$branch:refs/remotes/origin/$branch" &&
     git merge --ff-only "refs/remotes/origin/$branch"; then
    echo
    echo "Готово."
    restart_menu_if_changed "$menu_before"
  else
    echo
    echo "Оновлення не виконано. Подивись повідомлення Git вище."
  fi

  pause_menu
}

build_download_apk() {
  clear
  bash "$REPO/tools/termux/reno-build-apk.sh"
  pause_menu
}

download_latest_apk() {
  clear
  bash "$REPO/tools/termux/reno-download-apk.sh"
  pause_menu
}


download_phone_candidate() {
  clear
  local menu_before
  menu_before="$(menu_fingerprint)"
  bash "$REPO/tools/termux/reno-candidate.sh"
  restart_menu_if_changed "$menu_before"
  pause_menu
}

return_to_main() {
  clear
  echo "Повертаю Renault на main..."
  echo
  cd "$REPO" || exit 1

  if repo_dirty; then
    echo "У репозиторії є локальні або нові файли."
    echo "Перехід на main скасовано, щоб нічого не втратити."
    echo
    git status --short
    pause_menu
    return
  fi

  if git fetch --prune origin \
       "+refs/heads/main:refs/remotes/origin/main" &&
     git switch main &&
     git merge --ff-only refs/remotes/origin/main; then
    echo
    echo "Готово. Поточна гілка: main"
  else
    echo
    echo "Не вдалося повернутися на main."
  fi

  pause_menu
}

open_latest_apk_folder() {
  clear
  bash "$REPO/tools/termux/reno-open-latest-apk.sh"
  pause_menu
}

fast_converter() {
  clear
  bash "$REPO/tools/termux/reno-fast-convert.sh"
  pause_menu
}

build_rdpkg() {
  clear
  bash "$REPO/tools/termux/reno-build-rdpkg.sh"
  pause_menu
}

rebuild_modern_index() {
  clear
  echo "Оновлюю Fast/Modern package готового dataset..."
  echo
  cd "$REPO" || exit 1
  python tools/package_dataset.py
  pause_menu
}

export_runtime_section_bundle() {
  clear
  cd "$REPO" || exit 1

  echo "Runtime IR — source bundle секції"
  echo
  printf "Том [NT8183A]: "
  read -r volume
  volume="${volume:-NT8183A}"
  printf "Секція [101]: "
  read -r section
  section="${section:-101}"
  echo

  python tools/export_runtime_section_bundle.py \
    --volume "$volume" \
    --section "$section"

  pause_menu
}

export_runtime_section_ir() {
  clear
  cd "$REPO" || exit 1

  echo "Runtime IR — готова JSON секція"
  echo
  printf "Том [NT8183A]: "
  read -r volume
  volume="${volume:-NT8183A}"
  printf "Секція [101]: "
  read -r section
  section="${section:-101}"
  echo

  python tools/export_runtime_section_ir.py \
    --volume "$volume" \
    --section "$section"

  pause_menu
}

export_runtime_ir_coverage() {
  clear
  cd "$REPO" || exit 1

  echo "Runtime IR — coverage по всіх томах/роках"
  echo
  python tools/export_runtime_ir_coverage.py

  pause_menu
}

show_project_status() {
  clear
  bash "$REPO/tools/termux/reno-project-status.sh"
  pause_menu
}

restore_accepted_signer() {
  clear
  bash "$REPO/tools/termux/reno-restore-accepted-signer.sh"
  pause_menu
}

audit_legacy_android() {
  clear
  bash "$REPO/tools/termux/reno-audit-legacy-android.sh"
  pause_menu
}

check_dataset_links() {
  clear
  bash "$REPO/tools/termux/reno-check-dataset-links.sh"
  pause_menu
}

quarantine_legacy_android() {
  clear
  bash "$REPO/tools/termux/reno-quarantine-legacy-android.sh"
  pause_menu
}

migrate_rdpkg_names() {
  clear
  bash "$REPO/tools/termux/reno-migrate-rdpkg-names.sh"
  pause_menu
}

fix_volume_logo() {
  clear
  bash "$REPO/tools/termux/reno-fix-volume-logo.sh"
  pause_menu
}

logo_fixed_report() {
  clear
  bash "$REPO/tools/termux/reno-logo-fixed-report.sh"
  pause_menu
}

build_public_catalog() {
  clear
  bash "$REPO/tools/termux/reno-build-public-catalog.sh"
  pause_menu
}

refresh_termux_integration() {
  clear
  echo "Оновлюю Renault aliases та Termux:Widget shortcut..."
  echo
  bash "$REPO/tools/install_termux_aliases.sh"
  echo
  bash "$REPO/tools/install_termux_widget.sh"
  echo
  echo "Готово. Для aliases відкрий нову Termux-сесію або виконай source ~/.bashrc."
  echo "У Termux:Widget натисни Refresh."
  pause_menu
}

show_paths() {
  clear
  echo "Renault paths"
  echo
  echo "Код:"
  echo "  $REPO"
  echo
  echo "Оригінал:"
  echo "  /storage/emulated/0/Documents/Renault/laguna 2 2001-2006"
  echo
  echo "Legacy quarantine:"
  echo "  /storage/emulated/0/Documents/Renault/legacy-quarantine"
  echo
  echo "Готовий legacy dataset (після quarantine):"
  echo "  /storage/emulated/0/Documents/Renault/legacy-quarantine/laguna 2 2001-2006_android"
  echo
  echo "APK/packages:"
  echo "  /storage/emulated/0/Documents/Renault/packages"
  pause_menu
}

# Шапка меню: велика назва між смужками (faric 2026-10-16 — «щоб одразу видно, яке це меню»).
# Букви — напівблоки ▀▄█ (шрифт 3×5, згенеровано скриптом menu_banner.py), ширина 29 знаків.
# Вужчий екран — проста шапка одним рядком.
show_banner() {
  local c="\033[1;33m" d="\033[0;33m" z="\033[0m" cols
  cols="$(stty size 2>/dev/null | cut -d' ' -f2)"
  if [ "${cols:-80}" -ge 29 ] 2>/dev/null; then
    echo -e "${d}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${z}"
    echo -e "${c} █▀▄ █▀▀ █▀▄ █▀█ █ █ █   ▀█▀${z}"
    echo -e "${c} █▀▄ █▀  █ █ █▀█ █ █ █    █${z}"
    echo -e "${c} ▀ ▀ ▀▀▀ ▀ ▀ ▀ ▀ ▀▀▀ ▀▀▀  ▀${z}"
    echo -e "${c}       █▀▄ █▀█ █▀▀ █▀▀${z}"
    echo -e "${c}       █ █ █ █ █   ▀▀█${z}"
    echo -e "${c}       ▀▀  ▀▀▀ ▀▀▀ ▀▀▀${z}"
    echo -e "${d}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${z}"
  else
    echo -e "${c}━━ RENAULT DOCS ━━${z}"
  fi
}

while true; do
  clear
  show_banner
  echo "          Renault Docs Menu"
  echo "  $(menu_context)"
  echo
  echo "1 — Відкрити браузерну документацію"
  echo "2 — Відкрити shell у коді Renault"
  echo "3 — Статус локального сервера"
  echo "4 — Зупинити локальний сервер"
  echo "5 — Оновити проєкт з GitHub"
  echo "6 — Показати основні папки Renault"
  echo "7 — Build + Download APK"
  echo "8 — Download APK для поточного commit"
  echo "9 — Оновити Fast/Modern package"
  echo "10 — Експорт Runtime IR source bundle"
  echo "11 — Експорт готової Runtime IR JSON секції"
  echo "12 — Експорт Runtime IR coverage"
  echo "13 — Відкрити папку останнього APK"
  echo "14 — Швидкий конвертер (Termux/direct filesystem)"
  echo "15 — Створити single-volume .rdpkg"
  echo "16 — Тестовий candidate PR: перейти + build/download APK"
  echo "17 — Повернутися на main"
  echo "18 — Оновити Termux aliases / Widget"
  echo "19 — Статус проєкту / build"
  echo "20 — Відновити accepted signer + build"
  echo "21 — Аудит legacy *_android (read-only)"
  echo "22 — Перевірити посилання dataset (read-only)"
  echo "23 — Legacy quarantine *_android (move/restore, без видалення)"
  echo "24 — Міграція назв локальних .rdpkg (безпечне перейменування)"
  echo "25 — Створити public Catalog v4 (15 пакетів)"
  echo "26 — Замінити зіпсоване лого в томах Laguna II"
  echo "27 — Звіт для Диска: томи Laguna II з новим лого"
  echo "0 — Вийти"
  echo
  printf "Вибір: "
  read -r choice

  case "$choice" in
    1)
      run_browser
      exit $?
      ;;
    2)
      open_code
      ;;
    3)
      show_status
      ;;
    4)
      stop_server
      ;;
    5)
      update_project
      ;;
    6)
      show_paths
      ;;
    7)
      build_download_apk
      ;;
    8)
      download_latest_apk
      ;;
    9)
      rebuild_modern_index
      ;;
    10)
      export_runtime_section_bundle
      ;;
    11)
      export_runtime_section_ir
      ;;
    12)
      export_runtime_ir_coverage
      ;;
    13)
      open_latest_apk_folder
      ;;
    14)
      fast_converter
      ;;
    15)
      build_rdpkg
      ;;
    16)
      download_phone_candidate
      ;;
    17)
      return_to_main
      ;;
    18)
      refresh_termux_integration
      ;;
    19)
      show_project_status
      ;;
    20)
      restore_accepted_signer
      ;;
    21)
      audit_legacy_android
      ;;
    22)
      check_dataset_links
      ;;
    23)
      quarantine_legacy_android
      ;;
    24)
      migrate_rdpkg_names
      ;;
    25)
      build_public_catalog
      ;;
    26)
      fix_volume_logo
      ;;
    27)
      logo_fixed_report
      ;;
    0)
      clear
      exit 0
      ;;
    *)
      echo
      echo "Невідомий пункт: $choice"
      sleep 1
      ;;
  esac
done
