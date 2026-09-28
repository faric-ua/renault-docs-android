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


if __name__ == "__main__":
    unittest.main()
