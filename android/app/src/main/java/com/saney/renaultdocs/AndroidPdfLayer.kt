package com.saney.renaultdocs

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.LruCache
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Locale
import org.json.JSONObject

class AndroidPdfLayer(
    private val resolver: SafDatasetResolver,
    private val cacheNamespace: String,
    initialZoomPercent: Int = 100,
    zoomStepPercent: Int = 10,
    private val viewerStateScope: String = "main",
    private val companionControlEnabled: Boolean = false,
    private val compactMode: Boolean = false,
) {
    private val initialZoomPercent =
        initialZoomPercent.coerceIn(
            50,
            400,
        )

    private val zoomStepPercent =
        zoomStepPercent.coerceIn(
            5,
            50,
        )

    private val maxRenderWidth =
        if (compactMode) 1800 else 3000

    private val fastRenderWidth =
        if (compactMode) 1200 else 1600

    private val neighborRenderWidthCap =
        if (compactMode) 900 else 1200

    private val highZoomNeighborRadius =
        if (compactMode) 0 else 1

    private val highZoomRetainRadius =
        if (compactMode) 0 else 1

    private data class PdfInfo(
        val pageCount: Int,
        val firstPageWidth: Int,
        val firstPageHeight: Int,
    )

    fun intercept(
        requestUrl: String,
        relativePath: String,
    ): WebResourceResponse? {
        if (
            relativePath ==
            PAGE_ENDPOINT
        ) {
            return renderPageResponse(
                Uri.parse(requestUrl),
            )
        }

        if (
            relativePath
                .lowercase(Locale.ROOT)
                .endsWith(".pdf")
        ) {
            return viewerResponse(
                relativePath,
            )
        }

        return null
    }

    private fun viewerResponse(
        relativePath: String,
    ): WebResourceResponse {
        val info = readInfo(relativePath)
            ?: return errorHtml(
                title = "PDF не відкрився",
                body = relativePath,
            )

        val quotedPath =
            JSONObject.quote(relativePath)
        val quotedViewerStateKey =
            JSONObject.quote(
                "renaultPdfView|" +
                    cacheNamespace +
                    "|" +
                    viewerStateScope +
                    "|" +
                    relativePath
            )

        val companionButtonHtml =
            if (companionControlEnabled) {
                """
                  <button
                    id="companion"
                    aria-label="Відкрити документацію тому"
                    aria-pressed="false"
                    title="Документація поруч"
                  >
                    <svg viewBox="0 0 18 18" width="18" height="18" aria-hidden="true">
                      <path
                        d="M2 3h6v12H2zM10 3h6v12h-6z"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="1.6"
                        stroke-linejoin="round"
                      />
                    </svg>
                  </button>
                """.trimIndent()
            } else {
                ""
            }

        val companionCss =
            if (companionControlEnabled) {
                """
                #companion[aria-pressed="true"] {
                  background: #6baee8;
                  border-color: #6baee8;
                  color: #0f141b;
                  box-shadow:
                    inset 0 0 0 1px rgba(255,255,255,.22);
                }
                """.trimIndent()
            } else {
                ""
            }

        val companionScript =
            if (companionControlEnabled) {
                """
                const companionButton =
                  document.getElementById('companion');

                window.renaultSetCompanion =
                  enabled => {
                    const active =
                      Boolean(enabled);
                    companionButton.setAttribute(
                      'aria-pressed',
                      active ? 'true' : 'false'
                    );
                    companionButton.title =
                      active
                        ? 'Закрити документацію поруч'
                        : 'Документація поруч';
                    companionButton.setAttribute(
                      'aria-label',
                      companionButton.title
                    );
                  };

                companionButton.addEventListener(
                  'click',
                  () => {
                    window.location.href =
                      'renaultcompanion://toggle';
                  }
                );
                """.trimIndent()
            } else {
                ""
            }

        val html = """
            <!doctype html>
            <html lang="uk">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
              <title>PDF</title>
              <style>
                :root {
                  color-scheme: dark;
                  --bg: #0e1116;
                  --surface: #171c24;
                  --line: #344050;
                  --text: #f3f6f8;
                  --muted: #aab5c2;
                }
                * { box-sizing: border-box; }
                html, body {
                  margin: 0;
                  padding: 0;
                  width: 100%;
                  height: 100%;
                  background: var(--bg);
                  color: var(--text);
                  font-family: system-ui, sans-serif;
                  overflow: hidden;
                }
                body {
                  display: flex;
                  flex-direction: column;
                }
                #toolbar {
                  z-index: 20;
                  flex: 0 0 auto;
                  display: flex;
                  flex-direction: column;
                  gap: 4px;
                  padding: 4px;
                  background: rgba(14,17,22,.98);
                  border-bottom: 1px solid var(--line);
                  width: 100%;
                  overflow: visible;
                }
                body.renault-split-focus #toolbar {
                  position: fixed;
                  top: 6px;
                  left: 6px;
                  right: 6px;
                  width: auto;
                  max-width: calc(100% - 12px);
                  z-index: 400;
                  border: 1px solid var(--line);
                  border-radius: 11px;
                  box-shadow: 0 8px 24px rgba(0,0,0,.48);
                  transition:
                    opacity 140ms ease,
                    transform 140ms ease;
                }
                body.renault-split-focus.controls-hidden #toolbar {
                  opacity: 0;
                  transform: translateY(-12px);
                  pointer-events: none;
                }
                body.renault-split-focus #pdfViewport {
                  flex: 1 1 auto;
                  height: 100%;
                }
                .toolbar-row {
                  display: flex;
                  gap: 4px;
                  align-items: center;
                  width: 100%;
                  min-width: 0;
                }
                #toolbarNav {
                  justify-content: flex-start;
                }
                #toolbarZoom {
                  justify-content: flex-start;
                }
                @media (orientation: landscape) {
                  #toolbar {
                    flex-direction: row;
                    align-items: center;
                    flex-wrap: nowrap;
                    overflow-x: auto;
                    overflow-y: hidden;
                    scrollbar-width: none;
                  }
                  #toolbar::-webkit-scrollbar {
                    display: none;
                  }
                  .toolbar-row {
                    width: auto;
                    flex: 0 0 auto;
                  }
                }
                #pdfViewport {
                  flex: 1 1 auto;
                  min-height: 0;
                  width: 100%;
                  overflow: auto;
                  overscroll-behavior: contain;
                  -webkit-overflow-scrolling: touch;
                  touch-action: pan-x pan-y;
                }
                button {
                  min-width: 38px;
                  min-height: 40px;
                  padding: 0 7px;
                  border: 1px solid var(--line);
                  border-radius: 9px;
                  background: var(--surface);
                  color: var(--text);
                  font-size: 16px;
                  display: inline-flex;
                  align-items: center;
                  justify-content: center;
                  text-align: center;
                  line-height: 1;
                  vertical-align: middle;
                  flex: 0 0 auto;
                }
                #counter {
                  min-width: 54px;
                  height: 40px;
                  display: inline-flex;
                  align-items: center;
                  justify-content: center;
                  text-align: center;
                  font-size: 13px;
                  color: var(--muted);
                  flex: 0 0 auto;
                }
                .zoom-picker {
                  position: relative;
                  display: flex;
                  gap: 3px;
                  align-items: center;
                  flex: 0 0 auto;
                }
                #zoomInput {
                  width: 78px;
                  height: 40px;
                  padding: 0 5px;
                  border: 1px solid var(--line);
                  border-radius: 9px;
                  outline: none;
                  background: var(--surface);
                  color: var(--text);
                  text-align: center;
                  font: inherit;
                  font-size: 15px;
                  line-height: 40px;
                }
                #zoomInput:focus {
                  border-color: #6baee8;
                }
                #zoomMenuButton {
                  min-width: 30px;
                  width: 30px;
                  padding: 0;
                }
                #zoomMenu {
                  position: fixed;
                  top: 0;
                  left: 0;
                  z-index: 200;
                  width: 112px;
                  padding: 6px;
                  border: 1px solid var(--line);
                  border-radius: 10px;
                  background: #171c24;
                  box-shadow: 0 8px 24px rgba(0,0,0,.45);
                }
                #zoomMenu[hidden] {
                  display: none;
                }
                .zoom-option {
                  width: 100%;
                  min-height: 38px;
                  margin: 0 0 4px;
                  font-size: 15px;
                }
                .zoom-option:last-child {
                  margin-bottom: 0;
                }
                #fullscreen[aria-pressed="true"] {
                  background: #6baee8;
                  border-color: #6baee8;
                  color: #0f141b;
                  box-shadow:
                    inset 0 0 0 1px rgba(255,255,255,.22);
                }
                #fullscreen[aria-pressed="true"] svg {
                  transform: scale(.88);
                }
                $companionCss
                #fitWidth {
                  min-width: 116px;
                  white-space: nowrap;
                  font-size: 14px;
                  padding: 0 10px;
                }
                .fit-width-content {
                  width: 100%;
                  height: 100%;
                  display: inline-flex;
                  align-items: center;
                  justify-content: center;
                  gap: 6px;
                  line-height: 1;
                }
                .fit-width-icon {
                  width: 18px;
                  height: 18px;
                  display: block;
                  flex: 0 0 18px;
                }
                #save {
                  min-width: 68px;
                  padding: 0 7px;
                  font-size: 14px;
                  white-space: nowrap;
                }
                #pages {
                  width: max-content;
                  min-width: 100%;
                  padding: 10px 6px 32px;
                }
                .page {
                  position: relative;
                  width: min(100%, 1100px);
                  margin: 0 auto 12px;
                  background: #fff;
                  border-radius: 4px;
                  overflow: hidden;
                  box-shadow: 0 3px 18px rgba(0,0,0,.35);
                  aspect-ratio: ${info.firstPageWidth} / ${info.firstPageHeight};
                  contain: layout paint;
                }
                .page img {
                  display: block;
                  width: 100%;
                  height: auto;
                  background: #fff;
                }
                .loading {
                  position: absolute;
                  inset: 0;
                  display: grid;
                  place-items: center;
                  color: #555;
                  font-size: 13px;
                  background: #fff;
                }
                .page.loaded .loading {
                  display: none;
                }
              </style>
            </head>
            <body>
              <div id="toolbar">
                <div
                  class="toolbar-row"
                  id="toolbarNav"
                >
                  <button id="prev" aria-label="Попередня сторінка">↑</button>
                  <span id="counter">1 / ${info.pageCount}</span>
                  <button id="next" aria-label="Наступна сторінка">↓</button>
                  <button id="save" aria-label="Зберегти PDF">⇩ PDF</button>
                  <button
                    id="fullscreen"
                    aria-label="Увімкнути або вимкнути повноекранний режим"
                    aria-pressed="false"
                    title="Повноекранний режим"
                  >
                    <svg viewBox="0 0 18 18" width="18" height="18" aria-hidden="true">
                      <path
                        d="M2 7V2h5M11 2h5v5M16 11v5h-5M7 16H2v-5"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="1.8"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                      />
                    </svg>
                  </button>
                  $companionButtonHtml
                </div>

                <div
                  class="toolbar-row"
                  id="toolbarZoom"
                >
                  <button id="minus" aria-label="Зменшити масштаб">−</button>
                  <div class="zoom-picker" id="zoomPicker">
                    <input
                      id="zoomInput"
                      type="text"
                      inputmode="numeric"
                      enterkeyhint="done"
                      value="${initialZoomPercent}%"
                      aria-label="Масштаб PDF у відсотках"
                    >
                    <button
                      id="zoomMenuButton"
                      aria-label="Швидкий вибір масштабу"
                      aria-haspopup="menu"
                      aria-expanded="false"
                    >▾</button>
                    <div id="zoomMenu" role="menu" hidden>
                      <button class="zoom-option" data-zoom="85" role="menuitem">85%</button>
                      <button class="zoom-option" data-zoom="100" role="menuitem">100%</button>
                      <button class="zoom-option" data-zoom="120" role="menuitem">120%</button>
                      <button class="zoom-option" data-zoom="150" role="menuitem">150%</button>
                      <button class="zoom-option" data-zoom="200" role="menuitem">200%</button>
                      <button class="zoom-option" data-zoom="300" role="menuitem">300%</button>
                      <button class="zoom-option" data-zoom="400" role="menuitem">400%</button>
                    </div>
                  </div>
                  <button id="plus" aria-label="Збільшити масштаб">＋</button>
                  <button id="fitWidth" aria-label="Вмістити PDF по ширині">
                    <span class="fit-width-content">
                      <svg
                        class="fit-width-icon"
                        viewBox="0 0 18 18"
                        aria-hidden="true"
                      >
                        <path
                          d="M2 9h14M5 6L2 9l3 3M13 6l3 3-3 3"
                          fill="none"
                          stroke="currentColor"
                          stroke-width="1.8"
                          stroke-linecap="round"
                          stroke-linejoin="round"
                        />
                      </svg>
                      <span>По ширині</span>
                    </span>
                  </button>
                </div>
              </div>

              <div id="pdfViewport">
                <div id="pages"></div>
              </div>

              <script>
                const pdfPath = $quotedPath;
                const pageCount = ${info.pageCount};
                const viewerStateKey =
                  $quotedViewerStateKey;
                let restoredViewerState = null;

                try {
                  const rawState =
                    window.localStorage
                      .getItem(
                        viewerStateKey
                      );

                  if (rawState) {
                    const parsedState =
                      JSON.parse(rawState);

                    if (
                      parsedState &&
                      Number.isFinite(
                        parsedState.savedAt
                      ) &&
                      Date.now() -
                        parsedState.savedAt <
                        5 * 60 * 1000
                    ) {
                      restoredViewerState =
                        parsedState;
                    }
                  }
                } catch (_) {
                  restoredViewerState = null;
                }

                let currentPage =
                  restoredViewerState &&
                  Number.isFinite(
                    restoredViewerState.page
                  )
                    ? Math.max(
                        0,
                        Math.min(
                          pageCount - 1,
                          Math.round(
                            restoredViewerState.page
                          )
                        )
                      )
                    : 0;
                let zoom =
                  restoredViewerState &&
                  Number.isFinite(
                    restoredViewerState.zoom
                  )
                    ? Math.max(
                        0.5,
                        Math.min(
                          4,
                          restoredViewerState.zoom
                        )
                      )
                    : ${initialZoomPercent} / 100;
                const zoomStepPercent =
                  ${zoomStepPercent};

                const pdfViewport =
                  document.getElementById(
                    'pdfViewport'
                  );
                const pagesRoot =
                  document.getElementById('pages');
                const counter =
                  document.getElementById('counter');
                const zoomInput =
                  document.getElementById('zoomInput');
                const zoomPicker =
                  document.getElementById('zoomPicker');
                const zoomMenuButton =
                  document.getElementById('zoomMenuButton');
                const zoomMenu =
                  document.getElementById('zoomMenu');
                const toolbar =
                  document.getElementById('toolbar');
                const fullscreenButton =
                  document.getElementById('fullscreen');

                $companionScript

                window.renaultSetSplitFocus =
                  (enabled, controlsVisible) => {
                    const active =
                      Boolean(enabled);
                    const visible =
                      Boolean(controlsVisible);

                    document.body.classList.toggle(
                      'renault-split-focus',
                      active
                    );
                    document.body.classList.toggle(
                      'controls-hidden',
                      active && !visible
                    );

                    if (!active) {
                      document.body.classList.remove(
                        'controls-hidden'
                      );
                    }

                    positionZoomMenu();
                  };

                window.renaultSetFullscreen =
                  enabled => {
                    const active =
                      Boolean(enabled);

                    window.renaultNativeFullscreenExpected =
                      active;

                    fullscreenButton
                      .setAttribute(
                        'aria-pressed',
                        active
                          ? 'true'
                          : 'false'
                      );
                    fullscreenButton.title =
                      active
                        ? 'Вийти з повноекранного режиму'
                        : 'Повноекранний режим';
                    fullscreenButton
                      .setAttribute(
                        'aria-label',
                        fullscreenButton.title
                      );

                    return (
                      fullscreenButton
                        .getAttribute(
                          'aria-pressed'
                        ) ===
                      (
                        active
                          ? 'true'
                          : 'false'
                      )
                    );
                  };

                function reapplyNativeFullscreenState() {
                  if (
                    typeof window
                      .renaultNativeFullscreenExpected !==
                    'boolean'
                  ) {
                    return;
                  }

                  window.renaultSetFullscreen(
                    window
                      .renaultNativeFullscreenExpected
                  );
                }

                window.addEventListener(
                  'resize',
                  reapplyNativeFullscreenState
                );

                window.addEventListener(
                  'pageshow',
                  reapplyNativeFullscreenState
                );

                document.addEventListener(
                  'visibilitychange',
                  () => {
                    if (!document.hidden) {
                      reapplyNativeFullscreenState();
                    }
                  }
                );

                let persistStateTimer = null;
                let zoomRenderTimer = null;
                let pinchActive = false;
                let pinchStartDistance = 0;
                let pinchStartZoom = zoom;
                let pinchAnchorSection = null;
                let pinchAnchorXRatio = 0.5;
                let pinchAnchorYRatio = 0.5;

                function persistViewerState() {
                  try {
                    const section =
                      sections[currentPage];
                    const sectionHeight =
                      Math.max(
                        1,
                        section
                          ? section.offsetHeight
                          : 1
                      );
                    const sectionTop =
                      section
                        ? section.offsetTop
                        : 0;
                    const pageOffsetRatio =
                      Math.max(
                        0,
                        Math.min(
                          1,
                          (
                            pdfViewport.scrollTop -
                            sectionTop
                          ) /
                          sectionHeight
                        )
                      );
                    const horizontalRange =
                      Math.max(
                        1,
                        pdfViewport.scrollWidth -
                          pdfViewport.clientWidth
                      );
                    const horizontalRatio =
                      Math.max(
                        0,
                        Math.min(
                          1,
                          pdfViewport.scrollLeft /
                            horizontalRange
                        )
                      );

                    window.localStorage.setItem(
                      viewerStateKey,
                      JSON.stringify({
                        page:
                          currentPage,
                        zoom:
                          zoom,
                        pageOffsetRatio:
                          pageOffsetRatio,
                        horizontalRatio:
                          horizontalRatio,
                        savedAt:
                          Date.now()
                      })
                    );
                  } catch (_) {
                    // State persistence is an optimization only.
                  }
                }

                function schedulePersistViewerState() {
                  if (persistStateTimer !== null) {
                    window.clearTimeout(
                      persistStateTimer
                    );
                  }

                  persistStateTimer =
                    window.setTimeout(
                      persistViewerState,
                      160
                    );
                }

                // Keep the popup outside toolbar rows so it cannot be clipped
                // by the one-row landscape toolbar.
                document.body.appendChild(
                  zoomMenu
                );

                const sections = [];

                for (let i = 0; i < pageCount; i++) {
                  const section =
                    document.createElement('section');
                  section.className = 'page';
                  section.dataset.page = String(i);

                  const loading =
                    document.createElement('div');
                  loading.className = 'loading';
                  loading.textContent =
                    'Сторінка ' + (i + 1);

                  const img =
                    document.createElement('img');
                  img.alt =
                    'PDF сторінка ' + (i + 1);
                  img.decoding = 'async';

                  img.addEventListener(
                    'load',
                    () => {
                      section.classList.add(
                        'loaded'
                      );
                    }
                  );

                  section.appendChild(loading);
                  section.appendChild(img);
                  pagesRoot.appendChild(section);
                  sections.push(section);
                }

                function targetWidth() {
                  const viewport =
                    Math.max(
                      360,
                      pdfViewport.clientWidth ||
                        document.documentElement.clientWidth
                    );
                  const dpr =
                    Math.min(
                      2.5,
                      window.devicePixelRatio || 1
                    );

                  return Math.round(
                    Math.min(
                      ${maxRenderWidth},
                      Math.max(
                        800,
                        viewport * dpr * zoom
                      )
                    )
                  );
                }

                function fastTargetWidth() {
                  const fullWidth =
                    targetWidth();

                  return zoom >= 1.5
                    ? Math.min(
                        fullWidth,
                        ${fastRenderWidth}
                      )
                    : fullWidth;
                }

                function pageUrl(
                  index,
                  renderWidth
                ) {
                  return (
                    '/__renault_pdf__/page' +
                    '?path=' +
                    encodeURIComponent(pdfPath) +
                    '&page=' +
                    index +
                    '&width=' +
                    renderWidth
                  );
                }

                function loadPage(
                  index,
                  renderWidth = targetWidth()
                ) {
                  if (
                    index < 0 ||
                    index >= pageCount
                  ) {
                    return;
                  }

                  const section =
                    sections[index];
                  const img =
                    section.querySelector('img');
                  const wanted =
                    pageUrl(
                      index,
                      renderWidth
                    );
                  const current =
                    img.getAttribute('src');
                  const currentWidth =
                    Number(
                      img.dataset.renderWidth ||
                      0
                    );

                  if (
                    current === wanted ||
                    (
                      current &&
                      currentWidth >=
                        renderWidth
                    )
                  ) {
                    return;
                  }

                  // Keep the previous bitmap visible while a sharper/new-width
                  // render is loading. This avoids the white flash on zoom,
                  // scrolling or orientation changes.
                  if (!current) {
                    section.classList.remove(
                      'loaded'
                    );
                  }

                  img.dataset.renderWidth =
                    String(renderWidth);
                  img.setAttribute(
                    'src',
                    wanted
                  );
                }

                function neighborRadius() {
                  if (${highZoomNeighborRadius} === 0) {
                    return 0;
                  }

                  return zoom >= 1.5
                    ? ${highZoomNeighborRadius}
                    : 2;
                }

                function retainRadius() {
                  if (${highZoomRetainRadius} === 0) {
                    return 0;
                  }

                  return zoom >= 1.5
                    ? ${highZoomRetainRadius}
                    : 4;
                }

                function neighborRenderWidth(
                  preferFast = false
                ) {
                  if (zoom >= 2.5) {
                    return Math.min(
                      targetWidth(),
                      ${neighborRenderWidthCap}
                    );
                  }

                  return preferFast
                    ? fastTargetWidth()
                    : targetWidth();
                }

                function evictFarPages(index) {
                  const radius =
                    retainRadius();

                  for (
                    let i = 0;
                    i < sections.length;
                    i++
                  ) {
                    if (
                      Math.abs(i - index) <=
                      radius
                    ) {
                      continue;
                    }

                    const section =
                      sections[i];
                    const img =
                      section.querySelector('img');

                    if (
                      img &&
                      img.hasAttribute('src')
                    ) {
                      img.removeAttribute('src');
                      delete img.dataset.renderWidth;
                      section.classList.remove(
                        'loaded'
                      );
                    }
                  }
                }

                let documentScrollActive = false;
                let documentScrollIdleTimer = null;
                let neighborLoadTimer = null;

                function loadAround(
                  index,
                  preferFast = false
                ) {
                  const renderWidth =
                    preferFast
                      ? fastTargetWidth()
                      : targetWidth();
                  const neighborWidth =
                    neighborRenderWidth(
                      preferFast
                    );

                  loadPage(
                    index,
                    renderWidth
                  );
                  evictFarPages(index);

                  if (neighborLoadTimer !== null) {
                    window.clearTimeout(
                      neighborLoadTimer
                    );
                  }

                  neighborLoadTimer =
                    window.setTimeout(
                      () => {
                        const radius =
                          neighborRadius();

                        for (
                          let offset = 1;
                          offset <= radius;
                          offset++
                        ) {
                          loadPage(
                            index - offset,
                            neighborWidth
                          );
                          loadPage(
                            index + offset,
                            neighborWidth
                          );
                        }
                      },
                      zoom >= 1.5
                        ? 90
                        : 60
                    );
                }

                const lazyObserver =
                  new IntersectionObserver(
                    entries => {
                      for (const entry of entries) {
                        if (
                          entry.isIntersecting &&
                          !pinchActive
                        ) {
                          const index =
                            Number(
                              entry.target.dataset.page
                            );
                          loadPage(
                            index,
                            documentScrollActive
                              ? fastTargetWidth()
                              : targetWidth()
                          );
                        }
                      }
                    },
                    {
                      root:
                        pdfViewport,
                      rootMargin:
                        '300px 0px'
                    }
                  );

                const currentObserver =
                  new IntersectionObserver(
                    entries => {
                      let best = null;

                      for (const entry of entries) {
                        if (
                          !entry.isIntersecting
                        ) {
                          continue;
                        }

                        if (
                          best === null ||
                          entry.intersectionRatio >
                            best.intersectionRatio
                        ) {
                          best = entry;
                        }
                      }

                      if (best !== null) {
                        setCurrentPage(
                          Number(
                            best.target.dataset.page
                          )
                        );
                        if (!pinchActive) {
                          loadAround(
                            currentPage,
                            documentScrollActive
                          );
                        }
                        schedulePersistViewerState();
                      }
                    },
                    {
                      root:
                        pdfViewport,
                      threshold:
                        [0.25, 0.5, 0.75]
                    }
                  );

                function setCurrentPage(
                  index
                ) {
                  const safe =
                    Math.max(
                      0,
                      Math.min(
                        pageCount - 1,
                        index
                      )
                    );

                  if (currentPage !== safe) {
                    currentPage = safe;
                    counter.textContent =
                      (currentPage + 1) +
                      ' / ' +
                      pageCount;
                  }

                  return safe;
                }

                function updateCurrentPageFromViewportCenter() {
                  if (sections.length === 0) {
                    return currentPage;
                  }

                  const centerY =
                    pdfViewport.scrollTop +
                    pdfViewport.clientHeight / 2;

                  let bestIndex =
                    currentPage;
                  let bestDistance =
                    Number.POSITIVE_INFINITY;

                  for (
                    let index = 0;
                    index < sections.length;
                    index++
                  ) {
                    const section =
                      sections[index];
                    const top =
                      section.offsetTop;
                    const bottom =
                      top +
                      section.offsetHeight;

                    if (
                      centerY >= top &&
                      centerY <= bottom
                    ) {
                      bestIndex = index;
                      bestDistance = 0;
                      break;
                    }

                    const sectionCenter =
                      top +
                      section.offsetHeight / 2;
                    const distance =
                      Math.abs(
                        sectionCenter -
                        centerY
                      );

                    if (distance < bestDistance) {
                      bestDistance =
                        distance;
                      bestIndex =
                        index;
                    }
                  }

                  return setCurrentPage(
                    bestIndex
                  );
                }

                function startPageObservers() {
                  sections.forEach(section => {
                    lazyObserver.observe(section);
                    currentObserver.observe(section);
                  });
                }

                function scrollToPage(index) {
                  const safe =
                    Math.max(
                      0,
                      Math.min(
                        pageCount - 1,
                        index
                      )
                    );

                  sections[safe]
                    .scrollIntoView({
                      behavior: 'smooth',
                      block: 'start'
                    });
                }

                document
                  .getElementById('prev')
                  .addEventListener(
                    'click',
                    () => {
                      scrollToPage(
                        currentPage - 1
                      );
                    }
                  );

                document
                  .getElementById('next')
                  .addEventListener(
                    'click',
                    () => {
                      scrollToPage(
                        currentPage + 1
                      );
                    }
                  );

                function applyZoomLayout() {
                  const viewport =
                    Math.max(
                      320,
                      pdfViewport.clientWidth ||
                        document.documentElement.clientWidth
                    );
                  const baseWidth =
                    Math.min(
                      1100,
                      Math.max(
                        320,
                        viewport - 12
                      )
                    );
                  const displayWidth =
                    Math.round(
                      baseWidth * zoom
                    );

                  for (const section of sections) {
                    section.style.width =
                      displayWidth + 'px';
                  }

                  if (
                    document.activeElement !==
                    zoomInput
                  ) {
                    zoomInput.value =
                      Math.round(
                        zoom * 100
                      ) + '%';
                  }
                }

                function rerenderForZoom() {
                  // Keep the currently decoded bitmap visible while the
                  // sharper zoom-specific render is requested.
                  lastRenderWidth =
                    targetWidth();
                  loadAround(
                    currentPage,
                    false
                  );
                }

                function scheduleZoomQualityRender(
                  delayMs = 180
                ) {
                  if (zoomRenderTimer !== null) {
                    window.clearTimeout(
                      zoomRenderTimer
                    );
                  }

                  zoomRenderTimer =
                    window.setTimeout(
                      () => {
                        zoomRenderTimer = null;

                        if (pinchActive) {
                          scheduleZoomQualityRender(
                            delayMs
                          );
                          return;
                        }

                        rerenderForZoom();
                      },
                      delayMs
                    );
                }

                function normalizeZoomPercent(percent) {
                  return Math.max(
                    50,
                    Math.min(
                      400,
                      Math.round(percent)
                    )
                  );
                }

                function updateZoomDisplay() {
                  zoomInput.value =
                    Math.round(
                      zoom * 100
                    ) + '%';
                }

                function setZoomPercent(percent) {
                  const normalized =
                    normalizeZoomPercent(
                      percent
                    );
                  const nextZoom =
                    normalized / 100;

                  if (
                    Math.abs(
                      nextZoom - zoom
                    ) < 0.001
                  ) {
                    zoomInput.value =
                      normalized + '%';
                    return;
                  }

                  zoom = nextZoom;
                  applyZoomLayout();
                  zoomInput.value =
                    normalized + '%';
                  scheduleZoomQualityRender();
                  schedulePersistViewerState();
                }

                function touchDistance(touches) {
                  if (touches.length < 2) {
                    return 0;
                  }

                  const dx =
                    touches[0].clientX -
                    touches[1].clientX;
                  const dy =
                    touches[0].clientY -
                    touches[1].clientY;

                  return Math.hypot(
                    dx,
                    dy
                  );
                }

                function touchCenter(touches) {
                  const rect =
                    pdfViewport
                      .getBoundingClientRect();
                  const clientX =
                    (
                      touches[0].clientX +
                      touches[1].clientX
                    ) / 2;
                  const clientY =
                    (
                      touches[0].clientY +
                      touches[1].clientY
                    ) / 2;

                  return {
                    clientX:
                      clientX,
                    clientY:
                      clientY,
                    x:
                      clientX -
                      rect.left,
                    y:
                      clientY -
                      rect.top
                  };
                }

                function beginPinch(event) {
                  if (event.touches.length !== 2) {
                    return;
                  }

                  const distance =
                    touchDistance(
                      event.touches
                    );

                  if (distance <= 0) {
                    return;
                  }

                  event.preventDefault();
                  pinchActive = true;
                  pinchStartDistance =
                    distance;
                  pinchStartZoom =
                    zoom;

                  const center =
                    touchCenter(
                      event.touches
                    );
                  const hit =
                    document.elementFromPoint(
                      center.clientX,
                      center.clientY
                    );
                  const hitSection =
                    hit &&
                    hit.closest
                      ? hit.closest(
                          '.page'
                        )
                      : null;
                  pinchAnchorSection =
                    hitSection ||
                    sections[currentPage] ||
                    null;

                  if (pinchAnchorSection) {
                    pinchAnchorXRatio =
                      Math.max(
                        0,
                        Math.min(
                          1,
                          (
                            pdfViewport.scrollLeft +
                            center.x -
                            pinchAnchorSection.offsetLeft
                          ) /
                          Math.max(
                            1,
                            pinchAnchorSection.offsetWidth
                          )
                        )
                      );
                    pinchAnchorYRatio =
                      Math.max(
                        0,
                        Math.min(
                          1,
                          (
                            pdfViewport.scrollTop +
                            center.y -
                            pinchAnchorSection.offsetTop
                          ) /
                          Math.max(
                            1,
                            pinchAnchorSection.offsetHeight
                          )
                        )
                      );
                  }

                  if (zoomRenderTimer !== null) {
                    window.clearTimeout(
                      zoomRenderTimer
                    );
                    zoomRenderTimer = null;
                  }

                  if (neighborLoadTimer !== null) {
                    window.clearTimeout(
                      neighborLoadTimer
                    );
                    neighborLoadTimer = null;
                  }

                  if (
                    documentScrollIdleTimer !== null
                  ) {
                    window.clearTimeout(
                      documentScrollIdleTimer
                    );
                    documentScrollIdleTimer = null;
                  }

                  documentScrollActive = false;
                }

                function movePinch(event) {
                  if (
                    !pinchActive ||
                    event.touches.length !== 2
                  ) {
                    return;
                  }

                  const distance =
                    touchDistance(
                      event.touches
                    );

                  if (
                    distance <= 0 ||
                    pinchStartDistance <= 0
                  ) {
                    return;
                  }

                  event.preventDefault();

                  const normalized =
                    normalizeZoomPercent(
                      pinchStartZoom *
                      100 *
                      (
                        distance /
                        pinchStartDistance
                      )
                    );
                  const nextZoom =
                    normalized / 100;

                  if (
                    Math.abs(
                      nextZoom - zoom
                    ) < 0.001
                  ) {
                    updateZoomDisplay();
                    return;
                  }

                  const center =
                    touchCenter(
                      event.touches
                    );

                  zoom = nextZoom;
                  applyZoomLayout();
                  updateZoomDisplay();

                  if (pinchAnchorSection) {
                    pdfViewport.scrollLeft =
                      pinchAnchorSection.offsetLeft +
                      pinchAnchorSection.offsetWidth *
                        pinchAnchorXRatio -
                      center.x;
                    pdfViewport.scrollTop =
                      pinchAnchorSection.offsetTop +
                      pinchAnchorSection.offsetHeight *
                        pinchAnchorYRatio -
                      center.y;
                  }

                  scheduleZoomQualityRender();
                  schedulePersistViewerState();
                }

                function endPinch() {
                  if (!pinchActive) {
                    return;
                  }

                  pinchActive = false;
                  pinchAnchorSection = null;
                  scheduleZoomQualityRender();
                  schedulePersistViewerState();
                }

                pdfViewport.addEventListener(
                  'touchstart',
                  beginPinch,
                  {
                    passive: false
                  }
                );

                pdfViewport.addEventListener(
                  'touchmove',
                  movePinch,
                  {
                    passive: false
                  }
                );

                pdfViewport.addEventListener(
                  'touchend',
                  event => {
                    if (
                      event.touches.length < 2
                    ) {
                      endPinch();
                    }
                  },
                  {
                    passive: true
                  }
                );

                pdfViewport.addEventListener(
                  'touchcancel',
                  endPinch,
                  {
                    passive: true
                  }
                );

                function parseZoomInput() {
                  const raw =
                    String(
                      zoomInput.value
                    )
                      .replace('%', '')
                      .replace(',', '.')
                      .trim();
                  const percent =
                    Number(raw);

                  if (!Number.isFinite(percent)) {
                    zoomInput.value =
                      Math.round(
                        zoom * 100
                      ) + '%';
                    return;
                  }

                  setZoomPercent(percent);
                }

                function changeZoom(deltaPercent) {
                  setZoomPercent(
                    zoom * 100 +
                    deltaPercent
                  );
                }

                document
                  .getElementById('minus')
                  .addEventListener(
                    'click',
                    () => {
                      changeZoom(
                        -zoomStepPercent
                      );
                    }
                  );

                document
                  .getElementById('plus')
                  .addEventListener(
                    'click',
                    () => {
                      changeZoom(
                        zoomStepPercent
                      );
                    }
                  );

                document
                  .getElementById('fitWidth')
                  .addEventListener(
                    'click',
                    () => {
                      setZoomPercent(100);
                      pdfViewport.scrollTo({
                        left: 0,
                        behavior: 'smooth'
                      });
                    }
                  );

                zoomInput.addEventListener(
                  'focus',
                  () => {
                    zoomInput.select();
                  }
                );

                zoomInput.addEventListener(
                  'keydown',
                  event => {
                    if (event.key === 'Enter') {
                      event.preventDefault();
                      parseZoomInput();
                      zoomInput.blur();
                    } else if (event.key === 'Escape') {
                      zoomInput.value =
                        Math.round(
                          zoom * 100
                        ) + '%';
                      zoomInput.blur();
                    }
                  }
                );

                zoomInput.addEventListener(
                  'blur',
                  () => {
                    parseZoomInput();
                  }
                );

                function positionZoomMenu() {
                  if (zoomMenu.hidden) {
                    return;
                  }

                  const buttonRect =
                    zoomMenuButton
                      .getBoundingClientRect();

                  const menuRect =
                    zoomMenu
                      .getBoundingClientRect();

                  const margin = 6;

                  const left =
                    Math.max(
                      margin,
                      Math.min(
                        window.innerWidth -
                          menuRect.width -
                          margin,
                        buttonRect.right -
                          menuRect.width
                      )
                    );

                  const roomBelow =
                    window.innerHeight -
                    buttonRect.bottom -
                    margin;

                  const top =
                    roomBelow >=
                    menuRect.height
                      ? buttonRect.bottom +
                        margin
                      : Math.max(
                          margin,
                          buttonRect.top -
                            menuRect.height -
                            margin
                        );

                  zoomMenu.style.left =
                    Math.round(left) + 'px';
                  zoomMenu.style.top =
                    Math.round(top) + 'px';
                }

                function closeZoomMenu() {
                  zoomMenu.hidden = true;
                  zoomMenuButton.setAttribute(
                    'aria-expanded',
                    'false'
                  );
                }

                function openZoomMenu() {
                  zoomMenu.hidden = false;
                  zoomMenuButton.setAttribute(
                    'aria-expanded',
                    'true'
                  );

                  requestAnimationFrame(
                    positionZoomMenu
                  );
                }

                zoomMenuButton.addEventListener(
                  'click',
                  event => {
                    event.preventDefault();
                    event.stopPropagation();

                    if (zoomMenu.hidden) {
                      openZoomMenu();
                    } else {
                      closeZoomMenu();
                    }
                  }
                );

                zoomMenu
                  .querySelectorAll(
                    '.zoom-option'
                  )
                  .forEach(option => {
                    option.addEventListener(
                      'click',
                      () => {
                        setZoomPercent(
                          Number(
                            option.dataset.zoom
                          )
                        );
                        closeZoomMenu();
                      }
                    );
                  });

                document.addEventListener(
                  'click',
                  event => {
                    if (
                      !zoomPicker.contains(
                        event.target
                      ) &&
                      !zoomMenu.contains(
                        event.target
                      )
                    ) {
                      closeZoomMenu();
                    }
                  }
                );

                document
                  .getElementById('save')
                  .addEventListener(
                    'click',
                    () => {
                      window.location.href =
                        'renaultsavepdf://save?path=' +
                        encodeURIComponent(pdfPath);
                    }
                  );

                document
                  .getElementById('fullscreen')
                  .addEventListener(
                    'click',
                    () => {
                      window.location.href =
                        'renaultfullscreen://toggle';
                    }
                  );

                let lastRenderWidth =
                  targetWidth();
                let resizeRenderTimer = null;

                window.addEventListener(
                  'resize',
                  () => {
                    applyZoomLayout();
                    positionZoomMenu();

                    if (resizeRenderTimer !== null) {
                      window.clearTimeout(
                        resizeRenderTimer
                      );
                    }

                    resizeRenderTimer =
                      window.setTimeout(
                        () => {
                          const nextWidth =
                            targetWidth();

                          if (
                            Math.abs(
                              nextWidth -
                              lastRenderWidth
                            ) >= 100
                          ) {
                            lastRenderWidth =
                              nextWidth;
                            loadAround(
                              currentPage
                            );
                          }
                        },
                        120
                      );
                  }
                );

                document
                  .getElementById('toolbar')
                  .addEventListener(
                    'scroll',
                    () => {
                      positionZoomMenu();
                    }
                  );

                pdfViewport.addEventListener(
                  'scroll',
                  () => {
                    positionZoomMenu();
                    schedulePersistViewerState();

                    if (pinchActive) {
                      return;
                    }

                    documentScrollActive = true;
                    updateCurrentPageFromViewportCenter();

                    if (
                      documentScrollIdleTimer !== null
                    ) {
                      window.clearTimeout(
                        documentScrollIdleTimer
                      );
                    }

                    if (zoom >= 1.5) {
                      loadAround(
                        currentPage,
                        true
                      );
                    }

                    documentScrollIdleTimer =
                      window.setTimeout(
                        () => {
                          documentScrollActive =
                            false;

                          if (pinchActive) {
                            return;
                          }

                          updateCurrentPageFromViewportCenter();
                          loadAround(
                            currentPage,
                            false
                          );
                        },
                        180
                      );
                  },
                  {
                    passive: true
                  }
                );

                window.addEventListener(
                  'beforeunload',
                  persistViewerState
                );

                applyZoomLayout();
                zoomInput.value =
                  Math.round(
                    zoom * 100
                  ) + '%';
                counter.textContent =
                  (currentPage + 1) +
                  ' / ' +
                  pageCount;

                window.requestAnimationFrame(
                  () => {
                    if (restoredViewerState) {
                      const section =
                        sections[currentPage];
                      const pageOffsetRatio =
                        Number.isFinite(
                          restoredViewerState
                            .pageOffsetRatio
                        )
                          ? Math.max(
                              0,
                              Math.min(
                                1,
                                restoredViewerState
                                  .pageOffsetRatio
                              )
                            )
                          : 0;
                      const horizontalRatio =
                        Number.isFinite(
                          restoredViewerState
                            .horizontalRatio
                        )
                          ? Math.max(
                              0,
                              Math.min(
                                1,
                                restoredViewerState
                                  .horizontalRatio
                              )
                            )
                          : 0;
                      const horizontalRange =
                        Math.max(
                          0,
                          pdfViewport.scrollWidth -
                            pdfViewport.clientWidth
                        );

                      pdfViewport.scrollTo({
                        top:
                          section
                            ? section.offsetTop +
                              section.offsetHeight *
                                pageOffsetRatio
                            : 0,
                        left:
                          horizontalRange *
                            horizontalRatio,
                        behavior: 'auto'
                      });
                    }

                    startPageObservers();
                    loadAround(
                      currentPage
                    );
                  }
                );
              </script>
            </body>
            </html>
        """.trimIndent()

        return htmlResponse(html)
    }

    private fun renderPageResponse(
        uri: Uri,
    ): WebResourceResponse {
        val path = uri.getQueryParameter(
            "path",
        ) ?: return textError(
            "Missing PDF path.",
        )

        val pageIndex = uri
            .getQueryParameter("page")
            ?.toIntOrNull()
            ?: return textError(
                "Invalid page.",
            )

        val requestedWidth = uri
            .getQueryParameter("width")
            ?.toIntOrNull()
            ?: 1000

        val width = requestedWidth
            .coerceIn(600, maxRenderWidth)
            .let {
                ((it + 99) / 200) * 200
            }

        val cacheKey =
            "$cacheNamespace|$path|$pageIndex|$width"

        sharedImageCache
            .get(
                cacheKey,
            )
            ?.let {
                return imageResponse(it)
            }

        val bytes = synchronized(
            sharedRenderLock,
        ) {
            sharedImageCache
                .get(
                    cacheKey,
                )
                ?: renderPage(
                    path = path,
                    pageIndex = pageIndex,
                    width = width,
                )?.also {
                    sharedImageCache.put(
                        cacheKey,
                        it,
                    )
                }
        } ?: return textError(
            "PDF page render failed.",
        )

        return imageResponse(bytes)
    }

    private fun readInfo(
        path: String,
    ): PdfInfo? {
        val pfd = resolver
            .openFileDescriptor(path)
            ?: return null

        return runCatching {
            pfd.use { descriptor ->
                PdfRenderer(
                    descriptor,
                ).use { renderer ->
                    if (
                        renderer.pageCount <= 0
                    ) {
                        null
                    } else {
                        renderer.openPage(
                            0,
                        ).use { page ->
                            PdfInfo(
                                pageCount =
                                    renderer.pageCount,
                                firstPageWidth =
                                    page.width,
                                firstPageHeight =
                                    page.height,
                            )
                        }
                    }
                }
            }
        }.getOrNull()
    }

    private fun renderPage(
        path: String,
        pageIndex: Int,
        width: Int,
    ): ByteArray? {
        val pfd = resolver
            .openFileDescriptor(path)
            ?: return null

        return runCatching {
            pfd.use { descriptor ->
                PdfRenderer(
                    descriptor,
                ).use { renderer ->
                    if (
                        pageIndex !in
                        0 until renderer.pageCount
                    ) {
                        null
                    } else {
                        renderer.openPage(
                            pageIndex,
                        ).use { page ->
                            val height =
                                (
                                    width.toDouble() *
                                        page.height /
                                        page.width
                                )
                                    .toInt()
                                    .coerceAtLeast(1)

                            val bitmap =
                                Bitmap.createBitmap(
                                    width,
                                    height,
                                    Bitmap.Config.ARGB_8888,
                                )

                            try {
                                bitmap.eraseColor(
                                    Color.WHITE,
                                )

                                page.render(
                                    bitmap,
                                    null,
                                    null,
                                    PdfRenderer.Page
                                        .RENDER_MODE_FOR_DISPLAY,
                                )

                                ByteArrayOutputStream()
                                    .use { out ->
                                        bitmap.compress(
                                            Bitmap.CompressFormat.PNG,
                                            100,
                                            out,
                                        )
                                        out.toByteArray()
                                    }
                            } finally {
                                bitmap.recycle()
                            }
                        }
                    }
                }
            }
        }.getOrNull()
    }

    private fun imageResponse(
        bytes: ByteArray,
    ): WebResourceResponse =
        WebResourceResponse(
            "image/png",
            null,
            200,
            "OK",
            mapOf(
                "Cache-Control" to
                    "public, max-age=86400",
            ),
            ByteArrayInputStream(bytes),
        )

    private fun htmlResponse(
        html: String,
    ): WebResourceResponse =
        WebResourceResponse(
            "text/html",
            "UTF-8",
            200,
            "OK",
            mapOf(
                "Cache-Control" to
                    "no-store",
            ),
            ByteArrayInputStream(
                html.toByteArray(
                    Charsets.UTF_8,
                )
            ),
        )

    private fun errorHtml(
        title: String,
        body: String,
    ): WebResourceResponse {
        val html = """
            <!doctype html>
            <html lang="uk">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width,initial-scale=1">
              <style>
                body {
                  margin: 0;
                  padding: 24px;
                  background: #101318;
                  color: #f3f6f8;
                  font-family: system-ui, sans-serif;
                }
                p {
                  color: #aab5c2;
                  overflow-wrap: anywhere;
                }
              </style>
            </head>
            <body>
              <h2>${escape(title)}</h2>
              <p>${escape(body)}</p>
            </body>
            </html>
        """.trimIndent()

        return htmlResponse(html)
    }

    private fun textError(
        text: String,
    ): WebResourceResponse =
        WebResourceResponse(
            "text/plain",
            "UTF-8",
            400,
            "Bad Request",
            mapOf(
                "Cache-Control" to
                    "no-store",
            ),
            ByteArrayInputStream(
                text.toByteArray(
                    Charsets.UTF_8,
                )
            ),
        )

    private fun escape(
        value: String,
    ): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    companion object {
        private val sharedRenderLock =
            Any()

        private val sharedImageCache =
            object :
                LruCache<String, ByteArray>(
                    32 * 1024 * 1024,
                ) {
                override fun sizeOf(
                    key: String,
                    value: ByteArray,
                ): Int =
                    value.size
            }

        const val PAGE_ENDPOINT =
            "__renault_pdf__/page"
    }
}
