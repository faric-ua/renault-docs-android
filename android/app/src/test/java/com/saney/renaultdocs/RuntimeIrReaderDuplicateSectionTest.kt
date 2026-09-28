package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Test

class RuntimeIrReaderDuplicateSectionTest {
    @Test
    fun resolvesDuplicateDisplayCodeByLegacyEntrypoint() {
        val index =
            """
            {
              "schema_version": 2,
              "compiler_phase": "section-ir-v2",
              "volumes": [
                {
                  "classic_entrypoint": "V/INDEX.HTM",
                  "sections": {
                    "101": "_renault/runtime-ir/sections/v1/101.json"
                  },
                  "section_entries": [
                    {
                      "code": "101",
                      "entrypoint": "V/A/101.HTM",
                      "path": "_renault/runtime-ir/sections/v1/101.json"
                    },
                    {
                      "code": "101",
                      "entrypoint": "V/B/101.HTM",
                      "path": "_renault/runtime-ir/sections/v1/101-2.json"
                    }
                  ]
                }
              ]
            }
            """.trimIndent()

        val lookup =
            RuntimeIrReader.findSectionShard(
                indexText = index,
                volumeEntrypoint = "V/INDEX.HTM",
                sectionCode = "101",
                sectionEntrypoint = "V/B/101.HTM",
            )

        assertEquals(
            "_renault/runtime-ir/sections/v1/101-2.json",
            lookup.path,
        )
    }

    @Test
    fun oldIndexStillFallsBackToCodeMap() {
        val index =
            """
            {
              "schema_version": 2,
              "compiler_phase": "section-ir-v2",
              "volumes": [
                {
                  "classic_entrypoint": "V/INDEX.HTM",
                  "sections": {
                    "R325": "_renault/runtime-ir/sections/v1/R325.json"
                  }
                }
              ]
            }
            """.trimIndent()

        val lookup =
            RuntimeIrReader.findSectionShard(
                indexText = index,
                volumeEntrypoint = "V/INDEX.HTM",
                sectionCode = "R325",
                sectionEntrypoint = "V/R325.HTM",
            )

        assertEquals(
            "_renault/runtime-ir/sections/v1/R325.json",
            lookup.path,
        )
    }
}
