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

        version_code = int(
            gradle.split("versionCode = ", 1)[1].splitlines()[0].strip()
        )
        version_name = tuple(
            int(part)
            for part in gradle.split('versionName = "', 1)[1].split('"', 1)[0].split(".")
        )
        self.assertGreaterEqual(version_code, 67)
        self.assertGreaterEqual(version_name, (0, 5, 51))


if __name__ == "__main__":
    unittest.main()
