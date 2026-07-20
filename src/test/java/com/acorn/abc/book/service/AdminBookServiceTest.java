package com.acorn.abc.book.service;

import com.acorn.abc.admin.book.dto.AdminBookSaveRequest;
import com.acorn.abc.admin.book.dto.AdminBookStatusChangeRequest;
import com.acorn.abc.book.domain.Author;
import com.acorn.abc.book.domain.Book;
import com.acorn.abc.book.domain.BookAuthor;
import com.acorn.abc.book.domain.BookCategory;
import com.acorn.abc.book.domain.Category;
import com.acorn.abc.book.domain.Publisher;
import com.acorn.abc.book.repository.AuthorRepository;
import com.acorn.abc.book.repository.BookAuthorRepository;
import com.acorn.abc.book.repository.BookCategoryRepository;
import com.acorn.abc.book.repository.BookDetailRepository;
import com.acorn.abc.book.repository.BookRepository;
import com.acorn.abc.book.repository.PublisherRepository;
import com.acorn.abc.category.repository.CategoryRepository;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.statistic.domain.AdminAuditLogActionType;
import com.acorn.abc.statistic.domain.AdminAuditLogTargetType;
import com.acorn.abc.statistic.service.AdminAuditLogWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 관리자 도서 API(API-ADMIN-BOOK-001~004) 정책/transaction/감사로그 검증 — DB에 연결하지 않는 단위 테스트
 * (관리자 도서 API 구현계획 §5.1).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminBookServiceTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private BookDetailRepository bookDetailRepository;
    @Mock
    private BookAuthorRepository bookAuthorRepository;
    @Mock
    private BookCategoryRepository bookCategoryRepository;
    @Mock
    private PublisherRepository publisherRepository;
    @Mock
    private AuthorRepository authorRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private AdminAuditLogWriter adminAuditLogWriter;

    private AdminBookService newService() {
        return new AdminBookService(bookRepository, bookDetailRepository, bookAuthorRepository, bookCategoryRepository,
                publisherRepository, authorRepository, categoryRepository, adminAuditLogWriter);
    }

    // ----- 001 목록 조회 -----

    @Test
    void getAdminBooks_잘못된_status면_VALIDATION_ERROR를_던진다() {
        AdminBookService service = newService();

        assertThatThrownBy(() -> service.getAdminBooks(null, null, null, "DELETED", 0, 20))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void getAdminBooks_잘못된_rentalType이면_VALIDATION_ERROR를_던진다() {
        AdminBookService service = newService();

        assertThatThrownBy(() -> service.getAdminBooks(null, null, "RENTAL", null, 0, 20))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void getAdminBooks_저자와_카테고리를_bookId로_bulk_조회해서_묶는다() {
        AdminBookService service = newService();
        Publisher publisher = publisherOf(1L, "출판사");
        Book book = bookOf(10L, publisher, "INACTIVE");
        Page<Book> page = new PageImpl<>(List.of(book), PageRequest.of(0, 20), 1);
        given(bookRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(page);
        Author author = authorOf(2L, "저자1");
        Category category = categoryOf(3L, "카테고리1", "ACTIVE");
        given(bookAuthorRepository.findAllByBookIdIn(List.of(10L)))
                .willReturn(List.of(BookAuthor.create(book, author, 1)));
        given(bookCategoryRepository.findAllByBookIdIn(List.of(10L)))
                .willReturn(List.of(BookCategory.create(book, category)));

        var response = service.getAdminBooks(null, null, null, null, 0, 20);

        assertThat(response.content()).hasSize(1);
        var summary = response.content().get(0);
        assertThat(summary.authorNames()).containsExactly("저자1");
        assertThat(summary.categories()).extracting("categoryId").containsExactly(3L);
        assertThat(summary.status()).isEqualTo("INACTIVE");
    }

    // ----- 002 등록 -----

    @Test
    void createAdminBook_제목이_공백이면_VALIDATION_ERROR를_던지고_저장하지_않는다() {
        AdminBookService service = newService();
        AdminBookSaveRequest request = requestOf("   ", null, "출판사", List.of("작가"), List.of(), "FREE", 0, 7, null);

        assertThatThrownBy(() -> service.createAdminBook(request, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(bookRepository, never()).save(any());
    }

    @Test
    void createAdminBook_저자명이_중복이면_VALIDATION_ERROR를_던진다() {
        AdminBookService service = newService();
        AdminBookSaveRequest request = requestOf("제목", null, "출판사", List.of("작가", "작가"), List.of(), "FREE", 0, 7, null);

        assertThatThrownBy(() -> service.createAdminBook(request, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void createAdminBook_categoryId가_중복이면_VALIDATION_ERROR를_던진다() {
        AdminBookService service = newService();
        AdminBookSaveRequest request = requestOf("제목", null, "출판사", List.of("작가"), List.of(1L, 1L), "FREE", 0, 7, null);

        assertThatThrownBy(() -> service.createAdminBook(request, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void createAdminBook_FREE인데_가격이_0이_아니면_VALIDATION_ERROR를_던진다() {
        AdminBookService service = newService();
        AdminBookSaveRequest request = requestOf("제목", null, "출판사", List.of("작가"), List.of(), "FREE", 100, 7, null);

        assertThatThrownBy(() -> service.createAdminBook(request, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void createAdminBook_PAID인데_가격이_0이면_VALIDATION_ERROR를_던진다() {
        AdminBookService service = newService();
        AdminBookSaveRequest request = requestOf("제목", null, "출판사", List.of("작가"), List.of(), "PAID", 0, 7, null);

        assertThatThrownBy(() -> service.createAdminBook(request, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void createAdminBook_ISBN이_중복이면_BOOK_ISBN_ALREADY_EXISTS를_던진다() {
        AdminBookService service = newService();
        given(bookRepository.existsByIsbn("1234567890123")).willReturn(true);
        AdminBookSaveRequest request =
                requestOf("제목", "1234567890123", "출판사", List.of("작가"), List.of(), "FREE", 0, 7, null);

        assertThatThrownBy(() -> service.createAdminBook(request, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOOK_ISBN_ALREADY_EXISTS);
    }

    @Test
    void createAdminBook_존재하지_않는_카테고리면_CATEGORY_NOT_FOUND를_던진다() {
        AdminBookService service = newService();
        given(categoryRepository.findById(99L)).willReturn(Optional.empty());
        AdminBookSaveRequest request = requestOf("제목", null, "출판사", List.of("작가"), List.of(99L), "FREE", 0, 7, null);

        assertThatThrownBy(() -> service.createAdminBook(request, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    void createAdminBook_비활성_카테고리면_CATEGORY_NOT_FOUND를_던진다() {
        AdminBookService service = newService();
        Category inactive = categoryOf(5L, "비활성", "INACTIVE");
        given(categoryRepository.findById(5L)).willReturn(Optional.of(inactive));
        AdminBookSaveRequest request = requestOf("제목", null, "출판사", List.of("작가"), List.of(5L), "FREE", 0, 7, null);

        assertThatThrownBy(() -> service.createAdminBook(request, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    void createAdminBook_정상_요청은_INACTIVE로_생성하고_BOOK_CREATE_감사로그를_남긴다() {
        AdminBookService service = newService();
        given(publisherRepository.findByPublisherName("출판사")).willReturn(Optional.empty());
        given(publisherRepository.save(any(Publisher.class))).willAnswer(inv -> inv.getArgument(0));
        given(authorRepository.findFirstByAuthorNameOrderByAuthorIdAsc("작가")).willReturn(Optional.empty());
        given(authorRepository.save(any(Author.class))).willAnswer(inv -> inv.getArgument(0));
        given(bookRepository.save(any(Book.class))).willAnswer(inv -> {
            Book book = inv.getArgument(0);
            setId(book, "bookId", 100L);
            return book;
        });
        AdminBookSaveRequest request = requestOf("제목", null, "출판사", List.of("작가"), List.of(), "FREE", 0, 7, null);

        var response = service.createAdminBook(request, 1L);

        assertThat(response.status()).isEqualTo("INACTIVE");
        verify(bookDetailRepository).save(any());
        verify(bookAuthorRepository).saveAll(any());
        verify(adminAuditLogWriter).record(eq(1L), eq(AdminAuditLogActionType.BOOK_CREATE),
                eq(AdminAuditLogTargetType.BOOK), eq(100L), eq(null), any());
    }

    // ----- 003 수정 -----

    @Test
    void updateAdminBook_없는_도서면_BOOK_NOT_FOUND를_던진다() {
        AdminBookService service = newService();
        given(bookRepository.findById(999L)).willReturn(Optional.empty());
        AdminBookSaveRequest request = requestOf("제목", null, "출판사", List.of("작가"), List.of(), "FREE", 0, 7, null);

        assertThatThrownBy(() -> service.updateAdminBook(999L, request, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOOK_NOT_FOUND);
    }

    @Test
    void updateAdminBook_ISBN이_그대로면_중복검사를_하지_않는다() {
        AdminBookService service = newService();
        Publisher publisher = publisherOf(1L, "출판사");
        Book book = bookOf(10L, publisher, "AVAILABLE");
        setField(book, "isbn", "1234567890123");
        given(bookRepository.findById(10L)).willReturn(Optional.of(book));
        given(bookRepository.findByIdForUpdate(10L)).willReturn(Optional.of(book));
        given(bookAuthorRepository.findAllByBookIdIn(List.of(10L))).willReturn(List.of());
        given(bookCategoryRepository.findAllByBookIdIn(List.of(10L))).willReturn(List.of());
        given(publisherRepository.findByPublisherName("출판사")).willReturn(Optional.of(publisher));
        given(authorRepository.findFirstByAuthorNameOrderByAuthorIdAsc("작가")).willReturn(Optional.of(authorOf(2L, "작가")));
        AdminBookSaveRequest request =
                requestOf("새 제목", "1234567890123", "출판사", List.of("작가"), List.of(), "FREE", 0, 7, null);

        service.updateAdminBook(10L, request, 1L);

        verify(bookRepository, never()).existsByIsbn(any());
    }

    @Test
    void updateAdminBook_ISBN을_다른_도서가_쓰는_값으로_바꾸면_BOOK_ISBN_ALREADY_EXISTS를_던진다() {
        AdminBookService service = newService();
        Publisher publisher = publisherOf(1L, "출판사");
        Book book = bookOf(10L, publisher, "AVAILABLE");
        setField(book, "isbn", "1111111111111");
        given(bookRepository.findById(10L)).willReturn(Optional.of(book));
        given(bookRepository.existsByIsbn("2222222222222")).willReturn(true);
        AdminBookSaveRequest request =
                requestOf("제목", "2222222222222", "출판사", List.of("작가"), List.of(), "FREE", 0, 7, null);

        assertThatThrownBy(() -> service.updateAdminBook(10L, request, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOOK_ISBN_ALREADY_EXISTS);
    }

    @Test
    void updateAdminBook_정상_요청은_기존_매핑을_지우고_새로_저장한다() {
        AdminBookService service = newService();
        Publisher publisher = publisherOf(1L, "출판사");
        Book book = bookOf(10L, publisher, "AVAILABLE");
        given(bookRepository.findById(10L)).willReturn(Optional.of(book));
        given(bookRepository.findByIdForUpdate(10L)).willReturn(Optional.of(book));
        given(bookAuthorRepository.findAllByBookIdIn(List.of(10L))).willReturn(List.of());
        given(bookCategoryRepository.findAllByBookIdIn(List.of(10L))).willReturn(List.of());
        given(bookDetailRepository.findByBook_BookId(10L)).willReturn(Optional.empty());
        given(publisherRepository.findByPublisherName("출판사")).willReturn(Optional.of(publisher));
        given(authorRepository.findFirstByAuthorNameOrderByAuthorIdAsc("작가")).willReturn(Optional.of(authorOf(2L, "작가")));
        AdminBookSaveRequest request =
                requestOf("새 제목", null, "출판사", List.of("작가"), List.of(), "PAID", 5000, 14, null);

        var response = service.updateAdminBook(10L, request, 1L);

        assertThat(response.bookId()).isEqualTo(10L);
        assertThat(book.getTitle()).isEqualTo("새 제목");
        assertThat(book.getRentalType()).isEqualTo("PAID");
        verify(bookCategoryRepository).deleteByBook_BookId(10L);
        verify(bookAuthorRepository).deleteByBook_BookId(10L);
        verify(bookCategoryRepository).flush();
        verify(bookAuthorRepository).flush();
        verify(adminAuditLogWriter).record(eq(1L), eq(AdminAuditLogActionType.BOOK_UPDATE),
                eq(AdminAuditLogTargetType.BOOK), eq(10L), any(), any());
    }

    // ----- 004 상태 변경 -----

    @Test
    void changeAdminBookStatus_없는_도서면_BOOK_NOT_FOUND를_던진다() {
        AdminBookService service = newService();
        given(bookRepository.findByIdForUpdate(999L)).willReturn(Optional.empty());
        AdminBookStatusChangeRequest request = new AdminBookStatusChangeRequest("HIDDEN", "사유");

        assertThatThrownBy(() -> service.changeAdminBookStatus(999L, request, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOOK_NOT_FOUND);
    }

    @Test
    void changeAdminBookStatus_실제_변경이면_사유를_감사로그에_남긴다() {
        AdminBookService service = newService();
        Publisher publisher = publisherOf(1L, "출판사");
        Book book = bookOf(10L, publisher, "INACTIVE");
        given(bookRepository.findByIdForUpdate(10L)).willReturn(Optional.of(book));
        AdminBookStatusChangeRequest request = new AdminBookStatusChangeRequest("AVAILABLE", "콘텐츠 준비 완료");

        var response = service.changeAdminBookStatus(10L, request, 1L);

        assertThat(response.status()).isEqualTo("AVAILABLE");
        assertThat(book.getStatus()).isEqualTo("AVAILABLE");
        verify(adminAuditLogWriter).record(eq(1L), eq(AdminAuditLogActionType.BOOK_UPDATE),
                eq(AdminAuditLogTargetType.BOOK), eq(10L), any(), any());
    }

    // 대응계획 §2.2 — 동일 상태 요청은 사유 없이도 200 no-op이며 상태·updatedAt·감사 로그를 바꾸지 않는다.
    @Test
    void changeAdminBookStatus_동일_상태면_사유_없이도_no_op으로_성공한다() {
        AdminBookService service = newService();
        Publisher publisher = publisherOf(1L, "출판사");
        Book book = bookOf(10L, publisher, "AVAILABLE");
        given(bookRepository.findByIdForUpdate(10L)).willReturn(Optional.of(book));
        AdminBookStatusChangeRequest request = new AdminBookStatusChangeRequest("AVAILABLE", null);

        var response = service.changeAdminBookStatus(10L, request, 1L);

        assertThat(response.status()).isEqualTo("AVAILABLE");
        assertThat(book.getUpdatedAt()).isNull();
        verify(adminAuditLogWriter, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void changeAdminBookStatus_실제_변경인데_사유가_공백이면_VALIDATION_ERROR를_던진다() {
        AdminBookService service = newService();
        Publisher publisher = publisherOf(1L, "출판사");
        Book book = bookOf(10L, publisher, "INACTIVE");
        given(bookRepository.findByIdForUpdate(10L)).willReturn(Optional.of(book));
        AdminBookStatusChangeRequest request = new AdminBookStatusChangeRequest("AVAILABLE", "   ");

        assertThatThrownBy(() -> service.changeAdminBookStatus(10L, request, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(book.getStatus()).isEqualTo("INACTIVE");
        verify(adminAuditLogWriter, never()).record(any(), any(), any(), any(), any(), any());
    }

    // ----- 005 상세 조회 -----

    @Test
    void getAdminBookDetail_없는_도서면_BOOK_NOT_FOUND를_던진다() {
        AdminBookService service = newService();
        given(bookRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAdminBookDetail(999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOOK_NOT_FOUND);
    }

    @Test
    void getAdminBookDetail_비활성_작가_카테고리도_status와_함께_반환하고_감사로그를_남기지_않는다() {
        AdminBookService service = newService();
        Publisher publisher = publisherOf(1L, "출판사");
        Book book = bookOf(10L, publisher, "AVAILABLE");
        given(bookRepository.findById(10L)).willReturn(Optional.of(book));
        Author inactiveAuthor = authorOf(2L, "작가1");
        setField(inactiveAuthor, "status", "INACTIVE");
        given(bookAuthorRepository.findForAdminDetail(10L))
                .willReturn(List.of(BookAuthor.create(book, inactiveAuthor, 1)));
        Category inactiveCategory = categoryOf(3L, "카테고리1", "INACTIVE");
        given(bookCategoryRepository.findForAdminDetail(10L))
                .willReturn(List.of(BookCategory.create(book, inactiveCategory)));
        given(bookDetailRepository.findByBook_BookId(10L)).willReturn(Optional.empty());

        var response = service.getAdminBookDetail(10L);

        assertThat(response.authors()).extracting("authorId", "status").containsExactly(tuple(2L, "INACTIVE"));
        assertThat(response.categories()).extracting("categoryId", "status").containsExactly(tuple(3L, "INACTIVE"));
        assertThat(response.detail().description()).isNull();
        verify(adminAuditLogWriter, never()).record(any(), any(), any(), any(), any(), any());
    }

    // ----- fixtures -----

    private AdminBookSaveRequest requestOf(String title, String isbn, String publisherName, List<String> authors,
                                            List<Long> categoryIds, String rentalType, int rentalPrice,
                                            int defaultRentalDays, String coverImageUrl) {
        return new AdminBookSaveRequest(title, isbn, publisherName, authors, categoryIds,
                rentalType, rentalPrice, defaultRentalDays, coverImageUrl, null);
    }

    private Book bookOf(Long id, Publisher publisher, String status) {
        Book book = Book.create(publisher, "제목", null, null, null, "FREE", 0, 7, 0, status);
        setId(book, "bookId", id);
        return book;
    }

    private Publisher publisherOf(Long id, String name) {
        Publisher publisher = Publisher.create(name);
        setId(publisher, "publisherId", id);
        return publisher;
    }

    private Author authorOf(Long id, String name) {
        Author author = Author.create(name);
        setId(author, "authorId", id);
        return author;
    }

    private Category categoryOf(Long id, String name, String status) {
        Category category = Category.create(name, null, 1);
        setId(category, "categoryId", id);
        setField(category, "status", status);
        return category;
    }

    private void setId(Object entity, String fieldName, Long id) {
        setField(entity, fieldName, id);
    }

    private void setField(Object entity, String fieldName, Object value) {
        try {
            var field = entity.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(entity, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
