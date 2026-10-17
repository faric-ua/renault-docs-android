"""v0.5.146 — TODO №21 (owner 2026-10-17 04:19, 04:42, video 04:43): the «unknown how long»
bar flickered at the left edge of the collapsed «Підготовка тому» card «like a flag in the
wind». Now a stripe runs across the whole bar at changing speed (like YTM), in every
operation card that uses SharedOperationProgressBar.
"""

import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


def source(name: str) -> str:
    return (SRC / name).read_text(encoding="utf-8")


class SweepProgressContract(unittest.TestCase):
    def test_shared_bar_uses_the_sweep(self):
        bar = source("SharedOperationProgressBar.kt")
        self.assertIn("indeterminateDrawable = SweepProgressDrawable(Ui.success, Ui.border)", bar)
        self.assertIn("as? SweepProgressDrawable", bar)

    def test_drawable_animates_itself(self):
        drawable = source("SweepProgressDrawable.kt")
        self.assertIn("Drawable(), Animatable", drawable)
        self.assertIn("ValueAnimator.INFINITE", drawable)
        self.assertIn("SweepProgressMath.stripes(phase)", drawable)
        self.assertIn("fun ease(", source("SweepProgressMath.kt"))

    def test_version(self):
        gradle = (ROOT / "android/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertGreaterEqual(int(gradle.split("versionCode = ")[1].split()[0]), 162)


if __name__ == "__main__":
    unittest.main()
