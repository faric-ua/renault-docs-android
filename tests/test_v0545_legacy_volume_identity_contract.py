from pathlib import Path
import unittest


class V0545LegacyVolumeIdentityContractTests(unittest.TestCase):
    def _read(self, path: str) -> str:
        repo = Path(__file__).resolve().parents[1]
        return (repo / path).read_text(encoding="utf-8")

    def test_incomplete_legacy_volume_can_be_repaired_from_entrypoint(self):
        store = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectStore.kt"
        )

        self.assertIn("inferVolumeMetadata(", store)
        self.assertIn('"NT[0-9A-Z]+"', store)
        self.assertIn(
            '"(20\\\\d{2})[._-](\\\\d{2})[._-](\\\\d{2})"',
            store,
        )
        self.assertIn("documentCode =", store)
        self.assertIn("date =", store)

    def test_readding_same_volume_can_replace_legacy_record_even_if_tree_uri_differs(self):
        store = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectStore.kt"
        )

        self.assertIn(
            "pair.second.entrypoint ==\n                                volume.entrypoint",
            store,
        )

    def test_release_version(self):
        gradle = self._read("android/app/build.gradle.kts")

        self.assertIn("versionCode = 67", gradle)
        self.assertIn('versionName = "0.5.51"', gradle)


if __name__ == "__main__":
    unittest.main()
