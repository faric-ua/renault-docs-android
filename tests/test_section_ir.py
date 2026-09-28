import tempfile
import unittest
from pathlib import Path

from core.section_ir import SectionIrCompiler


class SectionIrCompilerTests(unittest.TestCase):
    def _fixture(self, root: Path) -> tuple[dict, dict]:
        volume_root = root / "Laguna X74 NT8183A 2001_01_22"
        menu = volume_root / "RUS" / "HTM" / "MENU"
        sch = volume_root / "RUS" / "HTM" / "SCH"
        nm = volume_root / "RUS" / "HTM" / "NM"
        lien = nm / "LIENNM"
        pc = volume_root / "COMMUN" / "HTM" / "PC"
        pdf_sch = volume_root / "COMMUN" / "PDF" / "SCH"
        pdf_nm = volume_root / "COMMUN" / "PDF" / "NM"
        pdf_pc = volume_root / "COMMUN" / "PDF" / "PC"
        document = volume_root / "RUS" / "DOCUMENT"
        blank = volume_root / "COMMUN" / "HTM"

        for path in (
            menu,
            sch,
            lien,
            pc,
            pdf_sch,
            pdf_nm,
            pdf_pc,
            document,
            blank,
        ):
            path.mkdir(parents=True, exist_ok=True)

        (blank / "BLANK.HTM").write_text(
            "<html><body></body></html>",
            encoding="utf-8",
        )
        (volume_root / "RUS" / "HTM" / "ERREUR.HTM").write_text(
            "<html><body>choose</body></html>",
            encoding="utf-8",
        )

        (menu / "101.HTM").write_text(
            """
            <html><body
              onload="parent.nav.location='../../../COMMUN/HTM/BLANK.HTM';
                      parent.doc.location='../../../COMMUN/HTM/BLANK.HTM'">
              <a target="nav" href="../SCH/101.HTM">
                <img src="../../../COMMUN/IMAGES/SITE/BOUTONS/SCH.GIF">
              </a>
              <a target="nav" href="../NM/101.HTM">
                <img src="../../../COMMUN/IMAGES/SITE/BOUTONS/NM.GIF">
              </a>
              <a target="nav" href="../../../COMMUN/HTM/PC/101.HTM">
                <img src="../../../COMMUN/IMAGES/SITE/BOUTONS/PC.GIF">
              </a>
              <a target="doc" href="../../DOCUMENT/AIDE.PDF">
                <img src="../../../COMMUN/IMAGES/SITE/BOUTONS/AIDE.GIF">
              </a>
            </body></html>
            """,
            encoding="utf-8",
        )

        (sch / "101.HTM").write_text(
            """
            <html><body onload="parent.doc.location='../../../COMMUN/HTM/BLANK.HTM'">
              <select name="listeCritere"
                onchange="afficherSchema(this, parent.doc)">
                <option value="../ERREUR.HTM">ВЫБЕРИТЕ СХЕМУ</option>
                <option value="../../../COMMUN/PDF/SCH/S007.pdf#zoom=1,2,3,4">
                  АВТОМОБИЛИ ВСЕХ ТИПОВ
                </option>
              </select>
            </body></html>
            """,
            encoding="utf-8",
        )

        (nm / "101.HTM").write_text(
            """
            <html><body>
              <select name="listeCritere"
                onchange="afficherNomenclature(this, parent.doc)">
                <option value="../ERREUR.HTM">ВЫБЕРИТЕ ЭЛЕМЕНТ</option>
                <option value="LIENNM/101_1.HTM">DG/E2</option>
              </select>
            </body></html>
            """,
            encoding="utf-8",
        )

        (pc / "101.HTM").write_text(
            """
            <html><body>
              <a target="doc" href="../../PDF/PC/S7.pdf">
                <img src="../../IMAGES/VIGNETTE/VS7.gif">
              </a>
            </body></html>
            """,
            encoding="utf-8",
        )

        (lien / "101_1.HTM").write_text(
            """
            <html>
              <frameset rows="60%,40%">
                <frame name="dessin"
                  src="../../../../COMMUN/PDF/NM/101_1.PDF">
                <frame name="alveoles"
                  src="T_101_1.HTM">
              </frameset>
            </html>
            """,
            encoding="utf-8",
        )

        (lien / "T_101_1.HTM").write_text(
            """
            <html><body>
              <h4>101<br>ПРИКУРИВАТЕЛЬ<br>DG/E2</h4>
              <table>
                <tr><th>PIN</th><th>CODE</th></tr>
                <tr><td>1</td><td>LPG</td></tr>
              </table>
            </body></html>
            """,
            encoding="utf-8",
        )

        for path in (
            pdf_sch / "S007.pdf",
            pdf_nm / "101_1.PDF",
            pdf_pc / "S7.pdf",
            document / "AIDE.PDF",
        ):
            path.write_bytes(b"%PDF-1.4\n")

        volume = {
            "id": "laguna-x74-nt8183a-2001-01-22",
            "document_code": "NT8183A",
            "source_folder": volume_root.name,
        }
        section = {
            "code": "101",
            "title": "ПРИКУРИВАТЕЛЬ",
            "entrypoint":
                f"{volume_root.name}/RUS/HTM/MENU/101.HTM",
        }
        return volume, section

    def test_compiles_menu_selects_routes_and_composite_document(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            volume, section = self._fixture(root)

            result = SectionIrCompiler(
                output_root=root,
                volume=volume,
            ).compile_section(section)

            self.assertEqual(
                "section-ir-v2",
                result["compile_state"],
            )

            panel_kinds = {
                item["kind"]
                for item in result["panels"]
            }
            self.assertTrue(
                {"menu", "schematic", "nomenclature", "pc"}
                .issubset(panel_kinds)
            )

            selects = [
                item
                for item in result["controls"]
                if item["type"] == "select"
            ]
            self.assertEqual(2, len(selects))

            schematic = next(
                item
                for item in selects
                if item["panel_id"] == "schematic"
            )
            active = [
                option
                for option in schematic["options"]
                if option.get("enabled")
            ]
            self.assertEqual(1, len(active))
            self.assertEqual(
                "route",
                active[0]["kind"],
            )

            pdf_paths = {
                item["path"]
                for item in result["documents"]
                if item["type"] == "pdf"
            }
            self.assertTrue(
                any(path.endswith("/COMMUN/PDF/SCH/S007.pdf") for path in pdf_paths)
            )
            self.assertTrue(
                any(path.endswith("/COMMUN/PDF/PC/S7.pdf") for path in pdf_paths)
            )

            composite = next(
                item
                for item in result["documents"]
                if item["type"] == "composite-document"
            )
            self.assertEqual(
                {"rows": "60%,40%"},
                composite["layout"],
            )
            self.assertEqual(
                {"dessin", "alveoles"},
                {part["role"] for part in composite["parts"]},
            )

            details = [
                item
                for item in result["documents"]
                if item["type"] == "structured-html"
                and item["path"].endswith("T_101_1.HTM")
            ]
            self.assertEqual(1, len(details))
            self.assertTrue(details[0]["tables"])

            routes = {
                item.get("route_type")
                for item in result["actions"]
            }
            self.assertIn("open-panel", routes)
            self.assertIn("open-document", routes)


if __name__ == "__main__":
    unittest.main()
