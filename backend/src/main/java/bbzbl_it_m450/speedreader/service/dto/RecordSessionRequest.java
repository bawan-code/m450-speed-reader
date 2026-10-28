package bbzbl_it_m450.speedreader.service.dto;

public record RecordSessionRequest(
        Long userId,
        Long textId,
        int wpm,
        int wordsRead,
        int durationSeconds,
        Integer comprehensionScore) {
}
