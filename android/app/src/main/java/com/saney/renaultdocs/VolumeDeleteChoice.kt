package com.saney.renaultdocs

/**
 * v0.5.147 (owner 2026-10-17 05:02): «Видалити томи разом з файлами» — what the list shows,
 * what is ticked at first and what the confirmation says. Plain JVM (tested on the build
 * machine); the window is in [HomeProjectDialogController], the deleting in [VolumeTrash].
 */
data class VolumeDeleteItem(
    val volumeId: String,
    val label: String,
    /** Id of the installed package, or null — the volume is not kept by the app (e.g. a SAF folder). */
    val packageId: String?,
    val onCard: Boolean,
    val unpackedFolder: Boolean,
    /** The same package is also in another project — only this record goes, files stay. */
    val usedInOtherProject: Boolean,
) {
    /** True — the app's own files of this volume are erased too. */
    val erasesFiles: Boolean get() = packageId != null && !usedInOtherProject
}

data class VolumeDeleteOutcome(
    val withFiles: List<String>,
    val recordOnly: List<String>,
    val failed: List<String>,
)

object VolumeDeleteChoice {
    fun placeLabel(item: VolumeDeleteItem): String =
        when {
            item.packageId == null -> "файли не в застосунку — прибереться лише запис"
            item.usedInOtherProject -> "є і в іншому проєкті — файли лишаться, прибереться лише запис"
            else ->
                (if (item.onCard) "карта пам'яті" else "телефон") + " · " +
                    (if (item.unpackedFolder) "розпакована папка" else "один файл")
        }

    /**
     * Ticked at first: the unpacked folders on the memory card — exactly the slow ones the
     * owner wants gone (Megane II on M30s). Everything else only when the owner ticks it.
     */
    fun defaultSelection(items: List<VolumeDeleteItem>): Set<String> =
        items.filter { it.erasesFiles && it.onCard && it.unpackedFolder }.map { it.volumeId }.toSet()

    fun confirmTitle(count: Int): String = "Видалити ${volumeCount(count)} назавжди?"

    fun confirmMessage(
        projectTitle: String,
        selected: List<VolumeDeleteItem>,
    ): String {
        val names = selected.joinToString("\n") { "• " + it.label }
        val withFiles = selected.count { it.erasesFiles }
        val recordOnly = selected.size - withFiles
        return buildString {
            append(names)
            append("\n\nТоми зникнуть з проєкту ")
            append(projectTitle)
            append('.')
            if (withFiles > 0) {
                append(" Файли ")
                append(if (withFiles == selected.size) "цих томів" else "$withFiles з них")
                append(" зітруться у фоні — повернути їх можна лише імпортом заново ")
                append("(наприклад, файл .rdproject з іншого телефона).")
            }
            if (recordOnly > 0) {
                append(" Для ")
                append(volumeCount(recordOnly))
                append(" прибереться лише запис, файли лишаться.")
            }
            append("\n\nОригінальні файли Renault, Google Диск і підготовлені .rdproject / .rdpkg не чіпаються.")
        }
    }

    fun resultMessage(outcome: VolumeDeleteOutcome): String =
        buildString {
            val removed = outcome.withFiles.size + outcome.recordOnly.size
            append("Видалено з проєкту: ")
            append(volumeCount(removed))
            append('.')
            if (outcome.withFiles.isNotEmpty()) {
                // v0.5.148: «Файли 23 томів» (was «Файли 23 томи» — wrong case, M30s 05:24).
                append(" Файли ")
                append(volumeCountOf(outcome.withFiles.size))
                append(" стираються у фоні (смуга — нижче і в шторці) — можна вже імпортувати їх заново.")
            }
            if (outcome.recordOnly.isNotEmpty()) {
                append(" Лише запис (файли лишились): ")
                append(outcome.recordOnly.joinToString(", "))
                append('.')
            }
            if (outcome.failed.isNotEmpty()) {
                append(" Не вдалося прибрати (том лишився в проєкті): ")
                append(outcome.failed.joinToString(", "))
                append('.')
            }
        }

    /** «файли 1 тому», «файли 23 томів» — after a noun (genitive). */
    fun volumeCountOf(count: Int): String {
        val noun = if (count % 10 == 1 && count % 100 != 11) "тому" else "томів"
        return "$count $noun"
    }

    // v0.5.148 (owner 05:25): the bar of the background eraser — words for the card and the shade.
    const val ERASE_TITLE = "Стираю файли видалених томів"

    /** «Том 5 з 23 · 34 % · залишилось ~12 хв» (or «рахую файли…» at the start of a volume). */
    fun eraseCounter(p: VolumeTrashCore.EraseProgress): String =
        buildString {
            append("Том ")
            append(p.itemIndex)
            append(" з ")
            append(p.itemTotal)
            append(" · ")
            if (p.counting) {
                append("рахую файли…")
                if (p.itemDone > 0) append(" ").append(p.itemDone)
            } else {
                append((p.fraction * 100).toInt().coerceIn(0, 100))
                append(" %")
            }
            p.remainingMs?.let {
                append(" · залишилось ")
                append(durationLabel(it))
            }
        }

    fun eraseDetail(p: VolumeTrashCore.EraseProgress): String =
        if (p.counting) {
            "Рахую файли тому ${p.itemIndex}… знайдено: ${p.itemDone}. Стерто вже: ${p.deletedFiles}."
        } else {
            "Стерто файлів: ${p.deletedFiles} · у цьому томі ${p.itemDone} з ${p.itemFiles}."
        }

    fun eraseDoneTitle(left: Int): String =
        if (left == 0) "Файли видалених томів стерто" else "Файли видалених томів · стерто не все"

    fun eraseDoneDetail(
        files: Long,
        volumes: Int,
        ms: Long,
        left: Int,
    ): String =
        buildString {
            append("Стерто файлів: ")
            append(files)
            append(" (")
            append(volumeCount(volumes))
            append(") за ")
            append(durationLabel(ms).removePrefix("~"))
            append('.')
            if (left > 0) {
                append(" Не стерлось: ")
                append(volumeCount(left))
                append(" — спробую ще раз при наступному відкритті застосунку.")
            }
        }

    /** «менше хвилини», «~12 хв», «~1 год 05 хв». */
    fun durationLabel(ms: Long): String {
        val minutes = (ms + 30_000L) / 60_000L
        return when {
            ms < 60_000L -> "менше хвилини"
            minutes < 60 -> "~$minutes хв"
            else -> "~${minutes / 60} год %02d хв".format(minutes % 60)
        }
    }

    fun volumeCount(count: Int): String {
        val mod100 = count % 100
        val mod10 = count % 10
        val noun =
            when {
                mod100 in 11..14 -> "томів"
                mod10 == 1 -> "том"
                mod10 in 2..4 -> "томи"
                else -> "томів"
            }
        return "$count $noun"
    }
}
