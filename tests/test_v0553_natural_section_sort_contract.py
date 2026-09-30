from pathlib import Path
import unittest


class V0553NaturalSectionSortContractTests(unittest.TestCase):
    def _read(self, path: str) -> str:
        repo = Path(__file__).resolve().parents[1]
        return (repo / path).read_text(encoding="utf-8")

    def test_display_sort_is_ui_only_and_runtime_source_order_stays_intact(self):
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ModernVolumeActivity.kt"
        )
        reader = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ModernSectionsReader.kt"
        )
        sorter = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ModernSectionDisplayOrder.kt"
        )

        self.assertIn("ModernSectionDisplayOrder.sorted(", activity)
        self.assertNotIn("sortedWith(", reader)
        self.assertIn("UI-only ordering", sorter)
        self.assertIn("compareCodes(", sorter)
        self.assertIn("withIndex()", sorter)
        self.assertIn("left.index.compareTo(", sorter)

    def test_release_version(self):
        gradle = self._read("android/app/build.gradle.kts")

        self.assertIn("versionCode = 69", gradle)
        self.assertIn('versionName = "0.5.53"', gradle)


if __name__ == "__main__":
    unittest.main()
