#!/data/data/com.termux/files/usr/bin/bash
# Reuse only a reviewed, signed GitHub prerelease with exactly matching Android source.
# Returns nonzero if no compatible release exists; caller can keep its current behavior.
set -euo pipefail

REPO_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
VERSION="${1:?versionName missing}"
HEAD_SHA="${2:?local HEAD SHA missing}"
GH_REPO="${3:?repo missing}"
TAG="v${VERSION}-debug"
APK_NAME="Renault-Docs-v${VERSION}-debug.apk"
PROMOTION="$REPO_DIR/docs/release-promotions/v${VERSION}.json"
TMP="$HOME/renault-release-apk"

if [ "$GH_REPO" != "faric-ua/renault-docs-android" ]; then
  echo "Немає перевіреного Release для іншого GitHub repository." >&2
  exit 1
fi
if [ ! -f "$PROMOTION" ]; then
  echo "Немає перевіреного promotion manifest: v$VERSION" >&2
  exit 1
fi

# shellcheck source=reno-github.sh
source "$REPO_DIR/tools/termux/reno-github.sh"
RELEASE_METADATA="$(
  gh api "repos/$GH_REPO/releases/tags/$TAG" \
    --jq '[.target_commitish, (.prerelease|tostring), (.draft|tostring)] | @tsv' \
    2>/dev/null || true
)"
if [ -z "$RELEASE_METADATA" ]; then
  echo "Підписаний Release v$VERSION поки відсутній." >&2
  exit 1
fi
IFS="$(printf '\t')" read -r RELEASE_SHA IS_PRERELEASE IS_DRAFT <<EOF
$RELEASE_METADATA
EOF
if [ "$IS_PRERELEASE" != "true" ] || [ "$IS_DRAFT" != "false" ]; then
  echo "Release має неправильний статус." >&2
  exit 1
fi
PROMOTION_SHA="$(
  python - "$PROMOTION" <<'PY'
import json
import sys
with open(sys.argv[1], encoding="utf-8") as source:
    data = json.load(source)
assert data["prerelease"] is True
assert data["tag"] == "v" + data["version"] + "-debug"
print(data["source_sha"])
PY
)"
if [ "$RELEASE_SHA" != "$PROMOTION_SHA" ]; then
  echo "Release SHA не збігається із зафіксованою підписаною збіркою." >&2
  exit 1
fi
if ! reno_android_build_compatible "$RELEASE_SHA" "$HEAD_SHA"; then
  echo "STOP: Release APK має інший Android-код; стару версію не завантажую." >&2
  exit 1
fi
ASSETS="$(
  gh api "repos/$GH_REPO/releases/tags/$TAG" --jq '.assets[].name'
)"
if ! printf '%s\n' "$ASSETS" | grep -Fxq "$APK_NAME" ||
   ! printf '%s\n' "$ASSETS" | grep -Fxq "$APK_NAME.sha256"; then
  echo "Release не має пари APK + SHA256." >&2
  exit 1
fi

mkdir -p "$TMP"
# Remove only our scratch files, not installed app/data or archives.
rm -f "$TMP/$APK_NAME" "$TMP/$APK_NAME.sha256"
gh release download "$TAG" --repo "$GH_REPO" \
  --pattern "$APK_NAME" \
  --pattern "$APK_NAME.sha256" \
  --dir "$TMP"

(
  cd "$TMP"
  sha256sum -c "$APK_NAME.sha256"
)
PHONE_DIR="/storage/emulated/0/Documents/Renault/packages/Renault-Docs-v${VERSION}-build"
mkdir -p "$PHONE_DIR"
cp -f "$TMP/$APK_NAME" "$TMP/$APK_NAME.sha256" "$PHONE_DIR/"
sync
(
  cd "$PHONE_DIR"
  sha256sum -c "$APK_NAME.sha256"
)
if command -v termux-media-scan >/dev/null 2>&1; then
  termux-media-scan "$PHONE_DIR/$APK_NAME" >/dev/null 2>&1 || true
fi
echo
echo "READY · v$VERSION · перевірений GitHub Release"
echo "Файл: $PHONE_DIR/$APK_NAME"
echo "Підписаний вихідний commit: $RELEASE_SHA"
bash "$REPO_DIR/tools/termux/reno-open-latest-apk.sh" "$PHONE_DIR" || true
