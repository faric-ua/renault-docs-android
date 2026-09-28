package com.saney.renaultdocs

data class ModernSection(
    val code: String,
    val title: String,
    val entrypoint: String,
)

data class ModernVolumeSections(
    val volumeId: String?,
    val volumeTitle: String?,
    val documentCode: String?,
    val date: String?,
    val volumeEntrypoint: String,
    val sourceFile: String?,
    val sections: List<ModernSection>,
)
