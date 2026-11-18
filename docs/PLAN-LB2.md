# Work Plan – M450 LB2 Integration Testing (Speed Reader Backend)

Three people (**A**, **B** and **C**) connect the Speed Reader backend to MySQL and test the integration between the code and the database. Doing LB2 as a team of three is cleared with the teacher.

**Product:** a test concept (German, based on `M450_LB2_01_Testkonzept_Vorlage.docx`) **with executed tests and documented results**, sent as PDF.
**Deadline:** end of the lesson in **week 6** of the quarter (the 6th school day). Late delivery costs 0.4 grade points per 24 h.
**File name:** `M450_LB2_<Klasse>_<Name1>_<Name2>_<Name3>.pdf`, e-mail subject `M450 LB2 <Klasse>`, to the teacher, CC to the other two.

| # | Criterion (rubric) | Weight | Owner |
|---|---|---|---|
| 1 | Structure and formatting | 1 | C |
| 2 | Test environment described in detail (software + hardware) | 2 | A |
| 3 | Three focus areas chosen, reasons detailed and convincing | 2 | each owner writes the reason for their area, C edits |
| 4 | Focus 1 – **Datenbankverbindung**: test cases (incl. in/out of scope) | 1 | A |
| 5 | Focus 1 – execution + results + defect class | 1 | A |
| 6 | Focus 2 – **Transaktionen / CRUD**: test cases (incl. in/out of scope) | 1 | B |
| 7 | Focus 2 – execution + results + defect class | 1 | B |
| 8 | Focus 3 – **Datenintegrität**: test cases (incl. in/out of scope) | 1 | C |
| 9 | Focus 3 – execution + results + defect class | 1 | C |

Total 33 points. The assignment asks for **3 to 5 test cases per focus area**; we plan **5 each** (15 in total).

**Decisions (2026-11-18):**

| Topic | Decision |
|---|---|
| Database | MySQL 9.7 LTS (Docker image `mysql:9.7`) |
| Schema | **Flyway** migration `V1__init.sql`, Hibernate only validates (`ddl-auto=validate`) – so the constraints we test are written down and reviewable |
| How tests run | **JUnit 5 + Testcontainers**: every integration test starts a real MySQL in Docker. Runs locally and in GitHub Actions |
| Docker | Available on all three machines |
| Focus areas | 1 Datenbankverbindung (A), 2 Transaktionen / CRUD (B), 3 Datenintegrität (C) |

---

## 1. Starting point

What we already have from LB1:

- 4 JPA entities → 4 tables: `users`, `texts`, `reading_sessions`, `bookmarks` (≥ 3 tables required ✓)
- 4 Spring Data repositories, `ReadingSessionService`, `ReadingStatisticsService`
- Unit tests with mocked repositories, PIT 93/93, CI on every push

What is missing for LB2:

| Missing | Where it is added |
|---|---|
| Datasource config (`application.properties` only has the app name) | Phase 0 |
| Database schema | Phase 0 (Flyway) |
| Local MySQL for running the app | Phase 0 (`docker-compose.yml`) |
| Integration test setup (Testcontainers, failsafe) | Phase 0 |
| CI step for integration tests | Phase 0 |

Already visible in the code (we test it, we don't fix it in advance): `ReadingSessionService.recordSession` has **no `@Transactional`**. The session and the bookmark are saved in two separate transactions. If the bookmark save fails, the session stays in the database. Test **T-014** is designed to show this (see §5.2).

---

## 2. Test setup

```
 JUnit 5 test class (*IT)                       GitHub Actions (ubuntu-latest)
        │  @SpringBootTest                             │  ./mvnw -B verify
        ▼                                              ▼
 Spring context ── services ── repositories      same tests, Docker on the runner
        │
 Hibernate (ddl-auto=validate)
        │
 HikariCP connection pool
        │  JDBC (mysql-connector-j)
        ▼
 MySQL 9.7 in Docker  ◄── started by Testcontainers, random port,
        ▲                 Flyway V1 creates the 4 tables on start
        │
 Flyway migrations (src/main/resources/db/migration)
```

Rules:

- Integration test classes end in **`IT`** and live in `bbzbl_it_m450.speedreader.integration`. Maven **failsafe** runs them in `verify`; surefire (unit tests) and PIT do not pick them up (PIT's `targetTests` is `service.*Test`). PIT stays fast and stays at 100 %.
- **No mocks of repositories** in integration tests – the point is the real database. Only for T-014 one repository is wrapped with `@MockitoSpyBean` to force an error.
- Every test starts from a known state: tables are emptied in `@BeforeEach` (in FK order: `bookmarks`, `reading_sessions`, `texts`, `users`).
- Every test method has a `@DisplayName` that starts with its **test ID** (`"T-011 Session speichern und wieder lesen"`), so the test report maps 1:1 to the document.

---

## 3. Phase 0 – Connect MySQL and set up integration tests (B, ~4 h)

Branch `chore/b-mysql-integration`. A and C review the PR together (that is how all three understand the setup).

- [ ] 0.1 `pom.xml`: add `flyway-core`, `flyway-mysql`, `spring-boot-testcontainers` (test), `org.testcontainers:junit-jupiter` (test), `org.testcontainers:mysql` (test) – all versions come from the Spring Boot parent. Add `maven-failsafe-plugin` (goals `integration-test`, `verify`).
- [ ] 0.2 `src/main/resources/db/migration/V1__init.sql`: the 4 tables exactly as the entities define them (PKs, FKs `user_id`/`text_id`, `NOT NULL`, unique (`user_id`, `text_id`) on `bookmarks`, `LONGTEXT` for `content`, `utf8mb4`). **No extra CHECK constraints** – the schema must match today's code, so the tests show the real state.
- [ ] 0.3 `application.properties`: datasource for local MySQL (`jdbc:mysql://localhost:3306/speedreader`, user/password from env vars with defaults), `spring.jpa.hibernate.ddl-auto=validate`, `spring.flyway.enabled=true`.
- [ ] 0.4 `docker-compose.yml` in the repo root: `mysql:9.7`, database `speedreader`, port 3306, volume. For starting the app by hand, not for the tests.
- [ ] 0.5 `src/test/java/.../integration/AbstractMySqlIT.java`: `@SpringBootTest`, `@Testcontainers`, one static `MySQLContainer<?>("mysql:9.7")` with `@ServiceConnection`, `@BeforeEach` that empties the tables, small helper methods `createUser()`, `createText(int wordCount)`.
- [ ] 0.6 `src/test/resources/application-test.properties`: short timeouts so failure tests don't hang (`spring.datasource.hikari.connection-timeout=2000`, `validation-timeout=1000`).
- [ ] 0.7 Smoke test `ConnectionSmokeIT`: context starts, Flyway ran, the 4 tables exist. Run locally: `.\mvnw.cmd verify`.
- [ ] 0.8 CI: replace the step `./mvnw -B test` with `./mvnw -B verify` (unit + integration tests), keep PIT after it, upload `backend/target/failsafe-reports/` as artifact **`integration-test-report`**.
- [ ] 0.9 Update `README.md`, `CLAUDE.md` (Boot 3.5 / Java 21, MySQL, how to run ITs) and `docs/tech-stack.md`.
- [ ] 0.10 PR → A + C review → merge (merge commit). CI must be green on `main`.

After 0.10, A, B and C work independently.

**Known risk:** Flyway may print "MySQL 9.7 is newer than this version of Flyway" – a warning only. If the migration really fails, use the image `mysql:8.4` (also LTS) and write that into the test environment.

---

## 4. Contracts (agreed up front – tell the others before changing them)

```java
// src/test/java/bbzbl_it_m450/speedreader/integration/AbstractMySqlIT.java (B, Phase 0)
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractMySqlIT {
    @Container @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:9.7");

    @Autowired protected JdbcTemplate jdbc;          // for checks directly in the DB
    @Autowired protected UserRepository users;
    @Autowired protected TextRepository texts;
    @Autowired protected ReadingSessionRepository sessions;
    @Autowired protected BookmarkRepository bookmarks;

    @BeforeEach void cleanDatabase() { /* delete in FK order */ }

    protected User createUser() { ... }              // persisted user, unique username
    protected Text createText(int wordCount) { ... } // persisted text
}
```

- **Test IDs** (same numbering scheme as the template):
  - Datenbankverbindung: `T-001` … `T-005` → `DatabaseConnectionIT` (A)
  - Transaktionen / CRUD: `T-011` … `T-015` → `TransactionCrudIT` (B)
  - Datenintegrität: `T-021` … `T-025` → `DataIntegrityIT` (C)
- **Check the database directly** with `jdbc.queryForObject("select count(*) from ...", Integer.class)` for postconditions – not only through the repository that wrote the data.
- **Expected exceptions** are Spring's: `DataIntegrityViolationException`, `CannotGetJdbcConnectionException` / `DataAccessResourceFailureException`, `ResourceNotFoundException` (ours).
- A test that **reveals a defect** stays in the code and asserts the **expected** (correct) behaviour. It is allowed to be red only on the owner's branch until the fix; `main` stays green (see §7).

---

## 5. Focus areas and test cases

Each owner writes in the document: the reason (§3 of the template), in/out of scope, the 5 test case tables (§5 of the template), and the execution part of each table.

### 5.1 Datenbankverbindung (A) – `DatabaseConnectionIT`

**Why (draft for the reason):** Without a working connection nothing works. A speed reader must save every session, a lost connection must not crash the app or lose data silently, and after the database comes back the user must be able to continue without a restart.

| ID | Bezeichnung | Vorbedingung | Schritte | Erwartetes Ergebnis |
|---|---|---|---|---|
| T-001 | Erfolgreiche Verbindung zur DB | MySQL-Container läuft | Anwendung starten, `SELECT 1` über die DataSource | Kontext startet, `SELECT 1` liefert 1, Verbindung aus dem Hikari-Pool |
| T-002 | Schema wird beim Start erstellt | Leere Datenbank | Anwendung starten | Flyway V1 erfolgreich, Tabellen `users`, `texts`, `reading_sessions`, `bookmarks` existieren, Hibernate-Validierung ohne Fehler |
| T-003 | Falsche Zugangsdaten | MySQL läuft, falsches Passwort konfiguriert | Zweiten Spring-Kontext mit falschem Passwort starten | Start bricht ab mit klarer Meldung «Access denied for user» – keine halb gestartete Anwendung |
| T-004 | DB nicht erreichbar | Falscher Port / Host konfiguriert | Anwendung starten | Start bricht nach max. Timeout (2 s) mit Verbindungsfehler ab, kein Hängen |
| T-005 | Unerwarteter DB-Ausfall und Wiederverbindung | App läuft, Daten vorhanden | 1. Container pausieren (`pauseContainerCmd`) 2. Daten abfragen 3. Container fortsetzen 4. Daten erneut abfragen | Schritt 2: `DataAccessResourceFailureException` statt Hängen. Schritt 4: Abfrage funktioniert ohne Neustart, Daten unverändert |

How to: T-003/T-004 with `new SpringApplicationBuilder(SpeedreaderApplication.class).properties(...).run()` inside `assertThrows`, then check the root cause message. T-005 needs **its own container** (not the shared one), pause/unpause instead of stop/start (stop/start changes the port).

**In scope:** connect, schema creation, wrong configuration, outage during runtime and reconnect.
**Out of scope:** encryption (SSL/TLS) of the connection, performance/load, multi-user/concurrency, database backups – reasons: not part of our code / not required by our app.

### 5.2 Transaktionen / CRUD (B) – `TransactionCrudIT`

**Why (draft):** The core flow of the app is "record a session": it writes two tables (`reading_sessions` and `bookmarks`) in one business step. Only a real database shows whether both writes are atomic, whether queries like "the 5 newest sessions" work in SQL, and whether update and delete really change the stored data.

| ID | Bezeichnung | Vorbedingung | Schritte | Erwartetes Ergebnis |
|---|---|---|---|---|
| T-011 | Session speichern und lesen (Create / Read) | User + Text (100 Wörter) in DB | `recordSession(300 WPM, 50 Wörter, 120 s, Score 80)`, dann `getSession(id)` und direkt per SQL lesen | Alle Felder gleich wie eingegeben, Datum gesetzt, genau 1 Zeile in `reading_sessions`, Bookmark auf 50 |
| T-012 | Bookmark aktualisieren (Update) | Bookmark auf 30 vorhanden | `recordSession` mit 50 Wörtern | Bookmark-Zeile hat `word_position` 80, weiterhin genau 1 Bookmark für User + Text |
| T-013 | Session löschen (Delete) | 2 Sessions vorhanden | `deleteSession(id1)` | Nur Session 1 ist weg, Session 2 und Bookmark unverändert |
| T-014 | Rollback bei Fehler (Atomicity) | User + Text vorhanden; `BookmarkRepository.save` wird mit `@MockitoSpyBean` so präpariert, dass es eine Exception wirft | `recordSession(...)` aufrufen | Exception kommt beim Aufrufer an, **keine** neue Zeile in `reading_sessions` (alles oder nichts) |
| T-015 | Abfragen gegen die echte DB | 7 Sessions mit verschiedenen Daten für einen User, 1 Session für einen anderen User | `getSessionsForUser`, `ReadingStatisticsService.getRecommendedWpm` | Liste neueste zuerst, nur Sessions dieses Users; Empfehlung basiert nur auf den 5 neusten Sessions |

**Expected finding T-014:** without `@Transactional` the session is committed before the bookmark save fails → test fails → defect class **3 (schwerer Mangel)**: inconsistent data, the bookmark no longer matches the sessions. Fix: `@Transactional` on `recordSession` (and `deleteSession`), then run the test again → class 0. Both runs go into the document (see §7).

**In scope:** create, read, update, delete through our services; atomicity of `recordSession`; derived queries (`findByUserIdOrderByDateDesc`, `findTop5ByUserIdOrderByDateDesc`) against MySQL.
**Out of scope:** isolation between parallel transactions (single-user app, no concurrency in our use case), durability after a server crash (responsibility of MySQL/operations), performance with large data volumes.

### 5.3 Datenintegrität (C) – `DataIntegrityIT`

**Why (draft):** The statistics (average and recommended WPM) are only correct if the stored data is correct and consistent. The database must reject data that breaks the relations (session without user, two bookmarks for the same text), and data must come back exactly as it was written (special characters, long texts, timestamps).

| ID | Bezeichnung | Vorbedingung | Schritte | Erwartetes Ergebnis |
|---|---|---|---|---|
| T-021 | Fremdschlüssel: Session ohne gültigen User | Nur ein Text in DB | Per SQL eine Session mit `user_id = 999` einfügen | DB lehnt ab (FK-Verletzung), keine Zeile gespeichert |
| T-022 | User mit Sessions löschen | User mit 1 Session | `users.deleteById(userId)` | DB lehnt ab (`DataIntegrityViolationException`), User und Session bleiben – keine verwaisten Sessions |
| T-023 | Eindeutiges Bookmark pro User und Text | Bookmark für User + Text vorhanden | Zweites Bookmark für dieselbe Kombination speichern | DB lehnt ab (Unique-Verletzung), weiterhin genau 1 Bookmark |
| T-024 | Daten kommen unverändert zurück | User vorhanden | Text mit Umlauten und Emoji (`Grüezi Zürich – ß 📚`) und 100 000 Wörtern speichern, Session mit festem Datum speichern, alles neu laden | Inhalt Zeichen für Zeichen gleich, Länge gleich, Datum ohne Verschiebung (Zeitzone) |
| T-025 | Ungültige Werte direkt in der DB | User + Text vorhanden | Per SQL Session mit `comprehension_score = 150` und `wpm = 5` einfügen (an der Anwendung vorbei) | DB lehnt ab – Werte ausserhalb der fachlichen Grenzen dürfen nicht gespeichert werden |

**Expected finding T-025:** the schema has no CHECK constraints, only the service validates → the insert succeeds → defect class **2 (leichter Mangel)**: normal use through the app is safe, but other writers (scripts, imports) can store invalid data. Fix: Flyway `V2__check_constraints.sql` (`wpm BETWEEN 60 AND 1500`, `comprehension_score BETWEEN 0 AND 100`), run again → class 0. Discuss with B before adding V2 (schema change).

**In scope:** foreign keys, unique constraint, NOT NULL, delete behaviour with dependent rows, exact round trip of text, long content and timestamps, value ranges in the DB.
**Out of scope:** data validation in the service (already covered by the LB1 unit tests), security (SQL injection, permissions – Spring Data uses prepared statements; not our focus), backups and recovery.

---

## 6. Work streams

### Estimated workload

| | Person A | Person B | Person C |
|---|---|---|---|
| Setup | review Phase 0 0.5 h | Phase 0 4 h | review Phase 0 0.5 h |
| Tests (5 each) | `DatabaseConnectionIT` 5 h (T-005 is the hardest) | `TransactionCrudIT` 4 h + fix T-014 0.5 h | `DataIntegrityIT` 4 h + V2 migration 0.5 h |
| Document: own focus | reason, scope, 5 tables, execution 2.5 h | same 2.5 h | same 2.5 h |
| Document: shared | Testumgebung chapter 2 h | Testprotokoll evidence from CI 0.5 h | Einleitung, Mängelklassifizierung, formatting, PDF, e-mail 2.5 h |
| Reviews | review C's PR 0.5 h | review A's PR 0.5 h | review B's PR 0.5 h |
| **Total** | **≈ 10.5 h** | **≈ 12 h** | **≈ 10.5 h** |

### 6.1 Person A – Datenbankverbindung + Testumgebung

**Owned files:** `integration/DatabaseConnectionIT.java`, chapters 2 (Testumgebung), 3.1 and 5.1 of the document.

- [ ] A1 Branch `feature/a-db-connection-it`. Implement T-001 … T-005 (§5.1).
- [ ] A2 Run locally and in CI, note actual result, date and defect class per test.
- [ ] A3 Write chapter **Testumgebung** (weight 2 – be detailed):
  - Hardware: CPU, RAM, OS of each of the three machines + GitHub-hosted runner `ubuntu-latest`
  - Software with exact versions: Java (Temurin 21), Spring Boot 3.5.15, Hibernate, HikariCP, MySQL Connector/J, MySQL 9.7 image tag, Flyway, Testcontainers, JUnit 5, Maven (wrapper), Docker Desktop, IntelliJ IDEA
  - Network: Testcontainers maps MySQL to a random localhost port; CI runs in GitHub's VM
  - Test data: created per test in `@BeforeEach`, nothing shared between tests
  - A diagram of the setup (can be based on §2)
  - Tip: `.\mvnw.cmd dependency:list` shows the exact library versions; the Testcontainers log prints the MySQL server version.
- [ ] A4 Write 3.1 (reason) and 5.1 (scope, test case tables, execution) in the document.
- [ ] A5 PR → C reviews → merge.

### 6.2 Person B – Phase 0, Transaktionen / CRUD, CI evidence

**Owned files:** Phase 0 files (§3), `integration/AbstractMySqlIT.java`, `integration/TransactionCrudIT.java`, `.github/workflows/ci.yml`, `pom.xml`, chapters 3.2 and 5.2 of the document.

- [ ] B1 Phase 0 (§3).
- [ ] B2 Branch `feature/b-transaction-crud-it`. Implement T-011 … T-015 (§5.2).
- [ ] B3 First run: T-014 is expected to fail. Record it (§7), commit the test, **then** add `@Transactional` in a separate commit and run again.
- [ ] B4 Check PIT is still 93/93 after the `@Transactional` change.
- [ ] B5 Write 3.2 and 5.2 in the document.
- [ ] B6 Provide the CI evidence for all three: link/screenshot of the green run and the `integration-test-report` artifact.
- [ ] B7 PR → A reviews → merge.

### 6.3 Person C – Datenintegrität + document owner

**Owned files:** `integration/DataIntegrityIT.java`, `db/migration/V2__check_constraints.sql`, the Word document (structure, chapters 1, 3 intro, 3.3, 4, 5.3), final PDF.

- [ ] C1 Branch `feature/c-data-integrity-it`. Implement T-021 … T-025 (§5.3).
- [ ] C2 First run: T-025 is expected to fail. Record it (§7), then add V2 (after telling B), run again.
- [ ] C3 Set up the shared document from the template (OneDrive/Teams, so all three can write at the same time). Keep the template's structure and numbering.
- [ ] C4 Write chapter 1 (Einleitung: what the app does, the 4 tables, goal of the tests), chapter 4 (Mängelklassifizierung – table from the template), 3.3 and 5.3.
- [ ] C5 Final pass: consistent wording, all 15 test IDs unique, every table has Tester, Datum, Mängelklasse, Mängelbeschreibung filled, table of contents updated, screenshots readable.
- [ ] C6 Export PDF with the correct file name, send e-mail (§0 header), CC A and B.
- [ ] C7 PR → B reviews → merge.

---

## 7. Executing and documenting tests

For **every** test case the "Ausführung" part of the template table is filled:

| Field | What goes in |
|---|---|
| Tester | who ran it (owner) |
| Datum | date of the run |
| Mängelklasse | 0–4 (template table, chapter 4) |
| Mängelbeschreibung | for class 0: "Keine. Ergebnis entspricht der Erwartung." – otherwise: what happened, the error message, why this class |

Evidence (the rubric wants traceable results):

- Screenshot of the IntelliJ test run for your class (green/red per test ID), **or** the lines from `target/failsafe-reports/*.txt`.
- For failures: the relevant part of the stack trace / log (e.g. `DataIntegrityViolationException: Duplicate entry '1-1' for key ...`).
- Link to the green CI run on `main` (B provides it).

**When a test finds a defect** (planned: T-014, T-025 – maybe others):

1. Commit the test on your branch with the red result (`test: T-014 rollback when bookmark save fails (fails: no transaction)`).
2. Document the **first run**: actual result, error message, defect class.
3. Fix in a **separate commit** (`fix: make recordSession transactional`).
4. Run again and document the **re-test** in the same table: "Nachtest am …: bestanden, Mängelklasse 0".
5. Only merge when the branch is green.

A defect we decide **not** to fix stays documented with its class, and the test gets `@Disabled("T-0xx: known defect, class 1, see Testkonzept")` so `main` stays green.

Defect classes (from the template): 0 mängelfrei · 1 belangloser Mangel · 2 leichter Mangel · 3 schwerer Mangel · 4 kritischer Mangel.

---

## 8. Git workflow

- `main` is always green. No direct commits to `main` after this plan is merged.
- Branch names: `chore/b-…`, `feature/a-…`, `feature/b-…`, `feature/c-…`. Create branches **without** tracking `origin/main` (`git switch -c <branch> --no-track origin/main`), otherwise a push lands on `main`.
- Small PRs, one focus area per PR, reviewed by another person (A → C, B → A, C → B, as in LB1).
- Before a PR: merge `main` into the branch, `.\mvnw.cmd verify` and PIT locally green.
- Merge strategy: merge commit (keeps the red → fix history of T-014 / T-025).
- Conflict hot spots: `pom.xml` and `ci.yml` (only B), `AbstractMySqlIT` (only B – ask for new helpers), Flyway migrations (new file per change, never edit `V1` after merge).

### Schedule

| Week | A | B | C |
|---|---|---|---|
| 4 | Review Phase 0, A1 | **Phase 0 (B1)** first, then B2 | Review Phase 0, C3, C1 |
| 5 | A1–A3, review C's PR | B2–B4, B6, review A's PR | C1–C2, C4, review B's PR |
| 6 (first half) | A4–A5 | B5, B7 | C5–C6 – **send the PDF** |

Internal deadline: document complete **two days before** the official deadline, so C has time for the final pass.

---

## 9. Final checklist before sending

**Document (rubric)**
- [ ] Clear structure, template chapters in order, table of contents, consistent formatting.
- [ ] Testumgebung: hardware and software with exact versions, network, test data, diagram.
- [ ] Three focus areas named, each with a detailed reason specific to **our** app.
- [ ] Per focus area: in scope **and** out of scope with reasons.
- [ ] 15 test cases, IDs unique (T-001–005, T-011–015, T-021–025), each with description, precondition, steps, expected result.
- [ ] Every test case executed: tester, date, defect class, defect description filled.
- [ ] Found defects documented with first run + fix + re-test.
- [ ] Screenshots / log excerpts included and readable.
- [ ] PDF named `M450_LB2_<Klasse>_<Name1>_<Name2>_<Name3>.pdf`, e-mail subject `M450 LB2 <Klasse>`, CC the team.

**Code**
- [ ] `.\mvnw.cmd verify` green on all three machines (Docker running).
- [ ] CI on `main` green: unit tests, integration tests, PIT 100 %.
- [ ] `integration-test-report` artifact downloadable.
- [ ] Each person can explain their own tests **and** the setup (Testcontainers, Flyway, failsafe).

---

## 10. Open points

- Class name for the file name and the e-mail subject: `<Klasse>` = ?
- Exact date of the 6th school day: ?
