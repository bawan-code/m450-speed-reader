package bbzbl_it_m450.speedreader.service.dto;

import java.util.List;

public record ReadingPlan(
        List<WordTiming> words,
        int wpm,
        long totalDurationMs) {
}
