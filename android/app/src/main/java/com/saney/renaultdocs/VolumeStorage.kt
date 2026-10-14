package com.saney.renaultdocs

import android.content.Context
import android.os.Environment
import java.io.File

/**
 * TODO №1 (2026-10-15): where installed volumes lie — the phone or the memory card.
 * Decisions are in [VolumeStoragePolicy]; this object only looks at the folders.
 *
 * - Phone: `noBackupFilesDir/rdpkg/<id>` (as before).
 * - Card: the app's own folder on the removable card, `Android/data/<package>/files/rdpkg/<id>`.
 *   No permission is needed and other apps cannot read it.
 *
 * Volumes put on the card are remembered by id, so when the card is out the app says
 * «вставте карту» instead of treating the volume as broken. Nothing is ever deleted here.
 */
object VolumeStorage {
    private const val DIR = "rdpkg"
    private const val PREFS = "volume_storage"
    private const val KEY_TARGET = "target_place"
    private const val KEY_ON_CARD = "on_card_ids"

    fun phoneRoot(context: Context): File = File(context.applicationContext.noBackupFilesDir, DIR)

    /** The app's folder on a mounted removable card, or null when there is no card. */
    fun cardRoot(context: Context): File? {
        val dirs = runCatching { context.applicationContext.getExternalFilesDirs(null) }.getOrNull() ?: return null
        return dirs
            .filterNotNull()
            .firstOrNull { dir ->
                runCatching {
                    Environment.isExternalStorageRemovable(dir) &&
                        Environment.getExternalStorageState(dir) == Environment.MEDIA_MOUNTED
                }.getOrDefault(false)
            }?.let { File(it, DIR) }
    }

    fun cardPresent(context: Context): Boolean = cardRoot(context) != null

    fun root(
        context: Context,
        place: VolumePlace,
    ): File? =
        when (place) {
            VolumePlace.PHONE -> phoneRoot(context)
            VolumePlace.CARD -> cardRoot(context)
        }

    /** Where the volume is now, or null. The phone is checked first (as before). */
    fun locate(
        context: Context,
        packageId: String,
    ): File? {
        val phone = File(phoneRoot(context), packageId)
        if (phone.isDirectory) return phone
        val card = cardRoot(context)?.let { File(it, packageId) }
        return card?.takeIf { it.isDirectory }
    }

    fun placeOf(
        context: Context,
        packageId: String,
    ): VolumePlace? {
        val found = locate(context, packageId) ?: return null
        return if (found.parentFile == phoneRoot(context)) VolumePlace.PHONE else VolumePlace.CARD
    }

    fun state(
        context: Context,
        packageId: String,
    ): VolumeState {
        val cardRoot = cardRoot(context)
        return VolumeStoragePolicy.state(
            onPhone = File(phoneRoot(context), packageId).isDirectory,
            onCard = cardRoot?.let { File(it, packageId).isDirectory } == true,
            rememberedOnCard = rememberedOnCard(context, packageId),
            cardPresent = cardRoot != null,
        )
    }

    fun chosenPlace(context: Context): VolumePlace =
        VolumeStoragePolicy.parsePlace(prefs(context).getString(KEY_TARGET, null))

    fun setChosenPlace(
        context: Context,
        place: VolumePlace,
    ) {
        prefs(context).edit().putString(KEY_TARGET, place.name).apply()
    }

    fun freeBytes(
        context: Context,
        place: VolumePlace,
    ): Long? {
        val root = root(context, place) ?: return null
        return runCatching {
            root.mkdirs()
            root.usableSpace
        }.getOrNull()
    }

    /** One line for the settings row and the import screen. */
    fun choiceLine(context: Context): String {
        val place = chosenPlace(context)
        return VolumeStoragePolicy.choiceLine(
            place = place,
            cardPresent = cardPresent(context),
            freeBytes = freeBytes(context, place),
        )
    }

    fun rememberPlace(
        context: Context,
        packageId: String,
        place: VolumePlace,
    ) {
        val prefs = prefs(context)
        val ids = prefs.getStringSet(KEY_ON_CARD, emptySet()).orEmpty().toMutableSet()
        val changed = if (place == VolumePlace.CARD) ids.add(packageId) else ids.remove(packageId)
        if (changed) prefs.edit().putStringSet(KEY_ON_CARD, ids).apply()
    }

    private fun rememberedOnCard(
        context: Context,
        packageId: String,
    ): Boolean = prefs(context).getStringSet(KEY_ON_CARD, emptySet()).orEmpty().contains(packageId)

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
