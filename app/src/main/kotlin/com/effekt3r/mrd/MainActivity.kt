package com.effekt3r.mrd

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.core.util.Consumer
import androidx.navigation.compose.rememberNavController
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme
import com.effekt3r.mrd.navigation.MrdNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            // On a cold start, NavHost handles the launch intent's deep link itself. On a warm start the activity is
            // singleTop, so the deep link arrives here; handleDeepLink() then restarts the task for an intent that
            // asks for a new one, as one from outside the app does, and builds Browse under Details (spec §4.5).
            DisposableEffect(navController) {
                val listener = Consumer<Intent> { navController.handleDeepLink(it) }
                addOnNewIntentListener(listener)
                onDispose { removeOnNewIntentListener(listener) }
            }
            // MrdTheme and enableEdgeToEdge() both follow the system's dark mode, so the status-bar icons are dark on
            // the light theme and light on the dark one.
            MrdTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MrdNavHost(navController = navController)
                }
            }
        }
    }
}
