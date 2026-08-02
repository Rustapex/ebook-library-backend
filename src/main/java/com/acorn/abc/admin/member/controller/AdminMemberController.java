package com.acorn.abc.admin.member.controller;

import com.acorn.abc.admin.member.dto.AdminMemberDetailResponse;
import com.acorn.abc.admin.member.dto.AdminMemberDetailTab;
import com.acorn.abc.admin.member.dto.AdminMemberPaymentTabItemResponse;
import com.acorn.abc.admin.member.dto.AdminMemberRentalTabItemResponse;
import com.acorn.abc.admin.member.dto.AdminMemberReportTabItemResponse;
import com.acorn.abc.admin.member.dto.AdminMemberSanctionTabItemResponse;
import com.acorn.abc.admin.member.dto.AdminMemberStatusChangeRequest;
import com.acorn.abc.admin.member.dto.AdminMemberStatusChangeResponse;
import com.acorn.abc.admin.member.dto.AdminMemberSummaryResponse;
import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.common.security.CustomUserDetails;
import com.acorn.abc.member.dto.PointHistoryResponse;
import com.acorn.abc.member.service.AdminMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// API-ADMIN-MEMBER-001~003(구현계획 2026-07-16). 004(포인트 조정)는 AdminMemberPointController.
@Tag(name = "AdminMember", description = "관리자 회원 API")
@RestController
@RequestMapping("/api/v1/admin/members")
@RequiredArgsConstructor
@Validated
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    @Operation(summary = "회원 목록 조회", description = "검색/필터 조건으로 회원 목록을 조회합니다. 개인정보는 마스킹되어 반환됩니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminMemberSummaryResponse>>> getMembers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Long gradeId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        PageResponse<AdminMemberSummaryResponse> response =
                adminMemberService.getAdminMembers(q, status, role, gradeId, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // PR51 코드리뷰 대응 §2.3-2 — 실제 반환 타입은 tab에 따라 달라져 제네릭 하나로 표현할 수 없으므로,
    // Swagger 문서에는 탭별 실제 DTO를 oneOf로 명시해 모호한 Object 노출을 없앤다.
    @Operation(summary = "회원 상세 조회", description = "tab 파라미터로 기본 상세 또는 대여/결제/신고/포인트/제재 이력을 조회합니다.",
            responses = @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    content = @Content(schema = @Schema(oneOf = {
                            AdminMemberDetailResponse.class,
                            AdminMemberRentalTabItemResponse.class,
                            AdminMemberPaymentTabItemResponse.class,
                            AdminMemberReportTabItemResponse.class,
                            PointHistoryResponse.class,
                            AdminMemberSanctionTabItemResponse.class
                    }))))
    @GetMapping("/{memberId}")
    public ResponseEntity<ApiResponse<?>> getMemberDetail(
            @PathVariable Long memberId,
            @Parameter(description = "DEFAULT, RENTALS, PAYMENTS, REPORTS, POINTS, SANCTIONS 중 하나. 미지정 시 DEFAULT.")
            @RequestParam(required = false) AdminMemberDetailTab tab,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        AdminMemberDetailTab resolvedTab = tab == null ? AdminMemberDetailTab.DEFAULT : tab;
        Object body = switch (resolvedTab) {
            case DEFAULT -> adminMemberService.getAdminMemberDetail(memberId);
            case RENTALS -> adminMemberService.getAdminMemberRentals(memberId, page, size);
            case PAYMENTS -> adminMemberService.getAdminMemberPayments(memberId, page, size);
            case REPORTS -> adminMemberService.getAdminMemberReports(memberId, page, size);
            case POINTS -> adminMemberService.getAdminMemberPoints(memberId, page, size);
            case SANCTIONS -> adminMemberService.getAdminMemberSanctions(memberId, page, size);
        };
        return ResponseEntity.ok(ApiResponse.ok(body));
    }

    @Operation(summary = "회원 상태/제재 변경", description = "회원 상태를 변경하거나 제재 이력을 생성합니다. ADMIN 대상, 탈퇴/비활성 대상은 거부됩니다.")
    @PatchMapping("/{memberId}/status")
    public ResponseEntity<ApiResponse<AdminMemberStatusChangeResponse>> changeStatus(
            @PathVariable Long memberId,
            @Valid @RequestBody AdminMemberStatusChangeRequest request,
            @AuthenticationPrincipal CustomUserDetails adminDetails) {
        AdminMemberStatusChangeResponse response =
                adminMemberService.changeMemberStatus(memberId, request, adminDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
