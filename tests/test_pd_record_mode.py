"""Phone Diagnostics 36/37 — «Режим запису» (faric 2026-10-20 01:05).

Функції запускаємо по-справжньому з несправжнім `adb`, який веде стан телефона у файлі:
36 міняє лише те, що записав у файл стану; 37 повертає рівно як було.
"""
import json
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
import json, os, sys
db = os.environ["FAKE_PHONE"]
st = json.load(open(db))
args = sys.argv[1:]
if args[:1] == ["-s"]:
    args = args[2:]
if args[:1] != ["shell"]:
    sys.exit(0)
cmd = " ".join(args[1:]).split()
log = open(db + ".log", "a"); log.write(" ".join(cmd) + "\n"); log.close()
out = ""
code = 0
def save():
    json.dump(st, open(db, "w"))
if cmd[:2] == ["getprop", "ro.serialno"]: out = "R58M30S"
elif cmd[:2] == ["getprop", "ro.product.model"]: out = "SM-M307FN"
elif cmd[:2] == ["pm", "path"]:
    code = 0 if cmd[2] in st["installed"] else 1
    out = "package:/data/app/x/base.apk" if code == 0 else ""
elif cmd[:3] == ["pm", "list", "packages"]:
    if "-e" in cmd: out = "\n".join("package:" + p for p in st["installed"] if p not in st["disabled"])
    elif "-d" in cmd: out = "\n".join("package:" + p for p in st["disabled"])
    else: out = "\n".join("package:" + p for p in st["installed"])
elif cmd[:2] == ["pm", "disable-user"]:
    if cmd[-1] not in st.get("refuse", []):
        st["disabled"].append(cmd[-1]); save()
    out = "new state: disabled-user"
elif cmd[:2] == ["pm", "enable"]:
    st["disabled"] = [p for p in st["disabled"] if p != cmd[-1]]; save(); out = "new state: enabled"
elif cmd[:3] == ["cmd", "appops", "get"]:
    v = st["appops"].get(cmd[3])
    out = ("RUN_ANY_IN_BACKGROUND: %s" % v) if v else "No operations."
elif cmd[:3] == ["cmd", "appops", "set"]:
    if cmd[5] == "default": st["appops"].pop(cmd[3], None)
    else: st["appops"][cmd[3]] = cmd[5]
    save()
elif cmd[:2] == ["am", "get-standby-bucket"]: out = str(st["bucket"].get(cmd[2], 10))
elif cmd[:2] == ["am", "set-standby-bucket"]:
    names = {"active": 10, "working_set": 20, "frequent": 30, "rare": 40, "restricted": 45}
    st["bucket"][cmd[2]] = names[cmd[3]]; save()
elif cmd[:2] == ["am", "force-stop"]: st["stopped"].append(cmd[2]); save()
elif cmd[:3] == ["settings", "get", "global"]: out = str(st["zen"])
elif cmd[:3] == ["cmd", "notification", "set_dnd"]:
    st["zen"] = {"off": 0, "priority": 1, "none": 2, "alarms": 3}[cmd[3]]; save()
elif cmd[:1] == ["pidof"]: code = 1
if out: print(out)
sys.exit(code)
'''

START = {
    "installed": ["ua.faric.trackcatcher", "com.suno.android", "com.maxmpz.audioplayer", "com.aimp.player",
                  "com.zhiliaoapp.musically", "com.estrongs.android.pop", "com.android.vending",
                  "com.anthropic.claude", "com.samsung.android.app.watchmanager2", "com.samsung.android.app.sharelive",
                  "com.samsung.android.scloud", "com.facebook.services", "com.whatsapp", "org.telegram.messenger",
                  "com.termux", "com.moonshot.kimichat", "com.samsung.android.game.gos", "com.google.android.gm"],
    "refuse": ["com.samsung.android.game.gos"],
    "disabled": ["com.moonshot.kimichat"],
    "appops": {"com.estrongs.android.pop": "ignore"},
    "bucket": {"com.estrongs.android.pop": 40, "com.zhiliaoapp.musically": 20, "com.android.vending": 10},
    "zen": 1,
    "stopped": [],
}


def block():
    text = MENU.read_text(encoding="utf-8")
    m = re.search(r"# record_mode start\n(.*?)# record_mode end\n", text, re.S)
    assert m
    labels = re.search(r"(package_label\(\) \{.*?\n\}\n)", text, re.S).group(1)
    return labels + m.group(1)


class RecordModeTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp(prefix="pd36-"))
        (self.tmp / "bin").mkdir()
        adb = self.tmp / "bin" / "adb"
        adb.write_text(FAKE_ADB)
        adb.chmod(0o755)
        self.db = self.tmp / "phone.json"
        self.db.write_text(json.dumps(START))
        (self.tmp / "state").mkdir()

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)

    def run_fn(self, fn, answers):
        script = (
            "set -u\n"
            "require_connection() { ADB_TARGET=192.168.0.103:5555; }\n"
            "new_log() { CURRENT_LOG=\"$STATE_DIR/log.txt\"; : > \"$CURRENT_LOG\"; }\n"
            "pause_menu() { :; }\nclear() { :; }\ngetprop() { echo SM-A266B; }\n"
            + block() + fn + "\n"
        )
        env = dict(os.environ, PATH=str(self.tmp / "bin") + ":" + os.environ["PATH"],
                   FAKE_PHONE=str(self.db), STATE_DIR=str(self.tmp / "state"))
        r = subprocess.run(["bash", "-c", script], input=answers, text=True, capture_output=True, env=env, timeout=60)
        self.assertEqual(r.returncode, 0, r.stderr)
        return r.stdout

    def phone(self):
        return json.loads(self.db.read_text())

    def test_on_then_off_restores_exactly(self):
        out = self.run_fn("record_mode_on", "1\nтак\n")
        self.assertIn("режим запису увімкнено", out)
        p = self.phone()
        self.assertEqual(p["zen"], 3)
        for pkg in ("com.samsung.android.app.watchmanager2", "com.samsung.android.app.sharelive",
                    "com.samsung.android.scloud", "com.facebook.services"):
            self.assertIn(pkg, p["disabled"])
        self.assertEqual(p["appops"]["com.zhiliaoapp.musically"], "ignore")
        self.assertEqual(p["bucket"]["com.android.vending"], 45)
        self.assertIn("com.anthropic.claude", p["stopped"])
        self.assertIn("com.google.android.gm", p["stopped"])
        self.assertIn("телефон не дав вимкнути", out)
        self.assertNotIn("com.samsung.android.game.gos", p["disabled"])
        self.assertIn("com.maxmpz.audioplayer", p["stopped"])  # інший плеєр
        for keep in ("com.suno.android", "ua.faric.trackcatcher", "com.termux", "com.whatsapp", "org.telegram.messenger"):
            self.assertNotIn(keep, p["stopped"])
            self.assertNotIn(keep, p["disabled"])
        # Повторно 36 — не дає перезаписати «як було».
        self.assertIn("вже увімкнено", self.run_fn("record_mode_on", "1\nтак\n"))

        out = self.run_fn("record_mode_off", "")
        self.assertIn("усе повернуто", out)
        p = self.phone()
        self.assertEqual(p["zen"], 1)
        self.assertEqual(p["disabled"], ["com.moonshot.kimichat"])  # Kimi вимкнув сам faric — лишився
        self.assertEqual(p["appops"].get("com.estrongs.android.pop"), "ignore")
        self.assertNotIn("com.zhiliaoapp.musically", p["appops"])
        self.assertEqual(p["bucket"]["com.zhiliaoapp.musically"], 20)
        self.assertEqual(p["bucket"]["com.android.vending"], 10)
        self.assertEqual(p["bucket"]["com.estrongs.android.pop"], 40)
        states = list((self.tmp / "state").glob("record-mode-*"))
        self.assertTrue(any(".done-" in s.name for s in states))
        self.assertFalse(any(s.name.endswith(".state") for s in states))

    def test_cancel_changes_nothing(self):
        self.run_fn("record_mode_on", "1\nні\n")
        self.assertEqual(self.phone(), START)
        self.assertIn("не вмикався", self.run_fn("record_mode_off", ""))

    def test_never_uninstalls_or_clears(self):
        src = block()
        for forbidden in ("pm uninstall", "pm clear", "trim-caches", "rm -", "reboot"):
            self.assertNotIn(forbidden, src)


class RecordModeMenuTest(unittest.TestCase):
    def test_wired(self):
        text = MENU.read_text(encoding="utf-8")
        self.assertIn("    36) record_mode_on ;;", text)
        self.assertIn("    37) record_mode_off ;;", text)


if __name__ == "__main__":
    unittest.main()
