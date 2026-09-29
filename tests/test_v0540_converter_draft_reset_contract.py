from pathlib import Path
import unittest


class V0540ConverterDraftResetContractTests(unittest.TestCase):
    def _read(self, path: str) -> str:
        repo = Path(__file__).resolve().parents[1]
        return (repo / path).read_text(encoding="utf-8")

    def test_new_folder_selection_clears_previous_completed_run(self):
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ConversionActivity.kt"
        )

        self.assertIn(
            "// A newly selected source or destination starts a new conversion draft.",
            activity,
        )
        self.assertIn("runStore.clearFinished()", activity)

        select_pos = activity.index(
            "// A newly selected source or destination starts a new conversion draft."
        )
        save_pos = activity.index("draftStore.saveSource(", select_pos)
        self.assertLess(select_pos, save_pos)

    def test_plan_validation_does_not_show_stale_previous_output(self):
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ConversionActivity.kt"
        )

        validate_pos = activity.index("private fun validatePlan()")
        clear_pos = activity.index("runStore.clearFinished()", validate_pos)
        validated_pos = activity.index("validatedPlan()", validate_pos)

        self.assertLess(clear_pos, validated_pos)
        self.assertIn("renderRunState()", activity[validate_pos:validated_pos])

    def test_release_version(self):
        gradle = self._read("android/app/build.gradle.kts")

        version_code = int(
            gradle.split("versionCode = ", 1)[1].splitlines()[0].strip()
        )
        version_name = tuple(
            int(part)
            for part in gradle.split('versionName = "', 1)[1].split('"', 1)[0].split(".")
        )
        self.assertGreaterEqual(version_code, 67)
        self.assertGreaterEqual(version_name, (0, 5, 51))


if __name__ == "__main__":
    unittest.main()
