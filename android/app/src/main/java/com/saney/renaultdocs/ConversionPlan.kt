package com.saney.renaultdocs

data class ConversionPlan(
    val sourceUri: String,
    val destinationUri: String,
    val sourceName: String,
    val outputFolderName: String,
)

object ConversionPlanFactory {
    fun create(
        sourceUri: String?,
        destinationUri: String?,
        sourceName: String?,
    ): ConversionPlan {
        require(!sourceUri.isNullOrBlank()) {
            "Спочатку вибери стару Renault-папку."
        }
        require(!destinationUri.isNullOrBlank()) {
            "Спочатку вибери папку призначення."
        }
        require(sourceUri != destinationUri) {
            "Source і destination не можуть бути тією самою папкою."
        }

        val cleanSourceName = sourceName
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "Renault"

        return ConversionPlan(
            sourceUri = sourceUri,
            destinationUri = destinationUri,
            sourceName = cleanSourceName,
            outputFolderName = outputName(cleanSourceName),
        )
    }

    fun outputName(sourceName: String): String {
        val cleaned = sourceName
            .trim()
            .replace(Regex("""[\\/:*?"<>|]+"""), "_")
            .trim('.', ' ')
            .ifBlank { "Renault" }

        return cleaned + "_android"
    }
}
