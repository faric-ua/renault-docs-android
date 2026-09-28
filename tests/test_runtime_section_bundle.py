import json
import tempfile
import unittest
import zipfile
from pathlib import Path

from tools.export_runtime_section_bundle import (
    build_bundle,
    pick_section,
    pick_volume,
)


class RuntimeSectionBundleTests(unittest.TestCase):
    def test_exports_section_sources_and_dependency_manifest(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            volume_root = root / "Laguna X74 NT8183A 2001_01_22"
            menu = volume_root / "RUS" / "HTM" / "MENU"
            pc = volume_root / "COMMUN" / "HTM" / "PC"
            js = volume_root / "COMMUN" / "JS"
            pdf = volume_root / "COMMUN" / "PDF" / "PC"

            menu.mkdir(parents=True)
            pc.mkdir(parents=True)
            js.mkdir(parents=True)
            pdf.mkdir(parents=True)

            (menu / "101.HTM").write_text(
                '<script src="../../../COMMUN/JS/common.js"></script>'
                '<a href="../../../COMMUN/HTM/PC/101.HTM">open</a>',
                encoding="utf-8",
            )
            (pc / "101.HTM").write_text(
                '<script>parent.frames["doc"].location='
                '"../../PDF/PC/S7.pdf";</script>',
                encoding="utf-8",
            )
            (js / "common.js").write_text(
                'function x(){return "../HTM/PC/101.HTM";}',
                encoding="utf-8",
            )
            (pdf / "S7.pdf").write_bytes(
                b"%PDF-1.4\n"
            )

            volume = {
                "id": "laguna-x74-nt8183a-2001-01-22",
                "title": "NT8183A · 2001-01-22",
                "document_code": "NT8183A",
                "source_folder": volume_root.name,
                "classic": {"pages": []},
                "modern": {
                    "sections": [
                        {
                            "code": "101",
                            "title": "ПРИКУРИВАТЕЛЬ",
                            "legacy_entrypoint":
                                f"{volume_root.name}/RUS/HTM/MENU/101.HTM",
                            "controls": [],
                            "actions": [],
                            "documents": [],
                        }
                    ]
                },
            }

            output = root / "bundle.zip"
            manifest = build_bundle(
                dataset_root=root,
                volume=volume,
                section=volume["modern"]["sections"][0],
                output_path=output,
            )

            self.assertTrue(output.is_file())
            self.assertGreaterEqual(
                manifest["text_file_count"],
                2,
            )
            self.assertTrue(
                any(
                    item.endswith(
                        "/RUS/HTM/MENU/101.HTM"
                    )
                    for item in manifest["text_files"]
                )
            )
            self.assertTrue(
                any(
                    item.endswith(
                        "/COMMUN/HTM/PC/101.HTM"
                    )
                    for item in manifest["text_files"]
                )
            )
            self.assertTrue(
                any(
                    item.endswith("/COMMUN/PDF/PC/S7.pdf")
                    for item in manifest["binary_references"]
                )
            )

            with zipfile.ZipFile(output) as archive:
                names = set(archive.namelist())
                self.assertIn(
                    "BUNDLE_MANIFEST.json",
                    names,
                )
                self.assertIn(
                    "RUNTIME_SUBSET.json",
                    names,
                )

    def test_volume_and_section_selection(self):
        tree = {
            "volumes": [
                {
                    "id": "x",
                    "document_code": "NT8183A",
                    "title": "NT8183A · 2001-01-22",
                    "modern": {
                        "sections": [
                            {
                                "code": "101",
                                "title": "ПРИКУРИВАТЕЛЬ",
                            }
                        ]
                    },
                }
            ]
        }

        volume = pick_volume(tree, "NT8183A")
        section = pick_section(volume, "101")

        self.assertEqual("x", volume["id"])
        self.assertEqual("101", section["code"])


if __name__ == "__main__":
    unittest.main()
