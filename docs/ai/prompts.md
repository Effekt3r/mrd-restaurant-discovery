# Prompts

Everything I said to Claude Code while building this project, in order: what I typed, and how I answered its multiple-choice questions. The brief asks for the prompts behind AI-assisted work, and [`AI_USAGE.md`](../../AI_USAGE.md) explains the decisions they led to and why I made them.

- **How this file is made:** [`tools/ai/extract_prompts.py`](../../tools/ai/extract_prompts.py) reads Claude Code's session logs and writes it. The logs aren't committed: they also hold tool output, local paths and the agents' working.
- **Left out:** system reminders, tool output, task notifications, skill text, compaction summaries, interruption markers, command wrappers and empty messages.
- **Redacted:** home paths become `~/`, other drives `[another drive]`, a keystore's path `[keystore path]`, email addresses `[email]` and my employer's name `[work]`. The OfferZen brief I pasted in at the start is replaced by a pointer to spec §2, which maps each of its requirements to the design.
- **Claude's questions:** where I answered one, its question and options come first, in Claude's words, and my answer is the quote. "(Recommended)" is Claude's label for the option it recommended.
- **Not here:** the prompts Claude wrote for its own subagents. The milestone plans in [`docs/plans/`](../plans/) are what the implementing agents worked from, and the verifier's reports are in [`docs/qa/`](../qa/).
- **Times** are UTC. Cape Town is two hours ahead.

38 prompts, the last at 2026-11-19 10:24 UTC.

## 2026-11-18

### Prompt 1

*06:33 UTC · typed*

> busy with a interview process for a Senior Android developer role at MrD. https://play.google.com/store/apps/details?id=com.mrd.food&hl=en_ZA . now we need to do the take home assessment, they allow the use of ai, but part of the next step would be to explain why and how we used ai, which decisions were made and what our reasoning was. here's a brief ->
>
> [the OfferZen assignment brief, pasted; see spec §2]
>
>  then also ~/Downloads/MRD\ \|\ Senior\ Android\ Engineer\ Assignment.pdf . so under https://github.com/Effekt3r we need to create a public repo, as we would for a new project. android kotlin based with jetpack compose. start off with the spec then we go from there, no claude authoring, commit all as myself. I gave you the google play store listing so that you can design the app to look like theirs. ~/Downloads/Mr_D_Senior_Software_Engineer_Android_Job_Spec.pdf lets see how we can impress them.

### Prompt 2

*06:40 UTC · my answer to Claude's question*

Claude asked: "How ambitious should the scope be, given you'll have to defend every line in the technical interview?"

- **Brief, done exceptionally (Recommended):** Every requirement, both stretch goals (favourites and deep link), one measurable Task-3 improvement, a real multi-module build with convention plugins, tests at each layer, and GitHub Actions CI. Includes an explicit 'what I chose not to do' list. About 1–1.5 days including your review.
- **Stay close to 4h:** Requirements plus one Task-3 item, a single app module with clean package boundaries, and unit tests. Modularisation is only described in SOLUTION.md. Least to defend, least differentiation.
- **Maximal showcase:** The recommended scope plus adaptive list-detail for tablets and foldables, screenshot tests, Macrobenchmark with Baseline Profiles, and a debug network-chaos panel. Most impressive on paper, but the most to defend, and it can read as AI bloat against a 4h brief.

> Brief, done exceptionally (Recommended)

### Prompt 3

*06:40 UTC · my answer to Claude's question*

Claude asked: "Where should the REST/JSON restaurant data come from?"

- **Static JSON in this repo (Recommended):** A seeded generator writes the JSON into the public repo, and the app fetches it over real HTTPS from raw.githubusercontent.com with Retrofit and OkHttp. The response is snapshotted into Room as the source of truth. Airplane mode genuinely fails, so offline, retry and first-launch-with-no-network all demo for real. Needs internet on first launch, which is realistic.
- **In-app fake backend:** An OkHttp interceptor serves bundled JSON with simulated latency and failures. It can mimic paging and search APIs, and it's fully self-contained and deterministic. But airplane mode does nothing, so offline only works if you trigger it in code.
- **Bundled assets only:** Read JSON from assets. The brief explicitly allows this and it's the simplest option, but the networking and offline story is weakest.

> Static JSON in this repo (Recommended)

### Prompt 4

*06:41 UTC · typed while Claude was working*

> let's ensure R8, edge-to-edge support, signing setup, if we use google maps, we want them to be able to debug so don't restrict it - note it that this was intentionally set so that they can test and the api only allows the required google services

### Prompt 5

*06:41 UTC · my answer to Claude's question*

Claude asked: "Which Task-3 'measurable improvement' should be the headline?"

- **Open-now correct offline (Recommended):** Strengthens the offline baseline. A cached is_open boolean goes stale: offline, a place that closed at 21:00 keeps showing Open. So the API also ships weekly opening hours, and the app works out open/closed at current Cape Town time, re-checked every minute while the screen is visible. Adds an offline banner ('Updated 12 min ago') and refreshes automatically when the connection returns. Measured with boundary unit tests (overnight hours, midnight, missing data) and shown live in the video.
- **Large-list perf, benchmarked:** Macrobenchmark of scroll jank and startup on a 10,000-restaurant dataset, with vs. without a Baseline Profile; numbers go in SOLUTION.md. The most literally 'measurable', and matches the job spec's 'monitor and profile performance'. Adds a benchmark module, and emulator numbers are noisy.
- **Survive restarts:** Keep the search text, Open-now toggle and scroll position (anchored by restaurant id) across rotation, process death and cold restarts. Proven with state-restoration tests. Solid but the least distinctive.

> Open-now correct offline (Recommended)

### Prompt 6

*06:55 UTC · my answer to Claude's question, in my own words rather than one of its options*

Claude asked: "Should the app use Google Maps? The brief doesn't require it, but the job spec lists Google Maps."

- **Map card on details (Recommended):** A small non-interactive (lite mode) Google Map with the restaurant pin and address on the details screen. The data gains address and lat/lng around Cape Town suburbs. Offline it falls back to showing just the address. Uses the key policy you described. Needs a Google Cloud project with billing for the key; I can create it with gcloud if you're logged in.
- **No Maps:** Stay within the brief: no Maps SDK and no key to manage. A location map gets mentioned as a next step in SOLUTION.md.

> No maps, we don't have location data for those restaurants

### Prompt 7

*07:01 UTC · typed*

> yeah happy to proceed with approach A

### Prompt 8

*07:06 UTC · typed*

> brilliant - then a side note, look at my other project's claude.md and workflow.md files (~/Development and [another drive]) for best practices, something other developers doing this assignment with claude might not have

### Prompt 9

*07:09 UTC · typed while Claude was working*

> for any spec we use like jetpack compose, hilt, room etc, give the url to the android developer article such as https://developer.android.com/training/dependency-injection/hilt-android - all part of documentation

### Prompt 10

*07:11 UTC · typed while Claude was working*

> Assume this feature will later live inside a multimodule application maintained by multiple squads. - what's the plan here? build as a module wrap in an app?

### Prompt 11

*07:22 UTC · typed*

> amazing

### Prompt 12

*07:23 UTC · typed while Claude was working*

> remember now the git repo, don't commit all at once, we need a master, a dev and feature/fix/chore branches - we need to demo how we worked, how everything was setup, also public safety for the public repo, PRs are required for merge to master

### Prompt 13

*07:45 UTC · typed*

> both merged

### Prompt 14

*07:47 UTC · typed while Claude was working*

> this would also need to be set as the requirements to get the project up and running, in the readme file

### Prompt 15

*07:50 UTC · typed while Claude was working*

> yeah we also don't have to use the latest

### Prompt 16

*07:50 UTC · typed while Claude was working*

> it's okay if we use a older version of anything to ensure stability and compatibility

### Prompt 17

*08:21 UTC · typed*

> merged and 1

### Prompt 18

*10:12 UTC · typed*

> continue

### Prompt 19

*11:38 UTC · typed*

> please launch this on my s25 when ready

### Prompt 20

*11:39 UTC · typed*

> shouldnt show open sample restaurants button, should just open it

### Prompt 21

*13:35 UTC · typed*

> making progress?

### Prompt 22

*15:10 UTC · typed*

> continue

### Prompt 23

*16:16 UTC · my answer to Claude's question*

Claude asked: "Which branch should be the repo's default on GitHub (what visitors see first, and what Dependabot and new PRs use)?"

- **dev (Recommended):** Make dev the default, as many git-flow teams do. Dependabot, security-fix PRs and the Gradle CI cache then work as documented, and new PRs target dev automatically. The landing page shows the latest integrated work; master still receives only reviewed releases, and we finish with a release to master before submission.
- **master:** Keep master as the default, so the landing page always shows the last reviewed release. Dependabot stays idle until the first release to master (planned right after milestone 2), security-fix PRs go to master and get merged back into dev like hotfixes, and we change the CI cache settings so dev pushes still fill it.

> master

### Prompt 24

*17:17 UTC · typed*

> https://github.com/Effekt3r/mrd-restaurant-discovery/pull/4 merged

### Prompt 25

*18:47 UTC · typed*

> merged

## 2026-11-19

### Prompt 26

*03:14 UTC · typed while Claude was working*

> Continue

### Prompt 27

*03:57 UTC · typed while Claude was working*

> Merged

### Prompt 28

*05:19 UTC · typed*

> merged

### Prompt 29

*05:31 UTC · typed*

> merged, please launch on the samsung

### Prompt 30

*05:33 UTC · my answer to Claude's question*

Claude asked: "v0.1.0 is merged into master (merge commit b2d3ebb) and its production endpoint answers. May I tag that merge commit v0.1.0 and create the GitHub release? Release tags can't be moved or deleted afterwards (the tag ruleset blocks it)."

- **Tag and release (Recommended):** Annotated tag v0.1.0 on b2d3ebb, plus a GitHub Release with notes and no APK (signed builds arrive in milestone 7).
- **Tag only:** Push the v0.1.0 tag but don't create a GitHub Release page.
- **Not yet:** Leave master untagged for now; I'll tag later when you say so.

> Not yet

### Prompt 31

*05:33 UTC · typed while Claude was working*

> did the assignment mention we can generate more of our own mock data? if so lets do. also doesnt really look like the MrD app and the list items doesnt show necessary detail

### Prompt 32

*05:36 UTC · my answer to Claude's question*

Claude asked: "Should we also generate menus (the brief's optional menu data) and show them on the Details screen?"

- **Yes, menus on Details (Recommended):** Extend the generator with a deterministic menu per restaurant (categories, items, prices in cents, options, availability, deliberately messy fields), served as mock-api/v1/menus/{id}.json. Details shows the restaurant at once from the cache, then loads the menu with its own loading and error state, cached for offline. It adds roughly half a milestone, spread over milestones 2b, 3 and 6.
- **No, keep the current scope:** 220 + 10,000 restaurants already satisfy the brief's dataset requirement. Menus stay in spec §12's 'deliberately not done', and the time goes into the Mr D look, Browse and Details.
- **More restaurants instead:** Grow the main dataset beyond 220, for example to 500. The 10,000 stress file already covers scale, so this mainly changes how long the list feels.

> Yes, menus on Details (Recommended)

### Prompt 33

*05:42 UTC · typed while Claude was working*

> This was supposed to be a 4 hour task. Executed exceptionally well. But we're reaching 24hours now...

### Prompt 34

*07:17 UTC · typed*

> please merge all those

### Prompt 35

*07:26 UTC · typed*

> and 9-15 , also merge

### Prompt 36

*09:19 UTC · typed*

> continue

### Prompt 37

*09:27 UTC · typed*

> please sort out PR 12, 15, 18 - merge them

### Prompt 38

*10:24 UTC · my answer to Claude's question*

Claude asked: "For the signed v1.0.0 release, how should the upload keystore and its GitHub secrets be handled?"

- **I generate it (Recommended):** I create an upload keystore with keytool on this Mac, outside the repo ([keystore path]), with a random password saved in your macOS Keychain. I set GitHub secrets (base64 keystore + passwords) on a `release` environment limited to v* tags, and release.yml attaches the signed APK to the v1.0.0 GitHub Release. You back the keystore up afterwards.
- **You create it yourself:** I write release.yml and step-by-step instructions; you run keytool and set the secrets yourself (gh secret set or GitHub's UI). I never touch the keystore or secrets.
- **Skip signing:** No release workflow or signed APK: AC-22 is listed as not done in SOLUTION.md, and local release builds keep the debug-key fallback.

> I generate it (Recommended)
