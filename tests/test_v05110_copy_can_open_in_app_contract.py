"""v0.5.110: the browser copy can also be opened from the app (owner, 2026-10-12).

v0.5.109 removed library registration of the converter's *_android copy
(owner: it is a browser folder). Then the owner recalled the app used to open
such folders («Старі бібліотеки» on Home) and asked to keep that possibility.

Now:
- the converter never adds the copy by itself;
- a button «Додати копію в застосунок» appears after a finished conversion;
- the copy is addressed inside the permitted destination tree, and the dataset
  reader / resolver understand such an address (root = that folder).
"""
from pathlib import Path
import unittest

RETIRED_V05112 = "v0.5.112: «Конвертер»/«Legacy»/«Старі бібліотеки» removed by owner decision 2026-10-12; code kept in branch archive/legacy-converter-v0.5.111"

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


def read(name):
    return (ANDROID / name).read_text(encoding="utf-8")


def between(text, start, end):
    assert start in text, start
    rest = text.split(start, 1)[1]
    assert end in rest, end
    return rest.split(end, 1)[0]


class CopyCanOpenInAppContract(unittest.TestCase):
    @unittest.skip(RETIRED_V05112)
    def test_button_only_after_finished_conversion(self):
        activity = read("ConversionActivity.kt")
        self.assertIn('"Додати копію в застосунок"', activity)
        visibility = between(activity, "addToAppButton.visibility =", "cancelButton.visibility =")
        self.assertIn("state.phase == ConversionRunPhase.COMPLETE", visibility)
        self.assertIn("!copyInApp", visibility)

    @unittest.skip(RETIRED_V05112)
    def test_add_reads_copy_inside_destination(self):
        activity = read("ConversionActivity.kt")
        add = between(activity, "private fun addCopyToApp()", "/** v0.5.104: owner asked")
        self.assertIn("destination.findFile(outputFolderName)", add)
        self.assertIn("DatasetReader.readFolder(", add)
        self.assertIn("datasetStore.upsert(record)", add)
        self.assertIn("«Старі бібліотеки»", add)
        self.assertNotIn("buildTreeDocumentUri", add)

    def test_resolver_root_understands_folder_inside_tree(self):
        resolver = read("SafDatasetResolver.kt")
        self.assertIn("rootDocumentIdOf(treeUri)", resolver)
        helper = between(resolver, "fun rootDocumentIdOf(", "\n        }\n")
        self.assertIn('segments[0] == "tree"', helper)
        self.assertIn('segments[2] == "document"', helper)
        self.assertIn("segments[3]", helper)
        self.assertIn("DocumentsContract.getTreeDocumentId(uri)", helper)
        self.assertIn("SafDatasetResolver\n                    .rootDocumentIdOf(", read("SafDisplayPath.kt"))

    @unittest.skip(RETIRED_V05112)
    def test_service_mentions_button(self):
        service = read("ConversionService.kt")
        self.assertIn("«Додати копію в застосунок»", service)

    def test_version_bumped(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        code = int(gradle.split("versionCode = ", 1)[1].splitlines()[0].strip())
        self.assertGreaterEqual(code, 126)


if __name__ == "__main__":
    unittest.main()
