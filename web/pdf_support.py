from __future__ import annotations

import html
import json
import os
from pathlib import Path
from urllib.parse import unquote, urlencode, urlsplit


PDFJS_VERSION = "6.3.289"
PDFJS_ARCHIVE_SHA256 = "98c5832ffe7af4edd59853476a478c0d4d4d76dd49c1701f4c86f7182725cdf9"
PDFJS_DIST_URL = (
    "https://github.com/mozilla/pdf.js/releases/download/"
    f"v{PDFJS_VERSION}/pdfjs-{PDFJS_VERSION}-dist.zip"
)

PDF_RAW_ROUTE = "/__renault__/pdf/raw"
PDFJS_ROUTE_PREFIX = "/__renault__/pdfjs/"


def is_pdf_request(request_target: str) -> bool:
    path = urlsplit(request_target).path
    if path == PDF_RAW_ROUTE or path.startswith(PDFJS_ROUTE_PREFIX):
        return False
    return path.lower().endswith(".pdf")


def resolve_dataset_path(root: Path, request_path: str) -> Path:
    root_resolved = root.resolve()
    clean_path = unquote(urlsplit(request_path).path).lstrip("/")
    candidate = (root_resolved / clean_path).resolve()

    try:
        common = Path(os.path.commonpath([str(root_resolved), str(candidate)]))
    except ValueError as exc:
        raise ValueError("Path is outside dataset root") from exc

    if common != root_resolved:
        raise ValueError("Path is outside dataset root")

    return candidate


def raw_pdf_url(request_path: str) -> str:
    relative = unquote(urlsplit(request_path).path).lstrip("/")
    return f"{PDF_RAW_ROUTE}?{urlencode({'path': relative})}"


def pdfjs_vendor_ready(vendor_root: Path) -> bool:
    return (
        (vendor_root / "build" / "pdf.mjs").is_file()
        and (vendor_root / "build" / "pdf.worker.mjs").is_file()
    )


def setup_required_html(pdf_path: str) -> str:
    escaped = html.escape(unquote(pdf_path))
    return f"""<!doctype html>
<html lang="uk">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Renault Docs — PDF setup</title>
  <style>
    body {{
      margin: 0;
      font-family: system-ui, sans-serif;
      background: #101318;
      color: #f3f5f7;
    }}
    main {{
      max-width: 720px;
      margin: 0 auto;
      padding: 28px 20px;
    }}
    .card {{
      background: #1a1f27;
      border: 1px solid #343c49;
      border-radius: 16px;
      padding: 20px;
    }}
    code {{
      display: block;
      overflow-wrap: anywhere;
      padding: 12px;
      margin-top: 12px;
      border-radius: 10px;
      background: #0d1015;
      color: #dfe7ef;
    }}
    .muted {{ color: #aeb8c4; }}
  </style>
</head>
<body>
<main>
  <div class="card">
    <h1>PDF viewer ще не встановлений</h1>
    <p>HTML-документація працює. Для автоматичного відкриття PDF один раз встанови локальний PDF.js:</p>
    <code>reno-code<br>python tools/install_pdfjs.py</code>
    <p class="muted">Після встановлення перезапусти <b>reno-docs</b> і відкрий PDF ще раз.</p>
    <p class="muted">Запитаний файл: {escaped}</p>
  </div>
</main>
</body>
</html>
"""


def viewer_html(request_path: str) -> str:
    raw_url = raw_pdf_url(request_path)
    title = Path(unquote(urlsplit(request_path).path)).name
    title_json = json.dumps(title, ensure_ascii=False)
    raw_json = json.dumps(raw_url, ensure_ascii=False)

    return f"""<!doctype html>
<html lang="uk">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
  <title>{html.escape(title)}</title>
  <style>
    :root {{
      color-scheme: dark;
      --bg: #0f1217;
      --surface: #181d25;
      --surface2: #242b36;
      --border: #3b4655;
      --text: #f3f6f8;
      --muted: #aab5c2;
      --accent: #69b7ff;
      --danger: #ff7a88;
      --paper-gap: 18px;
    }}

    * {{ box-sizing: border-box; }}

    html, body {{
      width: 100%;
      height: 100%;
      margin: 0;
      overflow: hidden;
      background: var(--bg);
      color: var(--text);
      font-family: system-ui, -apple-system, sans-serif;
    }}

    body {{
      display: grid;
      grid-template-rows: auto 1fr;
    }}

    header {{
      display: flex;
      align-items: center;
      gap: 8px;
      min-height: 52px;
      padding: max(6px, env(safe-area-inset-top)) 8px 6px;
      background: var(--surface);
      border-bottom: 1px solid var(--border);
      overflow-x: auto;
      white-space: nowrap;
      scrollbar-width: thin;
    }}

    button, input, a.control {{
      min-height: 40px;
      border-radius: 10px;
      border: 1px solid var(--border);
      background: var(--surface2);
      color: var(--text);
      font: inherit;
    }}

    button {{
      min-width: 44px;
      padding: 0 12px;
    }}

    button:disabled {{ opacity: .45; }}

    input {{
      width: 58px;
      padding: 0 8px;
      text-align: center;
    }}

    a.control {{
      display: inline-flex;
      align-items: center;
      padding: 0 10px;
      text-decoration: none;
    }}

    #title {{
      overflow: hidden;
      text-overflow: ellipsis;
      max-width: min(42vw, 420px);
      font-weight: 650;
    }}

    .muted {{ color: var(--muted); }}

    #viewer {{
      position: relative;
      overflow: auto;
      overscroll-behavior: contain;
      background: #31343a;
      scroll-behavior: smooth;
    }}

    #pagesContainer {{
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: var(--paper-gap);
      width: max-content;
      min-width: 100%;
      min-height: 100%;
      padding: 12px 12px 28px;
    }}

    .pdf-page {{
      display: flex;
      flex-direction: column;
      align-items: center;
      width: max-content;
      max-width: none;
      scroll-margin-top: 12px;
    }}

    .page-label {{
      align-self: flex-start;
      margin: 0 0 6px 2px;
      padding: 4px 8px;
      border-radius: 8px;
      background: #161b22dd;
      color: var(--muted);
      font-size: 12px;
      line-height: 1.2;
    }}

    .page-surface {{
      position: relative;
      flex: none;
      background: white;
      box-shadow: 0 2px 14px #0008;
      transition: width 90ms linear, height 90ms linear;
    }}

    canvas {{
      display: block;
      width: 100%;
      height: 100%;
      background: white;
    }}

    canvas[hidden] {{
      display: none;
    }}

    .page-error {{
      position: absolute;
      inset: 12px;
      display: grid;
      place-items: center;
      padding: 12px;
      color: var(--danger);
      background: #241419ee;
      text-align: center;
    }}

    .page-error[hidden] {{
      display: none !important;
    }}

    #status {{
      position: absolute;
      z-index: 20;
      top: 16px;
      left: 16px;
      max-width: calc(100% - 32px);
      padding: 10px 12px;
      border-radius: 10px;
      background: #111820e8;
      color: var(--muted);
      pointer-events: none;
    }}

    #status.error {{ color: var(--danger); }}

    @media (max-width: 620px) {{
      header {{ gap: 6px; }}
      #title {{ max-width: 34vw; }}
      .wide-label {{ display: none; }}
    }}
  </style>
</head>
<body>
<header>
  <button id="back" type="button" title="Назад">Назад</button>
  <span id="title"></span>
  <button id="prev" type="button" title="До попередньої сторінки">↑ стор.</button>
  <input id="page" inputmode="numeric" pattern="[0-9]*" aria-label="Номер поточної сторінки">
  <span id="pages" class="muted"></span>
  <button id="next" type="button" title="До наступної сторінки">↓ стор.</button>
  <button id="zoomOut" type="button" title="Зменшити">−</button>
  <span id="zoom" class="muted">100%</span>
  <button id="zoomIn" type="button" title="Збільшити">+</button>
  <button id="fit" type="button" title="По ширині">
    <span class="wide-label">По ширині</span><span class="muted">↔</span>
  </button>
  <a id="raw" class="control" target="_blank" rel="noopener">PDF</a>
</header>

<div id="viewer">
  <div id="pagesContainer"></div>
  <div id="status">Завантаження PDF…</div>
</div>

<script type="module">
  import * as pdfjsLib from "/__renault__/pdfjs/build/pdf.mjs";

  pdfjsLib.GlobalWorkerOptions.workerSrc =
    "/__renault__/pdfjs/build/pdf.worker.mjs";

  const RAW_URL = {raw_json};
  const FILE_TITLE = {title_json};

  const viewer = document.getElementById("viewer");
  const pagesContainer = document.getElementById("pagesContainer");
  const status = document.getElementById("status");
  const pageInput = document.getElementById("page");
  const pages = document.getElementById("pages");
  const zoomLabel = document.getElementById("zoom");
  const title = document.getElementById("title");
  const rawLink = document.getElementById("raw");

  title.textContent = FILE_TITLE;
  rawLink.href = RAW_URL;

  let pdf = null;
  let currentPage = 1;
  let scale = 1;
  let fitWidth = true;
  let firstPageWidth = 1;
  let firstPageHeight = 1;
  let currentObserver = null;
  let visiblePages = new Map();
  const pageStates = [];

  function showStatus(message, error = false) {{
    status.textContent = message;
    status.classList.toggle("error", error);
    status.hidden = !message;
  }}

  function clamp(value, min, max) {{
    return Math.min(max, Math.max(min, value));
  }}

  function parseViewRect() {{
    const match = window.location.hash.match(
      /(?:^#|[&#])viewrect=([-+0-9.]+),([-+0-9.]+),([-+0-9.]+),([-+0-9.]+)/i
    );

    if (!match) return null;

    const values = match.slice(1).map(Number);
    if (values.some((value) => !Number.isFinite(value))) return null;

    const [left, top, width, height] = values;
    if (width <= 0 || height <= 0) return null;

    return {{ left, top, width, height }};
  }}

  function updateToolbar() {{
    pageInput.value = String(currentPage);
    pages.textContent = "/ " + (pdf?.numPages || "—");
    zoomLabel.textContent = Math.round(scale * 100) + "%";
    document.getElementById("prev").disabled = currentPage <= 1;
    document.getElementById("next").disabled =
      !pdf || currentPage >= pdf.numPages;
  }}

  function pageDimensions(state) {{
    const width = (state.baseWidth || firstPageWidth) * scale;
    const height = (state.baseHeight || firstPageHeight) * scale;
    return {{
      width: Math.max(1, Math.round(width)),
      height: Math.max(1, Math.round(height))
    }};
  }}

  function updatePageShell(state) {{
    const {{ width, height }} = pageDimensions(state);
    state.surface.style.width = width + "px";
    state.surface.style.height = height + "px";

    if (!state.canvas.hidden) {{
      state.canvas.style.width = width + "px";
      state.canvas.style.height = height + "px";
    }}
  }}

  function releasePage(state) {{
    state.generation += 1;

    if (state.renderTask) {{
      try {{ state.renderTask.cancel(); }} catch (_) {{}}
    }}

    state.renderTask = null;
    state.loading = false;
    state.renderedScale = null;
    state.canvas.width = 1;
    state.canvas.height = 1;
    state.canvas.hidden = true;
  }}

  function releaseFarPages(center) {{
    for (const state of pageStates) {{
      if (Math.abs(state.number - center) > 4 && state.renderedScale !== null) {{
        releasePage(state);
      }}
    }}
  }}

  async function renderPage(pageNumber) {{
    if (!pdf || pageNumber < 1 || pageNumber > pdf.numPages) return;

    const state = pageStates[pageNumber - 1];
    const requestedScale = scale;

    if (
      state.loading ||
      (state.renderedScale !== null &&
        Math.abs(state.renderedScale - requestedScale) < 0.001)
    ) {{
      return;
    }}

    state.generation += 1;
    const generation = state.generation;
    state.loading = true;

    if (state.renderTask) {{
      try {{ state.renderTask.cancel(); }} catch (_) {{}}
      state.renderTask = null;
    }}

    try {{
      const page = await pdf.getPage(pageNumber);
      if (state.generation !== generation) return;

      const base = page.getViewport({{ scale: 1 }});
      state.baseWidth = base.width;
      state.baseHeight = base.height;
      updatePageShell(state);

      const viewport = page.getViewport({{ scale: requestedScale }});
      const outputScale = Math.min(window.devicePixelRatio || 1, 2);

      state.canvas.width = Math.max(
        1,
        Math.floor(viewport.width * outputScale)
      );
      state.canvas.height = Math.max(
        1,
        Math.floor(viewport.height * outputScale)
      );
      state.canvas.style.width = Math.floor(viewport.width) + "px";
      state.canvas.style.height = Math.floor(viewport.height) + "px";
      state.canvas.hidden = false;

      const context = state.canvas.getContext("2d", {{ alpha: false }});
      const transform =
        outputScale === 1
          ? null
          : [outputScale, 0, 0, outputScale, 0, 0];

      const task = page.render({{
        canvasContext: context,
        transform,
        viewport
      }});

      state.renderTask = task;
      await task.promise;

      if (state.generation !== generation) return;
      state.renderedScale = requestedScale;
      state.error.hidden = true;
    }} catch (error) {{
      if (error?.name === "RenderingCancelledException") return;
      console.error("PDF page render failed", pageNumber, error);

      if (state.generation === generation) {{
        state.error.textContent =
          "Не вдалося відтворити сторінку " +
          pageNumber +
          ". " +
          (error?.message || error);
        state.error.hidden = false;
      }}
    }} finally {{
      if (state.generation === generation) {{
        state.loading = false;
        state.renderTask = null;
      }}
    }}
  }}

  function renderAround(center) {{
    const from = Math.max(1, center - 2);
    const to = Math.min(pdf.numPages, center + 2);

    for (let number = from; number <= to; number += 1) {{
      void renderPage(number);
    }}

    releaseFarPages(center);
  }}

  function setCurrentPage(pageNumber) {{
    if (!pdf) return;

    const next = clamp(Number(pageNumber) || 1, 1, pdf.numPages);
    const changed = next !== currentPage;
    currentPage = next;
    updateToolbar();

    if (changed) {{
      renderAround(currentPage);
    }}
  }}

  function scrollToPage(pageNumber, smooth = true) {{
    if (!pdf) return;

    const next = clamp(Number(pageNumber) || 1, 1, pdf.numPages);
    const state = pageStates[next - 1];

    setCurrentPage(next);
    renderAround(next);

    const top = Math.max(
      0,
      state.element.offsetTop - pagesContainer.offsetTop - 8
    );

    viewer.scrollTo({{
      top,
      behavior: smooth ? "smooth" : "auto"
    }});
  }}

  function createPagePlaceholders() {{
    const fragment = document.createDocumentFragment();

    for (let number = 1; number <= pdf.numPages; number += 1) {{
      const element = document.createElement("section");
      element.className = "pdf-page";
      element.dataset.page = String(number);

      const label = document.createElement("div");
      label.className = "page-label";
      label.textContent = "Сторінка " + number + " з " + pdf.numPages;

      const surface = document.createElement("div");
      surface.className = "page-surface";

      const canvas = document.createElement("canvas");
      canvas.hidden = true;

      const error = document.createElement("div");
      error.className = "page-error";
      error.hidden = true;

      surface.append(canvas, error);
      element.append(label, surface);
      fragment.append(element);

      const state = {{
        number,
        element,
        label,
        surface,
        canvas,
        error,
        baseWidth: firstPageWidth,
        baseHeight: firstPageHeight,
        renderedScale: null,
        renderTask: null,
        loading: false,
        generation: 0
      }};

      pageStates.push(state);
      updatePageShell(state);
    }}

    pagesContainer.append(fragment);
  }}

  function chooseCurrentVisiblePage() {{
    let bestPage = null;
    let bestRatio = -1;
    let bestDistance = Number.POSITIVE_INFINITY;

    for (const [pageNumber, entry] of visiblePages.entries()) {{
      if (!entry.isIntersecting || entry.intersectionRatio <= 0) continue;

      const rootTop = entry.rootBounds?.top ?? 0;
      const distance = Math.abs(entry.boundingClientRect.top - rootTop);

      if (
        entry.intersectionRatio > bestRatio + 0.001 ||
        (
          Math.abs(entry.intersectionRatio - bestRatio) <= 0.001 &&
          distance < bestDistance
        )
      ) {{
        bestPage = pageNumber;
        bestRatio = entry.intersectionRatio;
        bestDistance = distance;
      }}
    }}

    if (bestPage !== null) {{
      setCurrentPage(bestPage);
    }}
  }}

  function observeCurrentPage() {{
    currentObserver?.disconnect();
    visiblePages = new Map();

    currentObserver = new IntersectionObserver(
      (entries) => {{
        for (const entry of entries) {{
          const pageNumber = Number(entry.target.dataset.page);
          visiblePages.set(pageNumber, entry);
        }}
        chooseCurrentVisiblePage();
      }},
      {{
        root: viewer,
        threshold: [0, 0.05, 0.2, 0.4, 0.6, 0.8, 1]
      }}
    );

    for (const state of pageStates) {{
      currentObserver.observe(state.element);
    }}
  }}

  function clearRenderedPages() {{
    for (const state of pageStates) {{
      releasePage(state);
      updatePageShell(state);
    }}
  }}

  function applyScale(nextScale, useFitWidth, keepPage = true) {{
    if (!pdf) return;

    const anchorPage = currentPage;
    scale = clamp(nextScale, 0.25, 6);
    fitWidth = useFitWidth;

    clearRenderedPages();
    updateToolbar();

    requestAnimationFrame(() => {{
      if (keepPage) {{
        scrollToPage(anchorPage, false);
      }}
      renderAround(anchorPage);
    }});
  }}

  function fitScale() {{
    const availableWidth = Math.max(120, viewer.clientWidth - 28);
    return clamp(availableWidth / firstPageWidth, 0.25, 6);
  }}

  async function applyLegacyViewRectIfNeeded() {{
    const legacyRect = parseViewRect();
    if (!legacyRect) return false;

    const availableWidth = Math.max(120, viewer.clientWidth - 28);
    const availableHeight = Math.max(120, viewer.clientHeight - 28);

    scale = clamp(
      Math.min(
        availableWidth / legacyRect.width,
        availableHeight / legacyRect.height
      ),
      0.25,
      6
    );
    fitWidth = false;

    for (const state of pageStates) {{
      updatePageShell(state);
    }}

    await renderPage(1);
    updateToolbar();

    const state = pageStates[0];
    const firstPage = await pdf.getPage(1);
    const viewport = firstPage.getViewport({{ scale }});
    const [x, y] = viewport.convertToViewportPoint(
      legacyRect.left,
      legacyRect.top
    );

    requestAnimationFrame(() => {{
      const viewerRect = viewer.getBoundingClientRect();
      const surfaceRect = state.surface.getBoundingClientRect();

      viewer.scrollLeft = Math.max(
        0,
        viewer.scrollLeft +
          (surfaceRect.left - viewerRect.left) +
          x -
          12
      );
      viewer.scrollTop = Math.max(
        0,
        viewer.scrollTop +
          (surfaceRect.top - viewerRect.top) +
          y -
          12
      );
    }});

    return true;
  }}

  document.getElementById("back").addEventListener("click", () => {{
    history.back();
  }});

  document.getElementById("prev").addEventListener("click", () => {{
    scrollToPage(currentPage - 1);
  }});

  document.getElementById("next").addEventListener("click", () => {{
    scrollToPage(currentPage + 1);
  }});

  pageInput.addEventListener("change", () => {{
    scrollToPage(pageInput.value);
  }});

  pageInput.addEventListener("keydown", (event) => {{
    if (event.key === "Enter") {{
      pageInput.blur();
      scrollToPage(pageInput.value);
    }}
  }});

  document.getElementById("zoomIn").addEventListener("click", () => {{
    applyScale(scale * 1.25, false);
  }});

  document.getElementById("zoomOut").addEventListener("click", () => {{
    applyScale(scale / 1.25, false);
  }});

  document.getElementById("fit").addEventListener("click", () => {{
    applyScale(fitScale(), true);
  }});

  let resizeTimer = null;
  window.addEventListener("resize", () => {{
    if (!fitWidth || !pdf) return;

    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(() => {{
      applyScale(fitScale(), true);
    }}, 140);
  }});

  try {{
    const loadingTask = pdfjsLib.getDocument({{
      url: RAW_URL,
      disableRange: true,
      disableStream: true,
      cMapUrl: "/__renault__/pdfjs/cmaps/",
      cMapPacked: true,
      standardFontDataUrl: "/__renault__/pdfjs/standard_fonts/",
      wasmUrl: "/__renault__/pdfjs/wasm/"
    }});

    pdf = await loadingTask.promise;

    const firstPage = await pdf.getPage(1);
    const firstViewport = firstPage.getViewport({{ scale: 1 }});
    firstPageWidth = firstViewport.width;
    firstPageHeight = firstViewport.height;

    pageInput.min = "1";
    pageInput.max = String(pdf.numPages);
    scale = fitScale();

    createPagePlaceholders();
    observeCurrentPage();
    updateToolbar();
    showStatus("");

    const usedLegacyViewRect = await applyLegacyViewRectIfNeeded();

    if (!usedLegacyViewRect) {{
      fitWidth = true;
      renderAround(1);
    }}
  }} catch (error) {{
    console.error(error);
    showStatus(
      "Не вдалося відкрити PDF. Перевір PDF.js та server.log. " +
      (error?.message || error),
      true
    );
  }}
</script>
</body>
</html>
"""
