# v0.2.2 Bug Register

## PERF-001 — SAF-backed legacy viewer feels slow

Observed on real phone in v0.2.1.

Likely cause:
each resource path is resolved by repeated `DocumentFile.findFile()` calls. This can issue multiple provider queries for every frame/image/script.

Fix target:
lazy per-directory child cache + resolved path cache + normal WebView cache mode.

## PDF-001 — PDF placeholder instead of actual document

Observed on real phone in v0.2.1.

Actual:
PDF navigation reaches a controlled page saying `PDF — наступний етап`.

Fix target:
native `PdfRenderer` page rendering exposed through the same controlled local WebView origin.
