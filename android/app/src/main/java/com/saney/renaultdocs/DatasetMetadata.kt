package com.saney.renaultdocs

data class DatasetMetadata(
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
)
