import unittest
from collections import defaultdict

from core.convert_paths import patch_text


class ConvertPathsTests(unittest.TestCase):
    def setUp(self):
        self.files = {
            'Version/RUS/HTM/SCH/A.HTM',
            'Version/COMMUN/PDF/SCH/0207_A3.PDF',
            'Version/COMMUN/GIF/icon.GIF',
            'Version/COMMUN/HTM/NEXT.HTM',
        }
        self.lower_map = defaultdict(list)
        for path in self.files:
            self.lower_map[path.lower()].append(path)

    def test_static_case_is_normalized_and_fragment_is_preserved(self):
        source = 'Version/RUS/HTM/SCH/A.HTM'
        text = '<a href="../../../COMMUN/PDF/SCH/0207_A3.pdf#viewrect=1,2,3,4">x</a>'
        patched, changes = patch_text(source, text, self.files, self.lower_map)
        self.assertIn(
            '../../../COMMUN/PDF/SCH/0207_A3.PDF#viewrect=1,2,3,4',
            patched,
        )
        self.assertEqual(1, len(changes))

    def test_external_url_is_not_changed(self):
        source = 'Version/RUS/HTM/SCH/A.HTM'
        text = '<a href="https://example.com/test.pdf">x</a>'
        patched, changes = patch_text(source, text, self.files, self.lower_map)
        self.assertEqual(text, patched)
        self.assertEqual([], changes)

    def test_dynamic_print_suffixes_are_fixed_but_comments_are_left_alone(self):
        source = 'Version/COMMUN/JS/VISU.JS'
        text = (
            '// old = "_print";\n'
            'x = "_print";\n'
            'y = "_printNB";\n'
        )
        patched, changes = patch_text(source, text, self.files, self.lower_map)
        self.assertIn('// old = "_print";', patched)
        self.assertIn('x = "_PRINT";', patched)
        self.assertIn('y = "_PRINTNB";', patched)
        self.assertEqual(
            2,
            sum(c['kind'] == 'dynamic_print_case' for c in changes),
        )


if __name__ == '__main__':
    unittest.main()
