#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

REPO_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
WORKFLOW="android-debug.yml"
RUN_ID="${1:-}"
TMP="$HOME/renault-apk-artifact"

# shellcheck source=reno-github.sh
source "$REPO_DIR/tools/termux/reno-github.sh"

cd "$REPO_DIR"

reno_require_gh || exit 1

GH_REPO="$(reno_github_repo)" || {
  echo "Не вдалося визначити GitHub repository з origin."
  echo "Перевір: git remote -v"
  exit 1
}

VERSION="$(
  awk -F'"' '
    /^[[:space:]]*versionName[[:space:]]*=/ {
      print $2
      exit
    }
  ' android/app/build.gradle.kts
)"

if [ -z "$VERSION" ]; then
  echo "Не вдалося визначити versionName з android/app/build.gradle.kts."
  exit 1
fi

HEAD_SHA="$(git rev-parse HEAD)"
BRANCH="$(git branch --show-current)"
EXPECTED_ARTIFACT="Renault-Docs-v${VERSION}-Debug"
COMPATIBLE_ARTIFACT_ID=""

if [ -z "$RUN_ID" ]; then
  echo "GitHub: $GH_REPO"
  echo "Поточна версія коду: v$VERSION"
  echo "Поточний commit: $HEAD_SHA"
  echo "Шукаю безпечний APK для поточного Android-коду..."
  echo

  COMPATIBLE="$(
    reno_find_compatible_android_run \
      "$GH_REPO" \
      "$WORKFLOW" \
      "$BRANCH" \
      "$HEAD_SHA" \
      "$EXPECTED_ARTIFACT" \
      2>/dev/null || true
  )"

  if [ -n "$COMPATIBLE" ]; then
    IFS='|' read -r RUN_ID COMPATIBLE_SHA COMPATIBLE_ARTIFACT_ID <<EOF
$COMPATIBLE
EOF
  fi
fi

if [ -z "$RUN_ID" ]; then
  # Actions keeps APK artifacts only briefly. Prefer a reviewed GitHub Release
  # as a safe fallback, but never substitute a different Android build.
  if [ "$BRANCH" = "main" ] && bash     "$REPO_DIR/tools/termux/reno-download-release-apk.sh"     "$VERSION" "$HEAD_SHA" "$GH_REPO"; then
    exit 0
  fi
  echo "Не знайдено безпечного APK artifact для поточного Android-коду."
  echo
  echo "Старішу версію з іншим Android-кодом або artifact з іншої гілки НЕ завантажую."
  echo "Немає сумісного GitHub Release. Запусти Renault Menu → 7 для нового build."
  exit 1
fi

RUN_INFO="$(
  gh api "repos/$GH_REPO/actions/runs/$RUN_ID" \
    --jq '[.head_sha, .head_branch, .conclusion, .event] | @tsv'
)"

IFS="$(printf '\t')" read -r RUN_SHA RUN_BRANCH RUN_CONCLUSION RUN_EVENT <<EOF
$RUN_INFO
EOF

if [ "$RUN_BRANCH" != "$BRANCH" ]; then
  echo "STOP: APK build належить іншій гілці."
  echo "Поточна: $BRANCH"
  echo "Build:    $RUN_BRANCH"
  exit 1
fi

if [ "$RUN_SHA" != "$HEAD_SHA" ]; then
  if ! reno_android_build_compatible "$RUN_SHA" "$HEAD_SHA"; then
    echo "STOP: APK build має інший Android-код."
    echo "Поточний commit: $HEAD_SHA"
    echo "Build commit:    $RUN_SHA"
    exit 1
  fi

  echo "✓ Використовую попередній build з тим самим Android-кодом."
  echo "  Build commit: $RUN_SHA"
  echo "  Current:      $HEAD_SHA"
  echo
fi

if [ "$RUN_CONCLUSION" != "success" ]; then
  echo "STOP: Run ID $RUN_ID не має conclusion=success."
  echo "Conclusion: $RUN_CONCLUSION"
  exit 1
fi

ARTIFACT_ID="$COMPATIBLE_ARTIFACT_ID"

if [ -z "$ARTIFACT_ID" ]; then
  ARTIFACT_ID="$(
    gh api "repos/$GH_REPO/actions/runs/$RUN_ID/artifacts?per_page=100" \
      --jq ".artifacts[] | select(.expired == false and .name == \"$EXPECTED_ARTIFACT\") | .id" \
      | sed -n '1p'
  )"
fi

if [ -z "$ARTIFACT_ID" ]; then
  echo "У вибраному run не знайдено очікуваний не прострочений artifact:"
  echo "  $EXPECTED_ARTIFACT"
  echo "Run ID: $RUN_ID"
  exit 1
fi

rm -rf "$TMP"
mkdir -p "$TMP"

echo "Renault Docs"
echo "GitHub:   $GH_REPO"
echo "Версія:  v$VERSION"
echo "Branch:   ${RUN_BRANCH:-$BRANCH}"
echo "Build commit: $RUN_SHA"
if [ "$RUN_SHA" != "$HEAD_SHA" ]; then
  echo "Current commit: $HEAD_SHA"
  echo "Android-код:    без змін"
fi
echo "Artifact: $EXPECTED_ARTIFACT"
echo "Run ID:   $RUN_ID"
echo

gh run download "$RUN_ID" \
  --repo "$GH_REPO" \
  --name "$EXPECTED_ARTIFACT" \
  --dir "$TMP"

APK="$(find "$TMP" -type f -name "Renault-Docs-v${VERSION}-debug.apk" -print -quit)"
if [ -z "$APK" ] || [ ! -f "$APK" ]; then
  echo "Очікуваний APK не знайдено після завантаження:"
  echo "  Renault-Docs-v${VERSION}-debug.apk"
  exit 1
fi

SHA="$APK.sha256"
if [ ! -f "$SHA" ]; then
  echo "SHA-256 файл не знайдено:"
  echo "  $SHA"
  exit 1
fi

APK_NAME="$(basename "$APK")"
PHONE_DIR="/storage/emulated/0/Documents/Renault/packages/Renault-Docs-v${VERSION}-build"

echo "Перевіряю SHA-256..."
(
  cd "$(dirname "$APK")"
  sha256sum -c "$(basename "$SHA")"
)

mkdir -p "$PHONE_DIR"
cp -f "$APK" "$SHA" "$PHONE_DIR/"
sync

echo
echo "Перевіряю копію в Renault/packages..."
(
  cd "$PHONE_DIR"
  sha256sum -c "$APK_NAME.sha256"
)

if command -v termux-media-scan >/dev/null 2>&1; then
  termux-media-scan "$PHONE_DIR/$APK_NAME" >/dev/null 2>&1 || true
fi

echo
echo "========================================"
echo "READY · v$VERSION"
echo "========================================"
echo "Папка:"
echo "  $PHONE_DIR"
echo
echo "APK:"
echo "  $PHONE_DIR/$APK_NAME"
echo
ls -lh "$PHONE_DIR"

echo
echo "Відкриваю папку з APK..."
bash "$REPO_DIR/tools/termux/reno-open-latest-apk.sh" "$PHONE_DIR" || true
