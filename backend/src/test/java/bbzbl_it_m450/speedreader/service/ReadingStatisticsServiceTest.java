package bbzbl_it_m450.speedreader.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.OptionalDouble;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import bbzbl_it_m450.speedreader.common.WpmLimits;
import bbzbl_it_m450.speedreader.model.ReadingSession;
import bbzbl_it_m450.speedreader.repository.ReadingSessionRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(MockitoExtension.class)
class ReadingStatisticsServiceTest {

  @Mock
  private ReadingSessionRepository sessions;

  private ReadingStatisticsService service;

  @BeforeEach
  void setUp() {
    service = new ReadingStatisticsService(sessions);
  }

  @Test
  @DisplayName("getAverageWpm returns empty when the user has no sessions")
  void averageIsEmptyWhenNoSessions() {
    when(sessions.findByUserId(1L)).thenReturn(List.of());

    OptionalDouble result = service.getAverageWpm(1L);

    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("getAverageWpm of one session")
  void averageOfOneSession() {
    ReadingSession session = ReadingSession.builder().wordsRead(300).durationSeconds(60).build();
    when(sessions.findByUserId(1L)).thenReturn(List.of(session));

    OptionalDouble result = service.getAverageWpm(1L);

    assertEquals(300.0, result.getAsDouble(), 0.001);
  }

  @Test
  @DisplayName("getAverageWpm of several sessions")
  void averageOfSeveralSessions() {
    ReadingSession sessionFirst = ReadingSession.builder().wordsRead(600).durationSeconds(120).build();
    ReadingSession sessionSecond = ReadingSession.builder().wordsRead(100).durationSeconds(60).build();
    when(sessions.findByUserId(1L)).thenReturn(List.of(sessionFirst, sessionSecond));

    OptionalDouble result = service.getAverageWpm(1L);

    assertEquals(233.333, result.getAsDouble(), 0.001);
  }

  @Test
  @DisplayName("Recommended average")

  void recommendedIsDefaultWhenNoSessions() {
    when(sessions.findTop5ByUserIdOrderByDateDesc(1L)).thenReturn(List.of());

    int result = service.getRecommendedWpm(1L);

    assertEquals(WpmLimits.DEFAULT_WPM, result);
  }

  @Test
  @DisplayName("getRecommendedWpm is the average of the recent session")
  void recommendedIsAverageOfRecentWpm() {
    ReadingSession first = ReadingSession.builder().wpm(300).build();
    ReadingSession second = ReadingSession.builder().wpm(320).build();
    ReadingSession third = ReadingSession.builder().wpm(310).build();
    when(sessions.findTop5ByUserIdOrderByDateDesc(1L)).thenReturn(List.of(first, second, third));

    int result = service.getRecommendedWpm(1L);

    assertEquals(310, result);
  }

  // Executes the test with the values 304, 300 and 305, 310 as parameters -
  // parameterized test
  @ParameterizedTest
  @CsvSource({ "304, 300", "305, 310" })
  @DisplayName("getRecommendedWpm rounds the mean to the nearest 10, half up")
  void recommendedIsRoundedToTen(int wpm, int expected) {
    ReadingSession session = ReadingSession.builder().wpm(wpm).build();
    when(sessions.findTop5ByUserIdOrderByDateDesc(1L)).thenReturn(List.of(session));

    int result = service.getRecommendedWpm(1L);

    assertEquals(expected, result);
  }

  @Test
  @DisplayName("getRecommendedWpm speeds up by 10 % when the average score is 80 or more")
  void recommendedSpeedsUpForGoodComprehension() {
    ReadingSession first = ReadingSession.builder().wpm(300).comprehensionScore(90).build();
    ReadingSession second = ReadingSession.builder().wpm(300).comprehensionScore(80).build();
    when(sessions.findTop5ByUserIdOrderByDateDesc(1L)).thenReturn(List.of(first, second));

    int result = service.getRecommendedWpm(1L);

    assertEquals(330, result);
  }

  // Was already green, used to test boundaries. Edges.
  @ParameterizedTest
  @CsvSource({ "80, 330", "79, 300" })
  @DisplayName("getRecommendedWpm speeds up from a mean score of exactly 80")
  void recommendedBoundaryAtScore80(int score, int expected) {
    ReadingSession session = ReadingSession.builder().wpm(300).comprehensionScore(score).build();
    when(sessions.findTop5ByUserIdOrderByDateDesc(1L)).thenReturn(List.of(session));

    int result = service.getRecommendedWpm(1L);

    assertEquals(expected, result);
  }

  @ParameterizedTest
  @CsvSource({ "59, 270", "60, 300" })
  @DisplayName("getRecommendedWpm slows down when the mean score is below 60")
  void recommendedSlowsDownForBadComprehension(int score, int expected) {
    ReadingSession session = ReadingSession.builder().wpm(300).comprehensionScore(score).build();
    when(sessions.findTop5ByUserIdOrderByDateDesc(1L)).thenReturn(List.of(session));

    int result = service.getRecommendedWpm(1L);

    assertEquals(expected, result);
  }

  @Test
  @DisplayName("getRecommendedWpm ignores sessions without a score")
  void recommendedIgnoresMissingScores() {
    ReadingSession scored = ReadingSession.builder().wpm(300).comprehensionScore(90).build();
    ReadingSession unscored = ReadingSession.builder().wpm(300).build();
    when(sessions.findTop5ByUserIdOrderByDateDesc(1L)).thenReturn(List.of(scored, unscored));

    int result = service.getRecommendedWpm(1L);

    assertEquals(330, result);
  }

  @Test
  @DisplayName("getRecommendedWpm never recommends more than the maximum")
  void recommendedIsClampedToMax() {
    ReadingSession session = ReadingSession.builder().wpm(1500).comprehensionScore(100).build();
    when(sessions.findTop5ByUserIdOrderByDateDesc(1L)).thenReturn(List.of(session));

    int result = service.getRecommendedWpm(1L);

    assertEquals(WpmLimits.MAX_WPM, result);
  }

  @Test
  @DisplayName("getRecommendedWpm never recommends less than the minimum")
  void recommendedIsClampedToMin() {
    ReadingSession session = ReadingSession.builder().wpm(60).comprehensionScore(0).build();
    when(sessions.findTop5ByUserIdOrderByDateDesc(1L)).thenReturn(List.of(session));

    int result = service.getRecommendedWpm(1L);

    assertEquals(WpmLimits.MIN_WPM, result);
  }
}
