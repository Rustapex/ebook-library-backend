package com.acorn.abc.favorite.controller;

import com.acorn.abc.book.dto.BookCardResponse;
import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.common.security.CustomUserDetails;
import com.acorn.abc.favorite.dto.FavoriteDeleteResponse;
import com.acorn.abc.favorite.dto.FavoriteRequest;
import com.acorn.abc.favorite.dto.FavoriteResponse;
import com.acorn.abc.favorite.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Favorites", description = "즐겨찾기 API")
@RestController
@RequestMapping("/api/v1/me/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "즐겨찾기 등록", description = "회원+도서 기준 1개만 등록됩니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<FavoriteResponse>> addFavorite(
            @Valid @RequestBody FavoriteRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        FavoriteResponse response = favoriteService.addFavorite(userDetails.getMemberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @Operation(summary = "즐겨찾기 삭제", description = "본인 즐겨찾기를 삭제합니다.")
    @DeleteMapping("/{bookId}")
    public ResponseEntity<ApiResponse<FavoriteDeleteResponse>> removeFavorite(
            @PathVariable Long bookId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        FavoriteDeleteResponse response = favoriteService.removeFavorite(userDetails.getMemberId(), bookId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "즐겨찾기 목록 조회", description = "본인 즐겨찾기 도서 목록을 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BookCardResponse>>> getMyFavorites(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PageResponse<BookCardResponse> response = favoriteService.getMyFavorites(userDetails.getMemberId(), page, size, sort);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
