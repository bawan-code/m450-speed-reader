# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project context

School project for module M450 "Applikationen testen", assessment LB1 (Unit-Testing). Backend for a "Speed Reader" app: text is shown word by word at a given WPM, with an ORP (optimal recognition point) letter highlighted and a per-word display time depending on WPM, word length and punctuation. Reading sessions are stored to compute statistics (average / recommended WPM).

- `backend/` – Spring Boot backend (the only code in this repo). Package root: `bbzbl_it_m450.speedreader`.
- The Angular frontend exists elsewhere and is not in this repo; it is out of scope for LB1.
- `docs/assignment.md` and `docs/criterias.md` – the assignment and grading rubric (German). The four graded parts: unit test with mocking, unit test for complex logic, PIT mutation testing with **100 % mutation score** on those two units, and a TDD-built statistics function (Red → Green → Refactor as separate commits). Automation is graded too (git-push pipeline = max points).
- `docs/PLAN.md` – the agreed work plan: package layout, contracts (`ReadingPlanCalculator`, `ReadingSessionService`, `ReadingStatisticsService`, DTOs `WordTiming`/`ReadingPlan`/`RecordSessionRequest`, `WpmLimits`), exact calculation rules, test-case lists, file ownership per person, and git workflow. Read it before implementing anything; the tests are written against the rules defined there.

## Commands

Run from `backend/` (use `mvnw.cmd` on Windows, `./mvnw` elsewhere):

```
./mvnw test                                              # all unit tests
./mvnw test -Dtest=ReadingPlanCalculatorTest             # one test class
./mvnw test -Dtest=ReadingPlanCalculatorTest#methodName  # one test method
./mvnw org.pitest:pitest-maven:mutationCoverage          # PIT mutation testing (run after `test`)
```

PIT report: `backend/target/pit-reports/index.html` (non-timestamped, HTML + XML).

CI: `.github/workflows/ci.yml` runs `test` and PIT on every push/PR and uploads the PIT report as the `pit-report` artifact.

## Stack and constraints

- Spring Boot 4.1.2, Java 25 (pom + CI). Note: the assignment brief says Java 21 / Boot 3, and `docs/PLAN.md` §0 discusses possibly downgrading for PIT/JUnit compatibility; check the current pom before assuming.
- Spring Data JPA with MySQL driver, Lombok, Bean Validation.
- **No database is needed or allowed in LB1 tests.** All repositories are mocked with Mockito (`@ExtendWith(MockitoExtension.class)`). Never use `@SpringBootTest` or `@DataJpaTest` – there is no running DB and PIT runs tests many times. `SpeedreaderApplicationTests` is a leftover `@SpringBootTest` class and is excluded from PIT.
- Time-dependent code must take an injected `java.time.Clock` so tests and mutants are deterministic.

## Mutation testing rules

- PIT config lives in the `pitest-maven` plugin block of `backend/pom.xml`. `targetClasses` should be limited to the tested service units (not entities, Lombok code, config or the main class), otherwise 100 % is unreachable.
- Tests must assert exact values (and `verify` mock interactions, including `never()` / `verifyNoInteractions`) so DEFAULTS mutators (boundary, negate conditionals, math, return values, void method calls) are killed.
- Avoid writing code that produces equivalent mutants (redundant/unreachable conditions, re-validating already validated values); rewrite the code instead of accepting < 100 %.

## Git

- TDD branch history must stay visible: merge PRs with merge commits, never squash.
