# Restaurant Discovery: design spec

| | |
|---|---|
| Date | 2026-11-18 |
| Author | Franco Landman |
| Status | Design agreed during brainstorming; reviewed in this pull request |
| Brief | Mr D Senior Android Engineer take-home: Restaurant Discovery Mini App (scoped to about 4 hours) |

This spec came out of a brainstorming session with Claude Code. I took each decision below myself, one question at a time; the prompts and the reasoning behind them are recorded in `AI_USAGE.md`, which is added later in the project.

## 1. Goal and scope

Build a small, production-minded slice of the Mr D Android app. Users browse nearby restaurants, search by name, filter to what's open now, and open a restaurant's details, and all of it keeps working offline. The code should read like something a large app maintained by several squads could adopt, and the UI should look like the current Mr D app.

The scope is the brief, done thoroughly:

- every requirement;
- both stretch goals (favourites and deep links);
- one measurable Task 3 improvement;
- a real multi-module build;
- tests at each layer;
- CI;
- an R8-minified, signed release.

Everything else is listed in §12 with the reason it was left out.

## 2. From the brief to the design

| Brief requirement | Design | Checked by |
|---|---|---|
| Browse list with name, cuisines, rating, ETA, open/closed status and delivery fee | Restaurant cards (§6.2) | AC-1 |
| Text search by restaurant name | Search on a normalised name column, debounced (§4.4, §5.5) | AC-2 |
| A single "Open now" toggle | Chip; status worked out from opening hours (§5.6) | AC-3, AC-10 |
| Details: name, cuisines, rating, ETA, fee, status, image if available | Details screen (§6.3) | AC-4 |
| Distinct loading, empty and problem states | State tables (§6.2, §6.3) | AC-5, AC-6, AC-8, AC-12 |
| Stretch: favourites from either screen, kept across restarts | `favourites` table (§5.8) | AC-11 |
| Stretch: open a restaurant from a deep link | `mrd://restaurant/{id}` (§4.5) | AC-12 |
| About 200 realistic restaurants; designed for a much larger list | 220 generated restaurants plus a 10,000-restaurant stress file; Paging 3 over Room with filtering in SQL (§5.2, §5.6) | AC-16 |
| A REST-style JSON source | Static JSON served over HTTPS (§5.1) | AC-5 |
| Offline: the last successful list is available | Room is the single source of truth (§5.3) | AC-6 |
| The UI stays responsive under load | Sync off the main thread, one-transaction snapshot swap, paging (§5.3, §6.2) | AC-16 |
| Temporary network problems: clear messages and a way to retry | Typed errors, banner, full-screen error, automatic retry on reconnect (§5.4, §6.2) | AC-5 to AC-7 |
| Task 3: SOLUTION.md on modules, boundaries, testing, smooth scrolling at scale, and AI | §10 | AC-23 |
| Task 3: one small, measurable improvement | "Open now" stays correct offline (§5.6) | AC-10, plus the measurement in §5.6 |
| Deliverables: README.md, SOLUTION.md, video | §10 | AC-23 |
| AI use: include the prompts and explain the reasoning | `AI_USAGE.md` and `docs/ai/` (§10) | AC-23 |

## 3. Key decisions

| Decision | Chosen | Also considered | Why |
|---|---|---|---|
| Data pipeline | Room as the single source of truth, Paging 3, search and filter in SQL | Room with a plain `Flow<List>`; network first with Room as a fallback | It meets "design for a much larger list" in code. Offline isn't a special code path. The network-first option has two sources of truth. |
| Data source | Static JSON in this repo, fetched over real HTTPS | An in-app fake backend (OkHttp interceptor); bundled assets | Real network behaviour: airplane mode really fails, so offline and retry are shown honestly. |
| Task 3 improvement | "Open now" stays correct offline | A Macrobenchmark of large-list scrolling; surviving restarts | Offline is one of the brief's primary focus areas, and a cached open/closed flag silently goes wrong. |
| Maps | None | A lite-mode map card on the details screen | The generated restaurants have no real location data. |
| Navigation | Navigation Compose with type-safe routes | Navigation 3 | For two screens it gives built-in deep links (with Browse beneath Details) and predictive back. Navigation 3 earns its place with an adaptive list-detail layout. |
| Serialization | kotlinx.serialization | Moshi | One compile-time, reflection-free serializer for network data and navigation routes, and easy on R8. |
| Dependency injection | Hilt | Koin; manual DI | The standard for large Android apps, with multi-module and testing support. |
| Domain layer | None | A use case per action | Not needed yet; add use cases when two features share logic. |
| minSdk | 26 | 24 with core library desugaring | `java.time` without desugaring. |

## 4. Architecture

### 4.1 Modules and ownership

```
:app                 host shell (stands in for the Mr D app)
:feature:browse      Browse screen and ViewModel
:feature:details     Details screen and ViewModel
:core:data           discovery: restaurant API, Room database, repositories, sync
:core:model          discovery: pure Kotlin models, no Android
:core:ui             discovery: restaurant UI shared by both features
:core:network        platform stand-in: shared OkHttpClient, JSON config, connectivity
:core:common         platform stand-in: dispatchers, clock, minute ticker
:core:designsystem   platform stand-in: Mr D design tokens and generic components
:core:testing        platform stand-in: test rules, fakes, test data
build-logic/         convention plugins (an included build)
```

- **Platform stand-ins** are things the Mr D app already has: a design system, a networking stack, common infrastructure and test utilities. If this feature moved into their codebase, theirs would replace these.
- **Discovery modules** would move across as they are.
- **`:app` holds no feature logic.** It contains the Hilt application class, `MainActivity`, the wiring of the navigation graph, the deep-link intent filter, and the signing and R8 configuration.
- Ownership is also written down in `.github/CODEOWNERS`, with illustrative squad names.

### 4.2 Dependency rules

- `:feature:*` may depend on `:core:data`, `:core:model`, `:core:ui`, `:core:designsystem` and `:core:common`, and on `:core:testing` for tests. A feature never depends on another feature.
- `:core:data` depends on `:core:network`, `:core:common` and `:core:model`. Retrofit, OkHttp and Room are `implementation` dependencies, so features can't import them. Repository implementations are `internal`; only their interfaces are public.
- `:core:ui` depends on `:core:model` and `:core:designsystem`.
- `:core:model` and `:core:common` have no Android dependencies.
- **The build enforces the layering.** A project-wide rule in `settings.gradle.kts` fails the build when a `:feature:*` module depends on another feature, or a `:core:*` module depends on a feature or on `:app`. It checks every module's dependencies, so a core module can't carry one feature onto another's classpath. Rules inside the code are enforced by architecture tests (§7.2).

### 4.3 How a host app adopts the feature

- **The host provides, through Hilt:**
  - an `OkHttpClient`, carrying whatever auth, logging or certificate-pinning interceptors the host uses;
  - the `Json` configuration;
  - an `@IoDispatcher` `CoroutineDispatcher`;
  - a `java.time.Clock`.

  In this repo, `:core:network` and `:core:common` provide them.
- **The features expose:**
  - the route types `BrowseRoute` and `RestaurantRoute(id)`;
  - `NavGraphBuilder.browseScreen(onRestaurantClick)`;
  - `NavGraphBuilder.restaurantScreen(onBack)`;
  - `NavController.navigateToRestaurant(id)`.

  The host connects the callbacks.
- **Adopting the feature in the Mr D app** means adding the module dependencies and calling those two functions in its navigation host. It keeps its own platform bindings.

### 4.4 State and unidirectional data flow

- **One state stream per screen.** Each ViewModel exposes a single `StateFlow` of an immutable UI state. The UI collects it with `collectAsStateWithLifecycle()`.
- **Upstream work stops in the background.** State flows are shared with `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), …)`, so the ticker and the connectivity callback stop 5 seconds after the screen leaves the foreground. They keep running through a rotation.
- **Paging.** The restaurant list is a `Flow<PagingData<RestaurantSummary>>` cached with `cachedIn(viewModelScope)`.
- **Inputs and navigation.** User input arrives as plain ViewModel functions (`onOpenNowToggled()`, `onRetry()`, `onFavouriteToggled(id)`). Navigation is a callback passed into the screen, not an event channel.
- **Search field.** It's a `TextFieldState` held in the ViewModel and copied into `SavedStateHandle`. The query sent to the database is debounced by 250 ms, so typing never lags.
- **Toggle and scroll position.** The Open now toggle is a `SavedStateHandle` flow, so the search text and the toggle both survive process death. Scroll position lives in `rememberLazyGridState()`. The grid isn't composed until the first page arrives, so a restored position isn't reset to the top.
- **Previewable screens.** Each screen is a stateless composable (`BrowseScreen(state, items, callbacks)`), wrapped by a route composable that gets the ViewModel with `hiltViewModel()`. Screens can then be previewed and tested without Hilt.

### 4.5 Navigation and deep links

- **Routes.** Navigation Compose with `@Serializable` routes: `BrowseRoute` (a data object) and `RestaurantRoute(id: String)`.
- **Deep link.** The link `mrd://restaurant/{id}` is declared with `navDeepLink<RestaurantRoute>(basePath = "mrd://restaurant")`. Its intent filter sits on `MainActivity` in `:app`, because the host owns external entry points.
- **Back behaviour.** A deep link on a cold start, or on a warm start through `onNewIntent` and `handleDeepLink`, opens Details with Browse underneath, so Back returns to the list.
- **Predictive back** is enabled (`android:enableOnBackInvokedCallback="true"`), and Navigation Compose animates it.
- **Not HTTPS App Links.** Verifying them needs a hosted `assetlinks.json` that matches the signing key, which reviewers' own debug builds wouldn't match. SOLUTION.md covers this.
- **Manual check:** `adb shell am start -W -a android.intent.action.VIEW -d "mrd://restaurant/r-1001" com.effekt3r.mrd`

## 5. Data and offline

### 5.1 API contract

`GET {API_BASE_URL}v1/restaurants.json`

- **Hosting.** The mock API is static JSON in this repository, served by `raw.githubusercontent.com`.
- **Environments.** Debug builds read it from the `dev` branch and release builds from `master`, so `dev` plays the part of staging and `master` of production. The base URL is a build config value that a Gradle property (`mrd.apiBaseUrl`) can override.
- **Versioned path.** The `v1/` path means a later schema change can't break builds already installed.

```json
{
  "restaurants": [
    {
      "id": "r-1001",
      "name": "Cape Town Grill",
      "cuisines": ["Steakhouse", "Grill"],
      "rating": 4.6,
      "delivery_fee_cents": 1500,
      "eta_minutes": 32,
      "is_open": true,
      "image_url": "https://www.themealdb.com/images/media/meals/<id>.jpg",
      "opening_hours": [
        { "day": "MONDAY", "opens": "11:00", "closes": "22:00" },
        { "day": "FRIDAY", "opens": "18:00", "closes": "02:00" }
      ]
    }
  ]
}
```

| Field | Type | When missing, null or invalid |
|---|---|---|
| `id` | string, required | The record is dropped. |
| `name` | string, required, not blank | The record is dropped. |
| `cuisines` | array of strings | Empty list. Blank entries and duplicates are removed. |
| `rating` | number from 0 to 5 | Shown as "New". A value outside 0 to 5 is treated as missing. |
| `delivery_fee_cents` | integer, 0 or more | Left out of the card; "–" on Details. A negative value is treated as missing. |
| `eta_minutes` | integer, more than 0 | No ETA pill on the card; "–" on Details. |
| `is_open` | boolean | The server's snapshot. Only used when opening hours are missing (§5.6). |
| `image_url` | HTTPS URL | A cuisine-coloured placeholder. |
| `opening_hours` | array | Missing or empty means the hours are unknown. |

**Rules for opening hours:**
- `day` is `MONDAY` to `SUNDAY`. `opens` and `closes` are `HH:mm` on a 24-hour clock, in the restaurant's local time (Africa/Johannesburg).
- `closes` may be `24:00`. A `closes` earlier than `opens` means closing on the next day. An entry whose `opens` equals its `closes` is invalid and dropped.
- A day can have several entries (split shifts). A day with no entries is a day the restaurant is closed.

### 5.2 Generated dataset

- **The generator** is a Kotlin script in `tools/datagen/` with a fixed random seed. It writes `mock-api/v1/restaurants.json` (220 restaurants) and `mock-api/v1/restaurants-10k.json` (10,000).
- **Images** come from a pool of TheMealDB photos, fetched once per cuisine and committed as `tools/datagen/image-pool.json`. Generation is therefore repeatable and needs no network.
- **Names** are fictional and Cape Town-flavoured. There are no real brands.
- **The data is deliberately messy:**
  - about 6% have no rating, 4% no fee, 4% no ETA, 6% no image and 5% no hours;
  - there are overnight hours, split lunch and dinner shifts, 24-hour places, and restaurants closed on some weekdays;
  - about 15% have free delivery;
  - one record has no id, one has a blank name, and one id appears twice.
- **The stress dataset** is selected for debug builds with `-Pmrd.dataset=10k`.

### 5.3 Sync

- **A refresh** fetches the JSON, then maps and validates it on `Dispatchers.Default`. One Room transaction then:
  - upserts the restaurants;
  - replaces their opening intervals;
  - deletes restaurants missing from the new snapshot;
  - records `sync_state.last_synced_at`.

  Favourites are never touched.
- **When a refresh runs:**
  - when Browse becomes visible and the snapshot is missing or more than 5 minutes old;
  - on pull-to-refresh and on Retry;
  - when the connection returns after a failed refresh;
  - when Details is opened for an id that isn't cached and no sync has happened yet.
- **One request at a time.** Refreshes that overlap join the one already running, so there is only one network call.
- **HTTP caching.** OkHttp has a 10 MB disk cache, and each request sends `Cache-Control: no-cache`, so the response is always revalidated against GitHub's ETag. Unchanged data comes back as a 304 of a few hundred bytes. A 304 skips the database write and only updates `last_synced_at`, unless the database is empty.
- **Timeouts** are 10 seconds to connect and 20 seconds to read, since the stress file is about 5 MB.
- **Bad data never wipes the cache.** If the payload contained records but none survived validation, the refresh fails as `BadData` and the current snapshot stays.

### 5.4 Errors

- **Error types.** `:core:data` maps every failure exactly once into a `SyncError`:
  - `Network`: no connection, DNS failure or timeout;
  - `Server(httpCode)`;
  - `BadData`: malformed JSON, or no usable records.
- **What the ViewModels see** is a `SyncStatus(lastSyncedAt, isRefreshing, lastError)`, never an exception.
- **Error copy.** The UI picks its wording using the connectivity state: "You're offline" when there's no network, and "Couldn't connect" when there is.

### 5.5 Room schema

The database is `discovery.db`, version 1. Its schema is exported to `core/data/schemas/`, so future migrations can be tested.

| Table | Columns | Notes |
|---|---|---|
| `restaurants` | `id` (primary key), `name`, `name_search`, `cuisines`, `rating`, `delivery_fee_cents`, `eta_minutes`, `is_open_snapshot`, `image_url`, `has_hours`, `position` | `name_search` is the name lowercased, with accents removed and whitespace collapsed, so "cafe" finds "Café". `cuisines` is stored as a JSON array through a type converter. `position` keeps the server's order, because ranking is a backend concern. |
| `opening_intervals` | `restaurant_id`, `start_minute`, `end_minute` | Primary key (`restaurant_id`, `start_minute`). A foreign key to `restaurants` with cascade delete. |
| `favourites` | `restaurant_id` (primary key), `created_at` | No foreign key, so a favourite survives snapshot replacement and a restaurant that temporarily disappears. |
| `sync_state` | `id` (always 0), `last_synced_at` | A single row. |

**Search.** The query is normalised the same way as `name_search`. The wildcard characters `%`, `_` and `\` are escaped, and the query is matched with `LIKE '%' || :query || '%' ESCAPE '\'`.

### 5.6 Open now stays correct offline (Task 3)

**The problem.** The API's `is_open` describes the moment the response was served. Once it's cached and the user is offline it goes wrong: a few hours later, a closed restaurant still shows "Open" and still passes the Open now filter.

**The model:**
- **Ingest.** Opening hours become week-minute intervals `[start, end)` in the restaurant's local time (Africa/Johannesburg, where there's no daylight saving). Week minute 0 is Monday 00:00 and 10,080 is the end of Sunday.
  - Spans past midnight are split.
  - Sunday-to-Monday spans wrap to the start of the week.
  - Overlapping or touching intervals are merged.
- **Status has three values:**
  - **Open** or **Closed** when the hours are known, depending on whether the current week minute falls inside an interval;
  - when the hours are unknown, the server's `is_open`, but only while the snapshot is less than 30 minutes old;
  - **Unknown** after that.
- **Open now includes only Open.** The app never claims a restaurant is open when it can't back that up.
- **Details shows the next change:** "Closes 22:00" (following intervals across midnight), "Opens 11:00", "Opens tomorrow 11:00" or "Opens Mon 11:00".
- **Status is computed in SQL.** The list status and the filter come from a single `:weekMinute` parameter plus `:snapshotFresh`, so paging and the result count stay correct at 10,000 rows. Details uses the same rules as pure Kotlin functions.
- **Keeping it current.** A minute ticker driven by `java.time.Clock` and aligned to minute boundaries runs only while a screen is collecting. On each tick, the repository invalidates the current PagingSource rather than building a new Pager. Paging's refresh key then keeps the scroll position when a status flips.

**How it's measured:**
- **Correctness.** The same boundary cases run against both the Kotlin functions and the SQL query:
  - opening time included and closing time excluded;
  - overnight hours and the Sunday-to-Monday wrap;
  - split shifts, 24-hour places and closed days;
  - missing hours with a fresh snapshot and with a stale one.
- **Impact.** A JVM test counts, across the generated dataset, how many statuses a cached `is_open` would get wrong after 1, 3, 6 and 12 hours offline, sampled across the week. Derived status gets none wrong. The numbers go into SOLUTION.md.
- **Demo.** Turn on airplane mode and set the emulator clock to one minute before a restaurant's closing time. It flips to Closed and drops out of the Open now list.

### 5.7 Connectivity and images

- **Connectivity.** The `ConnectivityManager` default-network callback is exposed as a `Flow<Boolean>` meaning "validated internet". The callback is registered only while someone collects the flow. When the connection returns after a failed refresh, the app refreshes.
- **Images.** Coil 3 uses the shared `OkHttpClient` and Coil's own disk cache, so images the user has already seen still show offline.
- **Image sizes.** The API returns one canonical image URL, and a small resolver asks for a size that suits where it's shown. TheMealDB serves `/medium` (about 33 KB) for cards and `/large` (about 58 KB) for the details header, against about 105 KB at full size. The resolver stands in for an image CDN's resize parameters.

### 5.8 Favourites

`toggleFavourite(id)` inserts or deletes a row in `favourites`. Browse and Details both observe that table, so they always agree. Favourites survive restarts and refreshes. They aren't a filter, because the brief limits filtering to search and Open now.

## 6. UI

### 6.1 Design tokens (`:core:designsystem`)

- **Colour:**
  - The values are sampled from the Play Store listing: brand sky `#6DCFF6`, ink `#1D1D1B`, white surfaces and field grey `#E7EBEE`.
  - Sky blue as text on white has a contrast of about 1.8:1, which fails accessibility. It's therefore only used as a fill behind ink text, and links use a darker blue derived from it.
  - Status colours meet WCAG AA. A dark theme is built from the same tokens.
  - Nothing outside the design system uses a raw colour value (§7.2).
- **Type:** Figtree, an open-licence geometric sans close to the lettering in the Mr D screenshots. It's bundled with the app rather than downloaded, so it works offline. Headings are extra-bold and body text is regular.
- **Shapes and spacing:** fields and image cards have 12 dp rounded corners; chips and pills are fully rounded. Spacing steps are 4, 8, 12, 16 and 24 dp.
- **Components:** search field, filter chip, ETA pill, status label, rating, favourite button, skeleton card, state screens (error and empty), and the offline banner.

### 6.2 Browse

**Layout:**
- **Pinned header:**
  - a "Restaurants" title;
  - a grey search field ("Search restaurants", with a clear button and the keyboard's search action);
  - the Open now chip, filled with ink when on and outlined when off;
  - a result count ("214 restaurants" or "86 open now") that TalkBack announces when it changes.
- **Cards, in Mr D's style:**
  - a full-width 16:9 photo with 12 dp corners, a white ETA pill bottom-left ("25 mins") and a heart in a white circle top-right;
  - below the photo: the name (one line), the cuisines ("Steakhouse · Grill", one line) and a details row ("★ 4.6 · R15 delivery · Open").
- **Card variants:**
  - Closed places have a dimmed photo and "Closed · Opens 11:00".
  - Free delivery is highlighted.
  - A missing rating shows as "New".
  - A missing photo shows a cuisine-coloured placeholder with a food icon.
- **Adaptive grid:** `GridCells.Adaptive` gives one column on phones and two or three on foldables, tablets and landscape.
- **Scrolling:** pull-to-refresh. Changing a filter scrolls to the top; ticks and refreshes never move the list.

**States:**

| State | When | What the user sees |
|---|---|---|
| First load | No snapshot yet; a refresh is running | Skeleton cards |
| First-load error | No snapshot; the refresh failed | A full-screen error with offline-specific wording when offline, and Retry |
| Content | A snapshot exists | The grid. A banner if the last refresh failed ("You're offline · Showing restaurants from 10:42 · Retry"). The pull-to-refresh indicator while refreshing. |
| No matches | A snapshot exists, but the filters match nothing | "No restaurants match 'xyz'", with Clear search and, when the toggle is on, Turn off Open now |
| Empty area | The snapshot has no restaurants | "No restaurants deliver here yet" |

### 6.3 Details

- **Header:** the photo runs edge-to-edge under the status bar, with a dark gradient behind the status icons. White circular back and favourite buttons sit over it.
- **Content:**
  - the name and cuisines;
  - a row of three stats: rating, ETA and delivery fee;
  - a status line showing the next change;
  - the week's opening hours, with today highlighted.
- **States:** content; not found (unknown id); and loading or offline error, which only happen for a deep link whose restaurant isn't cached yet.

### 6.4 System UI

- **Edge-to-edge:** `enableEdgeToEdge()`, with insets handled once per screen.
- **Insets:** the list's padding includes the navigation bar, the search field respects the keyboard, and display cutouts are handled in landscape.
- **Status-bar icons:** dark on Browse and light over the Details photo.
- **Predictive back** is on.

### 6.5 Accessibility

- **Cards.** Each card is a single TalkBack item with a full description: "Cape Town Grill. Steakhouse, Grill. Rated 4.6. 32 minutes. R15 delivery. Open, closes 22:00." It has a custom action, "Add to favourites" or "Remove from favourites".
- **Controls.** The favourite button has a role and a state description. Touch targets are at least 48 dp, and headings are marked as headings.
- **Announcements.** The result count is a polite live region.
- **Contrast and scaling.** Text contrast is at least 4.5:1, and layouts work at 200% font scale.
- **Release check.** A full TalkBack walk is part of the release gate.

### 6.6 Copy and formatting

- **Strings:** all text is in string resources (en-ZA), with plurals for counts.
- **Money:** integer cents shown as "R15" or "R15.50"; zero is "Free delivery".
- **ETA:** "25 mins", Mr D's format.
- **Rating:** one decimal place ("4.6"); no rating is "New".
- **Times:** 24-hour, as in "22:00".
- **Branding:** the app is labelled "Mr D Discover", with an original launcher icon in Mr D blue and ink. Mr D's logo isn't copied into this public repo.

## 7. Testing and verification

### 7.1 Tests

Tests run on the JVM, so CI doesn't need an emulator.

| Layer | What's tested | Tools |
|---|---|---|
| Rules in plain Kotlin | Opening hours (all §5.6 boundary cases), validation (nulls, malformed records, duplicates), money, ETA and rating formatting, search normalisation and escaping | JUnit |
| Contract | The committed `mock-api/v1/restaurants.json` decodes with the production `Json` config and mapper, with the expected counts of kept and dropped records | JUnit |
| Network | 200, 304 with ETag, server errors, timeouts and malformed JSON each map to the right `SyncError` | MockWebServer |
| Database | Paging queries (search, escaping, open now at the same boundaries as the Kotlin tests, so SQL and Kotlin provably agree); favourites surviving snapshot replacement; the single-transaction swap; the bad-data guard | Room in-memory, Robolectric, paging-testing |
| Repository | Refresh triggers, one request at a time, the freshness rule, a 304 skipping the write | Fakes, coroutine test dispatcher |
| ViewModels | Loading, content, banner, empty and error states; debounce; retry; refresh on reconnect; state restored after process death | Turbine, fakes, `SavedStateHandle` |
| Compose UI | Every screen state; the single TalkBack item and its custom action; deep link to Details and back to Browse | Compose test APIs on Robolectric, Hilt testing |
| Architecture | §7.2 | Konsist |

Screenshot tests and Macrobenchmark are left out on purpose (§12).

### 7.2 Architecture tests

These house rules fail CI instead of relying on memory. They are also what keeps AI-written code on the rails. `ArchitectureTest` in `:app` checks them with Konsist:

- no `:feature:*` code imports another feature's package;
- `:core:model` and `:core:common` import nothing from `android.*` or `androidx.*`;
- money is never a `Double` or `Float`: not as a declared type, a literal or a conversion from cents;
- no `Color(…)` values or named `Color` constants outside `:core:designsystem`;
- ViewModels expose read-only state with explicit types (`StateFlow`, never `MutableStateFlow`), internal members included;
- repository implementations are `internal`; only the fakes in `:core:testing` are public;
- `:core:model` types are immutable (no `var`, mutable collections or arrays), because the Compose stability configuration declares them stable.

Konsist reads sources straight from disk, so the test task declares every Kotlin source as a Gradle input; otherwise Gradle could skip the tests or restore a cached pass. The probe kit in `tools/architecture-probes/` proves each rule against deliberate violations, and lists what the rules don't catch.

### 7.3 Verification gate

Following the rule that a passing build is not proof:

- **Every feature** is run on the emulator before its pull request.
- **The final gate is a walk through the R8 release APK**, covering:
  - airplane mode and the offline paths;
  - process death ("Don't keep activities"), rotation and 200% font size;
  - TalkBack and dark theme;
  - the landscape grid, with both gesture and three-button navigation;
  - the deep link.
- **Evidence** (commands, results and screenshots) goes into `docs/qa/`.
- **The verifier.** An independent verifier agent (§10) writes the verdict against the acceptance criteria in §11.

## 8. Build, release and CI

### 8.1 Toolchain

- **Tools:** Android Studio 2026.2.2 or newer, and JDK 17 or newer (21 recommended), as the README lists.
- **Versions:** proven rather than newest. The toolchain is the combination Google's Now in Android builds with, libraries have been out for at least four weeks, and a spike build validated the set before the plan was written (README, version policy).
- **SDK levels:** compile and target SDK 36; minSdk 26.
- **Dependencies** are declared in a version catalogue (`gradle/libs.versions.toml`).
- **Convention plugins** in `build-logic/convention`: `mrd.android.application`, `mrd.android.library`, `mrd.android.feature` (library, Compose, Hilt, serialization and the shared feature dependencies), `mrd.android.compose`, `mrd.hilt` and `mrd.jvm.library`. `mrd.android.room` arrives with the data layer in milestone 3. The layering rule lives in `settings.gradle.kts` (§4.2).
- **Formatting:** Spotless with ktlint.
- **Compose stability:** a stability configuration file marks the `:core:model` types as stable.

### 8.2 R8

- **Release settings:** `isMinifyEnabled = true`, `isShrinkResources = true` and `proguard-android-optimize.txt`.
- **Keep rules:** the libraries' bundled rules are used. Any rule the app adds carries a comment saying why it's needed.
- **CI** fails if R8's `missing_rules.txt` isn't empty, or if R8's `mapping.txt` is missing, so a moved output can't pass unchecked. It keeps `mapping.txt` as a build artifact; in production this would be uploaded for crash de-obfuscation.
- **Device check:** the release APK is walked through on the emulator (§7.3).

### 8.3 Signing

- **Release config.** The release signing config reads `MRD_KEYSTORE_FILE`, `MRD_KEYSTORE_PASSWORD`, `MRD_KEY_ALIAS` and `MRD_KEY_PASSWORD` from Gradle properties or the environment.
- **When they're missing:**
  - A local release build falls back to the debug key and Gradle prints a warning, so reviewers can still run R8 builds.
  - A CI tag build fails instead of falling back, so a debug-signed "release" can never ship.
- **The upload keystore** is generated with keytool and kept outside the repo.
  - Its base64 copy and passwords live in GitHub Secrets on a `release` environment that only `v*` tags can use.
  - Pull requests and other branches can never read it.
- **Play App Signing** (the upload key versus the app signing key) is explained in SOLUTION.md.
- **Versioning.** The version name and code come from one place and are bumped on every release.

### 8.4 CI

- **`ci.yml`** runs on pull requests into `dev` and `master`, and on pushes to them:
  - the Spotless check and Android Lint;
  - the unit, Robolectric and architecture tests;
  - `assembleRelease` with the debug-key fallback, and the R8 missing-rules check;
  - gitleaks.

  It uploads the APKs, test reports and `mapping.txt`.
- **Hardening:**
  - The workflow token is read-only (`permissions: contents: read`). The one job that can write submits the dependency graph and runs no build code.
  - Every action is pinned to a commit SHA, and checkout uses `persist-credentials: false`.
  - Only GitHub-owned actions and `gradle/actions` may run, and the repository requires SHA pinning. That's stricter than allowing verified creators.
  - Gradle caching uses the open-source `basic` provider. Pushes to `dev` and `master` write the cache; pull requests only read it.
- **Required checks.** **Build and test** and **Secret scan** are required status checks in the `master` and `dev` rulesets.
- **`release.yml`** runs on `v*` tags. It builds the signed release APK and attaches it to a GitHub Release with `gh release create`, without a third-party release action.
- **Dependabot** sends weekly, grouped version updates for Gradle and GitHub Actions, targeting `dev`, after a cooldown: 30 days for libraries and 7 for actions.
- **Dependency submission.** `dependency-submission.yml` sends the resolved Gradle dependencies to GitHub on pushes to `master`, so Dependabot can raise vulnerability alerts; GitHub can't read a Gradle build on its own. Dependabot reads its configuration from `master`, so both start with the first release.

### 8.5 App security

- **Network:** the network security configuration turns cleartext traffic off.
- **Data:** no secrets, analytics or personal data are in the app; favourites stay on the device.
- **Release builds** are not debuggable.

## 9. Repository workflow and public-repo safety

This is set up by the repository-setup pull request (#1), and `CONTRIBUTING.md` documents it.

- **Branches:**
  - `master` only receives reviewed releases.
  - `dev` is the integration branch.
  - Work happens on short-lived `feature/`, `fix/` and `chore/` branches cut from `dev`.
  - Hotfixes go to `master`, and then `master` is merged back into `dev`.
- **Rulesets on `master` and `dev`:**
  - changes arrive only by pull request;
  - force pushes and deletion are blocked;
  - review threads must be resolved;
  - merge commits only, so each branch's commits stay visible.

  Required approvals are 0, because it's a solo project and GitHub doesn't let you approve your own pull request.
- **Release tags** (`v*`) can't be moved or deleted.
- **Settings are code.** `tools/github/apply-repo-settings.sh` and `.github/rulesets/` apply them idempotently.
- **Security controls:**
  - secret scanning with push protection;
  - Dependabot alerts, fed by dependency submission, and security-fix pull requests for direct dependencies in the version catalog;
  - private vulnerability reporting;
  - a read-only Actions token by default; workflows can't approve pull requests, and outside contributors' workflows need approval;
  - wiki and projects off;
  - a gitleaks pre-commit hook;
  - `.gitignore` entries for keystores, signing properties, `.env` files and local Claude Code state.
- **Commits** are small, one logical step each, with conventional subjects and a `Verified:` line in the body. They're authored by me with no AI trailers; AI use is disclosed in `AI_USAGE.md` and in each pull request's AI notes.
- **First release.** `dev` is released to `master` once the mock API data exists, so the release build's API URL resolves, and from then on at each milestone.

## 10. AI-assisted workflow and documentation

- **Process:** brainstorm, then spec (this document), then plan. Implementation is done by subagents with test-driven development, followed by independent verification, and every change goes through a pull request.
- **Committed so assessors can see it:**
  - `CLAUDE.md`: a short cold-start brief with the module map, commands, hard rules (each with the reason it exists), the definition of done, and "keep this file true".
  - `WORKFLOW.md`: the general way of working (one step per commit; never bypass hooks; tests with every change; the emulator gate; design tokens only; localised strings; money in integer cents).
  - `.claude/agents/verifier.md`: a QA agent instructed never to edit code; it only writes its reports under `docs/qa/`. It checks §11 with evidence and gives PASS or FAIL first; a FAIL blocks the merge.
  - `.claude/settings.json`: stops Claude Code's Read tool from opening keystores, signing properties and `.env` files, and denies force pushes and `git commit --no-verify`. The Read rule doesn't cover shell commands, so those files stay outside the repository.
  - `AI_USAGE.md`: tools, workflow, the decisions I made, and where AI helped or was wrong.
  - `docs/ai/prompts.md`: the exact prompts. A script in `tools/ai/` extracts them from the Claude Code session logs, and the raw logs are never committed.
  - `docs/ai/corrections.md`: each time the AI was wrong and how it was caught.
  - `docs/specs/`, `docs/plans/` and `docs/qa/`.
- **For assessors:**
  - `README.md`: how to run and test the app, the stress dataset, the deep link, downloading the APK, and a technology table with official documentation links.
  - `SOLUTION.md`: answers to every Task 3 question, the design and its trade-offs, what I chose not to do, and the measured results from §5.6.

## 11. Acceptance criteria

| # | Criterion |
|---|---|
| AC-1 | Browse shows a scrollable list of restaurants with name, cuisines, rating (or "New"), ETA, delivery fee (or "Free delivery") and open status. |
| AC-2 | Search filters by restaurant name only, ignoring case and accents, as you type (debounced to at most 250 ms) with no input lag. Clearing the search restores the list. |
| AC-3 | The Open now chip limits the list to restaurants whose status is Open at the current Cape Town time. Closed and Unknown restaurants are excluded. |
| AC-4 | Tapping a card opens Details with the name, cuisines, rating, ETA, delivery fee, open status with the next change, opening hours, and the photo or a placeholder. |
| AC-5 | First launch with a connection goes from skeleton cards to content. First launch offline shows the full-screen offline error with Retry, and Retry after reconnecting loads the content. |
| AC-6 | With cached data and no connection, the last successful list is shown with the offline banner (time of the last update, and Retry). Search, the toggle, Details and favourites all work offline. |
| AC-7 | When the connection returns after a failed refresh, the app refreshes on its own and the banner disappears. |
| AC-8 | When nothing matches, an empty state names the query and offers Clear search, plus Turn off Open now when the toggle is on. |
| AC-9 | Pull-to-refresh doesn't blank the list, and refreshes that overlap make a single network request. |
| AC-10 | While a screen is visible, open and closed status updates within a minute of an opening or closing time, without moving the scroll position. |
| AC-11 | Favourites can be toggled from a card and from Details, stay in sync between the two screens, and survive app restarts and data refreshes. |
| AC-12 | `mrd://restaurant/{id}` opens Details for that id, on a cold or warm start, with Browse underneath. An unknown id shows the not-found state. An id that isn't cached, while offline, shows the offline error with Retry. |
| AC-13 | The search text, the Open now toggle and the scroll position survive rotation and process death. |
| AC-14 | Malformed or partial records never crash the app. Records without an id or name are skipped, and missing values show sensible fallbacks. |
| AC-15 | A refresh whose payload had records but none usable is treated as bad data and doesn't wipe the cache. |
| AC-16 | The UI stays responsive during a 10,000-restaurant sync: the old list stays scrollable and searchable until the new snapshot is committed. |
| AC-17 | Edge-to-edge: content isn't hidden by the system bars or the keyboard, with gesture or three-button navigation, in portrait or landscape. Status-bar icons are readable on both screens. |
| AC-18 | Accessibility: each card is one TalkBack item with a favourite action, touch targets are at least 48 dp, text contrast is at least 4.5:1, and layouts work at 200% font scale. |
| AC-19 | The dark theme renders correctly on both screens. |
| AC-20 | The release build is R8-minified with resource shrinking, reports no missing keep rules, and passes the release walk (§7.3). |
| AC-21 | CI passes on `dev` and `master`: formatting, lint, unit, Robolectric and architecture tests, the release build and the secret scan. |
| AC-22 | A `v*` tag produces a signed release APK attached to a GitHub Release. |
| AC-23 | README.md explains how to run and test the app, use the stress dataset and trigger the deep link. SOLUTION.md answers every Task 3 question. AI_USAGE.md includes the prompts. |

## 12. Deliberately not done

| Not done | Why, and when it would be done |
|---|---|
| Menus on Details | The brief makes them optional. They would be a separate endpoint: Details renders straight away from the cache and the menu loads afterwards with its own state. |
| Address picker and maps | There's no location data, and the API already returns the restaurants for one delivery area. |
| Navigation 3 and a two-pane list-detail layout | Worth it once tablet layouts are a goal. The adaptive grid covers large screens for now. |
| `api`/`impl` module splits, a database per squad, dynamic feature delivery | Worth it when many squads navigate into Details (faster builds) and when migrations start to collide. SOLUTION.md covers them. |
| Server-side paging and search (`RemoteMediator`) | Needed once the list is national rather than one delivery area. A full snapshot of one area stays small. |
| Full-text search (FTS) | `LIKE` on a normalised column is fast enough for thousands of rows. FTS is the next step beyond that. |
| WorkManager background sync | Refreshing on open, plus refreshing when the connection returns, is enough for this flow. |
| Screenshot tests, Macrobenchmark, Baseline Profiles | Each costs time to set up and maintain. SOLUTION.md says when they'd earn their place. |
| Crashlytics, analytics, Braze | They need accounts and consent flows, and are out of scope for a self-contained app. |
| HTTPS App Links | Verification needs an `assetlinks.json` that matches the signing key (§4.5). |
| Languages other than English | The strings are already resources, so adding a language is a translation task. |
| Signed commits | Would be added with an SSH signing key; not needed for a solo assessment. |

## 13. Risks

| Risk | Mitigation |
|---|---|
| `raw.githubusercontent.com` could be slow or rate-limited | The app works offline from the cache, and the base URL is configuration. |
| TheMealDB images could disappear | Placeholders, and Coil's disk cache. |
| Toolchain compatibility (the Android Gradle Plugin's built-in Kotlin, KSP, Hilt) | Versions are checked together at plan time. The scaffold comes first, with CI from the start. |
| Subtleties in Paging invalidation | Repository tests with paging-testing, plus the emulator walk. |
| No physical device | The emulator is used with gesture and three-button navigation, several font scales and landscape. This limitation is stated in SOLUTION.md. |
| Scope creep | Anything beyond §1 needs a decision first. §12 records what was left out. |

## 14. References

Every link below was checked and resolves.

**Architecture and modules**
- Guide to app architecture: https://developer.android.com/topic/architecture
- Architecture recommendations: https://developer.android.com/topic/architecture/recommendations
- UI layer: https://developer.android.com/topic/architecture/ui-layer
- State holders and UI state: https://developer.android.com/topic/architecture/ui-layer/stateholders
- UI events: https://developer.android.com/topic/architecture/ui-layer/events
- Data layer: https://developer.android.com/topic/architecture/data-layer
- Build an offline-first app: https://developer.android.com/topic/architecture/data-layer/offline-first
- Guide to Android app modularization: https://developer.android.com/topic/modularization
- Common modularization patterns: https://developer.android.com/topic/modularization/patterns
- Now in Android (reference app): https://github.com/android/nowinandroid

**Build and release**
- Version catalogs: https://developer.android.com/build/migrate-to-catalogs
- Convention plugins: https://docs.gradle.org/current/userguide/sharing_build_logic_between_subprojects.html and https://github.com/android/nowinandroid/tree/main/build-logic
- KSP: https://developer.android.com/build/migrate-to-ksp
- Compose Compiler Gradle plugin: https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler
- Java 8+ APIs (`java.time` on minSdk 26): https://developer.android.com/studio/write/java8-support
- R8 app optimization: https://developer.android.com/topic/performance/app-optimization/enable-app-optimization
- Keep rules: https://developer.android.com/topic/performance/app-optimization/add-keep-rules
- Sign your app: https://developer.android.com/studio/publish/app-signing
- Play App Signing: https://support.google.com/googleplay/android-developer/answer/9842756
- Lint: https://developer.android.com/studio/write/lint
- Spotless: https://github.com/diffplug/spotless
- ktlint: https://github.com/ktlint/ktlint

**Compose and UI**
- Jetpack Compose: https://developer.android.com/develop/ui/compose/documentation
- State and Jetpack Compose: https://developer.android.com/develop/ui/compose/state
- Where to hoist state: https://developer.android.com/develop/ui/compose/state-hoisting
- Save UI state in Compose: https://developer.android.com/develop/ui/compose/state-saving
- Save UI states (process death): https://developer.android.com/topic/libraries/architecture/saving-states
- Text fields (`TextFieldState`): https://developer.android.com/develop/ui/compose/text/user-input
- Lazy lists and grids: https://developer.android.com/develop/ui/compose/lists
- Material 3 in Compose: https://developer.android.com/develop/ui/compose/designsystems/material3 and https://m3.material.io/
- Custom design systems: https://developer.android.com/develop/ui/compose/designsystems/custom
- Fonts: https://developer.android.com/develop/ui/compose/text/fonts
- Chips: https://developer.android.com/develop/ui/compose/components/chip
- Pull to refresh: https://developer.android.com/develop/ui/compose/components/pull-to-refresh
- Window insets: https://developer.android.com/develop/ui/compose/system/insets
- Edge-to-edge background: https://developer.android.com/develop/ui/views/layout/edge-to-edge
- Adaptive apps: https://developer.android.com/develop/ui/compose/layouts/adaptive/get-started-with-adaptive-apps
- Window size classes: https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes
- Compose performance: https://developer.android.com/develop/ui/compose/performance
- Performance best practices: https://developer.android.com/develop/ui/compose/performance/bestpractices
- Stability in Compose: https://developer.android.com/develop/ui/compose/performance/stability
- Fix stability issues: https://developer.android.com/develop/ui/compose/performance/stability/fix
- Accessibility in Compose: https://developer.android.com/develop/ui/compose/accessibility
- Semantics: https://developer.android.com/develop/ui/compose/accessibility/semantics
- Merging and clearing semantics: https://developer.android.com/develop/ui/compose/accessibility/merging-clearing
- Loading images: https://developer.android.com/develop/ui/compose/graphics/images/loading
- Coil: https://coil-kt.github.io/coil/

**Navigation and deep links**
- Navigation: https://developer.android.com/guide/navigation
- Type safety in Navigation Compose: https://developer.android.com/guide/navigation/design/type-safety
- Deep link to a destination: https://developer.android.com/guide/navigation/design/deep-link
- Navigation in multi-module projects: https://developer.android.com/guide/navigation/integrations/multi-module
- Predictive back: https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture
- About deep links: https://developer.android.com/training/app-links
- Create deep links: https://developer.android.com/training/app-links/create-deeplinks
- Navigation 3 (considered): https://developer.android.com/guide/navigation/navigation-3

**Lifecycle, state and dependency injection**
- ViewModel: https://developer.android.com/topic/libraries/architecture/viewmodel
- Saved State module for ViewModel: https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate
- Lifecycle in Jetpack Compose: https://developer.android.com/topic/libraries/architecture/lifecycle
- Coroutines with lifecycle-aware components: https://developer.android.com/topic/libraries/architecture/coroutines
- Hilt: https://developer.android.com/training/dependency-injection/hilt-android
- Hilt with Jetpack libraries: https://developer.android.com/training/dependency-injection/hilt-jetpack
- Hilt in multi-module apps: https://developer.android.com/training/dependency-injection/hilt-multi-module
- Hilt testing: https://developer.android.com/training/dependency-injection/hilt-testing

**Data, networking and concurrency**
- Room: https://developer.android.com/training/data-storage/room
- Room DAOs: https://developer.android.com/training/data-storage/room/accessing-data
- Room migrations: https://developer.android.com/training/data-storage/room/migrating-db-versions
- Testing Room: https://developer.android.com/training/data-storage/room/testing-db
- Paging overview: https://developer.android.com/topic/libraries/architecture/paging/v3-overview
- Load and display paged data: https://developer.android.com/topic/libraries/architecture/paging/v3-paged-data
- Page from network and database (`RemoteMediator`, considered): https://developer.android.com/topic/libraries/architecture/paging/v3-network-db
- Testing Paging: https://developer.android.com/topic/libraries/architecture/paging/test
- Kotlin coroutines on Android: https://developer.android.com/kotlin/coroutines
- Coroutines best practices: https://developer.android.com/kotlin/coroutines/coroutines-best-practices
- Testing coroutines: https://developer.android.com/kotlin/coroutines/test
- Kotlin flows: https://developer.android.com/kotlin/flow
- StateFlow and SharedFlow: https://developer.android.com/kotlin/flow/stateflow-and-sharedflow
- Testing flows: https://developer.android.com/kotlin/flow/test
- Connect to the network: https://developer.android.com/develop/connectivity/network-ops/connecting
- Read network state: https://developer.android.com/develop/connectivity/network-ops/reading-network-state
- Network security configuration: https://developer.android.com/privacy-and-security/security-config
- Retrofit (moved from Square to lysine.dev in 2026; Maven coordinates unchanged): https://lysine.dev/retrofit/
- OkHttp: https://lysine.dev/okhttp/
- kotlinx.serialization: https://kotlinlang.org/docs/serialization.html

**Testing**
- Fundamentals of testing: https://developer.android.com/training/testing/fundamentals
- Testing strategies: https://developer.android.com/training/testing/fundamentals/strategies
- Test doubles: https://developer.android.com/training/testing/fundamentals/test-doubles
- Local unit tests: https://developer.android.com/training/testing/local-tests
- Robolectric strategies: https://developer.android.com/training/testing/local-tests/robolectric and https://robolectric.org/
- Testing Compose: https://developer.android.com/develop/ui/compose/testing
- Turbine: https://github.com/cashapp/turbine
- Konsist: https://docs.konsist.lemonappdev.com/
- Developer options ("Don't keep activities"): https://developer.android.com/studio/debug/dev-options

**Resources**
- Localize your app: https://developer.android.com/guide/topics/resources/localization
- String resources and plurals: https://developer.android.com/guide/topics/resources/string-resource

**Considered for later**
- Baseline Profiles: https://developer.android.com/topic/performance/baselineprofiles/overview
- Macrobenchmark: https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview
- Task scheduling (WorkManager): https://developer.android.com/develop/background-work/background-tasks/persistent

**Repository and CI**
- Rulesets: https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/about-rulesets
- Push protection: https://docs.github.com/en/code-security/concepts/secret-security/push-protection
- Dependabot security updates: https://docs.github.com/en/code-security/concepts/supply-chain-security/dependabot-security-updates
- Secure use of GitHub Actions: https://docs.github.com/en/actions/reference/security/secure-use
- Deployment environments: https://docs.github.com/en/actions/how-tos/deploy/configure-and-manage-deployments/manage-environments
- GitHub Actions: https://docs.github.com/en/actions
- CODEOWNERS: https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-code-owners
- gitleaks: https://github.com/gitleaks/gitleaks

**Data**
- TheMealDB (food photos): https://www.themealdb.com/api.php
