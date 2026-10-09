package com.saney.renaultdocs

import java.io.File
import kotlin.io.FileTreeWalk

/**
 * A source archive may contain a Renault INDEX.HTM at its root and separate
 * Renault volumes underneath it. A selected volume must not import the bytes
 * belonging to its independently selectable nested volumes.
 *
 * This logic operates ONLY on extracted app-private staging. It never moves,
 * rewrites or deletes the user's SAF archive or any installed volume.
 */
internal object ArchiveRawVolumeIsolation {
    fun excludedDescendantRoots(
        extractionRoot: File,
        selectedRawRoot: File,
        candidateRelativePaths: List<String>,
    ): Set<File> {
        val stagingRoot = extractionRoot.canonicalFile
        val selected = selectedRawRoot.canonicalFile
        val stagingPrefix = stagingRoot.path + File.separator

        require(
            selected.isDirectory &&
                (selected.path == stagingRoot.path ||
                    selected.path.startsWith(stagingPrefix)),
        ) {
            "Вибраний том не належить до archive staging."
        }

        val selectedPrefix = selected.path + File.separator
        return candidateRelativePaths.asSequence()
            .map { ArchiveIntake.resolveRawRoot(stagingRoot, it) }
            .filter { candidate ->
                candidate != selected && candidate.path.startsWith(selectedPrefix)
            }
            .toSet()
    }

    fun walkSelectedRoot(
        selectedRawRoot: File,
        excludedDescendants: Set<File>,
    ): FileTreeWalk {
        val selected = selectedRawRoot.canonicalFile
        val selectedPrefix = selected.path + File.separator
        val excluded = excludedDescendants.map { it.canonicalFile }.toSet()
        require(
            excluded.all { it.isDirectory && it.path.startsWith(selectedPrefix) },
        ) {
            "Некоректна межа виключення вкладеного тому."
        }

        return selected.walkTopDown().onEnter { directory ->
            directory.canonicalFile !in excluded
        }
    }
}
