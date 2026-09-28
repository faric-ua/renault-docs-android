#!/data/data/com.termux/files/usr/bin/bash
set -eu

SECRET_ROOT="$HOME/.renault-secrets/renault-docs-dev"
KEYSTORE="$SECRET_ROOT/renault-docs-dev.jks"
CERT_DER="$SECRET_ROOT/renault-docs-dev.cer"
SECRETS_FILE="$SECRET_ROOT/github-secrets.txt"
LOCAL_ENV_FILE="$SECRET_ROOT/local-build-env.sh"
ALIAS="renault-docs-dev"

if ! command -v keytool >/dev/null 2>&1; then
  echo "keytool не знайдено."
  echo "Встановіть Java у Termux (наприклад: pkg install openjdk-17) і повторіть."
  exit 1
fi

if [ -e "$SECRET_ROOT" ]; then
  echo "Каталог уже існує: $SECRET_ROOT"
  echo "Щоб не перезаписати чинний ключ, скрипт зупинено."
  exit 1
fi

mkdir -p "$SECRET_ROOT"
chmod 700 "$SECRET_ROOT"

PASSWORD="$(od -An -N32 -tx1 /dev/urandom | tr -d ' \n')"

keytool -genkeypair   -keystore "$KEYSTORE"   -storetype JKS   -storepass "$PASSWORD"   -keypass "$PASSWORD"   -alias "$ALIAS"   -keyalg RSA   -keysize 3072   -validity 3650   -dname "CN=Renault Docs Development, OU=Development, O=faric-ua, C=UA"   >/dev/null 2>&1

keytool -exportcert   -keystore "$KEYSTORE"   -storepass "$PASSWORD"   -alias "$ALIAS"   -file "$CERT_DER"   >/dev/null 2>&1

CERT_SHA256="$(sha256sum "$CERT_DER" | awk '{print $1}')"
KEYSTORE_B64="$(base64 "$KEYSTORE" | tr -d '\n')"

cat > "$SECRETS_FILE" <<EOF
RENAULT_DEV_KEYSTORE_B64=$KEYSTORE_B64
RENAULT_DEV_STORE_PASSWORD=$PASSWORD
RENAULT_DEV_KEY_ALIAS=$ALIAS
RENAULT_DEV_KEY_PASSWORD=$PASSWORD
RENAULT_DEV_CERT_SHA256=$CERT_SHA256
EOF

cat > "$LOCAL_ENV_FILE" <<EOF
export RENAULT_DEV_KEYSTORE_PATH='$KEYSTORE'
export RENAULT_DEV_STORE_PASSWORD='$PASSWORD'
export RENAULT_DEV_KEY_ALIAS='$ALIAS'
export RENAULT_DEV_KEY_PASSWORD='$PASSWORD'
export RENAULT_DEV_CERT_SHA256='$CERT_SHA256'
EOF

chmod 600 "$KEYSTORE" "$CERT_DER" "$SECRETS_FILE" "$LOCAL_ENV_FILE"

echo
echo "Новий development signer створено."
echo "Keystore: $KEYSTORE"
echo "GitHub Secrets: $SECRETS_FILE"
echo "Local build env: $LOCAL_ENV_FILE"
echo "Certificate SHA-256: $CERT_SHA256"
echo
echo "Не копіюйте ці файли в Git."
echo "Наступний крок: створити зашифрований повний backup історії."
