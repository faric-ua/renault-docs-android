#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

REPO_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
# shellcheck source=reno-github.sh
source "$REPO_DIR/tools/termux/reno-github.sh"

SOURCE_REPO="faric-ua/renault-docs-android-private-archive"
SOURCE_REF="3a8cc7dc1c7047917bc543221890bfaecfb8a821"
SOURCE_KEY_PATH=".github/signing/renault-docs-dev.jks.b64"
SOURCE_GRADLE_PATH="android/app/build.gradle.kts"
EXPECTED_KEYSTORE_SHA256="7944e7d78bd2442021731a4cfd3105c06f475c13d70d4195b2378539604dfd10"
EXPECTED_CERT_SHA256="dd588fbb3093a047f81c24397fc6d2ab7f5a32040425a21dd24506cce59a9802"

cd "$REPO_DIR"

reno_require_gh || exit 1
TARGET_REPO="$(reno_github_repo)" || {
  echo "Не вдалося визначити активний Renault repository."
  exit 1
}

BRANCH="$(git branch --show-current)"
if [ "$BRANCH" != "main" ]; then
  echo "Зараз гілка: $BRANCH"
  echo "Для відновлення підпису спочатку повернись у Renault Menu → 17, потім → 5."
  exit 1
fi

if ! git diff --quiet ||
   ! git diff --cached --quiet ||
   [ -n "$(git ls-files --others --exclude-standard)" ]; then
  echo "Є локальні незбережені зміни. Зупинено."
  git status --short
  exit 1
fi

echo "Renault Docs · відновлення accepted development signer"
echo
echo "Джерело: приватний історичний archive"
echo "Ціль:    $TARGET_REPO"
echo
echo "Секретні значення на екран НЕ виводяться."
echo

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

fetch_private_text() {
  local repo="$1"
  local path="$2"
  local ref="$3"
  local out="$4"

  gh api "repos/$repo/contents/$path?ref=$ref"     --jq '.content'     | tr -d '\n'     | base64 -d > "$out"

  test -s "$out"
}

echo "1/6 · Отримую старий accepted key з private archive..."
fetch_private_text "$SOURCE_REPO" "$SOURCE_KEY_PATH" "$SOURCE_REF" "$TMP_DIR/key.b64"
base64 -d "$TMP_DIR/key.b64" > "$TMP_DIR/renault-docs-dev.jks"
chmod 600 "$TMP_DIR/renault-docs-dev.jks"

KEYSTORE_SHA="$(sha256sum "$TMP_DIR/renault-docs-dev.jks" | awk '{print $1}')"
if [ "$KEYSTORE_SHA" != "$EXPECTED_KEYSTORE_SHA256" ]; then
  echo "FAIL: keystore SHA-256 не збігається."
  exit 1
fi
echo "PASS · keystore identity підтверджена."

echo "2/6 · Отримую старі signing parameters з private archive..."
fetch_private_text "$SOURCE_REPO" "$SOURCE_GRADLE_PATH" "$SOURCE_REF" "$TMP_DIR/old-build.gradle.kts"

STORE_PASSWORD="$(
  sed -n 's/.*storePassword = "\([^"]*\)".*/\1/p' "$TMP_DIR/old-build.gradle.kts" | sed -n '1p'
)"
KEY_ALIAS="$(
  sed -n 's/.*keyAlias = "\([^"]*\)".*/\1/p' "$TMP_DIR/old-build.gradle.kts" | sed -n '1p'
)"
KEY_PASSWORD="$(
  sed -n 's/.*keyPassword = "\([^"]*\)".*/\1/p' "$TMP_DIR/old-build.gradle.kts" | sed -n '1p'
)"

test -n "$STORE_PASSWORD"
test -n "$KEY_ALIAS"
test -n "$KEY_PASSWORD"

echo "3/6 · Перевіряю certificate identity..."
keytool -exportcert   -keystore "$TMP_DIR/renault-docs-dev.jks"   -storepass "$STORE_PASSWORD"   -alias "$KEY_ALIAS"   -file "$TMP_DIR/cert.der" >/dev/null 2>&1

CERT_SHA="$(sha256sum "$TMP_DIR/cert.der" | awk '{print $1}')"
if [ "$CERT_SHA" != "$EXPECTED_CERT_SHA256" ]; then
  echo "FAIL: certificate SHA-256 не збігається."
  exit 1
fi
echo "PASS · accepted certificate:"
echo "  $CERT_SHA"

echo "4/6 · Оновлюю GitHub Actions Secrets у public repository..."
KEYSTORE_B64="$(base64 "$TMP_DIR/renault-docs-dev.jks" | tr -d '\n')"

printf '%s' "$KEYSTORE_B64" | gh secret set RENAULT_DEV_KEYSTORE_B64 --repo "$TARGET_REPO"
printf '%s' "$STORE_PASSWORD" | gh secret set RENAULT_DEV_STORE_PASSWORD --repo "$TARGET_REPO"
printf '%s' "$KEY_ALIAS" | gh secret set RENAULT_DEV_KEY_ALIAS --repo "$TARGET_REPO"
printf '%s' "$KEY_PASSWORD" | gh secret set RENAULT_DEV_KEY_PASSWORD --repo "$TARGET_REPO"
printf '%s' "$EXPECTED_CERT_SHA256" | gh secret set RENAULT_DEV_CERT_SHA256 --repo "$TARGET_REPO"

unset KEYSTORE_B64 STORE_PASSWORD KEY_ALIAS KEY_PASSWORD

echo "PASS · GitHub Secrets оновлено без виведення значень."

echo "5/6 · Перевіряю, що main актуальний..."
git fetch --prune origin "+refs/heads/main:refs/remotes/origin/main"
LOCAL_SHA="$(git rev-parse HEAD)"
REMOTE_SHA="$(git rev-parse refs/remotes/origin/main)"
if [ "$LOCAL_SHA" != "$REMOTE_SHA" ]; then
  echo "Local main ще не актуальний."
  echo "Зроби Renault Menu → 5 і повтори цей пункт."
  echo "local:  $LOCAL_SHA"
  echo "remote: $REMOTE_SHA"
  exit 1
fi

echo "PASS · main синхронізований."

echo "6/6 · Запускаю exact public-main build з відновленим accepted signer..."
echo
exec bash "$REPO_DIR/tools/termux/reno-build-apk.sh"
