package io.github.jayeshdankhara.permiscope.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.jayeshdankhara.permiscope.data.Prefs

/** Restarts the monitor after a reboot or an app update, if the user turned it on. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val relevant = intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        if (relevant && Prefs.monitorEnabled(context)) {
            runCatching { MonitorService.start(context) }
        }
    }
}
