package bbzbl_it_m450.speedreader.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import bbzbl_it_m450.speedreader.service.dto.ReadingPlan;
import bbzbl_it_m450.speedreader.service.dto.WordTiming;

class ReadingPlanCalculatorTest {

    private final ReadingPlanCalculator calculator = new ReadingPlanCalculator();

    @Nested
    @DisplayName("tokenize")
    class Tokenize {

        @Test
        @DisplayName("splits on a single space")
        void splitsOnSpace() {
            assertThat(calculator.tokenize("Hello world")).containsExactly("Hello", "world");
        }

        @Test
        @DisplayName("ignores multiple, leading and trailing spaces")
        void ignoresExtraSpaces() {
            assertThat(calculator.tokenize("  Hello   world  ")).containsExactly("Hello", "world");
        }

        @Test
        @DisplayName("splits on tabs and newlines")
        void splitsOnTabsAndNewlines() {
            assertThat(calculator.tokenize("Hello\tworld\nagain")).containsExactly("Hello", "world", "again");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\t\n"})
        @DisplayName("returns an empty list for blank text")
        void returnsEmptyListForBlankText(String text) {
            assertThat(calculator.tokenize(text)).isEmpty();
        }

        @Test
        @DisplayName("rejects null")
        void rejectsNull() {
            assertThrows(IllegalArgumentException.class, () -> calculator.tokenize(null));
        }

        @Test
        @DisplayName("keeps hyphenated words as one token")
        void keepsHyphenatedWords() {
            assertThat(calculator.tokenize("well-known fact")).containsExactly("well-known", "fact");
        }

        @Test
        @DisplayName("keeps numbers as tokens")
        void keepsNumbers() {
            assertThat(calculator.tokenize("In 2024 pi was 3.14")).containsExactly("In", "2024", "pi", "was", "3.14");
        }

        @Test
        @DisplayName("drops tokens without a letter or digit")
        void dropsPunctuationOnlyTokens() {
            assertThat(calculator.tokenize("wait - what ... – ok")).containsExactly("wait", "what", "ok");
        }

        @Test
        @DisplayName("keeps punctuation attached to the word")
        void keepsAttachedPunctuation() {
            assertThat(calculator.tokenize("Hello, world!")).containsExactly("Hello,", "world!");
        }

        @Test
        @DisplayName("keeps a single letter and umlauts")
        void keepsSingleLetterAndUmlauts() {
            assertThat(calculator.tokenize("a für")).containsExactly("a", "für");
        }
    }

    @Nested
    @DisplayName("orpIndex")
    class OrpIndex {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "a, 0",
                "to, 1",
                "hello, 1",
                "planet, 2",
                "wonderful, 2",
                "everything, 3",
                "extraordinary, 3",
                "internationals, 4",
                "internationally, 4"
        })
        @DisplayName("uses the offset for the core length")
        void usesOffsetForCoreLength(String word, int expected) {
            assertEquals(expected, calculator.orpIndex(word));
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource(delimiter = '|', value = {
                "(hello | 2",
                "\"hello\" | 2",
                "((hello | 3",
                "hello. | 1",
                "\"a\" | 1",
                "für! | 1"
        })
        @DisplayName("adds leading punctuation, ignores trailing punctuation")
        void countsOnlyLeadingPunctuation(String word, int expected) {
            assertEquals(expected, calculator.orpIndex(word));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "..."})
        @DisplayName("returns the first position for a word without a core")
        void returnsZeroWithoutCore(String word) {
            assertEquals(0, calculator.orpIndex(word));
        }
    }

    @Nested
    @DisplayName("delayMs")
    class DelayMs {

        @ParameterizedTest(name = "{0} at {1} WPM -> {2} ms")
        @CsvSource({
                "word, 300, 200",
                "word, 600, 100",
                "word, 700, 86",
                "word, 60, 1000",
                "word, 1500, 40"
        })
        @DisplayName("base delay depends on the WPM and is rounded")
        void baseDelay(String word, int wpm, long expected) {
            assertEquals(expected, calculator.delayMs(word, wpm));
        }

        @ParameterizedTest
        @ValueSource(strings = {"word.", "word!", "word?"})
        @DisplayName("doubles the delay at a sentence end")
        void sentenceEnd(String word) {
            assertEquals(400, calculator.delayMs(word, 300));
        }

        @ParameterizedTest
        @ValueSource(strings = {"word,", "word;", "word:"})
        @DisplayName("adds half the base delay at a clause pause")
        void clausePause(String word) {
            assertEquals(300, calculator.delayMs(word, 300));
        }

        @ParameterizedTest
        @ValueSource(strings = {".word", ",word", "wo.rd", "wo,rd"})
        @DisplayName("ignores punctuation that is not at the end")
        void ignoresPunctuationNotAtEnd(String word) {
            assertEquals(200, calculator.delayMs(word, 300));
        }

        @ParameterizedTest(name = "{0} -> {1} ms")
        @CsvSource({
                "eightchr, 200",
                "ninechars, 300"
        })
        @DisplayName("adds half the base delay for words longer than 8 characters")
        void longWord(String word, long expected) {
            assertEquals(expected, calculator.delayMs(word, 300));
        }

        @Test
        @DisplayName("measures the length without surrounding punctuation")
        void lengthIgnoresPunctuation() {
            assertEquals(200, calculator.delayMs("(eightchr)", 300));
        }

        @ParameterizedTest
        @ValueSource(strings = {"2024", "a1"})
        @DisplayName("adds half the base delay for words with a digit")
        void digit(String word) {
            assertEquals(300, calculator.delayMs(word, 300));
        }

        @ParameterizedTest(name = "{0} -> {1} ms")
        @CsvSource(delimiter = '|', value = {
                "extraordinary. | 500",
                "extraordinary, | 400",
                "2024. | 500",
                "2024, | 400",
                "1234567890. | 600"
        })
        @DisplayName("adds up all factors")
        void addsFactors(String word, long expected) {
            assertEquals(expected, calculator.delayMs(word, 300));
        }

        @ParameterizedTest
        @ValueSource(ints = {59, 1501, 0, -1})
        @DisplayName("rejects a WPM outside the limits")
        void rejectsInvalidWpm(int wpm) {
            assertThrows(IllegalArgumentException.class, () -> calculator.delayMs("word", wpm));
        }
    }

    @Nested
    @DisplayName("createPlan")
    class CreatePlan {

        @Test
        @DisplayName("creates one timing per word with the total duration")
        void createsPlan() {
            ReadingPlan plan = calculator.createPlan("Hi there.", 300);

            assertEquals(new ReadingPlan(
                    List.of(new WordTiming("Hi", 1, 200), new WordTiming("there.", 1, 400)), 300, 600), plan);
        }

        @Test
        @DisplayName("keeps the order of the words")
        void keepsOrder() {
            ReadingPlan plan = calculator.createPlan("one two three four", 300);

            assertThat(plan.words()).extracting(WordTiming::word).containsExactly("one", "two", "three", "four");
        }

        @Test
        @DisplayName("uses the given WPM for the delays")
        void usesGivenWpm() {
            ReadingPlan plan = calculator.createPlan("word", 600);

            assertEquals(new ReadingPlan(List.of(new WordTiming("word", 1, 100)), 600, 100), plan);
        }

        @Test
        @DisplayName("returns an empty plan for empty text")
        void emptyText() {
            assertEquals(new ReadingPlan(List.of(), 300, 0), calculator.createPlan("", 300));
        }

        @Test
        @DisplayName("rejects null text")
        void rejectsNullText() {
            assertThrows(IllegalArgumentException.class, () -> calculator.createPlan(null, 300));
        }

        @Test
        @DisplayName("rejects an invalid WPM")
        void rejectsInvalidWpm() {
            assertThrows(IllegalArgumentException.class, () -> calculator.createPlan("word", 59));
        }

        @Test
        @DisplayName("rejects an invalid WPM even if the text is empty")
        void rejectsInvalidWpmWithEmptyText() {
            assertThrows(IllegalArgumentException.class, () -> calculator.createPlan("", 1501));
        }
    }
}
