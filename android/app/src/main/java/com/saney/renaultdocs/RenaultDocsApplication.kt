package com.saney.renaultdocs

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.content.res.Configuration
import java.lang.ref.WeakReference

class RenaultDocsApplication :
    Application(),
    Application.ActivityLifecycleCallbacks {

    private var resumedActivity:
        WeakReference<Activity>? =
        null

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(
            this,
        )
        // TODO №12: volumes put on the card before v0.5.137 lie in a folder without
        // `.nomedia` — add the marker once, off the main thread (the card can be slow).
        Thread {
            runCatching { VolumeStorage.ensureCardNoMedia(this) }
            // v0.5.147: finish erasing files of volumes deleted earlier (if the app was
            // closed before the background eraser was done). Only the trash folders.
            runCatching { VolumeTrash.startErase(this) }
        }.start()
    }

    override fun onConfigurationChanged(
        newConfig: Configuration,
    ) {
        super.onConfigurationChanged(
            newConfig,
        )

        resumedActivity
            ?.get()
            ?.takeIf {
                !it.isFinishing &&
                    !it.isDestroyed
            }
            ?.let {
                applyBarsToActivity(it)
                DialogUi.reapplyOrientation(it)
            }
    }

    private fun applyBarsToActivity(activity: Activity) {
        // Explicit PDF fullscreen takes precedence over portrait system bars.
        if (activity is ViewerActivity && activity.isPdfFullscreenActive()) return
        Ui.applyOrientationSystemBars(activity)
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {
        applyBarsToActivity(activity)
        DialogUi.reapplyOrientation(activity)
    }

    override fun onActivityResumed(
        activity: Activity,
    ) {
        resumedActivity =
            WeakReference(
                activity,
            )

        // Re-post only recorded, undismissed completed notifications after
        // a package update. Never restart workers, scan ZIPs or change volumes.
        CompletedNotificationHistory.restoreAfterPackageUpdate(activity)
        applyBarsToActivity(activity)
        DialogUi.reapplyOrientation(activity)
    }

    override fun onActivityDestroyed(
        activity: Activity,
    ) {
        if (
            resumedActivity
                ?.get() ===
            activity
        ) {
            resumedActivity =
                null
        }
    }

    override fun onActivityStarted(
        activity: Activity,
    ) = Unit

    override fun onActivityPaused(
        activity: Activity,
    ) = Unit

    override fun onActivityStopped(
        activity: Activity,
    ) = Unit

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) = Unit
}
