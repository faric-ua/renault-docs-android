"""Phone Diagnostics 43 — чому не встановилось оновлення (запит чату Dorama T328, 04.11.2026).

Несправжній adb: Dorama (dev) на телефоні, два установщики, журнал з Permission Denial — або без помилок,
але без дозволу «невідомі застосунки». Перевіряємо зібрані дані і висновок одним рядком.
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
args = sys.argv[1:]
if args[:1] == ["-s"]:
    args = args[2:]
cmd = " ".join(args[1:])
open(os.environ["ADB_LOG"], "a").write(cmd + "\n")
if "getprop" in cmd:
    print("SM-M307FN")
elif "pm list packages" in cmd:
    print("package:ua.faric.doramaland.video.dev")
elif "appops get" in cmd:
    print("REQUEST_INSTALL_PACKAGES: " + os.environ.get("OPS", "allow"))
elif "query-activities" in cmd:
    print("2 activities found:")
    print("  com.google.android.packageinstaller/com.android.packageinstaller.InstallStart")
    print("  com.samsung.android.packageinstaller/.InstallStart")
elif "resolve-activity" in cmd:
    print("android/com.android.internal.app.ResolverActivity")
elif "date" in cmd:
    print("11-04 00:10:00.000")
elif "logcat" in cmd:
    print("11-04 00:10:05.100 I/ActivityTaskManager( 1000): START u0 {act=android.intent.action.VIEW typ=application/vnd.android.package-archive} from uid 10445")
    print("11-04 00:10:06.200 I/ActivityTaskManager( 1000): Displayed com.android.internal.app.ResolverActivity")
    print("11-04 00:10:07.300 D/Wifi( 999): noise line")
    if os.environ.get("DENY"):
        print("11-04 00:10:08.400 W/UriGrantsManager( 1000): Permission Denial: opening provider androidx.core.content.FileProvider from ProcessRecord{ com.google.android.packageinstaller}")
    print("11-04 00:10:09.000 I/PackageInstaller( 2222): InstallStart finished")
sys.exit(0)
'''


class InstallWatchTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp(prefix="pd43-"))
        (self.tmp / "bin").mkdir()
        f = self.tmp / "bin" / "adb"
        f.write_text(FAKE_ADB)
        f.chmod(0o755)

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)

    def run_item(self, **extra):
        text = MENU.read_text(encoding="utf-8")
        rec = re.search(r"(rec_adb\(\) \{.*?\n\}\n)", text, re.S).group(1)
        body = re.search(r"# install_watch start\n(.*?)# install_watch end\n", text, re.S).group(1)
        script = ("set -u\nclear() { :; }\npause_menu() { echo PAUSE; }\n"
                  "load_target() { echo 192.168.0.106:5555; }\nrequire_connection() { :; }\n"
                  f"new_log() {{ CURRENT_LOG='{self.tmp}/log.txt'; : > \"$CURRENT_LOG\"; }}\n"
                  + rec + body + "install_watch\n")
        (self.tmp / "item.sh").write_text(script)
        env = dict(os.environ, PATH=f"{self.tmp}/bin:" + os.environ["PATH"], ADB_LOG=str(self.tmp / "adb.log"), **extra)
        out = subprocess.run(["bash", str(self.tmp / "item.sh")], input="\n", capture_output=True, text=True,
                             env=env, timeout=60).stdout
        return out, (self.tmp / "log.txt").read_text()

    def test_permission_denial(self):
        out, log = self.run_item(DENY="1")
        self.assertIn("ua.faric.doramaland.video.dev: REQUEST_INSTALL_PACKAGES: allow", out)
        self.assertIn("com.samsung.android.packageinstaller/.InstallStart", out)
        self.assertIn("ResolverActivity", out)
        self.assertIn("Permission Denial", log)
        self.assertNotIn("noise line", log)
        self.assertIn("Що закрило установщик: установщик не отримав доступу до файлу", out)
        adb_log = (self.tmp / "adb.log").read_text()
        self.assertIn("logcat -d -v time -T '11-04 00:10:00.000'", adb_log)
        self.assertNotIn("logcat -c", adb_log)

    def test_no_unknown_sources_permission(self):
        out, _ = self.run_item(OPS="default")
        self.assertIn("у Dorama нема дозволу «невідомі застосунки»", out)

    def test_menu_lists_item(self):
        text = MENU.read_text(encoding="utf-8")
        self.assertIn('echo "43 — Чому не встановилось оновлення', text)
        self.assertIn("    43) install_watch ;;", text)


if __name__ == "__main__":
    unittest.main()
