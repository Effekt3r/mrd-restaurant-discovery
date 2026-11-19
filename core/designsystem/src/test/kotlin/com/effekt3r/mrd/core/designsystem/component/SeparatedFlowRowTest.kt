package com.effekt3r.mrd.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SeparatedFlowRowTest {

    @get:Rule
    val compose = createComposeRule()

    // Where each placed separator is. A separator that's left out is measured but never placed, so it never reports.
    private val separators = mutableListOf<Offset>()

    // Three 40 dp items with 8 dp separators and 4 dp gaps: two items and a separator fill 96 dp of a 100 dp row.
    private fun setRow(width: Int) = compose.setContent {
        Box(Modifier.width(width.dp)) {
            SeparatedFlowRow(
                separator = { Box(Modifier.size(8.dp).onGloballyPositioned { separators += it.positionInRoot() }) },
                items = List(3) { index -> item(index) }
            )
        }
    }

    private fun item(index: Int): @Composable () -> Unit = { Box(Modifier.size(40.dp).testTag("item$index")) }

    @Test
    fun anItemThatWrapsStartsItsLineWithoutTheSeparatorBeforeIt() {
        setRow(width = 100)
        val first = compose.onNodeWithTag("item0").getBoundsInRoot()
        val second = compose.onNodeWithTag("item1").getBoundsInRoot()
        val wrapped = compose.onNodeWithTag("item2").getBoundsInRoot()
        assertTrue("item2 should be on the second line", wrapped.top > first.top)
        assertEquals(first.left, wrapped.left)
        compose.runOnIdle {
            assertEquals("only the separator between the first two items is placed", 1, separators.distinct().size)
            assertTrue(separators.single().x > first.right.value && separators.single().x < second.left.value)
        }
    }

    @Test
    fun aRowThatFitsKeepsEverySeparator() {
        setRow(width = 200)
        val first = compose.onNodeWithTag("item0").getBoundsInRoot()
        assertEquals(first.top, compose.onNodeWithTag("item2").getBoundsInRoot().top)
        compose.runOnIdle { assertEquals(2, separators.distinct().size) }
    }
}
