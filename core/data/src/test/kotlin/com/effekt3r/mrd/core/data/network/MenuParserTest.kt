package com.effekt3r.mrd.core.data.network

import com.effekt3r.mrd.core.model.Menu
import com.effekt3r.mrd.core.model.MenuCategory
import com.effekt3r.mrd.core.model.MenuItem
import com.effekt3r.mrd.core.model.MenuOption
import com.effekt3r.mrd.core.model.Money
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Spec §5.9's rules, with milestone 2b's deliberate edge cases as fixtures. */
class MenuParserTest {

    private fun parse(json: String, restaurantId: String = "r-1") =
        MenuParser.parse(Json.parseToJsonElement(json), restaurantId)

    private fun menu(vararg categories: String) =
        """{"restaurant_id": "r-1", "categories": [${categories.joinToString()}]}"""

    private fun item(id: String, extra: String = """"price_cents": 6900""") =
        """{"id": "$id", "name": "Item $id", $extra}"""

    @Test
    fun aValidMenuKeepsEveryFieldAndTheApiOrder() {
        val json = menu(
            """{"id": "c-1", "name": "Starters", "items": [
                {"id": "i-1", "name": "Grilled Halloumi", "price_cents": 6900, "available": false,
                 "description": "Citrus glaze", "image_url": "https://example.com/i.jpg",
                 "options": [{"id": "o-1", "name": "Extra lemon", "price_cents": 500},
                             {"id": "o-2", "name": "Dip", "price_cents": 0}]},
                {"id": "i-2", "name": "Bobotie", "price_cents": 12900}]}"""
        )
        val expected = Menu(
            listOf(
                MenuCategory(
                    "c-1",
                    "Starters",
                    listOf(
                        MenuItem(
                            "i-1",
                            "Grilled Halloumi",
                            Money(6900),
                            isAvailable = false,
                            description = "Citrus glaze",
                            imageUrl = "https://example.com/i.jpg",
                            options = listOf(
                                MenuOption("o-1", "Extra lemon", Money(500)),
                                MenuOption("o-2", "Dip", Money(0))
                            )
                        ),
                        MenuItem("i-2", "Bobotie", Money(12900), true, null, null, emptyList())
                    )
                )
            )
        )
        assertEquals(expected, parse(json))
    }

    @Test
    fun itemsThatCantBeOrderedAreDropped() {
        val json = menu(
            """{"id": "c-1", "name": "Mains", "items": [
                ${item("i-1", """"name": "No price"""")}, ${item("i-2", """"price_cents": -100""")},
                {"id": "i-3", "price_cents": 100}, ${item("i-4", """"price_cents": "6900"""")},
                ${item("i-5")}, ${item("i-5", """"price_cents": 1""")}]}"""
        )
        assertEquals(listOf("i-5"), parse(json)?.categories?.single()?.items?.map { it.id })
        assertEquals(Money(6900), parse(json)?.categories?.single()?.items?.single()?.price) // the first i-5 wins
    }

    @Test
    fun missingOrBadOptionalValuesReadAsAbsent() {
        val json = menu(
            """{"id": "c-1", "name": "Mains", "items": [{"id": "i-1", "name": "Gatsby", "price_cents": 8900,
                "available": "no", "description": "  ", "image_url": "http://example.com/i.jpg",
                "options": [{"id": "o-1", "name": "Chips", "price_cents": -500}, {"name": "No id", "price_cents": 0},
                            {"id": "o-2", "name": "Cheese", "price_cents": 1500.5}]}]}"""
        )
        assertEquals(
            MenuItem("i-1", "Gatsby", Money(8900), true, null, null, emptyList()),
            parse(json)?.categories?.single()?.items?.single()
        )
    }

    @Test
    fun categoriesWithoutANameOrValidItemsAreHidden() {
        val json = menu(
            """{"id": "c-1", "items": [${item("i-1")}]}""",
            """{"id": "c-2", "name": "Broken", "items": [{"id": "i-2"}]}""",
            """{"id": "c-3", "name": "Desserts", "items": [${item("i-3")}]}""",
            """{"id": "c-3", "name": "Repeated", "items": [${item("i-4")}]}"""
        )
        assertEquals(listOf("Desserts"), parse(json)?.categories?.map { it.name })
    }

    @Test
    fun anotherRestaurantsMenuOrItemsThatAllFailAreBadData() {
        assertNull(parse(menu("""{"id": "c-1", "name": "Mains", "items": [${item("i-1")}]}"""), restaurantId = "r-2"))
        assertNull(parse(menu("""{"id": "c-1", "name": "Mains", "items": [{"id": "i-1"}]}""")))
        assertNull(parse("""{"restaurant_id": "r-1"}"""))
    }

    @Test
    fun noCategoriesIsAnEmptyMenu() {
        assertEquals(Menu(emptyList()), parse(menu()))
    }
}
