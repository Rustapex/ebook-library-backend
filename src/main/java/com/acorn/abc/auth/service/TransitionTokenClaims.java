package com.acorn.abc.auth.service;

import com.acorn.abc.auth.domain.OauthProvider;

/**
 * transition token에서 복원한 server-signed claim.
 *
 * <p>onboarding/link 요청에서 frontend가 다시 보낸 email/sub/provider는 신뢰하지 않고
 * 오직 이 값만 사용한다.
 *
 * <p>{@code memberId}는 LINK 목적일 때만 채워진다. 연결 대상 회원을 발급 시점에 고정해
 * 재인증 단계에서 다른 회원으로 바꿔치기할 수 없게 한다.
 *
 * <p>주의: transition token은 서명만 되어 있고 암호화되지 않는다. 따라서 token 보유자는 payload를
 * base64 디코딩해 {@code memberId}와 {@code provider}/{@code purpose}를 읽을 수 있다. 보유자는 이미
 * 해당 email의 Google 계정 통제를 증명한 주체이므로 감수 가능한 노출로 판단했다. 더 민감한 값을
 * claim에 추가하지 않는다.
 */
public record TransitionTokenClaims(
        TransitionTokenPurpose purpose,
        OauthProvider provider,
        String providerUserId,
        String email,
        String name,
        Long memberId
) {}
