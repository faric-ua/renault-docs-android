from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
HELPER = ROOT / "tools/termux/reno-github.sh"
STATUS = ROOT / "tools/termux/reno-project-status.sh"
DOWNLOAD = ROOT / "tools/termux/reno-download-apk.sh"


class TermuxCompatibleApkContractTests(unittest.TestCase):
    def test_compatibility_is_ancestor_and_android_path_scoped(self):
        text = HELPER.read_text(encoding="utf-8")

        self.assertIn("reno_android_build_compatible()", text)
        self.assertIn("git merge-base --is-ancestor", text)
        self.assertIn("git diff --quiet", text)
        self.assertIn("android \\", text)
        self.assertIn(".github/workflows/android-debug.yml", text)

    def test_compatible_run_must_have_nonexpired_expected_artifact(self):
        text = HELPER.read_text(encoding="utf-8")

        self.assertIn("reno_find_compatible_android_run()", text)
        self.assertIn('expired == false', text)
        self.assertIn('expected_artifact', text)
        self.assertIn('--branch "$branch"', text)

    def test_docs_only_commit_does_not_require_tests_run(self):
        helper = HELPER.read_text(encoding="utf-8")
        status = STATUS.read_text(encoding="utf-8")

        self.assertIn("reno_current_commit_tests_ignored()", helper)
        self.assertIn("docs/*|*.md", helper)
        self.assertIn("Tests: не потрібні · лише docs/markdown", status)

    def test_downloader_rejects_other_branch_or_changed_android_code(self):
        text = DOWNLOAD.read_text(encoding="utf-8")

        self.assertIn('if [ "$RUN_BRANCH" != "$BRANCH" ]; then', text)
        self.assertIn('reno_android_build_compatible "$RUN_SHA" "$HEAD_SHA"', text)
        self.assertIn("STOP: APK build має інший Android-код.", text)


if __name__ == "__main__":
    unittest.main()
