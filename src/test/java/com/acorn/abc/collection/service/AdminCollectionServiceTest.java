package com.acorn.abc.collection.service;

import com.acorn.abc.admin.collection.dto.AdminCollectionBooksUpdateRequest;
import com.acorn.abc.admin.collection.dto.AdminCollectionPatchRequest;
import com.acorn.abc.admin.collection.dto.AdminCollectionSaveRequest;
import com.acorn.abc.book.domain.Book;
import com.acorn.abc.book.repository.BookRepository;
import com.acorn.abc.collection.domain.Collection;
import com.acorn.abc.collection.domain.CollectionBook;
import com.acorn.abc.collection.repository.CollectionBookRepository;
import com.acorn.abc.collection.repository.CollectionRepository;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.member.repository.MemberRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 관리자 컬렉션 API 구현계획(2026-07-16) §9.2 — 정책/transaction/감사로그 검증.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminCollectionServiceTest {

    @Mock
    private CollectionRepository collectionRepository;

    @Mock
    private CollectionBookRepository collectionBookRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AdminAuditLogWriter adminAuditLogWriter;

    private AdminCollectionService newService() {
        return new AdminCollectionService(
                collectionRepository, collectionBookRepository, bookRepository, memberRepository, adminAuditLogWriter);
    }

    @Test
    void getAdminCollections_잘못된_collectionType이면_VALIDATION_ERROR를_던진다() {
        AdminCollectionService adminCollectionService = newService();

        assertThatThrownBy(() -> adminCollectionService.getAdminCollections("NOVEL", null, 0, 10))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void getAdminCollections_mappedCount를_batch_조회해서_채운다() {
        AdminCollectionService adminCollectionService = newService();
        Collection collection = collectionOf(1L, "SERIES", "ACTIVE");
        Page<Collection> page = new PageImpl<>(List.of(collection), PageRequest.of(0, 10), 1);
        given(collectionRepository.searchForAdmin(eq(null), eq(null), any())).willReturn(page);
        CollectionBookRepository.CollectionMappedCountView view = mock(CollectionBookRepository.CollectionMappedCountView.class);
        given(view.getCollectionId()).willReturn(1L);
        given(view.getMappedCount()).willReturn(3L);
        given(collectionBookRepository.countByCollectionIds(anyList())).willReturn(List.of(view));

        var response = adminCollectionService.getAdminCollections(null, null, 0, 10);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).mappedCount()).isEqualTo(3L);
    }

    @Test
    void getAdminCollectionDetail_없는_컬렉션이면_COMMON_NOT_FOUND를_던진다() {
        AdminCollectionService adminCollectionService = newService();
        given(collectionRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminCollectionService.getAdminCollectionDetail(999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);
    }

    @Test
    void getAdminCollectionDetail_EVENT는_createdAt_순으로_조회한다() {
        AdminCollectionService adminCollectionService = newService();
        Collection collection = collectionOf(1L, "EVENT", "ACTIVE");
        given(collectionRepository.findById(1L)).willReturn(Optional.of(collection));
        given(collectionBookRepository.findByCollection_CollectionIdOrderByCreatedAtAscCollectionBookIdAsc(1L))
                .willReturn(List.of());

        adminCollectionService.getAdminCollectionDetail(1L);

        verify(collectionBookRepository).findByCollection_CollectionIdOrderByCreatedAtAscCollectionBookIdAsc(1L);
        verify(collectionBookRepository, never()).findByCollection_CollectionIdOrderByDisplayOrderAscCollectionBookIdAsc(anyLong());
    }

    @Test
    void createAdminCollection_SERIES인데_discountRate가_있으면_VALIDATION_ERROR를_던진다() {
        AdminCollectionService adminCollectionService = newService();
        AdminCollectionSaveRequest request = new AdminCollectionSaveRequest(
                "이름", "SERIES", BigDecimal.TEN, null, null, null, "ACTIVE");

        assertThatThrownBy(() -> adminCollectionService.createAdminCollection(request, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(collectionRepository, never()).save(any());
    }

    @Test
    void createAdminCollection_EVENT_startDate가_endDate보다_늦으면_VALIDATION_ERROR를_던진다() {
        AdminCollectionService adminCollectionService = newService();
        AdminCollectionSaveRequest request = new AdminCollectionSaveRequest(
                "이름", "EVENT", null, null, LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 10), "ACTIVE");

        assertThatThrownBy(() -> adminCollectionService.createAdminCollection(request, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void createAdminCollection_정상_요청은_생성하고_감사로그를_기록한다() {
        AdminCollectionService adminCollectionService = newService();
        AdminCollectionSaveRequest request = new AdminCollectionSaveRequest(
                " 이름 ", "SERIES", null, null, null, null, "ACTIVE");

        adminCollectionService.createAdminCollection(request, 1L);

        verify(collectionRepository).save(any(Collection.class));
        verify(adminAuditLogWriter).record(eq(1L), eq(AdminAuditLogActionType.COLLECTION_CREATE),
                eq(AdminAuditLogTargetType.COLLECTION), any(), eq(null), any());
    }

    @Test
    void updateAdminCollection_없는_컬렉션이면_COMMON_NOT_FOUND를_던진다() {
        AdminCollectionService adminCollectionService = newService();
        given(collectionRepository.findByIdForUpdate(999L)).willReturn(Optional.empty());
        AdminCollectionSaveRequest request = new AdminCollectionSaveRequest(
                "이름", "SERIES", null, null, null, null, "ACTIVE");

        assertThatThrownBy(() -> adminCollectionService.updateAdminCollection(999L, request, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMON_NOT_FOUND);
    }

    @Test
    void updateAdminCollection_mapping이_있는데_유형을_바꾸면_VALIDATION_ERROR를_던진다() {
        AdminCollectionService adminCollectionService = newService();
        Collection collection = collectionOf(1L, "SERIES", "ACTIVE");
        given(collectionRepository.findByIdForUpdate(1L)).willReturn(Optional.of(collection));
        given(collectionBookRepository.existsByCollection_CollectionId(1L)).willReturn(true);
        AdminCollectionSaveRequest request = new AdminCollectionSaveRequest(
                "이름", "EVENT", null, null, null, null, "ACTIVE");

        assertThatThrownBy(() -> adminCollectionService.updateAdminCollection(1L, request, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void upsertCollectionBooks_요청_내_bookId가_중복이면_VALIDATION_ERROR를_던진다() {
        AdminCollectionService adminCollectionService = newService();
        Collection collection = collectionOf(1L, "SERIES", "ACTIVE");
        given(collectionRepository.findByIdForUpdate(1L)).willReturn(Optional.of(collection));
        AdminCollectionBooksUpdateRequest request = new AdminCollectionBooksUpdateRequest(List.of(
                new AdminCollectionBooksUpdateRequest.BookItem(10L, 1),
                new AdminCollectionBooksUpdateRequest.BookItem(10L, 2)));

        assertThatThrownBy(() -> adminCollectionService.upsertCollectionBooks(1L, request, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void upsertCollectionBooks_존재하지_않는_bookId가_있으면_BOOK_NOT_FOUND로_전체_rollback한다() {
        AdminCollectionService adminCollectionService = newService();
        Collection collection = collectionOf(1L, "SERIES", "ACTIVE");
        given(collectionRepository.findByIdForUpdate(1L)).willReturn(Optional.of(collection));
        Book existingBook = bookOf(10L);
        given(bookRepository.findAllById(List.of(10L, 20L))).willReturn(List.of(existingBook));
        AdminCollectionBooksUpdateRequest request = new AdminCollectionBooksUpdateRequest(List.of(
                new AdminCollectionBooksUpdateRequest.BookItem(10L, 1),
                new AdminCollectionBooksUpdateRequest.BookItem(20L, 2)));

        assertThatThrownBy(() -> adminCollectionService.upsertCollectionBooks(1L, request, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOOK_NOT_FOUND);
        verify(collectionBookRepository, never()).save(any());
    }

    @Test
    void upsertCollectionBooks_SERIES인데_displayOrder가_없으면_VALIDATION_ERROR를_던진다() {
        AdminCollectionService adminCollectionService = newService();
        Collection collection = collectionOf(1L, "SERIES", "ACTIVE");
        given(collectionRepository.findByIdForUpdate(1L)).willReturn(Optional.of(collection));
        Book book = bookOf(10L);
        given(bookRepository.findAllById(List.of(10L))).willReturn(List.of(book));
        AdminCollectionBooksUpdateRequest request = new AdminCollectionBooksUpdateRequest(List.of(
                new AdminCollectionBooksUpdateRequest.BookItem(10L, null)));

        assertThatThrownBy(() -> adminCollectionService.upsertCollectionBooks(1L, request, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void upsertCollectionBooks_EVENT인데_displayOrder가_있으면_VALIDATION_ERROR를_던진다() {
        AdminCollectionService adminCollectionService = newService();
        Collection collection = collectionOf(1L, "EVENT", "ACTIVE");
        given(collectionRepository.findByIdForUpdate(1L)).willReturn(Optional.of(collection));
        Book book = bookOf(10L);
        given(bookRepository.findAllById(List.of(10L))).willReturn(List.of(book));
        AdminCollectionBooksUpdateRequest request = new AdminCollectionBooksUpdateRequest(List.of(
                new AdminCollectionBooksUpdateRequest.BookItem(10L, 1)));

        assertThatThrownBy(() -> adminCollectionService.upsertCollectionBooks(1L, request, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void upsertCollectionBooks_신규_추가와_기존_순서_갱신을_함께_처리하고_감사로그를_남긴다() {
        AdminCollectionService adminCollectionService = newService();
        Collection collection = collectionOf(1L, "SERIES", "ACTIVE");
        given(collectionRepository.findByIdForUpdate(1L)).willReturn(Optional.of(collection));
        Book bookA = bookOf(10L);
        Book bookB = bookOf(20L);
        given(bookRepository.findAllById(List.of(10L, 20L))).willReturn(List.of(bookA, bookB));
        given(collectionBookRepository.findByCollection_CollectionIdOrderByDisplayOrderAscCollectionBookIdAsc(1L))
                .willReturn(List.of());
        CollectionBook existingMapping = CollectionBook.create(collection, bookA, 5);
        given(collectionBookRepository.findByCollection_CollectionIdAndBook_BookIdIn(1L, List.of(10L, 20L)))
                .willReturn(List.of(existingMapping));
        given(collectionBookRepository.countByCollection_CollectionId(1L)).willReturn(2L);
        AdminCollectionBooksUpdateRequest request = new AdminCollectionBooksUpdateRequest(List.of(
                new AdminCollectionBooksUpdateRequest.BookItem(10L, 1),
                new AdminCollectionBooksUpdateRequest.BookItem(20L, 2)));

        adminCollectionService.upsertCollectionBooks(1L, request, 1L);

        assertThat(existingMapping.getDisplayOrder()).isEqualTo(1);
        verify(collectionBookRepository, times(1)).save(any(CollectionBook.class));
        verify(adminAuditLogWriter).record(eq(1L), eq(AdminAuditLogActionType.COLLECTION_BOOKS_UPDATE),
                eq(AdminAuditLogTargetType.COLLECTION), eq(1L), eq(null), any());
    }

    @Test
    void patchCollection_status와_removeBookIds가_모두_없으면_VALIDATION_ERROR를_던진다() {
        AdminCollectionService adminCollectionService = newService();
        AdminCollectionPatchRequest request = new AdminCollectionPatchRequest(null, null);

        assertThatThrownBy(() -> adminCollectionService.patchCollection(1L, request, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(collectionRepository, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void patchCollection_removeBookIds에_중복이_있으면_VALIDATION_ERROR를_던진다() {
        AdminCollectionService adminCollectionService = newService();
        AdminCollectionPatchRequest request = new AdminCollectionPatchRequest(null, List.of(10L, 10L));

        assertThatThrownBy(() -> adminCollectionService.patchCollection(1L, request, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    // PR50 코드리뷰 MEDIUM 반영 — Controller의 @Valid를 우회하는 직접 서비스 호출도 방어한다.
    @Test
    void patchCollection_removeBookIds에_null_원소가_있으면_VALIDATION_ERROR를_던진다() {
        AdminCollectionService adminCollectionService = newService();
        AdminCollectionPatchRequest request = new AdminCollectionPatchRequest(null, java.util.Arrays.asList(10L, null));

        assertThatThrownBy(() -> adminCollectionService.patchCollection(1L, request, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
        verify(collectionRepository, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void patchCollection_removeBookIds에_0이_있으면_VALIDATION_ERROR를_던진다() {
        AdminCollectionService adminCollectionService = newService();
        AdminCollectionPatchRequest request = new AdminCollectionPatchRequest(null, List.of(0L));

        assertThatThrownBy(() -> adminCollectionService.patchCollection(1L, request, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void patchCollection_removeBookIds에_음수가_있으면_VALIDATION_ERROR를_던진다() {
        AdminCollectionService adminCollectionService = newService();
        AdminCollectionPatchRequest request = new AdminCollectionPatchRequest(null, List.of(-1L));

        assertThatThrownBy(() -> adminCollectionService.patchCollection(1L, request, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void patchCollection_status만_변경해도_성공한다() {
        AdminCollectionService adminCollectionService = newService();
        Collection collection = collectionOf(1L, "SERIES", "ACTIVE");
        given(collectionRepository.findByIdForUpdate(1L)).willReturn(Optional.of(collection));
        given(collectionBookRepository.countByCollection_CollectionId(1L)).willReturn(0L);
        AdminCollectionPatchRequest request = new AdminCollectionPatchRequest("HIDDEN", null);

        var response = adminCollectionService.patchCollection(1L, request, 1L);

        assertThat(response.status()).isEqualTo("HIDDEN");
        verify(collectionBookRepository, never()).deleteByCollection_CollectionIdAndBook_BookIdIn(anyLong(), anyList());
    }

    @Test
    void patchCollection_이미_없는_bookId_제거_재요청도_idempotent하게_성공한다() {
        AdminCollectionService adminCollectionService = newService();
        Collection collection = collectionOf(1L, "SERIES", "ACTIVE");
        given(collectionRepository.findByIdForUpdate(1L)).willReturn(Optional.of(collection));
        given(collectionBookRepository.countByCollection_CollectionId(1L)).willReturn(0L);
        given(collectionBookRepository.deleteByCollection_CollectionIdAndBook_BookIdIn(1L, List.of(999L))).willReturn(0L);
        AdminCollectionPatchRequest request = new AdminCollectionPatchRequest(null, List.of(999L));

        var response = adminCollectionService.patchCollection(1L, request, 1L);

        assertThat(response.mappedCount()).isEqualTo(0L);
    }

    private Collection collectionOf(Long id, String type, String status) {
        Collection collection = Collection.create("이름", type, null, null, null, null, status, null);
        setId(collection, id);
        return collection;
    }

    private Book bookOf(Long id) {
        Book book = mock(Book.class);
        given(book.getBookId()).willReturn(id);
        return book;
    }

    private void setId(Collection collection, Long id) {
        try {
            var field = Collection.class.getDeclaredField("collectionId");
            field.setAccessible(true);
            field.set(collection, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
