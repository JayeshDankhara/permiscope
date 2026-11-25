package io.github.jayeshdankhara.permiscope

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.jayeshdankhara.permiscope.data.AccessLog
import io.github.jayeshdankhara.permiscope.ui.PermiscopeApp
import io.github.jayeshdankhara.permiscope.ui.PermiscopeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AccessLog.init(this)
        enableEdgeToEdge()
        setContent {
            PermiscopeTheme {
                PermiscopeApp()
            }
        }
    }
}
