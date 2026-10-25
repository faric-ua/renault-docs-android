package com.saney.renaultdocs

/**
 * TODO №37 (faric 2026-10-25, Megane II · 119 · «Опис контактів»): таблиця з 5 колонками
 * не влазила — опис був «середньою» колонкою в один рядок (ширший за екран, обрізаний),
 * а остання колонка діставала ~0 px, і її текст ламався по літері → дуже високі рядки.
 *
 * Тепер ширини рахуються тут (чиста функція, JVM-тест):
 * - кожна колонка, крім останньої, — за найдовшим текстом, але не ширша за [maxMiddle];
 *   довша — переноситься на кілька рядків (wrap);
 * - остання колонка — не вужча за [minLast], забирає решту місця й переноситься;
 * - якщо сума ширша за екран — таблиця прокручується вліво-вправо (HorizontalScrollView).
 */
object TableColumnPlan {
    data class Plan(
        val widths: IntArray,
        val wrap: BooleanArray,
    ) {
        val total: Int
            get() = widths.sum()
    }

    fun plan(
        natural: IntArray,
        available: Int,
        minLast: Int,
        maxMiddle: Int,
    ): Plan {
        val count = natural.size.coerceAtLeast(1)
        val widths = IntArray(count)
        val wrap = BooleanArray(count)
        val middleCap = maxOf(maxMiddle, 1)

        for (column in 0 until count - 1) {
            val width = natural[column]
            if (width > middleCap) {
                widths[column] = middleCap
                wrap[column] = true
            } else {
                widths[column] = width
            }
        }

        val used = widths.take(count - 1).sum()
        widths[count - 1] = maxOf(minLast, available - used)
        wrap[count - 1] = true

        return Plan(widths, wrap)
    }
}
