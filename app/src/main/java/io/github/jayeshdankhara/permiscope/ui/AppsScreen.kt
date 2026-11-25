package io.github.jayeshdankhara.permiscope.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.jayeshdankhara.permiscope.data.AppEntry
import io.github.jayeshdankhara.permiscope.data.ScanResult
import io.github.jayeshdankhara.permiscope.data.Sensitivity

private val chipFilters: List<Pair<String, AppFilter>> = listOf(
    "All" to AppFilter.All,
    "Microphone" to AppFilter.Has(Sensitivity.MICROPHONE),
    "Camera" to AppFilter.Has(Sensitivity.CAMERA),
    "Location" to AppFilter.Has(Sensitivity.LOCATION),
    "Background location" to AppFilter.Has(Sensitivity.BACKGROUND_LOCATION),
    "SMS" to AppFilter.Has(Sensitivity.SMS),
    "Special access" to AppFilter.AnySpecial,
    "Unused" to AppFilter.Unused,
)

@Composable
fun AppsScreen(
    scan: ScanResult?,
    filter: AppFilter,
    onFilter: (AppFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (scan == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    var query by rememberSaveable { mutableStateOf("") }
    var showSystem by rememberSaveable { mutableStateOf(false) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }

    val visible = scan.apps.filter { app ->
        (showSystem || !app.isSystem) &&
            (query.isBlank() || app.label.contains(query, ignoreCase = true) ||
                app.packageName.contains(query, ignoreCase = true)) &&
            when (filter) {
                AppFilter.All -> true
                is AppFilter.Has -> filter.sensitivity in app.access
                AppFilter.AnySpecial -> app.access.any { it.special }
                AppFilter.Unused -> app.isUnusedWithAccess()
            }
    }

    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search apps") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            chipFilters.forEach { (label, f) ->
                FilterChip(selected = filter == f, onClick = { onFilter(f) }, label = { Text(label) })
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${visible.size} apps",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            Text("Show system apps", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(8.dp))
            Switch(checked = showSystem, onCheckedChange = { showSystem = it })
        }
        HorizontalDivider()
        LazyColumn(Modifier.fillMaxSize()) {
            items(visible, key = { it.packageName }) { app ->
                AppRow(app) { selected = app.packageName }
                HorizontalDivider()
            }
        }
    }

    scan.apps.firstOrNull { it.packageName == selected }?.let { app ->
        AppDetailDialog(app) { selected = null }
    }
}

@Composable
private fun AppRow(app: AppEntry, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                app.access.sortedByDescending { it.weight }.take(6).forEach {
                    Icon(
                        it.icon(),
                        contentDescription = it.title,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (app.access.size > 6) {
                    Text("+${app.access.size - 6}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        if (app.access.isNotEmpty()) RiskBadge(app)
    }
}

@Composable
private fun AppIcon(app: AppEntry) {
    val bmp = app.icon
    if (bmp != null) {
        Image(bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.size(40.dp))
    } else {
        Box(Modifier.size(40.dp))
    }
}

@Composable
private fun RiskBadge(app: AppEntry) {
    Surface(color = app.level.color(), shape = RoundedCornerShape(12.dp)) {
        Text(
            app.level.label(),
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun AppDetailDialog(app: AppEntry, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { AppIcon(app) },
        title = { Text(app.label) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    val used = app.lastUsed?.let {
                        "Last opened " + DateUtils.getRelativeTimeSpanString(it).toString().lowercase()
                    } ?: "Last opened: unknown"
                    Text(
                        "${app.packageName}\n$used",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (app.access.isEmpty()) {
                    item { Text("This app has no sensitive permissions or special access.") }
                }
                items(app.access.sortedByDescending { it.weight }) { s ->
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(s.icon(), contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(s.title, style = MaterialTheme.typography.titleSmall)
                            Text(s.explanation(), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}")),
                )
            }) { Text("Manage permissions") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
