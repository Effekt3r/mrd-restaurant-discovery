package com.effekt3r.mrd.tools.datagen

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Deliberately broken menus, at fixed restaurant indexes below 220 that end in 5, so none is a restaurant edge case.
 * Each has exactly one problem, for milestone 3's validation tests (spec §5.9). mock-api/README.md lists them.
 */
internal object MenuEdgeCases {

    // The first item of the first category is dropped.
    const val ITEM_WITHOUT_PRICE = 5
    const val NEGATIVE_PRICE = 15
    const val ITEM_WITHOUT_NAME = 25
    const val PRICE_AS_STRING = 35

    // A category is dropped, or hidden because none of its items survives.
    const val CATEGORY_WITHOUT_NAME = 45
    const val CATEGORY_ALL_INVALID = 55

    // The item is kept without the bad part.
    const val NEGATIVE_OPTION_PRICE = 65
    const val HTTP_IMAGE = 75

    // The whole menu.
    const val WRONG_RESTAURANT_ID = 85
    const val NO_CATEGORIES = 95
    const val DUPLICATE_ITEM_ID = 105

    val indexes = setOf(
        ITEM_WITHOUT_PRICE, NEGATIVE_PRICE, ITEM_WITHOUT_NAME, PRICE_AS_STRING, CATEGORY_WITHOUT_NAME,
        CATEGORY_ALL_INVALID, NEGATIVE_OPTION_PRICE, HTTP_IMAGE, WRONG_RESTAURANT_ID, NO_CATEGORIES, DUPLICATE_ITEM_ID
    )

    fun apply(index: Int, menu: JsonObject): JsonObject = when (index) {
        ITEM_WITHOUT_PRICE -> menu.editFirstItem { it.withField("price_cents", null) }
        NEGATIVE_PRICE -> menu.editFirstItem { it.withField("price_cents", JsonPrimitive(-500)) }
        ITEM_WITHOUT_NAME -> menu.editFirstItem { it.withField("name", null) }
        PRICE_AS_STRING -> menu.editFirstItem { it.withField("price_cents", JsonPrimitive(it.text("price_cents"))) }
        CATEGORY_WITHOUT_NAME -> menu.editCategory(0) { it.withField("name", null) }
        CATEGORY_ALL_INVALID -> menu.editCategory(1) { category ->
            category.withField("items", JsonArray(category.items().map { it.withField("price_cents", JsonNull) }))
        }
        NEGATIVE_OPTION_PRICE -> menu.editFirstItem({ "options" in it }) { item ->
            val options = item.getValue("options").jsonArray.map { it.jsonObject }
            val negative = options.last().withField("price_cents", JsonPrimitive(-500))
            item.withField("options", JsonArray(options.dropLast(1) + negative))
        }
        HTTP_IMAGE -> menu.editFirstItem({ "image_url" in it }) {
            it.withField("image_url", JsonPrimitive(it.text("image_url").replace("https:", "http:")))
        }
        WRONG_RESTAURANT_ID -> menu.withField("restaurant_id", JsonPrimitive(Generator.idFor(index + 1)))
        NO_CATEGORIES -> menu.withField("categories", JsonArray(emptyList()))
        DUPLICATE_ITEM_ID -> menu.editCategory(0) { category ->
            val items = category.items()
            val repeated = items[1].withField("id", items[0].getValue("id"))
            category.withField("items", JsonArray(listOf(items[0], repeated) + items.drop(2)))
        }
        else -> menu
    }

    /** This object with [key] set to [value] in its place, or left out when [value] is null. */
    private fun JsonObject.withField(key: String, value: JsonElement?): JsonObject {
        val fields = toMutableMap()
        if (value == null) fields.remove(key) else fields[key] = value
        return JsonObject(fields)
    }

    private fun JsonObject.items() = getValue("items").jsonArray.map { it.jsonObject }

    private fun JsonObject.text(key: String) = getValue(key).jsonPrimitive.content

    private fun JsonObject.editCategory(position: Int, edit: (JsonObject) -> JsonObject): JsonObject {
        val categories = getValue("categories").jsonArray.map { it.jsonObject }
        return withField(
            "categories",
            JsonArray(
                categories.mapIndexed { i, it ->
                    if (i ==
                        position
                    ) {
                        edit(it)
                    } else {
                        it
                    }
                }
            )
        )
    }

    /** This menu with its first item that matches [where] edited. Fails when no item matches. */
    private fun JsonObject.editFirstItem(
        where: (JsonObject) -> Boolean = { true },
        edit: (JsonObject) -> JsonObject
    ): JsonObject {
        var edited = false
        val categories = getValue("categories").jsonArray.map { element ->
            val category = element.jsonObject
            category.withField(
                "items",
                JsonArray(
                    category.items().map { item ->
                        if (!edited && where(item)) item.also { edited = true }.let(edit) else item
                    }
                )
            )
        }
        check(edited) { "No item to break in menu ${text("restaurant_id")}" }
        return withField("categories", JsonArray(categories))
    }
}
