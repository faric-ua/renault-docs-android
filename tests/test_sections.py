import json
import tempfile
import unittest
from pathlib import Path

from core.sections import (
    MODERN_SECTIONS_FILENAME,
    build_modern_sections_index,
    discover_volume_sections,
    write_modern_sections_index,
)


class ModernSectionsTests(unittest.TestCase):
    def test_discovers_sections_from_legacy_frames_and_rows(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            volume = root / "Laguna X74 NT8236A 2002_11_18"
            volume.mkdir()

            (volume / "INDEX.HTM").write_text(
                """
                <html>
                  <frameset cols="190,*">
                    <frame src="MENU.HTM">
                    <frame src="CONTENT.HTM">
                  </frameset>
                </html>
                """,
                encoding="utf-8",
            )

            (volume / "MENU.HTM").write_text(
                """
                <html><body><table>
                  <tr>
                    <td><a href="101.HTM">101</a></td>
                    <td>ПРИКУРИВАТЕЛЬ</td>
                  </tr>
                  <tr>
                    <td><a href="103.HTM">103</a></td>
                    <td>ГЕНЕРАТОР</td>
                  </tr>
                  <tr>
                    <td><a href="105.HTM">105</a></td>
                    <td>ОСН. ЭЛМАНГ. З</td>
                  </tr>
                  <tr>
                    <td><a href="107.HTM">107</a></td>
                    <td>АККУМУЛЯТОР</td>
                  </tr>
                </table></body></html>
                """,
                encoding="utf-8",
            )

            for code in (
                "101",
                "103",
                "105",
                "107",
            ):
                (volume / f"{code}.HTM").write_text(
                    f"<html>{code}</html>",
                    encoding="utf-8",
                )

            result = discover_volume_sections(
                output_root=root,
                volume={
                    "id": "nt8236a",
                    "title": "NT8236A · 2002-11-18",
                    "document_code": "NT8236A",
                    "date": "2002-11-18",
                    "source_folder": volume.name,
                    "entrypoint": f"{volume.name}/INDEX.HTM",
                },
            )

            self.assertEqual(
                "Laguna X74 NT8236A 2002_11_18/MENU.HTM",
                result["source_file"],
            )
            self.assertEqual(
                ["101", "103", "105", "107"],
                [
                    item["code"]
                    for item in result["sections"]
                ],
            )
            self.assertEqual(
                "ПРИКУРИВАТЕЛЬ",
                result["sections"][0]["title"],
            )
            self.assertEqual(
                f"{volume.name}/101.HTM",
                result["sections"][0]["entrypoint"],
            )

    def test_discovers_javascript_href_and_cp1251_menu(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            volume = root / "Volume"
            volume.mkdir()

            (volume / "INDEX.HTM").write_text(
                '<frame src="nav/menu.htm">',
                encoding="utf-8",
            )

            nav = volume / "nav"
            nav.mkdir()

            menu_html = """
            <html>
            <meta http-equiv="Content-Type" content="text/html; charset=windows-1251">
            <body>
              <a href="javascript:parent.main.location='../119.htm'">
                119 АБС
              </a>
              <a onclick="parent.main.location='../123.htm'" href="#">
                123 ЗАМКИ
              </a>
              <a href="../125.htm">125 АВАРИЙНАЯ СИГНАЛИЗАЦИЯ</a>
              <a href="../129.htm">129 ПЕРЕКЛ. ПРОГРАММ</a>
            </body></html>
            """
            (nav / "menu.htm").write_bytes(
                menu_html.encode("cp1251")
            )

            for code in (
                "119",
                "123",
                "125",
                "129",
            ):
                (volume / f"{code}.htm").write_text(
                    "<html></html>",
                    encoding="utf-8",
                )

            result = discover_volume_sections(
                output_root=root,
                volume={
                    "title": "Test",
                    "source_folder": volume.name,
                    "entrypoint": "Volume/INDEX.HTM",
                },
            )

            by_code = {
                item["code"]: item
                for item in result["sections"]
            }

            self.assertEqual(
                "АБС",
                by_code["119"]["title"],
            )
            self.assertEqual(
                "ЗАМКИ",
                by_code["123"]["title"],
            )
            self.assertTrue(
                by_code["129"]["entrypoint"]
                .endswith("/129.htm")
            )

    def test_preserves_full_classic_catalog_source_order_and_duplicate_codes(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            volume = root / "Volume"
            volume.mkdir()

            (volume / "INDEX.HTM").write_text(
                '<frame src="MENU.HTM">',
                encoding="utf-8",
            )

            entries = [
                ("101", "CIGAR LIGHTER", "101_a.HTM"),
                ("1405", "EXTRA NUMERIC", "1405.HTM"),
                ("R325", "RELAY", "R325.HTM"),
                ("MAH", "ALPHA GROUP", "MAH.HTM"),
                ("NT", "ALPHA SHORT", "NT.HTM"),
                ("NU", "ALPHA SHORT 2", "NU.HTM"),
                ("101", "CIGAR LIGHTER ALT", "101_b.HTM"),
            ]

            (volume / "MENU.HTM").write_text(
                "<html><body>" +
                "".join(
                    f'<a href="{target}">{code} {title}</a>'
                    for code, title, target in entries
                ) +
                '<a href="missing.HTM">R999 MISSING</a>' +
                "</body></html>",
                encoding="utf-8",
            )

            for _, _, target in entries:
                (volume / target).write_text(
                    "<html></html>",
                    encoding="utf-8",
                )

            result = discover_volume_sections(
                output_root=root,
                volume={
                    "title": "Test",
                    "source_folder": volume.name,
                    "entrypoint": "Volume/INDEX.HTM",
                },
            )

            self.assertEqual(
                [item[0] for item in entries],
                [
                    item["code"]
                    for item in result["sections"]
                ],
            )
            self.assertEqual(
                [item[2] for item in entries],
                [
                    Path(item["entrypoint"]).name
                    for item in result["sections"]
                ],
            )
            self.assertEqual(
                2,
                sum(
                    item["code"] == "101"
                    for item in result["sections"]
                ),
            )
            self.assertNotIn(
                "R999",
                [
                    item["code"]
                    for item in result["sections"]
                ],
            )


    def test_writes_sections_index(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            package = root / "_renault"
            package.mkdir()

            volume = root / "V"
            volume.mkdir()
            (volume / "INDEX.HTM").write_text(
                '<a href="101.htm">101 Test</a>'
                '<a href="103.htm">103 Generator</a>'
                '<a href="105.htm">105 Light</a>'
                '<a href="107.htm">107 Battery</a>',
                encoding="utf-8",
            )

            for code in (
                "101",
                "103",
                "105",
                "107",
            ):
                (volume / f"{code}.htm").write_text(
                    "",
                    encoding="utf-8",
                )

            volumes = [
                {
                    "id": "v",
                    "title": "V",
                    "entrypoint": "V/INDEX.HTM",
                    "source_folder": "V",
                }
            ]

            index = build_modern_sections_index(
                root,
                volumes,
            )
            self.assertEqual(
                2,
                index["schema_version"],
            )
            self.assertEqual(
                4,
                index["section_count"],
            )

            path = write_modern_sections_index(
                output_root=root,
                volumes=volumes,
                package_root=package,
            )

            self.assertEqual(
                MODERN_SECTIONS_FILENAME,
                path.name,
            )

            data = json.loads(
                path.read_text(
                    encoding="utf-8",
                )
            )
            self.assertEqual(
                "legacy-html-navigation",
                data["source"],
            )


if __name__ == "__main__":
    unittest.main()
