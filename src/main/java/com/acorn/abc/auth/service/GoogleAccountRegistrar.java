package com.acorn.abc.auth.service;

import com.acorn.abc.auth.domain.OauthAccount;
import com.acorn.abc.auth.dto.GoogleSignupRequest;
import com.acorn.abc.auth.repository.OauthAccountRepository;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.domain.MemberGrade;
import com.acorn.abc.member.repository.MemberGradeRepository;
import com.acorn.abc.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Google 계정 연결의 쓰기 경로.
 *
 * <p>{@link GoogleAuthService}와 별도 bean으로 둔 이유는 transaction 경계 때문이다. 제약 위반이
 * 발생하면 해당 transaction은 rollback-only가 되어 같은 transaction 안에서 다시 조회할 수 없다.
 * 쓰기를 이 bean의 transaction으로 끝내고, 충돌 해소 조회는 호출자가 새 transaction에서 수행한다.
 *
 * <p>제약 위반을 감지하려면 transaction 종료 시점이 아니라 메서드 안에서 예외가 나야 하므로
 * {@code saveAndFlush}를 사용한다.
 */
@Service
@RequiredArgsConstructor
public class GoogleAccountRegistrar {

    // TODO: 기본 등급 정책 확정 전까지 grade_id=1 사용 (AuthService.signup과 동일)
    private static final long DEFAULT_GRADE_ID = 1L;

    private final MemberRepository memberRepository;
    private final MemberGradeRepository memberGradeRepository;
    private final OauthAccountRepository oauthAccountRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원과 외부 계정 연결을 한 transaction에서 만든다. 둘 중 하나만 남는 상태가 생기지 않는다.
     *
     * <p>email은 요청이 아니라 서버가 서명한 transition token claim에서 온다. name은 Google이 항상
     * 주지는 않으므로 검증된 요청 값을 쓴다(GoogleSignupRequest 참고). 신원 식별자는 email/sub뿐이다.
     *
     * <p>password/passwordConfirm 일치 검증은 호출자({@code GoogleAuthService.completeSignup})가
     * 이 메서드에 도달하기 전에 수행한다. 여기에 두면 멱등 경로에서 검증이 통째로 건너뛰어진다.
     */
    @Transactional
    public Member createMemberWithOauthAccount(TransitionTokenClaims claims, GoogleSignupRequest request) {
        if (memberRepository.existsByLoginId(request.loginId())) {
            throw new BusinessException(ErrorCode.MEMBER_LOGIN_ID_DUPLICATED);
        }
        if (memberRepository.existsByEmail(claims.email())) {
            throw new BusinessException(ErrorCode.MEMBER_EMAIL_DUPLICATED);
        }

        MemberGrade defaultGrade = memberGradeRepository.findById(DEFAULT_GRADE_ID)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_NOT_FOUND));

        Member member = memberRepository.saveAndFlush(Member.create(
                request.loginId(),
                passwordEncoder.encode(request.password()),
                request.name(),
                claims.email(),
                request.phone(),
                request.birthDate(),
                request.gender(),
                defaultGrade
        ));

        oauthAccountRepository.saveAndFlush(OauthAccount.link(
                member, claims.provider(), claims.providerUserId(), claims.email()));

        return member;
    }

    /**
     * 비밀번호 재인증을 마친 기존 회원에 외부 계정을 연결한다.
     */
    @Transactional
    public void linkOauthAccount(Member member, TransitionTokenClaims claims) {
        oauthAccountRepository.saveAndFlush(OauthAccount.link(
                member, claims.provider(), claims.providerUserId(), claims.email()));
    }

    /**
     * provider가 알려준 email이 바뀐 경우 snapshot만 갱신한다. 로그인 성공 경로에서만 호출한다.
     */
    @Transactional
    public void refreshProviderEmail(Long oauthAccountId, String providerEmail) {
        oauthAccountRepository.findById(oauthAccountId)
                .ifPresent(oauthAccount -> oauthAccount.updateProviderEmail(providerEmail));
    }
}
