"""v0.5.114 — «Критерії / скорочення» pinned on top; picks kept while the app runs.

Owner request 2026-10-12 21:44 (screenshot, section 118 «Схеми»): the
abbreviations button sat at the very end of the list and its table opened
even lower. Owner: pin it right under «Документація» on every tab that has
it, the list scrolls under it (mockup «Скорочення — закріплені зверху»).

Phone QA 22:35 (video): «Назад» from «Загальна документація» returns to
«Документація тому» and the frame is there — back works. The frame was lost
only after leaving the documentation screen and opening it again: picks
lived as long as the screen. Now they are kept for the app session.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/saney/renaultdocs"
RES = ROOT / "android/app/src/main/res"


def read(name):
    return (SRC / name).read_text(encoding="utf-8")


def between(text, start, end):
    assert start in text, start
    rest = text.split(start, 1)[1]
    assert end in rest, end
    return rest.split(end, 1)[0]


class CriteriaPinnedContract(unittest.TestCase):
    def test_criteria_is_recognised(self):
        presentation = read("NativeRuntimePresentation.kt")
        self.assertIn("fun isCriteria(", presentation)
        self.assertIn('normalize(raw) == "CRITERE"', presentation)
        self.assertTrue((RES / "drawable/ic_abbreviations.xml").exists())

    def test_pinned_under_the_menu_above_connector_card_and_list(self):
        section = read("NativeSectionActivity.kt")
        content = between(section, "private fun buildContent(): View {", "private fun buildCriteriaOverlay()")
        menu = content.index("menuContainer,")
        criteria = content.index("criteriaContainer,")
        pinned = content.index("pinnedContainer,")
        frame = content.index("bodyFrame,")
        self.assertLess(menu, criteria)
        self.assertLess(criteria, pinned)
        self.assertLess(pinned, frame)
        # The sheet lies over the list, in the same frame.
        self.assertLess(content.index("bodyFrame.addView(\n            bodyScroll,"),
                        content.index("bodyFrame.addView(\n            criteriaOverlay,"))

    def test_criteria_is_taken_out_of_every_list_kind(self):
        section = read("NativeSectionActivity.kt")
        panel = between(section, "private fun renderPanel(", "private fun renderControl(")
        self.assertIn("currentPanelCriteria = null", panel)
        self.assertIn("captureCriteria(", panel)
        self.assertLess(panel.index("renderCriteriaButton("), panel.index("renderPinnedConnector("))
        self.assertIn("captureCriteria(", between(section, "private fun renderSelect(", "private fun isSelectPrompt("))
        self.assertIn("captureCriteria(", between(section, "private fun renderDocumentList(", "private fun addActionButton("))
        capture = between(section, "private fun captureCriteria(", "private fun renderCriteriaButton(")
        self.assertIn("currentViewKind != VIEW_PANEL", capture)

    def test_table_opens_over_the_list_and_closes(self):
        section = read("NativeSectionActivity.kt")
        show = between(section, "private fun showCriteriaOverlay(", "private fun hideCriteriaOverlay()")
        self.assertIn("fillStructuredDocument(", show)
        self.assertIn("criteriaOverlayBody", show)
        self.assertIn("showSectionHeading =\n                false", show)
        overlay = between(section, "private fun buildCriteriaOverlay()", "private fun activePanelId()")
        self.assertIn('"Закрити скорочення"', overlay)
        self.assertIn("closeCriteria()", overlay)
        back = between(section, "override fun onBackPressed()", "super.onBackPressed()")
        self.assertIn("closeCriteria()", back)
        open_ = between(section, "private fun openCriteria(", "private fun showCriteriaOverlay(")
        self.assertIn("handleAction(action)", open_)  # not a table: the old way

    def test_structured_document_draws_into_any_container(self):
        section = read("NativeSectionActivity.kt")
        render = between(section, "private fun renderStructuredDocument(", "private fun fillStructuredDocument(")
        self.assertIn("target = bodyContainer", render)
        fill = between(section, "private fun fillStructuredDocument(", "private fun structuredHeadingTexts(")
        self.assertNotIn("bodyContainer.addView(", fill)
        header = between(section, "private fun renderStructuredHeader(", "private fun structuredTables(")
        self.assertNotIn("bodyContainer.addView(", header)

    def test_sheet_survives_rotation_and_does_not_follow_other_tabs(self):
        section = read("NativeSectionActivity.kt")
        self.assertIn("STATE_CRITERIA_OPEN,\n            criteriaOpenPanelId,", section)
        button = between(section, "private fun renderCriteriaButton(", "private fun criteriaDocument(")
        self.assertIn("criteriaOpenPanelId != panelId", button)
        part = between(section, "private fun pinnedPartButton(", "private fun encodeSelectionState()")
        self.assertIn('criteriaOpenPanelId = ""', part)

    def test_picks_are_kept_while_the_app_runs(self):
        memory = read("ChoiceMemory.kt")
        self.assertIn("object ChoiceMemory", memory)
        self.assertIn("@Synchronized", memory)
        section = read("NativeSectionActivity.kt")
        for name in ('"choice"', '"pinned"', '"pinnedLabel"', '"pinnedPart"'):
            self.assertIn("ChoiceMemory.map(memoryOwner, " + name + ")", section)
        documentation = read("VolumeDocumentationActivity.kt")
        self.assertIn('"documentation|"', documentation)
        volume = read("ModernVolumeActivity.kt")
        self.assertIn('sectionMemory()["last"] =', volume)

    def test_version_bumped(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertIn('versionName = "0.5.114"', gradle)
        self.assertIn("versionCode = 130", gradle)


if __name__ == "__main__":
    unittest.main()
