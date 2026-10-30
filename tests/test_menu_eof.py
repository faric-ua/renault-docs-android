"""Меню виходить саме, коли Termux закрили не пунктом 0 (інспектор GitHub Storage 2026-10-30).

Телефон 30.10: меню висіли у фоні з 46–133 хв процесора — read без екрана повертав
помилку, і while true малював меню ~50 разів на секунду. Тепер read ... || exit 0.
"""
import os
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MENUS = [
        "menu.sh",
        "tools/termux/phone-diagnostics-menu.sh",]


class MenuEofTest(unittest.TestCase):
    def test_menus_exit_when_input_closed(self):
        for rel in MENUS:
            with self.subTest(menu=rel), tempfile.TemporaryDirectory() as home:
                menu = ROOT / rel
                env = dict(os.environ, HOME=home, TERM="dumb")
                r = subprocess.run(["bash", menu.name], cwd=menu.parent, stdin=subprocess.DEVNULL,
                                   capture_output=True, text=True, env=env, timeout=60)
                self.assertEqual(r.returncode, 0, r.stdout[-500:])
                self.assertLessEqual(r.stdout.count("Вийти"), 1, "меню крутиться по колу без екрана")


if __name__ == "__main__":
    unittest.main()
