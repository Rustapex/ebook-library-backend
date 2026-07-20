package com.acorn.abc.admin.book.controller;

import com.acorn.abc.admin.book.dto.AdminBookDetailResponse;
import com.acorn.abc.admin.book.dto.AdminBookSaveRequest;
import com.acorn.abc.admin.book.dto.AdminBookSaveResponse;
import com.acorn.abc.admin.book.dto.AdminBookStatusChangeRequest;
import com.acorn.abc.admin.book.dto.AdminBookStatusChangeResponse;
import com.acorn.abc.admin.book.dto.AdminBookSummaryResponse;
import com.acorn.abc.book.service.AdminBookService;
import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.common.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AdminBook", description = "관리자 도서 API")
@RestController
@RequestMapping("/api/v1/admin/books")
@RequiredArgsConstructor
@Validated
public class AdminBookController {

    private final AdminBookService adminBookService;

    @Operation(summary = "도서 목록 조회", description = "q/categoryId/rentalType/status로 필터링한 관리자 도서 목록을 반환합니다. HIDDEN/INACTIVE도 포함됩니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminBookSummaryResponse>>> getBooks(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String rentalType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size는 1 이상이어야 합니다.")
            @Max(value = 100, message = "size는 100 이하여야 합니다.") int size) {
        PageResponse<AdminBookSummaryResponse> response =
                adminBookService.getAdminBooks(q, categoryId, rentalType, status, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "도서 상세 조회", description = "허용된 메타정보·작가·카테고리·안내성 상세를 반환합니다. HIDDEN/INACTIVE도 조회됩니다.")
    @GetMapping("/{bookId}")
    public ResponseEntity<ApiResponse<AdminBookDetailResponse>> getBookDetail(@PathVariable Long bookId) {
        return ResponseEntity.ok(ApiResponse.ok(adminBookService.getAdminBookDetail(bookId)));
    }

    @Operation(summary = "도서 등록", description = "허용된 메타정보만으로 신규 도서를 등록합니다. 신규 도서는 항상 INACTIVE로 생성됩니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<AdminBookSaveResponse>> createBook(
            @Valid @RequestBody AdminBookSaveRequest request,
            @AuthenticationPrincipal CustomUserDetails adminDetails) {
        AdminBookSaveResponse response = adminBookService.createAdminBook(request, adminDetails.getMemberId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @Operation(summary = "도서 수정", description = "허용된 메타정보를 전체 교체합니다. 실 열람 원문·페이지·키워드·파일은 변경하지 않습니다.")
    @PutMapping("/{bookId}")
    public ResponseEntity<ApiResponse<AdminBookSaveResponse>> updateBook(
            @PathVariable Long bookId,
            @Valid @RequestBody AdminBookSaveRequest request,
            @AuthenticationPrincipal CustomUserDetails adminDetails) {
        AdminBookSaveResponse response =
                adminBookService.updateAdminBook(bookId, request, adminDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "도서 상태 변경", description = "물리 삭제 없이 AVAILABLE/HIDDEN/INACTIVE 상태만 변경합니다.")
    @PatchMapping("/{bookId}/status")
    public ResponseEntity<ApiResponse<AdminBookStatusChangeResponse>> changeBookStatus(
            @PathVariable Long bookId,
            @Valid @RequestBody AdminBookStatusChangeRequest request,
            @AuthenticationPrincipal CustomUserDetails adminDetails) {
        AdminBookStatusChangeResponse response =
                adminBookService.changeAdminBookStatus(bookId, request, adminDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
