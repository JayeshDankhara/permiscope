package io.github.jayeshdankhara.permiscope.data

import android.content.Context
import androidx.core.content.edit

object Prefs {
    private const val NAME = "permiscope"
    private const val MONITOR = "monitor_enabled"
    private const val NOTIFY_ALL = "notify_every_access"

    private fun prefs(context: Context) = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun monitorEnabled(context: Context) = prefs(context).getBoolean(MONITOR, false)
    fun setMonitorEnabled(context: Context, value: Boolean) = prefs(context).edit { putBoolean(MONITOR, value) }

    /** Off: notify only when access starts while the phone is locked or no app is in front. */
    fun notifyEveryAccess(context: Context) = prefs(context).getBoolean(NOTIFY_ALL, false)
    fun setNotifyEveryAccess(context: Context, value: Boolean) = prefs(context).edit { putBoolean(NOTIFY_ALL, value) }
}
