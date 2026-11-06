#!/data/data/com.termux/files/usr/bin/bash
# 🔑 Покласти ключ підпису в «Ключі» (faric 2026-11-06; інспектор GitHub Storage).
#
# Той самий файл у всіх репо faric-ua; у кожному репо в таблиці лише його рядок.
# Виклик: faric-keys-put.sh "<Назва>".
# КОПІЮЄ ключ цього застосунку з Termux у ~/Ключі/<Назва>/ разом з паролями,
# відбитком і контрольними сумами — формат як у YTM (меню YTM 8 → 3 / 8 → 4).
# Оригінал ключа не чіпає. Нічого не видаляє: якщо в папці вже інший ключ,
# новий кладе поруч у «<Назва> (новий <дата>)». Нічого не відправляє в інтернет.
set -uo pipefail

KEYS_DIR="${FARIC_KEYS_DIR:-$HOME/Ключі}"
NAME="${1:-}"

# Назва | repo | файл ключа | файл паролів | вид | назва ключа | секрети GitHub (keystore store key) | пакети на телефоні
app_row() {
  case "$1" in
    "Renault Docs") echo "renault-docs-android|$HOME/.renault-secrets/renault-docs-dev/renault-docs-dev.jks|$HOME/.renault-secrets/renault-docs-dev/local-build-env.sh|env:RENAULT_DEV|renault-docs-dev|RENAULT_DEV_KEYSTORE_B64 RENAULT_DEV_STORE_PASSWORD RENAULT_DEV_KEY_PASSWORD (+ RENAULT_DEV_KEY_ALIAS, RENAULT_DEV_CERT_SHA256)|com.farsan.renodocs" ;;
    *) return 1 ;;
  esac
}

fail() { echo; echo "❌ $*"; echo "Надішли скрін Claude."; exit 1; }

# значення з файлу паролів: props (key=value) або env (export PREFIX_X='...')
prop() { sed -n "s/^$2=//p" "$1" | head -n 1; }
envv() { sed -n "s/^export $2='\(.*\)'\$/\1/p; s/^$2=//p" "$1" | head -n 1; }

# SHA-256 сертифіката з підписаного APK (схема v2/v3) — без Android SDK.
apk_cert() {
  command -v python3 >/dev/null 2>&1 || return 1
  python3 - "$1" <<'PY' 2>/dev/null
import sys, struct, hashlib
d = open(sys.argv[1], 'rb').read()
eocd = d.rfind(b'PK\x05\x06'); cd = struct.unpack('<I', d[eocd+16:eocd+20])[0]
assert d[cd-16:cd] == b'APK Sig Block 42'
size = struct.unpack('<Q', d[cd-24:cd-16])[0]; pos = cd - size; end = cd - 24
def lp(b, p):
    n = struct.unpack('<I', b[p:p+4])[0]; return b[p+4:p+4+n], p+4+n
while pos < end:
    n, ident = struct.unpack('<QI', d[pos:pos+12]); val = d[pos+12:pos+8+n]; pos += 8+n
    if ident not in (0x7109871a, 0xf05368c0): continue
    signers, _ = lp(val, 0); signer, _ = lp(signers, 0); signed, _ = lp(signer, 0)
    _, q = lp(signed, 0); certs, _ = lp(signed, q); c, _ = lp(certs, 0)
    print(hashlib.sha256(c).hexdigest()); break
PY
}

main() {
  local row repo key pwfile kind alias secrets pkgs sp kp al kfp="" check="" when dest stamp tmp p apk afp
  [ -n "$NAME" ] || fail "Не вказано назву застосунку."
  row="$(app_row "$NAME")" || fail "Не знаю застосунку «$NAME»."
  IFS='|' read -r repo key pwfile kind alias secrets pkgs <<< "$row"
  echo "🔑 $NAME — покласти копію ключа в «Ключі»"
  echo
  [ -f "$key" ] || fail "Ключа нема на цьому телефоні: ${key/#$HOME/~}"
  case "$kind" in
    props) [ -r "$pwfile" ] || fail "Нема файла паролів: ${pwfile/#$HOME/~}"
           sp="$(prop "$pwfile" storePassword)"; kp="$(prop "$pwfile" keyPassword)"; al="$(prop "$pwfile" keyAlias)" ;;
    env:*) [ -r "$pwfile" ] || fail "Нема файла паролів: ${pwfile/#$HOME/~}"
           p="${kind#env:}"; sp="$(envv "$pwfile" "${p}_STORE_PASSWORD")"; kp="$(envv "$pwfile" "${p}_KEY_PASSWORD")"; al="$(envv "$pwfile" "${p}_KEY_ALIAS")" ;;
    fixed:*) sp="${kind#fixed:}"; kp="$sp" ;;
  esac
  al="${al:-$alias}"
  [ -n "$sp" ] && [ -n "$kp" ] || fail "У файлі паролів бракує паролів."
  echo "✓ Ключ і паролі знайдено"

  # Перевірка паролів і відбиток ключа (потрібен keytool — Java в Termux).
  if command -v keytool >/dev/null 2>&1; then
    kfp="$(keytool -list -v -keystore "$key" -storepass "$sp" -alias "$al" 2>/dev/null | sed -n 's/^[[:space:]]*SHA256: //p' | tr -d ':' | tr 'A-F' 'a-f' | head -n 1)"
    [ -n "$kfp" ] || fail "Пароль файлу або назва ключа не підходять — ключ НЕ скопійовано."
    tmp="$(mktemp -d)"
    if keytool -importkeystore -noprompt -srckeystore "$key" -srcstorepass "$sp" -srcalias "$al" -srckeypass "$kp" \
         -destkeystore "$tmp/t.p12" -deststoretype pkcs12 -deststorepass check123 -destkeypass check123 >/dev/null 2>&1; then
      echo "✓ Обидва паролі підходять"
    else
      rm -f "$tmp/t.p12"; rmdir "$tmp"; fail "Пароль ключа не підходить — ключ НЕ скопійовано."
    fi
    rm -f "$tmp/t.p12"; rmdir "$tmp"
  else
    echo "• keytool нема — паролі не перевірено"
  fi

  # Порівняти з установленим застосунком на цьому телефоні.
  for p in $pkgs; do
    apk="$(pm path "$p" 2>/dev/null | sed -n 's/^package://p' | grep -m1 'base.apk')"
    [ -n "$apk" ] && [ -r "$apk" ] || continue
    afp="$(apk_cert "$apk")" || continue
    [ -n "$afp" ] || continue
    if [ -n "$kfp" ] && [ "$afp" = "$kfp" ]; then check="✓ збігається з установленим $p"; break; fi
    [ -n "$kfp" ] && check="✗ НЕ збігається з установленим $p (там ${afp:0:16}…)"
  done
  if [ -z "$check" ]; then
    if [ -n "$kfp" ]; then check="не перевірено: застосунку на телефоні нема або нема python3"; else check="не перевірено: нема keytool"; fi
  fi
  echo "• Перевірка: $check"

  # Куди класти: не перезаписувати інший ключ.
  stamp="$(date '+%d.%m.%Y %H:%M')"
  dest="$KEYS_DIR/$NAME"
  mkdir -p "$KEYS_DIR"; chmod 700 "$KEYS_DIR"
  if [ -f "$dest/$(basename "$key")" ]; then
    if cmp -s "$dest/$(basename "$key")" "$key"; then
      echo; echo "✓ Цей самий ключ уже лежить у «Ключі/$NAME» — нічого не міняв."; return 0
    fi
    dest="$KEYS_DIR/$NAME (новий $(date '+%d.%m.%Y'))"
    echo "⚠️ У «Ключі/$NAME» вже ІНШИЙ ключ — новий кладу поруч: «$(basename "$dest")». Скажи Claude."
  fi
  mkdir -p "$dest"; chmod 700 "$dest"
  cp -p "$key" "$dest/"
  {
    echo "Застосунок: $NAME (faric-ua/$repo)"
    echo "Файл ключа: $(basename "$key")"
    echo "Пароль файлу (storePassword): $sp"
    echo "Назва ключа (keyAlias): $al"
    echo "Пароль ключа (keyPassword): $kp"
    echo
    echo "Секрети GitHub для збірки (Settings → Secrets and variables → Actions):"
    echo "  $secrets"
    echo "  (файл ключа в base64: base64 -w0 $(basename "$key"))"
  } > "$dest/паролі.txt"
  {
    echo "SHA-256 сертифіката: ${kfp:-невідомо (нема keytool)}"
    echo "Перевірка з релізом: $check"
    echo "Скопійовано з телефона: $stamp, з ${key/#$HOME/~}"
  } > "$dest/відбиток.txt"
  (cd "$dest" && sha256sum "$(basename "$key")" паролі.txt відбиток.txt > контроль.sha256)
  chmod 600 "$dest"/*
  echo
  echo "✓ Готово: Ключі/$(basename "$dest")"
  echo "  $(basename "$key") · паролі.txt · відбиток.txt · контроль.sha256"
  echo "  Відбиток: ${kfp:0:16}…"
  echo "Оригінал ключа лишився на місці. Подивитись усі ключі — меню YTM → 8 → 3."
}

main
