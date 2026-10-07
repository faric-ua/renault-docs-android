from pathlib import Path
import unittest


class TermuxCandidateMenuContractTests(unittest.TestCase):
    def _read(self, path: str) -> str:
        repo = Path(__file__).resolve().parents[1]
        return (repo / path).read_text(encoding="utf-8")

    def test_menu_updates_current_branch_not_hardcoded_main(self):
        menu = self._read("menu.sh")

        self.assertIn('branch="$(git branch --show-current)"', menu)
        self.assertIn(
            '"+refs/heads/$branch:refs/remotes/origin/$branch"',
            menu,
        )
        self.assertIn(
            'git merge --ff-only "refs/remotes/origin/$branch"',
            menu,
        )

    def test_menu_exposes_candidate_and_return_to_main(self):
        menu = self._read("menu.sh")

        self.assertIn("16 — Тестовий candidate PR: перейти + build/download APK", menu)
        self.assertIn("17 — Повернутися на main", menu)
        self.assertIn('bash "$REPO/tools/termux/reno-candidate.sh"', menu)

    def test_candidate_script_switches_remote_pr_branch_safely(self):
        script = self._read("tools/termux/reno-candidate.sh")

        self.assertIn('CANDIDATE_PR="${RENAULT_CANDIDATE_PR:-}"', script)
        self.assertIn('gh pr list', script)
        self.assertIn('Номер PR (0 — назад):', script)
        self.assertIn('gh pr view "$CANDIDATE_PR"', script)
        self.assertIn("--jq '[.state, .headRefName, .title, .url] | @tsv'", script)
        self.assertNotIn("| jq -r", script)
        self.assertIn("git diff --quiet", script)
        self.assertIn(
            '"+refs/heads/$PR_BRANCH:refs/remotes/origin/$PR_BRANCH"',
            script,
        )
        self.assertIn(
            'refs/remotes/origin/$PR_BRANCH',
            script,
        )
        self.assertIn(
            'REMOTE_REF="refs/remotes/origin/$PR_BRANCH"',
            script,
        )
        self.assertIn(
            'git switch -c "$PR_BRANCH" "$REMOTE_REF"',
            script,
        )
        self.assertNotIn("git switch --track", script)
        self.assertIn(
            'git merge --ff-only "$REMOTE_REF"',
            script,
        )
        self.assertIn('reno-build-apk.sh', script)
        self.assertNotIn('exec bash "$REPO_DIR/tools/termux/reno-download-apk.sh"', script)
        self.assertIn('git write-tree', script)
        self.assertIn('git rev-parse "$REMOTE_REF^{tree}"', script)
        self.assertIn('git reset --hard HEAD', script)
        self.assertIn('Знайдено слід перерваного переходу на candidate.', script)

    def test_candidate_download_remains_exact_version_artifact(self):
        downloader = self._read("tools/termux/reno-download-apk.sh")

        self.assertIn('EXPECTED_ARTIFACT="Renault-Docs-v${VERSION}-Debug"', downloader)
        self.assertIn(
            "Старішу версію з іншим Android-кодом або artifact з іншої гілки НЕ завантажую.",
            downloader,
        )
        self.assertIn('if [ "$RUN_BRANCH" != "$BRANCH" ]; then', downloader)
        self.assertIn('reno_android_build_compatible "$RUN_SHA" "$HEAD_SHA"', downloader)

    def test_apk_lookup_does_not_abort_under_pipefail(self):
        downloader = self._read("tools/termux/reno-download-apk.sh")

        self.assertIn("set -euo pipefail", downloader)
        self.assertIn("| sed -n '1p'", downloader)
        self.assertNotIn("| head -n 1", downloader)
        self.assertIn("-print -quit", downloader)


if __name__ == "__main__":
    unittest.main()
