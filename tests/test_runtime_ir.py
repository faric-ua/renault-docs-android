import json
import tempfile
import unittest
from pathlib import Path

from core.runtime_ir import (
    RUNTIME_TREE_FILENAME,
    build_runtime_tree,
    write_runtime_tree,
)


class RuntimeIrTests(unittest.TestCase):
    def _make_volume(self, root: Path) -> tuple[Path, dict]:
        volume = root / "Laguna X74 NT8183A 2001_01_22"
        htm = volume / "RUS" / "HTM"
        menu = htm / "MENU"
        commun = volume / "COMMUN" / "HTM"
        document = volume / "RUS" / "DOCUMENT"

        menu.mkdir(parents=True)
        commun.mkdir(parents=True)
        document.mkdir(parents=True)

        (volume / "INDEX.HTM").write_text(
            '<frameset><frame src="RUS/HTM/ENTREE.HTM"></frameset>',
            encoding="utf-8",
        )

        (htm / "ENTREE.HTM").write_text(
            """
            <html>
              <head><title>LAGUNA II</title></head>
              <frameset cols="216,747">
                <frameset rows="82,510">
                  <frame name="titre" src="CTITRE.HTM">
                  <frame name="org" src="CODE.HTM">
                </frameset>
                <frameset rows="150,543">
                  <frameset rows="50%,50%">
                    <frame name="menu" src="MENU.HTM">
                    <frame name="nav" src="../../COMMUN/HTM/BLANK.HTM">
                  </frameset>
                  <frame name="doc" src="../DOCUMENT/CLAUSLEG.PDF">
                </frameset>
              </frameset>
            </html>
            """,
            encoding="utf-8",
        )

        (htm / "CTITRE.HTM").write_text(
            "<html>Laguna 2</html>",
            encoding="utf-8",
        )
        (htm / "MENU.HTM").write_text(
            "<html></html>",
            encoding="utf-8",
        )
        (commun / "BLANK.HTM").write_text(
            "<html></html>",
            encoding="utf-8",
        )
        (document / "CLAUSLEG.PDF").write_bytes(
            b"%PDF-1.4\n"
        )

        (htm / "CODE.HTM").write_text(
            """
            <html><body>
              <a href="MENU/101.HTM">101 ПРИКУРИВАТЕЛЬ</a>
              <a href="MENU/103.HTM">103 ГЕНЕРАТОР</a>
              <a href="MENU/105.HTM">105 СИГНАЛ</a>
              <a href="MENU/107.HTM">107 АККУМУЛЯТОР</a>
            </body></html>
            """,
            encoding="utf-8",
        )

        for code in ("101", "103", "105", "107"):
            (menu / f"{code}.HTM").write_text(
                f"<html><title>CMP {code}</title></html>",
                encoding="utf-8",
            )

        volume_data = {
            "id": "laguna-x74-nt8183a-2001-01-22",
            "title": "NT8183A · 2001-01-22",
            "document_code": "NT8183A",
            "date": "2001-01-22",
            "kind": "technical-documentation",
            "source_folder": volume.name,
            "entrypoint": f"{volume.name}/INDEX.HTM",
        }
        return volume, volume_data

    def test_compiles_classic_topology_and_modern_navigation_ir(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            _, volume = self._make_volume(root)

            data = build_runtime_tree(
                output_root=root,
                volumes=[volume],
            )

            self.assertEqual(2, data["schema_version"])
            self.assertEqual("renault-runtime-ir", data["format"])
            self.assertTrue(data["classic_preserved"])
            self.assertEqual(
                "normalized-json",
                data["modern_data_contract"],
            )

            compiled = data["volumes"][0]
            named = compiled["classic"]["named_frames"]

            self.assertEqual(
                {"titre", "org", "menu", "nav"},
                {"titre", "org", "menu", "nav"}.intersection(named),
            )
            self.assertIn("doc", named)
            self.assertEqual(
                "Laguna X74 NT8183A 2001_01_22/RUS/HTM/CODE.HTM",
                named["org"][0]["target"],
            )

            sections = compiled["modern"]["sections"]
            self.assertEqual(
                ["101", "103", "105", "107"],
                [item["code"] for item in sections],
            )
            self.assertEqual(
                "ПРИКУРИВАТЕЛЬ",
                sections[0]["title"],
            )
            self.assertEqual(
                "section-ir-v2",
                sections[0]["compile_state"],
            )
            self.assertIn(
                "section-static-controls",
                compiled["compiler"]["completed"],
            )
            self.assertIn(
                "native-renderer-parity",
                compiled["compiler"]["pending"],
            )

    def test_writes_single_runtime_tree_json(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            package = root / "_renault"
            package.mkdir()
            _, volume = self._make_volume(root)

            path = write_runtime_tree(
                output_root=root,
                volumes=[volume],
                package_root=package,
            )

            self.assertEqual(
                RUNTIME_TREE_FILENAME,
                path.name,
            )

            data = json.loads(
                path.read_text(encoding="utf-8")
            )
            self.assertEqual(1, data["volume_count"])
            self.assertEqual(4, data["section_count"])

            index_path = package / "runtime-ir-index.json"
            self.assertTrue(index_path.is_file())

            index = json.loads(
                index_path.read_text(encoding="utf-8")
            )
            self.assertEqual(
                "sharded-section-json",
                index["runtime_data_contract"],
            )

            volume_index = index["volumes"][0]
            section_path = root / volume_index["sections"]["101"]
            self.assertTrue(section_path.is_file())

            payload = json.loads(
                section_path.read_text(encoding="utf-8")
            )
            self.assertEqual(
                "101",
                payload["section"]["code"],
            )
            self.assertEqual(
                "section-ir-v2",
                payload["section"]["compile_state"],
            )


if __name__ == "__main__":
    unittest.main()
