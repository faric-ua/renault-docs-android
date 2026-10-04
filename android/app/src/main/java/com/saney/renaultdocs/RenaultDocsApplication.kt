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
                Ui.applyOrientationSystemBars(
                    it,
                )
            }
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {
        Ui.applyOrientationSystemBars(
            activity,
        )
    }

    override fun onActivityResumed(
        activity: Activity,
    ) {
        resumedActivity =
            WeakReference(
                activity,
            )

        Ui.applyOrientationSystemBars(
            activity,
        )
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
