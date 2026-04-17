package com.lifetracker.mobile.core.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

class TimeZoneChangedReceiver(
    private val heroTimeZoneSyncManager: HeroTimeZoneSyncManager,
) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_TIMEZONE_CHANGED) {
            Timber.d("System timezone changed, syncing hero timezone")
            heroTimeZoneSyncManager.syncIfNeededAsync()
        }
    }
}
