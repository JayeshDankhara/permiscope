package io.github.jayeshdankhara.permiscope.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.jayeshdankhara.permiscope.data.AppScanner
import io.github.jayeshdankhara.permiscope.data.ScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Filters the Overview cards hand to the Apps tab. */
sealed interface AppFilter {
    data object All : AppFilter
    data class Has(val sensitivity: io.github.jayeshdankhara.permiscope.data.Sensitivity) : AppFilter
    data object AnySpecial : AppFilter
    data object Unused : AppFilter
}

private enum class Tab(val title: String, val icon: ImageVector) {
    OVERVIEW("Overview", Icons.Filled.Shield),
    APPS("Apps", Icons.Filled.Apps),
    ACTIVITY("Activity", Icons.Filled.History),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermiscopeApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var filter by remember { mutableStateOf<AppFilter>(AppFilter.All) }
    var scan by remember { mutableStateOf<ScanResult?>(null) }

    // Rescan whenever the user comes back, e.g. after revoking a permission in Settings
    LifecycleResumeEffect(Unit) {
        val job = scope.launch {
            scan = withContext(Dispatchers.Default) { AppScanner(context).scan() }
        }
        onPauseOrDispose { job.cancel() }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(Tab.entries[tab].title) }) },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.title) },
                    )
                }
            }
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (Tab.entries[tab]) {
            Tab.OVERVIEW -> OverviewScreen(scan, modifier) { f ->
                filter = f
                tab = Tab.APPS.ordinal
            }
            Tab.APPS -> AppsScreen(scan, filter, { filter = it }, modifier)
            Tab.ACTIVITY -> ActivityScreen(modifier)
        }
    }
}
