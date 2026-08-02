package com.acorn.abc.member.controller;

import com.acorn.abc.common.response.ApiResponse;
import com.acorn.abc.common.security.CustomUserDetails;
import com.acorn.abc.member.dto.MeInfoResponse;
import com.acorn.abc.member.dto.MemberUpdateRequest;
import com.acorn.abc.member.dto.MemberWithdrawRequest;
import com.acorn.abc.member.dto.MemberWithdrawResponse;
import com.acorn.abc.member.dto.PasswordChangeRequest;
import com.acorn.abc.member.dto.PasswordChangeResponse;
import com.acorn.abc.member.service.MeInfoService;
import com.acorn.abc.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Me", description = "내 정보 API")
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final MemberService memberService;
    private final MeInfoService meInfoService;

    @Operation(summary = "내 정보 조회", description = "로그인한 회원의 기본 정보와 마이페이지 요약을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<MeInfoResponse>> getMe(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(meInfoService.getMe(userDetails.getMemberId())));
    }

    @Operation(summary = "내 정보 수정", description = "로그인한 회원의 이름/이메일/전화번호를 수정합니다.")
    @PatchMapping
    public ResponseEntity<ApiResponse<MeInfoResponse>> updateMe(
            @Valid @RequestBody MemberUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(memberService.updateMyInfo(userDetails.getMemberId(), request)));
    }

    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호 확인 후 새 비밀번호로 변경합니다.")
    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<PasswordChangeResponse>> changePassword(
            @Valid @RequestBody PasswordChangeRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PasswordChangeResponse response = memberService.changePassword(userDetails.getMemberId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "회원 탈퇴", description = "현재 비밀번호 확인 후 본인 계정을 탈퇴 처리합니다. 진행 중인 대여가 있으면 거부됩니다.")
    @DeleteMapping
    public ResponseEntity<ApiResponse<MemberWithdrawResponse>> withdraw(
            @Valid @RequestBody MemberWithdrawRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MemberWithdrawResponse response = memberService.withdraw(userDetails.getMemberId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
