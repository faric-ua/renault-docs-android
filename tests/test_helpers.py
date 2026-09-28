import json
import tempfile
import unittest
from pathlib import Path

from tools.prepare_from_config import load_config
from web.serve import find_entrypoints


class HelperTests(unittest.TestCase):
    def test_load_config_requires_source_and_build_paths(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "config.json"
            path.write_text(
                json.dumps({
                    "source_root": "/source",
                    "build_root": "/build",
                }),
                encoding="utf-8",
            )
            config = load_config(path)
            self.assertEqual("/source", config["source_root"])
            self.assertEqual("/build", config["build_root"])

    def test_find_entrypoints_finds_nested_index(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            nested = root / "version" / "RUS"
            nested.mkdir(parents=True)
            entry = nested / "INDEX.HTM"
            entry.write_text("<html></html>", encoding="utf-8")

            entries = find_entrypoints(root)
            self.assertIn(entry, entries)


if __name__ == "__main__":
    unittest.main()
