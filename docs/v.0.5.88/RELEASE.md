# Renault Docs v0.5.88 — unified operation progress

Status: **DEVELOPMENT / PR checks and phone QA pending**.

Scope:
- Keep the existing compact status card and thin progress bar.
- Render stage and file count in independent fixed-height slots in portrait; on one fixed-height row in landscape.
- Preserve complete status copy text.
- Use one measured progress bar utility for Project, Home/Status, Drive/catalog operation cards and Converter.
- Remove obsolete hidden Home bar.
- Do not bounce progress when totals are unknown; use real measured progress when available.
- Remove duplicate raw extraction/staging/packing messages that overwrote structured file counts; persist native message and measurement atomically.
- Preserve operation cancellation, restart/rotation restoration, output/export/install paths, Classic and user data.

Video finding behind #100: unpacking alternated between `Розпаковую ZIP... 1361/3360` and `Розпаковую ZIP... · Файлів: 2312 / 3360`. Fix targets presentation and duplicate progress publications, not extraction correctness.

No new package conversion has been performed by these source-only changes. Source files, exported RDPKG, installed volumes and private user data remain untouched. CI and device evidence are separate acceptance gates.
