"""v0.5.103: converter progress moves live (phone QA v0.5.102, faric).

Before: scan updated every 500/100 files and validation every 250 files,
so the green bar jumped. Now scan/validation report by time (80 ms) and the
service persists at most every 100 ms, matching the Activity refresh.
"""
from pathlib import Path
import unittest

RETIRED_V05112 = "v0.5.112: «Конвертер»/«Legacy»/«Старі бібліотеки» removed by owner decision 2026-10-12; code kept in branch archive/legacy-converter-v0.5.111"

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


class LiveConverterProgressContract(unittest.TestCase):
    @unittest.skip(RETIRED_V05112)
    def test_engine_reports_by_time_not_file_count(self):
        engine = (ANDROID / "SafConversionEngine.kt").read_text(encoding="utf-8")
        self.assertIn("private fun progressDue(): Boolean", engine)
        self.assertIn("PROGRESS_INTERVAL_MS =\n            80L", engine)
        self.assertGreaterEqual(engine.count("progressDue()"), 4)
        for old in ("FAST_SCAN_PROGRESS_EVERY", "FALLBACK_SCAN_PROGRESS_EVERY", "VALIDATION_PROGRESS_EVERY"):
            self.assertNotIn(old, engine)
        # The last validated file is always reported, so the bar reaches 100%.
        self.assertIn("progressDue() ||\n                            count ==\n                                expectedSourceFiles", engine)

    @unittest.skip(RETIRED_V05112)
    def test_service_persists_progress_at_activity_refresh_rate(self):
        service = (ANDROID / "ConversionService.kt").read_text(encoding="utf-8")
        activity = (ANDROID / "ConversionActivity.kt").read_text(encoding="utf-8")
        self.assertIn("PROGRESS_STORE_THROTTLE_MS =\n            100L", service)
        self.assertIn("REFRESH_INTERVAL_MS =\n            100L", activity)


if __name__ == "__main__":
    unittest.main()
