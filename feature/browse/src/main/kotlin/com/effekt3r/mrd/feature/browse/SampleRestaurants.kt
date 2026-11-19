package com.effekt3r.mrd.feature.browse

/**
 * Four restaurants from mock-api/v1/restaurants.json for the Browse placeholder: one open, one with free delivery, one
 * closed and one not rated yet. Names, cuisines and times are data, as the API sends them; the words around them are
 * string resources. Milestone 5 replaces this with the real list.
 */
internal data class SampleRestaurant(
    val id: String,
    val name: String,
    val cuisines: List<String>,
    val rating: String?,
    val deliveryFeeCents: Long,
    val etaMinutes: Int,
    /** When a closed restaurant opens next; null while it's open. */
    val opensAt: String? = null
)

internal val sampleRestaurants = listOf(
    SampleRestaurant("r-1001", "Cape Town Grill", listOf("Steakhouse", "Grill"), "4.6", 1500, 32),
    SampleRestaurant("r-1015", "Woodstock Taverna", listOf("Greek"), "4.6", 0, 40),
    SampleRestaurant("r-1010", "Kirstenbosch Coffee Bar", listOf("Café", "Breakfast"), "4.6", 1200, 16, "07:30"),
    SampleRestaurant("r-1070", "Kommetjie Ramen Bar", listOf("Japanese"), null, 2500, 43)
)
