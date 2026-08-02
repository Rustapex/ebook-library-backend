package com.acorn.abc.auth.controller;

import com.acorn.abc.auth.dto.GoogleAuthResponse;
import com.acorn.abc.auth.dto.GoogleLinkRequest;
import com.acorn.abc.auth.dto.GoogleLoginRequest;
import com.acorn.abc.auth.dto.GoogleSignupRequest;
import com.acorn.abc.auth.service.GoogleAuthService;
import com.acorn.abc.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Google 로그인 API.
 *
 * <p>transition token은 query parameter가 아니라 request body로만 받는다. URL은 log, referrer,
 * 브라우저 history에 남기 때문이다.
 */
@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/v1/auth/google")
@RequiredArgsConstructor
public class GoogleAuthController {

    private final GoogleAuthService googleAuthService;

    @Operation(summary = "Google 로그인",
            description = "Google ID token을 검증한다. 연결된 회원이면 AUTHENTICATED와 access token을, "
                    + "처음 보는 계정이면 SIGNUP_REQUIRED를, 같은 이메일의 기존 회원이 있으면 LINK_REQUIRED를 반환한다. "
                    + "SIGNUP_REQUIRED와 LINK_REQUIRED는 로그인 완료가 아니며 access token을 포함하지 않는다.")
    @PostMapping
    public ResponseEntity<ApiResponse<GoogleAuthResponse>> authenticate(
            @Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(googleAuthService.authenticate(request)));
    }

    @Operation(summary = "Google 신규 가입 완료",
            description = "SIGNUP_REQUIRED로 받은 transition token과 추가 회원 정보로 가입을 완료하고 access token을 발급한다. "
                    + "이름과 이메일은 요청이 아니라 transition token의 서버 서명 claim에서 가져온다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<GoogleAuthResponse>> completeSignup(
            @Valid @RequestBody GoogleSignupRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(googleAuthService.completeSignup(request)));
    }

    @Operation(summary = "Google 계정 연결 완료",
            description = "LINK_REQUIRED로 받은 transition token과 기존 비밀번호 재인증으로 Google 계정을 연결하고 "
                    + "access token을 발급한다. 현재 access token만으로는 연결할 수 없다.")
    @PostMapping("/link")
    public ResponseEntity<ApiResponse<GoogleAuthResponse>> completeLink(
            @Valid @RequestBody GoogleLinkRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(googleAuthService.completeLink(request)));
    }
}
