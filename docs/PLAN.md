# Work Plan – M450 LB1 Unit-Testing (Speed Reader Backend)

Two people (**A** and **B**) build and test the Speed Reader backend in parallel.
Deadline: oral demo ("Fachgespräch") at the end of **week 3** of the quarter. Both must be able to explain **every** part.

| # | Criterion | Weight | Owner |
|---|-----------|--------|-------|
| 1 | Unit test with mocking – `ReadingSessionService` | 10 | B |
| 2 | Unit test for logic – `ReadingPlanCalculator` | 10 | A |
| 3 | Mutation testing (PIT, 100 % for #1 and #2, HTML report, explain mutators) | 10 | A (setup, CI, report, mutator doc) + each owner kills the mutants of their unit |
| 4 | TDD – `ReadingStatisticsService` (Red → Green → Refactor commits) | 10 | B |
| 5 | Automation – GitHub Actions on every push | 5 | A |

---

## 0. Things to fix in the current repo first

The repo state today (`backend/`) does not match the stack described in the assignment:

| Topic | Current repo | Assignment / plan | Action (Phase 0) |
|---|---|---|---|
| Spring Boot | `4.1.2` | Spring Boot 3 | Recommended: switch parent to latest **3.5.x** |
| Java | `25` | Java 21 | Recommended: set `<java.version>21</java.version>` |
| Test starters | `spring-boot-starter-*-test` (Boot 4 names) | JUnit 5 + Mockito | Replace with `spring-boot-starter-test` (contains JUnit 5, Mockito, AssertJ) |
| `SpeedreaderApplicationTests` | still exists with `@SpringBootTest` | no Spring context tests | **Delete** the file |
| PIT `targetClasses` | `bbzbl_it_m450.speedreader.*` (everything, incl. entities) | only the tested units | Limit to the 3 service classes (see §1.3) |
| CI | no `.github/` folder | GitHub Actions | Create workflow (§1.4) |

Why downgrade: Spring Boot 4 ships JUnit 6, and `pitest-junit5-plugin` + Mockito are best tested against JUnit 5 / Java 21. If you decide to stay on Boot 4 / Java 25, the Phase 0 "PIT smoke test" (step 0.6) **must** pass before anyone continues; otherwise downgrade.

---

## 1. Project structure overview

### 1.1 Packages and classes

```
backend/src/main/java/bbzbl_it_m450/speedreader/
├── SpeedreaderApplication.java
├── common/
│   ├── WpmLimits.java                  // MIN_WPM = 60, MAX_WPM = 1500, DEFAULT_WPM = 250   (Phase 0)
│   └── ResourceNotFoundException.java  // RuntimeException                                  (Phase 0)
├── model/                              // JPA entities                                      (Phase 0)
│   ├── User.java
│   ├── Text.java
│   ├── ReadingSession.java
│   └── Bookmark.java
├── repository/                         // Spring Data interfaces                             (Phase 0)
│   ├── UserRepository.java
│   ├── TextRepository.java
│   ├── ReadingSessionRepository.java
│   └── BookmarkRepository.java
└── service/
    ├── ReadingPlanCalculator.java      // Part 2 – pure logic                                (A)
    ├── ReadingSessionService.java      // Part 1 – uses repositories                         (B)
    ├── ReadingStatisticsService.java   // Part 4 – TDD, does NOT exist before the Red commit (B)
    └── dto/
        ├── WordTiming.java             // record                                             (A)
        ├── ReadingPlan.java            // record                                             (A)
        └── RecordSessionRequest.java   // record                                             (B)

backend/src/test/java/bbzbl_it_m450/speedreader/service/
├── ReadingPlanCalculatorTest.java      (A)
├── ReadingSessionServiceTest.java      (B)
└── ReadingStatisticsServiceTest.java   (B)
```

No controllers are needed for LB1. If a REST layer for the Angular frontend is added later, it is **not** in PIT's `targetClasses`.

### 1.2 Entities (Phase 0)

Lombok (`@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`) is fine for entities.

| Entity | Fields |
|---|---|
| `User` | `Long id`, `String username`, `String email` |
| `Text` | `Long id`, `String title`, `@Lob String content`, `int wordCount` |
| `ReadingSession` | `Long id`, `@ManyToOne User user`, `@ManyToOne Text text`, `int wpm`, `int wordsRead`, `int durationSeconds`, `LocalDateTime date`, `Integer comprehensionScore` (nullable, 0–100) |
| `Bookmark` | `Long id`, `@ManyToOne User user`, `@ManyToOne Text text`, `int wordPosition`; unique constraint on (user, text) |

### 1.3 Repositories (Phase 0)

```java
public interface UserRepository extends JpaRepository<User, Long> {}
public interface TextRepository extends JpaRepository<Text, Long> {}

public interface ReadingSessionRepository extends JpaRepository<ReadingSession, Long> {
    List<ReadingSession> findByUserIdOrderByDateDesc(Long userId);
    List<ReadingSession> findTop5ByUserIdOrderByDateDesc(Long userId);   // used by TDD (B)
    List<ReadingSession> findByUserId(Long userId);                       // used by TDD (B)
}

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    Optional<Bookmark> findByUserIdAndTextId(Long userId, Long textId);
}
```

Repositories are only ever **mocked** in LB1. No `application.properties` datasource is needed because no Spring context is started in tests.

### 1.4 pom.xml plugins and CI

`pom.xml` (Phase 0 sets it up, A refines it later):

```xml
<plugin>
  <groupId>org.pitest</groupId>
  <artifactId>pitest-maven</artifactId>
  <version>${pitest.version}</version>
  <dependencies>
    <dependency>
      <groupId>org.pitest</groupId>
      <artifactId>pitest-junit5-plugin</artifactId>
      <version>${pitest-junit5.version}</version>
    </dependency>
  </dependencies>
  <configuration>
    <targetClasses>
      <param>bbzbl_it_m450.speedreader.service.ReadingPlanCalculator</param>
      <param>bbzbl_it_m450.speedreader.service.ReadingSessionService</param>
      <param>bbzbl_it_m450.speedreader.service.ReadingStatisticsService</param>
    </targetClasses>
    <targetTests>
      <param>bbzbl_it_m450.speedreader.service.*Test</param>
    </targetTests>
    <mutators><mutator>DEFAULTS</mutator></mutators>
    <outputFormats><param>HTML</param><param>XML</param></outputFormats>
    <timestampedReports>false</timestampedReports>
    <!-- enabled by A at the end of week 2, once both units are at 100 % -->
    <!-- <mutationThreshold>100</mutationThreshold> -->
  </configuration>
</plugin>
```

Report location: `backend/target/pit-reports/index.html`.

CI workflow `.github/workflows/ci.yml` (Phase 0 = minimal version, A extends it):

```yaml
name: CI
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: backend
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '21', cache: maven }
      - run: ./mvnw -B test
      - run: ./mvnw -B org.pitest:pitest-maven:mutationCoverage     # added by A
      - uses: actions/upload-artifact@v4                              # added by A
        if: always()
        with: { name: pit-report, path: backend/target/pit-reports }
```

---

## 2. Contracts (agreed up front – do not change without telling the other person)

### 2.1 Shared (Phase 0)

```java
public final class WpmLimits {
    public static final int MIN_WPM = 60;
    public static final int MAX_WPM = 1500;
    public static final int DEFAULT_WPM = 250;
    private WpmLimits() {}
}

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}
```

Invalid arguments → `IllegalArgumentException`. Missing entity → `ResourceNotFoundException`.

### 2.2 ReadingPlanCalculator (A)

```java
public record WordTiming(String word, int orpIndex, long displayMs) {}
public record ReadingPlan(List<WordTiming> words, int wpm, long totalDurationMs) {}

@Component
public class ReadingPlanCalculator {
    public ReadingPlan createPlan(String text, int wpm);
    public List<String> tokenize(String text);
    public int orpIndex(String word);
    public long delayMs(String word, int wpm);
}
```

**Rules (the spec the tests are written against):**

1. **Validation**: `text == null` → `IllegalArgumentException`. `wpm < MIN_WPM || wpm > MAX_WPM` → `IllegalArgumentException`.
2. **Tokenize**: trim, split on any whitespace run (`\s+`: spaces, tabs, newlines). Blank text → empty list. Hyphenated words stay one token (`well-known`). Numbers are tokens (`2024`, `3.14`). Tokens that contain **no letter and no digit** (`-`, `–`, `...`) are dropped.
3. **Core** of a word = the word without leading/trailing non-letter/non-digit characters (`"(Hello!"` → `Hello`). `coreLength` = its length.
4. **ORP index** = number of leading punctuation characters + offset from this table:

   | coreLength | 1 | 2–5 | 6–9 | 10–13 | ≥ 14 |
   |---|---|---|---|---|---|
   | offset | 0 | 1 | 2 | 3 | 4 |

5. **Delay**: `base = 60000.0 / wpm`, `factor = 1.0`, then add:
   - `+1.0` if the word ends with `.` `!` `?` (sentence end)
   - `+0.5` if the word ends with `,` `;` `:` (clause pause)
   - `+0.5` if `coreLength > 8` (long word)
   - `+0.5` if the core contains a digit (numbers)

   `displayMs = Math.round(base * factor)`.
6. **createPlan**: one `WordTiming` per token, in order; `totalDurationMs` = sum of all `displayMs`; `wpm` is echoed back. Blank text → empty list, total `0`.

Reference values at 300 WPM (base = 200 ms): `word` → 200, `word,` → 300, `word.` → 400, `extraordinary.` → 500, `2024` → 300.

### 2.3 ReadingSessionService (B)

```java
public record RecordSessionRequest(Long userId, Long textId, int wpm, int wordsRead,
                                   int durationSeconds, Integer comprehensionScore) {}

@Service
public class ReadingSessionService {
    public ReadingSessionService(ReadingSessionRepository sessions, UserRepository users,
                                 TextRepository texts, BookmarkRepository bookmarks, Clock clock);

    public ReadingSession recordSession(RecordSessionRequest request);
    public ReadingSession getSession(Long sessionId);
    public List<ReadingSession> getSessionsForUser(Long userId);
    public void deleteSession(Long sessionId);
}
```

**Rules:**

- `recordSession`
  1. Validate request **before any repository call**: `wpm` in `[MIN_WPM, MAX_WPM]`, `wordsRead >= 0`, `durationSeconds > 0`, `comprehensionScore == null` or in `[0, 100]`. Else `IllegalArgumentException`.
  2. Load user and text (`findById`), missing → `ResourceNotFoundException`.
  3. `wordsRead > text.wordCount` → `IllegalArgumentException` (nothing saved).
  4. Build session, `date = LocalDateTime.now(clock)`, save via `sessions.save(...)`, return the saved instance.
  5. Bookmark: if `wordsRead == 0` → bookmark untouched. Else `bookmarks.findByUserIdAndTextId`: none → create with `wordPosition = wordsRead`; existing → `wordPosition = min(old + wordsRead, text.wordCount)`; save.
- `getSession`: `findById`, missing → `ResourceNotFoundException`.
- `getSessionsForUser`: user missing (`users.existsById`) → `ResourceNotFoundException`; else `findByUserIdOrderByDateDesc`.
- `deleteSession`: `existsById` false → `ResourceNotFoundException` and **no** `deleteById`; else `deleteById`.

A `Clock` is injected so the date is deterministic in tests (needs a `@Bean Clock clock() { return Clock.systemDefaultZone(); }` in a config class).

### 2.4 ReadingStatisticsService (B, built with TDD – signatures only, no code before Red!)

```java
public OptionalDouble getAverageWpm(Long userId);
public int getRecommendedWpm(Long userId);
```

- `getAverageWpm` = **effective** WPM over all sessions: `sum(wordsRead) * 60.0 / sum(durationSeconds)`. No sessions → `OptionalDouble.empty()`.
- `getRecommendedWpm` (replaces the hard-coded default WPM in the frontend):
  1. Take `findTop5ByUserIdOrderByDateDesc`. Empty → `DEFAULT_WPM` (250).
  2. `avg` = mean of the sessions' `wpm`.
  3. Mean of the **non-null** comprehension scores: `>= 80` → `avg * 1.10`; `< 60` → `avg * 0.90`; otherwise / no scores → unchanged.
  4. Round to the nearest 10 (`Math.round(x / 10.0) * 10`, half up).
  5. Clamp to `[MIN_WPM, MAX_WPM]`.

---

## 3. Phase 0 – Shared foundation (together, ~3 h, day 1–2)

Do this as **one pair-programming session** on branch `chore/foundation` so both understand the base. Swap the keyboard halfway.

- [ ] 0.1 Align pom.xml with the agreed stack (§0): Boot 3.5.x, Java 21, `spring-boot-starter-test`, keep Lombok, MySQL driver, PIT plugin.
- [ ] 0.2 Delete `SpeedreaderApplicationTests.java`.
- [ ] 0.3 Create `common/WpmLimits`, `common/ResourceNotFoundException`, `ClockConfig` (Clock bean).
- [ ] 0.4 Create the 4 entities and 4 repository interfaces (§1.2, §1.3).
- [ ] 0.5 Narrow PIT `targetClasses` / `targetTests` (§1.4).
- [ ] 0.6 **PIT smoke test**: one throw-away test, run `mvnw test` and `mvnw org.pitest:pitest-maven:mutationCoverage` locally, check `target/pit-reports/index.html` is produced. Remove the throw-away test.
- [ ] 0.7 Minimal `.github/workflows/ci.yml` with `mvnw -B test`. Make `mvnw` executable in git: `git update-index --chmod=+x backend/mvnw`.
- [ ] 0.8 Merge to `main`, check CI is green on GitHub.

After 0.8, A and B work independently.

---

## 4. Work streams

### Estimated workload

| | Person A | Person B |
|---|---|---|
| Implementation | Calculator 3 h | Session service 2.5 h, statistics (in TDD) — |
| Tests | Calculator tests 5 h | Session tests 4 h, TDD cycles 5 h |
| PIT to 100 % | 2 h | 2 h |
| Automation / docs | CI + PIT step + threshold + artifact 2 h, mutator explanation doc 1.5 h | TDD commit log doc 1 h |
| Reviews | Review B's PRs 1.5 h | Review A's PRs 1.5 h |
| **Total** | **≈ 15 h** | **≈ 16 h** |

B's TDD work is heavier in tests; A compensates with CI, PIT config and the mutator explanation used in the demo.

### 4.1 Person A – Logic unit, PIT, CI

**Owned files** (only A edits these):
- `service/ReadingPlanCalculator.java`, `service/dto/WordTiming.java`, `service/dto/ReadingPlan.java`
- `test/.../service/ReadingPlanCalculatorTest.java`
- `.github/workflows/ci.yml`
- PIT `<configuration>` block in `pom.xml` (after Phase 0)
- `docs/mutation-testing.md`

**Checklist**
- [ ] A1 Branch `feature/a-reading-plan-calculator`. Create DTO records.
- [ ] A2 Implement `tokenize`, `orpIndex`, `delayMs`, `createPlan` per §2.2. Keep methods small, no logging, no unreachable branches.
- [ ] A3 Write `ReadingPlanCalculatorTest` (cases in §6.1) with `@Nested` classes per method and `@ParameterizedTest` for tables (ORP, delay).
- [ ] A4 Run PIT, open the HTML report, kill every surviving mutant of `ReadingPlanCalculator`. Note each survivor and which test killed it (input for A8).
- [ ] A5 PR → B reviews → merge.
- [ ] A6 Branch `chore/a-ci-pit`: add PIT step + `upload-artifact` for the HTML report to the workflow. Optionally a status badge in `README.md`.
- [ ] A7 End of week 2, after B's units are merged at 100 %: enable `<mutationThreshold>100</mutationThreshold>` so CI fails if a mutant survives. Verify by temporarily deleting one assertion locally (CI must go red).
- [ ] A8 Write `docs/mutation-testing.md`: list of PIT DEFAULTS mutators with one concrete example **from our code** each (see §7.1), how to run PIT, where the report is, screenshot of 100 %.

### 4.2 Person B – Mocking unit, TDD

**Owned files** (only B edits these):
- `service/ReadingSessionService.java`, `service/dto/RecordSessionRequest.java`
- `service/ReadingStatisticsService.java`
- `test/.../service/ReadingSessionServiceTest.java`, `test/.../service/ReadingStatisticsServiceTest.java`
- `docs/tdd-log.md`

**Checklist**
- [ ] B1 Branch `feature/b-session-service`. Implement `ReadingSessionService` per §2.3.
- [ ] B2 Write `ReadingSessionServiceTest` (cases in §6.2): `@ExtendWith(MockitoExtension.class)`, `@Mock` for the 4 repositories, `Clock.fixed(...)`, service built manually in `@BeforeEach` (because Clock is not a mock). Use `ArgumentCaptor`, `verify`, `verify(..., never())`, `verifyNoInteractions`, `assertThrows`.
- [ ] B3 Run PIT, kill every surviving mutant of `ReadingSessionService`.
- [ ] B4 PR → A reviews → merge.
- [ ] B5 Branch `feature/b-tdd-statistics`. Follow the TDD cycles in §6.3. **Each phase = one commit**:
  - `test(red): getAverageWpm returns empty when no sessions` (test fails / does not compile)
  - `feat(green): ...` (minimal code, all tests green)
  - `refactor: ...` (only if something to clean up; tests stay green)
  Run `mvnw test` before every commit and paste the result line (failures/green) in the commit body.
- [ ] B6 Run PIT on `ReadingStatisticsService` too and get it to 100 % (bonus, shows test quality).
- [ ] B7 Write `docs/tdd-log.md`: table *cycle → red commit hash → green commit hash → refactor commit hash → what changed*.
- [ ] B8 PR → A reviews → merge **with a merge commit (no squash, no rebase-squash)**, otherwise the TDD history is lost.

---

## 5. Git workflow

- `main` is always green. Nobody commits directly to `main` after Phase 0.
- Branch names: `feature/a-…`, `feature/b-…`, `chore/a-…`.
- Small PRs (one unit or one CI change per PR). The other person reviews **every** PR — this is how both learn all parts for the demo.
- Before opening a PR: `git pull origin main`, merge/rebase main into the branch, `mvnw test` + PIT locally green.
- Merge strategy: **merge commit** for all PRs (required for the TDD branch, simplest to keep consistent).
- Conflict hot spot: `pom.xml`. Only A changes the PIT block; if B needs a pom change, tell A or do it in a separate tiny PR.
- Commit at least daily, push daily so CI runs.

### Schedule

| Week | A | B |
|---|---|---|
| 1 | Phase 0 (together) → A1–A3 | Phase 0 (together) → B1–B2 |
| 2 | A4–A6, review B's PR, A7 at end of week | B3–B4, B5 (TDD), review A's PR |
| 3 (first half) | A8, demo rehearsal | B6–B8, demo rehearsal |
| 3 (end) | **Demo** | **Demo** |

---

## 6. Test cases

Rule of thumb for killing mutants: **assert exact values** (not `> 0`), test **both sides of every boundary**, and **verify every interaction** with a mock (including the ones that must *not* happen).

### 6.1 ReadingPlanCalculatorTest (A)

**tokenize**
| Input | Expected |
|---|---|
| `"Hello world"` | `[Hello, world]` |
| `"  Hello   world  "` | `[Hello, world]` (multiple + leading/trailing spaces) |
| `"Hello\tworld\nagain"` | `[Hello, world, again]` |
| `""`, `"   "` | `[]` |
| `null` | `IllegalArgumentException` |
| `"well-known fact"` | `[well-known, fact]` |
| `"In 2024 pi was 3.14"` | `[In, 2024, pi, was, 3.14]` |
| `"wait - what ..."` | `[wait, what]` (punctuation-only tokens dropped) |
| `"Hello, world!"` | `[Hello,, world!]` (punctuation stays attached) |
| `"a"` | `[a]` |

**orpIndex** (parameterized, boundaries of every table row)
| Word | coreLength | Expected |
|---|---|---|
| `a` | 1 | 0 |
| `to` | 2 | 1 |
| `hello` | 5 | 1 |
| `planet` | 6 | 2 |
| `wonderful` | 9 | 2 |
| `everything` | 10 | 3 |
| `extraordinary` | 13 | 3 |
| `internationals` | 14 | 4 |
| `(hello` | 5 + 1 leading | 2 |
| `"hello"` | 5 + 1 leading | 2 (trailing punctuation does not shift) |
| `hello.` | 5 | 1 |

**delayMs** (300 WPM → base 200 ms unless stated)
| Word | WPM | Expected | Kills |
|---|---|---|---|
| `word` | 300 | 200 | MATH on `/` |
| `word` | 600 | 100 | |
| `word` | 700 | 86 (85.71 rounded, not truncated) | rounding bug |
| `word.` / `word!` / `word?` | 300 | 400 | each branch of sentence end |
| `word,` / `word;` / `word:` | 300 | 300 | each branch of clause pause |
| `eightchr` (8) | 300 | 200 | `>` → `>=` boundary |
| `ninechars` (9) | 300 | 300 | |
| `2024` | 300 | 300 | digit factor |
| `extraordinary.` | 300 | 500 | factors are added |
| `word` | 60 | 1000 | MIN accepted |
| `word` | 1500 | 40 | MAX accepted |
| `word` | 59 / 1501 / 0 / -1 | `IllegalArgumentException` | boundary + negation |

**createPlan**
- `"Hi there."` at 300 → 2 timings `(Hi,1,200)`, `(there.,1,400)`, `totalDurationMs = 600`, `wpm = 300`. Assert the **whole** records (record `equals`), not single fields.
- Order is preserved for a 3+ word text.
- `""` → empty list, total `0`, wpm echoed.
- `null` text → exception; invalid WPM with valid text → exception; invalid WPM with empty text → still exception (validation first).
- Returned list is the size of `tokenize(text)`.

### 6.2 ReadingSessionServiceTest (B)

Fixture: `user(id=1)`, `text(id=10, wordCount=100)`, `Clock.fixed(2026-11-02T10:00Z, UTC)`, valid request `(1, 10, 300, 50, 120, 80)`.

**recordSession – happy path**
- Saves a session; `ArgumentCaptor` checks **every** field: user, text, wpm 300, wordsRead 50, duration 120, score 80, date = `2026-11-02T10:00`.
- Returns exactly the object returned by `sessions.save` (`assertSame`).
- `comprehensionScore = null` is accepted and stored as null.

**recordSession – validation (each: `assertThrows` + `verifyNoInteractions` of all repositories)**
| Field | Rejected | Accepted (separate test) |
|---|---|---|
| wpm | 59, 1501 | 60, 1500 |
| wordsRead | -1 | 0 |
| durationSeconds | 0 | 1 |
| comprehensionScore | -1, 101 | 0, 100 |

**recordSession – lookups**
- User not found → `ResourceNotFoundException`, `sessions.save` never called, text repo never called (if user is checked first).
- Text not found → `ResourceNotFoundException`, no save.
- `wordsRead = 101` (> wordCount) → `IllegalArgumentException`, no save, no bookmark access.
- `wordsRead = 100` (= wordCount) → accepted.

**recordSession – bookmark**
- No bookmark exists → new bookmark saved with user, text, `wordPosition = 50` (captor).
- Existing bookmark at 30 → updated to 80 and saved.
- Existing bookmark at 70 + read 50 → clamped to 100 (not 120).
- Existing at 50 + read 50 → exactly 100 (boundary of `min`).
- `wordsRead = 0` → `bookmarks` never touched (`verifyNoInteractions(bookmarks)`), session still saved.

**getSession / getSessionsForUser / deleteSession**
- `getSession` found → returns it; missing → exception with message containing the id.
- `getSessionsForUser` → returns the list from `findByUserIdOrderByDateDesc` (assert same list, verify called with id 1); empty list → empty list (kills EMPTY_RETURNS confusion only if non-empty case is also tested); unknown user → exception and repository query never called.
- `deleteSession` exists → `verify(sessions).deleteById(5L)`; not exists → exception and `verify(sessions, never()).deleteById(any())`.

### 6.3 ReadingStatisticsServiceTest – TDD cycles (B)

Each row = one Red → Green (→ Refactor) cycle, in this order.

| # | Test | Data | Expected |
|---|---|---|---|
| 1 | average – no sessions | `[]` | `OptionalDouble.empty()` |
| 2 | average – one session | 300 words / 60 s | `300.0` |
| 3 | average – weighted, not plain mean | 600 w / 120 s + 100 w / 60 s | `233.333…` (delta 0.001; plain mean would be 200) |
| 4 | recommended – no sessions | `[]` | `250` |
| 5 | recommended – mean of wpm | wpm 300, 320, 310, no scores | `310` |
| 6 | recommended – rounding | wpm 304 / 305 | `300` / `310` |
| 7 | recommended – good comprehension | wpm 300, 300, scores 90, 80 (mean 85) | `330` |
| 8 | boundary 80 | wpm 300, score 80 / 79 | `330` / `300` |
| 9 | bad comprehension | wpm 300, score 59 / 60 | `270` / `300` |
| 10 | null scores ignored | wpm 300, scores 90, null | `330` (mean of 90 only) |
| 11 | clamp upper | wpm 1500, score 100 | `1500` |
| 12 | clamp lower | wpm 60, score 0 | `60` |
| 13 | uses the top-5 query | any | `verify(sessions).findTop5ByUserIdOrderByDateDesc(1L)` and `never().findByUserId` |

---

## 7. PIT – mutators and pitfalls

### 7.1 Mutators used (DEFAULTS) – what to explain in the demo

| Mutator | Example from our code | Killed by |
|---|---|---|
| CONDITIONALS_BOUNDARY | `coreLength > 8` → `>= 8` | delay test with an 8-letter word |
| NEGATE_CONDITIONALS | `wpm < MIN_WPM` → `wpm >= MIN_WPM` | valid and invalid WPM tests |
| MATH | `60000.0 / wpm` → `60000.0 * wpm`; `old + wordsRead` → `old - wordsRead` | exact ms / exact bookmark position |
| INCREMENTS | `leading++` → `leading--` | ORP with leading punctuation |
| INVERT_NEGS | `-x` → `x` | (only if negation is used) |
| VOID_METHOD_CALLS | removes `bookmarks.save(b)` / `sessions.deleteById(id)` | `verify(...)` |
| EMPTY_RETURNS | returns `List.of()` / `OptionalDouble.empty()` | asserting non-empty results |
| NULL_RETURNS | returns `null` instead of the saved session | `assertSame` on the return value |
| PRIMITIVE_RETURNS | returns `0` instead of the delay / recommended WPM | exact numeric assertions |
| TRUE_RETURNS / FALSE_RETURNS | boolean helper `isSentenceEnd()` always true/false | tests for both cases |

### 7.2 Pitfalls and how to avoid them

1. **Too broad `targetClasses`** → entities, Lombok getters/`equals`/`hashCode`, config and the main class produce mutants no unit test can sensibly kill. Target only the 3 service classes (§1.4).
2. **Equivalent mutants** (mutant behaves the same as the original, can never be killed). Avoid them by design:
   - No unreachable/redundant conditions (e.g. `if (len <= 1)` when `len` can never be 0 – write `len == 1`, or make sure 0 is impossible and covered).
   - Don't double-check what was already validated (`if (wpm <= 0)` after the range check).
   - Use `Math.min/max` instead of hand-written `if (a > b)` for clamping, or test equality at the boundary (`old + read == wordCount`).
   - If a true equivalent mutant remains, rewrite the code so it disappears; don't just accept < 100 %.
3. **Untested branches** → every `if`, every `||` operand (`.`, `!`, `?` each) needs its own test. Check line coverage in the PIT report first, then mutation coverage.
4. **Weak assertions** (`assertNotNull`, `size() > 0`) let MATH and RETURNS mutants survive. Assert exact values and whole records.
5. **Missing `verify`** → VOID_METHOD_CALLS on `save`/`deleteById` survive. Also verify the *negative* cases with `never()` / `verifyNoInteractions`.
6. **Logging / `toString`**: no logging in the target classes (or keep it — PIT ignores calls to `org.slf4j`, `java.util.logging`, log4j by default via `avoidCallsTo`). Don't put logic in `toString`/exception messages you don't assert.
7. **Time-dependent code** (`LocalDateTime.now()`) → flaky tests and unkillable mutants. Inject `Clock`.
8. **Mockito strict stubs**: `MockitoExtension` fails on unused stubbing (`UnnecessaryStubbingException`). Good — it keeps tests clean. Stub only what each test needs.
9. **No `@SpringBootTest`** anywhere — PIT runs each test many times; a Spring context (and a missing DB) makes it slow or fail.
10. **Stale report**: run `mvnw test` before PIT, `timestampedReports=false` so the report is always at the same path.
11. **TDD + PIT order**: don't add `mutationThreshold=100` until all units are done, or every PR in between turns CI red.

---

## 8. Final checklist before the demo

**Code & tests**
- [ ] `mvnw test` green locally (both people, on both machines).
- [ ] No `@SpringBootTest`, no real DB access; all repositories are `@Mock`.
- [ ] Every test has a descriptive name (`@DisplayName` or `should…_when…`) and Arrange / Act / Assert structure.

**Mutation testing**
- [ ] PIT runs without errors: `mvnw org.pitest:pitest-maven:mutationCoverage`.
- [ ] `ReadingPlanCalculator` 100 % mutation score.
- [ ] `ReadingSessionService` 100 % mutation score.
- [ ] (Bonus) `ReadingStatisticsService` 100 %.
- [ ] HTML report available locally (`backend/target/pit-reports/index.html`) **and** as CI artifact; screenshot in `docs/mutation-testing.md`.
- [ ] `mutationThreshold=100` active.

**Automation**
- [ ] GitHub Actions green on the latest `main` commit, runs `test` + PIT on every push.
- [ ] Demo trick prepared: break one line (e.g. `>` → `>=`) on a branch, push, show CI turning red with the failing test / surviving mutant.

**TDD**
- [ ] Red / Green / Refactor commits visible in `git log --oneline` on `main` (not squashed).
- [ ] `docs/tdd-log.md` with commit hashes per cycle.

**Fachgespräch – both people can explain**
- [ ] Why each dependency is mocked and how `verify` / `ArgumentCaptor` / `never()` are used (Part 1).
- [ ] The calculator rules and why each test exists (boundary values) (Part 2).
- [ ] At least 5 PIT mutators with an example from our code, and what an equivalent mutant is (Part 3).
- [ ] Walk through 2 TDD cycles via the commits (Part 4).
- [ ] The CI workflow file line by line (Part 5).
- [ ] Rehearsal done: A presents B's parts and B presents A's parts once.
