"""Release promotion checks; no network, publishing or repository mutations."""
import hashlib
import importlib.util
from pathlib import Path
import tempfile
import unittest
import zipfile

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location(
    "renault_release_publisher", ROOT / "scripts/publish_debug_release.py"
)
publisher = importlib.util.module_from_spec(spec)
spec.loader.exec_module(publisher)


class SignedReleasePromotionContract(unittest.TestCase):
    def test_checked_in_v0583_promotion_matches_release_evidence(self):
        data = publisher.validate_manifest(
            "docs/release-promotions/v0.5.83.json"
        )
        self.assertEqual(data["version"], "0.5.83")
        self.assertEqual(data["version_code"], 99)
        self.assertEqual(data["signed_run_number"], 146)
        self.assertEqual(data["signed_run_id"], 37853352879)
        self.assertEqual(data["tag"], "v0.5.83-debug")
        self.assertTrue(data["prerelease"])
        self.assertEqual(data["qa_status"], "core-phone-pass-extended-paused")

    def test_other_or_untracked_promotion_paths_rejected(self):
        with self.assertRaises(ValueError):
            publisher.validate_manifest(ROOT / "docs/v.0.5.83/RELEASE_META.json")
        with self.assertRaises(ValueError):
            publisher.validate_manifest("../../etc/passwd")

    def test_apk_checksum_verified_not_artifact_zip_digest(self):
        with tempfile.TemporaryDirectory() as temporary:
            folder = Path(temporary)
            apk = folder / "Renault-Docs-v0.5.83-debug.apk"
            with zipfile.ZipFile(apk, "w", compression=zipfile.ZIP_STORED) as z:
                z.writestr("AndroidManifest.xml", b"x" * 110000)
                z.writestr("classes.dex", b"classes")
            digest = hashlib.sha256(apk.read_bytes()).hexdigest()
            checksum = folder / (apk.name + ".sha256")
            checksum.write_text(f"{digest}  {apk.name}\n", encoding="utf-8")
            self.assertEqual(
                publisher.check_downloaded_apk(folder, "0.5.83")[2], digest
            )
            checksum.write_text("0" * 64 + f"  {apk.name}\n", encoding="utf-8")
            with self.assertRaises(ValueError):
                publisher.check_downloaded_apk(folder, "0.5.83")

    def test_workflow_only_on_approved_main_promotion_not_each_apk_build(self):
        workflow = (ROOT / ".github/workflows/publish-verified-apk.yml").read_text(
            encoding="utf-8"
        )
        self.assertIn('branches: [main]', workflow)
        self.assertIn('"docs/release-promotions/*.json"', workflow)
        self.assertIn("workflow_dispatch:", workflow)
        self.assertIn("contents: write", workflow)
        self.assertIn("actions: read", workflow)
        self.assertIn("cancel-in-progress: false", workflow)
        self.assertNotIn("pull_request:", workflow)
        source = (ROOT / "scripts/publish_debug_release.py").read_text(
            encoding="utf-8"
        )
        for guard in (
            'repo != "faric-ua/renault-docs-android"',
            'os.environ.get("GITHUB_REF") != "refs/heads/main"',
            '"Android Debug APK"',
            'run.get("conclusion") != "success"',
            'run.get("head_sha") != data["source_sha"]',
            'run.get("run_number") != data["signed_run_number"]',
            'and x.get("digest") == data["artifact_digest"]',
            '"sha256")',
            '"--prerelease"',
            '"--target"',
        ):
            self.assertIn(guard, source)


if __name__ == "__main__":
    unittest.main()
