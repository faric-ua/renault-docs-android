"""Phone Diagnostics 41 — що за флешка (faric 2026-11-03: «щоб я не розбирав телефон»).

Несправжній adb: «телефон» — тека з /sys/block/mmcblk1 (карта SanDisk 512 ГБ) і /storage/ABCD-1234.
Перевіряємо: паспорт прочитано словами, об'єм пораховано, без «так» нічого не пишеться,
з «так» — замір є, а тимчасовий файл прибрано.
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
import os, subprocess, sys
dev = os.environ["DEV"]
args = sys.argv[1:]
if args[:1] == ["-s"]:
    args = args[2:]
open(os.environ["ADB_LOG"], "a").write(" ".join(args) + "\n")
def real(p):
    return p.replace("/sys/", dev + "/sys/").replace("/storage/", dev + "/storage/")
if args == ["get-state"]:
    print("device"); sys.exit(0)
if args[:1] == ["shell"]:
    out = subprocess.run(["sh", "-c", real(" ".join(args[1:]))], capture_output=True, text=True).stdout
    sys.stdout.write(out.replace(dev, "")); sys.exit(0)
sys.exit(0)
'''


class SdCardTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp(prefix="pd41-"))
        (self.tmp / "bin").mkdir()
        adb = self.tmp / "bin" / "adb"
        adb.write_text(FAKE_ADB)
        adb.chmod(0o755)
        dev = self.tmp / "dev"
        emmc = dev / "sys" / "block" / "mmcblk0" / "device"
        emmc.mkdir(parents=True)
        (emmc / "type").write_text("MMC\n")
        card = dev / "sys" / "block" / "mmcblk1"
        (card / "device").mkdir(parents=True)
        for k, v in {"type": "SD", "name": "SN512", "manfid": "0x000003", "oemid": "0x5344", "date": "06/2023"}.items():
            (card / "device" / k).write_text(v + "\n")
        (card / "size").write_text(str(1000000000 * 512 // 512) + "\n")  # 512 ГБ
        self.sd = dev / "storage" / "ABCD-1234"
        self.sd.mkdir(parents=True)

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)

    def run_item(self, answers):
        text = MENU.read_text(encoding="utf-8")
        rec = re.search(r"(rec_adb\(\) \{.*?\n\}\n)", text, re.S).group(1)
        body = re.search(r"# sd_card_info start\n(.*?)# sd_card_info end\n", text, re.S).group(1)
        script = (
            "set -u\n"
            "clear() { :; }\n"
            "pause_menu() { echo PAUSE; }\n"
            "load_target() { echo 192.168.0.106:5555; }\n"
            "require_connection() { :; }\n"
            f"new_log() {{ CURRENT_LOG='{self.tmp}/log.txt'; : > \"$CURRENT_LOG\"; }}\n"
            + rec + body + "sd_card_info\n"
        )
        (self.tmp / "item.sh").write_text(script)
        env = dict(os.environ, PATH=f"{self.tmp}/bin:" + os.environ["PATH"],
                   ADB_LOG=str(self.tmp / "adb.log"), DEV=str(self.tmp / "dev"))
        return subprocess.run(["bash", str(self.tmp / "item.sh")], input=answers,
                              capture_output=True, text=True, env=env, timeout=60).stdout

    def test_passport_without_writing(self):
        out = self.run_item("\n")
        self.assertIn("SanDisk", out)
        self.assertIn("SN512", out)
        self.assertIn("06/2023", out)
        self.assertIn("512 ГБ", out)
        self.assertIn("Замір пропущено", out)
        self.assertNotIn("dd if=", (self.tmp / "adb.log").read_text())

    def test_speed_test_cleans_up(self):
        out = self.run_item("так\n")
        self.assertRegex(out, r"Запис: \d+ МБ/с")
        self.assertEqual(list(self.sd.iterdir()), [])

    def test_menu_lists_item(self):
        text = MENU.read_text(encoding="utf-8")
        self.assertIn('echo "41 — Що за флешка', text)
        self.assertIn("    41) sd_card_info ;;", text)


if __name__ == "__main__":
    unittest.main()
