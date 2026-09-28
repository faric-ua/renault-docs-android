#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

REPO_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
CANDIDATE_PR="${RENAULT_CANDIDATE_PR:-}"

# shellcheck source=reno-github.sh
source "$REPO_DIR/tools/termux/reno-github.sh"

cd "$REPO_DIR"

reno_require_gh || exit 1

GH_REPO="$(reno_github_repo)" || {
  echo "Не вдалося визначити GitHub repository з origin."
  echo "Перевір: git remote -v"
  exit 1
}

if [ -z "$CANDIDATE_PR" ]; then
  echo "Renault Docs · відкриті Pull Requests"
  echo
  OPEN_PRS="$(
    gh pr list \
      --repo "$GH_REPO" \
      --state open \
      --limit 20 \
      --json number,headRefName,title \
      --jq '.[] | "#\(.number)\t\(.headRefName)\t\(.title)"'
  )"

  if [ -z "$OPEN_PRS" ]; then
    echo "Відкритих Pull Requests немає."
    exit 1
  fi

  printf '%s\n' "$OPEN_PRS"
  echo
  printf "Номер PR (0 — назад): "
  read -r CANDIDATE_PR

  case "$CANDIDATE_PR" in
    0|'')
      echo "Скасовано."
      exit 0
      ;;
    *[!0-9]*)
      echo "Потрібен числовий номер PR."
      exit 1
      ;;
  esac
fi

PR_LINE="$(
  gh pr view "$CANDIDATE_PR" \
    --repo "$GH_REPO" \
    --json title,state,headRefName,url \
    --jq '[.state, .headRefName, .title, .url] | @tsv'
)"

IFS="$(printf '\t')" read -r PR_STATE PR_BRANCH PR_TITLE PR_URL <<EOF
$PR_LINE
EOF

if [ "$PR_STATE" != "OPEN" ] || [ -z "$PR_BRANCH" ] || [ "$PR_BRANCH" = "null" ]; then
  echo "Тестовий candidate PR #$CANDIDATE_PR зараз недоступний."
  echo "Стан: $PR_STATE"
  exit 1
fi

echo
echo "Renault Docs · тестовий candidate"
echo
echo "GitHub:  $GH_REPO"
echo "PR:      #$CANDIDATE_PR"
echo "Назва:   $PR_TITLE"
echo "Гілка:   $PR_BRANCH"
echo "URL:     $PR_URL"
echo

echo "Оновлюю candidate branch з GitHub..."
git fetch --prune origin \
  "+refs/heads/$PR_BRANCH:refs/remotes/origin/$PR_BRANCH"

if ! git show-ref --verify --quiet "refs/remotes/origin/$PR_BRANCH"; then
  echo "GitHub не повернув remote ref для candidate branch:"
  echo "  $PR_BRANCH"
  exit 1
fi

REMOTE_REF="refs/remotes/origin/$PR_BRANCH"

if ! git diff --quiet ||
   ! git diff --cached --quiet ||
   [ -n "$(git ls-files --others --exclude-standard)" ]; then
  current_branch="$(git branch --show-current)"
  staged_tree=""
  remote_tree=""

  if git diff --quiet &&
     [ -z "$(git ls-files --others --exclude-standard)" ]; then
    staged_tree="$(git write-tree)"
    remote_tree="$(git rev-parse "$REMOTE_REF^{tree}")"
  fi

  if [ "$current_branch" != "$PR_BRANCH" ] &&
     [ -n "$staged_tree" ] &&
     [ "$staged_tree" = "$remote_tree" ]; then
    echo
    echo "Знайдено слід перерваного переходу на candidate."
    echo "Staged tree точно збігається з GitHub candidate; відновлюю поточну гілку..."
    git reset --hard HEAD
  else
    echo "У Renault repository є локальні зміни."
    echo "Перехід на тестовий candidate зупинено, щоб нічого не втратити."
    echo
    git status --short
    exit 1
  fi
fi

if git show-ref --verify --quiet "refs/heads/$PR_BRANCH"; then
  git switch "$PR_BRANCH"
else
  git switch -c "$PR_BRANCH" "$REMOTE_REF"
fi

git merge --ff-only "$REMOTE_REF"

VERSION="$(
  awk -F'"' '
    /^[[:space:]]*versionName[[:space:]]*=/ {
      print $2
      exit
    }
  ' android/app/build.gradle.kts
)"

if [ -z "$VERSION" ]; then
  echo "Не вдалося визначити версію candidate."
  exit 1
fi

echo
echo "Candidate готовий локально:"
echo "  branch:  $PR_BRANCH"
echo "  version: v$VERSION"
echo "  commit:  $(git rev-parse HEAD)"
echo
echo "Завантажую exact APK artifact саме для цього commit..."
echo

exec bash "$REPO_DIR/tools/termux/reno-download-apk.sh"
