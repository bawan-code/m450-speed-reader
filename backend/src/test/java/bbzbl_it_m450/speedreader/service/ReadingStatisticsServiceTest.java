package bbzbl_it_m450.speedreader.service;

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
}
