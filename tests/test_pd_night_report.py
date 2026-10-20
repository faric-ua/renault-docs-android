"""Phone Diagnostics 38 — «Що було за ніч» (faric 2026-10-20 14:42).

Пункт лише читає: з несправжнім `adb` перевіряємо, що жодна команда не міняє телефон,
а у звіті є всі розділи.
"""
import os
import re
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MENU = ROOT / "tools" / "termux" / "phone-diagnostics-menu.sh"

FAKE_ADB = r'''#!/usr/bin/env python3
import os, sys
try:
    sys.stdin.read()
except Exception:
    pass
args = sys.argv[1:]
if args[:1] == ["-s"]:
    args = args[2:]
open(os.environ["ADB_LOG"], "a").write(" ".join(args) + "\n")
cmd = args[1:] if args[:1] == ["shell"] else args
if cmd[:2] == ["pm", "path"]:
    sys.exit(0 if cmd[2] in ("com.suno.android", "ua.faric.trackcatcher") else 1)
if cmd[:3] == ["dumpsys", "activity", "exit-info"]:
    print("  ApplicationExitInfo #0:\n    timestamp=2026-10-20 07:12:03.1 pid=9295 realUid=10422\n    reason=3 (LOW_MEMORY) subreason=0\n    description=lmk")
if cmd[:1] == ["pidof"]:
    sys.exit(1)
sys.exit(0)
'''

MUTATING = re.compile(r"\b(disable-user|enable|force-stop|set-standby-bucket|appops set|set_dnd|settings put|uninstall|install|pm clear|logcat -c|rm )")


class NightReportTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp(prefix="pd38-"))
        (self.tmp / "bin").mkdir()
        adb = self.tmp / "bin" / "adb"
        adb.write_text(FAKE_ADB)
        adb.chmod(0o755)

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)

    def test_read_only_and_all_sections(self):
        text = MENU.read_text(encoding="utf-8")
        rec = re.search(r"(rec_adb\(\) \{.*?\n\}\n)", text, re.S).group(1)
        has = re.search(r"(rec_has\(\) \{.*?\n\}\n)", text, re.S).group(1)
        night = re.search(r"# night_report start\n(.*?)# night_report end\n", text, re.S).group(1)
        log = self.tmp / "adb.log"
        script = (
            "set -u\n"
            "require_connection() { ADB_TARGET=192.168.0.103:5555; }\n"
            f"new_log() {{ CURRENT_LOG='{self.tmp}/log.txt'; : > \"$CURRENT_LOG\"; }}\n"
            "pause_menu() { :; }\nclear() { :; }\n"
            + rec + has + night + "night_report\n"
        )
        env = dict(os.environ, PATH=str(self.tmp / "bin") + ":" + os.environ["PATH"], ADB_LOG=str(log))
        r = subprocess.run(["bash", "-c", script], input="\n", text=True, capture_output=True, env=env, timeout=60)
        self.assertEqual(r.returncode, 0, r.stderr)
        for section in ("Чому закривались", "Вбивства", "Тиск пам'яті за 12 год", "Нагрів",
                        "Обриви звуку", "Wi-Fi", "Track Catcher про себе"):
            self.assertIn(section, r.stdout)
        self.assertIn("LOW_MEMORY", r.stdout)
        calls = log.read_text()
        self.assertIn("exit-info com.suno.android", calls)
        self.assertNotIn("exit-info com.aimp.player", calls)  # не встановлений — пропущено
        for line in calls.splitlines():
            self.assertIsNone(MUTATING.search(line), line)

    def test_menu_lists_and_dispatches_38(self):
        text = MENU.read_text(encoding="utf-8")
        self.assertIn('echo "38 — Що було за ніч', text)
        self.assertIn("38) night_report ;;", text)


if __name__ == "__main__":
    unittest.main()
