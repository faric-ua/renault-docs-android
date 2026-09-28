package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DatasetManifestParserTest {
    @Test
    fun catalogEntrypointIsPreferredAndVolumesAreCounted() {
        val metadata = DatasetManifestParser.parse(
            """
            {
              "schema_version": 1,
              "id": "renault-laguna-ii-x74-2001-2006",
              "title": "Renault Laguna II 2001–2006",
              "manufacturer": "Renault",
              "model": "Laguna II",
              "platform": "X74",
              "years": {"from": 2001, "to": 2006},
              "entrypoint": "INDEX.HTM",
              "catalog_entrypoint": "_renault/START.html",
              "volumes": [{}, {}, {}]
            }
            """.trimIndent()
        )

        assertEquals("_renault/START.html", metadata.openEntrypoint)
        assertEquals("2001–2006", metadata.yearsLabel)
        assertEquals("X74", metadata.platform)
        assertEquals(3, metadata.volumeCount)
    }

    @Test
    fun legacyEntrypointIsUsedWhenCatalogIsAbsent() {
        val metadata = DatasetManifestParser.parse(
            """
            {
              "schema_version": 1,
              "id": "dataset",
              "title": "Dataset",
              "manufacturer": "Renault",
              "model": "Test",
              "entrypoint": "INDEX.HTM"
            }
            """.trimIndent()
        )

        assertEquals("INDEX.HTM", metadata.openEntrypoint)
    }

    @Test
    fun missingRequiredFieldIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            DatasetManifestParser.parse(
                """
                {
                  "schema_version": 1,
                  "id": "dataset",
                  "title": "Dataset",
                  "manufacturer": "Renault",
                  "entrypoint": "INDEX.HTM"
                }
                """.trimIndent()
            )
        }
    }
}
