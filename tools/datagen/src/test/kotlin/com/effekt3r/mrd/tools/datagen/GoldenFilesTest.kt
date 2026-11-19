package com.effekt3r.mrd.tools.datagen

import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The committed mock API files must be exactly what the generator writes, byte for byte: nobody edits them by hand,
 * and a generator change can't land without the data it produces. tools/datagen/build.gradle.kts declares mock-api/
 * as an input of this test, so Gradle re-runs it whenever a committed file changes.
 */
class GoldenFilesTest {

    private val pool = ImagePool.load()

    @Test
    fun restaurantsJsonIsUpToDate() = assertUpToDate(Dataset.STANDARD)

    @Test
    fun restaurants10kJsonIsUpToDate() = assertUpToDate(Dataset.STRESS)

    private fun assertUpToDate(dataset: Dataset) {
        // Gradle runs the tests in the module's directory, tools/datagen.
        val committed = File("../..", dataset.path)
        val regenerate = "Run ./gradlew :tools:datagen:run and commit the result."
        assertTrue("${dataset.path} is missing. $regenerate", committed.isFile)
        assertArrayEquals(
            "${dataset.path} isn't what the generator writes. $regenerate",
            dataset.render(pool).toByteArray(),
            committed.readBytes()
        )
    }
}
