"""TODO №37 (faric 2026-10-25): широкі таблиці гортаються вбік, колонки не схлопуються."""

from pathlib import Path
import unittest

REPO = Path(__file__).resolve().parents[1]
SRC = REPO / "android/app/src/main/java/com/saney/renaultdocs"


class WideTablesTests(unittest.TestCase):
    def setUp(self):
        self.native = (SRC / "NativeSectionActivity.kt").read_text(encoding="utf-8")
        start = self.native.index("private fun buildStructuredTable(")
        self.build = self.native[start:self.native.index("private fun measuredCompactColumnWidth(")]

    def test_table_is_wrapped_in_horizontal_scroll(self):
        self.assertIn("HorizontalScrollView(this)", self.build)
        self.assertIn("return tableScroll", self.build)
        self.assertNotIn("return tableBody", self.build)

    def test_no_weight_column_that_can_collapse_to_zero(self):
        self.assertNotIn("1f,", self.build)
        self.assertIn("TableColumnPlan.plan(", self.build)
        self.assertIn("params.weight = 0f", self.build)
        self.assertIn("addOnLayoutChangeListener", self.build)

    def test_plan_limits(self):
        plan = (SRC / "TableColumnPlan.kt").read_text(encoding="utf-8")
        self.assertIn("maxOf(minLast, available - used)", plan)
        self.assertIn("TABLE_MIN_LAST_COLUMN_DP = 140", self.native)
        self.assertIn("TABLE_MAX_MIDDLE_SHARE = 0.6f", self.native)
        self.assertTrue((REPO / "android/app/src/test/java/com/saney/renaultdocs/TableColumnPlanTest.kt").exists())

    def test_version_and_guide(self):
        gradle = (REPO / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertGreaterEqual(int(__import__("re").search(r"versionCode = (\d+)", gradle).group(1)), 179)
        self.assertTrue((REPO / "android/app/src/main/assets/whats_new.txt").read_text(encoding="utf-8").__contains__("0.6.9\n"))
        for path in ("docs/ua/USER_GUIDE.md", "android/app/src/main/assets/guide/index.html"):
            text = (REPO / path).read_text(encoding="utf-8")
            self.assertRegex(text, r"v0\.6\.\d+")
            self.assertIn("уліво-вправо", text)


if __name__ == "__main__":
    unittest.main()
