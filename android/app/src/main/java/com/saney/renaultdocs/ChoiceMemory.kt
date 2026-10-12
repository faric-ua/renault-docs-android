package com.saney.renaultdocs

/**
 * v0.5.114 (phone QA 2026-10-12 22:35): what was picked on a screen is kept
 * while the app runs. Before, the green frame lived only as long as the
 * screen itself: leave «Документація тому» and open it again — no frame.
 * Kept in memory only; nothing is written to the phone.
 */
object ChoiceMemory {
    private val maps =
        HashMap<String, HashMap<String, String>>()

    @Synchronized
    fun map(
        owner: String,
        name: String,
    ): HashMap<String, String> =
        maps.getOrPut(
            owner + "\u0001" + name,
        ) {
            HashMap()
        }
}
