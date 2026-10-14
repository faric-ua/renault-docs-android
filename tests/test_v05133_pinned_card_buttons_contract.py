"""Phone QA 2026-10-14 19:56 (M30s, Laguna II, section 103, «Розʼєм»):
in the «ВИБРАНО» card «Схема розʼєму» and «Опис контактів» wrap to two lines
while «Схема + піни» stays on one; the row aligned buttons by text baseline,
the two-line buttons slid down and were cut off by the card.
"""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ACT = ROOT / "android/app/src/main/java/com/saney/renaultdocs/NativeSectionActivity.kt"


class PinnedCardButtons(unittest.TestCase):
    def test_row_has_no_baseline_alignment_and_equal_heights(self):
        src = ACT.read_text(encoding="utf-8")
        block = src.split(".chunked(3)", 1)[1].split("pinnedContainer.addView(", 1)[0]
        self.assertIn("isBaselineAligned =\n                            false", block)
        add = block.split("pinnedPartButton(", 1)[1]
        self.assertIn("LinearLayout.LayoutParams\n                                .MATCH_PARENT,\n                            1f,", add)

    def test_button_still_allows_two_lines(self):
        src = ACT.read_text(encoding="utf-8")
        button = src.split("private fun pinnedPartButton(", 1)[1].split("setOnClickListener", 1)[0]
        self.assertIn("maxLines = 2", button)


if __name__ == "__main__":
    unittest.main()
