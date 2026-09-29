from pathlib import Path
import unittest


class SettingsContractTests(unittest.TestCase):
    def test_settings_are_wired_into_library_and_pdf_viewer(self):
        repo = Path(__file__).resolve().parents[1]

        settings = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/AppSettings.kt"
        ).read_text(encoding="utf-8")
        screen = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/SettingsActivity.kt"
        ).read_text(encoding="utf-8")
        main = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/MainActivity.kt"
        ).read_text(encoding="utf-8")
        client = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/SafDatasetWebViewClient.kt"
        ).read_text(encoding="utf-8")
        pdf = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/AndroidPdfLayer.kt"
        ).read_text(encoding="utf-8")
        manifest = (
            repo
            / "android/app/src/main/AndroidManifest.xml"
        ).read_text(encoding="utf-8")
        gradle = (
            repo
            / "android/app/build.gradle.kts"
        ).read_text(encoding="utf-8")

        self.assertIn("renault_docs_settings", settings)
        self.assertIn("default_open_mode", settings)
        self.assertIn("pdf_default_zoom_percent", settings)
        self.assertIn("pdf_zoom_step_percent", settings)
        self.assertIn("backup_tree_uri", settings)

        self.assertIn("SettingsActivity", main)
        self.assertIn("DatasetOpenMode.CLASSIC", main)
        self.assertIn("ViewerActivity", main)

        self.assertIn("Масштаб за замовчуванням", screen)
        self.assertIn("Крок кнопок − / +", screen)
        self.assertIn("Папка резервних копій", screen)
        self.assertIn("Documents/Renault/backups", screen)
        self.assertIn("Скинути вибір", screen)
        self.assertIn("Змінити папку", screen)
        self.assertIn("Вибрати папку", screen)
        self.assertIn("Вибрана папка", screen)
        self.assertIn("body.addView(\n            backupCard()", screen)
        self.assertIn("friendlyBackupPath", screen)
        self.assertIn("SafDisplayPath.tree(", screen)
        formatter = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/SafDisplayPath.kt"
        ).read_text(encoding="utf-8")
        self.assertIn("getTreeDocumentId", formatter)
        self.assertIn("Файли не видалялися", screen)
        self.assertIn("Про програму", screen)

        self.assertIn("pdfDefaultZoomPercent", client)
        self.assertIn("pdfZoomStepPercent", client)
        self.assertIn("initialZoomPercent", pdf)
        self.assertIn("zoomStepPercent", pdf)

        self.assertIn(".SettingsActivity", manifest)
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
