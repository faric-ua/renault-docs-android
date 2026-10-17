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
                append(" Файли ")
                append(volumeCount(outcome.withFiles.size))
                append(" стираються у фоні — можна вже імпортувати їх заново.")
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
