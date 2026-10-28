package bbzbl_it_m450.speedreader.service;

import static bbzbl_it_m450.speedreader.common.WpmLimits.MAX_WPM;
import static bbzbl_it_m450.speedreader.common.WpmLimits.MIN_WPM;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import bbzbl_it_m450.speedreader.common.ResourceNotFoundException;
import bbzbl_it_m450.speedreader.model.Bookmark;
import bbzbl_it_m450.speedreader.model.ReadingSession;
import bbzbl_it_m450.speedreader.model.Text;
import bbzbl_it_m450.speedreader.model.User;
import bbzbl_it_m450.speedreader.repository.BookmarkRepository;
import bbzbl_it_m450.speedreader.repository.ReadingSessionRepository;
import bbzbl_it_m450.speedreader.repository.TextRepository;
import bbzbl_it_m450.speedreader.repository.UserRepository;
import bbzbl_it_m450.speedreader.service.dto.RecordSessionRequest;

@Service
public class ReadingSessionService {

    private static final int MIN_SCORE = 0;
    private static final int MAX_SCORE = 100;

    private final ReadingSessionRepository sessions;
    private final UserRepository users;
    private final TextRepository texts;
    private final BookmarkRepository bookmarks;
    private final Clock clock;

    public ReadingSessionService(ReadingSessionRepository sessions, UserRepository users,
                                 TextRepository texts, BookmarkRepository bookmarks, Clock clock) {
        this.sessions = sessions;
        this.users = users;
        this.texts = texts;
        this.bookmarks = bookmarks;
        this.clock = clock;
    }

    public ReadingSession recordSession(RecordSessionRequest request) {
        validate(request);

        User user = users.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.userId()));
        Text text = texts.findById(request.textId())
                .orElseThrow(() -> new ResourceNotFoundException("Text not found: " + request.textId()));

        if (request.wordsRead() > text.getWordCount()) {
            throw new IllegalArgumentException("wordsRead must not exceed the word count of the text");
        }

        ReadingSession session = ReadingSession.builder()
                .user(user)
                .text(text)
                .wpm(request.wpm())
                .wordsRead(request.wordsRead())
                .durationSeconds(request.durationSeconds())
                .comprehensionScore(request.comprehensionScore())
                .date(LocalDateTime.now(clock))
                .build();
        ReadingSession saved = sessions.save(session);

        updateBookmark(user, text, request.wordsRead());

        return saved;
    }

    public ReadingSession getSession(Long sessionId) {
        return sessions.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Reading session not found: " + sessionId));
    }

    public List<ReadingSession> getSessionsForUser(Long userId) {
        if (!users.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        return sessions.findByUserIdOrderByDateDesc(userId);
    }

    public void deleteSession(Long sessionId) {
        if (!sessions.existsById(sessionId)) {
            throw new ResourceNotFoundException("Reading session not found: " + sessionId);
        }
        sessions.deleteById(sessionId);
    }

    private void validate(RecordSessionRequest request) {
        if (request.wpm() < MIN_WPM || request.wpm() > MAX_WPM) {
            throw new IllegalArgumentException("wpm must be between " + MIN_WPM + " and " + MAX_WPM);
        }
        if (request.wordsRead() < 0) {
            throw new IllegalArgumentException("wordsRead must not be negative");
        }
        if (request.durationSeconds() <= 0) {
            throw new IllegalArgumentException("durationSeconds must be positive");
        }
        Integer score = request.comprehensionScore();
        if (score != null && (score < MIN_SCORE || score > MAX_SCORE)) {
            throw new IllegalArgumentException("comprehensionScore must be between " + MIN_SCORE + " and " + MAX_SCORE);
        }
    }

    private void updateBookmark(User user, Text text, int wordsRead) {
        if (wordsRead == 0) {
            return;
        }
        Bookmark bookmark = bookmarks.findByUserIdAndTextId(user.getId(), text.getId())
                .map(existing -> {
                    existing.setWordPosition(Math.min(existing.getWordPosition() + wordsRead, text.getWordCount()));
                    return existing;
                })
                .orElseGet(() -> Bookmark.builder()
                        .user(user)
                        .text(text)
                        .wordPosition(wordsRead)
                        .build());
        bookmarks.save(bookmark);
    }
}
