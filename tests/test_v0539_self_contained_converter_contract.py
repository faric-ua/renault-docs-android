from pathlib import Path
import unittest


class V0539SelfContainedConverterContractTests(unittest.TestCase):
    def _read(self, path: str) -> str:
        repo = Path(__file__).resolve().parents[1]
        return (repo / path).read_text(encoding="utf-8")

    def test_converter_keeps_screen_and_cpu_awake_while_running(self):
        manifest = self._read(
            "android/app/src/main/AndroidManifest.xml"
        )
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ConversionActivity.kt"
        )
        service = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ConversionService.kt"
        )

        self.assertIn("android.permission.WAKE_LOCK", manifest)
        self.assertIn('android:stopWithTask="false"', manifest)
        self.assertIn("FLAG_KEEP_SCREEN_ON", activity)
        self.assertIn("PowerManager.PARTIAL_WAKE_LOCK", service)
        self.assertIn("acquireWakeLock()", service)
        self.assertIn("releaseWakeLock()", service)

    def test_completed_conversion_is_auto_registered(self):
        service = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ConversionService.kt"
        )
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ConversionActivity.kt"
        )

        self.assertIn("DatasetReader.read(", service)
        self.assertIn("DatasetStore(", service)
        self.assertIn(".upsert(", service)
        self.assertIn("Додано в бібліотеку автоматично.", service)
        self.assertIn("outputRegistered", activity)

    def test_nested_android_destination_is_rejected(self):
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ConversionActivity.kt"
        )

        self.assertIn('"renault-dataset.json"', activity)
        self.assertIn('endsWith(', activity)
        self.assertIn('"_android"', activity)
        self.assertIn(
            "Destination має бути батьківською папкою",
            activity,
        )

    def test_validation_has_real_progress(self):
        engine = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/SafConversionEngine.kt"
        )
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ConversionActivity.kt"
        )
        service = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ConversionService.kt"
        )

        self.assertIn("VALIDATION_PROGRESS_EVERY", engine)
        self.assertIn('"Перевіряю output…"', engine)
        self.assertIn('"Перевірка"', activity)
        self.assertIn("ConversionRunPhase.VALIDATING", service)

    def test_interrupted_finalized_output_is_recovered(self):
        activity = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ConversionActivity.kt"
        )
        service = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ConversionService.kt"
        )

        self.assertIn("reconcileInterruptedRun()", activity)
        self.assertIn("ConversionService.isActive()", activity)
        self.assertIn("treeUriForDocument(", activity)
        self.assertIn("Попередній процес завершив файлову конвертацію", activity)
        self.assertIn("fun isActive()", service)

    def test_android_ui_never_requires_termux_post_processing(self):
        reader = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ModernSectionsReader.kt"
        )
        volume = self._read(
            "android/app/src/main/java/com/saney/renaultdocs/ModernVolumeActivity.kt"
        )

        self.assertNotIn("Renault → 9", reader)
        self.assertIn("сумісному Classic режимі", reader)
        self.assertIn('"Сумісний режим"', volume)
        self.assertIn('"Відкрити документацію"', volume)

    def test_release_version(self):
        gradle = self._read(
            "android/app/build.gradle.kts"
        )

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
