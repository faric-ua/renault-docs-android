"""v0.5.107: three phone-QA findings recorded on 2026-10-12.

(a) After the owner deleted a prepared ⇧ file, the old «завершено» card
    turned into «Підготовка … · помилка — не знайдено». Now the owner's own
    delete closes that card; a file that vanished otherwise shows a neutral
    «файлу вже немає» card, not an error.
(b) Converter: the failure reason was shown twice (under the bar and in the
    status text) and texts mixed English words (source, destination, Output,
    staging, dataset…). Now the reason is shown once and the converter
    speaks Ukrainian: стара папка, папка призначення, готова копія,
    тимчасова папка.
(c) Converter, Kangoo NT8486: «Готово · томів 0 · Автододавання не вдалося».
    The owner chose the volume folder itself (INDEX at its root); volumes are
    always sub-folders. Now the run stops before copying with a clear hint
    («вибери папку на рівень вище»), and an auto-add failure shows its reason.
"""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


def read(name):
    return (ANDROID / name).read_text(encoding="utf-8")


def between(text, start, end):
    assert start in text, start
    rest = text.split(start, 1)[1]
    assert end in rest, end
    return rest.split(end, 1)[0]


# File names such as renault-dataset.json / conversion-report.json are allowed.
ENGLISH = re.compile(
    r"(?<![-\w])(source|Source|destination|Destination|output|Output|staging|Staging|"
    r"dataset|Dataset|merge|package|validation|Validation|scanner|compatibility|"
    r"foreground|authority|parent|manifest)(?![-.\w])"
)


class FindingsContract(unittest.TestCase):
    def test_a_owner_delete_closes_old_completed_card(self):
        main = read("MainActivity.kt")
        deleted = between(main, "onPreparedProjectDeleted = {", "onShareProgress =")
        self.assertIn("last.phase == RdprojectShareRunPhase.COMPLETE", deleted)
        self.assertIn("last.projectId == project.id", deleted)
        self.assertIn("rdprojectShareRunStore.dismissTerminal(last.finishedAtMs)", deleted)
        project = read("ProjectActivity.kt")
        volume = between(project, "PreparedShareStore.deleteVolume(", ".create()")
        self.assertIn("last.volumeId == volume.id", volume)
        self.assertIn("rdpkgShareRunStore.dismissTerminal(last.finishedAtMs)", volume)

    def test_a_missing_file_is_neutral_not_an_error(self):
        for name, title in (
            ("MainActivity.kt", "Підготовка проєкту · файлу вже немає"),
            ("ProjectActivity.kt", "Підготовка тому · файлу вже немає"),
        ):
            source = read(name)
            with self.subTest(screen=name):
                self.assertIn(title, source)
                card = source.split(title, 1)[1][:600]
                self.assertIn("OperationTerminalOutcome.NEUTRAL", card)
        self.assertNotIn('"Підготовлений .rdproject не знайдено."', read("MainActivity.kt"))
        self.assertNotIn('"Підготовлений .rdpkg не знайдено."', read("ProjectActivity.kt"))

    def test_b_failure_reason_shown_once(self):
        activity = read("ConversionActivity.kt")
        bar = between(activity, "state.phase == ConversionRunPhase.FAILED -> {", "state.phase == ConversionRunPhase.CANCELLED ->")
        self.assertIn('progressText.text = "Помилка конвертації"', bar)
        self.assertNotIn("state.message", bar)
        status = between(activity, "ConversionRunPhase.FAILED -> {\n                statusText", "ConversionRunPhase.CANCELLED ->")
        self.assertIn("state.message", status)

    def test_b_converter_texts_are_ukrainian(self):
        for name in ("ConversionActivity.kt", "ConversionService.kt", "SafConversionEngine.kt",
                     "AndroidDatasetPackageWriter.kt"):
            text = read(name)
            for number, line in enumerate(text.splitlines(), 1):
                if "Log." in line:
                    continue
                for literal in re.findall(r'"((?:[^"\\]|\\.)*)"', line):
                    if not re.search("[А-Яа-яІіЇїЄєҐґ]", literal):
                        continue
                    found = ENGLISH.findall(literal)
                    with self.subTest(file=name, line=number):
                        self.assertEqual([], found, literal)
        activity = read("ConversionActivity.kt")
        for label in ('"Вибрати стару папку"', '"Вибрати папку призначення"', '"\\n\\nГотова копія: "'):
            self.assertIn(label, activity)

    def test_c_volume_folder_chosen_itself_is_stopped_before_copying(self):
        writer = read("AndroidDatasetPackageWriter.kt")
        self.assertIn("fun isVolumeFolderItself(", writer)
        engine = read("SafConversionEngine.kt")
        guard = between(engine, "val volumeRoots =\n            AndroidDatasetPackageWriter", "val lowerMap =")
        self.assertIn("if (volumeRoots.isEmpty()) {", guard)
        self.assertIn("isVolumeFolderItself(exactFiles)", guard)
        self.assertIn("Вибери папку на рівень вище", guard)
        self.assertIn("Нічого не скопійовано, стару папку не змінено.", guard)
        # The guard runs before staging is created or anything is copied.
        self.assertLess(engine.index("if (volumeRoots.isEmpty()) {"), engine.index('"Створюю тимчасову папку…"'))

    def test_c_auto_add_removed_by_owner(self):
        # v0.5.107 showed the auto-add failure reason («Вибраний об'єкт не є
        # папкою»). v0.5.108: the owner decided the *_android copy is a
        # browser folder and must not be added to the library at all.
        service = read("ConversionService.kt")
        self.assertNotIn("Автододавання в бібліотеку", service)
        self.assertNotIn("registration", service)

    def test_c_kotlin_unit_tests_cover_both_choices(self):
        test = (ROOT / "android/app/src/test/java/com/saney/renaultdocs/AndroidDatasetPackageWriterTest.kt").read_text(encoding="utf-8")
        self.assertIn("fun volumeFolderChosenItselfHasNoVolumeRoots()", test)
        self.assertIn("fun parentFolderOfVolumeIsNotVolumeItself()", test)

    def test_version_bumped(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        code = int(gradle.split("versionCode = ", 1)[1].splitlines()[0].strip())
        self.assertGreaterEqual(code, 123)


if __name__ == "__main__":
    unittest.main()
