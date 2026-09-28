package com.saney.renaultdocs

import org.json.JSONObject

object VolumeDocumentationWebPage {
    fun html(
        data: RuntimeIrVolumeDocumentationData,
    ): String {
        val documentationJson =
            data.documentation
                .toString()
                .replace(
                    "</",
                    "<\\/",
                )

        val volumeTitle =
            JSONObject.quote(
                data.volumeTitle,
            )

        return """
            <!doctype html>
            <html lang="uk">
            <head>
              <meta charset="utf-8">
              <meta
                name="viewport"
                content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no"
              >
              <title>Документація</title>
              <style>
                :root {
                  color-scheme: dark;
                  --bg: #0e1116;
                  --surface: #171c24;
                  --surface-alt: #202937;
                  --line: #344050;
                  --accent: #6baee8;
                  --text: #f3f6f8;
                  --muted: #aab5c2;
                }
                * {
                  box-sizing: border-box;
                }
                html,
                body {
                  margin: 0;
                  padding: 0;
                  width: 100%;
                  min-height: 100%;
                  background: var(--bg);
                  color: var(--text);
                  font-family: system-ui, sans-serif;
                }
                body {
                  padding: 10px;
                }
                h1 {
                  margin: 0 0 4px;
                  font-size: 19px;
                }
                #subtitle {
                  margin-bottom: 10px;
                  color: var(--muted);
                  font-size: 12px;
                }
                .group-title {
                  margin: 8px 2px 6px;
                  color: var(--muted);
                  font-size: 12px;
                  font-weight: 700;
                }
                .tile {
                  display: block;
                  width: 100%;
                  min-height: 46px;
                  margin: 0 0 7px;
                  padding: 8px 10px;
                  border: 1px solid var(--accent);
                  border-radius: 10px;
                  background: var(--surface-alt);
                  color: var(--text);
                  text-align: left;
                  font-size: 16px;
                  line-height: 1.25;
                }
                .tile:active {
                  background: #2a3748;
                }
                .muted {
                  color: var(--muted);
                }
                .empty {
                  padding: 12px;
                  border: 1px solid var(--line);
                  border-radius: 10px;
                  background: var(--surface);
                  color: var(--muted);
                }
              </style>
            </head>
            <body>
              <h1 id="heading">Документація тому</h1>
              <div id="subtitle"></div>
              <main id="content"></main>

              <script>
                const documentation =
                  $documentationJson;
                const volumeTitle =
                  $volumeTitle;

                const arrays = {
                  actions:
                    documentation.actions || [],
                  panels:
                    documentation.panels || [],
                  controls:
                    documentation.controls || [],
                  documents:
                    documentation.documents || []
                };

                const byId = {};
                Object.entries(arrays)
                  .forEach(([key, values]) => {
                    byId[key] =
                      Object.fromEntries(
                        values
                          .filter(
                            value =>
                              value &&
                              value.id
                          )
                          .map(
                            value => [
                              String(value.id),
                              value
                            ]
                          )
                      );
                  });

                const content =
                  document.getElementById(
                    'content'
                  );
                const heading =
                  document.getElementById(
                    'heading'
                  );
                const subtitle =
                  document.getElementById(
                    'subtitle'
                  );

                subtitle.textContent =
                  volumeTitle || '';

                function normalizeLabel(
                  value
                ) {
                  const raw =
                    String(value || '')
                      .trim();

                  const upper =
                    raw.toUpperCase();

                  if (upper === 'GENE') {
                    return 'Загальна документація';
                  }

                  if (upper === 'PLATFUSI') {
                    return 'Запобіжники';
                  }

                  if (upper === 'AIDE') {
                    return 'Довідка';
                  }

                  return raw || 'Документ';
                }

                function datasetUrl(path) {
                  const normalized =
                    String(path || '')
                      .replace(/\\\\/g, '/')
                      .replace(/^\/+/, '');

                  return (
                    'https://renault.local/' +
                    normalized
                      .split('/')
                      .map(encodeURIComponent)
                      .join('/')
                  );
                }

                function actionById(id) {
                  return byId.actions[
                    String(id || '')
                  ] || null;
                }

                function panelById(id) {
                  return byId.panels[
                    String(id || '')
                  ] || null;
                }

                function controlById(id) {
                  return byId.controls[
                    String(id || '')
                  ] || null;
                }

                function documentById(id) {
                  return byId.documents[
                    String(id || '')
                  ] || null;
                }

                function clear() {
                  content.replaceChildren();
                }

                function addGroupTitle(label) {
                  const div =
                    document.createElement(
                      'div'
                    );
                  div.className =
                    'group-title';
                  div.textContent =
                    normalizeLabel(label);
                  content.appendChild(div);
                }

                function addTile(
                  label,
                  handler
                ) {
                  const button =
                    document.createElement(
                      'button'
                    );
                  button.className =
                    'tile';
                  button.textContent =
                    '› ' +
                    normalizeLabel(label);
                  button.addEventListener(
                    'click',
                    handler
                  );
                  content.appendChild(
                    button
                  );
                }

                function addEmpty(text) {
                  const div =
                    document.createElement(
                      'div'
                    );
                  div.className =
                    'empty';
                  div.textContent = text;
                  content.appendChild(div);
                }

                function navigateToPath(
                  path
                ) {
                  if (!path) {
                    addEmpty(
                      'Шлях документа відсутній.'
                    );
                    return;
                  }

                  window.location.href =
                    datasetUrl(path);
                }

                function openDocument(
                  documentId,
                  fallbackLabel
                ) {
                  const doc =
                    documentById(
                      documentId
                    );

                  if (!doc) {
                    addEmpty(
                      'Документ не знайдено.'
                    );
                    return;
                  }

                  if (doc.path) {
                    navigateToPath(
                      doc.path
                    );
                    return;
                  }

                  const parts =
                    Array.isArray(
                      doc.parts
                    )
                      ? doc.parts
                      : [];

                  if (parts.length === 1) {
                    openDocument(
                      parts[0].document_id,
                      fallbackLabel
                    );
                    return;
                  }

                  if (parts.length > 1) {
                    const state = {
                      kind: 'document',
                      documentId:
                        String(
                          documentId
                        ),
                      label:
                        String(
                          fallbackLabel ||
                          'Документ'
                        )
                    };

                    history.pushState(
                      state,
                      '',
                      '#document=' +
                        encodeURIComponent(
                          state.documentId
                        )
                    );
                    renderDocumentParts(
                      doc,
                      state.label
                    );
                    return;
                  }

                  addEmpty(
                    'У документа немає доступного вмісту.'
                  );
                }

                function handleAction(
                  action
                ) {
                  if (!action) {
                    return;
                  }

                  if (
                    action.type !== 'route'
                  ) {
                    addEmpty(
                      'Цей пункт поки потребує Classic runtime.'
                    );
                    return;
                  }

                  switch (
                    action.route_type
                  ) {
                    case 'open-panel': {
                      const panelId =
                        String(
                          action.panel_id ||
                          ''
                        );

                      if (!panelId) {
                        return;
                      }

                      history.pushState(
                        {
                          kind: 'panel',
                          panelId
                        },
                        '',
                        '#panel=' +
                          encodeURIComponent(
                            panelId
                          )
                      );
                      renderPanel(
                        panelId
                      );
                      return;
                    }

                    case 'open-document': {
                      if (
                        action.document_id
                      ) {
                        openDocument(
                          action.document_id,
                          action.label
                        );
                      } else {
                        navigateToPath(
                          action.target
                        );
                      }
                      return;
                    }

                    default:
                      navigateToPath(
                        action.target
                      );
                  }
                }

                function renderActionTile(
                  actionId,
                  label
                ) {
                  const action =
                    actionById(
                      actionId
                    );

                  if (!action) {
                    return;
                  }

                  addTile(
                    label ||
                      action.label ||
                      'Відкрити',
                    () =>
                      handleAction(
                        action
                      )
                  );
                }

                function renderControl(
                  control
                ) {
                  if (!control) {
                    return;
                  }

                  if (
                    control.type ===
                    'action-bar'
                  ) {
                    (control.items || [])
                      .forEach(item => {
                        renderActionTile(
                          item.action_id,
                          item.label
                        );
                      });
                    return;
                  }

                  if (
                    control.type ===
                    'document-list'
                  ) {
                    (control.action_ids || [])
                      .forEach(actionId => {
                        renderActionTile(
                          actionId,
                          ''
                        );
                      });
                    return;
                  }

                  if (
                    control.type ===
                    'select'
                  ) {
                    (control.options || [])
                      .forEach(option => {
                        if (
                          option.enabled
                        ) {
                          renderActionTile(
                            option.action_id,
                            option.label
                          );
                        } else if (
                          option.label &&
                          option.kind !==
                            'separator'
                        ) {
                          addGroupTitle(
                            option.label
                          );
                        }
                      });
                  }
                }

                function renderRoot(
                  replaceHistory = false
                ) {
                  clear();
                  heading.textContent =
                    'Документація тому';

                  if (replaceHistory) {
                    history.replaceState(
                      {
                        kind: 'root'
                      },
                      '',
                      '#root'
                    );
                  }

                  const items =
                    documentation.menu_items ||
                    [];

                  if (items.length === 0) {
                    addEmpty(
                      'Документація цього тому порожня.'
                    );
                    return;
                  }

                  items.forEach(item => {
                    renderActionTile(
                      item.action_id,
                      item.label
                    );
                  });
                }

                function renderPanel(
                  panelId
                ) {
                  const panel =
                    panelById(
                      panelId
                    );

                  if (!panel) {
                    renderRoot();
                    return;
                  }

                  clear();
                  heading.textContent =
                    normalizeLabel(
                      panel.title ||
                      panel.label ||
                      'Документація'
                    );

                  const referenced =
                    new Set();

                  (panel.control_ids || [])
                    .forEach(controlId => {
                      const control =
                        controlById(
                          controlId
                        );

                      if (!control) {
                        return;
                      }

                      (control.items || [])
                        .forEach(item => {
                          if (
                            item.action_id
                          ) {
                            referenced.add(
                              String(
                                item.action_id
                              )
                            );
                          }
                        });

                      (control.options || [])
                        .forEach(option => {
                          if (
                            option.action_id
                          ) {
                            referenced.add(
                              String(
                                option.action_id
                              )
                            );
                          }
                        });

                      (control.action_ids || [])
                        .forEach(actionId => {
                          referenced.add(
                            String(
                              actionId
                            )
                          );
                        });

                      renderControl(
                        control
                      );
                    });

                  (panel.action_ids || [])
                    .forEach(actionId => {
                      if (
                        referenced.has(
                          String(actionId)
                        )
                      ) {
                        return;
                      }

                      renderActionTile(
                        actionId,
                        ''
                      );
                    });
                }

                function renderDocumentParts(
                  doc,
                  label
                ) {
                  clear();
                  heading.textContent =
                    normalizeLabel(
                      label ||
                      'Документ'
                    );

                  const parts =
                    Array.isArray(
                      doc.parts
                    )
                      ? doc.parts
                      : [];

                  parts.forEach(
                    (part, index) => {
                      const nested =
                        documentById(
                          part.document_id
                        );

                      if (!nested) {
                        return;
                      }

                      addTile(
                        part.role ||
                        nested.title ||
                        ('Документ ' +
                          (index + 1)),
                        () =>
                          openDocument(
                            part.document_id,
                            part.role ||
                            nested.title
                          )
                      );
                    }
                  );
                }

                window.addEventListener(
                  'popstate',
                  event => {
                    const state =
                      event.state || {
                        kind: 'root'
                      };

                    if (
                      state.kind ===
                      'panel'
                    ) {
                      renderPanel(
                        state.panelId
                      );
                    } else if (
                      state.kind ===
                      'document'
                    ) {
                      const doc =
                        documentById(
                          state.documentId
                        );
                      if (doc) {
                        renderDocumentParts(
                          doc,
                          state.label
                        );
                      } else {
                        renderRoot();
                      }
                    } else {
                      renderRoot();
                    }
                  }
                );

                renderRoot(true);
              </script>
            </body>
            </html>
        """.trimIndent()
    }
}
