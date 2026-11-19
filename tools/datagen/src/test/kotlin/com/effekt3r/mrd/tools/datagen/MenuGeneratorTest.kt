package com.effekt3r.mrd.tools.datagen

import java.io.File
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MenuGeneratorTest {

    @get:Rule
    val folder = TemporaryFolder()

    private companion object {
        val pool = ImagePool.load()
        val restaurants = Generator.generate(Dataset.STANDARD.size, pool)
        val menus = MenuGenerator.menus(pool)

        /** Each menu but the edge cases, with the restaurant it was made from: its id's first record. */
        val normal: List<Pair<JsonObject, GeneratedRestaurant>> = menus.mapNotNull { (id, menu) ->
            val index = restaurants.indexOfFirst { it.record["id"]?.jsonPrimitive?.content == id }
            if (index in MenuEdgeCases.indexes) null else menu to restaurants[index]
        }

        val items = normal.flatMap { (menu, _) -> menu.categories().flatMap { it.items() } }

        fun JsonObject.categories() = getValue("categories").jsonArray.map { it.jsonObject }

        fun JsonObject.items() = getValue("items").jsonArray.map { it.jsonObject }

        fun JsonObject.text(key: String) = getValue(key).jsonPrimitive.content

        fun percent(predicate: (JsonObject) -> Boolean) = 100.0 * items.count(predicate) / items.size
    }

    @Test
    fun generatingTwiceGivesTheSameMenus() {
        assertEquals(menus, MenuGenerator.menus(pool))
    }

    @Test
    fun everyDistinctIdInThe220FileHasOneMenuMadeFromItsFirstRecord() {
        val ids = restaurants.mapNotNull { it.record["id"]?.jsonPrimitive?.content }.distinct()
        assertEquals(218, ids.size)
        assertEquals(ids, menus.keys.toList())
        assertFalse(Generator.idFor(EdgeCases.MISSING_ID) in menus)
        // The duplicated id's menu is its first record's: a pizza place's courses, not the second record's.
        val first = restaurants[EdgeCases.DUPLICATE_ID_SOURCE]
        val courses = menus.getValue(Generator.idFor(EdgeCases.DUPLICATE_ID_SOURCE)).categories().map {
            it.text("name")
        }
        assertEquals(Courses.of(first.concept).map { it.name }, courses)
    }

    @Test
    fun everyNormalMenuListsItsConceptsCoursesWithFourToEightOfTheirDishes() {
        normal.forEach { (menu, restaurant) ->
            val id = menu.text("restaurant_id")
            val number = id.removePrefix("r-")
            assertEquals(listOf("restaurant_id", "categories"), menu.keys.toList())
            val courses = Courses.of(restaurant.concept)
            assertEquals(id, courses.map { it.name }, menu.categories().map { it.text("name") })
            menu.categories().zip(courses).forEachIndexed { position, (category, course) ->
                assertEquals(id, "c-$number-${position + 1}", category.text("id"))
                val names = category.items().map { it.text("name") }
                assertTrue("$id: $names", names.size in 4..8)
                val menuOrder = course.dishes.map { it.name }.filter { it in names }
                assertEquals("$id: dishes from the course, in its order", menuOrder, names)
                category.items().forEachIndexed { item, it ->
                    assertEquals(id, "i-$number-${(position + 1) * 100 + item + 1}", it.text("id"))
                }
            }
        }
    }

    @Test
    fun everyNormalItemFollowsTheSchema() {
        val keys = listOf("id", "name", "price_cents", "available", "description", "image_url", "options")
        normal.forEach { (menu, restaurant) ->
            menu.categories().zip(Courses.of(restaurant.concept)).forEach { (category, course) ->
                category.items().forEach { item ->
                    val where = "${menu.text("restaurant_id")}: $item"
                    assertEquals(where, keys.filter { it in item }, item.keys.toList())
                    item.getValue("available").jsonPrimitive.boolean
                    val dish = course.dishes.single { it.name == item.text("name") }
                    item["description"]?.let { assertEquals(where, dish.description, it.jsonPrimitive.content) }
                    val photos = when (course.photos) {
                        Photos.RESTAURANT -> pool.photos.getValue(restaurant.photoKey)
                        Photos.DESSERT -> pool.photos.getValue("dessert")
                        Photos.NONE -> emptyList()
                    }
                    item["image_url"]?.let { assertTrue(where, it.jsonPrimitive.content in photos) }
                    item["options"]?.jsonArray?.map { it.jsonObject }?.let { options ->
                        assertTrue(where, options.size in 1..3)
                        options.forEachIndexed { position, option ->
                            assertEquals(
                                where,
                                "o-${item.text("id").removePrefix("i-")}-${position + 1}",
                                option.text("id")
                            )
                            assertTrue(where, option.text("name") in course.options)
                            assertTrue(where, option.getValue("price_cents").jsonPrimitive.int in 0..3_500 step 500)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun pricesAreWholeRandsInTheirCoursesRangeWithOneStylePerMenu() {
        normal.forEach { (menu, restaurant) ->
            val prices = menu.categories().zip(Courses.of(restaurant.concept)).flatMap { (category, course) ->
                category.items().map { item ->
                    val cents = item.getValue("price_cents").jsonPrimitive.int
                    assertTrue("${menu.text("restaurant_id")}: $item", cents % 100 == 0 && cents / 100 in course.rands)
                    cents / 100
                }
            }
            val style = prices.map {
                if (it % 10 == 9) {
                    "nine"
                } else if (it % 5 == 0) {
                    "five"
                } else {
                    "other"
                }
            }.toSet()
            assertTrue("${menu.text("restaurant_id")}: $prices", style == setOf("nine") || style == setOf("five"))
        }
    }

    @Test
    fun theRatesFollowTheMenuDesign() {
        assertEquals("sold out", 8.0, percent { !it.getValue("available").jsonPrimitive.boolean }, 1.5)
        assertEquals("no description", 25.0, percent { "description" !in it }, 1.5)
        assertEquals("a photo", 50.0, percent { "image_url" in it }, 1.5)
        assertEquals("options", 30.0, percent { "options" in it }, 1.5)
        val options = items.flatMap { item -> item["options"]?.jsonArray.orEmpty().map { it.jsonObject } }
        val free = 100.0 * options.count { it.getValue("price_cents").jsonPrimitive.int == 0 } / options.size
        assertEquals("free options", 20.0, free, 2.0)
    }

    @Test
    fun writingDeletesStaleMenusAndLeavesOtherFiles() {
        val directory = folder.newFolder("menus")
        File(directory, "r-9999.json").writeText("{}\n")
        File(directory, "r-1001.json").writeText("old\n")
        File(directory, "notes.txt").writeText("kept\n")
        val deleted = MenuGenerator.write(directory, mapOf("r-1001.json" to "new\n", "r-1002.json" to "new\n"))
        assertEquals(listOf("r-9999.json"), deleted)
        assertEquals(listOf("notes.txt", "r-1001.json", "r-1002.json"), directory.list()!!.sorted())
        assertEquals("new\n", File(directory, "r-1001.json").readText())
    }
}
