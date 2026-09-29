package com.saney.renaultdocs

import java.nio.file.Files
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeSectionIrCompilerTest {
    @Test
    fun compilesMenuSelectRoutesDocumentsAndInlineSideEffects() {
        val root =
            Files.createTempDirectory(
                "native-section-ir-",
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
            val navRoot =
                volumeRoot.resolve(
                    "RUS/HTM/NM",
                )
            val pdfRoot =
                volumeRoot.resolve(
                    "COMMUN/PDF",
                )

            assertTrue(
                menuRoot.mkdirs(),
            )
            assertTrue(
                navRoot.mkdirs(),
            )
            assertTrue(
                pdfRoot.mkdirs(),
            )

            val menu =
                menuRoot.resolve(
                    "101.HTM",
                )

            menu.writeText(
                """
                <html>
                  <head><title>101 menu</title></head>
                  <body onload="parent.doc.location='../../../COMMUN/PDF/INIT.PDF'">
                    <a href="../NM/101.HTM" target="nav">NM</a>
                    <a href="../../../COMMUN/PDF/S7.PDF#page=2" target="doc"
                       onclick="parent.nav.location='../NM/BLANK.HTM'">SCH</a>
                    <select name="variant"
                            onchange="parent.doc.location=this.options[this.selectedIndex].value">
                      <option value="">ВЫБЕРИТЕ</option>
                      <option value="../../../COMMUN/PDF/A.PDF">A</option>
                    </select>
                    <a href="javascript:imprimer()">Print</a>
                  </body>
                </html>
                """.trimIndent(),
            )

            navRoot
                .resolve(
                    "101.HTM",
                )
                .writeText(
                    """
                    <html>
                      <head><title>Nomenclature</title></head>
                      <body>
                        <a href="../../../COMMUN/PDF/NM.PDF" target="doc">Open NM</a>
                      </body>
                    </html>
                    """.trimIndent(),
                )

            navRoot
                .resolve(
                    "BLANK.HTM",
                )
                .writeText(
                    "<html></html>",
                )

            listOf(
                "INIT.PDF",
                "S7.PDF",
                "A.PDF",
                "NM.PDF",
            ).forEach {
                name ->
                pdfRoot
                    .resolve(
                        name,
                    )
                    .writeBytes(
                        "%PDF-test".toByteArray(),
                    )
            }

            val compiler =
                NativeSectionIrCompiler(
                    outputRoot =
                        root,
                    volume =
                        JSONObject()
                            .put(
                                "source_folder",
                                volumeRoot.name,
                            ),
                )

            val result =
                compiler.compileSection(
                    JSONObject()
                        .put(
                            "code",
                            "101",
                        )
                        .put(
                            "entrypoint",
                            volumeRoot.name +
                                "/RUS/HTM/MENU/101.HTM",
                        ),
                )

            assertEquals(
                "section-ir-v2",
                result.getString(
                    "compile_state",
                ),
            )

            val panels =
                result.getJSONArray(
                    "panels",
                )
            assertTrue(
                panels.length() >=
                    2
            )

            val controls =
                result.getJSONArray(
                    "controls",
                )
            assertTrue(
                (0 until controls.length())
                    .map {
                        controls
                            .getJSONObject(
                                it,
                            )
                            .getString(
                                "type",
                            )
                    }
                    .contains(
                        "action-bar",
                    )
            )
            assertTrue(
                (0 until controls.length())
                    .map {
                        controls
                            .getJSONObject(
                                it,
                            )
                            .getString(
                                "type",
                            )
                    }
                    .contains(
                        "select",
                    )
            )

            val actions =
                result.getJSONArray(
                    "actions",
                )
            val actionObjects =
                (0 until actions.length())
                    .map {
                        actions.getJSONObject(
                            it,
                        )
                    }

            assertTrue(
                actionObjects.any {
                    it.optString(
                        "route_type",
                    ) ==
                        "open-panel"
                }
            )
            assertTrue(
                actionObjects.any {
                    it.optString(
                        "route_type",
                    ) ==
                        "open-document"
                }
            )
            assertTrue(
                actionObjects.any {
                    it.optString(
                        "type",
                    ) ==
                        "set-surface-location" &&
                        it.optString(
                            "semantic",
                        ) ==
                        "clear-surface"
                }
            )
            assertTrue(
                actionObjects.any {
                    it.optString(
                        "type",
                    ) ==
                        "print"
                }
            )

            val documents =
                result.getJSONArray(
                    "documents",
                )
            assertTrue(
                documents.length() >=
                    3
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun compilesCompositeDocumentAndStructuredPinTable() {
        val root =
            Files.createTempDirectory(
                "native-section-ir-doc-",
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
            val docRoot =
                volumeRoot.resolve(
                    "RUS/HTM/NM",
                )
            val pdfRoot =
                volumeRoot.resolve(
                    "COMMUN/PDF",
                )

            assertTrue(
                menuRoot.mkdirs(),
            )
            assertTrue(
                docRoot.mkdirs(),
            )
            assertTrue(
                pdfRoot.mkdirs(),
            )

            menuRoot
                .resolve(
                    "120.HTM",
                )
                .writeText(
                    """
                    <html><body>
                      <a href="../NM/120_FRAME.HTM" target="doc">NM</a>
                    </body></html>
                    """.trimIndent(),
                )

            docRoot
                .resolve(
                    "120_FRAME.HTM",
                )
                .writeText(
                    """
                    <html>
                      <frameset rows="45%,55%">
                        <frame name="dessin" src="../../../COMMUN/PDF/120_18.PDF">
                        <frame name="alveoles" src="120_18.HTM">
                      </frameset>
                    </html>
                    """.trimIndent(),
                )

            pdfRoot
                .resolve(
                    "120_18.PDF",
                )
                .writeBytes(
                    "%PDF-test".toByteArray(),
                )

            docRoot
                .resolve(
                    "120_18.HTM",
                )
                .writeText(
                    """
                    <html>
                      <head><title>Contacts</title></head>
                      <body>
                        <h2>120 — ECU</h2>
                        <table>
                          <tr><td>1</td><td>0.6</td><td>3N</td><td>Signal A</td></tr>
                          <tr><td>2</td><td>1.4</td><td>3CV</td><td>Signal B</td></tr>
                        </table>
                      </body>
                    </html>
                    """.trimIndent(),
                )

            val result =
                NativeSectionIrCompiler(
                    outputRoot =
                        root,
                    volume =
                        JSONObject()
                            .put(
                                "source_folder",
                                volumeRoot.name,
                            ),
                )
                    .compileSection(
                        JSONObject()
                            .put(
                                "entrypoint",
                                volumeRoot.name +
                                    "/RUS/HTM/MENU/120.HTM",
                            ),
                    )

            val documents =
                result.getJSONArray(
                    "documents",
                )
            val documentObjects =
                (0 until documents.length())
                    .map {
                        documents.getJSONObject(
                            it,
                        )
                    }

            val composite =
                documentObjects.first {
                    it.getString(
                        "type",
                    ) ==
                        "composite-document"
                }

            assertEquals(
                "45%,55%",
                composite
                    .getJSONObject(
                        "layout",
                    )
                    .getString(
                        "rows",
                    ),
            )
            assertEquals(
                2,
                composite
                    .getJSONArray(
                        "parts",
                    )
                    .length(),
            )

            val structured =
                documentObjects.first {
                    it.getString(
                        "type",
                    ) ==
                        "structured-html"
                }

            val rows =
                structured
                    .getJSONArray(
                        "tables",
                    )
                    .getJSONObject(
                        0,
                    )
                    .getJSONArray(
                        "rows",
                    )

            val header =
                rows.getJSONArray(
                    0,
                )

            assertEquals(
                "№",
                header
                    .getJSONObject(
                        0,
                    )
                    .getString(
                        "text",
                    ),
            )
            assertEquals(
                "мм²",
                header
                    .getJSONObject(
                        1,
                    )
                    .getString(
                        "text",
                    ),
            )
            assertTrue(
                header
                    .getJSONObject(
                        0,
                    )
                    .getBoolean(
                        "header",
                    )
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun missingEntrypointDoesNotPretendToBeCompiled() {
        val root =
            Files.createTempDirectory(
                "native-section-ir-missing-",
            ).toFile()

        try {
            val compiler =
                NativeSectionIrCompiler(
                    outputRoot =
                        root,
                    volume =
                        JSONObject()
                            .put(
                                "source_folder",
                                "missing",
                            ),
                )

            val result =
                compiler.compileSection(
                    JSONObject()
                        .put(
                            "entrypoint",
                            "missing/101.HTM",
                        ),
                )

            assertEquals(
                "navigation-indexed",
                result.getString(
                    "compile_state",
                ),
            )
            assertTrue(
                result
                    .getJSONArray(
                        "warnings",
                    )
                    .length() >
                    0
            )
        } finally {
            root.deleteRecursively()
        }
    }
}
