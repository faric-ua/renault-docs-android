"""v0.5.153 — Core Stable candidate (owner 2026-10-17 20:10 «Так» on the scope list).

The user guide catches up with v0.5.142–0.5.152: deleting volumes with their files, the app
update from Google Drive, leftovers of broken-off imports. Guards that the source, the
Markdown and the in-app page all carry the new sections and the app version.
"""
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BUILDER = ROOT / "docs/ua/user-guide"


class GuideCoreStableTest(unittest.TestCase):
    def setUp(self):
        sys.path.insert(0, str(BUILDER))
        import build_guide
        self.g = build_guide
        self.md = (ROOT / "docs/ua/USER_GUIDE.md").read_text(encoding="utf-8")
        self.page = (ROOT / "android/app/src/main/assets/guide/index.html").read_text(encoding="utf-8")

    def test_guide_version_matches_app(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        app = gradle.split('versionName = "')[1].split('"')[0]
        self.assertEqual(self.g.VERSION, "v" + app)

    def test_new_sections_everywhere(self):
        ids = [sid for sid, _, _ in self.g.S]
        for sid, words in (
            ("vydalyty", "Видалити назавжди"),
            ("onovlennia", "Перевірити оновлення"),
        ):
            self.assertIn(sid, ids)
            self.assertIn(f'<a id="{sid}"></a>', self.md)
            self.assertIn(words, self.md)
            self.assertIn(words, self.page)

    def test_settings_mention_leftovers_and_updates(self):
        for words in ("Залишки обірваних імпортів", "Оновлення застосунку", "Знайдено файлів: N"):
            self.assertIn(words, self.md)

    def test_home_tiles_cover_every_section(self):
        sys.path.insert(0, str(BUILDER))
        import build_app_guide
        ids = {sid for sid, _, _ in self.g.S}
        self.assertEqual(ids, set(build_app_guide.HOME))


if __name__ == "__main__":
    unittest.main()
