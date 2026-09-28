import tempfile
import unittest
from pathlib import Path

from core.dataset_package import build_dataset_package
from core.volumes import discover_volumes


class DatasetPackageTests(unittest.TestCase):
    def test_discovers_top_level_renault_volumes(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)

            first = root / "Laguna X74 NT8183A 2001_01_22"
            first.mkdir()
            (first / "INDEX.HTM").write_text("<html></html>", encoding="utf-8")

            visu = root / "Laguna II X74_NT8328A_Visu v3.0_2006.05.09"
            visu.mkdir()
            (visu / "INDEX.HTM").write_text("<html></html>", encoding="utf-8")

            ignored = root / "img"
            ignored.mkdir()

            volumes = discover_volumes(root)

            self.assertEqual(2, len(volumes))
            by_code = {volume["document_code"]: volume for volume in volumes}
            self.assertEqual("2001-01-22", by_code["NT8183A"]["date"])
            self.assertEqual("technical-documentation", by_code["NT8183A"]["kind"])
            self.assertEqual("wiring-diagrams", by_code["NT8328A"]["kind"])
            self.assertTrue(by_code["NT8328A"]["entrypoint"].endswith("/INDEX.HTM"))

    def test_build_package_generates_catalog_help_and_manifest(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "INDEX.HTM").write_text("<html>root</html>", encoding="utf-8")

            volume = root / "Laguna X74 NT8218A 2002_05_01"
            volume.mkdir()
            (volume / "INDEX.HTM").write_text("<html>volume</html>", encoding="utf-8")

            result = build_dataset_package(
                {
                    "id": "renault-test",
                    "title": "Renault Test",
                    "manufacturer": "Renault",
                    "model": "Test",
                    "platform": "X00",
                    "years": {"from": 2001, "to": 2002},
                    "entrypoint": "INDEX.HTM",
                },
                root,
            )

            self.assertEqual(1, len(result["volumes"]))
            self.assertTrue((root / "_renault" / "START.html").is_file())
            self.assertTrue((root / "_renault" / "README_UA.html").is_file())
            self.assertTrue((root / "_renault" / "volumes.json").is_file())
            self.assertTrue((root / "_renault" / "modern-index.json").is_file())
            self.assertTrue((root / "_renault" / "modern-sections.json").is_file())
            self.assertTrue((root / "_renault" / "runtime-tree.json").is_file())
            self.assertTrue((root / "_renault" / "runtime-ir-index.json").is_file())
            self.assertTrue((root / "_renault" / "runtime-ir" / "sections").is_dir())
            self.assertTrue((root / "_renault" / "runtime-ir-coverage.json").is_file())
            self.assertTrue(result["runtime_ir_index_path"].is_file())
            self.assertTrue(result["runtime_ir_coverage_path"].is_file())
            self.assertTrue(result["fast_pack_path"].is_file())
            self.assertGreater(result["fast_pack"]["file_count"], 0)

            manifest = (root / "renault-dataset.json").read_text(encoding="utf-8")
            self.assertIn('"catalog_entrypoint": "_renault/START.html"', manifest)
            self.assertIn('"legacy_entrypoint": "INDEX.HTM"', manifest)
            self.assertIn('"modern_index": "_renault/modern-index.json"', manifest)
            self.assertIn('"modern_sections": "_renault/modern-sections.json"', manifest)
            self.assertIn('"runtime_tree": "_renault/runtime-tree.json"', manifest)
            self.assertIn('"runtime_ir_index": "_renault/runtime-ir-index.json"', manifest)
            self.assertIn('"runtime_ir_coverage": "_renault/runtime-ir-coverage.json"', manifest)
            self.assertIn('"fast_pack"', manifest)
            self.assertIn('"format": "zip-web-v1"', manifest)
            self.assertIn('"volumes"', manifest)

            catalog = (root / "_renault" / "START.html").read_text(encoding="utf-8")
            self.assertIn("NT8218A", catalog)
            self.assertIn("../Laguna%20X74%20NT8218A%202002_05_01/INDEX.HTM", catalog)


if __name__ == "__main__":
    unittest.main()
