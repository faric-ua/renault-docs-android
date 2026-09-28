import unittest

from core.runtime_ir_coverage import build_runtime_ir_coverage


class RuntimeIrCoverageTests(unittest.TestCase):
    def test_reports_menu_and_unsupported_variants_across_volumes(self):
        tree = {
            "schema_version": 2,
            "compiler_phase": "section-ir-v2",
            "section_count": 2,
            "volumes": [
                {
                    "document_code": "NT8183A",
                    "date": "2001-01-22",
                    "modern": {
                        "sections": [
                            {
                                "code": "101",
                                "compile_state": "section-ir-v2",
                                "panels": [
                                    {"kind": "menu"},
                                ],
                                "controls": [
                                    {
                                        "type": "action-bar",
                                        "items": [
                                            {"label": "SCH"},
                                            {"label": "NM"},
                                        ],
                                    }
                                ],
                                "actions": [
                                    {
                                        "type": "route",
                                        "route_type": "open-panel",
                                    }
                                ],
                                "documents": [
                                    {"type": "pdf"},
                                ],
                            }
                        ]
                    },
                },
                {
                    "document_code": "NT8328A",
                    "date": "2006-05-09",
                    "modern": {
                        "sections": [
                            {
                                "code": "101",
                                "compile_state": "section-ir-v2",
                                "panels": [
                                    {"kind": "menu"},
                                ],
                                "controls": [
                                    {
                                        "type": "action-bar",
                                        "items": [
                                            {"label": "SCH"},
                                            {"label": "NEWER"},
                                        ],
                                    }
                                ],
                                "actions": [
                                    {
                                        "type": "legacy-javascript",
                                        "label": "NEWER",
                                        "script": "specialMode()",
                                    }
                                ],
                                "documents": [],
                                "warnings": [
                                    "example-warning",
                                ],
                            }
                        ]
                    },
                },
            ],
        }

        report = build_runtime_ir_coverage(tree)

        self.assertEqual(2, report["volume_count"])
        self.assertEqual(2, report["section_count"])
        self.assertEqual(
            2,
            report["menu_labels"]["SCH"],
        )
        self.assertEqual(
            1,
            report["menu_labels"]["NEWER"],
        )
        self.assertEqual(
            1,
            report["unsupported_action_count"],
        )
        self.assertEqual(
            "NT8328A",
            report["unsupported_action_samples"][0]["document_code"],
        )
        self.assertEqual(1, report["warning_count"])


if __name__ == "__main__":
    unittest.main()
