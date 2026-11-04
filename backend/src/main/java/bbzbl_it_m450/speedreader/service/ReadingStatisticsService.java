package bbzbl_it_m450.speedreader.service;

import org.springframework.stereotype.Service;
import java.util.OptionalDouble;
import bbzbl_it_m450.speedreader.repository.ReadingSessionRepository;

@Service
public class ReadingStatisticsService {
  private final ReadingSessionRepository sessions;

  public ReadingStatisticsService(ReadingSessionRepository sessions) {
    this.sessions = sessions;
  }

  public OptionalDouble getAverageWpm(Long userId) {
    sessions.findByUserId(userId);
    return OptionalDouble.empty();
  }
}
