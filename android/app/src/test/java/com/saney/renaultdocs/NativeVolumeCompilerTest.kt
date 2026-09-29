package com.saney.renaultdocs

import java.nio.file.Files
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeVolumeCompilerTest {
    @Test
    fun discoversNtVolumeWithPythonParityFields() {
        val root =
            Files.createTempDirectory(
                "native-volume-compiler-",
            ).toFile()

        try {
            val volumeRoot =
                root.resolve(
                    "Megane II NT8340A 2006_04_18",
                )

            assertTrue(
                volumeRoot.mkdirs(),
            )

            volumeRoot
                .resolve(
                    "INDEX.HTM",
                )
                .writeText(
                    "<html></html>",
                )

            val volumes =
                NativeVolumeCompiler
                    .discoverVolumes(
                        root,
                    )

            assertEquals(
                1,
                volumes.size,
            )

            val volume =
                volumes.single()

            assertEquals(
                "NT8340A",
                volume.getString(
                    "document_code",
                ),
            )
            assertEquals(
                "2006-04-18",
                volume.getString(
                    "date",
                ),
            )
            assertEquals(
                "Megane II NT8340A 2006_04_18/INDEX.HTM",
                volume.getString(
                    "entrypoint",
                ),
            )
            assertEquals(
                "NT8340A · 2006-04-18",
                volume.getString(
                    "title",
                ),
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun modernIndexKeepsVolumeAndDatasetIdentity() {
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
                    "Megane II NT8340A 2006_04_18",
                )
                .put(
                    "entrypoint",
                    "Megane II NT8340A 2006_04_18/INDEX.HTM",
                )

        val index =
            NativeVolumeCompiler
                .buildModernIndex(
                    dataset =
                        JSONObject()
                            .put(
                                "id",
                                "megane-ii",
                            )
                            .put(
                                "title",
                                "Megane II",
                            )
                            .put(
                                "model",
                                "Megane II",
                            ),
                    volumes =
                        listOf(
                            volume,
                        ),
                )

        assertEquals(
            1,
            index.getInt(
                "schema_version",
            ),
        )
        assertEquals(
            "Renault",
            index.getJSONObject(
                "dataset",
            )
                .getString(
                    "manufacturer",
                ),
        )
        assertEquals(
            "volumes",
            index.getJSONObject(
                "navigation",
            )
                .getString(
                    "level",
                ),
        )
        assertEquals(
            "NT8340A",
            index.getJSONObject(
                "navigation",
            )
                .getJSONArray(
                    "volumes",
                )
                .getJSONObject(
                    0,
                )
                .getString(
                    "document_code",
                ),
        )
    }
}
