"""v0.5.109: the converter's *_android copy is a browser folder (owner, 2026-10-12).

Phone QA (Kangoo NT8486 «(RUS)», Test2): conversion PASS (10026 files, 1
volume) but «Автододавання в бібліотеку не вдалося: Вибраний об'єкт не є
папкою».

Cause: after converting, the app built a separate tree address for the new
sub-folder (buildTreeDocumentUri). Android granted access only to the chosen
destination (Test2); grants match whole path segments, so the sub-folder
address had no permission and every query looked like «not a folder». The
same address was used when adding volumes to an existing copy (merge), so
that path would have failed the same way.

Owner decision: the copy is a standalone folder to open in any browser,
zip and give away — it is NOT added to the library (the library keeps
volumes in its own Modern/Classic format). So:
- auto-add and «Додати готову папку в бібліотеку» are removed;
- the result explains how to open the copy;
- the copy is always addressed inside the permitted destination tree.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


def read(name):
    return (ANDROID / name).read_text(encoding="utf-8")


class BrowserCopyNotInLibraryContract(unittest.TestCase):
    def test_no_library_registration(self):
        service = read("ConversionService.kt")
        activity = read("ConversionActivity.kt")
        for source in (service, activity):
            self.assertNotIn("DatasetStore", source)
            self.assertNotIn(".upsert(", source)
        self.assertNotIn("registerConvertedDataset", activity)
        self.assertNotIn("Додати готову папку в бібліотеку", activity)

    def test_result_explains_how_to_open(self):
        service = read("ConversionService.kt")
        self.assertIn("Копію для браузера створено.", service)
        self.assertIn("_renault/START.html", service)
        self.assertIn("застосунок не потрібен", service)

    def test_copy_is_addressed_inside_permitted_tree(self):
        engine = read("SafConversionEngine.kt")
        activity = read("ConversionActivity.kt")
        for source in (engine, activity):
            self.assertNotIn("buildTreeDocumentUri", source)
            self.assertNotIn("treeUriForDocument", source)
        self.assertIn("staging.uri.toString()", engine)
        self.assertIn("existingOutput.uri", engine)
        self.assertIn("DatasetReader.readFolder(", engine)
        self.assertIn("DatasetReader.readFolder(", activity)
        # The fast scanner starts from the folder inside the tree.
        self.assertIn("if (DocumentsContract.isDocumentUri(context, treeUri)) {", engine)

    def test_reader_has_in_folder_entry(self):
        reader = read("DatasetReader.kt")
        self.assertIn("fun readFolder(", reader)
        self.assertIn("readFolder(context, root, treeUri.toString()).getOrThrow()", reader)

    def test_help_says_not_added_to_library(self):
        activity = read("ConversionActivity.kt")
        self.assertIn("У бібліотеку Renault Docs вона не додається", activity)
        self.assertIn("папку можна запакувати й передати", activity)
        self.assertNotIn("копія сама додається в бібліотеку", activity)

    def test_version_bumped(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        code = int(gradle.split("versionCode = ", 1)[1].splitlines()[0].strip())
        self.assertGreaterEqual(code, 125)


if __name__ == "__main__":
    unittest.main()
