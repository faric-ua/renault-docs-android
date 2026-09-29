package com.saney.renaultdocs

import java.nio.file.Files
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeSectionCompilerTest {
    @Test
    fun discoversOpaqueSectionIdsInLegacySourceOrder() {
        val root =
            Files.createTempDirectory(
                "native-sections-",
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

            assertTrue(
                menuRoot.mkdirs(),
            )

            volumeRoot
                .resolve(
                    "INDEX.HTM",
                )
                .writeText(
                    """
                    <html>
                      <frameset>
                        <frame name="org" src="CODE.HTM">
                      </frameset>
                    </html>
                    """.trimIndent(),
                )

            volumeRoot
                .resolve(
                    "CODE.HTM",
                )
                .writeText(
                    """
                    <html><body>
                      <a href="RUS/HTM/MENU/101.HTM">101 - Запальничка</a>
                      <a href="RUS/HTM/MENU/1405.HTM">1405 - Модуль</a>
                      <a href="RUS/HTM/MENU/R15.HTM">R15 - Реле</a>
                      <a href="RUS/HTM/MENU/MA.HTM">MA - Маса</a>
                    </body></html>
                    """.trimIndent(),
                )

            listOf(
                "101",
                "1405",
                "R15",
                "MA",
            ).forEach {
                code ->
                menuRoot
                    .resolve(
                        "$code.HTM",
                    )
                    .writeText(
                        "<html><body>$code</body></html>",
                    )
            }

            val volume =
                JSONObject()
                    .put(
                        "id",
                        "nt8340a-2006-04-18",
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
                NativeSectionCompiler
                    .discoverVolumeSections(
                        outputRoot =
                            root,
                        volume =
                            volume,
                    )

            val sections =
                result.getJSONArray(
                    "sections",
                )

            assertEquals(
                4,
                sections.length(),
            )
            assertEquals(
                listOf(
                    "101",
                    "1405",
                    "R15",
                    "MA",
                ),
                (0 until sections.length())
                    .map {
                        index ->
                        sections
                            .getJSONObject(
                                index,
                            )
                            .getString(
                                "code",
                            )
                    },
            )
            assertEquals(
                volumeRoot.name +
                    "/CODE.HTM",
                result.getString(
                    "source_file",
                ),
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rowTextAndOnclickTargetsRemainDiscoverable() {
        val root =
            Files.createTempDirectory(
                "native-sections-row-",
            ).toFile()

        try {
            val volumeRoot =
                root.resolve(
                    "NT9999A_2006-10-09",
                )
            val menuRoot =
                volumeRoot.resolve(
                    "RUS/HTM/MENU",
                )

            assertTrue(
                menuRoot.mkdirs(),
            )

            volumeRoot
                .resolve(
                    "INDEX.HTM",
                )
                .writeText(
                    """
                    <html><body>
                      <table>
                        <tr>
                          <td><a href="RUS/HTM/MENU/103.HTM"><img src="x.gif"></a></td>
                          <td>103 - Контакти</td>
                        </tr>
                      </table>
                      <a href="#" onclick="parent.menu.location='RUS/HTM/MENU/R262.HTM'">
                        R262 - Блок
                      </a>
                    </body></html>
                    """.trimIndent(),
                )

            menuRoot
                .resolve(
                    "103.HTM",
                )
                .writeText(
                    "<html></html>",
                )
            menuRoot
                .resolve(
                    "R262.HTM",
                )
                .writeText(
                    "<html></html>",
                )

            val volume =
                JSONObject()
                    .put(
                        "id",
                        "nt9999a",
                    )
                    .put(
                        "title",
                        "NT9999A",
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

            val sections =
                NativeSectionCompiler
                    .discoverVolumeSections(
                        outputRoot =
                            root,
                        volume =
                            volume,
                    )
                    .getJSONArray(
                        "sections",
                    )

            assertEquals(
                2,
                sections.length(),
            )
            assertEquals(
                "103",
                sections
                    .getJSONObject(
                        0,
                    )
                    .getString(
                        "code",
                    ),
            )
            assertEquals(
                "R262",
                sections
                    .getJSONObject(
                        1,
                    )
                    .getString(
                        "code",
                    ),
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun acceptsKnownOpaqueIdentifierFamilies() {
        listOf(
            "101",
            "1405",
            "R15",
            "R262",
            "MA",
            "MAH",
        ).forEach {
            value ->
            assertTrue(
                value,
                NativeSectionCompiler
                    .looksLikeSectionId(
                        value,
                    ),
            )
        }
    }
}
