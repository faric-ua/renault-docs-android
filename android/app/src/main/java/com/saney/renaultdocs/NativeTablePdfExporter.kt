package com.saney.renaultdocs

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import kotlin.math.max

data class NativeTableRow(
    val cells: List<String>,
    val header: Boolean = false,
)

data class NativeTableData(
    val rows: List<NativeTableRow>,
)

data class NativeTablePdfData(
    val title: String,
    val criteria: String?,
    val metadata: List<String>,
    val tables: List<NativeTableData>,
    val suggestedFileName: String,
)

object NativeTablePdfExporter {
    private const val PORTRAIT_PAGE_WIDTH = 595
    private const val PORTRAIT_PAGE_HEIGHT = 842
    private const val LANDSCAPE_PAGE_WIDTH = 842
    private const val LANDSCAPE_PAGE_HEIGHT = 595
    private const val MARGIN = 57f
    private const val CELL_PADDING = 5f
    private const val BODY_TEXT_SIZE = 10f
    private const val HEADER_TEXT_SIZE = 10f
    private const val TABLE_HEADER_TEXT_SIZE = 14f
    private const val TITLE_TEXT_SIZE = 16f
    private const val SUBTITLE_TEXT_SIZE = 11f
    private const val LINE_GAP = 3f

    fun write(
        context: Context,
        uri: Uri,
        data: NativeTablePdfData,
    ) {
        val document = PdfDocument()

        val widestColumnCount =
            data.tables.maxOfOrNull { table ->
                table.rows.maxOfOrNull {
                    it.cells.size
                } ?: 1
            } ?: 1

        val isPinesExport =
            widestColumnCount >= 4 &&
                data.suggestedFileName
                    .contains(
                        "(pines)",
                        ignoreCase = true,
                    )

        val pageWidth =
            if (
                isPinesExport ||
                widestColumnCount <= 2
            ) {
                PORTRAIT_PAGE_WIDTH
            } else {
                LANDSCAPE_PAGE_WIDTH
            }

        val pageHeight =
            if (
                isPinesExport ||
                widestColumnCount <= 2
            ) {
                PORTRAIT_PAGE_HEIGHT
            } else {
                LANDSCAPE_PAGE_HEIGHT
            }

        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var canvas = null as android.graphics.Canvas?
        var y = MARGIN

        val textPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = BODY_TEXT_SIZE
                typeface =
                    Typeface.create(
                        Typeface.SANS_SERIF,
                        Typeface.NORMAL,
                    )
            }

        val boldPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = HEADER_TEXT_SIZE
                typeface =
                    Typeface.create(
                        Typeface.SANS_SERIF,
                        Typeface.BOLD,
                    )
            }

        val tableHeaderPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = TABLE_HEADER_TEXT_SIZE
                typeface =
                    Typeface.create(
                        Typeface.SANS_SERIF,
                        Typeface.BOLD,
                    )
            }

        val titlePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = TITLE_TEXT_SIZE
                typeface =
                    Typeface.create(
                        Typeface.SANS_SERIF,
                        Typeface.BOLD,
                    )
            }

        val subtitlePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.DKGRAY
                textSize = SUBTITLE_TEXT_SIZE
                typeface =
                    Typeface.create(
                        Typeface.SANS_SERIF,
                        Typeface.NORMAL,
                    )
            }

        val pinCodePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 15f
                typeface =
                    Typeface.create(
                        Typeface.SANS_SERIF,
                        Typeface.BOLD,
                    )
            }

        val pinTitlePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 17f
                typeface =
                    Typeface.create(
                        Typeface.SANS_SERIF,
                        Typeface.BOLD,
                    )
            }

        val pinCriteriaPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 10.5f
                typeface =
                    Typeface.create(
                        Typeface.SANS_SERIF,
                        Typeface.BOLD,
                    )
            }

        val linePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.GRAY
                style = Paint.Style.STROKE
                strokeWidth = 0.7f
            }

        val connectorAccent =
            Color.rgb(
                33,
                150,
                243,
            )
        val pinNumberAccent =
            Color.rgb(
                0,
                137,
                123,
            )
        val wireSectionAccent =
            Color.rgb(
                67,
                160,
                71,
            )
        val wireCodeAccent =
            Color.rgb(
                251,
                140,
                0,
            )
        val descriptionAccent =
            Color.rgb(
                123,
                31,
                162,
            )

        val iconStrokePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = connectorAccent
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }

        val iconFillPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = connectorAccent
                style = Paint.Style.FILL
            }

        val iconBadgePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }

        fun finishPage() {
            val current = page
            if (current != null) {
                document.finishPage(current)
            }
            page = null
            canvas = null
        }

        fun startPage() {
            finishPage()
            pageNumber += 1
            page =
                document.startPage(
                    PdfDocument.PageInfo
                        .Builder(
                            pageWidth,
                            pageHeight,
                            pageNumber,
                        )
                        .create()
                )
            canvas = page!!.canvas
            y = MARGIN
        }

        fun availableWidth(): Float =
            pageWidth - MARGIN * 2f

        fun wrap(
            value: String,
            paint: Paint,
            maxWidth: Float,
        ): List<String> {
            val text = value.trim()
            if (text.isBlank()) {
                return listOf("")
            }

            val words =
                text
                    .replace("\n", " ")
                    .split(
                        Regex("\\s+"),
                    )

            val lines =
                mutableListOf<String>()
            var current = ""

            for (word in words) {
                val candidate =
                    if (current.isBlank()) {
                        word
                    } else {
                        "$current $word"
                    }

                if (
                    paint.measureText(candidate) <=
                    maxWidth
                ) {
                    current = candidate
                    continue
                }

                if (current.isNotBlank()) {
                    lines.add(current)
                    current = ""
                }

                if (
                    paint.measureText(word) <=
                    maxWidth
                ) {
                    current = word
                    continue
                }

                var chunk = ""
                for (char in word) {
                    val next = chunk + char
                    if (
                        paint.measureText(next) >
                        maxWidth &&
                        chunk.isNotBlank()
                    ) {
                        lines.add(chunk)
                        chunk = char.toString()
                    } else {
                        chunk = next
                    }
                }
                current = chunk
            }

            if (current.isNotBlank()) {
                lines.add(current)
            }

            return lines.ifEmpty {
                listOf("")
            }
        }

        fun drawWrapped(
            value: String,
            paint: Paint,
            left: Float,
            top: Float,
            width: Float,
        ): Float {
            val lines =
                wrap(
                    value,
                    paint,
                    max(
                        12f,
                        width,
                    ),
                )
            val lineHeight =
                paint.textSize + LINE_GAP
            var baseline =
                top + paint.textSize

            for (line in lines) {
                canvas!!.drawText(
                    line,
                    left,
                    baseline,
                    paint,
                )
                baseline += lineHeight
            }

            return lines.size * lineHeight
        }

        fun drawCenteredWrapped(
            value: String,
            paint: Paint,
            top: Float,
            width: Float,
        ): Float {
            val lines =
                wrap(
                    value,
                    paint,
                    max(
                        12f,
                        width,
                    ),
                )
            val lineHeight =
                paint.textSize + LINE_GAP
            var baseline =
                top + paint.textSize
            val centerX =
                pageWidth / 2f

            for (line in lines) {
                canvas!!.drawText(
                    line,
                    centerX -
                        paint.measureText(
                            line,
                        ) / 2f,
                    baseline,
                    paint,
                )
                baseline += lineHeight
            }

            return lines.size * lineHeight
        }

        fun drawDocumentHeader() {
            val width = availableWidth()

            val titleHeight =
                drawCenteredWrapped(
                    data.title,
                    titlePaint,
                    y,
                    width,
                )
            y += titleHeight + 12f

            data.criteria
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    val height =
                        drawCenteredWrapped(
                            it,
                            subtitlePaint,
                            y,
                            width * 0.94f,
                        )
                    y += height + 8f
                }

            for (item in data.metadata) {
                val height =
                    drawCenteredWrapped(
                        item,
                        boldPaint,
                        y,
                        width * 0.94f,
                    )
                y += height + 4f
            }

            if (data.metadata.isNotEmpty()) {
                y += 6f
            }
        }

        fun prepareIconPaints(
            accent: Int,
        ) {
            iconStrokePaint.color =
                accent
            iconFillPaint.color =
                accent
            iconBadgePaint.color =
                Color.argb(
                    30,
                    Color.red(accent),
                    Color.green(accent),
                    Color.blue(accent),
                )
        }

        fun drawIconBadge(
            centerX: Float,
            centerY: Float,
            radius: Float = 9f,
        ) {
            canvas!!.drawCircle(
                centerX,
                centerY,
                radius,
                iconBadgePaint,
            )
        }

        fun drawConnectorIcon(
            left: Float,
            top: Float,
            width: Float,
            height: Float,
        ) {
            prepareIconPaints(
                connectorAccent,
            )

            val centerX =
                left +
                    width / 2f
            val centerY =
                top +
                    height / 2f +
                    1f

            drawIconBadge(
                centerX,
                centerY,
                radius =
                    minOf(
                        width,
                        height,
                    ) * 0.31f,
            )

            val bodyWidth =
                width * 0.58f
            val bodyHeight =
                height * 0.46f
            val bodyLeft =
                centerX -
                    bodyWidth / 2f
            val bodyTop =
                centerY -
                    bodyHeight / 2f +
                    2f

            canvas!!.drawRoundRect(
                bodyLeft,
                bodyTop,
                bodyLeft + bodyWidth,
                bodyTop + bodyHeight,
                4f,
                4f,
                iconStrokePaint,
            )

            val tabWidth =
                bodyWidth * 0.30f
            val tabHeight =
                height * 0.11f

            canvas!!.drawRect(
                centerX -
                    tabWidth / 2f,
                bodyTop - tabHeight,
                centerX +
                    tabWidth / 2f,
                bodyTop,
                iconStrokePaint,
            )

            val socketRadius =
                max(
                    1.7f,
                    width * 0.040f,
                )
            val startX =
                bodyLeft +
                    bodyWidth * 0.24f
            val stepX =
                bodyWidth * 0.26f
            val firstY =
                bodyTop +
                    bodyHeight * 0.34f
            val secondY =
                bodyTop +
                    bodyHeight * 0.72f

            for (column in 0..2) {
                val cx =
                    startX +
                        stepX * column

                canvas!!.drawCircle(
                    cx,
                    firstY,
                    socketRadius,
                    iconStrokePaint,
                )
                canvas!!.drawCircle(
                    cx,
                    secondY,
                    socketRadius,
                    iconStrokePaint,
                )
            }
        }

        fun drawPinNumberIcon(
            centerX: Float,
            centerY: Float,
        ) {
            prepareIconPaints(
                pinNumberAccent,
            )
            drawIconBadge(
                centerX,
                centerY,
            )

            canvas!!.drawCircle(
                centerX,
                centerY - 2f,
                3.0f,
                iconStrokePaint,
            )
            canvas!!.drawLine(
                centerX,
                centerY + 1.5f,
                centerX,
                centerY + 6f,
                iconStrokePaint,
            )
        }

        fun drawWireSectionIcon(
            centerX: Float,
            centerY: Float,
        ) {
            prepareIconPaints(
                wireSectionAccent,
            )
            drawIconBadge(
                centerX,
                centerY,
            )

            canvas!!.drawCircle(
                centerX,
                centerY,
                5.5f,
                iconStrokePaint,
            )

            val offsets =
                arrayOf(
                    Pair(0f, 0f),
                    Pair(-2.8f, -1.9f),
                    Pair(2.8f, -1.9f),
                    Pair(-2.8f, 2.4f),
                    Pair(2.8f, 2.4f),
                )

            for (offset in offsets) {
                canvas!!.drawCircle(
                    centerX + offset.first,
                    centerY + offset.second,
                    0.85f,
                    iconFillPaint,
                )
            }
        }

        fun drawWireCodeIcon(
            centerX: Float,
            centerY: Float,
        ) {
            prepareIconPaints(
                wireCodeAccent,
            )
            drawIconBadge(
                centerX,
                centerY,
            )

            val path =
                Path().apply {
                    moveTo(
                        centerX - 8f,
                        centerY + 3f,
                    )
                    lineTo(
                        centerX - 3f,
                        centerY - 4f,
                    )
                    lineTo(
                        centerX + 1f,
                        centerY + 2f,
                    )
                    lineTo(
                        centerX + 8f,
                        centerY - 4f,
                    )
                }

            canvas!!.drawPath(
                path,
                iconStrokePaint,
            )
        }

        fun drawDescriptionIcon(
            centerX: Float,
            centerY: Float,
        ) {
            prepareIconPaints(
                descriptionAccent,
            )
            drawIconBadge(
                centerX,
                centerY,
                radius = 10f,
            )

            canvas!!.drawCircle(
                centerX - 8f,
                centerY,
                1.0f,
                iconFillPaint,
            )
            canvas!!.drawLine(
                centerX - 4f,
                centerY,
                centerX + 6f,
                centerY,
                iconStrokePaint,
            )
            canvas!!.drawLine(
                centerX + 6f,
                centerY,
                centerX + 2f,
                centerY - 3.5f,
                iconStrokePaint,
            )
            canvas!!.drawLine(
                centerX + 6f,
                centerY,
                centerX + 2f,
                centerY + 3.5f,
                iconStrokePaint,
            )
            canvas!!.drawCircle(
                centerX + 10f,
                centerY,
                1.0f,
                iconFillPaint,
            )
        }

        fun pinTableOf():
            NativeTableData? =
            data.tables.firstOrNull { table ->
                table.rows.maxOfOrNull {
                    it.cells.size
                } == 4
            }

        fun connectorInfoLines(
            pinTable: NativeTableData,
        ): List<String> {
            val result =
                mutableListOf<String>()

            for (table in data.tables) {
                if (table === pinTable) {
                    continue
                }

                val columns =
                    table.rows.maxOfOrNull {
                        it.cells.size
                    } ?: 0

                if (columns > 2) {
                    continue
                }

                for (row in table.rows) {
                    val value =
                        row.cells
                            .firstOrNull {
                                it.isNotBlank()
                            }
                            ?.trim()
                            .orEmpty()

                    if (
                        value.isNotBlank() &&
                        value !in result
                    ) {
                        result.add(value)
                    }
                }
            }

            return result
        }

        fun drawConnectorCard(
            lines: List<String>,
        ) {
            if (lines.isEmpty()) {
                return
            }

            val width =
                (
                    availableWidth() *
                        0.58f
                )
                    .coerceAtMost(
                        315f
                    )
            val left =
                (
                    pageWidth -
                        width
                ) / 2f
            val iconWidth =
                72f
                    .coerceAtMost(
                        width * 0.27f
                    )
            val height =
                56f
            val half =
                height / 2f

            canvas!!.drawRect(
                left,
                y,
                left + width,
                y + height,
                linePaint,
            )
            canvas!!.drawLine(
                left + iconWidth,
                y,
                left + iconWidth,
                y + height,
                linePaint,
            )
            canvas!!.drawLine(
                left + iconWidth,
                y + half,
                left + width,
                y + half,
                linePaint,
            )

            drawConnectorIcon(
                left = left,
                top = y,
                width = iconWidth,
                height = height,
            )

            val rightLeft =
                left + iconWidth
            val rightWidth =
                width - iconWidth
            val topText =
                lines.getOrNull(0)
                    .orEmpty()
            val bottomText =
                lines.getOrNull(1)
                    .orEmpty()

            val topBaseline =
                y +
                    half / 2f +
                    tableHeaderPaint.textSize /
                    2.6f
            val bottomBaseline =
                y +
                    half +
                    half / 2f +
                    tableHeaderPaint.textSize /
                    2.6f

            canvas!!.drawText(
                topText,
                rightLeft +
                    (
                        rightWidth -
                            tableHeaderPaint.measureText(
                                topText
                            )
                    ) / 2f,
                topBaseline,
                tableHeaderPaint,
            )

            if (bottomText.isNotBlank()) {
                canvas!!.drawText(
                    bottomText,
                    rightLeft +
                        (
                            rightWidth -
                                tableHeaderPaint
                                    .measureText(
                                        bottomText
                                    )
                        ) / 2f,
                    bottomBaseline,
                    tableHeaderPaint,
                )
            }

            y += height + 12f

            for (
                index in
                2 until lines.size
            ) {
                val extra =
                    lines[index]
                val heightUsed =
                    drawCenteredWrapped(
                        extra,
                        subtitlePaint,
                        y,
                        availableWidth() *
                            0.82f,
                    )
                y += heightUsed + 2f
            }

            if (lines.size > 2) {
                y += 6f
            }
        }

        fun splitTitle():
            Pair<String, String> {
            val parts =
                data.title
                    .split(
                        Regex(
                            "\\s+[—-]\\s+"
                        ),
                        limit = 2,
                    )

            return if (parts.size == 2) {
                Pair(
                    parts[0].trim(),
                    parts[1].trim(),
                )
            } else {
                Pair(
                    "",
                    data.title.trim(),
                )
            }
        }

        fun drawPinIdentityBlock(
            continuation: Boolean,
        ) {
            val (
                code,
                title,
            ) = splitTitle()

            if (continuation) {
                val compact =
                    if (code.isNotBlank()) {
                        "$code — $title"
                    } else {
                        title
                    }

                val used =
                    drawCenteredWrapped(
                        compact,
                        boldPaint,
                        y,
                        availableWidth(),
                    )
                y += used + 4f
                return
            }

            for (item in data.metadata) {
                val used =
                    drawCenteredWrapped(
                        item,
                        subtitlePaint,
                        y,
                        availableWidth() *
                            0.84f,
                    )
                y += used + 2f
            }

            if (data.metadata.isNotEmpty()) {
                y += 4f
            }

            if (code.isNotBlank()) {
                val codeHeight =
                    drawCenteredWrapped(
                        code,
                        pinCodePaint,
                        y,
                        availableWidth(),
                    )
                y += codeHeight + 2f
            }

            val titleHeight =
                drawCenteredWrapped(
                    title,
                    pinTitlePaint,
                    y,
                    availableWidth() *
                        0.92f,
                )
            y += titleHeight + 4f

            data.criteria
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    val height =
                        drawCenteredWrapped(
                            it,
                            pinCriteriaPaint,
                            y,
                            availableWidth() *
                                0.96f,
                        )
                    y += height + 10f
                }
                ?: run {
                    y += 6f
                }
        }

        fun pinColumnWidths(
            table: NativeTableData,
        ): FloatArray {
            val total =
                availableWidth()
            val labels =
                listOf(
                    "№",
                    "мм²",
                    "Код",
                    "Опис",
                )
            val mins =
                floatArrayOf(
                    30f,
                    36f,
                    36f,
                )
            val maxs =
                floatArrayOf(
                    46f,
                    56f,
                    68f,
                )
            val compact =
                FloatArray(3)

            for (column in 0..2) {
                var measured =
                    tableHeaderPaint.measureText(
                        labels[column]
                    )

                for (row in table.rows) {
                    val value =
                        row.cells
                            .getOrNull(column)
                            .orEmpty()
                    val paint =
                        if (row.header) {
                            tableHeaderPaint
                        } else {
                            boldPaint
                        }

                    measured =
                        max(
                            measured,
                            paint.measureText(
                                value
                            ),
                        )
                }

                compact[column] =
                    (
                        measured +
                            CELL_PADDING * 2f +
                            6f
                    )
                        .coerceIn(
                            mins[column],
                            maxs[column],
                        )
            }

            val maxCompactTotal =
                total * 0.34f
            val compactTotal =
                compact.sum()

            if (
                compactTotal >
                maxCompactTotal
            ) {
                val scale =
                    maxCompactTotal /
                        compactTotal
                for (
                    index in
                    compact.indices
                ) {
                    compact[index] *=
                        scale
                }
            }

            return floatArrayOf(
                compact[0],
                compact[1],
                compact[2],
                total -
                    compact[0] -
                    compact[1] -
                    compact[2],
            )
        }

        fun drawPinHeaderRow(
            widths: FloatArray,
        ) {
            val labels =
                listOf(
                    "№",
                    "мм²",
                    "Код",
                    "Опис",
                )
            val rowHeight =
                43f
            var x =
                MARGIN

            for (column in 0..3) {
                val cellWidth =
                    widths[column]

                canvas!!.drawRect(
                    x,
                    y,
                    x + cellWidth,
                    y + rowHeight,
                    linePaint,
                )

                val centerX =
                    x +
                        cellWidth / 2f
                val iconY =
                    y + 12f

                when (column) {
                    0 ->
                        drawPinNumberIcon(
                            centerX,
                            iconY,
                        )

                    1 ->
                        drawWireSectionIcon(
                            centerX,
                            iconY,
                        )

                    2 ->
                        drawWireCodeIcon(
                            centerX,
                            iconY,
                        )

                    else ->
                        drawDescriptionIcon(
                            centerX,
                            iconY,
                        )
                }

                val label =
                    labels[column]
                val baseline =
                    y +
                        rowHeight -
                        6f

                canvas!!.drawText(
                    label,
                    centerX -
                        tableHeaderPaint
                            .measureText(
                                label
                            ) / 2f,
                    baseline,
                    tableHeaderPaint,
                )

                x += cellWidth
            }

            y += rowHeight
        }

        fun drawPinContinuationHeader(
            widths: FloatArray,
        ) {
            drawPinIdentityBlock(
                continuation = true,
            )
            drawPinHeaderRow(
                widths,
            )
        }

        fun drawPinBodyRow(
            row: NativeTableRow,
            widths: FloatArray,
        ) {
            val description =
                row.cells
                    .getOrNull(3)
                    .orEmpty()
            val descriptionLines =
                wrap(
                    description,
                    textPaint,
                    max(
                        12f,
                        widths[3] -
                            CELL_PADDING * 2f,
                    ),
                )
            val lineHeight =
                textPaint.textSize +
                    LINE_GAP
            val rowHeight =
                max(
                    textPaint.textSize +
                        CELL_PADDING * 2f +
                        LINE_GAP,
                    descriptionLines.size *
                        lineHeight +
                        CELL_PADDING * 2f,
                )

            if (
                y + rowHeight >
                pageHeight - MARGIN
            ) {
                startPage()
                drawPinContinuationHeader(
                    widths,
                )
            }

            var x =
                MARGIN

            for (column in 0..3) {
                val cellWidth =
                    widths[column]

                canvas!!.drawRect(
                    x,
                    y,
                    x + cellWidth,
                    y + rowHeight,
                    linePaint,
                )

                if (column < 3) {
                    val value =
                        row.cells
                            .getOrNull(column)
                            .orEmpty()
                    val baseline =
                        y +
                            (
                                rowHeight -
                                    boldPaint.textSize
                            ) / 2f +
                            boldPaint.textSize *
                                0.78f

                    canvas!!.drawText(
                        value,
                        x +
                            (
                                cellWidth -
                                    boldPaint
                                        .measureText(
                                            value
                                        )
                            ) / 2f,
                        baseline,
                        boldPaint,
                    )
                } else {
                    val blockHeight =
                        descriptionLines.size *
                            lineHeight -
                            LINE_GAP
                    var baseline =
                        y +
                            (
                                rowHeight -
                                    blockHeight
                            ) / 2f +
                            textPaint.textSize

                    for (
                        line in
                        descriptionLines
                    ) {
                        canvas!!.drawText(
                            line,
                            x + CELL_PADDING,
                            baseline,
                            textPaint,
                        )
                        baseline +=
                            lineHeight
                    }
                }

                x += cellWidth
            }

            y += rowHeight
        }

        fun drawPinesDocument(
            pinTable: NativeTableData,
        ) {
            val widths =
                pinColumnWidths(
                    pinTable,
                )
            val bodyRows =
                pinTable.rows.dropWhile {
                    it.header
                }
            val infoLines =
                connectorInfoLines(
                    pinTable,
                )

            startPage()
            drawConnectorCard(
                infoLines,
            )
            drawPinIdentityBlock(
                continuation = false,
            )

            if (
                y + 43f >
                pageHeight - MARGIN
            ) {
                startPage()
                drawPinContinuationHeader(
                    widths,
                )
            } else {
                drawPinHeaderRow(
                    widths,
                )
            }

            for (row in bodyRows) {
                drawPinBodyRow(
                    row,
                    widths,
                )
            }
        }

        fun drawTable(
            table: NativeTableData,
        ) {
            if (table.rows.isEmpty()) {
                return
            }

            val columnCount =
                table.rows.maxOf {
                    it.cells.size
                }
            val fractions =
                NativeTableLayout
                    .columnFractions(
                        table,
                    )
            val width =
                availableWidth()
            val tableHeaderRows =
                table.rows.takeWhile {
                    it.header
                }
            val bodyRows =
                table.rows.drop(
                    tableHeaderRows.size,
                )

            fun layoutRow(
                row: NativeTableRow,
            ): Pair<
                List<List<String>>,
                Float
            > {
                val paint =
                    if (row.header) {
                        tableHeaderPaint
                    } else {
                        textPaint
                    }

                val lineSets =
                    mutableListOf<List<String>>()
                var rowHeight =
                    paint.textSize +
                        CELL_PADDING * 2f +
                        LINE_GAP

                for (
                    column in
                    0 until columnCount
                ) {
                    val value =
                        row.cells
                            .getOrNull(column)
                            .orEmpty()
                    val cellWidth =
                        width *
                            fractions[column]
                    val lines =
                        if (
                            column <
                            columnCount - 1
                        ) {
                            listOf(value)
                        } else {
                            wrap(
                                value,
                                paint,
                                max(
                                    12f,
                                    cellWidth -
                                        CELL_PADDING * 2f,
                                ),
                            )
                        }
                    lineSets.add(lines)

                    val needed =
                        lines.size *
                            (
                                paint.textSize +
                                    LINE_GAP
                            ) +
                            CELL_PADDING * 2f
                    rowHeight =
                        max(
                            rowHeight,
                            needed,
                        )
                }

                return Pair(
                    lineSets,
                    rowHeight,
                )
            }

            fun drawRow(
                row: NativeTableRow,
                lineSets: List<List<String>>,
                rowHeight: Float,
            ) {
                val paint =
                    if (row.header) {
                        tableHeaderPaint
                    } else {
                        textPaint
                    }

                var x = MARGIN

                for (
                    column in
                    0 until columnCount
                ) {
                    val cellWidth =
                        width *
                            fractions[column]
                    val cellPaint =
                        when {
                            row.header ->
                                tableHeaderPaint

                            columnCount == 2 &&
                                column == 0 ->
                                boldPaint

                            else ->
                                paint
                        }

                    canvas!!.drawRect(
                        x,
                        y,
                        x + cellWidth,
                        y + rowHeight,
                        linePaint,
                    )

                    val lineHeight =
                        cellPaint.textSize +
                            LINE_GAP
                    val textBlockHeight =
                        lineSets[column].size *
                            lineHeight -
                            LINE_GAP
                    var baseline =
                        y +
                            (
                                rowHeight -
                                    textBlockHeight
                            ) /
                            2f +
                            cellPaint.textSize

                    for (
                        line in
                        lineSets[column]
                    ) {
                        val textX =
                            if (
                                row.header ||
                                column <
                                columnCount - 1
                            ) {
                                x +
                                    (
                                        cellWidth -
                                            cellPaint.measureText(
                                                line,
                                            )
                                    ) /
                                    2f
                            } else {
                                x +
                                    CELL_PADDING
                            }

                        canvas!!.drawText(
                            line,
                            textX,
                            baseline,
                            cellPaint,
                        )
                        baseline +=
                            lineHeight
                    }

                    x += cellWidth
                }

                y += rowHeight
            }

            fun drawTableHeaderRows() {
                for (
                    headerRow in
                    tableHeaderRows
                ) {
                    val layout =
                        layoutRow(
                            headerRow,
                        )
                    val lineSets =
                        layout.first
                    val rowHeight =
                        layout.second

                    if (
                        y + rowHeight >
                        pageHeight - MARGIN
                    ) {
                        startPage()
                        drawDocumentHeader()
                    }

                    drawRow(
                        row = headerRow,
                        lineSets = lineSets,
                        rowHeight = rowHeight,
                    )
                }
            }

            if (tableHeaderRows.isNotEmpty()) {
                drawTableHeaderRows()
            }

            for (row in bodyRows) {
                val layout =
                    layoutRow(
                        row,
                    )
                val lineSets =
                    layout.first
                val rowHeight =
                    layout.second

                if (
                    y + rowHeight >
                    pageHeight - MARGIN
                ) {
                    startPage()
                    drawDocumentHeader()

                    if (
                        tableHeaderRows
                            .isNotEmpty()
                    ) {
                        drawTableHeaderRows()
                    }
                }

                drawRow(
                    row = row,
                    lineSets = lineSets,
                    rowHeight = rowHeight,
                )
            }

            y += 8f
        }

        val pinTable =
            if (isPinesExport) {
                pinTableOf()
            } else {
                null
            }

        if (pinTable != null) {
            drawPinesDocument(
                pinTable,
            )
        } else {
            startPage()
            drawDocumentHeader()

            for (table in data.tables) {
                drawTable(table)
            }
        }

        finishPage()

        context
            .contentResolver
            .openOutputStream(uri)
            ?.use {
                document.writeTo(it)
            }
            ?: error(
                "Не вдалося створити PDF файл."
            )

        document.close()
    }
}
