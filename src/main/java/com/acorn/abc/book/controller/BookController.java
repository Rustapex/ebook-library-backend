package com.acorn.abc.book.controller;

import com.acorn.abc.book.dto.BookCardResponse;
import com.acorn.abc.book.dto.BookDetailResponse;
import com.acorn.abc.book.dto.BookRecommendationResponse;
import com.acorn.abc.book.dto.BookSearchResponse;
import com.acorn.abc.book.service.BookService;
import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.common.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Books", description = "Book public APIs")
@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookController {

    private static final String ANONYMOUS_IF_NOT_LOGGED_IN = "#this == 'anonymousUser' ? null : #this";

    private final BookService bookService;

    @Operation(summary = "Get books", description = "Returns available books for public user browsing.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BookCardResponse>>> getBooks(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long parentCategoryId,
            @RequestParam(required = false) Long collectionId,
            @RequestParam(required = false) String rentalType,
            @RequestParam(required = false) String section,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal(expression = ANONYMOUS_IF_NOT_LOGGED_IN) CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.ok(bookService.getBooks(
                categoryId,
                parentCategoryId,
                collectionId,
                rentalType,
                section,
                sort,
                page,
                size,
                memberId(userDetails)
        )));
    }

    @Operation(summary = "Search books", description = "Searches available books by title, author, publisher, ISBN, or keyword.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<BookSearchResponse>> searchBooks(
            @RequestParam String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String rentalType,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal(expression = ANONYMOUS_IF_NOT_LOGGED_IN) CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                bookService.searchBooks(q, categoryId, rentalType, sort, page, size, memberId(userDetails))
        ));
    }

    @Operation(summary = "Get book detail", description = "Returns a single available book detail.")
    @GetMapping("/{bookId}")
    public ResponseEntity<ApiResponse<BookDetailResponse>> getBookDetail(
            @PathVariable Long bookId,
            @AuthenticationPrincipal(expression = ANONYMOUS_IF_NOT_LOGGED_IN) CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.ok(bookService.getBookDetail(bookId, memberId(userDetails))));
    }

    @Operation(summary = "Get book recommendations", description = "Returns simple recommendations without saving recommendation logs.")
    @GetMapping("/recommendations")
    public ResponseEntity<ApiResponse<BookRecommendationResponse>> getRecommendations(
            @RequestParam(required = false) Long baseBookId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(ApiResponse.ok(bookService.getRecommendations(baseBookId, type, limit)));
    }

    private Long memberId(CustomUserDetails userDetails) {
        return userDetails == null ? null : userDetails.getMemberId();
    }
}
