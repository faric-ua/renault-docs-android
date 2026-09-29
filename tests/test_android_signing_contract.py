from pathlib import Path
import unittest


class AndroidSigningContractTests(unittest.TestCase):
    def test_debug_workflow_uses_secret_backed_stable_development_signer(self):
        repo = Path(__file__).resolve().parents[1]
        workflow = (
            repo / ".github/workflows/android-debug.yml"
        ).read_text(encoding="utf-8")
        gradle = (
            repo / "android/app/build.gradle.kts"
        ).read_text(encoding="utf-8")
        tracked_key = repo / ".github/signing/renault-docs-dev.jks.b64"

        self.assertFalse(
            tracked_key.exists(),
            "Development keystore material must not be tracked in Git.",
        )

        for secret_name in (
            "RENAULT_DEV_KEYSTORE_B64",
            "RENAULT_DEV_STORE_PASSWORD",
            "RENAULT_DEV_KEY_ALIAS",
            "RENAULT_DEV_KEY_PASSWORD",
            "RENAULT_DEV_CERT_SHA256",
        ):
            self.assertIn(f"secrets.{secret_name}", workflow)

        for env_name in (
            "RENAULT_DEV_KEYSTORE_PATH",
            "RENAULT_DEV_STORE_PASSWORD",
            "RENAULT_DEV_KEY_ALIAS",
            "RENAULT_DEV_KEY_PASSWORD",
        ):
            self.assertIn(env_name, gradle)

        self.assertNotRegex(
            gradle,
            r'(?:storePassword|keyPassword)\\s*=\\s*"[^"]+"',
            "Signing passwords must not be hardcoded in Gradle.",
        )
        self.assertNotIn(
            "base64 --decode .github/signing/renault-docs-dev.jks.b64",
            workflow,
        )
        self.assertIn('create("stableDebug")', gradle)
        self.assertIn("renaultStableDebugSigningAvailable", gradle)


    def test_android_signing_workflow_does_not_run_on_pull_requests(self):
        repo = Path(__file__).resolve().parents[1]
        workflow = (
            repo / ".github/workflows/android-debug.yml"
        ).read_text(encoding="utf-8")

        self.assertNotIn(
            "pull_request:",
            workflow,
            "Stable signing workflow must never run automatically for PRs.",
        )
        self.assertIn("workflow_dispatch:", workflow)
        self.assertIn("branches: [main]", workflow)


if __name__ == "__main__":
    unittest.main()
