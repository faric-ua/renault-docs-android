package com.saney.renaultdocs

import java.nio.file.Files
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeRuntimeIrCompilerTest {
    @Test
    fun writesShardedRuntimeIndexReadableByExistingAndroidReader() {
        val root =
            Files.createTempDirectory(
                "native-runtime-ir-",
            ).toFile()

        try {
            val volumeRoot =
                root.resolve(
                    "Megane II NT8340A 2006_04_18",
                )
            val menuRoot =
                volumeRoot.resolve(
                    "RUS/HTM/MENU",
                )
            val pdfRoot =
                volumeRoot.resolve(
                    "RUS/DOCUMENT",
                )

            assertTrue(
                menuRoot.mkdirs(),
            )
            assertTrue(
                pdfRoot.mkdirs(),
            )

            volumeRoot
                .resolve(
                    "INDEX.HTM",
                )
                .writeText(
                    """
                    <html><body>
                      <a href="RUS/HTM/MENU/101.HTM">101 - ПРИКУРИВАТЕЛЬ</a>
                      <a href="RUS/HTM/MENU/R15.HTM">R15 - РЕЛЕ</a>
                    </body></html>
                    """.trimIndent(),
                )

            menuRoot
                .resolve(
                    "101.HTM",
                )
                .writeText(
                    """
                    <html><body>
                      <a target="doc" href="../../../RUS/DOCUMENT/AIDE.PDF">
                        <img src="../../../COMMUN/IMAGES/SITE/BOUTONS/AIDE.GIF">
                      </a>
                    </body></html>
                    """.trimIndent(),
                )

            menuRoot
                .resolve(
                    "R15.HTM",
                )
                .writeText(
                    "<html><title>R15</title></html>",
                )

            pdfRoot
                .resolve(
                    "AIDE.PDF",
                )
                .writeBytes(
                    "%PDF-test".toByteArray(),
                )

            val image =
                volumeRoot.resolve(
                    "COMMUN/IMAGES/SITE/BOUTONS/AIDE.GIF",
                )
            assertTrue(
                image.parentFile.mkdirs(),
            )
            image.writeBytes(
                byteArrayOf(
                    1,
                    2,
                    3,
                )
            )

            val volume =
                JSONObject()
                    .put(
                        "id",
                        "megane-ii-nt8340a-2006-04-18",
                    )
                    .put(
                        "title",
                        "NT8340A · 2006-04-18",
                    )
                    .put(
                        "document_code",
                        "NT8340A",
                    )
                    .put(
                        "date",
                        "2006-04-18",
                    )
                    .put(
                        "kind",
                        "technical-documentation",
                    )
                    .put(
                        "source_folder",
                        volumeRoot.name,
                    )
                    .put(
                        "entrypoint",
                        volumeRoot.name +
                            "/INDEX.HTM",
                    )

            val result =
                NativeRuntimeIrCompiler.compile(
                    outputRoot =
                        root,
                    volumes =
                        listOf(
                            volume,
                        ),
                )

            assertEquals(
                2,
                result.sectionCount,
            )
            assertTrue(
                result.runtimeTreePath.isFile,
            )

            val runtimeTree =
                JSONObject(
                    result.runtimeTreePath
                        .readText(),
                )

            assertEquals(
                2,
                runtimeTree.getInt(
                    "section_count",
                ),
            )

            assertTrue(
                result.runtimeIndexPath.isFile,
            )
            assertTrue(
                result.coveragePath.isFile,
            )

            val indexText =
                result.runtimeIndexPath
                    .readText()

            val lookup =
                RuntimeIrReader.findSectionShard(
                    indexText =
                        indexText,
                    volumeEntrypoint =
                        volume.getString(
                            "entrypoint",
                        ),
                    sectionCode =
                        "101",
                    sectionEntrypoint =
                        volumeRoot.name +
                            "/RUS/HTM/MENU/101.HTM",
                )

            assertTrue(
                lookup.path.isNotBlank(),
            )

            val payload =
                root.resolve(
                    lookup.path,
                )
                    .readText()

            val parsed =
                RuntimeIrReader.parseSectionShard(
                    sectionText =
                        payload,
                    expectedSchema =
                        lookup.schemaVersion,
                    expectedCompilerPhase =
                        lookup.compilerPhase,
                    expectedSectionCode =
                        "101",
                    volumeDocumentationPath =
                        lookup.documentationPath,
                )

            assertEquals(
                "NT8340A",
                parsed.documentCode,
            )
            assertEquals(
                "section-ir-v2",
                parsed.section.getString(
                    "compile_state",
                ),
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun hoistsIdenticalDocumentationGraphAtVolumeScope() {
        val root =
            Files.createTempDirectory(
                "native-runtime-docs-",
            ).toFile()

        try {
            val volumeRoot =
                root.resolve(
                    "NT8340A_2006-04-18",
                )
            val menuRoot =
                volumeRoot.resolve(
                    "RUS/HTM/MENU",
                )
            val documentRoot =
                volumeRoot.resolve(
                    "RUS/DOCUMENT",
                )
            val buttonRoot =
                volumeRoot.resolve(
                    "COMMUN/IMAGES/SITE/BOUTONS",
                )

            assertTrue(
                menuRoot.mkdirs(),
            )
            assertTrue(
                documentRoot.mkdirs(),
            )
            assertTrue(
                buttonRoot.mkdirs(),
            )

            volumeRoot
                .resolve(
                    "INDEX.HTM",
                )
                .writeText(
                    """
                    <html><body>
                      <a href="RUS/HTM/MENU/101.HTM">101 - A</a>
                      <a href="RUS/HTM/MENU/103.HTM">103 - B</a>
                      <a href="RUS/HTM/MENU/105.HTM">105 - C</a>
                      <a href="RUS/HTM/MENU/107.HTM">107 - D</a>
                    </body></html>
                    """.trimIndent(),
                )

            listOf(
                "AIDE",
                "GENE",
                "PLATFUSI",
            ).forEach {
                name ->
                documentRoot
                    .resolve(
                        "$name.PDF",
                    )
                    .writeBytes(
                        "%PDF-$name".toByteArray(),
                    )
                buttonRoot
                    .resolve(
                        "$name.GIF",
                    )
                    .writeBytes(
                        byteArrayOf(
                            1,
                        )
                    )
            }

            for (
                code in
                listOf(
                    "101",
                    "103",
                    "105",
                    "107",
                )
            ) {
                menuRoot
                    .resolve(
                        "$code.HTM",
                    )
                    .writeText(
                        """
                        <html><body>
                          <a target="doc" href="../../DOCUMENT/GENE.PDF">
                            <img src="../../../COMMUN/IMAGES/SITE/BOUTONS/GENE.GIF">
                          </a>
                          <a target="doc" href="../../DOCUMENT/PLATFUSI.PDF">
                            <img src="../../../COMMUN/IMAGES/SITE/BOUTONS/PLATFUSI.GIF">
                          </a>
                          <a target="doc" href="../../DOCUMENT/AIDE.PDF">
                            <img src="../../../COMMUN/IMAGES/SITE/BOUTONS/AIDE.GIF">
                          </a>
                        </body></html>
                        """.trimIndent(),
                    )
            }

            val volume =
                JSONObject()
                    .put(
                        "id",
                        "nt8340a",
                    )
                    .put(
                        "title",
                        "NT8340A · 2006-04-18",
                    )
                    .put(
                        "document_code",
                        "NT8340A",
                    )
                    .put(
                        "date",
                        "2006-04-18",
                    )
                    .put(
                        "source_folder",
                        volumeRoot.name,
                    )
                    .put(
                        "entrypoint",
                        volumeRoot.name +
                            "/INDEX.HTM",
                    )

            val result =
                NativeRuntimeIrCompiler.compile(
                    outputRoot =
                        root,
                    volumes =
                        listOf(
                            volume,
                        ),
                )

            val index =
                JSONObject(
                    result.runtimeIndexPath
                        .readText(),
                )

            val volumeIndex =
                index
                    .getJSONArray(
                        "volumes",
                    )
                    .getJSONObject(
                        0,
                    )

            val documentationPath =
                volumeIndex.getString(
                    "documentation_path",
                )

            val documentationPayload =
                JSONObject(
                    root.resolve(
                        documentationPath,
                    ).readText(),
                )

            val documentation =
                documentationPayload
                    .getJSONObject(
                        "documentation",
                    )

            assertEquals(
                "volume",
                documentation.getString(
                    "scope",
                ),
            )
            assertEquals(
                4,
                documentation.getInt(
                    "verified_section_count",
                ),
            )

            val actions =
                documentation.getJSONArray(
                    "actions",
                )

            assertTrue(
                (0 until actions.length())
                    .all {
                        actions
                            .getJSONObject(
                                it,
                            )
                            .getString(
                                "id",
                            )
                            .startsWith(
                                "vdoc-action-",
                            )
                    }
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun coverageRecordsUnsupportedLegacyJavascript() {
        val runtimeTree =
            JSONObject(
                """
                {
                  "schema_version": 2,
                  "compiler_phase": "section-ir-v2",
                  "section_count": 1,
                  "volumes": [{
                    "id": "v1",
                    "document_code": "NT1",
                    "modern": {
                      "sections": [{
                        "code": "101",
                        "compile_state": "section-ir-v2",
                        "panels": [{"kind":"menu"}],
                        "controls": [{"type":"action-bar","items":[{"label":"SCH"}]}],
                        "actions": [{"type":"legacy-javascript","label":"x","script":"foo()"}],
                        "documents": [{"type":"pdf"}]
                      }]
                    }
                  }]
                }
                """.trimIndent(),
            )

        val coverage =
            NativeRuntimeIrCompiler
                .buildCoverage(
                    runtimeTree,
                )

        assertEquals(
            1,
            coverage.getInt(
                "unsupported_action_count",
            ),
        )
        assertEquals(
            1,
            coverage
                .getJSONObject(
                    "compile_states",
                )
                .getInt(
                    "section-ir-v2",
                ),
        )
        assertEquals(
            1,
            coverage
                .getJSONObject(
                    "menu_labels",
                )
                .getInt(
                    "SCH",
                ),
        )
    }
}
