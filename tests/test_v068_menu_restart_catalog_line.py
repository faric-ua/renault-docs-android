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


class LastImportCompactTests(unittest.TestCase):
    """TODO №36, варіант А (faric 2026-10-18 21:40)."""

    def setUp(self):
        self.activity = CATALOG.read_text(encoding="utf-8")
        self.store = (CATALOG.parent / "CatalogImportRunStore.kt").read_text(encoding="utf-8")

    def test_store_remembers_card_was_seen_and_resets(self):
        self.assertIn('KEY_TERMINAL_SEEN = "terminal_seen"', self.store)
        mark = self.store[self.store.index("fun markTerminalSeen()"):self.store.index("fun isTerminalSeen()")]
        self.assertIn("CatalogImportPhase.COMPLETE", mark)  # помилки не згортаються
        # Новий імпорт і × чистять усе (prefs.clear) — разом з позначкою.
        self.assertIn(".clear()", self.store[self.store.index("fun begin("):self.store.index("fun update(")])
        self.assertIn(".clear()", self.store[self.store.index("fun clearTerminal()"):])

    def test_activity_compacts_only_on_next_opening(self):
        a = self.activity
        on_create = a[a.index("override fun onCreate("):a.index("override fun onSaveInstanceState(")]
        self.assertIn("importStore.isTerminalSeen()", on_create)
        self.assertIn("STATE_COMPACT_LAST_IMPORT", on_create)
        refresh = a[a.index("private fun refreshImportState()"):a.index("private fun closeFinishedImport(")]
        running = refresh[:refresh.index("!state.isTerminal")]
        self.assertIn("compactLastImport =\n                false", running)
        self.assertIn("LastImportLine.text(", refresh)
        self.assertLess(refresh.index("LastImportLine.text("), refresh.index("importStore.markTerminalSeen()"))
        self.assertLess(refresh.index("importStore.markTerminalSeen()"), refresh.index("operationStatus.showTerminal("))
        close = a[a.index("private fun closeFinishedImport("):]
        self.assertIn("importStore\n            .clearTerminal()", close)
        self.assertIn("lastImportRow.visibility", close)

    def test_formatter_and_jvm_test_exist(self):
        line = (CATALOG.parent / "LastImportLine.kt").read_text(encoding="utf-8")
        self.assertIn('"Останній імпорт: ', line)
        self.assertTrue((REPO / "android/app/src/test/java/com/saney/renaultdocs/LastImportLineTest.kt").exists())

    def test_version_and_guide(self):
        gradle = (REPO / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertGreaterEqual(int(__import__("re").search(r"versionCode = (\d+)", gradle).group(1)), 178)
        self.assertTrue((REPO / "android/app/src/main/assets/whats_new.txt").read_text(encoding="utf-8").__contains__("0.6.8\n"))
        for path in ("docs/ua/USER_GUIDE.md", "android/app/src/main/assets/guide/index.html"):
            text = (REPO / path).read_text(encoding="utf-8")
            self.assertRegex(text, r"v0\.6\.\d+")
            self.assertIn("Останній імпорт", text)


if __name__ == "__main__":
    unittest.main()
