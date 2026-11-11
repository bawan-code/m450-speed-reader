# Mutation Testing (PIT)

Part 3 of LB1: proof that the unit tests of Part 1 and Part 2 are complete.
Target: **no surviving mutants (mutation score 100 %)**.

## 1. What PIT does

PIT changes the compiled code of our service classes in small ways. Every changed version is a **mutant**.
For each mutant PIT runs our unit tests:

| Status | Meaning |
|---|---|
| **Killed** | at least one test fails → the tests noticed the bug (good) |
| **Survived** | all tests still pass → the tests have a gap (bad) |
| **No coverage** | no test executes this line at all |

Mutation score = killed mutants / all mutants.
Unlike line coverage, it shows whether the tests actually **check** the result, not only whether a line was executed.

## 2. How to run it

From `backend/` (`.\mvnw.cmd` on Windows, `./mvnw` elsewhere):

```bash
./mvnw test
./mvnw org.pitest:pitest-maven:mutationCoverage
```

Report: `backend/target/pit-reports/index.html` (HTML + `mutations.xml`, not timestamped, always the same path).

In CI (`.github/workflows/ci.yml`) PIT runs on every push and pull request after the unit tests.
The report is uploaded as the artifact **`pit-report`**: *Actions → run → Summary → Artifacts*.

## 3. Configuration (`backend/pom.xml`)

| Setting | Value | Why |
|---|---|---|
| PIT version | 1.23.0 + `pitest-junit5-plugin` 1.2.3 | JUnit 5 support |
| `targetClasses` | `ReadingPlanCalculator`, `ReadingSessionService`, `ReadingStatisticsService` | only the tested units; entities, Lombok code, config and the main class would produce mutants no unit test can sensibly kill |
| `targetTests` | `bbzbl_it_m450.speedreader.service.*Test` | our unit tests |
| `mutators` | `DEFAULTS` | PIT's standard set (see §4) |
| `outputFormats` | HTML, XML | HTML for the demo, XML for evaluation |
| `mutationThreshold` | 100 *(enabled at the end of week 2)* | the build fails if a single mutant survives |

## 4. Mutators with examples from our code

PIT 1.23 `DEFAULTS` produced the following mutators.

### 4.1 `ReadingSessionService` (Part 1)

| Mutator | What it changes | Example from our code | Killed by |
|---|---|---|---|
| `CONDITIONALS_BOUNDARY` | `<` ↔ `<=`, `>` ↔ `>=` | `request.wpm() < MIN_WPM` → `request.wpm() <= MIN_WPM` | `acceptsWpmAtBoundaries` (wpm 60 must be accepted) |
| | | `request.wordsRead() > text.getWordCount()` → `>=` | `wordsReadEqualToWordCount` (100 of 100 words is accepted) |
| `REMOVE_CONDITIONALS_ORDER_ELSE` | replaces a `<` / `>` comparison with `false`, so the `if` never runs | `if (request.wordsRead() < 0)` → `if (false)` | `rejectsNegativeWordsRead` (-1 must throw) |
| `REMOVE_CONDITIONALS_EQUAL_ELSE` | replaces a `==` / `!=` check with `false` | `if (wordsRead == 0) return;` → never returns early | `noBookmarkWhenNothingRead` (`verifyNoInteractions(bookmarks)`) |
| | | `if (!sessions.existsById(id))` → never throws | `deleteSession.throwsWhenMissing` |
| `MATH` | `+` ↔ `-`, `*` ↔ `/` | `getWordPosition() + wordsRead` → `getWordPosition() - wordsRead` | `reachesWordCountExactly` (50 + 50 must be exactly 100) |
| `VOID_METHOD_CALLS` | removes a call to a `void` method | removes `sessions.deleteById(sessionId)` | `deletesExistingSession` (`verify(sessions).deleteById(5L)`) |
| | | removes `validate(request)` | the validation tests (invalid input must throw) |
| | | removes `updateBookmark(...)` / `setWordPosition(...)` | bookmark tests (`ArgumentCaptor` on `bookmarks.save`) |
| `NULL_RETURNS` | returns `null` instead of an object | `return saved;` → `return null;` | `returnsSavedInstance` (`assertSame`) |
| | | lambda in `orElseThrow(...)` returns `null` instead of the exception | `unknownUser`, `unknownText`, `getSession.throwsWhenMissing` (`assertThrows`) |
| `EMPTY_RETURNS` | returns an empty value (`List.of()`, `""`, `Optional.empty()`) | `getSessionsForUser` returns `Collections.emptyList()` | `returnsSessions` (`assertSame` on the non-empty list) |

Result: **31 mutants, 31 killed, 100 %** (line coverage 58/58).

### 4.2 `ReadingPlanCalculator` (Part 2, A)

*To be filled in from A's PIT report (A4: list of survivors and the test that killed each).*

| Mutator | Example from our code | Killed by |
|---|---|---|
| `CONDITIONALS_BOUNDARY` | | |
| `MATH` | | |
| `INCREMENTS` | | |
| `PRIMITIVE_RETURNS` | | |
| `TRUE_RETURNS` / `FALSE_RETURNS` | | |
| `EMPTY_RETURNS` | | |

### 4.3 `ReadingStatisticsService` (Part 4, C – bonus)

*To be filled in once C's TDD branch is merged.*

## 5. Results

| Class | Owner | Mutants | Killed | Score |
|---|---|---|---|---|
| `ReadingSessionService` | B | 31 | 31 | **100 %** |
| `ReadingPlanCalculator` | A | – | – | – |
| `ReadingStatisticsService` | C | – | – | – |

*Screenshot of the final report with all classes at 100 % goes here (after B6).*

## 6. Lessons learned

- **Assert exact values.** `assertNotNull` or `size() > 0` let `MATH` and return-value mutants survive.
- **Test both sides of every boundary** (e.g. wpm 59 rejected *and* 60 accepted), otherwise `CONDITIONALS_BOUNDARY` survives.
- **Verify side effects.** Without `verify(...)` / `verifyNoInteractions(...)`, `VOID_METHOD_CALLS` on `save` / `deleteById` survive.
- **Inject a `Clock`** instead of `LocalDateTime.now()`, so dates are deterministic.
- **Avoid equivalent mutants** (a mutant that behaves exactly like the original and can never be killed): no redundant or unreachable conditions, use `Math.min` / `Math.max` for clamping.
- **No `@SpringBootTest`**: PIT runs every test many times, a Spring context would be slow and needs a database.
