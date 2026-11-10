package bbzbl_it_m450.speedreader.service;

import java.util.List;
import java.util.OptionalDouble;

import org.springframework.stereotype.Service;

import bbzbl_it_m450.speedreader.common.WpmLimits;
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

  public int getRecommendedWpm(Long userId) {
    List<ReadingSession> recent = sessions.findTop5ByUserIdOrderByDateDesc(userId);
    if (recent.isEmpty()) {
      return WpmLimits.DEFAULT_WPM;
    }

    int totalWpm = 0;
    for (ReadingSession session : recent) {
      totalWpm += session.getWpm();
    }

    double average = (double) totalWpm / recent.size();
    OptionalDouble score = averageScore(recent);

    if (score.isPresent() && score.getAsDouble() >= 80) {
      average = average * 1.10;
    } else if (score.isPresent() && score.getAsDouble() < 60) {
      average = average * 0.90;
    }
    int rounded = (int) Math.round(average / 10.0) * 10;
    return Math.min(rounded, WpmLimits.MAX_WPM);
  }

  // Helper function for getRecommendedWpm. Handles null, so the tests stay green.
  private OptionalDouble averageScore(List<ReadingSession> recent) {
    int total = 0;
    int count = 0;
    for (ReadingSession session : recent) {
      Integer score = session.getComprehensionScore();
      if (score != null) {
        total += score;
        count++;
      }
    }
    if (count == 0) {
      return OptionalDouble.empty();
    }
    return OptionalDouble.of((double) total / count);
  }
}
