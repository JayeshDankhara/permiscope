package io.github.jayeshdankhara.permiscope.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.jayeshdankhara.permiscope.data.RiskLevel
import io.github.jayeshdankhara.permiscope.data.ScanResult
import io.github.jayeshdankhara.permiscope.data.Sensitivity

private val headline = listOf(
    Sensitivity.MICROPHONE,
    Sensitivity.CAMERA,
    Sensitivity.LOCATION,
    Sensitivity.BACKGROUND_LOCATION,
    Sensitivity.SMS,
    Sensitivity.CONTACTS,
)

@Composable
fun OverviewScreen(scan: ScanResult?, modifier: Modifier = Modifier, onFilter: (AppFilter) -> Unit) {
    if (scan == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val context = LocalContext.current
    // System apps are hidden by default, as in the Apps tab
    val apps = scan.apps.filter { !it.isSystem }
    val special = apps.filter { a -> a.access.any { it.special } }
    val unused = apps.filter { it.isUnusedWithAccess() }
    val high = apps.count { it.level == RiskLevel.HIGH }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text(
                    "${apps.size} apps installed",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (high == 0) "No app has a high exposure score."
                    else "$high ${if (high == 1) "app has" else "apps have"} a high exposure score.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (!scan.hasUsageAccess) {
            item {
                SetupCard(
                    title = "Allow usage access",
                    body = "Needed to spot apps you no longer use and to name the app using your camera or " +
                        "microphone. This information never leaves your phone.",
                    action = "Open settings",
                ) { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
            }
        }

        item { SectionTitle("Who can access") }
        items(headline) { s ->
            val count = apps.count { s in it.access }
            SummaryRow(s.icon(), s.title, count) { onFilter(AppFilter.Has(s)) }
        }

        item { SectionTitle("Powerful special access") }
        item {
            SummaryRow(
                Icons.Filled.Warning,
                "Accessibility, device admin, notification access and more",
                special.size,
                highlight = special.isNotEmpty(),
            ) { onFilter(AppFilter.AnySpecial) }
        }

        if (scan.hasUsageAccess) {
            item { SectionTitle("Clean-up suggestions") }
            item {
                SummaryRow(
                    Icons.Filled.Bedtime,
                    "Unused for 30+ days but still have sensitive access",
                    unused.size,
                    highlight = unused.isNotEmpty(),
                ) { onFilter(AppFilter.Unused) }
            }
        }

        item {
            Text(
                "Permiscope has no internet permission. Nothing it sees can leave your phone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun SummaryRow(
    icon: ImageVector,
    title: String,
    count: Int,
    highlight: Boolean = false,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = if (highlight) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun SetupCard(title: String, body: String, action: String, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onClick) { Text(action) }
        }
    }
}
