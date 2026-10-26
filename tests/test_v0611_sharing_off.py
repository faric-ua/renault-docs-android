"""TODO №39 (faric 2026-10-26): у Renodocs передавати томи й проєкти вимкнено — пункти сірі."""

from pathlib import Path
import re
import unittest

REPO = Path(__file__).resolve().parents[1]
SRC = REPO / "android/app/src/main/java/com/saney/renaultdocs"


def body(text: str, name: str) -> list[str]:
    out = []
    for m in re.finditer(r"private fun " + re.escape(name) + r"\(", text):
        out.append(text[m.start():m.start() + 900])
    return out


class SharingOffTests(unittest.TestCase):
    def test_policy_off(self):
        policy = (SRC / "SharePolicy.kt").read_text(encoding="utf-8")
        self.assertIn("const val SHARING_ENABLED = false", policy)
        self.assertIn('OFF_SUFFIX = " · вимкнено"', policy)

    def test_every_share_or_export_entry_is_guarded(self):
        cases = {
            "ProjectActivity.kt": ["shareRdpkg", "sharePreparedRdpkg", "startRdpkgExport"],
            "MainActivity.kt": ["openProjectFolderExportPicker", "startSeparateVolumeExport", "sharePreparedRdproject"],
            "HomeProjectDialogController.kt": ["showExportWizard", "shareProject", "sharePreparedProject"],
        }
        for file, names in cases.items():
            text = (SRC / file).read_text(encoding="utf-8")
            for name in names:
                bodies = body(text, name)
                self.assertTrue(bodies, (file, name))
                for b in bodies:
                    self.assertIn("if (!SharePolicy.SHARING_ENABLED)", b, (file, name))

    def test_menu_items_are_greyed(self):
        project = (SRC / "ProjectActivity.kt").read_text(encoding="utf-8")
        for label in ("Експортувати .rdpkg", "Підготувати і поділитися томом", "Поділитися підготовленим .rdpkg"):
            self.assertIn(f'title = SharePolicy.title("{label}")', project)
        home = (SRC / "HomeProjectDialogController.kt").read_text(encoding="utf-8")
        self.assertIn('SharePolicy.title("Експортувати томи окремо (.rdpkg)")', home)
        self.assertIn("SharePolicy.title(\n                    preparedShareTitle(", home)
        self.assertEqual(home.count("enabled = SharePolicy.SHARING_ENABLED"), 2)
        self.assertEqual(project.count("enabled = SharePolicy.SHARING_ENABLED"), 3)

    def test_version_and_guide(self):
        gradle = (REPO / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertIn('versionName = "0.6.11"', gradle)
        self.assertIn("versionCode = 181", gradle)
        self.assertTrue((REPO / "android/app/src/main/assets/whats_new.txt").read_text(encoding="utf-8").startswith("0.6.11\n"))
        guide = (REPO / "android/app/src/main/assets/guide/index.html").read_text(encoding="utf-8")
        self.assertIn("v0.6.11", guide)
        self.assertIn("вимкнено автором", guide)


if __name__ == "__main__":
    unittest.main()
