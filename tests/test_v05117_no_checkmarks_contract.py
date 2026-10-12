"""v0.5.117 — green frame stays, the ✓ goes (owner 2026-10-13 00:41).

Screenshot «Документація тому»: «Довідка» picked — green frame and a green ✓.
Owner (also 21:41 on 12.10): the ✓ takes width from the text and the text
jumps; keep the green frame, remove all the selection ✓ icons.
Text statuses like «✓ Встановлено» are not selection marks and stay.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


def read(name):
    return (SRC / name).read_text(encoding="utf-8")


class NoCheckmarksContract(unittest.TestCase):
    def test_no_selection_check_icon_anywhere(self):
        for path in SRC.glob("*.kt"):
            with self.subTest(file=path.name):
                self.assertNotIn("R.drawable.ic_check_circle", path.read_text(encoding="utf-8"))

    def test_green_frame_stays(self):
        ui = read("Ui.kt")
        style = ui.split("fun styleChoice(", 1)[1].split("fun refreshChoices(", 1)[0]
        self.assertIn("selectedBackground(context, radiusDp)", style)
        self.assertIn("setCompoundDrawablesRelativeWithIntrinsicBounds(\n            0,\n            0,\n            0,\n            0,", style)
        self.assertIn("Ui.selectedBackground(", read("ModernVolumeActivity.kt"))
        self.assertIn('"ВИБРАНО"', read("NativeSectionActivity.kt"))

    def test_version_bumped(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertGreaterEqual(int(gradle.split("versionCode = ", 1)[1].splitlines()[0]), 133)


if __name__ == "__main__":
    unittest.main()
