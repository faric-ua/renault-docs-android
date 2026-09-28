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
