package com.saney.renaultdocs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Android invokes this on user dismissal/clear. An app update by itself is not
 * a user request to erase the locally preserved completed-results journal.
 */
class CompletedNotificationDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            CompletedNotificationHistory.ACTION_DISMISS_RESULT,
            CompletedNotificationHistory.ACTION_DISMISS_GROUP ->
                CompletedNotificationHistory.markDismissed(context, intent)
        }
    }
}
