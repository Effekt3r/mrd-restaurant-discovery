package com.effekt3r.mrd.tools.datagen

import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * The generator's entry point, run from the repository root by Gradle. `./gradlew :tools:datagen:run` writes both
 * datasets from the committed image pool, with no network. `--args=fetch-images` refreshes the pool from TheMealDB,
 * the only command that needs the network.
 */
fun main(args: Array<String>) {
    check(File("tools/datagen").isDirectory) {
        "Run the generator from the repository root, through Gradle: ./gradlew :tools:datagen:run"
    }
    when (args.singleOrNull()) {
        "generate" -> generate()
        "fetch-images" -> fetchImages()
        else -> error("Unknown command '${args.joinToString(" ")}'. Use generate (the default) or fetch-images.")
    }
}

private fun generate() {
    val pool = ImagePool.load()
    Dataset.entries.forEach { dataset ->
        val text = dataset.render(pool)
        File(dataset.path).apply { parentFile.mkdirs() }.writeText(text)
        println("Wrote ${dataset.path}: ${dataset.size} restaurants, ${text.toByteArray().size} bytes")
    }
}

private fun fetchImages() {
    val text = ImagePoolFetcher.fetch(::httpGet)
    File(ImagePool.SOURCE_PATH).writeText(text)
    ImagePool.parse(text).photos.forEach { (key, photos) -> println("$key: ${photos.size} photos") }
    println("Wrote ${ImagePool.SOURCE_PATH}")
}

private val client: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()

/** GETs [url] and returns the body. Anything but HTTP 200 stops the run, so a partial pool is never written. */
private fun httpGet(url: String): String {
    val request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    check(response.statusCode() == 200) { "GET $url returned HTTP ${response.statusCode()}" }
    return response.body()
}
