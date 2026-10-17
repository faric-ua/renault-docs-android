"""Core Stable (#147, owner 2026-10-17 20:10): the stable release is the very same signed
artifact as the phone-tested -debug release — never a rebuild, never another key."""
import importlib.util
import json
from pathlib import Path
import shutil
import tempfile
import unittest
from unittest import mock

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location(
    "renault_release_publisher_cs", ROOT / "scripts/publish_debug_release.py"
)
publisher = importlib.util.module_from_spec(spec)
spec.loader.exec_module(publisher)

DEBUG = json.loads((ROOT / "docs/release-promotions/v0.5.153.json").read_text(encoding="utf-8"))


def core_request(**changes):
    data = {
        "schema_version": 1,
        "channel": "core-stable",
        **{k: DEBUG[k] for k in (
            "version", "version_code", "source_sha", "signed_run_id", "signed_run_number",
            "artifact_id", "artifact_name", "artifact_digest",
        )},
        "tag": "v0.5.153-core-stable",
        "prerelease": False,
        "qa_status": "core-stable-phone-pass",
        "phone_evidence": ["A26: встановлено поверх, томи на місці"],
        "known_limitations": ["Не з Google Play"],
    }
    data.update(changes)
    return data


class CoreStablePromotionContract(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.tmp)
        promo = self.tmp / "docs/release-promotions"
        promo.mkdir(parents=True)
        shutil.copy(ROOT / "docs/release-promotions/v0.5.153.json", promo)
        meta = self.tmp / "docs/v.0.5.153"
        meta.mkdir(parents=True)
        shutil.copy(ROOT / "docs/v.0.5.153/RELEASE_META.json", meta)
        self.promo = promo
        patcher = mock.patch.multiple(publisher, ROOT=self.tmp, PROMOTION_DIR=promo.resolve())
        patcher.start()
        self.addCleanup(patcher.stop)

    def write(self, data, name="v0.5.153-core-stable.json"):
        path = self.promo / name
        path.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
        return path

    def test_same_artifact_accepted(self):
        data = publisher.validate_manifest(self.write(core_request()))
        self.assertEqual(data["tag"], "v0.5.153-core-stable")
        self.assertFalse(data["prerelease"])

    def test_other_artifact_rejected(self):
        for key, value in (
            ("artifact_digest", "sha256:" + "0" * 64),
            ("signed_run_id", DEBUG["signed_run_id"] + 1),
            ("source_sha", "0" * 40),
        ):
            with self.subTest(key=key), self.assertRaises(ValueError):
                publisher.validate_manifest(self.write(core_request(**{key: value})))

    def test_needs_phone_pass_evidence_and_limitations(self):
        for change in (
            {"qa_status": "phone-qa-pending"},
            {"phone_evidence": []},
            {"known_limitations": []},
            {"known_limitations": [" "]},
            {"prerelease": True},
            {"tag": "v0.5.153-debug"},
        ):
            with self.subTest(change=change), self.assertRaises(ValueError):
                publisher.validate_manifest(self.write(core_request(**change)))

    def test_filename_and_channel_must_match(self):
        with self.assertRaises(ValueError):
            publisher.validate_manifest(self.write(core_request(), name="v0.5.153-stable.json"))
        with self.assertRaises(ValueError):
            publisher.validate_manifest(self.write(core_request(channel="beta")))

    def test_debug_promotion_still_validates(self):
        self.assertEqual(
            publisher.validate_manifest(self.promo / "v0.5.153.json")["tag"], "v0.5.153-debug"
        )

    def test_notes_carry_limitations_signer_and_digest(self):
        notes = publisher.core_stable_notes(core_request(), "ab" * 32, publisher.STABLE_SIGNER_SHA256)
        for part in ("Core Stable", "Не з Google Play", "A26: встановлено поверх",
                     "ab" * 32, publisher.STABLE_SIGNER_SHA256, "ставиться поверх"):
            self.assertIn(part, notes)


class ApkSignerParsingContract(unittest.TestCase):
    """First real publish (2026-10-17, run 39058925799): «Signer certificate digest not printed»."""

    def run_with(self, out):
        import os, subprocess
        tmp = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, tmp)
        tool = tmp / "build-tools/35.0.0/apksigner"
        tool.parent.mkdir(parents=True)
        tool.write_text("x")
        with mock.patch.dict(os.environ, {"ANDROID_SDK_ROOT": str(tmp)}), mock.patch.object(
            publisher, "command", return_value=subprocess.CompletedProcess([], 0, out, "")
        ) as cmd:
            return publisher.apk_signer_sha256(Path("a.apk")), cmd

    def test_both_apksigner_formats_and_verbose_flag(self):
        digest = publisher.STABLE_SIGNER_SHA256
        for prefix in ("Signer #1", "Signer (minSdkVersion=26, maxSdkVersion=2147483647)"):
            got, cmd = self.run_with(f"{prefix} certificate SHA-256 digest: {digest}\n")
            self.assertEqual(got, digest)
            self.assertIn("--verbose", cmd.call_args.args)

    def test_no_or_two_digests_rejected_with_output(self):
        with self.assertRaisesRegex(ValueError, "got 0"):
            self.run_with("Verifies\n")
        with self.assertRaisesRegex(ValueError, "got 2"):
            self.run_with(
                "Signer #1 certificate SHA-256 digest: " + "a" * 64 + "\n"
                "Signer #2 certificate SHA-256 digest: " + "b" * 64 + "\n"
            )


class CoreStableWorkflowContract(unittest.TestCase):
    def test_workflow_accepts_core_stable_file_and_publisher_checks_signer(self):
        wf = (ROOT / ".github/workflows/publish-verified-apk.yml").read_text(encoding="utf-8")
        self.assertIn(r"(-core-stable)?\.json$", wf)
        src = (ROOT / "scripts/publish_debug_release.py").read_text(encoding="utf-8")
        self.assertIn("signer != STABLE_SIGNER_SHA256", src)
        self.assertIn('"--latest"', src)
        # Actions logs are not readable from Claude's environment — failures must be annotations.
        self.assertIn("::error title=Publisher::", src)


if __name__ == "__main__":
    unittest.main()
