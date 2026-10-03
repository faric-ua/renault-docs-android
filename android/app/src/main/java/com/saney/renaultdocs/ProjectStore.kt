package com.saney.renaultdocs

import android.content.Context
import android.net.Uri
import java.text.Normalizer
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

data class RenaultProject(
    val id: String,
    val title: String,
    val model: String,
)

data class ProjectVolumeRecord(
    val id: String,
    val title: String,
    val documentCode: String?,
    val date: String?,
    val projectHint: String?,
    val datasetId: String,
    val datasetTitle: String,
    val manufacturer: String,
    val model: String,
    val platform: String?,
    val yearsLabel: String?,
    val contentType: String?,
    val entrypoint: String,
    val openEntrypoint: String,
    val treeUri: String,
) {
    fun asDatasetRecord(): DatasetRecord =
        DatasetRecord(
            id = datasetId,
            title = datasetTitle,
            manufacturer = manufacturer,
            model = model,
            platform = platform,
            yearsLabel = yearsLabel,
            contentType = contentType,
            entrypoint = entrypoint,
            openEntrypoint = openEntrypoint,
            volumeCount = 1,
            treeUri = treeUri,
        )
}

class ProjectStore(
    context: Context,
) {
    private val appContext =
        context.applicationContext

    private val prefs =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    init {
        ensureDefaultProjects()
    }

    fun projects(): List<RenaultProject> =
        loadProjects()
            .sortedWith(
                compareBy<RenaultProject> {
                    when (
                        it.id
                    ) {
                        "megane-ii" ->
                            0

                        "laguna-ii" ->
                            1

                        "kangoo-ii" ->
                            2

                        else ->
                            100
                    }
                }.thenBy {
                    it.title.lowercase(
                        Locale.ROOT,
                    )
                },
            )

    fun project(
        projectId: String,
    ): RenaultProject? =
        projects()
            .firstOrNull {
                it.id == projectId
            }

    fun createProject(
        title: String,
        model: String = title,
    ): RenaultProject {
        val cleanTitle =
            title.trim()

        require(
            cleanTitle.isNotBlank(),
        ) {
            "Назва проєкту порожня."
        }

        val id =
            slugify(
                model,
            )

        val record =
            RenaultProject(
                id = id,
                title = cleanTitle,
                model = model.trim()
                    .ifBlank {
                        cleanTitle
                    },
            )

        val updated =
            loadProjects()
                .filterNot {
                    it.id == id
                }
                .plus(
                    record,
                )

        saveProjects(
            updated,
        )

        return record
    }

    fun volumes(
        projectId: String,
    ): List<ProjectVolumeRecord> =
        loadVolumes()
            .filter {
                it.first == projectId
            }
            .map {
                inferVolumeMetadata(
                    it.second,
                )
            }
            .sortedWith(
                compareBy<ProjectVolumeRecord> {
                    it.date == null
                }.thenBy {
                    it.date ?: ""
                }.thenBy {
                    it.documentCode
                        ?: it.title
                },
            )

    fun upsertVolume(
        projectId: String,
        volume: ProjectVolumeRecord,
    ) {
        require(
            project(
                projectId,
            ) != null,
        ) {
            "Проєкт не знайдено: " +
                projectId
        }

        val updated =
            loadVolumes()
                .filterNot {
                    pair ->
                    val legacyPlaceholderFromSameTree =
                        pair.second.treeUri ==
                            volume.treeUri &&
                        pair.second.id !=
                            volume.id &&
                        pair.second.entrypoint !=
                            volume.entrypoint &&
                        pair.second.documentCode
                            .isNullOrBlank() &&
                        pair.second.date
                            .isNullOrBlank() &&
                        pair.second.title ==
                            pair.second.datasetTitle

                    pair.first ==
                        projectId &&
                        (
                            pair.second.id ==
                                volume.id ||
                            pair.second.entrypoint ==
                                volume.entrypoint ||
                            legacyPlaceholderFromSameTree
                        )
                }
                .plus(
                    projectId to
                        volume,
                )

        saveVolumes(
            updated,
        )
    }

    fun removeProject(
        projectId: String,
    ) {
        saveProjects(
            loadProjects()
                .filterNot {
                    it.id ==
                        projectId
                },
        )
        saveVolumes(
            loadVolumes()
                .filterNot {
                    it.first ==
                        projectId
                },
        )
    }

    fun removeVolume(
        projectId: String,
        volumeId: String,
    ) {
        saveVolumes(
            loadVolumes()
                .filterNot {
                    pair ->
                    pair.first ==
                        projectId &&
                        pair.second.id ==
                        volumeId
                },
        )
    }

    fun moveVolume(
        fromProjectId: String,
        toProjectId: String,
        volumeId: String,
    ) {
        require(fromProjectId != toProjectId) {
            "Проєкт призначення збігається з поточним."
        }
        require(project(toProjectId) != null) {
            "Проєкт призначення не знайдено: " + toProjectId
        }

        val all = loadVolumes()
        val volume =
            all.firstOrNull { pair ->
                pair.first == fromProjectId &&
                    pair.second.id == volumeId
            }?.second
                ?: error("Том не знайдено: " + volumeId)

        val updated =
            all.filterNot { pair ->
                (pair.first == fromProjectId &&
                    pair.second.id == volumeId) ||
                    (pair.first == toProjectId &&
                        (pair.second.id == volume.id ||
                            pair.second.entrypoint == volume.entrypoint))
            }.plus(toProjectId to volume)

        saveVolumes(updated)
    }

    fun migrateLegacySingleVolumeDatasets(
        legacy: List<DatasetRecord>,
    ) {
        if (
            prefs.getBoolean(
                KEY_LEGACY_MIGRATION_DONE,
                false,
            )
        ) {
            return
        }

        legacy
            .filter {
                it.volumeCount ==
                    1
            }
            .forEach {
                record ->
                val target =
                    findProjectForModel(
                        record.model,
                    )
                        ?: return@forEach

                val prepared =
                    runCatching {
                        PreparedVolumeReader
                            .read(
                                context =
                                    appContext,
                                treeUri =
                                    Uri.parse(
                                        record.treeUri,
                                    ),
                            )
                            .getOrNull()
                    }
                        .getOrNull()

                val volume =
                    prepared
                        ?: inferVolumeMetadata(
                            ProjectVolumeRecord(
                                id =
                                    slugify(
                                        record.entrypoint,
                                    ),
                                title =
                                    record.title,
                                documentCode =
                                    null,
                                date =
                                    null,
                                projectHint =
                                    target.id,
                                datasetId =
                                    record.id,
                                datasetTitle =
                                    record.title,
                                manufacturer =
                                    record.manufacturer,
                                model =
                                    record.model,
                                platform =
                                    record.platform,
                                yearsLabel =
                                    record.yearsLabel,
                                contentType =
                                    record.contentType,
                                entrypoint =
                                    record.entrypoint,
                                openEntrypoint =
                                    record.openEntrypoint,
                                treeUri =
                                    record.treeUri,
                            ),
                        )

                upsertVolume(
                    projectId =
                        target.id,
                    volume =
                        volume.copy(
                            projectHint =
                                target.id,
                        ),
                )
            }

        prefs.edit()
            .putBoolean(
                KEY_LEGACY_MIGRATION_DONE,
                true,
            )
            .apply()
    }

    fun repairIncompleteVolumeMetadata(
        projectId: String? = null,
    ): Int {
        val current =
            loadVolumes()

        var repairedCount =
            0

        val repaired =
            current.map {
                pair ->
                if (
                    projectId != null &&
                    pair.first !=
                        projectId
                ) {
                    return@map pair
                }

                val volume =
                    pair.second

                val needsRepair =
                    volume.documentCode
                        .isNullOrBlank() ||
                    volume.date
                        .isNullOrBlank()

                if (
                    !needsRepair
                ) {
                    return@map pair
                }

                val prepared =
                    runCatching {
                        PreparedVolumeReader
                            .read(
                                context =
                                    appContext,
                                treeUri =
                                    Uri.parse(
                                        volume.treeUri,
                                    ),
                            )
                            .getOrNull()
                    }
                        .getOrNull()
                        ?.let {
                            inferVolumeMetadata(
                                it,
                            )
                        }
                        ?: inferVolumeMetadata(
                            volume,
                        )

                val hasBetterMetadata =
                    !prepared.documentCode
                        .isNullOrBlank() ||
                    !prepared.date
                        .isNullOrBlank() ||
                    prepared.title !=
                        volume.title

                if (
                    !hasBetterMetadata
                ) {
                    return@map pair
                }

                repairedCount +=
                    1

                pair.first to
                    prepared.copy(
                        projectHint =
                            pair.first,
                    )
            }

        if (
            repairedCount >
            0
        ) {
            val deduped =
                linkedMapOf<
                    String,
                    Pair<
                        String,
                        ProjectVolumeRecord,
                    >,
                >()

            repaired.forEach {
                pair ->
                val key =
                    pair.first +
                        "\u0000" +
                        pair.second.treeUri

                deduped[key] =
                    pair
            }

            saveVolumes(
                deduped.values
                    .toList(),
            )
        }

        return repairedCount
    }

    private fun inferVolumeMetadata(
        volume: ProjectVolumeRecord,
    ): ProjectVolumeRecord {
        val source =
            listOf(
                volume.entrypoint,
                volume.openEntrypoint,
                volume.title,
                volume.datasetTitle,
            )
                .joinToString(
                    " ",
                )

        val documentCode =
            Regex(
                "NT[0-9A-Z]+",
                RegexOption.IGNORE_CASE,
            )
                .find(
                    source,
                )
                ?.value
                ?.uppercase(
                    Locale.ROOT,
                )

        val dateMatch =
            Regex(
                "(20\\d{2})[._-](\\d{2})[._-](\\d{2})",
            )
                .find(
                    source,
                )

        val date =
            dateMatch
                ?.groupValues
                ?.let {
                    groups ->
                    groups[1] +
                        "-" +
                        groups[2] +
                        "-" +
                        groups[3]
                }

        if (
            documentCode ==
                null &&
            date ==
                null
        ) {
            return volume
        }

        val title =
            listOfNotNull(
                documentCode,
                date,
            )
                .joinToString(
                    " · ",
                )
                .ifBlank {
                    volume.title
                }

        return volume.copy(
            title =
                title,
            documentCode =
                documentCode
                    ?: volume.documentCode,
            date =
                date
                    ?: volume.date,
        )
    }

    fun findProjectForModel(
        model: String,
    ): RenaultProject? {
        val normalized =
            slugify(
                model,
            )

        return projects()
            .firstOrNull {
                project ->
                project.id ==
                    normalized ||
                    slugify(
                        project.model,
                    ) ==
                    normalized
            }
    }

    private fun ensureDefaultProjects() {
        if (
            prefs.getBoolean(
                KEY_DEFAULTS_CREATED,
                false,
            )
        ) {
            return
        }

        val existing =
            loadProjects()

        val defaults =
            listOf(
                RenaultProject(
                    id = "megane-ii",
                    title = "Megane II",
                    model = "Megane II",
                ),
                RenaultProject(
                    id = "laguna-ii",
                    title = "Laguna II",
                    model = "Laguna II",
                ),
                RenaultProject(
                    id = "kangoo-ii",
                    title = "Kangoo II",
                    model = "Kangoo II",
                ),
            )

        saveProjects(
            existing +
                defaults.filter {
                    candidate ->
                    existing.none {
                        it.id ==
                            candidate.id
                    }
                },
        )

        prefs.edit()
            .putBoolean(
                KEY_DEFAULTS_CREATED,
                true,
            )
            .apply()
    }

    private fun loadProjects():
        List<RenaultProject> {
        val raw =
            prefs.getString(
                KEY_PROJECTS,
                null,
            )
                ?: return emptyList()

        return runCatching {
            val array =
                JSONArray(
                    raw,
                )

            buildList {
                for (
                    index in
                    0 until array.length()
                ) {
                    val item =
                        array.getJSONObject(
                            index,
                        )

                    add(
                        RenaultProject(
                            id =
                                item.getString(
                                    "id",
                                ),
                            title =
                                item.getString(
                                    "title",
                                ),
                            model =
                                item.getString(
                                    "model",
                                ),
                        ),
                    )
                }
            }
        }.getOrDefault(
            emptyList(),
        )
    }

    private fun saveProjects(
        records: List<RenaultProject>,
    ) {
        val array =
            JSONArray()

        records.forEach {
            record ->
            array.put(
                JSONObject()
                    .put(
                        "id",
                        record.id,
                    )
                    .put(
                        "title",
                        record.title,
                    )
                    .put(
                        "model",
                        record.model,
                    ),
            )
        }

        prefs.edit()
            .putString(
                KEY_PROJECTS,
                array.toString(),
            )
            .apply()
    }

    private fun loadVolumes():
        List<Pair<String, ProjectVolumeRecord>> {
        val raw =
            prefs.getString(
                KEY_VOLUMES,
                null,
            )
                ?: return emptyList()

        return runCatching {
            val array =
                JSONArray(
                    raw,
                )

            buildList {
                for (
                    index in
                    0 until array.length()
                ) {
                    val item =
                        array.getJSONObject(
                            index,
                        )

                    val projectId =
                        item.getString(
                            "projectId",
                        )

                    add(
                        projectId to
                            ProjectVolumeRecord(
                                id =
                                    item.getString(
                                        "id",
                                    ),
                                title =
                                    item.getString(
                                        "title",
                                    ),
                                documentCode =
                                    item.optString(
                                        "documentCode",
                                    )
                                        .takeIf {
                                            it.isNotBlank()
                                        },
                                date =
                                    item.optString(
                                        "date",
                                    )
                                        .takeIf {
                                            it.isNotBlank()
                                        },
                                projectHint =
                                    item.optString(
                                        "projectHint",
                                    )
                                        .takeIf {
                                            it.isNotBlank()
                                        },
                                datasetId =
                                    item.getString(
                                        "datasetId",
                                    ),
                                datasetTitle =
                                    item.getString(
                                        "datasetTitle",
                                    ),
                                manufacturer =
                                    item.getString(
                                        "manufacturer",
                                    ),
                                model =
                                    item.getString(
                                        "model",
                                    ),
                                platform =
                                    item.optString(
                                        "platform",
                                    )
                                        .takeIf {
                                            it.isNotBlank()
                                        },
                                yearsLabel =
                                    item.optString(
                                        "yearsLabel",
                                    )
                                        .takeIf {
                                            it.isNotBlank()
                                        },
                                contentType =
                                    item.optString(
                                        "contentType",
                                    )
                                        .takeIf {
                                            it.isNotBlank()
                                        },
                                entrypoint =
                                    item.getString(
                                        "entrypoint",
                                    ),
                                openEntrypoint =
                                    item.getString(
                                        "openEntrypoint",
                                    ),
                                treeUri =
                                    item.getString(
                                        "treeUri",
                                    ),
                            ),
                    )
                }
            }
        }.getOrDefault(
            emptyList(),
        )
    }

    private fun saveVolumes(
        records:
            List<Pair<String, ProjectVolumeRecord>>,
    ) {
        val array =
            JSONArray()

        records.forEach {
            pair ->
            val projectId =
                pair.first
            val record =
                pair.second

            array.put(
                JSONObject()
                    .put(
                        "projectId",
                        projectId,
                    )
                    .put(
                        "id",
                        record.id,
                    )
                    .put(
                        "title",
                        record.title,
                    )
                    .put(
                        "documentCode",
                        record.documentCode ?: "",
                    )
                    .put(
                        "date",
                        record.date ?: "",
                    )
                    .put(
                        "projectHint",
                        record.projectHint ?: "",
                    )
                    .put(
                        "datasetId",
                        record.datasetId,
                    )
                    .put(
                        "datasetTitle",
                        record.datasetTitle,
                    )
                    .put(
                        "manufacturer",
                        record.manufacturer,
                    )
                    .put(
                        "model",
                        record.model,
                    )
                    .put(
                        "platform",
                        record.platform ?: "",
                    )
                    .put(
                        "yearsLabel",
                        record.yearsLabel ?: "",
                    )
                    .put(
                        "contentType",
                        record.contentType ?: "",
                    )
                    .put(
                        "entrypoint",
                        record.entrypoint,
                    )
                    .put(
                        "openEntrypoint",
                        record.openEntrypoint,
                    )
                    .put(
                        "treeUri",
                        record.treeUri,
                    ),
            )
        }

        prefs.edit()
            .putString(
                KEY_VOLUMES,
                array.toString(),
            )
            .apply()
    }

    companion object {
        private const val PREFS_NAME =
            "renault_docs_projects"
        private const val KEY_PROJECTS =
            "projects"
        private const val KEY_VOLUMES =
            "volumes"
        private const val KEY_DEFAULTS_CREATED =
            "defaults_created"
        private const val KEY_LEGACY_MIGRATION_DONE =
            "legacy_single_volume_migration_done"

        fun slugify(
            value: String,
        ): String {
            val normalized =
                Normalizer.normalize(
                    value,
                    Normalizer.Form.NFKD,
                )
            val ascii =
                normalized
                    .replace(
                        Regex(
                            "\\p{M}+",
                        ),
                        "",
                    )
            val slug =
                ascii
                    .replace(
                        Regex(
                            "[^A-Za-z0-9]+",
                        ),
                        "-",
                    )
                    .trim(
                        '-',
                    )
                    .lowercase(
                        Locale.ROOT,
                    )

            return slug
                .ifBlank {
                    "project"
                }
        }
    }
}
