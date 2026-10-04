from pathlib import Path
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "tools/termux/reno-quarantine-legacy-android.sh"

class LegacyQuarantineContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.text = SCRIPT.read_text(encoding="utf-8")

    def test_quarantine_is_move_only_and_reversible(self):
        self.assertIn('QUARANTINE="${RENAULT_LEGACY_QUARANTINE:-$ROOT/legacy-quarantine}"', self.text)
        self.assertIn('mv -- "$src" "$dst"', self.text)
        self.assertIn("RESTORE", self.text)
        self.assertIn("manifest.tsv", self.text)
        self.assertNotIn("rm -", self.text)
        self.assertNotIn("rmdir", self.text)
        self.assertNotIn("find -delete", self.text)

    def test_move_requires_explicit_confirmation(self):
        self.assertIn('Для підтвердження введи MOVE', self.text)
        self.assertIn('[ "$confirm" = "MOVE" ]', self.text)
        self.assertIn('Для підтвердження введи RESTORE', self.text)

    def test_quarantine_verifies_file_count_and_byte_size(self):
        self.assertIn("before_files", self.text)
        self.assertIn("before_bytes", self.text)
        self.assertIn("after_files", self.text)
        self.assertIn("after_bytes", self.text)
        self.assertIn("VERIFY FAIL", self.text)

    def test_script_has_valid_bash_syntax(self):
        subprocess.run(
            ["bash", "-n", str(SCRIPT)],
            check=True,
            capture_output=True,
            text=True,
        )

if __name__ == "__main__":
    unittest.main()
