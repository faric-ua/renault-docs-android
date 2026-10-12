"""v0.5.111: «Старі бібліотеки» is visible on Home (phone QA, 2026-10-12).

The owner added the converted Kangoo copy with «Legacy» (status: «Додано…
· томів: 1») but could not find it: the «Старі бібліотеки» list lived inside
the collapsible «Додати» panel (cut off when open, hidden when closed). It is
now its own section in the library list, under «Мої Renault».
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "android/app/src/main/java/com/saney/renaultdocs/MainActivity.kt"


class OldLibrariesVisibleContract(unittest.TestCase):
    def test_section_is_in_library_list_after_projects(self):
        main = MAIN.read_text(encoding="utf-8")
        renderer = main.split("private fun renderLibrary()", 1)[1].split("private fun buildProjectTile(", 1)[0]
        self.assertLess(renderer.index('value = "Мої Renault"'), renderer.index('value = "Старі бібліотеки"'))
        self.assertLess(renderer.index("buildProjectTile("), renderer.index('value = "Старі бібліотеки"'))
        self.assertIn("libraryContainer.addView(\n                    buildDatasetTile(record)", renderer)
        self.assertIn("Копії для браузера (…_android)", renderer)

    def test_not_inside_add_panel_any_more(self):
        main = MAIN.read_text(encoding="utf-8")
        panel = main.split("private fun buildHomeAddPanel(): View", 1)[1].split("private fun buildHomeChoiceCard(", 1)[0]
        self.assertNotIn("legacyAddContainer", main)
        self.assertNotIn("Старі бібліотеки", panel)

    def test_version_bumped(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        code = int(gradle.split("versionCode = ", 1)[1].splitlines()[0].strip())
        self.assertGreaterEqual(code, 127)


if __name__ == "__main__":
    unittest.main()
