# Corrections log

Every time the AI got something wrong and it was caught: what happened, what caught it, the fix and the lesson. Entries are newest first.

This is the honest half of `AI_USAGE.md`. Most of these were caught by the two independent review passes that every task goes through: one checks the work against its specification, the other checks code quality. Both reviewers build the code and run probes.

## 2026-11-18 · A probe meant to show a caching bug changed two things at once

- **What:** the AI coordinating the work (the controller) wrote probe P9 to show that Gradle skipped the architecture tests for a nested module. The probe also wrote a file under a path Gradle already watched, so the tests re-ran anyway and the bug didn't show.
- **Caught by:** the implementing agent. It stopped instead of improvising, isolated the variable, and reproduced the bug cleanly.
- **Fix:** P9 was split into P9 (the nested module only) and P10 (the other probes).
- **Lesson:** a probe that demonstrates a caching bug must change only the variable under test.

## 2026-11-18 · The architecture tests' Gradle inputs missed nested modules

- **What:** the first fix for skipped tests declared `app/src`, `core/*/src` and `feature/*/src` as inputs. That only matches modules one level deep, so a module such as `feature/browse/impl` still left the task UP-TO-DATE.
- **Caught by:** an independent second reviewer, with a reproduction.
- **Fix:** every Kotlin source outside build output and hidden folders is now an input. That's a superset of what the test reads, so the two lists can't drift apart.

## 2026-11-18 · Gradle could skip the architecture tests entirely

- **What:** Konsist reads source files straight from disk, which Gradle can't see. A rule violation in `:core:data`, which isn't on `:app`'s test classpath, left `:app:testDebugUnitTest` UP-TO-DATE. With the build cache on, CI could even restore a cached pass.
- **Caught by:** the Task 7 code-quality review. A public repository class added to `:core:data` passed the build.
- **Fix:** the test task declares the sources as inputs. Probes P8 and P9 in `tools/architecture-probes/` prove it.
- **Lesson:** a test that reads files Gradle doesn't know about is invisible to Gradle's up-to-date checks.

## 2026-11-18 · The architecture rules matched absolute paths and missed common leaks

- **What:** three problems.
  - The rules filtered on absolute paths, so they failed falsely in any checkout whose path contains `/feature/`.
  - They also scanned other branches' worktrees.
  - They missed the most common ways to break each rule: `val state = _state`, a `*RepositoryImpl` class, and `Color(255, 0, 0)` or `Color.White`.
- **Caught by:** the Task 7 code-quality review, using probe files.
- **Fix:** the rules now filter by Gradle module, and are tighter. Each one is proven against deliberate violations sitting next to allowed look-alikes.

## 2026-11-18 · Dark mode made the status-bar icons unreadable

- **What:** the plan's app shell used `MaterialTheme {}`, which is always light. `enableEdgeToEdge()` follows the system theme, so in dark mode the status-bar icons were white on a near-white screen.
- **Caught by:** the Task 6 code-quality review, from the activity and Material 3 sources.
- **Fix:** the colour scheme follows `isSystemInDarkTheme()`. It was checked with a dark-mode screenshot.

## 2026-11-18 · The placeholder made you press a button to see a restaurant

- **What:** the plan's Browse placeholder had an "Open a sample restaurant" button.
- **Caught by:** me, after installing the build on my phone: "shouldn't show open sample restaurants button, should just open it".
- **Fix:** the placeholder is now a tappable restaurant row, which is how the real list works.

## 2026-11-18 · A double tap during a screen transition could leave a blank screen

- **What:** the placeholders called their navigation callbacks directly. The screen you're leaving stays clickable during the 700 ms transition, so a double tap on Back popped past the start screen.
- **Caught by:** the Task 5 code-quality review, from the Navigation and Lifecycle library bytecode.
- **Fix:**
  - clicks go through `dropUnlessResumed`;
  - Back uses `navigateUp`, which never pops the last screen.

## 2026-11-18 · Gradle 9's "no tests discovered" check would have failed CI

- **What:** Hilt's bytecode transform leaves generated classes in the unit-test output. So Gradle 9 failed `testDebugUnitTest` in every Hilt module that didn't have tests yet.
- **Caught by:** the Task 2 reviewer on its second pass. Its first pass had only dry-run the test tasks.
- **Fix:** the check stays on wherever `src/test` exists.
- **Lesson:** verifying means running the thing, not dry-running it.

## 2026-11-18 · The plan's architecture test wouldn't have compiled

- **What:** the app module's convention plugin added no JUnit, and Konsist doesn't bring it in, so the planned test in `:app` couldn't compile.
- **Caught by:** the Task 2 code-quality review.
- **Fix:** the app convention plugin adds JUnit, like the library ones do.

## 2026-11-18 · Pure Kotlin modules could call Java 21 methods that crash on Android 8 to 14

- **What:** these modules compiled against the JDK running Gradle, which is 21. So `list.removeFirst()` in `:core:model` compiled to a Java 21 method that doesn't exist below Android 15, and lint didn't flag it.
- **Caught by:** the Task 2 code-quality review, with a reproduction.
- **Fix:** they now compile with `-Xjdk-release=17`.

## 2026-11-18 · The "features never depend on features" check could be bypassed

- **What:** the check only ran inside modules that applied the feature convention plugin, and only on their own dependencies. A `:core:data` → `:feature:details` dependency slipped a feature onto another feature's classpath.
- **Caught by:** the Task 2 code-quality review, with a reproduction.
- **Fix:** a project-wide layering rule in `settings.gradle.kts`. It also stops core modules depending on features or `:app`.

## 2026-11-18 · Spotless would have reformatted other branches' files

- **What:** the `**/*.kt` target reached into `.worktrees/` and `.claude/worktrees/`, so running `spotlessApply` in the main checkout would rewrite other branches' uncommitted work.
- **Caught by:** the Task 2 code-quality review.
- **Fix:** Spotless now targets a file tree that skips build output and hidden top-level folders.

## 2026-11-18 · An implementing agent added an AI trailer to a commit

- **What:** the agent's environment told it to add a `Claude-Session:` trailer to commits, which this repository doesn't allow.
- **Caught by:** the agent itself, checking its work against its instructions. It amended the commit before review.
- **Fix:** the shared briefing for agents now names that trailer explicitly.

## 2026-11-18 · The feature-boundary check rejected each module's dependency on itself

- **What:** the first version failed with `:feature:browse must not depend on :feature:browse`. AGP adds a dependency from a module's test configurations back to the module.
- **Caught by:** a throwaway spike build, before the plan was written.
- **Fix:** a module's own path is allowed.
- **Lesson:** prove build logic on a real build before putting it in a plan.

## 2026-11-18 · "Use the latest versions" was the wrong default

- **What:** the AI's first version list took the newest release of everything: AGP 9.5.1 (released two weeks earlier), Kotlin 2.4.21, and a Robolectric released that morning.
- **Caught by:** me: "it's okay if we use an older version of anything to ensure stability and compatibility".
- **Fix:**
  - the toolchain is the combination Google's Now in Android builds with;
  - libraries are at least four weeks old;
  - a spike build validated the set;
  - Dependabot waits 30 days before proposing an update.

## 2026-11-18 · Konsist 0.18.1 would have pulled in Kotlin 2.4's standard library

- **What:** the newest Konsist depends on `kotlin-stdlib` 2.4.21, but the project uses Kotlin 2.3.21.
- **Caught by:** checking each library's POM against the project's Kotlin version before pinning it.
- **Fix:** Konsist 0.17.3, which the spike validated.

## 2026-11-18 · Retrofit's and OkHttp's repositories redirected to an unfamiliar organisation

- **What:** `github.com/square/retrofit` and `square/okhttp` now redirect to `lysine-dev`, and their old documentation sites return 404.
- **Checked:**
  - they're the same repositories, with history back to 2010 and 2012;
  - they moved to lysine.dev, a Commonhaus Foundation member;
  - the Maven coordinates are unchanged.
- **Lesson:** a core dependency changing hands is what a supply-chain attack looks like, so check before trusting it.

## 2026-11-18 · The design described the Coil and OkHttp caches wrongly

- **What:** during brainstorming the AI said Coil would "share OkHttp's disk cache". Coil 3 uses the shared `OkHttpClient` for connections but keeps its own disk cache.
- **Caught by:** the AI, while writing the spec. It was corrected before the commit (spec §5.7).
