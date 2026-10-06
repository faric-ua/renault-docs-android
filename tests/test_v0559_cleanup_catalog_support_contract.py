from pathlib import Path
import unittest


class V0559CleanupCatalogSupportContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.repo = Path(__file__).resolve().parents[1]

    def read(self, path: str) -> str:
        return (self.repo / path).read_text(encoding="utf-8")

    def test_prepared_project_delete_refreshes_home_indicator(self):
        home = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/MainActivity.kt"
        )
        dialogs = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/HomeProjectDialogController.kt"
        )

        self.assertIn("onPreparedProjectDeleted", dialogs)
        self.assertIn(
            "onPreparedProjectDeleted(\n                        project,\n                        deleted,",
            dialogs,
        )
        self.assertIn("onPreparedProjectDeleted =", home)
        callback = home.split("onPreparedProjectDeleted =", 1)[1]
        callback = callback.split("onShareProgress =", 1)[0]
        self.assertIn("renderLibrary()", callback)

    def test_raw_help_is_user_facing(self):
        project = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/ProjectActivity.kt"
        )
        help_block = project.split("HELP_RAW ->", 1)[1].split("else ->", 1)[0]

        for banned in (
            "Python",
            "Termux",
            "*_android",
            "rotation",
            "Activity",
        ):
            self.assertNotIn(banned, help_block)

        self.assertIn("оригінальну папку Renault", help_block)
        self.assertIn("переносний .rdpkg", help_block)

    def test_other_help_surfaces_do_not_expose_lifecycle_jargon(self):
        paths = (
            "ConversionActivity.kt",
            "ModernVolumeActivity.kt",
            "NativeSectionActivity.kt",
            "VolumeDocumentationActivity.kt",
        )
        base = (
            self.repo
            / "android/app/src/main/java/com/saney/renaultdocs"
        )

        for name in paths:
            text = (base / name).read_text(encoding="utf-8")
            help_block = text.split("private fun helpSpec(", 1)[1]
            help_block = help_block.split("private fun ", 1)[0]
            for banned in (
                "rotation",
                "Activity",
                "legacy entrypoint",
                "opaque",
            ):
                self.assertNotIn(
                    banned,
                    help_block,
                    msg=name,
                )

    def test_project_catalog_uses_one_central_drive_url(self):
        links = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/ExternalLinks.kt"
        )
        main = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/MainActivity.kt"
        )

        self.assertIn("PROJECT_CATALOG_URL", links)
        self.assertIn(
            "https://drive.google.com/drive/folders/",
            links,
        )
        self.assertIn('"Готові проєкти"', main)
        self.assertIn('"Завантажити з Google Drive"', main)
        self.assertTrue(
            "DriveCatalogActivity::class.java" in main
            or "ExternalLinks.PROJECT_CATALOG_URL" in main
        )

    def test_support_action_is_provider_independent(self):
        links = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/ExternalLinks.kt"
        )
        settings = self.read(
            "android/app/src/main/java/com/saney/renaultdocs/SettingsActivity.kt"
        )

        self.assertIn("SUPPORT_URL", links)
        self.assertIn('"Підтримати Renault Docs"', settings)
        self.assertIn("ExternalLinks.SUPPORT_URL", settings)
        self.assertIn(
            "Не відкриває додаткових функцій.",
            settings,
        )

    def test_release_version_is_v0559_or_newer(self):
        gradle = self.read("android/app/build.gradle.kts")

        version_code = int(
            gradle.split("versionCode = ", 1)[1].splitlines()[0].strip()
        )
        version_name = tuple(
            int(part)
            for part in gradle.split('versionName = "', 1)[1].split('"', 1)[0].split(".")
        )

        self.assertGreaterEqual(version_code, 75)
        self.assertGreaterEqual(version_name, (0, 5, 59))


if __name__ == "__main__":
    unittest.main()
