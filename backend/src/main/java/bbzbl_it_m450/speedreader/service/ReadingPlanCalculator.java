package bbzbl_it_m450.speedreader.service;

import static bbzbl_it_m450.speedreader.common.WpmLimits.MAX_WPM;
import static bbzbl_it_m450.speedreader.common.WpmLimits.MIN_WPM;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import bbzbl_it_m450.speedreader.service.dto.ReadingPlan;
import bbzbl_it_m450.speedreader.service.dto.WordTiming;

@Component
public class ReadingPlanCalculator {

    private static final double MS_PER_MINUTE = 60000.0;
    private static final String SURROUNDING_PUNCTUATION = "^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$";

    public ReadingPlan createPlan(String text, int wpm) {
        validateWpm(wpm);

        List<WordTiming> words = tokenize(text).stream()
                .map(word -> new WordTiming(word, orpIndex(word), delayMs(word, wpm)))
                .toList();
        long totalDurationMs = words.stream().mapToLong(WordTiming::displayMs).sum();

        return new ReadingPlan(words, wpm, totalDurationMs);
    }

    public List<String> tokenize(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        return Arrays.stream(text.split("\\s+"))
                .filter(token -> !core(token).isEmpty())
                .toList();
    }

    public int orpIndex(String word) {
        String core = core(word);
        return word.indexOf(core) + orpOffset(core.length());
    }

    public long delayMs(String word, int wpm) {
        validateWpm(wpm);

        String core = core(word);
        double factor = 1.0;
        if (word.matches(".*[.!?]")) {
            factor += 1.0;
        }
        if (word.matches(".*[,;:]")) {
            factor += 0.5;
        }
        if (core.length() > 8) {
            factor += 0.5;
        }
        if (core.chars().anyMatch(Character::isDigit)) {
            factor += 0.5;
        }
        return Math.round(MS_PER_MINUTE / wpm * factor);
    }

    private void validateWpm(int wpm) {
        if (wpm < MIN_WPM || wpm > MAX_WPM) {
            throw new IllegalArgumentException("wpm must be between " + MIN_WPM + " and " + MAX_WPM);
        }
    }

    private String core(String word) {
        return word.replaceAll(SURROUNDING_PUNCTUATION, "");
    }

    private int orpOffset(int coreLength) {
        if (coreLength <= 1) {
            return 0;
        }
        if (coreLength <= 5) {
            return 1;
        }
        if (coreLength <= 9) {
            return 2;
        }
        if (coreLength <= 13) {
            return 3;
        }
        return 4;
    }
}
