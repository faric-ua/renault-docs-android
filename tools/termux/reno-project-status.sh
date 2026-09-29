#!/data/data/com.termux/files/usr/bin/bash
set -u

REPO_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
# shellcheck source=reno-github.sh
source "$REPO_DIR/tools/termux/reno-github.sh"

cd "$REPO_DIR" || exit 1

clean_md() {
  printf '%s' "$1" | sed 's/\*\*//g; s/`//g'
}

json_string_field() {
  local file="$1"
  local key="$2"
  sed -n "s/.*\"$key\"[[:space:]]*:[[:space:]]*\"\([^\"]*\)\".*/\1/p" "$file" | sed -n '1p'
}

workflow_label() {
  local status="$1"
  local conclusion="$2"

  if [ "$status" != "completed" ]; then
    case "$status" in
      in_progress) printf 'RUNNING' ;;
      queued|pending|requested|waiting) printf 'WAITING' ;;
      *) printf '%s' "$status" ;;
    esac
    return
  fi

  case "$conclusion" in
    success) printf 'PASS' ;;
    failure) printf 'FAIL' ;;
    cancelled) printf 'CANCELLED' ;;
    skipped) printf 'SKIPPED' ;;
    *) printf '%s' "${conclusion:-completed}" ;;
  esac
}

show_workflow() {
  local title="$1"
  local workflow="$2"
  local row run_id status conclusion run_sha event created label

  row="$(
    gh run list       --repo "$GH_REPO"       --workflow "$workflow"       --branch "$BRANCH"       --limit 1       --json databaseId,status,conclusion,headSha,event,createdAt       --jq '.[0] | "\(.databaseId)|\(.status)|\(.conclusion // "-")|\(.headSha)|\(.event)|\(.createdAt)"'       2>/dev/null || true
  )"

  if [ -z "$row" ]; then
    echo "$title: немає run для гілки $BRANCH"
    return
  fi

  IFS='|' read -r run_id status conclusion run_sha event created <<EOF
$row
EOF

  label="$(workflow_label "$status" "$conclusion")"

  echo "$title: $label"
  echo "  Run ID:  $run_id"
  echo "  Commit:  $run_sha"
  echo "  Trigger: $event"
  echo "  Time:    $created"

  if [ "$workflow" = "android-debug.yml" ] &&
     git cat-file -e "$run_sha^{commit}" 2>/dev/null &&
     git merge-base --is-ancestor "$run_sha" "$LOCAL_SHA" 2>/dev/null &&
     git diff --quiet "$run_sha..$LOCAL_SHA" -- android .github/workflows/android-debug.yml; then
    echo "  Coverage: OK — після цього run Android/build файли не змінювались"
  fi
}

BRANCH="$(git branch --show-current 2>/dev/null || true)"
[ -n "$BRANCH" ] || BRANCH="detached"
LOCAL_SHA="$(git rev-parse HEAD 2>/dev/null || true)"
VERSION_NAME="$(sed -n 's/.*versionName = "\([^"]*\)".*/\1/p' android/app/build.gradle.kts | sed -n '1p')"
VERSION_CODE="$(sed -n 's/.*versionCode = \([0-9][0-9]*\).*/\1/p' android/app/build.gradle.kts | sed -n '1p')"

PLAN="$REPO_DIR/docs/assistant-kit/CURRENT_PLAN.md"
META="$REPO_DIR/docs/v.0.5.51/RELEASE_META.json"

PROJECT_STATUS=""
NEXT_STEP=""
if [ -f "$PLAN" ]; then
  PROJECT_STATUS="$(sed -n 's/^Статус:[[:space:]]*//p' "$PLAN" | sed -n '1p')"
  NEXT_STEP="$(
    awk '
      /^## Поточний наступний крок/ { capture=1; next }
      capture && NF { print; exit }
    ' "$PLAN"
  )"
fi

QA_STATUS=""
DISTRIBUTION_STATUS=""
MERGED_SHA=""
EXPECTED_SIGNER=""
if [ -f "$META" ]; then
  QA_STATUS="$(json_string_field "$META" "qa_status")"
  DISTRIBUTION_STATUS="$(json_string_field "$META" "distribution_status")"
  MERGED_SHA="$(json_string_field "$META" "merged_source_sha")"
  EXPECTED_SIGNER="$(json_string_field "$META" "signer_certificate_sha256")"
fi

echo "========================================"
echo "       Renault Docs · Status"
echo "========================================"
echo
echo "Project:  Renault Docs"
echo "Version:  v${VERSION_NAME:-?} (code ${VERSION_CODE:-?})"
echo "Branch:   $BRANCH"
echo "Commit:   ${LOCAL_SHA:-невідомий}"
[ -n "$MERGED_SHA" ] && echo "Merged:   $MERGED_SHA"
echo

if [ -n "$PROJECT_STATUS" ]; then
  echo "Project status:"
  echo "  $(clean_md "$PROJECT_STATUS")"
fi
if [ -n "$QA_STATUS" ]; then
  echo "QA:       $QA_STATUS"
fi
if [ -n "$DISTRIBUTION_STATUS" ]; then
  echo "Delivery: $DISTRIBUTION_STATUS"
fi
if [ -n "$EXPECTED_SIGNER" ]; then
  echo "Signer expected:"
  echo "  $EXPECTED_SIGNER"
fi

echo
if [ -n "$NEXT_STEP" ]; then
  echo "Next step:"
  echo "  $(clean_md "$NEXT_STEP")"
  echo
fi

if command -v gh >/dev/null 2>&1 && gh auth status -h github.com >/dev/null 2>&1; then
  GH_REPO="$(reno_github_repo 2>/dev/null || true)"
  if [ -n "$GH_REPO" ]; then
    echo "GitHub Actions:"
    show_workflow "  Android Debug APK" "android-debug.yml"
    echo
    show_workflow "  Tests" "tests.yml"
  else
    echo "GitHub Actions: не вдалося визначити repository з origin."
  fi
else
  echo "GitHub Actions: недоступно — gh не встановлений або не авторизований."
fi

echo
echo "Підказка:"
echo "  PASS build ≠ готовність до встановлення."
echo "  Якщо Delivery = SIGNER_CONTINUITY_PENDING, APK поки не ставимо поверх поточного застосунку."
