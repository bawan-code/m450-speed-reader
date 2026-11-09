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
}
