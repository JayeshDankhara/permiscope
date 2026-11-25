package io.github.jayeshdankhara.permiscope.monitor

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.jayeshdankhara.permiscope.MainActivity
import io.github.jayeshdankhara.permiscope.R
import io.github.jayeshdankhara.permiscope.data.AccessLog
import io.github.jayeshdankhara.permiscope.data.AppScanner
import io.github.jayeshdankhara.permiscope.data.Prefs
import io.github.jayeshdankhara.permiscope.data.Sensor

/**
 * Watches for camera and microphone use without holding either permission.
 *
 * Android only says *that* a sensor is in use, not *which* app is using it, so
 * the app in the foreground at that moment is recorded as the likely user. Use
 * that starts while the phone is locked, or with no app in front, is flagged.
 */
class MonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var cameraManager: CameraManager
    private lateinit var audioManager: AudioManager

    private val camerasInUse = mutableSetOf<String>()
    private var cameraEventId: Long? = null
    private var micEventId: Long? = null

    private val cameraCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraUnavailable(cameraId: String) {
            camerasInUse += cameraId
            if (cameraEventId == null) cameraEventId = onAccessStarted(Sensor.CAMERA)
        }

        override fun onCameraAvailable(cameraId: String) {
            camerasInUse -= cameraId
            if (camerasInUse.isEmpty()) {
                cameraEventId?.let(AccessLog::finish)
                cameraEventId = null
            }
        }
    }

    private val micCallback = object : AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>) {
            val active = configs.isNotEmpty()
            if (active && micEventId == null) {
                micEventId = onAccessStarted(Sensor.MICROPHONE)
            } else if (!active) {
                micEventId?.let(AccessLog::finish)
                micEventId = null
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        AccessLog.init(this)
        AccessLog.closeDangling()
        createChannels(this)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, ONGOING_ID, ongoingNotification(), type)

        cameraManager = getSystemService(CameraManager::class.java)
        audioManager = getSystemService(AudioManager::class.java)
        cameraManager.registerAvailabilityCallback(cameraCallback, handler)
        audioManager.registerAudioRecordingCallback(micCallback, handler)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        cameraManager.unregisterAvailabilityCallback(cameraCallback)
        audioManager.unregisterAudioRecordingCallback(micCallback)
        cameraEventId?.let(AccessLog::finish)
        micEventId?.let(AccessLog::finish)
        super.onDestroy()
    }

    private fun onAccessStarted(sensor: Sensor): Long {
        val locked = isLocked()
        val pkg = if (locked) null else foregroundPackage()
        val label = pkg?.let(::labelFor)
        val event = AccessLog.start(sensor, pkg, label, locked)

        val suspicious = locked || pkg == null
        if (suspicious || Prefs.notifyEveryAccess(this)) notifyAccess(sensor, label, locked)
        return event.id
    }

    private fun isLocked(): Boolean {
        val power = getSystemService(PowerManager::class.java)
        val keyguard = getSystemService(KeyguardManager::class.java)
        return !power.isInteractive || keyguard.isKeyguardLocked
    }

    /** The most recent app brought to the foreground in the last two minutes, if any. */
    private fun foregroundPackage(): String? {
        if (!AppScanner.hasUsageAccess(this)) return null
        val usm = getSystemService(UsageStatsManager::class.java) ?: return null
        val end = System.currentTimeMillis()
        val events = usm.queryEvents(end - 120_000, end)
        val e = UsageEvents.Event()
        var last: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            @Suppress("DEPRECATION")
            if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) last = e.packageName
        }
        return last?.takeIf { it != packageName }
    }

    private fun labelFor(pkg: String): String = try {
        val info = packageManager.getApplicationInfo(pkg, 0)
        packageManager.getApplicationLabel(info).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        pkg
    }

    private fun notifyAccess(sensor: Sensor, label: String?, locked: Boolean) {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        val what = if (sensor == Sensor.CAMERA) "Camera" else "Microphone"
        val text = when {
            locked -> "$what started while your phone was locked"
            label == null -> "$what started with no app on screen"
            else -> "$what in use, likely by $label"
        }
        val n = NotificationCompat.Builder(this, ALERT_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(what + " access")
            .setContentText(text)
            .setContentIntent(openApp())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            NotificationManagerCompat.from(this).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), n)
        } catch (_: SecurityException) {
            // Notification permission revoked; the event is still in the log
        }
    }

    private fun ongoingNotification(): Notification =
        NotificationCompat.Builder(this, MONITOR_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Watching camera and microphone")
            .setContentText("Tap to see recent access")
            .setContentIntent(openApp())
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        this, 0,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        private const val ONGOING_ID = 1
        private const val MONITOR_CHANNEL = "monitor"
        private const val ALERT_CHANNEL = "alerts"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, MonitorService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MonitorService::class.java))
        }

        private fun createChannels(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(MONITOR_CHANNEL, "Monitor running", NotificationManager.IMPORTANCE_MIN),
            )
            nm.createNotificationChannel(
                NotificationChannel(ALERT_CHANNEL, "Camera and microphone alerts", NotificationManager.IMPORTANCE_HIGH),
            )
        }
    }
}
