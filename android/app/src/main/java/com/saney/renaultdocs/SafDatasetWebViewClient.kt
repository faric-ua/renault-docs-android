package com.saney.renaultdocs

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream
import org.json.JSONObject
import org.json.JSONTokener
import java.util.Locale

class SafDatasetWebViewClient(
    private val context: Context,
    private val treeUri: Uri,
    private val legacyStandaloneMode: Boolean = false,
    private val legacySectionCode: String = "",
    private val legacySectionRootEntrypoint: String = "",
    private val legacySectionFallbackEntrypoint: String = "",
    private val onHybridSectionReady: (Boolean) -> Unit = {},
    private val onSavePdf: (String) -> Unit = {},
    private val onToggleFullscreen: () -> Unit = {},
    private val onToggleCompanion: () -> Unit = {},
    private val pdfStateScope: String = "main",
    private val pdfCompanionControlEnabled: Boolean = false,
    private val pdfCompactMode: Boolean = false,
) : WebViewClient() {
    @Volatile
    private var activeHybridSectionCode: String =
        legacySectionCode

    private val clientStartedAtMs: Long =
        SystemClock.elapsedRealtime()

    @Volatile
    private var fastPackPrepareStartedAtMs: Long? =
        null

    @Volatile
    private var fastPackPrepareFinishedAtMs: Long? =
        null

    @Volatile
    private var fastPackCopiedToLocalCache: Boolean? =
        null

    private val resolver =
        SafDatasetResolver(
            context = context,
            treeUri = treeUri,
        )

    private val appSettings =
        AppSettings(context)

    private val pdfLayer =
        AndroidPdfLayer(
            resolver = resolver,
            cacheNamespace =
                treeUri.toString(),
            initialZoomPercent =
                appSettings
                    .pdfDefaultZoomPercent,
            zoomStepPercent =
                appSettings
                    .pdfZoomStepPercent,
            viewerStateScope =
                pdfStateScope,
            companionControlEnabled =
                pdfCompanionControlEnabled,
            compactMode =
                pdfCompactMode,
        )

    private val fastArchive =
        lazy(
            LazyThreadSafetyMode.SYNCHRONIZED,
        ) {
            fastPackPrepareStartedAtMs =
                SystemClock.elapsedRealtime()

            val prepared =
                FastContentArchive
                    .prepare(
                        context = context,
                        treeUri = treeUri,
                    )
                    .getOrNull()

            fastPackPrepareFinishedAtMs =
                SystemClock.elapsedRealtime()
            fastPackCopiedToLocalCache =
                prepared
                    ?.copiedToLocalCache

            prepared?.archive
        }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?,
    ): WebResourceResponse? {
        val requestUrl =
            request?.url?.toString()
                ?: return null

        val relativePath =
            DatasetVirtualUrl
                .relativePath(requestUrl)
                ?: return blockExternalResource(
                    requestUrl,
                )

        if (relativePath.isBlank()) {
            return htmlResponse(
                statusCode = 404,
                reason = "Not Found",
                title = "Renault Docs",
                body = "Порожній шлях усередині dataset.",
            )
        }

        pdfLayer.intercept(
            requestUrl = requestUrl,
            relativePath = relativePath,
        )?.let {
            return it
        }

        fastArchive.value
            ?.open(
                relativePath,
            )
            ?.let { stream ->
                return WebResourceResponse(
                    mimeType(relativePath),
                    encoding(relativePath),
                    200,
                    "OK",
                    mapOf(
                        "Cache-Control" to
                            "public, max-age=86400",
                        "X-Renault-Source" to
                            "fast-pack",
                    ),
                    stream,
                )
            }

        val node =
            resolver.resolve(
                relativePath,
            )
                ?: return htmlResponse(
                    statusCode = 404,
                    reason = "Not Found",
                    title = "Файл не знайдено",
                    body = relativePath,
                )

        if (node.isDirectory) {
            return htmlResponse(
                statusCode = 404,
                reason = "Not Found",
                title = "Це не файл",
                body = relativePath,
            )
        }

        val stream =
            resolver.openInputStream(
                relativePath,
            )
                ?: return htmlResponse(
                    statusCode = 500,
                    reason = "Read Error",
                    title = "Не вдалося прочитати файл",
                    body = relativePath,
                )

        return WebResourceResponse(
            mimeType(relativePath),
            encoding(relativePath),
            200,
            "OK",
            mapOf(
                "Cache-Control" to
                    "public, max-age=3600",
            ),
            stream,
        )
    }

    override fun onPageFinished(
        view: WebView?,
        url: String?,
    ) {
        super.onPageFinished(
            view,
            url,
        )

        val relativePath =
            url
                ?.let {
                    DatasetVirtualUrl
                        .relativePath(it)
                }
                ?: return

        if (
            relativePath.equals(
                "_renault/START.html",
                ignoreCase = true,
            )
        ) {
            sortClassicCatalogByDate(
                view,
            )
        }

        if (
            relativePath.equals(
                "_renault/README_UA.html",
                ignoreCase = true,
            )
        ) {
            applyReadmeMobileLayout(
                view,
            )
        }

        if (
            legacyStandaloneMode &&
            isLegacyHtml(
                relativePath,
            )
        ) {
            applyStandaloneLegacyCompat(
                view,
            )
        }

        if (
            activeHybridSectionCode.isNotBlank() &&
            legacySectionRootEntrypoint
                .isNotBlank() &&
            isLegacyHtml(
                relativePath,
            )
        ) {
            applyHybridSectionShell(
                view = view,
                sectionCode =
                    activeHybridSectionCode,
            )
        }
    }

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?,
    ): Boolean {
        val url =
            request?.url
                ?: return false

        if (
            url.scheme.equals(
                PDF_COMPANION_SCHEME,
                ignoreCase = true,
            )
        ) {
            view?.post {
                onToggleCompanion()
            }
            return true
        }

        if (
            url.scheme.equals(
                PDF_FULLSCREEN_SCHEME,
                ignoreCase = true,
            )
        ) {
            view?.post {
                onToggleFullscreen()
            }
            return true
        }

        if (
            url.scheme.equals(
                PDF_SAVE_SCHEME,
                ignoreCase = true,
            )
        ) {
            val path =
                url.getQueryParameter(
                    "path",
                )
                    ?.takeIf {
                        it
                            .lowercase(
                                Locale.ROOT,
                            )
                            .endsWith(".pdf")
                    }

            if (
                !path.isNullOrBlank()
            ) {
                view?.post {
                    onSavePdf(path)
                }
            }

            return true
        }

        if (
            DatasetVirtualUrl.isLocal(
                url.toString(),
            )
        ) {
            return false
        }

        if (!request.isForMainFrame) {
            return true
        }

        return runCatching {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    url,
                ).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK,
                )
            )
            true
        }.getOrDefault(true)
    }

    private fun sameDatasetPath(
        left: String,
        right: String,
    ): Boolean =
        left
            .replace(
                '\\',
                '/',
            )
            .trimStart('/')
            .equals(
                right
                    .replace(
                        '\\',
                        '/',
                    )
                    .trimStart('/'),
                ignoreCase = true,
            )

    private fun applyHybridSectionShell(
        view: WebView?,
        sectionCode: String,
    ) {
        view
            ?: return

        val safeCode =
            sectionCode
                .filter {
                    it.isDigit()
                }
                .take(3)

        if (
            safeCode.length != 3
        ) {
            return
        }

        activeHybridSectionCode =
            safeCode

        val script =
            """
            (() => {
              const targetCode =
                "$safeCode";

              const stateKey =
                '__renaultHybridSection_' +
                targetCode;

              const existingState =
                window[stateKey];

              if (
                existingState &&
                (
                  existingState.done ||
                  existingState.timer
                )
              ) {
                return true;
              }

              const state =
                existingState || {
                  done: false,
                  attempts: 0,
                  triggerAttempts: 0,
                  lastTriggerAt: 0,
                  phase: 'waiting',
                  startedAt:
                    performance.now(),
                  durationMs: null
                };

              window[stateKey] =
                state;

              const cleanText =
                value =>
                  (value || '')
                    .replace(
                      /\s+/g,
                      ' '
                    )
                    .trim();

              const codeFrom =
                value => {
                  const match =
                    cleanText(value)
                      .match(
                        /^(\d{3})(?!\d)/
                      );

                  return match
                    ? match[1]
                    : null;
                };

              const frameByName =
                name => {
                  try {
                    const named =
                      window.frames[name];

                    if (named) {
                      return named;
                    }
                  } catch (_) {
                    // Fall through to element lookup.
                  }

                  try {
                    const frame =
                      Array.from(
                        document.querySelectorAll(
                          'frame[name], iframe[name]'
                        )
                      )
                        .find(
                          element =>
                            (
                              element.getAttribute(
                                'name'
                              ) || ''
                            ) === name
                        );

                    return frame
                      ? frame.contentWindow
                      : null;
                  } catch (_) {
                    return null;
                  }
                };

              const hrefOf =
                current => {
                  try {
                    return (
                      current &&
                      current.location
                        ? current.location.href
                        : ''
                    );
                  } catch (_) {
                    return '';
                  }
                };

              const titleOf =
                current => {
                  try {
                    return (
                      current &&
                      current.document
                        ? current.document.title ||
                          ''
                        : ''
                    );
                  } catch (_) {
                    return '';
                  }
                };

              const textOf =
                current => {
                  try {
                    const body =
                      current &&
                      current.document
                        ? current.document.body
                        : null;

                    return cleanText(
                      body
                        ? body.innerText ||
                          body.textContent ||
                          ''
                        : ''
                    );
                  } catch (_) {
                    return '';
                  }
                };

              const targetSelected =
                () => {
                  const menuWindow =
                    frameByName('menu');
                  const navWindow =
                    frameByName('nav');

                  const exactMenu =
                    '/menu/' +
                    targetCode.toLowerCase() +
                    '.htm';
                  const exactPc =
                    '/pc/' +
                    targetCode.toLowerCase() +
                    '.htm';
                  const titleMarker =
                    'cmp ' +
                    targetCode.toLowerCase();

                  const signals = [
                    hrefOf(menuWindow),
                    hrefOf(navWindow),
                    titleOf(menuWindow),
                    titleOf(navWindow),
                    textOf(menuWindow)
                      .slice(
                        0,
                        120
                      ),
                    textOf(navWindow)
                      .slice(
                        0,
                        120
                      )
                  ]
                    .map(
                      value =>
                        (value || '')
                          .toLowerCase()
                    );

                  return signals.some(
                    value =>
                      value.includes(
                        exactMenu
                      ) ||
                      value.includes(
                        exactPc
                      ) ||
                      value.includes(
                        titleMarker
                      )
                  );
                };

              const findNavigationWindow =
                () => {
                  const org =
                    frameByName('org');

                  if (!org) {
                    return null;
                  }

                  try {
                    const text =
                      textOf(org);

                    if (
                      text.includes(
                        targetCode
                      )
                    ) {
                      return org;
                    }
                  } catch (_) {
                    // Named frame is still the
                    // deterministic navigation frame.
                  }

                  return org;
                };

              const findTargetLeaf =
                documentRef => {
                  const root =
                    documentRef.body ||
                    documentRef.documentElement;

                  if (!root) {
                    return null;
                  }

                  try {
                    const walker =
                      documentRef.createTreeWalker(
                        root,
                        NodeFilter.SHOW_TEXT
                      );

                    let node =
                      walker.nextNode();

                    while (node) {
                      const parent =
                        node.parentElement;
                      const tag =
                        parent
                          ? parent.tagName
                          : '';

                      if (
                        tag !== 'SCRIPT' &&
                        tag !== 'STYLE' &&
                        codeFrom(
                          node.nodeValue
                        ) ===
                          targetCode
                      ) {
                        return parent;
                      }

                      node =
                        walker.nextNode();
                    }
                  } catch (_) {
                    // Fall through to element scan.
                  }

                  const nodes =
                    Array.from(
                      documentRef
                        .querySelectorAll(
                          'a, area, tr, td, li, p, div, span, option, button, input, [onclick]'
                        )
                    )
                      .filter(
                        element =>
                          codeFrom(
                            element.innerText ||
                            element.textContent ||
                            element.value ||
                            element.alt ||
                            element.title ||
                            ''
                          ) ===
                            targetCode
                      )
                      .sort(
                        (left, right) =>
                          cleanText(
                            left.textContent ||
                            left.value ||
                            ''
                          ).length -
                          cleanText(
                            right.textContent ||
                            right.value ||
                            ''
                          ).length
                      );

                  return nodes.length > 0
                    ? nodes[0]
                    : null;
                };

              const triggerSection =
                navigationWindow => {
                  let documentRef;

                  try {
                    documentRef =
                      navigationWindow.document;
                  } catch (_) {
                    return false;
                  }

                  const matched =
                    findTargetLeaf(
                      documentRef
                    );

                  if (!matched) {
                    return false;
                  }

                  if (
                    matched.tagName ===
                    'OPTION'
                  ) {
                    const select =
                      matched.parentElement;

                    if (
                      select &&
                      select.tagName ===
                        'SELECT'
                    ) {
                      select.value =
                        matched.value;

                      select.dispatchEvent(
                        new Event(
                          'change',
                          {
                            bubbles: true
                          }
                        )
                      );

                      return true;
                    }
                  }

                  const actionable =
                    matched.closest
                      ? matched.closest(
                          'a[href], area[href], button, input, [onclick], [onmousedown], [onmouseup]'
                        )
                      : null;

                  const target =
                    actionable ||
                    matched;

                  try {
                    const mouse =
                      type =>
                        target.dispatchEvent(
                          new MouseEvent(
                            type,
                            {
                              bubbles: true,
                              cancelable: true,
                              view:
                                navigationWindow
                            }
                          )
                        );

                    mouse('mousedown');
                    mouse('mouseup');

                    if (
                      typeof target.click ===
                      'function'
                    ) {
                      target.click();
                    } else {
                      mouse('click');
                    }

                    return true;
                  } catch (_) {
                    try {
                      matched.dispatchEvent(
                        new MouseEvent(
                          'click',
                          {
                            bubbles: true,
                            cancelable: true,
                            view:
                              navigationWindow
                          }
                        )
                      );

                      return true;
                    } catch (_) {
                      return false;
                    }
                  }
                };

              const projectClassicNavigationBranch =
                navigationWindow => {
                  const titreWindow =
                    frameByName('titre');
                  const menuWindow =
                    frameByName('menu');
                  const navWindow =
                    frameByName('nav');
                  const docWindow =
                    frameByName('doc');

                  if (
                    !navigationWindow ||
                    !titreWindow ||
                    !menuWindow ||
                    !navWindow ||
                    !docWindow
                  ) {
                    return false;
                  }

                  let orgFrame;
                  let titreFrame;

                  try {
                    orgFrame =
                      navigationWindow
                        .frameElement;
                    titreFrame =
                      titreWindow
                        .frameElement;
                  } catch (_) {
                    return false;
                  }

                  if (
                    !orgFrame ||
                    !titreFrame
                  ) {
                    return false;
                  }

                  const leftBranch =
                    orgFrame.parentElement;

                  if (
                    !leftBranch ||
                    leftBranch.tagName !==
                      'FRAMESET' ||
                    titreFrame.parentElement !==
                      leftBranch
                  ) {
                    return false;
                  }

                  const outer =
                    leftBranch.parentElement;

                  if (
                    !outer ||
                    outer.tagName !==
                      'FRAMESET'
                  ) {
                    return false;
                  }

                  const children =
                    Array.from(
                      outer.children
                    )
                      .filter(
                        element =>
                          element.tagName ===
                            'FRAME' ||
                          element.tagName ===
                            'FRAMESET'
                      );

                  const branchIndex =
                    children.indexOf(
                      leftBranch
                    );

                  if (
                    branchIndex < 0 ||
                    children.length < 2
                  ) {
                    return false;
                  }

                  const attribute =
                    outer.hasAttribute(
                      'cols'
                    )
                      ? 'cols'
                      : (
                          outer.hasAttribute(
                            'rows'
                          )
                            ? 'rows'
                            : ''
                        );

                  if (!attribute) {
                    return false;
                  }

                  const raw =
                    outer.getAttribute(
                      attribute
                    ) || '';

                  let parts =
                    raw
                      .split(',')
                      .map(
                        part =>
                          part.trim()
                      )
                      .filter(
                        part =>
                          part.length > 0
                      );

                  if (
                    parts.length !==
                    children.length
                  ) {
                    parts =
                      children.map(
                        () => '*'
                      );
                  }

                  parts[branchIndex] =
                    '0';

                  if (
                    children.length === 2
                  ) {
                    parts[
                      branchIndex === 0
                        ? 1
                        : 0
                    ] = '*';
                  }

                  const desired =
                    parts.join(',');

                  const applyProjection =
                    () => {
                      if (
                        outer.getAttribute(
                          attribute
                        ) !== desired
                      ) {
                        outer.setAttribute(
                          attribute,
                          desired
                        );
                      }
                    };

                  applyProjection();

                  try {
                    leftBranch.style.pointerEvents =
                      'none';
                  } catch (_) {
                    // Geometry is owned by outer frameset.
                  }

                  if (
                    !state.projectionObserver
                  ) {
                    const observer =
                      new MutationObserver(
                        () => {
                          applyProjection();
                        }
                      );

                    observer.observe(
                      outer,
                      {
                        attributes: true,
                        attributeFilter: [
                          attribute
                        ]
                      }
                    );

                    state.projectionObserver =
                      observer;
                  }

                  state.projection = {
                    attribute,
                    value: desired,
                    branchIndex,
                    originalValue: raw,
                    navigationFrame:
                      'org',
                    titleFrame:
                      'titre',
                    preservedFrames: [
                      'menu',
                      'nav',
                      'doc'
                    ]
                  };

                  return true;
                };

              const finishIfReady =
                () => {
                  if (
                    !targetSelected()
                  ) {
                    return false;
                  }

                  const navigationWindow =
                    findNavigationWindow();

                  if (
                    !navigationWindow ||
                    !projectClassicNavigationBranch(
                      navigationWindow
                    )
                  ) {
                    return false;
                  }

                  state.done = true;
                  state.phase =
                    'projected';
                  state.durationMs =
                    Math.round(
                      performance.now() -
                      (
                        state.startedAt ||
                        performance.now()
                      )
                    );
                  state.menuWindow =
                    navigationWindow;

                  if (state.timer) {
                    window.clearInterval(
                      state.timer
                    );
                    state.timer = null;
                  }

                  return true;
                };

              const stopIfTimedOut =
                () => {
                  if (
                    state.attempts < 80
                  ) {
                    return false;
                  }

                  state.phase =
                    'timeout';

                  if (state.timer) {
                    window.clearInterval(
                      state.timer
                    );
                    state.timer = null;
                  }

                  return true;
                };

              const tryOpen =
                () => {
                  if (
                    state.done
                  ) {
                    return;
                  }

                  state.attempts += 1;

                  if (
                    finishIfReady()
                  ) {
                    return;
                  }

                  const navigationWindow =
                    findNavigationWindow();

                  if (!navigationWindow) {
                    stopIfTimedOut();
                    return;
                  }

                  const now =
                    Date.now();

                  if (
                    !state.lastTriggerAt ||
                    now -
                      state.lastTriggerAt >=
                        850
                  ) {
                    if (
                      triggerSection(
                        navigationWindow
                      )
                    ) {
                      state.triggerAttempts +=
                        1;
                      state.lastTriggerAt =
                        now;
                      state.phase =
                        'triggered';
                    }
                  }

                  window.setTimeout(
                    finishIfReady,
                    80
                  );

                  stopIfTimedOut();
                };

              state.timer =
                window.setInterval(
                  tryOpen,
                  150
                );

              tryOpen();

              return true;
            })();
            """.trimIndent()

        view.evaluateJavascript(
            script,
            null,
        )

        pollHybridSectionReady(
            view = view,
            stateKey =
                "__renaultHybridSection_" +
                    safeCode,
            attempt = 0,
        )
    }

    private fun pollHybridSectionReady(
        view: WebView,
        stateKey: String,
        attempt: Int,
    ) {
        val escapedKey =
            JSONObject.quote(
                stateKey,
            )

        view.evaluateJavascript(
            """
            (() => {
              const state =
                window[$escapedKey];

              return !!(
                state &&
                state.done &&
                state.phase ===
                  'projected'
              );
            })();
            """.trimIndent(),
        ) { result ->
            if (
                result == "true"
            ) {
                view.postDelayed(
                    {
                        onHybridSectionReady(
                            true,
                        )
                    },
                    320L,
                )
                return@evaluateJavascript
            }

            if (
                attempt >= 90
            ) {
                onHybridSectionReady(
                    false,
                )
                return@evaluateJavascript
            }

            view.postDelayed(
                {
                    pollHybridSectionReady(
                        view = view,
                        stateKey = stateKey,
                        attempt =
                            attempt + 1,
                    )
                },
                150L,
            )
        }
    }

    private fun isLegacyHtml(
        relativePath: String,
    ): Boolean {
        val normalized =
            relativePath.replace(
                '\\',
                '/',
            )

        if (
            normalized.startsWith(
                "_renault/",
                ignoreCase = true,
            )
        ) {
            return false
        }

        return normalized
            .substringAfterLast(
                '.',
                "",
            )
            .lowercase(
                Locale.ROOT,
            ) in setOf(
                "htm",
                "html",
            )
    }

    private fun applyStandaloneLegacyCompat(
        view: WebView?,
    ) {
        view
            ?: return

        val script =
            """
            (() => {
              const transparent = value =>
                !value ||
                value === 'transparent' ||
                value === 'rgba(0, 0, 0, 0)';

              const html =
                document.documentElement;
              const body =
                document.body;

              if (html && body) {
                const htmlBg =
                  getComputedStyle(html)
                    .backgroundColor;
                const bodyBg =
                  getComputedStyle(body)
                    .backgroundColor;

                if (
                  transparent(htmlBg) &&
                  transparent(bodyBg)
                ) {
                  html.style.backgroundColor =
                    '#ffffff';
                  body.style.backgroundColor =
                    '#ffffff';
                  html.style.colorScheme =
                    'light';
                }
              }

              const standardTarget =
                value => {
                  const normalized =
                    (value || '')
                      .trim()
                      .toLowerCase();

                  return (
                    normalized === '' ||
                    normalized === '_self' ||
                    normalized === '_top' ||
                    normalized === '_parent' ||
                    normalized === '_blank'
                  );
                };

              const frameExists =
                name => {
                  if (!name) {
                    return false;
                  }

                  try {
                    return window.frames[name] != null;
                  } catch (_) {
                    return false;
                  }
                };

              const isLocalCandidate =
                value => {
                  const candidate =
                    (value || '')
                      .trim();

                  if (!candidate) {
                    return false;
                  }

                  if (
                    candidate.startsWith('#') ||
                    candidate
                      .toLowerCase()
                      .startsWith('javascript:')
                  ) {
                    return false;
                  }

                  try {
                    const resolved =
                      new URL(
                        candidate,
                        window.location.href
                      );

                    return (
                      resolved.origin ===
                      window.location.origin
                    );
                  } catch (_) {
                    return false;
                  }
                };

              const extractCandidate =
                source => {
                  const text =
                    (source || '')
                      .replace(
                        /&quot;/gi,
                        '"'
                      )
                      .replace(
                        /&#39;/gi,
                        "'"
                      );

                  const match =
                    text.match(
                      /["']([^"'\\]+?\.(?:html?|pdf)(?:[?#][^"']*)?)["']/i
                    );

                  return match
                    ? match[1]
                    : null;
                };

              const navigateCandidate =
                candidate => {
                  if (
                    !isLocalCandidate(
                      candidate
                    )
                  ) {
                    return false;
                  }

                  window.location.href =
                    candidate;
                  return true;
                };

              const bridgeHost =
                (() => {
                  let host =
                    document.getElementById(
                      '__renaultFrameBridgeHost'
                    );

                  if (host) {
                    return host;
                  }

                  host =
                    document.createElement(
                      'div'
                    );
                  host.id =
                    '__renaultFrameBridgeHost';
                  host.style.display =
                    'none';

                  (
                    document.body ||
                    document.documentElement
                  ).appendChild(host);

                  return host;
                })();

              const attachBridgeWatcher =
                frame => {
                  if (
                    frame.dataset
                      .renaultBridgeWatched ===
                    '1'
                  ) {
                    return;
                  }

                  frame.dataset
                    .renaultBridgeWatched =
                    '1';

                  frame.addEventListener(
                    'load',
                    () => {
                      let href = '';

                      try {
                        href =
                          frame.contentWindow
                            .location.href;
                      } catch (_) {
                        return;
                      }

                      if (
                        !href ||
                        href === 'about:blank' ||
                        href ===
                          window.location.href
                      ) {
                        return;
                      }

                      if (
                        isLocalCandidate(href)
                      ) {
                        window.location.href =
                          href;
                      }
                    }
                  );
                };

              const ensureNamedBridge =
                name => {
                  const normalized =
                    (name || '').trim();

                  if (
                    !normalized ||
                    standardTarget(normalized) ||
                    frameExists(normalized)
                  ) {
                    return;
                  }

                  const frame =
                    document.createElement(
                      'iframe'
                    );
                  frame.name =
                    normalized;
                  frame.setAttribute(
                    'aria-hidden',
                    'true'
                  );
                  frame.src =
                    'about:blank';

                  attachBridgeWatcher(
                    frame
                  );
                  bridgeHost.appendChild(
                    frame
                  );
                };

              const ensureNumericBridges =
                maxIndex => {
                  if (
                    !Number.isFinite(
                      maxIndex
                    ) ||
                    maxIndex < 0
                  ) {
                    return;
                  }

                  while (
                    window.frames.length <=
                    maxIndex
                  ) {
                    const frame =
                      document.createElement(
                        'iframe'
                      );
                    frame.setAttribute(
                      'aria-hidden',
                      'true'
                    );
                    frame.src =
                      'about:blank';

                    attachBridgeWatcher(
                      frame
                    );
                    bridgeHost.appendChild(
                      frame
                    );
                  }
                };

              const discoverFrameReferences =
                source => {
                  const text =
                    source || '';

                  const numericPattern =
                    /(?:parent|top|window)?\s*\.?\s*frames\s*\[\s*(\d+)\s*\]/gi;

                  let numericMatch;
                  let maxIndex = -1;

                  while (
                    (
                      numericMatch =
                        numericPattern.exec(
                          text
                        )
                    ) !== null
                  ) {
                    maxIndex =
                      Math.max(
                        maxIndex,
                        Number(
                          numericMatch[1]
                        )
                      );
                  }

                  ensureNumericBridges(
                    maxIndex
                  );

                  const namedPatterns = [
                    /(?:parent|top|window)?\s*\.?\s*frames\s*\[\s*["']([^"']+)["']\s*\]/gi,
                    /(?:parent|top)\s*\.\s*([A-Za-z_][\w-]*)\s*\.\s*(?:location|document)/gi
                  ];

                  for (
                    const pattern
                    of namedPatterns
                  ) {
                    let match;

                    while (
                      (
                        match =
                          pattern.exec(text)
                      ) !== null
                    ) {
                      ensureNamedBridge(
                        match[1]
                      );
                    }
                  }
                };

              discoverFrameReferences(
                (
                  document.documentElement &&
                  document.documentElement
                    .outerHTML
                ) || ''
              );

              document
                .querySelectorAll(
                  'script[src]'
                )
                .forEach(
                  scriptElement => {
                    const src =
                      scriptElement.src;

                    if (!src) {
                      return;
                    }

                    try {
                      const parsed =
                        new URL(
                          src,
                          window.location.href
                        );

                      if (
                        parsed.origin !==
                        window.location.origin
                      ) {
                        return;
                      }
                    } catch (_) {
                      return;
                    }

                    fetch(src)
                      .then(
                        response =>
                          response.ok
                            ? response.text()
                            : ''
                      )
                      .then(
                        discoverFrameReferences
                      )
                      .catch(
                        () => {}
                      );
                  }
                );

              const repairTarget =
                element => {
                  if (!element) {
                    return;
                  }

                  const target =
                    element.getAttribute(
                      'target'
                    );

                  if (
                    !target ||
                    standardTarget(target) ||
                    frameExists(target)
                  ) {
                    return;
                  }

                  element.setAttribute(
                    'target',
                    '_self'
                  );
                };

              document
                .querySelectorAll(
                  'a[target], area[target], form[target], base[target]'
                )
                .forEach(
                  repairTarget
                );

              if (
                !window.__renaultStandaloneObserver
              ) {
                const observer =
                  new MutationObserver(
                    mutations => {
                      for (
                        const mutation
                        of mutations
                      ) {
                        for (
                          const node
                          of mutation.addedNodes
                        ) {
                          if (
                            !node ||
                            node.nodeType !== 1
                          ) {
                            continue;
                          }

                          if (
                            node.matches &&
                            node.matches(
                              'a[target], area[target], form[target], base[target]'
                            )
                          ) {
                            repairTarget(
                              node
                            );
                          }

                          if (
                            node.querySelectorAll
                          ) {
                            node
                              .querySelectorAll(
                                'a[target], area[target], form[target], base[target]'
                              )
                              .forEach(
                                repairTarget
                              );
                          }

                          if (
                            node.outerHTML
                          ) {
                            discoverFrameReferences(
                              node.outerHTML
                            );
                          }
                        }
                      }
                    }
                  );

                observer.observe(
                  document.documentElement,
                  {
                    childList: true,
                    subtree: true,
                  }
                );

                window.__renaultStandaloneObserver =
                  observer;
              }

              if (
                !window.__renaultStandaloneOpenPatched
              ) {
                const originalOpen =
                  window.open;

                window.open =
                  function(
                    url,
                    target,
                    features
                  ) {
                    if (
                      target &&
                      !standardTarget(target) &&
                      !frameExists(target)
                    ) {
                      if (url) {
                        navigateCandidate(
                          String(url)
                        );
                      }
                      return window;
                    }

                    return originalOpen
                      .call(
                        window,
                        url,
                        target,
                        features
                      );
                  };

                window.__renaultStandaloneOpenPatched =
                  true;
              }

              if (
                !window.__renaultSelectFallback
              ) {
                document.addEventListener(
                  'change',
                  event => {
                    const select =
                      event.target &&
                      event.target.closest
                        ? event.target.closest(
                            'select'
                          )
                        : null;

                    if (!select) {
                      return;
                    }

                    const before =
                      window.location.href;

                    const option =
                      select.options &&
                      select.selectedIndex >= 0
                        ? select.options[
                            select.selectedIndex
                          ]
                        : null;

                    const candidates = [
                      option
                        ? option.value
                        : null,
                      select.value,
                      extractCandidate(
                        select.getAttribute(
                          'onchange'
                        )
                      ),
                      option
                        ? extractCandidate(
                            option.getAttribute(
                              'onclick'
                            )
                          )
                        : null
                    ];

                    window.setTimeout(
                      () => {
                        if (
                          window.location.href !==
                          before
                        ) {
                          return;
                        }

                        for (
                          const candidate
                          of candidates
                        ) {
                          if (
                            navigateCandidate(
                              candidate
                            )
                          ) {
                            break;
                          }
                        }
                      },
                      120
                    );
                  },
                  true
                );

                window.__renaultSelectFallback =
                  true;
              }

              if (
                !window.__renaultClickFallback
              ) {
                document.addEventListener(
                  'click',
                  event => {
                    const element =
                      event.target &&
                      event.target.closest
                        ? event.target.closest(
                            'a, area, button, input, img'
                          )
                        : null;

                    if (!element) {
                      return;
                    }

                    const anchor =
                      element.closest
                        ? element.closest(
                            'a, area'
                          )
                        : null;

                    const before =
                      window.location.href;

                    const sources = [
                      anchor
                        ? anchor.getAttribute(
                            'href'
                          )
                        : null,
                      anchor
                        ? anchor.getAttribute(
                            'onclick'
                          )
                        : null,
                      element.getAttribute
                        ? element.getAttribute(
                            'onclick'
                          )
                        : null,
                      element.parentElement &&
                      element.parentElement
                        .getAttribute
                        ? element.parentElement
                            .getAttribute(
                              'onclick'
                            )
                        : null
                    ];

                    const candidates = [];

                    for (
                      const source
                      of sources
                    ) {
                      if (
                        isLocalCandidate(
                          source
                        )
                      ) {
                        candidates.push(
                          source
                        );
                      }

                      const extracted =
                        extractCandidate(
                          source
                        );

                      if (extracted) {
                        candidates.push(
                          extracted
                        );
                      }
                    }

                    if (
                      candidates.length === 0
                    ) {
                      return;
                    }

                    window.setTimeout(
                      () => {
                        if (
                          window.location.href !==
                          before
                        ) {
                          return;
                        }

                        for (
                          const candidate
                          of candidates
                        ) {
                          if (
                            navigateCandidate(
                              candidate
                            )
                          ) {
                            break;
                          }
                        }
                      },
                      120
                    );
                  },
                  true
                );

                window.__renaultClickFallback =
                  true;
              }

              return true;
            })();
            """.trimIndent()

        view.evaluateJavascript(
            script,
            null,
        )
    }

    private fun applyReadmeMobileLayout(
        view: WebView?,
    ) {
        view
            ?: return

        val script =
            """
            (() => {
              const body =
                document.body;

              if (!body) {
                return false;
              }

              if (
                !document.getElementById(
                  '__renaultReadmeMobileStyle'
                )
              ) {
                const style =
                  document.createElement(
                    'style'
                  );

                style.id =
                  '__renaultReadmeMobileStyle';

                style.textContent = `
                  :root {
                    color-scheme: dark;
                  }
                  html, body {
                    background: #101318 !important;
                    color: #f3f6f8 !important;
                  }
                  body {
                    max-width: 860px !important;
                    margin: 0 auto !important;
                    padding: 14px 14px 28px !important;
                    box-sizing: border-box;
                    font-family: system-ui, -apple-system, sans-serif !important;
                    font-size: 15px !important;
                    line-height: 1.45 !important;
                  }
                  h1 {
                    margin: 0 0 8px !important;
                    font-size: 22px !important;
                    line-height: 1.18 !important;
                    letter-spacing: -0.2px;
                  }
                  .dataset-years {
                    display: block;
                    margin-top: 3px;
                    color: #aab5c2;
                    font-size: 15px;
                    font-weight: 600;
                    letter-spacing: 0;
                  }
                  h2 {
                    margin: 22px 0 10px !important;
                    font-size: 16px !important;
                    line-height: 1.25 !important;
                  }
                  p {
                    margin: 8px 0 12px !important;
                  }
                  a {
                    color: #76bdff !important;
                  }
                  .box {
                    margin: 12px 0 16px !important;
                    padding: 10px 12px !important;
                    border: 1px solid #384352 !important;
                    border-radius: 12px !important;
                    background: #181d25 !important;
                    font-size: 14px !important;
                  }
                  .readme-action {
                    margin: 0 0 18px !important;
                  }
                  .readme-action a {
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
                  }
                  .readme-action a::after {
                    content: '›';
                    font-size: 22px;
                    line-height: 1;
                  }
                  .file-table {
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
                  }
                  .file-table th,
                  .file-table td {
                    box-sizing: border-box;
                    padding: 9px 8px;
                    vertical-align: top;
                    text-align: left;
                    overflow-wrap: anywhere;
                    word-break: break-word;
                  }
                  .file-table th {
                    color: #aab5c2;
                    background: #222936;
                    font-size: 12px;
                    font-weight: 700;
                  }
                  .file-table th:first-child,
                  .file-table td:first-child {
                    width: 43%;
                    border-right: 1px solid #384352;
                  }
                  .file-table tr + tr td {
                    border-top: 1px solid #384352;
                  }
                  code {
                    padding: 1px 4px !important;
                    border-radius: 5px !important;
                    background: #202732 !important;
                    white-space: normal !important;
                    overflow-wrap: anywhere;
                    font-size: 12px !important;
                  }
                `;

                (
                  document.head ||
                  document.documentElement
                ).appendChild(
                  style
                );
              }

              const clean =
                value =>
                  (value || '')
                    .replace(
                      /\\s+/g,
                      ' '
                    )
                    .trim();

              const heading =
                label =>
                  Array.from(
                    document.querySelectorAll(
                      'h2'
                    )
                  ).find(
                    item =>
                      clean(
                        item.textContent
                      ) === label
                  );

              const title =
                document.querySelector(
                  'h1'
                );

              if (
                title &&
                title.dataset
                  .renaultSplitTitle !==
                  '1'
              ) {
                const raw =
                  clean(
                    title.textContent
                  );
                const match =
                  raw.match(
                    /^(.*?)(?:\\s*[·,|]\\s*|\\s+)(\\d{4})\\s*[-–—]\\s*(\\d{4})$/
                  );

                if (
                  match &&
                  clean(match[1])
                ) {
                  const name =
                    document.createElement(
                      'span'
                    );
                  name.textContent =
                    clean(match[1]);

                  const years =
                    document.createElement(
                      'span'
                    );
                  years.className =
                    'dataset-years';
                  years.textContent =
                    match[2] +
                    '–' +
                    match[3];

                  title.textContent = '';
                  title.appendChild(name);
                  title.appendChild(years);
                }

                title.dataset
                  .renaultSplitTitle =
                  '1';
              }

              const firstParagraph =
                body.querySelector(
                  'p'
                );

              if (
                firstParagraph &&
                clean(
                  firstParagraph.textContent
                ).startsWith(
                  'Це конвертований Renault dataset'
                )
              ) {
                firstParagraph.textContent =
                  'Конвертований Renault dataset. Внутрішні файли не потрібно редагувати вручну.';
              }

              const infoBox =
                body.querySelector(
                  '.box'
                );

              if (
                infoBox &&
                clean(
                  infoBox.textContent
                ).includes(
                  'Знайдено внутрішніх томів'
                )
              ) {
                const count =
                  clean(
                    infoBox.textContent
                  ).match(
                    /(\\d+)/
                  );

                if (count) {
                  infoBox.textContent =
                    'Томів у цьому dataset: ' +
                    count[1];
                }
              }

              const startHeading =
                heading(
                  'З чого почати'
                );

              if (startHeading) {
                startHeading.textContent =
                  'Документація';

                const action =
                  startHeading
                    .nextElementSibling;

                if (action) {
                  action.classList.add(
                    'readme-action'
                  );

                  const link =
                    action.querySelector(
                      'a'
                    );

                  if (link) {
                    link.textContent =
                      'Відкрити каталог';
                  }
                }
              }

              const filesHeading =
                heading(
                  'Що знаходиться у папці'
                ) ||
                heading(
                  'Основні файли'
                );

              if (filesHeading) {
                filesHeading.textContent =
                  'Основні файли';

                const source =
                  filesHeading
                    .nextElementSibling;

                if (
                  source &&
                  source.tagName === 'P' &&
                  !document.getElementById(
                    '__renaultReadmeTable'
                  )
                ) {
                  const rows =
                    source.innerHTML
                      .split(
                        /<br\\s*\\/?>/i
                      )
                      .map(
                        item =>
                          item.trim()
                      )
                      .filter(Boolean);

                  const table =
                    document.createElement(
                      'table'
                    );
                  table.id =
                    '__renaultReadmeTable';
                  table.className =
                    'file-table';

                  const thead =
                    document.createElement(
                      'thead'
                    );
                  const headerRow =
                    document.createElement(
                      'tr'
                    );

                  for (
                    const label of [
                      'Файл',
                      'Призначення'
                    ]
                  ) {
                    const cell =
                      document.createElement(
                        'th'
                      );
                    cell.textContent =
                      label;
                    headerRow.appendChild(
                      cell
                    );
                  }

                  thead.appendChild(
                    headerRow
                  );
                  table.appendChild(
                    thead
                  );

                  const tbody =
                    document.createElement(
                      'tbody'
                    );

                  for (
                    const rawRow of rows
                  ) {
                    const temp =
                      document.createElement(
                        'div'
                      );
                    temp.innerHTML =
                      rawRow;

                    const codes =
                      Array.from(
                        temp.querySelectorAll(
                          'code'
                        )
                      ).map(
                        item =>
                          clean(
                            item.textContent
                          )
                      );

                    const textClone =
                      temp.cloneNode(
                        true
                      );

                    textClone
                      .querySelectorAll(
                        'code'
                      )
                      .forEach(
                        item =>
                          item.remove()
                      );

                    const description =
                      clean(
                        textClone.textContent
                      ).replace(
                        /^[-—–]\\s*/,
                        ''
                      );

                    const row =
                      document.createElement(
                        'tr'
                      );
                    const fileCell =
                      document.createElement(
                        'td'
                      );
                    const descriptionCell =
                      document.createElement(
                        'td'
                      );

                    if (
                      codes.length > 0
                    ) {
                      codes.forEach(
                        (value, index) => {
                          if (index > 0) {
                            fileCell.appendChild(
                              document.createTextNode(
                                ' + '
                              )
                            );
                          }

                          const code =
                            document.createElement(
                              'code'
                            );
                          code.textContent =
                            value;
                          fileCell.appendChild(
                            code
                          );
                        }
                      );
                    } else {
                      fileCell.textContent =
                        'Інше';
                    }

                    descriptionCell.textContent =
                      description ||
                      clean(
                        temp.textContent
                      );

                    row.appendChild(
                      fileCell
                    );
                    row.appendChild(
                      descriptionCell
                    );
                    tbody.appendChild(
                      row
                    );
                  }

                  table.appendChild(
                    tbody
                  );
                  source.replaceWith(
                    table
                  );
                }
              }

              const androidHeading =
                heading(
                  'Android'
                );

              if (androidHeading) {
                androidHeading.textContent =
                  'Renault Docs';

                const paragraph =
                  androidHeading
                    .nextElementSibling;

                if (
                  paragraph &&
                  paragraph.tagName === 'P'
                ) {
                  paragraph.textContent =
                    'Застосунок читає manifest, додає dataset до бібліотеки та відкриває PDF у власному viewer.';
                }
              }

              return true;
            })();
            """.trimIndent()

        view.evaluateJavascript(
            script,
            null,
        )
    }

    private fun sortClassicCatalogByDate(
        view: WebView?,
    ) {
        view
            ?: return

        val script =
            """
            (() => {
              const grid =
                document.querySelector('.grid');

              if (!grid) {
                return false;
              }

              const cards =
                Array.from(
                  grid.querySelectorAll('.card')
                );

              if (cards.length < 2) {
                return false;
              }

              const keyFor = card => {
                const text =
                  card.textContent || '';

                const match =
                  text.match(
                    /(20\d{2})[-._](\d{2})[-._](\d{2})/
                  );

                if (!match) {
                  return '9999-99-99';
                }

                return (
                  match[1] + '-' +
                  match[2] + '-' +
                  match[3]
                );
              };

              cards.sort((left, right) => {
                const dateCompare =
                  keyFor(left)
                    .localeCompare(
                      keyFor(right)
                    );

                if (dateCompare !== 0) {
                  return dateCompare;
                }

                return (
                  left.textContent || ''
                ).localeCompare(
                  right.textContent || ''
                );
              });

              for (const card of cards) {
                grid.appendChild(card);
              }

              return true;
            })();
            """.trimIndent()

        view.evaluateJavascript(
            script,
            null,
        )
    }

    fun switchHybridSection(
        view: WebView,
        sectionCode: String,
        onResult: (Boolean) -> Unit,
    ) {
        val safeCode =
            sectionCode
                .filter {
                    it.isDigit()
                }
                .take(3)

        if (
            safeCode.length != 3
        ) {
            onResult(false)
            return
        }

        activeHybridSectionCode =
            safeCode

        val script =
            """
            (() => {
              const targetCode =
                "$safeCode";
              const stateKey =
                '__renaultLiveSectionSwitch';

              const previous =
                window[stateKey];

              if (
                previous &&
                previous.timer
              ) {
                window.clearInterval(
                  previous.timer
                );
              }

              const state = {
                targetCode,
                done: false,
                success: false,
                attempts: 0,
                triggerAttempts: 0,
                startedAt:
                  performance.now(),
                durationMs: null,
                phase: 'waiting',
                timer: null
              };

              window[stateKey] =
                state;

              const cleanText =
                value =>
                  (value || '')
                    .replace(
                      /\s+/g,
                      ' '
                    )
                    .trim();

              const codeFrom =
                value => {
                  const match =
                    cleanText(value)
                      .match(
                        /^(\d{3})(?!\d)/
                      );

                  return match
                    ? match[1]
                    : null;
                };

              const frameByName =
                name => {
                  try {
                    const named =
                      window.frames[name];

                    if (named) {
                      return named;
                    }
                  } catch (_) {
                    // Fall through.
                  }

                  try {
                    const element =
                      Array.from(
                        document.querySelectorAll(
                          'frame[name], iframe[name]'
                        )
                      )
                        .find(
                          item =>
                            (
                              item.getAttribute(
                                'name'
                              ) || ''
                            ) === name
                        );

                    return element
                      ? element.contentWindow
                      : null;
                  } catch (_) {
                    return null;
                  }
                };

              const hrefOf =
                current => {
                  try {
                    return current
                      ? current.location.href ||
                        ''
                      : '';
                  } catch (_) {
                    return '';
                  }
                };

              const titleOf =
                current => {
                  try {
                    return current &&
                      current.document
                      ? current.document.title ||
                        ''
                      : '';
                  } catch (_) {
                    return '';
                  }
                };

              const textOf =
                current => {
                  try {
                    const body =
                      current &&
                      current.document
                        ? current.document.body
                        : null;

                    return cleanText(
                      body
                        ? body.innerText ||
                          body.textContent ||
                          ''
                        : ''
                    );
                  } catch (_) {
                    return '';
                  }
                };

              const targetSelected =
                () => {
                  const menuWindow =
                    frameByName('menu');
                  const navWindow =
                    frameByName('nav');
                  const exactMenu =
                    '/menu/' +
                    targetCode.toLowerCase() +
                    '.htm';
                  const exactPc =
                    '/pc/' +
                    targetCode.toLowerCase() +
                    '.htm';
                  const titleMarker =
                    'cmp ' +
                    targetCode.toLowerCase();

                  return [
                    hrefOf(menuWindow),
                    hrefOf(navWindow),
                    titleOf(menuWindow),
                    titleOf(navWindow),
                    textOf(menuWindow)
                      .slice(0, 120),
                    textOf(navWindow)
                      .slice(0, 120)
                  ]
                    .map(
                      value =>
                        (value || '')
                          .toLowerCase()
                    )
                    .some(
                      value =>
                        value.includes(
                          exactMenu
                        ) ||
                        value.includes(
                          exactPc
                        ) ||
                        value.includes(
                          titleMarker
                        )
                    );
                };

              const findTargetLeaf =
                documentRef => {
                  const root =
                    documentRef.body ||
                    documentRef.documentElement;

                  if (!root) {
                    return null;
                  }

                  try {
                    const walker =
                      documentRef.createTreeWalker(
                        root,
                        NodeFilter.SHOW_TEXT
                      );

                    let node =
                      walker.nextNode();

                    while (node) {
                      const parent =
                        node.parentElement;
                      const tag =
                        parent
                          ? parent.tagName
                          : '';

                      if (
                        tag !== 'SCRIPT' &&
                        tag !== 'STYLE' &&
                        codeFrom(
                          node.nodeValue
                        ) ===
                          targetCode
                      ) {
                        return parent;
                      }

                      node =
                        walker.nextNode();
                    }
                  } catch (_) {
                    // Fall through.
                  }

                  return Array.from(
                    documentRef
                      .querySelectorAll(
                        'a, area, tr, td, li, p, div, span, option, button, input, [onclick], [onmousedown], [onmouseup]'
                      )
                  )
                    .filter(
                      element =>
                        codeFrom(
                          element.innerText ||
                          element.textContent ||
                          element.value ||
                          element.alt ||
                          element.title ||
                          ''
                        ) ===
                          targetCode
                    )
                    .sort(
                      (left, right) =>
                        cleanText(
                          left.textContent ||
                          left.value ||
                          ''
                        ).length -
                        cleanText(
                          right.textContent ||
                          right.value ||
                          ''
                        ).length
                    )[0] ||
                    null;
                };

              const triggerSection =
                () => {
                  const org =
                    frameByName('org');

                  if (!org) {
                    return false;
                  }

                  let documentRef;

                  try {
                    documentRef =
                      org.document;
                  } catch (_) {
                    return false;
                  }

                  const matched =
                    findTargetLeaf(
                      documentRef
                    );

                  if (!matched) {
                    return false;
                  }

                  const actionable =
                    matched.closest
                      ? matched.closest(
                          'a[href], area[href], button, input, [onclick], [onmousedown], [onmouseup]'
                        )
                      : null;
                  const target =
                    actionable ||
                    matched;

                  try {
                    const mouse =
                      type =>
                        target.dispatchEvent(
                          new MouseEvent(
                            type,
                            {
                              bubbles: true,
                              cancelable: true,
                              view: org
                            }
                          )
                        );

                    mouse('mousedown');
                    mouse('mouseup');

                    if (
                      typeof target.click ===
                      'function'
                    ) {
                      target.click();
                    } else {
                      mouse('click');
                    }

                    return true;
                  } catch (_) {
                    return false;
                  }
                };

              const finish =
                success => {
                  state.done = true;
                  state.success =
                    success;
                  state.phase =
                    success
                      ? 'selected'
                      : 'timeout';
                  state.durationMs =
                    Math.round(
                      performance.now() -
                      state.startedAt
                    );

                  if (state.timer) {
                    window.clearInterval(
                      state.timer
                    );
                    state.timer = null;
                  }
                };

              const tick =
                () => {
                  if (state.done) {
                    return;
                  }

                  state.attempts += 1;

                  if (
                    targetSelected()
                  ) {
                    finish(true);
                    return;
                  }

                  if (
                    state.triggerAttempts === 0 ||
                    state.attempts % 8 === 0
                  ) {
                    if (
                      triggerSection()
                    ) {
                      state.triggerAttempts +=
                        1;
                      state.phase =
                        'triggered';
                    }
                  }

                  if (
                    state.attempts >= 50
                  ) {
                    finish(false);
                  }
                };

              state.timer =
                window.setInterval(
                  tick,
                  100
                );

              tick();

              return true;
            })();
            """.trimIndent()

        view.evaluateJavascript(
            script,
            null,
        )

        pollHybridSectionSwitch(
            view = view,
            targetCode = safeCode,
            attempt = 0,
            onResult = onResult,
        )
    }

    private fun pollHybridSectionSwitch(
        view: WebView,
        targetCode: String,
        attempt: Int,
        onResult: (Boolean) -> Unit,
    ) {
        val escapedCode =
            JSONObject.quote(
                targetCode,
            )

        view.evaluateJavascript(
            """
            (() => {
              const state =
                window.__renaultLiveSectionSwitch;

              if (
                !state ||
                state.targetCode !==
                  $escapedCode
              ) {
                return 'waiting';
              }

              if (!state.done) {
                return 'waiting';
              }

              return state.success
                ? 'success'
                : 'failure';
            })();
            """.trimIndent(),
        ) { raw ->
            val state =
                raw
                    ?.trim('"')
                    .orEmpty()

            when (state) {
                "success" -> {
                    onResult(true)
                    return@evaluateJavascript
                }

                "failure" -> {
                    onResult(false)
                    return@evaluateJavascript
                }
            }

            if (
                attempt >= 60
            ) {
                onResult(false)
                return@evaluateJavascript
            }

            view.postDelayed(
                {
                    pollHybridSectionSwitch(
                        view = view,
                        targetCode =
                            targetCode,
                        attempt =
                            attempt + 1,
                        onResult =
                            onResult,
                    )
                },
                100L,
            )
        }
    }

    fun collectFrameDebugReport(
        view: WebView,
        onResult: (String) -> Unit,
    ) {
        val nowMs =
            SystemClock.elapsedRealtime()
        val fastPrepareMs =
            if (
                fastPackPrepareStartedAtMs != null &&
                fastPackPrepareFinishedAtMs != null
            ) {
                fastPackPrepareFinishedAtMs!! -
                    fastPackPrepareStartedAtMs!!
            } else {
                null
            }
        val clientAgeMs =
            nowMs -
                clientStartedAtMs
        val copiedFastPack =
            fastPackCopiedToLocalCache

        val script =
            """
            (() => {
              const result = {
                generatedAt:
                  new Date().toISOString(),
                topUrl:
                  window.location.href,
                topTitle:
                  document.title || '',
                targetSection:
                  ${JSONObject.quote(activeHybridSectionCode)},
                runtimeTiming: {
                  clientAgeMs:
                    $clientAgeMs,
                  fastPackPrepareMs:
                    ${fastPrepareMs?.toString() ?: "null"},
                  fastPackCopiedToLocalCache:
                    ${copiedFastPack?.toString() ?: "null"}
                },
                navigationTiming:
                  (() => {
                    const nav =
                      performance
                        .getEntriesByType(
                          'navigation'
                        )[0];

                    return nav
                      ? {
                          domContentLoadedMs:
                            Math.round(
                              nav.domContentLoadedEventEnd
                            ),
                          loadEventMs:
                            Math.round(
                              nav.loadEventEnd
                            ),
                          responseEndMs:
                            Math.round(
                              nav.responseEnd
                            )
                        }
                      : null;
                  })(),
                liveSectionSwitch:
                  (() => {
                    const state =
                      window.__renaultLiveSectionSwitch;

                    return state
                      ? {
                          targetCode:
                            state.targetCode ||
                            '',
                          done:
                            !!state.done,
                          success:
                            !!state.success,
                          attempts:
                            state.attempts || 0,
                          triggerAttempts:
                            state.triggerAttempts || 0,
                          phase:
                            state.phase || '',
                          durationMs:
                            state.durationMs
                        }
                      : null;
                  })(),
                rootEntrypoint:
                  ${JSONObject.quote(legacySectionRootEntrypoint)},
                fallbackEntrypoint:
                  ${JSONObject.quote(legacySectionFallbackEntrypoint)},
                hybridStates: [],
                frames: [],
                errors: []
              };

              const cleanText =
                value =>
                  (value || '')
                    .replace(
                      /\\s+/g,
                      ' '
                    )
                    .trim();

              const sectionCodes =
                documentRef => {
                  const codes =
                    new Set();

                  const addCode =
                    value => {
                      const match =
                        cleanText(value)
                          .match(
                            /^(\d{3})(?!\d)/
                          );

                      if (match) {
                        codes.add(
                          match[1]
                        );
                      }
                    };

                  documentRef
                    .querySelectorAll(
                      'a, area, tr, td, li, p, div, span, option, button, input, [onclick]'
                    )
                    .forEach(
                      element => {
                        addCode(
                          element.innerText ||
                          element.textContent ||
                          element.value ||
                          element.alt ||
                          element.title ||
                          ''
                        );
                      }
                    );

                  try {
                    const root =
                      documentRef.body ||
                      documentRef.documentElement;
                    const walker =
                      documentRef.createTreeWalker(
                        root,
                        NodeFilter.SHOW_TEXT
                      );

                    let node =
                      walker.nextNode();

                    while (node) {
                      const parent =
                        node.parentElement;
                      const tag =
                        parent
                          ? parent.tagName
                          : '';

                      if (
                        tag !== 'SCRIPT' &&
                        tag !== 'STYLE'
                      ) {
                        addCode(
                          node.nodeValue
                        );
                      }

                      node =
                        walker.nextNode();
                    }
                  } catch (_) {
                    // Element scan above is enough
                    // when TreeWalker is unavailable.
                  }

                  return Array.from(codes)
                    .sort();
                };

              const relativeUrl =
                value => {
                  try {
                    const parsed =
                      new URL(value);

                    return (
                      parsed.pathname +
                      parsed.search +
                      parsed.hash
                    );
                  } catch (_) {
                    return value || '';
                  }
                };

              const describeFrameElement =
                current => {
                  let element = null;

                  try {
                    element =
                      current.frameElement;
                  } catch (_) {
                    return null;
                  }

                  if (!element) {
                    return null;
                  }

                  let rect = null;

                  try {
                    const raw =
                      element
                        .getBoundingClientRect();

                    rect = {
                      x: Math.round(raw.x),
                      y: Math.round(raw.y),
                      width:
                        Math.round(raw.width),
                      height:
                        Math.round(raw.height)
                    };
                  } catch (_) {
                    // Ignore geometry failures.
                  }

                  let computed = null;

                  try {
                    const style =
                      current.parent
                        .getComputedStyle(
                          element
                        );

                    computed = {
                      display:
                        style.display || '',
                      visibility:
                        style.visibility || '',
                      opacity:
                        style.opacity || ''
                    };
                  } catch (_) {
                    // Ignore style failures.
                  }

                  const parent =
                    element.parentElement;

                  return {
                    tag:
                      element.tagName || '',
                    name:
                      element.getAttribute(
                        'name'
                      ) || '',
                    id:
                      element.id || '',
                    src:
                      element.getAttribute(
                        'src'
                      ) || '',
                    widthAttr:
                      element.getAttribute(
                        'width'
                      ) || '',
                    heightAttr:
                      element.getAttribute(
                        'height'
                      ) || '',
                    scrolling:
                      element.getAttribute(
                        'scrolling'
                      ) || '',
                    rect,
                    computed,
                    parentFrameset:
                      parent &&
                      parent.tagName ===
                        'FRAMESET'
                        ? {
                            rows:
                              parent.getAttribute(
                                'rows'
                              ) || '',
                            cols:
                              parent.getAttribute(
                                'cols'
                              ) || ''
                          }
                        : null
                  };
                };

              const describeWindow =
                (
                  current,
                  depth,
                  indexInParent,
                  path
                ) => {
                  let documentRef;

                  try {
                    documentRef =
                      current.document;
                  } catch (error) {
                    result.errors.push({
                      path,
                      error:
                        String(error)
                    });
                    return;
                  }

                  let href = '';

                  try {
                    href =
                      current.location.href;
                  } catch (_) {
                    href = '';
                  }

                  const codes =
                    sectionCodes(
                      documentRef
                    );

                  const body =
                    documentRef.body;

                  let background = '';

                  try {
                    background =
                      body
                        ? current
                            .getComputedStyle(
                              body
                            )
                            .backgroundColor
                        : '';
                  } catch (_) {
                    background = '';
                  }

                  const textFingerprint =
                    cleanText(
                      body
                        ? body.innerText ||
                          body.textContent ||
                          ''
                        : (
                            documentRef
                              .documentElement
                              ?.textContent ||
                            ''
                          )
                    )
                      .slice(
                        0,
                        220
                      );

                  let childCount = 0;

                  try {
                    childCount =
                      current.frames.length;
                  } catch (_) {
                    childCount = 0;
                  }

                  const selects =
                    documentRef
                      .querySelectorAll(
                        'select'
                      );

                  const selectedValues =
                    Array.from(selects)
                      .slice(0, 8)
                      .map(
                        select => ({
                          name:
                            select.name || '',
                          id:
                            select.id || '',
                          value:
                            select.value || '',
                          options:
                            select.options
                              ? select.options
                                  .length
                              : 0
                        })
                      );

                  const pdfLinks =
                    documentRef
                      .querySelectorAll(
                        'a[href$=".pdf" i], area[href$=".pdf" i]'
                      ).length;

                  const imageCount =
                    documentRef
                      .images
                      ? documentRef
                          .images.length
                      : 0;

                  const buttonCount =
                    documentRef
                      .querySelectorAll(
                        'button, input[type="button"], input[type="submit"], [onclick]'
                      ).length;

                  const record = {
                    path,
                    depth,
                    indexInParent,
                    windowName:
                      current.name || '',
                    title:
                      documentRef.title || '',
                    url: href,
                    relativeUrl:
                      relativeUrl(href),
                    frameElement:
                      describeFrameElement(
                        current
                      ),
                    childCount,
                    sectionCodeCount:
                      codes.length,
                    sectionCodes:
                      codes.slice(
                        0,
                        80
                      ),
                    selectCount:
                      selects.length,
                    selectedValues,
                    pdfLinkCount:
                      pdfLinks,
                    imageCount,
                    buttonCount,
                    bodyBackground:
                      background,
                    textFingerprint
                  };

                  record.roleHints = {
                    menuCandidate:
                      codes.length >= 4,
                    comboCandidate:
                      selects.length > 0,
                    pdfCandidate:
                      pdfLinks > 0,
                    contentCandidate:
                      codes.length < 4 &&
                      (
                        selects.length > 0 ||
                        imageCount > 0 ||
                        buttonCount > 0 ||
                        textFingerprint.length >
                          40
                      )
                  };

                  result.frames.push(
                    record
                  );

                  for (
                    let index = 0;
                    index < childCount;
                    index += 1
                  ) {
                    try {
                      describeWindow(
                        current.frames[
                          index
                        ],
                        depth + 1,
                        index,
                        path + '/' + index
                      );
                    } catch (error) {
                      result.errors.push({
                        path:
                          path +
                          '/' +
                          index,
                        error:
                          String(error)
                      });
                    }
                  }
                };

              for (
                const key
                of Object.keys(window)
              ) {
                if (
                  !key.startsWith(
                    '__renaultHybridSection_'
                  )
                ) {
                  continue;
                }

                const state =
                  window[key];

                result.hybridStates.push({
                  key,
                  done:
                    !!(
                      state &&
                      state.done
                    ),
                  attempts:
                    state &&
                    Number.isFinite(
                      state.attempts
                    )
                      ? state.attempts
                      : null,
                  triggerAttempts:
                    state &&
                    Number.isFinite(
                      state.triggerAttempts
                    )
                      ? state.triggerAttempts
                      : null,
                  phase:
                    state &&
                    state.phase
                      ? state.phase
                      : '',
                  durationMs:
                    state &&
                    Number.isFinite(
                      state.durationMs
                    )
                      ? state.durationMs
                      : null,
                  projection:
                    state &&
                    state.projection
                      ? state.projection
                      : null,
                  hasMenuWindow:
                    !!(
                      state &&
                      state.menuWindow
                    )
                });
              }

              describeWindow(
                window,
                0,
                -1,
                'root'
              );

              result.summary = {
                frameCount:
                  result.frames.length,
                menuCandidates:
                  result.frames
                    .filter(
                      item =>
                        item.roleHints
                          .menuCandidate
                    )
                    .map(
                      item => ({
                        path:
                          item.path,
                        name:
                          item.windowName,
                        title:
                          item.title,
                        url:
                          item.relativeUrl,
                        sectionCodeCount:
                          item.sectionCodeCount,
                        sectionCodes:
                          item.sectionCodes
                      })
                    ),
                comboCandidates:
                  result.frames
                    .filter(
                      item =>
                        item.roleHints
                          .comboCandidate
                    )
                    .map(
                      item => ({
                        path:
                          item.path,
                        name:
                          item.windowName,
                        title:
                          item.title,
                        url:
                          item.relativeUrl,
                        selectCount:
                          item.selectCount,
                        selectedValues:
                          item.selectedValues
                      })
                    )
              };

              return JSON.stringify(
                result,
                null,
                2
              );
            })();
            """.trimIndent()

        view.evaluateJavascript(
            script,
        ) { raw ->
            val decoded =
                runCatching {
                    JSONTokener(
                        raw ?: "\"\"",
                    ).nextValue()
                }
                    .getOrNull()
                    ?.toString()
                    ?: (
                        raw
                            ?: """{"error":"empty evaluateJavascript result"}"""
                    )

            onResult(decoded)
        }
    }

    fun close() {
        if (
            fastArchive.isInitialized()
        ) {
            fastArchive.value?.close()
        }
    }

    fun copyPdfTo(
        relativePath: String,
        destinationUri: Uri,
    ): Result<Long> = runCatching {
        require(
            relativePath
                .lowercase(
                    Locale.ROOT,
                )
                .endsWith(".pdf")
        ) {
            "Експорт дозволений лише для PDF."
        }

        val input =
            resolver.openInputStream(
                relativePath,
            )
                ?: error(
                    "Не вдалося відкрити PDF у dataset."
                )

        val output =
            context.contentResolver
                .openOutputStream(
                    destinationUri,
                    "w",
                )
                ?: error(
                    "Не вдалося відкрити файл призначення."
                )

        input.use { source ->
            output.use { target ->
                source.copyTo(
                    target,
                    DEFAULT_BUFFER_SIZE,
                )
            }
        }
    }

    private fun blockExternalResource(
        url: String,
    ): WebResourceResponse? {
        val scheme =
            runCatching {
                Uri.parse(url)
                    .scheme
                    ?.lowercase(
                        Locale.ROOT,
                    )
            }.getOrNull()

        if (
            scheme !in
            setOf(
                "http",
                "https",
            )
        ) {
            return null
        }

        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            403,
            "Offline",
            mapOf(
                "Cache-Control" to
                    "no-store",
            ),
            ByteArrayInputStream(
                (
                    "Blocked external resource " +
                        "in offline Renault Docs."
                ).toByteArray(
                    Charsets.UTF_8,
                )
            ),
        )
    }

    private fun htmlResponse(
        statusCode: Int,
        reason: String,
        title: String,
        body: String,
    ): WebResourceResponse {
        val html = """
            <!doctype html>
            <html lang="uk">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width,initial-scale=1">
              <title>${escape(title)}</title>
              <style>
                body {
                  margin: 0;
                  padding: 24px;
                  background: #101318;
                  color: #f3f6f8;
                  font-family: system-ui, sans-serif;
                  line-height: 1.55;
                }
                .box {
                  max-width: 760px;
                  margin: 24px auto;
                  padding: 20px;
                  border: 1px solid #384352;
                  border-radius: 16px;
                  background: #181d25;
                }
                h1 {
                  margin-top: 0;
                  font-size: 24px;
                }
                p {
                  color: #aab5c2;
                  overflow-wrap: anywhere;
                }
              </style>
            </head>
            <body>
              <div class="box">
                <h1>${escape(title)}</h1>
                <p>${escape(body)}</p>
              </div>
            </body>
            </html>
        """.trimIndent()

        return WebResourceResponse(
            "text/html",
            "UTF-8",
            statusCode,
            reason,
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
    }

    private fun mimeType(
        path: String,
    ): String {
        return when (
            path.substringAfterLast(
                '.',
                "",
            ).lowercase(
                Locale.ROOT,
            )
        ) {
            "htm",
            "html" -> "text/html"

            "js" ->
                "text/javascript"

            "css" ->
                "text/css"

            "gif" ->
                "image/gif"

            "ico" ->
                "image/x-icon"

            "png" ->
                "image/png"

            "jpg",
            "jpeg" ->
                "image/jpeg"

            "svg" ->
                "image/svg+xml"

            "json" ->
                "application/json"

            else ->
                "application/octet-stream"
        }
    }

    private fun encoding(
        path: String,
    ): String? {
        val normalized =
            path.replace(
                '\\',
                '/',
            )

        if (
            normalized.startsWith(
                "_renault/",
            )
        ) {
            return "UTF-8"
        }

        return null
    }

    private fun escape(
        value: String,
    ): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    companion object {
        const val PDF_SAVE_SCHEME =
            "renaultsavepdf"

        const val PDF_FULLSCREEN_SCHEME =
            "renaultfullscreen"

        const val PDF_COMPANION_SCHEME =
            "renaultcompanion"
    }
}
