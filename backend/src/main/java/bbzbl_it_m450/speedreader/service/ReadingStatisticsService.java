package bbzbl_it_m450.speedreader.service;

import org.springframework.stereotype.Service;

import java.util.OptionalDouble;
import bbzbl_it_m450.speedreader.repository.ReadingSessionRepository;

import java.util.List;
import bbzbl_it_m450.speedreader.model.ReadingSession;

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

    ReadingSession session = list.get(0);
    double minutes = session.getDurationSeconds() / 60.0;

    return OptionalDouble.of(session.getWordsRead() / minutes);
  }
}
