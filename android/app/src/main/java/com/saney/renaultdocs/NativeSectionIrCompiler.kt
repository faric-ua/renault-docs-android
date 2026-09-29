package com.saney.renaultdocs

import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URI
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser

/**
 * Kotlin parity implementation of core/section_ir.py.
 *
 * It is intentionally static: legacy JavaScript is inspected for routing
 * patterns but never executed. The output contract matches section-ir-v2.
 */
class NativeSectionIrCompiler(
    outputRoot: File,
    volume: JSONObject,
) {
    private val root =
        outputRoot.canonicalFile

    private val volumeRoot =
        File(
            root,
            volume.optString(
                "source_folder",
            ),
        ).canonicalFile

    private val pageCache =
        object :
            LinkedHashMap<File, ParsedPage?>(
                PAGE_CACHE_LIMIT +
                    1,
                0.75f,
                true,
            ) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<File, ParsedPage?>?,
            ): Boolean =
                size >
                    PAGE_CACHE_LIMIT
        }

    private val seenPanels =
        linkedSetOf<String>()
    private val seenDocuments =
        linkedSetOf<String>()
    private val seenAssets =
        linkedSetOf<String>()
    private val seenSources =
        linkedSetOf<String>()

    private var actionCounter =
        0

    data class ParsedImage(
        val src: String,
        val alt: String,
        val width: String,
        val height: String,
    )

    data class ParsedAnchor(
        val href: String,
        val target: String,
        val onclick: String,
        val text: String,
        val images: List<ParsedImage>,
    )

    data class ParsedOption(
        val value: String,
        val text: String,
    )

    data class ParsedSelect(
        val name: String,
        val id: String,
        val onchange: String,
        val options: List<ParsedOption>,
    )

    data class FrameNode(
        val type: String,
        val name: String = "",
        val src: String = "",
        val rows: String = "",
        val cols: String = "",
        val scrolling: String = "",
        val children: List<FrameNode> = emptyList(),
    )

    data class ParsedHeading(
        val level: Int,
        val text: String,
    )

    data class ParsedCell(
        val text: String,
        val header: Boolean,
    )

    data class ParsedPage(
        val title: String,
        val onload: String,
        val anchors: List<ParsedAnchor>,
        val selects: List<ParsedSelect>,
        val images: List<ParsedImage>,
        val frameRoots: List<FrameNode>,
        val headings: List<ParsedHeading>,
        val tables: List<List<List<ParsedCell>>>,
    )

    fun compileSection(
        section: JSONObject,
    ): JSONObject {
        resetState()

        val entrypoint =
            section.optString(
                "entrypoint",
                section.optString(
                    "legacy_entrypoint",
                ),
            ).trim()

        val menuFile =
            File(
                root,
                entrypoint,
            ).canonicalFile

        val result =
            JSONObject()
                .put(
                    "panels",
                    JSONArray(),
                )
                .put(
                    "controls",
                    JSONArray(),
                )
                .put(
                    "actions",
                    JSONArray(),
                )
                .put(
                    "documents",
                    JSONArray(),
                )
                .put(
                    "assets",
                    JSONArray(),
                )
                .put(
                    "source_files",
                    JSONArray(),
                )
                .put(
                    "warnings",
                    JSONArray(),
                )
                .put(
                    "compile_state",
                    "section-ir-v2",
                )

        if (
            !isValidFile(
                menuFile,
            )
        ) {
            result.put(
                "compile_state",
                "navigation-indexed",
            )
            result
                .getJSONArray(
                    "warnings",
                )
                .put(
                    "legacy-entrypoint-missing",
                )
            return result
        }

        compilePanel(
            path =
                menuFile,
            result =
                result,
            kind =
                "menu",
            explicitPanelId =
                "menu",
        )

        if (
            result
                .getJSONArray(
                    "warnings",
                )
                .length() ==
            0
        ) {
            result.remove(
                "warnings",
            )
        }

        return result
    }

    private fun resetState() {
        seenPanels.clear()
        seenDocuments.clear()
        seenAssets.clear()
        seenSources.clear()
        actionCounter =
            0
    }

    private fun compilePanel(
        path: File,
        result: JSONObject,
        kind: String? = null,
        explicitPanelId: String? = null,
    ): String? {
        if (
            !isValidFile(
                path,
            )
        ) {
            return null
        }

        val relative =
            relative(
                path,
            )
        val panelId =
            explicitPanelId
                ?: panelId(
                    path,
                )

        if (
            panelId in
            seenPanels
        ) {
            return panelId
        }

        val parsed =
            parse(
                path,
            )

        if (
            parsed ==
            null
        ) {
            result
                .getJSONArray(
                    "warnings",
                )
                .put(
                    "panel-parse-failed:" +
                        relative,
                )
            return null
        }

        seenPanels +=
            panelId

        recordSource(
            path =
                path,
            result =
                result,
        )
        recordImages(
            parsed =
                parsed,
            currentFile =
                path,
            result =
                result,
        )

        val panel =
            JSONObject()
                .put(
                    "id",
                    panelId,
                )
                .put(
                    "kind",
                    kind
                        ?: panelKind(
                            path,
                        ),
                )
                .put(
                    "title",
                    parsed.title,
                )
                .put(
                    "source",
                    relative,
                )
                .put(
                    "control_ids",
                    JSONArray(),
                )
                .put(
                    "action_ids",
                    JSONArray(),
                )

        val initActions =
            compileInlineRoutes(
                script =
                    parsed.onload,
                currentFile =
                    path,
                result =
                    result,
                sourcePanelId =
                    panelId,
                event =
                    "load",
            )

        if (
            initActions.isNotEmpty()
        ) {
            panel.put(
                "init_action_ids",
                JSONArray(
                    initActions,
                ),
            )
        }

        parsed.selects
            .forEachIndexed {
                index,
                parsedSelect ->
                val control =
                    compileSelect(
                        parsedSelect =
                            parsedSelect,
                        currentFile =
                            path,
                        result =
                            result,
                        panelId =
                            panelId,
                        index =
                            index +
                                1,
                    )

                result
                    .getJSONArray(
                        "controls",
                    )
                    .put(
                        control,
                    )
                panel
                    .getJSONArray(
                        "control_ids",
                    )
                    .put(
                        control.getString(
                            "id",
                        )
                    )
            }

        val anchorActionIds =
            mutableListOf<String>()

        parsed.anchors
            .forEachIndexed {
                index,
                anchor ->
                compileAnchor(
                    anchor =
                        anchor,
                    currentFile =
                        path,
                    result =
                        result,
                    sourcePanelId =
                        panelId,
                    index =
                        index +
                            1,
                )
                    ?.let {
                        anchorActionIds +=
                            it
                    }
            }

        anchorActionIds
            .forEach {
                panel
                    .getJSONArray(
                        "action_ids",
                    )
                    .put(
                        it,
                    )
            }

        if (
            panel.getString(
                "kind",
            ) ==
            "menu" &&
            parsed.anchors.isNotEmpty()
        ) {
            val items =
                JSONArray()

            parsed.anchors
                .forEach {
                    anchor ->
                    val actionId =
                        actionForAnchor(
                            anchor =
                                anchor,
                            currentFile =
                                path,
                            result =
                                result,
                            sourcePanelId =
                                panelId,
                        )
                            ?: return@forEach

                    items.put(
                        JSONObject()
                            .put(
                                "label",
                                anchorLabel(
                                    anchor,
                                ),
                            )
                            .put(
                                "action_id",
                                actionId,
                            )
                    )
                }

            val linkedImages =
                parsed.anchors
                    .flatMap {
                        it.images
                    }
                    .map {
                        it.src
                    }
                    .toSet()

            parsed.images
                .forEach {
                    image ->
                    if (
                        image.src.isBlank() ||
                        image.src in
                        linkedImages
                    ) {
                        return@forEach
                    }

                    if (
                        imageLabel(
                            image.src,
                        )
                            .equals(
                                "blank",
                                ignoreCase =
                                    true,
                            )
                    ) {
                        items.put(
                            JSONObject()
                                .put(
                                    "label",
                                    "blank",
                                )
                                .put(
                                    "enabled",
                                    false,
                                )
                        )
                    }
                }

            if (
                items.length() >
                0
            ) {
                val controlId =
                    panelId +
                        "-toolbar"

                result
                    .getJSONArray(
                        "controls",
                    )
                    .put(
                        JSONObject()
                            .put(
                                "id",
                                controlId,
                            )
                            .put(
                                "type",
                                "action-bar",
                            )
                            .put(
                                "panel_id",
                                panelId,
                            )
                            .put(
                                "items",
                                items,
                            )
                    )

                val controlIds =
                    panel.getJSONArray(
                        "control_ids",
                    )
                val reordered =
                    JSONArray()
                        .put(
                            controlId,
                        )

                for (
                    i in
                    0 until controlIds.length()
                ) {
                    reordered.put(
                        controlIds.get(
                            i,
                        )
                    )
                }

                panel.put(
                    "control_ids",
                    reordered,
                )
            }
        } else if (
            parsed.selects.isEmpty() &&
            anchorActionIds.isNotEmpty() &&
            panel.getString(
                "kind",
            ) in
            setOf(
                "pc",
                "general",
            )
        ) {
            val controlId =
                panelId +
                    "-documents"

            result
                .getJSONArray(
                    "controls",
                )
                .put(
                    JSONObject()
                        .put(
                            "id",
                            controlId,
                        )
                        .put(
                            "type",
                            "document-list",
                        )
                        .put(
                            "panel_id",
                            panelId,
                        )
                        .put(
                            "action_ids",
                            JSONArray(
                                anchorActionIds,
                            ),
                        )
                )

            panel
                .getJSONArray(
                    "control_ids",
                )
                .put(
                    controlId,
                )
        }

        result
            .getJSONArray(
                "panels",
            )
            .put(
                panel,
            )

        return panelId
    }

    private fun compileSelect(
        parsedSelect: ParsedSelect,
        currentFile: File,
        result: JSONObject,
        panelId: String,
        index: Int,
    ): JSONObject {
        val name =
            parsedSelect.name
                .ifBlank {
                    parsedSelect.id
                }
                .ifBlank {
                    "select-" +
                        index
                }

        val controlId =
            panelId +
                "-" +
                slug(
                    name,
                )

        val onchange =
            parsedSelect.onchange
        val handler =
            handlerName(
                onchange,
            )
        val targetSurface =
            surfaceFromScript(
                onchange,
            )
                .ifBlank {
                    "doc"
                }

        val options =
            JSONArray()

        parsedSelect.options
            .forEach {
                option ->
                val label =
                    cleanText(
                        option.text,
                    )
                val value =
                    option.value.trim()

                val entry =
                    JSONObject()
                        .put(
                            "label",
                            label,
                        )
                        .put(
                            "legacy_value",
                            value,
                        )

                if (
                    isNonDocumentOption(
                        value =
                            value,
                        label =
                            label,
                    )
                ) {
                    entry
                        .put(
                            "kind",
                            nonDocumentKind(
                                label,
                            ),
                        )
                        .put(
                            "enabled",
                            false,
                        )

                    options.put(
                        entry,
                    )
                    return@forEach
                }

                val resolved =
                    resolveWithFragment(
                        currentFile =
                            currentFile,
                        reference =
                            value,
                    )

                if (
                    resolved.first ==
                    null
                ) {
                    entry
                        .put(
                            "kind",
                            "unknown",
                        )
                        .put(
                            "enabled",
                            false,
                        )

                    options.put(
                        entry,
                    )
                    return@forEach
                }

                val actionId =
                    addRouteAction(
                        result =
                            result,
                        sourcePanelId =
                            panelId,
                        target =
                            resolved.first!!,
                        targetSurface =
                            targetSurface,
                        label =
                            label,
                        legacyHandler =
                            handler,
                        legacyFragment =
                            resolved.second,
                        event =
                            "select",
                    )

                entry
                    .put(
                        "kind",
                        "route",
                    )
                    .put(
                        "action_id",
                        actionId,
                    )
                    .put(
                        "enabled",
                        true,
                    )

                options.put(
                    entry,
                )
            }

        return JSONObject()
            .put(
                "id",
                controlId,
            )
            .put(
                "type",
                "select",
            )
            .put(
                "panel_id",
                panelId,
            )
            .put(
                "name",
                name,
            )
            .put(
                "legacy_handler",
                handler,
            )
            .put(
                "target_surface",
                targetSurface,
            )
            .put(
                "options",
                options,
            )
    }

    private fun compileAnchor(
        anchor: ParsedAnchor,
        currentFile: File,
        result: JSONObject,
        sourcePanelId: String,
        index: Int,
    ): String? {
        val href =
            anchor.href.trim()

        if (
            href.isBlank()
        ) {
            return null
        }

        if (
            href.startsWith(
                "javascript:",
                ignoreCase =
                    true,
            )
        ) {
            val script =
                href.substringAfter(
                    ':',
                )
            val handler =
                handlerName(
                    script,
                )

            if (
                handler.equals(
                    "imprimer",
                    ignoreCase =
                        true,
                )
            ) {
                return appendAction(
                    result =
                        result,
                    action =
                        JSONObject()
                            .put(
                                "type",
                                "print",
                            )
                            .put(
                                "source_panel_id",
                                sourcePanelId,
                            )
                            .put(
                                "label",
                                anchorLabel(
                                    anchor,
                                )
                                    .ifBlank {
                                        "print"
                                    },
                            )
                            .put(
                                "legacy_handler",
                                handler,
                            ),
                )
            }

            return appendAction(
                result =
                    result,
                action =
                    JSONObject()
                        .put(
                            "type",
                            "legacy-javascript",
                        )
                        .put(
                            "source_panel_id",
                            sourcePanelId,
                        )
                        .put(
                            "label",
                            anchorLabel(
                                anchor,
                            ),
                        )
                        .put(
                            "script",
                            script,
                        ),
            )
        }

        val resolved =
            resolveWithFragment(
                currentFile =
                    currentFile,
                reference =
                    href,
            )

        val target =
            resolved.first
                ?: return null

        val targetSurface =
            anchor.target
                .trim()
                .ifBlank {
                    "self"
                }

        val actionId =
            addRouteAction(
                result =
                    result,
                sourcePanelId =
                    sourcePanelId,
                target =
                    target,
                targetSurface =
                    targetSurface,
                label =
                    anchorLabel(
                        anchor,
                    )
                        .ifBlank {
                            "action-" +
                                index
                        },
                legacyFragment =
                    resolved.second,
                event =
                    "click",
            )

        val sideEffects =
            compileInlineRoutes(
                script =
                    anchor.onclick,
                currentFile =
                    currentFile,
                result =
                    result,
                sourcePanelId =
                    sourcePanelId,
                event =
                    "click-side-effect",
            )

        if (
            sideEffects.isNotEmpty()
        ) {
            findAction(
                result =
                    result,
                actionId =
                    actionId,
            )
                ?.put(
                    "side_effect_action_ids",
                    JSONArray(
                        sideEffects,
                    ),
                )
        }

        return actionId
    }

    private fun actionForAnchor(
        anchor: ParsedAnchor,
        currentFile: File,
        result: JSONObject,
        sourcePanelId: String,
    ): String? {
        val href =
            anchor.href.trim()
        val label =
            anchorLabel(
                anchor,
            )

        val actions =
            result.getJSONArray(
                "actions",
            )

        for (
            i in
            0 until actions.length()
        ) {
            val action =
                actions.getJSONObject(
                    i,
                )

            if (
                action.optString(
                    "source_panel_id",
                ) ==
                sourcePanelId &&
                action.optString(
                    "label",
                ) ==
                label &&
                action.optString(
                    "legacy_target",
                ) ==
                href
            ) {
                return action.getString(
                    "id",
                )
            }
        }

        val target =
            resolveWithFragment(
                currentFile =
                    currentFile,
                reference =
                    href,
            ).first
                ?: return null

        val relative =
            relative(
                target,
            )

        for (
            i in
            0 until actions.length()
        ) {
            val action =
                actions.getJSONObject(
                    i,
                )

            if (
                action.optString(
                    "source_panel_id",
                ) ==
                sourcePanelId &&
                action.optString(
                    "target",
                ) ==
                relative
            ) {
                return action.getString(
                    "id",
                )
            }
        }

        return null
    }

    private fun addRouteAction(
        result: JSONObject,
        sourcePanelId: String,
        target: File,
        targetSurface: String,
        label: String,
        legacyHandler: String = "",
        legacyFragment: String = "",
        event: String = "",
    ): String {
        val suffix =
            target
                .extension
                .lowercase(
                    Locale.ROOT,
                )
        val relative =
            relative(
                target,
            )

        val action =
            JSONObject()
                .put(
                    "type",
                    "route",
                )
                .put(
                    "source_panel_id",
                    sourcePanelId,
                )
                .put(
                    "label",
                    label,
                )
                .put(
                    "event",
                    event,
                )
                .put(
                    "target_surface",
                    targetSurface,
                )
                .put(
                    "target",
                    relative,
                )

        if (
            legacyHandler.isNotBlank()
        ) {
            action.put(
                "legacy_handler",
                legacyHandler,
            )
        }

        if (
            legacyFragment.isNotBlank()
        ) {
            action.put(
                "legacy_fragment",
                legacyFragment,
            )
        }

        if (
            targetSurface.equals(
                "nav",
                ignoreCase =
                    true,
            ) &&
            suffix in
            textExtensions
        ) {
            val nestedPanelId =
                compilePanel(
                    path =
                        target,
                    result =
                        result,
                )

            action.put(
                "route_type",
                if (
                    nestedPanelId !=
                    null
                ) {
                    "open-panel"
                } else {
                    "legacy-page"
                },
            )

            if (
                nestedPanelId !=
                null
            ) {
                action.put(
                    "panel_id",
                    nestedPanelId,
                )
            }
        } else if (
            suffix in
            (
                pdfExtensions +
                    textExtensions
            )
        ) {
            val documentId =
                compileDocument(
                    path =
                        target,
                    result =
                        result,
                )

            action.put(
                "route_type",
                "open-document",
            )

            if (
                documentId !=
                null
            ) {
                action.put(
                    "document_id",
                    documentId,
                )
            }
        } else {
            action.put(
                "route_type",
                "resource",
            )
        }

        return appendAction(
            result =
                result,
            action =
                action,
        )
    }

    private fun compileInlineRoutes(
        script: String,
        currentFile: File,
        result: JSONObject,
        sourcePanelId: String,
        event: String,
    ): List<String> {
        val ids =
            mutableListOf<String>()

        parentLocationRegex
            .findAll(
                script,
            )
            .forEach {
                match ->
                val surface =
                    match.groupValues[
                        1
                    ]
                val reference =
                    match.groupValues[
                        2
                    ]

                val resolved =
                    resolveWithFragment(
                        currentFile =
                            currentFile,
                        reference =
                            reference,
                    )
                val target =
                    resolved.first
                        ?: return@forEach
                val relative =
                    relative(
                        target,
                    )

                val action =
                    JSONObject()
                        .put(
                            "type",
                            "set-surface-location",
                        )
                        .put(
                            "source_panel_id",
                            sourcePanelId,
                        )
                        .put(
                            "event",
                            event,
                        )
                        .put(
                            "target_surface",
                            surface,
                        )
                        .put(
                            "target",
                            relative,
                        )

                if (
                    resolved.second.isNotBlank()
                ) {
                    action.put(
                        "legacy_fragment",
                        resolved.second,
                    )
                }

                if (
                    isBlankPath(
                        relative,
                    )
                ) {
                    action.put(
                        "semantic",
                        "clear-surface",
                    )
                }

                ids +=
                    appendAction(
                        result =
                            result,
                        action =
                            action,
                    )
            }

        return ids
    }

    private fun compileDocument(
        path: File,
        result: JSONObject,
    ): String? {
        if (
            !isValidFile(
                path,
            )
        ) {
            return null
        }

        val documentId =
            "doc-" +
                stableId(
                    relative(
                        path,
                    ),
                )

        if (
            documentId in
            seenDocuments
        ) {
            return documentId
        }

        seenDocuments +=
            documentId

        val relative =
            relative(
                path,
            )
        val suffix =
            path.extension.lowercase(
                Locale.ROOT,
            )

        if (
            suffix in
            pdfExtensions
        ) {
            result
                .getJSONArray(
                    "documents",
                )
                .put(
                    JSONObject()
                        .put(
                            "id",
                            documentId,
                        )
                        .put(
                            "type",
                            "pdf",
                        )
                        .put(
                            "path",
                            relative,
                        )
                )

            return documentId
        }

        val parsed =
            parse(
                path,
            )
                ?: return null

        recordSource(
            path =
                path,
            result =
                result,
        )
        recordImages(
            parsed =
                parsed,
            currentFile =
                path,
            result =
                result,
        )

        if (
            parsed.frameRoots.isNotEmpty()
        ) {
            val parts =
                JSONArray()

            fun walk(
                nodes: List<FrameNode>,
            ) {
                nodes.forEach {
                    node ->
                    if (
                        node.type ==
                        "frameset"
                    ) {
                        walk(
                            node.children,
                        )
                        return@forEach
                    }

                    val resolved =
                        resolveWithFragment(
                            currentFile =
                                path,
                            reference =
                                node.src,
                        )
                    val target =
                        resolved.first
                            ?: return@forEach
                    val nestedId =
                        compileDocument(
                            path =
                                target,
                            result =
                                result,
                        )

                    val part =
                        JSONObject()
                            .put(
                                "role",
                                node.name
                                    .ifBlank {
                                        "content"
                                    },
                            )
                            .put(
                                "target",
                                relative(
                                    target,
                                ),
                            )

                    if (
                        nestedId !=
                        null
                    ) {
                        part.put(
                            "document_id",
                            nestedId,
                        )
                    }

                    if (
                        resolved.second.isNotBlank()
                    ) {
                        part.put(
                            "legacy_fragment",
                            resolved.second,
                        )
                    }

                    parts.put(
                        part,
                    )
                }
            }

            walk(
                parsed.frameRoots,
            )

            val document =
                JSONObject()
                    .put(
                        "id",
                        documentId,
                    )
                    .put(
                        "type",
                        "composite-document",
                    )
                    .put(
                        "path",
                        relative,
                    )
                    .put(
                        "title",
                        parsed.title,
                    )
                    .put(
                        "parts",
                        parts,
                    )

            frameLayout(
                parsed.frameRoots,
            )
                ?.let {
                    document.put(
                        "layout",
                        it,
                    )
                }

            result
                .getJSONArray(
                    "documents",
                )
                .put(
                    document,
                )

            return documentId
        }

        val headings =
            JSONArray()

        parsed.headings
            .filter {
                it.text.isNotBlank()
            }
            .forEach {
                heading ->
                headings.put(
                    JSONObject()
                        .put(
                            "level",
                            heading.level,
                        )
                        .put(
                            "text",
                            cleanText(
                                heading.text,
                            ),
                        )
                )
            }

        val document =
            JSONObject()
                .put(
                    "id",
                    documentId,
                )
                .put(
                    "type",
                    "structured-html",
                )
                .put(
                    "path",
                    relative,
                )
                .put(
                    "title",
                    parsed.title,
                )
                .put(
                    "headings",
                    headings,
                )
                .put(
                    "tables",
                    structuredTables(
                        parsed.tables,
                    ),
                )

        if (
            parsed.anchors.any {
                it.href.startsWith(
                    "javascript:imprimer",
                    ignoreCase =
                        true,
                )
            }
        ) {
            document.put(
                "native_actions",
                JSONArray()
                    .put(
                        JSONObject()
                            .put(
                                "type",
                                "print",
                            )
                    ),
            )
        }

        result
            .getJSONArray(
                "documents",
            )
            .put(
                document,
            )

        return documentId
    }

    private fun structuredTables(
        tables: List<List<List<ParsedCell>>>,
    ): JSONArray {
        val result =
            JSONArray()

        tables.forEach {
            table ->
            val rows =
                table
                    .mapNotNull {
                        row ->
                        val cells =
                            row.map {
                                cell ->
                                JSONObject()
                                    .put(
                                        "text",
                                        cleanText(
                                            cell.text,
                                        ),
                                    )
                                    .put(
                                        "header",
                                        cell.header,
                                    )
                            }

                        if (
                            cells.isEmpty() ||
                            cells.none {
                                it.optString(
                                    "text",
                                ).isNotBlank() ||
                                    it.optBoolean(
                                        "header",
                                        false,
                                    )
                            }
                        ) {
                            null
                        } else {
                            cells
                        }
                    }

            if (
                rows.isEmpty()
            ) {
                return@forEach
            }

            result.put(
                JSONObject()
                    .put(
                        "rows",
                        JSONArray(
                            normalizePinTableRows(
                                rows,
                            ).map {
                                JSONArray(
                                    it,
                                )
                            },
                        ),
                    )
            )
        }

        return result
    }

    fun normalizePinTableRows(
        rows: List<List<JSONObject>>,
    ): List<List<JSONObject>> {
        val columnCount =
            rows.maxOfOrNull {
                it.size
            }
                ?: 0

        if (
            columnCount !=
            4
        ) {
            return rows
        }

        val normalized =
            rows.map {
                row ->
                buildList {
                    addAll(
                        row,
                    )

                    repeat(
                        columnCount -
                            row.size,
                    ) {
                        add(
                            JSONObject()
                                .put(
                                    "text",
                                    "",
                                )
                                .put(
                                    "header",
                                    false,
                                )
                        )
                    }
                }
            }

        if (
            normalized
                .first()
                .any {
                    it.optBoolean(
                        "header",
                        false,
                    )
                }
        ) {
            return normalized
        }

        val bodyIndex =
            normalized
                .take(
                    3,
                )
                .indexOfFirst {
                    looksLikePinBodyRow(
                        it,
                    )
                }

        if (
            bodyIndex <
            0
        ) {
            return normalized
        }

        val semanticHeader =
            listOf(
                "№",
                "мм²",
                "Код",
                "Опис",
            ).map {
                value ->
                JSONObject()
                    .put(
                        "text",
                        value,
                    )
                    .put(
                        "header",
                        true,
                    )
            }

        return if (
            bodyIndex ==
            0
        ) {
            listOf(
                semanticHeader,
            ) +
                normalized
        } else {
            listOf(
                semanticHeader,
            ) +
                normalized.drop(
                    bodyIndex,
                )
        }
    }

    fun looksLikePinBodyRow(
        row: List<JSONObject>,
    ): Boolean {
        if (
            row.size <
            4
        ) {
            return false
        }

        val values =
            row.take(
                4,
            )
                .map {
                    it.optString(
                        "text",
                    ).trim()
                }

        val firstIsPin =
            Regex(
                """[A-Za-z]?\d+[A-Za-z0-9.-]*"""
            ).matches(
                values[
                    0
                ],
            )

        val secondIsCrossSection =
            Regex(
                """\d+(?:[.,]\d+)?"""
            ).matches(
                values[
                    1
                ],
            )

        val thirdIsWireCode =
            values[
                2
            ].isBlank() ||
                Regex(
                    """[A-Za-z0-9.+/_-]{1,16}"""
                ).matches(
                    values[
                        2
                    ],
                )

        return values[
            3
        ].isNotBlank() &&
            firstIsPin &&
            secondIsCrossSection &&
            thirdIsWireCode
    }

    private fun recordImages(
        parsed: ParsedPage,
        currentFile: File,
        result: JSONObject,
    ) {
        parsed.images
            .forEach {
                image ->
                val target =
                    resolveWithFragment(
                        currentFile =
                            currentFile,
                        reference =
                            image.src,
                    ).first
                        ?: return@forEach

                val relative =
                    relative(
                        target,
                    )

                if (
                    !seenAssets.add(
                        relative,
                    )
                ) {
                    return@forEach
                }

                val upper =
                    relative.uppercase(
                        Locale.ROOT,
                    )

                val role =
                    when {
                        "/BOUTONS/" in
                            upper ->
                            "button-icon"

                        "/VIGNETTE/" in
                            upper ->
                            "thumbnail"

                        "/ICONES/" in
                            upper ->
                            "content-icon"

                        else ->
                            "image"
                    }

                result
                    .getJSONArray(
                        "assets",
                    )
                    .put(
                        JSONObject()
                            .put(
                                "type",
                                "image",
                            )
                            .put(
                                "role",
                                role,
                            )
                            .put(
                                "path",
                                relative,
                            )
                    )
            }
    }

    private fun recordSource(
        path: File,
        result: JSONObject,
    ) {
        val relative =
            relative(
                path,
            )

        if (
            !seenSources.add(
                relative,
            )
        ) {
            return
        }

        result
            .getJSONArray(
                "source_files",
            )
            .put(
                relative,
            )
    }

    private fun parse(
        path: File,
    ): ParsedPage? {
        val canonical =
            path.canonicalFile

        if (
            canonical in
            pageCache
        ) {
            return pageCache[
                canonical
            ]
        }

        if (
            !isValidFile(
                canonical,
            ) ||
            canonical.extension
                .lowercase(
                    Locale.ROOT,
                ) !in
            textExtensions
        ) {
            pageCache[
                canonical
            ] =
                null
            return null
        }

        val raw =
            runCatching {
                canonical.readBytes()
            }.getOrNull()

        if (
            raw ==
            null
        ) {
            pageCache[
                canonical
            ] =
                null
            return null
        }

        val document =
            runCatching {
                Jsoup.parse(
                    decodeHtml(
                        raw,
                    )
                )
            }.getOrNull()

        if (
            document ==
            null
        ) {
            pageCache[
                canonical
            ] =
                null
            return null
        }

        val parsed =
            parseDocument(
                document,
            )

        pageCache[
            canonical
        ] =
            parsed

        return parsed
    }

    private fun parseDocument(
        document: Document,
    ): ParsedPage {
        val anchors =
            document
                .getElementsByTag(
                    "a",
                )
                .map {
                    element ->
                    ParsedAnchor(
                        href =
                            element.attr(
                                "href",
                            ).trim(),
                        target =
                            element.attr(
                                "target",
                            ).trim(),
                        onclick =
                            element.attr(
                                "onclick",
                            ).trim(),
                        text =
                            cleanText(
                                element.text(),
                            ),
                        images =
                            element
                                .getElementsByTag(
                                    "img",
                                )
                                .map {
                                    image ->
                                    parseImage(
                                        image,
                                    )
                                },
                    )
                }

        val selects =
            document
                .getElementsByTag(
                    "select",
                )
                .map {
                    element ->
                    ParsedSelect(
                        name =
                            element.attr(
                                "name",
                            ).trim(),
                        id =
                            element.attr(
                                "id",
                            ).trim(),
                        onchange =
                            element.attr(
                                "onchange",
                            ).trim(),
                        options =
                            element
                                .getElementsByTag(
                                    "option",
                                )
                                .map {
                                    option ->
                                    ParsedOption(
                                        value =
                                            option.attr(
                                                "value",
                                            ).trim(),
                                        text =
                                            cleanText(
                                                option.text(),
                                            ),
                                    )
                                },
                    )
                }

        val images =
            document
                .getElementsByTag(
                    "img",
                )
                .map {
                    parseImage(
                        it,
                    )
                }

        val headings =
            (1..6)
                .flatMap {
                    level ->
                    document
                        .getElementsByTag(
                            "h" +
                                level,
                        )
                        .map {
                            element ->
                            ParsedHeading(
                                level =
                                    level,
                                text =
                                    cleanText(
                                        element.text(),
                                    ),
                            )
                        }
                }

        val tables =
            document
                .getElementsByTag(
                    "table",
                )
                .map {
                    table ->
                    parseTable(
                        table,
                    )
                }

        val frameRoots =
            parseFrameRoots(
                document,
            )

        return ParsedPage(
            title =
                cleanText(
                    document.title(),
                ),
            onload =
                document.body()
                    ?.attr(
                        "onload",
                    )
                    ?.trim()
                    .orEmpty(),
            anchors =
                anchors,
            selects =
                selects,
            images =
                images,
            frameRoots =
                frameRoots,
            headings =
                headings,
            tables =
                tables,
        )
    }

    private fun parseImage(
        element: Element,
    ): ParsedImage =
        ParsedImage(
            src =
                element.attr(
                    "src",
                ).trim(),
            alt =
                element.attr(
                    "alt",
                ).trim(),
            width =
                element.attr(
                    "width",
                ).trim(),
            height =
                element.attr(
                    "height",
                ).trim(),
        )

    private fun parseFrameRoots(
        document: Document,
    ): List<FrameNode> {
        val nodes =
            document
                .getAllElements()
                .filter {
                    it.normalName() in
                        setOf(
                            "frameset",
                            "frame",
                            "iframe",
                        )
                }

        return nodes
            .filter {
                element ->
                generateSequence(
                    element.parent(),
                ) {
                    it.parent()
                }
                    .none {
                        parent ->
                        parent.normalName() in
                            setOf(
                                "frameset",
                                "frame",
                                "iframe",
                            )
                    }
            }
            .map {
                parseFrameNode(
                    it,
                )
            }
    }

    private fun parseFrameNode(
        element: Element,
    ): FrameNode {
        if (
            element.normalName() ==
            "frameset"
        ) {
            return FrameNode(
                type =
                    "frameset",
                rows =
                    element.attr(
                        "rows",
                    ).trim(),
                cols =
                    element.attr(
                        "cols",
                    ).trim(),
                children =
                    element.children()
                        .filter {
                            it.normalName() in
                                setOf(
                                    "frameset",
                                    "frame",
                                    "iframe",
                                )
                        }
                        .map {
                            parseFrameNode(
                                it,
                            )
                        },
            )
        }

        return FrameNode(
            type =
                element.normalName(),
            name =
                element.attr(
                    "name",
                ).trim(),
            src =
                element.attr(
                    "src",
                ).trim(),
            scrolling =
                element.attr(
                    "scrolling",
                ).trim(),
        )
    }

    private fun parseTable(
        table: Element,
    ): List<List<ParsedCell>> {
        val rows =
            table
                .getElementsByTag(
                    "tr",
                )
                .filter {
                    row ->
                    nearestAncestor(
                        row,
                        "table",
                    ) ===
                        table
                }

        return rows.map {
            row ->
            row.children()
                .filter {
                    it.normalName() in
                        setOf(
                            "td",
                            "th",
                        )
                }
                .map {
                    cell ->
                    ParsedCell(
                        text =
                            cleanText(
                                cell.text(),
                            ),
                        header =
                            cell.normalName() ==
                                "th",
                    )
                }
        }
    }

    private fun nearestAncestor(
        element: Element,
        tagName: String,
    ): Element? =
        generateSequence(
            element.parent(),
        ) {
            it.parent()
        }
            .firstOrNull {
                it.normalName() ==
                    tagName
            }

    private fun resolveWithFragment(
        currentFile: File,
        reference: String,
    ): Pair<File?, String> {
        val cleaned =
            Parser
                .unescapeEntities(
                    reference,
                    false,
                )
                .trim()

        if (
            cleaned.isBlank()
        ) {
            return null to
                ""
        }

        val lowered =
            cleaned.lowercase(
                Locale.ROOT,
            )

        if (
            listOf(
                "http:",
                "https:",
                "mailto:",
                "data:",
                "javascript:",
            ).any {
                lowered.startsWith(
                    it,
                )
            }
        ) {
            return null to
                ""
        }

        val fragment =
            cleaned
                .substringAfter(
                    '#',
                    "",
                )
                .substringBefore(
                    '?',
                )

        val pathPart =
            cleaned
                .substringBefore(
                    '#',
                )
                .substringBefore(
                    '?',
                )
                .replace(
                    '\\',
                    '/',
                )

        val decoded =
            percentDecode(
                pathPart,
            )
                .trim()

        if (
            decoded.isBlank()
        ) {
            return null to
                fragment
        }

        val target =
            if (
                decoded.startsWith(
                    '/',
                )
            ) {
                File(
                    root,
                    decoded.trimStart(
                        '/',
                    ),
                )
            } else {
                File(
                    currentFile.parentFile,
                    decoded,
                )
            }
                .canonicalFile

        if (
            !isWithin(
                target,
                root,
            )
        ) {
            return null to
                fragment
        }

        return target to
            fragment
    }

    private fun panelKind(
        path: File,
    ): String {
        val parts =
            path.toPath()
                .map {
                    it.toString()
                        .uppercase(
                            Locale.ROOT,
                        )
                }

        return when {
            "SCH" in
                parts ->
                "schematic"

            "NM" in
                parts ->
                "nomenclature"

            "PC" in
                parts ->
                "pc"

            path.nameWithoutExtension
                .equals(
                    "GENERAL",
                    ignoreCase =
                        true,
                ) ->
                "general"

            else ->
                "legacy-panel"
        }
    }

    private fun panelId(
        path: File,
    ): String {
        val kind =
            panelKind(
                path,
            )

        return if (
            kind !=
            "legacy-panel"
        ) {
            kind
        } else {
            "panel-" +
                stableId(
                    relative(
                        path,
                    ),
                )
        }
    }

    private fun appendAction(
        result: JSONObject,
        action: JSONObject,
    ): String {
        actionCounter +=
            1

        val actionId =
            "action-" +
                actionCounter

        action.put(
            "id",
            actionId,
        )

        result
            .getJSONArray(
                "actions",
            )
            .put(
                action,
            )

        return actionId
    }

    private fun findAction(
        result: JSONObject,
        actionId: String,
    ): JSONObject? {
        val actions =
            result.getJSONArray(
                "actions",
            )

        for (
            i in
            0 until actions.length()
        ) {
            val action =
                actions.getJSONObject(
                    i,
                )

            if (
                action.optString(
                    "id",
                ) ==
                actionId
            ) {
                return action
            }
        }

        return null
    }

    private fun anchorLabel(
        anchor: ParsedAnchor,
    ): String {
        val text =
            cleanText(
                anchor.text,
            )

        if (
            text.isNotBlank()
        ) {
            return text
        }

        return anchor.images
            .firstOrNull()
            ?.src
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let {
                imageLabel(
                    it,
                )
            }
            .orEmpty()
    }

    private fun imageLabel(
        src: String,
    ): String =
        src
            .replace(
                '\\',
                '/',
            )
            .substringAfterLast(
                '/',
            )
            .substringBeforeLast(
                '.',
            )

    private fun handlerName(
        script: String,
    ): String =
        handlerRegex
            .find(
                script,
            )
            ?.groupValues
            ?.getOrNull(
                1,
            )
            .orEmpty()

    private fun surfaceFromScript(
        script: String,
    ): String =
        parentTargetRegex
            .find(
                script,
            )
            ?.groupValues
            ?.getOrNull(
                1,
            )
            .orEmpty()

    private fun isNonDocumentOption(
        value: String,
        label: String,
    ): Boolean {
        val normalized =
            value.lowercase(
                Locale.ROOT,
            )

        return value.isBlank() ||
            "erreur.htm" in
            normalized ||
            "blank.htm" in
            normalized
    }

    private fun nonDocumentKind(
        label: String,
    ): String {
        val stripped =
            label.trim()

        if (
            stripped.isNotBlank() &&
            stripped.all {
                it ==
                    '-'
            }
        ) {
            return "separator"
        }

        val upper =
            stripped.uppercase(
                Locale.ROOT,
            )

        if (
            listOf(
                "ВЫБЕРИТЕ",
                "SELECT",
                "CHOISISSEZ",
            ).any {
                upper.startsWith(
                    it,
                )
            }
        ) {
            return "prompt"
        }

        return "group"
    }

    private fun isBlankPath(
        relative: String,
    ): Boolean =
        relative
            .lowercase(
                Locale.ROOT,
            )
            .endsWith(
                "/blank.htm",
            )

    private fun frameLayout(
        roots: List<FrameNode>,
    ): JSONObject? {
        if (
            roots.size !=
            1 ||
            roots.single().type !=
            "frameset"
        ) {
            return null
        }

        val root =
            roots.single()
        val result =
            JSONObject()

        if (
            root.rows.isNotBlank()
        ) {
            result.put(
                "rows",
                root.rows,
            )
        }

        if (
            root.cols.isNotBlank()
        ) {
            result.put(
                "cols",
                root.cols,
            )
        }

        return result.takeIf {
            it.length() >
                0
        }
    }

    private fun relative(
        path: File,
    ): String =
        path
            .canonicalFile
            .relativeTo(
                root,
            )
            .invariantSeparatorsPath

    private fun isValidFile(
        path: File,
    ): Boolean =
        isWithin(
            path,
            root,
        ) &&
            path.isFile

    private fun isWithin(
        path: File,
        root: File,
    ): Boolean =
        path
            .canonicalFile
            .toPath()
            .startsWith(
                root
                    .canonicalFile
                    .toPath(),
            )

    private fun stableId(
        value: String,
    ): String =
        MessageDigest
            .getInstance(
                "SHA-1",
            )
            .digest(
                value.toByteArray(
                    Charsets.UTF_8,
                )
            )
            .take(
                6,
            )
            .joinToString(
                "",
            ) {
                byte ->
                "%02x".format(
                    byte.toInt() and
                        0xff,
                )
            }

    private fun slug(
        value: String,
    ): String =
        value
            .trim()
            .replace(
                Regex(
                    "[^A-Za-z0-9_-]+",
                ),
                "-",
            )
            .trim(
                '-',
            )
            .ifBlank {
                "control"
            }

    private fun decodeHtml(
        raw: ByteArray,
    ): String {
        val head =
            raw
                .copyOfRange(
                    0,
                    minOf(
                        8192,
                        raw.size,
                    ),
                )
                .toString(
                    Charsets.ISO_8859_1,
                )

        val encodings =
            buildList {
                charsetRegex
                    .find(
                        head,
                    )
                    ?.groupValues
                    ?.getOrNull(
                        1,
                    )
                    ?.trim()
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?.let {
                        add(
                            it,
                        )
                    }

                add(
                    "UTF-8",
                )
                add(
                    "windows-1251",
                )
                add(
                    "windows-1252",
                )
                add(
                    "ISO-8859-1",
                )
            }

        val seen =
            mutableSetOf<String>()

        encodings.forEach {
            name ->
            val key =
                name.lowercase(
                    Locale.ROOT,
                )

            if (
                !seen.add(
                    key,
                )
            ) {
                return@forEach
            }

            val decoded =
                runCatching {
                    val charset =
                        java.nio.charset.Charset
                            .forName(
                                name,
                            )
                    val decoder =
                        charset
                            .newDecoder()
                            .onMalformedInput(
                                CodingErrorAction.REPORT,
                            )
                            .onUnmappableCharacter(
                                CodingErrorAction.REPORT,
                            )

                    decoder
                        .decode(
                            ByteBuffer.wrap(
                                raw,
                            )
                        )
                        .toString()
                }.getOrNull()

            if (
                decoded !=
                null
            ) {
                return decoded
            }
        }

        return raw.toString(
            Charsets.UTF_8,
        )
    }

    private fun percentDecode(
        value: String,
    ): String {
        if (
            '%' !in
            value
        ) {
            return value
        }

        val output =
            ByteArrayOutputStream()
        var index =
            0

        while (
            index <
            value.length
        ) {
            val char =
                value[
                    index
                ]

            if (
                char ==
                '%' &&
                index +
                    2 <
                value.length &&
                value[
                    index +
                        1
                ].isHexDigit() &&
                value[
                    index +
                        2
                ].isHexDigit()
            ) {
                output.write(
                    value
                        .substring(
                            index +
                                1,
                            index +
                                3,
                        )
                        .toInt(
                            16,
                        )
                )
                index +=
                    3
                continue
            }

            output.write(
                char
                    .toString()
                    .toByteArray(
                        StandardCharsets.UTF_8,
                    )
            )
            index +=
                1
        }

        return output
            .toByteArray()
            .toString(
                StandardCharsets.UTF_8,
            )
    }

    private fun cleanText(
        value: String,
    ): String =
        Parser
            .unescapeEntities(
                value,
                false,
            )
            .replace(
                '\u00a0',
                ' ',
            )
            .trim()
            .replace(
                Regex(
                    """\s+"""
                ),
                " ",
            )

    private fun Char.isHexDigit(): Boolean =
        this in
            '0'..'9' ||
            this in
            'a'..'f' ||
            this in
            'A'..'F'

    companion object {
        private const val PAGE_CACHE_LIMIT =
            64

        private val textExtensions =
            setOf(
                "htm",
                "html",
            )

        private val pdfExtensions =
            setOf(
                "pdf",
            )

        private val charsetRegex =
            Regex(
                """charset\s*=\s*["']?\s*([A-Za-z0-9._-]+)""",
                RegexOption.IGNORE_CASE,
            )

        private val parentLocationRegex =
            Regex(
                """parent\.(nav|doc|menu|org)\.location(?:\.href)?\s*=\s*["']([^"']+)["']""",
                RegexOption.IGNORE_CASE,
            )

        private val parentTargetRegex =
            Regex(
                """parent\.(nav|doc|menu|org)""",
                RegexOption.IGNORE_CASE,
            )

        private val handlerRegex =
            Regex(
                """([A-Za-z_$][\w$]*)\s*\("""
            )
    }
}
