from pathlib import Path
import unittest


class BrowserLauncherContractTests(unittest.TestCase):
    def test_root_launcher_prepares_browser_environment(self):
        repo = Path(__file__).resolve().parents[1]
        text = (repo / "browser.sh").read_text(encoding="utf-8")

        self.assertIn("tools/package_dataset.py", text)
        self.assertIn("tools/install_pdfjs.py --check", text)
        self.assertIn("tools/install_pdfjs.py", text)
        self.assertIn("tools/termux/reno-docs.sh", text)
        self.assertIn("config/current-device.json", text)


if __name__ == "__main__":
    unittest.main()
