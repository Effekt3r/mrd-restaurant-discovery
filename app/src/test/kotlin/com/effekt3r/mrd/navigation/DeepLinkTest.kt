package com.effekt3r.mrd.navigation

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
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
import org.robolectric.Robolectric
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

    private val launch = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

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

    @Test
    fun aDeepLinkToTheRunningAppRestartsItsTask() {
        val running = Robolectric.buildActivity(HostActivity::class.java, launch).setup()
        running.newIntent(deepLink(Intent.FLAG_ACTIVITY_NEW_TASK))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(Uri.parse("mrd://restaurant/r-1001"), shadowOf(running.get()).nextStartedActivity?.data)
        assertTrue(running.get().isFinishing)
    }

    @Test
    fun aDeepLinkOpensDetailsAfterAndroidHasKilledTheAppInTheBackground() {
        // The app was on Browse and went to the background. Android stopped the activity, saved its state and then
        // killed the process, so the task is still in Recents but only that saved state is left.
        val saved = Bundle()
        Robolectric.buildActivity(HostActivity::class.java, launch).setup().pause().stop().saveInstanceState(saved)

        // A link arrives. Android recreates the activity from its saved state and, since the activity is singleTop and
        // on top of its task, delivers the link as a new intent after onPostCreate and before onResume, so before
        // anything is composed (ActivityThread.performResumeActivity).
        val recreated = Robolectric.buildActivity(HostActivity::class.java, launch)
            .create(saved)
            .start()
            .restoreInstanceState(saved)
            .postCreate(saved)
            .newIntent(deepLink(Intent.FLAG_ACTIVITY_NEW_TASK))
            .resume()
            .visible()
        shadowOf(Looper.getMainLooper()).idle()

        // Navigation restarts the task for the link, as it does when the app is running...
        val restarted = shadowOf(recreated.get()).nextStartedActivity
        assertEquals(Uri.parse("mrd://restaurant/r-1001"), restarted?.data)
        assertTrue("the recreated activity makes way for the new task", recreated.get().isFinishing)

        // ...and the new task opens Details, with Browse underneath.
        val details = Robolectric.buildActivity(HostActivity::class.java, restarted).setup().get()
        val screens = details.navController.currentBackStack.value.filterNot { it.destination is NavGraph }
        assertEquals(2, screens.size)
        assertTrue(screens[0].destination.hasRoute<BrowseRoute>())
        assertEquals(RestaurantRoute("r-1001"), screens[1].toRoute<RestaurantRoute>())
    }

    /**
     * MainActivity's content setup without Hilt. The app's graph is set on the NavController as NavHost sets it, but
     * not composed, so no screen, ViewModel or Hilt is needed.
     */
    class HostActivity : ComponentActivity() {
        lateinit var navController: NavHostController

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setNavigationContent { navController ->
                this@HostActivity.navController = navController
                navController.graph = remember(navController) {
                    navController.createGraph(startDestination = BrowseRoute) { mrdDestinations(navController) }
                }
            }
        }
    }
}
