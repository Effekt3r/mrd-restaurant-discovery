package com.effekt3r.mrd.tools.datagen

import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The committed mock API files must be exactly what the generator writes, byte for byte: nobody edits them by hand,
 * and a generator change can't land without the data it produces. tools/datagen/build.gradle.kts declares mock-api/v1/
 * as an input of this test, so Gradle re-runs it whenever a committed file changes, menus included.
 */
class GoldenFilesTest {

    private val pool = ImagePool.load()
    private val regenerate = "Run ./gradlew :tools:datagen:run and commit the result."

    @Test
    fun restaurantsJsonIsUpToDate() = assertUpToDate(Dataset.STANDARD)

    @Test
    fun restaurants10kJsonIsUpToDate() = assertUpToDate(Dataset.STRESS)

    @Test
    fun menusAreUpToDateAndNoOtherFileIsThere() {
        val expected = MenuGenerator.files(pool)
        val directory = File("../..", MenuGenerator.DIRECTORY)
        // Hidden files such as .DS_Store are the operating system's, and git ignores them.
        val committed = directory.list().orEmpty().filterNot { it.startsWith(".") }.sorted()
        assertEquals("${MenuGenerator.DIRECTORY}/ has other files. $regenerate", expected.keys.sorted(), committed)
        expected.forEach { (name, text) ->
            assertArrayEquals(
                "${MenuGenerator.DIRECTORY}/$name isn't what the generator writes. $regenerate",
                text.toByteArray(),
                File(directory, name).readBytes()
            )
        }
    }

    private fun assertUpToDate(dataset: Dataset) {
        // Gradle runs the tests in the module's directory, tools/datagen.
        val committed = File("../..", dataset.path)
        assertTrue("${dataset.path} is missing. $regenerate", committed.isFile)
        assertArrayEquals(
            "${dataset.path} isn't what the generator writes. $regenerate",
            dataset.render(pool).toByteArray(),
            committed.readBytes()
        )
    }
}
