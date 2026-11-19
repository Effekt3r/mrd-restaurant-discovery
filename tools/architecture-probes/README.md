# Architecture-test probes

These scripts plant deliberate violations next to allowed look-alikes. They prove that each rule in `app/src/test/kotlin/com/effekt3r/mrd/ArchitectureTest.kt` catches what it should, and nothing else.

- Run them from the repository root whenever a rule changes, and remove the probes afterwards.
- They only add and remove untracked probe files.
  - `remove-probes.sh` deletes exactly the files the create scripts write, then prunes directories left empty. It's safe in the main checkout, where `.worktrees/` holds real worktrees.
- Where the probes go:
  - **Probes in real modules** (`:core:common`, `:core:data`, `:core:model`) go in `src/probe/kotlin`. Konsist reports them under the right module, but Gradle never compiles that source set, so they can't break the build once these modules have real code.
  - **Feature probes** go in `feature/probe/`, which isn't a Gradle module. Their package must stay `…feature.probe`, because P1 works out a feature's own package from its directory name.
  - **The nested-checkout control** goes in `.worktrees/zz-architecture-probe-control/`.

| Script | What it plants |
|---|---|
| `create-p1-p7.sh` | One probe file per rule (P1 to P7), plus a violation inside a nested `.worktrees/` checkout that must be ignored |
| `create-p8.sh` | A public repository in `:core:data`'s `src/probe` folder, which no Gradle task compiles, so it's never on `:app`'s test classpath. It proves Gradle re-runs the tests instead of reporting them UP-TO-DATE. |
| `create-p9.sh` | A raw colour in a nested module (`feature/browse/impl`), and nothing else. It proves Gradle re-runs the tests for a change in a nested module. |
| `create-p10.sh` | Nullable-fee money conversions, a KDoc line that must not count, and `internal` ViewModel state |
| `remove-probes.sh` | Removes every probe and the empty directories the probes created |

## How to check

1. Run the probes:
   ```sh
   ./gradlew :app:testDebugUnitTest --tests "com.effekt3r.mrd.ArchitectureTest" --console=plain
   ```
   Add `--rerun` for P1 to P7 and P10. Leave it off for P8 and P9, because those also prove that Gradle notices the change on its own. Run P8 and P9 on their own, each after a run that passes.
2. Read the failures in `app/build/test-results/testDebugUnitTest/TEST-com.effekt3r.mrd.ArchitectureTest.xml`.
3. Run `remove-probes.sh` and confirm that `git status --porcelain` is empty.

## Expected results

**P1 to P7:** all seven tests fail, with exactly these entries.

| Rule | Flagged | Must not be flagged |
|---|---|---|
| `featureModulesNeverImportAnotherFeature` | `ProbeCrossFeature.kt` imports `…feature.details.navigation.RestaurantRoute` | the import of its own feature (`ProbeThing`) |
| `modelAndCommonModulesHaveNoAndroidDependencies` | `ProbeAndroid.kt` imports `androidx.annotation.VisibleForTesting` | |
| `rawColoursOnlyAppearInTheDesignSystem` | `ProbeColours.kt` lines 5 to 8 | `Color.Transparent`, `Color.Unspecified`, `lightenColor(0x20)` |
| `moneyIsNeverAFloatingPointNumber` | `ProbeMoney.kt` lines 3 to 7 | `priceAlpha`, `feedScrollOffset`, `dimAmount`, `rating`, `Long` cents, `cents / 100` |
| `viewModelsNeverExposeMutableState` | `uiState` (no explicit type), `events` (Channel), `selectedId` (public setter), `state()` (returns MutableStateFlow), `ProbeScreenState.count` | private state, `readOnly`, `query` with a private setter, `searchField` |
| `repositoryImplementationsAreInternal` | `RestaurantRepositoryImpl`, `RestaurantStore`, `CachedRestaurantRepository` | the interface, internal and private classes, `RepositoryModule` |
| `modelTypesAreImmutable` | `ProbeRestaurant.name`, `.tags`, `.groups`, `.ids`, `.cache`, `ProbeShape.size`, `ProbeRestaurant.Companion.instances`, `ProbeDefaults.counter`, `probeTopLevel` | `note` (a KDoc that mentions var), `legacy` (an annotation), the nested-worktree `NestedCheckout` |

**P8 on its own:**
1. Start from a run that passes, then run P8 without `--rerun`.
2. `:app:testDebugUnitTest` runs rather than reporting UP-TO-DATE.
3. `repositoryImplementationsAreInternal` fails on `PublicProbeRepository`.

**P9 on its own:** start from a run that passes and don't pass `--rerun`. `:app:testDebugUnitTest` runs rather than reporting UP-TO-DATE or FROM-CACHE. `rawColoursOnlyAppearInTheDesignSystem` flags `feature/browse/impl/…/ProbeNested.kt:6` only. Line 5 is a comment.

**P10:**
- `moneyIsNeverAFloatingPointNumber` flags `ProbeRound3.kt` lines 4 and 5 only. The KDoc on line 3 and `seconds / 60` on line 6 don't count.
- `viewModelsNeverExposeMutableState` flags `ProbeRound3ViewModel.exposed` (internal) and `ProbeRound3ViewModel.items` (`StateFlow<MutableList<Int>>`).

## Known gaps

These aren't claimed as covered:
- An expression-bodied ViewModel function with an inferred return type (`fun state() = _state`) isn't checked.
- Members of a ViewModel's companion object aren't checked.
- Money held in a `List<Double>`, in UPPER_SNAKE constants, or under names such as `amount` isn't caught.
- For the text-based rules, a comment after code on the same line still counts.
- A line that starts with a comment is skipped as a whole, so code after an inline block comment on that line isn't checked. ktlint's formatting rejects that shape anyway.
- A line starting with Kotlin's spread operator (`*items`) is also treated as a comment.
- Konsist is pinned at 0.17.3. After an upgrade, re-run every probe: the module-name format and the type-name text are the most likely to change.
