package com.effekt3r.mrd.core.data.network

import com.effekt3r.mrd.core.model.Menu
import com.effekt3r.mrd.core.model.MenuCategory
import com.effekt3r.mrd.core.model.MenuItem
import com.effekt3r.mrd.core.model.MenuOption
import com.effekt3r.mrd.core.model.Money
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** Spec §5.9's rules for a menu. Within a menu, the first of two equal ids wins. */
internal object MenuParser {

    /** The validated menu, or null for bad data: another restaurant's menu, or items that all failed validation. */
    fun parse(payload: JsonElement, restaurantId: String): Menu? {
        val fields = payload as? JsonObject ?: return null
        if (fields.string("restaurant_id") != restaurantId) return null
        val rawCategories = fields["categories"] as? JsonArray ?: return null
        val categoryIds = HashSet<String>()
        val itemIds = HashSet<String>()
        val categories = rawCategories.mapNotNull { element ->
            val category = element as? JsonObject ?: return@mapNotNull null
            val id = category.text("id") ?: return@mapNotNull null
            val name = category.text("name") ?: return@mapNotNull null
            if (!categoryIds.add(id)) return@mapNotNull null
            // A category left with no valid items is hidden.
            category.array("items").mapNotNull(::item).filter { itemIds.add(it.id) }
                .takeIf { it.isNotEmpty() }
                ?.let { MenuCategory(id, name, it) }
        }
        val hadItems = rawCategories.any { (it as? JsonObject)?.array("items")?.isNotEmpty() == true }
        return if (hadItems && categories.isEmpty()) null else Menu(categories)
    }

    // An item without an id, a name or a price of 0 or more can't be ordered, so it's dropped.
    private fun item(element: JsonElement): MenuItem? {
        val fields = element as? JsonObject ?: return null
        val optionIds = HashSet<String>()
        return MenuItem(
            id = fields.text("id") ?: return null,
            name = fields.text("name") ?: return null,
            price = fields.money("price_cents") ?: return null,
            isAvailable = fields.boolean("available") ?: true,
            description = fields.text("description"),
            imageUrl = fields.httpsUrl("image_url"),
            options = fields.array("options").mapNotNull(::option).filter { optionIds.add(it.id) }
        )
    }

    private fun option(element: JsonElement): MenuOption? {
        val fields = element as? JsonObject ?: return null
        return MenuOption(
            id = fields.text("id") ?: return null,
            name = fields.text("name") ?: return null,
            price = fields.money("price_cents") ?: return null
        )
    }

    private fun JsonObject.money(key: String): Money? = long(key)?.takeIf { it >= 0 }?.let(::Money)
}
