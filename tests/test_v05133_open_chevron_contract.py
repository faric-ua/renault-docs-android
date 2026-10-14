"""v0.5.133 (owner, phone QA 2026-10-14 19:59–20:02, M30s, Laguna II 103):
«›» stood alone on the first line of a wrapped tile, and codes like
«F9Q650,…,G9T600» broke mid-code («G9 / T600»). «›» is now its own icon,
centred by the tile height; a line may end only after «,» or «/».
"""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/saney/renaultdocs"
RES = ROOT / "android/app/src/main/res"


def read(name):
    return (SRC / name).read_text(encoding="utf-8")


class OpenChevron(unittest.TestCase):
    def test_helper_is_a_centred_icon(self):
        ui = read("Ui.kt")
        helper = ui.split("fun applyOpenChevron(", 1)[1].split("fun breakableLabel(", 1)[0]
        self.assertIn("R.drawable.ic_chevron_open", helper)
        self.assertIn("setCompoundDrawablesRelative(icon, null, null, null)", helper)
        self.assertTrue((RES / "drawable/ic_chevron_open.xml").is_file())
        self.assertIn('name="tag_open_chevron"', (RES / "values/ids.xml").read_text(encoding="utf-8"))

    def test_line_breaks_only_between_codes(self):
        ui = read("Ui.kt")
        self.assertIn('text.replace(",", ",\\u200B").replace("/", "/\\u200B")', ui)

    def test_choice_style_keeps_the_icon(self):
        style = read("Ui.kt").split("fun styleChoice(", 1)[1].split("fun refreshChoices(", 1)[0]
        clear = style.index("setCompoundDrawablesRelativeWithIntrinsicBounds(")
        restore = style.index("R.id.tag_open_chevron")
        self.assertLess(clear, restore)

    def test_no_chevron_left_in_tile_text(self):
        for name in ("NativeSectionActivity.kt", "VolumeDocumentationActivity.kt"):
            with self.subTest(file=name):
                src = read(name)
                self.assertNotIn('"› " + label', src)
                self.assertNotIn('"› $label"', src)
                self.assertIn("Ui.breakableLabel(label)", src)
                self.assertIn("Ui.applyOpenChevron(this)", src)
        nav = read("CompanionNavigatorPage.kt")
        self.assertNotIn("'› ' + label", nav)
        self.assertIn("el('span', 'chev', '›')", nav)


if __name__ == "__main__":
    unittest.main()
