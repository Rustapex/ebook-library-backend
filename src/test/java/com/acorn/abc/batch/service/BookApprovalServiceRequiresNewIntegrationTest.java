package com.acorn.abc.batch.service;

import com.acorn.abc.admin.book.dto.BookIsbnTempApproveRequest;
import com.acorn.abc.admin.book.dto.BookIsbnTempApproveResponse;
import com.acorn.abc.batch.domain.BookIsbnTemp;
import com.acorn.abc.batch.repository.BookIsbnTempRepository;
import com.acorn.abc.book.domain.Book;
import com.acorn.abc.book.repository.BookAuthorRepository;
import com.acorn.abc.book.repository.BookCategoryRepository;
import com.acorn.abc.book.repository.BookDetailRepository;
import com.acorn.abc.book.repository.BookRepository;
import com.acorn.abc.category.repository.CategoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BookApprovalServiceTest는 new BookApprovalService(...)로 직접 생성해 @Transactional이
 * 적용되지 않으므로 REQUIRES_NEW 트랜잭션 경계를 실제로 통과하지 않는다.
 * 이 클래스는 실제 Spring이 관리하는 빈(프록시)을 그대로 호출해, approve()의 REQUIRED
 * 트랜잭션과 알라딘 category 자동 생성의 REQUIRES_NEW가 서로 다른 물리 트랜잭션으로
 * 분리되는 운영 조건을 그대로 재현한다.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookApprovalServiceRequiresNewIntegrationTest {

    @Autowired private BookApprovalService bookApprovalService;
    @Autowired private BookIsbnTempRepository bookIsbnTempRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private BookCategoryRepository bookCategoryRepository;
    @Autowired private BookAuthorRepository bookAuthorRepository;
    @Autowired private BookDetailRepository bookDetailRepository;
    @Autowired private CategoryRepository categoryRepository;

    private String isbn;
    private String aladinCategoryId;

    @AfterEach
    void cleanUp() {
        if (isbn != null) {
            bookRepository.findByIsbn(isbn).ifPresent(book -> {
                bookCategoryRepository.deleteAll(bookCategoryRepository.findActiveByBookId(book.getBookId()));
                bookAuthorRepository.deleteAll(bookAuthorRepository.findActiveByBookId(book.getBookId()));
                bookDetailRepository.findByBook_BookId(book.getBookId()).ifPresent(bookDetailRepository::delete);
                bookRepository.delete(book);
            });
            bookIsbnTempRepository.findByIsbn13(isbn).ifPresent(bookIsbnTempRepository::delete);
        }
        if (aladinCategoryId != null) {
            categoryRepository.findByAladinCategoryId(aladinCategoryId).ifPresent(categoryRepository::delete);
        }
    }

    @Test
    void approve_freshAladinCategory_succeedsOnFirstAttempt_viaRealRequiresNewBoundary() {
        isbn = uniqueIsbn13();
        aladinCategoryId = "IT-" + UUID.randomUUID().toString().substring(0, 8);

        BookIsbnTemp temp = BookIsbnTemp.create(isbn);
        temp.applyMetadata("실DB REQUIRES_NEW 재현", "테스트출판사", "테스트저자", null, null, null,
                aladinCategoryId, "국내도서 > 컴퓨터/모바일 > 프로그래밍 > 소프트웨어공학", (byte) 1, (byte) 1);
        temp.markReady();
        Long tempId = bookIsbnTempRepository.save(temp).getId();

        BookIsbnTempApproveRequest request = new BookIsbnTempApproveRequest(
                "실DB REQUIRES_NEW 재현", "테스트출판사", List.of("테스트저자"), List.of(),
                "PAID", 3000, "AVAILABLE",
                new BookIsbnTempApproveRequest.Detail("설명", null, null, null), List.of());

        // 실제 프록시 호출: approve()가 REQUIRED로 새 물리 트랜잭션을 시작하고,
        // 그 안에서 알라딘 category 자동 생성이 REQUIRES_NEW로 별도 커밋된다.
        // 수정 전 코드였다면 이 첫 시도에서 CATEGORY_NOT_FOUND로 실패했어야 한다.
        BookIsbnTempApproveResponse response = bookApprovalService.approve(tempId, request);

        assertThat(response.bookId()).isNotNull();
        Book book = bookRepository.findById(response.bookId()).orElseThrow();
        assertThat(book.getIsbn()).isEqualTo(isbn);
        assertThat(bookCategoryRepository.findActiveByBookId(book.getBookId())).hasSize(1);

        var category = categoryRepository.findByAladinCategoryId(aladinCategoryId);
        assertThat(category).isPresent();
        assertThat(category.get().getCategoryName()).isEqualTo("소프트웨어공학");
    }

    private String uniqueIsbn13() {
        long n = System.nanoTime() % 10_000_000_000L;
        return String.format("979%010d", n);
    }
}
