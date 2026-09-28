from pathlib import Path
import tempfile
import unittest

from core.section_ir import SectionIrCompiler


class V0518PinTableSemanticsContractTests(unittest.TestCase):
    def test_headerless_legacy_pin_table_gets_semantic_header(self):
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

            self.assertEqual(1, len(compiled))
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

    def test_visual_legacy_header_is_replaced_not_duplicated(self):
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
                            {"header": False, "text_parts": ["№"]},
                            {"header": False, "text_parts": ["mm²"]},
                            {"header": False, "text_parts": []},
                            {"header": False, "text_parts": ["... → ..."]},
                        ],
                        [
                            {"header": False, "text_parts": ["1"]},
                            {"header": False, "text_parts": ["0.35"]},
                            {"header": False, "text_parts": ["LPG"]},
                            {"header": False, "text_parts": ["опис"]},
                        ],
                    ]
                }
            ]

            compiled = compiler._structured_tables(
                tables,
                source_path=root / "T_101_1.HTM",
            )

            rows = compiled[0]["rows"]
            self.assertEqual(2, len(rows))
            self.assertEqual(
                ["№", "мм²", "Код", "Опис"],
                [cell["text"] for cell in rows[0]],
            )
            self.assertEqual("1", rows[1][0]["text"])

    def test_non_pin_structured_table_is_not_given_synthetic_header(self):
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
                source_path=root / "OTHER.HTM",
            )
            self.assertEqual(1, len(compiled[0]["rows"]))
            self.assertFalse(
                any(
                    cell["header"]
                    for cell in compiled[0]["rows"][0]
                )
            )

    def test_android_compact_pin_columns_do_not_wrap(self):
        repo = Path(__file__).resolve().parents[1]

        native = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeSectionActivity.kt"
        ).read_text(encoding="utf-8")
        layout = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeTableLayout.kt"
        ).read_text(encoding="utf-8")
        exporter = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/NativeTablePdfExporter.kt"
        ).read_text(encoding="utf-8")
        gradle = (
            repo
            / "android/app/build.gradle.kts"
        ).read_text(encoding="utf-8")

        self.assertIn("columnCount - 1", native)
        self.assertIn("column <", native)
        self.assertIn("setSingleLine(true)", native)

        self.assertIn("maxColumnLength", layout)
        self.assertIn("maxTotal = 0.34f", layout)

        self.assertIn("columnCount - 1", exporter)
        self.assertIn("listOf(value)", exporter)



if __name__ == "__main__":
    unittest.main()
