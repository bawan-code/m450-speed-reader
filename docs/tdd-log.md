# TDD log – ReadingStatisticsService (C)

`ReadingStatisticsService` was built test-first on `feature/c-tdd-statistics`, following the 13 cycles in [PLAN.md §6.3](PLAN.md). Every Red commit came before its Green commit; `git log --oneline` on the branch shows the order.

- **Red** – a new test that fails (or does not compile), committed before any code for it exists
- **Green** – the least code that makes all tests pass
- **Refactor** – none of the cycles needed one; the code stayed short enough
- **Passed immediately** – three tests (cycles 8, 10, 13) were green on the first run, because an earlier Green already covered them. They were kept as guards (boundary and interaction checks for PIT) and committed as test-only commits instead of faking a Red.

## Cycles

| # | Test | Red | Red result | Green | What changed |
|---|---|---|---|---|---|
| 1 | average – no sessions → empty | `18695e0` | compile error: `ReadingStatisticsService` does not exist | `0a71f67` | Service created; `getAverageWpm` always returns `OptionalDouble.empty()` |
| 2 | average – one session, 300 words / 60 s → 300.0 | `fb1f509` | `NoSuchElementException: No value present` | `9ca4387` | Average of the first session: words / minutes |
| 3 | average – 600 w / 120 s + 100 w / 60 s → 233.333 (weighted, not 200) | `52dd600` | expected 233.333 but was 300.0 | `cde5ffe` | Loop over all sessions: total words / total minutes |
| 4 | recommended – no sessions → 250 | `3ac7789` | compile error: `getRecommendedWpm` does not exist | `9388da5` | `getRecommendedWpm` returns `WpmLimits.DEFAULT_WPM` |
| 5 | recommended – wpm 300, 320, 310 → 310 | `e43bcee` | expected 310 but was 250 | `3f6ca44` | Mean `wpm` of the recent sessions |
| 6 | recommended – mean 304 → 300, 305 → 310 | `499236c` | expected 300 but was 304; expected 310 but was 305 | `690e6f2` | Mean as `double`, `Math.round(x / 10.0) * 10` |
| 7 | good comprehension – scores 90, 80 → 330 | `4852645` | expected 330 but was 300 | `2901360` | Helper `averageScore` (skips null scores); mean score ≥ 80 → × 1.10 |
| 8 | boundary 80 – score 80 → 330, 79 → 300 | `957624a` | passed immediately (cycle 7 already uses `>=`) | – | Test only: guards `>=` against a `>` mutant |
| 9 | bad comprehension – score 59 → 270, 60 → 300 | `e2f655e` | expected 270 but was 300 | `4401932` | Mean score < 60 → × 0.90 |
| 10 | null scores ignored – scores 90, null → 330 | `a4324b1` | passed immediately (`averageScore` skips null since cycle 7) | – | Test only |
| 11 | clamp upper – wpm 1500, score 100 → 1500 | `99a8c75` | expected 1500 but was 1650 | `e776c4d` | `Math.min(rounded, MAX_WPM)` |
| 12 | clamp lower – wpm 60, score 0 → 60 | `7951032` | expected 60 but was 50 | `75665be` | `Math.max(MIN_WPM, …)` |
| 13 | uses the top-5 query – `verify` top-5, `never()` `findByUserId` | `121bafc` | passed immediately (top-5 query used since cycle 4) | – | Test only |

`d02f9e9` merges `main` into the branch between cycles 1 and 2 (A's calculator and the executable `mvnw`); it does not touch the statistics files.

## Notes

- Commit titles are kept as committed; a few contain typos (`Wpom`, `rouns`, `slos fown`). The table above is the corrected description.
- The commits have no test-result line in their body (PLAN §4.3 C1 asked for one); the Red results above come from the `mvnw test` runs at the time.
