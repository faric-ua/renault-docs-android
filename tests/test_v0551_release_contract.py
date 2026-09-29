from pathlib import Path
import unittest


class V0551ReleaseContractTests(unittest.TestCase):
    def _read(self, path: str) -> str:
        repo = Path(__file__).resolve().parents[1]
        return (repo / path).read_text(encoding="utf-8")

    def test_candidate_version(self):
        gradle = self._read("android/app/build.gradle.kts")

        self.assertIn("versionCode = 67", gradle)
        self.assertIn('versionName = "0.5.51"', gradle)

    def test_phone_gate_is_raw_folder_specific(self):
        qa = self._read("docs/v.0.5.51/qa/PHONE_TEST.md")

        self.assertIn("raw-folder converter", qa)
        self.assertIn("347 · native", qa)
        self.assertIn("no new public `*_android`", qa)
        self.assertIn("RdpkgImporter.install()", qa)
        self.assertIn("rotation/background/external-app", qa.lower())
        self.assertIn("no duplicate/parallel run", qa.lower())

    def test_durable_plan_tracks_phone_gate(self):
        plan = self._read("docs/assistant-kit/CURRENT_PLAN.md")

        self.assertIn("v0.5.51", plan)
        self.assertIn("NT8340A", plan)
        self.assertIn("PHONE PASS", plan)
        self.assertIn("# Renault Docs — CURRENT PLAN", plan)


if __name__ == "__main__":
    unittest.main()
