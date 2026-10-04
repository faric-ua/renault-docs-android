from pathlib import Path
import subprocess
import unittest


class LegacyAndroidAuditTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.repo = Path(__file__).resolve().parents[1]
        cls.script = cls.repo / "tools/termux/reno-audit-legacy-android.sh"
        cls.text = cls.script.read_text(encoding="utf-8")
        cls.menu = (cls.repo / "menu.sh").read_text(encoding="utf-8")

    def test_menu_exposes_read_only_audit(self):
        self.assertIn("21 — Аудит legacy *_android (read-only)", self.menu)
        self.assertIn('bash "$REPO/tools/termux/reno-audit-legacy-android.sh"', self.menu)

    def test_audit_scans_active_and_quarantined_android_folders(self):
        self.assertIn('"$ROOT"/*_android', self.text)
        self.assertIn('"$ROOT/legacy-quarantine"/*_android', self.text)
        self.assertIn("KEEP:", self.text)
        self.assertIn("LEGACY:", self.text)
        self.assertIn("ARCHIVED:", self.text)
        self.assertIn("SAFE TO REMOVE:", self.text)

    def test_audit_reports_provenance_signals(self):
        self.assertIn("renault-dataset.json", self.text)
        self.assertIn(".nomedia", self.text)
        self.assertIn("build_root", self.text)
        self.assertIn("git -C", self.text)
        self.assertIn("Згадки в repo", self.text)

    def test_audit_is_conservative_about_deletion(self):
        self.assertIn("LEGACY ≠ SAFE TO REMOVE", self.text)
        self.assertIn("app-private/SAF reference", self.text)
        destructive_tokens = (
            "rm ",
            "rm\t",
            "rmdir ",
            "mv ",
            "find -delete",
            "unlink ",
            "trash ",
        )
        for token in destructive_tokens:
            with self.subTest(token=token):
                self.assertNotIn(token, self.text)

    def test_script_has_valid_bash_syntax(self):
        subprocess.run(
            ["bash", "-n", str(self.script)],
            check=True,
            capture_output=True,
            text=True,
        )


if __name__ == "__main__":
    unittest.main()
