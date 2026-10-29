"""Phone Diagnostics 40 — склеїти шматки пункту 39 + звук пісні з Track Catcher (faric 2026-10-29).

Справжній ffmpeg, несправжній adb («телефон» — тека). Три шматки по 2 с з пропусками по 1 с,
пісня 3 с з пробілом у назві. Перевіряємо: одне відео 6 с, звук рівно там, де пісня
звучала під час запису (пропуски між шматками звук не зсувають), решта — тиша.
"""
import array
import os
import re
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MENU = ROOT / "tools" / "termux" / "phone-diagnostics-menu.sh"
HAVE_FFMPEG = shutil.which("ffmpeg") and shutil.which("ffprobe")

FAKE_ADB = r'''#!/usr/bin/env python3
import os, shutil, subprocess, sys
dev = os.environ["DEV"]
args = sys.argv[1:]
if args[:1] == ["-s"]:
    args = args[2:]
open(os.environ["ADB_LOG"], "a").write(" ".join(args) + "\n")
def real(p):
    return p.replace("/sdcard", dev + "/sdcard")
if args == ["get-state"]:
    print("device"); sys.exit(0)
if args[:1] == ["pull"]:
    shutil.copy(real(args[1]), args[2]); sys.exit(0)
if args[:1] == ["shell"]:
    rest = args[1:]
    if len(rest) == 1:
        out = subprocess.run(["sh", "-c", real(rest[0])], capture_output=True, text=True).stdout
        sys.stdout.write(out.replace(dev + "/sdcard", "/sdcard")); sys.exit(0)
    if rest[:1] == ["ls"]:
        for n in sorted(os.listdir(real(rest[1]))):
            print(n)
        sys.exit(0)
sys.exit(0)
'''


def make(cmd):
    subprocess.run(["ffmpeg", "-nostdin", "-y", "-v", "error"] + cmd, check=True)


@unittest.skipUnless(HAVE_FFMPEG, "ffmpeg недоступний")
class ScreenGlueTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp(prefix="pd40-"))
        (self.tmp / "bin").mkdir()
        adb = self.tmp / "bin" / "adb"
        adb.write_text(FAKE_ADB)
        adb.chmod(0o755)
        self.movies = self.tmp / "dev" / "sdcard" / "Movies" / "PhoneDiagnostics"
        self.music = self.tmp / "dev" / "sdcard" / "Music" / "Track Catcher"
        self.movies.mkdir(parents=True)
        self.music.mkdir(parents=True)
        base = 1_800_000_000
        # шматки: 0–2, 3–5, 6–8 с (пропуски по 1 с); час файлу = кінець шматка
        for i, end in enumerate((2, 5, 8), 1):
            f = self.movies / f"screen-20261029-1448_{i:03d}.mp4"
            make(["-f", "lavfi", "-i", "testsrc=size=160x120:rate=25", "-t", "2",
                  "-c:v", "libx264", "-pix_fmt", "yuv420p", str(f)])
            os.utime(f, (base + end, base + end))
        # пісня звучала з 4-ї по 7-му секунду
        # Track Catcher кладе пісню в підпапку альбому (faric 2026-10-29 15:17 — так її й не знайшло)
        (self.music / "VOL. 2 — NIGHT").mkdir()
        song = self.music / "VOL. 2 — NIGHT" / "Виконавець — Пісня (2).mp3"
        make(["-f", "lavfi", "-i", "sine=frequency=440:sample_rate=48000", "-t", "3",
              "-ac", "2", str(song)])
        os.utime(song, (base + 7, base + 7))
        old = self.music / "Стара пісня.mp3"
        make(["-f", "lavfi", "-i", "sine=frequency=300", "-t", "2", str(old)])
        os.utime(old, (base - 5000, base - 5000))
        self.local = self.tmp / "local-movies"

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)

    def run_item(self, answers):
        text = MENU.read_text(encoding="utf-8")
        rec = re.search(r"(rec_adb\(\) \{.*?\n\}\n)", text, re.S).group(1)
        body = re.search(r"# screen_glue start\n(.*?)# screen_glue end\n", text, re.S).group(1)
        script = (
            "set -u\n"
            'SREC_DIR="/sdcard/Movies/PhoneDiagnostics"\n'
            "clear() { :; }\n"
            "pause_menu() { echo PAUSE; }\n"
            "require_connection() { ADB_TARGET=192.168.0.103:5555; }\n"
            f"new_log() {{ CURRENT_LOG='{self.tmp}/log.txt'; : > \"$CURRENT_LOG\"; }}\n"
            + rec + body + "screen_glue\n"
        )
        (self.tmp / "item.sh").write_text(script)
        env = dict(os.environ)
        env.update({
            "PATH": f"{self.tmp}/bin:" + os.environ["PATH"],
            "ADB_LOG": str(self.tmp / "adb.log"),
            "DEV": str(self.tmp / "dev"),
            "HOME": str(self.tmp / "home"),
            "PD_LOCAL_MOVIES": str(self.local),
        })
        return subprocess.run(["bash", str(self.tmp / "item.sh")], input=answers,
                              capture_output=True, text=True, env=env, timeout=120).stdout

    def loudness(self, path):
        raw = subprocess.run(["ffmpeg", "-nostdin", "-v", "error", "-i", str(path), "-ac", "1",
                              "-ar", "8000", "-f", "s16le", "-"], capture_output=True, check=True).stdout
        a = array.array("h", raw)
        return lambda s, e: max((abs(x) for x in a[int(s * 8000):int(e * 8000)]), default=0)

    def test_glue_with_song_in_sync(self):
        out = self.run_item("\ny\n")
        self.assertIn("♪ Виконавець — Пісня (2).mp3", out)
        self.assertNotIn("Стара пісня", out)
        result = self.local / "screen-20261029-1448_zvuk.mp4"
        self.assertTrue(result.exists(), out + (self.tmp / "log.txt").read_text())
        dur = float(subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration",
                                    "-of", "csv=p=0", str(result)], capture_output=True, text=True).stdout)
        self.assertAlmostEqual(dur, 6.0, delta=0.3)
        peak = self.loudness(result)
        # у відео: шматок 1 = 0–2 (тиша), шматок 2 = 2–4 (пісня з 3-ї), шматок 3 = 4–6 (пісня до 5-ї)
        self.assertLess(peak(0.2, 2.8), 300)
        self.assertGreater(peak(3.2, 3.8), 1500)
        self.assertGreater(peak(4.2, 4.8), 1500)
        self.assertLess(peak(5.3, 5.9), 300)
        self.assertFalse((self.tmp / "home" / ".cache" / "pd-glue" / "screen-20261029-1448").exists())

    def test_no_without_yes(self):
        out = self.run_item("\n\n")
        self.assertIn("Не склеюю", out)
        self.assertFalse(self.local.exists() and any(self.local.iterdir()))


class ScreenGlueMenuTest(unittest.TestCase):
    def test_menu_lists_item(self):
        text = MENU.read_text(encoding="utf-8")
        self.assertIn('echo "40 — Склеїти запис 39', text)
        self.assertIn("    40) screen_glue ;;", text)


if __name__ == "__main__":
    unittest.main()
