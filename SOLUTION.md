# Solution

How I designed Mr D Restaurant Discovery, the trade-offs I made, and how I'd evolve it for a large app maintained by several squads (the brief's Task 3). The [design spec](docs/specs/2026-11-18-restaurant-discovery-design.md) has the full detail, and the sections below point into it. The [README](README.md) says how to run the app, and [AI_USAGE.md](AI_USAGE.md) how I used AI.

## What I built

- **Browse:** a paged, adaptive grid of restaurants with search by name, an Open now toggle, a result count, pull-to-refresh, an offline banner, and distinct loading, empty and error states (spec [§6.2](docs/specs/2026-11-18-restaurant-discovery-design.md#62-browse)).
- **Details:** the photo, the rating, ETA and delivery fee, the status with its next change, the week's opening hours and the menu, which loads separately with its own states (spec [§6.3](docs/specs/2026-11-18-restaurant-discovery-design.md#63-details)).
- **Favourites** on both screens, kept across restarts and refreshes (spec [§5.8](docs/specs/2026-11-18-restaurant-discovery-design.md#58-favourites)).
- **A deep link,** `mrd://restaurant/{id}`, that opens Details with Browse underneath (spec [§4.5](docs/specs/2026-11-18-restaurant-discovery-design.md#45-navigation-and-deep-links)).
- **Offline first.** Room is the single source of truth, so all of the above works offline from the last snapshot.
- **The Task 3 improvement:** Open now stays correct offline, [measured below](#the-measurable-improvement-open-now-stays-correct-offline).
- **A mock API** that a seeded generator writes into this repository, served over HTTPS, with deliberately messy data and a 10,000-restaurant stress file.
- **A release pipeline:** a `v*` tag builds an R8-minified APK, signed with the upload key, and attaches it to a GitHub Release.

## Task 3: evolving this for many squads

The brief asks five questions. Each answer starts from what's in this repository, then says what would change with several squads.

### How I'd split modules and share components

**As built.** Ten Gradle modules, plus the build logic and the data generator:

| Module | Role | Owner if this lived in the Mr D app |
|---|---|---|
| `:app` | Host shell: the navigation graph, edge-to-edge, R8 and signing. No feature logic. | Mobile Platform |
| `:feature:browse`, `:feature:details` | The two screens and their ViewModels | Discovery |
| `:core:data` | The restaurant and menu API, Room, sync and repositories | Discovery |
| `:core:model` | Domain types and the open-status rules, in plain Kotlin | Discovery |
| `:core:ui` | Restaurant UI shared by both features | Discovery |
| `:core:designsystem` | Mr D's tokens, Figtree, `MrdTheme` and generic components | Design Platform |
| `:core:network`, `:core:common`, `:core:testing` | HTTP and JSON; dispatchers, clock and ticker; test utilities | Mobile Platform |

- **Two kinds of core module.** The platform stand-ins (`:core:network`, `:core:common`, `:core:designsystem` and `:core:testing`) are things the Mr D app already has, and its own would replace them. The discovery modules would move across as they are (spec [§4.1](docs/specs/2026-11-18-restaurant-discovery-design.md#41-modules-and-ownership)).
- **The host contract runs through Hilt.** The host provides an `OkHttpClient` carrying its own auth, logging or pinning interceptors, the `Json` configuration, an IO dispatcher, a `Clock`, a `NetworkMonitor` and an application scope. Each feature exposes its route types and a function that adds it to the host's navigation graph (spec [§4.3](docs/specs/2026-11-18-restaurant-discovery-design.md#43-how-a-host-app-adopts-the-feature)).
- **Components are shared in two layers.** The design system's components take display-ready text, never model types. `:core:ui` turns the restaurant model into that text for both features. So the design system knows nothing about restaurants, and neither feature repeats the other's formatting.
- **Convention plugins** in `build-logic/` (`mrd.android.application`, `.library`, `.compose`, `.feature`, `.room`, `mrd.hilt` and `mrd.jvm.library`) hold the shared setup, so each module's build file states only what's unique to it.
- **The layering is enforced, not just documented.** A rule in `settings.gradle.kts` fails the build when any module breaks it: a feature depending on a feature, or a core module on a feature or on `:app`. Konsist's `ArchitectureTest` enforces the rules inside the code, and the [probe kit](tools/architecture-probes/README.md) proves each rule fails when it's broken.
- **Ownership is written down** in [`.github/CODEOWNERS`](.github/CODEOWNERS), with illustrative squad names.

**With many squads:**
- **Split each feature into `api` and `impl`.** `:feature:details:api` would hold only `RestaurantRoute` and `navigateToRestaurant`. Another squad's screen, say order history, could open Details by depending on that small module, and changes inside `impl` wouldn't recompile it. The settings rule would then also forbid depending on another feature's `impl`.
- **The design system gets an owner.** A Design Platform squad owns `:core:designsystem`, reviews every change through CODEOWNERS, and covers each component with screenshot tests, so a visual change arrives as a reviewed diff rather than a surprise in another squad's screen.
- **A database per squad,** so one squad's migration can't block another's release. Discovery's `discovery.db` is already its own, with its schema exported for migration tests.
- **Convention plugins are the platform squad's lever:** SDK levels, lint policy and compiler options change in one place for every module.
- **CODEOWNERS with real teams,** whose review is required.
- **Dynamic feature delivery,** only if install size or rarely used flows justify it.

### How I'd organise navigation, state and data boundaries

**As built:**
- **Navigation.** Each feature owns `@Serializable` routes: `BrowseRoute` and `RestaurantRoute(id)`. It exposes `NavGraphBuilder.browseScreen(onRestaurantClick)`, `NavGraphBuilder.restaurantScreen(onBack)` and `NavController.navigateToRestaurant(id)`, and [`MrdNavHost`](app/src/main/kotlin/com/effekt3r/mrd/navigation/MrdNavHost.kt) in `:app` is the only place they meet. Browse doesn't know Details exists; it only reports which restaurant was tapped.
- **Deep links.** `mrd://restaurant/{id}` is declared on `RestaurantRoute`, and its intent filter sits on `MainActivity` in `:app`, because the host owns external entry points. On a cold or a warm start it opens Details with Browse underneath, so Back returns to the list (spec [§4.5](docs/specs/2026-11-18-restaurant-discovery-design.md#45-navigation-and-deep-links)).
- **State, in a unidirectional data flow.** Each screen's ViewModel exposes one `StateFlow` of immutable UI state, shared with `WhileSubscribed(5_000)`, so the minute ticker and the connectivity callback stop once the screen leaves the foreground. Input goes up as plain function calls, state comes down, and navigation is a callback, not an event. The search text and the Open now toggle live in `SavedStateHandle`, so they survive process death (spec [§4.4](docs/specs/2026-11-18-restaurant-discovery-design.md#44-state-and-unidirectional-data-flow)).
- **Data.** `RestaurantRepository` and `MenuRepository` are `:core:data`'s public face. Their implementations are `internal`, and Retrofit, OkHttp and Room are `implementation` dependencies, so a feature can't reach past the interfaces. Room is the single source of truth: the screens read only from the database, and network responses only ever land in it. Every failure is mapped once into a typed `SyncError` (`Network`, `Server`, `BadData` or `Storage`), and the screens see a `SyncStatus`, never an exception (spec [§5.4](docs/specs/2026-11-18-restaurant-discovery-design.md#54-errors)).

**With many squads:**
- **Routes in `api` modules, wiring in the host.** Other squads navigate with the route types from `:feature:details:api`, and the app's navigation graph wires them. A feature declares its deep links next to its routes, so it brings its own entry points.
- **Navigation 3** once an adaptive list-detail layout for tablets and foldables is a goal. For two screens, Navigation Compose's built-in deep links and predictive back were enough (spec [§3](docs/specs/2026-11-18-restaurant-discovery-design.md#3-key-decisions)).
- **Data owned per squad, shared through interfaces.** Squads share repository interfaces and model types through small `api` modules, never DAOs or entities. A domain layer of use cases arrives when two features share logic, not before (spec [§3](docs/specs/2026-11-18-restaurant-discovery-design.md#3-key-decisions)).
- **Background sync with WorkManager** once data must be fresh before a screen opens, for example for notifications.

### How I'd approach testing

**As built.** Every test runs on the JVM, so CI needs no emulator: Room runs on Robolectric's SQLite, the network on MockWebServer, and the paged list through paging-testing. `./gradlew testDebugUnitTest` runs 144 tests:

| Layer | What's tested | Tests |
|---|---|---|
| Rules in plain Kotlin | Opening hours as week-minute intervals: past midnight, the Sunday wrap, merging, 24 hours, Cape Town time; the minute ticker | 8 |
| Parsing and validation | The committed `restaurants.json` through the production parser, with the expected records kept and dropped; the menu rules | 12 |
| Network and sync | 200, a 304 with and without a matching ETag, a 500, timeouts, malformed JSON, bad data, a database failure, cancellation and single flight | 14 |
| Database | The snapshot swap is one transaction; SQL and Kotlin agree on 20 open-status cases | 2 |
| Repositories | Paging, search escaping, the count and Details following the clock, favourites surviving a swap, retry on reconnect, and each of the menu cache's states | 17 |
| Measurement | The staleness numbers [below](#the-numbers) | 2 |
| Networking setup | The client's timeouts | 1 |
| Design system | Text contrast meets WCAG AA in both themes; the XML colours match the palette | 5 |
| Architecture | The seven Konsist rules | 7 |
| Data generator | Determinism, edge cases, rates, no real restaurant names, and the golden files | 76 |

- **Tests that can fail.** Each verification broke the code on purpose. [Milestone 3's report](docs/qa/2026-11-19-m3-data-layer.md) lists 17 mutations: 14 failed a test, and the three that didn't led to new or stronger tests.
- **ViewModels and Compose UI.** ViewModel tests use Turbine and fakes for each screen's states, the search debounce, retry, refresh on reconnect and state restored after process death. Compose UI tests on Robolectric cover every screen state, the card's single TalkBack item and its action, and the deep link (spec [§7.1](docs/specs/2026-11-18-restaurant-discovery-design.md#71-tests)).
- **What JVM tests can't show** is walked on the R8 release APK: airplane mode, process death, rotation, 200% font, TalkBack, the dark theme, landscape, both navigation modes and the deep link (spec [§7.3](docs/specs/2026-11-18-restaurant-discovery-design.md#73-verification-gate)). The reports are in [`docs/qa/`](docs/qa/).

**With many squads:**
- **Screenshot tests** for the design system's components first, once several squads change shared UI, so visual regressions show up in review. I left them out because they cost time to set up and maintain for one developer.
- **Macrobenchmark and Baseline Profiles** once scroll smoothness and startup are release metrics, measured on real devices rather than an emulator, whose numbers are noisy.
- **Contract tests against the real backend,** replacing the test that parses the committed JSON.
- **Each squad owns its modules' tests** through CODEOWNERS, and CI runs the affected modules' tests only.
- **A small instrumented smoke suite** on a device farm, for what Robolectric can't show.

### How I'd keep the UI smooth as the list grows

**As built:**
- **Paging 3 over Room.** Paging loads 30 rows at a time as the grid scrolls. The database does the search, the Open now filter and the status, so the UI never filters or maps the whole list in memory.
- **Status in SQL.** The list's status, the filter and the count come from two query parameters, the current week minute and the current time, so paging and the count stay right at 10,000 rows (spec [§5.6](docs/specs/2026-11-18-restaurant-discovery-design.md#56-open-now-stays-correct-offline-task-3)).
- **Search on a normalised column.** `name_search` holds the name lowercased, without accents and with whitespace collapsed. The query is normalised the same way, its wildcards are escaped, and it's matched with `LIKE`, so "cafe" finds "Café".
- **Updates that don't move the list.** Each minute tick invalidates the current `PagingSource` rather than building a new pager, so Paging's refresh key keeps the scroll position when a status flips.
- **A sync that doesn't block the UI.** The response is read into memory and parsed record by record on the IO dispatcher, never as a JSON tree, then swapped in with one Room transaction, off the main thread. The old list stays scrollable and searchable until the new one commits. Deleting and inserting the whole snapshot avoids `NOT IN` with 10,000 ids, which would exceed SQLite's 999-variable limit on Android 10 and lower.
- **Cheap recomposition.** `:core:model`'s types are immutable, which a Konsist rule checks, and [`compose-stability.conf`](compose-stability.conf) declares them stable, so Compose can skip a card whose data hasn't changed. The grid keys each card by restaurant id.
- **Images sized for where they're shown.** A resolver asks TheMealDB for `/medium` images on cards (about 33 KB) and `/large` on Details (about 58 KB), not the 105 KB original. Coil shares the app's `OkHttpClient` and keeps its own disk cache, so images already seen still show offline (spec [§5.7](docs/specs/2026-11-18-restaurant-discovery-design.md#57-connectivity-and-images)).
- **Debounced search.** The query reaches the database 250 ms after the last keystroke, and the text field itself never waits (spec [§4.4](docs/specs/2026-11-18-restaurant-discovery-design.md#44-state-and-unidirectional-data-flow)).
- **A stress dataset.** `-Pmrd.dataset=10k` points a debug build at 10,000 restaurants, about 6.1 MB (about 470 KB gzipped), to exercise all of this under load.

**As it grows further:**
- **Server-side paging and search** (`RemoteMediator`) once the list is national rather than one delivery area. One area's snapshot stays small.
- **Full-text search** when `LIKE` on a normalised column gets slow. It's fast enough for thousands of rows.
- **A `maxSize` for Paging,** so pages far from the viewport are dropped rather than kept for the whole session.
- **`@Streaming`,** so a larger body is parsed as it arrives rather than read into memory first (spec [§5.3](docs/specs/2026-11-18-restaurant-discovery-design.md#53-sync)).
- **Baseline Profiles** for the list and Details, **Macrobenchmark** for scroll jank and startup in CI on real devices, and **JankStats** in production.

### How I'd use AI productively while keeping quality

**As built.** [AI_USAGE.md](AI_USAGE.md) has the detail.
- **The AI drafted and I decided.** Every decision with real options came to me with a recommendation, and five times I went against it.
- **A spec and a plan before code,** and each plan proven by a spike that ran its code first.
- **Independent review that runs the code.** Fresh subagents reviewed each task, and later each milestone, by building it, running probes and breaking it on purpose. A verifier checked the acceptance criteria with evidence.
- **Rules the AI can't argue with:** the settings rule, the Konsist rules and their probes, the golden data tests, the required CI checks and Claude Code's deny rules.
- **An honest record:** [my prompts](docs/ai/prompts.md), my decisions, and [47 logged mistakes](docs/ai/corrections.md), each with what caught it.

**With many squads:**
- **House rules as code.** A rule that reviewers keep repeating becomes a lint check or an architecture test, so people and agents meet the same bar.
- **A short `CLAUDE.md` per repository,** and per module where it matters: what the module is, its hard rules and why. It's fixed in the same change that would make it wrong.
- **Review by evidence.** An AI-written change comes with tests that fail without it, and the reviewer, a person or an agent, runs them. "It compiles" is never evidence.
- **AI notes on every pull request,** so a reviewer knows what was generated and what was checked.
- **A corrections log per squad,** whose lessons become the next rules.
- **Measure it:** defects caught before merge, review time and escaped bugs, to see where AI helps and where it costs time.
- **Secrets out of reach:** deny rules in the agents' settings, and no secrets in repositories.

## The measurable improvement: Open now stays correct offline

Of the brief's options, I chose "Enhance the offline baseline in a meaningful, demonstrable way" (spec [§5.6](docs/specs/2026-11-18-restaurant-discovery-design.md#56-open-now-stays-correct-offline-task-3)).

### The problem

The API's `is_open` describes the moment the response was served. Once it's cached and the phone is offline, it goes wrong: a few hours later, a closed restaurant still shows Open and still passes the Open now filter.

### The model

- **Opening hours become week-minute intervals** in the restaurant's time zone, Africa/Johannesburg, which has no daylight saving. Minute 0 is Monday 00:00 and 10,080 is the end of Sunday. Spans past midnight are split, Sunday-to-Monday spans wrap to the start of the week, and touching intervals merge.
- **Three states.** Open or Closed when the hours are known. Without hours, the server's `is_open` counts only while the snapshot is less than 30 minutes old, and after that the status is Unknown. Open now includes only Open, so the app never claims a restaurant is open when it can't back that up.
- **The same rules, twice.** The list's status is computed in SQL, so paging and the count stay correct at 10,000 rows. Details uses the same rules as plain Kotlin functions in `:core:model`.
- **A minute ticker,** driven by `java.time.Clock` and aligned to minute boundaries, runs only while a screen collects it, and each tick updates the status in place.

### The proof

`OpenStatusAgreementTest` runs 20 boundary cases through the Kotlin rules, the real paging query and the real count:
- the opening minute included and the closing minute excluded;
- overnight hours and the Sunday-to-Monday wrap;
- split shifts, 24-hour places, a closed day and touching windows;
- no hours with a snapshot 29 and 30 minutes old, 6 hours old, from the future, and without `is_open`.

Each result is compared with a hand-written expected status, not with the other side, so the test doesn't check the code against itself. In milestone 3's verification, four deliberate breaks in the SQL and one in the Kotlin each made it fail, naming the cases that broke.

### The numbers

`StalenessMeasurementTest` replays the generated data. At every quarter hour of the week, a refresh stores the true status as `is_open`, and then the phone goes offline. Among the restaurants with hours, this is how often the cached flag is wrong:

| Dataset | Restaurants with hours | 1 hour offline | 3 hours | 6 hours | 12 hours |
|---|---|---|---|---|---|
| [`restaurants.json`](mock-api/v1/restaurants.json) | 205 of 217 | 8.8% | 26.1% | 44.5% | 77.9% |
| [`restaurants-10k.json`](mock-api/v1/restaurants-10k.json) | 9,481 of 9,997 | 8.8% | 26.1% | 44.5% | 77.4% |

- **With a cached flag,** on average across the week, an hour offline means about one restaurant in eleven shows the wrong status, and 12 hours means about four in five.
- **The derived status gets none of them wrong.** That's true by construction, because the hours are both its input and the truth here. What proves the derived status is the agreement test above.
- **Restaurants without hours** show as Unknown once the snapshot is 30 minutes old, rather than guessing.

`./gradlew :core:data:testDebugUnitTest --tests '*StalenessMeasurementTest'` reruns it. The test pins these exact numbers, so a change to the data or the rules fails it, and this table is updated with the new numbers.

### Try it

1. Install a debug build and open it once online, so the restaurants are cached.
2. Turn on airplane mode.
3. In the device's date and time settings, turn off automatic time, set the time zone to South Africa (GMT+2), and set the clock to 21:59 on a Monday.
4. Open the app. Cape Town Grill, the first restaurant, is open until 22:00, and it's in the Open now list. Its cached `is_open` says open too.
5. At 22:00 it flips to Closed, opening at 11:00, and drops out of the Open now list within a minute, without the list moving. Its cached `is_open` still says open.

Restaurants without hours show as Unknown, because the snapshot is no longer fresh.

## Key decisions and trade-offs

| Decision | Instead of | Why |
|---|---|---|
| `Cache-Control: max-age=0` on every request | `no-cache` | OkHttp revalidates its cached copy against GitHub's ETag, so unchanged data costs a 304 of a few hundred bytes. With `no-cache`, OkHttp skips its cache and downloads the whole file every time, 6 MB for the stress file. Milestone 3's spike caught this ([§5.3](docs/specs/2026-11-18-restaurant-discovery-design.md#53-sync)). |
| Storing the snapshot's ETag in `sync_state` | Trusting any 304 | A 304 proves OkHttp's cache is current, not the database: a refresh can fail after OkHttp has cached the body. So a 304 skips the write only when its ETag matches the stored snapshot's ([§5.3](docs/specs/2026-11-18-restaurant-discovery-design.md#53-sync), [§5.5](docs/specs/2026-11-18-restaurant-discovery-design.md#55-room-schema)). |
| `SyncError.Storage` | Letting database exceptions escape | A full disk during a swap used to escape `refresh()` and leave it refreshing for good. Now every failure is one typed error, and a refresh always ends ([§5.4](docs/specs/2026-11-18-restaurant-discovery-design.md#54-errors)). |
| A full snapshot, swapped in one transaction | `RemoteMediator` and server-side paging | The API returns one delivery area, which stays small. Server-side paging comes once the list is national ([§5.3](docs/specs/2026-11-18-restaurant-discovery-design.md#53-sync), [§12](docs/specs/2026-11-18-restaurant-discovery-design.md#12-deliberately-not-done)). |
| `LIKE` on a normalised name column | Full-text search | It's fast enough for thousands of rows, and handles case and accents. FTS is the next step ([§5.5](docs/specs/2026-11-18-restaurant-discovery-design.md#55-room-schema), [§12](docs/specs/2026-11-18-restaurant-discovery-design.md#12-deliberately-not-done)). |
| Compile SDK 37, target SDK 36 | Moving both, or holding back the androidx updates | Compose 1.12, Core 1.19, Navigation 2.10 and androidx.hilt 1.4 need compile SDK 37, which only changes the APIs the code compiles against. The target SDK opts the app into Android 17's behaviour changes, so it moves once the app has been tested against them ([§8.1](docs/specs/2026-11-18-restaurant-discovery-design.md#81-toolchain)). |
| A menu endpoint per restaurant | Menus inside the list | The list stays small. A menu loads when Details opens, with its own loading, not-available and error states, and is cached per restaurant for offline ([§5.9](docs/specs/2026-11-18-restaurant-discovery-design.md#59-menus)). |
| Navigation Compose with type-safe routes | Navigation 3 | Built-in deep links with Browse underneath, and predictive back, for two screens. Navigation 3 earns its place with an adaptive list-detail layout ([§3](docs/specs/2026-11-18-restaurant-discovery-design.md#3-key-decisions), [§4.5](docs/specs/2026-11-18-restaurant-discovery-design.md#45-navigation-and-deep-links)). |
| kotlinx.serialization | Moshi | One compile-time, reflection-free serializer for the network data and the navigation routes, and easy on R8 ([§3](docs/specs/2026-11-18-restaurant-discovery-design.md#3-key-decisions)). |
| A custom `mrd://` scheme | HTTPS App Links | App Links need a hosted `assetlinks.json` that matches the signing key, and reviewers' own debug builds wouldn't match it. In the Mr D app they'd be App Links on Mr D's domain ([§4.5](docs/specs/2026-11-18-restaurant-discovery-design.md#45-navigation-and-deep-links)). |
| Static JSON in this repository, over HTTPS | An in-app fake server, or bundled assets | Real network behaviour: airplane mode really fails, so offline and retry are shown honestly ([§3](docs/specs/2026-11-18-restaurant-discovery-design.md#3-key-decisions), [§5.1](docs/specs/2026-11-18-restaurant-discovery-design.md#51-api-contract)). |
| No domain layer | A use case per action | Nothing is shared between features yet. Use cases arrive when two features share logic ([§3](docs/specs/2026-11-18-restaurant-discovery-design.md#3-key-decisions)). |
| minSdk 26 | 24 with desugaring | `java.time` without core library desugaring ([§3](docs/specs/2026-11-18-restaurant-discovery-design.md#3-key-decisions)). |

- **Play App Signing.** The release workflow signs with an upload key, kept outside the repository. On Google Play, Play App Signing keeps the app signing key, and Google signs what users install; the upload key only proves that an upload came from me, and Google can reset it if it's lost or leaked. The APK on GitHub Releases is signed with the upload key itself, because it's installed directly, not through Play (spec [§8.3](docs/specs/2026-11-18-restaurant-discovery-design.md#83-signing)).
- **Device coverage.** Real-device checks ran on my Galaxy S25 FE (Android 17, three-button navigation). The emulator covered gesture navigation, several font scales and landscape (spec [§13](docs/specs/2026-11-18-restaurant-discovery-design.md#13-risks)).

## Deliberately not done

From spec [§12](docs/specs/2026-11-18-restaurant-discovery-design.md#12-deliberately-not-done), updated with what I learned while building:

| Not done | Why, and when it would be done |
|---|---|
| Address picker and maps | There's no location data, and the API already returns the restaurants for one delivery area. |
| Navigation 3 and a two-pane list-detail layout | Worth it once tablet layouts are a goal. The adaptive grid covers large screens for now. |
| `api`/`impl` splits, a database per squad, dynamic feature delivery | Worth it when many squads navigate into Details and migrations start to collide. [Above](#how-id-split-modules-and-share-components). |
| Server-side paging and search (`RemoteMediator`) | Needed once the list is national rather than one delivery area. |
| Full-text search | `LIKE` on a normalised column is fast enough for thousands of rows. |
| WorkManager background sync | Refreshing when Browse opens, on pull-to-refresh and when the connection returns is enough for this flow. |
| Screenshot tests, Macrobenchmark, Baseline Profiles | Each costs time to set up and maintain. [Above](#how-id-approach-testing) says when they'd earn their place. |
| Crashlytics, analytics, Braze | They need accounts and consent flows, out of scope for a self-contained app. |
| HTTPS App Links | Verification needs an `assetlinks.json` that matches the signing key. |
| Languages other than English | The strings are already resources, so a language is a translation task. |
| Signed commits | They'd need an SSH signing key; not needed for a solo assessment. |
| `@Streaming` responses | The 6 MB stress file fits in memory. Streaming also changes when OkHttp writes its cache, so it waits until payloads grow ([§5.3](docs/specs/2026-11-18-restaurant-discovery-design.md#53-sync)). |
| Target SDK 37 | After the app has been tested against Android 17's behaviour changes ([§8.1](docs/specs/2026-11-18-restaurant-discovery-design.md#81-toolchain)). |
| Discovery's fakes in a test module of their own | `FakeRestaurantRepository` and `FakeMenuRepository` live in `:core:testing`, a platform stand-in. With several squads they'd move to a discovery-owned test module, as in Now in Android's `:core:data-test`. |
| Two weak tests in the data generator | Milestone 2b's review found that the menu edge-case tests check only the spot each one breaks, and that a "generate twice" test compares two runs in one process. The golden test still pins every byte of the data, so I left them as known gaps ([corrections log](docs/ai/corrections.md)). |

Menus were on this list until I added them on 2026-11-19 (spec [§5.9](docs/specs/2026-11-18-restaurant-discovery-design.md#59-menus)).

## Security and the supply chain

[SECURITY.md](SECURITY.md) has the full detail.

- **The app:** HTTPS only, R8-minified and not debuggable in release, and no secrets, analytics or personal data. Favourites stay on the device, and backup and device transfer are off.
- **The repository:** rulesets on `master`, `dev` and `v*` tags; required **Build and test** and **Secret scan** checks; secret scanning with push protection; and a gitleaks pre-commit hook.
- **GitHub Actions:** the workflow token is read-only by default. Two jobs can write: dependency submission's submit job, which runs no build code, and the release job, which runs only for `v*` tags. Only GitHub-owned actions and `gradle/actions` may run, each pinned to a commit SHA, and checkout doesn't keep credentials. The signing secrets live only on the `release` environment, which only `v*` tags can deploy from.
- **Dependabot's alerts.** It reports 53 open alerts, from `master`'s dependency graph: 3 critical, 22 high, 26 medium and 2 low. All of them are in build-time tooling, and none ships in the APK: `:app`'s release runtime classpath has none of the affected packages.

  | Package | Alerts | Comes in through |
  |---|---|---|
  | Netty | 41 | AGP's Unified Test Platform, which runs instrumented tests on a device or emulator. CI runs none. |
  | Bouncy Castle | 6 | AGP's SDK tools and Android Lint. `dev`'s AGP 9.5.1 brings 1.80.2, which fixes three of the six. |
  | Guava, Apache Commons Lang, Apache HttpClient | 3 | AGP, Android Lint, and Guava also through Hilt's annotation processor |
  | jose4j, JDOM | 2 | bundletool, which builds app bundles, and Jetifier, which this build doesn't enable |
  | Kotlin Gradle plugin | 1 | Gradle's `kotlin-dsl`, which compiles `build-logic` with the Kotlin embedded in Gradle |

  Kotlin 2.4.21 fixed the build-cache advisory for the project's own Kotlin plugin, but that alert stays open until a Gradle release embeds Kotlin 2.4.20 or later. A team would triage alerts like these by where the code runs. In the APK, the fix ships within days. In the build, it comes with the next AGP or Gradle update, alerts are dismissed with a recorded reason, and the risk is reviewed whenever the toolchain changes. [SECURITY.md](SECURITY.md#open-dependabot-alerts) has the assessment.
