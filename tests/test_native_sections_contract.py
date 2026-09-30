from pathlib import Path
import unittest


class NativeSectionsContractTests(unittest.TestCase):
    def test_package_and_android_use_native_section_index(self):
        repo = Path(__file__).resolve().parents[1]

        package = (
            repo
            / "core/dataset_package.py"
        ).read_text(encoding="utf-8")
        sections = (
            repo
            / "core/sections.py"
        ).read_text(encoding="utf-8")
        package_tool = (
            repo
            / "tools/package_dataset.py"
        ).read_text(encoding="utf-8")
        modern = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/ModernDatasetActivity.kt"
        ).read_text(encoding="utf-8")
        volume = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/ModernVolumeActivity.kt"
        ).read_text(encoding="utf-8")
        reader = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/ModernSectionsReader.kt"
        ).read_text(encoding="utf-8")
        viewer = (
            repo
            / "android/app/src/main/java/com/saney/renaultdocs/ViewerActivity.kt"
        ).read_text(encoding="utf-8")

        self.assertIn("modern-sections.json", sections)
        self.assertIn("write_modern_sections_index", package)
        self.assertIn('"modern_sections"', package)
        self.assertIn("Sections:", package_tool)

        self.assertIn("ModernVolumeActivity", modern)
        self.assertIn("ModernSectionsReader", volume)
        self.assertIn("Розділів:", volume)
        self.assertIn("Пошук: 101", volume)
        self.assertIn('label =\n                    "Classic"', volume)
        self.assertIn("Ui.modeButton(", volume)
        self.assertIn("section.code", volume)
        self.assertIn("section.title", volume)
        self.assertIn("NativeSectionActivity", volume)
        self.assertIn(".intentForSection(", volume)

        self.assertIn("_renault/modern-sections.json", reader)
        self.assertIn("modernVolumeEntrypoint", viewer)
        self.assertIn("ModernVolumeActivity", viewer)


if __name__ == "__main__":
    unittest.main()
