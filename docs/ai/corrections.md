# Corrections log

Every time the AI got something wrong and it was caught: what happened, what caught it, the fix and the lesson. Entries are newest first.

This is the honest half of `AI_USAGE.md`, which arrives with milestone 7. Most of these were caught by the two independent review passes that every task goes through: one checks the work against its specification, the other checks code quality. Both reviewers build the code and run probes.

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

## 2026-11-19 · Three of the generator's tests couldn't fail

- **What:** three of the plan's checks passed whatever the data said.
  - The check that some places open 24 hours looked for a week whose every entry runs from `00:00` to `24:00`. An empty week passes that, and a normal record with `"opening_hours": []` is always there.
  - The rate checks allowed 1.5 points either way. That let through a 13.8% free-delivery rate and a 5.3% missing-fee rate.
  - Nothing pinned `is_open` to Friday 19:00. Moving the snapshot to 13:00 flipped 888 values, and every test still passed.
- **Caught by:** the code-quality review of milestone 2's generator, which broke the code on purpose in a scratch clone.
- **Fix:** the 24-hour check skips empty weeks, the rates must hold within a point, and a test pins the snapshot to week minute 6,900.
- **Lesson:** a test is only proven when breaking the code makes it fail. The same goes for house rules.

## 2026-11-19 · The generator's open check read a same-time entry as open all day

- **What:** `isOpenAt` treated an entry that opens and closes at the same time as a window lasting a whole day, though §5.1 drops such entries. No generated data reached it, because the edge case with that entry is added after `is_open` is computed.
- **Caught by:** the same review, with a probe test.
- **Fix:** such an entry is never open, and a test covers it.

## 2026-11-18 · Marking the stress file `-diff` would have hidden it from the secret scan

- **What:** milestone 2's design marked `mock-api/v1/restaurants-10k.json` as `-diff` in `.gitattributes`, to keep git's output readable. Git then treats the file as binary, and gitleaks reads git's patches: in the pre-commit hook and in CI's required Secret scan, it would have scanned none of the file's 6 MB.
- **Caught by:** the spike for milestone 2's plan. A fake AWS key in the stress file passed `gitleaks git`; the same key in `restaurants.json` failed it.
- **Fix:** the file isn't marked `-diff`. `linguist-generated=true` still collapses it in pull requests, and scanning it adds about 2.5 seconds.
- **Lesson:** before an attribute changes how git sees a file, check what else reads git's view of it. A check that silently skips its input fails open.

## 2026-11-18 · TheMealDB's area list didn't match its meals

- **What:** milestone 2's design fetched photos by the area names that TheMealDB's `list.php?a=list` returns. For American, Indian, Lebanese and South African, `filter.php` finds no meals: those meals are filed under country names (United States, India), and there are no South African dishes at all. A search for pizza finds three meals.
- **Caught by:** the spike for milestone 2's plan, whose fetch fails when an image key has fewer than five photos.
- **Fix:** the queries use country names where needed, the pizza key adds flatbread searches, and there's no South African key: Cape Malay restaurants use the Indian and lamb photos.
- **Lesson:** check what an API returns for each request you rely on, not what its listing endpoints suggest.

## 2026-11-18 · The verifier's report misquoted its own setup step

- **What:** the verifier's second report said it created `local.properties` with `sdk.dir=~/Library/Android/sdk`. Its command had been `echo "sdk.dir=$HOME/Library/Android/sdk"`, so the shell wrote an absolute path. The Android Gradle plugin doesn't expand `~`, so a reader copying the quoted line would hit "SDK location not found". The README didn't say the path must be absolute either.
- **Caught by:** the controller, which tested the quoted line literally in a scratch clone before committing the report. The verifier then confirmed from its own command history that every build had run with `ANDROID_HOME` unset, so the evidence held and only the quote was wrong.
- **Fix:** the README asks for an absolute path and says `~` isn't expanded. The report quotes the command that was actually run.
- **Lesson:** evidence quotes the exact command and its output, never a paraphrase. A verifier's report is checked like any other claim.

## 2026-11-18 · The README's setup only worked on a machine that was already set up

- **What:** two gaps for someone starting from the public repository:
  - cloning checks out `master`, which holds no code until the first release;
  - on the command line, Gradle needs `ANDROID_HOME` or `sdk.dir` in `local.properties`, and the README didn't say so. Every earlier check passed only because this machine has `ANDROID_HOME` set.
- **Caught by:** the verifier, which unset `ANDROID_HOME` in a disposable clone, and the final whole-branch review of milestone 1, which cloned the default branch.
- **Fix:** the README clones `dev` until the first release, and says how Gradle finds the SDK.
- **Lesson:** test setup instructions in a clean environment, starting from the public URL.

## 2026-11-18 · Four claims in the docs weren't true

- **What:** the final whole-branch review checked every claim in the documents against the repository and found four that didn't hold:
  - the README and WORKFLOW.md described the release workflow, which arrives in milestone 7;
  - the version policy said every library was the newest stable release at least four weeks old, but Activity and Core were a minor version behind;
  - CLAUDE.md and the verifier's instructions still checked R8's output in the way that fails open, and the README still said CI gates every pull request, after Task 9 had fixed both elsewhere;
  - Claude Code's Read deny list missed `.p12`, `.pem` and `secrets.properties` files, which `.gitignore` treats as signing material.
- **Caught by:** the final whole-branch review of milestone 1.
- **Fix:** each claim now matches the repository, and the deny list mirrors `.gitignore`.
- **Lesson:** when a fix changes a claim or a check, search for every copy of it. WORKFLOW.md already says to fix the whole class of bug, not just the instance.

## 2026-11-18 · The Gradle wrapper JAR came from Gradle 8.14.5

- **What:** the wrapper was generated with the Gradle 8.14 installed on this machine, so `gradle-wrapper.jar` was 8.14.5's, although it downloads Gradle 9.7.1. Nothing broke, because CI's wrapper validation accepts any Gradle release's JAR.
- **Caught by:** the final whole-branch review, which matched the JAR's SHA-256 against Gradle's published checksums.
- **Fix:** Gradle 9.7.1's own `wrapper` task regenerated the wrapper.
- **Lesson:** after generating a wrapper with another Gradle version, run the wrapper task once more with the new one.

## 2026-11-18 · Links in pull request descriptions broke when their branches were deleted

- **What:** the descriptions of pull requests #2 and #3 linked to files on their branches. The repository deletes a branch when its pull request merges, so the links returned 404.
- **Caught by:** the final whole-branch review of milestone 1.
- **Fix:** the links point at the merge commits.
- **Lesson:** anything that outlives a branch links to a commit.

## 2026-11-18 · The split submit job started the commercial cache and asked for a permission it didn't need

- **What:** two slips in the job that submits the dependency graph.
  - The controller's YAML set no cache option for it. The action sets up Gradle caching before it checks the mode, so the job would have started the default, commercial caching provider, although no Gradle runs there and SECURITY.md says caching uses the open-source provider.
  - The implementing agent added `actions: read`, copied from Gradle's documented example for a different trigger (`workflow_run`, which reads another run's artifacts).
- **Caught by:** the controller, reading the action's source at the pinned commit before closing the task.
- **Fix:** caching is off in the submit job, and `actions: read` is gone.
- **Lesson:** check an action's behaviour in its source at the pinned version. A documentation example may answer a different question.

## 2026-11-18 · The dependency-submission job ran the build with a write token

- **What:** the first dependency-submission workflow ran the whole Gradle build, including any plugin it downloads, in a job holding a token that can write to the repository.
- **Caught by:** the Task 9 re-review, citing Gradle's documented way to split the job.
- **Fix:** a read-only job runs the build and uploads the dependency graph. A separate job with write access submits it and runs no build code.
- **Lesson:** give a write token only to the step that needs it, and never to a step that runs downloaded code.

## 2026-11-18 · SECURITY.md promised Dependabot fixes for every Gradle dependency

- **What:** it said Dependabot opens security-fix pull requests. For Gradle dependencies sent by dependency submission, that mostly works only for direct dependencies declared in a version catalog. Transitive dependencies get an alert but no pull request.
- **Caught by:** the Task 9 re-review, from Gradle's dependency-submission FAQ.
- **Fix:** SECURITY.md and CONTRIBUTING.md say which dependencies get fix pull requests.

## 2026-11-18 · SECURITY.md claimed vulnerability alerts that covered nothing

- **What:** it said Dependabot alerts covered the project's dependencies. GitHub can't read a Gradle build on its own, and actions pinned to a commit SHA never get alerts, so no dependency was covered. It also claimed a secret scan on every pull request, when CI only runs for pull requests into `dev` and `master`.
- **Caught by:** the Task 9 code-quality review, from GitHub's documentation.
- **Fix:**
  - a dependency-submission workflow sends the resolved Gradle dependencies to GitHub;
  - Dependabot checks actions after 7 days instead of 30;
  - SECURITY.md states each control's real scope.
- **Lesson:** before documenting a security control, check what it actually covers.

## 2026-11-18 · Dependabot was described as running before it could start

- **What:** the plan assumed Dependabot would start once its configuration reached `dev`. GitHub reads Dependabot's configuration only from the default branch, which is `master`, so nothing runs until the first release.
- **Caught by:** the Task 9 code-quality review.
- **Fix:** I chose to keep `master` as the default branch. SECURITY.md and the README now say that Dependabot starts with the first release.

## 2026-11-18 · A hidden ruleset default could have blocked every merge

- **What:** when a ruleset leaves out `require_extra_approval_for_unattributed_changes`, GitHub fills it in as true. A pull request containing a commit from an email address not linked to the account would then need an approval, which a solo maintainer can't give.
- **Caught by:** the Task 9 code-quality review, from the rulesets read back from GitHub.
- **Fix:** the rulesets set it to false explicitly, in code.
- **Lesson:** read back what an API stored, not just what was sent.

## 2026-11-18 · Every pull request into dev built with a cold Gradle cache

- **What:** setup-gradle's default cache provider is proprietary and only saves the cache on the default branch. That's `master`, which only receives releases, so pull requests into `dev` never had a warm cache.
- **Caught by:** the Task 9 code-quality review, from the CI logs.
- **Fix:** the MIT-licensed `basic` provider. Pushes to `dev` and `master` write the cache, and pull requests read it.

## 2026-11-18 · The R8 missing-rules check would have passed if the output moved

- **What:** CI checked that `missing_rules.txt` was absent or empty. If R8's output moved, for example in a new AGP version, the file would be absent and the check would pass without checking anything.
- **Caught by:** the Task 9 code-quality review.
- **Fix:** the check first requires R8's mapping file, so a moved output fails the build.
- **Lesson:** a check that passes when its input is missing fails open. Make it prove that its input exists.

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
- **Fix:** clicks go through `dropUnlessResumed`. On the same review's advice, the app shell (written next) used `navigateUp` for Back, which never pops the last screen.

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
