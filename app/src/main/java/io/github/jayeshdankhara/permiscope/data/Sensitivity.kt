package io.github.jayeshdankhara.permiscope.data

/**
 * What an app can reach. Each entry is either a runtime permission group the
 * user granted, or a "special access" the user switched on in system settings.
 * [weight] feeds the per-app exposure score.
 */
enum class Sensitivity(
    val title: String,
    val weight: Int,
    val special: Boolean = false,
    val permissions: Set<String> = emptySet(),
) {
    MICROPHONE("Microphone", 3, permissions = setOf("android.permission.RECORD_AUDIO")),
    CAMERA("Camera", 3, permissions = setOf("android.permission.CAMERA")),
    LOCATION(
        "Location", 2,
        permissions = setOf(
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
        ),
    ),
    BACKGROUND_LOCATION(
        "Location all the time", 4,
        permissions = setOf("android.permission.ACCESS_BACKGROUND_LOCATION"),
    ),
    SMS(
        "SMS", 4,
        permissions = setOf(
            "android.permission.READ_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.SEND_SMS",
            "android.permission.RECEIVE_MMS",
        ),
    ),
    CALL_LOG(
        "Call logs", 3,
        permissions = setOf(
            "android.permission.READ_CALL_LOG",
            "android.permission.WRITE_CALL_LOG",
            "android.permission.PROCESS_OUTGOING_CALLS",
        ),
    ),
    CONTACTS(
        "Contacts", 2,
        permissions = setOf(
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS",
            "android.permission.GET_ACCOUNTS",
        ),
    ),
    PHONE(
        "Phone", 1,
        permissions = setOf(
            "android.permission.READ_PHONE_STATE",
            "android.permission.READ_PHONE_NUMBERS",
            "android.permission.CALL_PHONE",
            "android.permission.ANSWER_PHONE_CALLS",
        ),
    ),
    CALENDAR(
        "Calendar", 1,
        permissions = setOf(
            "android.permission.READ_CALENDAR",
            "android.permission.WRITE_CALENDAR",
        ),
    ),
    BODY_SENSORS(
        "Body sensors", 2,
        permissions = setOf(
            "android.permission.BODY_SENSORS",
            "android.permission.BODY_SENSORS_BACKGROUND",
            "android.permission.ACTIVITY_RECOGNITION",
        ),
    ),
    PHOTOS_FILES(
        "Photos & files", 1,
        permissions = setOf(
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.READ_MEDIA_VIDEO",
            "android.permission.READ_MEDIA_AUDIO",
        ),
    ),
    NEARBY_DEVICES(
        "Nearby devices", 1,
        permissions = setOf(
            "android.permission.BLUETOOTH_SCAN",
            "android.permission.BLUETOOTH_CONNECT",
            "android.permission.NEARBY_WIFI_DEVICES",
        ),
    ),

    // Special access, switched on in system settings rather than granted at runtime
    ACCESSIBILITY("Accessibility service", 5, special = true),
    DEVICE_ADMIN("Device admin", 5, special = true),
    NOTIFICATION_ACCESS("Reads all notifications", 4, special = true),
    DRAW_OVER_APPS("Draws over other apps", 3, special = true),
    INSTALL_APPS("Installs other apps", 2, special = true),
    USAGE_ACCESS("Sees which apps you use", 2, special = true),
    ;

    companion object {
        val runtime = entries.filter { !it.special }
        val specialAccess = entries.filter { it.special }

        /** Permission name -> group, for the runtime groups. */
        val byPermission: Map<String, Sensitivity> =
            runtime.flatMap { g -> g.permissions.map { it to g } }.toMap()
    }
}
