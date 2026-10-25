"""TODO №38 (faric 2026-10-25): посилання на оновлення Renodocs вшите в застосунок."""

from pathlib import Path
import unittest

REPO = Path(__file__).resolve().parents[1]
SRC = REPO / "android/app/src/main/java/com/saney/renaultdocs"
STABLE_ID = "1eCbsQzdHb0LclRL64nDjGCU3ICWELNC0"  # канал стабільної Renault Docs — не для Renodocs


class BuiltInUpdateLinkTests(unittest.TestCase):
    def test_renodocs_channel_is_built_in_and_not_stable(self):
        ui = (SRC / "AppUpdateUi.kt").read_text(encoding="utf-8")
        self.assertIn('const val BUILT_IN_MANIFEST_ID = "1W3iekCBodJCbJcXxjH-8t7CnZsxB_R6w"', ui)
        self.assertNotIn(STABLE_ID, ui)
        self.assertIn("RenaultDocs → AppsPlay", ui)

    def test_file_names_stay_renodocs(self):
        core = (SRC / "AppUpdate.kt").read_text(encoding="utf-8")
        self.assertIn('APK_FILE_NAME = "Renodocs.apk"', core)
        self.assertIn('VERSION_FILE_NAME = "renodocs-onovlennia.json"', core)

    def test_version_and_texts(self):
        gradle = (REPO / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertIn('versionName = "0.6.10"', gradle)
        self.assertIn("versionCode = 180", gradle)
        self.assertTrue((REPO / "android/app/src/main/assets/whats_new.txt").read_text(encoding="utf-8").startswith("0.6.10\n"))
        guide = (REPO / "android/app/src/main/assets/guide/index.html").read_text(encoding="utf-8")
        self.assertIn("v0.6.10", guide)
        self.assertIn("Посилання на оновлення вже вбудоване", guide)


if __name__ == "__main__":
    unittest.main()
