"""TODO №34 (меню відкривається заново після оновлення) і №35 (рядок каталогу не губить «через акаунт Google»)."""

from pathlib import Path
import re
import subprocess
import tempfile
import unittest

REPO = Path(__file__).resolve().parents[1]
MENU = REPO / "menu.sh"
CATALOG = REPO / "android/app/src/main/java/com/saney/renaultdocs/DriveCatalogActivity.kt"


def _function(text: str, name: str) -> str:
    match = re.search(rf"^{name}\(\) \{{\n.*?^\}}\n", text, re.S | re.M)
    assert match, name
    return match.group(0)


class MenuRestartTests(unittest.TestCase):
    def setUp(self):
        self.text = MENU.read_text(encoding="utf-8")

    def test_update_and_candidate_remember_menu_before_and_restart(self):
        update = _function(self.text, "update_project")
        self.assertIn('menu_before="$(menu_fingerprint)"', update)
        # Відбиток — до git fetch, перезапуск — лише після успішного оновлення.
        self.assertLess(update.index("menu_fingerprint"), update.index("git fetch"))
        self.assertLess(update.index('echo "Готово."'), update.index("restart_menu_if_changed"))
        candidate = _function(self.text, "download_phone_candidate")
        self.assertLess(candidate.index("menu_fingerprint"), candidate.index("reno-candidate.sh"))
        self.assertIn('restart_menu_if_changed "$menu_before"', candidate)

    def _run(self, change: bool) -> str:
        helpers = _function(self.text, "menu_fingerprint") + _function(self.text, "restart_menu_if_changed")
        with tempfile.TemporaryDirectory() as tmp:
            menu = Path(tmp) / "menu.sh"
            menu.write_text('echo "OLD MENU"\n', encoding="utf-8")
            script = (
                f'REPO="{tmp}"\npause_menu() {{ :; }}\n{helpers}'
                'before="$(menu_fingerprint)"\n'
                + (f"printf 'echo \"NEW MENU\"\\n' > \"{menu}\"\n" if change else "")
                + 'restart_menu_if_changed "$before"\necho "STAYED"\n'
            )
            result = subprocess.run(["bash", "-c", script], capture_output=True, text=True, check=True)
            return result.stdout

    def test_changed_menu_reopens_new_menu(self):
        out = self._run(change=True)
        self.assertIn("Меню теж оновилось", out)
        self.assertIn("NEW MENU", out)
        self.assertNotIn("STAYED", out)

    def test_unchanged_menu_stays(self):
        out = self._run(change=False)
        self.assertNotIn("Меню теж оновилось", out)
        self.assertIn("STAYED", out)


class CatalogLineTests(unittest.TestCase):
    def test_selection_summary_uses_catalog_status(self):
        text = CATALOG.read_text(encoding="utf-8")
        body = text[text.index("private fun updateSelectionSummary()"):text.index("private fun startImport()")]
        self.assertIn("catalogStatus(", body)
        self.assertNotIn('"Каталог v"', body)
        # «Каталог v» лишився в одному місці — у catalogStatus.
        self.assertEqual(text.count('"Каталог v"'), 1)


if __name__ == "__main__":
    unittest.main()
