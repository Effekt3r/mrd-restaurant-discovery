**PASS**

Commit verified: `11177254b443a72d9d8be9b9cec9ce6a673e629e` ("docs(ai): log the verifier's misquoted setup step"), branch `chore/m1-verification`, verified 2026-11-18. Pull request #4 (which carried the commit first verified below, `0d40936f7f5e0a80e520e428f8cca15eed3f46fa`) merged into `dev` as merge commit `14f30a0eb330e7d578e000c6e5209a60072bfa82`, with the same tree as `0d40936` — the merge itself changed nothing. The fixes landed as a follow-up, pull request #5, on the branch now checked out here: first `66322fce0b9ff5d522834fa1910e652868edca8c` (the README/wrapper/doc fixes), then `7e25eb9` (the README now asks for an absolute `sdk.dir`, not `~`) and `1117725` (logging this report's own misquote — see "Re-run").

## Re-run

The first pass (captured below, now superseded) found criteria 1–3 solidly passing and criterion 4 failing on a real, reproducible gap: a fresh clone only built because this machine already had `ANDROID_HOME` set outside the project, and the README never said Gradle needed it or a `local.properties`.

**What changed**, confirmed with `git diff --stat 0d40936 HEAD`: only doc/config files — `README.md`, `CLAUDE.md`, `WORKFLOW.md`, `.claude/agents/verifier.md`, `.claude/settings.json`, `docs/ai/corrections.md`, `docs/plans/2026-11-18-00-roadmap.md`, `docs/plans/2026-11-18-01-foundation.md`, `docs/specs/2026-11-18-restaurant-discovery-design.md`, `gradle/libs.versions.toml` (comment wording only, no version bump), and the Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`). No `app`, `feature`, `core` or `build-logic` file changed, `settings.gradle.kts` didn't change, and `app/src/test/kotlin/com/effekt3r/mrd/ArchitectureTest.kt` didn't change — confirmed by its absence from the diffstat.

In substance:
- the README's "Getting started" now says how Gradle finds the SDK (`ANDROID_HOME`, or `sdk.dir` in the git-ignored `local.properties`, which Android Studio writes automatically), and clones `--branch dev` until the first release reaches `master`;
- the Gradle wrapper was regenerated with Gradle 9.7.1 itself — the committed `gradle-wrapper.jar` had actually been Gradle 8.14.5's (visible in `gradlew`'s diff: the regenerated script's header comment and template URL match Gradle's current generator, not the older one that produced the committed version);
- `CLAUDE.md`, `.claude/agents/verifier.md` and the spec now describe the fail-closed R8 check (`test -s mapping.txt && test ! -s missing_rules.txt`, which also catches R8's output moving or not running at all, not just non-empty `missing_rules.txt`);
- the remaining changes correct stale claims a final whole-branch review found (the release workflow described as already present when it's milestone 7 work, the version-policy wording, the PR #4/verifier timeline in the plan, and `.claude/settings.json`'s signing-material deny list gaining `*.p12`, `*.pem` and `secrets.properties`).

**This report's own misquote, found and corrected.** My second pass (against `66322fc`) correctly built a `local.properties` with an absolute SDK path — the command was `echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties`, which the shell expands into an absolute path before Gradle ever sees it — and every README command passed against that real file. But the report then *described* that step as `sdk.dir=~/Library/Android/sdk`, which is not what the command wrote and not something the Android Gradle plugin can resolve (it doesn't expand `~`; it tried the literal four-character string and reported the directory doesn't exist). The controller caught this by testing the quoted line literally before committing anything. I confirmed from my own command history that every build genuinely ran with `ANDROID_HOME` unset — the verdict was never in doubt — but the quote was wrong, and `docs/ai/corrections.md` now has an entry for it. Two commits then landed: `7e25eb9`, which makes the README ask for an absolute path and say `~` isn't expanded, and `1117725`, which logs the misquote. Both are doc-only.

**New evidence for criterion 4 at the current head** (full detail in §4), gathered in a fresh disposable clone of `chore/m1-verification`: first, a **negative check** — a `local.properties` containing the literal four characters `~/Library/Android/sdk` (exactly what my second pass's write-up wrongly implied) fails `:app:assembleDebug` with "SDK location not found", confirming the controller's finding. Then, following the README's words for real — `echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties`, letting the shell do the expansion — all five of its documented commands pass, with `ANDROID_HOME`, `ANDROID_SDK_ROOT` and `JAVA_HOME` unset on every one individually. The gap is closed; the evidence now matches the command that produced it.

Criteria 1–3 were re-checked too: CI on the current head (§1), and criterion 1's prior runs (§1); §2's and §3's unchanged-code evidence from the first pass still stands; the diffstat from `66322fc` to the current head touches only `README.md` and `docs/ai/corrections.md`, so it doesn't change anything §2 or §3 covers either.

## Criteria

| # | Criterion | Verdict | Evidence |
|---|---|---|---|
| 1 | **AC-21:** CI passes on the branch: formatting, lint, tests, release build and secret scan. | PASS | §1 |
| 2 | **The module boundaries are enforced** by the build (feature↔feature, core↔feature/`:app`) and by the architecture tests. | PASS | §2 |
| 3 | **The app shell navigates Browse to Details and back, edge-to-edge, on the release APK.** | PASS | §3 |
| 4 | **The README's requirements are complete:** following only the README, a fresh clone builds and tests. | PASS | §4 |

All four criteria pass. Criterion 4 failed on the first pass and is fixed; see "Re-run" above and §4 below.

## Evidence

### §1 CI (AC-21) — PASS

**Current head (`1117725`):**

- `git rev-parse HEAD` → `11177254b443a72d9d8be9b9cec9ce6a673e629e`; `gh pr view 5` confirms `headRefOid` matches exactly, base `dev`, draft.
- `gh pr checks 5`, polled in a bounded loop (`until gh pr checks 5 ...; do sleep 15; done`) while **Build and test** was `pending`, until it completed:
  ```
  Build and test   pass   3m52s
  Secret scan      pass   6s
  ```
- `gh run view 44366562281 --json headSha,conclusion,event,status,headBranch` → `headSha` matches the commit above exactly; `conclusion: success`; `event: pull_request`; `headBranch: chore/m1-verification`.
- Every step of the **Build and test** job reports `success`: Set up job, Check out, Set up JDK 21, Set up Gradle, Formatting, Lint, Unit/Robolectric/architecture tests, Release build (R8), No missing R8 keep rules, Upload build outputs, and the post-steps. **Secret scan** also `success`.

**Previous head (`66322fc`), not re-run this time since the only change since (`7e25eb9`, `1117725`) is `README.md` and `docs/ai/corrections.md` — nothing CI checks:**

- `gh pr checks 5` at that head: `Build and test pass 3m32s`, `Secret scan pass 5s`; `gh run view 44364278781` confirmed `headSha` matched, `conclusion: success`.
- The always-run local gate, re-run in the worktree with the regenerated wrapper: `./gradlew --version` → `Gradle 9.7.1` (build time 2026-08-19, revision `92f0512e`) — confirms the wrapper now actually launches 9.7.1, not the previously-committed 8.14.5 JAR. Then `./gradlew spotlessCheck :app:lintDebug testDebugUnitTest :app:assembleRelease --console=plain` → exit 0, `BUILD SUCCESSFUL in 2s` (517 actionable tasks: 3 executed, 1 from cache, 513 up-to-date — unchanged code, so almost everything is legitimately up to date).
  - Fail-closed check: `test -s app/build/outputs/mapping/release/mapping.txt && test ! -s app/build/outputs/mapping/release/missing_rules.txt` → passes (`mapping.txt` is 161,884 lines; `missing_rules.txt` absent).
  - `:core:common:test` and `:core:model:test` → `NO-SOURCE` again, same as before (still no tests in those modules).
  - `app/build/test-results/testDebugUnitTest/TEST-com.effekt3r.mrd.ArchitectureTest.xml`: `tests="7" skipped="0" failures="0" errors="0"`, same as before.
  - `:app:lintDebug`: 0 errors, 1 warning — same pre-existing warning as the first pass (see Observations).

**First pass, on the original head (`0d40936`, still accurate for that commit):**

- `gh pr checks 4`:
  ```
  Build and test   pass   5m20s   .../actions/runs/44355433953/job/134982161329
  Secret scan      pass   6s      .../actions/runs/44355433953/job/134982160848
  ```
- `gh run view 44355433953 --json headSha,conclusion,event,status` → `headSha` matches the commit above exactly; `conclusion: success`; `event: pull_request`; `status: completed`.
- Steps of job **Build and test** (all `success`): Check out, Set up JDK 21, Set up Gradle, Formatting, Lint, Unit, Robolectric and architecture tests, Release build (R8), No missing R8 keep rules, Upload build outputs.
- Steps of job **Secret scan** (all `success`): Check out (full history), gitleaks.
- Rulesets, read back with `gh api repos/Effekt3r/mrd-restaurant-discovery/rulesets/27529683` (name `dev`) and `.../27529684` (name `master`): both `"enforcement": "active"`, both carry `deletion`, `non_fast_forward`, `pull_request` and `required_status_checks` rules, and both require exactly `{"context":"Build and test","integration_id":15368}` and `{"context":"Secret scan","integration_id":15368}` (15368 = the GitHub Actions app, so only Actions can satisfy them).
- The always-run local gate, in the worktree: `./gradlew spotlessCheck :app:lintDebug testDebugUnitTest :app:assembleRelease --console=plain` → exit 0, `BUILD SUCCESSFUL in 877ms` (513 actionable tasks, mostly up-to-date/cached since the tree was already built at this exact commit — `git status --porcelain` was empty beforehand, so the cache reflects this commit's sources).
  - `:core:common:test` → `NO-SOURCE`, `:core:model:test` → `NO-SOURCE`. Expected: neither module has tests yet (milestone 3 adds the data layer); `testDebugUnitTest` aggregates both modules via each one's own `testDebugUnitTest` alias task, so the single command still exercises them.
  - `app/build/outputs/mapping/release/mapping.txt`: present, non-empty (161,884 lines).
  - `app/build/outputs/mapping/release/missing_rules.txt`: absent.
  - `:app:lintDebug`: 0 errors, 1 warning (see Observations).
  - `app/build/test-results/testDebugUnitTest/TEST-com.effekt3r.mrd.ArchitectureTest.xml`: `tests="7" skipped="0" failures="0" errors="0"`.

### §2 Module boundaries — PASS

**Re-run:** not repeated at either follow-up commit. `git diff --stat 0d40936 HEAD` touches no `settings.gradle.kts`, no `app/src/test/kotlin/com/effekt3r/mrd/ArchitectureTest.kt`, no `build-logic` file and no `feature:*`/`core:*` build file — exactly what this criterion covers — across every commit since, including the two latest (`7e25eb9`, `1117725`, both README/corrections-log only). The evidence below, gathered against `0d40936`, still describes the current head unchanged.

Done in a disposable clone of `chore/project-foundation`, never in the worktree.

**Build-time layering rule (`settings.gradle.kts`), two manual probes:**

| Probe | Change | Command | Result |
|---|---|---|---|
| Feature → feature | Appended `dependencies { implementation(projects.feature.details) }` to `feature/browse/build.gradle.kts` | `./gradlew :feature:browse:help` | `BUILD FAILED`: "`:feature:browse must not depend on :feature:details (declared in 'implementation'). Features depend only on :core modules, and only :app connects features.`" |
| Core → feature | Appended `dependencies { implementation(projects.feature.browse) }` to `core/data/build.gradle.kts` | `./gradlew :core:data:help` | `BUILD FAILED`: "`:core:data must not depend on :feature:browse (declared in 'implementation'). Features depend only on :core modules, and only :app connects features.`" |

Both files reverted with `git checkout --` immediately after; `git status --porcelain` was clean before moving on.

**Architecture tests (`app/src/test/kotlin/com/effekt3r/mrd/ArchitectureTest.kt`), the full probe kit in `tools/architecture-probes/`,** run per its README ("How to check"):

| Step | Command | Result vs. `tools/architecture-probes/README.md`'s "Expected results" |
|---|---|---|
| Baseline | `./gradlew :app:testDebugUnitTest --tests "com.effekt3r.mrd.ArchitectureTest"` | `BUILD SUCCESSFUL`, 7/7 pass |
| `create-p1-p7.sh`, then rerun with `--rerun` | same test target | All 7 tests failed. Every flagged line/member and every "must not be flagged" look-alike matched the table exactly, e.g. `rawColoursOnlyAppearInTheDesignSystem` flagged `ProbeColours.kt` lines 5–8 only (not `Color.Transparent`, `Color.Unspecified`, `lightenColor(0x20)`); `modelTypesAreImmutable` flagged all 9 listed members (`ProbeRestaurant.name/tags/groups/ids/cache`, `ProbeShape.size`, `Companion.instances`, `ProbeDefaults.counter`, `probeTopLevel`), not the KDoc, the annotation or the nested-worktree control |
| `remove-probes.sh` | — | probes removed; `git status --porcelain` empty |
| `create-p8.sh` (no `--rerun`, starting from a passing run) | same test target | Prior run was `FROM-CACHE`; this run actually executed `:app:testDebugUnitTest` (not cached) and failed only `repositoryImplementationsAreInternal`, naming `PublicProbeRepository` — exactly as predicted |
| `remove-probes.sh` | — | clean |
| `create-p9.sh` (no `--rerun`, starting from a passing run) | same test target | Prior run was `FROM-CACHE`; this run re-executed and failed only `rawColoursOnlyAppearInTheDesignSystem`, flagging `feature/browse/impl/.../ProbeNested.kt:6` only (line 5 is a comment) — exactly as predicted |
| `remove-probes.sh` | — | clean |
| `create-p10.sh`, then rerun with `--rerun` | same test target | 2 tests failed: `moneyIsNeverAFloatingPointNumber` flagged `ProbeRound3.kt` lines 4–5 only (not the line-3 KDoc or line-6 `seconds / 60`); `viewModelsNeverExposeMutableState` flagged `.exposed` (internal) and `.items` (`StateFlow<MutableList<Int>>`), not the private `.hidden` |
| `remove-probes.sh` | — | clean |

The disposable clone's tree was clean (`git status --porcelain` empty) before it was deleted.

### §3 App shell: navigation and edge-to-edge on the release APK — PASS

**Re-run:** the wrapper changed between the first pass and the second, so the gate rebuilt the release APK once on that head (§1). `:app:assembleRelease` reported `UP-TO-DATE` rather than doing new work, because none of its inputs changed (no `app`/`feature`/`core` file is in the diffstat) — confirmed by hashing the output: `shasum -a 256 app/build/outputs/apk/release/app-release.apk` gives the same 979,364-byte file, byte-for-byte, as the one walked below. I still repeated the short walk on `emulator-5556` with this APK: reinstalled with `adb install -r` (`Success`, cold start), tapped "Cape Town Grill" (same bounds, `540,409`) → "Restaurant r-1001" / "Back" shown, `input keyevent KEYCODE_BACK` → back to "Restaurants" / "Cape Town Grill", `dumpsys activity activities` again shows `com.effekt3r.mrd/.MainActivity` as `topResumedActivity`, `state=RESUMED`, `finishing=false`. Identical behaviour to the first pass, as expected since the code didn't change; no new screenshots taken, since nothing looks different. The emulator was already back at `accelerometer_rotation=0`, `user_rotation=0`, night mode `no` from the first pass, and that re-run didn't touch rotation or theme, so nothing needed restoring afterwards. The two commits since (`7e25eb9`, `1117725`) are README/corrections-log only, so this walk wasn't repeated again for the current head — nothing it covers could have changed.

**First pass:** used `app/build/outputs/apk/release/app-release.apk` built by the gate run in §1 (979,364 bytes). Every `adb`/`uiautomator` command below targeted `-s emulator-5556` explicitly; the other connected device was never touched.

- `adb -s emulator-5556 install -r app/build/outputs/apk/release/app-release.apk` → `Success`; `adb -s emulator-5556 shell am start -W -n com.effekt3r.mrd/.MainActivity` → `LaunchState: COLD`.
- **Browse:** "Restaurants" heading and a tappable "Cape Town Grill" / "Steakhouse · Grill" row. Found the row with `uiautomator dump` (the clickable container's bounds are `[0,315][1080,504]`) and tapped its centre, `input tap 540 409`. Screenshot: `docs/qa/screenshots/m1-browse.png`.
- **Details:** after the tap, the screen shows "Restaurant r-1001" and a "Back" button (confirmed via a second `uiautomator dump`). Screenshot: `docs/qa/screenshots/m1-details.png`.
- **Back:** `input keyevent KEYCODE_BACK` returns to a screen showing "Restaurants" / "Cape Town Grill" again (a third `uiautomator dump`). `dumpsys activity activities` shows `topResumedActivity=...com.effekt3r.mrd/.MainActivity`, `state=RESUMED`, `finishing=false` — the app stayed open and landed back on Browse; it did not pop past the start destination (consistent with `navigateUp`, per the plan's Task 6 review note).
- **Edge-to-edge, portrait:** in both screenshots above, the title text clears the status-bar icons with margin, and the content stops above the gesture-navigation indicator at the bottom; nothing is clipped by either system bar.
- **Edge-to-edge, landscape:** `adb -s emulator-5556 shell settings put system accelerometer_rotation 0` then `... settings put system user_rotation 1`; Browse re-screenshotted as `docs/qa/screenshots/m1-browse-landscape.png` — content starts clear of the status-bar row and the side inset, with no clipping. Restored with `... settings put system user_rotation 0` afterwards.
- **Dark theme:** `adb -s emulator-5556 shell cmd uimode night yes`. Re-captured both screens: `docs/qa/screenshots/m1-browse-dark.png` and `docs/qa/screenshots/m1-details-dark.png`. Status-bar icons (clock, Wi-Fi, signal, battery) render white and are clearly legible against the dark background on both screens; the "Back" button also keeps good contrast (dark text on a light-lavender fill). Restored with `cmd uimode night no`.
- **Offline / error path: NOT TESTABLE.** Milestone 1 has no data layer, networking or cached state yet — both screens are static placeholders with no network call to fail and no cache to fall back to (confirmed: `core/data` has no production source files beyond its `build.gradle.kts` at this commit). There is nothing to take offline or make error in this milestone. Per the roadmap, milestone 2 delivers the mock API data; the offline/error walk becomes testable once milestone 3 adds the data layer (`:core:data`, Room, sync) on top of it.
- **Emulator left as found:** `accelerometer_rotation=0`, `user_rotation=0`, night mode `no` — the exact values read from the device before this session changed anything.
- Screenshots were viewed individually before saving and are small: `docs/qa/screenshots/{m1-browse,m1-details,m1-browse-dark,m1-details-dark,m1-browse-landscape}.png`, each resized with `sips -Z 1200` (540×1200 for the three portrait pairs, 1200×540 for the landscape one).

### §4 README requirements — PASS (was FAIL on the first pass)

**First pass, against `0d40936`:** FAIL. In a disposable clone of `chore/project-foundation`, with `local.properties` removed, every README-documented command (`assembleDebug`, `testDebugUnitTest`, `spotlessCheck`, `:app:lintDebug`, `:app:assembleRelease`) still passed — but only because this machine already has `ANDROID_HOME` exported globally, outside the project and outside anything the README created. Re-running with `env -u ANDROID_HOME -u ANDROID_SDK_ROOT ./gradlew :app:assembleDebug` reproduced a hard failure: `SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable or by setting the sdk.dir path in your project's local properties file at '.../local.properties'.` Neither the README's "Requirements" table nor its "Getting started" section mentioned `ANDROID_HOME` or `local.properties`; that mechanism lived only in the plan's "Before you start" section. This is exactly the gap `docs/ai/corrections.md`'s "The README's setup only worked on a machine that was already set up" entry now credits to this verifier and to the final whole-branch review.

**Second pass, against `66322fc`:** PASS, correctly, but misquoted. I made a disposable clone, ran every command with `ANDROID_HOME`, `ANDROID_SDK_ROOT` and `JAVA_HOME` unset individually, and wrote `local.properties` with `echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties` — the shell expands `$HOME` before Gradle ever sees the file, so this wrote a real absolute path, and all five README commands passed against it. But my report described that step as creating `local.properties` "with `sdk.dir=~/Library/Android/sdk`", which is not the command I ran and not something the Android Gradle plugin can read (see below). The controller caught the mismatch by testing the quoted line literally; see "Re-run" above and `docs/ai/corrections.md`.

**This pass, against the current head (`1117725`), in a new disposable clone of `chore/m1-verification`:** PASS, with the quote fixed and a negative check added.

- *Negative check first.* `printf 'sdk.dir=~/Library/Android/sdk\n' > local.properties`, then `cat local.properties` (confirms the file holds the literal four characters `~/Library...`, not an expanded path), then `env -u ANDROID_HOME -u ANDROID_SDK_ROOT -u JAVA_HOME ./gradlew :app:assembleDebug` → `BUILD FAILED`:
  ```
  WARNING: The following problems were found when resolving the SDK location:
  Where: sdk.dir property in local.properties file. Problem: Directory does not exist

  FAILURE: Build failed with an exception.
  * What went wrong:
  Could not determine the dependencies of task ':app:compileDebugJavaWithJavac'.
  > SDK location not found. Define a valid SDK location with an ANDROID_HOME
    environment variable or by setting the sdk.dir path in your project's
    local properties file at '.../local.properties'.
  ```
  This confirms the controller's finding: the Android Gradle plugin reads `sdk.dir` literally and tried to resolve a directory named `~/Library/Android/sdk` relative to the project, which doesn't exist; it does not expand `~` the way a shell would.
- *Then, following the README's words for real:* `echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties` — the shell, not Gradle, does the expansion, so the file holds a real absolute path (never quoted here, since it contains this machine's username). `git config core.hooksPath .githooks` → ok. Then, each with `env -u ANDROID_HOME -u ANDROID_SDK_ROOT -u JAVA_HOME` prefixed individually:
  - `./gradlew assembleDebug` (in place of `installDebug`'s build step, to avoid installing a second build onto the shared emulator used for §3) → `BUILD SUCCESSFUL`.
  - `./gradlew testDebugUnitTest` → `BUILD SUCCESSFUL`.
  - `./gradlew spotlessCheck` → `BUILD SUCCESSFUL`.
  - `./gradlew :app:lintDebug` → `BUILD SUCCESSFUL`, 0 errors, 1 warning (same `MissingApplicationIcon` warning as §1).
  - `./gradlew :app:assembleRelease` → `BUILD SUCCESSFUL`, printed the documented `MRD_KEYSTORE_FILE is not set, so this release APK is signed with the debug key.` warning; fail-closed check (`test -s mapping.txt && test ! -s missing_rules.txt`) passes.
- `git status --porcelain` in the clone was clean throughout (`local.properties` is git-ignored).

The negative check shows the exact failure a reader who writes `~` would hit; the positive check shows the README's own words — followed literally, with the shell doing the only expansion involved — are now sufficient. `ANDROID_HOME`, `ANDROID_SDK_ROOT` and `JAVA_HOME` were unset for every one of these Gradle invocations, individually, not just the first. The gap is closed, and this report's own evidence now matches the commands that produced it.

## Observations outside this milestone's scope

- `:app:lintDebug` reports one warning (not an error, so it doesn't fail lint or CI): `MissingApplicationIcon` at `app/src/main/AndroidManifest.xml:8` — no launcher icon is set yet. Spec §6.6 calls for "an original launcher icon in Mr D blue and ink"; this reads as a later-milestone/branding item rather than a milestone 1 gap, but is noted here for tracking.
- PR #4 (`chore/project-foundation` → `dev`) merged as `14f30a0` before this re-run (same tree as `0d40936`). PR #5 (`chore/m1-verification` → `dev`, "Milestone 1 follow-up: verification and review fixes") is now open as a draft, carrying the fixes this re-run verified.
- A second device (`R5GL34C7T3W`) was connected to this Mac throughout; it was never targeted by any command in this session — every `adb` and `uiautomator` call used `-s emulator-5556` explicitly.
