package com.saney.renaultdocs

import kotlin.random.Random

/**
 * Background emblem behind the project tiles on the home screen (TODO №27,
 * owner 2026-10-18 00:56): one of the owner's four emblems, a different one on
 * every app launch.
 *
 * Picked once per process — so it stays the same while the app is open
 * (rotation, returning from a project) and changes on the next launch.
 * The pictures are the owner's `docs/brand/emblems/emblem-1..4.png`, scaled to
 * 720 px WebP (the originals are 1254 px and 1.5–2 MB each — too big for the APK).
 */
object HomeEmblem {
    val drawables: IntArray = intArrayOf(
        R.drawable.home_emblem_1,
        R.drawable.home_emblem_2,
        R.drawable.home_emblem_3,
        R.drawable.home_emblem_4,
    )

    /** Muted so the tiles stay easy to read. */
    const val ALPHA: Float = 0.14f

    val current: Int by lazy { drawables[pick(Random.Default, drawables.size)] }

    fun pick(random: Random, count: Int): Int = random.nextInt(count)
}
