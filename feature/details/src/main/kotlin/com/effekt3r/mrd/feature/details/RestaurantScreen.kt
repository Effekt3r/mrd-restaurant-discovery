package com.effekt3r.mrd.feature.details

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.effekt3r.mrd.core.designsystem.component.MrdBackButton
import com.effekt3r.mrd.core.designsystem.component.MrdFavouriteButton
import com.effekt3r.mrd.core.designsystem.component.MrdMenuCategoryHeader
import com.effekt3r.mrd.core.designsystem.component.MrdMenuItemRow
import com.effekt3r.mrd.core.designsystem.component.MrdRating
import com.effekt3r.mrd.core.designsystem.component.MrdSkeletonRow
import com.effekt3r.mrd.core.designsystem.component.MrdStateScreen
import com.effekt3r.mrd.core.designsystem.component.MrdStatusLabel
import com.effekt3r.mrd.core.designsystem.component.MrdTag
import com.effekt3r.mrd.core.designsystem.icon.MrdIcons
import com.effekt3r.mrd.core.designsystem.preview.MrdPreviews
import com.effekt3r.mrd.core.designsystem.theme.MrdShapes
import com.effekt3r.mrd.core.designsystem.theme.MrdSpacing
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme
import com.effekt3r.mrd.core.model.DailyWindow
import com.effekt3r.mrd.core.model.Menu
import com.effekt3r.mrd.core.model.MenuCategory
import com.effekt3r.mrd.core.model.MenuItem
import com.effekt3r.mrd.core.model.MenuState
import com.effekt3r.mrd.core.model.Money
import com.effekt3r.mrd.core.model.OpenStatus
import com.effekt3r.mrd.core.model.OpeningHours
import com.effekt3r.mrd.core.model.Rating
import com.effekt3r.mrd.core.model.Restaurant
import com.effekt3r.mrd.core.model.RestaurantId
import com.effekt3r.mrd.core.model.RestaurantSummary
import com.effekt3r.mrd.core.model.WeekMinute
import com.effekt3r.mrd.core.ui.ImageSize
import com.effekt3r.mrd.core.ui.RestaurantFormatter
import com.effekt3r.mrd.core.ui.RestaurantImage
import com.effekt3r.mrd.core.ui.SyncProblem
import com.effekt3r.mrd.core.ui.rememberRestaurantFormatter
import com.effekt3r.mrd.core.ui.tone
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle

/** Details, with its ViewModel: the state is collected only while the screen is at least STARTED (spec §4.4). */
@Composable
internal fun RestaurantScreen(onBack: () -> Unit, viewModel: DetailsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RestaurantScreen(
        state = state,
        onBack = onBack,
        onFavouriteToggled = viewModel::onFavouriteToggled,
        onRetry = viewModel::onRetry,
        onRetryMenu = viewModel::onRetryMenu
    )
}

/**
 * Details (spec §6.3): the photo edge to edge under the status bar, with back and favourite buttons over it, then the
 * name, cuisines, stats, status and the week's hours, and the menu, which has states of its own. Before the
 * restaurant is cached, as after a deep link, the screen shows loading, not found, or an error with Retry.
 */
@Composable
internal fun RestaurantScreen(
    state: DetailsUiState,
    onBack: () -> Unit,
    onFavouriteToggled: () -> Unit,
    onRetry: () -> Unit,
    onRetryMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val back = dropUnlessResumed { onBack() }
    when (val restaurant = state.restaurant) {
        is RestaurantState.Found ->
            RestaurantDetails(restaurant.restaurant, state, back, onFavouriteToggled, onRetryMenu, modifier)

        RestaurantState.Loading -> StateWithBackButton(back, modifier) {
            val loading = stringResource(R.string.details_loading)
            Box(
                Modifier.fillMaxSize().semantics(mergeDescendants = true) { contentDescription = loading },
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        RestaurantState.NotFound -> StateWithBackButton(back, modifier) {
            MrdStateScreen(
                icon = MrdIcons.SearchOff,
                title = stringResource(R.string.details_not_found_title),
                message = stringResource(R.string.details_not_found_message)
            ) {
                Button(onClick = back) { Text(stringResource(R.string.details_see_all_restaurants)) }
            }
        }

        is RestaurantState.Unavailable -> StateWithBackButton(back, modifier) {
            val (title, message) = when (state.restaurantProblem) {
                SyncProblem.Offline -> R.string.details_offline_title to R.string.details_offline_message
                SyncProblem.Storage -> R.string.details_storage_title to R.string.details_storage_message
                SyncProblem.CouldNotConnect, null -> R.string.details_failed_title to R.string.details_failed_message
            }
            MrdStateScreen(
                icon = MrdIcons.CloudOff,
                title = stringResource(title),
                message = stringResource(message)
            ) {
                Button(onClick = onRetry) { Text(stringResource(R.string.details_retry)) }
            }
        }
    }
}

@Composable
private fun StateWithBackButton(onBack: () -> Unit, modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        content()
        MrdBackButton(onClick = onBack, modifier = Modifier.padding(MrdSpacing.Small))
    }
}

@Composable
private fun RestaurantDetails(
    restaurant: Restaurant,
    state: DetailsUiState,
    onBack: () -> Unit,
    onFavouriteToggled: () -> Unit,
    onRetryMenu: () -> Unit,
    modifier: Modifier
) {
    val listState = rememberLazyListState()
    // The photo's gradient sits behind the status bar until so much of the photo has scrolled away that the bar
    // would be over its plain part. From then on a surface strip goes behind the bar, with the theme's icons.
    val density = LocalDensity.current
    val gradientUnderBar = with(density) { SCRIM_HEIGHT.roundToPx() } - WindowInsets.statusBars.getTop(density)
    val overPhoto by remember(gradientUnderBar) {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 &&
                listState.firstVisibleItemScrollOffset < gradientUnderBar
        }
    }
    StatusBarIcons(overPhoto)
    // Side insets go on each part below the photo, not on the list, so the photo runs under a landscape cutout while
    // the text and the buttons stay clear of it, each inset once.
    val sides = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
    Box(modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues()
        ) {
            item(key = "photo") { Photo(restaurant, onBack, onFavouriteToggled) }
            item(key = "about") { About(restaurant, state, sides.padding(MrdSpacing.Large)) }
            item(key = "hours") { Hours(restaurant.openingHours, state.weekMinute.day, sides) }
            item(key = "menu") {
                Text(
                    text = stringResource(R.string.details_menu),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = sides.padding(
                        start = MrdSpacing.Large,
                        end = MrdSpacing.Large,
                        top = MrdSpacing.ExtraLarge
                    )
                        .semantics { heading() }
                )
            }
            menu(state, onRetryMenu, sides)
        }
        if (!overPhoto) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(MaterialTheme.colorScheme.surface)
            )
        }
    }
}

@Composable
private fun Photo(restaurant: Restaurant, onBack: () -> Unit, onFavouriteToggled: () -> Unit) {
    val summary = restaurant.summary
    Box(Modifier.fillMaxWidth().height(PHOTO_HEIGHT)) {
        RestaurantImage(
            url = summary.imageUrl,
            placeholderSeed = summary.cuisines.firstOrNull() ?: summary.name,
            size = ImageSize.Header,
            modifier = Modifier.fillMaxSize()
        )
        // A dark gradient behind the status bar's icons and the buttons, so both read over any photo (spec §6.3).
        Box(
            Modifier
                .fillMaxWidth()
                .height(SCRIM_HEIGHT)
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA), Color.Transparent)
                    )
                )
        )
        MrdBackButton(
            onClick = onBack,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Start))
                .padding(MrdSpacing.Small)
        )
        MrdFavouriteButton(
            isFavourite = summary.isFavourite,
            onFavouriteChange = { onFavouriteToggled() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.End))
                .padding(MrdSpacing.Small)
        )
    }
}

@Composable
private fun About(restaurant: Restaurant, state: DetailsUiState, modifier: Modifier) {
    val format = rememberRestaurantFormatter()
    val summary = restaurant.summary
    Column(modifier, verticalArrangement = Arrangement.spacedBy(MrdSpacing.Small)) {
        Text(
            text = summary.name,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() }
        )
        if (summary.cuisines.isNotEmpty()) {
            Text(
                text = format.cuisines(summary.cuisines),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Stats(restaurant, format)
        MrdStatusLabel(
            text = format.statusLine(summary.openStatus, state.weekMinute, restaurant.openingHours),
            tone = summary.openStatus.tone
        )
    }
}

/** Rating, ETA and fee, each read by TalkBack as one item: "Delivery time, 32 minutes" (spec §6.3). */
@Composable
private fun Stats(restaurant: Restaurant, format: RestaurantFormatter) {
    val summary = restaurant.summary
    val missing = stringResource(R.string.details_missing)
    val notKnown = stringResource(R.string.details_not_known)
    val fee = summary.deliveryFee
    val free = stringResource(R.string.details_free)
    Row(
        Modifier.fillMaxWidth().padding(vertical = MrdSpacing.Small),
        horizontalArrangement = Arrangement.spacedBy(MrdSpacing.Medium)
    ) {
        Stat(
            label = stringResource(R.string.details_rating),
            spoken = summary.rating?.let(format::rating) ?: stringResource(R.string.details_rating_new_spoken),
            modifier = Modifier.weight(1f)
        ) { MrdRating(summary.rating?.let(format::rating)) }
        Stat(
            label = stringResource(R.string.details_delivery_time),
            spoken = summary.etaMinutes?.let(format::etaSpoken) ?: notKnown,
            modifier = Modifier.weight(1f)
        ) { StatValue(summary.etaMinutes?.let(format::eta) ?: missing) }
        Stat(
            label = stringResource(R.string.details_delivery_fee),
            spoken = when {
                fee == null -> notKnown
                fee.cents == 0L -> free
                else -> format.money(fee)
            },
            modifier = Modifier.weight(1f)
        ) { if (fee?.cents == 0L) MrdTag(free) else StatValue(fee?.let(format::money) ?: missing) }
    }
}

@Composable
private fun Stat(label: String, spoken: String, modifier: Modifier, value: @Composable () -> Unit) {
    val description = stringResource(R.string.details_stat_spoken, label, spoken)
    Column(
        modifier.clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(MrdSpacing.ExtraSmall)
    ) {
        value()
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatValue(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelLarge)
}

/** The week's hours from Monday, with today highlighted (spec §6.3). */
@Composable
private fun Hours(hours: OpeningHours?, today: DayOfWeek, modifier: Modifier) {
    Column(
        modifier.padding(start = MrdSpacing.Large, end = MrdSpacing.Large, top = MrdSpacing.Small),
        verticalArrangement = Arrangement.spacedBy(MrdSpacing.ExtraSmall)
    ) {
        Text(
            text = stringResource(R.string.details_hours),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = MrdSpacing.ExtraSmall).semantics { heading() }
        )
        if (hours == null) {
            Text(
                text = stringResource(R.string.details_hours_unknown),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            weekHours(hours).forEach { (day, dayHours) -> DayRow(day, dayHours, isToday = day == today) }
        }
    }
}

@Composable
private fun DayRow(day: DayOfWeek, hours: DayHours, isToday: Boolean) {
    val format = rememberRestaurantFormatter()
    val resources = LocalResources.current
    val locale = resources.configuration.locales[0]
    val separator = stringResource(R.string.details_windows_separator)
    val (shown, spoken) = when (hours) {
        DayHours.Closed -> stringResource(R.string.details_closed).let { it to it }

        DayHours.AllDay -> stringResource(R.string.details_open_24_hours).let { it to it }

        is DayHours.Windows -> hours.windows.joinToString(separator) {
            resources.getString(R.string.details_window, format.time(it.opens), format.time(it.closes))
        } to hours.windows.joinToString(separator) {
            resources.getString(R.string.details_window_spoken, format.time(it.opens), format.time(it.closes))
        }
    }
    val dayName = day.getDisplayName(TextStyle.FULL, locale)
    val spokenDay = stringResource(R.string.details_day_spoken, dayName, spoken)
    val description = if (isToday) {
        stringResource(R.string.details_day_spoken, stringResource(R.string.details_today), spokenDay)
    } else {
        spokenDay
    }
    val weight = if (isToday) FontWeight.Bold else null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isToday) {
                    Modifier.background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MrdShapes.Field
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = MrdSpacing.Small, vertical = MrdSpacing.ExtraSmall)
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(MrdSpacing.Medium)
    ) {
        Text(
            text = dayName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = weight,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = shown,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = weight,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.4f)
        )
    }
}

/** The menu's own states (spec §6.3): it loads after the restaurant and never holds it up. */
private fun LazyListScope.menu(state: DetailsUiState, onRetry: () -> Unit, sides: Modifier) {
    when (val menu = state.menu) {
        MenuState.Loading -> item(key = "menu-loading") { MenuLoading(sides) }

        is MenuState.Content -> categories(menu.menu, sides)

        MenuState.NotAvailable -> item(key = "menu-not-available") {
            MenuMessage(stringResource(R.string.details_menu_not_available), sides)
        }

        is MenuState.Error -> {
            val cached = menu.cached
            if (cached != null) {
                item(key = "menu-note") { CachedMenuNote(cached.fetchedAt, state, onRetry, sides) }
                categories(cached.menu, sides)
            } else {
                item(key = "menu-error") { MenuError(state.menuProblem, onRetry, sides) }
            }
        }
    }
}

private fun LazyListScope.categories(menu: Menu, sides: Modifier) {
    menu.categories.forEach { category ->
        item(key = "category-${category.id}") { MrdMenuCategoryHeader(category.name, sides) }
        items(category.items, key = { "item-${category.id}-${it.id}" }) { item -> Dish(item, sides) }
    }
}

@Composable
private fun Dish(item: MenuItem, modifier: Modifier) {
    val format = rememberRestaurantFormatter()
    val resources = LocalResources.current
    val free = stringResource(R.string.details_free)
    val separator = stringResource(R.string.details_options_separator)
    MrdMenuItemRow(
        name = item.name,
        price = format.money(item.price),
        modifier = modifier,
        description = item.description,
        options = item.options.takeIf { it.isNotEmpty() }?.joinToString(separator) { option ->
            val price = if (option.price.cents == 0L) free else format.money(option.price)
            resources.getString(R.string.details_option, option.name, price)
        },
        isSoldOut = !item.isAvailable,
        image = item.imageUrl?.let { url -> thumbnail(url, item.name) }
    )
}

// A typed helper: a lambda built inside let() isn't inferred as @Composable.
private fun thumbnail(url: String, placeholderSeed: String): @Composable () -> Unit = {
    RestaurantImage(url, placeholderSeed, ImageSize.Thumbnail, Modifier.fillMaxSize())
}

@Composable
private fun MenuLoading(modifier: Modifier) {
    val loading = stringResource(R.string.details_menu_loading)
    Column(modifier.semantics(mergeDescendants = true) { contentDescription = loading }) {
        repeat(MENU_SKELETON_ROWS) { MrdSkeletonRow() }
    }
}

@Composable
private fun MenuMessage(text: String, modifier: Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(MrdSpacing.Large)
    )
}

@Composable
private fun CachedMenuNote(
    fetchedAt: java.time.Instant,
    state: DetailsUiState,
    onRetry: () -> Unit,
    modifier: Modifier
) {
    val format = rememberRestaurantFormatter()
    Row(
        modifier.padding(start = MrdSpacing.Large, end = MrdSpacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(
                R.string.details_menu_from,
                format.fetchedAt(fetchedAt, state.now, ZoneId.systemDefault())
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onRetry) { Text(stringResource(R.string.details_retry)) }
    }
}

@Composable
private fun MenuError(problem: SyncProblem?, onRetry: () -> Unit, modifier: Modifier) {
    Column(
        modifier.fillMaxWidth().padding(MrdSpacing.Large),
        verticalArrangement = Arrangement.spacedBy(MrdSpacing.Medium),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = stringResource(
                when (problem) {
                    SyncProblem.Offline -> R.string.details_menu_offline
                    SyncProblem.Storage -> R.string.details_menu_storage
                    SyncProblem.CouldNotConnect, null -> R.string.details_menu_failed
                }
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onRetry) { Text(stringResource(R.string.details_retry)) }
    }
}

/**
 * Light status-bar icons while they're over the photo's dark gradient, and the theme's own otherwise (spec §6.4).
 * MainActivity's enableEdgeToEdge() chose the theme's; leaving Details puts them back.
 */
@Composable
private fun StatusBarIcons(overPhoto: Boolean) {
    val view = LocalView.current
    val darkTheme = isSystemInDarkTheme()
    DisposableEffect(view, overPhoto, darkTheme) {
        val controller = view.context.findActivity()?.window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.isAppearanceLightStatusBars = !overPhoto && !darkTheme
        onDispose { controller?.isAppearanceLightStatusBars = !darkTheme }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private val PHOTO_HEIGHT = 240.dp
private val SCRIM_HEIGHT = 120.dp
private const val SCRIM_ALPHA = 0.6f
private const val MENU_SKELETON_ROWS = 3

@MrdPreviews
@Composable
private fun RestaurantScreenPreview() {
    val now = Instant.parse("2026-11-16T10:00:00Z")
    val grill = Restaurant(
        summary = RestaurantSummary(
            id = RestaurantId("r-1001"),
            name = "Cape Town Grill",
            cuisines = listOf("Steakhouse", "Grill"),
            rating = Rating(46),
            deliveryFee = Money(1500),
            etaMinutes = 32,
            imageUrl = null,
            openStatus = OpenStatus.Open(closesAt = WeekMinute.of(DayOfWeek.MONDAY, 22)),
            isFavourite = true
        ),
        openingHours = OpeningHours.from(DayOfWeek.entries.map { DailyWindow(it, 11 * 60, 22 * 60) })
    )
    val menu = Menu(
        listOf(
            MenuCategory(
                "c-1",
                "Starters",
                listOf(
                    MenuItem(
                        "i-1",
                        "Grilled Halloumi",
                        Money(8900),
                        true,
                        "Citrus glaze, micro herbs",
                        null,
                        emptyList()
                    ),
                    MenuItem("i-2", "Rump 300 g", Money(18900), false, null, null, emptyList())
                )
            )
        )
    )
    MrdTheme {
        Surface {
            RestaurantScreen(
                state = DetailsUiState(RestaurantState.Found(grill), MenuState.Content(menu, now), now = now),
                onBack = {},
                onFavouriteToggled = {},
                onRetry = {},
                onRetryMenu = {}
            )
        }
    }
}
