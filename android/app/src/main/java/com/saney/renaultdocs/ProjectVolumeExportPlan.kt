package com.saney.renaultdocs

/**
 * A project is an ordered set of independent single-volume .rdpkg exports.
 * This does not produce a .rdproject and never assigns Drive IDs by guessing.
 */
object ProjectVolumeExportPlan {
    data class Item(
        val volumeId: String,
        val fileName: String,
        val documentCode: String?,
        val date: String?,
    )

    fun build(
        project: RenaultProject,
        volumes: List<ProjectVolumeRecord>,
    ): List<Item> {
        require(volumes.isNotEmpty()) {
            "У проєкті немає томів для експорту."
        }
        require(volumes.all(RdpkgExporter::canFastExport)) {
            "Деякі томи не встановлені як готові .rdpkg."
        }
        val items = volumes.map { volume ->
            Item(
                volumeId = volume.id,
                fileName = RdpkgExporter.defaultFileName(project, volume),
                documentCode = RdpkgExporter.resolvedMetadata(volume).documentCode,
                date = RdpkgExporter.resolvedMetadata(volume).date,
            )
        }
        require(items.map { it.volumeId }.toSet().size == items.size) {
            "У проєкті є повторювані volume ID."
        }
        require(items.map { it.fileName.lowercase(java.util.Locale.ROOT) }.toSet().size == items.size) {
            "Деякі томи мають однакові назви файлів .rdpkg. Потрібен окремий розгляд."
        }
        return items
    }
}
