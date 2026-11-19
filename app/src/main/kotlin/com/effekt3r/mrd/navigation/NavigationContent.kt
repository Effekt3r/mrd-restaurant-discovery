package com.effekt3r.mrd.navigation

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

/**
 * Sets the activity's [content] with a NavController that also follows the deep links the activity receives as new
 * intents (spec §4.5). Call it from onCreate.
 *
 * On a cold start, NavHost handles the launch intent's deep link itself. MainActivity is singleTop, so a link to a
 * task that's still there arrives through onNewIntent instead. That happens while the app runs, and also after Android
 * has killed the app's process in the background: Android then recreates the activity from its saved state and
 * delivers the link before onResume, so before anything is composed. The listener is therefore registered here,
 * before any new intent can arrive, and the link waits until the NavController exists. handleDeepLink() then restarts
 * the task for an intent that asks for a new one, as a link from outside the app does, and builds Browse under
 * Details.
 */
internal fun ComponentActivity.setNavigationContent(content: @Composable (NavHostController) -> Unit) {
    var pendingDeepLink by mutableStateOf<Intent?>(null)
    addOnNewIntentListener { pendingDeepLink = it }
    setContent {
        val navController = rememberNavController()
        LaunchedEffect(navController, pendingDeepLink) {
            pendingDeepLink?.let { deepLink ->
                navController.handleDeepLink(deepLink)
                pendingDeepLink = null
            }
        }
        content(navController)
    }
}
