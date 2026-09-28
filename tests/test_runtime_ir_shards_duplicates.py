import json
import tempfile
import unittest
from pathlib import Path

from core.runtime_ir_shards import write_runtime_ir_shards


class RuntimeIrShardDuplicateCodeTests(unittest.TestCase):
    def test_keeps_duplicate_display_codes_as_distinct_section_entries(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            package = root / "_renault"
            package.mkdir()

            runtime_tree = {
                "schema_version": 2,
                "compiler_phase": "section-ir-v2",
                "volumes": [
                    {
                        "id": "v1",
                        "title": "V1",
                        "classic": {
                            "entrypoint": "V/INDEX.HTM",
                        },
                        "modern": {
                            "sections": [
                                {
                                    "code": "101",
                                    "title": "A",
                                    "legacy_entrypoint": "V/A/101.HTM",
                                    "compile_state": "section-ir-v2",
                                },
                                {
                                    "code": "101",
                                    "title": "B",
                                    "legacy_entrypoint": "V/B/101.HTM",
                                    "compile_state": "section-ir-v2",
                                },
                                {
                                    "code": "R325",
                                    "title": "Relay",
                                    "legacy_entrypoint": "V/R325.HTM",
                                    "compile_state": "section-ir-v2",
                                },
                            ],
                        },
                    },
                ],
            }

            index_path = write_runtime_ir_shards(
                runtime_tree=runtime_tree,
                package_root=package,
            )

            index = json.loads(
                index_path.read_text(encoding="utf-8")
            )
            volume = index["volumes"][0]

            self.assertEqual(3, volume["section_count"])
            self.assertEqual(
                ["101", "101", "R325"],
                [
                    item["code"]
                    for item in volume["section_entries"]
                ],
            )
            self.assertEqual(
                [
                    "V/A/101.HTM",
                    "V/B/101.HTM",
                    "V/R325.HTM",
                ],
                [
                    item["entrypoint"]
                    for item in volume["section_entries"]
                ],
            )

            paths = [
                item["path"]
                for item in volume["section_entries"]
            ]
            self.assertEqual(3, len(set(paths)))

            # Compatibility map still exists, but duplicate resolution is no
            # longer expected to rely on it.
            self.assertIn("101", volume["sections"])
            self.assertIn("R325", volume["sections"])


if __name__ == "__main__":
    unittest.main()
