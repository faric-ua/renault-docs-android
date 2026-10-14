"""Builds the in-app user guide: android/app/src/main/assets/guide/index.html.

Same single source as the Markdown/Word/PDF guide (build_guide.S), so the page
inside the app never drifts from docs/ua/USER_GUIDE.md.

The page works offline: system font, no network, diagrams are vertical card trees
made from the Mermaid source (Mermaid cannot run offline; Graphviz layouts were too wide for a phone). Dark theme = colours of Ui.kt.
Layout follows the Track Catcher guide (owner request 2026-10-14): a home list
of sections, a tap opens one section, "Назад" returns.

Usage: python3 build_app_guide.py [out_html]
"""
import html
import re
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import build_guide  # noqa: E402

ROOT = HERE.parents[2]
DEFAULT_OUT = ROOT / "android/app/src/main/assets/guide/index.html"

# Icon + one-line description for the home list, per section id.
HOME = {
    "shcho": ("📘", "Проєкт, том, .rdpkg і ⇧ — що це таке"),
    "holovnyi": ("🏠", "Панель «Додати», «Мої Renault», меню ⋮"),
    "dodaty": ("➕", "З файлу, Windows-папки, архіву або каталогу"),
    "katalog": ("☁️", "Каталог: «✓ Встановлено», «Імпортувати вибране»"),
    "chytaty": ("📖", "Modern і Classic, пошук, розʼєми, зелена рамка"),
    "pdf": ("🔍", "Масштаб до 500%, повний екран, друк"),
    "poruch": ("◫", "Два документи одночасно: зверху і знизу"),
    "podilytysia": ("📤", "Один том, кілька томів у папку, весь проєкт"),
    "dovhi": ("⏳", "Картка операції, шторка, звук, «Скасувати»"),
    "nalashtuvannia": ("⚙️", "Режим відкриття, масштаб, крок кнопок"),
    "bezpeka": ("🛡", "Що застосунок ніколи не робить без питання"),
}

NODE = re.compile(r'([A-Za-z0-9_]+)(\["(.*?)"\]|\{"(.*?)"\})')
EDGE = re.compile(r'^\s*([A-Za-z0-9_]+)(?:\[".*?"\]|\{".*?"\})?\s*-->\s*(?:\|"(.*?)"\|\s*)?([A-Za-z0-9_]+)')

# Ui.kt palette
BG, SURFACE, SURFACE_ALT, BORDER = "#101318", "#181D25", "#222936", "#384352"
TEXT, MUTED, ACCENT, SUCCESS = "#F3F6F8", "#AAB5C2", "#76BDFF", "#62D296"
WARM_FILL, WARM_BORDER, WARNING = "#2B251F", "#6E5A42", "#F0B968"


def parse_flowchart(src):
    """Mermaid flowchart → (nodes {id: (label, shape)} in first-seen order, edges [(a, b, label)])."""
    nodes, edges = {}, []
    for line in src.splitlines()[1:]:
        for m in NODE.finditer(line):
            nid, _, box, diamond = m.groups()
            if nid not in nodes:
                nodes[nid] = (box, "box") if box is not None else (diamond, "diamond")
        e = EDGE.match(line)
        if e:
            edges.append((e.group(1), e.group(3), e.group(2)))
    return nodes, edges


def tree_html(mermaid):
    """A phone-friendly diagram: a vertical tree of cards with arrows.

    Graphviz layouts came out 500–1450 pt wide (siblings side by side), i.e. tiny
    text on a 390 px screen (checked in Chromium at phone width, 2026-10-14).
    A node reached again is not repeated: a loop / «←» back step becomes
    «↩ назад до …», a shared result of several paths becomes «→ …».
    """
    nodes, edges = parse_flowchart(mermaid)
    children = {nid: [] for nid in nodes}
    incoming = set()
    for a, b, label in edges:
        children.setdefault(a, []).append((b, label))
        incoming.add(b)
    roots = [nid for nid in nodes if nid not in incoming] or list(nodes)[:1]
    seen = set()

    def card(nid):
        label, shape = nodes[nid]
        cls = "q" if shape == "diamond" else "n"
        return f'<span class="{cls}">{html.escape(label)}</span>'

    def walk(nid, via, path):
        tag = f'<span class="via">{html.escape(via)}</span>' if via else ""
        name = html.escape(nodes[nid][0])
        if nid in path:  # a real loop / «←» back step
            return f'<li>{tag}<span class="n loop">↩ назад до «{name}»</span></li>'
        if nid in seen:  # several paths end in the same result
            return f'<li>{tag}<span class="n loop">→ «{name}»</span></li>'
        seen.add(nid)
        kids = "".join(walk(c, lbl, path | {nid}) for c, lbl in children.get(nid, []))
        sub = f"<ul>{kids}</ul>" if kids else ""
        return f"<li>{tag}{card(nid)}{sub}</li>"

    return '<ul class="tree">' + "".join(walk(r, None, frozenset()) for r in roots) + "</ul>"


def inline(text):
    return build_guide.inline_html(text)


CSS = f"""
:root{{
  --bg:{BG}; --surface:{SURFACE}; --surface-2:{SURFACE_ALT}; --border:{BORDER};
  --text:{TEXT}; --muted:{MUTED}; --accent:{ACCENT}; --ok:{SUCCESS}; --ok-fill:#16271E;
  --warm:{WARM_FILL}; --warm-border:{WARM_BORDER}; --warning:{WARNING};
  --font: system-ui, "Roboto", "Segoe UI", sans-serif;
  color-scheme: dark;
}}
*{{box-sizing:border-box}}
html,body{{background:var(--bg); color:var(--text)}}
body{{margin:0; font-family:var(--font); font-size:16px; line-height:1.55; -webkit-text-size-adjust:100%}}
.wrap{{max-width:760px; margin:0 auto; padding-inline:16px; padding-block:20px 48px}}
h1,h2{{margin:0; line-height:1.25; font-weight:700}}
h1{{font-size:1.7rem; color:#C8E5FF}}
h2{{font-size:1.35rem; color:#C8E5FF}}
p{{margin:0}}
a{{color:var(--accent)}}
b{{color:var(--text)}}
.stack{{display:flex; flex-direction:column; gap:14px}}
.muted{{color:var(--muted)}}
.small{{font-size:.88rem}}
.features{{display:grid; grid-template-columns:1fr; gap:10px; margin:0; padding:0; list-style:none}}
@media (min-width:620px){{ .features{{grid-template-columns:1fr 1fr}} }}
.features a{{display:grid; grid-template-columns:44px 1fr 16px; gap:12px; align-items:center; min-height:64px;
  text-decoration:none; color:var(--text); background:var(--surface); border:1px solid var(--border);
  border-radius:16px; padding:12px 14px}}
.features a:active{{border-color:var(--ok); background:var(--ok-fill)}}
.features .ic{{width:44px; height:44px; border-radius:12px; display:grid; place-items:center; background:var(--surface-2); font-size:20px}}
.features b{{display:block; font-weight:600}}
.features span.d{{display:block; color:var(--muted); font-size:.88rem; line-height:1.4}}
.features .chev{{color:var(--muted)}}
.back{{display:inline-flex; align-items:center; gap:6px; min-height:44px; padding:8px 14px; border-radius:12px; align-self:flex-start;
  border:1px solid var(--border); background:var(--surface-2); color:var(--text); text-decoration:none; font-weight:600}}
ul.points{{margin:0; padding:0; list-style:none; display:flex; flex-direction:column; gap:10px}}
ul.points li{{background:var(--surface); border:1px solid var(--border); border-radius:14px; padding:10px 14px}}
.diagram{{background:var(--surface); border:1px solid var(--border); border-radius:16px; padding:12px; overflow-x:auto}}
.tree, .tree ul{{list-style:none; margin:0; padding:0}}
.tree ul{{margin-left:14px; padding-left:14px; border-left:2px solid var(--border)}}
.tree li{{position:relative; padding-top:10px}}
.tree > li:first-child{{padding-top:0}}
.tree ul > li::before{{content:""; position:absolute; left:-14px; top:28px; width:12px; border-top:2px solid var(--border)}}
.tree .n, .tree .q{{display:inline-block; padding:8px 12px; border-radius:12px; border:1px solid var(--accent); background:var(--surface-2); line-height:1.35}}
.tree .q{{border-color:var(--warning); background:var(--warm); color:#F6DDB8}}
.tree .loop{{border-style:dashed; border-color:var(--border); color:var(--muted); background:transparent}}
.tree .via{{display:block; font-size:.8rem; color:var(--ok); margin:0 0 4px 2px}}
.tree .via::before{{content:"↳ "}}
.note{{background:var(--warm); border:1px solid var(--warm-border); border-radius:14px; padding:12px 14px; color:#D8B58B}}
.next{{display:flex; justify-content:space-between; gap:10px; flex-wrap:wrap; margin-top:6px}}
[hidden]{{display:none !important}}
"""

SCRIPT = """
(function () {
  function show() {
    var id = location.hash.slice(1);
    var target = id ? document.getElementById(id) : null;
    var isPage = !!(target && target.classList.contains('page'));
    document.getElementById('home').hidden = isPage;
    var pages = document.querySelectorAll('section.page');
    for (var i = 0; i < pages.length; i++) pages[i].hidden = pages[i] !== target;
    window.scrollTo(0, 0);
  }
  // v0.5.131: sections switch with location.replace — no history entries, so the
  // app's «Назад» rule is simple (section → list → close), see GuideActivity.goBack().
  document.addEventListener('click', function (e) {
    var a = e.target.closest ? e.target.closest('a') : null;
    if (!a) return;
    var h = a.getAttribute('href');
    if (h && h.charAt(0) === '#') {
      e.preventDefault();
      location.replace(h);
    }
  });
  window.addEventListener('hashchange', show);
  show();
})();
"""


def build():
    sections = build_guide.S
    missing = [sid for sid, _, _ in sections if sid not in HOME]
    if missing:
        raise SystemExit(f"HOME has no icon/description for: {missing}")
    items = []
    for sid, title, _ in sections:
        icon, desc = HOME[sid]
        items.append(
            f'<li><a href="#{sid}"><span class="ic" aria-hidden="true">{icon}</span>'
            f'<span><b>{html.escape(title)}</b><span class="d">{html.escape(desc)}</span></span>'
            f'<span class="chev" aria-hidden="true">›</span></a></li>')
    pages = []
    for n, (sid, title, blocks) in enumerate(sections):
        out = [f'<section id="{sid}" class="page stack" hidden>',
               '<a class="back" href="#">← Усі розділи</a>',
               f"<h2>{html.escape(title)}</h2>"]
        for kind, val in blocks:
            if kind == "p":
                out.append(f"<p>{inline(val)}</p>")
            elif kind == "ul":
                out.append('<ul class="points">' + "".join(f"<li>{inline(i)}</li>" for i in val) + "</ul>")
            elif kind == "note":
                out.append(f'<p class="note">{inline(val)}</p>')
            elif kind == "mermaid":
                out.append(f'<div class="diagram">{tree_html(val)}</div>')
        nxt = sections[n + 1] if n + 1 < len(sections) else None
        links = ['<a class="back" href="#">← Усі розділи</a>']
        if nxt:
            links.append(f'<a class="back" href="#{nxt[0]}">{html.escape(nxt[1])} →</a>')
        out.append('<div class="next">' + "".join(links) + "</div>")
        out.append("</section>")
        pages.append("\n".join(out))
    return f"""<!doctype html>
<html lang="uk">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<title>Інструкція Renault Docs</title>
<!-- Згенеровано docs/ua/user-guide/build_app_guide.py з build_guide.py. Не редагувати вручну. -->
<style>{CSS}</style>
</head>
<body>
<div class="wrap">
<main id="home" class="stack">
  <h1>Renault Docs</h1>
  <p class="muted">Інструкція користувача · {build_guide.VERSION} · {build_guide.DATE}. Технічна документація Renault на телефоні: томи, схеми, розʼєми, PDF. Дотик по розділу — докладно, кнопка «Назад» телефона повертає.</p>
  <ul class="features">
{chr(10).join(items)}
  </ul>
  <p class="small muted">Працює без інтернету. Той самий посібник є у репозиторії: docs/ua/USER_GUIDE.md.</p>
</main>
{chr(10).join(pages)}
</div>
<script>{SCRIPT}</script>
</body>
</html>
"""


if __name__ == "__main__":
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_OUT
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(build(), encoding="utf-8")
    print(f"{out}: {out.stat().st_size // 1024} КБ, розділів: {len(build_guide.S)}")
