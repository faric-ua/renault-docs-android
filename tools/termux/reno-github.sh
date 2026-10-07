#!/data/data/com.termux/files/usr/bin/bash

reno_github_repo() {
  if [ -n "${RENAULT_GH_REPO:-}" ]; then
    printf '%s\n' "$RENAULT_GH_REPO"
    return 0
  fi

  local url slug
  url="$(git remote get-url origin 2>/dev/null || true)"

  case "$url" in
    https://github.com/*)
      slug="${url#https://github.com/}"
      ;;
    https://*@github.com/*)
      slug="${url#*@github.com/}"
      ;;
    git@github.com:*)
      slug="${url#git@github.com:}"
      ;;
    ssh://git@github.com/*)
      slug="${url#ssh://git@github.com/}"
      ;;
    *)
      return 1
      ;;
  esac

  slug="${slug%.git}"

  case "$slug" in
    */*)
      printf '%s\n' "$slug"
      ;;
    *)
      return 1
      ;;
  esac
}

reno_require_gh() {
  if ! command -v gh >/dev/null 2>&1; then
    echo "GitHub CLI (gh) не встановлений."
    echo "Виконай: pkg install gh"
    return 1
  fi

  if ! gh auth status -h github.com >/dev/null 2>&1; then
    echo "GitHub CLI не авторизований."
    echo "Виконай: gh auth login -h github.com -p https -w"
    return 1
  fi
}

reno_android_build_compatible() {
  local build_sha="$1"
  local current_sha="${2:-HEAD}"

  if [ -z "$build_sha" ] || [ -z "$current_sha" ]; then
    return 1
  fi

  if ! git cat-file -e "$build_sha^{commit}" 2>/dev/null; then
    return 1
  fi

  if ! git cat-file -e "$current_sha^{commit}" 2>/dev/null; then
    return 1
  fi

  if ! git merge-base --is-ancestor "$build_sha" "$current_sha" 2>/dev/null; then
    return 1
  fi

  git diff --quiet \
    "$build_sha" \
    "$current_sha" \
    -- \
    android \
    .github/workflows/android-debug.yml
}

reno_find_compatible_android_run() {
  local repo="$1"
  local workflow="$2"
  local branch="$3"
  local current_sha="$4"
  local expected_artifact="$5"
  local run_id run_sha artifact_id

  while IFS='|' read -r run_id run_sha; do
    [ -n "$run_id" ] || continue
    [ -n "$run_sha" ] || continue

    if ! reno_android_build_compatible "$run_sha" "$current_sha"; then
      continue
    fi

    artifact_id="$(
      gh api "repos/$repo/actions/runs/$run_id/artifacts?per_page=100" \
        --jq ".artifacts[] | select(.expired == false and .name == \"$expected_artifact\") | .id" \
        2>/dev/null |
        sed -n '1p'
    )"

    if [ -z "$artifact_id" ]; then
      continue
    fi

    printf '%s|%s|%s\n' "$run_id" "$run_sha" "$artifact_id"
    return 0
  done < <(
    gh run list \
      --repo "$repo" \
      --workflow "$workflow" \
      --branch "$branch" \
      --limit 30 \
      --json databaseId,status,conclusion,headSha \
      --jq '.[] | select(.status == "completed" and .conclusion == "success") | "\(.databaseId)|\(.headSha)"' \
      2>/dev/null
  )

  return 1
}

reno_current_commit_tests_ignored() {
  local current_sha="${1:-HEAD}"
  local parent path

  parent="$(git rev-parse "$current_sha^1" 2>/dev/null || true)"
  [ -n "$parent" ] || return 1

  while IFS= read -r path; do
    [ -n "$path" ] || continue

    case "$path" in
      docs/*|*.md)
        ;;
      *)
        return 1
        ;;
    esac
  done < <(
    git diff --name-only "$parent" "$current_sha" 2>/dev/null
  )

  return 0
}

