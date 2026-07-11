package com.acorn.abc.favorite.service;

import com.acorn.abc.book.domain.Book;
import com.acorn.abc.book.dto.BookCardResponse;
import com.acorn.abc.book.service.BookService;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.favorite.domain.FavoriteBook;
import com.acorn.abc.favorite.dto.FavoriteDeleteResponse;
import com.acorn.abc.favorite.dto.FavoriteRequest;
import com.acorn.abc.favorite.dto.FavoriteResponse;
import com.acorn.abc.favorite.repository.FavoriteBookRepository;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private final MemberRepository memberRepository;
    private final FavoriteBookRepository favoriteBookRepository;
    private final BookService bookService;

    public FavoriteResponse addFavorite(Long memberId, FavoriteRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        // User API 공통 규칙: BOOK.status=AVAILABLE만 노출한다 (BookService에 위치를 통일)
        Book book = bookService.getAvailableBookOrThrow(request.bookId());

        if (favoriteBookRepository.existsByMember_MemberIdAndBook_BookId(memberId, request.bookId())) {
            throw new BusinessException(ErrorCode.FAVORITE_ALREADY_EXISTS);
        }

        FavoriteBook favoriteBook;
        try {
            favoriteBook = favoriteBookRepository.saveAndFlush(FavoriteBook.create(member, book));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.FAVORITE_ALREADY_EXISTS);
        }

        return new FavoriteResponse(favoriteBook.getFavoriteId(), book.getBookId());
    }

    public FavoriteDeleteResponse removeFavorite(Long memberId, Long bookId) {
        FavoriteBook favoriteBook = favoriteBookRepository.findByMember_MemberIdAndBook_BookId(memberId, bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FAVORITE_NOT_FOUND));
        favoriteBookRepository.delete(favoriteBook);
        return new FavoriteDeleteResponse(true);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookCardResponse> getMyFavorites(Long memberId, Integer page, Integer size, String sort) {
        normalizeSort(sort);
        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size));
        Page<FavoriteBook> favorites = favoriteBookRepository.findMyFavorites(memberId, pageable);
        var books = favorites.getContent().stream().map(FavoriteBook::getBook).toList();
        var content = bookService.toCardResponses(books, memberId);
        return PageResponse.of(content, favorites.getNumber(), favorites.getSize(), favorites.getTotalElements());
    }

    private int normalizePage(Integer page) {
        return page == null || page < 0 ? DEFAULT_PAGE : page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    // 현재 지원 sort는 latest뿐이다. 값이 없으면 기본값(latest)로 취급하고, 그 외 값은 VALIDATION_ERROR로 응답한다
    private String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return "latest";
        }
        if (!"latest".equals(sort.trim())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return "latest";
    }
}
