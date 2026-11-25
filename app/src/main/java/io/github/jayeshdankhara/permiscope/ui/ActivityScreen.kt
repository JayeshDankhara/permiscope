package io.github.jayeshdankhara.permiscope.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.github.jayeshdankhara.permiscope.data.AccessEvent
import io.github.jayeshdankhara.permiscope.data.AccessLog
import io.github.jayeshdankhara.permiscope.data.Prefs
import io.github.jayeshdankhara.permiscope.data.Sensor
import io.github.jayeshdankhara.permiscope.monitor.MonitorService
import java.util.Date

@Composable
fun ActivityScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val events by AccessLog.events.collectAsState()
    var monitoring by remember { mutableStateOf(Prefs.monitorEnabled(context)) }
    var notifyAll by remember { mutableStateOf(Prefs.notifyEveryAccess(context)) }

    fun setMonitoring(on: Boolean) {
        monitoring = on
        Prefs.setMonitorEnabled(context, on)
        if (on) MonitorService.start(context) else MonitorService.stop(context)
    }

    // Alerts need notification permission on Android 13+; the log works without it
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { setMonitoring(true) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingRow(
                        "Watch camera and microphone",
                        "Logs every use and alerts you when it starts while the phone is locked.",
                        monitoring,
                    ) { on ->
                        val needsPermission = on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                            PackageManager.PERMISSION_GRANTED
                        if (needsPermission) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            setMonitoring(on)
                        }
                    }
                    SettingRow(
                        "Alert on every use",
                        "Off: alert only for use while locked or with no app on screen.",
                        notifyAll,
                        enabled = monitoring,
                    ) {
                        notifyAll = it
                        Prefs.setNotifyEveryAccess(context, it)
                    }
                }
            }
        }

        item {
            Text(
                "Android reports that a sensor is in use, but not which app is using it. The app shown " +
                    "is the one that was on screen at the time.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (events.isEmpty()) {
            item {
                Text(
                    if (monitoring) "No camera or microphone use recorded yet." else "Turn on watching to start a log.",
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        } else {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Last 30 days",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { AccessLog.clear() }) { Text("Clear") }
                }
            }
            items(events, key = { it.id }) { EventRow(it) }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun EventRow(event: AccessEvent) {
    val context = LocalContext.current
    val flagged = event.whileLocked || event.packageName == null
    Card(
        colors = if (flagged) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (event.sensor == Sensor.CAMERA) Icons.Filled.Videocam else Icons.Filled.Mic,
                contentDescription = event.sensor.name.lowercase(),
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        event.whileLocked -> "While phone was locked"
                        event.appLabel != null -> event.appLabel
                        else -> "No app on screen"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                val time = DateFormat.getTimeFormat(context).format(Date(event.start))
                val day = DateUtils.getRelativeTimeSpanString(
                    event.start, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS,
                )
                val duration = event.durationMs?.let { formatDuration(it) } ?: "in use now"
                Text("$day, $time · $duration", style = MaterialTheme.typography.bodySmall)
            }
            if (event.whileLocked) Icon(Icons.Filled.Lock, contentDescription = "Locked", modifier = Modifier.size(18.dp))
        }
    }
}

private fun formatDuration(ms: Long): String {
    val s = ms / 1000
    return when {
        s < 1 -> "under 1 s"
        s < 60 -> "$s s"
        s < 3600 -> "${s / 60} min ${s % 60} s"
        else -> "${s / 3600} h ${(s % 3600) / 60} min"
    }
}
