package com.saney.renaultdocs

data class ModernVolume(
    val id: String,
    val title: String,
    val documentCode: String?,
    val date: String?,
    val kind: String?,
    val sourceFolder: String?,
    val entrypoint: String,
)

data class ModernCatalog(
    val title: String,
    val manufacturer: String?,
    val model: String?,
    val platform: String?,
    val yearsLabel: String?,
    val contentType: String?,
    val volumes: List<ModernVolume>,
    val source: String,
)
