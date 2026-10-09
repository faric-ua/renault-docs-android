package com.saney.renaultdocs

/**
 * Separate a running operation's stage from its measured item count.
 * The UI reserves stable positions for both, even while the count is unknown.
 */
internal object OperationStatusDetailFormatter {
    data class Parts(val stage: String, val counter: String?)

    private val itemCounter =
        Regex("""^(.*?)\s+·\s+([^:\n]{1,40}):\s*(\d+)(?:\s*/\s*(\d+))?\s*$""")

    fun split(message: String): Parts {
        val text = message.trim().ifBlank { "Виконую…" }
        val match = itemCounter.matchEntire(text) ?: return Parts(text, null)
        val stage = match.groupValues[1].trim().ifBlank { "Виконую…" }
        val label = match.groupValues[2].trim()
        val completed = match.groupValues[3]
        val total = match.groupValues[4]
        val counter = if (total.isNotBlank()) {
            "$label: $completed / $total"
        } else {
            "$label: $completed"
        }
        return Parts(stage, counter)
    }
}
