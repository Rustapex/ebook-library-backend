package com.acorn.abc.auth.dto;

/**
 * Google 인증 요청의 처리 결과 상태.
 *
 * <p>{@code AUTHENTICATED}만 로그인 완료다. 나머지 두 상태는 추가 절차가 남아 있으며
 * access token을 포함하지 않는다.
 */
public enum GoogleAuthStatus {
    /** 연결된 회원이 있어 access token을 발급했다. */
    AUTHENTICATED,
    /** 처음 보는 Google 계정이다. onboarding으로 회원 정보를 받아야 한다. */
    SIGNUP_REQUIRED,
    /** 같은 email의 기존 일반 회원이 있다. 비밀번호 재인증 후 연결해야 한다. */
    LINK_REQUIRED
}
