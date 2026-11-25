package io.github.jayeshdankhara.permiscope.data

import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import java.util.concurrent.TimeUnit

data class AppEntry(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val icon: Bitmap?,
    val access: Set<Sensitivity>,
    /** Last time the user opened the app, or null if unknown (no usage access). */
    val lastUsed: Long?,
) {
    val score: Int get() = access.sumOf { it.weight }

    val level: RiskLevel
        get() = when {
            score >= 10 -> RiskLevel.HIGH
            score >= 5 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

    /** Holds sensitive access but has not been opened for [UNUSED_DAYS] days. */
    fun isUnusedWithAccess(now: Long = System.currentTimeMillis()): Boolean {
        val used = lastUsed ?: return false
        return score >= 3 && now - used > TimeUnit.DAYS.toMillis(UNUSED_DAYS)
    }

    companion object {
        const val UNUSED_DAYS = 30L
    }
}

enum class RiskLevel { LOW, MEDIUM, HIGH }

data class ScanResult(
    val apps: List<AppEntry>,
    val hasUsageAccess: Boolean,
    val scannedAt: Long = System.currentTimeMillis(),
)

/** Reads installed apps and what each can access. Everything stays on the device. */
class AppScanner(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    fun scan(): ScanResult {
        val usageAccess = hasUsageAccess(context)
        val lastUsed = if (usageAccess) lastUsedTimes() else emptyMap()
        val accessibility = enabledAccessibilityPackages()
        val notificationListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
        val deviceAdmins = activeDeviceAdmins()
        val iconSize = (48 * context.resources.displayMetrics.density).toInt()

        val apps = installedPackages()
            .filter { it.packageName != context.packageName }
            .mapNotNull { info ->
                val appInfo = info.applicationInfo ?: return@mapNotNull null
                val access = mutableSetOf<Sensitivity>()
                access += grantedGroups(info)
                if (info.packageName in accessibility) access += Sensitivity.ACCESSIBILITY
                if (info.packageName in notificationListeners) access += Sensitivity.NOTIFICATION_ACCESS
                if (info.packageName in deviceAdmins) access += Sensitivity.DEVICE_ADMIN
                if (opAllowed(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, appInfo, info, "android.permission.SYSTEM_ALERT_WINDOW")) {
                    access += Sensitivity.DRAW_OVER_APPS
                }
                if (opAllowed(OPSTR_REQUEST_INSTALL_PACKAGES, appInfo, info, "android.permission.REQUEST_INSTALL_PACKAGES")) {
                    access += Sensitivity.INSTALL_APPS
                }
                if (opAllowed(AppOpsManager.OPSTR_GET_USAGE_STATS, appInfo, info, "android.permission.PACKAGE_USAGE_STATS")) {
                    access += Sensitivity.USAGE_ACCESS
                }

                AppEntry(
                    packageName = info.packageName,
                    label = appInfo.loadLabel(pm).toString(),
                    isSystem = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                    icon = runCatching { appInfo.loadIcon(pm).toBitmap(iconSize, iconSize) }.getOrNull(),
                    access = access,
                    lastUsed = lastUsed[info.packageName],
                )
            }
            .sortedWith(compareByDescending<AppEntry> { it.score }.thenBy { it.label.lowercase() })

        return ScanResult(apps, usageAccess)
    }

    private fun installedPackages(): List<PackageInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        }

    /** Runtime permission groups the user has actually granted (not just requested). */
    private fun grantedGroups(info: PackageInfo): Set<Sensitivity> {
        val requested = info.requestedPermissions ?: return emptySet()
        val flags = info.requestedPermissionsFlags ?: return emptySet()
        val result = mutableSetOf<Sensitivity>()
        for (i in requested.indices) {
            val granted = i < flags.size && flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0
            if (granted) Sensitivity.byPermission[requested[i]]?.let { result += it }
        }
        return result
    }

    /**
     * True when an app requested [permission] and the matching app-op is allowed.
     * Some devices refuse to report app-ops for other apps; then we report nothing
     * rather than guessing.
     */
    private fun opAllowed(op: String, appInfo: ApplicationInfo, info: PackageInfo, permission: String): Boolean {
        if (info.requestedPermissions?.contains(permission) != true) return false
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        return try {
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(op, appInfo.uid, info.packageName)
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(op, appInfo.uid, info.packageName)
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    private fun enabledAccessibilityPackages(): Set<String> {
        val raw = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return emptySet()
        return raw.split(':').mapNotNull { it.substringBefore('/').takeIf(String::isNotBlank) }.toSet()
    }

    private fun activeDeviceAdmins(): Set<String> {
        val dpm = context.getSystemService(DevicePolicyManager::class.java) ?: return emptySet()
        return dpm.activeAdmins?.map { it.packageName }?.toSet() ?: emptySet()
    }

    private fun lastUsedTimes(): Map<String, Long> {
        val usm = context.getSystemService(UsageStatsManager::class.java) ?: return emptyMap()
        val end = System.currentTimeMillis()
        val start = end - TimeUnit.DAYS.toMillis(365)
        return usm.queryAndAggregateUsageStats(start, end)
            .mapValues { it.value.lastTimeUsed }
            .filterValues { it > 0 }
    }

    companion object {
        // AppOpsManager.OPSTR_REQUEST_INSTALL_PACKAGES is hidden from the public SDK.
        private const val OPSTR_REQUEST_INSTALL_PACKAGES = "android:request_install_packages"

        fun hasUsageAccess(context: Context): Boolean {
            val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            }
            return mode == AppOpsManager.MODE_ALLOWED
        }
    }
}
