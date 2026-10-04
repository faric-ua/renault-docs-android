import json
import tempfile
import unittest
from pathlib import Path
from zipfile import ZipFile

from core.rdpkg import (
    RDPKG_FORMAT,
    build_rdpkg,
    default_package_filename,
    list_packageable_volumes,
    select_volume,
)


class RdpkgTests(unittest.TestCase):
    def _prepared_dataset(self, root: Path) -> Path:
        prepared = root / "Megane II_android"
        prepared.mkdir()

        first = prepared / "Megane II_NT8340A_2006.04.18"
        first.mkdir()
        (first / "INDEX.HTM").write_text(
            "<html><body>NT8340A</body></html>",
            encoding="latin-1",
        )

        second = prepared / "Megane II_NT8342A_2006.10.09"
        second.mkdir()
        (second / "INDEX.HTM").write_text(
            "<html><body>NT8342A</body></html>",
            encoding="latin-1",
        )

        backup = prepared / "Backup"
        backup.mkdir()
        (backup / "INDEX.HTM").write_text(
            "<html><body>support only</body></html>",
            encoding="latin-1",
        )

        manifest = {
            "schema_version": 1,
            "id": "megane-ii",
            "title": "Megane II",
            "manufacturer": "Renault",
            "model": "Megane II",
            "project_id": "megane-ii",
            "entrypoint": "Megane II_NT8340A_2006.04.18/INDEX.HTM",
            "viewer_profile": "renault-legacy-web-v1",
        }
        (prepared / "renault-dataset.json").write_text(
            json.dumps(
                manifest,
                ensure_ascii=False,
                indent=2,
            ),
            encoding="utf-8",
        )
        return prepared

    def test_lists_and_selects_volume_by_document_code(self):
        with tempfile.TemporaryDirectory() as temp:
            prepared = self._prepared_dataset(
                Path(temp),
            )
            volumes = list_packageable_volumes(
                prepared,
            )

            self.assertEqual(
                ["NT8340A", "NT8342A"],
                [
                    item.get("document_code")
                    for item in volumes
                ],
            )
            self.assertNotIn(
                "Backup",
                [
                    item.get("source_folder")
                    for item in volumes
                ],
            )
            selected = select_volume(
                volumes,
                "NT8340A",
            )
            self.assertEqual(
                "2006-04-18",
                selected.get("date"),
            )

    def test_multi_volume_dataset_requires_selector(self):
        with tempfile.TemporaryDirectory() as temp:
            prepared = self._prepared_dataset(
                Path(temp),
            )
            volumes = list_packageable_volumes(
                prepared,
            )

            with self.assertRaisesRegex(
                ValueError,
                "--volume",
            ):
                select_volume(
                    volumes,
                    None,
                )

    def test_builds_portable_single_volume_package(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            prepared = self._prepared_dataset(
                root,
            )
            output = root / "NT8340A.rdpkg"

            result = build_rdpkg(
                prepared,
                output,
                volume_selector="NT8340A",
            )

            self.assertTrue(
                output.is_file(),
            )
            self.assertEqual(
                "megane-ii",
                result["project_id"],
            )

            with ZipFile(output) as archive:
                names = set(
                    archive.namelist(),
                )
                self.assertIn(
                    "rdpkg.json",
                    names,
                )
                self.assertIn(
                    "renault-dataset.json",
                    names,
                )
                self.assertIn(
                    "Megane II_NT8340A_2006.04.18/INDEX.HTM",
                    names,
                )
                self.assertNotIn(
                    "Megane II_NT8342A_2006.10.09/INDEX.HTM",
                    names,
                )

                package = json.loads(
                    archive.read(
                        "rdpkg.json",
                    )
                )
                self.assertEqual(
                    RDPKG_FORMAT,
                    package["format"],
                )
                self.assertEqual(
                    "NT8340A",
                    package["volume"][
                        "document_code"
                    ],
                )

                dataset = json.loads(
                    archive.read(
                        "renault-dataset.json",
                    )
                )
                self.assertEqual(
                    1,
                    len(
                        dataset[
                            "volumes"
                        ]
                    ),
                )
                self.assertEqual(
                    "NT8340A",
                    dataset[
                        "volumes"
                    ][0][
                        "document_code"
                    ],
                )

    def test_default_filename_is_human_readable(self):
        dataset = {
            "model": "Megane II",
        }
        volume = {
            "document_code": "NT8340A",
            "date": "2006-04-18",
        }

        self.assertEqual(
            "Megane-II_NT8340A_2006-04-18.rdpkg",
            default_package_filename(
                dataset,
                volume,
            ),
        )


    def test_default_filename_includes_canonical_volume_identity(self):
        dataset = {
            "model": "Megane II",
        }
        volume = {
            "document_code": "NT8340A",
            "date": "2006-04-18",
            "vehicle_codes": ["E84", "L84", "K84"],
            "document_type": "Visu",
            "document_version": "3.0",
        }

        self.assertEqual(
            "Megane-II_E84-L84-K84_NT8340A_Visu-v3.0_2006-04-18.rdpkg",
            default_package_filename(
                dataset,
                volume,
            ),
        )

    def test_default_filename_keeps_region_before_document_code(self):
        dataset = {
            "model": "Megane II",
        }
        volume = {
            "document_code": "NT8342A",
            "date": "2006-10-09",
            "vehicle_codes": ["E84", "L84", "K84"],
            "document_type": "Visu",
            "document_version": "3.0",
            "region": "Europe",
        }

        self.assertEqual(
            "Megane-II_E84-L84-K84_Europe_NT8342A_Visu-v3.0_2006-10-09.rdpkg",
            default_package_filename(
                dataset,
                volume,
            ),
        )



if __name__ == "__main__":
    unittest.main()
