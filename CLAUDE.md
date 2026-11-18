# CLAUDE.md

Cold-start brief for AI-assisted sessions in this repository. Keep it true: when a change makes something here wrong, fix it in the same change.

@WORKFLOW.md

## What this is

Mr D Restaurant Discovery is a take-home assessment for Mr D (Senior Android Engineer), written in Kotlin and Jetpack Compose. It's offline-first and split into modules. The design is in `docs/specs/2026-11-18-restaurant-discovery-design.md`. The plans are in `docs/plans/`: the roadmap first, then one plan per milestone.

## Modules

| Module | Role |
|---|---|
| `:app` | Host shell: navigation graph, edge-to-edge, R8, signing. No feature logic. |
| `:feature:browse`, `:feature:details` | Screens. They never depend on each other. |
| `:core:data` | Restaurant API, Room, repositories (implementations are `internal`). |
| `:core:model`, `:core:common` | Plain Kotlin: domain types, dispatchers and clock. |
| `:core:ui` | Restaurant UI shared by both features. |
| `:core:network`, `:core:designsystem`, `:core:testing` | Platform stand-ins: HTTP and JSON, tokens and components, test utilities. |
| `build-logic/` | Convention plugins `mrd.android.application`, `.library`, `.compose`, `.feature`, `mrd.hilt` and `mrd.jvm.library`. |

## Commands

- Build and install: `./gradlew installDebug`
- Tests: `./gradlew testDebugUnitTest` (every module, including the pure Kotlin ones)
- Format: `./gradlew spotlessApply` (CI runs `spotlessCheck`)
- Lint: `./gradlew :app:lintDebug`
- R8 release build: `./gradlew :app:assembleRelease`, then check that R8 ran and found nothing missing: `test -s app/build/outputs/mapping/release/mapping.txt && test ! -s app/build/outputs/mapping/release/missing_rules.txt`

## Hard rules, and why

- **Features never depend on features, and core modules never depend on features or `:app`.** A project-wide rule in `settings.gradle.kts` fails the build for both; `ArchitectureTest` also fails on feature-to-feature imports. Why: squads ship features independently and `:app` is the only place features meet, and core modules sit below features, so depending upward would invert the architecture.
- **Pinned versions stay pinned** unless a plan says otherwise. Why: the version policy in the README; a spike build validated the set together.
- **Money is integer cents, never floating point; colours come from design tokens; user-facing text lives in string resources.** `ArchitectureTest` enforces the first two: it rejects `Double` or `Float` money and raw colours outside the design system.
- **Never weaken a test or a rule to make something pass.** Fix the code, or stop and ask.
- **House rules are proven, not assumed.** When you change `ArchitectureTest`, re-run the probes in `tools/architecture-probes/` and compare the results with its README. A rule that can't fail is worse than having no rule.
- **Release builds are R8-minified.** Anything read by name at runtime needs a keep rule with a comment. Check R8 changes by running the release APK, not by compiling.
- **Never commit secrets.** Signing comes from `MRD_*` properties or environment variables, so no keystore or password ever lives in the repository. `.claude/settings.json` also stops Claude Code's Read tool from opening keystore and signing files, but it doesn't cover shell commands, so keep those files outside the repository.

## Definition of done for each change

1. Tests for new logic, written first where practical.
2. `spotlessCheck`, lint and the tests pass locally.
3. UI changes walked on the emulator: light and dark theme, 200% font, rotation.
4. One logical step per commit, with a `Verified:` line. The PR uses the template, including AI notes.
5. Anything the AI got wrong that was caught goes into `docs/ai/corrections.md`.

## Current state

Milestone 1 (foundation) is done: the modules, convention plugins, app shell, architecture tests, CI and the README requirements. Its verification report is `docs/qa/2026-11-18-m1-foundation.md`. Next is milestone 2 (mock API data), which ends with the first release to `master`; see `docs/plans/2026-11-18-00-roadmap.md`.
