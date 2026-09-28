import unittest

from tools.export_runtime_section_ir import build_section_payload


class RuntimeSectionIrExportTests(unittest.TestCase):
    def test_builds_small_section_payload(self):
        runtime_tree = {
            "schema_version": 2,
            "compiler_phase": "section-ir-v2",
        }
        volume = {
            "id": "v1",
            "title": "NT8183A",
            "document_code": "NT8183A",
            "date": "2001-01-22",
            "kind": "technical-documentation",
            "source_folder": "Laguna",
            "classic": {"large": "not-exported"},
        }
        section = {
            "code": "101",
            "controls": [{"id": "c1"}],
            "actions": [{"id": "a1"}],
            "documents": [{"id": "d1"}],
        }

        payload = build_section_payload(
            runtime_tree=runtime_tree,
            volume=volume,
            section=section,
        )

        self.assertEqual(
            2,
            payload["runtime_schema_version"],
        )
        self.assertEqual(
            "section-ir-v2",
            payload["compiler_phase"],
        )
        self.assertEqual(
            "NT8183A",
            payload["volume"]["document_code"],
        )
        self.assertNotIn(
            "classic",
            payload["volume"],
        )
        self.assertIs(
            section,
            payload["section"],
        )


if __name__ == "__main__":
    unittest.main()
