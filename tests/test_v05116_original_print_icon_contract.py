"""v0.5.116 — print through the ORIGINAL Renault printer icon.

Owner 2026-10-13 00:27 (screenshots: section 119 «Схема + піни», the old
printer icon on the original page): «не там ти друк поставив… нічого ніде
ніяку кнопку не додавати, вона вже є кнопка друкувати, просто на неї
нацепити». So the v0.5.115 buttons (PDF viewer toolbar, table «Друк») were
removed, and the original icon now opens the Android print window
(printer or «Зберегти як PDF»).

How the original icon works: `javascript:imprimer()` from VISU.JS opens the
print variant of the scheme, `X_PRINT.PDF` / `X_PRINTNB.PDF` (real files use
these upper-case names, see docs/initial-analysis.md). The app catches that
and prints; if a volume has no print variant, the scheme itself is printed.
Pages that call window.print() are printed as shown.
"""
from pathlib import Path
import re
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


class OriginalPrintIconContract(unittest.TestCase):
    def test_added_buttons_are_gone(self):
        layer = read("AndroidPdfLayer.kt")
        self.assertNotIn('id="print"', layer)
        section = read("NativeSectionActivity.kt")
        fill = between(section, "private fun fillStructuredDocument(", "private fun structuredHeadingTexts(")
        self.assertNotIn('text = "Друк"', fill)
        self.assertIn('"Зберегти таблицю PDF"', fill)

    def test_print_variant_pattern(self):
        client = read("SafDatasetWebViewClient.kt")
        self.assertIn('"_PRINT(NB)?(\\\\.PDF)$"', client)
        self.assertIn("RegexOption.IGNORE_CASE", client)
        pattern = re.compile(r"_PRINT(NB)?(\.PDF)$", re.IGNORECASE)
        for path in ("119/X_PRINT.PDF", "119/x_printnb.pdf", "A/B/SCH_PRINTNB.PDF"):
            self.assertTrue(pattern.search(path), path)
        for path in ("119/X.PDF", "119/PRINTER.PDF", "119/X_PRINT.HTM"):
            self.assertFalse(pattern.search(path), path)
        self.assertEqual(pattern.sub(r"\2", "119/X_PRINTNB.PDF"), "119/X.PDF")

    def test_navigation_to_print_variant_prints_and_stays(self):
        client = read("SafDatasetWebViewClient.kt")
        route = between(client, "override fun shouldOverrideUrlLoading(", "val localPath =")
        block = between(route, "PRINT_VARIANT.containsMatchIn(it)", "return true")
        self.assertIn("onPrintPdf(printPath)", block)
        self.assertIn("PAGE_PRINT_SCHEME", route)
        self.assertIn("onPrintPage(view)", route)

    def test_missing_print_variant_falls_back_to_the_scheme(self):
        client = read("SafDatasetWebViewClient.kt")
        write = between(client, "fun writePdf(", "private fun blockExternalResource(")
        self.assertIn("PRINT_VARIANT.containsMatchIn(it)", write)
        self.assertIn('"$2"', write)

    def test_window_print_is_bridged_in_every_frame(self):
        client = read("SafDatasetWebViewClient.kt")
        finished = between(client, "override fun onPageFinished(", "override fun shouldOverrideUrlLoading(")
        self.assertIn("applyPrintBridge(", finished)
        bridge = between(client, "private fun applyPrintBridge(", "fun writePdf(")
        for marker in ("w.print = function", "$PAGE_PRINT_SCHEME://print",
                       "querySelectorAll('frame, iframe')", "addEventListener('load'",
                       "__renaultPrintPatched"):
            self.assertIn(marker, bridge)

    def test_viewer_prints_print_variant_once(self):
        layer = read("AndroidPdfLayer.kt")
        self.assertIn("/_PRINT(NB)?\\.PDF${'$'}/i.test(pdfPath)", layer)
        self.assertIn("'renaultPrinted|' + pdfPath", layer)
        self.assertIn("'renaultprintpdf://print?path=' +", layer)

    def test_viewer_wires_page_print_for_main_and_companion(self):
        viewer = read("ViewerActivity.kt")
        self.assertEqual(viewer.count("onPrintPage = { page ->"), 2)
        printer = read("PdfPrinter.kt")
        self.assertIn("webView.createPrintDocumentAdapter(name)", printer)

    def test_version_bumped(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertIn('versionName = "0.5.116"', gradle)
        self.assertIn("versionCode = 132", gradle)


if __name__ == "__main__":
    unittest.main()
