from pathlib import Path
import tempfile
import unittest

from core.section_ir import SectionIrCompiler


class V0519TableSemanticsWidthsContractTests(unittest.TestCase):
    def test_pin_table_is_detected_by_structure_without_t_prefix(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            compiler = SectionIrCompiler(
                output_root=root,
                volume={"source_folder": "volume"},
            )

            tables = [
                {
                    "rows": [
                        [
                            {"header": False, "text_parts": ["1"]},
                            {"header": False, "text_parts": ["0.6"]},
                            {"header": False, "text_parts": ["2A"]},
                            {
                                "header": False,
                                "text_parts": [
                                    "УПР. - СИГН. ЛАМПЫ ЗАРЯДКИ АККУМУЛ. БАТАРЕИ"
                                ],
                            },
                        ]
                    ]
                }
            ]

            compiled = compiler._structured_tables(
                tables,
                source_path=root / "103_5.HTM",
            )

            rows = compiled[0]["rows"]
            self.assertEqual(
                ["№", "мм²", "Код", "Опис"],
                [cell["text"] for cell in rows[0]],
            )
            self.assertTrue(
                all(cell["header"] for cell in rows[0])
            )
            self.assertEqual(
                ["1", "0.6", "2A"],
                [cell["text"] for cell in rows[1][:3]],
            )

    def test_legacy_visual_header_before_body_is_replaced(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            compiler = SectionIrCompiler(
                output_root=root,
                volume={"source_folder": "volume"},
            )

            tables = [
                {
                    "rows": [
                        [
                            {"header": False, "text_parts": ["N°"]},
                            {"header": False, "text_parts": ["mm²"]},
                            {"header": False, "text_parts": []},
                            {"header": False, "text_parts": ["... → ..."]},
                        ],
                        [
                            {"header": False, "text_parts": ["2"]},
                            {"header": False, "text_parts": ["1.4"]},
                            {"header": False, "text_parts": ["3CV"]},
                            {
                                "header": False,
                                "text_parts": ["УПР. - КАТУШКИ ЗАЖИГ."],
                            },
                        ],
                    ]
                }
            ]

            compiled = compiler._structured_tables(
                tables,
                source_path=root / "108_3.HTM",
            )

            rows = compiled[0]["rows"]
            self.assertEqual(2, len(rows))
            self.assertEqual(
                ["№", "мм²", "Код", "Опис"],
                [cell["text"] for cell in rows[0]],
            )
            self.assertEqual("2", rows[1][0]["text"])

    def test_unrelated_four_column_table_is_unchanged(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            compiler = SectionIrCompiler(
                output_root=root,
                volume={"source_folder": "volume"},
            )

            tables = [
                {
                    "rows": [
                        [
                            {"header": False, "text_parts": ["A"]},
                            {"header": False, "text_parts": ["B"]},
                            {"header": False, "text_parts": ["C"]},
                            {"header": False, "text_parts": ["D"]},
                        ]
                    ]
                }
            ]

            compiled = compiler._structured_tables(
                tables,
                source_path=root / "GENERAL.HTM",
            )

            self.assertEqual(1, len(compiled[0]["rows"]))
            self.assertFalse(
                any(
                    cell["header"]
                    for cell in compiled[0]["rows"][0]
                )
            )

    def test_layout_uses_headers_and_content_for_compact_columns(self):
        repo = Path(__file__).resolve().parents[1]

        layout = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeTableLayout.kt"
        ).read_text(encoding="utf-8")
        native = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeSectionActivity.kt"
        ).read_text(encoding="utf-8")
        exporter = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeTablePdfExporter.kt"
        ).read_text(encoding="utf-8")
        gradle = (
            repo
            / "android/app/build.gradle.kts"
        ).read_text(encoding="utf-8")

        self.assertIn("maxColumnLength", layout)
        self.assertNotIn("maxBodyLength", layout)
        self.assertIn("0.27f", layout)
        self.assertIn("maxTotal = 0.34f", layout)

        self.assertIn("column <\n                columnCount - 1", native)
        self.assertIn("setSingleLine(true)", native)

        self.assertIn("column <\n                            columnCount - 1", exporter)
        self.assertIn("listOf(value)", exporter)


if __name__ == "__main__":
    unittest.main()
