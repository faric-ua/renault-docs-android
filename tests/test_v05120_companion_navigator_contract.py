"""v0.5.120 — owner requests 2026-10-13 02:30–02:45.

- The lower window («Документація поруч») becomes a navigator of the volume:
  it opens on «Документація тому»; «←» leads to the volume (sections +
  search), never to an empty «about:blank»; a section opens with its tabs
  (Схеми / Розʼєм / Положення / Документація) and ANY document of it opens
  in the lower window while the upper one keeps its own — two different
  documents at the top and at the bottom.
- PDF toolbar: still too big everywhere and huge in landscape «Схема + піни»
  → smaller base 0.72, frame ratio from top.visualViewport.scale.
"""
from pathlib import Path
import shutil
import subprocess
import tempfile
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


def page_script():
    page = read("CompanionNavigatorPage.kt")
    template = between(page, 'private val TEMPLATE = """', '""".trimIndent()')
    script = between(template, "<script>", "</script>")
    return script


class CompanionNavigatorContract(unittest.TestCase):
    def test_page_is_served_as_real_url_by_the_client(self):
        client = read("SafDatasetWebViewClient.kt")
        self.assertIn("private val virtualPage: (String) -> String? = { null }", client)
        intercept = between(client, "override fun shouldInterceptRequest(", "pdfLayer.intercept(")
        self.assertIn("CompanionNavigatorPage.PREFIX", intercept)
        self.assertIn('"no-store"', intercept)
        legacy = between(client, "private fun isLegacyHtml(", "private fun applyStandaloneLegacyCompat(")
        self.assertIn("CompanionNavigatorPage.PREFIX", legacy)

    def test_viewer_loads_navigator_instead_of_message_page(self):
        viewer = read("ViewerActivity.kt")
        load = between(viewer, "private fun loadCompanionDocumentation()", "private fun showCompanionMessage(")
        self.assertIn("companionWebView.loadUrl(", load)
        self.assertIn("CompanionNavigatorPage.PATH", load)
        self.assertNotIn("showCompanionMessage(\n            \"Відкриваю документацію тому…\"", load)
        self.assertNotIn("VolumeDocumentationWebPage", load)
        companion = between(viewer, "companionClient =", "companionWebView =")
        self.assertIn("virtualPage = { path ->", companion)
        self.assertIn("CompanionNavigatorPage.read(", companion)
        back = between(viewer, "private fun companionBack()", "private fun loadCompanionDocumentation()")
        self.assertIn("companionWebView.goBack()", back)
        self.assertIn("hidePdfCompanion()", back)

    def test_reader_parses_index_once_for_all_sections(self):
        reader = read("RuntimeIrReader.kt")
        self.assertIn("fun readIndexText(", reader)
        self.assertIn("fun findVolumeShardPaths(", reader)
        self.assertIn("private fun sectionPathInVolume(", reader)
        shard = between(reader, "fun findSectionShard(", "fun findVolumeShardPaths(")
        self.assertIn("sectionPathInVolume(", shard)
        page = read("CompanionNavigatorPage.kt")
        self.assertIn("ModernSectionsReader", page)
        self.assertIn("ModernSectionDisplayOrder.sorted(", page)

    def test_navigator_chain_docs_then_volume(self):
        script = page_script()
        # first open: volume at the bottom of the «←» chain, docs on top
        first = script.split("// First open:", 1)[1]
        self.assertIn("history.replaceState({ v: 'root' }, '', '#v=root');", first)
        self.assertIn("go({ v: 'docs' });", first)
        self.assertIn("window.addEventListener('popstate'", script)
        # volume screen: search + sections + documentation
        root = between(script, "function renderRoot()", "function renderDocsRoot(")
        self.assertIn("Пошук: код або назва розділу…", root)
        self.assertIn("РОЗДІЛИ ТОМУ", root)
        self.assertIn("'Документація тому'", root)
        self.assertIn("go({ v: 'panel', s: section.code });", root)

    def test_section_tabs_and_any_document_open_below(self):
        script = page_script()
        self.assertIn("case 'SCH': return 'Схеми';", script)
        self.assertIn("case 'NM': return 'Розʼєм';", script)
        self.assertIn("case 'PC': return 'Положення на авто';", script)
        tabs = between(script, "function renderSectionPanel(", "function renderPanelBody(")
        self.assertIn("'Документація'", tabs)
        self.assertIn("go({ v: 'docs' })", tabs)
        opener = between(script, "function openDocument(", "// ---- screens ----")
        self.assertIn("'structured-html'", opener)
        self.assertIn("'composite-document'", opener)
        self.assertIn("openPath(doc.path);", opener)
        parts = between(script, "function renderParts(", "function renderTable(")
        self.assertIn("Схема + піни розʼєма", parts)
        self.assertIn("Схема розʼєму", parts)
        self.assertIn("Опис контактів", parts)
        self.assertIn("location.href = datasetUrl(path);", script)

    def test_selected_item_green_frame_without_checkmark(self):
        page = read("CompanionNavigatorPage.kt")
        self.assertIn("border: 2px solid var(--ok) !important;", page)
        self.assertNotIn("✓", page)

    def test_navigator_script_has_no_kotlin_template_dollar(self):
        page = read("CompanionNavigatorPage.kt")
        template = between(page, 'private val TEMPLATE = """', '""".trimIndent()')
        self.assertNotIn("$", template)

    @unittest.skipUnless(shutil.which("node"), "node not available")
    def test_navigator_script_is_valid_javascript(self):
        script = page_script().replace("__NAVIGATOR_DATA__", "{}")
        with tempfile.NamedTemporaryFile("w", suffix=".js", delete=False) as handle:
            handle.write(script)
            name = handle.name
        result = subprocess.run(["node", "--check", name], capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stderr)


class ToolbarScaleContract(unittest.TestCase):
    def test_toolbar_smaller_and_uses_visual_viewport(self):
        layer = read("AndroidPdfLayer.kt")
        # v0.5.121: base 0.72 → 0.84 (owner: +15–20%), see v05121 test.
        self.assertIn("zoom: var(--ui-scale, 0.84);", layer)
        block = between(layer, "const BASE_UI_SCALE = 0.84;", "const pdfViewport =")
        self.assertIn("window.top.visualViewport", block)
        self.assertIn("1 / pageScale", block)
        self.assertNotIn("screen.width", block)

    def test_version(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertGreaterEqual(int(gradle.split("versionCode = ", 1)[1].splitlines()[0]), 136)


if __name__ == "__main__":
    unittest.main()
