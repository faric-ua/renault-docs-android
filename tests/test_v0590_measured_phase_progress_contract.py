"""Source contract for user-reported native progress gaps in v0.5.90."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


class MeasuredPhaseProgressContractTests(unittest.TestCase):
    def code(self, name):
        return (SRC / name).read_text(encoding="utf-8")

    def test_unknown_total_is_busy_not_static_zero(self):
        bar = self.code("SharedOperationProgressBar.kt")
        self.assertIn("if (!completed && measured == null)", bar)
        self.assertIn("bar.isIndeterminate = true", bar)
        self.assertIn("bar.isIndeterminate = false", bar)
        self.assertIn("bar.progressTintList = colors", bar)
        self.assertIn("bar.indeterminateTintList = colors", bar)
        self.assertIn("Ui.success", bar)

    def test_native_status_one_authoritative_stream(self):
        service = self.code("NativeRdpkgPreparationService.kt")
        self.assertIn("onMessage = { _ -> }", service)
        self.assertIn("onProgress = { _ -> }", service)
        self.assertIn("message = progressPrefix + progress.displayText()", service)
        self.assertIn("phase = NativeRdpkgRunPhase.IMPORTING", service)
        self.assertIn("phase = NativeRdpkgRunPhase.PREPARING", service)

    def test_runtime_reports_completed_sections_not_guesswork(self):
        runtime = self.code("NativeRuntimeIrCompiler.kt")
        engine = self.code("NativeRdpkgPreparationEngine.kt")
        self.assertIn("sectionProgress?.invoke(0, knownTotal)", runtime)
        self.assertIn("onSectionCompiled?.invoke(index + 1, sectionIndex.length())", runtime)
        self.assertIn("sectionsProcessed + done", runtime)
        self.assertIn("phaseProgress?.invoke(\"Зберігаю індекси…\")", runtime)
        self.assertIn('itemLabel = "Розділів"', engine)
        self.assertIn("OperationProgress.indeterminate(stage)", engine)

    def test_fast_pack_uses_actual_file_total(self):
        writer = self.code("NativeFastPackWriter.kt")
        engine = self.code("NativeRdpkgPreparationEngine.kt")
        self.assertIn("fileProgress?.invoke(0, sources.size)", writer)
        self.assertIn("fileProgress?.invoke(completed, sources.size)", writer)
        self.assertIn('stage = "Пакую дані…"', engine)
        self.assertIn('itemLabel = "Файлів"', engine)

    def test_terminal_display_is_lossless_and_semantically_colored(self):
        view = self.code("OperationStatusView.kt")
        fmt = self.code("OperationStatusDisplayFormat.kt")
        self.assertIn("OperationStatusDisplayFormat.wrapHashesForDisplay(detail)", view)
        self.assertIn("fullDetail.trim()", view)
        self.assertIn("digest.chunked(12)", fmt)
        self.assertIn("OperationTerminalOutcome.FAILED -> Ui.danger", view)
        self.assertIn("OperationTerminalOutcome.CANCELLED -> Ui.warning", view)
        self.assertIn("OperationTerminalOutcome.SUCCESS -> Ui.success", view)
        self.assertIn('"Триває обробка…"', view)

    def test_converter_also_displays_terminal_errors_in_color(self):
        conv = self.code("ConversionActivity.kt")
        self.assertIn("state.phase == ConversionRunPhase.FAILED", conv)
        self.assertIn("color = Ui.danger", conv)
        self.assertIn("state.phase == ConversionRunPhase.CANCELLED", conv)
        self.assertIn("color = Ui.warning", conv)


if __name__ == "__main__":
    unittest.main()
