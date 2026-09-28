import tempfile
import unittest
from pathlib import Path
from zipfile import ZipFile

from core.fast_pack import build_fast_pack


class FastPackTests(unittest.TestCase):
    def test_builds_web_only_archive_and_excludes_pdfs(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            package = root / "_renault"
            package.mkdir()

            (root / "INDEX.HTM").write_text(
                "<html>root</html>",
                encoding="latin-1",
            )

            volume = root / "Volume"
            volume.mkdir()
            (volume / "PAGE.HTM").write_text(
                "<html>page</html>",
                encoding="latin-1",
            )
            (volume / "VISU.JS").write_text(
                "var ok = true;",
                encoding="latin-1",
            )
            (volume / "PIC.GIF").write_bytes(
                b"GIF89a",
            )
            (volume / "DOC.PDF").write_bytes(
                b"%PDF-1.4\n",
            )

            (package / "modern-index.json").write_text(
                '{"schema_version": 1}',
                encoding="utf-8",
            )

            result = build_fast_pack(
                output_root=root,
                package_root=package,
            )

            archive_path = (
                root / result["path"]
            )

            self.assertTrue(
                archive_path.is_file()
            )
            self.assertGreater(
                result["bytes"],
                0,
            )
            self.assertEqual(
                4,
                result["file_count"],
            )

            with ZipFile(
                archive_path,
                "r",
            ) as archive:
                names = set(
                    archive.namelist()
                )

            self.assertIn(
                "INDEX.HTM",
                names,
            )
            self.assertIn(
                "Volume/PAGE.HTM",
                names,
            )
            self.assertNotIn(
                "_renault/modern-index.json",
                names,
            )
            self.assertNotIn(
                "Volume/DOC.PDF",
                names,
            )

    def test_reports_progress_and_stays_deterministic(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            package = root / "_renault"
            package.mkdir()

            (root / "INDEX.HTM").write_text(
                "<html>same</html>",
                encoding="latin-1",
            )

            messages: list[str] = []
            first = build_fast_pack(
                root,
                package,
                progress=messages.append,
            )
            second = build_fast_pack(
                root,
                package,
                progress=messages.append,
            )

            self.assertEqual(
                first["sha256"],
                second["sha256"],
            )
            self.assertTrue(
                any(
                    message.startswith(
                        "Fast Pack: сканую dataset"
                    )
                    for message in messages
                )
            )
            self.assertTrue(
                any(
                    message.startswith(
                        "Fast Pack: пакую "
                    )
                    for message in messages
                )
            )
            self.assertTrue(
                any(
                    message.startswith(
                        "Fast Pack: готово"
                    )
                    for message in messages
                )
            )

    def test_rebuild_replaces_old_fast_pack(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            package = root / "_renault"
            package.mkdir()

            (root / "INDEX.HTM").write_text(
                "one",
                encoding="latin-1",
            )

            first = build_fast_pack(
                root,
                package,
            )
            first_path = root / first["path"]

            (root / "INDEX.HTM").write_text(
                "two",
                encoding="latin-1",
            )

            second = build_fast_pack(
                root,
                package,
            )
            second_path = root / second["path"]

            self.assertNotEqual(
                first["sha256"],
                second["sha256"],
            )
            self.assertFalse(
                first_path.exists()
            )
            self.assertTrue(
                second_path.exists()
            )


if __name__ == "__main__":
    unittest.main()
