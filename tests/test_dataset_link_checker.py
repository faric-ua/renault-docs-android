import json
import tempfile
import unittest
from pathlib import Path

from tools.check_dataset_links import check_dataset


class DatasetLinkCheckerTests(unittest.TestCase):
    def test_valid_relative_html_and_css_links_pass(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "img").mkdir()
            (root / "css").mkdir()
            (root / "img" / "a.gif").write_bytes(b"gif")
            (root / "INDEX.HTM").write_text(
                '<html><head><link href="css/main.css"></head>'
                '<body background="img/a.gif"><a href="PAGE.HTM#x">go</a></body></html>',
                encoding="utf-8",
            )
            (root / "PAGE.HTM").write_text("<html></html>", encoding="utf-8")
            (root / "css" / "main.css").write_text(
                'body { background-image: url("../img/a.gif"); }',
                encoding="utf-8",
            )
            (root / "renault-dataset.json").write_text(
                json.dumps({"entrypoint": "INDEX.HTM"}),
                encoding="utf-8",
            )

            report = check_dataset(root)

            self.assertEqual("PASS", report["status"])
            self.assertEqual(0, report["missing_count"])
            self.assertGreaterEqual(report["checked_references"], 4)

    def test_missing_local_link_is_reported(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "INDEX.HTM").write_text(
                '<img src="IMAGES/MISSING.GIF">',
                encoding="utf-8",
            )

            report = check_dataset(root)

            self.assertEqual("FAIL", report["status"])
            self.assertEqual(1, report["missing_count"])
            self.assertEqual("INDEX.HTM", report["missing"][0]["source"])
            self.assertEqual("IMAGES/MISSING.GIF", report["missing"][0]["resolved"])

    def test_external_fragment_and_data_links_are_ignored(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "INDEX.HTM").write_text(
                '<a href="#local">x</a>'
                '<a href="https://example.com/a">web</a>'
                '<img src="data:image/gif;base64,AAAA">',
                encoding="utf-8",
            )

            report = check_dataset(root)

            self.assertEqual("PASS", report["status"])
            self.assertEqual(0, report["checked_references"])

    def test_query_and_fragment_are_removed_before_resolution(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "PAGE.HTM").write_text("<html></html>", encoding="utf-8")
            (root / "INDEX.HTM").write_text(
                '<a href="PAGE.HTM?mode=1#anchor">x</a>',
                encoding="utf-8",
            )

            report = check_dataset(root)

            self.assertEqual("PASS", report["status"])
            self.assertEqual(1, report["checked_references"])

    def test_parent_escape_is_not_reported_as_missing_inside_dataset(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp) / "dataset"
            root.mkdir()
            (root / "INDEX.HTM").write_text(
                '<a href="../../outside.htm">x</a>',
                encoding="utf-8",
            )

            report = check_dataset(root)

            self.assertEqual("PASS", report["status"])
            self.assertEqual(1, report["skipped_outside_root"])


if __name__ == "__main__":
    unittest.main()
