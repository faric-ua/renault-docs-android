from __future__ import annotations

import html
import json
from pathlib import Path
from typing import Any, Callable
from urllib.parse import quote

from core.dataset_manifest import write_manifest
from core.fast_pack import build_fast_pack
from core.modern_index import MODERN_INDEX_FILENAME, write_modern_index
from core.runtime_ir import (
    RUNTIME_TREE_FILENAME,
    write_runtime_tree,
)
from core.runtime_ir_coverage import (
    RUNTIME_IR_COVERAGE_FILENAME,
    write_runtime_ir_coverage,
)
from core.runtime_ir_shards import (
    RUNTIME_IR_INDEX_FILENAME,
)
from core.sections import (
    MODERN_SECTIONS_FILENAME,
    write_modern_sections_index,
)
from core.volumes import discover_volumes


PACKAGE_DIR = "_renault"
CATALOG_FILENAME = "START.html"
README_FILENAME = "README_UA.html"
VOLUMES_FILENAME = "volumes.json"


def build_dataset_package(
    dataset: dict[str, Any],
    output_root: Path,
    conversion_summary: dict[str, Any] | None = None,
    progress: Callable[[str], None] | None = None,
) -> dict[str, Any]:
    output_root = output_root.resolve()
    package_root = output_root / PACKAGE_DIR
    package_root.mkdir(parents=True, exist_ok=True)

    if progress is not None:
        progress("Package: шукаю томи...")

    volumes = discover_volumes(output_root)

    if progress is not None:
        progress(f"Package: знайдено томів: {len(volumes)}")

    manifest_dataset = dict(dataset)
    legacy_entrypoint = manifest_dataset.get("entrypoint")
    manifest_dataset["legacy_entrypoint"] = legacy_entrypoint
    manifest_dataset["catalog_entrypoint"] = f"{PACKAGE_DIR}/{CATALOG_FILENAME}"
    manifest_dataset["modern_index"] = f"{PACKAGE_DIR}/{MODERN_INDEX_FILENAME}"
    manifest_dataset["modern_sections"] = f"{PACKAGE_DIR}/{MODERN_SECTIONS_FILENAME}"
    manifest_dataset["runtime_tree"] = f"{PACKAGE_DIR}/{RUNTIME_TREE_FILENAME}"
    manifest_dataset["runtime_ir_index"] = f"{PACKAGE_DIR}/{RUNTIME_IR_INDEX_FILENAME}"
    manifest_dataset["runtime_ir_coverage"] = f"{PACKAGE_DIR}/{RUNTIME_IR_COVERAGE_FILENAME}"
    manifest_dataset["volumes"] = volumes

    volumes_path = package_root / VOLUMES_FILENAME
    volumes_path.write_text(
        json.dumps(volumes, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    modern_index_path = write_modern_index(
        manifest_dataset,
        volumes,
        package_root,
    )

    if progress is not None:
        progress(
            "Package: будую native index розділів..."
        )

    modern_sections_path = write_modern_sections_index(
        output_root=output_root,
        volumes=volumes,
        package_root=package_root,
        progress=progress,
    )

    if progress is not None:
        progress(
            "Package: компілюю Classic runtime у Modern JSON IR..."
        )

    runtime_tree_path = write_runtime_tree(
        output_root=output_root,
        volumes=volumes,
        package_root=package_root,
        progress=progress,
    )

    if progress is not None:
        progress(
            "Package: аналізую Runtime IR coverage по всіх томах..."
        )

    runtime_ir_index_path = (
        package_root
        / RUNTIME_IR_INDEX_FILENAME
    )

    runtime_ir_coverage_path = write_runtime_ir_coverage(
        runtime_tree_path=runtime_tree_path,
        package_root=package_root,
    )

    catalog_path = package_root / CATALOG_FILENAME
    catalog_path.write_text(
        render_catalog_html(manifest_dataset, volumes),
        encoding="utf-8",
    )

    readme_path = package_root / README_FILENAME
    readme_path.write_text(
        render_readme_html(manifest_dataset, volumes),
        encoding="utf-8",
    )

    fast_pack = build_fast_pack(
        output_root=output_root,
        package_root=package_root,
        progress=progress,
    )
    manifest_dataset["fast_pack"] = fast_pack

    manifest_path = write_manifest(
        manifest_dataset,
        output_root,
        conversion_summary,
    )

    return {
        "manifest_path": manifest_path,
        "catalog_path": catalog_path,
        "readme_path": readme_path,
        "volumes_path": volumes_path,
        "modern_index_path": modern_index_path,
        "modern_sections_path": modern_sections_path,
        "runtime_tree_path": runtime_tree_path,
        "runtime_ir_index_path": runtime_ir_index_path,
        "runtime_ir_coverage_path": runtime_ir_coverage_path,
        "fast_pack": fast_pack,
        "fast_pack_path": output_root / fast_pack["path"],
        "volumes": volumes,
    }


def render_catalog_html(
    dataset: dict[str, Any],
    volumes: list[dict[str, Any]],
) -> str:
    title = html.escape(str(dataset.get("title") or "Renault Docs"))
    model = html.escape(str(dataset.get("model") or "Renault"))
    years = _years_label(dataset.get("years"))
    platform = html.escape(str(dataset.get("platform") or ""))

    cards = []
    for volume in volumes:
        href = _relative_from_package(volume["entrypoint"])
        subtitle_bits = [
            value
            for value in (
                volume.get("kind"),
                volume.get("source_folder"),
            )
            if value
        ]
        cards.append(
            f"""
      <a class="card" href="{html.escape(href, quote=True)}">
        <div class="card-title">{html.escape(str(volume.get("title") or volume["source_folder"]))}</div>
        <div class="card-subtitle">{html.escape(" · ".join(subtitle_bits))}</div>
      </a>"""
        )

    legacy_entrypoint = dataset.get("legacy_entrypoint")
    legacy_link = ""
    if legacy_entrypoint:
        legacy_link = (
            f'<a class="secondary" href="{html.escape(_relative_from_package(legacy_entrypoint), quote=True)}">'
            "Відкрити оригінальний INDEX"
            "</a>"
        )

    empty_state = ""
    if not cards:
        empty_state = (
            '<div class="empty">Внутрішні томи автоматично не знайдені. '
            "Скористайся оригінальним INDEX.</div>"
        )

    return f"""<!doctype html>
<html lang="uk">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
  <title>{title}</title>
  <style>
    :root {{
      color-scheme: dark;
      --bg: #101318;
      --surface: #181d25;
      --surface2: #222936;
      --border: #384352;
      --text: #f3f6f8;
      --muted: #aab5c2;
      --accent: #76bdff;
    }}
    * {{ box-sizing: border-box; }}
    body {{
      margin: 0;
      min-height: 100vh;
      font-family: system-ui, -apple-system, sans-serif;
      background: var(--bg);
      color: var(--text);
    }}
    main {{
      width: min(1100px, 100%);
      margin: 0 auto;
      padding: 24px;
    }}
    header {{
      margin-bottom: 22px;
    }}
    h1 {{
      margin: 0 0 6px;
      font-size: clamp(26px, 5vw, 42px);
    }}
    .meta {{
      color: var(--muted);
      line-height: 1.6;
    }}
    .grid {{
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
      gap: 14px;
    }}
    .card {{
      display: block;
      min-height: 120px;
      padding: 18px;
      border: 1px solid var(--border);
      border-radius: 16px;
      background: var(--surface);
      color: var(--text);
      text-decoration: none;
    }}
    .card:active {{
      transform: scale(.99);
    }}
    .card-title {{
      font-size: 18px;
      font-weight: 700;
      margin-bottom: 10px;
    }}
    .card-subtitle {{
      color: var(--muted);
      overflow-wrap: anywhere;
    }}
    .actions {{
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      margin-top: 22px;
    }}
    .secondary {{
      display: inline-flex;
      align-items: center;
      min-height: 44px;
      padding: 0 14px;
      border: 1px solid var(--border);
      border-radius: 12px;
      color: var(--accent);
      text-decoration: none;
      background: var(--surface2);
    }}
    .empty {{
      padding: 18px;
      border-radius: 14px;
      background: var(--surface);
      color: var(--muted);
    }}
  </style>
</head>
<body>
<main>
  <header>
    <h1>{title}</h1>
    <div class="meta">
      {model}
      {" · " + html.escape(years) if years else ""}
      {" · " + platform if platform else ""}
      · томів: {len(volumes)}
    </div>
  </header>

  <div class="grid">
    {"".join(cards)}
  </div>

  {empty_state}

  <div class="actions">
    {legacy_link}
    <a class="secondary" href="{README_FILENAME}">Як користуватися</a>
  </div>
</main>
</body>
</html>
"""


def render_readme_html(
    dataset: dict[str, Any],
    volumes: list[dict[str, Any]],
) -> str:
    title = html.escape(str(dataset.get("title") or "Renault Docs"))
    display_name = html.escape(
        str(dataset.get("model") or dataset.get("title") or "Renault Docs")
    )
    years = _years_label(dataset.get("years"))
    years_html = (
        f'<span class="dataset-years">{html.escape(years)}</span>'
        if years
        else ""
    )

    return f"""<!doctype html>
<html lang="uk">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
  <title>Як користуватися — {title}</title>
  <style>
    :root {{ color-scheme: dark; }}
    body {{
      max-width: 860px;
      margin: 0 auto;
      padding: 14px 14px 28px;
      box-sizing: border-box;
      font-family: system-ui, -apple-system, sans-serif;
      font-size: 15px;
      line-height: 1.45;
      background: #101318;
      color: #f3f6f8;
    }}
    h1 {{
      margin: 0 0 8px;
      font-size: 22px;
      line-height: 1.18;
    }}
    .dataset-years {{
      display: block;
      margin-top: 3px;
      color: #aab5c2;
      font-size: 15px;
      font-weight: 600;
    }}
    h2 {{
      margin: 22px 0 10px;
      font-size: 16px;
      line-height: 1.25;
    }}
    p {{ margin: 8px 0 12px; }}
    a {{ color: #76bdff; }}
    .box {{
      margin: 12px 0 16px;
      padding: 10px 12px;
      border: 1px solid #384352;
      border-radius: 12px;
      background: #181d25;
      font-size: 14px;
    }}
    .readme-action {{
      margin: 0 0 18px;
    }}
    .readme-action a {{
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      box-sizing: border-box;
      width: 100%;
      padding: 11px 12px;
      border: 1px solid #76bdff;
      border-radius: 11px;
      background: #181d25;
      text-decoration: none;
      font-size: 15px;
      font-weight: 700;
    }}
    .readme-action a::after {{
      content: "›";
      font-size: 22px;
      line-height: 1;
    }}
    .file-table {{
      width: 100%;
      table-layout: fixed;
      border-collapse: separate;
      border-spacing: 0;
      overflow: hidden;
      border: 1px solid #384352;
      border-radius: 12px;
      background: #181d25;
      font-size: 13px;
      line-height: 1.35;
    }}
    .file-table th,
    .file-table td {{
      box-sizing: border-box;
      padding: 9px 8px;
      vertical-align: top;
      text-align: left;
      overflow-wrap: anywhere;
      word-break: break-word;
    }}
    .file-table th {{
      color: #aab5c2;
      background: #222936;
      font-size: 12px;
    }}
    .file-table th:first-child,
    .file-table td:first-child {{
      width: 43%;
      border-right: 1px solid #384352;
    }}
    .file-table tr + tr td {{
      border-top: 1px solid #384352;
    }}
    code {{
      padding: 1px 4px;
      border-radius: 5px;
      background: #202732;
      white-space: normal;
      overflow-wrap: anywhere;
      font-size: 12px;
    }}
  </style>
</head>
<body>
  <h1><span>{display_name}</span>{years_html}</h1>
  <p>Конвертований Renault dataset. Внутрішні файли не потрібно редагувати вручну.</p>

  <div class="box"><b>Томів у цьому dataset:</b> {len(volumes)}</div>

  <h2>Документація</h2>
  <p class="readme-action"><a href="{CATALOG_FILENAME}">Відкрити каталог</a></p>

  <h2>Основні файли</h2>
  <table class="file-table">
    <thead>
      <tr><th>Файл</th><th>Призначення</th></tr>
    </thead>
    <tbody>
      <tr><td><code>renault-dataset.json</code></td><td>Опис dataset і точки входу Renault Docs.</td></tr>
      <tr><td><code>{PACKAGE_DIR}/{CATALOG_FILENAME}</code></td><td>Каталог внутрішніх томів.</td></tr>
      <tr><td><code>{PACKAGE_DIR}/{VOLUMES_FILENAME}</code></td><td>Список томів та їхніх entrypoint.</td></tr>
      <tr><td><code>{PACKAGE_DIR}/{MODERN_INDEX_FILENAME}</code></td><td>Швидкий індекс томів для Modern.</td></tr>
      <tr><td><code>{PACKAGE_DIR}/{MODERN_SECTIONS_FILENAME}</code></td><td>Native-індекс розділів для Modern.</td></tr>
      <tr><td><code>{PACKAGE_DIR}/{RUNTIME_TREE_FILENAME}</code></td><td>Повний compiler/debug Runtime IR.</td></tr>
      <tr><td><code>{PACKAGE_DIR}/{RUNTIME_IR_INDEX_FILENAME}</code> + <code>{PACKAGE_DIR}/runtime-ir/sections/…</code></td><td>Шардований Runtime IR для роботи на телефоні.</td></tr>
      <tr><td><code>{PACKAGE_DIR}/{RUNTIME_IR_COVERAGE_FILENAME}</code></td><td>Аудит покриття меню, дій і документів.</td></tr>
      <tr><td><code>{PACKAGE_DIR}/fast-content-*.zip</code></td><td>Fast Pack для швидкого читання локального контенту.</td></tr>
      <tr><td><code>HTM / PDF / GIF</code></td><td>Документація Renault після нормалізації шляхів.</td></tr>
    </tbody>
  </table>

  <h2>Renault Docs</h2>
  <p>Застосунок читає manifest, додає dataset до бібліотеки та відкриває PDF у власному viewer.</p>

  <h2>Важливо</h2>
  <p>Не перейменовуй внутрішні папки після конвертації: це може зламати зв’язки між Classic-файлами.</p>
</body>
</html>
"""


def _relative_from_package(path: str) -> str:
    # START.html/README_UA.html live in _renault/, so go one level up first.
    encoded = "/".join(quote(part) for part in Path(path).as_posix().split("/"))
    return "../" + encoded


def _years_label(years: Any) -> str | None:
    if not isinstance(years, dict):
        return None
    start = years.get("from")
    end = years.get("to")
    if start and end:
        return f"{start}–{end}"
    return str(start or end) if (start or end) else None
