package com.acorn.abc.auth.service;

/**
 * transition token의 용도. 발급 시 claim으로 넣고 소비하는 endpoint에서 기대값과 일치하는지 검증한다.
 * SIGNUP token으로 link를 수행하거나 그 반대를 할 수 없다.
 */
public enum TransitionTokenPurpose {
    /** 신규 Google 사용자의 추가 가입(onboarding) 완료 전용. */
    SIGNUP,
    /** 동일 email 기존 회원의 계정 연결 전용. */
    LINK
}
