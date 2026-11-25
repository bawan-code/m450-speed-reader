# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project context

School project for module M450 "Applikationen testen". Backend for a "Speed Reader" app: text is shown word by word at a given WPM, with an ORP (optimal recognition point) letter highlighted and a per-word display time depending on WPM, word length and punctuation. Reading sessions are stored to compute statistics (average / recommended WPM).

- `backend/` – Spring Boot backend. Package root: `bbzbl_it_m450.speedreader`. Four tables: `users`, `texts`, `reading_sessions`, `bookmarks`.
- `frontend/` – Angular app, out of scope for both assessments.

## Assessments – one plan each

Read the plan of the assessment you are working on before implementing anything.

| | LB1 – Unit-Testing (done) | LB2 – Integration-Testing (current) |
|---|---|---|
| Plan | `docs/PLAN.md` | `docs/PLAN-LB2.md` |
| Assignment, rubric | `docs/assignment.md`, `docs/criterias.md` | outside this repo (PDFs from the teacher) |
| Tests | `*Test` in `service/` | `*IT` in `integration/` |
| Database | none – repositories mocked with Mockito | real MySQL, started by Testcontainers |
| Graded output | the code: mocking, complex logic, 100 % PIT, TDD history | a Testkonzept (PDF) with executed tests and results |

The two kinds of test live side by side; the class name decides the rules:

- **`*Test` (LB1):** `@ExtendWith(MockitoExtension.class)`, no Spring context, no database – PIT runs them many times. Never `@SpringBootTest` or `@DataJpaTest` here.
- **`*IT` (LB2):** real database, no repository mocks. Setup, contracts and test IDs → `docs/PLAN-LB2.md` §2–4. Not yet built (Phase 0, §3) – check the code before assuming it exists.

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

- Spring Boot 3.5, Java 21 (pom + CI). Spring Data JPA with MySQL driver, Lombok, Bean Validation.
- Time-dependent code must take an injected `java.time.Clock` so tests and mutants are deterministic.

## Mutation testing rules (LB1, still enforced in CI)

- PIT config lives in the `pitest-maven` plugin block of `backend/pom.xml`. `targetClasses` is limited to the three service units, `targetTests` to `service.*Test` – integration tests stay out of PIT.
- Tests must assert exact values (and `verify` mock interactions, including `never()` / `verifyNoInteractions`) so DEFAULTS mutators (boundary, negate conditionals, math, return values, void method calls) are killed.
- Avoid writing code that produces equivalent mutants (redundant/unreachable conditions, re-validating already validated values); rewrite the code instead of accepting < 100 %.
- A production change made for LB2 (e.g. `@Transactional`) must keep PIT at 100 %.

## Git

- Merge PRs with merge commits, never squash – the TDD history (LB1) and the red test → fix → re-test history (LB2) must stay visible.
- Branches: create without tracking `origin/main` (`git switch -c <branch> --no-track origin/main`), otherwise a push lands on `main`.
