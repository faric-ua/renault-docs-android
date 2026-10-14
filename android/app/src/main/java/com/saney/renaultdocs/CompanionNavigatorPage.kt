package com.saney.renaultdocs

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/**
 * v0.5.120 (owner): the lower window («Документація поруч») is a small
 * navigator of the whole volume, not only the volume documentation.
 *
 *  • «←» from «Документація тому» leads to the volume (sections + search),
 *    never to an empty «about:blank» page;
 *  • a section opens with its tabs (Схеми / Розʼєм / Положення /
 *    Документація) and any document of it — a scheme, a connector (its
 *    parts), a table — opens in the lower window while the upper window
 *    keeps its own document. Top and bottom can show two different documents.
 *
 * The page is served by [SafDatasetWebViewClient] at [PATH] (a real URL, so
 * the WebView history works and «←» steps back through the chain). The page
 * reads the section shards itself with fetch() from the same dataset host.
 */
object CompanionNavigatorPage {
    const val PREFIX =
        "__renault_companion__/"

    const val PATH =
        PREFIX + "navigator.html"

    fun isNavigatorPath(
        relativePath: String,
    ): Boolean =
        relativePath.equals(
            PATH,
            ignoreCase = true,
        )

    /** Never throws: a problem is shown inside the page. */
    fun read(
        context: Context,
        treeUri: Uri,
        volumeEntrypoint: String,
    ): JSONObject {
        val errors =
            mutableListOf<String>()

        val sections =
            ModernSectionsReader
                .read(
                    context = context,
                    treeUri = treeUri,
                    volumeEntrypoint =
                        volumeEntrypoint,
                )
                .onFailure {
                    errors.add(
                        it.message
                            ?: "Розділи тому не прочитано."
                    )
                }
                .getOrNull()

        val sorted =
            ModernSectionDisplayOrder.sorted(
                sections?.sections.orEmpty(),
            )

        val paths =
            runCatching {
                RuntimeIrReader
                    .findVolumeShardPaths(
                        indexText =
                            RuntimeIrReader
                                .readIndexText(
                                    context = context,
                                    treeUri = treeUri,
                                ),
                        volumeEntrypoint =
                            volumeEntrypoint,
                        sections =
                            sorted,
                    )
            }
                .onFailure {
                    errors.add(
                        it.message
                            ?: "Runtime IR index не прочитано."
                    )
                }
                .getOrNull()

        val list =
            JSONArray()

        for (section in sorted) {
            list.put(
                JSONObject()
                    .put(
                        "code",
                        section.code,
                    )
                    .put(
                        "title",
                        section.title,
                    )
                    .put(
                        "path",
                        paths
                            ?.sections
                            ?.get(section.code)
                            .orEmpty(),
                    )
            )
        }

        return JSONObject()
            .put(
                "volumeTitle",
                sections
                    ?.volumeTitle
                    .orEmpty(),
            )
            .put(
                "sections",
                list,
            )
            .put(
                "documentationPath",
                paths
                    ?.documentationPath
                    .orEmpty(),
            )
            .put(
                "error",
                errors.joinToString(
                    " · ",
                ),
            )
    }

    fun html(
        data: JSONObject,
    ): String {
        val json =
            data
                .toString()
                .replace(
                    "</",
                    "<\\/",
                )

        return TEMPLATE.replace(
            "__NAVIGATOR_DATA__",
            json,
        )
    }

    private val TEMPLATE = """
<!doctype html>
<html lang="uk">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
  <title>Документація тому</title>
  <style>
    :root {
      color-scheme: dark;
      --bg: #0e1116;
      --surface: #171c24;
      --surface-alt: #202937;
      --line: #344050;
      --accent: #6baee8;
      --title: #c8e5ff;
      --text: #f3f6f8;
      --muted: #aab5c2;
      --ok: #62d296;
      --ok-fill: #16271e;
    }
    * { box-sizing: border-box; }
    html, body {
      margin: 0;
      padding: 0;
      width: 100%;
      min-height: 100%;
      background: var(--bg);
      color: var(--text);
      font-family: system-ui, sans-serif;
      -webkit-text-size-adjust: 100%;
    }
    body { padding: 8px 8px 24px; }
    h1 {
      margin: 2px 2px 8px;
      font-size: 17px;
      color: var(--title);
    }
    .sub {
      margin: -4px 2px 8px;
      color: var(--muted);
      font-size: 12px;
    }
    .group {
      margin: 8px 2px 5px;
      color: var(--muted);
      font-size: 12px;
      font-weight: 700;
    }
    .search {
      position: sticky;
      top: 0;
      z-index: 5;
      padding: 0 0 8px;
      background: var(--bg);
    }
    .search label {
      display: flex;
      align-items: center;
      gap: 6px;
      height: 40px;
      padding: 0 12px;
      border: 1px solid var(--accent);
      border-radius: 20px;
      background: #101318;
    }
    .search input {
      flex: 1;
      min-width: 0;
      border: none;
      outline: none;
      background: none;
      color: var(--text);
      font-size: 15px;
      font-family: inherit;
    }
    .tile, .row, .tab {
      display: block;
      width: 100%;
      margin: 0 0 6px;
      border: 1px solid var(--accent);
      border-radius: 10px;
      background: var(--surface-alt);
      color: var(--text);
      text-align: left;
      font-family: inherit;
    }
    .tile {
      min-height: 44px;
      padding: 8px 12px;
      font-size: 15px;
      line-height: 1.3;
    }
    /* v0.5.133: «›» is its own element, centred by height; text wraps beside it. */
    .tile {
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .tile .chev {
      flex: none;
      color: var(--accent);
      font-size: 20px;
      line-height: 1;
    }
    .tile .lbl {
      flex: 1;
      min-width: 0;
    }
    .tile.criteria {
      border-style: dashed;
      background: #1d2530;
    }
    .row {
      display: flex;
      align-items: center;
      gap: 10px;
      min-height: 50px;
      padding: 6px 10px;
      border-color: var(--line);
      background: var(--surface);
    }
    .code {
      min-width: 52px;
      height: 36px;
      padding: 0 4px;
      display: flex;
      align-items: center;
      justify-content: center;
      border: 1px solid var(--accent);
      border-radius: 8px;
      background: var(--surface-alt);
      color: var(--accent);
      font-weight: 700;
      font-size: 15px;
    }
    .row .name {
      flex: 1;
      color: var(--title);
      font-weight: 600;
      font-size: 15px;
    }
    .tabs {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 6px;
      margin: 0 0 8px;
    }
    .tab {
      margin: 0;
      min-height: 40px;
      padding: 4px 8px;
      text-align: center;
      font-size: 14px;
    }
    .tab.wide { grid-column: 1 / -1; }
    .card {
      margin: 0 0 6px;
      padding: 6px 8px 2px;
      border: 1px solid var(--line);
      border-radius: 12px;
      background: var(--surface);
    }
    .card .group { margin-top: 2px; }
    .sel {
      border: 2px solid var(--ok) !important;
      background: var(--ok-fill) !important;
      color: #ffffff !important;
      font-weight: 700;
    }
    .note {
      padding: 12px;
      margin: 0 0 6px;
      border: 1px solid var(--line);
      border-radius: 10px;
      background: var(--surface);
      color: var(--muted);
      font-size: 14px;
    }
    .note.error { color: #ffb4a8; }
    .tablewrap {
      overflow-x: auto;
      margin: 0 0 8px;
      border: 1px solid var(--line);
      border-radius: 8px;
    }
    table {
      border-collapse: collapse;
      width: 100%;
      font-size: 13px;
    }
    td, th {
      padding: 6px 7px;
      border: 1px solid var(--line);
      vertical-align: top;
      text-align: left;
    }
    th { background: var(--surface-alt); font-weight: 700; }
    .heading {
      margin: 0 2px 4px;
      color: var(--muted);
      font-size: 13px;
      text-align: center;
    }
  </style>
</head>
<body>
  <main id="app"></main>
  <script>
    const DATA = __NAVIGATOR_DATA__;
    const app = document.getElementById('app');
    const jsonCache = {};
    let renderToken = 0;

    const DOC_CODES = ['GENE', 'PLATFUSI', 'AIDE'];

    function store(key, value) {
      try { sessionStorage.setItem('renaultNav|' + key, value); } catch (_) {}
    }
    function load(key) {
      try { return sessionStorage.getItem('renaultNav|' + key) || ''; } catch (_) { return ''; }
    }

    function upper(value) {
      return String(value || '').trim().toUpperCase();
    }

    function menuLabel(raw) {
      switch (upper(raw)) {
        case 'SCH': return 'Схеми';
        case 'NM': return 'Розʼєм';
        case 'PC': return 'Положення на авто';
        case 'GENE': return 'Загальна документація';
        case 'PLATFUSI': return 'Запобіжники';
        case 'AIDE': return 'Довідка';
        case 'CRITERE': return 'Критерії / скорочення';
        default: return String(raw || '').trim() || 'Документ';
      }
    }

    function isCriteria(raw) {
      return upper(raw) === 'CRITERE' ||
        upper(raw) === upper('Критерії / скорочення');
    }

    function isPrompt(label) {
      const value = upper(label);
      return value.indexOf('ВЫБЕР') === 0 ||
        value.indexOf('ВИБЕР') === 0 ||
        value.indexOf('SELECT') === 0;
    }

    function datasetUrl(path) {
      const normalized = String(path || '')
        .replace(/\\/g, '/')
        .replace(/^\/+/, '');
      return 'https://renault.local/' +
        normalized.split('/').map(encodeURIComponent).join('/');
    }

    function loadJson(path) {
      if (!jsonCache[path]) {
        jsonCache[path] = fetch(datasetUrl(path))
          .then(response => {
            if (!response.ok) {
              throw new Error('Файл не відкрито: ' + path);
            }
            return response.json();
          });
        jsonCache[path].catch(() => { delete jsonCache[path]; });
      }
      return jsonCache[path];
    }

    // ---- state in the URL hash: the WebView history is the «←» chain ----

    function stateFromHash() {
      const state = {};
      String(location.hash || '').replace(/^#/, '').split('&')
        .forEach(part => {
          const index = part.indexOf('=');
          if (index > 0) {
            state[part.slice(0, index)] =
              decodeURIComponent(part.slice(index + 1));
          }
        });
      if (!state.v) state.v = 'root';
      return state;
    }

    function hashOf(state) {
      return '#' + ['v', 's', 'p', 'd', 'l']
        .filter(key => state[key])
        .map(key => key + '=' + encodeURIComponent(state[key]))
        .join('&');
    }

    function rememberScroll() {
      store('scroll|' + location.hash, String(window.scrollY || 0));
    }

    function go(state, replace) {
      rememberScroll();
      if (replace) {
        history.replaceState(state, '', hashOf(state));
      } else {
        history.pushState(state, '', hashOf(state));
      }
      render(state, false);
    }

    function openPath(path) {
      if (!path) {
        note('Шлях документа відсутній.', true);
        return;
      }
      rememberScroll();
      location.href = datasetUrl(path);
    }

    // ---- small builders ----

    function clear(title) {
      app.replaceChildren();
      document.title = title;
    }

    function el(tag, className, text) {
      const node = document.createElement(tag);
      if (className) node.className = className;
      if (text !== undefined) node.textContent = text;
      return node;
    }

    function heading(text, sub) {
      app.appendChild(el('h1', '', text));
      if (sub) app.appendChild(el('div', 'sub', sub));
    }

    function note(text, isError, parent) {
      (parent || app).appendChild(
        el('div', isError ? 'note error' : 'note', text)
      );
    }

    function tile(parent, label, selected, onClick, extraClass) {
      const button = el(
        'button',
        'tile' + (extraClass ? ' ' + extraClass : '') + (selected ? ' sel' : '')
      );
      // v0.5.133: «›» separate; a line may end after «,» or «/», never mid-code.
      button.appendChild(el('span', 'chev', '›'));
      button.appendChild(el('span', 'lbl', String(label).replace(/[,\/]/g, m => m + '\u200B')));
      button.addEventListener('click', onClick);
      parent.appendChild(button);
      return button;
    }

    function byId(list, id) {
      const wanted = String(id || '');
      return (list || []).find(item => item && String(item.id) === wanted) || null;
    }

    function sectionByCode(code) {
      return (DATA.sections || []).find(item => item.code === code) || null;
    }

    // ---- data sources: a section shard or the volume documentation ----

    function sourceFor(state) {
      if (state.s) {
        const section = sectionByCode(state.s);
        if (!section || !section.path) {
          return Promise.reject(
            new Error('Розділ ' + state.s + ' не знайдено в Runtime IR. Запусти Renault → 9.')
          );
        }
        return loadJson(section.path).then(shard => {
          const root = shard.section || {};
          return { kind: 'section', code: state.s, title: section.title, root: root };
        });
      }
      if (!DATA.documentationPath) {
        return Promise.reject(new Error('Для цього тому документація відсутня.'));
      }
      return loadJson(DATA.documentationPath).then(shard => {
        return { kind: 'docs', root: shard.documentation || {} };
      });
    }

    function selectionKey(state) {
      return 'sel|' + (state.s || 'docs') + '|' + (state.p || state.d || state.v);
    }

    // ---- actions ----

    function handleAction(src, state, action, label) {
      if (!action) return;
      store(selectionKey(state), String(action.id || label));
      if (action.type === 'print') {
        note('Друк: відкрий документ — у ньому є кнопка друку.');
        return;
      }
      if (action.type !== 'route') {
        note('Цей пункт відкривається лише у верхньому вікні.', true);
        return;
      }
      if (action.route_type === 'open-panel' && action.panel_id) {
        go({ v: 'panel', s: state.s, p: String(action.panel_id) });
        return;
      }
      if (action.route_type === 'open-document' && action.document_id) {
        openDocument(src, state, action.document_id, label || action.label);
        return;
      }
      openPath(action.target);
    }

    function openDocument(src, state, documentId, label) {
      const doc = byId(src.root.documents, documentId);
      if (!doc) {
        note('Документ не знайдено.', true);
        return;
      }
      const parts = Array.isArray(doc.parts) ? doc.parts : [];
      if (doc.type === 'structured-html') {
        go({ v: 'table', s: state.s, d: String(doc.id), l: label || '' });
        return;
      }
      if (doc.type === 'composite-document' || (!doc.path && parts.length > 1)) {
        go({ v: 'parts', s: state.s, d: String(doc.id), l: label || '' });
        return;
      }
      if (!doc.path && parts.length === 1) {
        openDocument(src, state, parts[0].document_id, label);
        return;
      }
      openPath(doc.path);
    }

    // ---- screens ----

    function renderRoot() {
      clear('Том · розділи');
      heading('Том', DATA.volumeTitle || '');

      const bar = el('div', 'search');
      const label = el('label');
      label.appendChild(el('span', '', '⌕'));
      const input = el('input');
      input.type = 'search';
      input.placeholder = 'Пошук: код або назва розділу…';
      input.setAttribute('aria-label', 'Пошук розділу');
      input.value = load('query');
      label.appendChild(input);
      bar.appendChild(label);
      app.appendChild(bar);

      if (DATA.error) note(DATA.error, true);

      if (DATA.documentationPath) {
        tile(app, 'Документація тому', load('last') === 'docs', () => {
          store('last', 'docs');
          go({ v: 'docs' });
        });
      }

      app.appendChild(el('div', 'group', 'РОЗДІЛИ ТОМУ'));
      const list = el('div');
      app.appendChild(list);

      function fill() {
        list.replaceChildren();
        const query = input.value.trim().toLowerCase();
        const found = (DATA.sections || []).filter(section =>
          !query ||
          (section.code + ' ' + section.title).toLowerCase().indexOf(query) >= 0
        );
        if (found.length === 0) {
          note('Нічого не знайдено.', false, list);
          return;
        }
        found.forEach(section => {
          const row = el(
            'button',
            'row' + (load('last') === 'section:' + section.code ? ' sel' : '')
          );
          row.appendChild(el('span', 'code', section.code));
          row.appendChild(el('span', 'name', section.title));
          row.addEventListener('click', () => {
            store('last', 'section:' + section.code);
            go({ v: 'panel', s: section.code });
          });
          list.appendChild(row);
        });
      }

      input.addEventListener('input', () => {
        store('query', input.value);
        fill();
      });
      fill();
    }

    function renderDocsRoot(src, state) {
      clear('Документація тому');
      heading('Документація тому', DATA.volumeTitle || '');
      const items = src.root.menu_items || [];
      if (items.length === 0) {
        note('Документація цього тому порожня.');
        return;
      }
      const selected = load(selectionKey(state));
      items.forEach(item => {
        const action = byId(src.root.actions, item.action_id);
        if (!action) return;
        tile(app, menuLabel(item.label || action.label), selected === String(action.id),
          () => handleAction(src, state, action, menuLabel(item.label)));
      });
    }

    function sectionTabs(src, state) {
      const controls = src.root.controls || [];
      const toolbar = byId(controls, 'menu-toolbar');
      const items = (toolbar && toolbar.items) || [];
      const order = { SCH: 0, NM: 1, PC: 2 };
      const tabs = [];
      items.forEach(item => {
        const raw = String(item.label || '');
        if (upper(raw) === 'BLANK' || DOC_CODES.indexOf(upper(raw)) >= 0) return;
        const action = byId(src.root.actions, item.action_id);
        if (!action) return;
        tabs.push({
          label: menuLabel(raw),
          action: action,
          rank: order[upper(raw)] !== undefined ? order[upper(raw)] : 10 + tabs.length
        });
      });
      tabs.sort((a, b) => a.rank - b.rank);
      return tabs;
    }

    function renderSectionPanel(src, state) {
      const tabs = sectionTabs(src, state);

      if (!state.p) {
        const first = tabs.find(tab =>
          tab.action.route_type === 'open-panel' && tab.action.panel_id);
        if (first) {
          state.p = String(first.action.panel_id);
          history.replaceState(state, '', hashOf(state));
        }
      }

      const activeTab = tabs.find(tab =>
        String(tab.action.panel_id || '') === String(state.p || ''));
      clear(state.s + ' · ' + (activeTab ? activeTab.label : 'розділ'));
      heading(state.s + ' — ' + (src.title || ''));

      const grid = el('div', 'tabs');
      tabs.forEach((tab, index) => {
        const button = el('button', 'tab' + (tab === activeTab ? ' sel' : ''), tab.label);
        if (index === 2) button.classList.add('wide');
        button.addEventListener('click', () => {
          if (tab.action.route_type === 'open-panel' && tab.action.panel_id) {
            // tabs replace each other — «←» goes back to the volume, not tab by tab
            go({ v: 'panel', s: state.s, p: String(tab.action.panel_id) }, true);
          } else {
            handleAction(src, state, tab.action, tab.label);
          }
        });
        grid.appendChild(button);
      });
      if (DATA.documentationPath) {
        const docs = el('button', 'tab wide', 'Документація');
        docs.addEventListener('click', () => go({ v: 'docs' }));
        grid.appendChild(docs);
      }
      app.appendChild(grid);

      renderPanelBody(src, state);
    }

    function renderPanelBody(src, state) {
      const panel = byId(src.root.panels, state.p);
      if (!panel) {
        if (state.s) {
          note('Обери вкладку розділу.');
        } else {
          note('Панель не знайдено.', true);
        }
        return;
      }

      if (!state.s) {
        clear(menuLabel(panel.title || panel.label || 'Документація'));
        heading(menuLabel(panel.title || panel.label || 'Документація'));
      }

      const selected = load(selectionKey(state));
      const criteria = el('div');
      const body = el('div');
      app.appendChild(criteria);
      app.appendChild(body);
      const used = new Set();

      function addAction(parent, action, label) {
        if (!action) return;
        const text = menuLabel(label || action.label || 'Відкрити');
        if (isCriteria(label || action.label)) {
          criteria.replaceChildren();
          tile(criteria, 'Критерії / скорочення', selected === String(action.id),
            () => handleAction(src, state, action, 'Критерії / скорочення'), 'criteria');
          return;
        }
        tile(parent, text, selected === String(action.id),
          () => handleAction(src, state, action, text));
      }

      (panel.control_ids || []).forEach(controlId => {
        const control = byId(src.root.controls, controlId);
        if (!control) return;
        (control.items || []).forEach(item => used.add(String(item.action_id)));
        (control.options || []).forEach(option => used.add(String(option.action_id)));
        (control.action_ids || []).forEach(id => used.add(String(id)));

        if (control.type === 'select') {
          let card = null;
          (control.options || []).forEach(option => {
            const label = String(option.label || '').trim();
            if (!option.enabled) {
              if (option.kind === 'separator') { card = null; return; }
              if (!label) return;
              if (isPrompt(label)) {
                card = null;
                body.appendChild(el('div', 'group', label));
                return;
              }
              card = el('div', 'card');
              card.appendChild(el('div', 'group', label));
              body.appendChild(card);
              return;
            }
            const action = byId(src.root.actions, option.action_id);
            if (action && isCriteria(label || action.label)) {
              addAction(body, action, label);
              return;
            }
            if (!card) {
              card = el('div', 'card');
              body.appendChild(card);
            }
            addAction(card, action, label);
          });
        } else if (control.type === 'document-list') {
          (control.action_ids || []).forEach(id =>
            addAction(body, byId(src.root.actions, id), ''));
        } else if (control.type === 'action-bar' && !state.s) {
          (control.items || []).forEach(item =>
            addAction(body, byId(src.root.actions, item.action_id), item.label));
        }
      });

      (panel.action_ids || []).forEach(id => {
        if (used.has(String(id))) return;
        addAction(body, byId(src.root.actions, id), '');
      });

      if (!body.firstChild && !criteria.firstChild) {
        note('У цій вкладці документів немає.');
      }
    }

    function renderParts(src, state) {
      const doc = byId(src.root.documents, state.d);
      const title = state.l || (doc && doc.title) || 'Документ';
      clear((state.s ? state.s + ' · ' : '') + title);
      heading(title, state.s ? state.s + ' — ' + (src.title || '') : '');
      if (!doc) {
        note('Документ не знайдено.', true);
        return;
      }
      const selected = load(selectionKey(state));
      const parts = Array.isArray(doc.parts) ? doc.parts : [];
      const resolved = parts
        .map(part => ({ part: part, doc: byId(src.root.documents, part.document_id) }))
        .filter(item => item.doc);
      const hasDrawing = resolved.some(item => item.doc.type === 'pdf');
      const hasPins = resolved.some(item => item.doc.type === 'structured-html');

      function pick(key, onClick) {
        return () => {
          store(selectionKey(state), key);
          onClick();
        };
      }

      if (doc.type === 'composite-document' && hasDrawing && hasPins && doc.path) {
        tile(app, 'Схема + піни розʼєма', selected === 'both',
          pick('both', () => openPath(doc.path)));
      }
      resolved.forEach((item, index) => {
        const role = upper(item.part.role);
        const key = 'part:' + item.doc.id;
        let label;
        if (item.doc.type === 'pdf') {
          label = role === 'DESSIN' ? 'Схема розʼєму' : 'PDF';
        } else if (item.doc.type === 'structured-html') {
          label = role === 'ALVEOLES' ? 'Опис контактів' : (item.part.role || item.doc.title || 'Опис');
        } else {
          label = item.part.role || item.doc.title || ('Документ ' + (index + 1));
        }
        tile(app, menuLabel(label), selected === key,
          pick(key, () => openDocument(src, state, item.doc.id, label)));
      });
      if (resolved.length === 0) {
        if (doc.path) {
          tile(app, 'Відкрити документ', false, () => openPath(doc.path));
        } else {
          note('У документа немає доступного вмісту.');
        }
      }
    }

    function renderTable(src, state) {
      const doc = byId(src.root.documents, state.d);
      const title = state.l || (doc && doc.title) || 'Таблиця';
      clear((state.s ? state.s + ' · ' : '') + title);
      heading(title, state.s ? state.s + ' — ' + (src.title || '') : '');
      if (!doc) {
        note('Документ не знайдено.', true);
        return;
      }
      const seen = new Set();
      (doc.headings || []).forEach(item => {
        const text = String((item && item.text) || '').trim();
        if (!text || seen.has(text)) return;
        seen.add(text);
        app.appendChild(el('div', 'heading', text));
      });
      const tables = doc.tables || [];
      let shown = 0;
      tables.forEach(table => {
        const rows = (table.rows || []).filter(row =>
          (row || []).some(cell => String((cell && cell.text) || '').trim()));
        if (rows.length === 0) return;
        const wrap = el('div', 'tablewrap');
        const tableNode = el('table');
        rows.forEach(row => {
          const tr = el('tr');
          row.forEach(cell => {
            tr.appendChild(el(cell && cell.header ? 'th' : 'td', '',
              String((cell && cell.text) || '').trim()));
          });
          tableNode.appendChild(tr);
        });
        wrap.appendChild(tableNode);
        app.appendChild(wrap);
        shown++;
      });
      if (shown === 0) {
        note('Табличні дані відсутні.');
        if (doc.path) tile(app, 'Відкрити оригінал', false, () => openPath(doc.path));
      }
    }

    function render(state, restoreScroll) {
      const token = ++renderToken;
      if (state.v === 'root') {
        renderRoot();
        finish(restoreScroll);
        return;
      }
      clear('Завантаження…');
      note('Відкриваю…');
      sourceFor(state)
        .then(src => {
          if (token !== renderToken) return;
          if (state.v === 'docs') {
            renderDocsRoot(src, state);
          } else if (state.v === 'panel') {
            if (state.s) {
              renderSectionPanel(src, state);
            } else {
              renderPanelBody(src, state);
            }
          } else if (state.v === 'parts') {
            renderParts(src, state);
          } else if (state.v === 'table') {
            renderTable(src, state);
          } else {
            renderRoot();
          }
          finish(restoreScroll);
        })
        .catch(error => {
          if (token !== renderToken) return;
          clear('Помилка');
          note((error && error.message) || 'Не вдалося відкрити.', true);
        });
    }

    function finish(restoreScroll) {
      const saved = restoreScroll ? Number(load('scroll|' + location.hash)) : 0;
      window.scrollTo(0, saved > 0 ? saved : 0);
    }

    window.addEventListener('popstate', () => {
      render(stateFromHash(), true);
    });

    // First open: the volume is the bottom of the «←» chain and the volume
    // documentation is opened on top of it (as the companion did before).
    if (!location.hash || location.hash === '#') {
      history.replaceState({ v: 'root' }, '', '#v=root');
      if (DATA.documentationPath) {
        go({ v: 'docs' });
      } else {
        render({ v: 'root' }, false);
      }
    } else {
      render(stateFromHash(), true);
    }
  </script>
</body>
</html>
""".trimIndent()
}
