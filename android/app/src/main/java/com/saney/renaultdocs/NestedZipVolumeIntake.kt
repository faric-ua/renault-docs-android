package com.saney.renaultdocs

import java.io.BufferedOutputStream
import java.io.File
import java.util.Locale
import org.apache.commons.compress.archivers.zip.ZipFile

/**
 * Exactly one level of ZIP-in-archive intake for a batch of Renault volumes.
 * Source archives remain read-only. Every output stays under private staging.
 *
 * Deliberately no arbitrary recursion: an inner ZIP must contain a normal
 * Renault raw root (INDEX.HTM / INDEX.HTML / ACCUEIL.HTM), not another ZIP.
 */
internal object NestedZipVolumeIntake {
    data class Result(
        val rawRoots: List<File>,
        val files: Int,
        val directories: Int,
        val expandedBytes: Long,
    )

    private const val MAX_INNER_ARCHIVES = 20
    private const val MAX_SOURCE_BYTES = 768L * 1024L * 1024L
    private const val MAX_TOTAL_EXPANDED_BYTES = 8L * 1024L * 1024L * 1024L
    private const val MAX_TOTAL_ENTRIES = 200_000
    private const val MIN_FREE_SPACE_BYTES = 128L * 1024L * 1024L
    private const val BUFFER_SIZE = 128 * 1024
    private const val MAX_DEPTH = 16
    private const val NESTED_DIR = "__renault_nested_zip_roots"

    /**
     * Only called when outer archive has NO direct raw roots, to avoid
     * interpreting arbitrary ZIP assets inside valid Renault tomes as sources.
     */
    fun expandOneLevel(
        extractionRoot: File,
        outerEntries: Int,
        outerExpandedBytes: Long,
        isCancelled: () -> Boolean = { false },
        onProgress: (ArchiveIntake.Progress) -> Unit = {},
    ): Result? {
        require(extractionRoot.isDirectory) { "Archive staging недоступний." }
        checkCancelled(isCancelled)

        val candidates = extractionRoot.walkTopDown()
            .maxDepth(MAX_DEPTH)
            .filter { it.isFile && it.extension.lowercase(Locale.ROOT) in setOf("zip", "7z", "rar") }
            .sortedBy { it.relativeTo(extractionRoot).path.lowercase(Locale.ROOT) }
            .toList()

        if (candidates.isEmpty()) return null
        require(candidates.size <= MAX_INNER_ARCHIVES) {
            "Занадто багато вкладених архівів: " + candidates.size +
                ". Максимум: " + MAX_INNER_ARCHIVES + "."
        }

        val unsupported = candidates.filter {
            !it.extension.equals("zip", ignoreCase = true)
        }
        require(unsupported.isEmpty()) {
            "Виявлено вкладений 7Z/RAR. Поки підтримується лише ZIP усередині архіву."
        }
        require(candidates.sumOf { it.length() } <= MAX_SOURCE_BYTES) {
            "Вкладені ZIP надто великі. Розпакуйте частину архівів окремо."
        }
        require(outerExpandedBytes in 0..MAX_TOTAL_EXPANDED_BYTES) {
            "Загальний розмір архівів перевищує безпечне обмеження."
        }

        val nestedRoot = File(extractionRoot, NESTED_DIR)
        // Never overwrite any name/contents originating from the user archive.
        require(!nestedRoot.exists()) {
            "Архів містить конфліктну службову папку: " + NESTED_DIR
        }
        require(nestedRoot.mkdir()) {
            "Не вдалося створити staging для вкладених ZIP."
        }

        var cumulativeBytes = outerExpandedBytes
        var entriesDone = outerEntries
        var files = 0
        var directories = 0
        val foundRoots = mutableListOf<File>()

        candidates.forEachIndexed { index, inner ->
            checkCancelled(isCancelled)
            require(ArchiveIntake.detectFormat(inner) == ArchiveIntake.Format.ZIP) {
                "Вкладений файл не є ZIP: " + inner.name
            }
            val safeName = inner.nameWithoutExtension
                .replace(Regex("[^\\p{L}\\p{N}._-]"), "_")
                .take(70)
                .trim('.')
                .ifBlank { "volume" }
            val destination = File(
                nestedRoot,
                (index + 1).toString().padStart(3, '0') + "-" + safeName,
            )
            require(destination.mkdir()) {
                "Не вдалося підготувати папку вкладеного тому."
            }

            ZipFile(inner).use { archive ->
                val entries = archive.entries().toList()
                require(entriesDone.toLong() + entries.size <= MAX_TOTAL_ENTRIES) {
                    "Забагато файлів у сукупності вкладених архівів."
                }
                val pathGuard = ArchiveIntake.EntryPathGuard()
                entries.forEachIndexed { entryIndex, entry ->
                    checkCancelled(isCancelled)
                    pathGuard.check(entry.name, entry.isDirectory)
                    val target = ArchiveIntake.safeTarget(destination, entry.name)
                    if (entry.isDirectory) {
                        require(target.isDirectory || target.mkdirs()) {
                            "Не вдалося створити папку вкладеного ZIP."
                        }
                        directories++
                    } else {
                        val parent = target.parentFile
                            ?: error("Некоректний шлях у вкладеному ZIP.")
                        require(parent.isDirectory || parent.mkdirs()) {
                            "Не вдалося створити папку для файла вкладеного ZIP."
                        }
                        archive.getInputStream(entry).use { input ->
                            BufferedOutputStream(target.outputStream(), BUFFER_SIZE).use { output ->
                                val buffer = ByteArray(BUFFER_SIZE)
                                while (true) {
                                    checkCancelled(isCancelled)
                                    val read = input.read(buffer)
                                    if (read < 0) break
                                    if (read == 0) continue
                                    require(
                                        read.toLong() <= MAX_TOTAL_EXPANDED_BYTES - cumulativeBytes,
                                    ) {
                                        "Загальний розпакований обсяг вкладених архівів завеликий."
                                    }
                                    require(extractionRoot.usableSpace - read >= MIN_FREE_SPACE_BYTES) {
                                        "Недостатньо вільного місця для вкладених ZIP."
                                    }
                                    output.write(buffer, 0, read)
                                    cumulativeBytes += read
                                }
                            }
                        }
                        files++
                    }
                    entriesDone++
                    onProgress(
                        ArchiveIntake.Progress(
                            stage = "Вкладений ZIP " + (index + 1) + "/" +
                                candidates.size + "…",
                            entriesDone = entryIndex + 1,
                            entriesTotal = entries.size,
                            bytesDone = cumulativeBytes,
                            bytesTotal = null,
                        ),
                    )
                }
            }

            val roots = ArchiveIntake.findRenaultRawRoots(destination)
            require(roots.isNotEmpty()) {
                "У вкладеному ZIP «" + inner.name + "» не знайдено папки Renault " +
                    "з INDEX.HTM / INDEX.HTML / ACCUEIL.HTM. " +
                    "Глибше вкладені архіви не розпаковуються автоматично."
            }
            foundRoots += roots
        }

        return Result(
            rawRoots = foundRoots,
            files = files,
            directories = directories,
            expandedBytes = cumulativeBytes - outerExpandedBytes,
        )
    }

    private fun checkCancelled(isCancelled: () -> Boolean) {
        if (isCancelled()) throw ConversionCancelledException()
    }
}
