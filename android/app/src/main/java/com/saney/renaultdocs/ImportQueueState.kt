package com.saney.renaultdocs

/**
 * «Авто» with several .rdpkg files at once (owner 2026-10-18 04:55, after
 * putting 10 Laguna II volumes in one by one at 04:04).
 *
 * How it works: the import service still installs **one** package at a time and
 * the project screen adds the finished volume to the project (unchanged path).
 * This queue only remembers which files are still waiting; when one import
 * finishes (done or failed) the project screen starts the next one.
 * «Скасувати» stops the current one and drops the rest.
 *
 * Limit (honest): the next file starts when the project screen notices the
 * previous one finished — if the screen is closed, the queue waits until it is
 * opened again. Nothing is lost: the waiting list is saved.
 */
data class ImportQueueState(
    val projectId: String = "",
    val pending: List<String> = emptyList(),
    val total: Int = 0,
    val done: Int = 0,
    val failed: List<String> = emptyList(),
) {
    val isActive: Boolean get() = projectId.isNotBlank() && total > 1

    /** Number of the file being imported now, 1-based. */
    val currentNumber: Int get() = (done + failed.size + 1).coerceAtMost(total)

    fun label(): String = "Том $currentNumber з $total"

    fun next(): Pair<String, ImportQueueState>? {
        val first = pending.firstOrNull() ?: return null
        return first to copy(pending = pending.drop(1))
    }

    fun markDone(): ImportQueueState = copy(done = done + 1)

    fun markFailed(name: String): ImportQueueState = copy(failed = failed + name)

    val isFinished: Boolean get() = isActive && pending.isEmpty() && done + failed.size >= total

    fun summary(): String = buildString {
        append("Імпортовано томів: ").append(done).append(" з ").append(total)
        if (failed.isNotEmpty()) {
            append(". Не вийшло: ").append(failed.joinToString(", "))
        }
        append('.')
    }

    fun encode(): String = listOf(
        projectId,
        total.toString(),
        done.toString(),
        failed.joinToString(SEP_ITEM),
        pending.joinToString(SEP_ITEM),
    ).joinToString(SEP_FIELD)

    companion object {
        private const val SEP_FIELD = "\u001E"
        private const val SEP_ITEM = "\u001F"

        fun start(projectId: String, uris: List<String>): ImportQueueState =
            ImportQueueState(projectId = projectId, pending = uris, total = uris.size)

        fun decode(raw: String?): ImportQueueState {
            if (raw.isNullOrEmpty()) return ImportQueueState()
            val parts = raw.split(SEP_FIELD)
            if (parts.size != 5) return ImportQueueState()
            fun items(value: String) = if (value.isEmpty()) emptyList() else value.split(SEP_ITEM)
            return ImportQueueState(
                projectId = parts[0],
                total = parts[1].toIntOrNull() ?: return ImportQueueState(),
                done = parts[2].toIntOrNull() ?: return ImportQueueState(),
                failed = items(parts[3]),
                pending = items(parts[4]),
            )
        }
    }
}
