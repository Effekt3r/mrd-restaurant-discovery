# Milestone 4: design system, implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mr D's look on the phone, early:
- `:core:designsystem` with Mr D's tokens (colours for light and dark, Figtree, shapes and spacing), `MrdTheme`, and every component the screens need, including the restaurant card's and the menu row's visuals;
- an original launcher icon, and `MrdTheme` in `:app`;
- the Browse and Details placeholders restyled with it. Browse gets the real header and four sample restaurants, and Details a sample menu. Milestones 5 and 6 replace both.

**Architecture:**
- **Material 3, themed.** `MrdTheme` wraps `MaterialTheme` with Mr D's colour schemes and Figtree. `MrdTheme.colors` adds the roles Material has no slot for: links, Open, Closed and photo placeholders. Material's own components then look like Mr D's without wrappers.
- **Tokens are the only source of colour.** Raw values live in the internal `MrdPalette`. `ContrastTest` proves every text pair in both themes. `colors.xml` repeats three of them for XML (the launcher icon and the window background), and `ColorResourcesTest` keeps the two in step.
- **Components are stateless.** They take display-ready text, enums and slots, never `:core:model` types: milestone 5's `:core:ui` formats the model and calls them. Each file has previews at 360 dp in light, dark and 200% font (`@MrdPreviews`).
- **Assets are downloaded, never drawn by hand:** Figtree's variable font and its licence from `google/fonts`, and nine Material Symbols icons from `google/material-design-icons`. Each comes from a pinned commit and is checked by SHA-256.
- **Resource prefix.** `:core:designsystem` is the first core module with resources, so it gets `resourcePrefix = "designsystem_"`, as each feature has its own.

**Tech stack:** Kotlin 2.3.21, Jetpack Compose (BOM 2026.06.01: UI and Foundation 1.11.4, Material 3 1.4.0), Figtree (SIL Open Font License 1.1), Material Symbols (Apache 2.0), adaptive icons, JUnit 4.13.2. No new libraries.

**Spec:** `docs/specs/2026-11-18-restaurant-discovery-design.md`, §6.1, §6.2, §6.3, §6.5, §6.6 and §11 (AC-18 and AC-19, for the components). The menu components follow §5.9. **Roadmap:** `docs/plans/2026-11-18-00-roadmap.md`.

**Branch:** `feature/design-system`, cut from `dev`, with this plan as its first commit. It ends in one pull request into `dev`. Run every command from the repository root.

---

## Spike results

A throwaway spike built this plan's code in a disposable clone on 2026-11-19 and ran it on the `mrd_api36` emulator (API 36, 1080 × 2400 pixels at 420 dpi, so 411 dp wide). The code blocks below are copied from it. The final state was built, tested and walked; the commits in between weren't replayed one by one, so their checks are stated as conditions.
- **Figtree:** `google/fonts` ships only the variable font, `Figtree[wght].ttf`: 62,712 bytes, with weights from 300 to 900. That one file serves the five weights used (400 to 800), each a `Font` with its own weight setting. Compose 1.11.4 applies variable-font settings from API 26, the app's minimum: its `PlatformTypefaces` checks `SDK_INT >= 26`, then sets the weight with `Paint.setFontVariationSettings` (read from its bytecode). It still marks the API `@ExperimentalTextApi`, so one private function opts in, as Google's fonts guide does. On the emulator the weights render distinctly.
- **APK size** (release, R8): 979,364 bytes on `dev`, 1,422,296 after this plan (+442,932).
  - The font adds 35,171 bytes compressed, and the icons and launcher drawables about 5 KB.
  - Most of the rest is code that the restyled placeholders use for the first time and milestone 5 needs anyway. `classes.dex` grows by 349,481 bytes (Compose's text field, lazy grid, `FlowRow` and toggles) and `resources.arsc` by 41,240 (those libraries' translations).
  - An English-only locale filter would save another 97,872 bytes. It isn't in this plan.
- **Contrast** (WCAG 2.2). Text needs 4.5:1, and the outline of a chip that's off needs 3:1 (non-text contrast).

  | Pair | Light | Dark |
  |---|---|---|
  | Text on the screen | 16.9 | 16.9 |
  | Secondary text | 5.9 | 8.8 |
  | Text in the search field | 14.1 | 13.1 |
  | The search field's placeholder | 4.9 | 6.8 |
  | A chip that's on, and buttons | 16.9 | 16.9 |
  | Ink on sky (tags, the offline banner) | 9.6 | 9.6 |
  | Errors | 6.5 | 7.4 |
  | Links | 5.2 | 12.4 |
  | Open | 5.3 | 9.8 |
  | Closed and Sold out | 6.5 | 7.4 |
  | The off chip's outline | 3.3 | 4.7 |

  Sky as text on white is 1.8:1, so sky is only ever a fill behind ink text.
- **Tests:** 5 in `:core:designsystem`, `ContrastTest` (4) and `ColorResourcesTest` (1), taking 0.04 seconds. Both bite:
  - a lighter `InkMuted` fails `ContrastTest` and names both failing pairs;
  - after an edit to `colors.xml`, Gradle re-runs `ColorResourcesTest` without `--rerun`, and it fails. No extra input declaration is needed: the library convention includes Android resources in unit tests, which makes the merged resources an input of the test task.
- **Gate:** `spotlessCheck :app:lintDebug testDebugUnitTest :app:assembleRelease` passed in 47 seconds with a warm daemon, and R8 reported no missing rules. Lint went from 1 warning (`MissingApplicationIcon`) to `No issues found.`
- **Emulator:** Browse and Details in light, dark and landscape, and at 200% font on a 360 dp-wide screen (`wm density 480`). Nothing overflows: the details row wraps between its parts. The components that no placeholder shows (the offline banner, the state screen and the skeletons) were checked the same way on a throwaway screen. `uiautomator dump` shows:
  - each card as one clickable node whose description follows spec §6.5, such as "Cape Town Grill. Steakhouse, Grill. Rated 4.6. 32 minutes. R15 delivery. Open.";
  - no separate node for a card's heart;
  - the Open now chip as a checkable node 126 pixels (48 dp) tall.
- **What the spike changed**, each already in the code below:
  - **Icons.** Deleting each icon's `android:tint` line closed `arrow_back`'s tag early, because its `android:autoMirrored` attribute comes after the tint. The file still parsed as XML, but lint's `ExtraText` failed the gate. The download now removes the attribute, not the line.
  - **Insets.** `WindowInsets.safeDrawing` is a composable getter, so a plain `Modifier` extension can't use it. Details pads its list instead.
  - **Status bar.** Lists scrolled under the transparent status bar, behind its icons: on Browse always, and on Details at 200% font. A surface-coloured strip now sits behind the icons, on Details only once its header has scrolled away.
  - **Plurals.** Lint's `PluralsCandidate` flagged `R%1$d delivery`, so the sample fee is passed as text (`R%1$s delivery`).
  - **Slots.** A lambda inside an `if` isn't inferred as `@Composable`, so Details builds its photo slot with a typed helper.

---

## Before you start

- **JDK 21:** on the `PATH` or in `JAVA_HOME` (`export JAVA_HOME=$(/usr/libexec/java_home -v 21)` on macOS).
- **Android SDK:** platform 36 and build-tools 36. Either `ANDROID_HOME` is set, or a git-ignored `local.properties` contains `sdk.dir=<absolute path to the SDK>`.
- **Network:** Tasks 1 and 2 download the font, its licence and the icons from `raw.githubusercontent.com`, at pinned commits. Nothing else needs the network.
- **Versions:** don't change any. This milestone adds no library: Compose Foundation and Material 3 are already `api` dependencies of `:core:designsystem`.
- **Formatting:** run `./gradlew spotlessApply` before every commit. The code below is already formatted.
- **Commits** have a conventional subject, the reason in the body and a `Verified:` line, and Franco is the author. Add no AI trailers: no `Co-Authored-By` or `Claude-Session` lines.
- **Device:** an emulator or phone on API 26 or newer, for Tasks 4 to 6 and 8. If more than one device is connected, add `-s <serial>` to every `adb` command. Put back every setting you change: dark mode, font scale, density and rotation.
- **Tests:** `./gradlew :core:designsystem:testDebugUnitTest` runs this module's tests, and `./gradlew testDebugUnitTest` includes them. This counts the results:

  ```bash
  cat core/designsystem/build/test-results/testDebugUnitTest/*.xml | grep -o '<testsuite [^>]*' \
    | sed -E 's/.*\.([A-Za-z]+)" tests="([0-9]+)" skipped="[0-9]+" failures="([0-9]+)" errors="([0-9]+)".*/\1 \2 \3 \4/' \
    | awk '{t+=$2; f+=$3+$4; print} END {print "total:", t, "tests,", f, "failed"}'
  ```
- **House rules:**
  - raw colours only in `:core:designsystem` (`ArchitectureTest`);
  - every word the user sees in a string resource. The sample restaurants' names and cuisines are data, as the API sends them, so they're Kotlin values; the words around them are resources;
  - every resource in `:core:designsystem` starts with `designsystem_`, or lint fails.
- **Placeholders:** Browse and Details stay placeholders until milestones 5 and 6, and their KDoc says so.

## File map

```
core/designsystem/build.gradle.kts                    (modified) the designsystem_ resource prefix
core/designsystem/FIGTREE-OFL.txt                     Figtree's licence, from google/fonts
core/designsystem/src/main/res/
  font/designsystem_figtree.ttf                       Figtree, the variable font (downloaded)
  drawable/designsystem_ic_*.xml                      nine Material Symbols icons (downloaded)
  values/strings.xml                                  the components' words
  values/colors.xml                                   three palette colours, for XML
core/designsystem/src/main/kotlin/com/effekt3r/mrd/core/designsystem/
  theme/Color.kt, Type.kt, Dimens.kt, Theme.kt        tokens and MrdTheme
  icon/MrdIcons.kt                                    the icons
  preview/MrdPreviews.kt                              light, dark and 200% font previews at 360 dp
  component/SearchField.kt, Pills.kt, RestaurantInfo.kt, CircleButtons.kt   the controls
  component/RestaurantCard.kt, Menu.kt, Skeletons.kt, StateScreen.kt, OfflineBanner.kt
core/designsystem/src/test/kotlin/com/effekt3r/mrd/core/designsystem/theme/
  ContrastTest.kt, ColorResourcesTest.kt
app/src/main/res/mipmap-anydpi/ic_launcher.xml        the adaptive launcher icon
app/src/main/res/drawable/ic_launcher_foreground.xml, ic_launcher_monochrome.xml
app/src/main/AndroidManifest.xml                      (modified) the icon
app/src/main/res/values/themes.xml, values-night/themes.xml   (modified) the window background
app/src/main/kotlin/com/effekt3r/mrd/MainActivity.kt  (modified) MrdTheme
feature/browse/src/main/kotlin/com/effekt3r/mrd/feature/browse/
  BrowseScreen.kt (modified), SampleRestaurants.kt    the restyled placeholder and its sample data
feature/details/src/main/kotlin/com/effekt3r/mrd/feature/details/RestaurantScreen.kt   (modified)
feature/{browse,details}/src/main/res/values/strings.xml   (modified)
README.md, CLAUDE.md, docs/specs/2026-11-18-restaurant-discovery-design.md, docs/ai/corrections.md   (modified)
docs/qa/<date>-m4-design-system.md, docs/qa/screenshots/m4-*.png   verification
```

---

### Task 1: Tokens, Figtree and MrdTheme

**Files:**
- Modify: `core/designsystem/build.gradle.kts`
- Create (downloaded): `core/designsystem/src/main/res/font/designsystem_figtree.ttf`, `core/designsystem/FIGTREE-OFL.txt`
- Create: `core/designsystem/src/main/kotlin/com/effekt3r/mrd/core/designsystem/theme/Color.kt`, `Type.kt`, `Dimens.kt`, `Theme.kt`
- Test: `core/designsystem/src/test/kotlin/com/effekt3r/mrd/core/designsystem/theme/ContrastTest.kt`

- [ ] **Step 1: Check out the branch.** The controller created it from `dev`, with this plan as its first commit.

```bash
git fetch origin && git switch feature/design-system
git log -1 --format=%s
```

Expected: the subject of the commit that added this plan.

- [ ] **Step 2: Give the module its resource prefix.** Replace `core/designsystem/build.gradle.kts` with:

```kotlin
plugins {
    alias(libs.plugins.mrd.android.library)
    alias(libs.plugins.mrd.android.compose)
}

android {
    namespace = "com.effekt3r.mrd.core.designsystem"
    // Resource names from every module merge into one namespace; lint fails names without this prefix.
    resourcePrefix = "designsystem_"
}

dependencies {
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.material3)
}
```

- [ ] **Step 3: Download Figtree and its licence** from `google/fonts`, pinned to the commit that last changed `ofl/figtree`:

```bash
FONTS=https://raw.githubusercontent.com/google/fonts/a60a77e14f28abd4ef243a1b5dfc48df0cec5205/ofl/figtree
mkdir -p core/designsystem/src/main/res/font
curl -sSfL -o core/designsystem/src/main/res/font/designsystem_figtree.ttf "$FONTS/Figtree%5Bwght%5D.ttf"
curl -sSfL -o core/designsystem/FIGTREE-OFL.txt "$FONTS/OFL.txt"
shasum -a 256 core/designsystem/src/main/res/font/designsystem_figtree.ttf core/designsystem/FIGTREE-OFL.txt
```

Expected:

```
26ad3db9b31ff7dde67a91ff515d022d2f495cd506590699cf264f0bfe6fb714  core/designsystem/src/main/res/font/designsystem_figtree.ttf
140d37233e7f3ce7313798befa9600893bcceaf41a55fa0fa5ad52f7f657a268  core/designsystem/FIGTREE-OFL.txt
```

If either hash differs, stop and report it.

- [ ] **Step 4: Write the failing test.** Create `core/designsystem/src/test/kotlin/com/effekt3r/mrd/core/designsystem/theme/ContrastTest.kt`:

```kotlin
package com.effekt3r.mrd.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WCAG 2.2 contrast in both themes: at least 4.5:1 for every text colour on the background the design system puts it
 * on (spec section 6.5), and 3:1 for the outline that marks a chip that's off (non-text contrast).
 */
class ContrastTest {

    @Test
    fun textMeetsAaInTheLightTheme() = assertTextContrast(LightColorScheme, LightMrdColors)

    @Test
    fun textMeetsAaInTheDarkTheme() = assertTextContrast(DarkColorScheme, DarkMrdColors)

    @Test
    fun theOffChipOutlineIsVisibleInBothThemes() {
        assertTrue(contrast(LightColorScheme.outline, LightColorScheme.surface) >= 3.0)
        assertTrue(contrast(DarkColorScheme.outline, DarkColorScheme.surface) >= 3.0)
    }

    @Test
    fun skyFailsAsTextOnWhiteSoItIsOnlyEverAFill() {
        // Spec section 6.1: about 1.8:1. That's why no text colour in either scheme is sky.
        assertTrue(contrast(MrdPalette.Sky, MrdPalette.White) < 2.0)
    }

    private fun assertTextContrast(scheme: ColorScheme, mrd: MrdColors) {
        val pairs = mapOf(
            "text on the screen" to (scheme.onSurface to scheme.surface),
            "secondary text on the screen" to (scheme.onSurfaceVariant to scheme.surface),
            "text in the search field" to (scheme.onSurface to scheme.surfaceVariant),
            "the search field's placeholder" to (scheme.onSurfaceVariant to scheme.surfaceVariant),
            "a chip that's on, and buttons" to (scheme.onPrimary to scheme.primary),
            "tags and the offline banner (sky)" to (scheme.onPrimaryContainer to scheme.primaryContainer),
            "errors" to (scheme.onError to scheme.error),
            "links" to (mrd.link to scheme.surface),
            "Open" to (mrd.open to scheme.surface),
            "Closed and Sold out" to (mrd.closed to scheme.surface)
        )
        val failures = pairs.mapNotNull { (use, colours) ->
            val ratio = contrast(colours.first, colours.second)
            if (ratio < 4.5) "$use: ${"%.2f".format(ratio)}:1" else null
        }
        assertTrue("Text below WCAG AA's 4.5:1:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    /** WCAG 2.2's contrast ratio, from 1:1 to 21:1. */
    private fun contrast(a: Color, b: Color): Double {
        val (lighter, darker) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun luminance(color: Color): Double {
        fun linear(channel: Float): Double =
            if (channel <= 0.04045f) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)
        return 0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)
    }
}
```

- [ ] **Step 5: Run it to see it fail**

```bash
./gradlew :core:designsystem:testDebugUnitTest
```

Expected: `BUILD FAILED`, starting with `e: …/theme/ContrastTest.kt:16:59 Unresolved reference 'LightColorScheme'.`

- [ ] **Step 6: Create `core/designsystem/src/main/kotlin/com/effekt3r/mrd/core/designsystem/theme/Color.kt`**

```kotlin
package com.effekt3r.mrd.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Mr D's colours, sampled from its Play Store listing (spec section 6.1), plus the tones derived from them. Sky as text
 * on white is about 1.8:1, so sky is only ever a fill behind ink text; links use [SkyDeep], sky's hue made dark
 * enough to read. ContrastTest proves every text pair and the chip outline in both themes.
 */
internal object MrdPalette {
    val Sky = Color(0xFF6DCFF6)
    val Ink = Color(0xFF1D1D1B)
    val White = Color(0xFFFFFFFF)
    val FieldGrey = Color(0xFFE7EBEE)

    /** Sky's hue and saturation at 33% lightness: links and text buttons on white. */
    val SkyDeep = Color(0xFF0A749E)

    /** Sky's hue at 82% lightness: links on ink, in the dark theme. */
    val SkyPale = Color(0xFFADE5FB)
    val InkMuted = Color(0xFF5C6670)
    val Outline = Color(0xFF858F98)
    val Green = Color(0xFF1E7B34)
    val Red = Color(0xFFB3261E)

    // The dark theme: ink surfaces, white text.
    val InkRaised = Color(0xFF2E3134)
    val MutedOnInk = Color(0xFFB4BCC4)
    val OutlineOnInk = Color(0xFF7F8891)
    val GreenOnInk = Color(0xFF7BD992)
    val RedOnInk = Color(0xFFFF8A80)

    /** Cuisine-coloured photo placeholders: pale tones on white, deep tones on ink. */
    val PlaceholdersLight = listOf(
        Color(0xFFD3F0FC),
        Color(0xFFFCE1CF),
        Color(0xFFD5F2DF),
        Color(0xFFE6DEF8),
        Color(0xFFF5E9C9),
        Color(0xFFF9D9E0)
    )
    val PlaceholdersDark = listOf(
        Color(0xFF1F4250),
        Color(0xFF4D3324),
        Color(0xFF22432D),
        Color(0xFF3A3152),
        Color(0xFF4A4127),
        Color(0xFF4B2A33)
    )
}

/** Colours Material's [ColorScheme] has no role for. Read them through [MrdTheme.colors]. */
@Immutable
data class MrdColors(
    /** Links and text buttons. */
    val link: Color,
    /** "Open" status text. */
    val open: Color,
    /** "Closed" status text and "Sold out". */
    val closed: Color,
    /** Photo placeholders, chosen per cuisine. */
    val imagePlaceholders: List<Color>,
    /** The food icon on a photo placeholder. */
    val onImagePlaceholder: Color
)

internal val LightColorScheme: ColorScheme = lightColorScheme(
    primary = MrdPalette.Ink,
    onPrimary = MrdPalette.White,
    primaryContainer = MrdPalette.Sky,
    onPrimaryContainer = MrdPalette.Ink,
    secondary = MrdPalette.SkyDeep,
    onSecondary = MrdPalette.White,
    background = MrdPalette.White,
    onBackground = MrdPalette.Ink,
    surface = MrdPalette.White,
    onSurface = MrdPalette.Ink,
    surfaceVariant = MrdPalette.FieldGrey,
    onSurfaceVariant = MrdPalette.InkMuted,
    surfaceTint = MrdPalette.White,
    surfaceContainerLowest = MrdPalette.White,
    surfaceContainerLow = MrdPalette.White,
    surfaceContainer = MrdPalette.White,
    surfaceContainerHigh = MrdPalette.White,
    surfaceContainerHighest = MrdPalette.FieldGrey,
    inverseSurface = MrdPalette.Ink,
    inverseOnSurface = MrdPalette.White,
    inversePrimary = MrdPalette.Sky,
    error = MrdPalette.Red,
    onError = MrdPalette.White,
    outline = MrdPalette.Outline,
    outlineVariant = MrdPalette.FieldGrey
)

internal val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = MrdPalette.White,
    onPrimary = MrdPalette.Ink,
    primaryContainer = MrdPalette.Sky,
    onPrimaryContainer = MrdPalette.Ink,
    secondary = MrdPalette.SkyPale,
    onSecondary = MrdPalette.Ink,
    background = MrdPalette.Ink,
    onBackground = MrdPalette.White,
    surface = MrdPalette.Ink,
    onSurface = MrdPalette.White,
    surfaceVariant = MrdPalette.InkRaised,
    onSurfaceVariant = MrdPalette.MutedOnInk,
    surfaceTint = MrdPalette.Ink,
    surfaceContainerLowest = MrdPalette.Ink,
    surfaceContainerLow = MrdPalette.Ink,
    surfaceContainer = MrdPalette.Ink,
    surfaceContainerHigh = MrdPalette.Ink,
    surfaceContainerHighest = MrdPalette.InkRaised,
    inverseSurface = MrdPalette.White,
    inverseOnSurface = MrdPalette.Ink,
    inversePrimary = MrdPalette.Ink,
    error = MrdPalette.RedOnInk,
    onError = MrdPalette.Ink,
    outline = MrdPalette.OutlineOnInk,
    outlineVariant = MrdPalette.InkRaised
)

internal val LightMrdColors = MrdColors(
    link = MrdPalette.SkyDeep,
    open = MrdPalette.Green,
    closed = MrdPalette.Red,
    imagePlaceholders = MrdPalette.PlaceholdersLight,
    onImagePlaceholder = MrdPalette.Ink.copy(alpha = 0.55f)
)

internal val DarkMrdColors = MrdColors(
    link = MrdPalette.SkyPale,
    open = MrdPalette.GreenOnInk,
    closed = MrdPalette.RedOnInk,
    imagePlaceholders = MrdPalette.PlaceholdersDark,
    onImagePlaceholder = MrdPalette.White.copy(alpha = 0.6f)
)

internal val LocalMrdColors = staticCompositionLocalOf { LightMrdColors }
```

- [ ] **Step 7: Create `core/designsystem/src/main/kotlin/com/effekt3r/mrd/core/designsystem/theme/Type.kt`**

```kotlin
package com.effekt3r.mrd.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.effekt3r.mrd.core.designsystem.R

/**
 * Figtree, bundled rather than downloaded so text renders offline (spec section 6.1). It's one variable font with a
 * weight axis from 300 to 900. Each weight below is the same file with a different weight setting, which Android
 * applies from API 26, the app's minimum.
 */
internal val Figtree = FontFamily(
    figtree(FontWeight.Normal),
    figtree(FontWeight.Medium),
    figtree(FontWeight.SemiBold),
    figtree(FontWeight.Bold),
    figtree(FontWeight.ExtraBold)
)

// Variable-font settings are still marked experimental in Compose 1.11; Google's fonts guide opts in the same way.
@OptIn(ExperimentalTextApi::class)
private fun figtree(weight: FontWeight) = Font(
    resId = R.font.designsystem_figtree,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

/** Material's type scale in Figtree: extra-bold headings, bold titles, regular body text and semi-bold labels. */
internal val MrdTypography: Typography = Typography().run {
    Typography(
        displayLarge = displayLarge.figtree(FontWeight.ExtraBold),
        displayMedium = displayMedium.figtree(FontWeight.ExtraBold),
        displaySmall = displaySmall.figtree(FontWeight.ExtraBold),
        headlineLarge = headlineLarge.figtree(FontWeight.ExtraBold),
        headlineMedium = headlineMedium.figtree(FontWeight.ExtraBold),
        headlineSmall = headlineSmall.figtree(FontWeight.ExtraBold),
        titleLarge = titleLarge.figtree(FontWeight.ExtraBold),
        titleMedium = titleMedium.figtree(FontWeight.Bold),
        titleSmall = titleSmall.figtree(FontWeight.Bold),
        bodyLarge = bodyLarge.figtree(FontWeight.Normal),
        bodyMedium = bodyMedium.figtree(FontWeight.Normal),
        bodySmall = bodySmall.figtree(FontWeight.Normal),
        labelLarge = labelLarge.figtree(FontWeight.SemiBold),
        labelMedium = labelMedium.figtree(FontWeight.SemiBold),
        labelSmall = labelSmall.figtree(FontWeight.SemiBold)
    )
}

private fun TextStyle.figtree(weight: FontWeight) = copy(fontFamily = Figtree, fontWeight = weight)
```

- [ ] **Step 8: Create `core/designsystem/src/main/kotlin/com/effekt3r/mrd/core/designsystem/theme/Dimens.kt`**

```kotlin
package com.effekt3r.mrd.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** The spacing steps (spec section 6.1). Layouts use these, never other values. */
object MrdSpacing {
    val ExtraSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val ExtraLarge = 24.dp
}

/** Fields and image cards have 12 dp corners; chips and pills are fully rounded (spec section 6.1). */
object MrdShapes {
    val Card = RoundedCornerShape(12.dp)
    val Field = RoundedCornerShape(12.dp)
    val Pill = RoundedCornerShape(percent = 50)
}
```

- [ ] **Step 9: Create `core/designsystem/src/main/kotlin/com/effekt3r/mrd/core/designsystem/theme/Theme.kt`**

```kotlin
package com.effekt3r.mrd.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * The Mr D theme: Material 3 with Mr D's colours, Figtree and the extra colours in [MrdColors]. It follows the system's
 * dark mode, as `enableEdgeToEdge()` does, so the status-bar icons always contrast with the screen behind them.
 */
@Composable
fun MrdTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMrdColors provides if (darkTheme) DarkMrdColors else LightMrdColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = MrdTypography,
            content = content
        )
    }
}

/** Mr D's extra colours; Material's are in `MaterialTheme.colorScheme`. */
object MrdTheme {
    val colors: MrdColors
        @Composable
        @ReadOnlyComposable
        get() = LocalMrdColors.current
}
```

- [ ] **Step 10: Run the tests**

```bash
./gradlew :core:designsystem:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`, and the count shows `ContrastTest 4 0 0`.

- [ ] **Step 11: Prove the test bites.** Lighten the secondary text colour, run the tests, then put it back:

```bash
F=core/designsystem/src/main/kotlin/com/effekt3r/mrd/core/designsystem/theme/Color.kt
perl -pi -e 's/InkMuted = Color\(0xFF5C6670\)/InkMuted = Color(0xFF8A939B)/' "$F"
./gradlew :core:designsystem:testDebugUnitTest
perl -pi -e 's/InkMuted = Color\(0xFF8A939B\)/InkMuted = Color(0xFF5C6670)/' "$F"
./gradlew :core:designsystem:testDebugUnitTest
```

Expected: the first run fails `ContrastTest > textMeetsAaInTheLightTheme` with `Text below WCAG AA's 4.5:1:`, then `secondary text on the screen: 3.12:1` and `the search field's placeholder: 2.60:1`. The second run passes.

- [ ] **Step 12: Commit**

```bash
./gradlew spotlessApply
git add core/designsystem
git commit -m "feat(designsystem): add Mr D's tokens, Figtree and MrdTheme" \
  -m "MrdTheme wraps Material 3 with Mr D's colours, sampled from the Play Store listing, and Figtree, so Material's own components take on the Mr D look. MrdTheme.colors adds links, Open, Closed and photo placeholders. Sky reads at 1.8:1 as text on white, so it's only a fill behind ink text; links use a darker blue derived from it, and the dark theme is built from the same palette. Figtree is bundled rather than downloaded, so text renders offline: google/fonts ships one variable font, which covers every weight from API 26, the app's minimum. The module gets the designsystem_ resource prefix, like the features." \
  -m "Verified: ContrastTest (4 tests) fails to compile without the tokens and passes with them; a lighter secondary text colour fails it, naming both pairs."
```

---

### Task 2: The icons and the controls

**Files:**
- Create (downloaded): `core/designsystem/src/main/res/drawable/designsystem_ic_*.xml` (nine icons)
- Create: `core/designsystem/src/main/res/values/strings.xml`
- Create: `core/designsystem/src/main/kotlin/com/effekt3r/mrd/core/designsystem/icon/MrdIcons.kt`, `preview/MrdPreviews.kt`
- Create: `core/designsystem/src/main/kotlin/com/effekt3r/mrd/core/designsystem/component/SearchField.kt`, `Pills.kt`, `RestaurantInfo.kt`, `CircleButtons.kt`

The controls are visuals with no logic of their own, so there are no unit tests here. The previews compile in this task; the emulator proves the controls in Tasks 5, 6 and 8, and lint runs in Task 3, once every resource is used.

- [ ] **Step 1: Download the icons.** They're Material Symbols (rounded, 24 dp), pinned to a commit of `google/material-design-icons`. Each file's `android:tint="?attr/colorControlNormal"` needs AppCompat, which the app doesn't use, so the download removes that attribute; Compose's `Icon` tints them instead. Remove the attribute itself, never its line: in `arrow_back`, `android:autoMirrored` follows it on the same tag.

```bash
ICONS=https://raw.githubusercontent.com/google/material-design-icons/10ecc14790612ca8fd164e3f4ffe9c16fc50d5b7/symbols/android
RES=core/designsystem/src/main/res/drawable
mkdir -p "$RES"
for icon in arrow_back:arrow_back_24px:arrow_back close:close_24px:close cloud_off:cloud_off_24px:cloud_off \
  favorite:favorite_24px:favourite favorite:favorite_fill1_24px:favourite_filled restaurant:restaurant_24px:restaurant \
  search:search_24px:search search_off:search_off_24px:search_off star:star_fill1_24px:star_filled; do
  IFS=: read -r name file local <<< "$icon"
  curl -sSfL "$ICONS/$name/materialsymbolsrounded/$file.xml" | perl -0pe 's/\s+android:tint="[^"]*"//' \
    > "$RES/designsystem_ic_$local.xml"
done
shasum -a 256 "$RES"/designsystem_ic_*.xml
```

Expected:

```
0f88419ce961a6a526d06f98040627fc2dcfe6720c4c11555122ff4ab6ac5932  core/designsystem/src/main/res/drawable/designsystem_ic_arrow_back.xml
7962f8496787fcfc319a4f806c7c584424da01c1cd37ba1220fb5aaa068bea8d  core/designsystem/src/main/res/drawable/designsystem_ic_close.xml
9d2d7830b60fababbada5e1914acc95a5bb477162e74a8163e03dc17ac2b9d83  core/designsystem/src/main/res/drawable/designsystem_ic_cloud_off.xml
19c9a2626631b72b2c331448df0a71d6c39ab0444227b61ef2ce8eb04139d090  core/designsystem/src/main/res/drawable/designsystem_ic_favourite_filled.xml
7d7f6ae94fdf3c1e91a3570b027ddff1bd89506d5a3b00941eea4b16c16c8364  core/designsystem/src/main/res/drawable/designsystem_ic_favourite.xml
84e37604f1049ae76d543a7e44ec987e92a943a178976c8b5e060ff672d29e1c  core/designsystem/src/main/res/drawable/designsystem_ic_restaurant.xml
95448f1de7b2009dbd90f4e4c8e99b0903d0260eb45dadea92927ef348a11d71  core/designsystem/src/main/res/drawable/designsystem_ic_search_off.xml
77e0ddb97e290798fbc29e5b2192b3299797e50af0386fe56d73d1f85cf004f2  core/designsystem/src/main/res/drawable/designsystem_ic_search.xml
ace8a4239b8d9c30175039433836c7daf64b3b30225367e7aabe491c93ee3a7f  core/designsystem/src/main/res/drawable/designsystem_ic_star_filled.xml
```

If any hash differs, stop and report it.

- [ ] **Step 2: Create `core/designsystem/src/main/res/values/strings.xml`** with every component's words, including the ones Task 3 uses:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="designsystem_clear_search">Clear search</string>
    <string name="designsystem_favourite">Favourite</string>
    <string name="designsystem_favourite_on">In favourites</string>
    <string name="designsystem_favourite_off">Not in favourites</string>
    <string name="designsystem_add_to_favourites">Add to favourites</string>
    <string name="designsystem_remove_from_favourites">Remove from favourites</string>
    <string name="designsystem_rating_new">New</string>
    <string name="designsystem_sold_out">Sold out</string>
    <string name="designsystem_back">Back</string>
    <!-- Between the parts of a card's details row: "★ 4.6 · R15 delivery · Open". -->
    <string name="designsystem_separator">·</string>
</resources>
```

- [ ] **Step 3: Create `core/designsystem/src/main/kotlin/com/effekt3r/mrd/core/designsystem/icon/MrdIcons.kt`**

```kotlin
package com.effekt3r.mrd.core.designsystem.icon

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.effekt3r.mrd.core.designsystem.R

/**
 * The app's icons: Google's Material Symbols (rounded, 24 dp, Apache 2.0), committed as vector drawables. `Icon` tints
 * them with the current content colour.
 */
object MrdIcons {
    val ArrowBack: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.designsystem_ic_arrow_back)
    val Close: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.designsystem_ic_close)
    val CloudOff: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.designsystem_ic_cloud_off)
    val Favourite: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.designsystem_ic_favourite)
    val FavouriteFilled: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.designsystem_ic_favourite_filled)
    val Restaurant: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.designsystem_ic_restaurant)
    val Search: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.designsystem_ic_search)
    val SearchOff: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.designsystem_ic_search_off)
    val StarFilled: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.designsystem_ic_star_filled)
}
```

- [ ] **Step 4: Create `core/designsystem/src/main/kotlin/com/effekt3r/mrd/core/designsystem/preview/MrdPreviews.kt`**

```kotlin
package com.effekt3r.mrd.core.designsystem.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/** Previews a composable at the smallest supported width (360 dp) in light, dark and at 200% font scale. */
@Preview(name = "Light", widthDp = 360, showBackground = true)
@Preview(name = "Dark", widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "200% font", widthDp = 360, showBackground = true, fontScale = 2f)
annotation class MrdPreviews
```

- [ ] **Step 5: Create `core/designsystem/src/main/kotlin/com/effekt3r/mrd/core/designsystem/component/SearchField.kt`.** It's built on `BasicTextField` with a `TextFieldState`, as spec §4.4 asks, so milestone 5's ViewModel can own the state.

```kotlin
package com.effekt3r.mrd.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.effekt3r.mrd.core.designsystem.R
import com.effekt3r.mrd.core.designsystem.icon.MrdIcons
import com.effekt3r.mrd.core.designsystem.preview.MrdPreviews
import com.effekt3r.mrd.core.designsystem.theme.MrdShapes
import com.effekt3r.mrd.core.designsystem.theme.MrdSpacing
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme

/**
 * Mr D's grey, rounded search field. The caller owns [state] (spec section 4.4 keeps it in the ViewModel). A clear
 * button appears once there's text, and the keyboard's search key closes the keyboard: results update as you type.
 */
@Composable
fun MrdSearchField(state: TextFieldState, placeholder: String, modifier: Modifier = Modifier) {
    val keyboard = LocalSoftwareKeyboardController.current
    val colors = MaterialTheme.colorScheme
    BasicTextField(
        state = state,
        modifier = modifier.fillMaxWidth().semantics { contentDescription = placeholder },
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
        cursorBrush = SolidColor(colors.onSurface),
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        onKeyboardAction = { keyboard?.hide() },
        decorator = { innerTextField ->
            Row(
                modifier = Modifier
                    .background(colors.surfaceVariant, MrdShapes.Field)
                    .heightIn(min = 48.dp)
                    .padding(start = MrdSpacing.Medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(MrdIcons.Search, contentDescription = null, tint = colors.onSurface)
                Box(Modifier.weight(1f).padding(horizontal = MrdSpacing.Small, vertical = MrdSpacing.Medium)) {
                    if (state.text.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clearAndSetSemantics {}
                        )
                    }
                    innerTextField()
                }
                if (state.text.isNotEmpty()) {
                    IconButton(onClick = { state.clearText() }) {
                        Icon(
                            MrdIcons.Close,
                            contentDescription = stringResource(R.string.designsystem_clear_search),
                            tint = colors.onSurface
                        )
                    }
                } else {
                    Spacer(Modifier.width(MrdSpacing.Medium))
                }
            }
        }
    )
}

@MrdPreviews
@Composable
private fun MrdSearchFieldPreview() {
    MrdTheme {
        Surface {
            MrdSearchField(rememberTextFieldState(), "Search restaurants", Modifier.padding(MrdSpacing.Large))
        }
    }
}
```

- [ ] **Step 6: Create `core/designsystem/src/main/kotlin/com/effekt3r/mrd/core/designsystem/component/Pills.kt`**

```kotlin
package com.effekt3r.mrd.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.effekt3r.mrd.core.designsystem.preview.MrdPreviews
import com.effekt3r.mrd.core.designsystem.theme.MrdShapes
import com.effekt3r.mrd.core.designsystem.theme.MrdSpacing
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme

/** A filter that's on or off, such as Open now: filled with ink when on, outlined when off. Its touch target is 48 dp. */
@Composable
fun MrdFilterChip(
    label: String,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(MrdShapes.Pill)
            .background(if (selected) colors.primary else colors.surface)
            .border(1.dp, if (selected) colors.primary else colors.outline, MrdShapes.Pill)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = onSelectedChange)
            .padding(horizontal = MrdSpacing.Large, vertical = MrdSpacing.Small),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.onPrimary else colors.onSurface
        )
    }
}

/** The white "25 mins" pill that sits on a photo's bottom-left corner. */
@Composable
fun MrdEtaPill(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, MrdShapes.Pill)
            .padding(horizontal = MrdSpacing.Small, vertical = MrdSpacing.ExtraSmall)
    )
}

/** A highlight such as "Free delivery" or "New": ink text on sky, the only way sky appears behind text. */
@Composable
fun MrdTag(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        maxLines = 1,
        modifier = modifier
            .background(MaterialTheme.colorScheme.primaryContainer, MrdShapes.Pill)
            .padding(horizontal = MrdSpacing.Small, vertical = 2.dp)
    )
}

@MrdPreviews
@Composable
private fun PillsPreview() {
    MrdTheme {
        Surface {
            Row(Modifier.padding(MrdSpacing.Large), verticalAlignment = Alignment.CenterVertically) {
                MrdFilterChip("Open now", selected = true, onSelectedChange = {})
                MrdFilterChip("Open now", selected = false, onSelectedChange = {})
                MrdEtaPill("25 mins", Modifier.padding(MrdSpacing.Small))
                MrdTag("Free delivery")
            }
        }
    }
}
```

- [ ] **Step 7: Create `core/designsystem/src/main/kotlin/com/effekt3r/mrd/core/designsystem/component/RestaurantInfo.kt`**

```kotlin
package com.effekt3r.mrd.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.effekt3r.mrd.core.designsystem.R
import com.effekt3r.mrd.core.designsystem.icon.MrdIcons
import com.effekt3r.mrd.core.designsystem.preview.MrdPreviews
import com.effekt3r.mrd.core.designsystem.theme.MrdSpacing
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme

/** How a status reads: green for open, red for closed, muted when the app can't tell (spec section 5.6). */
enum class MrdStatusTone { Open, Closed, Unknown }

/** A restaurant's status, such as "Open" or "Closed · Opens 11:00", in its tone's colour. */
@Composable
fun MrdStatusLabel(text: String, tone: MrdStatusTone, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = when (tone) {
            MrdStatusTone.Open -> MrdTheme.colors.open
            MrdStatusTone.Closed -> MrdTheme.colors.closed
            MrdStatusTone.Unknown -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = modifier
    )
}

/** A star and the rating ("4.6"), or a "New" tag when [rating] is null. */
@Composable
fun MrdRating(rating: String?, modifier: Modifier = Modifier) {
    if (rating == null) {
        MrdTag(stringResource(R.string.designsystem_rating_new), modifier)
        return
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(MrdIcons.StarFilled, contentDescription = null, modifier = Modifier.size(16.dp))
        Text(text = rating, style = MaterialTheme.typography.labelLarge)
    }
}

@MrdPreviews
@Composable
private fun RestaurantInfoPreview() {
    MrdTheme {
        Surface {
            Column(Modifier.padding(MrdSpacing.Large), verticalArrangement = Arrangement.spacedBy(MrdSpacing.Small)) {
                MrdRating("4.6")
                MrdRating(null)
                MrdStatusLabel("Open", MrdStatusTone.Open)
                MrdStatusLabel("Closed · Opens 11:00", MrdStatusTone.Closed)
                MrdStatusLabel("Hours unknown", MrdStatusTone.Unknown)
            }
        }
    }
}
```

- [ ] **Step 8: Create `core/designsystem/src/main/kotlin/com/effekt3r/mrd/core/designsystem/component/CircleButtons.kt`**

```kotlin
package com.effekt3r.mrd.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.effekt3r.mrd.core.designsystem.R
import com.effekt3r.mrd.core.designsystem.icon.MrdIcons
import com.effekt3r.mrd.core.designsystem.preview.MrdPreviews
import com.effekt3r.mrd.core.designsystem.theme.MrdSpacing
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme

/** A heart in a white circle, for photos. A 48 dp toggle whose state TalkBack reads (spec section 6.5). */
@Composable
fun MrdFavouriteButton(isFavourite: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.designsystem_favourite)
    val state = stringResource(
        if (isFavourite) R.string.designsystem_favourite_on else R.string.designsystem_favourite_off
    )
    CircleButton(
        icon = if (isFavourite) MrdIcons.FavouriteFilled else MrdIcons.Favourite,
        modifier = modifier
            .clip(CircleShape)
            .toggleable(value = isFavourite, role = Role.Checkbox, onValueChange = { onClick() })
            .semantics {
                contentDescription = label
                stateDescription = state
            }
    )
}

/** A back arrow in a white circle, for screens that open over a photo. */
@Composable
fun MrdBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.designsystem_back)
    CircleButton(
        icon = MrdIcons.ArrowBack,
        modifier = modifier
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
    )
}

@Composable
private fun CircleButton(icon: ImageVector, modifier: Modifier) {
    Box(modifier = modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@MrdPreviews
@Composable
private fun CircleButtonsPreview() {
    MrdTheme {
        Row(
            Modifier.background(MrdTheme.colors.imagePlaceholders[0]).padding(MrdSpacing.Small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MrdBackButton(onClick = {})
            MrdFavouriteButton(isFavourite = false, onClick = {})
            MrdFavouriteButton(isFavourite = true, onClick = {})
            Text("48 dp targets", Modifier.padding(MrdSpacing.Small))
        }
    }
}
```

- [ ] **Step 9: Compile**

```bash
./gradlew :core:designsystem:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 10: Commit**

```bash
./gradlew spotlessApply
git add core/designsystem
git commit -m "feat(designsystem): add the icons and the controls" \
  -m "The search field, the Open now filter chip, the ETA pill, the sky tag, the status label, the rating, and the favourite and back buttons, each with previews at 360 dp in light, dark and 200% font. They take display-ready text and enums, never model types, so milestone 5's :core:ui formats the model and calls them. The search field is built on TextFieldState, which the spec keeps in the ViewModel. The favourite button is a 48 dp toggle with a state description. The icons are Material Symbols, downloaded at a pinned commit, with the AppCompat tint attribute removed." \
  -m "Verified: the module compiles, previews included; the icons match their recorded SHA-256 hashes."
```

---

### Task 3: The restaurant card, the menu rows and the state components

**Files:**
- Create: `core/designsystem/src/main/kotlin/com/effekt3r/mrd/core/designsystem/component/RestaurantCard.kt`, `Menu.kt`, `Skeletons.kt`, `StateScreen.kt`, `OfflineBanner.kt`

The card follows spec §6.2 and §6.5, and the menu rows `menus-design.md` (spec §5.9). Each card is one TalkBack item: `clearAndSetSemantics` replaces its children's semantics with the caller's description, a click action and a custom favourite action, so the heart inside isn't a separate stop.

- [ ] **Step 1: Create `core/designsystem/src/main/kotlin/com/effekt3r/mrd/core/designsystem/component/RestaurantCard.kt`**

```kotlin
package com.effekt3r.mrd.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.effekt3r.mrd.core.designsystem.R
import com.effekt3r.mrd.core.designsystem.icon.MrdIcons
import com.effekt3r.mrd.core.designsystem.preview.MrdPreviews
import com.effekt3r.mrd.core.designsystem.theme.MrdShapes
import com.effekt3r.mrd.core.designsystem.theme.MrdSpacing
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme

/**
 * A restaurant in Mr D's card style (spec section 6.2): a 16:9 photo with the ETA pill and the favourite heart, then
 * the name, the cuisines and a details row that wraps between its parts at large font sizes. A closed restaurant's
 * photo is dimmed.
 *
 * It takes display-ready text, not model types: milestone 5's `:core:ui` formats a restaurant and calls this. TalkBack
 * reads the whole card as one item, [contentDescription], with a custom action for the favourite (spec section 6.5).
 *
 * @param rating "4.6", or null for no rating yet, shown as "New".
 * @param deliveryFee "R15 delivery" or "Free delivery", or null to leave it out.
 * @param isFreeDelivery highlights [deliveryFee].
 * @param eta "25 mins", or null for no pill.
 * @param placeholderSeed picks the placeholder's colour, such as the first cuisine.
 * @param image the photo; null shows the placeholder.
 */
@Composable
fun MrdRestaurantCard(
    name: String,
    cuisines: String,
    rating: String?,
    deliveryFee: String?,
    isFreeDelivery: Boolean,
    status: String,
    statusTone: MrdStatusTone,
    eta: String?,
    isFavourite: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    onFavouriteClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholderSeed: String = cuisines,
    image: (@Composable () -> Unit)? = null
) {
    val favouriteAction = stringResource(
        if (isFavourite) R.string.designsystem_remove_from_favourites else R.string.designsystem_add_to_favourites
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MrdShapes.Card)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                this.contentDescription = contentDescription
                role = Role.Button
                onClick {
                    onClick()
                    true
                }
                customActions = listOf(
                    CustomAccessibilityAction(favouriteAction) {
                        onFavouriteClick()
                        true
                    }
                )
            },
        verticalArrangement = Arrangement.spacedBy(MrdSpacing.ExtraSmall)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(MrdShapes.Card)) {
            Box(
                Modifier.matchParentSize().graphicsLayer {
                    alpha = if (statusTone == MrdStatusTone.Closed) CLOSED_PHOTO_ALPHA else 1f
                }
            ) {
                if (image != null) image() else MrdImagePlaceholder(placeholderSeed, Modifier.fillMaxSize())
            }
            if (eta != null) {
                MrdEtaPill(eta, Modifier.align(Alignment.BottomStart).padding(MrdSpacing.Small))
            }
            MrdFavouriteButton(
                isFavourite = isFavourite,
                onClick = onFavouriteClick,
                modifier = Modifier.align(Alignment.TopEnd).padding(MrdSpacing.ExtraSmall)
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = MrdSpacing.ExtraSmall)
        )
        Text(
            text = cuisines,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MrdSpacing.ExtraSmall),
            verticalArrangement = Arrangement.spacedBy(MrdSpacing.ExtraSmall),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            MrdRating(rating)
            if (deliveryFee != null) {
                Separator()
                if (isFreeDelivery) {
                    MrdTag(deliveryFee)
                } else {
                    Text(text = deliveryFee, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Separator()
            MrdStatusLabel(status, statusTone)
        }
    }
}

/** A photo placeholder in a colour picked by [seed], such as a cuisine, with a food icon. */
@Composable
fun MrdImagePlaceholder(seed: String, modifier: Modifier = Modifier) {
    val tones = MrdTheme.colors.imagePlaceholders
    Box(modifier.background(tones[seed.hashCode().mod(tones.size)]), contentAlignment = Alignment.Center) {
        Icon(
            MrdIcons.Restaurant,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MrdTheme.colors.onImagePlaceholder
        )
    }
}

@Composable
private fun Separator() {
    Text(
        text = stringResource(R.string.designsystem_separator),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private const val CLOSED_PHOTO_ALPHA = 0.45f

@MrdPreviews
@Composable
private fun MrdRestaurantCardPreview() {
    MrdTheme {
        Surface {
            Column(
                Modifier.padding(MrdSpacing.Large),
                verticalArrangement = Arrangement.spacedBy(MrdSpacing.ExtraLarge)
            ) {
                MrdRestaurantCard(
                    name = "Cape Town Grill",
                    cuisines = "Steakhouse · Grill",
                    rating = "4.6",
                    deliveryFee = "R15 delivery",
                    isFreeDelivery = false,
                    status = "Open",
                    statusTone = MrdStatusTone.Open,
                    eta = "32 mins",
                    isFavourite = true,
                    contentDescription = "",
                    onClick = {},
                    onFavouriteClick = {}
                )
                MrdRestaurantCard(
                    name = "Kirstenbosch Coffee Bar",
                    cuisines = "Café · Breakfast",
                    rating = null,
                    deliveryFee = "Free delivery",
                    isFreeDelivery = true,
                    status = "Closed · Opens 07:30",
                    statusTone = MrdStatusTone.Closed,
                    eta = "16 mins",
                    isFavourite = false,
                    contentDescription = "",
                    onClick = {},
                    onFavouriteClick = {}
                )
            }
        }
    }
}
```

- [ ] **Step 2: Create `core/designsystem/src/main/kotlin/com/effekt3r/mrd/core/designsystem/component/Menu.kt`**

```kotlin
package com.effekt3r.mrd.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.effekt3r.mrd.core.designsystem.R
import com.effekt3r.mrd.core.designsystem.preview.MrdPreviews
import com.effekt3r.mrd.core.designsystem.theme.MrdShapes
import com.effekt3r.mrd.core.designsystem.theme.MrdSpacing
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme

/** A menu category's name, such as "Starters", marked as a heading for TalkBack. */
@Composable
fun MrdMenuCategoryHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = MrdSpacing.Large,
                end = MrdSpacing.Large,
                top = MrdSpacing.ExtraLarge,
                bottom = MrdSpacing.Small
            )
            .semantics { heading() }
    )
}

/**
 * One dish: its name, up to two lines of description, an options summary ("+ Extra lemon R5"), the price and an
 * optional 64 dp thumbnail. A sold-out dish is greyed out and says "Sold out". TalkBack reads the row as one item.
 */
@Composable
fun MrdMenuItemRow(
    name: String,
    price: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    options: String? = null,
    isSoldOut: Boolean = false,
    image: (@Composable () -> Unit)? = null
) {
    val textColor = if (isSoldOut) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = MrdSpacing.Large, vertical = MrdSpacing.Medium),
        horizontalArrangement = Arrangement.spacedBy(MrdSpacing.Large)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MrdSpacing.ExtraSmall)) {
            Text(text = name, style = MaterialTheme.typography.titleSmall, color = textColor)
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (options != null) {
                Text(
                    text = options,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MrdSpacing.Small),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = price, style = MaterialTheme.typography.labelLarge, color = textColor)
                if (isSoldOut) {
                    MrdStatusLabel(stringResource(R.string.designsystem_sold_out), MrdStatusTone.Closed)
                }
            }
        }
        if (image != null) {
            Box(Modifier.size(64.dp).clip(MrdShapes.Card).graphicsLayer { alpha = if (isSoldOut) 0.45f else 1f }) {
                image()
            }
        }
    }
}

@MrdPreviews
@Composable
private fun MenuPreview() {
    MrdTheme {
        Surface {
            Column {
                MrdMenuCategoryHeader("Starters")
                MrdMenuItemRow(
                    name = "Grilled halloumi",
                    price = "R69",
                    description = "Citrus glaze, micro herbs",
                    options = "+ Extra lemon R5",
                    image = { MrdImagePlaceholder("Starters", Modifier.fillMaxSize()) }
                )
                MrdMenuItemRow(name = "Rump 250 g", price = "R189", isSoldOut = true)
            }
        }
    }
}
```

- [ ] **Step 3: Create `core/designsystem/src/main/kotlin/com/effekt3r/mrd/core/designsystem/component/Skeletons.kt`**

```kotlin
package com.effekt3r.mrd.core.designsystem.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.effekt3r.mrd.core.designsystem.preview.MrdPreviews
import com.effekt3r.mrd.core.designsystem.theme.MrdShapes
import com.effekt3r.mrd.core.designsystem.theme.MrdSpacing
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme

/**
 * A restaurant card's shape while the list loads. Skeletons are hidden from TalkBack: the screen announces the
 * loading state once instead.
 */
@Composable
fun MrdSkeletonCard(modifier: Modifier = Modifier) {
    val alpha = pulse()
    Column(
        modifier = modifier.fillMaxWidth().clearAndSetSemantics {},
        verticalArrangement = Arrangement.spacedBy(MrdSpacing.Small)
    ) {
        Bone(Modifier.fillMaxWidth().aspectRatio(16f / 9f), alpha, MrdShapes.Card)
        Bone(Modifier.fillMaxWidth(0.6f).height(16.dp), alpha)
        Bone(Modifier.fillMaxWidth(0.4f).height(12.dp), alpha)
        Bone(Modifier.fillMaxWidth(0.5f).height(12.dp), alpha)
    }
}

/** A menu item's shape while the menu loads. */
@Composable
fun MrdSkeletonRow(modifier: Modifier = Modifier) {
    val alpha = pulse()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {}
            .padding(horizontal = MrdSpacing.Large, vertical = MrdSpacing.Medium),
        horizontalArrangement = Arrangement.spacedBy(MrdSpacing.Large)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MrdSpacing.Small)) {
            Bone(Modifier.fillMaxWidth(0.5f).height(16.dp), alpha)
            Bone(Modifier.fillMaxWidth(0.9f).height(12.dp), alpha)
            Bone(Modifier.fillMaxWidth(0.25f).height(12.dp), alpha)
        }
        Bone(Modifier.size(64.dp), alpha, MrdShapes.Card)
    }
}

/** A slow pulse, read only while drawing, so it redraws without recomposing. */
@Composable
private fun pulse(): State<Float> = rememberInfiniteTransition(label = "skeleton").animateFloat(
    initialValue = 0.45f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(durationMillis = 800), RepeatMode.Reverse),
    label = "skeleton alpha"
)

@Composable
private fun Bone(modifier: Modifier, alpha: State<Float>, shape: Shape = MrdShapes.Pill) {
    val color = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier.clip(shape).drawBehind { drawRect(color, alpha = alpha.value) })
}

@MrdPreviews
@Composable
private fun SkeletonsPreview() {
    MrdTheme {
        Surface {
            Column(Modifier.padding(MrdSpacing.Large)) {
                MrdSkeletonCard()
                MrdSkeletonRow()
            }
        }
    }
}
```

- [ ] **Step 4: Create `core/designsystem/src/main/kotlin/com/effekt3r/mrd/core/designsystem/component/StateScreen.kt`**

```kotlin
package com.effekt3r.mrd.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.effekt3r.mrd.core.designsystem.icon.MrdIcons
import com.effekt3r.mrd.core.designsystem.preview.MrdPreviews
import com.effekt3r.mrd.core.designsystem.theme.MrdSpacing
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme

/**
 * A full-screen error or empty state: an icon, a title, what happened and what to do next, and up to two [actions]
 * (spec section 6.2's state table). It scrolls, so 200% text never hides the actions.
 */
@Composable
fun MrdStateScreen(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actions: @Composable FlowRowScope.() -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(MrdSpacing.ExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MrdSpacing.Medium, Alignment.CenterVertically)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MrdSpacing.Small, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(MrdSpacing.Small),
            content = actions
        )
    }
}

@MrdPreviews
@Composable
private fun MrdStateScreenPreview() {
    MrdTheme {
        Surface {
            MrdStateScreen(
                icon = MrdIcons.SearchOff,
                title = "No restaurants match 'xyz'",
                message = "Check the spelling, or search for another name."
            ) {
                Button(onClick = {}) { Text("Clear search") }
                OutlinedButton(onClick = {}) { Text("Turn off Open now") }
            }
        }
    }
}
```

- [ ] **Step 5: Create `core/designsystem/src/main/kotlin/com/effekt3r/mrd/core/designsystem/component/OfflineBanner.kt`**

```kotlin
package com.effekt3r.mrd.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.effekt3r.mrd.core.designsystem.icon.MrdIcons
import com.effekt3r.mrd.core.designsystem.preview.MrdPreviews
import com.effekt3r.mrd.core.designsystem.theme.MrdShapes
import com.effekt3r.mrd.core.designsystem.theme.MrdSpacing
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme

/**
 * The banner over cached content when the last refresh failed ("You're offline · Showing restaurants from 10:42"),
 * with an action such as Retry. TalkBack announces it when it appears.
 */
@Composable
fun MrdOfflineBanner(message: String, actionLabel: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.primaryContainer, MrdShapes.Field)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(start = MrdSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(MrdIcons.CloudOff, contentDescription = null, tint = colors.onPrimaryContainer)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onPrimaryContainer,
            modifier = Modifier.weight(1f).padding(horizontal = MrdSpacing.Small, vertical = MrdSpacing.Medium)
        )
        TextButton(
            onClick = onAction,
            colors = ButtonDefaults.textButtonColors(contentColor = colors.onPrimaryContainer)
        ) {
            Text(actionLabel)
        }
    }
}

@MrdPreviews
@Composable
private fun MrdOfflineBannerPreview() {
    MrdTheme {
        Surface {
            MrdOfflineBanner(
                message = "You're offline · Showing restaurants from 10:42",
                actionLabel = "Retry",
                onAction = {},
                modifier = Modifier.padding(MrdSpacing.Large)
            )
        }
    }
}
```

- [ ] **Step 6: Compile and lint**

```bash
./gradlew :core:designsystem:compileDebugKotlin :app:lintDebug
tail -1 app/build/reports/lint-results-debug.txt
```

Expected: `BUILD SUCCESSFUL`, and lint's only finding is the `MissingApplicationIcon` warning that `dev` already has, which Task 4 removes (`0 errors, 1 warning`). If lint reports anything else, fix it before committing.

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add core/designsystem
git commit -m "feat(designsystem): add the restaurant card, menu rows and state components" \
  -m "The restaurant card follows spec section 6.2: a 16:9 photo slot with 12 dp corners, the ETA pill and the heart, then the name, the cuisines and a details row that wraps between its parts at large font sizes. A closed restaurant's photo is dimmed, free delivery is a sky tag, no rating shows New, and no photo shows a placeholder coloured by cuisine. TalkBack reads each card as one item with a favourite action (section 6.5). The menu category header and item row follow menus-design.md, with a sold-out state and a 64 dp thumbnail slot. The skeleton card and rows, the error and empty state screen and the offline banner complete section 6.1's list." \
  -m "Verified: the module compiles, previews included; lint reports only the existing MissingApplicationIcon warning."
```

---

### Task 4: The launcher icon and MrdTheme in :app

**Files:**
- Create: `core/designsystem/src/main/res/values/colors.xml`
- Test: `core/designsystem/src/test/kotlin/com/effekt3r/mrd/core/designsystem/theme/ColorResourcesTest.kt`
- Create: `app/src/main/res/mipmap-anydpi/ic_launcher.xml`, `app/src/main/res/drawable/ic_launcher_foreground.xml`, `app/src/main/res/drawable/ic_launcher_monochrome.xml`
- Modify: `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/themes.xml`, `app/src/main/res/values-night/themes.xml`, `app/src/main/kotlin/com/effekt3r/mrd/MainActivity.kt`

The icon is original (spec §6.6): a map pin in ink, holding a white knife and fork, on Mr D's sky. It never copies Mr D's logo. Its monochrome layer, the pin alone, gives Android 13's themed icons something to tint. Its colours come from `colors.xml` in the design system, so no raw colour lives in `:app`.

- [ ] **Step 1: Write the failing test.** Create `core/designsystem/src/test/kotlin/com/effekt3r/mrd/core/designsystem/theme/ColorResourcesTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Run it to see it fail**

```bash
./gradlew :core:designsystem:testDebugUnitTest
```

Expected: `BUILD FAILED`. `ColorResourcesTest > xmlColoursMatchThePalette FAILED` with `java.io.FileNotFoundException: src/main/res/values/colors.xml (No such file or directory)`.

- [ ] **Step 3: Create `core/designsystem/src/main/res/values/colors.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Palette colours for XML that can't read the Compose tokens: the launcher icon and the window background.
         They mirror MrdPalette in Color.kt, and ColorResourcesTest fails if the two drift apart. -->
    <color name="designsystem_sky">#FF6DCFF6</color>
    <color name="designsystem_ink">#FF1D1D1B</color>
    <color name="designsystem_white">#FFFFFFFF</color>
</resources>
```

- [ ] **Step 4: Run the tests**

```bash
./gradlew :core:designsystem:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`, and the count shows `ColorResourcesTest 1 0 0` and `ContrastTest 4 0 0`.

- [ ] **Step 5: Prove that Gradle re-runs it after an XML edit.** Without `--rerun`:

```bash
X=core/designsystem/src/main/res/values/colors.xml
perl -pi -e 's/#FF6DCFF6/#FF6DCFF7/' "$X"
./gradlew :core:designsystem:testDebugUnitTest --console=plain | grep -E ':core:designsystem:testDebugUnitTest|FAILED'
perl -pi -e 's/#FF6DCFF7/#FF6DCFF6/' "$X"
./gradlew :core:designsystem:testDebugUnitTest
```

Expected: the test task runs rather than reporting `UP-TO-DATE`, and fails with `expected:<#FF6DCFF[6]> but was:<#FF6DCFF[7]>`. After the colour is put back, the tests pass.

- [ ] **Step 6: Create the launcher icon.** First `app/src/main/res/mipmap-anydpi/ic_launcher.xml`. The `-anydpi` folder needs no `-v26` qualifier, because adaptive icons arrived in API 26, the app's minimum.

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- An original icon in Mr D's sky and ink (spec section 6.6): a map pin holding a knife and fork. It's not Mr D's
     logo. The monochrome layer is the pin alone, for Android 13's themed icons. -->
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/designsystem_sky" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
```

Then `app/src/main/res/drawable/ic_launcher_foreground.xml`. The pin is a circle of radius 17 around (54, 47) with tangents meeting at (54, 76), inside the 66 dp safe zone. The knife and fork are `designsystem_ic_restaurant`'s path, scaled by 0.024 and centred in the pin's head.

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- 108 dp, with the pin inside the 66 dp safe zone. The knife and fork are Material Symbols' "restaurant" glyph,
     scaled into the pin's head. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="@color/designsystem_ink"
        android:pathData="M54,76L40.23,56.97A17,17 0,1 1,67.77 56.97Z" />
    <group
        android:scaleX="0.024"
        android:scaleY="0.024"
        android:translateX="42.96"
        android:translateY="35.48">
        <path
            android:fillColor="@color/designsystem_white"
            android:pathData="M280,360L280,120Q280,103 291.5,91.5Q303,80 320,80Q337,80 348.5,91.5Q360,103 360,120L360,360L400,360L400,120Q400,103 411.5,91.5Q423,80 440,80Q457,80 468.5,91.5Q480,103 480,120L480,360Q480,416 445.5,458Q411,500 360,514L360,840Q360,857 348.5,868.5Q337,880 320,880Q303,880 291.5,868.5Q280,857 280,840L280,514Q229,500 194.5,458Q160,416 160,360L160,120Q160,103 171.5,91.5Q183,80 200,80Q217,80 228.5,91.5Q240,103 240,120L240,360L280,360ZM680,560L600,560Q583,560 571.5,548.5Q560,537 560,520L560,280Q560,210 611.5,145Q663,80 718,80Q736,80 748,94Q760,108 760,127L760,840Q760,857 748.5,868.5Q737,880 720,880Q703,880 691.5,868.5Q680,857 680,840L680,560Z" />
    </group>
</vector>
```

Then `app/src/main/res/drawable/ic_launcher_monochrome.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<!-- The pin with a round hole: the system tints it to match the wallpaper. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="@color/designsystem_ink"
        android:fillType="evenOdd"
        android:pathData="M54,76L40.23,56.97A17,17 0,1 1,67.77 56.97Z M54,40.5A6.5,6.5 0,1 1,54 53.5A6.5,6.5 0,1 1,54 40.5Z" />
</vector>
```

- [ ] **Step 7: Set the icon.** In `app/src/main/AndroidManifest.xml`, add this attribute to `<application>`, after `android:enableOnBackInvokedCallback="true"`:

```xml
        android:icon="@mipmap/ic_launcher"
```

- [ ] **Step 8: Match the window background to the theme.** Replace `app/src/main/res/values/themes.xml` with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Window theme only; Compose draws everything else with MrdTheme. The window background matches MrdTheme's, so
         launch and screen transitions don't flash a different colour. -->
    <style name="Theme.Mrd" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:windowBackground">@color/designsystem_white</item>
    </style>
</resources>
```

and `app/src/main/res/values-night/themes.xml` with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Mrd" parent="android:Theme.Material.NoActionBar">
        <item name="android:windowBackground">@color/designsystem_ink</item>
    </style>
</resources>
```

- [ ] **Step 9: Use MrdTheme.** Replace `app/src/main/kotlin/com/effekt3r/mrd/MainActivity.kt` with:

```kotlin
package com.effekt3r.mrd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme
import com.effekt3r.mrd.navigation.MrdNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // MrdTheme and enableEdgeToEdge() both follow the system's dark mode, so the status-bar icons are dark on
            // the light theme and light on the dark one.
            MrdTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MrdNavHost()
                }
            }
        }
    }
}
```

- [ ] **Step 10: Build, lint and look**

```bash
./gradlew :app:lintDebug installDebug
tail -1 app/build/reports/lint-results-debug.txt
adb shell am start -W -a android.settings.APPLICATION_DETAILS_SETTINGS -d package:com.effekt3r.mrd
```

Expected:
- `BUILD SUCCESSFUL`, and lint ends with `No issues found.`: `MissingApplicationIcon` is gone. If lint reports anything else, fix it before committing.
- App info shows "Mr D Discover" with the new icon: a sky disc, and an ink pin holding a white knife and fork.
- Launched, the milestone 1 placeholders now use Figtree, and dark mode turns them ink with white text and light status-bar icons.

- [ ] **Step 11: Commit**

```bash
./gradlew spotlessApply
git add core/designsystem app
git commit -m "feat(app): add the launcher icon and use MrdTheme" \
  -m "An original adaptive icon in Mr D's sky and ink: a map pin holding a knife and fork, not Mr D's logo (spec section 6.6), with a monochrome layer for themed icons. It removes lint's MissingApplicationIcon warning. Its colours come from colors.xml in the design system, which ColorResourcesTest keeps equal to the Compose palette. MrdTheme replaces MaterialTheme; it follows the system's dark mode, as enableEdgeToEdge() does, so the status-bar icons stay readable, and the window background matches it, so launch doesn't flash another colour." \
  -m "Verified: ColorResourcesTest fails without colors.xml and passes with it, and an edited colour re-runs and fails it without --rerun; lint reports no issues; the icon checked in App info on the emulator."
```

---

### Task 5: Browse, restyled

**Files:**
- Create: `feature/browse/src/main/kotlin/com/effekt3r/mrd/feature/browse/SampleRestaurants.kt`
- Modify: `feature/browse/src/main/kotlin/com/effekt3r/mrd/feature/browse/BrowseScreen.kt`, `feature/browse/src/main/res/values/strings.xml`

Browse gets the real header: the "Restaurants" title, an inert search field and an Open now chip that only toggles its look. Under it are four sample restaurants from `mock-api/v1/restaurants.json`: one open (`r-1001`), one with free delivery (`r-1015`), one closed (`r-1010`) and one not rated yet (`r-1070`). Each shows the photo placeholder, because images arrive in milestone 5. Tapping a card still opens Details, and a heart toggles on this screen only.

- [ ] **Step 1: Create `feature/browse/src/main/kotlin/com/effekt3r/mrd/feature/browse/SampleRestaurants.kt`**

```kotlin
package com.effekt3r.mrd.feature.browse

/**
 * Four restaurants from mock-api/v1/restaurants.json for the Browse placeholder: one open, one with free delivery, one
 * closed and one not rated yet. Names, cuisines and times are data, as the API sends them; the words around them are
 * string resources. Milestone 5 replaces this with the real list.
 */
internal data class SampleRestaurant(
    val id: String,
    val name: String,
    val cuisines: List<String>,
    val rating: String?,
    val deliveryFeeCents: Long,
    val etaMinutes: Int,
    /** When a closed restaurant opens next; null while it's open. */
    val opensAt: String? = null
)

internal val sampleRestaurants = listOf(
    SampleRestaurant("r-1001", "Cape Town Grill", listOf("Steakhouse", "Grill"), "4.6", 1500, 32),
    SampleRestaurant("r-1015", "Woodstock Taverna", listOf("Greek"), "4.6", 0, 40),
    SampleRestaurant("r-1010", "Kirstenbosch Coffee Bar", listOf("Café", "Breakfast"), "4.6", 1200, 16, "07:30"),
    SampleRestaurant("r-1070", "Kommetjie Ramen Bar", listOf("Japanese"), null, 2500, 43)
)
```

- [ ] **Step 2: Replace `feature/browse/src/main/res/values/strings.xml`.** The milestone 1 sample strings go, because nothing uses them now.

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="browse_title">Restaurants</string>
    <string name="browse_search_placeholder">Search restaurants</string>
    <string name="browse_open_now">Open now</string>

    <!-- The sample cards' words, until milestone 5's formatters replace them. -->
    <string name="browse_sample_separator">" · "</string>
    <plurals name="browse_sample_eta">
        <item quantity="one">%d min</item>
        <item quantity="other">%d mins</item>
    </plurals>
    <string name="browse_sample_delivery_fee">R%1$s delivery</string>
    <string name="browse_sample_free_delivery">Free delivery</string>
    <string name="browse_sample_open">Open</string>
    <string name="browse_sample_closed">Closed · Opens %1$s</string>

    <!-- What TalkBack reads for a sample card, as spec section 6.5 describes. -->
    <string name="browse_sample_description">%1$s. %2$s. %3$s. %4$s. %5$s. %6$s.</string>
    <string name="browse_sample_spoken_separator">", "</string>
    <string name="browse_sample_rated">Rated %1$s</string>
    <string name="browse_sample_not_rated">New</string>
    <plurals name="browse_sample_minutes">
        <item quantity="one">%d minute</item>
        <item quantity="other">%d minutes</item>
    </plurals>
    <string name="browse_sample_closed_spoken">Closed, opens %1$s</string>
</resources>
```

- [ ] **Step 3: Replace `feature/browse/src/main/kotlin/com/effekt3r/mrd/feature/browse/BrowseScreen.kt`**

```kotlin
package com.effekt3r.mrd.feature.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.effekt3r.mrd.core.designsystem.component.MrdFilterChip
import com.effekt3r.mrd.core.designsystem.component.MrdRestaurantCard
import com.effekt3r.mrd.core.designsystem.component.MrdSearchField
import com.effekt3r.mrd.core.designsystem.component.MrdStatusTone
import com.effekt3r.mrd.core.designsystem.preview.MrdPreviews
import com.effekt3r.mrd.core.designsystem.theme.MrdSpacing
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme

/**
 * Placeholder for Browse, restyled with the design system (milestone 4): the real header, with an inert search field
 * and an Open now chip that only toggles its look, above four sample restaurants. Photos show the placeholder until
 * images arrive. Tapping a card opens Details, and hearts toggle on this screen only. Milestone 5 replaces it.
 */
@Composable
internal fun BrowseScreen(onRestaurantClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val search = rememberTextFieldState()
    var openNow by rememberSaveable { mutableStateOf(false) }
    var favourites by rememberSaveable { mutableStateOf(emptySet<String>()) }
    Box(modifier.fillMaxSize()) {
        SampleList(
            search = search,
            openNow = openNow,
            onOpenNowChange = { openNow = it },
            favourites = favourites,
            onFavouriteClick = { id -> favourites = if (id in favourites) favourites - id else favourites + id },
            onRestaurantClick = onRestaurantClick
        )
        // The list scrolls under the transparent status bar; this strip keeps the status-bar icons on a plain surface.
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(MaterialTheme.colorScheme.surface)
        )
    }
}

@Composable
private fun SampleList(
    search: TextFieldState,
    openNow: Boolean,
    onOpenNowChange: (Boolean) -> Unit,
    favourites: Set<String>,
    onFavouriteClick: (String) -> Unit,
    onRestaurantClick: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 300.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = WindowInsets.safeDrawing
            .add(WindowInsets(MrdSpacing.Large, MrdSpacing.Large, MrdSpacing.Large, MrdSpacing.Large))
            .asPaddingValues(),
        horizontalArrangement = Arrangement.spacedBy(MrdSpacing.Large),
        verticalArrangement = Arrangement.spacedBy(MrdSpacing.ExtraLarge)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(MrdSpacing.Medium)) {
                Text(
                    text = stringResource(R.string.browse_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() }
                )
                MrdSearchField(search, placeholder = stringResource(R.string.browse_search_placeholder))
                MrdFilterChip(
                    label = stringResource(R.string.browse_open_now),
                    selected = openNow,
                    onSelectedChange = onOpenNowChange
                )
            }
        }
        items(sampleRestaurants, key = { it.id }) { sample ->
            SampleRestaurantCard(
                sample = sample,
                isFavourite = sample.id in favourites,
                onClick = dropUnlessResumed { onRestaurantClick(sample.id) },
                onFavouriteClick = { onFavouriteClick(sample.id) }
            )
        }
    }
}

@Composable
private fun SampleRestaurantCard(
    sample: SampleRestaurant,
    isFavourite: Boolean,
    onClick: () -> Unit,
    onFavouriteClick: () -> Unit
) {
    val isFree = sample.deliveryFeeCents == 0L
    val fee = if (isFree) {
        stringResource(R.string.browse_sample_free_delivery)
    } else {
        stringResource(R.string.browse_sample_delivery_fee, (sample.deliveryFeeCents / 100).toString())
    }
    val opensAt = sample.opensAt
    val description = stringResource(
        R.string.browse_sample_description,
        sample.name,
        sample.cuisines.joinToString(stringResource(R.string.browse_sample_spoken_separator)),
        sample.rating?.let { stringResource(R.string.browse_sample_rated, it) }
            ?: stringResource(R.string.browse_sample_not_rated),
        pluralStringResource(R.plurals.browse_sample_minutes, sample.etaMinutes, sample.etaMinutes),
        fee,
        opensAt?.let { stringResource(R.string.browse_sample_closed_spoken, it) }
            ?: stringResource(R.string.browse_sample_open)
    )
    MrdRestaurantCard(
        name = sample.name,
        cuisines = sample.cuisines.joinToString(stringResource(R.string.browse_sample_separator)),
        rating = sample.rating,
        deliveryFee = fee,
        isFreeDelivery = isFree,
        status = opensAt?.let { stringResource(R.string.browse_sample_closed, it) }
            ?: stringResource(R.string.browse_sample_open),
        statusTone = if (opensAt == null) MrdStatusTone.Open else MrdStatusTone.Closed,
        eta = pluralStringResource(R.plurals.browse_sample_eta, sample.etaMinutes, sample.etaMinutes),
        isFavourite = isFavourite,
        contentDescription = description,
        onClick = onClick,
        onFavouriteClick = onFavouriteClick,
        placeholderSeed = sample.cuisines.first()
    )
}

@MrdPreviews
@Composable
private fun BrowseScreenPreview() {
    MrdTheme {
        Surface {
            BrowseScreen(onRestaurantClick = {})
        }
    }
}
```

- [ ] **Step 4: Build, lint and look**

```bash
./gradlew :app:lintDebug installDebug
tail -1 app/build/reports/lint-results-debug.txt
adb shell am start -W -n com.effekt3r.mrd/.MainActivity
```

Expected: `BUILD SUCCESSFUL` and `No issues found.` On the device:
- the header: "Restaurants" in extra-bold Figtree, a grey rounded field reading "Search restaurants", and Open now, outlined; tapping it fills it with ink;
- the cards, each with a coloured 16:9 placeholder, a white ETA pill bottom-left ("32 mins" on the first) and a heart in a white circle top-right;
- below each photo, the name in bold, the cuisines in grey and the details row, such as "★ 4.6 · R15 delivery · Open" with Open in green;
- Woodstock Taverna's "Free delivery" as a sky tag, Kirstenbosch Coffee Bar dimmed with "Closed · Opens 07:30" in red, and Kommetjie Ramen Bar's "New" tag.

- [ ] **Step 5: Check what TalkBack reads**

```bash
adb shell uiautomator dump /sdcard/mrd-ui.xml > /dev/null
adb exec-out cat /sdcard/mrd-ui.xml | grep -o 'content-desc="[^"]*\. [^"]*"'
adb exec-out cat /sdcard/mrd-ui.xml | grep -c 'content-desc="Favourite"'
adb shell rm /sdcard/mrd-ui.xml
```

Expected: one description per card on screen, in spec §6.5's shape. On a 411 dp-wide phone that's three:

```
content-desc="Cape Town Grill. Steakhouse, Grill. Rated 4.6. 32 minutes. R15 delivery. Open."
content-desc="Woodstock Taverna. Greek. Rated 4.6. 40 minutes. Free delivery. Open."
content-desc="Kirstenbosch Coffee Bar. Café, Breakfast. Rated 4.6. 16 minutes. R12 delivery. Closed, opens 07:30."
```

The count is `0`: the hearts belong to their cards, as custom actions, not separate stops.

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add feature/browse
git commit -m "feat(browse): restyle the placeholder with the design system" \
  -m "Franco found the milestone 1 placeholder didn't look like Mr D and its rows lacked detail. Browse now has the real header (title, search field and Open now chip) above four sample restaurants from mock-api/v1/restaurants.json in Mr D's card style: open, free delivery, closed and not yet rated. Names and cuisines are data; the words around them are string resources. A surface strip keeps the status-bar icons readable over the scrolling list. It stays a placeholder: milestone 5 replaces it with the real list." \
  -m "Verified: lint reports no issues; emulator walk in light mode; uiautomator shows one TalkBack item per card with spec section 6.5's description, and no separate heart."
```

---

### Task 6: Details, restyled

**Files:**
- Modify: `feature/details/src/main/kotlin/com/effekt3r/mrd/feature/details/RestaurantScreen.kt`, `feature/details/src/main/res/values/strings.xml`

Details shows Cape Town Grill (`r-1001`) and part of a steakhouse menu, whichever card was tapped: routes carry only the id, and features can't share sample data. Milestone 6 shows the restaurant tapped and its real menu.

- [ ] **Step 1: Replace `feature/details/src/main/res/values/strings.xml`.** The milestone 1 strings go, because nothing uses them now.

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="details_menu">Menu</string>

    <!-- Cape Town Grill (r-1001) and part of a steakhouse menu, until milestone 6 shows real data. -->
    <string name="details_sample_name">Cape Town Grill</string>
    <string name="details_sample_cuisines">Steakhouse · Grill</string>
    <string name="details_sample_rating">4.6</string>
    <string name="details_sample_eta_and_fee">· 32 mins · R15 delivery</string>
    <string name="details_sample_status">Open · Closes 22:00</string>
    <string name="details_sample_starters">Starters</string>
    <string name="details_sample_halloumi">Grilled halloumi</string>
    <string name="details_sample_halloumi_price">R69</string>
    <string name="details_sample_halloumi_description">Citrus glaze, micro herbs</string>
    <string name="details_sample_halloumi_options">+ Extra lemon R5</string>
    <string name="details_sample_livers">Peri-peri chicken livers</string>
    <string name="details_sample_livers_price">R79</string>
    <string name="details_sample_livers_description">Creamy peri-peri sauce, served with toasted ciabatta for dipping</string>
    <string name="details_sample_steaks">Steaks</string>
    <string name="details_sample_sirloin">Sirloin 300 g</string>
    <string name="details_sample_sirloin_price">R219</string>
    <string name="details_sample_sirloin_description">Chips or salad, with pepper or mushroom sauce</string>
    <string name="details_sample_sirloin_options">+ Mushroom sauce R25 · + Egg R10</string>
    <string name="details_sample_rump">Rump 250 g</string>
    <string name="details_sample_rump_price">R189</string>
    <string name="details_sample_desserts">Desserts</string>
    <string name="details_sample_malva">Malva pudding</string>
    <string name="details_sample_malva_price">R65</string>
    <string name="details_sample_malva_description">Warm, with custard</string>
</resources>
```

- [ ] **Step 2: Replace `feature/details/src/main/kotlin/com/effekt3r/mrd/feature/details/RestaurantScreen.kt`**

```kotlin
package com.effekt3r.mrd.feature.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.effekt3r.mrd.core.designsystem.component.MrdBackButton
import com.effekt3r.mrd.core.designsystem.component.MrdFavouriteButton
import com.effekt3r.mrd.core.designsystem.component.MrdImagePlaceholder
import com.effekt3r.mrd.core.designsystem.component.MrdMenuCategoryHeader
import com.effekt3r.mrd.core.designsystem.component.MrdMenuItemRow
import com.effekt3r.mrd.core.designsystem.component.MrdRating
import com.effekt3r.mrd.core.designsystem.component.MrdStatusLabel
import com.effekt3r.mrd.core.designsystem.component.MrdStatusTone
import com.effekt3r.mrd.core.designsystem.preview.MrdPreviews
import com.effekt3r.mrd.core.designsystem.theme.MrdSpacing
import com.effekt3r.mrd.core.designsystem.theme.MrdTheme

/**
 * Placeholder for Details, restyled with the design system (milestone 4): Cape Town Grill (r-1001) and a sample of its
 * menu, whichever restaurant [id] was opened; the heart toggles for this screen only. Milestone 6 replaces it with the
 * restaurant's own details and menu.
 */
@Composable
internal fun RestaurantScreen(id: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var isFavourite by rememberSaveable(id) { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val headerScrolledAway by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    Box(modifier.fillMaxSize()) {
        SampleDetails(listState, isFavourite, onFavouriteClick = { isFavourite = !isFavourite }, onBack = onBack)
        // The header sits under the status bar; once it has scrolled away, a surface strip keeps the icons off the text.
        if (headerScrolledAway) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(MaterialTheme.colorScheme.surface)
            )
        }
    }
}

@Composable
private fun SampleDetails(
    listState: LazyListState,
    isFavourite: Boolean,
    onFavouriteClick: () -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = WindowInsets.safeDrawing
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
            .asPaddingValues()
    ) {
        item {
            Box(Modifier.fillMaxWidth().height(240.dp)) {
                MrdImagePlaceholder(seed = "Steakhouse", modifier = Modifier.fillMaxSize())
                MrdBackButton(
                    onClick = dropUnlessResumed { onBack() },
                    modifier = Modifier
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Top + WindowInsetsSides.Start
                            )
                        )
                        .padding(MrdSpacing.Small)
                )
                MrdFavouriteButton(
                    isFavourite = isFavourite,
                    onClick = onFavouriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                        )
                        .padding(MrdSpacing.Small)
                )
            }
        }
        item {
            Column(
                modifier = Modifier.padding(MrdSpacing.Large),
                verticalArrangement = Arrangement.spacedBy(MrdSpacing.Small)
            ) {
                Text(
                    text = stringResource(R.string.details_sample_name),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = stringResource(R.string.details_sample_cuisines),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MrdSpacing.ExtraSmall),
                    itemVerticalAlignment = Alignment.CenterVertically
                ) {
                    MrdRating(stringResource(R.string.details_sample_rating))
                    Text(
                        text = stringResource(R.string.details_sample_eta_and_fee),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                MrdStatusLabel(stringResource(R.string.details_sample_status), MrdStatusTone.Open)
            }
        }
        item {
            Text(
                text = stringResource(R.string.details_menu),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = MrdSpacing.Large).semantics { heading() }
            )
        }
        sampleMenu.forEach { category ->
            item { MrdMenuCategoryHeader(stringResource(category.name)) }
            category.dishes.forEach { dish ->
                item {
                    val name = stringResource(dish.name)
                    MrdMenuItemRow(
                        name = name,
                        price = stringResource(dish.price),
                        description = dish.description?.let { stringResource(it) },
                        options = dish.options?.let { stringResource(it) },
                        isSoldOut = dish.isSoldOut,
                        image = if (dish.hasPhoto) photoPlaceholder(seed = name) else null
                    )
                }
            }
        }
    }
}

/** A dish's photo slot, filled with a placeholder until milestone 6 loads real photos. */
private fun photoPlaceholder(seed: String): @Composable () -> Unit = {
    MrdImagePlaceholder(seed = seed, modifier = Modifier.fillMaxSize())
}

private class SampleCategory(val name: Int, val dishes: List<SampleDish>)

private class SampleDish(
    val name: Int,
    val price: Int,
    val description: Int? = null,
    val options: Int? = null,
    val isSoldOut: Boolean = false,
    val hasPhoto: Boolean = false
)

/** Part of a steakhouse menu, in the shape of menus-design.md's generated menus. */
private val sampleMenu = listOf(
    SampleCategory(
        R.string.details_sample_starters,
        listOf(
            SampleDish(
                R.string.details_sample_halloumi,
                R.string.details_sample_halloumi_price,
                description = R.string.details_sample_halloumi_description,
                options = R.string.details_sample_halloumi_options,
                hasPhoto = true
            ),
            SampleDish(
                R.string.details_sample_livers,
                R.string.details_sample_livers_price,
                description = R.string.details_sample_livers_description
            )
        )
    ),
    SampleCategory(
        R.string.details_sample_steaks,
        listOf(
            SampleDish(
                R.string.details_sample_sirloin,
                R.string.details_sample_sirloin_price,
                description = R.string.details_sample_sirloin_description,
                options = R.string.details_sample_sirloin_options,
                hasPhoto = true
            ),
            SampleDish(R.string.details_sample_rump, R.string.details_sample_rump_price, isSoldOut = true)
        )
    ),
    SampleCategory(
        R.string.details_sample_desserts,
        listOf(
            SampleDish(
                R.string.details_sample_malva,
                R.string.details_sample_malva_price,
                description = R.string.details_sample_malva_description
            )
        )
    )
)

@MrdPreviews
@Composable
private fun RestaurantScreenPreview() {
    MrdTheme {
        Surface {
            RestaurantScreen(id = "r-1001", onBack = {})
        }
    }
}
```

- [ ] **Step 3: Build, lint and look**

```bash
./gradlew :app:lintDebug installDebug
tail -1 app/build/reports/lint-results-debug.txt
adb shell am start -W -n com.effekt3r.mrd/.MainActivity
```

Expected: `BUILD SUCCESSFUL` and `No issues found.` Tap Cape Town Grill's photo. Details shows:
- a placeholder header under the status bar, with white circular back and favourite buttons;
- "Cape Town Grill" in extra-bold, "Steakhouse · Grill", "★ 4.6 · 32 mins · R15 delivery" and a green "Open · Closes 22:00";
- "Menu", then Starters, Steaks and Desserts. Dishes have descriptions, options lines and 64 dp thumbnails, and "Rump 250 g" is greyed with a red "Sold out".

The heart toggles, and Back returns to Browse. Then set `adb shell settings put system font_scale 2.0`, open Details and scroll to the end: once the header has gone, a white strip keeps the menu out from under the status-bar icons. Put the font scale back with `adb shell settings put system font_scale 1.0`.

- [ ] **Step 4: Commit**

```bash
./gradlew spotlessApply
git add feature/details
git commit -m "feat(details): restyle the placeholder with the design system" \
  -m "Details now shows a restaurant in Mr D's style: a header with white circular back and favourite buttons, the name, cuisines, stats and status, and a sample steakhouse menu built from the design system's menu components, including a sold-out dish. It shows Cape Town Grill whichever card was tapped, because routes carry only the id and features can't share sample data. Once the header has scrolled away, a surface strip keeps the status-bar icons off the text. It stays a placeholder: milestone 6 replaces it with the restaurant's own details and menu." \
  -m "Verified: lint reports no issues; emulator walk: a card opens Details, the heart toggles, Back returns to Browse, and at 200% font the strip appears once the header has scrolled away."
```

---

### Task 7: Document the design system

This is the controller's docs task.

**Files:**
- Modify: `README.md`, `docs/specs/2026-11-18-restaurant-discovery-design.md`, `docs/ai/corrections.md`

- [ ] **Step 1: Credit Figtree and the icons in `README.md`.** Insert this section directly above `## Documentation`:

```markdown
## Credits

- **Figtree**, the app's typeface, is by Erik Kennedy and the Figtree Project Authors, under the [SIL Open Font License 1.1](core/designsystem/FIGTREE-OFL.txt). The app bundles the variable font from [google/fonts](https://github.com/google/fonts/tree/main/ofl/figtree).
- **Icons** are Google's [Material Symbols](https://fonts.google.com/icons) (rounded), under the Apache License 2.0.
- **Food photos** in the mock API are from [TheMealDB](https://www.themealdb.com/api.php); see [`mock-api/README.md`](mock-api/README.md).
```

At the end of the documentation table, add:

```markdown
| Custom design systems (theming Material 3) | https://developer.android.com/develop/ui/compose/designsystems/custom |
| Fonts and variable fonts | https://developer.android.com/develop/ui/compose/text/fonts |
| Material Symbols in Compose | https://developer.android.com/develop/ui/compose/graphics/images/material |
| Adaptive icons | https://developer.android.com/develop/ui/views/launch/icon_design_adaptive |
| Accessibility in Compose | https://developer.android.com/develop/ui/compose/accessibility |
```

Update the status line to match what has merged into `dev` by then.

- [ ] **Step 2: Describe the design system as built, in spec §6.1.** Replace:

```markdown
- **Type:** Figtree, an open-licence geometric sans close to the lettering in the Mr D screenshots. It's bundled with the app rather than downloaded, so it works offline. Headings are extra-bold and body text is regular.
- **Shapes and spacing:** fields and image cards have 12 dp rounded corners; chips and pills are fully rounded. Spacing steps are 4, 8, 12, 16 and 24 dp.
- **Components:** search field, filter chip, ETA pill, status label, rating, favourite button, skeleton card, state screens (error and empty), and the offline banner.
```

with:

```markdown
- **Type:** Figtree, an open-licence geometric sans close to the lettering in the Mr D screenshots. It's bundled with the app rather than downloaded, so it works offline: one variable font covers every weight from API 26, the app's minimum. Headings are extra-bold, titles bold, body text regular and labels semi-bold.
- **Shapes and spacing:** fields and image cards have 12 dp rounded corners; chips and pills are fully rounded. Spacing steps are 4, 8, 12, 16 and 24 dp.
- **Icons:** Material Symbols (rounded), committed as vector drawables.
- **Theme:** `MrdTheme` themes Material 3 with these tokens, and adds the roles Material has no slot for (links, Open, Closed and photo placeholders).
- **Components:** search field, filter chip, ETA pill, tag, status label, rating, favourite and back buttons, photo placeholder, restaurant card, menu category header and item row, skeleton card and rows, state screens (error and empty), and the offline banner. They take display-ready text, not model types; `:core:ui` formats the model for them (milestone 5).
```

- [ ] **Step 3: Log the spike's catches.** The log is newest first. In `docs/ai/corrections.md`, insert these entries directly after the introduction, above the newest entry, with a blank line after each:

```markdown
## 2026-11-19 · Stripping an icon attribute by line broke the back arrow

- **What:** milestone 4's spike removed each Material Symbols icon's `android:tint` line, because the attribute needs AppCompat. In `arrow_back`, `android:autoMirrored` comes after the tint, so the `>` that closed the tag went with it, and the attribute became stray text.
- **Caught by:** lint's `ExtraText` error, in the spike's gate. The spike's own check, parsing each file as XML, passed, because stray text is valid XML.
- **Fix:** the download removes the attribute, not the line, and each icon is checked against a recorded SHA-256 hash.
- **Lesson:** check a generated file with the tool that consumes it. "Well-formed" isn't "valid for Android".

## 2026-11-19 · The restyled lists scrolled under the status-bar icons

- **What:** the restyled placeholders draw edge to edge, so their content scrolled behind the transparent status bar: Browse's cards always, and Details' menu at 200% font, where the text ran into the clock. Over a dark photo, the dark icons would have disappeared.
- **Caught by:** screenshots of the scrolled screens in milestone 4's spike; the first frames looked fine.
- **Fix:** a strip in the surface colour sits behind the status bar: always on Browse, and on Details once its header, which belongs under the status bar, has scrolled away.
- **Lesson:** walk a screen in motion and at 200% font, not just its first frame.
```

- [ ] **Step 4: Commit**

```bash
git add README.md docs/specs/2026-11-18-restaurant-discovery-design.md docs/ai/corrections.md
git commit -m "docs: credit Figtree and the icons, and describe the design system" \
  -m "The README credits Figtree under the SIL Open Font License, whose text is in core/designsystem/FIGTREE-OFL.txt, and Material Symbols under Apache 2.0, and links the official guides for the design system's techniques. Spec section 6.1 now describes the variable font, the icons, MrdTheme and the full component list. The corrections log records the spike's two catches." \
  -m "Verified: n/a (docs). Every link was opened and resolves."
```

---

### Task 8: Verification and the milestone pull request

**Files:**
- Create: `docs/qa/<date>-m4-design-system.md` and `docs/qa/screenshots/m4-*.png` (written by the verifier)
- Modify: `CLAUDE.md` ("Current state")

Franco merges as soon as CI is green, so the pull request is opened last: after the verifier's report is committed, and with its whole description written.

- [ ] **Step 1: Run the full gate**

```bash
./gradlew spotlessCheck :app:lintDebug testDebugUnitTest :app:assembleRelease
test -s app/build/outputs/mapping/release/mapping.txt && test ! -s app/build/outputs/mapping/release/missing_rules.txt && echo "R8 ran; no missing keep rules"
tail -1 app/build/reports/lint-results-debug.txt
ls -l app/build/outputs/apk/release/app-release.apk
```

Expected: `BUILD SUCCESSFUL`, `R8 ran; no missing keep rules` and `No issues found.` The APK is about 1.42 MB (the spike's was 1,422,296 bytes).

- [ ] **Step 2: Push the branch.** No pull request yet.

```bash
git push -u origin feature/design-system
```

- [ ] **Step 3: Run the verifier.** Dispatch a general-purpose subagent. Its prompt is the body of `.claude/agents/verifier.md` (everything after the front matter), followed by this request:

> Verify milestone 4 (design system) on branch `feature/design-system` against `docs/plans/2026-11-19-04-design-system.md`. The criteria:
> 1. **AC-18, for the components.** On Browse, `uiautomator dump` shows each sample card as one item whose description follows spec §6.5, with no separate heart item. The Open now chip, the hearts, the back button and the search field are at least 48 dp tall (node bounds divided by the screen density). `ContrastTest` passes, and fails when a text colour is lightened (restore it afterwards). At 200% font on a 360 dp-wide screen, nothing on Browse or Details is cut off or overlaps.
> 2. **AC-19, for the components.** Browse and Details render correctly in dark theme, and the status-bar icons are readable in light and dark, including over Browse's scrolled list and over Details scrolled to the end at 200% font.
> 3. **The Mr D look.** Browse has the "Restaurants" title, a grey rounded search field, the Open now chip (filled with ink when on, outlined when off) and four cards, each with a 16:9 placeholder, an ETA pill bottom-left, a heart top-right, the name, the cuisines and a details row. The closed card is dimmed with "Closed · Opens 07:30", free delivery is highlighted, and the unrated restaurant shows "New". Details has back and favourite buttons over its header, and a sample menu with a sold-out dish. Tapping a card opens Details, and Back returns.
> 4. **Figtree and the icons are bundled.** The release APK contains the 62,712-byte font (`unzip -lv`), text still renders in Figtree in airplane mode, and `core/designsystem/FIGTREE-OFL.txt` matches `ofl/figtree/OFL.txt` in `google/fonts`.
> 5. **The launcher icon** is adaptive with a monochrome layer, isn't Mr D's logo, and lint no longer reports `MissingApplicationIcon`.
> 6. **House rules.** `ArchitectureTest` passes. No feature or `:app` Kotlin file contains a `Color(` value, and the words users see come from string resources. The sample names and cuisines in `SampleRestaurants.kt` are data, not copy.
> 7. **The gate passes:** `./gradlew spotlessCheck :app:lintDebug testDebugUnitTest :app:assembleRelease`, then `test -s app/build/outputs/mapping/release/mapping.txt && test ! -s app/build/outputs/mapping/release/missing_rules.txt`.
>
> Save these screenshots: `docs/qa/screenshots/m4-browse-light.png`, `m4-browse-dark.png`, `m4-details-light.png`, `m4-details-dark.png`, `m4-browse-200.png` (360 dp wide: `adb shell wm density 480` on a 1080-pixel-wide screen, and `adb shell settings put system font_scale 2.0`) and `m4-browse-landscape.png` (`adb shell settings put system user_rotation 1`, with auto-rotate off). Capture each with `adb exec-out screencap -p > <file>`. Put every setting back afterwards: `cmd uimode night no`, `font_scale 1.0`, `wm density reset` and `user_rotation 0`. Quote the exact commands you ran and their output. Write `docs/qa/<today's date, YYYY-MM-DD>-m4-design-system.md`.

Expected: the report's first line is **PASS**. If it's FAIL, fix the cause in a new commit, then run the verifier again.

- [ ] **Step 4: Update "Current state" in `CLAUDE.md`.** Rewrite the section's paragraph so it names the milestones merged into `dev` by then, and add this sentence, with the report's real date for `<date>`:

```markdown
Milestone 4 (design system) is done: `:core:designsystem` has Mr D's tokens, Figtree, `MrdTheme` and the components, `:app` has the launcher icon, and the Browse and Details placeholders use them until milestones 5 and 6 replace them. Its verification report is `docs/qa/<date>-m4-design-system.md`.
```

- [ ] **Step 5: Commit and push**

```bash
git add docs/qa CLAUDE.md
git commit -m "docs(qa): add the milestone 4 verification report" \
  -m "The verifier's evidence and screenshots for the design system milestone, and CLAUDE.md's current state." \
  -m "Verified: the verifier's report is PASS."
git push
```

- [ ] **Step 6: Open the pull request with its full description.** First, if a review in this milestone caught something the AI got wrong, add it to `docs/ai/corrections.md` in its own commit and push. Links in the description are absolute and pinned to the branch's head commit, because the branch is deleted when it merges.

```bash
SHA=$(git rev-parse HEAD)
REPORT=$(ls docs/qa/*-m4-design-system.md)
cat > "${TMPDIR:-/tmp}/m4-pr.md" <<'BODY'
## What and why

Milestone 4 of the [roadmap]({blob}/docs/plans/2026-11-18-00-roadmap.md): Mr D's look, visible early. The milestone 1 placeholder didn't look like the Mr D app, and its rows lacked detail.
- **`:core:designsystem`:**
  - Mr D's colours, sampled from the Play Store listing, in light and dark;
  - Figtree, bundled as one variable font;
  - 12 dp corners, pills, and 4 to 24 dp spacing;
  - `MrdTheme`, which themes Material 3 and adds links, Open, Closed and photo-placeholder colours.
- **Components:** the search field, filter chip, ETA pill, tag, status label, rating, favourite and back buttons, photo placeholder, restaurant card, menu category header and item row, skeletons, state screen and offline banner. They're stateless, take display-ready text rather than model types, and have previews at 360 dp in light, dark and 200% font.
- **Accessibility:** each restaurant card is one TalkBack item with a favourite action; touch targets are 48 dp; every text colour meets WCAG AA in both themes, which `ContrastTest` proves.
- **Launcher icon:** original, in Mr D's sky and ink, with a monochrome layer. It isn't Mr D's logo.
- **Placeholders:** Browse has the real header and four sample restaurants from the mock API. Details shows a sample menu. Milestones 5 and 6 replace both.

## How it was verified

- `./gradlew spotlessCheck :app:lintDebug testDebugUnitTest :app:assembleRelease` passes, R8 ran with no missing keep rules, and lint reports no issues (`dev` had a `MissingApplicationIcon` warning).
- 5 new tests: `ContrastTest` and `ColorResourcesTest`. Both were broken on purpose and failed.
- Emulator walk: light, dark, 200% font at 360 dp wide, and landscape. `uiautomator dump` shows one TalkBack item per card.
- The release APK grew from 979 KB to 1.42 MB. The font is 35 KB of that; most of the rest is Compose's text field and lazy grid, used here for the first time and needed by milestone 5 anyway.
- The verifier's report is PASS: [{report}]({blob}/{report}).

## Screenshots

| Browse | Details | Dark | 200% font, 360 dp |
|---|---|---|---|
| ![Browse]({raw}/docs/qa/screenshots/m4-browse-light.png) | ![Details]({raw}/docs/qa/screenshots/m4-details-light.png) | ![Browse, dark]({raw}/docs/qa/screenshots/m4-browse-dark.png) | ![Browse at 200%]({raw}/docs/qa/screenshots/m4-browse-200.png) |

![Browse in landscape]({raw}/docs/qa/screenshots/m4-browse-landscape.png)

## AI notes

Planned and carried out with Claude Code, from [the milestone plan]({blob}/docs/plans/2026-11-19-04-design-system.md). A throwaway spike built the plan's code first and caught two mistakes: an icon download that broke the back arrow's XML, and a list that scrolled under the status-bar icons. Both are in [docs/ai/corrections.md]({blob}/docs/ai/corrections.md), along with anything the reviews caught.
BODY
perl -pi -e "s|\{blob\}|https://github.com/Effekt3r/mrd-restaurant-discovery/blob/$SHA|g; s|\{raw\}|https://raw.githubusercontent.com/Effekt3r/mrd-restaurant-discovery/$SHA|g; s|\{report\}|$REPORT|g" "${TMPDIR:-/tmp}/m4-pr.md"
gh pr create --base dev --head feature/design-system --title "Milestone 4: design system" --body-file "${TMPDIR:-/tmp}/m4-pr.md"
gh pr checks --watch
```

Expected: both checks, **Build and test** and **Secret scan**, pass. Franco reviews and merges.
