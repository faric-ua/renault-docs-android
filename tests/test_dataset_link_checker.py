import json
import tempfile
import unittest
from pathlib import Path

from tools.check_dataset_links import (
    check_dataset,
    compare_package_volume_parity,
    compare_volume_parity,
)


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

    def test_volume_parity_reports_missing_source_volume(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "source"
            build = root / "build"
            source.mkdir()
            build.mkdir()

            for parent, names in (
                (source, ("Volume A NT8183A 2001_01_22", "Volume B NT8218A 2002_05_01")),
                (build, ("Volume A NT8183A 2001_01_22",)),
            ):
                for name in names:
                    volume = parent / name
                    volume.mkdir()
                    (volume / "INDEX.HTM").write_text("<html></html>", encoding="utf-8")

            parity = compare_volume_parity(source, build)

            self.assertEqual("FAIL", parity["status"])
            self.assertEqual(2, parity["source_volume_count"])
            self.assertEqual(1, parity["build_volume_count"])
            self.assertEqual(
                ["Volume B NT8218A 2002_05_01"],
                parity["missing_in_build"],
            )
            self.assertEqual([], parity["extra_in_build"])

    def test_volume_parity_passes_for_matching_volume_sets(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "source"
            build = root / "build"
            source.mkdir()
            build.mkdir()

            for parent in (source, build):
                volume = parent / "Volume NT8183A 2001_01_22"
                volume.mkdir()
                (volume / "INDEX.HTM").write_text("<html></html>", encoding="utf-8")

            parity = compare_volume_parity(source, build)

            self.assertEqual("PASS", parity["status"])
            self.assertEqual(1, parity["source_volume_count"])
            self.assertEqual(1, parity["build_volume_count"])
            self.assertEqual([], parity["missing_in_build"])

    def test_package_volume_parity_detects_stale_three_volume_metadata(self):
        with tempfile.TemporaryDirectory() as tmp:
            build = Path(tmp)
            names = (
                "Volume A NT8183A 2001_01_22",
                "Volume B NT8218A 2002_05_01",
            )
            for name in names:
                volume = build / name
                volume.mkdir()
                (volume / "INDEX.HTM").write_text(
                    "<html></html>",
                    encoding="utf-8",
                )

            stale = [
                {
                    "source_folder": names[0],
                    "entrypoint": f"{names[0]}/INDEX.HTM",
                }
            ]
            (build / "_renault").mkdir()
            (build / "renault-dataset.json").write_text(
                json.dumps({"volumes": stale}),
                encoding="utf-8",
            )
            (build / "_renault" / "volumes.json").write_text(
                json.dumps(stale),
                encoding="utf-8",
            )
            (build / "_renault" / "modern-index.json").write_text(
                json.dumps({"navigation": {"volumes": stale}}),
                encoding="utf-8",
            )

            parity = compare_package_volume_parity(build)

            self.assertEqual("FAIL", parity["status"])
            self.assertEqual(2, parity["live_volume_count"])
            for source in parity["sources"].values():
                self.assertEqual("FAIL", source["status"])
                self.assertEqual(1, source["volume_count"])
                self.assertEqual(
                    [names[1]],
                    source["missing_from_metadata"],
                )

    def test_package_volume_parity_passes_when_all_metadata_matches_build(self):
        with tempfile.TemporaryDirectory() as tmp:
            build = Path(tmp)
            names = (
                "Volume A NT8183A 2001_01_22",
                "Volume B NT8218A 2002_05_01",
            )
            volumes = []
            for name in names:
                volume = build / name
                volume.mkdir()
                (volume / "INDEX.HTM").write_text(
                    "<html></html>",
                    encoding="utf-8",
                )
                volumes.append(
                    {
                        "source_folder": name,
                        "entrypoint": f"{name}/INDEX.HTM",
                    }
                )

            (build / "_renault").mkdir()
            (build / "renault-dataset.json").write_text(
                json.dumps({"volumes": volumes}),
                encoding="utf-8",
            )
            (build / "_renault" / "volumes.json").write_text(
                json.dumps(volumes),
                encoding="utf-8",
            )
            (build / "_renault" / "modern-index.json").write_text(
                json.dumps({"navigation": {"volumes": volumes}}),
                encoding="utf-8",
            )

            parity = compare_package_volume_parity(build)

            self.assertEqual("PASS", parity["status"])
            self.assertEqual(2, parity["live_volume_count"])
            for source in parity["sources"].values():
                self.assertEqual("PASS", source["status"])
                self.assertEqual(2, source["volume_count"])
                self.assertEqual([], source["missing_from_metadata"])
                self.assertEqual([], source["extra_in_metadata"])

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
