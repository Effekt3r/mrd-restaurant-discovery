**PASS**

Verified branch `feature/mock-menus` at commit `2ab6a5d2eab320386321bb742978ef8bd23bdedb`, which equals `origin/feature/mock-menus`. That's 10 commits on `origin/dev` at `90111f9de945fe8f026cabc32d1dd2e3d88469ba`, which is also the merge base. Checked against `docs/plans/2026-11-19-02b-mock-menus.md` and spec §5.9.
- The working tree was clean before and after every step, apart from the deliberate, reverted edit in §5.
- Code-review probes ran only in a disposable clone of the pushed branch, and every probe showed its diff before its test run.
- `--args=fetch-images` was never run, and nothing used adb. This milestone has no UI.

## Criteria

| # | Criterion | Verdict | Evidence |
|---|---|---|---|
| 1 | **The menus.** `mock-api/v1/menus/` holds exactly 218 files, one per distinct id in `restaurants.json`. The duplicated id's menu comes from its first record. Every menu follows §5.9's schema, at `mock-api/README.md`'s rates, with each edge case at its documented index. | PASS | **Files:** 218 directory entries, hidden files included, are exactly the 218 distinct ids. None is missing or extra, and no stress-only id has a menu. **Duplicated id:** `r-1030` has index 29's (Fynbos Pizza Bar) pizza courses, not index 30's (Wynberg Grill). **Schema and rates:** every normal menu passes an independent schema check, and every rate matches the README to 0.1 point. **Edge cases:** each of the 11 has exactly its documented defect and nothing else. See §1. |
| 2 | **No collateral change.** `restaurants.json` and `restaurants-10k.json` are byte-identical to `origin/dev`'s. | PASS | `git diff --stat` prints nothing (exit 0), and the blob ids are identical in `origin/dev`, `HEAD` and the working tree. See §2. |
| 3 | **Determinism.** `./gradlew :tools:datagen:run` leaves `git status --porcelain` empty. | PASS | `git status --porcelain` printed 0 lines before and after the run, which wrote all 218 menus. A disposable clone also regenerated with a clean status, and deleted a planted stale menu. See §3. |
| 4 | **The full gate,** the R8 check, and the datagen test count. | PASS | `BUILD SUCCESSFUL`, with 522 of 522 actionable tasks executed under `--rerun-tasks`, then the plain command also passed. The R8 check exited 0. **`:tools:datagen` has 76 tests, 0 failed.** See §4. |
| 5 | **The golden test catches edits.** Without `--rerun`, an edited committed menu makes it run and fail. | PASS | `test UP-TO-DATE`, then the edit (`r-1001.json \| 2 +-`), then `> Task :tools:datagen:test FAILED` and `GoldenFilesTest > menusAreUpToDateAndNoOtherFileIsThere FAILED`. `git checkout` restored the file, and the status was empty again. See §5. |
| 6 | **The endpoints.** `r-1001.json` returns 200 with an ETag and a 304 for `If-None-Match`, and `r-1300.json` returns 404. | PASS | `HTTP/2 200`, `etag: "668292a5…"`, then `If-None-Match: HTTP 304, 0 bytes`, then `r-1300: HTTP 404`. The served body is byte-identical to the committed blob. See §6. |
| 7 | **The docs.** `mock-api/README.md`, `README.md` and `CLAUDE.md` are accurate against the data. | PASS | Every number, rule, id and edge case in the docs matches the generated files. One sentence about the golden test's reach overstates it; it's in the code review as Minor M5 and isn't about the data. See §7. |

## Evidence

### §1 The menus: PASS

**The plan's own check** (Task 5 Step 1), run verbatim from the repository root. Its output matches the plan's expected block character for character:

```
218 files, 1610783 bytes
4945 items | sold out 9.0% | no description 25.6% | photo 49.7% | options 29.5% | free options 21.0%
5 r-1006 'Rondebosch Fish Shack' r-1006 [('i-1006-101', 'Oysters', 'none'), ('i-1006-102', 'Fish Cakes', 7900)]
15 r-1016 'Oranjezicht Mezze' r-1016 [('i-1016-101', 'Baba Ganoush', -500), ('i-1016-102', 'Falafel', 9000)]
25 r-1026 'Signal Hill Burger Co.' r-1026 [('i-1026-101', None, 13000), ('i-1026-102', 'Bacon & Cheese Burger', 8500)]
35 r-1036 'Fish Hoek Osteria' r-1036 [('i-1036-101', 'Bruschetta', '8900'), ('i-1036-102', 'Arancini', 9900)]
45 r-1046 'Newlands Grill' r-1046 [('i-1046-101', 'Peri-Peri Chicken Livers', 9900), ('i-1046-102', 'Biltong Platter', 7900)]
55 r-1056 'Kommetjie Burger Bar' r-1056 [('i-1056-101', 'Classic Beef Burger', 14500), ('i-1056-102', 'Cheese Burger', 11500)]
65 r-1066 'Bree Street Flame Chicken' r-1066 [('i-1066-101', 'Quarter Chicken', 17000), ('i-1066-102', 'Half Chicken', 13500)]
75 r-1076 'Kirstenbosch Wok' r-1076 [('i-1076-101', 'Pork Dumplings', 8900), ('i-1076-102', 'Prawn Har Gow', 4900)]
85 r-1086 'Bo-Kaap Pasta Bar' r-1087 [('i-1086-101', 'Bruschetta', 11500), ('i-1086-102', 'Arancini', 10000)]
95 r-1096 'Oranjezicht Sushi Bar' r-1096 []
105 r-1106 'Green Point Peri-Peri' r-1106 [('i-1106-101', 'Quarter Chicken', 15500), ('i-1106-101', 'Half Chicken', 18500)]
```

**An independent checker** in Python, kept outside the repository. It doesn't use the generator's code: it parses `Courses.kt` (46 courses, 16 concepts) and reads `image-pool.json` and both datasets as its references. It checks every menu against them:
- **The files:** the directory listing, hidden files included, against the distinct ids, plus the line endings and UTF-8.
- **Keys:** the order of top-level, category, item and option keys, and that a missing value leaves its key out. No `null` and no `"options": []` appears in any normal menu.
- **Ids:** `c-NNNN-k`, `i-NNNN-k0j` and `o-NNNN-k0j-n`.
- **Courses and dishes:** the concept's courses in order, 4 to 8 dishes per category in course order, and each description equal to its dish's.
- **Prices:** whole rands within the course's range, with one style per menu (round fives or ending in 9). Options have 1 to 3 distinct names from the course, priced 0 to R35 in steps of R5.
- **Photos:** HTTPS; drinks have none, and desserts use the `dessert` key. One pool key holds every other dish photo in a menu together with the restaurant's own photo.
- **Edge cases:** for each one, the exact set and count of rule violations.

```
oracle: 46 courses, 16 concepts, 19 pool keys, 220 + 10000 restaurants
records without id: indexes [10]; duplicated ids: ['r-1030'] at [29, 30]
distinct ids: 218; directory entries (hidden included): 218; .json files: 218
stress-only ids with a menu: 0; r-1300 in stress file: True, in restaurants.json: False
r-1030: first record index 29 'Fynbos Pizza Bar' ['Pizza', 'Italian'], second index 30 'Wynberg Grill' ['Grill', 'South African']; menu concept pizza, categories ['Pizzas', 'Sides', 'Desserts', 'Drinks']
normal menus: 207, items: 4945 | sold out 9.0% | no description 25.6% | photo 49.7% | options 29.5% | free options 21.0% of 2889
photo rate among dishes (courses with photos): 64.7% of 3796
prices: dishes R25-R250, no-photo courses R20-R45, paid options R5-R35
all 218: 871 categories, 5191 items, 1610783 bytes; smallest r-1096 52 B, largest r-1108 11546 B; r-1001 10802 B, 1873 B gzipped
categories per normal menu: [3, 4, 5]; items per category: [4, 5, 6, 7, 8]
edge   5 r-1006 (Rondebosch Fish Shack, seafood): violations ['price-missing'] -> ok; count 1 (expected 1)
edge  15 r-1016 (Oranjezicht Mezze, middle-eastern): violations ['price-negative'] -> ok; count 1 (expected 1)
edge  25 r-1026 (Signal Hill Burger Co., burgers): violations ['item-no-name'] -> ok; count 1 (expected 1)
edge  35 r-1036 (Fish Hoek Osteria, italian): violations ['price-string'] -> ok; count 1 (expected 1)
edge  45 r-1046 (Newlands Grill, steakhouse): violations ['cat-no-name'] -> ok; count 1 (expected 1)
edge  55 r-1056 (Kommetjie Burger Bar, burgers): violations ['price-null'] -> ok; count 5 (expected 5)
edge  65 r-1066 (Bree Street Flame Chicken, chicken): violations ['option-price'] -> ok; count 1 (expected 1)
edge  75 r-1076 (Kirstenbosch Wok, chinese): violations ['image-not-https'] -> ok; count 1 (expected 1)
edge  85 r-1086 (Bo-Kaap Pasta Bar, italian): violations ['restaurant_id'] -> ok; count 1 (expected 1)
edge  95 r-1096 (Oranjezicht Sushi Bar, None): violations ['no-categories'] -> ok; count 1 (expected 1)
edge 105 r-1106 (Green Point Peri-Peri, chicken): violations ['dup-item-id', 'item-id'] -> ok; count 2 (expected 2)
r-1106 Half Chicken options: ['o-1106-102-1', 'o-1106-102-2']
NO PROBLEMS
```

Exit code 0. The checker also confirmed each edge case's specifics from the README:
- **Index 25:** the nameless `i-1026-101` carries Classic Beef Burger's description, first in course order.
- **Index 55:** every Sides item keeps a `"price_cents": null` key, rather than leaving the key out.
- **Index 65:** `o-1066-102-2` is the last of Half Chicken's 2 options, and Half Chicken is the menu's first item with options.
- **Index 75:** `i-1076-103` is the first item with a photo.
- **Index 105:** Quarter Chicken and Half Chicken both have `i-1106-101`.

The dedicated validator isn't the generator's own test suite: the code review shows that suite wouldn't notice an edge case breaking more than documented.

### §2 No collateral change: PASS

```
$ git fetch origin --quiet && git rev-parse origin/dev
90111f9de945fe8f026cabc32d1dd2e3d88469ba
$ git diff --stat origin/dev -- 'mock-api/v1/restaurants*.json'; echo "exit=$?"
exit=0
$ for f in mock-api/v1/restaurants.json mock-api/v1/restaurants-10k.json; do echo "$f dev=$(git rev-parse origin/dev:$f) head=$(git rev-parse HEAD:$f) worktree=$(git hash-object $f)"; done
mock-api/v1/restaurants.json dev=93bf9cf49eda22a849539c6fe9f0b2bfc8e20c43 head=93bf9cf49eda22a849539c6fe9f0b2bfc8e20c43 worktree=93bf9cf49eda22a849539c6fe9f0b2bfc8e20c43
mock-api/v1/restaurants-10k.json dev=4025b1ddc6f1371b264abfa41071dce2a4afa9a8 head=4025b1ddc6f1371b264abfa41071dce2a4afa9a8 worktree=4025b1ddc6f1371b264abfa41071dce2a4afa9a8
```

`GoldenFilesTest.restaurantsJsonIsUpToDate` and `restaurants10kJsonIsUpToDate` also passed in §4. The branch changes only `tools/datagen/`, `mock-api/` (the menus and the README), `.gitattributes` and docs.

### §3 Determinism: PASS

```
$ export JAVA_HOME=$(/usr/libexec/java_home -v 21)
$ git status --porcelain | wc -l
0
$ ./gradlew :tools:datagen:run --console=plain
> Task :tools:datagen:run
Wrote mock-api/v1/restaurants.json: 220 restaurants, 180857 bytes
Wrote mock-api/v1/restaurants-10k.json: 10000 restaurants, 6137980 bytes
Wrote mock-api/v1/menus/: 218 menus, 1610783 bytes
BUILD SUCCESSFUL in 1s
$ git status --porcelain | wc -l
0
```

**In a disposable clone** (a fresh checkout at a different path, with its own Gradle project directory), a planted stale file was removed and the status stayed clean:

```
$ echo '{}' > mock-api/v1/menus/r-9999.json
$ ./gradlew :tools:datagen:run -q
Wrote mock-api/v1/restaurants.json: 220 restaurants, 180857 bytes
Wrote mock-api/v1/restaurants-10k.json: 10000 restaurants, 6137980 bytes
Wrote mock-api/v1/menus/: 218 menus, 1610783 bytes
Deleted mock-api/v1/menus/r-9999.json: no restaurant has that id
$ test ! -e mock-api/v1/menus/r-9999.json && echo "stale menu deleted"; git status --porcelain | wc -l
stale menu deleted
0
```

### §4 The full gate: PASS

Forced every task to run, so no result came from the cache:

```
$ ./gradlew spotlessCheck :app:lintDebug testDebugUnitTest :app:assembleRelease --rerun-tasks --console=plain
...
> Task :tools:datagen:test
> Task :tools:datagen:testDebugUnitTest
...
BUILD SUCCESSFUL in 30s
522 actionable tasks: 522 executed
```

Exit code 0. Then the documented command exactly as written, and the R8 check:

```
$ ./gradlew spotlessCheck :app:lintDebug testDebugUnitTest :app:assembleRelease
BUILD SUCCESSFUL in 2s
518 actionable tasks: 3 executed, 515 up-to-date
$ test -s app/build/outputs/mapping/release/mapping.txt && test ! -s app/build/outputs/mapping/release/missing_rules.txt && echo "R8 ran; no missing keep rules"
R8 ran; no missing keep rules
```

`mapping.txt` is 19,493,092 bytes, and `missing_rules.txt` is absent.

**Datagen tests,** counted with the plan's command from the forced run's XML. The files are timestamped within the run.

```
ConceptsTest 7 0 0
CoursesTest 2 0 0
EdgeCasesTest 11 0 0
GeneratorTest 11 0 0
GoldenFilesTest 3 0 0
HoursTest 10 0 0
ImagePoolFetcherTest 4 0 0
ImagePoolTest 4 0 0
JsonWriterTest 6 0 0
MenuEdgeCasesTest 7 0 0
MenuGeneratorTest 7 0 0
NamesTest 4 0 0
total: 76 tests, 0 failed
```

That's 76: milestone 2's 58 plus the branch's 18 (`CoursesTest` 2, `MenuEdgeCasesTest` 7, `MenuGeneratorTest` 7, and one more each in `GeneratorTest` and `GoldenFilesTest`). `GoldenFilesTest.menusAreUpToDateAndNoOtherFileIsThere` ran in 0.084 s.

### §5 The golden test catches edits: PASS

```
$ git ls-files --error-unmatch mock-api/v1/menus/r-1001.json
mock-api/v1/menus/r-1001.json
$ ./gradlew :tools:datagen:test --console=plain        # 1. after the passing gate run
> Task :tools:datagen:test UP-TO-DATE
BUILD SUCCESSFUL in 596ms
$ perl -0pi -e 's/"price_cents": 8900/"price_cents": 8800/' mock-api/v1/menus/r-1001.json   # 2. edit one tracked menu
$ git diff --stat; git diff -U0 mock-api/v1/menus/r-1001.json | grep '^[-+] '
 mock-api/v1/menus/r-1001.json | 2 +-
 1 file changed, 1 insertion(+), 1 deletion(-)
-          "price_cents": 8900,
+          "price_cents": 8800,
$ ./gradlew :tools:datagen:test --console=plain        # 3. no --rerun
> Task :tools:datagen:test FAILED
GoldenFilesTest > menusAreUpToDateAndNoOtherFileIsThere FAILED
        Caused by: java.lang.AssertionError at GoldenFilesTest.kt:33
76 tests completed, 1 failed
BUILD FAILED in 1s
$ git checkout mock-api/v1/menus/r-1001.json           # 4. restore
Updated 1 path from the index
$ git status --porcelain | wc -l
0
$ ./gradlew :tools:datagen:test --console=plain        # 5. green again
> Task :tools:datagen:test FROM-CACHE
BUILD SUCCESSFUL in 448ms
```

The test executed rather than reporting `UP-TO-DATE` or `FROM-CACHE`, and only the menu test failed. `GoldenFilesTest.kt:33` is the byte comparison.

### §6 The endpoints: PASS

```
$ BASE=https://raw.githubusercontent.com/Effekt3r/mrd-restaurant-discovery/feature/mock-menus/mock-api/v1/menus
$ curl -sS -o r-1001.served.json -D - "$BASE/r-1001.json" | grep -iE '^(HTTP|etag|content-type|cache-control|content-length)'
HTTP/2 200
cache-control: max-age=300
content-type: text/plain; charset=utf-8
etag: "668292a57765e3417d32d88fb4a33ebf12c9c4d069dd4d679a21c80bbe721a92"
content-length: 10802
$ ETAG=$(curl -sSI "$BASE/r-1001.json" | awk 'tolower($1)=="etag:" {print $2}' | tr -d '\r')
$ curl -sS -o /dev/null -w 'If-None-Match: HTTP %{http_code}, %{size_download} bytes\n' -H "If-None-Match: $ETAG" "$BASE/r-1001.json"
If-None-Match: HTTP 304, 0 bytes
$ curl -sS -o /dev/null -w 'r-1300: HTTP %{http_code}\n' "$BASE/r-1300.json"
r-1300: HTTP 404
$ echo "served blob=$(git hash-object r-1001.served.json) committed blob=$(git rev-parse HEAD:mock-api/v1/menus/r-1001.json)"
served blob=dfdc5e52758990bee226bc0aa1a1785223bedd10 committed blob=dfdc5e52758990bee226bc0aa1a1785223bedd10
```

`r-1300` is in the stress file but not in `restaurants.json`, as §1 shows.

### §7 The docs: PASS

| Claim | Where | Against the data |
|---|---|---|
| Each restaurant has a menu at `v1/menus/{id}.json`, served like the list; `r-1300` answers 404 | `mock-api/README.md`, Endpoints | Matches §6: 200, a strong ETag, 304, `text/plain; charset=utf-8`, `max-age=300` and a 404. The example link points at `dev`, which answers 404 until this branch merges. That's expected (see "Observations outside scope"). |
| 218 files: index 10 has no id and `r-1030` appears twice, its menu from index 29, Fynbos Pizza Bar | `mock-api/README.md`, Menus | Matches §1. |
| Key order, and a missing description, photo or options leaves the key out | Schema | Matches §1: no `null` or `[]` stand-ins in normal menus. |
| Ids `c-1001-1`, `i-1001-101`, `o-1001-101-1` | Ids | Matches; the checker validated every id. |
| 3 to 5 courses, 4 to 8 dishes in menu order; bobotie, gatsbys and malva pudding; a test rejects brands in any name, description or option | Content | Matches: categories [3, 4, 5] and items [4…8], in course order. The dishes exist at `Courses.kt` lines 68, 367, 549 and 567–572, and `CoursesTest.noMenuTextContainsARealName` covers course names, options, dishes and descriptions. |
| R25 to R250, and R20 to R45 for drinks; round fives or just under, never mixed; options R5 to R35, some free | Prices | Matches: dishes R25–R250, drinks R20–R45, one style per menu, paid options R5–R35, and 21.0% free. |
| 207 menus and 4,945 items: 9.0% sold out, 25.6% without a description, 49.7% with a photo, 29.5% with options, 21.0% of 2,889 options free | Rates table | Matches exactly. Photos are on 64.7% of dishes and on no drinks. |
| About 1.6 MB in all; `r-1001.json` about 11 KB, or 2 KB gzipped | Sizes | Matches: 1,610,783 B in all; `r-1001.json` is 10,802 B, and 1,907 B with `gzip -c`. |
| The eleven menu edge cases, each with only the problem listed | Menu edge cases | Matches §1: exact violation sets and counts. |
| `:tools:datagen:run` takes about two seconds and deletes stale menus, with no network | Regenerating | Matches §3: 1 s, and `r-9999.json` deleted. |
| `mock-api/v1/menus/`: 218 menus, one per restaurant in `restaurants.json`; stress-only restaurants have none (404) | `README.md`, Mock API table | Matches. |
| `GoldenFilesTest` fails when any file in `mock-api/v1/`, menus included, differs from the generator's output | `CLAUDE.md:41` | True for every file the generator writes (§5 proves the menus). A stray extra file directly in `mock-api/v1/` passes; see Minor M5. That's about the test, not the data. |

Spec §5.9 on the branch also matches: 218 files, the first record for the duplicated id, and 404 for stress-only restaurants. `CLAUDE.md`'s "Current state" doesn't mention milestone 2b yet. The plan adds that sentence after this report (Task 6 Step 5), so it's not a defect at this point.

## Code review

Scope: `git diff origin/dev...HEAD -- tools`, which is about 1,230 lines across 11 files. I found no defect in the code's output: §1's independent checker found the data exactly as documented. The findings are tests that can't fail, and readability. Every probe below ran in a disposable clone, and each clone was restored afterwards.

### Important

**I1. No test checks that each edge-case menu has only its documented problem.** The README promises it (`mock-api/README.md:92`, "Each has only the problem listed"), but nothing enforces it:
- **The per-case tests look only at the spot they broke:** `MenuEdgeCasesTest.kt:41-79`, for example `firstItem()` for indexes 5 to 35.
- **The rules test skips edge-case menus entirely:** `onlyTheEdgeCasesBreakTheRules` returns early at `MenuEdgeCasesTest.kt:85` for every index in `MenuEdgeCases.indexes`.
- **The golden test can't help:** it compares against regenerated output, and the workflow regenerates with every generator change.

So an edge case that breaks more than documented passes every test. The probe changed `ITEM_WITHOUT_PRICE` (`MenuEdgeCases.kt:43`) to strip the price from the whole first category, then regenerated:

```
-        ITEM_WITHOUT_PRICE -> menu.editFirstItem { it.withField("price_cents", null) }
+        ITEM_WITHOUT_PRICE -> menu.editCategory(0) { c -> c.withField("items", JsonArray(c.items().map { it.withField("price_cents", null) })) }
$ ./gradlew :tools:datagen:run -q && git diff --stat -- mock-api
 mock-api/v1/menus/r-1006.json | 4 ----
$ ./gradlew :tools:datagen:test --no-build-cache --console=plain
> Task :tools:datagen:test
BUILD SUCCESSFUL in 1s
$ cat tools/datagen/build/test-results/test/*.xml | ...   # the plan's count command
total: 76 tests, 0 failed
$ python3 -c '...'   # count r-1006's first-category items without price_cents
c-1006-1 Starters items without price_cents: 5 of 5
```

The documented case was one item dropped. The mutated data would make the app hide the whole Starters category, a different spec §5.9 path. Milestone 3 builds its own fixtures, but milestone 6 shows these menus as Details' deliberate problem states.

Suggested fix: for each edge index, undo the one documented defect, then run the same rules as `onlyTheEdgeCasesBreakTheRules`. Or assert the exact number of rule violations per menu, as §1's checker does.

### Minor

**M1. A test assertion can never fail.** At `GeneratorTest.kt:51`, `assertEquals(standard, generated.map { it.record })` compares `Generator.restaurants(...)` (`GeneratorTest.kt:19`) with `generate(...).map { it.record }`. But `restaurants()` is defined as exactly that expression (`Generator.kt:37`), so the assertion compares the code with itself. The same goes for the KDoc's "Generating them draws nothing more than [restaurants]" (`Generator.kt:39`). Only the golden test guards that the restaurants didn't change.

The probe added one extra `random.nextInt()` per restaurant (`Generator.kt`, after `val random = Random(SEED * 31 + index)`):

```
$ ./gradlew :tools:datagen:test --no-build-cache --tests '*GeneratorTest.eachRestaurantComesWithTheConceptAndPhotoKeyItWasDrawnFrom'
> Task :tools:datagen:test
BUILD SUCCESSFUL in 3s          (XML: tests="1" skipped="0" failures="0" errors="0")
$ ./gradlew :tools:datagen:test --no-build-cache --tests '*GoldenFilesTest.restaurantsJsonIsUpToDate'
> Task :tools:datagen:test FAILED
GoldenFilesTest > restaurantsJsonIsUpToDate FAILED
$ ./gradlew :tools:datagen:run -q && git diff --stat -- mock-api/v1/restaurants.json mock-api/v1/restaurants-10k.json
 mock-api/v1/restaurants-10k.json |    2 +-
 mock-api/v1/restaurants.json     | 5343 +++++++++++++++++++-------------------
```

Every restaurant changed and the suspect test still passed. The test's other assertions do bite: the concept and photo key must be consistent with the record. Suggested fix: drop line 51 and point the KDoc at `GoldenFilesTest`.

**M2. The "generate twice" test can't catch run-to-run nondeterminism.** `MenuGeneratorTest.generatingTwiceGivesTheSameMenus` (`MenuGeneratorTest.kt:44-47`) compares two calls in one JVM. Anything constant within a process but different between runs gets through: a time-based seed, or iteration over identity-hashed sets. The probe salted the menu seed once per JVM (`MenuGenerator.kt:25` and `:73`):

```
+    private val RUN_SALT = System.nanoTime() // probe: differs per JVM, constant within one
-        val random = Random((Generator.SEED * 31 + index) xor STREAM)
+        val random = Random(((Generator.SEED * 31 + index) xor STREAM) + RUN_SALT)
$ ./gradlew :tools:datagen:test --no-build-cache --tests '*MenuGeneratorTest.generatingTwiceGivesTheSameMenus'
BUILD SUCCESSFUL in 1s          (XML: tests="1" skipped="0" failures="0" errors="0")
$ ./gradlew :tools:datagen:run -q   # twice, hashing mock-api/v1/menus/*.json after each
run 1: menus digest c3719fb09c0baf7f, r-1001 price lines 64
run 2: menus digest 053f98755bed07e4, r-1001 price lines 40
```

The golden test is the real determinism guard, and criterion 3 confirms it. This test only catches per-call randomness. The pre-existing `GeneratorTest.generatingTwiceGivesTheSameRestaurants` (`GeneratorTest.kt:38-40`) has the same shape, outside this diff. Suggested fix: rename it to say what it proves, or drop it.

**M3. Readability in `MenuEdgeCases.kt`.**
- **Mangled formatting:** lines 85-93 split `if (i == position) edit(it) else it` across nine lines (`if (i ==` / `position` / `) {`). It's copied from the plan as formatted. `categories.mapIndexed { i, it -> if (i == position) edit(it) else it }` on one line, or a `when`, reads at a glance.
- **A side effect hidden in a chain:** line 110, `item.also { edited = true }.let(edit)`, buries a state change inside `map`. `if (!edited && where(item)) { edited = true; edit(item) } else item` says the same thing plainly.

**M4. Two functions are public but only used inside the object.** `MenuGenerator.menu(...)` (`MenuGenerator.kt:72`) and `MenuGenerator.price(...)` (`MenuGenerator.kt:125`) are called by nothing outside `MenuGenerator`: `grep` finds no test call either. They could be `private`, matching `optionPrice`. The impact is small inside an `internal object`.

**M5. `CLAUDE.md:41` overstates the golden test's reach.** It says `GoldenFilesTest` "fails when any file in `mock-api/v1/`, menus included, differs from the generator's output". Only `menus/` gets a stray-file check (`GoldenFilesTest.kt:26-39`). Directly in `mock-api/v1/`, the test checks just the two dataset paths (`GoldenFilesTest.kt:41-50`). The probe:

```
$ echo '{"restaurants": []}' > mock-api/v1/restaurants-old.json
$ ./gradlew :tools:datagen:test --no-build-cache --console=plain
> Task :tools:datagen:test
BUILD SUCCESSFUL in 2s
```

Either word the rule as "every file the generator writes", or list `mock-api/v1/` as well.

### Determinism risks checked: none found in the new code

- **Hash order:**
  - every map that's iterated preserves insertion order: `linkedMapOf` (`MenuGenerator.kt:41`), `associate`, `buildJsonObject`, and `toMutableMap()` in `withField`;
  - `setOf` is used only for membership;
  - `File.listFiles()` only feeds deletion, and its result is sorted (`MenuGenerator.kt:62-64`).
- **Locale:** no `format`, `lowercase` or `uppercase` in the new code. Numbers are written by `JsonPrimitive.toString()`.
- **Charset:**
  - `writeText`, `toByteArray`, `readText` and `URL.readText` use Kotlin's UTF-8 default, not the JVM's. That matters because `-Dfile.encoding=UTF-8` in `gradle.properties` reaches only the daemon, not the forked `run` and `test` JVMs.
  - Non-ASCII menu text, such as "Waffles & Crêpes" and "jalapeños", is written as UTF-8. §1 decoded every file.
  - `.gitattributes` has `* text=auto eol=lf`, and `git check-attr` reports `eol: lf` for the menus, so Windows checkouts keep the golden bytes.
- **Random streams:** each menu has its own `Random`, seeded from the seed and the index. `shuffled(random)` and `IntRange.random(random)` are Kotlin's own algorithms. The Kotlin-version caveat is documented in `mock-api/README.md`.
- **Across checkouts:** regenerating in a disposable clone at another path gave the same bytes (§3).

## Observations outside scope

These don't affect the verdict.
- **The `dev` link 404s until merge.** `mock-api/README.md`'s example link, `…/dev/mock-api/v1/menus/r-1001.json`, answers `HTTP 404` today (`curl` shows `dev r-1001: HTTP 404`). It works once this branch merges into `dev`.
- **One edge case leaves option ids pointing at the old item id.** In `r-1106`, Half Chicken's id becomes `i-1106-101`, but its options keep `o-1106-102-1` and `o-1106-102-2`. That's harmless: spec §5.9 doesn't tie option ids to item ids, and the item is dropped as a duplicate. But fixtures built from the README's id pattern ("option `o-1001-101-1`") shouldn't assume the prefix always matches.
- **The sold-out rate is 9.0% against 8% drawn,** about 2.6 standard errors over 4,945 items. That's chance under a fixed seed, within the test's 1.5-point tolerance, and documented as measured.
- **Commits follow the plan's rules:** all 10 are authored by Franco Landman, and none has a `Co-Authored-By` or `Claude-Session` trailer (`git log --format=%B origin/dev..HEAD | grep -ciE 'co-authored-by|claude-session'` gives `0`).

## Working tree

After the report was written, `git status --porcelain` showed only `?? docs/qa/2026-11-19-m2b-mock-menus.md`. Nothing was committed.
