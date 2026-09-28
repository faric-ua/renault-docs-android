package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModernSectionsReaderTest {
    @Test
    fun parsesRequestedVolumeAndPreservesSourceOrder() {
        val text =
            """
            {
              "schema_version": 1,
              "source": "legacy-html-navigation",
              "volumes": [
                {
                  "id": "nt8236a",
                  "title": "NT8236A · 2002-11-18",
                  "document_code": "NT8236A",
                  "date": "2002-11-18",
                  "entrypoint": "Laguna X74 NT8236A 2002_11_18/INDEX.HTM",
                  "source_file": "Laguna X74 NT8236A 2002_11_18/MENU.HTM",
                  "sections": [
                    {
                      "code": "107",
                      "title": "АККУМУЛЯТОР",
                      "entrypoint": "Laguna X74 NT8236A 2002_11_18/107.HTM"
                    },
                    {
                      "code": "101",
                      "title": "ПРИКУРИВАТЕЛЬ",
                      "entrypoint": "Laguna X74 NT8236A 2002_11_18/101.HTM"
                    }
                  ]
                }
              ]
            }
            """.trimIndent()

        val result =
            ModernSectionsReader.parse(
                text = text,
                volumeEntrypoint =
                    "Laguna X74 NT8236A 2002_11_18/INDEX.HTM",
            )

        assertEquals(
            "NT8236A",
            result.documentCode,
        )
        assertEquals(
            listOf(
                "107",
                "101",
            ),
            result.sections.map {
                it.code
            },
        )
        assertEquals(
            "АККУМУЛЯТОР",
            result.sections.first().title,
        )
        assertTrue(
            result.sourceFile
                ?.endsWith(
                    "/MENU.HTM",
                )
                == true,
        )
    }

    @Test(
        expected =
            IllegalStateException::class,
    )
    fun rejectsMissingVolume() {
        ModernSectionsReader.parse(
            text =
                """
                {
                  "schema_version": 1,
                  "volumes": []
                }
                """.trimIndent(),
            volumeEntrypoint =
                "missing/INDEX.HTM",
        )
    }
}
