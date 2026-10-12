"""v0.5.108: detailed converter help (owner, 2026-10-12).

The «?» window on «Конвертація Renault» said only two sentences. The owner
asked to explain what the converter is for, how it works and why: Windows
Renault documentation (HTML pages) → a copy for Android that opens in a
browser like the original, with fixed links, no app needed.
"""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
ACTIVITY = ROOT / "android/app/src/main/java/com/saney/renaultdocs/ConversionActivity.kt"


class ConverterHelpContract(unittest.TestCase):
    def setUp(self):
        text = ACTIVITY.read_text(encoding="utf-8")
        block = text.split("HELP_CONVERTER ->", 1)[1].split("else ->", 1)[0]
        self.help = "".join(re.findall(r'"((?:[^"\\]|\\.)*)"', block))

    def test_explains_purpose_choice_steps_and_safety(self):
        for section in ("НАВІЩО ЦЕ", "ЩО ВИБРАТИ", "ЯК ЦЕ ПРАЦЮЄ", "БЕЗПЕКА"):
            self.assertIn(section, self.help)

    def test_key_facts(self):
        for fact in (
            "Windows не розрізняє великі й малі літери",
            "без переробки «на новий лад»",
            "відкривається в браузері так само, як оригінал на Windows",
            "Застосунок для цього не потрібен",
            "Не сама папка тому!",
            "…_android",
            "Стара папка тільки читається",
            "недороблена копія не з'являється",
        ):
            self.assertIn(fact, self.help)

    def test_no_english_words(self):
        words = re.findall(r"(?<![-\w./])(source|destination|output|staging|dataset|merge)(?![-.\w])", self.help, re.IGNORECASE)
        self.assertEqual([], words)

    def test_version_bumped(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        code = int(gradle.split("versionCode = ", 1)[1].splitlines()[0].strip())
        self.assertGreaterEqual(code, 124)


if __name__ == "__main__":
    unittest.main()
