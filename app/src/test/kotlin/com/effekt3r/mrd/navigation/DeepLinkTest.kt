package com.effekt3r.mrd.navigation

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.createGraph
import androidx.navigation.testing.TestNavHostController
import androidx.navigation.toRoute
import androidx.test.core.app.ApplicationProvider
import com.effekt3r.mrd.feature.browse.navigation.BrowseRoute
import com.effekt3r.mrd.feature.details.navigation.RestaurantRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * mrd://restaurant/{id} against the app's real graph (spec §4.5, AC-12). The destinations are never composed, so no
 * ViewModel or Hilt is needed: this checks what the back stack becomes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class DeepLinkTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val navController = TestNavHostController(context).apply {
        navigatorProvider.addNavigator(ComposeNavigator())
        graph = createGraph(startDestination = BrowseRoute) { mrdDestinations(this@apply) }
    }

    private fun deepLink(flags: Int) = Intent(Intent.ACTION_VIEW, Uri.parse("mrd://restaurant/r-1001")).addFlags(flags)

    @Test
    fun theDeepLinkOpensDetailsWithBrowseUnderneath() {
        // What the activity sees once Navigation has restarted the task for a link from outside the app.
        assertTrue(
            navController.handleDeepLink(deepLink(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        )
        assertEquals(RestaurantRoute("r-1001"), navController.currentBackStackEntry!!.toRoute<RestaurantRoute>())
        assertTrue(navController.previousBackStackEntry!!.destination.hasRoute<BrowseRoute>())
        assertEquals(
            "Browse and Details, nothing else",
            2,
            navController.currentBackStack.value.count {
                it.destination.route !=
                    null &&
                    it.destination.parent != null
            }
        )
    }

    @Test
    fun aDeepLinkFromOutsideTheAppRestartsTheTaskToBuildThatBackStack() {
        // A warm start: the running activity gets the link as a new intent and hands it to handleDeepLink().
        assertTrue(navController.handleDeepLink(deepLink(Intent.FLAG_ACTIVITY_NEW_TASK)))
        val restarted = shadowOf(context).nextStartedActivity
        assertEquals(Uri.parse("mrd://restaurant/r-1001"), restarted.data)
        assertTrue("the task is cleared", restarted.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK != 0)
    }

    @Test
    fun anotherSchemeOrHostIsNotOurs() {
        assertEquals(false, navController.handleDeepLink(Intent(Intent.ACTION_VIEW, Uri.parse("mrd://menu/r-1001"))))
        assertEquals(
            false,
            navController.handleDeepLink(Intent(Intent.ACTION_VIEW, Uri.parse("https://restaurant/r-1001")))
        )
        assertTrue(navController.currentDestination!!.hasRoute<BrowseRoute>())
    }
}
