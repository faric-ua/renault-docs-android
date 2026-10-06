package com.saney.renaultdocs

import org.junit.Assert.assertEquals
import org.junit.Test

class DriveCatalogParserTest {
    @Test
    fun keepsVehicleCodesOnTheirVolume() {
        val catalog =
            DriveCatalogParser.parse(
                """
                {
                  "schema_version": 1,
                  "catalog_version": 4,
                  "projects": [
                    {
                      "id": "megane-ii",
                      "title": "Megane II",
                      "vehicle_codes": ["E84", "K84", "L84"],
                      "document_year_from": 2006,
                      "document_year_to": 2006,
                      "volumes": [
                        {
                          "id": "megane-ii-nt8340a-2006-04-18",
                          "document_code": "NT8340A",
                          "date": "2006-04-18",
                          "vehicle_codes": ["E84", "K84", "L84"],
                          "document_type": "Visu",
                          "document_version": "3.0",
                          "region": null,
                          "file_name": "Megane-II_E84-L84-K84_NT8340A_Visu-v3.0_2006-04-18.rdpkg",
                          "drive_file_id": "1Eajf4cVTUvLwHOx5dd5BNg09dEFktnNw",
                          "size_bytes": 73250913,
                          "sha256": "f8b6f227a87b318b7fa9c6ae1d723ee85eb3669a83722dff3065f2c3f899467b"
                        }
                      ]
                    }
                  ]
                }
                """.trimIndent(),
            )

        val project = catalog.projects.single()
        val volume = project.volumes.single()

        assertEquals(
            listOf("E84", "K84", "L84"),
            project.vehicleCodes,
        )
        assertEquals(
            listOf("E84", "K84", "L84"),
            volume.vehicleCodes,
        )
    }
}
