"""Termux APK release fallback: provenance/compatibility and shell syntax."""
from pathlib import Path
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[1]
DOWNLOAD = ROOT / "tools/termux/reno-download-apk.sh"
RELEASE = ROOT / "tools/termux/reno-download-release-apk.sh"


class TermuxReleaseFallbackContract(unittest.TestCase):
    def test_bash_syntax(self):
        for file in (DOWNLOAD, RELEASE):
            subprocess.run(
                ["bash", "-n", str(file)],
                check=True, capture_output=True, text=True,
            )

    def test_actions_download_remains_primary_exact_commit_route(self):
        downloader = DOWNLOAD.read_text(encoding="utf-8")
        self.assertIn("reno_find_compatible_android_run", downloader)
        self.assertIn("reno-download-release-apk.sh", downloader)
        self.assertIn('if [ "$BRANCH" = "main" ]', downloader)
        self.assertIn('if [ -z "$RUN_ID" ]; then', downloader)
        self.assertIn('if [ "$RUN_SHA" != "$HEAD_SHA" ]; then', downloader)
        self.assertIn('reno_android_build_compatible "$RUN_SHA" "$HEAD_SHA"', downloader)
        self.assertIn('gh run download "$RUN_ID"', downloader)
        # Explicit RUN_ID does not allow changing to unrelated Release.
        self.assertLess(
            downloader.index('reno-download-release-apk.sh'),
            downloader.index('RUN_INFO="$(')
        )

    def test_release_is_pinned_to_reviewed_source_and_checksum(self):
        release = RELEASE.read_text(encoding="utf-8")
        for marker in (
            'if [ "$GH_REPO" != "faric-ua/renault-docs-android" ]; then',
            'RELEASE_SHA',
            '"$RELEASE_SHA" != "$PROMOTION_SHA"',
            'reno_android_build_compatible "$RELEASE_SHA" "$HEAD_SHA"',
            '.prerelease|tostring',
            '.draft|tostring',
            'gh release download "$TAG"',
            '--pattern "$APK_NAME"',
            '--pattern "$APK_NAME.sha256"',
            'sha256sum -c "$APK_NAME.sha256"',
            'reno-open-latest-apk.sh',
            '/storage/emulated/0/Documents/Renault/packages/',
        ):
            self.assertIn(marker, release)
        self.assertNotIn("git reset", release)
        self.assertNotIn("rm -rf", release)
        self.assertNotIn("am force-stop", release)

if __name__ == "__main__":
    unittest.main()
