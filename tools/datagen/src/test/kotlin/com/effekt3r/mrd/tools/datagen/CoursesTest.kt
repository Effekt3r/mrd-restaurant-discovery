package com.effekt3r.mrd.tools.datagen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoursesTest {

    @Test
    fun everyCourseHasEnoughDishesAndOptionsAndPricesInTheDesignsRange() {
        assertEquals(Concepts.all.map { it.key }.toSet(), Courses.byConcept.keys)
        Courses.byConcept.forEach { (concept, courses) ->
            assertTrue(concept, courses.size in 3..5)
            courses.forEach { course ->
                val where = "$concept: ${course.name}"
                assertTrue(where, course.dishes.size >= 4)
                assertEquals(where, course.dishes.map { it.name }.distinct(), course.dishes.map { it.name })
                assertTrue(where, course.options.size >= 3)
                val range = if (course.photos == Photos.NONE) 20..45 else 25..250
                assertTrue(where, course.rands.first in range && course.rands.last in range)
                assertTrue(where, course.rands.first % 5 == 0 && course.rands.any { it % 10 == 9 })
            }
        }
    }

    @Test
    fun noMenuTextContainsARealName() {
        val text = Courses.byConcept.values.flatten().distinct().flatMap { course ->
            listOf(course.name) + course.options + course.dishes.flatMap { listOf(it.name, it.description) }
        }
        val clashes = text.distinct().mapNotNull { name -> name.realNameInside()?.let { "$name contains $it" } }
        assertEquals(emptyList<String>(), clashes)
    }
}
