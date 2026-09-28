from pathlib import Path
import unittest


class V0513ModernSectionTilesContractTests(unittest.TestCase):
    def test_global_nav_mode_switch_persistent_tiles_and_content_cards(self):
        repo = Path(__file__).resolve().parents[1]

        native = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeSectionActivity.kt"
        ).read_text(encoding="utf-8")
        volume = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/ModernVolumeActivity.kt"
        ).read_text(encoding="utf-8")

        # Global app navigation sits above the section context row.
        self.assertIn("ic_home", native)
        self.assertIn("ic_search", native)
        self.assertIn("ic_settings", native)
        self.assertIn('"Пошук розділів"', native)

        # The context row shows only the section identifier plus a
        # civilized Modern/Classic segmented switch.
        self.assertIn("value = sectionCode", native)
        self.assertIn('label = "Modern"', native)
        self.assertIn('label = "Classic"', native)
        self.assertIn("modeButton(", native)

        # Core section actions never disappear just because the current
        # Runtime IR section does not expose an action.
        self.assertIn('label = "Схеми"', native)
        self.assertIn('label = "Розʼєм"', native)
        self.assertIn('label =\n                    "Положення на авто"', native)
        self.assertIn('label = "Документація"', native)
        self.assertIn("item: JSONObject?", native)
        self.assertIn("0.38f", native)

        # Inner choice blocks are grouped into real cards/tiles.
        self.assertIn("contentTile()", native)
        self.assertIn("addContentTile(", native)
        self.assertIn("tileHeader(", native)
        self.assertIn("buildBodyActionButton(", native)

        # Search from a section reopens the volume with its search field shown.
        self.assertIn("openSearch = true", native)
        self.assertIn("EXTRA_OPEN_SEARCH", volume)
        self.assertIn("openSearchRequested", volume)


if __name__ == "__main__":
    unittest.main()
