# Mr D Restaurant Discovery

A restaurant discovery mini-app for the Mr D Senior Android Engineer take-home. You can browse nearby restaurants, search by name, filter to what's open now and open a restaurant's details, and it works offline. It's written in Kotlin and Jetpack Compose, with multiple modules, and built to slot into a large app maintained by several squads.

> **Status:** milestone 1 (foundation) is done. The project builds, the app shell navigates between placeholder screens, and CI gates every pull request into `dev` and `master`. The plan and progress are in [`docs/plans/`](docs/plans/).

## Requirements

| Tool | Version | Notes |
|---|---|---|
| Android Studio | Rabbit 2 (2026.2.2) or newer | Opens AGP 9.3 projects. |
| JDK | 17 or newer (21 recommended) | Runs Gradle. Android Studio's bundled JDK works. |
| Android SDK | Platform 36 and Build-Tools 36 | Install them with the SDK Manager. |
| Device or emulator | Android 8.0 (API 26) or newer | Gesture or three-button navigation. |
| Gradle | 9.7.1 | Comes with the wrapper; there's nothing to install. |
| gitleaks | 8.x (optional) | Scans each commit for secrets (`brew install gitleaks`). |

## Getting started

Until the first release reaches `master` (milestone 2), the code is on `dev`, so clone that branch. On the command line, Gradle also needs to know where the Android SDK is: set `ANDROID_HOME`, or put `sdk.dir=<path to the SDK>` in `local.properties`. That file is git-ignored, and Android Studio writes it for you when it opens the project.

```sh
git clone --branch dev https://github.com/Effekt3r/mrd-restaurant-discovery.git
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

- **Proven over newest.** The toolchain (Gradle 9.7, AGP 9.3, Kotlin 2.3, KSP 2.3, Hilt 2.59) is the combination Google's [Now in Android](https://github.com/android/nowinandroid) builds with. Libraries are stable releases that had been out for at least four weeks when the project started, and that work with compile SDK 36 and Kotlin 2.3. Most are the newest such release; Activity (1.12) and Core (1.17) are a minor version behind.
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

## Project structure

| Module | Role | Would be owned by |
|---|---|---|
| `:app` | Host shell: Application, MainActivity, navigation graph, R8 and signing | Mobile Platform |
| `:feature:browse` | Browse screen | Discovery squad |
| `:feature:details` | Details screen | Discovery squad |
| `:core:data` | Restaurant API, Room database, repositories, sync | Discovery squad |
| `:core:model` | Domain types (plain Kotlin) | Discovery squad |
| `:core:ui` | Restaurant UI shared by both features | Discovery squad |
| `:core:network` | Shared HTTP client, JSON and connectivity | Mobile Platform |
| `:core:common` | Dispatchers and clock (plain Kotlin) | Mobile Platform |
| `:core:designsystem` | Mr D tokens and components | Design Platform |
| `:core:testing` | Test utilities | Mobile Platform |
| `build-logic/` | Convention plugins (`mrd.*`) | Mobile Platform |

Features never depend on each other. The build enforces it, and so do the architecture tests.

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
