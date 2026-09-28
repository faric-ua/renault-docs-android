#!/data/data/com.termux/files/usr/bin/bash
set -eu

if ! command -v git >/dev/null 2>&1; then
  echo "git не знайдено."
  exit 1
fi

if ! command -v openssl >/dev/null 2>&1; then
  echo "openssl не знайдено."
  echo "Встановіть його: pkg install openssl-tool"
  exit 1
fi

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || true)"
if [ -z "$REPO_ROOT" ]; then
  echo "Запустіть скрипт усередині клонованого renault-docs-android."
  exit 1
fi

cd "$REPO_ROOT"

BACKUP_ROOT="${1:-/storage/emulated/0/Documents/Renault/backups}"
STAMP="$(date +%Y%m%d-%H%M%S)"
ARCHIVE="$BACKUP_ROOT/renault-docs-private-history-$STAMP.tar.enc"
CHECKSUM="$ARCHIVE.sha256"
TMP_ROOT="$(mktemp -d "${TMPDIR:-$PREFIX/tmp}/renault-public-backup.XXXXXX")"

cleanup() {
  rm -rf "$TMP_ROOT"
}
trap cleanup EXIT INT TERM

mkdir -p "$BACKUP_ROOT" 2>/dev/null || {
  echo "Немає доступу до $BACKUP_ROOT"
  echo "У Termux виконайте termux-setup-storage і повторіть."
  exit 1
}

echo "Оновлюю всі remote refs перед backup..."
git fetch origin   '+refs/heads/*:refs/remotes/origin/*'   '+refs/tags/*:refs/tags/*'   --prune

echo "Створюю повний Git bundle..."
git bundle create "$TMP_ROOT/renault-docs-full-history.bundle" --all
git bundle verify "$TMP_ROOT/renault-docs-full-history.bundle"   > "$TMP_ROOT/bundle-verify.txt" 2>&1

git for-each-ref   --format='%(refname)%09%(objectname)%09%(creatordate:iso-strict)'   refs/heads refs/remotes refs/tags   > "$TMP_ROOT/all-refs.tsv"

git log --all --date=iso-strict   --pretty=format:'%H%x09%P%x09%ad%x09%an%x09%ae%x09%d%x09%s'   > "$TMP_ROOT/all-commits.tsv"

git status --short --branch > "$TMP_ROOT/status.txt"
git branch -a -vv > "$TMP_ROOT/branches.txt"
git tag -n > "$TMP_ROOT/tags.txt" 2>/dev/null || true
git fsck --full --no-reflogs > "$TMP_ROOT/fsck.txt" 2>&1 || true

git archive --format=tar HEAD | gzip -9 > "$TMP_ROOT/current-worktree.tar.gz"

ORIGIN_URL="$(git remote get-url origin 2>/dev/null || true)"
printf '%s\n' "$ORIGIN_URL"   | sed -E 's#https://[^/@]+:[^/@]+@github.com/#https://github.com/#'   | sed -E 's#https://[^/@]+@github.com/#https://github.com/#'   > "$TMP_ROOT/origin-url.txt"

if [ -d "$HOME/.renault-secrets/renault-docs-dev" ]; then
  mkdir -p "$TMP_ROOT/new-signing-material"
  cp -a "$HOME/.renault-secrets/renault-docs-dev/."     "$TMP_ROOT/new-signing-material/"
fi

sha256sum "$TMP_ROOT/renault-docs-full-history.bundle"   "$TMP_ROOT/current-worktree.tar.gz"   > "$TMP_ROOT/payload-sha256.txt"

echo
read -r -s -p "Пароль для шифрованого backup: " BACKUP_PASS
echo
read -r -s -p "Повторіть пароль: " BACKUP_PASS_2
echo

if [ -z "$BACKUP_PASS" ] || [ "$BACKUP_PASS" != "$BACKUP_PASS_2" ]; then
  echo "Паролі не збігаються або пароль порожній."
  exit 1
fi
unset BACKUP_PASS_2

echo "Шифрую backup..."
tar -C "$TMP_ROOT" -cf - .   | BACKUP_PASS="$BACKUP_PASS" openssl enc       -aes-256-cbc       -pbkdf2       -iter 200000       -salt       -pass env:BACKUP_PASS       -out "$ARCHIVE"

echo "Перевіряю, що backup розшифровується..."
BACKUP_PASS="$BACKUP_PASS" openssl enc   -d   -aes-256-cbc   -pbkdf2   -iter 200000   -in "$ARCHIVE"   -pass env:BACKUP_PASS   | tar -tf - >/dev/null

unset BACKUP_PASS

sha256sum "$ARCHIVE" > "$CHECKSUM"

echo
echo "PASS: повний encrypted backup створено."
echo "Archive: $ARCHIVE"
echo "SHA-256: $CHECKSUM"
echo
echo "Архів містить:"
echo "  - повний git bundle з усіма refs;"
echo "  - список усіх гілок/ref/commit;"
echo "  - поточний worktree snapshot;"
echo "  - новий signing material, якщо його вже згенеровано."
echo
echo "Пароль зберігайте окремо. Без нього архів відновити неможливо."
