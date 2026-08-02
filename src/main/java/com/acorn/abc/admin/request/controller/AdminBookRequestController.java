package com.acorn.abc.admin.request.controller;

import com.acorn.abc.admin.request.dto.AdminBookRequestCandidateResponse;
import com.acorn.abc.admin.request.dto.AdminBookRequestStatusUpdateRequest;
import com.acorn.abc.admin.request.dto.AdminBookRequestStatusUpdateResponse;
import com.acorn.abc.admin.request.service.AdminBookRequestService;
import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.common.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AdminBookRequest", description = "관리자 희망도서 API")
@RestController
@RequestMapping("/api/v1/admin/book-request-candidates")
@RequiredArgsConstructor
public class AdminBookRequestController {

    private final AdminBookRequestService adminBookRequestService;

    @Operation(summary = "희망도서 후보 목록 조회", description = "status/q로 필터링합니다. 정렬 기준은 latestRequestedAt DESC, candidateId DESC입니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminBookRequestCandidateResponse>>> getCandidates(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        PageResponse<AdminBookRequestCandidateResponse> response =
                adminBookRequestService.getCandidates(status, q, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "희망도서 후보 상태 변경", description = "IN_REVIEW/APPROVED/REJECTED로 전환합니다. 이미 종료된 후보는 409입니다.")
    @PatchMapping("/{candidateId}/status")
    public ResponseEntity<ApiResponse<AdminBookRequestStatusUpdateResponse>> updateStatus(
            @PathVariable Long candidateId,
            @Valid @RequestBody AdminBookRequestStatusUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails adminDetails
    ) {
        AdminBookRequestStatusUpdateResponse response =
                adminBookRequestService.updateStatus(adminDetails.getMemberId(), candidateId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
