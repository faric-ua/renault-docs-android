package com.saney.renaultdocs

/**
 * Stage 0 of `docs/plans/2026-10-16_READ_FROM_RDPKG_PLAN.md` (TODO №16): measure what
 * opening a volume costs before rebuilding how volumes are read.
 *
 * Phone QA 2026-10-16 04:03–04:52 (M30s): volumes on the memory card open «капець як
 * довго», each document too; the same kind of volume in phone memory is fast; a restart
 * did not help. Every file of a volume is found through a listing of its folder
 * ([SafDatasetResolver]) and every listed entry costs the provider several file-system
 * calls — on the card each one goes through Android's media service (FUSE). These
 * counters show how many listings / opens happen and how long they take, per volume and
 * place, in logcat (tag [TAG]); Phone Diagnostics item 33 saves them to a file.
 *
 * Plain Kotlin, no Android types — tested on the JVM.
 */
object VolumeReadStats {
    const val TAG = "RenaultRead"

    /** A listing slower than this is logged on its own line. */
    const val SLOW_LISTING_MS = 100L

    /** An open slower than this is logged on its own line. */
    const val SLOW_OPEN_MS = 150L

    /** A summary line every this many file opens of one volume. */
    const val SUMMARY_EVERY_OPENS = 40

    data class Totals(
        val listings: Int = 0,
        val listedEntries: Long = 0L,
        val listingMs: Long = 0L,
        val opens: Int = 0,
        val openMs: Long = 0L,
        val missing: Int = 0,
    )

    private val byVolume = HashMap<String, Totals>()

    @Synchronized
    fun recordListing(
        volume: String,
        entries: Int,
        ms: Long,
    ): Totals {
        val now = byVolume[volume] ?: Totals()
        val next =
            now.copy(
                listings = now.listings + 1,
                listedEntries = now.listedEntries + entries.coerceAtLeast(0),
                listingMs = now.listingMs + ms.coerceAtLeast(0L),
            )
        byVolume[volume] = next
        return next
    }

    @Synchronized
    fun recordOpen(
        volume: String,
        ms: Long,
        found: Boolean,
    ): Totals {
        val now = byVolume[volume] ?: Totals()
        val next =
            if (found) {
                now.copy(opens = now.opens + 1, openMs = now.openMs + ms.coerceAtLeast(0L))
            } else {
                now.copy(missing = now.missing + 1)
            }
        byVolume[volume] = next
        return next
    }

    @Synchronized
    fun totals(volume: String): Totals = byVolume[volume] ?: Totals()

    @Synchronized
    fun reset() {
        byVolume.clear()
    }

    /** `pkg:<packageId>[:/…]` (our provider's document ids) → packageId, else null. */
    fun packageIdOf(rootDocumentId: String): String? {
        if (!rootDocumentId.startsWith("pkg:")) return null
        val rest = rootDocumentId.removePrefix("pkg:")
        val end = rest.indexOfFirst { it == ':' || it == '/' }
        val id = if (end >= 0) rest.substring(0, end) else rest
        return id.takeIf { it.isNotBlank() }
    }

    fun placeLabel(place: VolumePlace?): String =
        when (place) {
            VolumePlace.CARD -> "карта"
            VolumePlace.PHONE -> "телефон"
            null -> "інша папка"
        }

    fun listingLine(
        volume: String,
        place: String,
        folder: String,
        entries: Int,
        ms: Long,
    ): String = "LIST том=$volume місце=$place папка=«${folder.ifBlank { "/" }}» записів=$entries мс=$ms"

    fun openLine(
        volume: String,
        place: String,
        path: String,
        ms: Long,
    ): String = "OPEN том=$volume місце=$place файл=«$path» мс=$ms"

    fun summaryLine(
        volume: String,
        place: String,
        totals: Totals,
    ): String =
        "SUM том=$volume місце=$place " +
            "переліків=${totals.listings} записів=${totals.listedEntries} мс_переліків=${totals.listingMs} " +
            "відкрито=${totals.opens} мс_відкриття=${totals.openMs} не_знайдено=${totals.missing}"

    fun shouldSummarize(totals: Totals): Boolean = totals.opens > 0 && totals.opens % SUMMARY_EVERY_OPENS == 0
}
