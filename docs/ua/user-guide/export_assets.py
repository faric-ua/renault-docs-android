"""Exports the user guide content for the Word/PDF build.

Reads the sections from build_guide.py (one source for all formats), turns each
Mermaid flowchart into a Graphviz PNG (Mermaid cannot run offline here) and
writes guide.json for build_docx.js.

Usage: python3 export_assets.py <out_dir>
"""
import json
import textwrap
import re
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import build_guide  # noqa: E402  (module-level S holds the sections)

NODE = re.compile(r'([A-Za-z0-9_]+)(\["(.*?)"\]|\{"(.*?)"\})')
EDGE = re.compile(r'^\s*([A-Za-z0-9_]+)(?:\[".*?"\]|\{".*?"\})?\s*-->\s*(?:\|"(.*?)"\|\s*)?([A-Za-z0-9_]+)')

ACCENT = "#1f5fa8"
FILL = "#e5eef8"
DECISION = "#fbf1df"


def dot_escape(text, width=24):
    # Wrap long labels so diagrams stay narrow enough for an A4 page.
    lines = textwrap.wrap(text, width=width, break_long_words=False) or [text]
    return "\\n".join(line.replace("\\", "\\\\").replace('"', '\\"') for line in lines)


def mermaid_to_dot(src, rankdir="LR"):
    nodes = {}
    edges = []
    for line in src.splitlines()[1:]:
        for m in NODE.finditer(line):
            nid, _, box, diamond = m.groups()
            if box is not None:
                nodes[nid] = (box, "box")
            elif diamond is not None:
                nodes[nid] = (diamond, "diamond")
        e = EDGE.match(line)
        if e:
            edges.append((e.group(1), e.group(3), e.group(2)))
    out = [
        "digraph G {",
        f'graph [rankdir={rankdir}, bgcolor="white", nodesep=0.35, ranksep=0.45, pad=0.2, dpi=200];',
        f'node [fontname="DejaVu Sans", fontsize=13, shape=box, style="rounded,filled", fillcolor="{FILL}", color="{ACCENT}", fontcolor="#16202c", margin="0.18,0.10"];',
        f'edge [fontname="DejaVu Sans", fontsize=11, color="#566476", fontcolor="#566476", arrowsize=0.7];',
    ]
    for nid, (label, shape) in nodes.items():
        if shape == "diamond":
            out.append(f'{nid} [label="{dot_escape(label)}", shape=diamond, style="filled", fillcolor="{DECISION}", color="#8a5a12"];')
        else:
            out.append(f'{nid} [label="{dot_escape(label)}"];')
    for a, b, label in edges:
        attr = f' [label=" {dot_escape(label, 18)} "]' if label else ""
        out.append(f"{a} -> {b}{attr};")
    out.append("}")
    return "\n".join(out)


PAGE_W, PAGE_H = 600, 720  # display box in the Word page, px at 96 dpi


def png_size(path):
    data = Path(path).read_bytes()
    return int.from_bytes(data[16:20], "big"), int.from_bytes(data[20:24], "big")


def render_best(src, png):
    """Render top-down and left-right; keep the one whose text ends up larger on an A4 page."""
    best = None
    for rankdir in ("TB", "LR"):
        tmp = png.with_suffix(f".{rankdir}.png")
        subprocess.run(["dot", "-Tpng", "-o", str(tmp)], input=mermaid_to_dot(src, rankdir).encode("utf-8"), check=True)
        w, h = png_size(tmp)
        scale = min(PAGE_W / (w * 96 / 200), PAGE_H / (h * 96 / 200), 1.0)
        if best is None or scale > best[0]:
            best = (scale, tmp)
    for rankdir in ("TB", "LR"):
        tmp = png.with_suffix(f".{rankdir}.png")
        if tmp == best[1]:
            tmp.replace(png)
        else:
            tmp.unlink()


def main():
    out_dir = Path(sys.argv[1])
    img_dir = out_dir / "diagrams"
    img_dir.mkdir(parents=True, exist_ok=True)
    sections = []
    count = 0
    for sid, title, blocks in build_guide.S:
        items = []
        for kind, val in blocks:
            if kind == "mermaid":
                count += 1
                png = img_dir / f"{count:02d}-{sid}.png"
                render_best(val, png)
                items.append({"kind": "image", "path": str(png.resolve())})
            else:
                items.append({"kind": kind, "value": val})
        sections.append({"id": sid, "title": title, "blocks": items})
    meta = {"version": build_guide.VERSION, "date": build_guide.DATE}
    (out_dir / "guide.json").write_text(json.dumps({"meta": meta, "sections": sections}, ensure_ascii=False, indent=1), encoding="utf-8")
    print("diagrams:", count)


if __name__ == "__main__":
    main()
