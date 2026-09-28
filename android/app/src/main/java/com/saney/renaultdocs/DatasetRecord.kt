package com.saney.renaultdocs

data class DatasetRecord(
    val id: String,
    val title: String,
    val manufacturer: String,
    val model: String,
    val platform: String?,
    val yearsLabel: String?,
    val contentType: String?,
    val entrypoint: String,
    val openEntrypoint: String,
    val volumeCount: Int,
    val treeUri: String,
) {
    companion object {
        fun from(
            metadata: DatasetMetadata,
            treeUri: String,
        ): DatasetRecord = DatasetRecord(
            id = metadata.id,
            title = metadata.title,
            manufacturer = metadata.manufacturer,
            model = metadata.model,
            platform = metadata.platform,
            yearsLabel = metadata.yearsLabel,
            contentType = metadata.contentType,
            entrypoint = metadata.entrypoint,
            openEntrypoint = metadata.openEntrypoint,
            volumeCount = metadata.volumeCount,
            treeUri = treeUri,
        )
    }
}
