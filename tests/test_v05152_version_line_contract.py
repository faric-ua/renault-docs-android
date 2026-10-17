"""v0.5.152 — TODO №25 (M30s 2026-10-17 19:41, owner 19:42: «версія обрізана»): the version
line was the last row of the height-limited «Додати» actions list (BoundedAddActionsScrollView,
46 % of the screen), so on the M30s only its top half showed. It now stands under the list,
outside the scroll, and follows the panel (hidden when collapsed).
"""

import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


class VersionLineContract(unittest.TestCase):
    def test_version_line_outside_the_scroll(self):
        main = (SRC / "MainActivity.kt").read_text(encoding="utf-8")
        actions = main.split("val actions = LinearLayout(this@MainActivity)")[1].split("homeAddPanelBody = BoundedAddActionsScrollView")[0]
        self.assertNotIn("SAF reference mode", actions)
        after = main.split("homeAddPanelBody = BoundedAddActionsScrollView")[1].split("operationStatus = OperationStatusView(")[0]
        self.assertIn("homeVersionLine = Ui.textView(", after)
        self.assertIn('" · SAF reference mode"', after)
        self.assertIn("homeVersionLine.visibility = homeAddPanelBody.visibility", main)

    def test_version(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertGreaterEqual(int(gradle.split("versionCode = ")[1].split()[0]), 168)


if __name__ == "__main__":
    unittest.main()
