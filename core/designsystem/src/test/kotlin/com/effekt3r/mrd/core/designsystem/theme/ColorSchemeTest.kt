package com.effekt3r.mrd.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.lang.reflect.Method
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every Material colour role in both schemes is one of Mr D's palette colours. A role left unset keeps Material's
 * baseline, purple for the accents, which a stock component such as a tonal button or a selected chip would show.
 */
class ColorSchemeTest {

    @Test
    fun everyRoleInTheLightSchemeIsAPaletteColour() = assertEveryRoleFromThePalette(LightColorScheme)

    @Test
    fun everyRoleInTheDarkSchemeIsAPaletteColour() = assertEveryRoleFromThePalette(DarkColorScheme)

    private fun assertEveryRoleFromThePalette(scheme: ColorScheme) {
        val roles = colours(ColorScheme::class.java.methods, scheme)
        // Material 3 1.4.0 has 48 roles. Fewer would mean the reflection stopped finding them, and the test would pass
        // without checking anything.
        assertTrue("Found only ${roles.size} colour roles: ${roles.keys}", roles.size >= 48)
        val palette = colours(MrdPalette::class.java.methods, MrdPalette).values.toSet()
        val strays = roles.filterValues { it !in palette }
            .map { (role, colour) -> "$role: #%08X".format(colour.toArgb()) }
        assertTrue("Roles that aren't Mr D's colours:\n" + strays.joinToString("\n"), strays.isEmpty())
    }

    /** The Color properties of [receiver]. Color is a value class, so its getters return the packed value as a long. */
    private fun colours(getters: Array<Method>, receiver: Any): Map<String, Color> = getters
        .filter { it.parameterCount == 0 && it.returnType == java.lang.Long.TYPE && it.name.startsWith("get") }
        .associate {
            it.name.removePrefix("get").substringBefore('-') to Color((it.invoke(receiver) as Long).toULong())
        }
}
