"""v0.5.113 — what you picked stays visible (owner request 2026-10-12).

Phone screen recording 21:12: in section 103 «Розʼєм» a tapped connector gave
no sign it was chosen, and its documents («Схема + піни розʼєма», «Схема
розʼєму», «Опис контактів») were appended at the very bottom, so the owner
had to scroll down to find them. Owner picked mockup variant A:

- the chosen list item: green frame + ✓ on the right (everywhere you pick
  from a list — section tabs, panel lists, documentation, volume sections);
- the chosen connector and its documents pinned above the list;
- an opened inline document (table) is scrolled into view.
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


class SelectedChoiceContract(unittest.TestCase):
    def test_one_shared_selected_look(self):
        ui = read("Ui.kt")
        for marker in ("fun selectedBackground(", "data class ChoiceTag(",
                       "fun styleChoice(", "fun refreshChoices(",
                       "setStroke(dp(context, 2), success)"):
            # "R.drawable.ic_check_circle" left Ui in v0.5.117 (owner: no ✓).
            self.assertIn(marker, ui)
        check = (RES / "drawable/ic_check_circle.xml").read_text(encoding="utf-8")
        self.assertIn("#62D296", check)

    def test_list_items_get_frame_and_check(self):
        section = read("NativeSectionActivity.kt")
        button = between(section, "private fun buildBodyActionButton(", "private fun currentChoiceScope()")
        self.assertIn("Ui.ChoiceTag(", button)
        self.assertIn("selectedChoice[scope] == key", button)
        self.assertIn("Ui.refreshChoices(", button)
        self.assertIn("handleAction(action)", button)

    def test_active_tab_is_highlighted_in_portrait_too(self):
        section = read("NativeSectionActivity.kt")
        tile = between(section, "private fun menuTileButton(", "private fun openVolumeDocumentation()")
        self.assertIn("label == currentMenuLabel", tile)
        self.assertIn("Ui.selectedBackground(", tile)
        select = between(section, "private fun selectTopMenu(", "private fun headerAction(")
        self.assertIn("renderMenu()", select.split("if (isLandscapeOrientation()) {", 1)[1].split("}", 1)[1])

    def test_chosen_connector_is_pinned_above_the_list(self):
        section = read("NativeSectionActivity.kt")
        content = between(section, "private fun buildContent(): View {", "private fun isLandscapeOrientation()")
        self.assertLess(content.index("pinnedContainer,"), content.index("bodyScroll,"))
        panel = between(section, "private fun renderPanel(", "private fun renderControl(")
        self.assertIn("renderPinnedConnector(", panel)
        composite = between(section, "private fun renderCompositeDocument(", "private class CompositePart(")
        pinned_path = composite.split("if (validParent != null) {", 1)[1].split("return\n", 1)[0]
        self.assertIn("pinnedConnector[validParent] =", pinned_path)
        self.assertIn("renderPanel(", pinned_path)
        self.assertNotIn("addCompositePartButton(", pinned_path)
        parts = between(section, "private fun compositeParts(", "private fun hidePinnedConnector()")
        for label in ('"Схема + піни розʼєма"', '"Схема розʼєму"', '"Опис контактів"'):
            self.assertIn(label, parts)
        card = between(section, "private fun renderPinnedConnector(", "private fun encodeSelectionState()")
        self.assertIn('"ВИБРАНО"', card)
        self.assertIn("chunked(3)", card)
        self.assertIn("if (landscape) 1 else 2", card)

    def test_other_views_hide_the_pinned_card(self):
        section = read("NativeSectionActivity.kt")
        self.assertIn("hidePinnedConnector()",
                      between(section, "private fun renderDocumentationMenu(", "private fun renderPanel("))
        body = between(section, "private fun prepareDocumentBody(", "private fun renderStructuredDocument(")
        self.assertIn("hidePinnedConnector()", body)

    def test_opened_document_is_scrolled_into_view_only_after_a_tap(self):
        section = read("NativeSectionActivity.kt")
        body = between(section, "private fun prepareDocumentBody(", "private fun renderStructuredDocument(")
        self.assertIn("if (scrollToDocumentOnce)", body)
        self.assertIn("smoothScrollTo(", body)
        # 2 in v0.5.113; v0.5.114 adds the «Критерії / скорочення» fallback.
        # Every «true» is paired with a «false» right after the action.
        self.assertGreaterEqual(section.count("scrollToDocumentOnce = true"), 2)
        self.assertEqual(section.count("scrollToDocumentOnce = true"),
                         section.count("scrollToDocumentOnce = false"))

    def test_choice_survives_rotation(self):
        section = read("NativeSectionActivity.kt")
        self.assertIn("STATE_SELECTION,\n            encodeSelectionState(),", section)
        self.assertIn("restoreSelectionState(", section)
        volume = read("ModernVolumeActivity.kt")
        self.assertIn("STATE_LAST_SECTION,\n            lastOpenedSectionCode,", volume)
        documentation = read("VolumeDocumentationActivity.kt")
        self.assertIn("STATE_SELECTION,", documentation)

    def test_volume_sections_and_documentation_mark_the_last_opened(self):
        volume = read("ModernVolumeActivity.kt")
        row = between(volume, "private fun sectionRow(", "private fun openSection(")
        self.assertIn("section.code ==\n                    lastOpenedSectionCode", row)
        # v0.5.117: the ✓ in the row was removed; the green frame stays.
        self.assertIn("Ui.selectedBackground(", row)
        self.assertIn("lastOpenedSectionCode =\n            section.code",
                      between(volume, "private fun openSection(", "private fun openVolumeDocumentation()"))
        documentation = read("VolumeDocumentationActivity.kt")
        tile = between(documentation, "private fun addActionTile(", "private fun isActionResolvable(")
        self.assertIn("Ui.ChoiceTag(", tile)
        self.assertIn("Ui.refreshChoices(", tile)

    def test_version_bumped(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertGreaterEqual(int(gradle.split("versionCode = ", 1)[1].splitlines()[0]), 129)


if __name__ == "__main__":
    unittest.main()
