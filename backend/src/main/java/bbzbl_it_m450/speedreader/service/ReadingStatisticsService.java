package bbzbl_it_m450.speedreader.service;

import java.util.List;
import java.util.OptionalDouble;

import org.springframework.stereotype.Service;

import bbzbl_it_m450.speedreader.model.ReadingSession;
import bbzbl_it_m450.speedreader.repository.ReadingSessionRepository;

@Service
public class ReadingStatisticsService {
  private final ReadingSessionRepository sessions;

  public ReadingStatisticsService(ReadingSessionRepository sessions) {
    this.sessions = sessions;
  }

  public OptionalDouble getAverageWpm(Long userId) {
    List<ReadingSession> list = sessions.findByUserId(userId);
    if (list.isEmpty()) {
      return OptionalDouble.empty();
    }

    int totalWords = 0;
    int totalSeconds = 0;
    for (ReadingSession session : list) {
      totalWords += session.getWordsRead();
      totalSeconds += session.getDurationSeconds();
    }

    double totalMinutes = totalSeconds / 60.0;
    return OptionalDouble.of(totalWords / totalMinutes);
  }
}
