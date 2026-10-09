package com.saney.renaultdocs

/** Allow only *visual* line breaks in long hashes; never alter clipboard data. */
internal object OperationStatusDisplayFormat {
    private val hash = Regex("""(SHA-256:\s*)([0-9a-fA-F]{64})""")

    fun wrapHashesForDisplay(value: String): String =
        hash.replace(value) { match ->
            val prefix = match.groupValues[1]
            val digest = match.groupValues[2]
            // Explicit lines are reliable on Android skins where U+200B
            // soft breaks are ignored for a continuous SHA-256 digest.
            // Full original result is copied from OperationStatusView.fullDetail.
            prefix.trimEnd() + "\n" + digest.chunked(24).joinToString("\n")
        }
}

/** Colors are meaningful states, not fake percentages of completed work. */
enum class OperationTerminalOutcome {
    SUCCESS, FAILED, CANCELLED, NEUTRAL;

    companion object {
        fun fromTitle(title: String): OperationTerminalOutcome = when {
            title.contains("помилк", ignoreCase = true) ||
                title.contains("не вдалося", ignoreCase = true) ->
                FAILED
            title.contains("скасован", ignoreCase = true) ->
                CANCELLED
            title.contains("вже є", ignoreCase = true) ||
                title.contains("уже є", ignoreCase = true) ->
                NEUTRAL
            else -> SUCCESS
        }
    }
}
