package com.effekt3r.mrd.core.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.effekt3r.mrd.core.designsystem.component.MrdRestaurantCard
import com.effekt3r.mrd.core.model.RestaurantSummary
import com.effekt3r.mrd.core.model.WeekMinute

/**
 * [restaurant] as Mr D's card (spec §6.2): its photo, name, cuisines, rating, fee, ETA and status, formatted for en-ZA.
 * [now] is the current minute in Cape Town, so a closed restaurant can say "Opens tomorrow 11:00". TalkBack reads the
 * card as one item, with the favourite as a custom action (spec §6.5).
 */
@Composable
fun RestaurantCard(
    restaurant: RestaurantSummary,
    now: WeekMinute,
    onClick: () -> Unit,
    onToggleFavourite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val format = rememberRestaurantFormatter()
    val fee = restaurant.deliveryFee
    val placeholderSeed = restaurant.cuisines.firstOrNull() ?: restaurant.name
    MrdRestaurantCard(
        name = restaurant.name,
        cuisines = format.cuisines(restaurant.cuisines),
        rating = restaurant.rating?.let(format::rating),
        deliveryFee = fee?.let(format::deliveryFee),
        isFreeDelivery = fee?.cents == 0L,
        status = format.status(restaurant.openStatus, now),
        statusTone = restaurant.openStatus.tone,
        eta = restaurant.etaMinutes?.let(format::eta),
        isFavourite = restaurant.isFavourite,
        contentDescription = format.description(restaurant, now),
        onClick = onClick,
        // The repository toggles; the card's new value is the opposite of what it shows.
        onFavouriteChange = { onToggleFavourite() },
        modifier = modifier,
        placeholderSeed = placeholderSeed,
        image = restaurant.imageUrl?.let { url -> cardPhoto(url, placeholderSeed) }
    )
}

// A typed helper: a lambda built inside let() isn't inferred as @Composable.
private fun cardPhoto(url: String, placeholderSeed: String): @Composable () -> Unit = {
    RestaurantImage(url, placeholderSeed, ImageSize.Card, Modifier.fillMaxSize())
}
