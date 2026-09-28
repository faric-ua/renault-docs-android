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

if [ -z "$RUN_ID" ]; then
  echo "GitHub: $GH_REPO"
  echo "Поточна версія коду: v$VERSION"
  echo "Поточний commit: $HEAD_SHA"
  echo "Шукаю успішний Android build саме для цього commit..."
  echo

  RUN_ID="$(
    gh api \
      "repos/$GH_REPO/actions/workflows/$WORKFLOW/runs?head_sha=$HEAD_SHA&status=success&per_page=20" \
      --jq '.workflow_runs[0].id // empty'
  )"
fi

if [ -z "$RUN_ID" ]; then
  echo "Не знайдено успішного APK build для поточного commit:"
  echo "  $HEAD_SHA"
  echo
  echo "Старішу версію або artifact з іншої гілки автоматично НЕ завантажую."
  echo "Запусти Renault Menu → 7 для нового build."
  exit 1
fi

RUN_INFO="$(
  gh api "repos/$GH_REPO/actions/runs/$RUN_ID" \
    --jq '[.head_sha, .head_branch, .conclusion, .event] | @tsv'
)"

IFS="$(printf '\t')" read -r RUN_SHA RUN_BRANCH RUN_CONCLUSION RUN_EVENT <<EOF
$RUN_INFO
EOF

if [ "$RUN_SHA" != "$HEAD_SHA" ]; then
  echo "STOP: Run ID $RUN_ID належить іншому commit."
  echo "Поточний: $HEAD_SHA"
  echo "Run:      $RUN_SHA"
  exit 1
fi

if [ "$RUN_CONCLUSION" != "success" ]; then
  echo "STOP: Run ID $RUN_ID не має conclusion=success."
  echo "Conclusion: $RUN_CONCLUSION"
  exit 1
fi

ARTIFACT_ID="$(
  gh api "repos/$GH_REPO/actions/runs/$RUN_ID/artifacts?per_page=100" \
    --jq ".artifacts[] | select(.expired == false and .name == \"$EXPECTED_ARTIFACT\") | .id" \
    | sed -n '1p'
)"

if [ -z "$ARTIFACT_ID" ]; then
  echo "У точному run не знайдено очікуваний artifact:"
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
echo "Commit:   $RUN_SHA"
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
