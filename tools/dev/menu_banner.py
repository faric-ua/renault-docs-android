"""Великі букви для шапок Termux-меню (напівблоки ▀▄█, шрифт висотою 5 «пікселів» → 3 рядки).

faric 2026-10-16 02:12/02:22: «шапочка відділена полосочками, здоровими буквами, як називається меню» —
на кожне меню кожного проєкту. Звідси згенеровано `show_banner` у:
  renault-docs-android: menu.sh (RENAULT DOCS, жовта), tools/termux/phone-diagnostics-menu.sh (PHONE DIAGNOSTICS, зелена)
  dorama-land-video:    menu.sh (DORAMA DEV, синя), dorama-menu.py (DORAMA.LAND VIDEO, червона)
  ytm:                  tools/termux/ytm-menu.sh (YTM IMPORTER, червона)
  faric-music-visualizer: scripts/termux-menu.sh (MUSIC VISUALIZER, яскраво-зелена)
  faric-track-catcher:  menu.sh (TRACK CATCHER, фіолетова); faric-audio-converter: menu.sh (AUDIO CONVERTER, бірюзова)
Нове меню: `python3 -c "from menu_banner import bash; print(bash(['СЛОВО1','СЛОВО2'],'36','ПРОСТА НАЗВА','faric'))"`.
Ширина: Termux на телефоні вертикально ≈ 60 знаків; вужчий екран — шапка одним рядком.
"""
F = {
 'A':["###","#.#","###","#.#","#.#"], 'C':["###","#..","#..","#..","###"],
 'D':["##.","#.#","#.#","#.#","##."], 'E':["###","#..","##.","#..","###"],
 'G':["###","#..","#.#","#.#","###"], 'H':["#.#","#.#","###","#.#","#.#"],
 'I':["###",".#.",".#.",".#.","###"], 'K':["#.#","#.#","##.","#.#","#.#"],
 'L':["#..","#..","#..","#..","###"], 'M':["#...#","##.##","#.#.#","#...#","#...#"],
 'N':["##.","#.#","#.#","#.#","#.#"], 'O':["###","#.#","#.#","#.#","###"],
 'P':["###","#.#","###","#..","#.."], 'R':["##.","#.#","##.","#.#","#.#"],
 'S':["###","#..","###","..#","###"], 'T':["###",".#.",".#.",".#.",".#."],
 'U':["#.#","#.#","#.#","#.#","###"], 'V':["#.#","#.#","#.#","#.#",".#."],
 'Y':["#.#","#.#","###",".#.",".#."], 'Z':["###","..#",".#.","#..","###"],
 '.':[".",".",".",".","#"], ' ':["..","..","..","..",".."],
}
def render(word):
    rows = [" ".join(F[c][r] for c in word) for r in range(5)]
    rows.append("".join(" " if ch == " " else "." for ch in rows[0]))
    out = []
    for i in (0, 2, 4):
        out.append("".join(" " if x == " " else {("#","#"):"█",("#","."):"▀",(".","#"):"▄"}.get((x, y), " ")
                           for x, y in zip(rows[i], rows[i + 1])).rstrip())
    return out
def lines(words):
    blocks = [render(w) for w in words]
    width = max(len(l) for b in blocks for l in b)
    res = []
    for b in blocks:
        w = max(len(l) for l in b); pad = (width - w) // 2
        res += [" " * pad + l for l in b]
    return res, width
def bash(words, color, plain, who):
    art, width = lines(words)
    W = width + 2
    bar = "━" * W
    body = "\n".join(f'    echo -e "${{c}} {l}${{z}}"' for l in art)
    return f'''# Шапка меню: велика назва між смужками ({who} 2026-10-16 — «щоб одразу видно, яке це меню»).
# Букви — напівблоки ▀▄█ (шрифт 3×5, згенеровано скриптом menu_banner.py), ширина {W} знаків.
# Вужчий екран — проста шапка одним рядком.
show_banner() {{
  local c="\\033[1;{color}m" d="\\033[0;{color}m" z="\\033[0m" cols
  cols="$(stty size 2>/dev/null | cut -d' ' -f2)"
  if [ "${{cols:-80}}" -ge {W} ] 2>/dev/null; then
    echo -e "${{d}}{bar}${{z}}"
{body}
    echo -e "${{d}}{bar}${{z}}"
  else
    echo -e "${{c}}━━ {plain} ━━${{z}}"
  fi
}}
'''
def python(words, color, plain, who):
    art, width = lines(words)
    W = width + 2
    arts = ",\n".join(f'    " {l}"' for l in art)
    return f'''# Шапка меню: велика назва між смужками ({who} 2026-10-16 — «щоб одразу видно, яке це меню»).
# Букви — напівблоки ▀▄█ (шрифт 3×5, згенеровано скриптом menu_banner.py), ширина {W} знаків.
BANNER_ART = (
{arts},
)
BANNER_WIDTH = {W}


def show_banner():
    import shutil
    c, d, z = "\\033[1;{color}m", "\\033[0;{color}m", "\\033[0m"
    if shutil.get_terminal_size((80, 24)).columns < BANNER_WIDTH:
        print(f"{{c}}━━ {plain} ━━{{z}}")
        return
    print(d + "━" * BANNER_WIDTH + z)
    for line in BANNER_ART:
        print(c + line + z)
    print(d + "━" * BANNER_WIDTH + z)
'''
if __name__ == "__main__":
    import sys
    for w in sys.argv[1:]:
        print("\n".join(render(w)))
