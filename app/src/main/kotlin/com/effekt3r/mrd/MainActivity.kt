package com.effekt3r.mrd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme
import com.effekt3r.mrd.navigation.MrdNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // MrdTheme and enableEdgeToEdge() both follow the system's dark mode, so the status-bar icons are dark on
            // the light theme and light on the dark one.
            MrdTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MrdNavHost()
                }
            }
        }
    }
}
