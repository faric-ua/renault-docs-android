import json
import tempfile
import unittest
from pathlib import Path

from core.dataset_manifest import build_manifest, load_manifest, write_manifest
from core.library import scan_library


class DatasetManifestTests(unittest.TestCase):
    def test_build_manifest_detects_root_index(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "INDEX.HTM").write_text("<html></html>", encoding="utf-8")

            manifest = build_manifest(
                {
                    "id": "renault-test",
                    "title": "Renault Test",
                    "manufacturer": "Renault",
                    "model": "Test",
                    "years": {"from": 2001, "to": 2002},
                },
                root,
            )

            self.assertEqual("INDEX.HTM", manifest["entrypoint"])
            self.assertEqual(1, manifest["schema_version"])

    def test_write_and_scan_library(self):
        with tempfile.TemporaryDirectory() as tmp:
            library = Path(tmp)
            dataset_root = library / "laguna"
            dataset_root.mkdir()
            (dataset_root / "INDEX.HTM").write_text("<html></html>", encoding="utf-8")

            write_manifest(
                {
                    "id": "renault-laguna",
                    "title": "Renault Laguna",
                    "manufacturer": "Renault",
                    "model": "Laguna",
                    "entrypoint": "INDEX.HTM",
                },
                dataset_root,
            )

            loaded = load_manifest(dataset_root / "renault-dataset.json")
            self.assertEqual("Renault Laguna", loaded["title"])

            datasets = scan_library(library)
            self.assertEqual(1, len(datasets))
            self.assertEqual("renault-laguna", datasets[0]["id"])


if __name__ == "__main__":
    unittest.main()
