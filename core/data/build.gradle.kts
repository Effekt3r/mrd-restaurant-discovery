plugins {
    alias(libs.plugins.mrd.android.library)
    alias(libs.plugins.mrd.android.room)
    alias(libs.plugins.mrd.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// The mock API (spec §5.1). Debug builds read the dev branch (staging) and release builds master (production);
// -Pmrd.apiBaseUrl overrides both. -Pmrd.dataset=10k makes debug builds read the 10,000-restaurant stress file.
// They live here, not in :core:network, because the endpoint is discovery's backend contract: a host app adopting
// these modules keeps its own networking stack but not our API's address.
fun apiBaseUrl(branch: String): String {
    val override = providers.gradleProperty("mrd.apiBaseUrl").orNull
    if (override != null && !(override.startsWith("https://") && override.endsWith("/"))) {
        throw GradleException("mrd.apiBaseUrl must be an https:// URL ending in /, not '$override'.")
    }
    return override ?: "https://raw.githubusercontent.com/Effekt3r/mrd-restaurant-discovery/$branch/mock-api/"
}

val debugRestaurantsFile = when (val dataset = providers.gradleProperty("mrd.dataset").orNull) {
    null -> "restaurants.json"
    "10k" -> "restaurants-10k.json"
    else -> throw GradleException("mrd.dataset can only be 10k, not '$dataset'.")
}

android {
    namespace = "com.effekt3r.mrd.core.data"
    buildFeatures.buildConfig = true
    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl("dev")}\"")
            buildConfigField("String", "RESTAURANTS_PATH", "\"v1/$debugRestaurantsFile\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl("master")}\"")
            buildConfigField("String", "RESTAURANTS_PATH", "\"v1/restaurants.json\"")
        }
    }
}

dependencies {
    api(projects.core.common)
    api(projects.core.model)
    api(libs.androidx.paging.common)
    implementation(projects.core.network)
    implementation(libs.androidx.room.paging)
    implementation(libs.retrofit)

    testImplementation(projects.core.testing)
    testImplementation(libs.androidx.paging.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
}

// RestaurantParserTest and StalenessMeasurementTest read the committed mock API files from disk, which Gradle can't see.
// Declaring them makes Gradle re-run the tests when a dataset changes, instead of reporting UP-TO-DATE.
tasks.withType<Test>().configureEach {
    inputs.files(rootDir.resolve("mock-api/v1/restaurants.json"), rootDir.resolve("mock-api/v1/restaurants-10k.json"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("mockApiDatasets")
}
