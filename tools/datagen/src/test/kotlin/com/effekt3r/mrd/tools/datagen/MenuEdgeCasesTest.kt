package com.effekt3r.mrd.tools.datagen

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuEdgeCasesTest {

    private companion object {
        val pool = ImagePool.load()
        val menus = MenuGenerator.menus(pool)

        fun menu(index: Int): JsonObject = menus.getValue(Generator.idFor(index))

        fun JsonObject.categories() = getValue("categories").jsonArray.map { it.jsonObject }

        fun JsonObject.items() = getValue("items").jsonArray.map { it.jsonObject }

        fun JsonObject.firstItem() = categories().first().items().first()

        fun JsonObject.allItems() = categories().flatMap { it.items() }
    }

    @Test
    fun everyCaseHasItsOwnIndexBelow220AwayFromTheRestaurantEdgeCases() {
        assertEquals(11, MenuEdgeCases.indexes.size)
        assertTrue(MenuEdgeCases.indexes.all { it in 1 until Dataset.STANDARD.size })
        assertTrue(MenuEdgeCases.indexes.none { it in EdgeCases.indexes || it == EdgeCases.DUPLICATE_ID_SOURCE })
    }

    @Test
    fun theFirstItemHasNoPriceANegativePriceNoNameOrAPriceAsAString() {
        assertFalse("price_cents" in menu(MenuEdgeCases.ITEM_WITHOUT_PRICE).firstItem())
        assertEquals(JsonPrimitive(-500), menu(MenuEdgeCases.NEGATIVE_PRICE).firstItem()["price_cents"])
        assertFalse("name" in menu(MenuEdgeCases.ITEM_WITHOUT_NAME).firstItem())
        val price = menu(MenuEdgeCases.PRICE_AS_STRING).firstItem().getValue("price_cents").jsonPrimitive
        assertTrue(price.isString && price.content.toInt() > 0)
    }

    @Test
    fun aCategoryHasNoNameAndAnotherHasOnlyItemsWithANullPrice() {
        assertFalse("name" in menu(MenuEdgeCases.CATEGORY_WITHOUT_NAME).categories().first())
        val invalid = menu(MenuEdgeCases.CATEGORY_ALL_INVALID).categories()[1].items()
        assertTrue(invalid.isNotEmpty() && invalid.all { it["price_cents"] == JsonNull })
    }

    @Test
    fun anOptionHasANegativePriceAndAnImageIsHttp() {
        val prices = menu(MenuEdgeCases.NEGATIVE_OPTION_PRICE).allItems()
            .flatMap { it["options"]?.jsonArray.orEmpty() }
            .map { it.jsonObject.getValue("price_cents").jsonPrimitive.int }
        assertEquals(1, prices.count { it < 0 })
        val images = menu(MenuEdgeCases.HTTP_IMAGE).allItems().mapNotNull { it["image_url"]?.jsonPrimitive?.content }
        assertEquals(1, images.count { it.startsWith("http://www.themealdb.com/") })
        assertTrue(images.all { it.startsWith("http://www.themealdb.com/") || it.startsWith("https://") })
    }

    @Test
    fun oneMenuNamesTheNextRestaurantAndOneHasNoCategories() {
        val wrong = MenuEdgeCases.WRONG_RESTAURANT_ID
        assertEquals(Generator.idFor(wrong + 1), menu(wrong).getValue("restaurant_id").jsonPrimitive.content)
        assertEquals(JsonArray(emptyList()), menu(MenuEdgeCases.NO_CATEGORIES)["categories"])
    }

    @Test
    fun theFirstTwoItemsShareAnId() {
        val items = menu(MenuEdgeCases.DUPLICATE_ITEM_ID).categories().first().items()
        assertEquals(items[0]["id"], items[1]["id"])
        assertTrue(items[0]["name"] != items[1]["name"])
    }

    @Test
    fun onlyTheEdgeCasesBreakTheRules() {
        menus.forEach { (id, menu) ->
            val index = id.removePrefix("r-").toInt() - 1001
            if (index in MenuEdgeCases.indexes) return@forEach
            assertEquals(id, menu.getValue("restaurant_id").jsonPrimitive.content)
            assertTrue(id, menu.categories().isNotEmpty() && menu.categories().all { "name" in it })
            val items = menu.allItems()
            assertEquals(id, items.size, items.map { it["id"] }.distinct().size)
            items.forEach { item ->
                assertTrue("$id: $item", "name" in item && !item.getValue("price_cents").jsonPrimitive.isString)
                assertTrue("$id: $item", item.getValue("price_cents").jsonPrimitive.int >= 0)
                item["image_url"]?.let { assertTrue("$id: $item", it.jsonPrimitive.content.startsWith("https://")) }
                item["options"]?.jsonArray?.forEach {
                    assertTrue("$id: $item", it.jsonObject.getValue("price_cents").jsonPrimitive.int >= 0)
                }
            }
        }
    }
}
