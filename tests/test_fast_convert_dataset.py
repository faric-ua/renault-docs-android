from pathlib import Path
import json
import subprocess
import sys
import tempfile
import unittest


class FastConverterTests(unittest.TestCase):
    def setUp(self):
        self.repo = Path(__file__).resolve().parents[1]
        self.tool = self.repo / "tools" / "fast_convert_dataset.py"

    def test_new_conversion_uses_direct_filesystem_and_normalizes_paths(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            source = root / "Megane II"
            volume = source / "Megane NT0001"
            volume.mkdir(parents=True)
            (volume / "INDEX.HTM").write_bytes(
                b'<html><img src="pic.gif"></html>'
            )
            (volume / "PIC.GIF").write_bytes(b"GIF89a")
            output = root / "Megane II_android"

            completed = subprocess.run(
                [
                    sys.executable,
                    str(self.tool),
                    "--source",
                    str(source),
                    "--output",
                    str(output),
                    "--model",
                    "Megane II",
                    "--skip-package",
                ],
                cwd=self.repo,
                text=True,
                capture_output=True,
                check=False,
            )

            self.assertEqual(0, completed.returncode, completed.stderr)
            self.assertTrue((output / ".nomedia").is_file())
            self.assertTrue((output / "Megane NT0001" / "PIC.GIF").is_file())
            self.assertIn(
                b'src="PIC.GIF"',
                (output / "Megane NT0001" / "INDEX.HTM").read_bytes(),
            )

            report = json.loads(
                (output / "conversion-report.json").read_text(encoding="utf-8")
            )
            self.assertEqual("new", report["mode"])
            self.assertEqual(1, report["changed_files"])

    def test_merge_copies_only_missing_top_level_volumes(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            source = root / "Megane II"
            source_a = source / "Megane NT0001"
            source_b = source / "Megane NT0002"
            source_a.mkdir(parents=True)
            source_b.mkdir(parents=True)
            (source_a / "INDEX.HTM").write_text("A", encoding="latin-1")
            (source_b / "INDEX.HTM").write_text("B", encoding="latin-1")

            output = root / "Megane II_android"
            existing_a = output / "Megane NT0001"
            existing_a.mkdir(parents=True)
            (existing_a / "INDEX.HTM").write_text("OLD", encoding="latin-1")

            completed = subprocess.run(
                [
                    sys.executable,
                    str(self.tool),
                    "--source",
                    str(source),
                    "--output",
                    str(output),
                    "--model",
                    "Megane II",
                    "--merge",
                    "--skip-package",
                ],
                cwd=self.repo,
                text=True,
                capture_output=True,
                check=False,
            )

            self.assertEqual(0, completed.returncode, completed.stderr)
            self.assertEqual(
                "OLD",
                (existing_a / "INDEX.HTM").read_text(encoding="latin-1"),
            )
            self.assertEqual(
                "B",
                (output / "Megane NT0002" / "INDEX.HTM").read_text(
                    encoding="latin-1"
                ),
            )

            report = json.loads(
                (output / "conversion-report.json").read_text(encoding="utf-8")
            )
            self.assertEqual("merge", report["mode"])
            self.assertEqual(["Megane NT0002"], report["copied_volume_roots"])


if __name__ == "__main__":
    unittest.main()
