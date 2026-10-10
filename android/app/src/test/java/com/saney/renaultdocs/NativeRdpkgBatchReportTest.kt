package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeRdpkgBatchReportTest {
    private fun volume(index: Int) = NativeRdpkgBatchVolumeResult(
        label = "NT82${index}A",
        packageId = "megane-ii-nt82${index}a",
        volumeId = "volume-$index",
        sha256 = index.toString().repeat(64),
        outputUri = "content://packages/nt82${index}a.rdpkg",
        sections = 300 + index,
    )

    private fun state(
        report: NativeRdpkgBatchReport?,
        phase: NativeRdpkgRunPhase = NativeRdpkgRunPhase.COMPLETE,
        title: String = "Томів створено: 3",
    ): NativeRdpkgRunState = NativeRdpkgRunState(
        phase = phase,
        sourceKind = NativeRdpkgSourceKind.ARCHIVE_FILE,
        volumeTitle = title,
        batchReport = report,
    )

    @Test
    fun threeSuccessfulPackagesHaveDistinctIdentifiersAndHashes() {
        val result = (1..3).fold(NativeRdpkgBatchReport()) { report, i ->
            report.afterCompleted(volume(i))
        }
        assertEquals(3, result.completedCount)
        assertEquals(3, result.completed.map { it.packageId }.distinct().size)
        val detail = NativeRdpkgBatchReportFormatter.reportLines(state(result))
        assertTrue(detail.contains("Томів створено: 3"))
        assertTrue(detail.contains("Том 1: NT821A"))
        assertTrue(detail.contains("Том 3: NT823A"))
        for (i in 1..3) {
            assertTrue(detail.contains("Package ID: megane-ii-nt82${i}a"))
            assertTrue(detail.contains("SHA-256: " + i.toString().repeat(64)))
        }
        assertFalse(detail.contains("Package ID: немає"))
    }

    @Test
    fun aSingleSuccessfullyCreatedArchiveVolumeKeepsScalarReport() {
        val result = NativeRdpkgBatchReport().afterCompleted(volume(1))
        val source = state(result, title = "NT821A").copy(
            packageId = "megane-ii-nt821a",
            sha256 = "a".repeat(64),
        )
        val detail = NativeRdpkgBatchReportFormatter.reportLines(source)
        assertTrue(detail.contains("Том результату: NT821A"))
        assertTrue(detail.contains("Package ID: megane-ii-nt821a"))
        assertFalse(detail.contains("Томів створено:"))
    }

    @Test
    fun partialFailureKeepsCommittedEntryAndSkippedDuplicate() {
        val result = NativeRdpkgBatchReport()
            .afterCompleted(volume(1))
            .afterSkipped("NT8228A")
        val detail = NativeRdpkgBatchReportFormatter.reportLines(
            state(result, phase = NativeRdpkgRunPhase.FAILED),
        )
        assertTrue(detail.contains("Статус: створено"))
        assertTrue(detail.contains("Томів створено: 1"))
        assertTrue(detail.contains("Томів пропущено: 1"))
        assertTrue(detail.contains("• Уже є: NT8228A"))
        assertTrue(detail.contains("FAILED"))
        assertTrue(detail.contains("Package ID: megane-ii-nt821a"))
    }

    @Test
    fun cancellationWithNoCompletedEntriesNeverInventsChecksums() {
        val detail = NativeRdpkgBatchReportFormatter.reportLines(
            state(NativeRdpkgBatchReport(), phase = NativeRdpkgRunPhase.CANCELLED),
        )
        assertTrue(detail.contains("Томів створено: 0"))
        assertFalse(detail.contains("SHA-256:"))
    }

    @Test
    fun oldBatchOnlyExplainsMissingLegacyPerPackageMetadata() {
        val old = state(null, title = "3 томів").copy(
            packageId = "megane-ii-nt8274",
            sha256 = "",
        )
        val detail = NativeRdpkgBatchReportFormatter.reportLines(old)
        assertTrue(detail.contains("Томів створено: 3"))
        assertTrue(detail.contains("Дані про всі пакети"))
        assertFalse(detail.contains("Package ID: megane-ii-nt8274"))
        assertFalse(detail.contains("SHA-256:"))
    }

    @Test
    fun boundedRecordsDeduplicateAndTrackOverflow() {
        var report = NativeRdpkgBatchReport()
        repeat(NativeRdpkgBatchReport.MAX_RECORDED + 3) { index ->
            report = report.afterCompleted(volume(index))
        }
        assertEquals(NativeRdpkgBatchReport.MAX_RECORDED, report.completed.size)
        assertEquals(3, report.omittedCompleted)
        assertEquals(NativeRdpkgBatchReport.MAX_RECORDED + 3, report.completedCount)
        assertEquals(report, report.afterCompleted(volume(0)))
    }
}
