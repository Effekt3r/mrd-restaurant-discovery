package com.effekt3r.mrd.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * colors.xml repeats three palette colours for XML that can't read Compose tokens: the launcher icon and the window
 * background. This keeps the two in step. Gradle re-runs it when colors.xml changes: the library convention includes
 * Android resources in unit tests, which makes the merged resources an input of the test task.
 */
class ColorResourcesTest {

    // Gradle runs the tests in the module's directory, core/designsystem.
    private val xml = File("src/main/res/values/colors.xml").readText()

    @Test
    fun xmlColoursMatchThePalette() {
        assertEquals(hex(MrdPalette.Sky), xmlColour("designsystem_sky"))
        assertEquals(hex(MrdPalette.Ink), xmlColour("designsystem_ink"))
        assertEquals(hex(MrdPalette.White), xmlColour("designsystem_white"))
    }

    private fun hex(color: Color) = "#%08X".format(color.toArgb())

    private fun xmlColour(name: String): String? =
        Regex("""<color name="$name">(#[0-9A-Fa-f]{8})</color>""").find(xml)?.groupValues?.get(1)?.uppercase()
}
