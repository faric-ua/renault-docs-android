from pathlib import Path
import unittest


class V0546NormalizeSavedVolumeOnReadContractTests(unittest.TestCase):
    def _read(self, path: str) -> str:
        repo = Path(__file__).resolve().parents[1]
        return (repo / path).read_text(encoding="utf-8")

    def test_project_volume_list_normalizes_saved_identity_on_every_read(self):
        store = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectStore.kt"
        )

        self.assertIn(
            "inferVolumeMetadata(\n                    it.second,\n                )",
            store,
        )
        self.assertIn('"NT[0-9A-Z]+"', store)
        self.assertIn(
            '"(20\\\\d{2})[._-](\\\\d{2})[._-](\\\\d{2})"',
            store,
        )

    def test_release_version(self):
        gradle = self._read("android/app/build.gradle.kts")

        self.assertIn("versionCode = 66", gradle)
        self.assertIn('versionName = "0.5.50"', gradle)


if __name__ == "__main__":
    unittest.main()
