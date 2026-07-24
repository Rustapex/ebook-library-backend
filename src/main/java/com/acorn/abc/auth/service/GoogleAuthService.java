package com.acorn.abc.auth.service;

import com.acorn.abc.auth.config.GoogleAuthProperties;
import com.acorn.abc.auth.domain.OauthAccount;
import com.acorn.abc.auth.domain.OauthProvider;
import com.acorn.abc.auth.dto.GoogleAuthResponse;
import com.acorn.abc.auth.dto.GoogleLinkRequest;
import com.acorn.abc.auth.dto.GoogleLoginRequest;
import com.acorn.abc.auth.dto.GoogleSignupRequest;
import com.acorn.abc.auth.dto.LoginResponse;
import com.acorn.abc.auth.repository.OauthAccountRepository;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.security.JwtTokenProvider;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Google 로그인 orchestration.
 *
 * <p>정책:
 * <ul>
 *   <li>Google {@code sub}가 외부 identity의 유일한 기준이다. email로 계정을 자동 연결하지 않는다.</li>
 *   <li>처음 보는 sub이고 같은 email의 회원도 없으면 {@code SIGNUP_REQUIRED}로 onboarding을 요구한다.</li>
 *   <li>같은 email의 기존 회원이 있으면 {@code LINK_REQUIRED}로 비밀번호 재인증을 요구한다.</li>
 *   <li>{@code status != JOINED} 계정은 로그인과 연결을 모두 거부한다.</li>
 * </ul>
 *
 * <p>class 수준 {@code @Transactional}을 붙이지 않는다. 제약 위반 후 충돌 row를 다시 읽어야 하는데,
 * rollback-only가 된 transaction 안에서는 그 조회를 할 수 없기 때문이다. 쓰기는
 * {@link GoogleAccountRegistrar}의 transaction에서 끝내고 해소 조회는 여기서 새로 수행한다.
 */
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthService.class);

    private static final OauthProvider PROVIDER = OauthProvider.GOOGLE;
    private static final String ACTIVE_MEMBER_STATUS = "JOINED";

    private final GoogleAuthProperties googleAuthProperties;
    // 비활성화 상태에서는 verifier bean 자체가 만들어지지 않는다(JWKS 요청도 없음).
    private final ObjectProvider<GoogleIdTokenVerifier> googleIdTokenVerifier;
    private final TransitionTokenProvider transitionTokenProvider;
    private final GoogleAccountRegistrar googleAccountRegistrar;
    private final OauthAccountRepository oauthAccountRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public GoogleAuthResponse authenticate(GoogleLoginRequest request) {
        GoogleIdentity identity = verifier().verify(request.idToken());

        Optional<OauthAccount> linked =
                oauthAccountRepository.findByProviderAndProviderUserId(PROVIDER, identity.subject());
        if (linked.isPresent()) {
            return GoogleAuthResponse.authenticated(loginLinkedAccount(linked.get(), identity));
        }

        // 아직 연결되지 않은 sub다. email이 같다는 이유만으로 자동 연결하지 않는다.
        Optional<Member> sameEmailMember = memberRepository.findByEmail(identity.email());
        if (sameEmailMember.isPresent()) {
            // status != JOINED는 AUTH_LOGIN_FAILED로 수렴시킨다. 구분되는 code를 주면 "이 email로
            // 탈퇴/제재 계정이 존재한다"는 계정 열거가 되므로 의도적으로 구분하지 않는다.
            Member member = requireActive(sameEmailMember.get());
            // 이 회원이 이미 다른 Google 계정과 연결돼 있으면 연결이 성립할 수 없다(uk_oauth_account_member_id_provider).
            // 여기서 걸러내지 않으면 completeLink에서만 409가 나서, 사용자가 비밀번호를 정확히 입력해도
            // 실패하는 "상환 불가능한 LINK token"을 발급하게 된다.
            if (oauthAccountRepository.existsByMemberMemberIdAndProvider(member.getMemberId(), PROVIDER)) {
                throw new BusinessException(ErrorCode.AUTH_OAUTH_ALREADY_LINKED);
            }
            return GoogleAuthResponse.linkRequired(
                    transitionTokenProvider.issueLinkToken(PROVIDER, identity, member.getMemberId()),
                    transitionTokenProvider.getExpirationSeconds(),
                    identity.email(),
                    identity.name());
        }

        return GoogleAuthResponse.signupRequired(
                transitionTokenProvider.issueSignupToken(PROVIDER, identity),
                transitionTokenProvider.getExpirationSeconds(),
                identity.email(),
                identity.name());
    }

    public GoogleAuthResponse completeSignup(GoogleSignupRequest request) {
        requireEnabled();
        TransitionTokenClaims claims =
                transitionTokenProvider.verify(request.transitionToken(), TransitionTokenPurpose.SIGNUP);

        // 멱등 경로에서도 건너뛰지 않도록 쓰기 시도 전에 검증한다.
        if (!request.password().equals(request.passwordConfirm())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        // 이 sub가 이미 연결돼 있다면 이 signup token은 이미 소비된 것이다. 여기서 access token을
        // 재발급하면 transition token이 만료 전까지 반복 사용 가능한 로그인 credential이 된다
        // (signup 경로는 비밀번호를 요구하지 않으므로 재사용 비용이 0이다). 재시도는 client가
        // /api/v1/auth/google을 다시 호출해 Google ID token을 재검증하는 경로로만 허용한다.
        if (oauthAccountRepository.findByProviderAndProviderUserId(PROVIDER, claims.providerUserId())
                .isPresent()) {
            throw new BusinessException(ErrorCode.AUTH_OAUTH_ALREADY_LINKED);
        }

        Member member;
        try {
            member = googleAccountRegistrar.createMemberWithOauthAccount(claims, request);
        } catch (DataIntegrityViolationException e) {
            member = resolveSignupConflict(claims, request, e);
        }
        return GoogleAuthResponse.authenticated(issueLogin(member));
    }

    public GoogleAuthResponse completeLink(GoogleLinkRequest request) {
        requireEnabled();
        TransitionTokenClaims claims =
                transitionTokenProvider.verify(request.transitionToken(), TransitionTokenPurpose.LINK);

        // 회원이 사라진 경우와 비밀번호 불일치를 구분하면 계정 존재 여부가 드러난다. 응답을 통일한다.
        Member member = requireActive(memberRepository.findById(claims.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_LOGIN_FAILED)));

        // 현재 access token이 아니라 기존 비밀번호를 다시 확인한다. 실패 원인은 노출하지 않는다.
        if (!passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new BusinessException(ErrorCode.AUTH_LOGIN_FAILED);
        }

        Optional<OauthAccount> existing =
                oauthAccountRepository.findByProviderAndProviderUserId(PROVIDER, claims.providerUserId());
        if (existing.isPresent()) {
            // 같은 sub가 같은 회원에 이미 연결됐으면 재시도로 보고 멱등 처리한다.
            requireSameLink(existing.get(), member.getMemberId());
            return GoogleAuthResponse.authenticated(issueLogin(member));
        }
        if (oauthAccountRepository.existsByMemberMemberIdAndProvider(member.getMemberId(), PROVIDER)) {
            // 이 회원은 이미 다른 Google 계정과 연결돼 있다.
            throw new BusinessException(ErrorCode.AUTH_OAUTH_ALREADY_LINKED);
        }

        try {
            googleAccountRegistrar.linkOauthAccount(member, claims);
        } catch (DataIntegrityViolationException e) {
            // 동시 요청이 먼저 넣었다. 두 UNIQUE 중 어느 쪽이 걸렸는지 재조회로 확정한다.
            // sub 측(uk_oauth_account_provider_provider_user_id) 위반이면 같은 회원일 때만 멱등 성공,
            // 회원 측(uk_oauth_account_member_id_provider) 위반이면 다른 Google 계정이 먼저 연결된 것이므로 409다.
            log.warn("google link conflict for memberId={} provider={}", member.getMemberId(), PROVIDER, e);
            Optional<OauthAccount> conflicting =
                    oauthAccountRepository.findByProviderAndProviderUserId(PROVIDER, claims.providerUserId());
            if (conflicting.isPresent()) {
                requireSameLink(conflicting.get(), member.getMemberId());
            } else if (oauthAccountRepository.existsByMemberMemberIdAndProvider(member.getMemberId(), PROVIDER)) {
                throw new BusinessException(ErrorCode.AUTH_OAUTH_ALREADY_LINKED);
            } else {
                // 두 UNIQUE 어느 쪽으로도 설명되지 않는 위반은 원인을 단정하지 않고 전파한다.
                throw e;
            }
        }
        return GoogleAuthResponse.authenticated(issueLogin(member));
    }

    private LoginResponse loginLinkedAccount(OauthAccount oauthAccount, GoogleIdentity identity) {
        Member member = requireActive(oauthAccount.getMember());
        if (!identity.email().equals(oauthAccount.getProviderEmail())) {
            googleAccountRegistrar.refreshProviderEmail(oauthAccount.getOauthAccountId(), identity.email());
        }
        return issueLogin(member);
    }

    /**
     * 제약 위반의 실제 원인을 다시 읽어 판정한다. DB constraint 이름이나 예외 원문은 응답하지 않는다.
     *
     * <p>여기 도달했다는 것은 사전 조회에서 sub가 없었는데 쓰기에서 충돌했다는 뜻이므로 실제 동시 요청이다.
     * 재사용된 token은 {@code completeSignup}의 사전 조회에서 이미 409로 걸러진다.
     */
    private Member resolveSignupConflict(TransitionTokenClaims claims, GoogleSignupRequest request,
                                         DataIntegrityViolationException cause) {
        Optional<OauthAccount> linked =
                oauthAccountRepository.findByProviderAndProviderUserId(PROVIDER, claims.providerUserId());
        if (linked.isPresent()) {
            // 동일 provider + 동일 sub로 회원이 방금 만들어졌다. 중복 생성 없이 그 회원으로 로그인시킨다.
            return requireActive(linked.get().getMember());
        }
        if (memberRepository.existsByLoginId(request.loginId())) {
            throw new BusinessException(ErrorCode.MEMBER_LOGIN_ID_DUPLICATED);
        }
        if (memberRepository.existsByEmail(claims.email())) {
            throw new BusinessException(ErrorCode.MEMBER_EMAIL_DUPLICATED);
        }
        // 원인을 확정하지 못한 위반을 "이미 연결된 소셜 계정"으로 단정하면 실제 원인이 영구히 은폐된다.
        // 원본 예외를 전파해 GlobalExceptionHandler가 500으로 처리하게 하고, 진단 근거를 log에 남긴다.
        // (원문은 응답에 노출되지 않는다)
        log.warn("google signup conflict unresolved for provider={} sub-hash={}",
                PROVIDER, claims.providerUserId().hashCode(), cause);
        throw cause;
    }

    private void requireSameLink(OauthAccount oauthAccount, Long memberId) {
        if (!oauthAccount.isLinkedTo(memberId)) {
            throw new BusinessException(ErrorCode.AUTH_OAUTH_ALREADY_LINKED);
        }
    }

    private Member requireActive(Member member) {
        if (member == null || !ACTIVE_MEMBER_STATUS.equals(member.getStatus())) {
            throw new BusinessException(ErrorCode.AUTH_LOGIN_FAILED);
        }
        return member;
    }

    /**
     * <b>불변식:</b> 여기 들어오는 {@code member}는 detached다. 이 service는 의도적으로
     * {@code @Transactional}이 아니고 {@code spring.jpa.open-in-view=false}이므로 repository 호출마다
     * persistence context가 닫힌다. 따라서 {@link LoginResponse}는 {@code Member}의 LAZY 연관
     * ({@code grade}, {@code currentSanction})에 접근해서는 안 된다. 접근하면 운영에서
     * {@code LazyInitializationException}으로 500이 된다.
     *
     * <p>이 불변식은 {@code GoogleAuthPersistenceIntegrationTest}가 실제 DB로 검증한다.
     */
    private LoginResponse issueLogin(Member member) {
        String accessToken = jwtTokenProvider.generateToken(member.getMemberId(), member.getRole());
        return LoginResponse.of(accessToken, jwtTokenProvider.getExpiration(), member);
    }

    private GoogleIdTokenVerifier verifier() {
        requireEnabled();
        GoogleIdTokenVerifier verifier = googleIdTokenVerifier.getIfAvailable();
        if (verifier == null) {
            throw new BusinessException(ErrorCode.AUTH_GOOGLE_DISABLED);
        }
        return verifier;
    }

    private void requireEnabled() {
        if (!googleAuthProperties.enabled()) {
            throw new BusinessException(ErrorCode.AUTH_GOOGLE_DISABLED);
        }
    }
}
