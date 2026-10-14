package com.saney.renaultdocs

/**
 * TODO №1 (owner 2026-10-14 20:15, plan agreed 2026-10-15 01:55): installed volumes
 * may live in the phone (app's private storage) or on the memory card (the app's own
 * folder on the card, `Android/data/<package>/files/rdpkg`). A volume is opened by
 * its package id, never by a path, so nothing else needs to know where it lies.
 *
 * Plain decisions only (no Android types) — checked by JVM tests.
 */
enum class VolumePlace {
    PHONE,
    CARD,
}

enum class VolumeState {
    /** Installed in the phone memory. */
    PHONE,

    /** Installed on the memory card, card is in. */
    CARD,

    /** Was installed on the card, but the card is not in now. Nothing is deleted. */
    CARD_MISSING,

    /** Not installed anywhere we know of. */
    NOT_INSTALLED,
}

object VolumeStoragePolicy {
    const val CARD_MISSING_MESSAGE =
        "Цей том лежить на карті пам'яті, а карти зараз немає. Встав карту — і том відкриється. Нічого не видалено."

    const val CARD_NOT_AVAILABLE_FOR_INSTALL =
        "Карту пам'яті не знайдено — том не додано. Встав карту або вибери «Телефон» у Налаштуваннях → «Де зберігати нові томи»."

    fun state(
        onPhone: Boolean,
        onCard: Boolean,
        rememberedOnCard: Boolean,
        cardPresent: Boolean,
    ): VolumeState =
        when {
            onPhone -> VolumeState.PHONE
            onCard -> VolumeState.CARD
            rememberedOnCard && !cardPresent -> VolumeState.CARD_MISSING
            else -> VolumeState.NOT_INSTALLED
        }

    /** Stored value of the setting; anything unknown means the phone (the old behaviour). */
    fun parsePlace(raw: String?): VolumePlace =
        if (raw == VolumePlace.CARD.name) VolumePlace.CARD else VolumePlace.PHONE

    fun placeLabel(place: VolumePlace): String =
        when (place) {
            VolumePlace.PHONE -> "Телефон"
            VolumePlace.CARD -> "Карта пам'яті"
        }

    /** «9,0 ГБ» / «850 МБ» — short, Ukrainian decimal comma. */
    fun sizeLabel(bytes: Long): String {
        val safe = bytes.coerceAtLeast(0L)
        val gb = safe / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 1.0) {
            String.format(java.util.Locale.ROOT, "%.1f", gb).replace('.', ',') + " ГБ"
        } else {
            (safe / (1024L * 1024L)).toString() + " МБ"
        }
    }

    /** One line for settings and the import screen. */
    fun choiceLine(
        place: VolumePlace,
        cardPresent: Boolean,
        freeBytes: Long?,
    ): String =
        when {
            place == VolumePlace.CARD && !cardPresent -> "Карта пам'яті — зараз не вставлена"
            freeBytes != null -> placeLabel(place) + " · вільно " + sizeLabel(freeBytes)
            else -> placeLabel(place)
        }
}
