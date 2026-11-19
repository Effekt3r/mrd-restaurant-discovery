# Mr D Restaurant Discovery

A restaurant discovery mini-app for the Mr D Senior Android Engineer take-home. You can browse nearby restaurants, search by name, filter to what's open now and open a restaurant's details, and it works offline. It's written in Kotlin and Jetpack Compose, with multiple modules, and built to slot into a large app maintained by several squads.

> **Status:** milestones 1 to 6 are done. Browse lists the [mock API](#mock-api)'s restaurants from an offline-first cache, with search, Open now and every loading, empty and error state; Details shows a restaurant's hours and menu and opens from a [deep link](#deep-link). CI gates every pull request into `dev` and `master`. Milestone 7 brings the signed release and the remaining documentation. The plan and progress are in [`docs/plans/`](docs/plans/).

## Requirements

| Tool | Version | Notes |
|---|---|---|
| Android Studio | Rabbit 2 (2026.2.2) or newer | Opens AGP 9.3 projects. |
| JDK | 17 or newer (21 recommended) | Runs Gradle. Android Studio's bundled JDK works. |
| Android SDK | Platform 37 and Build-Tools 36 | Install them with the SDK Manager. The app compiles against API 37 and targets API 36. |
| Device or emulator | Android 8.0 (API 26) or newer | Gesture or three-button navigation. |
| Gradle | 9.7.1 | Comes with the wrapper; there's nothing to install. |
| gitleaks | 8.x (optional) | Scans each commit for secrets (`brew install gitleaks`). |

## Getting started

On the command line, Gradle needs to know where the Android SDK is: set `ANDROID_HOME`, or put `sdk.dir=<absolute path to the SDK>` in `local.properties` (Gradle doesn't expand `~`). That file is git-ignored, and Android Studio writes it for you when it opens the project.

```sh
git clone https://github.com/Effekt3r/mrd-restaurant-discovery.git
cd mrd-restaurant-discovery
git config core.hooksPath .githooks   # optional: secret scan on every commit
./gradlew installDebug                # build and install on a connected device or emulator
```

Or open the folder in Android Studio and run the `app` configuration.

| What | Command |
|---|---|
| Unit, Robolectric and architecture tests | `./gradlew testDebugUnitTest` |
| Formatting check (fix with `spotlessApply`) | `./gradlew spotlessCheck` |
| Lint | `./gradlew :app:lintDebug` |
| R8-minified release build | `./gradlew :app:assembleRelease` |

## Toolchain and version policy

- **Proven over newest.** The toolchain (Gradle 9.7, AGP 9.3, Kotlin 2.3, KSP 2.3, Hilt 2.59) is the combination Google's [Now in Android](https://github.com/android/nowinandroid) builds with. When the project started, libraries were stable releases that had been out for at least four weeks and worked with compile SDK 36 and Kotlin 2.3. Dependabot's androidx updates of 2026-11-19 (Compose BOM 2026.09, Core 1.19, Navigation 2.10 and androidx.hilt 1.4) need compile SDK 37, so the app now compiles against API 37 but still targets API 36: the [target SDK](https://developer.android.com/build#android_sdk_settings), not the compile SDK, decides which platform behaviour the app gets.
- **Why.** A time-boxed build has no room for toolchain surprises, and a large app updates on purpose. A throwaway spike build proved the toolchain before any project code was written. The full list is in the [roadmap](docs/plans/2026-11-18-00-roadmap.md).
- **Updates.** From the first release on, Dependabot proposes library updates weekly once a release is at least 30 days old, and GitHub Actions updates after 7 days.

## Signing

Release builds are R8-minified. The release signing key comes from Gradle properties or environment variables, never from the repository:

| Property | Meaning |
|---|---|
| `MRD_KEYSTORE_FILE` | Path to the upload keystore |
| `MRD_KEYSTORE_PASSWORD` | Keystore password |
| `MRD_KEY_ALIAS` | Key alias |
| `MRD_KEY_PASSWORD` | Key password |

Without them, a local release build is signed with the debug key, and Gradle prints a warning. That way anyone can install and test an R8 build. The release workflow, which arrives in milestone 7, will require real signing and fail without it.

- Use an absolute path for `MRD_KEYSTORE_FILE`: a relative path resolves against `app/`.
- Only the release workflow will pass `-Pmrd.requireReleaseSigning=true`, because it makes any build without a keystore fail.
- It will pass the signing secrets as environment variables, not `-P` arguments, which would show up in process listings.

## Mock API

The app's restaurants come from static JSON in this repository, served over HTTPS by `raw.githubusercontent.com`. The `dev` branch plays the part of staging and `master` of production: debug builds read `dev` and release builds `master`. `./gradlew installDebug -Pmrd.dataset=10k` installs a debug build that reads the stress file, and `-Pmrd.apiBaseUrl=<an https URL ending in />` points a build at another copy of `mock-api/`.

| File | Restaurants | Use |
|---|---|---|
| [`mock-api/v1/restaurants.json`](mock-api/v1/restaurants.json) | 220 | What the app shows |
| [`mock-api/v1/restaurants-10k.json`](mock-api/v1/restaurants-10k.json) | 10,000 | The stress test; its first 220 are the same restaurants |
| [`mock-api/v1/menus/`](mock-api/v1/menus/) | 218 menus, one per restaurant in `restaurants.json` | Details; stress-only restaurants have none (404) |

The data is fictional and deliberately messy, so the app's fallbacks get exercised. `./gradlew :tools:datagen:run` regenerates it, and a test fails if the committed files differ from what the generator writes. [`mock-api/README.md`](mock-api/README.md) has the endpoints, the edge cases and the image credits.

## Deep link

`mrd://restaurant/{id}` opens that restaurant's Details, with Browse underneath, whether the app is running or not:

```sh
adb shell am start -W -a android.intent.action.VIEW -d "mrd://restaurant/r-1001" com.effekt3r.mrd
```

An id the app doesn't know shows "Restaurant not found". Opened offline before the app has ever loaded its restaurants, Details says so and offers Retry.

## Project structure

| Module | Role | Would be owned by |
|---|---|---|
| `:app` | Host shell: Application, MainActivity, navigation graph, R8 and signing | Mobile Platform |
| `:feature:browse` | Browse screen | Discovery squad |
| `:feature:details` | Details screen | Discovery squad |
| `:core:data` | Restaurant API, Room database, repositories, sync | Discovery squad |
| `:core:model` | Domain types (plain Kotlin) | Discovery squad |
| `:core:ui` | Restaurant UI shared by both features | Discovery squad |
| `:core:network` | Shared HTTP client, JSON, the image loader and connectivity | Mobile Platform |
| `:core:common` | Dispatchers and clock (plain Kotlin) | Mobile Platform |
| `:core:designsystem` | Mr D tokens and components | Design Platform |
| `:core:testing` | Test utilities | Mobile Platform |
| `build-logic/` | Convention plugins (`mrd.*`) | Mobile Platform |
| `tools/datagen` | Build-time generator for the mock API's data; never ships in the app | Discovery squad |
| `mock-api/` | The mock API's JSON, served by `raw.githubusercontent.com` | Discovery squad |

Features never depend on each other. The build enforces it, and so do the architecture tests.

## Credits

- **Figtree**, the app's typeface, is by Erik Kennedy and the Figtree Project Authors, under the [SIL Open Font License 1.1](core/designsystem/FIGTREE-OFL.txt). The app bundles the variable font from [google/fonts](https://github.com/google/fonts/tree/main/ofl/figtree).
- **Icons** are Google's [Material Symbols](https://fonts.google.com/icons) (rounded), under the Apache License 2.0.
- **Food photos** in the mock API are from [TheMealDB](https://www.themealdb.com/api.php); see [`mock-api/README.md`](mock-api/README.md).

## Documentation

- [Design spec](docs/specs/2026-11-18-restaurant-discovery-design.md). Its §14 links every technology to its official documentation.
- [Roadmap and milestone plans](docs/plans/)
- [Architecture-test probes](tools/architecture-probes/README.md): proof that each house rule fails when it's broken
- [How we work](CONTRIBUTING.md) · [Security](SECURITY.md)

| Technology | Official documentation |
|---|---|
| Modularization | https://developer.android.com/topic/modularization |
| Convention plugins | https://docs.gradle.org/current/userguide/sharing_build_logic_between_subprojects.html |
| Version catalogs | https://developer.android.com/build/migrate-to-catalogs |
| Jetpack Compose | https://developer.android.com/develop/ui/compose/documentation |
| Navigation (type safety) | https://developer.android.com/guide/navigation/design/type-safety |
| Hilt | https://developer.android.com/training/dependency-injection/hilt-android |
| Edge-to-edge (window insets) | https://developer.android.com/develop/ui/compose/system/insets |
| Predictive back | https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture |
| R8 | https://developer.android.com/topic/performance/app-optimization/enable-app-optimization |
| App signing | https://developer.android.com/studio/publish/app-signing |
| Network security config | https://developer.android.com/privacy-and-security/security-config |
| Konsist | https://docs.konsist.lemonappdev.com/ |
| kotlinx.serialization (JSON) | https://kotlinlang.org/docs/serialization.html |
| Gradle application plugin (the data generator) | https://docs.gradle.org/current/userguide/application_plugin.html |
| TheMealDB API (food photos) | https://www.themealdb.com/api.php |
| Custom design systems (theming Material 3) | https://developer.android.com/develop/ui/compose/designsystems/custom |
| Fonts and variable fonts | https://developer.android.com/develop/ui/compose/text/fonts |
| Material Symbols in Compose | https://developer.android.com/develop/ui/compose/graphics/images/material |
| Adaptive icons | https://developer.android.com/develop/ui/views/launch/icon_design_adaptive |
| Accessibility in Compose | https://developer.android.com/develop/ui/compose/accessibility |
| Paging (paged data in Compose) | https://developer.android.com/topic/libraries/architecture/paging/v3-paged-data |
| Lazy lists and grids | https://developer.android.com/develop/ui/compose/lists |
| Text fields (`TextFieldState`) | https://developer.android.com/develop/ui/compose/text/user-input |
| Pull to refresh | https://developer.android.com/develop/ui/compose/components/pull-to-refresh |
| Nested scrolling (the header that slides away) | https://developer.android.com/develop/ui/compose/touch-input/pointer-input/scroll |
| Saved state in ViewModels | https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate |
| Lifecycle-aware coroutines | https://developer.android.com/topic/libraries/architecture/coroutines |
| Loading images (Coil) | https://developer.android.com/develop/ui/compose/graphics/images/loading |
| Deep links | https://developer.android.com/guide/navigation/design/deep-link |
| Testing Compose | https://developer.android.com/develop/ui/compose/testing |
| Testing flows (Turbine) | https://developer.android.com/kotlin/flow/test |
