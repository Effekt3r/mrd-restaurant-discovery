# How I used AI

I built this project with Claude Code. OfferZen's assessment details allow AI tools, and ask: "If you do, please include the prompts you used and explain your thinking." This file covers the tools, how the work flowed, the guardrails, the decisions I made and why, where the AI was wrong, and what the process cost.

- **My prompts:** [`docs/ai/prompts.md`](docs/ai/prompts.md) has every message I sent, in order, and my answers to Claude's questions. [`tools/ai/extract_prompts.py`](tools/ai/extract_prompts.py) extracts them from the session logs, which aren't committed.
- **The AI's mistakes:** [`docs/ai/corrections.md`](docs/ai/corrections.md) logs each one, with what caught it and the lesson.
- **The evidence:** the [spec](docs/specs/2026-11-18-restaurant-discovery-design.md), the [plans](docs/plans/) and the [verification reports](docs/qa/), and each pull request's "AI notes".

## Tools

- **Claude Code,** Anthropic's command-line coding agent. One session coordinated the project, and I call it the controller; it ran on Claude Opus 5.5. It gave each piece of work to a fresh subagent, more than 50 in all, on Opus 5.5 or Sonnet 5. Implementers and spec reviewers mostly ran on Sonnet 5. The spikes and plans, most code-quality reviews, the later combined reviews, and the screens and their fixes ran on Opus 5.5.
- **Skills from the superpowers plugin:**
  - brainstorming, for the design conversation that became the spec;
  - writing-plans, for the roadmap and each milestone's plan;
  - subagent-driven-development, for running a plan: a fresh subagent per task, with review after each;
  - using-git-worktrees, so each milestone had its own worktree and parallel work never shared a checkout.
- **The verifier,** defined in [`.claude/agents/verifier.md`](.claude/agents/verifier.md). For each milestone a fresh subagent followed those instructions: it checks the acceptance criteria with evidence, gives PASS or FAIL first, and only writes its report in `docs/qa/`. It never edits code.
- **My phone,** a Galaxy S25 FE on Android 17 with three-button navigation, for real-device checks. The emulator covered gesture navigation, font scales and landscape.

## How the work flowed

1. **Brainstorm.** Claude asked one question at a time, each with options and a recommendation, and I chose ([prompts 2 to 16](docs/ai/prompts.md#prompt-2)).
2. **Spec.** The decisions went into the [design spec](docs/specs/2026-11-18-restaurant-discovery-design.md), reviewed in pull request #2.
3. **Roadmap and plans.** The [roadmap](docs/plans/2026-11-18-00-roadmap.md) splits the work into seven milestones, plus 2b for the menus I added. Each milestone's plan was written just before it started.
4. **Spikes.** A throwaway build proved the toolchain before the first plan. From milestone 2 to milestone 4, the agent writing each plan first built the plan's code in a disposable clone and ran its checks, and the plan opens with what that spike found. Milestones 5 and 6 skipped the spike, to save time: one agent built both screens test-first from [64 lines of design notes](docs/plans/2026-11-19-05-screens.md).
5. **Implementation by subagents,** test-first where the code allowed, one logical step per commit, each with a `Verified:` line.
6. **Review.** In milestone 1, every task had two independent reviews: one against the spec, then one for code quality. The reviewers built the code and ran probes, and fixes went back through review. A whole-branch review closed the milestone. Milestone 2 had one implementer and one code-quality review for the whole milestone. From milestone 2b, after I asked for a faster pace, one combined verifier-and-review pass covered each milestone. For milestones 5 and 6 it ran twice: once on the first build, and again after the fixes for what it found.
7. **Verification.** The verifier's report, with the commands it ran and their output, is in [`docs/qa/`](docs/qa/).
8. **Pull request,** with a description and "AI notes": which prompts drove the change, and what the AI got wrong.
9. **Merge.** I merged the first eight pull requests myself. After that, Claude merged them: first the ones I asked for ([prompts 34](docs/ai/prompts.md#prompt-34), [35](docs/ai/prompts.md#prompt-35) and [37](docs/ai/prompts.md#prompt-37)), then each later one once its checks passed.

## Guardrails

AI writes plausible code quickly, so I put the house rules where they fail a build, not where a reviewer has to remember them.

- **[CLAUDE.md](CLAUDE.md)'s hard rules,** each with the reason it exists: features never depend on features; pinned versions stay pinned; money is integer cents, colours come from tokens and text from string resources; never weaken a test to make something pass; house rules are proven, not assumed; generated data is never edited by hand; secrets are never committed.
- **A layering rule in [`settings.gradle.kts`](settings.gradle.kts).** The build fails when a feature depends on another feature, or a core module depends on a feature or on `:app`. It checks every module, so a core module can't carry one feature onto another's classpath.
- **Seven Konsist rules** in [`ArchitectureTest`](app/src/test/kotlin/com/effekt3r/mrd/ArchitectureTest.kt): no feature imports another; `:core:model` and `:core:common` stay free of Android; money is never floating point; raw colours live only in the design system; ViewModels expose read-only state; repository implementations are internal; model types are immutable. The [probe kit](tools/architecture-probes/README.md) plants deliberate violations next to allowed look-alikes, proves that each rule fails when it should, and lists what the rules don't catch.
- **Golden data tests.** `GoldenFilesTest` fails when a committed mock-API file differs by a single byte from what the generator writes, so the data can't drift from the code and the docs that describe it.
- **CI gates.** **Build and test** (formatting, lint, the unit, Robolectric and architecture tests, the R8 release build and its missing-rules check) and **Secret scan** (gitleaks over the full history) are required checks on `dev` and `master`.
- **Claude Code's settings.** [`.claude/settings.json`](.claude/settings.json) stops its Read tool from opening keystores, certificates, signing and secrets properties and `.env` files, and denies force pushes and `git commit --no-verify`. The Read rule doesn't cover shell commands, so signing material stays outside the repository.
- **No AI attribution in commits,** by my choice. Every commit is authored by me, or by Dependabot for its own updates, and none carries an AI trailer. I disclose AI use here and in each pull request's AI notes instead.

## The decisions I made

Each decision links to the prompt where I made it. Where Claude offered options, the prompt shows them in Claude's words.

1. **The brief, done exceptionally** ([prompt 2](docs/ai/prompts.md#prompt-2)). Every requirement, both stretch goals, one Task 3 improvement, a real multi-module build, tests at each layer and CI. I wanted to impress ([prompt 1](docs/ai/prompts.md#prompt-1)) while being able to defend every line, and the "maximal showcase" option risked reading as AI bloat against a 4-hour brief. Claude estimated this scope at 1 to 1.5 days; see [What it cost](#what-it-cost).
2. **Static JSON in this repository, fetched over real HTTPS** ([prompt 3](docs/ai/prompts.md#prompt-3)). Airplane mode really fails, so offline and retry are shown honestly. With an in-app fake backend, airplane mode would do nothing.
3. **R8, edge-to-edge and signing from the start** ([prompt 4](docs/ai/prompts.md#prompt-4)). A release-quality build, not a debug demo.
4. **"Open now" stays correct offline, as the Task 3 improvement** ([prompt 5](docs/ai/prompts.md#prompt-5)). Offline is one of the brief's primary focus areas, and a cached open flag silently goes wrong. [SOLUTION.md](SOLUTION.md#the-measurable-improvement-open-now-stays-correct-offline) measures how wrong.
5. **No maps** ([prompt 6](docs/ai/prompts.md#prompt-6)), in my own words rather than Claude's recommended map card: the generated restaurants have no location data, so a map would show invented pins.
6. **Approach A: Room as the single source of truth, with Paging and filtering in SQL** ([prompt 7](docs/ai/prompts.md#prompt-7)). It meets "design for a much larger list" in code, offline isn't a special code path, and the network-first alternative would have had two sources of truth (spec §3).
7. **My own working practices** ([prompt 8](docs/ai/prompts.md#prompt-8)). I pointed Claude at the CLAUDE.md and workflow files of my other projects. [WORKFLOW.md](WORKFLOW.md) is adapted from that shared working agreement.
8. **Official documentation links** ([prompt 9](docs/ai/prompts.md#prompt-9)). Every technology in the spec and the README links to its developer.android.com article, or to the library's own docs where there's none.
9. **Modules inside a thin app** ([prompt 10](docs/ai/prompts.md#prompt-10)). I quoted the brief, "Assume this feature will later live inside a multimodule application maintained by multiple squads", and asked for the plan. The feature is library modules; `:app` stands in for the Mr D app; the platform modules are marked as stand-ins; CODEOWNERS names illustrative squads; and the build enforces the boundaries.
10. **A real git workflow, and a public repository run safely** ([prompt 12](docs/ai/prompts.md#prompt-12)). `master`, `dev` and short-lived branches, small commits and pull requests, because "we need to demo how we worked". Rulesets, required checks, secret scanning with push protection, a gitleaks hook, a read-only Actions token and [SECURITY.md](SECURITY.md) followed.
11. **Setup requirements in the README** ([prompt 14](docs/ai/prompts.md#prompt-14)), so a reviewer can run the project from a clean machine.
12. **Proven versions over the newest** ([prompts 15](docs/ai/prompts.md#prompt-15) and [16](docs/ai/prompts.md#prompt-16)): "it's okay if we use a older version of anything to ensure stability and compatibility". Claude's first version list took the newest release of everything. This became the [version policy](README.md#toolchain-and-version-policy): Now in Android's toolchain lines, libraries at least four weeks old, a spike build before the plan, and a 30-day cooldown on Dependabot's updates.
13. **Subagent-driven execution** ([prompt 17](docs/ai/prompts.md#prompt-17), "merged and 1"). Claude offered two ways to run a plan: 1, a fresh subagent per task with reviews in between, or 2, inline in the main session. I chose 1, so each task had an independent check.
14. **Testing on my own phone** ([prompts 19](docs/ai/prompts.md#prompt-19) and [29](docs/ai/prompts.md#prompt-29)). It runs Android 17, newer than the app's target SDK, with three-button navigation. The emulator covered gesture navigation, font scales and landscape.
15. **A placeholder without a button** ([prompt 20](docs/ai/prompts.md#prompt-20)). The first build on my phone had an "Open a sample restaurant" button: "shouldnt show open sample restaurants button, should just open it". It became a tappable restaurant row, the way the real list works.
16. **`master` stays the default branch** ([prompt 23](docs/ai/prompts.md#prompt-23)), against Claude's recommendation of `dev`, so visitors land on the last reviewed release. The cost: Dependabot reads its configuration from the default branch, so it only started with the first release.
17. **No v0.1.0 tag** ([prompt 30](docs/ai/prompts.md#prompt-30)). Claude recommended tagging the first release; I said not yet, and the first tag is `v1.0.0`.
18. **Menus on Details** ([prompts 31](docs/ai/prompts.md#prompt-31) and [32](docs/ai/prompts.md#prompt-32)). I asked whether the brief allowed generating more mock data, and said the app didn't yet look like Mr D's. Menus, the brief's optional data, became spec §5.9 and milestone 2b, and the roadmap moved the Mr D look forward to milestone 4.
19. **A faster pace when time ran long** ([prompt 33](docs/ai/prompts.md#prompt-33)): "This was supposed to be a 4 hour task. Executed exceptionally well. But we're reaching 24hours now..." From then on, a plan was the first commit on its milestone's branch instead of a pull request of its own; one combined verifier-and-review pass replaced two reviews per task; and independent milestones ran in parallel.
20. **Merging Dependabot's updates** ([prompts 35](docs/ai/prompts.md#prompt-35) and [37](docs/ai/prompts.md#prompt-37)). That included #12, the androidx update, which Claude suggested closing until a planned SDK upgrade because it needs compile SDK 37. It went in as #20: the app compiles against SDK 37 and still targets 36, so its runtime behaviour doesn't change.
21. **Signing with an upload key Claude generated** ([prompt 38](docs/ai/prompts.md#prompt-38)). The keystore was made on my Mac, outside the repository, and its password is in my macOS Keychain. The signing secrets live only on the `release` environment, which only `v*` tags can deploy from.

Five times I went against what Claude proposed: older versions rather than the newest, no maps, `master` as the default branch, no v0.1.0 tag, and merging #12.

## Where the AI was wrong

[`docs/ai/corrections.md`](docs/ai/corrections.md) has 54 entries, newest first: each time the AI was wrong and it was caught. One records a check rather than a mistake: Retrofit's and OkHttp's repositories had moved to a new organisation, and the move was genuine.

| Kind of mistake | Entries |
|---|---|
| The build and its checks: Gradle, the architecture tests, lint | 12 |
| CI, security and the supply chain, including version choices | 13 |
| How a library, an API or a service really behaves | 6 |
| Logic: sync, state, timing and the app's lifecycle | 6 |
| UI, layout and resources | 6 |
| Tests and probes that couldn't fail | 4 |
| Docs and commit messages that were wrong: claims, counts, links and setup steps | 6 |
| An agent breaking a house rule (an AI trailer, amended before review) | 1 |

**What caught them:** the independent reviews and the verifier, 28; the spikes, 9; me, 2; and the agents' own checks and tests, the controller and the build, 15.

The five I learned most from:

1. **`no-cache` would have downloaded the whole list on every refresh.** The spec said each request sends `Cache-Control: no-cache`, so OkHttp revalidates against GitHub's ETag. OkHttp does the opposite: it skips its cache and sends no `If-None-Match`. Milestone 3's spike caught it when its 304 test failed. Lesson: check what a library does with a header, not what the header means in the HTTP specification.
2. **Three failures the sync's tests never tried.** A full disk during the snapshot swap escaped `refresh()` and left it refreshing forever; a 304 was trusted as proof that the database was current; and nothing tested that the swap is one transaction. Milestone 3's combined verifier and code review proved each one with a probe test in a disposable clone. Each fix now has a test that fails without it.
3. **Gradle could skip the architecture tests.** Konsist reads source files Gradle can't see, so a violation in a module outside `:app`'s classpath left the tests UP-TO-DATE, or restored a cached pass. A code-quality review caught it by adding a public repository class and watching the build pass. The test task now declares every source as an input, and probes P8 and P9 prove it.
4. **A deep link was lost after Android killed the app in the background.** The listener for new intents was registered after the first composition, but when Android recreates a killed app's activity, the link arrives before anything is composed. The design notes, the tests and the first walk covered a cold start and a warm one, never process death with the task kept, which is how a link from a notification usually arrives on a real phone. All 221 tests passed; the verifier's walk on the release APK caught it, 3 times out of 3. Lesson: test every state an app can receive input in, not just cold and warm.
5. **"Use the latest versions" was the wrong default.** Claude's first version list took the newest release of everything, including a Robolectric released that morning. I caught that one, and it became the version policy.

The pattern: most of these were plausible claims that nobody had run. What caught them wasn't reading the code harder. It was running it, and breaking it on purpose.

## What it cost

The brief scopes the work at about 4 hours, and says I can take as long as I need. I chose a much larger scope ([prompt 2](docs/ai/prompts.md#prompt-2)), which Claude estimated at 1 to 1.5 days including my review. From my first prompt to these release documents took about a day and a half of wall-clock time. By the time I asked for a faster pace ([prompt 33](docs/ai/prompts.md#prompt-33)), 23 hours had passed, about 9 of them pauses.

Why it took that long:
- **The scope I chose:** both stretch goals, menus, ten modules, tests at every layer, CI, a hardened public repository and a signed release.
- **The ceremony around each milestone:** a spike, a plan and a pull request to approve it, then implementation with reviews (two per task in milestone 1), and a verifier.
- **Pauses:** a night, and five times the session stopped at Claude Code's usage limit until it reset.

The ceremony found real defects. By then the corrections log had about 35 entries, among them architecture tests that Gradle could skip, Java 21 methods that would crash Android 8 to 14, and a secret scan that would have missed a 6 MB file. After the change, milestones 2b, 3 and 4 ran in parallel and all three merged within five hours, with one combined review each. That single review still found the three gaps in the sync. Milestones 5 and 6 went lighter still, with no spike and no full plan, and their verification still found the deep link lost after process death and a menu retry that skipped a fetch still running. So the rigour paid for itself on the risky parts, and two reviews for every small task was overhead.

## What I'd keep, and what I'd drop

**Keep:**
- **The spec first, one decision at a time.** Claude recommended, I chose, and I overrode it where I knew better.
- **Spikes before plans.** They caught design errors before any plan carried them: `no-cache`, the secret-scan gap, TheMealDB's area names and the 219 menu files that were really 218.
- **Reviews that run the code and break it on purpose,** and house rules proven with probes.
- **The walk on the release APK.** It found the one critical bug, the deep link lost after process death, when every test passed.
- **The corrections log.** Each lesson went into the next brief.

**Drop or change:**
- **Two reviews for every task.** One combined review per milestone found the important gaps in far less time. I'd keep two reviews for the risky tasks only.
- **A pull request just to approve a plan.** The plan as the first commit on the milestone's branch is enough.
- **Plans with complete code for every step.** The plans total nearly 14,000 lines. Milestones 5 and 6 worked from 64 lines of design notes, and their verification still caught what mattered. Plans with the interfaces, the tests and the tricky parts would be faster to write and to review.
- **Milestones one after another.** Independent milestones ran in parallel once I asked; I'd plan for that from the start.
- **An open-ended time budget.** I'd set one at the start and fit the ceremony to it.
