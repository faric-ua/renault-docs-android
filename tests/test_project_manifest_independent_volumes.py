"""Regression: project catalog is lightweight refs, never a bundled project ZIP.

31 synthetic metadata-only tomes; this checks current catalog *grouping*, not
actual content verification or the future Android per-volume publish UI.
"""
from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from tools.build_drive_catalog import build_catalog


class ProjectManifestIndependentVolumesTests(unittest.TestCase):
    def test_21_megane_and_10_laguna_tomes_stay_separate_metadata_refs(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            plan = {"schema_version": 1, "catalog_version": 5, "catalog_id": "synthetic-catalog", "generated_at": "2026-10-11T17:00:00+03:00", "packages": []}
            by_name = {}
            for project_id, title, count in (
                ("megane-ii", "Megane II", 21),
                ("laguna-ii", "Laguna II", 10),
            ):
                for i in range(count):
                    # Keep neighboring Renault NTs independent, including
                    # the owner-identified distinct NT8340A and NT8342A.
                    if project_id == "megane-ii" and i < 2:
                        nt = ("NT8340A", "NT8342A")[i]
                    elif project_id == "megane-ii":
                        nt = f"NT{8200 + i}A"
                    else:
                        nt = f"NT{8100 + i}A"
                    filename = f"{project_id}_{nt}_{i:02d}.rdpkg"
                    path = root / filename
                    path.write_bytes(b"SYNTHETIC_PLACEHOLDER_NOT_A_RDPKG")
                    file_id = f"verified-id-{project_id}-{i:03d}"
                    plan["packages"].append({
                        "file_name": filename,
                        "drive_file_id": file_id,
                    })
                    by_name[filename] = {
                        "project_id": project_id,
                        "project_title": title,
                        "package_id": f"{project_id}-{nt.lower()}-{i:02d}",
                        "document_code": nt,
                        "date": "2006-10-09" if i % 2 else "2006-04-18",
                        "vehicle_codes": ["X84"] if project_id == "megane-ii" else ["X74"],
                        "document_type": "Visu",
                        "document_version": "3.0",
                        "region": None,
                        "file_name": filename,
                        "size_bytes": path.stat().st_size,
                        "sha256": f"{i + 1:064x}",
                    }

            # Only this specific metadata unit test mocks package inspection:
            # production build_catalog still inspects real canonical .rdpkg.
            with patch(
                "tools.build_drive_catalog.inspect_package",
                side_effect=lambda path: by_name[path.name],
            ) as inspector:
                result = build_catalog(root, plan)

            self.assertEqual(31, inspector.call_count)
            self.assertEqual(
                {"laguna-ii": 10, "megane-ii": 21},
                {p["id"]: len(p["volumes"]) for p in result["projects"]},
            )
            self.assertEqual(2, len(result["projects"]))
            self.assertEqual(
                31, len({v["drive_file_id"] for p in result["projects"] for v in p["volumes"]})
            )
            self.assertEqual(
                31, len({v["id"] for p in result["projects"] for v in p["volumes"]})
            )
            names = {v["document_code"] for p in result["projects"] for v in p["volumes"]}
            self.assertTrue({"NT8340A", "NT8342A"}.issubset(names))
            for p in result["projects"]:
                for volume in p["volumes"]:
                    self.assertEqual(
                        {
                            "id", "document_code", "date", "vehicle_codes",
                            "document_type", "document_version", "region", "file_name",
                            "drive_file_id", "size_bytes", "sha256",
                        },
                        set(volume),
                    )
                    self.assertTrue(volume["file_name"].endswith(".rdpkg"))
                    self.assertNotIn("payload", volume)
            rendered = json.dumps(result, ensure_ascii=False)
            self.assertLess(len(rendered.encode("utf-8")), 32768)
            self.assertNotIn("SYNTHETIC_PLACEHOLDER_NOT_A_RDPKG", rendered)
            self.assertNotIn(".rdproject", rendered)
            self.assertFalse(any(x.suffix == ".rdproject" for x in root.iterdir()))


if __name__ == "__main__":
    unittest.main()
