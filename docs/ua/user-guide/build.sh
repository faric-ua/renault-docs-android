#!/usr/bin/env bash
# Rebuilds the user guide in every format from build_guide.py (one source):
#   docs/ua/USER_GUIDE.md            — Markdown with Mermaid (GitHub)
#   docs/ua/Posibnyk-Renault-Docs.docx — Word with diagram pictures
#   docs/ua/Posibnyk-Renault-Docs.pdf  — PDF (converted from the Word file)
# Needs: python3, graphviz (dot), node + npm package "docx", LibreOffice (soffice).
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
UA="$(cd "$HERE/.." && pwd)"
TMP="$(mktemp -d)"
python3 -I "$HERE/build_guide.py" "$HERE/template.html" "$TMP"
cp "$TMP/USER_GUIDE.md" "$UA/USER_GUIDE.md"
python3 -I "$HERE/export_assets.py" "$TMP"
rm -f "$HERE"/diagrams/*.png && mkdir -p "$HERE/diagrams" && cp "$TMP"/diagrams/*.png "$HERE/diagrams/"
(cd "$TMP" && node "$HERE/build_docx.js" guide.json Posibnyk-Renault-Docs.docx \
  && soffice --headless --convert-to pdf Posibnyk-Renault-Docs.docx >/dev/null)
cp "$TMP/Posibnyk-Renault-Docs.docx" "$TMP/Posibnyk-Renault-Docs.pdf" "$UA/"
echo "Готово: USER_GUIDE.md, Posibnyk-Renault-Docs.docx, Posibnyk-Renault-Docs.pdf"
