**PASS**

Verified branch `feature/design-system` at commit `cf27fbd82a127fe5dfd39ca8563bcc04082a0309` on 2026-11-19. That's `git rev-parse HEAD`, equal to `origin/feature/design-system`; `git log origin/dev..HEAD` shows the plan and 7 implementation commits. Checked against `docs/plans/2026-11-19-04-design-system.md` and spec §6.1, §6.2, §6.3, §6.5 and §6.6, using the criteria the milestone's controller set. The working tree stayed clean apart from this report and its screenshots. The mutation probes in §2 ran in a disposable clone of the pushed branch, never in the worktree.

## Criteria

| # | Criterion | Verdict | Evidence |
|---|---|---|---|
| 1 | The full gate passes, R8 ran with no missing rules, lint reports no issues and `MissingApplicationIcon` is gone; test counts and APK size reported | PASS | §1: `BUILD SUCCESSFUL`, 529 of 529 tasks executed (`--rerun-tasks`); the R8 check exits 0; `No issues found.`; 70 tests, 0 failed; release APK 1,422,296 bytes |
| 2 | Tokens: `ContrastTest` proves contrast; sky never appears as text on white; raw colours appear only in `:core:designsystem`; `ArchitectureTest` passes | PASS | §2: `ContrastTest` 4/4 and `ArchitectureTest` 7/7 pass; three mutation probes each make a test fail; sky is only ever a fill behind ink |
| 3 | The Mr D look on the release APK: the Browse header, §6.2's cards and their variants, Details with the sample menu and a sold-out dish, and Back returning to Browse | PASS | §3 and the screenshots; the card photo measures 996 × 560 px (1.779, against 1.778 for 16:9) with a 31 px corner (12 dp) |
| 4 | Accessibility (§6.5): one TalkBack item per card; the heart, the chip and the back button at least 48 dp; nothing overflows at 200% font on a 360 dp-wide screen | PASS | §4: four `Button` nodes with no children; chip 97.1 × 48.0 dp, back and heart 48.0 × 48.0 dp; both screens walked at 200% on 360 dp |
| 5 | The launcher icon (§6.6): original, not Mr D's logo, adaptive, with a monochrome layer | PASS | §5: `aapt2` shows an `adaptive-icon` with background, foreground and monochrome layers; `m4-launcher-icon.png` |
| 6 | Licences and docs: `FIGTREE-OFL.txt` present; the README credits Figtree, Material Symbols and TheMealDB; spec §6.1 describes the components as built; every new documentation link resolves | PASS | §6: the OFL is byte-identical to `google/fonts`; 10 of 10 URLs return HTTP 200 |

## Evidence

### §1 The gate — PASS

```
$ export JAVA_HOME=$(/usr/libexec/java_home -v 21)
$ ./gradlew spotlessCheck :app:lintDebug testDebugUnitTest :app:assembleRelease --rerun-tasks
...
> Task :app:packageRelease
MRD_KEYSTORE_FILE is not set, so this release APK is signed with the debug key.

BUILD SUCCESSFUL in 41s
529 actionable tasks: 529 executed
(exit 0)
$ test -s app/build/outputs/mapping/release/mapping.txt && test ! -s app/build/outputs/mapping/release/missing_rules.txt
(exit 0)
```

`--rerun-tasks` makes every task, including the tests, lint and R8, run in this session instead of being skipped as up to date or loaded from the build cache. `mapping.txt` is 30,144,073 bytes, and there's no `missing_rules.txt`. The log has no compiler warnings (`grep -E '^w: |warning:'` finds nothing).

**Lint:**

```
$ tail -1 app/build/reports/lint-results-debug.txt
No issues found.
$ grep -c MissingApplicationIcon app/build/reports/lint-results-debug.txt app/build/reports/lint-results-debug.xml
app/build/reports/lint-results-debug.txt:0
app/build/reports/lint-results-debug.xml:0
```

The XML report contains no `<issue>` element. On `dev`, `MissingApplicationIcon` was lint's one warning (`docs/qa/2026-11-18-m1-foundation.md`, Observations), because `dev`'s manifest has no `android:icon`. This branch adds `android:icon="@mipmap/ic_launcher"`.

**Tests**, from this run's JUnit XML (every suite timestamped 2026-11-19T06:43Z):

| Suite | Tests | Failed |
|---|---|---|
| `ContrastTest` (new): `textMeetsAaInTheLightTheme`, `textMeetsAaInTheDarkTheme`, `theOffChipOutlineIsVisibleInBothThemes`, `skyFailsAsTextOnWhiteSoItIsOnlyEverAFill` | 4 | 0 |
| `ColorResourcesTest` (new): `xmlColoursMatchThePalette` | 1 | 0 |
| `ArchitectureTest` | 7 | 0 |
| `:tools:datagen`, 9 suites | 58 | 0 |
| **Total** | **70** | **0** (0 skipped) |

**Release APK:** `app/build/outputs/apk/release/app-release.apk` is **1,422,296 bytes**, the same as the plan's spike. The plan gives 979,364 bytes for `dev`, so the milestone adds 442,932. Resource shrinking renamed Figtree, but it's there: `unzip -lv` lists `res/7C.ttf` at 62,712 bytes (35,171 compressed).

### §2 Tokens — PASS

**Contrast is proven by tests.** `ContrastTest` passes all four tests (§1), and the committed file is identical to the plan's code block.

**The tests fail when they should.** Three mutation probes ran in a disposable clone of the pushed branch. Each edit was reverted afterwards, and the clone's `git status --porcelain` was empty.

| Probe | Command | Result |
|---|---|---|
| In `Color.kt`, `InkMuted` lightened from `0xFF5C6670` to `0xFF8A939B` | `./gradlew :core:designsystem:testDebugUnitTest` | exit 1. `textMeetsAaInTheLightTheme` fails with: `Text below WCAG AA's 4.5:1:` / `secondary text on the screen: 3.12:1` / `the search field's placeholder: 2.60:1` |
| In `colors.xml`, `designsystem_sky` changed to `#FF6DCFF7` | the same | exit 1. `ColorResourcesTest`: `expected:<#FF6DCFF[6]> but was:<#FF6DCFF[7]>`. Gradle re-ran the test without `--rerun`, so `colors.xml` is an input. |
| In `BrowseScreen.kt`, `private val probeColour = Color(0xFF000000)` added | `./gradlew :app:testDebugUnitTest --tests com.effekt3r.mrd.ArchitectureTest` | exit 1. `rawColoursOnlyAppearInTheDesignSystem` names `/feature/browse/.../BrowseScreen.kt:171: private val probeColour = Color(0xFF000000)` |

**Sky never appears as text on white.**

```
$ git grep -nE 'MrdPalette\.Sky\b|primaryContainer|inversePrimary|designsystem_sky' -- '*.kt' '*.xml' ':!**/test/**'
app/src/main/res/mipmap-anydpi/ic_launcher.xml:5:    <background android:drawable="@color/designsystem_sky" />
core/designsystem/.../component/OfflineBanner.kt:35:            .background(colors.primaryContainer, MrdShapes.Field)
core/designsystem/.../component/Pills.kt:74:            .background(MaterialTheme.colorScheme.primaryContainer, MrdShapes.Pill)
core/designsystem/.../theme/Color.kt:75:    primaryContainer = MrdPalette.Sky,
core/designsystem/.../theme/Color.kt:93:    inversePrimary = MrdPalette.Sky,
core/designsystem/.../theme/Color.kt:103:    primaryContainer = MrdPalette.Sky,
core/designsystem/.../theme/Color.kt:121:    inversePrimary = MrdPalette.Ink,
core/designsystem/src/main/res/values/colors.xml:5:    <color name="designsystem_sky">#FF6DCFF6</color>
```

- Sky is `primaryContainer` in both schemes. Both of its uses are fills behind `onPrimaryContainer`, which is ink: `MrdTag` (`Pills.kt:71-74`) and `MrdOfflineBanner` (`OfflineBanner.kt:35-44`).
- It's also the light scheme's `inversePrimary`. No code uses that role, and Material puts it on `inverseSurface`, which is ink (9.6:1).
- In XML, sky is only the launcher icon's background.
- Links use `SkyDeep` and `SkyPale`, not sky.
- `skyFailsAsTextOnWhiteSoItIsOnlyEverAFill` pins sky on white below 2:1.
- On the emulator, "Free delivery" and "New" are ink on sky.

**Raw colours appear only in `:core:designsystem`.**
- `ArchitectureTest.rawColoursOnlyAppearInTheDesignSystem` passes, and the probe above shows it catches a raw colour in a feature.
- Both of these return nothing:
  - `git grep -nE 'Color\(|Color\.(Black|White|Red|Green|Blue|Gray|Grey|Yellow|Cyan|Magenta|DarkGray|LightGray)|0xFF[0-9A-Fa-f]{6}' -- '*.kt' ':!core/designsystem' ':!**/test/**'`
  - `git grep -nE '#[0-9A-Fa-f]{6,8}' -- app/src/main/res feature`
- `:app`'s themes and launcher drawables use `@color/designsystem_*`.

### §3 The Mr D look on the release APK — PASS

The emulator is `mrd_api36`: API 36, 1080 × 2400 px at 420 dpi, with gesture navigation.

```
$ adb -s emulator-5556 install -r app/build/outputs/apk/release/app-release.apk
Performing Streamed Install
Success
$ adb -s emulator-5556 shell dumpsys package com.effekt3r.mrd | grep pkgFlags
    pkgFlags=[ HAS_CODE ALLOW_CLEAR_USER_DATA ]
$ adb -s emulator-5556 shell am start -W -n com.effekt3r.mrd/.MainActivity
Activity: com.effekt3r.mrd/.MainActivity
TotalTime: 457
Complete
```

`pkgFlags` has no `DEBUGGABLE`, so this is the release build.

**Browse header** (`m4-browse.png`): the "Restaurants" title in extra-bold Figtree, the grey rounded search field with its search icon and "Search restaurants", and the Open now chip, outlined while it's off.

**Cards** (`m4-browse.png`, then scrolled):
- Each card has a 16:9 photo area with rounded corners, the white ETA pill bottom-left ("32 mins") and the heart in a white circle top-right.
- Below the photo: the name, the cuisines ("Steakhouse · Grill") and the details row ("★ 4.6 · R15 delivery · Open", with Open in green).
- Measured on the full-resolution screenshot, the first photo's fill is 996 × 560 px: a ratio of 1.779, against 1.778 for 16:9. On its top row, the fill starts 31 px in from the edge; a 12 dp radius at 420 dpi is 31.5 px.

**Variants.** The four samples match `mock-api/v1/restaurants.json`: the names, cuisines, ratings, fees and ETAs of r-1001, r-1015, r-1010 and r-1070.
- **Closed:** Kirstenbosch Coffee Bar's photo is dimmed, and its row ends "Closed · Opens 07:30" in red.
- **Free delivery:** Woodstock Taverna's "Free delivery" is an ink-on-sky tag.
- **New:** Kommetjie Ramen Bar has no rating, so it shows a "New" tag.
- **No photo:** every card shows the cuisine-coloured placeholder with the knife-and-fork icon. r-1010 has no image in the API; the other photos arrive with milestone 5.

**Details** (`m4-details.png`, `m4-details-menu.png`). Tapping a card opens Details:
- a full-width placeholder header under the status bar, with white circular back and favourite buttons;
- "Cape Town Grill", the cuisines, "★ 4.6 · 32 mins · R15 delivery" and "Open · Closes 22:00" in green;
- the "Menu" heading, then Starters, Steaks and Desserts, with descriptions, options, prices and 64 dp thumbnails;
- "Rump 250 g", greyed out, with "R189" and a red "Sold out".

As its KDoc says, Details shows Cape Town Grill whichever card is tapped.

**Back.** `adb -s emulator-5556 shell input keyevent KEYCODE_BACK` returned to Browse at the same scroll position: the dump showed the same three cards as before Details opened. Details' own back button also returned to Browse.

**Dark theme** (`m4-browse-dark.png`, `m4-details-dark.png`): ink surfaces, white text, a raised grey search field, dark placeholders and light status-bar icons; the tags stay ink on sky. A heart toggled earlier, on Kirstenbosch Coffee Bar, stayed filled through the dark-mode, density and font-scale changes.

**Landscape** (`m4-browse-landscape.png`). Rotated with `settings put system user_rotation 1`, with auto-rotate off: a two-column grid, and the content clears the display cutout on the left (136 px).

**Offline.**
- `cmd connectivity airplane-mode enable` turned on airplane mode; `ping` then returned `connect: Network is unreachable`.
- A cold start (`am force-stop`, then `am start -W`: `Status: ok`) rendered Browse exactly as before, in Figtree, which is bundled in the APK.
- `cmd connectivity airplane-mode disable` turned it off again.

### §4 Accessibility — PASS

From the `uiautomator dump` of Browse, at 420 dpi (1 dp = 2.625 px):

```
EditText  [42,305][1038,432]  379.4x48.4dp  clickable focusable          (search field)
View      [42,465][297,591]   97.1x48.0dp   clickable checkable focusable (Open now chip; child TextView 'Open now')
Button desc='Cape Town Grill. Steakhouse, Grill. Rated 4.6. 32 minutes. R15 delivery. Open.'  [42,653][1038,1426]  children=0
Button desc='Woodstock Taverna. Greek. Rated 4.6. 40 minutes. Free delivery. Open.'  children=0
Button desc='Kirstenbosch Coffee Bar. Café, Breakfast. Rated 4.6. 16 minutes. R12 delivery. Closed, opens 07:30.'  children=0
Button desc='Kommetjie Ramen Bar. Japanese. New. 43 minutes. R25 delivery. Open.'  children=0
```

**Each card is one TalkBack item.** Each is a single clickable `Button` node, with its description in spec §6.5's order and no child nodes, so the heart has no node of its own. The favourite is a custom accessibility action in the code (`RestaurantCard.kt:88-93`). `uiautomator` doesn't list custom actions, so I didn't observe it on the device.

**Touch targets:**
- **Open now chip:** 97.1 × 48.0 dp.
- **Details' buttons:**
  - The back button is `[21,157][147,283]` and the heart `[933,157][1059,283]`, both 48.0 × 48.0 dp.
  - At 480 dpi they're `[24,160][168,304]` and `[912,160][1056,304]`, still 48.0 × 48.0 dp.
- **A card's heart:**
  - By design, it has no node in the dump: it's part of the card's single item.
  - Its box is `size(48.dp)` (`CircleButtons.kt:65`).
  - On the device, I tapped 55 px (21 dp) left of its centre, outside the 36 dp white circle. The heart toggled to filled, and Browse stayed open.

**200% font on a 360 dp-wide screen** (`m4-browse-200.png`, `m4-browse-200-scrolled.png`, `m4-details-200.png`):

```
$ adb -s emulator-5556 shell wm size 1080x2400
$ adb -s emulator-5556 shell wm density 480
$ adb -s emulator-5556 shell settings put system font_scale 2.0
$ adb -s emulator-5556 shell wm density
Physical density: 420
Override density: 480
$ adb -s emulator-5556 shell settings get system font_scale
2.0
```

I scrolled Browse through all four cards, and Details to the end of its menu. Nothing is cut off or overlaps, and every node lies within the screen's 1,080 px width.
- **Browse:** names and cuisines fit on their lines. The closed card's details row wraps cleanly onto a second line ("Closed · Opens 07:30").
- **Details:** the stats row wraps after the rating, and dish descriptions end with an ellipsis at their designed two lines.
- **Status bar:** whenever content scrolls under it, a surface-coloured strip keeps its icons on a plain background. That holds on Browse's scrolled list, and on Details scrolled to the end at 200%.

Afterwards, `wm size reset`, `wm density reset` and `settings put system font_scale 1.0` restored the screen.

**Headings and roles**, checked in the code only, because the dump doesn't show them:
- "Restaurants", the restaurant's name, "Menu", the menu categories and the state screen's title are marked `heading()`.
- The chip and the favourite button are `toggleable` with `Role.Checkbox`, and the favourite also has a state description.
- The back button has `Role.Button` and the label "Back".

### §5 The launcher icon — PASS

```
$ aapt2 dump badging app/build/outputs/apk/release/app-release.apk
application: label='Mr D Discover' icon='res/E4.xml'
$ aapt2 dump xmltree --file res/E4.xml app/build/outputs/apk/release/app-release.apk
N: android=http://schemas.android.com/apk/res/android (line=4)
  E: adaptive-icon (line=4)
      E: background (line=5)
        A: http://schemas.android.com/apk/res/android:drawable(0x01010199)=@0x7f040005
      E: foreground (line=6)
        A: http://schemas.android.com/apk/res/android:drawable(0x01010199)=@0x7f06000f
      E: monochrome (line=7)
        A: http://schemas.android.com/apk/res/android:drawable(0x01010199)=@0x7f060010
```

`m4-launcher-icon.png` shows the app drawer: an ink map pin holding a white knife and fork, on Mr D's sky blue.
- **Original:** the pin is a path drawn in `ic_launcher_foreground.xml`, with Material Symbols' "restaurant" glyph inside it.
- **Not Mr D's logo:** there's no lettering, so it isn't Mr D's wordmark.
- **Monochrome layer:** `ic_launcher_monochrome.xml` is the pin with a round hole. I didn't switch on themed icons to see it tinted.

### §6 Licences and docs — PASS

**Licences and assets:**
- `core/designsystem/FIGTREE-OFL.txt` is present (4,388 bytes) and byte-identical to `ofl/figtree/OFL.txt` in `google/fonts`, both at the plan's pinned commit `a60a77e1` and on `main`. `cmp` reports no difference, and its SHA-256 is `140d37233e7f…f7f657a268`.
- The font matches the pinned `Figtree[wght].ttf`: SHA-256 `26ad3db9b31f…bfe6fb714`, 62,712 bytes.
- All nine icons are byte-identical to a fresh run of the plan's download from `google/material-design-icons` at `10ecc147`.

**README:** a new "Credits" section credits:
- Figtree, under the SIL OFL 1.1, linking `FIGTREE-OFL.txt`;
- Material Symbols, under Apache 2.0;
- TheMealDB, linking `mock-api/README.md`.

Both relative links point to files that exist.

**Spec §6.1** matches the code: "Type" (one variable font, with a weight per role), "Icons", "Theme" and the component list. Every component it lists exists as a public composable:
- `MrdSearchField`, `MrdFilterChip`, `MrdEtaPill`, `MrdTag`, `MrdStatusLabel` and `MrdRating`;
- `MrdFavouriteButton`, `MrdBackButton`, `MrdImagePlaceholder` and `MrdRestaurantCard`;
- `MrdMenuCategoryHeader`, `MrdMenuItemRow`, `MrdSkeletonCard` and `MrdSkeletonRow`;
- `MrdStateScreen` and `MrdOfflineBanner`.

One wording nit is under Code review (finding 10).

**Links.** Every URL the branch adds to the documentation resolves. Checked with `curl -sS -L -w '%{http_code} %{num_redirects} %{url_effective}'`:

| URL | Result | Page title |
|---|---|---|
| `https://developer.android.com/develop/ui/compose/designsystems/custom` | 200 | Custom design systems in Compose |
| `https://developer.android.com/develop/ui/compose/text/fonts` | 200 | Work with fonts |
| `https://developer.android.com/develop/ui/compose/graphics/images/material` | 200 | Icons |
| `https://developer.android.com/develop/ui/views/launch/icon_design_adaptive` | 200, after 1 redirect to `/develop/ui/compose/system/icon_design_adaptive` | Adaptive icons |
| `https://developer.android.com/develop/ui/compose/accessibility` | 200 | Accessibility in Jetpack Compose |
| `https://fonts.google.com/icons` | 200 | Material Symbols and Icons - Google Fonts |
| `https://github.com/google/fonts/tree/main/ofl/figtree` | 200 | fonts/ofl/figtree at main · google/fonts |
| `https://www.themealdb.com/api.php` | 200 | TheMealDB.com - Free Recipe API |
| `https://github.com/erikdkennedy/figtree` (in the OFL text) | 200 | erikdkennedy/figtree |
| `http://scripts.sil.org/OFL` (in the OFL text) | 200, after 2 redirects to `https://openfontlicense.org/` | SIL Open Font License |

## Code review

This covers the diff `git diff origin/dev...HEAD -- core/designsystem app feature`. Below, `designsystem/` stands for `core/designsystem/src/main/kotlin/com/effekt3r/mrd/core/designsystem/`. None of these findings breaks a criterion.

### Important

1. **`CircleButton` paints its own background but inherits its icon's colour.** (`designsystem/component/CircleButtons.kt:67-70`)
   - **The problem:** the circle is `colorScheme.surface`, but the `Icon` takes `LocalContentColor`, which only matches under a `Surface`. In the app, it works because `MainActivity` wraps everything in a `Surface`.
   - **Where it breaks:** in Material 3 1.4.0, `LocalContentColor` defaults to `Color.Black`, and `MaterialTheme` doesn't provide it.
     - Outside any `Surface`, the icons are black. That already shows in this file's own `CircleButtonsPreview` (`:75-89`), which has no `Surface`: its Dark preview draws black icons on an ink circle.
     - Inside a `Surface` whose content colour differs from `onSurface`, such as `primary`, the icon would be white on a white circle in the light theme.
   - **Why it matters:** these buttons are meant to sit over photos in any container, in milestones 5 and 6. Every other component that paints a background pairs it with the matching content colour: `MrdEtaPill`, `MrdTag`, `MrdFilterChip` and `MrdOfflineBanner`.
   - **Fix:** `tint = MaterialTheme.colorScheme.onSurface`, or draw the circle with `Surface(shape = CircleShape)`.

### Minor

2. **Details' back button gets the start inset twice in landscape.** (`feature/details/src/main/kotlin/com/effekt3r/mrd/feature/details/RestaurantScreen.kt:96-100`)
   - The button is padded by `safeDrawing.only(Top + Start)`, but the `LazyColumn`'s `contentPadding` (`:86-88`) already includes the horizontal `safeDrawing` insets.
   - With the display cutout on the left (136 px), the button sits 293 px from the screen's edge, while the title sits at 178 px (`m4-details-landscape.png`). The favourite button (`:108-110`) correctly uses `Top` only.
   - Because of the same content padding, the header photo doesn't run under the cutout in landscape.
   - This placeholder goes in milestone 6, but the pattern shouldn't carry over.

3. **The details row can leave a separator dangling at the end of a line.** (`designsystem/component/RestaurantCard.kt:128-144`)
   - Each `Separator()` is its own `FlowRow` item, so when the row wraps, the "·" stays behind at the end of the first line. At 200% on 360 dp, the closed card reads "★ 4.6 · R12 delivery ·" and then "Closed · Opens 07:30" (`m4-browse-200-scrolled.png`).
   - Keeping each separator in the same item as the part that follows it would move them together.
   - Details' placeholder has the mirror image: `details_sample_eta_and_fee` starts with "· " (`feature/details/src/main/res/values/strings.xml:9`), so at 200% its wrapped line starts with a dot.

4. **`MrdRating` gives TalkBack no context.** (`designsystem/component/RestaurantInfo.kt:53-54`)
   - The star is decorative and the text is just "4.6".
   - Wherever the rating isn't inside a merged card, as on Details (`RestaurantScreen.kt:134`), TalkBack reads a bare "4.6".
   - A label such as "Rated 4.6", from a string resource, set on the row's semantics, would fix it for every caller.

5. **Material's unset colour roles keep their default purple.** (`designsystem/theme/Color.kt:72-126`)
   - The schemes set every role in use, but these keep Material's baseline values:
     - `secondaryContainer` and `onSecondaryContainer`;
     - the `tertiary` roles;
     - `errorContainer` and `onErrorContainer`;
     - `surfaceBright` and `surfaceDim`.
   - Nothing uses them yet. But if milestone 5 or 6 uses one of Material's own components that reads them, it will show purple: a selected `FilterChip`, a `FilledTonalButton`, the `NavigationBar` indicator or a `SegmentedButton`.
   - Mapping them to Mr D's tokens closes the gap.

6. **Raw sizes outside the spacing scale.**
   - `Dimens.kt:6` says "Layouts use these, never other values", and spec §6.1 lists steps from 4 to 24 dp. Yet `MrdTag` pads by `2.dp` (`Pills.kt:75`), and `MrdRating` spaces its star by `2.dp` (`RestaurantInfo.kt:50`).
   - Other literals are sizes rather than spacing:
     - `48.dp`, `36.dp` and `20.dp` in `CircleButtons.kt:65-70`;
     - `40.dp`, `16.dp` and `64.dp` in the placeholder, the rating and the menu row;
     - `300.dp` in `BrowseScreen.kt:83`, and `240.dp` in `RestaurantScreen.kt:91`.

     A small `MrdSizes` object, with names like `TouchTarget` and `Thumbnail`, would name them.
   - The dimming alpha is defined twice: `CLOSED_PHOTO_ALPHA = 0.45f` in `RestaurantCard.kt:171`, and a literal `0.45f` in `Menu.kt:100`.

7. **A new lambda on every call stops a row from skipping recomposition.** (`RestaurantScreen.kt:161`, `:170-172`)
   - `photoPlaceholder(seed = name)` returns a fresh lambda each time it's called, so `MrdMenuItemRow` always gets a new `image` and can't skip when its item recomposes.
   - That's negligible for a static placeholder. In milestone 6, remember the slot, or pass the image's URL and build the image inside the row.

8. **`MrdFavouriteButton`'s callback is named as if it were a plain click.** (`designsystem/component/CircleButtons.kt:33`, `:42`)
   - The button is a toggle, with `toggleable` and `Role.Checkbox`, but its callback is `onClick: () -> Unit`, and it discards the new value (`onValueChange = { onClick() }`).
   - `onFavouriteChange: (Boolean) -> Unit`, as in Material's `IconToggleButton`, would say what it does. The card's `onFavouriteClick` has the same issue.

9. **An announcement claim that nothing has tested.** (`designsystem/component/OfflineBanner.kt:25-27`, `:36`)
   - The KDoc says TalkBack announces the banner "when it appears". But a polite live region normally announces changes to a node that's already on screen, not a new node appearing.
   - No placeholder shows the banner, so milestone 5's TalkBack walk should confirm it.

10. **Documentation nits.**
    - **Title weights:** spec §6.1 and `MrdTypography`'s KDoc (`Type.kt:33`) say titles are bold, but `titleLarge` is extra-bold (`Type.kt:42`). Since it's used for headings (`MrdMenuCategoryHeader`, `MrdStateScreen`), the code makes sense; the wording should match it.
    - **The adaptive-icons link** redirects to `https://developer.android.com/develop/ui/compose/system/icon_design_adaptive`. Linking that URL directly keeps the link working if the redirect is dropped.
    - **The Apache licence:** the Credits say Material Symbols are under Apache 2.0, but don't link the licence's text. Apache 2.0 §4(a) asks redistributors to give recipients a copy, and a link to the licence in `google/material-design-icons` would cover the repository.

## Observations

These are outside the criteria and don't affect the verdict.

- **Screenshot names.**
  - This report uses the names the controller asked for, `m4-browse.png` and `m4-details.png`.
  - The pull-request body in the plan's Task 8 (`docs/plans/2026-11-19-04-design-system.md:2693`) embeds `m4-browse-light.png` and `m4-details-light.png`, which don't exist. Its images will break unless the body is updated.
  - Additional screenshots:
    - `m4-browse-200-scrolled.png`: the closed card's row wrapping at 200%, the evidence for Code review finding 3;
    - `m4-details-menu.png`: the sold-out dish;
    - `m4-details-dark.png`;
    - `m4-details-200.png`: Details scrolled to the end at 200%;
    - `m4-details-landscape.png`: the evidence for Code review finding 2.
- **In the dark theme, the "white" ETA pill and heart circle are ink**, because they use `colorScheme.surface` (`m4-browse-dark.png`). They're readable and consistent; spec §6.2's "white" describes the light theme.
- **Details' status bar.** Spec §6.3 asks for a dark gradient behind the status-bar icons over the photo. The placeholder's header is a pale colour with dark icons, which read fine; the gradient belongs with milestone 6's real photos.
- **Emulator.**
  - The release APK is still installed on `emulator-5556`. It replaced the debug build that was there before, with the same package and the same debug signing key.
  - Every setting is back as found: `Night mode: no`, `font_scale` 1.0, `accelerometer_rotation` 0, `user_rotation` 0, airplane mode off, and `wm size` and `wm density` with no overrides (1080 × 2400 px, 420 dpi).
  - No other device was touched.
