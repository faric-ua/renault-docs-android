#!/data/data/com.termux/files/usr/bin/bash
set -eu

REPO="${1:-faric-ua/renault-docs-android}"
SECRETS_FILE="${2:-$HOME/.renault-secrets/renault-docs-dev/github-secrets.txt}"
VERIFY_REF="${3:-chore/public-readiness-signing-20260928}"

if ! command -v gh >/dev/null 2>&1; then
  echo "GitHub CLI (gh) не знайдено."
  echo "Встановіть: pkg install gh"
  exit 1
fi

if ! gh auth status -h github.com >/dev/null 2>&1; then
  echo "GitHub CLI ще не авторизований."
  echo "Запустіть: gh auth login -h github.com -p https -w"
  exit 1
fi

if [ ! -f "$SECRETS_FILE" ]; then
  echo "Не знайдено secrets file: $SECRETS_FILE"
  exit 1
fi

EXPECTED_KEYS="RENAULT_DEV_KEYSTORE_B64 RENAULT_DEV_STORE_PASSWORD RENAULT_DEV_KEY_ALIAS RENAULT_DEV_KEY_PASSWORD RENAULT_DEV_CERT_SHA256"

for key in $EXPECTED_KEYS; do
  line="$(grep -m1 "^${key}=" "$SECRETS_FILE" || true)"
  if [ -z "$line" ]; then
    echo "Відсутнє значення: $key"
    exit 1
  fi
  value="${line#*=}"
  if [ -z "$value" ]; then
    echo "Порожнє значення: $key"
    exit 1
  fi
  printf "%s" "$value" | gh secret set "$key" --repo "$REPO"
  echo "OK: $key"
done

echo
echo "Перевіряю лише імена secrets (значення GitHub не повертає):"
gh secret list --repo "$REPO" | grep -E "^RENAULT_DEV_" || true

echo
echo "Запускаю перевірочну Android Debug APK збірку на migration ref:"
echo "  $VERIFY_REF"
gh workflow run "Android Debug APK" --repo "$REPO" --ref "$VERIFY_REF"

echo
echo "PASS: secrets передані GitHub без виведення їх значень."
echo "Workflow dispatch відправлено. Через кілька секунд перевірте:"
echo "  gh run list --repo \"$REPO\" --workflow \"Android Debug APK\" --limit 3"
