package com.acorn.abc.library.service;

import com.acorn.abc.book.domain.Book;
import com.acorn.abc.book.service.BookService;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.external.library.dto.InfoNaruLibraryItem;
import com.acorn.abc.external.library.dto.InfoNaruLibrarySearchResult;
import com.acorn.abc.library.domain.Library;
import com.acorn.abc.library.domain.LibraryHolding;
import com.acorn.abc.library.dto.LibrarySearchResponse;
import com.acorn.abc.library.repository.LibraryHoldingRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PhysicalLibrarySearchServiceTest {

    @Mock
    private BookService bookService;
    @Mock
    private LibraryHoldingRepository libraryHoldingRepository;
    @Mock
    private InfoNaruLibraryConcurrencyLimiter concurrencyLimiter;

    private Cache<LibraryCacheKey, CachedLibrarySearchResult> cache;
    private PhysicalLibrarySearchService service;

    @BeforeEach
    void setUp() {
        cache = Caffeine.newBuilder().build();
        service = new PhysicalLibrarySearchService(bookService, libraryHoldingRepository, concurrencyLimiter, cache);
    }

    // bookId/title/coverImageUrl은 응답을 실제로 만드는 테스트에서만 쓰이므로 lenient로 등록해
    // 검증 단계에서 예외를 던지는 테스트가 "unused stub"로 실패하지 않게 한다.
    private Book bookWithIsbn(String isbn) {
        Book book = mock(Book.class);
        lenient().when(book.getBookId()).thenReturn(1L);
        lenient().when(book.getTitle()).thenReturn("클린 코드");
        lenient().when(book.getCoverImageUrl()).thenReturn("http://cover/1.jpg");
        given(book.getIsbn()).willReturn(isbn);
        return book;
    }

    private InfoNaruLibraryItem item(String code) {
        return new InfoNaruLibraryItem(code, "도서관" + code, "주소", new BigDecimal("37.5"), new BigDecimal("127.0"),
                "http://lib", "월요일", "09:00-18:00");
    }

    @Test
    void isbn이_없으면_BOOK_ISBN_UNAVAILABLE() {
        Book book = bookWithIsbn(null);
        given(bookService.getAvailableBookOrThrow(1L)).willReturn(book);

        assertThatThrownBy(() -> service.search(1L, "11", null, 0, 20, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOK_ISBN_UNAVAILABLE);
    }

    @Test
    void isbn_형식이_13자리_숫자가_아니면_BOOK_ISBN_UNAVAILABLE() {
        Book book = bookWithIsbn("12345");
        given(bookService.getAvailableBookOrThrow(1L)).willReturn(book);

        assertThatThrownBy(() -> service.search(1L, "11", null, 0, 20, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BOOK_ISBN_UNAVAILABLE);
    }

    @Test
    void region_형식이_잘못되면_VALIDATION_ERROR() {
        Book book = bookWithIsbn("9788966260959");
        given(bookService.getAvailableBookOrThrow(1L)).willReturn(book);

        assertThatThrownBy(() -> service.search(1L, "1", null, 0, 20, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void fresh_cache가_있으면_외부_호출을_하지_않는다() {
        Book book = bookWithIsbn("9788966260959");
        given(bookService.getAvailableBookOrThrow(1L)).willReturn(book);
        LibraryCacheKey key = new LibraryCacheKey("9788966260959", "11", null, 0, 20);
        cache.put(key, new CachedLibrarySearchResult(
                InfoNaruLibrarySearchResult.success(List.of(item("A")), 1), Instant.now()));

        LibrarySearchResponse response = service.search(1L, "11", null, 0, 20, null, null);

        assertThat(response.source()).isEqualTo("INFONARU");
        assertThat(response.stale()).isFalse();
        assertThat(response.libraries()).hasSize(1);
        verify(concurrencyLimiter, never()).search(anyString(), anyString(), any(), eq(0), eq(20));
    }

    @Test
    void 외부_호출이_성공하면_cache에_저장되고_INFONARU_응답을_반환한다() {
        Book book = bookWithIsbn("9788966260959");
        given(bookService.getAvailableBookOrThrow(1L)).willReturn(book);
        given(concurrencyLimiter.search("9788966260959", "11", null, 0, 20))
                .willReturn(Optional.of(InfoNaruLibrarySearchResult.success(List.of(item("A")), 1)));

        LibrarySearchResponse response = service.search(1L, "11", null, 0, 20, null, null);

        assertThat(response.source()).isEqualTo("INFONARU");
        assertThat(response.stale()).isFalse();
        assertThat(cache.getIfPresent(new LibraryCacheKey("9788966260959", "11", null, 0, 20))).isNotNull();
    }

    @Test
    void NOT_TRIED도_실패로_취급해_stale_cache로_대체한다() {
        Book book = bookWithIsbn("9788966260959");
        given(bookService.getAvailableBookOrThrow(1L)).willReturn(book);
        LibraryCacheKey key = new LibraryCacheKey("9788966260959", "11", null, 0, 20);
        cache.put(key, new CachedLibrarySearchResult(
                InfoNaruLibrarySearchResult.success(List.of(item("A")), 1),
                Instant.now().minus(Duration.ofHours(10))));
        given(concurrencyLimiter.search("9788966260959", "11", null, 0, 20))
                .willReturn(Optional.of(InfoNaruLibrarySearchResult.notTried()));

        LibrarySearchResponse response = service.search(1L, "11", null, 0, 20, null, null);

        assertThat(response.source()).isEqualTo("INFONARU");
        assertThat(response.stale()).isTrue();
        assertThat(response.warnings()).isNotEmpty();
    }

    @Test
    void 외부_실패하고_stale도_없으면_manual_fallback을_dtlRegion과_함께_조회한다() {
        Book book = bookWithIsbn("9788966260959");
        given(bookService.getAvailableBookOrThrow(1L)).willReturn(book);
        given(concurrencyLimiter.search("9788966260959", "11", "11110", 2, 15))
                .willReturn(Optional.of(InfoNaruLibrarySearchResult.failedRetryable()));

        Library library = mock(Library.class);
        given(library.getLibraryCode()).willReturn("111108");
        given(library.getLibraryName()).willReturn("강남도서관");
        given(library.getAddress()).willReturn("서울시 강남구");
        LibraryHolding holding = mock(LibraryHolding.class);
        given(holding.getLibrary()).willReturn(library);

        Pageable expectedPageable = PageRequest.of(2, 15,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "library.libraryName"));
        given(libraryHoldingRepository.findManualFallback(eq("9788966260959"), eq("11"), eq("11110"), any()))
                .willReturn(new PageImpl<>(List.of(holding), expectedPageable, 31));

        LibrarySearchResponse response = service.search(1L, "11", "11110", 2, 15, null, null);

        assertThat(response.source()).isEqualTo("MANUAL");
        assertThat(response.libraries()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(31);
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(15);

        // any() 대신 실제 전달된 Pageable을 캡처해 page/size/sort가 요청값 그대로인지 검증한다.
        // (구현이 잘못돼서 항상 PageRequest.of(0, 20, ...)를 넘겨도 any()만으로는 테스트가 통과해버린다)
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(libraryHoldingRepository).findManualFallback(eq("9788966260959"), eq("11"), eq("11110"),
                pageableCaptor.capture());
        Pageable captured = pageableCaptor.getValue();
        assertThat(captured.getPageNumber()).isEqualTo(2);
        assertThat(captured.getPageSize()).isEqualTo(15);
        assertThat(captured.getSort().getOrderFor("library.libraryName")).isNotNull();
    }

    @Test
    void manual_fallback_데이터는_있지만_요청page가_마지막을_넘으면_503이_아니라_빈_목록_200을_반환한다() {
        Book book = bookWithIsbn("9788966260959");
        given(bookService.getAvailableBookOrThrow(1L)).willReturn(book);
        given(concurrencyLimiter.search("9788966260959", "11", null, 5, 20))
                .willReturn(Optional.of(InfoNaruLibrarySearchResult.failedRetryable()));

        Pageable requested = PageRequest.of(5, 20,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "library.libraryName"));
        // totalElements=3인데 page=5(0-base)를 요청하면 content는 비지만 데이터 자체는 존재한다.
        given(libraryHoldingRepository.findManualFallback(eq("9788966260959"), eq("11"), eq(null), any()))
                .willReturn(new PageImpl<>(List.of(), requested, 3));

        LibrarySearchResponse response = service.search(1L, "11", null, 5, 20, null, null);

        assertThat(response.source()).isEqualTo("MANUAL");
        assertThat(response.libraries()).isEmpty();
        assertThat(response.totalElements()).isEqualTo(3);
    }

    @Test
    void 외부_실패시_stale_cache가_있으면_manual_fallback을_조회하지_않는다() {
        Book book = bookWithIsbn("9788966260959");
        given(bookService.getAvailableBookOrThrow(1L)).willReturn(book);
        LibraryCacheKey key = new LibraryCacheKey("9788966260959", "11", null, 0, 20);
        cache.put(key, new CachedLibrarySearchResult(
                InfoNaruLibrarySearchResult.success(List.of(item("A")), 1),
                Instant.now().minus(Duration.ofHours(10))));
        given(concurrencyLimiter.search("9788966260959", "11", null, 0, 20))
                .willReturn(Optional.of(InfoNaruLibrarySearchResult.failedRetryable()));

        LibrarySearchResponse response = service.search(1L, "11", null, 0, 20, null, null);

        assertThat(response.source()).isEqualTo("INFONARU");
        assertThat(response.stale()).isTrue();
        verify(libraryHoldingRepository, never()).findManualFallback(any(), any(), any(), any());
    }

    @Test
    void 외부_실패하고_manual_fallback도_없으면_503() {
        Book book = bookWithIsbn("9788966260959");
        given(bookService.getAvailableBookOrThrow(1L)).willReturn(book);
        given(concurrencyLimiter.search("9788966260959", "11", null, 0, 20))
                .willReturn(Optional.of(InfoNaruLibrarySearchResult.failedRetryable()));
        given(libraryHoldingRepository.findManualFallback(eq("9788966260959"), eq("11"), eq(null), any()))
                .willReturn(Page.empty());

        assertThatThrownBy(() -> service.search(1L, "11", null, 0, 20, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.LIBRARY_EXTERNAL_API_UNAVAILABLE);
    }

    @Test
    void permit_미획득시_stale_manual_503_순서를_따른다() {
        Book book = bookWithIsbn("9788966260959");
        given(bookService.getAvailableBookOrThrow(1L)).willReturn(book);
        given(concurrencyLimiter.search("9788966260959", "11", null, 0, 20)).willReturn(Optional.empty());
        given(libraryHoldingRepository.findManualFallback(eq("9788966260959"), eq("11"), eq(null), any()))
                .willReturn(Page.empty());

        assertThatThrownBy(() -> service.search(1L, "11", null, 0, 20, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.LIBRARY_EXTERNAL_API_UNAVAILABLE);
        verify(concurrencyLimiter, times(1)).search("9788966260959", "11", null, 0, 20);
    }

    @Test
    void cache_key는_좌표를_포함하지_않아_다른_좌표라도_동일_key로_캐시된다() {
        Book book = bookWithIsbn("9788966260959");
        given(bookService.getAvailableBookOrThrow(1L)).willReturn(book);
        given(concurrencyLimiter.search("9788966260959", "11", null, 0, 20))
                .willReturn(Optional.of(InfoNaruLibrarySearchResult.success(List.of(item("A")), 1)));

        service.search(1L, "11", null, 0, 20, new BigDecimal("37.5"), new BigDecimal("127.0"));
        service.search(1L, "11", null, 0, 20, new BigDecimal("35.1"), new BigDecimal("129.0"));

        verify(concurrencyLimiter, times(1)).search("9788966260959", "11", null, 0, 20);
    }

    // ExecutorService만으로는 follower 스레드가 실제로 future.join()에서 블로킹된 뒤 release하는
    // 것을 보장하지 못한다. 스레드 스케줄링이 늦어지면 follower가 leader 완료 이후에야 실행돼
    // inFlight map에서 이미 빠진 key로 "자기 자신이 새 leader가 되어" 별도 외부 호출을 하는데도,
    // 결과적으로 상태가 같아 보여 테스트가 우연히 통과할 수 있다(false-positive). raw Thread를 직접
    // 만들어 각 follower가 WAITING/TIMED_WAITING 상태(= future.join() 내부에서 실제로 대기 중)에
    // 들어간 것을 폴링으로 확인한 뒤에만 release한다.
    private void awaitBlocked(List<Thread> threads, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        for (Thread t : threads) {
            while (t.getState() != Thread.State.WAITING && t.getState() != Thread.State.TIMED_WAITING) {
                if (System.currentTimeMillis() > deadline) {
                    throw new AssertionError("thread did not reach WAITING state in time: " + t.getState());
                }
                Thread.sleep(10);
            }
        }
    }

    @Test
    void 실제_동시_요청은_동일_key에_대해_외부_호출을_한번만_수행한다() throws Exception {
        Book book = bookWithIsbn("9788966260959");
        given(bookService.getAvailableBookOrThrow(1L)).willReturn(book);

        CountDownLatch startedLatch = new CountDownLatch(1);
        CountDownLatch releaseLatch = new CountDownLatch(1);
        given(concurrencyLimiter.search("9788966260959", "11", null, 0, 20)).willAnswer(invocation -> {
            startedLatch.countDown();
            releaseLatch.await(5, TimeUnit.SECONDS);
            return Optional.of(InfoNaruLibrarySearchResult.success(List.of(item("A")), 1));
        });

        List<LibrarySearchResponse> results = Collections.synchronizedList(new ArrayList<>());
        Thread leaderThread = new Thread(() -> results.add(service.search(1L, "11", null, 0, 20, null, null)));
        leaderThread.start();
        assertThat(startedLatch.await(5, TimeUnit.SECONDS)).isTrue();

        int followerCount = 5;
        List<Thread> followerThreads = new ArrayList<>();
        for (int i = 0; i < followerCount; i++) {
            Thread t = new Thread(() -> results.add(service.search(1L, "11", null, 0, 20, null, null)));
            t.start();
            followerThreads.add(t);
        }

        // follower들이 실제로 future.join()에서 블로킹 상태에 들어간 것을 확인한 뒤에만 release한다.
        awaitBlocked(followerThreads, 5000);
        releaseLatch.countDown();

        leaderThread.join(5000);
        for (Thread t : followerThreads) {
            t.join(5000);
        }

        assertThat(results).hasSize(followerCount + 1);
        results.forEach(r -> assertThat(r.source()).isEqualTo("INFONARU"));
        verify(concurrencyLimiter, times(1)).search("9788966260959", "11", null, 0, 20);
    }

    @Test
    void 외부_호출에서_예기치_못한_RuntimeException은_외부장애_fallback으로_감추지_않고_그대로_전파한다() {
        Book book = bookWithIsbn("9788966260959");
        given(bookService.getAvailableBookOrThrow(1L)).willReturn(book);
        given(concurrencyLimiter.search("9788966260959", "11", null, 0, 20))
                .willThrow(new IllegalStateException("예기치 못한 내부 오류"));

        // NPE/cache 오류 같은 예상 못한 내부 버그를 503(LIBRARY_EXTERNAL_API_UNAVAILABLE)으로 감추면
        // 실제 버그가 "외부 장애"로 위장되어 모니터링에서 드러나지 않는다. 예상된 외부 장애(permit
        // 미획득, 정보나루 실패 상태값)만 fallback 대상이고, 내부 버그는 그대로 전파돼야 한다.
        assertThatThrownBy(() -> service.search(1L, "11", null, 0, 20, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("예기치 못한 내부 오류");
        verify(libraryHoldingRepository, never()).findManualFallback(any(), any(), any(), any());
        verify(concurrencyLimiter, times(1)).search("9788966260959", "11", null, 0, 20);
    }

    @Test
    void 실제_동시_요청에서_예기치_못한_예외가_나면_leader와_follower_모두_동일하게_실패하고_외부_호출은_한번만_한다() throws Exception {
        Book book = bookWithIsbn("9788966260959");
        given(bookService.getAvailableBookOrThrow(1L)).willReturn(book);

        CountDownLatch startedLatch = new CountDownLatch(1);
        CountDownLatch releaseLatch = new CountDownLatch(1);
        given(concurrencyLimiter.search("9788966260959", "11", null, 0, 20)).willAnswer(invocation -> {
            startedLatch.countDown();
            releaseLatch.await(5, TimeUnit.SECONDS);
            throw new IllegalStateException("예기치 못한 내부 오류");
        });

        List<AtomicReference<Throwable>> failures = new ArrayList<>();
        AtomicReference<Throwable> leaderFailure = new AtomicReference<>();
        failures.add(leaderFailure);
        Thread leaderThread = new Thread(() -> {
            try {
                service.search(1L, "11", null, 0, 20, null, null);
            } catch (Throwable t) {
                leaderFailure.set(t);
            }
        });
        leaderThread.start();
        assertThat(startedLatch.await(5, TimeUnit.SECONDS)).isTrue();

        int followerCount = 3;
        List<Thread> followerThreads = new ArrayList<>();
        for (int i = 0; i < followerCount; i++) {
            AtomicReference<Throwable> followerFailure = new AtomicReference<>();
            failures.add(followerFailure);
            Thread t = new Thread(() -> {
                try {
                    service.search(1L, "11", null, 0, 20, null, null);
                } catch (Throwable throwable) {
                    followerFailure.set(throwable);
                }
            });
            t.start();
            followerThreads.add(t);
        }

        awaitBlocked(followerThreads, 5000);
        releaseLatch.countDown();

        leaderThread.join(5000);
        for (Thread t : followerThreads) {
            t.join(5000);
        }

        assertThat(failures).hasSize(followerCount + 1);
        for (AtomicReference<Throwable> failure : failures) {
            assertThat(failure.get()).isInstanceOf(IllegalStateException.class)
                    .hasMessage("예기치 못한 내부 오류");
        }
        verify(concurrencyLimiter, times(1)).search("9788966260959", "11", null, 0, 20);
    }

    @Test
    void SUCCESS_NO_DATA는_성공으로_취급해_manual_fallback을_호출하지_않는다() {
        Book book = bookWithIsbn("9788966260959");
        given(bookService.getAvailableBookOrThrow(1L)).willReturn(book);
        given(concurrencyLimiter.search("9788966260959", "11", null, 0, 20))
                .willReturn(Optional.of(InfoNaruLibrarySearchResult.noData()));

        LibrarySearchResponse response = service.search(1L, "11", null, 0, 20, null, null);

        assertThat(response.source()).isEqualTo("INFONARU");
        assertThat(response.stale()).isFalse();
        assertThat(response.libraries()).isEmpty();
        verify(libraryHoldingRepository, never()).findManualFallback(any(), any(), any(), any());
    }

    @Test
    void dtlRegion이_region_prefix와_다르면_VALIDATION_ERROR() {
        Book book = bookWithIsbn("9788966260959");
        given(bookService.getAvailableBookOrThrow(1L)).willReturn(book);

        assertThatThrownBy(() -> service.search(1L, "11", "26110", 0, 20, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void 좌표는_swap되지_않고_그대로_매핑된다() {
        Book book = bookWithIsbn("9788966260959");
        given(bookService.getAvailableBookOrThrow(1L)).willReturn(book);
        InfoNaruLibraryItem far = new InfoNaruLibraryItem("A", "먼도서관", "주소",
                new BigDecimal("35.0000000"), new BigDecimal("129.0000000"),
                "http://lib", "월", "09:00-18:00");
        given(concurrencyLimiter.search("9788966260959", "11", null, 0, 20))
                .willReturn(Optional.of(InfoNaruLibrarySearchResult.success(List.of(far), 1)));

        LibrarySearchResponse response = service.search(1L, "11", null, 0, 20, null, null);

        LibrarySearchResponse.LibraryResultItem result = response.libraries().get(0);
        // latitude/longitude가 뒤바뀌면 이 값이 서로 교차돼 지도 marker가 엉뚱한 위치에 찍힌다.
        assertThat(result.latitude()).isEqualByComparingTo(new BigDecimal("35.0000000"));
        assertThat(result.longitude()).isEqualByComparingTo(new BigDecimal("129.0000000"));
    }

    @Test
    void 좌표가_있으면_distanceKm을_계산하고_현재_page_내부에서_거리순으로_정렬한다() {
        Book book = bookWithIsbn("9788966260959");
        given(bookService.getAvailableBookOrThrow(1L)).willReturn(book);
        // 사용자 위치: 서울시청 근방(37.5665, 126.9780).
        // far: 부산(35.1, 129.0) - 멀다. near: 시청 바로 옆(37.5700, 126.9820) - 가깝다.
        InfoNaruLibraryItem far = new InfoNaruLibraryItem("FAR", "부산도서관", "주소",
                new BigDecimal("35.1"), new BigDecimal("129.0"), null, null, null);
        InfoNaruLibraryItem near = new InfoNaruLibraryItem("NEAR", "시청도서관", "주소",
                new BigDecimal("37.5700"), new BigDecimal("126.9820"), null, null, null);
        given(concurrencyLimiter.search("9788966260959", "11", null, 0, 20))
                .willReturn(Optional.of(InfoNaruLibrarySearchResult.success(List.of(far, near), 2)));

        LibrarySearchResponse response = service.search(1L, "11", null, 0, 20,
                new BigDecimal("37.5665"), new BigDecimal("126.9780"));

        assertThat(response.libraries()).hasSize(2);
        assertThat(response.libraries().get(0).libraryCode()).isEqualTo("NEAR");
        assertThat(response.libraries().get(1).libraryCode()).isEqualTo("FAR");
        assertThat(response.libraries().get(0).distanceKm()).isNotNull()
                .isLessThan(response.libraries().get(1).distanceKm());
        // 서울시청-부산 직선거리는 대략 320~330km 범위여야 한다(계산식 자체가 맞는지 대략 검증).
        assertThat(response.libraries().get(1).distanceKm()).isBetween(300.0, 350.0);
    }
}
