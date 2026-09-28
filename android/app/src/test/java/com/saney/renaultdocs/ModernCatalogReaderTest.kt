package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Test

class ModernCatalogReaderTest {
    @Test
    fun parsesModernIndexVolumes() {
        val catalog =
            ModernCatalogReader.parseModernIndex(
                """
                {
                  "schema_version": 1,
                  "dataset": {
                    "id": "laguna",
                    "title": "Renault Laguna II 2001–2006",
                    "manufacturer": "Renault",
                    "model": "Laguna II",
                    "platform": "X74",
                    "years": {"from": 2001, "to": 2006}
                  },
                  "navigation": {
                    "level": "volumes",
                    "volumes": [
                      {
                        "id": "nt8236a",
                        "title": "NT8236A · 2002-11-18",
                        "document_code": "NT8236A",
                        "date": "2002-11-18",
                        "kind": "technical-documentation",
                        "source_folder": "Laguna X74 NT8236A 2002_11_18",
                        "entrypoint": "Laguna X74 NT8236A 2002_11_18/INDEX.HTM"
                      }
                    ]
                  }
                }
                """.trimIndent()
            )

        assertEquals(
            "modern-index",
            catalog.source,
        )
        assertEquals(
            "2001–2006",
            catalog.yearsLabel,
        )
        assertEquals(
            1,
            catalog.volumes.size,
        )
        assertEquals(
            "NT8236A",
            catalog.volumes.single().documentCode,
        )
    }

    @Test
    fun sortsVolumesByReleaseDate() {
        val catalog =
            ModernCatalogReader.parseModernIndex(
                """
                {
                  "schema_version": 1,
                  "dataset": {
                    "id": "laguna",
                    "title": "Renault Laguna II"
                  },
                  "navigation": {
                    "level": "volumes",
                    "volumes": [
                      {
                        "title": "NT8283A · 2005-08-29",
                        "document_code": "NT8283A",
                        "date": "2005-08-29",
                        "entrypoint": "A/INDEX.HTM"
                      },
                      {
                        "title": "NT8183A · 2001-01-22",
                        "document_code": "NT8183A",
                        "date": "2001-01-22",
                        "entrypoint": "B/INDEX.HTM"
                      },
                      {
                        "title": "NT8236A · 2002-11-18",
                        "document_code": "NT8236A",
                        "date": "2002-11-18",
                        "entrypoint": "C/INDEX.HTM"
                      }
                    ]
                  }
                }
                """.trimIndent()
            )

        assertEquals(
            listOf(
                "NT8183A",
                "NT8236A",
                "NT8283A",
            ),
            catalog.volumes.map {
                it.documentCode
            },
        )
    }

    @Test
    fun fallsBackToManifestVolumes() {
        val catalog =
            ModernCatalogReader.parseManifestFallback(
                """
                {
                  "schema_version": 1,
                  "id": "laguna",
                  "title": "Renault Laguna II",
                  "manufacturer": "Renault",
                  "model": "Laguna II",
                  "platform": "X74",
                  "volumes": [
                    {
                      "title": "NT8283A",
                      "entrypoint": "A/INDEX.HTM"
                    },
                    {
                      "title": "NT8307A",
                      "entrypoint": "B/INDEX.HTM"
                    }
                  ]
                }
                """.trimIndent()
            )

        assertEquals(
            "manifest-fallback",
            catalog.source,
        )
        assertEquals(
            2,
            catalog.volumes.size,
        )
    }
}
