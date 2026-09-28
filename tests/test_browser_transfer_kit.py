from __future__ import annotations

import importlib.util
import tempfile
import unittest
import zipfile
from pathlib import Path


class BrowserTransferKitTests(unittest.TestCase):
    def _module(self):
        repo = Path(__file__).resolve().parents[1]
        path = repo / "tools" / "build_browser_transfer_kit.py"
        spec = importlib.util.spec_from_file_location("browser_transfer", path)
        assert spec and spec.loader
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        return module

    def test_runtime_file_set_excludes_android_git_and_pdfjs_vendor(self):
        module = self._module()
        rels = [p.relative_to(module.REPO_ROOT).as_posix() for p in module.runtime_files()]

        self.assertIn("browser.sh", rels)
        self.assertIn("web/serve.py", rels)
        self.assertIn("tools/install_pdfjs.py", rels)
        self.assertIn("tools/configure_dataset_from_manifest.py", rels)
        self.assertFalse(any(path.startswith("android/") for path in rels))
        self.assertFalse(any("/vendor/" in path for path in rels))
        self.assertFalse(any("/.git/" in path for path in rels))

    def test_generated_zip_has_install_readme_and_application_payload(self):
        module = self._module()

        with tempfile.TemporaryDirectory() as tmp:
            target = Path(tmp) / "kit.zip"
            module.write_zip(target)

            self.assertTrue(target.is_file())
            self.assertTrue(target.with_suffix(".zip.sha256").is_file())

            with zipfile.ZipFile(target) as zf:
                names = set(zf.namelist())

            self.assertIn("Renault-Browser-Transfer-Kit/install.sh", names)
            self.assertIn("Renault-Browser-Transfer-Kit/README_UA.txt", names)
            self.assertIn("Renault-Browser-Transfer-Kit/application/browser.sh", names)
            self.assertIn(
                "Renault-Browser-Transfer-Kit/application/tools/configure_dataset_from_manifest.py",
                names,
            )


if __name__ == "__main__":
    unittest.main()
