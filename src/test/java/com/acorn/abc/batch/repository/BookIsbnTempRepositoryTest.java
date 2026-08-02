package com.acorn.abc.batch.repository;

import com.acorn.abc.batch.domain.BookIsbnTemp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookIsbnTempRepositoryTest {

    private static final AtomicLong ISBN_SEQUENCE = new AtomicLong(9787000000000L);

    @Autowired
    private BookIsbnTempRepository bookIsbnTempRepository;

    @Test
    void findAllByIdInForUpdate_returnsRowsInAscendingIdOrder() {
        BookIsbnTemp first = bookIsbnTempRepository.saveAndFlush(pending());
        BookIsbnTemp second = bookIsbnTempRepository.saveAndFlush(pending());

        List<BookIsbnTemp> found = bookIsbnTempRepository.findAllByIdInForUpdate(
                List.of(second.getId(), first.getId()));

        assertThat(found).extracting(BookIsbnTemp::getId)
                .containsExactly(first.getId(), second.getId());
    }

    @Test
    void deleteAllByIdInAndStatusCd_deletesOnlyPendingRows() {
        BookIsbnTemp pending = bookIsbnTempRepository.saveAndFlush(pending());
        BookIsbnTemp ready = pending();
        ready.markReady();
        ready = bookIsbnTempRepository.saveAndFlush(ready);
        BookIsbnTemp merged = pending();
        merged.markMerged();
        merged = bookIsbnTempRepository.saveAndFlush(merged);

        int deleted = bookIsbnTempRepository.deleteAllByIdInAndStatusCd(
                List.of(pending.getId(), ready.getId(), merged.getId()),
                BookIsbnTemp.STATUS_PENDING);

        assertThat(deleted).isEqualTo(1);
        assertThat(bookIsbnTempRepository.findById(pending.getId())).isEmpty();
        assertThat(bookIsbnTempRepository.findById(ready.getId())).isPresent();
        assertThat(bookIsbnTempRepository.findById(merged.getId())).isPresent();
    }

    private BookIsbnTemp pending() {
        return BookIsbnTemp.create(Long.toString(ISBN_SEQUENCE.incrementAndGet()));
    }
}
