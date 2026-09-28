#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

REPO_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
WORKFLOW="android-debug.yml"

# shellcheck source=reno-github.sh
source "$REPO_DIR/tools/termux/reno-github.sh"

cd "$REPO_DIR"

reno_require_gh || exit 1

GH_REPO="$(reno_github_repo)" || {
  echo "Не вдалося визначити GitHub repository з origin."
  echo "Перевір: git remote -v"
  exit 1
}

BRANCH="$(git branch --show-current)"
if [ -z "$BRANCH" ]; then
  echo "Не вдалося визначити поточну Git-гілку."
  exit 1
fi

if ! git diff --quiet ||
   ! git diff --cached --quiet ||
   [ -n "$(git ls-files --others --exclude-standard)" ]; then
  echo "У repository є локальні незбережені зміни."
  echo "GitHub Actions їх не бачить, тому build зупинено."
  echo
  git status --short
  exit 1
fi

if ! git fetch --prune origin \
  "+refs/heads/$BRANCH:refs/remotes/origin/$BRANCH"; then
  echo "Не вдалося оновити origin/$BRANCH."
  exit 1
fi

LOCAL_SHA="$(git rev-parse HEAD)"
REMOTE_SHA="$(git rev-parse "refs/remotes/origin/$BRANCH")"

if [ "$LOCAL_SHA" != "$REMOTE_SHA" ]; then
  echo "Local HEAD і origin/$BRANCH не збігаються."
  echo "GitHub Actions будує remote commit, тому спочатку синхронізуй Git."
  echo
  echo "local:  $LOCAL_SHA"
  echo "remote: $REMOTE_SHA"
  exit 1
fi

BEFORE_RUN="$(
  gh run list \
    --repo "$GH_REPO" \
    --workflow "$WORKFLOW" \
    --branch "$BRANCH" \
    --event workflow_dispatch \
    --limit 1 \
    --json databaseId \
    --jq '.[0].databaseId // empty' 2>/dev/null || true
)"

echo "Renault Docs"
echo "GitHub: $GH_REPO"
echo "Гілка:  $BRANCH"
echo "Commit: $LOCAL_SHA"
echo
echo "Запускаю Android Debug APK build..."

gh workflow run "$WORKFLOW" \
  --repo "$GH_REPO" \
  --ref "$BRANCH"

RUN_ID=""
for _ in $(seq 1 45); do
  sleep 1
  RUN_ID="$(
    gh run list \
      --repo "$GH_REPO" \
      --workflow "$WORKFLOW" \
      --branch "$BRANCH" \
      --event workflow_dispatch \
      --limit 1 \
      --json databaseId,headSha \
      --jq ".[0] | select(.headSha == \"$LOCAL_SHA\") | .databaseId // empty" \
      2>/dev/null || true
  )"

  if [ -n "$RUN_ID" ] && [ "$RUN_ID" != "$BEFORE_RUN" ]; then
    break
  fi
done

if [ -z "$RUN_ID" ] || [ "$RUN_ID" = "$BEFORE_RUN" ]; then
  echo "Workflow запущено, але точний run для commit $LOCAL_SHA ще не знайдено."
  echo "Перевір пізніше через Renault Menu → 8."
  exit 1
fi

echo "Run ID: $RUN_ID"
echo
echo "Чекаю завершення GitHub Actions..."
gh run watch "$RUN_ID" \
  --repo "$GH_REPO" \
  --exit-status

echo
echo "Build PASS. Завантажую exact artifact цього commit..."
echo

exec bash "$REPO_DIR/tools/termux/reno-download-apk.sh" "$RUN_ID"
