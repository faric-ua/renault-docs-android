"""Phone Diagnostics 42 — перевірка під час завантаження (запит чату Dorama T326, 03.11.2026).

Несправжній adb віддає два знімки /proc/net/dev і /proc/diskstats: за 1 с через Wi-Fi пройшло 0,5 МБ,
на флешку (mmcblk1, тип SD) записано 10 МБ і вона зайнята 95 % часу. Контрольний curl — 4 МБ/с.
Перевіряємо цифри і висновок: вузьке місце — флешка.
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
cmd = " ".join(args[1:]) if args[:1] == ["shell"] else ""
cnt = os.environ["CNT"]
if "PDSNAP" in cmd:
    n = int(open(cnt).read()) if os.path.exists(cnt) else 0
    open(cnt, "w").write(str(n + 1))
    rx = 1000000 + n * 524288
    print("PDSNAP")
    print("Inter-|   Receive")
    print(f" wlan0: {rx} 100 0 0 0 0 0 0 5000 50 0 0 0 0 0 0")
    print("PDDISK")
    print(f" 179 0 mmcblk0 10 0 {100 + n*2048} 5 20 0 {200} 9 0 {100 + n*100} 10 0 0 0 0")
    print(f" 179 128 mmcblk1 10 0 100 5 20 0 {200 + n*20480} 9 3 {100 + n*950} 10 0 0 0 0")
elif "/sys/block" in cmd:
    print("mmcblk0 MMC"); print("mmcblk1 SD")
elif "wifi" in cmd:
    print("Wifi is connected to \"home\"\nWifiInfo: SSID: \"home\", RSSI: -67, Link speed: 72Mbps, Frequency: 2437MHz")
elif "top" in cmd:
    print("18341 u0_a445 20 0 6.4G 260M 90M S 20.0 7.2 1:58.50 ua.faric.doramaland.video.dev")
elif "meminfo" in cmd:
    print("MemTotal: 3692144 kB\nMemAvailable: 512000 kB\nSwapTotal: 3145724 kB\nSwapFree: 1906732 kB")
elif "battery" in cmd:
    print("  temperature: 356")
elif "getprop" in cmd:
    print("SM-M307FN")
sys.exit(0)
'''


class LoadCheckTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp(prefix="pd42-"))
        (self.tmp / "bin").mkdir()
        for name, body in (("adb", FAKE_ADB), ("curl", "#!/bin/sh\nprintf 4194304\n")):
            f = self.tmp / "bin" / name
            f.write_text(body)
            f.chmod(0o755)

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)

    def test_flash_is_bottleneck(self):
        text = MENU.read_text(encoding="utf-8")
        rec = re.search(r"(rec_adb\(\) \{.*?\n\}\n)", text, re.S).group(1)
        body = re.search(r"# load_check start\n(.*?)# load_check end\n", text, re.S).group(1)
        script = ("set -u\nclear() { :; }\npause_menu() { echo PAUSE; }\n"
                  "load_target() { echo 192.168.0.106:5555; }\nrequire_connection() { :; }\n"
                  f"new_log() {{ CURRENT_LOG='{self.tmp}/log.txt'; : > \"$CURRENT_LOG\"; }}\n"
                  + rec + body + "load_check\n")
        (self.tmp / "item.sh").write_text(script)
        env = dict(os.environ, PATH=f"{self.tmp}/bin:" + os.environ["PATH"], CNT=str(self.tmp / "cnt"),
                   PD_LOADCHECK_SEC="1")
        out = subprocess.run(["bash", str(self.tmp / "item.sh")], capture_output=True, text=True,
                             env=env, timeout=60).stdout
        self.assertIn("72 Мбіт/с", out)
        self.assertIn("-67 дБм", out)
        self.assertIn("2437 МГц", out)
        self.assertIn("Wi-Fi (Dorama та все інше): 0.50 МБ/с", out)
        self.assertIn("Контрольний файл (curl на ЦЬОМУ телефоні, той самий час): 4.00 МБ/с", out)
        self.assertIn("mmcblk1 (ФЛЕШКА): читання 0.0 МБ/с · запис 10.0 МБ/с · зайнята 95 %", out)
        self.assertIn("mmcblk0 (пам'ять телефона)", out)
        self.assertIn("Dorama: процесор 20.0 %", out)
        self.assertIn("Вільна пам'ять: 500 МБ", out)
        self.assertIn("Вузьке місце: ФЛЕШКА", out)
        self.assertIn("мережа 0.50 МБ/с (контрольний файл 4.00 МБ/с) · флешка запис 10.0 МБ/с, зайнята 95 %", out)

    def test_menu_lists_item(self):
        text = MENU.read_text(encoding="utf-8")
        self.assertIn('echo "42 — Перевірка під час завантаження', text)
        self.assertIn("    42) load_check ;;", text)


if __name__ == "__main__":
    unittest.main()
