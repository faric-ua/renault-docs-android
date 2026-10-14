"""v0.5.128 — #162 stage 2: the app reads catalog v5 (Megane II 22 volumes).

Owner 2026-10-14 04:11: «Так, погнали» after seeing the v5 summary. The new
catalog was built by tools/catalog_from_phone_export.py from the phone export
report (17 × EXPORTED_SHA256_VERIFIED) and the exact Drive listing (every size
matched), and created on Drive as a NEW public file; v4 stays untouched.
"""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]


class CatalogV5(unittest.TestCase):
    def test_manifest_points_to_the_new_file(self):
        links = (ROOT / "android/app/src/main/java/com/saney/renaultdocs/ExternalLinks.kt").read_text(encoding="utf-8")
        file_id = re.search(r'PROJECT_CATALOG_MANIFEST_FILE_ID =\s*"([^"]+)"', links).group(1)
        self.assertEqual(file_id, "1js1nkR4OSOtGIXxgMukIqOc8EeERn6vo")
        self.assertNotIn("1mH0YJo1ts_GzpXz9Y4bx7Aj9DKBSgwwA", links)
        self.assertIn("PROJECT_CATALOG_MANIFEST_FILE_ID +", links)

    def test_version(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertGreaterEqual(int(re.search(r"versionCode = (\d+)", gradle).group(1)), 144)


if __name__ == "__main__":
    unittest.main()
