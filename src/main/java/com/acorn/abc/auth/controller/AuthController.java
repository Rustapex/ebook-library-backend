package com.acorn.abc.auth.controller;

import com.acorn.abc.auth.dto.CheckLoginIdRequest;
import com.acorn.abc.auth.dto.CheckLoginIdResponse;
import com.acorn.abc.auth.dto.FindIdRequest;
import com.acorn.abc.auth.dto.FindIdResponse;
import com.acorn.abc.auth.dto.LoginRequest;
import com.acorn.abc.auth.dto.LoginResponse;
import com.acorn.abc.auth.dto.SignupRequest;
import com.acorn.abc.auth.dto.SignupResponse;
import com.acorn.abc.auth.service.AuthService;
import com.acorn.abc.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입", description = "신규 회원을 등록합니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @Operation(summary = "로그인", description = "아이디와 비밀번호로 로그인합니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "아이디 찾기", description = "이름과 이메일로 아이디를 조회합니다.")
    @PostMapping("/find-id")
    public ResponseEntity<ApiResponse<FindIdResponse>> findId(@Valid @RequestBody FindIdRequest request) {
        FindIdResponse response = authService.findId(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "로그인 아이디 중복 확인", description = "회원가입 전 로그인 아이디의 형식과 중복 여부를 확인합니다.")
    @GetMapping("/check-login-id")
    public ResponseEntity<ApiResponse<CheckLoginIdResponse>> checkLoginId(
            @Valid @ModelAttribute CheckLoginIdRequest request) {
        CheckLoginIdResponse response = authService.checkLoginId(request.loginId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
