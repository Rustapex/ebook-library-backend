package com.acorn.abc.rental.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ReadingProgressEpubTest {

    @Test
    void updateLocation_andCompleteAtEnd_updatesEpubStateOnce() {
        ReadingProgress progress = ReadingProgress.start(mock(BookRental.class), 3);
        LocalDateTime viewedAt = LocalDateTime.of(2026, 7, 18, 12, 0);

        progress.updateLocation(3, "epubcfi(/6/4!/4/2)", "chapter-1.xhtml", 0,
                new BigDecimal("0.99000"), viewedAt);
        boolean first = progress.completeIfEpubEnd(true, new BigDecimal("0.99000"), viewedAt);
        boolean second = progress.completeIfEpubEnd(true, BigDecimal.ONE, viewedAt.plusMinutes(1));

        assertThat(progress.getEpubVersion()).isEqualTo(3);
        assertThat(progress.getLocationCfi()).isEqualTo("epubcfi(/6/4!/4/2)");
        assertThat(progress.getProgression()).isEqualByComparingTo("0.99000");
        assertThat(first).isTrue();
        assertThat(second).isFalse();
        assertThat(progress.getCompletedAt()).isEqualTo(viewedAt);
    }

    @Test
    void completeAtEnd_belowThreshold_doesNotComplete() {
        ReadingProgress progress = ReadingProgress.start(mock(BookRental.class), 1);

        boolean completed = progress.completeIfEpubEnd(
                true, new BigDecimal("0.97999"), LocalDateTime.now());

        assertThat(completed).isFalse();
        assertThat(progress.getCompletedYn()).isFalse();
    }
}
