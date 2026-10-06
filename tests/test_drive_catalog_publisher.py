from __future__ import annotations

import hashlib
import json
import sys
import tempfile
import unittest
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile

REPO_ROOT = Path(__file__).resolve().parents[1]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from core.rdpkg import default_package_filename
from tools.build_drive_catalog import (
    build_catalog,
    load_publish_plan,
)


class DriveCatalogPublisherTests(unittest.TestCase):
    def make_package(
        self,
        root: Path,
        *,
        project_id: str,
        model: str,
        volume: dict,
    ) -> Path:
        dataset = {
            "id": f"{project_id}-dataset",
            "project_id": project_id,
            "title": model,
            "model": model,
            "manufacturer": "Renault",
            "volumes": [volume],
        }
        file_name = default_package_filename(
            dataset,
            volume,
        )
        path = root / file_name

        package_id = "-".join(
            [
                project_id,
                str(volume["document_code"]).lower(),
                str(volume["date"]),
            ]
        )

        package = {
            "schema_version": 1,
            "format": "renault-volume-package-v1",
            "package_id": package_id,
            "project_id": project_id,
            "dataset_id": dataset["id"],
            "dataset_title": model,
            "volume": volume,
            "dataset_manifest": "renault-dataset.json",
            "payload_file_count": 1,
            "payload_bytes": 6,
        }

        with ZipFile(
            path,
            "w",
            compression=ZIP_DEFLATED,
        ) as archive:
            archive.writestr(
                "rdpkg.json",
                json.dumps(package),
            )
            archive.writestr(
                "renault-dataset.json",
                json.dumps(dataset),
            )
            archive.writestr(
                str(volume["entrypoint"]),
                "<html><title>Visu Schema</title></html>",
            )

        return path

    def write_plan(
        self,
        root: Path,
        packages: list[dict[str, str]],
    ) -> Path:
        path = root / "publish-plan.json"
        path.write_text(
            json.dumps(
                {
                    "schema_version": 1,
                    "catalog_id": "renault-docs-public",
                    "catalog_version": 3,
                    "generated_at": "2026-10-06T20:00:00+03:00",
                    "packages": packages,
                }
            ),
            encoding="utf-8",
        )
        return path

    def test_builds_catalog_from_canonical_packages_and_drive_ids(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)

            first = self.make_package(
                root,
                project_id="laguna-ii",
                model="Laguna II",
                volume={
                    "id": "laguna-x74-nt8183a-2001-01-22",
                    "title": "NT8183A · 2001-01-22",
                    "document_code": "NT8183A",
                    "date": "2001-01-22",
                    "vehicle_codes": ["X74"],
                    "document_type": "Visu",
                    "document_version": "1.0",
                    "region": None,
                    "source_folder": "Laguna X74 NT8183A 2001_01_22",
                    "entrypoint": "Laguna X74 NT8183A 2001_01_22/INDEX.HTM",
                },
            )

            second = self.make_package(
                root,
                project_id="laguna-ii",
                model="Laguna II",
                volume={
                    "id": "laguna-x74-nt8328a-2006-05-09",
                    "title": "NT8328A · 2006-05-09",
                    "document_code": "NT8328A",
                    "date": "2006-05-09",
                    "vehicle_codes": ["X74"],
                    "document_type": "Visu",
                    "document_version": "2.0",
                    "region": "Europe",
                    "source_folder": "Laguna X74 Europe NT8328A Visu v2.0 2006_05_09",
                    "entrypoint": (
                        "Laguna X74 Europe NT8328A Visu v2.0 "
                        "2006_05_09/INDEX.HTM"
                    ),
                },
            )

            plan_path = self.write_plan(
                root,
                [
                    {
                        "file_name": second.name,
                        "drive_file_id": "drive-second",
                    },
                    {
                        "file_name": first.name,
                        "drive_file_id": "drive-first",
                    },
                ],
            )

            plan = load_publish_plan(
                plan_path,
            )
            catalog = build_catalog(
                root,
                plan,
            )

            self.assertEqual(
                catalog["schema_version"],
                1,
            )
            self.assertEqual(
                catalog["catalog_version"],
                3,
            )
            self.assertEqual(
                len(catalog["projects"]),
                1,
            )

            project = catalog["projects"][0]
            self.assertEqual(
                project["id"],
                "laguna-ii",
            )
            self.assertEqual(
                project["title"],
                "Laguna II",
            )
            self.assertEqual(
                project["vehicle_codes"],
                ["X74"],
            )
            self.assertEqual(
                project["document_year_from"],
                2001,
            )
            self.assertEqual(
                project["document_year_to"],
                2006,
            )
            self.assertEqual(
                [
                    volume["document_code"]
                    for volume in project["volumes"]
                ],
                [
                    "NT8183A",
                    "NT8328A",
                ],
            )

            first_volume = project["volumes"][0]
            self.assertEqual(
                first_volume["drive_file_id"],
                "drive-first",
            )
            self.assertEqual(
                first_volume["file_name"],
                first.name,
            )
            self.assertEqual(
                first_volume["size_bytes"],
                first.stat().st_size,
            )
            self.assertEqual(
                first_volume["sha256"],
                hashlib.sha256(
                    first.read_bytes()
                ).hexdigest(),
            )

    def test_public_catalog_v3_plan_is_complete(self):
        plan_path = REPO_ROOT / "config" / "catalog-publish-plan.v3.json"
        plan = load_publish_plan(plan_path)

        self.assertEqual(plan["catalog_version"], 3)
        self.assertEqual(len(plan["packages"]), 12)

        expected_codes = {
            "NT8183A",
            "NT8218A",
            "NT8236A",
            "NT8240A",
            "NT8254A",
            "NT8282A",
            "NT8283A",
            "NT8307A",
            "NT8327A",
            "NT8328A",
            "NT8340A",
            "NT8342A",
        }
        actual_codes = {
            next(
                code
                for code in expected_codes
                if code in package["file_name"]
            )
            for package in plan["packages"]
        }

        self.assertEqual(actual_codes, expected_codes)
        self.assertEqual(
            len({package["drive_file_id"] for package in plan["packages"]}),
            12,
        )

    def test_rejects_non_canonical_filename(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)

            package = self.make_package(
                root,
                project_id="laguna-ii",
                model="Laguna II",
                volume={
                    "id": "laguna-x74-nt8183a-2001-01-22",
                    "title": "NT8183A · 2001-01-22",
                    "document_code": "NT8183A",
                    "date": "2001-01-22",
                    "vehicle_codes": ["X74"],
                    "document_type": "Visu",
                    "document_version": "1.0",
                    "source_folder": "Laguna X74 NT8183A 2001_01_22",
                    "entrypoint": "Laguna X74 NT8183A 2001_01_22/INDEX.HTM",
                },
            )

            wrong = root / "NT8183A.rdpkg"
            package.rename(wrong)

            plan = load_publish_plan(
                self.write_plan(
                    root,
                    [
                        {
                            "file_name": wrong.name,
                            "drive_file_id": "drive-first",
                        }
                    ],
                )
            )

            with self.assertRaisesRegex(
                ValueError,
                "non-canonical filename",
            ):
                build_catalog(
                    root,
                    plan,
                )

    def test_publish_plan_rejects_duplicate_drive_ids(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            plan_path = self.write_plan(
                root,
                [
                    {
                        "file_name": "one.rdpkg",
                        "drive_file_id": "same",
                    },
                    {
                        "file_name": "two.rdpkg",
                        "drive_file_id": "same",
                    },
                ],
            )

            with self.assertRaisesRegex(
                ValueError,
                "duplicate drive_file_id",
            ):
                load_publish_plan(
                    plan_path,
                )


if __name__ == "__main__":
    unittest.main()
