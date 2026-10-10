package com.saney.renaultdocs

import com.github.junrar.Archive
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.zip.ZipFile
import org.apache.commons.compress.archivers.sevenz.SevenZFile

/**
 * Safe local archive extraction foundation for old Renault documentation.
 *
 * This class never decides where a user archive lives. Callers copy/read a SAF
 * document into app-private staging first, then pass that local file here.
 *
 * Every archive member is materialized as a normal file/directory under the
 * supplied extraction root. Link/redirection semantics are deliberately not
 * recreated, which keeps extraction contained inside app-owned staging.
 *
 * Duplicate preflight may read only a bounded prefix of ZIP entrypoint HTML
 * directly from the archive to recover a Renault NT identity before extraction.
 */
object ArchiveIntake {
    enum class Format {
        ZIP,
        SEVEN_Z,
        RAR,
    }

    data class Progress(
        val stage: String,
        val entriesDone: Int,
        val entriesTotal: Int?,
        val bytesDone: Long,
        val bytesTotal: Long?,
    )

    data class ExtractResult(
        val format: Format,
        val files: Int,
        val directories: Int,
        val extractedBytes: Long,
        val rawRoots: List<File>,
    )

    data class RawRootHint(
        val relativePath: String,
        val leafName: String,
        val documentCode: String? = null,
    )

    data class Inspection(
        val format: Format,
        val rawRoots: List<RawRootHint>,
        val preparedPackageDetected: Boolean = false,
    )

    fun detectFormat(
        source: File,
    ): Format {
        require(
            source.isFile,
        ) {
            "Архів недоступний."
        }

        val header =
            source.inputStream()
                .buffered()
                .use {
                    input ->
                    ByteArray(
                        8,
                    ).also {
                        bytes ->
                        var offset =
                            0
                        while (
                            offset <
                            bytes.size
                        ) {
                            val read =
                                input.read(
                                    bytes,
                                    offset,
                                    bytes.size -
                                        offset,
                                )
                            if (
                                read <=
                                0
                            ) {
                                break
                            }
                            offset +=
                                read
                        }
                    }
                }

        if (
            header.size >=
            4 &&
            header[0] ==
                0x50.toByte() &&
            header[1] ==
                0x4b.toByte() &&
            (
                header[2] ==
                    0x03.toByte() ||
                    header[2] ==
                    0x05.toByte() ||
                    header[2] ==
                    0x07.toByte()
            ) &&
            (
                header[3] ==
                    0x04.toByte() ||
                    header[3] ==
                    0x06.toByte() ||
                    header[3] ==
                    0x08.toByte()
            )
        ) {
            return Format.ZIP
        }

        if (
            header.size >=
            6 &&
            header.copyOfRange(
                0,
                6,
            ).contentEquals(
                byteArrayOf(
                    0x37,
                    0x7a,
                    0xbc.toByte(),
                    0xaf.toByte(),
                    0x27,
                    0x1c,
                ),
            )
        ) {
            return Format.SEVEN_Z
        }

        if (
            header.size >=
            7 &&
            header.copyOfRange(
                0,
                7,
            ).contentEquals(
                byteArrayOf(
                    0x52,
                    0x61,
                    0x72,
                    0x21,
                    0x1a,
                    0x07,
                    0x00,
                ),
            )
        ) {
            return Format.RAR
        }

        if (
            header.size >=
            8 &&
            header.copyOfRange(
                0,
                8,
            ).contentEquals(
                byteArrayOf(
                    0x52,
                    0x61,
                    0x72,
                    0x21,
                    0x1a,
                    0x07,
                    0x01,
                    0x00,
                ),
            )
        ) {
            return Format.RAR
        }

        error(
            "Непідтримуваний архів. Потрібен ZIP, 7Z або RAR."
        )
    }

    fun inspectRawRoots(
        source: File,
        sourceNameHint: String? = null,
    ): Inspection {
        require(
            source.isFile,
        ) {
            "Архів недоступний."
        }

        val format =
            detectFormat(
                source,
            )
        val rawParents =
            linkedSetOf<String>()
        val preparedParents =
            linkedSetOf<String>()
        val entryNames =
            mutableListOf<String>()
        val identityTextByParent =
            mutableMapOf<String, MutableList<String>>()
        val inspectedPaths = EntryPathGuard()
        var count =
            0

        fun inspectEntry(
            entryName: String,
            isDirectory: Boolean,
        ): String? {
            count += 1
            checkEntryCount(count)
            inspectedPaths.check(entryName, isDirectory)
            if (isDirectory) {
                return null
            }

            val segments =
                validatedEntrySegments(
                    entryName,
                )

            if (
                segments.isEmpty()
            ) {
                return null
            }

            entryNames +=
                segments.joinToString(
                    "/",
                )

            val fileName =
                segments.last()
                    .lowercase(
                        Locale.ROOT,
                    )
            val parent =
                segments
                    .dropLast(
                        1,
                    )
                    .joinToString(
                        "/",
                    )

            if (
                parent.count {
                    it ==
                        '/'
                } >
                MAX_DISCOVERY_DEPTH
            ) {
                return null
            }

            return when {
                fileName in
                    RAW_ENTRYPOINTS -> {
                    rawParents +=
                        parent
                    parent
                }

                fileName ==
                    "renault-dataset.json" -> {
                    preparedParents +=
                        parent
                    null
                }

                else ->
                    null
            }
        }

        when (
            format
        ) {
            Format.ZIP ->
                ZipFile(
                    source,
                ).use {
                    archive ->
                    val entries =
                        archive.entries()

                    while (
                        entries.hasMoreElements()
                    ) {
                        val entry =
                            entries.nextElement()

                        if (
                            entry.isDirectory
                        ) {
                            inspectEntry(entry.name, true)
                        } else {
                            val rawParent =
                                inspectEntry(
                                    entry.name,
                                    false,
                                )

                            if (
                                rawParent !=
                                null
                            ) {
                                runCatching {
                                    archive.getInputStream(
                                        entry,
                                    ).use {
                                        input ->
                                        readIdentityProbeText(
                                            input,
                                        )
                                    }
                                }.getOrNull()
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                    ?.let {
                                        text ->
                                        identityTextByParent
                                            .getOrPut(
                                                rawParent,
                                            ) {
                                                mutableListOf()
                                            }
                                            .add(
                                                text,
                                            )
                                    }
                            }
                        }
                    }
                }

            Format.SEVEN_Z -> {
                @Suppress(
                    "DEPRECATION",
                )
                val archive =
                    SevenZFile(
                        source,
                    )

                archive.use {
                    sevenZ ->
                    while (
                        true
                    ) {
                        val entry =
                            sevenZ.nextEntry
                                ?: break

                        val name =
                            entry.name
                                ?: error(
                                    "7Z містить запис без назви."
                                )

                        inspectEntry(
                            name,
                            entry.isDirectory,
                        )
                    }
                }
            }

            Format.RAR ->
                Archive(
                    source,
                ).use {
                    archive ->
                    require(
                        !archive.isPasswordProtected,
                    ) {
                        "RAR захищений паролем. Парольні архіви поки не підтримуються."
                    }

                    archive.fileHeaders
                        .forEach {
                            header ->
                            inspectEntry(
                                header.fileName,
                                header.isDirectory,
                            )
                        }
                }
        }

        val hints =
            rawParents
                .filterNot {
                    it in
                        preparedParents
                }
                .map {
                    parent ->
                    val leafName =
                        if (
                            parent.isBlank()
                        ) {
                            sourceNameHint
                                ?.substringBeforeLast(
                                    '.',
                                    sourceNameHint,
                                )
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: "archive-root"
                        } else {
                            parent
                                .substringAfterLast(
                                    '/',
                                )
                        }
                    val strongDocumentCodes =
                        linkedSetOf<String>()
                    val pathDocumentCodes =
                        linkedSetOf<String>()

                    fun collectDocumentCode(
                        target: MutableSet<String>,
                        value: String?,
                    ) {
                        RenaultVolumeIdentity
                            .parse(
                                value,
                            )
                            .documentCode
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?.let {
                                target +=
                                    it
                            }
                    }

                    collectDocumentCode(
                        strongDocumentCodes,
                        parent,
                    )
                    collectDocumentCode(
                        strongDocumentCodes,
                        leafName,
                    )

                    if (
                        rawParents.size ==
                        1
                    ) {
                        collectDocumentCode(
                            strongDocumentCodes,
                            sourceNameHint,
                        )
                    }

                    identityTextByParent[
                        parent
                    ]
                        .orEmpty()
                        .forEach {
                            collectDocumentCode(
                                strongDocumentCodes,
                                it,
                            )
                        }

                    entryNames
                        .asSequence()
                        .filter {
                            name ->
                            name ==
                                parent ||
                                name.startsWith(
                                    parent +
                                        "/",
                                )
                        }
                        .forEach {
                            collectDocumentCode(
                                pathDocumentCodes,
                                it,
                            )
                        }

                    val documentCode =
                        when {
                            strongDocumentCodes.size ==
                                1 ->
                                strongDocumentCodes
                                    .single()

                            strongDocumentCodes.isEmpty() &&
                                pathDocumentCodes.size ==
                                1 ->
                                pathDocumentCodes
                                    .single()

                            else ->
                                null
                        }

                    RawRootHint(
                        relativePath =
                            parent,
                        leafName =
                            leafName,
                        documentCode =
                            documentCode,
                    )
                }
                .filter {
                    it.leafName
                        .isNotBlank()
                }
                .sortedWith(
                    compareBy<RawRootHint> {
                        it.relativePath
                            .count {
                                char ->
                                char ==
                                    '/'
                            }
                    }.thenBy {
                        it.relativePath
                            .lowercase(
                                Locale.ROOT,
                            )
                    }
                )

        return Inspection(
            format =
                format,
            rawRoots =
                hints,
            preparedPackageDetected =
                preparedParents.isNotEmpty(),
        )
    }

    private fun readIdentityProbeText(
        input: InputStream,
    ): String {
        val buffer =
            ByteArray(
                MAX_IDENTITY_PROBE_BYTES,
            )
        var total =
            0

        while (
            total <
            buffer.size
        ) {
            val read =
                input.read(
                    buffer,
                    total,
                    buffer.size -
                        total,
                )

            if (
                read <=
                0
            ) {
                break
            }

            total +=
                read
        }

        return String(
            buffer,
            0,
            total,
            StandardCharsets.ISO_8859_1,
        )
    }

    private fun validatedEntrySegments(
        entryName: String,
    ): List<String> {
        val normalized =
            entryName
                .replace(
                    '\\',
                    '/',
                )
                .trim()

        require(
            normalized.isNotBlank(),
        ) {
            "Архів містить порожній шлях."
        }

        require(
            !normalized.startsWith(
                "/",
            ) &&
                !WINDOWS_ABSOLUTE
                    .containsMatchIn(
                        normalized,
                    )
        ) {
            "Архів містить абсолютний шлях: " +
                entryName
        }

        val segments =
            normalized
                .split(
                    '/',
                )
                .filter {
                    it.isNotEmpty() &&
                        it !=
                            "."
                }

        require(
            segments.isNotEmpty() &&
                segments.none {
                    it ==
                        ".."
                }
        ) {
            "Архів містить небезпечний шлях: " +
                entryName
        }

        return segments
    }

    /**
     * Reject duplicate payloads, case-fold collisions, and file/directory
     * conflicts before they can overwrite staged Renault source files.
     */
    internal class EntryPathGuard {
        private val files = hashSetOf<String>()
        private val directories = hashSetOf<String>()
        private val spelling = hashMapOf<String, String>()

        fun check(name: String, isDirectory: Boolean) {
            val segments = validatedEntrySegments(name)
            val path = segments.joinToString("/")
            val key = path.lowercase(Locale.ROOT)

            for (index in 1..segments.size) {
                val prefix = segments.take(index).joinToString("/")
                val prefixKey = prefix.lowercase(Locale.ROOT)
                val previous = spelling.putIfAbsent(prefixKey, prefix)
                require(previous == null || previous == prefix) {
                    "Архів містить конфлікт регістру шляху: " + name
                }
                if (index < segments.size) {
                    require(prefixKey !in files) {
                        "Архів містить файл замість папки: " + name
                    }
                    directories += prefixKey
                }
            }

            if (isDirectory) {
                require(key !in files) {
                    "Архів містить файл і папку з однаковою назвою: " + name
                }
                directories += key
            } else {
                require(key !in directories && files.add(key)) {
                    "Архів містить повторний або конфліктний файл: " + name
                }
            }
        }
    }

    fun extract(
        source: File,
        extractionRoot: File,
        onProgress: (Progress) -> Unit = {},
        isCancelled: () -> Boolean = { false },
        allowNoRawRoots: Boolean = false,
    ): ExtractResult {
        checkCancelled(
            isCancelled,
        )

        require(
            source.isFile,
        ) {
            "Архів недоступний."
        }

        if (
            extractionRoot.exists()
        ) {
            require(
                extractionRoot.deleteRecursively(),
            ) {
                "Не вдалося очистити старий archive staging."
            }
        }

        require(
            extractionRoot.mkdirs(),
        ) {
            "Не вдалося створити archive staging."
        }

        val format =
            detectFormat(
                source,
            )

        val stats =
            try {
                when (
                    format
                ) {
                    Format.ZIP ->
                        extractZip(
                            source = source,
                            root = extractionRoot,
                            onProgress = onProgress,
                            isCancelled = isCancelled,
                        )

                    Format.SEVEN_Z ->
                        extractSevenZ(
                            source = source,
                            root = extractionRoot,
                            onProgress = onProgress,
                            isCancelled = isCancelled,
                        )

                    Format.RAR ->
                        extractRar(
                            source = source,
                            root = extractionRoot,
                            onProgress = onProgress,
                            isCancelled = isCancelled,
                        )
                }
            } catch (
                error: ConversionCancelledException
            ) {
                extractionRoot.deleteRecursively()
                throw error
            } catch (
                error: Throwable
            ) {
                extractionRoot.deleteRecursively()
                throw archiveFailure(
                    format = format,
                    error = error,
                )
            }

        checkCancelled(
            isCancelled,
        )

        val rawRoots =
            findRenaultRawRoots(
                extractionRoot,
            )

        require(
            allowNoRawRoots || rawRoots.isNotEmpty(),
        ) {
            "В архіві не знайдено raw Renault тому з INDEX.HTM / INDEX.HTML / ACCUEIL.HTM у корені."
        }

        return ExtractResult(
            format = format,
            files = stats.files,
            directories = stats.directories,
            extractedBytes = stats.bytes,
            rawRoots = rawRoots,
        )
    }

    fun findRenaultRawRoots(
        extractionRoot: File,
    ): List<File> {
        if (
            !extractionRoot.isDirectory
        ) {
            return emptyList()
        }

        return extractionRoot
            .walkTopDown()
            .maxDepth(
                MAX_DISCOVERY_DEPTH,
            )
            .filter {
                it.isDirectory
            }
            .filter {
                directory ->
                val children =
                    directory.listFiles()
                        ?: return@filter false

                val fileNames =
                    children
                        .asSequence()
                        .filter {
                            it.isFile
                        }
                        .map {
                            it.name.lowercase(
                                Locale.ROOT,
                            )
                        }
                        .toSet()

                fileNames.none {
                    it ==
                        "renault-dataset.json"
                } &&
                    fileNames.any {
                        it in
                            RAW_ENTRYPOINTS
                    }
            }
            .sortedWith(
                compareBy<File> {
                    relativePath(
                        extractionRoot,
                        it,
                    ).count {
                        char ->
                        char ==
                            '/'
                    }
                }.thenBy {
                    relativePath(
                        extractionRoot,
                        it,
                    ).lowercase(
                        Locale.ROOT,
                    )
                }
            )
            .toList()
    }

    /**
     * Resolve an extracted Renault raw root saved by the multi-volume chooser.
     *
     * A raw INDEX.HTM may legitimately live at the archive's top level:
     * its relativePath is then "", representing extractionRoot itself.
     * Nonempty candidate paths must remain strictly within staging.
     */
    /**
     * The staging folder is named "extracted", not the Renault volume.
     * Do not let that private implementation name become a package ID.
     */
    internal fun rawSourceName(
        extractionRoot: File,
        rawRoot: File,
        archiveFileName: String,
    ): String =
        if (rawRoot.canonicalFile == extractionRoot.canonicalFile) {
            archiveFileName.substringBeforeLast(
                '.',
                archiveFileName,
            ).trim().ifBlank {
                "archive-root"
            }
        } else {
            rawRoot.name
        }

    internal fun resolveRawRoot(
        extractionRoot: File,
        relativePath: String,
    ): File {
        val root = extractionRoot.canonicalFile
        require(root.isDirectory) {
            "Archive staging більше не доступний."
        }

        val target =
            if (relativePath.isEmpty()) {
                root
            } else {
                safeTarget(root, relativePath)
            }

        require(
            target.isDirectory &&
                (target.path == root.path ||
                    target.path.startsWith(root.path + File.separator)),
        ) {
            "Втрачено вибраний raw-том архіву."
        }

        return target
    }

    internal fun safeTarget(
        root: File,
        entryName: String,
    ): File {
        val normalized =
            entryName
                .replace(
                    '\\',
                    '/',
                )
                .trim()

        require(
            normalized.isNotBlank(),
        ) {
            "Архів містить порожній шлях."
        }

        require(
            !normalized.startsWith(
                "/",
            ) &&
                !WINDOWS_ABSOLUTE
                    .containsMatchIn(
                        normalized,
                    )
        ) {
            "Архів містить абсолютний шлях: " +
                entryName
        }

        val segments =
            normalized
                .split(
                    '/',
                )
                .filter {
                    it.isNotEmpty() &&
                        it !=
                            "."
                }

        require(
            segments.isNotEmpty() &&
                segments.none {
                    it ==
                        ".."
                }
        ) {
            "Архів містить небезпечний шлях: " +
                entryName
        }

        val canonicalRoot =
            root.canonicalFile
        val target =
            segments.fold(
                canonicalRoot,
            ) {
                parent,
                segment ->
                File(
                    parent,
                    segment,
                )
            }
                .canonicalFile

        val rootPrefix =
            canonicalRoot.path +
                File.separator

        require(
            target.path ==
                canonicalRoot.path ||
                target.path.startsWith(
                    rootPrefix,
                )
        ) {
            "Архів намагається записати файл поза staging: " +
                entryName
        }

        return target
    }

    private data class Stats(
        var files: Int = 0,
        var directories: Int = 0,
        var bytes: Long = 0L,
    )

    private fun extractZip(
        source: File,
        root: File,
        onProgress: (Progress) -> Unit,
        isCancelled: () -> Boolean,
    ): Stats {
        ZipFile(
            source,
        ).use {
            archive ->
            val entries =
                archive.entries()
                    .toList()

            val totalBytes =
                entries
                    .asSequence()
                    .filter {
                        !it.isDirectory &&
                            it.size >
                                0L
                    }
                    .sumOf {
                        it.size
                    }
                    .takeIf {
                        it >
                            0L
                    }

            val stats =
                Stats()
            val entryPaths = EntryPathGuard()

            entries.forEachIndexed {
                index,
                entry ->
                checkCancelled(
                    isCancelled,
                )
                checkEntryCount(
                    index +
                        1,
                )

                entryPaths.check(entry.name, entry.isDirectory)
                val target =
                    safeTarget(
                        root,
                        entry.name,
                    )

                if (
                    entry.isDirectory
                ) {
                    ensureDirectory(
                        target,
                    )
                    stats.directories +=
                        1
                } else {
                    ensureParent(
                        target,
                    )
                    archive.getInputStream(
                        entry,
                    ).use {
                        input ->
                        stats.bytes +=
                            copyEntry(
                                input = input,
                                output = target,
                                currentBytes = stats.bytes,
                                root = root,
                                isCancelled = isCancelled,
                            )
                    }
                    stats.files +=
                        1
                }

                onProgress(
                    Progress(
                        stage = "Розпаковую ZIP…",
                        entriesDone = index + 1,
                        entriesTotal = entries.size,
                        bytesDone = stats.bytes,
                        bytesTotal = totalBytes,
                    ),
                )
            }

            return stats
        }
    }

    private fun extractSevenZ(
        source: File,
        root: File,
        onProgress: (Progress) -> Unit,
        isCancelled: () -> Boolean,
    ): Stats {
        @Suppress(
            "DEPRECATION",
        )
        val archive =
            SevenZFile(
                source,
            )

        archive.use {
            sevenZ ->
            val stats =
                Stats()
            val entryPaths = EntryPathGuard()
            var count =
                0

            while (
                true
            ) {
                checkCancelled(
                    isCancelled,
                )

                val entry =
                    sevenZ.nextEntry
                        ?: break

                count +=
                    1
                checkEntryCount(
                    count,
                )

                val name =
                    entry.name
                        ?: error(
                            "7Z містить запис без назви."
                        )

                entryPaths.check(name, entry.isDirectory)
                val target =
                    safeTarget(
                        root,
                        name,
                    )

                if (
                    entry.isDirectory
                ) {
                    ensureDirectory(
                        target,
                    )
                    stats.directories +=
                        1
                } else {
                    ensureParent(
                        target,
                    )
                    BufferedOutputStream(
                        FileOutputStream(
                            target,
                        ),
                        BUFFER_SIZE,
                    ).use {
                        output ->
                        val buffer =
                            ByteArray(
                                BUFFER_SIZE,
                            )

                        while (
                            true
                        ) {
                            checkCancelled(
                                isCancelled,
                            )

                            val read =
                                sevenZ.read(
                                    buffer,
                                )

                            if (
                                read <
                                0
                            ) {
                                break
                            }

                            if (
                                read ==
                                0
                            ) {
                                continue
                            }

                            reserveBytes(
                                root = root,
                                currentBytes = stats.bytes,
                                nextBytes = read.toLong(),
                            )
                            output.write(
                                buffer,
                                0,
                                read,
                            )
                            stats.bytes +=
                                read
                        }
                    }
                    stats.files +=
                        1
                }

                onProgress(
                    Progress(
                        stage = "Розпаковую 7Z…",
                        entriesDone = count,
                        entriesTotal = null,
                        bytesDone = stats.bytes,
                        bytesTotal = null,
                    ),
                )
            }

            return stats
        }
    }

    private fun extractRar(
        source: File,
        root: File,
        onProgress: (Progress) -> Unit,
        isCancelled: () -> Boolean,
    ): Stats {
        Archive(
            source,
        ).use {
            archive ->
            require(
                !archive.isPasswordProtected,
            ) {
                "RAR захищений паролем. Парольні архіви поки не підтримуються."
            }

            val headers =
                archive.fileHeaders

            val totalBytes =
                headers
                    .asSequence()
                    .filter {
                        !it.isDirectory &&
                            it.fullUnpackSize >
                                0L
                    }
                    .sumOf {
                        it.fullUnpackSize
                    }
                    .takeIf {
                        it >
                            0L
                    }

            val stats =
                Stats()
            val entryPaths = EntryPathGuard()

            headers.forEachIndexed {
                index,
                header ->
                checkCancelled(
                    isCancelled,
                )
                checkEntryCount(
                    index +
                        1,
                )

                entryPaths.check(header.fileName, header.isDirectory)
                val target =
                    safeTarget(
                        root,
                        header.fileName,
                    )

                if (
                    header.isDirectory
                ) {
                    ensureDirectory(
                        target,
                    )
                    stats.directories +=
                        1
                } else {
                    ensureParent(
                        target,
                    )
                    BufferedOutputStream(
                        FileOutputStream(
                            target,
                        ),
                        BUFFER_SIZE,
                    ).use {
                        output ->
                        archive.extractFile(
                            header,
                            LimitedOutputStream(
                                delegate = output,
                                root = root,
                                currentBytes = {
                                    stats.bytes
                                },
                                onBytes = {
                                    count ->
                                    stats.bytes +=
                                        count
                                },
                                isCancelled = isCancelled,
                            ),
                        )
                    }
                    stats.files +=
                        1
                }

                onProgress(
                    Progress(
                        stage = "Розпаковую RAR…",
                        entriesDone = index + 1,
                        entriesTotal = headers.size,
                        bytesDone = stats.bytes,
                        bytesTotal = totalBytes,
                    ),
                )
            }

            return stats
        }
    }

    private class LimitedOutputStream(
        private val delegate: java.io.OutputStream,
        private val root: File,
        private val currentBytes: () -> Long,
        private val onBytes: (Long) -> Unit,
        private val isCancelled: () -> Boolean,
    ) : java.io.OutputStream() {
        override fun write(
            value: Int,
        ) {
            checkCancelled(
                isCancelled,
            )
            reserveBytes(
                root = root,
                currentBytes = currentBytes(),
                nextBytes = 1L,
            )
            delegate.write(
                value,
            )
            onBytes(
                1L,
            )
        }

        override fun write(
            buffer: ByteArray,
            offset: Int,
            length: Int,
        ) {
            checkCancelled(
                isCancelled,
            )
            reserveBytes(
                root = root,
                currentBytes = currentBytes(),
                nextBytes = length.toLong(),
            )
            delegate.write(
                buffer,
                offset,
                length,
            )
            onBytes(
                length.toLong(),
            )
        }

        override fun flush() {
            delegate.flush()
        }
    }

    private fun copyEntry(
        input: java.io.InputStream,
        output: File,
        currentBytes: Long,
        root: File,
        isCancelled: () -> Boolean,
    ): Long {
        var written =
            0L

        BufferedInputStream(
            input,
            BUFFER_SIZE,
        ).use {
            source ->
            BufferedOutputStream(
                FileOutputStream(
                    output,
                ),
                BUFFER_SIZE,
            ).use {
                target ->
                val buffer =
                    ByteArray(
                        BUFFER_SIZE,
                    )

                while (
                    true
                ) {
                    checkCancelled(
                        isCancelled,
                    )

                    val read =
                        source.read(
                            buffer,
                        )

                    if (
                        read <
                        0
                    ) {
                        break
                    }

                    if (
                        read ==
                        0
                    ) {
                        continue
                    }

                    reserveBytes(
                        root = root,
                        currentBytes = currentBytes + written,
                        nextBytes = read.toLong(),
                    )
                    target.write(
                        buffer,
                        0,
                        read,
                    )
                    written +=
                        read
                }
            }
        }

        return written
    }

    private fun reserveBytes(
        root: File,
        currentBytes: Long,
        nextBytes: Long,
    ) {
        require(
            nextBytes >=
                0L
        )

        val nextTotal =
            currentBytes +
                nextBytes

        require(
            nextTotal <=
                MAX_EXPANDED_BYTES
        ) {
            "Архів занадто великий після розпакування."
        }

        require(
            root.usableSpace -
                nextBytes >=
                MIN_FREE_SPACE_BYTES
        ) {
            "Недостатньо вільного місця для розпакування архіву."
        }
    }

    private fun checkEntryCount(
        count: Int,
    ) {
        require(
            count <=
                MAX_ENTRIES
        ) {
            "Архів містить занадто багато файлів."
        }
    }

    private fun ensureDirectory(
        directory: File,
    ) {
        require(
            directory.isDirectory ||
                directory.mkdirs(),
        ) {
            "Не вдалося створити папку: " +
                directory.name
        }
    }

    private fun ensureParent(
        file: File,
    ) {
        val parent =
            file.parentFile
                ?: error(
                    "Некоректний шлях у архіві."
                )

        ensureDirectory(
            parent,
        )
    }

    private fun checkCancelled(
        isCancelled: () -> Boolean,
    ) {
        if (
            isCancelled()
        ) {
            throw ConversionCancelledException()
        }
    }

    private fun archiveFailure(
        format: Format,
        error: Throwable,
    ): IllegalStateException {
        val message =
            error.message
                .orEmpty()

        val friendly =
            when {
                message.contains(
                    "password",
                    ignoreCase = true,
                ) ||
                    message.contains(
                        "encrypted",
                        ignoreCase = true,
                    ) ->
                    "Архів захищений паролем. Парольні архіви поки не підтримуються."

                else ->
                    "Не вдалося розпакувати " +
                        format.name
                            .replace(
                                "_",
                                "",
                            ) +
                        ": " +
                        message.ifBlank {
                            error.javaClass.simpleName
                        }
            }

        return IllegalStateException(
            friendly,
            error,
        )
    }

    private fun relativePath(
        root: File,
        file: File,
    ): String =
        root
            .canonicalFile
            .toPath()
            .relativize(
                file.canonicalFile
                    .toPath(),
            )
            .toString()
            .replace(
                File.separatorChar,
                '/',
            )

    private val RAW_ENTRYPOINTS =
        setOf(
            "index.htm",
            "index.html",
            "accueil.htm",
        )

    private val WINDOWS_ABSOLUTE =
        Regex(
            "^[A-Za-z]:[/\\\\]"
        )

    private const val MAX_IDENTITY_PROBE_BYTES =
        64 *
            1024

    private const val BUFFER_SIZE =
        1024 *
            1024

    private const val MAX_ENTRIES =
        200_000

    private const val MAX_DISCOVERY_DEPTH =
        16

    private const val MAX_EXPANDED_BYTES =
        16L *
            1024L *
            1024L *
            1024L

    private const val MIN_FREE_SPACE_BYTES =
        128L *
            1024L *
            1024L
}
