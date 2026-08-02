package com.acorn.abc.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 같은 email의 기존 일반 회원에 Google 계정을 연결하기 위한 재인증 입력.
 *
 * <p>연결 대상 회원은 LINK transition token의 claim으로 고정된다. 요청 body의 값으로
 * 다른 회원을 지정할 수 없다.
 *
 * <p>현재 access token만으로는 연결할 수 없다. 반드시 기존 비밀번호를 다시 확인한다.
 */
public record GoogleLinkRequest(

        @NotBlank(message = "인증 정보가 필요합니다.")
        String transitionToken,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {}
