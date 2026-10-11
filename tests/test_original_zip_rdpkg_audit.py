"""Stage H: real-shaped Windows ZIP ↔ portable .rdpkg fidelity gate."""
from __future__ import annotations

import contextlib
import io
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
from zipfile import ZipFile

from core.convert_paths import patch_text
from tools.audit_original_zip_rdpkg import audit_original_zip_to_rdpkg, main


class OriginalZipRdpkgAuditTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.folder = "Megane II NT8340A 2006.04.18"
        self.original = self.root / "Megane II NT8340A 2006.04.18.zip"
        self.package = self.root / "Megane-II_NT8340A.rdpkg"
        self.page = self.folder + "/INDEX.HTM"
        self.gif = self.folder + "/IMAGE.GIF"
        self.pdf = self.folder + "/101(pines).PDF"

    def tearDown(self):
        self.temp.cleanup()

    def setup_pair(self, *, normalized=True, override=None, omit=(), extra=None):
        files = {
            self.page: b'<html><img src="image.gif"><a href="101(pines).pdf#page=1">PDF</a></html>',
            self.gif: b"GIF89a-raw-original",
            self.pdf: b"%PDF-1.4\nexample content\n%%EOF",
        }
        with ZipFile(self.original, "w") as raw:
            for name, data in files.items():
                raw.writestr(name, data)
        converted = dict(files)
        if normalized:
            sources = set(files)
            by_lower = {}
            for name in sources:
                by_lower.setdefault(name.casefold(), []).append(name)
            page, _ = patch_text(self.page, files[self.page].decode("latin-1"), sources, by_lower)
            converted[self.page] = page.encode("latin-1")
        if override:
            converted.update(override)
        for name in omit:
            converted.pop(name)
        converted.update(extra or {})
        dataset = {
            "schema_version": 1, "id": "megane-ii",
            "project_id": "megane-ii",
            "title": "Megane II", "model": "Megane II",
            "volumes": [{"id": "megane-nt8340a", "document_code": "NT8340A",
                         "date": "2006-04-18", "entrypoint": self.page,
                         "source_folder": self.folder}],
        }
        payload = {"renault-dataset.json": json.dumps(dataset).encode("utf-8"), **converted}
        rdpkg = {
            "schema_version": 1, "format": "renault-volume-package-v1",
            "package_id": "megane-nt8340a-2006-04-18",
            "project_id": "megane-ii",
            "dataset_manifest": "renault-dataset.json",
            "volume": dataset["volumes"][0],
            "payload_file_count": len(payload),
            "payload_bytes": sum(len(value) for value in payload.values()),
        }
        with ZipFile(self.package, "w") as prepared:
            prepared.writestr("rdpkg.json", json.dumps(rdpkg))
            for name, value in payload.items():
                prepared.writestr(name, value)

    def test_allowed_converter_normalization_and_pdf_gif_preserved(self):
        self.setup_pair()
        before = (self.original.read_bytes(), self.package.read_bytes())
        outcome = audit_original_zip_to_rdpkg(self.original, self.package)
        self.assertEqual("PASS", outcome["status"])
        self.assertEqual("NT8340A", outcome["document_code"])
        self.assertEqual(3, outcome["original_files"])
        self.assertEqual(2, outcome["identical_files"])
        self.assertEqual(1, outcome["allowed_text_normalizations"])
        self.assertEqual(0, outcome["unexplained_modifications"])
        self.assertEqual(0, outcome["missing"])
        self.assertEqual(0, outcome["extra"])
        self.assertGreaterEqual(outcome["static_links"]["counts"]["resolved_exact"], 2)
        self.assertEqual(0, outcome["static_links"]["counts"].get("potential_unresolved", 0))
        self.assertEqual(before, (self.original.read_bytes(), self.package.read_bytes()))
        self.assertNotIn(str(self.root), json.dumps(outcome))

    def test_preserved_but_wrong_case_static_links_require_review(self):
        self.setup_pair(normalized=False)
        result = audit_original_zip_to_rdpkg(self.original, self.package)
        self.assertEqual("REVIEW_REQUIRED", result["status"])
        self.assertEqual(3, result["identical_files"])
        self.assertEqual(0, result["allowed_text_normalizations"])
        self.assertIn("case_sensitive_mismatch", result["link_review_reasons"])

    def test_nontext_pdf_modified_is_rejected(self):
        self.setup_pair(override={self.pdf: b"%PDF-1.4 corrupted"})
        result = audit_original_zip_to_rdpkg(self.original, self.package)
        self.assertEqual("FAIL", result["status"])
        self.assertIn("modified_nontext_asset", [x["kind"] for x in result["mismatches"]])

    def test_unexplained_html_change_is_rejected(self):
        self.setup_pair(override={self.page: b"<html>different meaning</html>"})
        result = audit_original_zip_to_rdpkg(self.original, self.package)
        self.assertEqual("FAIL", result["status"])
        self.assertIn("unexpected_html_or_js_modification",
                      [x["kind"] for x in result["mismatches"]])

    def test_missing_file_is_detected_even_with_valid_rdpkg_manifest(self):
        self.setup_pair(omit=(self.gif,))
        result = audit_original_zip_to_rdpkg(self.original, self.package)
        self.assertEqual("FAIL", result["status"])
        self.assertEqual(1, result["missing"])

    def test_added_volume_file_is_detected(self):
        self.setup_pair(extra={self.folder + "/extra.PDF": b"%PDF-1.4\n"})
        result = audit_original_zip_to_rdpkg(self.original, self.package)
        self.assertEqual("FAIL", result["status"])
        self.assertEqual(1, result["extra"])

    def test_nt_codes_not_interchangeable(self):
        self.setup_pair()
        wrong = self.root / "Megane II NT8342A 2006.10.09.zip"
        self.original.rename(wrong)
        with self.assertRaisesRegex(ValueError, "NT differs"):
            audit_original_zip_to_rdpkg(wrong, self.package)

    def test_original_zip_outside_volume_scope_is_rejected(self):
        self.setup_pair()
        with ZipFile(self.original, "a") as archive:
            archive.writestr("Kangoo II NT8486/INDEX.HTM", "unexpected")
        with self.assertRaisesRegex(ValueError, "outside the selected source volume"):
            audit_original_zip_to_rdpkg(self.original, self.package)

    def test_unsafe_original_entry_is_rejected(self):
        self.setup_pair()
        with ZipFile(self.original, "a") as archive:
            archive.writestr("../untrusted.PDF", "bad")
        with self.assertRaisesRegex(ValueError, "unsafe entry"):
            audit_original_zip_to_rdpkg(self.original, self.package)

    def test_dynamically_constructed_htm_suffix_is_not_broken_link(self):
        # Static suffix markers in historical VISU HTML do not name a file.
        from tools.audit_original_zip_rdpkg import _reference_counts
        source_name = self.folder + "/CTITRE.HTM"
        temp = self.root / "dynamic-markers.zip"
        with ZipFile(temp, "w") as archive:
            archive.writestr(source_name, '<script>var url="./MENU/"+code+".HTM";</script>')
        with ZipFile(temp) as archive:
            result = _reference_counts(
                archive, {source_name}, {source_name.casefold(): [source_name]}
            )
        self.assertEqual(1, result["counts"].get("dynamic_suffix_marker"))
        self.assertEqual(0, result["counts"].get("potential_unresolved", 0))

    def test_cli_success_and_blocked_exit_without_writes(self):
        self.setup_pair()
        argv = ["audit_original_zip_rdpkg.py", "--original", str(self.original),
                "--package", str(self.package), "--skip-link-scan"]
        target = io.StringIO()
        with patch("sys.argv", argv), contextlib.redirect_stdout(target):
            self.assertEqual(0, main())
        self.assertEqual("PASS", json.loads(target.getvalue())["status"])
        self.setup_pair(override={self.pdf: b"broken"})
        with patch("sys.argv", argv), contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(2, main())


if __name__ == "__main__":
    unittest.main()
