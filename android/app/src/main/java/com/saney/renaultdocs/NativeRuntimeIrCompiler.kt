package com.saney.renaultdocs

import java.io.BufferedWriter
import java.io.File
import java.io.OutputStreamWriter
import java.io.Writer
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/**
 * Kotlin-native Runtime IR package compiler.
 *
 * This stage consumes the app-private normalized File staging tree and writes
 * the runtime-tree debug artifact, sharded section Runtime IR, volume-level
 * documentation shards, the small runtime index, and coverage diagnostics.
 */
object NativeRuntimeIrCompiler {
    const val SCHEMA_VERSION =
        2

    const val COMPILER_PHASE =
        "section-ir-v2"

    const val RUNTIME_TREE_FILENAME =
        "runtime-tree.json"

    const val RUNTIME_INDEX_FILENAME =
        "runtime-ir-index.json"

    const val COVERAGE_FILENAME =
        "runtime-ir-coverage.json"

    const val RUNTIME_DIRNAME =
        "runtime-ir"

    data class Result(
        val runtimeTreePath: File,
        val runtimeIndexPath: File,
        val coveragePath: File,
        val sectionCount: Int,
        val volumeCount: Int,
    )

    fun compile(
        outputRoot: File,
        volumes: List<JSONObject>,
        sectionsIndex: JSONObject? = null,
        progress: ((String) -> Unit)? = null,
    ): Result {
        val root =
            outputRoot.canonicalFile
        val packageRoot =
            File(
                root,
                NativeVolumeCompiler.PACKAGE_DIR,
            )

        require(
            packageRoot.isDirectory ||
                packageRoot.mkdirs(),
        ) {
            "Не вдалося створити _renault для Runtime IR."
        }

        val runtimeTree =
            buildRuntimeTree(
                outputRoot =
                    root,
                volumes =
                    volumes,
                sectionsIndex =
                    sectionsIndex,
                progress =
                    progress,
            )

        val runtimeTreePath =
            File(
                packageRoot,
                RUNTIME_TREE_FILENAME,
            )

        writeJsonFile(
            file =
                runtimeTreePath,
            value =
                runtimeTree,
        )

        val runtimeIndexPath =
            writeShards(
                runtimeTree =
                    runtimeTree,
                outputRoot =
                    root,
                packageRoot =
                    packageRoot,
            )

        val coveragePath =
            File(
                packageRoot,
                COVERAGE_FILENAME,
            )

        writeJsonFile(
            file =
                coveragePath,
            value =
                buildCoverage(
                    runtimeTree,
                ),
        )

        return Result(
            runtimeTreePath =
                runtimeTreePath,
            runtimeIndexPath =
                runtimeIndexPath,
            coveragePath =
                coveragePath,
            sectionCount =
                runtimeTree.optInt(
                    "section_count",
                    0,
                ),
            volumeCount =
                runtimeTree.optInt(
                    "volume_count",
                    0,
                ),
        )
    }

    fun buildRuntimeTree(
        outputRoot: File,
        volumes: List<JSONObject>,
        sectionsIndex: JSONObject? = null,
        progress: ((String) -> Unit)? = null,
    ): JSONObject {
        val compiledVolumes =
            JSONArray()
        var totalSections =
            0

        volumes.forEachIndexed {
            volumeIndex,
            volume ->
            val compiled =
                compileVolumeRuntime(
                    outputRoot =
                        outputRoot,
                    volume =
                        volume,
                    sectionsResult =
                        findSectionsResult(
                            sectionsIndex =
                                sectionsIndex,
                            volume =
                                volume,
                        ),
                )

            compiledVolumes.put(
                compiled,
            )

            val sectionCount =
                compiled
                    .getJSONObject(
                        "modern",
                    )
                    .optInt(
                        "section_count",
                        0,
                    )

            totalSections +=
                sectionCount

            progress?.invoke(
                "Runtime IR: " +
                    (
                        volumeIndex +
                            1
                    ) +
                    "/" +
                    volumes.size +
                    " · " +
                    volume
                        .optString(
                            "document_code",
                            volume.optString(
                                "title",
                                "volume-" +
                                    (
                                        volumeIndex +
                                            1
                                    ),
                            ),
                        ) +
                    " · " +
                    sectionCount +
                    " розділів"
            )
        }

        return JSONObject()
            .put(
                "schema_version",
                SCHEMA_VERSION,
            )
            .put(
                "format",
                "renault-runtime-ir",
            )
            .put(
                "source",
                "legacy-html-compiler",
            )
            .put(
                "classic_preserved",
                true,
            )
            .put(
                "modern_data_contract",
                "normalized-json",
            )
            .put(
                "compiler_phase",
                COMPILER_PHASE,
            )
            .put(
                "volume_count",
                compiledVolumes.length(),
            )
            .put(
                "section_count",
                totalSections,
            )
            .put(
                "volumes",
                compiledVolumes,
            )
    }

    private fun findSectionsResult(
        sectionsIndex: JSONObject?,
        volume: JSONObject,
    ): JSONObject? {
        val volumes =
            sectionsIndex
                ?.optJSONArray(
                    "volumes",
                )
                ?: return null

        val wantedEntrypoint =
            volume
                .optString(
                    "entrypoint",
                )
                .replace(
                    '\\',
                    '/',
                )
                .trim()

        for (
            index in
            0 until volumes.length()
        ) {
            val item =
                volumes.optJSONObject(
                    index,
                )
                    ?: continue

            val currentEntrypoint =
                item
                    .optString(
                        "entrypoint",
                    )
                    .replace(
                        '\\',
                        '/',
                    )
                    .trim()

            if (
                currentEntrypoint.equals(
                    wantedEntrypoint,
                    ignoreCase =
                        true,
                )
            ) {
                return item
            }
        }

        return null
    }

    fun compileVolumeRuntime(
        outputRoot: File,
        volume: JSONObject,
        sectionsResult: JSONObject? = null,
    ): JSONObject {
        val resolvedSectionsResult =
            sectionsResult
                ?: NativeSectionCompiler
                    .discoverVolumeSections(
                        outputRoot =
                            outputRoot,
                        volume =
                            volume,
                    )

        val sectionCompiler =
            NativeSectionIrCompiler(
                outputRoot =
                    outputRoot,
                volume =
                    volume,
            )

        val sections =
            JSONArray()

        val sectionIndex =
            resolvedSectionsResult
                .optJSONArray(
                    "sections",
                )
                ?: JSONArray()

        for (
            index in
            0 until sectionIndex.length()
        ) {
            val sourceSection =
                sectionIndex.getJSONObject(
                    index,
                )

            val compiled =
                sectionCompiler.compileSection(
                    sourceSection,
                )

            val section =
                JSONObject()
                    .put(
                        "code",
                        sourceSection.optString(
                            "code",
                        ),
                    )
                    .put(
                        "title",
                        sourceSection.optString(
                            "title",
                        ),
                    )
                    .put(
                        "legacy_entrypoint",
                        sourceSection.optString(
                            "entrypoint",
                        ),
                    )

            copyObjectEntries(
                from =
                    compiled,
                to =
                    section,
            )

            sections.put(
                section,
            )
        }

        val modern =
            JSONObject()
                .put(
                    "section_count",
                    sections.length(),
                )
                .put(
                    "sections",
                    sections,
                )

        resolvedSectionsResult
            .optString(
                "source_file",
            )
            .takeIf {
                it.isNotBlank()
            }
            ?.let {
                modern.put(
                    "section_source",
                    it,
                )
            }

        compileVolumeDocumentation(
            sections,
        )
            ?.let {
                modern.put(
                    "documentation",
                    it,
                )
            }

        val classic =
            JSONObject()
                .put(
                    "entrypoint",
                    volume.optString(
                        "entrypoint",
                    ),
                )
                .put(
                    "pages",
                    JSONArray(),
                )
                .put(
                    "named_frames",
                    JSONObject(),
                )

        val compiler =
            JSONObject()
                .put(
                    "phase",
                    COMPILER_PHASE,
                )
                .put(
                    "completed",
                    JSONArray(
                        listOf(
                            "section-catalog",
                            "section-static-controls",
                            "section-static-routing",
                            "section-document-graph",
                            "section-asset-index",
                        )
                    ),
                )
                .put(
                    "pending",
                    JSONArray(
                        listOf(
                            "classic-shell-topology-parity",
                            "native-renderer-parity",
                            "dynamic-js-patterns",
                            "cross-section-deduplication",
                        )
                    ),
                )

        val result =
            JSONObject()

        copyOptionalFields(
            source =
                volume,
            target =
                result,
            keys =
                listOf(
                    "id",
                    "title",
                    "document_code",
                    "date",
                    "kind",
                    "source_folder",
                ),
        )

        return result
            .put(
                "classic",
                classic,
            )
            .put(
                "modern",
                modern,
            )
            .put(
                "compiler",
                compiler,
            )
    }

    private fun compileVolumeDocumentation(
        sections: JSONArray,
    ): JSONObject? {
        val candidates =
            mutableListOf<Pair<List<String>, JSONObject>>()

        for (
            index in
            0 until sections.length()
        ) {
            val section =
                sections.getJSONObject(
                    index,
                )

            val items =
                documentationMenuItems(
                    section,
                )

            if (
                items.isEmpty()
            ) {
                continue
            }

            val signature =
                documentationSignature(
                    section =
                        section,
                    items =
                        items,
                )

            if (
                signature.isEmpty()
            ) {
                continue
            }

            candidates +=
                signature to
                    section
        }

        if (
            candidates.isEmpty()
        ) {
            return null
        }

        val expected =
            candidates
                .first()
                .first

        if (
            candidates
                .drop(
                    1,
                )
                .any {
                    it.first !=
                        expected
                }
        ) {
            return null
        }

        val bundle =
            documentationBundle(
                candidates
                    .first()
                    .second,
            )
                ?: return null

        bundle
            .put(
                "scope",
                "volume",
            )
            .put(
                "verified_section_count",
                candidates.size,
            )

        val signatureJson =
            JSONArray()

        expected.forEach {
            encoded ->
            val parts =
                encoded.split(
                    "\u0000",
                )

            signatureJson.put(
                JSONObject()
                    .put(
                        "code",
                        parts.getOrElse(
                            0,
                        ) {
                            ""
                        },
                    )
                    .put(
                        "type",
                        parts.getOrElse(
                            1,
                        ) {
                            ""
                        },
                    )
                    .put(
                        "route_type",
                        parts.getOrElse(
                            2,
                        ) {
                            ""
                        },
                    )
                    .put(
                        "target",
                        parts.getOrElse(
                            3,
                        ) {
                            ""
                        },
                    )
            )
        }

        bundle.put(
            "signature",
            signatureJson,
        )

        return bundle
    }

    private fun documentationMenuItems(
        section: JSONObject,
    ): List<JSONObject> {
        val controls =
            section
                .optJSONArray(
                    "controls",
                )
                ?: JSONArray()

        var toolbar:
            JSONObject? =
            null

        for (
            i in
            0 until controls.length()
        ) {
            val control =
                controls.getJSONObject(
                    i,
                )

            if (
                control.optString(
                    "id",
                ) ==
                "menu-toolbar"
            ) {
                toolbar =
                    control
                break
            }
        }

        val items =
            toolbar
                ?.optJSONArray(
                    "items",
                )
                ?: return emptyList()

        return buildList {
            for (
                i in
                0 until items.length()
            ) {
                val item =
                    items.optJSONObject(
                        i,
                    )
                        ?: continue
                val label =
                    item
                        .optString(
                            "label",
                        )
                        .trim()
                        .uppercase(
                            Locale.ROOT,
                        )

                if (
                    label in
                    documentationCodes
                ) {
                    add(
                        item,
                    )
                }
            }
        }
    }

    private fun documentationSignature(
        section: JSONObject,
        items: List<JSONObject>,
    ): List<String> {
        val actionMap =
            objectMapById(
                section.optJSONArray(
                    "actions",
                )
                    ?: JSONArray(),
            )

        val signature =
            mutableListOf<String>()

        items.forEach {
            item ->
            val actionId =
                item
                    .optString(
                        "action_id",
                    )
                    .trim()
            val action =
                actionMap[
                    actionId
                ]
                    ?: return emptyList()

            signature +=
                listOf(
                    item
                        .optString(
                            "label",
                        )
                        .trim()
                        .uppercase(
                            Locale.ROOT,
                        ),
                    action.optString(
                        "type",
                    ),
                    action.optString(
                        "route_type",
                    ),
                    action
                        .optString(
                            "target",
                        )
                        .replace(
                            '\\',
                            '/',
                        )
                        .lowercase(
                            Locale.ROOT,
                        ),
                ).joinToString(
                    "\u0000",
                )
        }

        return signature.sorted()
    }

    private fun documentationBundle(
        section: JSONObject,
    ): JSONObject? {
        val menuItems =
            documentationMenuItems(
                section,
            )

        if (
            menuItems.isEmpty()
        ) {
            return null
        }

        val arrays =
            linkedMapOf<String, Map<String, JSONObject>>()

        listOf(
            "actions",
            "panels",
            "controls",
            "documents",
        ).forEach {
            key ->
            arrays[
                key
            ] =
                objectMapById(
                    section.optJSONArray(
                        key,
                    )
                        ?: JSONArray(),
                )
        }

        val wanted =
            linkedMapOf(
                "actions" to
                    linkedSetOf<String>(),
                "panels" to
                    linkedSetOf<String>(),
                "controls" to
                    linkedSetOf<String>(),
                "documents" to
                    linkedSetOf<String>(),
            )

        val actionQueue =
            ArrayDeque<String>()
        val panelQueue =
            ArrayDeque<String>()
        val controlQueue =
            ArrayDeque<String>()
        val documentQueue =
            ArrayDeque<String>()

        menuItems.forEach {
            item ->
            item
                .optString(
                    "action_id",
                )
                .takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    actionQueue.add(
                        it,
                    )
                }
        }

        fun queueAction(
            value: String,
        ) {
            if (
                value.isNotBlank() &&
                value !in
                wanted.getValue(
                    "actions",
                )
            ) {
                actionQueue.add(
                    value,
                )
            }
        }

        while (
            actionQueue.isNotEmpty() ||
            panelQueue.isNotEmpty() ||
            controlQueue.isNotEmpty() ||
            documentQueue.isNotEmpty()
        ) {
            while (
                actionQueue.isNotEmpty()
            ) {
                val id =
                    actionQueue.removeFirst()

                if (
                    id.isBlank() ||
                    !wanted
                        .getValue(
                            "actions",
                        )
                        .add(
                            id,
                        )
                ) {
                    continue
                }

                val action =
                    arrays
                        .getValue(
                            "actions",
                        )[
                            id
                        ]
                        ?: continue

                action
                    .optString(
                        "panel_id",
                    )
                    .takeIf {
                        it.isNotBlank()
                    }
                    ?.let {
                        panelQueue.add(
                            it,
                        )
                    }

                action
                    .optString(
                        "document_id",
                    )
                    .takeIf {
                        it.isNotBlank()
                    }
                    ?.let {
                        documentQueue.add(
                            it,
                        )
                    }

                jsonStrings(
                    action.optJSONArray(
                        "side_effect_action_ids",
                    )
                        ?: JSONArray(),
                ).forEach {
                    queueAction(
                        it,
                    )
                }
            }

            while (
                panelQueue.isNotEmpty()
            ) {
                val id =
                    panelQueue.removeFirst()

                if (
                    id.isBlank() ||
                    !wanted
                        .getValue(
                            "panels",
                        )
                        .add(
                            id,
                        )
                ) {
                    continue
                }

                val panel =
                    arrays
                        .getValue(
                            "panels",
                        )[
                            id
                        ]
                        ?: continue

                jsonStrings(
                    panel.optJSONArray(
                        "control_ids",
                    )
                        ?: JSONArray(),
                ).forEach {
                    controlQueue.add(
                        it,
                    )
                }

                listOf(
                    "action_ids",
                    "init_action_ids",
                ).forEach {
                    key ->
                    jsonStrings(
                        panel.optJSONArray(
                            key,
                        )
                            ?: JSONArray(),
                    ).forEach {
                        queueAction(
                            it,
                        )
                    }
                }
            }

            while (
                controlQueue.isNotEmpty()
            ) {
                val id =
                    controlQueue.removeFirst()

                if (
                    id.isBlank() ||
                    !wanted
                        .getValue(
                            "controls",
                        )
                        .add(
                            id,
                        )
                ) {
                    continue
                }

                val control =
                    arrays
                        .getValue(
                            "controls",
                        )[
                            id
                        ]
                        ?: continue

                val items =
                    control.optJSONArray(
                        "items",
                    )
                        ?: JSONArray()

                for (
                    i in
                    0 until items.length()
                ) {
                    items
                        .optJSONObject(
                            i,
                        )
                        ?.optString(
                            "action_id",
                        )
                        ?.let {
                            queueAction(
                                it,
                            )
                        }
                }

                val options =
                    control.optJSONArray(
                        "options",
                    )
                        ?: JSONArray()

                for (
                    i in
                    0 until options.length()
                ) {
                    options
                        .optJSONObject(
                            i,
                        )
                        ?.optString(
                            "action_id",
                        )
                        ?.let {
                            queueAction(
                                it,
                            )
                        }
                }

                jsonStrings(
                    control.optJSONArray(
                        "action_ids",
                    )
                        ?: JSONArray(),
                ).forEach {
                    queueAction(
                        it,
                    )
                }
            }

            while (
                documentQueue.isNotEmpty()
            ) {
                val id =
                    documentQueue.removeFirst()

                if (
                    id.isBlank() ||
                    !wanted
                        .getValue(
                            "documents",
                        )
                        .add(
                            id,
                        )
                ) {
                    continue
                }

                val document =
                    arrays
                        .getValue(
                            "documents",
                        )[
                            id
                        ]
                        ?: continue

                val parts =
                    document.optJSONArray(
                        "parts",
                    )
                        ?: JSONArray()

                for (
                    i in
                    0 until parts.length()
                ) {
                    parts
                        .optJSONObject(
                            i,
                        )
                        ?.optString(
                            "document_id",
                        )
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?.let {
                            documentQueue.add(
                                it,
                            )
                        }
                }
            }
        }

        val selected =
            linkedMapOf<String, JSONArray>()

        arrays.forEach {
            key,
            map ->
            val output =
                JSONArray()

            val source =
                section.optJSONArray(
                    key,
                )
                    ?: JSONArray()

            for (
                i in
                0 until source.length()
            ) {
                val item =
                    source.optJSONObject(
                        i,
                    )
                        ?: continue

                if (
                    item.optString(
                        "id",
                    ) in
                    wanted.getValue(
                        key,
                    )
                ) {
                    output.put(
                        deepCopy(
                            item,
                        )
                    )
                }
            }

            selected[
                key
            ] =
                output
        }

        if (
            selected
                .getValue(
                    "actions",
                )
                .length() ==
            0
        ) {
            return null
        }

        val bundle =
            JSONObject()
                .put(
                    "menu_items",
                    JSONArray(
                        menuItems.map {
                            deepCopy(
                                it,
                            )
                        }
                    ),
                )

        selected.forEach {
            key,
            value ->
            bundle.put(
                key,
                value,
            )
        }

        val idMap =
            linkedMapOf<String, String>()

        listOf(
            "actions" to
                "action",
            "panels" to
                "panel",
            "controls" to
                "control",
            "documents" to
                "document",
        ).forEach {
            pair ->
            val array =
                bundle.getJSONArray(
                    pair.first,
                )

            for (
                i in
                0 until array.length()
            ) {
                val old =
                    array
                        .getJSONObject(
                            i,
                        )
                        .optString(
                            "id",
                        )

                if (
                    old.isNotBlank()
                ) {
                    idMap[
                        old
                    ] =
                        "vdoc-" +
                            pair.second +
                            "-" +
                            old
                }
            }
        }

        remapIds(
            bundle,
            idMap,
        )

        return bundle
    }

    private fun remapIds(
        value: Any?,
        idMap: Map<String, String>,
    ) {
        when (
            value
        ) {
            is JSONObject -> {
                val keys =
                    value.keys()
                        .asSequence()
                        .toList()

                keys.forEach {
                    key ->
                    val child =
                        value.opt(
                            key,
                        )

                    if (
                        key in
                        singularIdKeys &&
                        child is
                        String
                    ) {
                        value.put(
                            key,
                            idMap[
                                child
                            ]
                                ?: child,
                        )
                        return@forEach
                    }

                    if (
                        key in
                        pluralIdKeys &&
                        child is
                        JSONArray
                    ) {
                        val replaced =
                            JSONArray()

                        for (
                            i in
                            0 until child.length()
                        ) {
                            val raw =
                                child.optString(
                                    i,
                                )

                            replaced.put(
                                idMap[
                                    raw
                                ]
                                    ?: raw,
                            )
                        }

                        value.put(
                            key,
                            replaced,
                        )
                        return@forEach
                    }

                    remapIds(
                        child,
                        idMap,
                    )
                }
            }

            is JSONArray -> {
                for (
                    i in
                    0 until value.length()
                ) {
                    remapIds(
                        value.opt(
                            i,
                        ),
                        idMap,
                    )
                }
            }
        }
    }

    private fun writeShards(
        runtimeTree: JSONObject,
        outputRoot: File,
        packageRoot: File,
    ): File {
        val runtimeRoot =
            File(
                packageRoot,
                RUNTIME_DIRNAME,
            )

        if (
            runtimeRoot.exists()
        ) {
            require(
                runtimeRoot.deleteRecursively(),
            ) {
                "Не вдалося очистити старий Runtime IR."
            }
        }

        val sectionsRoot =
            File(
                runtimeRoot,
                "sections",
            )

        require(
            sectionsRoot.mkdirs(),
        ) {
            "Не вдалося створити Runtime IR sections."
        }

        val documentationRoot =
            File(
                runtimeRoot,
                "documentation",
            )

        require(
            documentationRoot.mkdirs(),
        ) {
            "Не вдалося створити Runtime IR documentation."
        }

        val indexVolumes =
            JSONArray()
        val usedVolumeKeys =
            linkedSetOf<String>()

        val volumes =
            runtimeTree.getJSONArray(
                "volumes",
            )

        for (
            volumeIndex in
            0 until volumes.length()
        ) {
            val volume =
                volumes.getJSONObject(
                    volumeIndex,
                )
            val volumeKey =
                uniqueVolumeKey(
                    volume =
                        volume,
                    fallback =
                        "volume-" +
                            (
                                volumeIndex +
                                    1
                            ),
                    used =
                        usedVolumeKeys,
                )

            val volumeDir =
                File(
                    sectionsRoot,
                    volumeKey,
                )

            require(
                volumeDir.mkdirs(),
            ) {
                "Не вдалося створити Runtime IR volume dir."
            }

            val volumeMeta =
                JSONObject()

            copyOptionalFields(
                source =
                    volume,
                target =
                    volumeMeta,
                keys =
                    listOf(
                        "id",
                        "title",
                        "document_code",
                        "date",
                        "kind",
                        "source_folder",
                    ),
            )

            volume
                .optJSONObject(
                    "classic",
                )
                ?.optString(
                    "entrypoint",
                )
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    volumeMeta.put(
                        "classic_entrypoint",
                        it,
                    )
                }

            var documentationPath:
                String? =
                null

            volume
                .optJSONObject(
                    "modern",
                )
                ?.optJSONObject(
                    "documentation",
                )
                ?.let {
                    documentation ->
                    val file =
                        File(
                            documentationRoot,
                            volumeKey +
                                ".json",
                        )

                    val payload =
                        JSONObject()
                            .put(
                                "runtime_schema_version",
                                SCHEMA_VERSION,
                            )
                            .put(
                                "compiler_phase",
                                COMPILER_PHASE,
                            )
                            .put(
                                "volume",
                                deepCopy(
                                    volumeMeta,
                                ),
                            )
                            .put(
                                "documentation",
                                documentation,
                            )

                    writeJsonFile(
                        file =
                            file,
                        value =
                            payload,
                    )

                    documentationPath =
                        file
                            .relativeTo(
                                outputRoot,
                            )
                            .invariantSeparatorsPath
                }

            val sectionPaths =
                JSONObject()
            val sectionEntries =
                JSONArray()
            val usedSectionKeys =
                linkedSetOf<String>()

            val sections =
                volume
                    .getJSONObject(
                        "modern",
                    )
                    .getJSONArray(
                        "sections",
                    )

            for (
                sectionIndex in
                0 until sections.length()
            ) {
                val section =
                    sections.getJSONObject(
                        sectionIndex,
                    )
                val code =
                    section
                        .optString(
                            "code",
                        )
                        .trim()

                var sectionKey =
                    safeKey(
                        code.ifBlank {
                            "section-" +
                                (
                                    sectionIndex +
                                        1
                                )
                        },
                    )

                if (
                    sectionKey in
                    usedSectionKeys
                ) {
                    sectionKey +=
                        "-" +
                        (
                            sectionIndex +
                                1
                        )
                }

                usedSectionKeys +=
                    sectionKey

                val sectionFile =
                    File(
                        volumeDir,
                        sectionKey +
                            ".json",
                    )

                val payload =
                    JSONObject()
                        .put(
                            "runtime_schema_version",
                            SCHEMA_VERSION,
                        )
                        .put(
                            "compiler_phase",
                            COMPILER_PHASE,
                        )
                        .put(
                            "volume",
                            deepCopy(
                                volumeMeta,
                            ),
                        )
                        .put(
                            "section",
                            section,
                        )

                writeJsonFile(
                    file =
                        sectionFile,
                    value =
                        payload,
                )

                val path =
                    sectionFile
                        .relativeTo(
                            outputRoot,
                        )
                        .invariantSeparatorsPath

                val entrypoint =
                    section
                        .optString(
                            "legacy_entrypoint",
                            section.optString(
                                "entrypoint",
                            ),
                        )
                        .trim()

                sectionEntries.put(
                    JSONObject()
                        .put(
                            "code",
                            code,
                        )
                        .put(
                            "entrypoint",
                            entrypoint,
                        )
                        .put(
                            "path",
                            path,
                        )
                )

                if (
                    !sectionPaths.has(
                        code,
                    )
                ) {
                    sectionPaths.put(
                        code,
                        path,
                    )
                }
            }

            val indexVolume =
                deepCopy(
                    volumeMeta,
                )
                    .put(
                        "section_count",
                        sectionEntries.length(),
                    )
                    .put(
                        "sections",
                        sectionPaths,
                    )
                    .put(
                        "section_entries",
                        sectionEntries,
                    )

            documentationPath
                ?.let {
                    indexVolume.put(
                        "documentation_path",
                        it,
                    )
                }

            indexVolumes.put(
                indexVolume,
            )
        }

        var totalSections =
            0

        for (
            i in
            0 until indexVolumes.length()
        ) {
            totalSections +=
                indexVolumes
                    .getJSONObject(
                        i,
                    )
                    .optInt(
                        "section_count",
                        0,
                    )
        }

        val index =
            JSONObject()
                .put(
                    "schema_version",
                    SCHEMA_VERSION,
                )
                .put(
                    "format",
                    "renault-runtime-ir-index",
                )
                .put(
                    "compiler_phase",
                    COMPILER_PHASE,
                )
                .put(
                    "runtime_data_contract",
                    "sharded-section-json",
                )
                .put(
                    "volume_count",
                    indexVolumes.length(),
                )
                .put(
                    "section_count",
                    totalSections,
                )
                .put(
                    "volumes",
                    indexVolumes,
                )

        val target =
            File(
                packageRoot,
                RUNTIME_INDEX_FILENAME,
            )

        writeJsonFile(
            file =
                target,
            value =
                index,
        )

        return target
    }

    private const val JSON_WRITE_BUFFER_SIZE =
        64 * 1024

    /**
     * Writes org.json values incrementally instead of materializing the whole
     * document as one giant String/ByteArray. Real Renault Runtime IR trees can
     * exceed 100 MB, so JSONObject.toString()/File.writeText() can temporarily
     * double memory usage and hit Android's heap growth limit.
     */
    private fun writeJsonFile(
        file: File,
        value: Any?,
    ) {
        file.parentFile
            ?.let {
                parent ->
                require(
                    parent.isDirectory ||
                        parent.mkdirs(),
                ) {
                    "Не вдалося створити папку для JSON: " +
                        parent
                }
            }

        BufferedWriter(
            OutputStreamWriter(
                file.outputStream(),
                Charsets.UTF_8,
            ),
            JSON_WRITE_BUFFER_SIZE,
        ).use {
            writer ->
            writeJsonValue(
                writer =
                    writer,
                value =
                    value,
            )
            writer.write(
                "\n",
            )
        }
    }

    private fun writeJsonValue(
        writer: Writer,
        value: Any?,
    ) {
        when (
            value
        ) {
            null,
            JSONObject.NULL,
            -> writer.write(
                "null",
            )

            is JSONObject -> {
                writer.write(
                    "{",
                )

                val keys =
                    value.keys()
                var first =
                    true

                while (
                    keys.hasNext()
                ) {
                    val key =
                        keys.next()

                    if (
                        !first
                    ) {
                        writer.write(
                            ",",
                        )
                    }

                    first =
                        false

                    writer.write(
                        JSONObject.quote(
                            key,
                        ),
                    )
                    writer.write(
                        ":",
                    )
                    writeJsonValue(
                        writer =
                            writer,
                        value =
                            value.opt(
                                key,
                            ),
                    )
                }

                writer.write(
                    "}",
                )
            }

            is JSONArray -> {
                writer.write(
                    "[",
                )

                for (
                    index in
                    0 until value.length()
                ) {
                    if (
                        index >
                        0
                    ) {
                        writer.write(
                            ",",
                        )
                    }

                    writeJsonValue(
                        writer =
                            writer,
                        value =
                            value.opt(
                                index,
                            ),
                    )
                }

                writer.write(
                    "]",
                )
            }

            is Number -> writer.write(
                JSONObject.numberToString(
                    value,
                ),
            )

            is Boolean -> writer.write(
                value.toString(),
            )

            is String -> writer.write(
                JSONObject.quote(
                    value,
                ),
            )

            else -> writer.write(
                JSONObject.quote(
                    value.toString(),
                ),
            )
        }
    }

    fun buildCoverage(
        runtimeTree: JSONObject,
    ): JSONObject {
        val global =
            CoverageCounters()
        val unsupported =
            JSONArray()
        val warnings =
            JSONArray()
        val volumesOut =
            JSONArray()

        val volumes =
            runtimeTree.optJSONArray(
                "volumes",
            )
                ?: JSONArray()

        for (
            vi in
            0 until volumes.length()
        ) {
            val volume =
                volumes.getJSONObject(
                    vi,
                )
            val local =
                CoverageCounters()
            var unsupportedCount =
                0
            var warningCount =
                0

            val sections =
                volume
                    .optJSONObject(
                        "modern",
                    )
                    ?.optJSONArray(
                        "sections",
                    )
                    ?: JSONArray()

            for (
                si in
                0 until sections.length()
            ) {
                val section =
                    sections.getJSONObject(
                        si,
                    )
                val code =
                    section.optString(
                        "code",
                    )
                val state =
                    section
                        .optString(
                            "compile_state",
                            "unknown",
                        )

                global.increment(
                    global.compileStates,
                    state,
                )
                local.increment(
                    local.compileStates,
                    state,
                )

                countObjects(
                    section,
                    "panels",
                    "kind",
                    global.panelKinds,
                    local.panelKinds,
                    global,
                    local,
                )
                countObjects(
                    section,
                    "controls",
                    "type",
                    global.controlTypes,
                    local.controlTypes,
                    global,
                    local,
                )
                countObjects(
                    section,
                    "documents",
                    "type",
                    global.documentTypes,
                    local.documentTypes,
                    global,
                    local,
                )

                val controls =
                    section.optJSONArray(
                        "controls",
                    )
                        ?: JSONArray()

                for (
                    ci in
                    0 until controls.length()
                ) {
                    val control =
                        controls.getJSONObject(
                            ci,
                        )

                    if (
                        control.optString(
                            "type",
                        ) !=
                        "action-bar"
                    ) {
                        continue
                    }

                    val items =
                        control.optJSONArray(
                            "items",
                        )
                            ?: JSONArray()

                    for (
                        ii in
                        0 until items.length()
                    ) {
                        val label =
                            items
                                .optJSONObject(
                                    ii,
                                )
                                ?.optString(
                                    "label",
                                )
                                ?.trim()
                                .orEmpty()

                        if (
                            label.isBlank()
                        ) {
                            continue
                        }

                        global.increment(
                            global.menuLabels,
                            label,
                        )
                        local.increment(
                            local.menuLabels,
                            label,
                        )
                    }
                }

                val actions =
                    section.optJSONArray(
                        "actions",
                    )
                        ?: JSONArray()

                for (
                    ai in
                    0 until actions.length()
                ) {
                    val action =
                        actions.getJSONObject(
                            ai,
                        )
                    val type =
                        action.optString(
                            "type",
                            "unknown",
                        )

                    global.increment(
                        global.actionTypes,
                        type,
                    )
                    local.increment(
                        local.actionTypes,
                        type,
                    )

                    val route =
                        action.optString(
                            "route_type",
                        )

                    if (
                        route.isNotBlank()
                    ) {
                        global.increment(
                            global.routeTypes,
                            route,
                        )
                        local.increment(
                            local.routeTypes,
                            route,
                        )
                    }

                    if (
                        type ==
                        "legacy-javascript"
                    ) {
                        unsupportedCount +=
                            1

                        if (
                            unsupported.length() <
                            200
                        ) {
                            unsupported.put(
                                JSONObject()
                                    .put(
                                        "document_code",
                                        volume.opt(
                                            "document_code",
                                        ),
                                    )
                                    .put(
                                        "date",
                                        volume.opt(
                                            "date",
                                        ),
                                    )
                                    .put(
                                        "section_code",
                                        code,
                                    )
                                    .put(
                                        "label",
                                        action.opt(
                                            "label",
                                        ),
                                    )
                                    .put(
                                        "script",
                                        action.opt(
                                            "script",
                                        ),
                                    )
                            )
                        }
                    }
                }

                val sectionWarnings =
                    section.optJSONArray(
                        "warnings",
                    )
                        ?: JSONArray()

                for (
                    wi in
                    0 until sectionWarnings.length()
                ) {
                    warningCount +=
                        1

                    if (
                        warnings.length() <
                        200
                    ) {
                        warnings.put(
                            JSONObject()
                                .put(
                                    "document_code",
                                    volume.opt(
                                        "document_code",
                                    ),
                                )
                                .put(
                                    "date",
                                    volume.opt(
                                        "date",
                                    ),
                                )
                                .put(
                                    "section_code",
                                    code,
                                )
                                .put(
                                    "warning",
                                    sectionWarnings.opt(
                                        wi,
                                    ),
                                )
                        )
                    }
                }
            }

            val volumeOut =
                JSONObject()
                    .put(
                        "id",
                        volume.opt(
                            "id",
                        ),
                    )
                    .put(
                        "document_code",
                        volume.opt(
                            "document_code",
                        ),
                    )
                    .put(
                        "date",
                        volume.opt(
                            "date",
                        ),
                    )
                    .put(
                        "section_count",
                        sections.length(),
                    )
                    .put(
                        "compile_states",
                        sortedCounter(
                            local.compileStates,
                        ),
                    )
                    .put(
                        "panel_kinds",
                        sortedCounter(
                            local.panelKinds,
                        ),
                    )
                    .put(
                        "control_types",
                        sortedCounter(
                            local.controlTypes,
                        ),
                    )
                    .put(
                        "action_types",
                        sortedCounter(
                            local.actionTypes,
                        ),
                    )
                    .put(
                        "route_types",
                        sortedCounter(
                            local.routeTypes,
                        ),
                    )
                    .put(
                        "document_types",
                        sortedCounter(
                            local.documentTypes,
                        ),
                    )
                    .put(
                        "menu_labels",
                        sortedCounter(
                            local.menuLabels,
                        ),
                    )
                    .put(
                        "unsupported_action_count",
                        unsupportedCount,
                    )
                    .put(
                        "warning_count",
                        warningCount,
                    )

            volumesOut.put(
                volumeOut,
            )
        }

        var totalWarnings =
            0

        for (
            i in
            0 until volumesOut.length()
        ) {
            totalWarnings +=
                volumesOut
                    .getJSONObject(
                        i,
                    )
                    .optInt(
                        "warning_count",
                        0,
                    )
        }

        return JSONObject()
            .put(
                "runtime_schema_version",
                runtimeTree.opt(
                    "schema_version",
                ),
            )
            .put(
                "compiler_phase",
                runtimeTree.opt(
                    "compiler_phase",
                ),
            )
            .put(
                "volume_count",
                volumesOut.length(),
            )
            .put(
                "section_count",
                runtimeTree.opt(
                    "section_count",
                ),
            )
            .put(
                "compile_states",
                sortedCounter(
                    global.compileStates,
                ),
            )
            .put(
                "panel_kinds",
                sortedCounter(
                    global.panelKinds,
                ),
            )
            .put(
                "control_types",
                sortedCounter(
                    global.controlTypes,
                ),
            )
            .put(
                "action_types",
                sortedCounter(
                    global.actionTypes,
                ),
            )
            .put(
                "route_types",
                sortedCounter(
                    global.routeTypes,
                ),
            )
            .put(
                "document_types",
                sortedCounter(
                    global.documentTypes,
                ),
            )
            .put(
                "menu_labels",
                sortedCounter(
                    global.menuLabels,
                ),
            )
            .put(
                "unsupported_action_count",
                unsupported.length(),
            )
            .put(
                "unsupported_action_samples",
                unsupported,
            )
            .put(
                "warning_count",
                totalWarnings,
            )
            .put(
                "warning_samples",
                warnings,
            )
            .put(
                "volumes",
                volumesOut,
            )
    }

    private class CoverageCounters {
        val panelKinds =
            linkedMapOf<String, Int>()
        val controlTypes =
            linkedMapOf<String, Int>()
        val actionTypes =
            linkedMapOf<String, Int>()
        val routeTypes =
            linkedMapOf<String, Int>()
        val documentTypes =
            linkedMapOf<String, Int>()
        val menuLabels =
            linkedMapOf<String, Int>()
        val compileStates =
            linkedMapOf<String, Int>()

        fun increment(
            counter: MutableMap<String, Int>,
            key: String,
        ) {
            counter[
                key
            ] =
                (
                    counter[
                        key
                    ]
                        ?: 0
                ) +
                    1
        }
    }

    private fun countObjects(
        section: JSONObject,
        arrayKey: String,
        fieldKey: String,
        globalCounter: MutableMap<String, Int>,
        localCounter: MutableMap<String, Int>,
        global: CoverageCounters,
        local: CoverageCounters,
    ) {
        val array =
            section.optJSONArray(
                arrayKey,
            )
                ?: JSONArray()

        for (
            i in
            0 until array.length()
        ) {
            val value =
                array
                    .getJSONObject(
                        i,
                    )
                    .optString(
                        fieldKey,
                        "unknown",
                    )

            global.increment(
                globalCounter,
                value,
            )
            local.increment(
                localCounter,
                value,
            )
        }
    }

    private fun sortedCounter(
        counter: Map<String, Int>,
    ): JSONObject {
        val result =
            JSONObject()

        counter.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> {
                    it.value
                }.thenBy {
                    it.key
                }
            )
            .forEach {
                result.put(
                    it.key,
                    it.value,
                )
            }

        return result
    }

    private fun uniqueVolumeKey(
        volume: JSONObject,
        fallback: String,
        used: MutableSet<String>,
    ): String {
        val base =
            safeKey(
                volume
                    .optString(
                        "id",
                    )
                    .ifBlank {
                        volume.optString(
                            "document_code",
                        )
                    }
                    .ifBlank {
                        volume.optString(
                            "source_folder",
                        )
                    }
                    .ifBlank {
                        fallback
                    },
            )

        var candidate =
            base
        var suffix =
            2

        while (
            candidate in
            used
        ) {
            candidate =
                base +
                    "-" +
                    suffix
            suffix +=
                1
        }

        used +=
            candidate

        return candidate
    }

    private fun safeKey(
        value: String,
    ): String =
        value
            .trim()
            .replace(
                Regex(
                    "[^A-Za-z0-9._-]+",
                ),
                "-",
            )
            .trim(
                '-',
                '.',
            )
            .ifBlank {
                "item"
            }

    private fun objectMapById(
        array: JSONArray,
    ): Map<String, JSONObject> =
        buildMap {
            for (
                i in
                0 until array.length()
            ) {
                val item =
                    array.optJSONObject(
                        i,
                    )
                        ?: continue
                val id =
                    item
                        .optString(
                            "id",
                        )
                        .trim()

                if (
                    id.isNotBlank()
                ) {
                    put(
                        id,
                        item,
                    )
                }
            }
        }

    private fun jsonStrings(
        array: JSONArray,
    ): List<String> =
        buildList {
            for (
                i in
                0 until array.length()
            ) {
                array
                    .optString(
                        i,
                    )
                    .takeIf {
                        it.isNotBlank()
                    }
                    ?.let {
                        add(
                            it,
                        )
                    }
            }
        }

    private fun copyOptionalFields(
        source: JSONObject,
        target: JSONObject,
        keys: List<String>,
    ) {
        keys.forEach {
            key ->
            if (
                source.has(
                    key,
                ) &&
                !source.isNull(
                    key,
                )
            ) {
                target.put(
                    key,
                    source.get(
                        key,
                    ),
                )
            }
        }
    }

    private fun copyObjectEntries(
        from: JSONObject,
        to: JSONObject,
    ) {
        val keys =
            from.keys()

        while (
            keys.hasNext()
        ) {
            val key =
                keys.next()

            to.put(
                key,
                from.get(
                    key,
                ),
            )
        }
    }

    private fun deepCopy(
        json: JSONObject,
    ): JSONObject =
        JSONObject(
            json.toString(),
        )

    private val documentationCodes =
        setOf(
            "GENE",
            "PLATFUSI",
            "AIDE",
        )

    private val singularIdKeys =
        setOf(
            "id",
            "action_id",
            "source_panel_id",
            "panel_id",
            "document_id",
        )

    private val pluralIdKeys =
        setOf(
            "action_ids",
            "side_effect_action_ids",
            "init_action_ids",
            "control_ids",
        )
}
