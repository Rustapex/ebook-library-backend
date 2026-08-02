package com.acorn.abc.admin.collection.controller;

import com.acorn.abc.admin.collection.dto.AdminCollectionBooksUpdateRequest;
import com.acorn.abc.admin.collection.dto.AdminCollectionBooksUpdateResponse;
import com.acorn.abc.admin.collection.dto.AdminCollectionDetailResponse;
import com.acorn.abc.admin.collection.dto.AdminCollectionPatchRequest;
import com.acorn.abc.admin.collection.dto.AdminCollectionPatchResponse;
import com.acorn.abc.admin.collection.dto.AdminCollectionSaveRequest;
import com.acorn.abc.admin.collection.dto.AdminCollectionSaveResponse;
import com.acorn.abc.admin.collection.dto.AdminCollectionSummaryResponse;
import com.acorn.abc.collection.service.AdminCollectionService;
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

@Tag(name = "AdminCollection", description = "관리자 컬렉션 API")
@RestController
@RequestMapping("/api/v1/admin/collections")
@RequiredArgsConstructor
@Validated
public class AdminCollectionController {

    private final AdminCollectionService adminCollectionService;

    @Operation(summary = "컬렉션 목록 조회", description = "type/status로 필터링한 관리자 컬렉션 목록을 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminCollectionSummaryResponse>>> getCollections(
            @RequestParam(required = false) String collectionType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "size는 1 이상이어야 합니다.")
            @Max(value = 100, message = "size는 100 이하여야 합니다.") int size) {
        PageResponse<AdminCollectionSummaryResponse> response =
                adminCollectionService.getAdminCollections(collectionType, status, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "컬렉션 상세 조회", description = "HIDDEN/ENDED 컬렉션과 비공개 도서 mapping까지 포함해 반환합니다.")
    @GetMapping("/{collectionId}")
    public ResponseEntity<ApiResponse<AdminCollectionDetailResponse>> getCollectionDetail(
            @PathVariable Long collectionId) {
        return ResponseEntity.ok(ApiResponse.ok(adminCollectionService.getAdminCollectionDetail(collectionId)));
    }

    @Operation(summary = "컬렉션 생성", description = "신규 컬렉션을 생성합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<AdminCollectionSaveResponse>> createCollection(
            @Valid @RequestBody AdminCollectionSaveRequest request,
            @AuthenticationPrincipal CustomUserDetails adminDetails) {
        AdminCollectionSaveResponse response =
                adminCollectionService.createAdminCollection(request, adminDetails.getMemberId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @Operation(summary = "컬렉션 수정", description = "기존 컬렉션의 기본 정보를 수정합니다. 신규 생성은 POST를 사용합니다.")
    @PutMapping("/{collectionId}")
    public ResponseEntity<ApiResponse<AdminCollectionSaveResponse>> updateCollection(
            @PathVariable Long collectionId,
            @Valid @RequestBody AdminCollectionSaveRequest request,
            @AuthenticationPrincipal CustomUserDetails adminDetails) {
        AdminCollectionSaveResponse response =
                adminCollectionService.updateAdminCollection(collectionId, request, adminDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "컬렉션 도서 추가·정렬", description = "전달된 도서만 추가하거나 순서를 갱신합니다. 미포함 기존 도서는 유지됩니다.")
    @PutMapping("/{collectionId}/books")
    public ResponseEntity<ApiResponse<AdminCollectionBooksUpdateResponse>> updateCollectionBooks(
            @PathVariable Long collectionId,
            @Valid @RequestBody AdminCollectionBooksUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails adminDetails) {
        AdminCollectionBooksUpdateResponse response =
                adminCollectionService.upsertCollectionBooks(collectionId, request, adminDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "컬렉션 상태 변경·도서 제외", description = "status 변경과 removeBookIds 제거 중 하나 이상을 처리합니다.")
    @PatchMapping("/{collectionId}")
    public ResponseEntity<ApiResponse<AdminCollectionPatchResponse>> patchCollection(
            @PathVariable Long collectionId,
            @Valid @RequestBody AdminCollectionPatchRequest request,
            @AuthenticationPrincipal CustomUserDetails adminDetails) {
        AdminCollectionPatchResponse response =
                adminCollectionService.patchCollection(collectionId, request, adminDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
