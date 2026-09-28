import tempfile
import unittest
from pathlib import Path

from web.pdf_support import (
    PDFJS_ARCHIVE_SHA256,
    PDFJS_DIST_URL,
    PDFJS_VERSION,
    is_pdf_request,
    pdfjs_vendor_ready,
    raw_pdf_url,
    resolve_dataset_path,
    setup_required_html,
    viewer_html,
)


class PdfSupportTests(unittest.TestCase):
    def test_pdf_request_detection_is_case_insensitive(self):
        self.assertTrue(is_pdf_request("/COMMUN/PDF/SCH/0207_A3.PDF"))
        self.assertTrue(is_pdf_request("/docs/manual.pdf?x=1"))
        self.assertFalse(is_pdf_request("/INDEX.HTM"))
        self.assertFalse(is_pdf_request("/__renault__/pdf/raw?path=x.PDF"))

    def test_resolve_dataset_path_decodes_spaces_and_stays_inside_root(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            target = root / "folder name" / "manual.PDF"
            target.parent.mkdir()
            target.write_bytes(b"%PDF-test")

            resolved = resolve_dataset_path(root, "/folder%20name/manual.PDF")
            self.assertEqual(target.resolve(), resolved)

    def test_resolve_dataset_path_rejects_traversal(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            with self.assertRaises(ValueError):
                resolve_dataset_path(root, "/../secret.PDF")

    def test_raw_url_round_trips_unicode_and_spaces(self):
        url = raw_pdf_url("/A B/СХЕМА.PDF")
        self.assertIn("/__renault__/pdf/raw?", url)
        self.assertIn("path=A+B%2F", url)

    def test_viewer_html_uses_local_pdfjs_and_raw_endpoint(self):
        page = viewer_html("/COMMUN/PDF/SCH/0207_A3.PDF")
        self.assertIn("/__renault__/pdfjs/build/pdf.mjs", page)
        self.assertIn("/__renault__/pdf/raw?", page)
        self.assertIn("viewrect=", page)
        self.assertIn("disableRange: true", page)
        self.assertIn('id="pagesContainer"', page)
        self.assertIn("IntersectionObserver", page)
        self.assertIn("renderAround", page)
        self.assertIn("Сторінка ", page)
        self.assertIn("releaseFarPages", page)
        self.assertIn(".page-error[hidden]", page)
        self.assertIn("display: none !important", page)
        self.assertNotIn("renderCurrentPage", page)

    def test_setup_page_contains_install_command(self):
        page = setup_required_html("/manual.PDF")
        self.assertIn("python tools/install_pdfjs.py", page)

    def test_vendor_ready_requires_core_build_files(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.assertFalse(pdfjs_vendor_ready(root))
            (root / "build").mkdir()
            (root / "build" / "pdf.mjs").write_text("", encoding="utf-8")
            (root / "build" / "pdf.worker.mjs").write_text("", encoding="utf-8")
            self.assertTrue(pdfjs_vendor_ready(root))

    def test_pdfjs_version_is_pinned_to_official_release_url(self):
        self.assertIn(PDFJS_VERSION, PDFJS_DIST_URL)
        self.assertTrue(PDFJS_DIST_URL.endswith("-dist.zip"))
        self.assertEqual(
            "98c5832ffe7af4edd59853476a478c0d4d4d76dd49c1701f4c86f7182725cdf9",
            PDFJS_ARCHIVE_SHA256,
        )


if __name__ == "__main__":
    unittest.main()
