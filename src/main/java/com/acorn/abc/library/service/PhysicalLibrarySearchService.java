package com.acorn.abc.library.service;

import com.acorn.abc.book.domain.Book;
import com.acorn.abc.book.service.BookService;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.external.library.dto.InfoNaruLibraryFetchStatus;
import com.acorn.abc.external.library.dto.InfoNaruLibraryItem;
import com.acorn.abc.external.library.dto.InfoNaruLibrarySearchResult;
import com.acorn.abc.library.domain.LibraryHolding;
import com.acorn.abc.library.dto.LibrarySearchResponse;
import com.acorn.abc.library.repository.LibraryHoldingRepository;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

// 종이책 소장 도서관 조회(계획 3.2/5.3). 우선순위: fresh cache -> 정보나루 실시간 호출 -> stale cache
// -> 관리자 manual fallback -> 503. cache key는 좌표를 포함하지 않고, cache value는 거리 계산 이전
// 원본 결과만 저장한다. distanceKm은 매 요청 계산하며 정렬은 "현재 조회된 page 내부"로 한정한다.
// class-level @Transactional을 두지 않는다: 도서 조회 후 정보나루 호출/캐시/semaphore/future.join()
// 대기까지 하나의 트랜잭션에 묶이면 외부 API 응답을 기다리는 동안 DB connection이 점유되어 동시
// 요청 시 connection pool 고갈 위험이 있다(계획과 무관한 런타임 위험, 리뷰로 발견). DB를 만지는
// bookService.getAvailableBookOrThrow()와 libraryHoldingRepository.findManualFallback()은 각각
// 자신의 짧은 트랜잭션(BookService 클래스 자체의 @Transactional, Spring Data JPA repository 기본
// 트랜잭션)으로 충분하고, 그 사이 외부 호출 구간은 트랜잭션 밖에서 실행된다.
@Slf4j
@Service
@RequiredArgsConstructor
public class PhysicalLibrarySearchService {

    private static final Duration FRESH_TTL = Duration.ofHours(6);
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final String ISBN13_PATTERN = "^[0-9]{13}$";

    private final BookService bookService;
    private final LibraryHoldingRepository libraryHoldingRepository;
    private final InfoNaruLibraryConcurrencyLimiter concurrencyLimiter;
    private final Cache<LibraryCacheKey, CachedLibrarySearchResult> libraryHoldingCache;

    // 동일 key 동시 요청이 외부 호출을 중복 발생시키지 않도록 single-flight으로 합친다.
    // putIfAbsent로 등록한 첫 스레드만 실제 조회를 수행하고, 나머지는 같은 future를 기다린다.
    // future가 완료된 뒤 map에서 제거하므로 완료 직전/직후에 진입한 스레드도 항상 유효한 결과를 얻는다
    // (기존 lock-object 방식은 remove와 monitor 해제 사이에 경쟁이 있어 같은 key로 중복 호출이 가능했다).
    private final ConcurrentHashMap<LibraryCacheKey, CompletableFuture<CachedLibrarySearchResult>> inFlight =
            new ConcurrentHashMap<>();

    public LibrarySearchResponse search(Long bookId, String region, String dtlRegion, int page, int size,
                                         BigDecimal latitude, BigDecimal longitude) {
        Book book = bookService.getAvailableBookOrThrow(bookId);
        String isbn = book.getIsbn();
        if (isbn == null || !isbn.matches(ISBN13_PATTERN)) {
            throw new BusinessException(ErrorCode.BOOK_ISBN_UNAVAILABLE);
        }
        validateRegion(region, dtlRegion);

        LibraryCacheKey key = new LibraryCacheKey(isbn, region, dtlRegion, page, size);
        List<String> warnings = new ArrayList<>();

        CachedLibrarySearchResult cached = libraryHoldingCache.getIfPresent(key);
        if (cached != null && isFresh(cached)) {
            return buildInfoNaruResponse(book, key, cached.result(), false, warnings, latitude, longitude);
        }

        CachedLibrarySearchResult refreshed = fetchWithCoalescing(key);
        if (refreshed != null) {
            return buildInfoNaruResponse(book, key, refreshed.result(), false, warnings, latitude, longitude);
        }

        // 외부 호출 실패(NOT_TRIED/FAILED_RETRYABLE) 또는 permit 미획득: stale cache -> manual fallback -> 503 순서
        if (cached != null) {
            warnings.add("정보나루 응답 지연으로 이전 조회 결과를 표시합니다.");
            return buildInfoNaruResponse(book, key, cached.result(), true, warnings, latitude, longitude);
        }

        Page<LibraryHolding> manualPage = libraryHoldingRepository.findManualFallback(
                isbn, region, dtlRegion,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "library.libraryName")));
        // hasContent()가 아니라 totalElements로 판단한다: manual 소장 데이터 자체는 있는데 요청한 page가
        // 마지막 page를 넘어가면 hasContent()==false가 되어 정상 응답(200, 빈 목록)을 503으로 오판하게 된다.
        if (manualPage.getTotalElements() > 0) {
            warnings.add("외부 도서관 정보를 조회할 수 없어 관리자가 등록한 정보를 표시합니다.");
            return buildManualResponse(book, region, manualPage, warnings, latitude, longitude);
        }

        throw new BusinessException(ErrorCode.LIBRARY_EXTERNAL_API_UNAVAILABLE);
    }

    // "예상된 외부 장애"(permit 미획득, 정보나루 호출/파싱 실패로 얻은 상태값)는 future.complete(null)로
    // 표시해 stale->manual->503 fallback을 타게 한다. 반면 NPE나 cache 내부 오류처럼 "예상하지 못한
    // 내부 버그"는 fallback으로 감추면 진짜 문제가 503 뒤에 숨어버리므로, future를 exceptionally 완료해
    // leader/follower 모두 같은 예외로 실패하게 한다(둘 다 fallback을 타거나 둘 다 예외로 실패하거나,
    // 항상 leader와 follower 결과가 같아야 한다).
    private CachedLibrarySearchResult fetchWithCoalescing(LibraryCacheKey key) {
        CompletableFuture<CachedLibrarySearchResult> future = new CompletableFuture<>();
        CompletableFuture<CachedLibrarySearchResult> existing = inFlight.putIfAbsent(key, future);
        if (existing != null) {
            return joinFuture(existing);
        }

        try {
            CachedLibrarySearchResult recheck = libraryHoldingCache.getIfPresent(key);
            if (recheck != null && isFresh(recheck)) {
                future.complete(recheck);
                return recheck;
            }

            Optional<InfoNaruLibrarySearchResult> fetched = concurrencyLimiter.search(
                    key.isbn(), key.region(), key.dtlRegion(), key.page(), key.size());
            if (fetched.isEmpty() || isFailure(fetched.get())) {
                future.complete(null);
                return null;
            }

            CachedLibrarySearchResult toCache = new CachedLibrarySearchResult(fetched.get(), Instant.now());
            libraryHoldingCache.put(key, toCache);
            future.complete(toCache);
            return toCache;
        } catch (Throwable t) {
            // RuntimeException만 잡으면 AssertionError, LinkageError 같은 Error는 여기서 빠져나가
            // future가 끝내 완료되지 않는다. finally에서 inFlight는 정리되지만, 이미 같은 future를
            // 붙잡고 join() 중인 follower는 그 future가 영원히 완료되지 않으므로 무한 대기하게 된다.
            // Throwable을 전부 잡아 future를 반드시 완료시켜야 follower가 풀려난다.
            log.warn("정보나루 조회 중 예기치 못한 내부 오류 발생, exceptionType={}", t.getClass().getSimpleName());
            future.completeExceptionally(t);
            if (t instanceof RuntimeException re) {
                throw re;
            }
            if (t instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("정보나루 조회 중 예기치 못한 예외", t);
        } finally {
            inFlight.remove(key, future);
        }
    }

    private CachedLibrarySearchResult joinFuture(CompletableFuture<CachedLibrarySearchResult> future) {
        try {
            return future.join();
        } catch (java.util.concurrent.CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw e;
        }
    }

    // NOT_TRIED(비활성/키 없음)와 FAILED_RETRYABLE(호출·파싱 실패) 모두 실패 흐름으로 처리한다.
    private boolean isFailure(InfoNaruLibrarySearchResult result) {
        return result.status() == InfoNaruLibraryFetchStatus.NOT_TRIED
                || result.status() == InfoNaruLibraryFetchStatus.FAILED_RETRYABLE;
    }

    private boolean isFresh(CachedLibrarySearchResult cached) {
        return Duration.between(cached.fetchedAt(), Instant.now()).compareTo(FRESH_TTL) <= 0;
    }

    private LibrarySearchResponse buildInfoNaruResponse(Book book, LibraryCacheKey key,
                                                          InfoNaruLibrarySearchResult result, boolean stale,
                                                          List<String> warnings,
                                                          BigDecimal latitude, BigDecimal longitude) {
        List<LibrarySearchResponse.LibraryResultItem> items = result.items().stream()
                .map(item -> toResultItem(item, latitude, longitude))
                .sorted(distanceComparator(latitude, longitude))
                .toList();

        return new LibrarySearchResponse(
                book.getBookId(), book.getTitle(), book.getCoverImageUrl(), key.region(),
                "INFONARU", stale, key.page(), key.size(), result.totalCount(),
                computeTotalPages(result.totalCount(), key.size()), items, warnings);
    }

    private LibrarySearchResponse buildManualResponse(Book book, String region, Page<LibraryHolding> manualPage,
                                                        List<String> warnings,
                                                        BigDecimal latitude, BigDecimal longitude) {
        List<LibrarySearchResponse.LibraryResultItem> items = manualPage.getContent().stream()
                .map(holding -> toResultItem(holding.getLibrary(), latitude, longitude))
                .sorted(distanceComparator(latitude, longitude))
                .toList();

        return new LibrarySearchResponse(
                book.getBookId(), book.getTitle(), book.getCoverImageUrl(), region,
                "MANUAL", false, manualPage.getNumber(), manualPage.getSize(),
                manualPage.getTotalElements(), manualPage.getTotalPages(), items, warnings);
    }

    private LibrarySearchResponse.LibraryResultItem toResultItem(InfoNaruLibraryItem item,
                                                                   BigDecimal latitude, BigDecimal longitude) {
        return new LibrarySearchResponse.LibraryResultItem(
                item.libraryCode(), item.libraryName(), item.address(), item.latitude(), item.longitude(),
                distanceKm(latitude, longitude, item.latitude(), item.longitude()),
                item.homepageUrl(), item.closedDays(), item.operationTime(), true, null);
    }

    private LibrarySearchResponse.LibraryResultItem toResultItem(com.acorn.abc.library.domain.Library library,
                                                                   BigDecimal latitude, BigDecimal longitude) {
        return new LibrarySearchResponse.LibraryResultItem(
                library.getLibraryCode(), library.getLibraryName(), library.getAddress(),
                library.getLatitude(), library.getLongitude(),
                distanceKm(latitude, longitude, library.getLatitude(), library.getLongitude()),
                null, null, null, true, null);
    }

    private Comparator<LibrarySearchResponse.LibraryResultItem> distanceComparator(BigDecimal latitude,
                                                                                     BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            return (a, b) -> 0;
        }
        return Comparator.comparing(
                LibrarySearchResponse.LibraryResultItem::distanceKm,
                Comparator.nullsLast(Double::compareTo));
    }

    private Double distanceKm(BigDecimal userLat, BigDecimal userLng, BigDecimal targetLat, BigDecimal targetLng) {
        if (userLat == null || userLng == null || targetLat == null || targetLng == null) {
            return null;
        }
        double lat1 = Math.toRadians(userLat.doubleValue());
        double lat2 = Math.toRadians(targetLat.doubleValue());
        double deltaLat = Math.toRadians(targetLat.doubleValue() - userLat.doubleValue());
        double deltaLng = Math.toRadians(targetLng.doubleValue() - userLng.doubleValue());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS_KM * c;
        return Math.round(distance * 100) / 100.0;
    }

    private int computeTotalPages(long totalElements, int size) {
        if (size <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElements / size);
    }

    private void validateRegion(String region, String dtlRegion) {
        if (region == null || !region.matches("^\\d{2}$")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        if (dtlRegion != null && (!dtlRegion.matches("^\\d{5}$") || !dtlRegion.startsWith(region))) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }
}
