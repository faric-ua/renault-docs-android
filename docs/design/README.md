# Design assets

This folder contains editable design and architecture artifacts for Renault Docs.

Canonical visual formats should be text-based and versionable whenever possible.

Preferred:
- SVG for exact diagrams and UI maps;
- Mermaid/PlantUML for structural diagrams;
- Markdown for product decisions.

Generated PNG/JPEG images are previews only. Text-heavy roadmaps should use SVG as the source of truth because it stays sharp at any zoom and can be reviewed in Git diffs.

Files:
- `renault-docs-vision-roadmap.svg` — current product/architecture roadmap visual.


## Automatic exports

Every `docs/design/**/*.svg` is automatically exported recursively on `main` to:

- matching `.png` at 2x raster scale for easy phone viewing;
- matching `.pdf` as a vector document for sharp zoom/printing.

Exporter:
- `tools/export_design_assets.py`

CI:
- `.github/workflows/export-design-assets.yml`

The SVG remains the canonical source. PNG and PDF are derived artifacts committed automatically by CI.


## Skins

Runtime/theme concepts live under `docs/design/skins/`. See `docs/design/skins/README.md` for lifecycle, naming, token, and implementation contracts.
