import json
import tempfile
import unittest
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile

from tools.migrate_rdpkg_filenames import (
    apply_migration,
    canonical_name,
    plan_migration,
)


class V0560LocalRdpkgFilenameMigrationTests(unittest.TestCase):
    def _write_package(
        self,
        path: Path,
        *,
        region: str | None = None,
    ) -> None:
        volume = {
            "id": "megane-ii-nt8340a",
            "title": "NT8340A · 2006-04-18",
            "document_code": "NT8340A",
            "date": "2006-04-18",
            "vehicle_codes": ["E84", "L84", "K84"],
            "document_type": "Visu",
            "document_version": "3.0",
            "source_folder": "E84, L84, K84 NT8340A Visu v3.0 2006.04.18",
            "entrypoint": "E84, L84, K84 NT8340A Visu v3.0 2006.04.18/INDEX.HTM",
        }
        if region:
            volume["region"] = region

        package = {
            "schema_version": 1,
            "format": "renault-volume-package-v1",
            "package_id": "megane-ii-nt8340a-2006-04-18",
            "dataset_id": "megane-ii-nt8340a",
            "dataset_title": "Megane II",
            "volume": volume,
            "dataset_manifest": "renault-dataset.json",
        }
        dataset = {
            "schema_version": 1,
            "id": "megane-ii-nt8340a",
            "title": "Megane II",
            "model": "Megane II",
            "volumes": [volume],
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

    def test_old_public_filename_plans_canonical_non_overwriting_rename(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            source = root / "Megane-II_NT8340A_2006-04-18.rdpkg"
            self._write_package(source)

            items = plan_migration(root)

            self.assertEqual(1, len(items))
            self.assertEqual("rename", items[0].status)
            self.assertEqual(
                "Megane-II_E84-L84-K84_NT8340A_Visu-v3.0_2006-04-18.rdpkg",
                items[0].target.name,
            )

            results = apply_migration(items)

            self.assertEqual("renamed", results[0].status)
            self.assertFalse(source.exists())
            self.assertTrue(results[0].target.is_file())

    def test_existing_canonical_target_is_never_overwritten(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            source = root / "Megane-II_NT8340A_2006-04-18.rdpkg"
            self._write_package(source)

            target = (
                root
                / "Megane-II_E84-L84-K84_NT8340A_Visu-v3.0_2006-04-18.rdpkg"
            )
            target.write_bytes(b"keep-me")

            items = plan_migration(root)
            old_item = next(
                item
                for item in items
                if item.source == source
            )

            self.assertEqual("conflict", old_item.status)
            self.assertEqual(b"keep-me", target.read_bytes())
            self.assertTrue(source.exists())

    def test_identity_can_be_recovered_from_legacy_source_folder(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            path = root / "legacy.rdpkg"

            package = {
                "schema_version": 1,
                "format": "renault-volume-package-v1",
                "package_id": "legacy",
                "dataset_title": "Megane II",
                "volume": {
                    "id": "legacy",
                    "source_folder": (
                        "E84, L84, K84 NT8340A Visu v3.0 2006.04.18"
                    ),
                },
                "dataset_manifest": "renault-dataset.json",
            }
            dataset = {
                "id": "legacy",
                "title": "Megane II",
                "model": "Megane II",
                "volumes": [
                    {
                        "id": "legacy",
                        "source_folder": package["volume"]["source_folder"],
                    }
                ],
            }

            with ZipFile(path, "w") as archive:
                archive.writestr("rdpkg.json", json.dumps(package))
                archive.writestr(
                    "renault-dataset.json",
                    json.dumps(dataset),
                )

            self.assertEqual(
                "Megane-II_E84-L84-K84_NT8340A_Visu-v3.0_2006-04-18.rdpkg",
                canonical_name(path),
            )

    def test_legacy_laguna_visu_type_is_recovered_from_entrypoint_html(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            path = root / "Laguna-II_NT8183A_2001-01-22.rdpkg"

            volume = {
                "id": "laguna-ii-nt8183a",
                "title": "NT8183A · 2001-01-22",
                "document_code": "NT8183A",
                "date": "2001-01-22",
                "vehicle_codes": ["X74"],
                "source_folder": "X74 NT8183A 2001.01.22",
                "entrypoint": "X74 NT8183A 2001.01.22/INDEX.HTM",
            }
            package = {
                "schema_version": 1,
                "format": "renault-volume-package-v1",
                "package_id": "laguna-ii-nt8183a-2001-01-22",
                "dataset_title": "Laguna II",
                "volume": volume,
                "dataset_manifest": "renault-dataset.json",
            }
            dataset = {
                "id": "laguna-ii-nt8183a",
                "title": "Laguna II",
                "model": "Laguna II",
                "volumes": [volume],
            }

            with ZipFile(path, "w") as archive:
                archive.writestr("rdpkg.json", json.dumps(package))
                archive.writestr(
                    "renault-dataset.json",
                    json.dumps(dataset),
                )
                archive.writestr(
                    volume["entrypoint"],
                    "<html><head><title>Visu Schema</title></head><body></body></html>",
                )

            self.assertEqual(
                "Laguna-II_X74_NT8183A_Visu_2001-01-22.rdpkg",
                canonical_name(path),
            )

    def test_menu_exposes_explicit_migration_action(self):
        repo = Path(__file__).resolve().parents[1]
        menu = (repo / "menu.sh").read_text(encoding="utf-8")
        launcher = (
            repo
            / "tools/termux/reno-migrate-rdpkg-names.sh"
        ).read_text(encoding="utf-8")

        self.assertIn(
            "24 — Міграція назв локальних .rdpkg",
            menu,
        )
        self.assertIn(
            "reno-migrate-rdpkg-names.sh",
            menu,
        )
        self.assertIn(
            "/storage/emulated/0/Documents/Renault/packages/rdpkg",
            launcher,
        )
        self.assertIn(
            "--apply",
            launcher,
        )


if __name__ == "__main__":
    unittest.main()
