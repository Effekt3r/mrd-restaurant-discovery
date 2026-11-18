pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android(\\..*)?")
                includeGroupByRegex("com\\.google(\\..*)?")
                includeGroupByRegex("androidx(\\..*)?")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android(\\..*)?")
                includeGroupByRegex("com\\.google(\\..*)?")
                includeGroupByRegex("androidx(\\..*)?")
            }
        }
        mavenCentral()
    }
}

// Module layering, enforced for every project: features never depend on features, and core modules never
// depend on features or :app. Only :app connects features.
gradle.lifecycle.beforeProject {
    val self = path
    configurations.configureEach {
        val configurationName = name
        dependencies.withType<ProjectDependency>().configureEach {
            val target = path
            val featureToFeature = self.startsWith(":feature:") && target.startsWith(":feature:")
            val coreReachesUp = self.startsWith(":core:") && (target.startsWith(":feature:") || target == ":app")
            if (target != self && (featureToFeature || coreReachesUp)) {
                throw GradleException(
                    "$self must not depend on $target (declared in '$configurationName'). " +
                        "Features depend only on :core modules, and only :app connects features."
                )
            }
        }
    }
}

rootProject.name = "mrd-restaurant-discovery"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":core:common")
include(":core:model")
include(":core:data")
include(":core:designsystem")
include(":core:network")
include(":core:testing")
include(":core:ui")
include(":feature:browse")
include(":feature:details")
include(":app")
