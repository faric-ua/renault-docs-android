"""v0.5.138 — quick step of TODO №16 (Phone Diagnostics 33, log 110, M30s 2026-10-16).

On the memory card ~3/4 of the wait was finding files: whole-folder listings (330
entries — 11–12.5 s), 4–5 file-system calls per entry, canonical paths on every request,
the same folder listed again and again. Volumes of our own provider are now found
directly by their known path, one call, remembered app-wide.
"""

import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "android/app/src/main/java/com/saney/renaultdocs"


def source(name: str) -> str:
    return (SRC / name).read_text(encoding="utf-8")


class VolumeQuickFindContract(unittest.TestCase):
    def test_resolver_finds_own_volume_files_without_listing(self):
        resolver = source("SafDatasetResolver.kt")
        self.assertIn("private fun directNode(", resolver)
        direct = resolver.index("directNode(normalized)?.let")
        walk = resolver.index("for (part in normalized.split('/'))")
        self.assertLess(direct, walk, "direct lookup comes before the folder walk")
        self.assertIn("VolumeFileIndex.attributes(file)", resolver)
        self.assertIn("VolumeFileIndex.rememberNode(", resolver)
        # Our files are opened directly, the provider stays as a fallback.
        self.assertIn("FileInputStream(it)", resolver)
        self.assertIn("ParcelFileDescriptor.open(it, ParcelFileDescriptor.MODE_READ_ONLY)", resolver)
        self.assertIn("contentResolver.openInputStream(", resolver)

    def test_provider_one_call_per_entry_and_no_canonical_paths(self):
        provider = source("LocalDatasetDocumentsProvider.kt")
        self.assertNotIn(".canonicalFile", provider)
        self.assertNotIn(".canonicalPath", provider)
        self.assertIn("VolumeFileIndex.root(packageId)", provider)
        self.assertIn("VolumeFileIndex.attributes(child)", provider)
        self.assertNotIn("file.lastModified()", provider)
        self.assertNotIn("file.length()", provider)
        self.assertIn("fun mimeTypeFor(", provider)
        # A missing remembered folder (card out, volume moved) is looked up once more.
        self.assertIn("VolumeFileIndex.forget(packageId)", provider)
        self.assertIn("VolumeStoragePolicy.CARD_MISSING_MESSAGE", provider)

    def test_index_refuses_links_and_never_writes(self):
        index = source("VolumeFileIndex.kt")
        self.assertIn("LinkOption.NOFOLLOW_LINKS", index)
        self.assertIn("attrs.isSymbolicLink", index)
        for call in (".delete(", ".deleteRecursively(", ".renameTo(", ".writeText(", ".createNewFile(", ".mkdirs("):
            self.assertNotIn(call, index)

    def test_install_forgets_remembered_volume(self):
        importer = source("RdpkgImporter.kt")
        self.assertGreaterEqual(importer.count("VolumeFileIndex.forget("), 3)
        archive = source("FastContentArchive.kt")
        self.assertIn("descriptors[cacheKey]", archive)
        self.assertIn("VolumeFileIndex.onForget", archive)


if __name__ == "__main__":
    unittest.main()
