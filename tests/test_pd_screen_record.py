"""Phone Diagnostics 39 — запис екрана шматками (faric 2026-10-29, з чату Track Catcher).

`adb shell screenrecord` не просить дозволу «Запис екрана», тож не заважає Track Catcher.
З несправжнім `adb` перевіряємо: шматки йдуть по порядку _001, _002…, кожен з
обмеженням часу, Enter зупиняє (сигнал INT, щоб файл закрився правильно),
а без «так» нічого не починається.
"""
import re
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MENU = ROOT / "tools" / "termux" / "phone-diagnostics-menu.sh"

FAKE_ADB = r'''#!/usr/bin/env python3
import os, sys, time
args = sys.argv[1:]
if args[:1] == ["-s"]:
    args = args[2:]
open(os.environ["ADB_LOG"], "a").write(" ".join(args) + "\n")
if args == ["get-state"]:
    print("device"); sys.exit(0)
cmd = args[1:] if args[:1] == ["shell"] else args
if cmd[:1] == ["getprop"]:
    print("SM-A266B"); sys.exit(0)
if cmd[:1] == ["df"]:
    print("Filesystem 1K-blocks Used Available Use% Mounted on")
    print("/data/media 100000000 50000000 40000000 50% /storage/emulated"); sys.exit(0)
if cmd[:1] == ["screenrecord"]:
    stop = os.environ["STOP_MARK"]
    for _ in range(64):
        if os.path.exists(stop):
            break
        time.sleep(0.05)
    with open(os.environ["FILES"], "a") as f:
        f.write(cmd[-1].rsplit("/", 1)[-1] + "\n")
    sys.exit(0)
if cmd and cmd[0].startswith("kill -INT"):
    open(os.environ["STOP_MARK"], "w").write("x"); sys.exit(0)
if cmd[:1] == ["ls"]:
    names = open(os.environ["FILES"]).read().split() if os.path.exists(os.environ["FILES"]) else []
    for n in names:
        print(("-rw-rw---- 1 u g 1000 2026-10-29 14:50 " + n) if "-l" in cmd else n)
    sys.exit(0)
sys.exit(0)
'''


class ScreenRecordTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp(prefix="pd39-"))
        (self.tmp / "bin").mkdir()
        (self.tmp / "state").mkdir()
        adb = self.tmp / "bin" / "adb"
        adb.write_text(FAKE_ADB)
        adb.chmod(0o755)

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)

    def run_item(self, stdin_script):
        text = MENU.read_text(encoding="utf-8")
        rec = re.search(r"(rec_adb\(\) \{.*?\n\}\n)", text, re.S).group(1)
        body = re.search(r"# screen_record start\n(.*?)# screen_record end\n", text, re.S).group(1)
        script = (
            "set -u\n"
            f"STATE_DIR='{self.tmp}/state'\n"
            "PD_SREC_SECONDS=180\n"
            "clear() { :; }\n"
            "pause_menu() { echo PAUSE; }\n"
            "require_connection() { ADB_TARGET=192.168.50.5:5555; }\n"
            f"new_log() {{ CURRENT_LOG='{self.tmp}/log.txt'; : > \"$CURRENT_LOG\"; }}\n"
            + rec + body + "screen_record_chunks\n"
        )
        (self.tmp / "item.sh").write_text(script)
        env = {
            "PATH": f"{self.tmp}/bin:/usr/bin:/bin",
            "ADB_LOG": str(self.tmp / "adb.log"),
            "STOP_MARK": str(self.tmp / "stopped"),
            "FILES": str(self.tmp / "files"),
            "HOME": str(self.tmp),
        }
        feeder = subprocess.Popen(["bash", "-c", stdin_script], stdout=subprocess.PIPE)
        out = subprocess.run(["bash", str(self.tmp / "item.sh")], stdin=feeder.stdout,
                             capture_output=True, text=True, env=env, timeout=60)
        feeder.wait()
        adb_log = (self.tmp / "adb.log").read_text() if (self.tmp / "adb.log").exists() else ""
        return out.stdout, adb_log

    def test_chunks_in_order_and_enter_stops(self):
        out, adb_log = self.run_item("echo y; sleep 5.5; echo")
        rec = [l for l in adb_log.splitlines() if " screenrecord " in f" {l} "]
        self.assertGreaterEqual(len(rec), 2, adb_log)
        for i, line in enumerate(rec, 1):
            self.assertIn("--time-limit 180", line)
            self.assertIn("--bit-rate 6000000", line)
            self.assertRegex(line, rf"/sdcard/Movies/PhoneDiagnostics/screen-\d{{8}}-\d{{4}}_{i:03d}\.mp4$")
        self.assertIn("kill -INT $(pidof screenrecord)", adb_log)
        self.assertIn("Шматків:", out)
        self.assertIn("_001.mp4", out)
        self.assertFalse((self.tmp / "state" / "screen-record.stop").exists())
        self.assertIn("PAUSE", out)

    def test_no_without_yes(self):
        out, adb_log = self.run_item("echo")
        self.assertIn("Не починаю", out)
        self.assertNotIn("screenrecord", adb_log)

    def test_menu_lists_item(self):
        text = MENU.read_text(encoding="utf-8")
        self.assertIn('echo "39 — Запис екрана шматками', text)
        self.assertIn("    39) screen_record_chunks ;;", text)


if __name__ == "__main__":
    unittest.main()
