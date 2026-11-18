# How we work

The general way of working in this repository: process, guardrails and terminology. Project details live in `CLAUDE.md`. This is adapted from the shared working agreement I use across my own repositories.

## Git and pull requests

- Branches: `master` (releases), `dev` (integration) and short-lived `feature/`, `fix/` and `chore/` branches. Details are in `CONTRIBUTING.md`.
- One logical step per commit, with its tests in the same commit.
- Never bypass a safety check: no `--no-verify`, and no force pushes to shared branches. If a hook fails, fix the cause.
- Never tag unless asked. A `v*` tag builds and publishes a signed release.
- Commit messages: a conventional subject, the reason in the body, and a `Verified:` line.

## Testing and verification

- Tests come with every change. Go red, then green, then commit, wherever the code allows it.
- Compiling isn't evidence. "BUILD SUCCESSFUL" says nothing about runtime behaviour; only running the app does.
- Run it before committing. UI changes are walked on the emulator; R8 changes are checked on the release APK.
- Before handing over a change, trace the real flow end to end: entry, state change, persistence and re-entry.

## Errors must teach

- Errors shown to users say what happened and what to do next. Errors in logs name the step that failed.
- Keep the corrections log (`docs/ai/corrections.md`), newest first: what happened, the root cause, the fix and the lesson.
- When you find a root cause, look for every other place it did damage. Fix the whole class of bug, not just the one instance.

## UI and architecture defaults

- Design for the smallest screen first (360 dp wide), then larger ones. Check every screen for overflow and at 200% font scale.
- Use design-system tokens only: no hard-coded colours, type or spacing.
- All user-facing text lives in string resources.
- Accessibility is part of done: TalkBack labels, 48 dp touch targets and enough contrast.

## Working with AI

- Brainstorm, then spec, then plan, then build, for each feature. Specs and plans are committed under `docs/`.
- AI drafts and a person decides. Every decision with real options comes to me with a recommendation, and I choose.
- Verify before claiming. Words like "fixed", "works" or "root cause" need evidence from the same session: command output, test results or a screenshot.
- Finish the agreed scope, and list follow-ups rather than quietly building them.
- Don't name tools or vendors in user-facing copy.
- Don't put local machine paths in the repository.

## Shared context

- Market: South Africa. Money is in rand as integer cents, the locale is en-ZA, and restaurant times are in Africa/Johannesburg.
