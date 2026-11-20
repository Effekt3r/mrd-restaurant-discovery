**PASS**

Verified branch `feature/restaurant-data` at commit `b67bb51d654bce134088f298866143fb9f33e6dd` (`git rev-parse HEAD` equals `git rev-parse origin/feature/restaurant-data`, so it's pushed). `git log origin/dev..HEAD` shows 14 commits: the plan, then the implementation. I checked against `docs/plans/2026-11-19-03-data-layer.md` and spec §4.2 and §5.1 to §5.9. The milestone has no UI, so there's no emulator walk. Every edit, mutation and probe test ran in a disposable clone of `b67bb51`, never in this repository. This report is also the milestone's senior code review: see "Code review".

All seven criteria pass. The review found 3 Important and 8 Minor issues. None breaks a criterion, but I-1, I-2 and I-3, and the one-line comment fix M-1, should be fixed before milestone 5 builds on this layer.

## Criteria

| # | Criterion | Verdict | Evidence |
|---|---|---|---|
| 1 | The full gate and the R8 check pass; test counts per module; APK size; `ArchitectureTest` green | PASS | §1. `BUILD SUCCESSFUL` with all 671 tasks executed (`--rerun-tasks`); R8 check exit 0 with no `missing_rules.txt`; 109 tests, 0 failed (`:app` 7, `:core:model` 6, `:core:common` 2, `:core:data` 36, `:tools:datagen` 58); APK 1,047,540 bytes; `ArchitectureTest` 7 of 7. |
| 2 | Parsing and validation (§5.1, AC-14, AC-15): every milestone 2 edge case is handled as spec §5.1 says; a payload with records but none usable is `BadData` and keeps the cache | PASS | §2. All 17 edge cases in `mock-api/README.md` map to assertions over the committed `restaurants.json`. `recordsThatAllFailValidationAreBadDataAndKeepTheSnapshot` covers AC-15. Mutations M10, M13, M16 and M17 each fail the tests that guard them. |
| 3 | Open status (§5.6, AC-3, AC-10): Kotlin and SQL agree on the boundary cases; a tick reloads the list in place; freshness is computed in SQL; the agreement test fails when the SQL is broken | PASS | §3. All 20 cases agree. Four SQL breaks (M1 to M4) and one Kotlin break (M5) each fail `OpenStatusAgreementTest`, which names the cases. Without the invalidation, the tick test fails (M6). Freshness is read from `sync_state` inside the query, and Room observes that table. |
| 4 | Sync (§5.3, AC-9): a 304 skips the write; single flight; timeouts; `max-age=0` explained in the spec; one-transaction swap; favourites survive | PASS | §4. The 304 test fails with `no-cache` (M7). The overlap test fails without single flight (M8). Timeouts are 10 s and 20 s (`NetworkModule.kt:41-42`). Spec §5.3 explains `max-age=0`, which OkHttp 5.3.2's source confirms. The generated code runs the swap in one transaction, and a rollback probe proves it. The favourites test passes. Review findings I-2 and I-3 concern this area but don't break the criterion. |
| 5 | Menus (§5.9, AC-24's data side): 200, 404 means not available and deletes the cache, `BadData`, a cached menu offline, the validation rules | PASS | §5. Six repository tests and six parser tests cover each point and §5.9's table. M14 and M15 each fail the right test. I checked §5.9 as milestone 2b merged it into `dev`; this branch's spec doesn't contain it (§7). |
| 6 | The staleness numbers agree in the test and the plan: 8.8%, 26.1%, 44.5% and about 78% | PASS | §6. The test, the plan's table, the commit message and the PR draft all agree. An independent Python recomputation from the committed files gives the same restaurants with hours (205 of 217; 9,481 of 9,997) and the same percentages (77.9% and 77.4% at 12 h). |
| 7 | Docs: the spec, README, CLAUDE.md and the corrections log are accurate against the code | PASS | §7. Every Task 12 edit matches the code. The BuildConfig values and the flag validation work as the README says. Both corrections-log citations check out against OkHttp 5.3.2. One caveat: on this branch alone, spec §5.5 cites §5.9, which isn't in the file, and §12 still lists menus as not done. `dev` has had §5.9 since milestone 2b merged today, and a trial merge gives a consistent spec. One stale code comment is review M-1. |

## Evidence

### §1 The full gate — PASS

```
$ export JAVA_HOME=$(/usr/libexec/java_home -v 21)
$ ./gradlew spotlessCheck :app:lintDebug testDebugUnitTest :app:assembleRelease
BUILD SUCCESSFUL in 2s
675 actionable tasks: 3 executed, 2 from cache, 670 up-to-date
```

Almost everything came from the build cache, which doesn't prove the tests ran in this session, so I deleted the mapping output and forced every task to run:

```
$ rm -rf app/build/outputs/mapping
$ ./gradlew spotlessCheck :app:lintDebug testDebugUnitTest :app:assembleRelease --rerun-tasks
BUILD SUCCESSFUL in 36s
671 actionable tasks: 671 executed
(exit 0)
$ test -s app/build/outputs/mapping/release/mapping.txt && test ! -s app/build/outputs/mapping/release/missing_rules.txt; echo "R8 check exit=$?"
R8 check exit=0
```

- **R8.** `mapping.txt` is 19,541,540 bytes, and R8 wrote no `missing_rules.txt`.
- **APK.** `app/build/outputs/apk/release/app-release.apk` is **1,047,540 bytes**, exactly the plan's figure (979,364 on `dev`, plus 68,176).
- **Warnings.** The log has no compiler warnings: `grep -n "^w: \|warning:"` finds nothing.
- **Lint.** `:app:lintDebug` analysed `:app`, `:core:data`, `:core:network`, `:core:designsystem`, `:core:ui`, `:feature:browse` and `:feature:details`. Its SARIF has one result, the informational `MissingApplicationIcon` in `app/src/main/AndroidManifest.xml`, a file this branch doesn't touch.
- **Working tree.** `git status --porcelain` was empty after the build, so the Room plugin re-exported the committed schema `1.json` unchanged.

Tests, counted from each module's JUnit XML (every file timestamped by this run):

| Module | Tests | Failed |
|---|---|---|
| `:app` (`ArchitectureTest`, 7 rules) | 7 | 0 |
| `:core:model` (`OpeningHoursTest`) | 6 | 0 |
| `:core:common` (`ClockMinuteTickerTest`) | 2 | 0 |
| `:core:data` | 36 | 0 |
| `:tools:datagen` | 58 | 0 |
| **Total** | **109** | **0** |

`:core:data`'s 36 tests: `RestaurantParserTest` 6, `MenuParserTest` 6, `OpenStatusAgreementTest` 1 (20 cases), `RestaurantSyncTest` 8, `OfflineFirstRestaurantRepositoryTest` 7, `OfflineFirstMenuRepositoryTest` 6 and `StalenessMeasurementTest` 2. `:core:network`, `:core:testing`, `:core:ui`, `:core:designsystem` and the features have no tests on this branch. That's 44 new tests and 109 in all, as the plan says.

### §2 Parsing and validation — PASS

`RestaurantParserTest` reads the committed `mock-api/v1/restaurants.json` with the production `RestaurantsPayloadReader`. `core/data/build.gradle.kts:59-63` declares both datasets as test inputs. Each edge case in `mock-api/README.md`, with the assertion that proves it:

| Index | Edge case | Spec §5.1's handling | Proof (`RestaurantParserTest.kt`) |
|---|---|---|---|
| 10 | No `id` | Dropped | :106, De Waterkant Pizza Kitchen is absent; :103-104, 220 records, 217 kept |
| 20 | Blank name | Dropped | :107, `r-1021` is absent |
| 30 | Repeats index 29's id | One kept, the first | :105, `committed("r-1030")` uses `single` and is Fynbos Pizza Bar |
| 40 | `rating` 5.4 | Missing ("New") | :112 |
| 50 | `rating` `null` | Missing | :113 |
| 60 | `rating` sent as the string `"4.5"` | Missing; the record is kept | :114, `single` also proves the record is kept; `JsonFields.kt:18-19` rejects strings |
| 70 | `delivery_fee_cents` -500 | Missing | :115 |
| 80 | `eta_minutes` 0 | Missing | :116 |
| 90 | `image_url` `http://` | Placeholder | :117 |
| 100 | `opening_hours` `[]` | Hours unknown | :118 |
| 110 | An entry opens and closes at 15:00 | That entry is dropped | :125. `DailyWindow`'s `require` also rules out equal times. |
| 120 | Day `FUNDAY` | That entry is invalid | :126, Tuesday's hours survive |
| 130 | Closes at `25:00` | That entry is invalid | :127-128, Saturday 10:30 is closed and 12:00 is open |
| 140 | Blank and repeated cuisines | Removed | :119, `[Pizza, Italian]` |
| 150 | No `is_open` and no hours | Unknown | :120-121; the agreement case "no hours and no is_open" gives `Unknown` |
| 160 | `%` in the name | Search escapes it | :122; `OfflineFirstRestaurantRepositoryTest.kt:111`, `search("%%")` finds nothing |
| 170 | An accent in the name | "cafe" finds it | :123, `searchKey` gives `cafe fynbos`; `OfflineFirstRestaurantRepositoryTest.kt:107`, `search("CAFE")` |

- **Fixtures.** `wrongTypedValuesReadAsMissing` and `recordsWithoutAnIdOrNameAreDroppedAndARepeatedIdKeepsTheFirst` cover the same rules on hand-written payloads, including a non-object record and `null`.
- **Malformed JSON.** `malformedJsonOrAMissingArrayIsBadData` covers a truncated payload, a missing array and an array that's an object.
- **AC-15.** `RestaurantSyncTest.recordsThatAllFailValidationAreBadDataAndKeepTheSnapshot` (:120) asserts `BadData` and that the seeded `r-1` stays. M10 removes the check, and that test fails with `expected:<BadData> but was:<null>`.
- **The tests can fail.** M13 accepts a rating sent as a string and fails 2 tests (`expected null, but was:<Rating(tenths=45)>`). M16 keeps a repeated id and fails 2 (`expected:<[First]> but was:<[First, Second]>`; `expected:<217> but was:<218>`). M17 keeps blank names and fails 3.

### §3 Open status — PASS

**Agreement.** `OpenStatusAgreementTest` runs the 20 cases in `OpenStatusCases.kt` through `openStatus()` in Kotlin, the real paging query and the real Open now count. Each result is compared with a hand-written expected status, not with the other side, so the test doesn't check the code against itself. The cases cover:
- the opening minute included and the closing minute excluded;
- overnight hours and the Sunday-to-Monday wrap;
- split shifts, 24 hours and a closed day;
- touching windows;
- missing hours with a snapshot 29 minutes old, 30 minutes old (Unknown), 6 hours old, from the future, and with no `is_open`.

The task asked me to break the SQL in a disposable clone and show the test fails. Five breaks, one edit each, all reverted (see "Mutation runs"):

| Edit | Result |
|---|---|
| M1: `c.end_minute > :weekMinute` becomes `>=`, so the closing minute counts as open | FAIL: "closing time is excluded", "overnight, at closing" and "Sunday into Monday, at closing", each `SQL gives Open(...)` plus `Open now keeps it` |
| M2: freshness `BETWEEN 0 AND 1799999` becomes `1800000` | FAIL: `no hours, snapshot 30 minutes old: SQL gives Open(closesAt=null), expected Unknown` |
| M3: Open now becomes `$STATUS <> 'CLOSED'`, which admits Unknown | FAIL: four `Open now keeps it` lines for the Unknown cases |
| M4: the Sunday carry-over looks up `w.start_minute = 1` instead of `0` | FAIL: `Sunday into Monday, before midnight: SQL gives Open(closesAt=WeekMinute(value=0)), expected Open(closesAt=WeekMinute(value=60))` |
| M5 (Kotlin side): `isSnapshotFresh` uses `<=` | FAIL: `no hours, snapshot 30 minutes old: Kotlin gives Open(closesAt=null), expected Unknown` |

**A tick reloads the list in place.** `OfflineFirstRestaurantRepository.kt:43-50` builds one `Pager` over an `InvalidatingPagingSourceFactory`, and each tick after the first calls `sources.invalidate()`. Paging's refresh key then keeps the position. `aTickReloadsTheListInPlaceWithTheNewStatus` (:76) shows that within one collection the list doesn't change without a tick, and drops `r-1` after one. M6 removes the invalidation, and the test fails with `expected:<[]> but was:<[r-1]>`.

**Freshness in SQL.** `RestaurantDao.kt:23-25` computes `(:now - (SELECT last_synced_at FROM sync_state WHERE id = 0)) BETWEEN 0 AND 1799999`. In the generated `RestaurantDao_Impl`, the paging source observes `favourites`, `sync_state`, `opening_intervals` and `restaurants`, and the count observes `restaurants`, `opening_intervals` and `sync_state`, so a sync re-runs both. `theCountFollowsTheClock` and `detailsFollowTheClockAndHaveTheHours` cover the count and Details.

### §4 Sync — PASS

- **A 304 skips the write.** `a304SkipsTheDatabaseWriteAndOnlyRecordsTheTime` (`RestaurantSyncTest.kt:84`) asserts that the second request carries `If-None-Match: "v1"`, that the seeded `r-local` survives (no write), and that `last_synced_at` moved to the clock. M7 sends `no-cache` and the test fails with `expected:<"v1"> but was:<null>`.
- **Single flight.** `RestaurantSync.kt:56-61`: under a `Mutex`, a caller joins the active `Deferred` or starts one in the `@ApplicationScope`, so a caller that leaves doesn't cancel the others. `overlappingRefreshesMakeOneRequest` (:140) launches 3 refreshes against a response delayed 300 ms. M8 removes the join and the test fails with `expected:<1> but was:<3>`.
- **Timeouts.** `NetworkModule.kt:41-42` sets `connectTimeout(10, TimeUnit.SECONDS)` and `readTimeout(20, TimeUnit.SECONDS)`, as §5.3 specifies. `aTimeoutIsANetworkErrorAndKeepsTheSnapshot` proves a read timeout maps to `SyncError.Network` and keeps the snapshot. No test pins the production values (review M-8).
- **`max-age=0`.** Both `DiscoveryApi` methods send `Cache-Control: max-age=0` (`DiscoveryApi.kt:15,19`). Spec §5.3, line 209, now says so and why: "`no-cache` would make OkHttp skip its cache and download the whole file every time". OkHttp 5.3.2's source agrees:
  - `CacheStrategy.kt:158`, at tag `parent-5.3.2`, is `if (requestCaching.noCache || hasConditions(request)) { return CacheStrategy(request, null) }`;
  - `Cache.kt:94-95` says "If it is only necessary to force a cached response to be validated by the server, use the more efficient `max-age=0` directive instead".
- **One transaction.**
  - `replaceSnapshot` (`RestaurantDao.kt:126-136`) is `@Transaction`, and the generated `RestaurantDao_Impl.replaceSnapshot` is `performInTransactionSuspending(__db) { super@RestaurantDao_Impl.replaceSnapshot(...) }`.
  - The generated database runs `PRAGMA foreign_keys = ON`, so deleting the restaurants cascades to their intervals.
  - Probe P1 (below) proves the rollback: a swap that hits a foreign-key violation after its delete leaves `[r-1]` and `last_synced_at = 1`.
  - Without `@Transaction` (M9), P1 fails, but all 36 committed tests still pass (review I-3).
- **Favourites survive.** `favouritesSurviveASnapshotSwap` (`OfflineFirstRestaurantRepositoryTest.kt:144`) toggles `r-1`, swaps in a snapshot without it, then one with it, and the favourite is still set. Then it toggles it off. `FavouriteEntity` has no foreign key (`Entities.kt:44-49`).

### §5 Menus — PASS

Spec §5.9 and AC-24 aren't in this branch's spec. They came with milestone 2b (`feature/mock-menus`), which merged into `dev` as #16 during this verification. I checked the code against §5.9 as it stands there.

| §5.9 rule | Code | Test |
|---|---|---|
| 200: the menu is shown and cached | `OfflineFirstMenuRepository.kt:60-68` | `a200ShowsTheMenuAndCachesIt` (`Loading`, then `Content` with `fetchedAt`; path `/v1/menus/r-1.json`) |
| 404: not available, and the cached menu is deleted | `:51`, `:73-76` | `a404IsNotAvailableAndDropsTheCachedMenu`; M14 skips the delete and fails it (`expected null, but was:<MenuEntity(...)>`) |
| A menu with no categories: not available | `:62` | `anEmptyMenuIsNotAvailable` |
| `BadData`: another restaurant's menu | `MenuParser.kt:18` | `badDataIsAnErrorWithoutACachedMenu` |
| A menu seen before shows offline | `:35-41` | `aMenuSeenBeforeShowsOfflineWithTheError` (the cached `Content`, then `Error(Network, cached)`); M15 emits `Loading` first and fails it |
| A server error keeps the cached menu | `:52` | `aServerErrorKeepsTheCachedMenu` |
| Categories without an id or name, or without valid items, are hidden; repeated ids: the first wins | `MenuParser.kt:22-31` | `categoriesWithoutANameOrValidItemsAreHidden` |
| Items without an id or name, or whose price isn't an integer of 0 or more, are dropped; the first of two equal ids wins | `:37-49`, `:60` | `itemsThatCantBeOrderedAreDropped` |
| `available` missing or wrong-typed means available; blank description and `http://` images are absent; bad options are dropped; a 0 price is kept | `:44-47`, `:51-58` | `missingOrBadOptionalValuesReadAsAbsent`, `aValidMenuKeepsEveryFieldAndTheApiOrder` |
| Items that all fail, or no `categories` key: `BadData` | `:19`, `:32-33` | `anotherRestaurantsMenuOrItemsThatAllFailAreBadData` |
| Requests revalidate with the ETag | `DiscoveryApi.kt:19` | Same header as the list (§4) |

### §6 The staleness measurement — PASS

| Source | `restaurants.json` | `restaurants-10k.json` |
|---|---|---|
| `StalenessMeasurementTest.kt:25,29` (passes) | `205 with hours; wrong after 1h 8.8%, 3h 26.1%, 6h 44.5%, 12h 77.9%` | `9481 with hours; wrong after 1h 8.8%, 3h 26.1%, 6h 44.5%, 12h 77.4%` |
| Plan, Spike results table (lines 62-63) | 205 of 217; 8.8%, 26.1%, 44.5%, 77.9% | 9,481 of 9,997; 8.8%, 26.1%, 44.5%, 77.4% |
| Commit `b316c7c` and the PR draft (plan lines 3798, 3972) | 8.8%, 26.1%, 44.5%, "about 78%" | the same, "in both datasets" |

I recomputed these from the committed files with a separate Python script. It doesn't reuse the Kotlin code: it re-implements spec §5.1's id, name and duplicate rules, the `HH:mm` and `24:00` window rules, and refreshes at every quarter hour of the week. Output:

```
restaurants.json: 220 records, 217 kept, 205 with hours; wrong after 1h 8.8% (12080/137760), 3h 26.1% (35932/137760), 6h 44.5% (61240/137760), 12h 77.9% (107316/137760)
restaurants-10k.json: 10000 records, 9997 kept, 9481 with hours; wrong after 1h 8.8% (560296/6371232), 3h 26.1% (1662904/6371232), 6h 44.5% (2836600/6371232), 12h 77.4% (4932652/6371232)
```

"Derived status gets none wrong" is true by construction: the hours are both the truth and the input. The test's KDoc says so. The PR draft's "wrong for … of restaurants" should say "of statuses, among restaurants with hours" (review M-8).

### §7 The docs — PASS

- **Spec.** Each Task 12 edit matches the code:
  - §4.3: the host provides a `NetworkMonitor` and an `@ApplicationScope` scope, as `CommonModule` and `NetworkModule` show.
  - §5.3: the payload is streamed per record on the IO dispatcher (`RestaurantSync.kt:99`); the swap deletes and inserts in one transaction; `max-age=0` and why.
  - §5.5: `rating_tenths` and the `menus` row match the generated `CREATE TABLE` statements.
  - §5.6: `:weekMinute` and `:now`, with freshness from `sync_state`.
  - §8.1: `mrd.android.room` exists.
- **The plan's stale-claims grep** finds only the explanation, as Task 12 Step 3 expects:
  ```
  $ grep -rn 'no-cache\|snapshotFresh\|from milestone 3\|will switch a debug\|arrives with the data layer' README.md CLAUDE.md mock-api/README.md docs/specs
  docs/specs/2026-11-18-restaurant-discovery-design.md:209:- **HTTP caching.** … `Cache-Control: max-age=0` … (`no-cache` would make OkHttp skip its cache and download the whole file every time.) …
  ```
  The same grep over `core/` finds `DiscoveryApi.kt:11`, a code comment that still says `no-cache` revalidates (review M-1).
- **README and `mock-api/README.md`.** The generated BuildConfig matches what they say:
  - debug: `API_BASE_URL = ".../mrd-restaurant-discovery/dev/mock-api/"`;
  - release: `".../master/mock-api/"`;
  - both: `RESTAURANTS_PATH = "v1/restaurants.json"`.

  The flags fail as documented:
  - `-Pmrd.apiBaseUrl=http://example.com/` fails with "mrd.apiBaseUrl must be an https:// URL ending in /, not 'http://example.com/'.";
  - `-Pmrd.dataset=5k` fails with "mrd.dataset can only be 10k, not '5k'."
- **CLAUDE.md.** The module table matches the modules and plugins. "Current state" still describes milestone 2, which Task 13 Step 4 updates after this report.
- **`docs/ai/corrections.md`.** Both entries are accurate:
  - the OkHttp citations, as in §4;
  - "fails again if the header goes back to `no-cache`", which M7 shows;
  - `checkDependencies = true` is set in `AndroidApplicationConventionPlugin.kt:20`, and `:app:lintDebug` now analyses `:core:data` and `:core:network` (§1).
- **Caveat: menus in the spec.** On this branch alone:
  - spec §5.5 cites §5.9, which isn't in the file;
  - §12 still lists "Menus on Details" as deliberately not done;
  - §2 and §11 lack the menu row and AC-24.

  The plan assigns §5.9 to milestone 2b ("Milestones 2b and 4 run in parallel … don't touch those"), and `dev` now has it. A trial merge of the current `dev` gives a consistent spec: §5.9 present, one `menus` row, `max-age=0` kept, AC-24 present, and the §12 row gone. See "Observations outside the milestone's scope".

### Mutation runs

In a disposable clone of `b67bb51`, each run made one exact text replacement, ran `./gradlew :core:data:testDebugUnitTest --tests '<class>' --no-build-cache`, read the JUnit XML, and reverted with `git checkout -- .`. A baseline run of the whole suite plus the probes gave 39 tests, 0 failed.

| # | Edit (`core/…`) | Ran | Result | First failure |
|---|---|---|---|---|
| M1 | `RestaurantDao.kt:19` `c.end_minute > :weekMinute` → `>=` | Agreement | **Caught** | 3 named closing-minute cases |
| M2 | `RestaurantDao.kt:24` `1799999` → `1800000` | Agreement | **Caught** | snapshot 30 minutes old |
| M3 | `RestaurantDao.kt:55` `= 'OPEN'` → `<> 'CLOSED'` | Agreement | **Caught** | 4 Unknown cases kept by Open now |
| M4 | `RestaurantDao.kt:48` `w.start_minute = 0` → `= 1` | Agreement | **Caught** | Sunday into Monday, before midnight |
| M5 | `OpeningHours.kt:139` `<` → `<=` | Agreement | **Caught** | Kotlin side, 30 minutes old |
| M6 | `OfflineFirstRestaurantRepository.kt:49` tick does nothing | Repository | **Caught** | `aTickReloads…`: `expected:<[]> but was:<[r-1]>` |
| M7 | `DiscoveryApi.kt:15` `max-age=0` → `no-cache` | Sync | **Caught** | `a304Skips…`: `expected:<"v1"> but was:<null>` |
| M8 | `RestaurantSync.kt:58` no join | Sync | **Caught** | `overlapping…`: `expected:<1> but was:<3>` |
| M9 | `RestaurantDao.kt:126` remove `@Transaction` | All 39 | **Missed by the 36 committed tests**; only probe P1 fails | P1: `expected:<[r-1]> but was:<[r-2]>` |
| M10 | `RestaurantSync.kt:101` BadData check disabled | Sync | **Caught** | `expected:<BadData> but was:<null>` |
| M11 | `RestaurantSync.kt:95` drop `&& dao.hasRestaurants()` | All 39 | **Missed**: 39 of 39 pass | (review M-3) |
| M12 | `Search.kt:16` drop the `_` escape | Repository | **Missed**: 7 of 7 pass | (review M-2) |
| M13 | `JsonFields.kt:19` accept numbers sent as strings | Parser | **Caught** | 2 tests |
| M14 | `OfflineFirstMenuRepository.kt:74` skip the delete on 404 | Menu repository | **Caught** | `a404…`: the cached row is still there |
| M15 | `OfflineFirstMenuRepository.kt:41` always emit `Loading` first | Menu repository | **Caught** | `aMenuSeenBefore…` |
| M16 | `RestaurantParser.kt:74` keep repeated ids | Parser | **Caught** | 2 tests |
| M17 | `RestaurantParser.kt:91` `text("name")` → `string("name")` | Parser | **Caught** | 3 tests |

### Probe tests (in the clone only, never committed)

Four Robolectric tests, built from the module's own helpers (`inMemoryDatabase()`, `testApi()`, `ok()`, `payload()`, `TestClock`). The application scope is `CoroutineScope(SupervisorJob() + Dispatchers.Default)`, as in production. All four pass on `b67bb51`. P1 passing proves a property; P2 to P4 passing proves a defect.

- **P1, the swap is atomic.** Seed `r-1` (`last_synced_at = 1`), then call `replaceSnapshot([r-2], [OpeningIntervalEntity("r-missing", 0, 10)], 2)`. It throws a foreign-key error after the delete. Asserts: failure, stored ids `[r-1]`, `last_synced_at` 1. Without `@Transaction` (M9) it fails.
- **P2, a 304 after `BadData` reports success** (review I-2).
  - Seed `r-1`, then a 200 with ETag `"v2"` whose records all fail: `lastError == BadData`, which is correct.
  - Advance the clock 10 minutes and refresh: the request carries `If-None-Match: "v2"` and the server answers 304.
  - Asserts that pass: `lastError == null`, `last_synced_at == now`, stored ids still `[r-1]`.
- **P3, a database failure escapes `refresh()`** (review I-1). A delegating `RestaurantDao` whose `replaceSnapshot` throws `SQLiteFullException("database or disk is full")`, plus a 200. Asserts that pass: `refresh()` throws `SQLiteFullException`, then `state.isRefreshing == true` and `state.lastError == null`.
- **P4, a cached menu that doesn't decode poisons the menu** (review M-4). Insert a `menus` row `{"categories": [{"id": "c-1"}]}` and enqueue a 200. Asserts that pass: `menu(r-1).toList()` throws `SerializationException`, and `server.requestCount == 0`.

### R8 with a caller (milestone 5's risk)

Nothing calls the data layer yet, so R8 removes it from this APK. To check what milestone 5's release walk will exercise, I added a temporary caller to the clone's `MrdApplication`. It injects both repositories and calls every public method behind `if (System.nanoTime() < 0)`, which R8 can't prove false. Then:

```
$ ./gradlew :app:assembleRelease --no-build-cache
BUILD SUCCESSFUL in 28s
$ test -s app/build/outputs/mapping/release/mapping.txt && test ! -s app/build/outputs/mapping/release/missing_rules.txt; echo "R8 check exit=$?"
R8 check exit=0
```

- **APK size.** 1,326,068 bytes, 278,528 more than without the caller; the plan says "about 278 KB more".
- **Mapping.**
  - `DiscoveryApi -> dr`, `retrofit2.http.GET -> p10`, `Headers -> s30` and `Path -> pv0`;
  - `kotlin.coroutines.Continuation -> om`, `retrofit2.Response -> j21` and `okhttp3.ResponseBody -> m21`;
  - `DiscoveryDatabase_Impl` keeps its name, which Room looks up by reflection;
  - `CachedMenu -> ld` can be renamed, because its serializer is called directly.
- **`dexdump -a`** (build-tools 37.0.0) shows that what Retrofit reads at run time survived:

```
Annotations on method #3386 'a'
  VISIBILITY_SYSTEM Ldalvik/annotation/Signature; value={ "(" "Ljava/lang/String;" "Lom<" "-" "Lj21<" "Lm21;" ">;>;)" "Ljava/lang/Object;" }
  VISIBILITY_RUNTIME Lp10; value="v1/menus/{id}.json"
  VISIBILITY_RUNTIME Ls30; value={ "Cache-Control: max-age=0" }
Annotations on method #3387 'b'
  VISIBILITY_SYSTEM Ldalvik/annotation/Signature; value={ "(" "Lom<" "-" "Lj21<" "Lm21;" ">;>;)" "Ljava/lang/Object;" }
  VISIBILITY_RUNTIME Lp10; value="v1/restaurants.json"
  VISIBILITY_RUNTIME Ls30; value={ "Cache-Control: max-age=0" }
Annotations on method #3386 'a' parameters
#0
  VISIBILITY_RUNTIME Lpv0; value="id"
```

Retrofit 3.0.0's bundled rules keep these: `-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation`, `… class retrofit2.Response`, and `-keepattributes Signature, RuntimeVisibleAnnotations, …`. I found no R8 or serialization risk. A device run in milestone 5's release walk is still the proof.

## Code review

Scope: `git diff origin/dev...HEAD -- core app build-logic gradle`, 55 files and 3,038 added lines, read in full, plus the generated Room code. "Before milestone 5" means fix it before Browse builds on this layer.

### Critical

None.

### Important

**I-1. A database failure escapes `refresh()` and leaves `isRefreshing` true for good.** Fix before milestone 5.
- **Where.** `core/data/src/main/kotlin/com/effekt3r/mrd/core/data/sync/RestaurantSync.kt:69-80`. `run()` catches only `IOException` and `IllegalArgumentException`, and resets the state only after the `try`.
- **What it breaks.**
  - The public contract, `RestaurantRepository.kt:30`: "Failures arrive through [syncStatus], not as exceptions."
  - Spec §5.4, line 219: ViewModels see "a `SyncStatus` …, never an exception."
- **Evidence.** Probe P3: an `SQLiteFullException` from the swap reaches every joined caller of `refresh()`, and afterwards `isRefreshing == true` and `lastError == null`.
  - Milestone 5's pull-to-refresh spinner would never stop.
  - The reconnect retry (`OfflineFirstRestaurantRepository.kt:71`) never fires, because `lastError` is null.
  - An unhandled exception in `viewModelScope` crashes the app.
- **Smallest fix.** Reset the state in a `finally`, and map any other non-cancellation exception to a `SyncError`: a new `SyncError.Storage`, or `BadData` with a comment. Commit P3 as the test.

**I-2. A 304 proves the HTTP cache is current, not the database.** Fix before milestone 5.
- **Where.** `RestaurantSync.kt:95-98`.
- **Evidence.** Probe P2: after a `BadData` refresh, OkHttp holds the bad body and its ETag. The next refresh gets a 304 and:
  - records `last_synced_at = now`;
  - clears `lastError`;
  - keeps the old snapshot.
- **Other routes to the same state.** It also happens when the process dies after OkHttp has committed a 200 to its cache but before `replaceSnapshot` commits, and when the write throws (I-1). The device then keeps an old snapshot that looks fresh until the server's ETag changes.
- **Why it matters for Task 3.** "Fresh" also makes the old `is_open` count again, for 30 minutes, for restaurants without hours (§5.6). That weakens Task 3's claim that the app never claims a restaurant is open without backing.
- **Smallest fix.** Store the ETag of the body that produced the snapshot: a nullable `etag` column on `sync_state`, written by `replaceSnapshot`. Take the 304 shortcut only when the response's `ETag` equals it; otherwise parse the cached body OkHttp returned with the 304. Do it now: `discovery.db` is version 1 and unreleased, so the column needs no migration.

**I-3. No committed test can fail if the snapshot swap stops being one transaction.** Fix before milestone 5, whose AC-16 check depends on it.
- **Where.** `RestaurantDao.kt:126-136`.
- **Evidence.** M9 removes `@Transaction`, and all 36 committed tests pass. Only probe P1 fails (`expected:<[r-1]> but was:<[r-2]>`). The guarantee holds today, as the generated code and P1 show, but nothing protects it, and AC-16 and spec §5.3 rest on it.
- **Smallest fix.** Commit P1 to `RestaurantSyncTest` or a DAO test. It needs only `inMemoryDatabase()` and an interval whose restaurant isn't in the snapshot.

### Minor

**M-1. A comment says the opposite of the code at the point of the milestone's subtlest fix.** Fix now; it's one line.
- **Where.** `core/data/src/main/kotlin/com/effekt3r/mrd/core/data/network/DiscoveryApi.kt:11-12`: "`no-cache` makes OkHttp revalidate its cached copy with the ETag on every call". It sits directly above `@Headers("Cache-Control: max-age=0")`.
- **Why it matters.** It states the misconception `docs/ai/corrections.md` records as fixed, in the one place a maintainer reads before touching the header. Task 12's grep only covered docs. The 304 test would catch a regression (M7), but a hiring panel reads this file.
- **Fix.** "`max-age=0` makes OkHttp revalidate its cached copy with the ETag on every call; `no-cache` would bypass the cache (spec §5.3)."

**M-2. Two of the three LIKE escapes aren't tested.**
- **Where.** `searchIgnoresCaseAndAccentsAndMatchesWildcardsLiterally` (`OfflineFirstRestaurantRepositoryTest.kt:98-113`).
- **Evidence.** No seeded name has a character other than `_` between `t` and `b`, so `search("t_b")` can't tell an escaped `_` from a wildcard. M12 drops the escape and all 7 tests pass. No query contains `\`. `%` is covered, by `search("%%")`.
- **Fix.** Also seed "Plant Based", which an unescaped `t_b` would match, and a name with a backslash.

**M-3. The 304 branch for an empty database isn't tested.**
- **Where.** `RestaurantSync.kt:95` ("unless the database is empty", spec §5.3).
- **Evidence.** M11 removes `&& dao.hasRestaurants()`, and 39 of 39 tests pass.
- **Fix.** Refresh with a 200, call `dao.deleteRestaurants()`, refresh with a 304, and assert the cached body was written.

**M-4. A cached menu that no longer decodes breaks that restaurant's menu for good.** Fix before milestone 6 changes `CachedMenu`.
- **Where.** `OfflineFirstMenuRepository.kt:35-40` decodes `menu_json` before fetching, with no guard.
- **Evidence.** Probe P4: `menu()` throws `SerializationException` and makes no request, so the bad row is never replaced. The stored JSON has no version, so adding a `CachedMenu` field without a default would cause this on every upgraded device.
- **Fix.** Decode with `runCatching` and treat a failure as no cache, optionally deleting the row.

**M-5. The minute ticker in `restaurants()` keeps running in the background once milestone 5 applies `cachedIn`.** Decide in milestone 5.
- **Where.** `OfflineFirstRestaurantRepository.kt:41-52`. Spec §4.4 prescribes `cachedIn(viewModelScope)`, and Paging's `CachedPagingData.kt` shares upstream with `SharingStarted.Lazily`.
- **Effect.** The `channelFlow` and its `ticker.ticks()` collector run until the ViewModel is cleared, so a PagingSource is invalidated every minute while the app is in the background. No load runs until the UI collects again, so the cost is small. But spec §5.6 says the ticker "runs only while a screen is collecting".
- **Fix.** Gate the ticks on the screen collecting, for example ticks the ViewModel shares `WhileSubscribed`, passed in. Or reword §5.6.

**M-6. A platform stand-in now depends on a discovery module.**
- **Where.** `core/testing/build.gradle.kts:10` has `api(projects.core.data)`, so it can host `FakeRestaurantRepository` and `FakeMenuRepository`.
- **Why it matters.** Spec §4.1 (line 77) calls `:core:testing` a platform stand-in that Mr D's own module would replace, and the discovery fakes would go with it.
- **Fix.** Move the two fakes to a discovery-owned test module, as Now in Android's `:core:data-test` does.

**M-7. Readability nits a panel may raise.**
- `RestaurantSync.kt:69` names the refresh body `run()`. At its call site, `scope.async { run() }` (:58), it reads like the standard library's `run {}`. A name like `fetchAndStore()` reads better.
- `RestaurantSync.kt:82-85`: a `//` comment, then `@OptIn`, then the KDoc. The KDoc should come before the annotation.
- `FakeRestaurantRepository.kt:17` and `:24`: a public property `restaurants` and the override `restaurants(query, openNow)` share a name. `allRestaurants` would read better in ViewModel tests.
- `RestaurantDao.kt:27-34` and `Mappers.kt:41-45`: the strings `'OPEN'`, `'CLOSED'` and `'UNKNOWN'` are duplicated, and the mapper's `else -> Unknown` would silently absorb a typo. Constants that both use would remove the duplication.

**M-8. Small gaps in tests and wording.**
- No test pins the production timeouts (`NetworkModule.kt:41-42`). The timeout test builds its own 200 ms client.
- The PR draft (plan line 3972) says a cached `is_open` "is wrong for … of restaurants". It should say "of statuses, among restaurants with hours", and present "derived status gets none wrong" as true by construction (§6), when SOLUTION.md quotes it.

### What holds up

- **Boundaries (§4.2).**
  - `:core:data` exposes only `RestaurantRepository`, `MenuRepository` and paging-common. Retrofit, OkHttp (through `implementation(projects.core.network)`) and Room are `implementation` dependencies.
  - Every implementation is `internal`: the repositories, `RestaurantSync`, `DiscoveryApi`, the DAOs, the entities and the Hilt modules. `ArchitectureTest` passes.
  - `:core:model` and `:core:common` import nothing from Android.
- **Hilt scoping.**
  - The database, `DiscoveryApi`, `RestaurantSync`, `OkHttpClient`, `Json`, `Clock` and the application scope are singletons.
  - The repositories are unscoped but stateless, and share the singleton `RestaurantSync`, so single flight holds across instances.
  - `Lazy<OkHttpClient>` keeps disk-cache setup off the main thread.
- **Threading and cancellation.**
  - Parsing runs on the IO dispatcher, mapping on `Default` in the application scope, and Room on its own executors. Menus use `flowOn(io)`.
  - No `catch` swallows `CancellationException`.
  - A leaving caller doesn't cancel a shared refresh.
- **SQL.**
  - Every value is a bound parameter.
  - The LIKE escape order is right: backslash first.
  - The containing-interval join and the next-change subqueries use the `(restaurant_id, start_minute)` primary key, `ORDER BY position` has its index, and the freshness subquery is uncorrelated.
- **Time zones.** Week minutes are always computed in `Africa/Johannesburg` from an `Instant` and passed to SQL as a parameter. There are no SQLite date functions and no dependence on the device's zone, and `weekMinutesAreCapeTownTime` pins the conversion.
- **Money.** `Long` cents, ratings in tenths from 0 to 50, and a bounded ETA. No floating point reaches money, and `ArchitectureTest` checks that.
- **Single flight.** It's race-free under the `Mutex`.
- **The API surface.** It covers milestones 5 and 6:
  - a paged list and a count by query and Open now;
  - sync status, `refresh` and `refreshIfStale`;
  - `toggleFavourite`;
  - Details with hours, and `WeekMinute` next changes for "Opens Mon 11:00";
  - `MenuState`, with `fetchedAt` for "Menu from 10:42".
- **Connectivity.** `ConnectivityManagerNetworkMonitor` is correct for a default-network callback: Android's "Read network state" guide says a replaced default gets `onAvailable` for the new network, and `onLost` means no default remains.

## Observations outside the milestone's scope

These don't affect the verdict.

- **`dev` moved during this verification.** Milestone 2b (#16) and milestone 4 (#17) merged; `origin/dev` is now `5e4d718`, and `origin/feature/mock-menus` is gone. The merge base is still `90111f9`, so the reviewed diff is unchanged.
- **The PR will conflict.** In the clone, `git merge --no-commit --no-ff` of the current `dev` into `b67bb51` reports one conflict: `docs/ai/corrections.md`, where both sides added entries at the top. Everything else merges on its own, including the spec, README, CLAUDE.md and `mock-api/README.md`. Neither dataset, the version catalog, `app/build.gradle.kts`, `settings.gradle.kts` nor `app/src/test` changed in `dev`, so the pinned numbers stand.
- **The merged tree passes the gate.** I kept both sides of the conflict and ran the full gate with `--no-build-cache`: `BUILD SUCCESSFUL in 40s`, R8 check exit 0, and 132 tests with 0 failed (`:app` 7, `:core:model` 6, `:core:common` 2, `:core:data` 36, `:core:designsystem` 5, `:tools:datagen` 76). The APK is 1,474,092 bytes, including milestone 4. Merge `dev` into the branch, resolve that one file and re-run the gate before opening the PR (plan Task 13 Step 6).
- **One informational lint result.** `MissingApplicationIcon` comes from the app manifest and is untouched here. Milestone 4 adds launcher icons on `dev`.
- **CLAUDE.md's "Current state"** is updated after this report, by plan Task 13 Step 4.

## Working tree

I wrote only this report and committed nothing. Every mutation, probe and the temporary R8 caller lived in the disposable clone, which I reset to `b67bb51` afterwards. `git status --porcelain` in this worktree shows only this report:

```
?? docs/qa/2026-11-19-m3-data-layer.md
```
