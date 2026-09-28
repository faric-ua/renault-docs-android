import json
import tempfile
import unittest
from pathlib import Path

from core.modern_index import (
    MODERN_INDEX_FILENAME,
    build_modern_index,
    write_modern_index,
)


class ModernIndexTests(unittest.TestCase):
    def test_builds_native_volume_navigation_contract(self):
        index = build_modern_index(
            {
                "id": "renault-laguna-ii",
                "title": "Renault Laguna II 2001–2006",
                "manufacturer": "Renault",
                "model": "Laguna II",
                "platform": "X74",
                "years": {"from": 2001, "to": 2006},
            },
            [
                {
                    "id": "nt8236a",
                    "title": "NT8236A · 2002-11-18",
                    "document_code": "NT8236A",
                    "date": "2002-11-18",
                    "kind": "technical-documentation",
                    "source_folder": "Laguna X74 NT8236A 2002_11_18",
                    "entrypoint": "Laguna X74 NT8236A 2002_11_18/INDEX.HTM",
                }
            ],
        )

        self.assertEqual(1, index["schema_version"])
        self.assertEqual("volumes", index["navigation"]["level"])
        self.assertEqual(
            "NT8236A",
            index["navigation"]["volumes"][0]["document_code"],
        )

    def test_writes_modern_index_file(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            path = write_modern_index(
                {"id": "test", "title": "Test", "model": "Test"},
                [],
                root,
            )

            self.assertEqual(MODERN_INDEX_FILENAME, path.name)
            data = json.loads(path.read_text(encoding="utf-8"))
            self.assertEqual(1, data["schema_version"])


if __name__ == "__main__":
    unittest.main()
