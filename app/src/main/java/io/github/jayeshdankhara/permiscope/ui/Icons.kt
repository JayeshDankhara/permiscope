package io.github.jayeshdankhara.permiscope.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.jayeshdankhara.permiscope.data.Sensitivity

fun Sensitivity.icon(): ImageVector = when (this) {
    Sensitivity.MICROPHONE -> Icons.Filled.Mic
    Sensitivity.CAMERA -> Icons.Filled.Videocam
    Sensitivity.LOCATION -> Icons.Filled.LocationOn
    Sensitivity.BACKGROUND_LOCATION -> Icons.Filled.MyLocation
    Sensitivity.SMS -> Icons.Filled.Sms
    Sensitivity.CALL_LOG -> Icons.Filled.Call
    Sensitivity.CONTACTS -> Icons.Filled.Contacts
    Sensitivity.PHONE -> Icons.Filled.Phone
    Sensitivity.CALENDAR -> Icons.Filled.CalendarMonth
    Sensitivity.BODY_SENSORS -> Icons.Filled.Sensors
    Sensitivity.PHOTOS_FILES -> Icons.Filled.Photo
    Sensitivity.NEARBY_DEVICES -> Icons.Filled.Bluetooth
    Sensitivity.ACCESSIBILITY -> Icons.Filled.Accessibility
    Sensitivity.DEVICE_ADMIN -> Icons.Filled.AdminPanelSettings
    Sensitivity.NOTIFICATION_ACCESS -> Icons.Filled.Notifications
    Sensitivity.DRAW_OVER_APPS -> Icons.Filled.Layers
    Sensitivity.INSTALL_APPS -> Icons.Filled.Download
    Sensitivity.USAGE_ACCESS -> Icons.Filled.QueryStats
}

/** One-line explanation shown in the app detail dialog. */
fun Sensitivity.explanation(): String = when (this) {
    Sensitivity.MICROPHONE -> "Can record audio while the app is in use"
    Sensitivity.CAMERA -> "Can take photos and video while the app is in use"
    Sensitivity.LOCATION -> "Knows where you are while the app is in use"
    Sensitivity.BACKGROUND_LOCATION -> "Knows where you are even when the app is closed"
    Sensitivity.SMS -> "Can read or send text messages, including one-time codes"
    Sensitivity.CALL_LOG -> "Can read who you called and when"
    Sensitivity.CONTACTS -> "Can read your contacts and accounts"
    Sensitivity.PHONE -> "Can see your phone number and call status, or make calls"
    Sensitivity.CALENDAR -> "Can read or change your calendar"
    Sensitivity.BODY_SENSORS -> "Can read heart rate, steps or physical activity"
    Sensitivity.PHOTOS_FILES -> "Can read your photos, videos, music or files"
    Sensitivity.NEARBY_DEVICES -> "Can find and connect to nearby devices"
    Sensitivity.ACCESSIBILITY -> "Can read everything on screen and tap for you. Malware often abuses this"
    Sensitivity.DEVICE_ADMIN -> "Can lock or wipe the phone and is harder to uninstall"
    Sensitivity.NOTIFICATION_ACCESS -> "Can read every notification, including messages and codes"
    Sensitivity.DRAW_OVER_APPS -> "Can show windows on top of other apps, a common phishing trick"
    Sensitivity.INSTALL_APPS -> "Can install other apps from outside the Play Store"
    Sensitivity.USAGE_ACCESS -> "Can see which apps you use and for how long"
}
