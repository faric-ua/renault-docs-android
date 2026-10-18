"""Renodocs 0.6.1 (2026-10-18): background emblem on the home screen (TODO №27)
and «Renault» removed from the app's own captions (TODO №28).

№28 — owner 02:48 / 03:00: the app is Renodocs, so its *own* captions no longer
say «Renault» («Мої Renault» → «Мої проєкти», …). Text that describes the
content of the volumes (original Renault pages, Renault codes) stays — that is
an honest description, not our name.
"""

import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/saney/renaultdocs"
RES = ROOT / "android/app/src/main/res/drawable-nodpi"


class HomeEmblem(unittest.TestCase):
    def test_four_small_emblems_are_packaged(self):
        for i in range(1, 5):
            path = RES / f"home_emblem_{i}.webp"
            self.assertTrue(path.is_file(), path)
            data = path.read_bytes()
            self.assertTrue(data.startswith(b"RIFF") and data[8:12] == b"WEBP")
            self.assertLess(len(data), 250_000, "keep the APK small")

    def test_emblem_object(self):
        kt = (SRC / "HomeEmblem.kt").read_text(encoding="utf-8")
        for i in range(1, 5):
            self.assertIn(f"R.drawable.home_emblem_{i}", kt)
        self.assertIn("by lazy", kt)  # once per launch, not on every redraw
        alpha = float(re.search(r"ALPHA: Float = ([0-9.]+)f", kt).group(1))
        self.assertGreater(alpha, 0.05)
        self.assertLessEqual(alpha, 0.25, "muted so the tiles stay readable")

    def test_emblem_sits_behind_the_project_list(self):
        main = (SRC / "MainActivity.kt").read_text(encoding="utf-8")
        start = main.index("val libraryArea = FrameLayout(this)")
        block = main[start:start + 2000]
        image = block.index("setImageResource(HomeEmblem.current)")
        scroll = block.index("libraryArea.addView(\n            scroll,")
        self.assertLess(image, scroll, "emblem first = drawn behind the tiles")
        self.assertIn("isClickable = false", block)
        self.assertIn("IMPORTANT_FOR_ACCESSIBILITY_NO", block)


class NoRenaultInOwnCaptions(unittest.TestCase):
    OLD = [
        "Мої Renault",
        "Новий проєкт Renault",
        "Бібліотека Renault технічної документації",
        "Проєкт = модель Renault",
        "Проєкт — це одна модель Renault",
        "Останні завершені операції з томами Renault",
    ]

    def test_app_captions(self):
        text = "\n".join(p.read_text(encoding="utf-8") for p in SRC.glob("*.kt"))
        for old in self.OLD:
            self.assertNotIn(old, text)
        self.assertIn('value = "Мої проєкти"', text)
        self.assertIn('"Новий проєкт"', text)
        self.assertIn("Бібліотека технічної документації для Android", text)
        self.assertIn("Проєкт = модель авто", text)

    def test_guide(self):
        for path in [ROOT / "docs/ua/user-guide/build_guide.py", ROOT / "docs/ua/USER_GUIDE.md",
                     ROOT / "android/app/src/main/assets/guide/index.html"]:
            self.assertNotIn("Мої Renault", path.read_text(encoding="utf-8"), path)


if __name__ == "__main__":
    unittest.main()
