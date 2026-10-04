import tempfile
import unittest
from pathlib import Path

from core.volumes import discover_volumes


class VolumeDiscoveryTests(unittest.TestCase):
    def test_sorts_dated_volumes_chronologically(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)

            names = [
                "Laguna II X74_NT8283A_Visu v3.0_2005.08.29",
                "Laguna X74 NT8183A 2001_01_22",
                "Laguna X74 NT8236A 2002_11_18",
                "Laguna X74 NT8218A 2002_05_01",
            ]

            for name in names:
                folder = root / name
                folder.mkdir()
                (folder / "INDEX.HTM").write_text(
                    "<html></html>",
                    encoding="latin-1",
                )

            volumes = discover_volumes(root)

            self.assertEqual(
                [
                    "2001-01-22",
                    "2002-05-01",
                    "2002-11-18",
                    "2005-08-29",
                ],
                [volume["date"] for volume in volumes],
            )
            self.assertEqual(
                [
                    "NT8183A",
                    "NT8218A",
                    "NT8236A",
                    "NT8283A",
                ],
                [volume["document_code"] for volume in volumes],
            )


    def test_extracts_grouped_vehicle_codes_and_visu_identity(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            folder = (
                root
                / "Megane II E,L,K 84_NT8340A_Visu v3.0_2006.04.18"
            )
            folder.mkdir()
            (folder / "INDEX.HTM").write_text(
                "<html></html>",
                encoding="latin-1",
            )

            volume = discover_volumes(root)[0]

            self.assertEqual(
                ["E84", "L84", "K84"],
                volume["vehicle_codes"],
            )
            self.assertEqual(
                "Visu",
                volume["document_type"],
            )
            self.assertEqual(
                "3.0",
                volume["document_version"],
            )
            self.assertEqual(
                "NT8340A",
                volume["document_code"],
            )
            self.assertEqual(
                "2006-04-18",
                volume["date"],
            )

    def test_extracts_platform_style_code_and_region(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            folder = (
                root
                / "Laguna II X74 Europe_NT8283A_Visu v3.0_2005.08.29"
            )
            folder.mkdir()
            (folder / "INDEX.HTM").write_text(
                "<html></html>",
                encoding="latin-1",
            )

            volume = discover_volumes(root)[0]

            self.assertEqual(
                ["X74"],
                volume["vehicle_codes"],
            )
            self.assertEqual(
                "Europe",
                volume["region"],
            )



    def test_legacy_visu_schema_title_recovers_document_type(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            folder = root / "Laguna X74 NT8183A 2001_01_22"
            folder.mkdir()
            (folder / "INDEX.HTM").write_text(
                "<html><head><title>Visu Schema</title></head>"
                "<body>Laguna 2 NT : 8183A 22.01.2001</body></html>",
                encoding="latin-1",
            )

            volume = discover_volumes(root)[0]

            self.assertEqual(
                ["X74"],
                volume["vehicle_codes"],
            )
            self.assertEqual(
                "Visu",
                volume["document_type"],
            )
            self.assertNotIn(
                "document_version",
                volume,
            )
            self.assertEqual(
                "wiring-diagrams",
                volume["kind"],
            )



if __name__ == "__main__":
    unittest.main()
