package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/** v0.5.150: update from Google Drive — links, version file, verdict, checks, what's new. */
class AppUpdateTest {
    private val id = "1AbCdEfGhIjKlMnOpQrStUvWxYz012345"
    private val sha = "a".repeat(64)

    private fun manifest(
        code: Long = 166,
        changes: String = "• нове",
        details: String = "0.5.150\n• нове",
    ) = AppUpdate.Manifest("com.saney.renaultdocs", code, "0.5.150", sha, 5_000_000, id, 1L, changes, details)

    @Test
    fun linksFromDrive() {
        assertEquals(id, AppUpdate.fileIdFromLink("https://drive.google.com/file/d/$id/view?usp=sharing"))
        assertEquals(id, AppUpdate.fileIdFromLink("https://drive.google.com/open?id=$id"))
        assertEquals(id, AppUpdate.fileIdFromLink(" $id "))
        assertNull(AppUpdate.fileIdFromLink("https://drive.google.com/drive/folders/$id"))
        assertNull(AppUpdate.fileIdFromLink("https://example.com/file/d/$id/view"))
        assertNull(AppUpdate.fileIdFromLink(""))
    }

    @Test
    fun versionFileRoundTripKeepsWhatsNew() {
        val m = manifest(changes = "• рядок 1\n• «лапки» і \"цитата\"", details = "0.5.150\n• a\n\n0.5.149\n• b")
        val back = AppUpdate.Manifest.parse(m.toJson())
        assertEquals(m, back)
        assertTrue(m.toJson().contains("\"apkLink\": \"https://drive.google.com/file/d/$id/view\""))
    }

    @Test
    fun oldVersionFileWithoutWhatsNewStillReads() {
        val json =
            "{\"format\": \"renault-docs-update/1\", \"package\": \"com.saney.renaultdocs\", \"versionCode\": 166, " +
                "\"versionName\": \"0.5.150\", \"apkSha256\": \"$sha\", \"apkSize\": 10, \"apkId\": \"$id\"}"
        val m = AppUpdate.Manifest.parse(json)
        assertEquals("", m.changes)
        assertEquals("", m.details)
    }

    @Test
    fun wrongFilesSaySoInWords() {
        try {
            AppUpdate.Manifest.parse("PK\u0003\u0004 binary")
            fail()
        } catch (e: AppUpdate.WrongFileException) {
            assertTrue(e.message!!.contains("не файл версії Renault Docs"))
        }
        try {
            AppUpdate.Manifest.parse(AppUpdate.PLACEHOLDER)
            fail()
        } catch (e: IllegalArgumentException) {
            assertEquals("Автор ще не опублікував жодної версії.", e.message)
        }
        try {
            AppUpdate.Manifest.parse("{\"format\": \"track-catcher-update/1\"}")
            fail()
        } catch (e: AppUpdate.WrongFileException) {
            assertTrue(e.message!!.contains("інший формат"))
        }
    }

    @Test
    fun verdictAndWindowText() {
        val m = manifest()
        assertEquals(AppUpdate.Verdict.NEWER, AppUpdate.compare(m, "com.saney.renaultdocs", 165))
        assertEquals(AppUpdate.Verdict.SAME, AppUpdate.compare(m, "com.saney.renaultdocs", 166))
        assertEquals(AppUpdate.Verdict.OLDER, AppUpdate.compare(m, "com.saney.renaultdocs", 167))
        assertEquals(AppUpdate.Verdict.OTHER_PACKAGE, AppUpdate.compare(m, "other", 1))
        val text = AppUpdate.newerMessage(m, "0.5.149", 165)
        assertTrue(text.startsWith("На Диску: 0.5.150 (166)\nУ тебе: 0.5.149 (165)\nРозмір: 4,8 МБ"))
        assertTrue(text.contains("Що змінилось:\n• нове"))
        assertTrue(text.contains("«Не зараз»"))
        assertTrue(!AppUpdate.newerMessage(manifest(changes = ""), "0.5.149", 165).contains("Що змінилось"))
    }

    @Test
    fun newestOfSavedAndBuiltIn() {
        val a = "saved" to manifest(code = 160)
        val b = "builtin" to manifest(code = 166)
        assertEquals("builtin", AppUpdate.newest(listOf(a, b))!!.first)
        assertEquals(listOf("p"), AppUpdate.manifestCandidates("p", "s", "b"))
        assertEquals(listOf("s", "b"), AppUpdate.manifestCandidates(null, "s", "b"))
        assertEquals(emptyList<String>(), AppUpdate.manifestCandidates(null, null, ""))
    }

    @Test
    fun downloadedApkIsCheckedFully() {
        val m = manifest()
        val cert = "c".repeat(64)
        assertNull(AppUpdate.checkDownloaded(m, sha, 5_000_000, "com.saney.renaultdocs", 166, cert, cert))
        assertTrue(AppUpdate.checkDownloaded(m, "b".repeat(64), 5_000_000, "com.saney.renaultdocs", 166, cert, cert)!!.contains("контрольна сума"))
        assertTrue(AppUpdate.checkDownloaded(m, sha, 5_000_001, "com.saney.renaultdocs", 166, cert, cert)!!.contains("контрольна сума"))
        assertTrue(AppUpdate.checkDownloaded(m, sha, 5_000_000, "x.y", 166, cert, cert)!!.contains("іншого застосунку"))
        assertTrue(AppUpdate.checkDownloaded(m, sha, 5_000_000, "com.saney.renaultdocs", 165, cert, cert)!!.contains("Версія всередині APK"))
        assertTrue(AppUpdate.checkDownloaded(m, sha, 5_000_000, "com.saney.renaultdocs", 166, "d".repeat(64), cert)!!.contains("іншим ключем"))
        // Stricter than Track Catcher: an unknown key is not installed.
        assertTrue(AppUpdate.checkDownloaded(m, sha, 5_000_000, "com.saney.renaultdocs", 166, null, cert)!!.contains("ключ підпису"))
    }

    @Test
    fun whatsNewBlocks() {
        val text = "0.5.150\n• a\n• b\n\n0.5.149\n• c\n\n0.5.148\n• d\n"
        assertEquals("• a\n• b", WhatsNew.forVersion(text, "0.5.150"))
        assertEquals("• c", WhatsNew.forVersion(text, "0.5.149"))
        assertEquals("", WhatsNew.forVersion(text, "0.5.1"))
        assertEquals("0.5.150\n• a\n• b\n\n0.5.149\n• c", WhatsNew.recent(text, 2))
    }

    @Test
    fun builtInWhatsNewHasThisVersion() {
        // The asset Claude writes with each release must start with the app's version.
        val asset = File("src/main/assets/whats_new.txt").takeIf { it.isFile }
            ?: File("android/app/src/main/assets/whats_new.txt")
        val gradle = File(asset.parentFile.parentFile.parentFile.parentFile, "build.gradle.kts").readText()
        val versionName = Regex("versionName = \"([0-9.]+)\"").find(gradle)!!.groupValues[1]
        val text = asset.readText()
        assertEquals(versionName, WhatsNew.blocks(text).first().first)
        assertTrue(WhatsNew.forVersion(text, versionName).isNotBlank())
        assertTrue(WhatsNew.forVersion(text, versionName).length <= AppUpdate.MAX_CHANGES)
    }
}
