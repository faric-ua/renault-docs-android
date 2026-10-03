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
        self.assertIn("displayGroup(", sorter)
        self.assertIn('normalized.startsWith(', sorter)
        self.assertIn('"R",', sorter)

    def test_release_version(self):
        gradle = self._read("android/app/build.gradle.kts")

        version_code = int(
            gradle.split("versionCode = ", 1)[1].splitlines()[0].strip()
        )
        version_name = tuple(
            int(part)
            for part in gradle.split('versionName = "', 1)[1].split('"', 1)[0].split(".")
        )
        self.assertGreaterEqual(version_code, 69)
        self.assertGreaterEqual(version_name, (0, 5, 53))


if __name__ == "__main__":
    unittest.main()
