"""v0.5.115 — «Друк»: Android print window (printer or «Зберегти як PDF»).

Owner request 2026-10-12 22:57 (screenshot of the printer icon in an
original Renault page) and 23:00 «Друк...»: tapping print should send the
file to a printer or the PDF printer.

- PDF viewer: a print button next to «⇩ PDF»; the PDF goes as it is.
- Modern tables: «Друк» next to «Зберегти таблицю PDF» (the same PDF).
- Modern «print» actions point to the viewer's print button.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


def read(name):
    return (SRC / name).read_text(encoding="utf-8")


def between(text, start, end):
    assert start in text, start
    rest = text.split(start, 1)[1]
    assert end in rest, end
    return rest.split(end, 1)[0]


class PrintContract(unittest.TestCase):
    def test_printer_uses_android_print_manager_with_pdf_as_is(self):
        printer = read("PdfPrinter.kt")
        for marker in ("PrintManager", "PrintDocumentAdapter", "CONTENT_TYPE_DOCUMENT",
                       "PageRange.ALL_PAGES", "onWriteCancelled()", "onWriteFailed(",
                       "Looper.getMainLooper()", "Thread {"):
            self.assertIn(marker, printer)
        self.assertIn('"Друк недоступний на цьому телефоні."', printer)

    def test_pdf_viewer_has_print_button(self):
        layer = read("AndroidPdfLayer.kt")
        self.assertIn('id="print"', layer)
        self.assertLess(layer.index('id="save"'), layer.index('id="print"'))
        self.assertIn("'renaultprintpdf://print?path=' +", layer)

    def test_client_routes_print_and_writes_pdf_bytes(self):
        client = read("SafDatasetWebViewClient.kt")
        self.assertIn('PDF_PRINT_SCHEME =\n            "renaultprintpdf"', client)
        self.assertIn("onPrintPdf(path)", client)
        write = between(client, "fun writePdf(", "private fun blockExternalResource(")
        self.assertIn('endsWith(".pdf")', write)
        self.assertIn("resolver.openInputStream(", write)
        self.assertNotIn("openOutputStream", write)  # nothing written next to originals

    def test_viewer_prints_from_main_and_companion(self):
        viewer = read("ViewerActivity.kt")
        self.assertEqual(viewer.count("onPrintPdf = { path ->"), 2)
        self.assertIn("datasetClient,\n                        path,", viewer)
        self.assertIn("companionClient,\n                        path,", viewer)
        self.assertIn("PdfPrinter.print(", between(viewer, "private fun printPdf(", "private fun requestPdfSave("))

    def test_table_print_uses_the_same_pdf_as_export(self):
        exporter = read("NativeTablePdfExporter.kt")
        self.assertIn("fun writeTo(", exporter)
        self.assertIn("writeTo(it, data)", between(exporter, "fun write(", "fun writeTo("))
        section = read("NativeSectionActivity.kt")
        fill = between(section, "private fun fillStructuredDocument(", "private fun structuredHeadingTexts(")
        self.assertIn('text = "Друк"', fill)
        self.assertIn("NativeTablePdfExporter.writeTo(", fill)
        self.assertIn('"Зберегти таблицю PDF"', fill)

    def test_old_later_messages_are_gone(self):
        self.assertNotIn("Друк буде додано у native renderer пізніше.", read("NativeSectionActivity.kt"))
        self.assertNotIn("Друк поки не підтримується", read("VolumeDocumentationActivity.kt"))

    def test_version_bumped(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertIn('versionName = "0.5.115"', gradle)
        self.assertIn("versionCode = 131", gradle)


if __name__ == "__main__":
    unittest.main()
