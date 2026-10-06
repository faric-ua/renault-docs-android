#!/data/data/com.termux/files/usr/bin/bash
set -u

REPO_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
# shellcheck source=reno-github.sh
source "$REPO_DIR/tools/termux/reno-github.sh"

cd "$REPO_DIR" || exit 1

if [ -t 1 ]; then
  GREEN='\033[32m'
  YELLOW='\033[33m'
  RED='\033[31m'
  BOLD='\033[1m'
  RESET='\033[0m'
else
  GREEN=''
  YELLOW=''
  RED=''
  BOLD=''
  RESET=''
fi

ok() {
  printf "%b✓%b %s\n" "$GREEN" "$RESET" "$*"
}

pending() {
  printf "%b…%b %s\n" "$YELLOW" "$RESET" "$*"
}

bad() {
  printf "%b✗%b %s\n" "$RED" "$RESET" "$*"
}

workflow_row_for_current_commit() {
  local workflow="$1"

  gh run list \
    --repo "$GH_REPO" \
    --workflow "$workflow" \
    --branch "$BRANCH" \
    --limit 30 \
    --json databaseId,status,conclusion,headSha \
    --jq ".[] | select(.headSha == \"$LOCAL_SHA\") | \"\\(.databaseId)|\\(.status)|\\(.conclusion // \"-\")\"" \
    2>/dev/null |
    sed -n '1p'
}

show_workflow_state() {
  local title="$1"
  local workflow="$2"
  local row run_id status conclusion

  row="$(workflow_row_for_current_commit "$workflow")"

  if [ -z "$row" ]; then
    bad "$title: build для поточного commit не знайдено"
    return 2
  fi

  IFS='|' read -r run_id status conclusion <<EOF
$row
EOF

  if [ "$status" != "completed" ]; then
    case "$status" in
      in_progress)
        pending "$title: ще виконується"
        ;;
      queued|pending|requested|waiting)
        pending "$title: у черзі"
        ;;
      *)
        pending "$title: $status"
        ;;
    esac
    return 1
  fi

  case "$conclusion" in
    success)
      ok "$title: PASS"
      return 0
      ;;
    failure)
      bad "$title: FAIL"
      return 2
      ;;
    cancelled)
      bad "$title: скасовано"
      return 2
      ;;
    *)
      bad "$title: ${conclusion:-невідомий результат}"
      return 2
      ;;
  esac
}

BRANCH="$(git branch --show-current 2>/dev/null || true)"
[ -n "$BRANCH" ] || BRANCH="detached"

LOCAL_SHA="$(git rev-parse HEAD 2>/dev/null || true)"
SHORT_SHA="$(git rev-parse --short=10 HEAD 2>/dev/null || true)"
VERSION_NAME="$(sed -n 's/.*versionName = "\([^"]*\)".*/\1/p' android/app/build.gradle.kts | sed -n '1p')"
VERSION_CODE="$(sed -n 's/.*versionCode = \([0-9][0-9]*\).*/\1/p' android/app/build.gradle.kts | sed -n '1p')"

echo "========================================"
echo "     Renault Docs · Статус"
echo "========================================"
echo

ok "Версія: v${VERSION_NAME:-?} / build ${VERSION_CODE:-?}"
ok "Гілка: $BRANCH"
ok "Commit: ${SHORT_SHA:-невідомий}"

UPDATED=0
if [ "$BRANCH" != "detached" ] &&
   git fetch origin "$BRANCH" --quiet 2>/dev/null; then
  REMOTE_SHA="$(git rev-parse "origin/$BRANCH" 2>/dev/null || true)"
  if [ -n "$REMOTE_SHA" ] && [ "$REMOTE_SHA" = "$LOCAL_SHA" ]; then
    ok "Проєкт оновлений"
    UPDATED=1
  else
    bad "Проєкт не оновлений → натисни 5"
  fi
else
  pending "Не вдалося перевірити оновлення GitHub"
fi

echo

if ! command -v gh >/dev/null 2>&1 ||
   ! gh auth status -h github.com >/dev/null 2>&1; then
  bad "GitHub Actions недоступний"
  echo
  printf "%bЩЕ НЕ ГОТОВО%b\n" "$RED" "$RESET"
  echo "Перевір gh login / інтернет."
  exit 0
fi

GH_REPO="$(reno_github_repo 2>/dev/null || true)"
if [ -z "$GH_REPO" ]; then
  bad "Не вдалося визначити GitHub repository"
  echo
  printf "%bЩЕ НЕ ГОТОВО%b\n" "$RED" "$RESET"
  exit 0
fi

TESTS_STATE=0
APK_STATE=0

show_workflow_state "Tests" "tests.yml" || TESTS_STATE=$?
show_workflow_state "APK build" "android-debug.yml" || APK_STATE=$?

echo

if [ "$UPDATED" -eq 1 ] &&
   [ "$TESTS_STATE" -eq 0 ] &&
   [ "$APK_STATE" -eq 0 ]; then
  printf "%b%b✓ МОЖНА ЗАВАНТАЖУВАТИ%b\n" "$GREEN" "$BOLD" "$RESET"
  echo "  Натисни: 8"
else
  if [ "$APK_STATE" -eq 1 ] || [ "$TESTS_STATE" -eq 1 ]; then
    printf "%b%b… ЩЕ НЕ ГОТОВО%b\n" "$YELLOW" "$BOLD" "$RESET"
    echo "  Build ще виконується. Через трохи знову натисни 19."
  else
    printf "%b%b✗ ЩЕ НЕ ГОТОВО%b\n" "$RED" "$BOLD" "$RESET"
    if [ "$UPDATED" -ne 1 ]; then
      echo "  Спочатку натисни: 5"
    elif [ "$APK_STATE" -eq 2 ]; then
      echo "  Якщо APK build відсутній — натисни: 7"
    else
      echo "  Перевір Tests / APK build."
    fi
  fi
fi
