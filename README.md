# m450-speed-reader
Speed reader app for Module 450. 

### Technologies

- **Frontend:** Angular v20.0 (TypeScript)
- **Backend:** Spring Boot v3.5.0 (Java 21)
- **Database:** MySQL v9.7.4 LTS
- **Build tool:** Maven
- **Testing:** JUnit 5, Mockito, PIT (mutation testing)
- **CI/CD:** GitHub Actions

---

### Folder Tree

```
backend/
  src/main/java/bbzbl_it_m450/speedreader/service    Holds the three services that we built
  src/test/java/bbzbl_it_m450/speedreader/service    Their unit tests
  pom.xml                                            dependencies + PIT configuration
docs/                                                Documentation
frontend/                                            Angular App
.github/workflows/ci.yml                             CI: Pipeline

```

---
### LB 1 Parts

| Part | Unit | Test | Docs |
|---|---|---|---|
| Mock | `ReadingSessionService` | `ReadingSessionServiceTest` | - |
| Logic | `ReadingPlanCalculator` | `ReadingPlanCalculatorTest` | - |
| TDD | `ReadingStatisticsService` | `ReadingStatisticsServiceTest` | docs/tdd-log.md |
| Mutation | All 3 Services | - | docs/mutation-testing.md |

---

### How to run tests
```shell
# Go into the backend folder
cd backend

# Run all unit tests (JUnit 5 + Mockito)
./mvnw test

# Same, but rebuild from scratch (if the output looks odd)
./mvnw clean test

# Mutation testing with PIT
./mvnw org.pitest:pitest-maven:mutationCoverage
# Report: backend/target/pit-reports/index.html
```

#### CI Pipeline:
Every push runs the tests and PIT.
