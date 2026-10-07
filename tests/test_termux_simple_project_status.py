from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "tools/termux/reno-project-status.sh"


class SimpleTermuxStatusContractTests(unittest.TestCase):
    def test_status_is_compact_and_actionable(self):
        text = SCRIPT.read_text(encoding="utf-8")

        self.assertIn('✓ МОЖНА ЗАВАНТАЖУВАТИ', text)
        self.assertIn('… ЩЕ НЕ ГОТОВО', text)
        self.assertIn('✗ ЩЕ НЕ ГОТОВО', text)
        self.assertIn('Натисни: 8', text)
        self.assertIn('натисни 5', text)
        self.assertIn('натисни: 7', text)

    def test_status_uses_colored_symbols(self):
        text = SCRIPT.read_text(encoding="utf-8")

        self.assertIn(r"\033[32m", text)
        self.assertIn(r"\033[33m", text)
        self.assertIn(r"\033[31m", text)
        self.assertIn('✓', text)
        self.assertIn('…', text)
        self.assertIn('✗', text)

    def test_status_checks_exact_commit_then_allows_safe_android_reuse(self):
        text = SCRIPT.read_text(encoding="utf-8")

        self.assertIn('headSha ==', text)
        self.assertIn('"$LOCAL_SHA"', text)
        self.assertIn('"tests.yml"', text)
        self.assertIn('"android-debug.yml"', text)
        self.assertIn('reno_current_commit_tests_ignored', text)
        self.assertIn('reno_find_compatible_android_run', text)
        self.assertIn('Android-код без змін', text)

    def test_default_view_hides_old_verbose_release_metadata(self):
        text = SCRIPT.read_text(encoding="utf-8")

        self.assertNotIn('Signer expected:', text)
        self.assertNotIn('Trigger:', text)
        self.assertNotIn('Coverage:', text)
        self.assertNotIn('Run ID:', text)


if __name__ == "__main__":
    unittest.main()
