package com.effekt3r.mrd.tools.datagen

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** A generated restaurant. A null value leaves its key out of the JSON, which is how the API says it's missing. */
internal data class Restaurant(
    val id: String,
    val name: String,
    val cuisines: List<String>,
    val rating: Double?,
    val deliveryFeeCents: Int?,
    val etaMinutes: Int?,
    val isOpen: Boolean,
    val imageUrl: String?,
    val openingHours: List<OpeningHours>?
) {

    fun toJson(): JsonObject = buildJsonObject {
        put("id", id)
        put("name", name)
        putJsonArray("cuisines") { cuisines.forEach { add(it) } }
        rating?.let { put("rating", it) }
        deliveryFeeCents?.let { put("delivery_fee_cents", it) }
        etaMinutes?.let { put("eta_minutes", it) }
        put("is_open", isOpen)
        imageUrl?.let { put("image_url", it) }
        openingHours?.let { hours -> putJsonArray("opening_hours") { hours.forEach { add(it.toJson()) } } }
    }

    companion object {
        /** The keys in spec §5.1's order, which every record keeps. */
        val KEYS = listOf(
            "id", "name", "cuisines", "rating", "delivery_fee_cents", "eta_minutes", "is_open", "image_url",
            "opening_hours"
        )
    }
}
