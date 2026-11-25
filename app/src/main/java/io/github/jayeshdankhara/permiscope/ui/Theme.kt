package io.github.jayeshdankhara.permiscope.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import io.github.jayeshdankhara.permiscope.data.RiskLevel

private val Light = lightColorScheme(
    primary = Color(0xFF0F5C5E),
    secondary = Color(0xFF4A6363),
    tertiary = Color(0xFF8C4A2F),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF7FD3D5),
    secondary = Color(0xFFB0CCCB),
    tertiary = Color(0xFFFFB59A),
)

@Composable
fun PermiscopeTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme: ColorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val ctx = LocalContext.current
        if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
    } else {
        if (dark) Dark else Light
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

/** Fixed colours so risk levels read the same regardless of wallpaper colours. */
fun RiskLevel.color(): Color = when (this) {
    RiskLevel.HIGH -> Color(0xFFC62828)
    RiskLevel.MEDIUM -> Color(0xFFEF8F00)
    RiskLevel.LOW -> Color(0xFF2E7D32)
}

fun RiskLevel.label(): String = when (this) {
    RiskLevel.HIGH -> "High"
    RiskLevel.MEDIUM -> "Medium"
    RiskLevel.LOW -> "Low"
}
