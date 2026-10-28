package bbzbl_it_m450.speedreader.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class ReadingSessionServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long TEXT_ID = 10L;
    private static final int WORD_COUNT = 100;
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-11-02T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 2, 10, 0);

    @Mock
    private ReadingSessionRepository sessions;

    @Mock
    private UserRepository users;

    @Mock
    private TextRepository texts;

    @Mock
    private BookmarkRepository bookmarks;

    @Captor
    private ArgumentCaptor<ReadingSession> sessionCaptor;

    @Captor
    private ArgumentCaptor<Bookmark> bookmarkCaptor;

    private ReadingSessionService service;
    private User user;
    private Text text;

    @BeforeEach
    void setUp() {
        service = new ReadingSessionService(sessions, users, texts, bookmarks, FIXED_CLOCK);
        user = User.builder().id(USER_ID).username("noam").email("noam@example.com").build();
        text = Text.builder().id(TEXT_ID).title("Test text").content("...").wordCount(WORD_COUNT).build();
    }

    private static RecordSessionRequest request(int wpm, int wordsRead, int durationSeconds, Integer score) {
        return new RecordSessionRequest(USER_ID, TEXT_ID, wpm, wordsRead, durationSeconds, score);
    }

    private static RecordSessionRequest validRequest() {
        return request(300, 50, 120, 80);
    }

    private void stubUserAndText() {
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(texts.findById(TEXT_ID)).thenReturn(Optional.of(text));
    }

    private ReadingSession captureSavedSession() {
        verify(sessions).save(sessionCaptor.capture());
        return sessionCaptor.getValue();
    }

    @Nested
    @DisplayName("recordSession - happy path")
    class RecordSessionHappyPath {

        @Test
        @DisplayName("saves a session with all fields from the request and the date from the clock")
        void savesSessionWithAllFields() {
            stubUserAndText();

            service.recordSession(validRequest());

            ReadingSession saved = captureSavedSession();
            assertSame(user, saved.getUser());
            assertSame(text, saved.getText());
            assertThat(saved.getWpm()).isEqualTo(300);
            assertThat(saved.getWordsRead()).isEqualTo(50);
            assertThat(saved.getDurationSeconds()).isEqualTo(120);
            assertThat(saved.getComprehensionScore()).isEqualTo(80);
            assertThat(saved.getDate()).isEqualTo(NOW);
            assertThat(saved.getId()).isNull();
        }

        @Test
        @DisplayName("returns exactly the object returned by the repository")
        void returnsSavedInstance() {
            stubUserAndText();
            ReadingSession persisted = ReadingSession.builder().id(99L).build();
            when(sessions.save(any(ReadingSession.class))).thenReturn(persisted);

            ReadingSession result = service.recordSession(validRequest());

            assertSame(persisted, result);
        }

        @Test
        @DisplayName("accepts a missing comprehension score and stores it as null")
        void acceptsNullScore() {
            stubUserAndText();

            service.recordSession(request(300, 50, 120, null));

            assertThat(captureSavedSession().getComprehensionScore()).isNull();
        }
    }

    @Nested
    @DisplayName("recordSession - validation")
    class RecordSessionValidation {

        @ParameterizedTest(name = "wpm = {0} is rejected")
        @ValueSource(ints = {59, 1501})
        void rejectsWpmOutOfRange(int wpm) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.recordSession(request(wpm, 50, 120, 80)));

            verifyNoInteractions(sessions, users, texts, bookmarks);
        }

        @ParameterizedTest(name = "wpm = {0} is accepted")
        @ValueSource(ints = {60, 1500})
        void acceptsWpmAtBoundaries(int wpm) {
            stubUserAndText();

            service.recordSession(request(wpm, 50, 120, 80));

            assertThat(captureSavedSession().getWpm()).isEqualTo(wpm);
        }

        @Test
        @DisplayName("wordsRead = -1 is rejected")
        void rejectsNegativeWordsRead() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.recordSession(request(300, -1, 120, 80)));

            verifyNoInteractions(sessions, users, texts, bookmarks);
        }

        @Test
        @DisplayName("wordsRead = 0 is accepted")
        void acceptsZeroWordsRead() {
            stubUserAndText();

            service.recordSession(request(300, 0, 120, 80));

            assertThat(captureSavedSession().getWordsRead()).isZero();
        }

        @Test
        @DisplayName("durationSeconds = 0 is rejected")
        void rejectsZeroDuration() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.recordSession(request(300, 50, 0, 80)));

            verifyNoInteractions(sessions, users, texts, bookmarks);
        }

        @Test
        @DisplayName("durationSeconds = 1 is accepted")
        void acceptsDurationOfOneSecond() {
            stubUserAndText();

            service.recordSession(request(300, 50, 1, 80));

            assertThat(captureSavedSession().getDurationSeconds()).isEqualTo(1);
        }

        @ParameterizedTest(name = "comprehensionScore = {0} is rejected")
        @ValueSource(ints = {-1, 101})
        void rejectsScoreOutOfRange(int score) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.recordSession(request(300, 50, 120, score)));

            verifyNoInteractions(sessions, users, texts, bookmarks);
        }

        @ParameterizedTest(name = "comprehensionScore = {0} is accepted")
        @ValueSource(ints = {0, 100})
        void acceptsScoreAtBoundaries(int score) {
            stubUserAndText();

            service.recordSession(request(300, 50, 120, score));

            assertThat(captureSavedSession().getComprehensionScore()).isEqualTo(score);
        }
    }

    @Nested
    @DisplayName("recordSession - lookups")
    class RecordSessionLookups {

        @Test
        @DisplayName("unknown user throws and nothing else is touched")
        void unknownUser() {
            when(users.findById(USER_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> service.recordSession(validRequest()));

            assertThat(exception.getMessage()).contains(String.valueOf(USER_ID));
            verifyNoInteractions(texts, sessions, bookmarks);
        }

        @Test
        @DisplayName("unknown text throws and nothing is saved")
        void unknownText() {
            when(users.findById(USER_ID)).thenReturn(Optional.of(user));
            when(texts.findById(TEXT_ID)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> service.recordSession(validRequest()));

            assertThat(exception.getMessage()).contains(String.valueOf(TEXT_ID));
            verifyNoInteractions(sessions, bookmarks);
        }

        @Test
        @DisplayName("wordsRead greater than the word count is rejected")
        void wordsReadAboveWordCount() {
            stubUserAndText();

            assertThrows(IllegalArgumentException.class,
                    () -> service.recordSession(request(300, WORD_COUNT + 1, 120, 80)));

            verifyNoInteractions(sessions, bookmarks);
        }

        @Test
        @DisplayName("wordsRead equal to the word count is accepted")
        void wordsReadEqualToWordCount() {
            stubUserAndText();

            service.recordSession(request(300, WORD_COUNT, 120, 80));

            assertThat(captureSavedSession().getWordsRead()).isEqualTo(WORD_COUNT);
        }
    }

    @Nested
    @DisplayName("recordSession - bookmark")
    class RecordSessionBookmark {

        private Bookmark existingBookmarkAt(int position) {
            Bookmark existing = Bookmark.builder().id(7L).user(user).text(text).wordPosition(position).build();
            when(bookmarks.findByUserIdAndTextId(USER_ID, TEXT_ID)).thenReturn(Optional.of(existing));
            return existing;
        }

        private Bookmark captureSavedBookmark() {
            verify(bookmarks).save(bookmarkCaptor.capture());
            return bookmarkCaptor.getValue();
        }

        @Test
        @DisplayName("creates a new bookmark when none exists")
        void createsNewBookmark() {
            stubUserAndText();
            when(bookmarks.findByUserIdAndTextId(USER_ID, TEXT_ID)).thenReturn(Optional.empty());

            service.recordSession(validRequest());

            Bookmark saved = captureSavedBookmark();
            assertThat(saved.getId()).isNull();
            assertSame(user, saved.getUser());
            assertSame(text, saved.getText());
            assertThat(saved.getWordPosition()).isEqualTo(50);
        }

        @Test
        @DisplayName("moves an existing bookmark forward by wordsRead")
        void updatesExistingBookmark() {
            stubUserAndText();
            Bookmark existing = existingBookmarkAt(30);

            service.recordSession(validRequest());

            Bookmark saved = captureSavedBookmark();
            assertSame(existing, saved);
            assertThat(saved.getWordPosition()).isEqualTo(80);
        }

        @Test
        @DisplayName("clamps the bookmark to the word count")
        void clampsBookmarkToWordCount() {
            stubUserAndText();
            existingBookmarkAt(70);

            service.recordSession(validRequest());

            assertThat(captureSavedBookmark().getWordPosition()).isEqualTo(WORD_COUNT);
        }

        @Test
        @DisplayName("reaches exactly the word count without clamping")
        void reachesWordCountExactly() {
            stubUserAndText();
            existingBookmarkAt(50);

            service.recordSession(validRequest());

            assertThat(captureSavedBookmark().getWordPosition()).isEqualTo(WORD_COUNT);
        }

        @Test
        @DisplayName("does not touch bookmarks when no words were read")
        void noBookmarkWhenNothingRead() {
            stubUserAndText();

            service.recordSession(request(300, 0, 120, 80));

            verify(sessions).save(any(ReadingSession.class));
            verifyNoInteractions(bookmarks);
        }
    }

    @Nested
    @DisplayName("getSession")
    class GetSession {

        @Test
        @DisplayName("returns the session when it exists")
        void returnsSession() {
            ReadingSession session = ReadingSession.builder().id(5L).build();
            when(sessions.findById(5L)).thenReturn(Optional.of(session));

            assertSame(session, service.getSession(5L));
        }

        @Test
        @DisplayName("throws with the id in the message when the session is missing")
        void throwsWhenMissing() {
            when(sessions.findById(5L)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> service.getSession(5L));

            assertThat(exception.getMessage()).contains("5");
        }
    }

    @Nested
    @DisplayName("getSessionsForUser")
    class GetSessionsForUser {

        @Test
        @DisplayName("returns the sessions from the repository, newest first")
        void returnsSessions() {
            List<ReadingSession> list = List.of(
                    ReadingSession.builder().id(2L).build(),
                    ReadingSession.builder().id(1L).build());
            when(users.existsById(USER_ID)).thenReturn(true);
            when(sessions.findByUserIdOrderByDateDesc(USER_ID)).thenReturn(list);

            List<ReadingSession> result = service.getSessionsForUser(USER_ID);

            assertSame(list, result);
            verify(sessions).findByUserIdOrderByDateDesc(USER_ID);
        }

        @Test
        @DisplayName("returns an empty list when the user has no sessions")
        void returnsEmptyList() {
            when(users.existsById(USER_ID)).thenReturn(true);
            when(sessions.findByUserIdOrderByDateDesc(USER_ID)).thenReturn(List.of());

            assertThat(service.getSessionsForUser(USER_ID)).isEmpty();
        }

        @Test
        @DisplayName("throws for an unknown user and never queries sessions")
        void throwsForUnknownUser() {
            when(users.existsById(USER_ID)).thenReturn(false);

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> service.getSessionsForUser(USER_ID));

            assertThat(exception.getMessage()).contains(String.valueOf(USER_ID));
            verifyNoInteractions(sessions);
        }
    }

    @Nested
    @DisplayName("deleteSession")
    class DeleteSession {

        @Test
        @DisplayName("deletes the session when it exists")
        void deletesExistingSession() {
            when(sessions.existsById(5L)).thenReturn(true);

            service.deleteSession(5L);

            verify(sessions).deleteById(5L);
        }

        @Test
        @DisplayName("throws and never deletes when the session is missing")
        void throwsWhenMissing() {
            when(sessions.existsById(5L)).thenReturn(false);

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> service.deleteSession(5L));

            assertThat(exception.getMessage()).contains("5");
            verify(sessions, never()).deleteById(any());
        }
    }
}
