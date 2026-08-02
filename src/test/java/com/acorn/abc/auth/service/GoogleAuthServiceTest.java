package com.acorn.abc.auth.service;

import com.acorn.abc.auth.config.GoogleAuthProperties;
import com.acorn.abc.auth.domain.OauthAccount;
import com.acorn.abc.auth.domain.OauthProvider;
import com.acorn.abc.auth.dto.GoogleAuthResponse;
import com.acorn.abc.auth.dto.GoogleAuthStatus;
import com.acorn.abc.auth.dto.GoogleLinkRequest;
import com.acorn.abc.auth.dto.GoogleLoginRequest;
import com.acorn.abc.auth.dto.GoogleSignupRequest;
import com.acorn.abc.auth.repository.OauthAccountRepository;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.security.JwtTokenProvider;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Google 로그인 구현 필수 test 1~8 — 인증 분기, onboarding, 계정 연결, 중복/동시성.
 */
class GoogleAuthServiceTest {

    private static final String ID_TOKEN = "google-id-token";
    private static final String SUB = "109876543210987654321";
    private static final String EMAIL = "google.user@example.com";
    private static final String JWT_SECRET = "test-jwt-secret-value-must-be-long-enough-for-hs256";
    private static final GoogleIdentity IDENTITY = new GoogleIdentity(SUB, EMAIL, "구글사용자");

    private GoogleIdTokenVerifier verifier;
    private TransitionTokenProvider transitionTokenProvider;
    private GoogleAccountRegistrar registrar;
    private OauthAccountRepository oauthAccountRepository;
    private MemberRepository memberRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private GoogleAuthService googleAuthService;

    @BeforeEach
    void setUp() {
        verifier = mock(GoogleIdTokenVerifier.class);
        transitionTokenProvider = new TransitionTokenProvider(JWT_SECRET,
                new GoogleAuthProperties(true, "client-id", 300));
        registrar = mock(GoogleAccountRegistrar.class);
        oauthAccountRepository = mock(OauthAccountRepository.class);
        memberRepository = mock(MemberRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);

        @SuppressWarnings("unchecked")
        ObjectProvider<GoogleIdTokenVerifier> verifierProvider = mock(ObjectProvider.class);
        given(verifierProvider.getIfAvailable()).willReturn(verifier);

        googleAuthService = new GoogleAuthService(
                new GoogleAuthProperties(true, "client-id", 300),
                verifierProvider,
                transitionTokenProvider,
                registrar,
                oauthAccountRepository,
                memberRepository,
                passwordEncoder,
                jwtTokenProvider);
    }

    private static Member memberWithId(Long memberId, String email, String status) {
        Member member = Member.create("existing1", "hashed", "기존회원", email,
                null, LocalDate.of(1995, 7, 14), "MALE", null);
        ReflectionTestUtils.setField(member, "memberId", memberId);
        ReflectionTestUtils.setField(member, "status", status);
        return member;
    }

    private static OauthAccount linkedAccount(Member member, String providerEmail) {
        OauthAccount account = OauthAccount.link(member, OauthProvider.GOOGLE, SUB, providerEmail);
        ReflectionTestUtils.setField(account, "oauthAccountId", 7L);
        return account;
    }

    private void givenVerifiedGoogleIdentity() {
        given(verifier.verify(ID_TOKEN)).willReturn(IDENTITY);
    }

    private void givenAccessTokenIssued() {
        given(jwtTokenProvider.generateToken(any(), anyString())).willReturn("issued-access-token");
        given(jwtTokenProvider.getExpiration()).willReturn(86400L);
    }

    // ---------- 1. 연결된 Google 회원 정상 로그인 ----------

    @Test
    void 연결된_Google_회원은_AUTHENTICATED와_access_token을_받는다() {
        givenVerifiedGoogleIdentity();
        givenAccessTokenIssued();
        Member member = memberWithId(1L, EMAIL, "JOINED");
        given(oauthAccountRepository.findByProviderAndProviderUserId(OauthProvider.GOOGLE, SUB))
                .willReturn(Optional.of(linkedAccount(member, EMAIL)));

        GoogleAuthResponse response = googleAuthService.authenticate(new GoogleLoginRequest(ID_TOKEN));

        assertThat(response.status()).isEqualTo(GoogleAuthStatus.AUTHENTICATED);
        assertThat(response.transition()).isNull();
        assertThat(response.login().accessToken()).isEqualTo("issued-access-token");
        assertThat(response.login().tokenType()).isEqualTo("Bearer");
        assertThat(response.login().expiresIn()).isEqualTo(86400L);
        assertThat(response.login().member().memberId()).isEqualTo(1L);
    }

    @Test
    void 연결된_회원이_JOINED가_아니면_로그인을_거부한다() {
        givenVerifiedGoogleIdentity();
        Member member = memberWithId(1L, EMAIL, "WITHDRAWN");
        given(oauthAccountRepository.findByProviderAndProviderUserId(OauthProvider.GOOGLE, SUB))
                .willReturn(Optional.of(linkedAccount(member, EMAIL)));

        assertThatThrownBy(() -> googleAuthService.authenticate(new GoogleLoginRequest(ID_TOKEN)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_LOGIN_FAILED);
    }

    @Test
    void 로그인_시_provider_email이_바뀌었으면_snapshot만_갱신한다() {
        givenVerifiedGoogleIdentity();
        givenAccessTokenIssued();
        Member member = memberWithId(1L, EMAIL, "JOINED");
        given(oauthAccountRepository.findByProviderAndProviderUserId(OauthProvider.GOOGLE, SUB))
                .willReturn(Optional.of(linkedAccount(member, "old.address@example.com")));

        googleAuthService.authenticate(new GoogleLoginRequest(ID_TOKEN));

        verify(registrar).refreshProviderEmail(7L, EMAIL);
    }

    // ---------- 2. 신규 이메일은 SIGNUP_REQUIRED ----------

    @Test
    void 처음_보는_Google_계정은_SIGNUP_REQUIRED를_반환하고_access_token을_주지_않는다() {
        givenVerifiedGoogleIdentity();
        given(oauthAccountRepository.findByProviderAndProviderUserId(OauthProvider.GOOGLE, SUB))
                .willReturn(Optional.empty());
        given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.empty());

        GoogleAuthResponse response = googleAuthService.authenticate(new GoogleLoginRequest(ID_TOKEN));

        assertThat(response.status()).isEqualTo(GoogleAuthStatus.SIGNUP_REQUIRED);
        assertThat(response.login()).isNull();
        assertThat(response.transition().transitionToken()).isNotBlank();
        assertThat(response.transition().email()).isEqualTo(EMAIL);

        // 발급된 token은 SIGNUP 목적으로만 쓸 수 있어야 한다.
        TransitionTokenClaims claims = transitionTokenProvider.verify(
                response.transition().transitionToken(), TransitionTokenPurpose.SIGNUP);
        assertThat(claims.providerUserId()).isEqualTo(SUB);
    }

    // ---------- 4. 동일 이메일 일반 회원은 LINK_REQUIRED ----------

    @Test
    void 같은_이메일의_기존_회원이_있으면_자동_연결하지_않고_LINK_REQUIRED를_반환한다() {
        givenVerifiedGoogleIdentity();
        given(oauthAccountRepository.findByProviderAndProviderUserId(OauthProvider.GOOGLE, SUB))
                .willReturn(Optional.empty());
        given(memberRepository.findByEmail(EMAIL))
                .willReturn(Optional.of(memberWithId(5L, EMAIL, "JOINED")));
        given(oauthAccountRepository.existsByMemberMemberIdAndProvider(5L, OauthProvider.GOOGLE))
                .willReturn(false);

        GoogleAuthResponse response = googleAuthService.authenticate(new GoogleLoginRequest(ID_TOKEN));

        assertThat(response.status()).isEqualTo(GoogleAuthStatus.LINK_REQUIRED);
        assertThat(response.login()).isNull();
        // 이 시점에는 어떤 연결도 만들어지지 않아야 한다.
        verify(registrar, never()).linkOauthAccount(any(), any());

        TransitionTokenClaims claims = transitionTokenProvider.verify(
                response.transition().transitionToken(), TransitionTokenPurpose.LINK);
        assertThat(claims.memberId()).isEqualTo(5L);
    }

    @Test
    void 이미_Google에_연결된_회원의_email이면_상환_불가능한_LINK_token을_발급하지_않는다() {
        givenVerifiedGoogleIdentity();
        given(oauthAccountRepository.findByProviderAndProviderUserId(OauthProvider.GOOGLE, SUB))
                .willReturn(Optional.empty());
        given(memberRepository.findByEmail(EMAIL))
                .willReturn(Optional.of(memberWithId(5L, EMAIL, "JOINED")));
        // 이 회원은 이미 다른 Google 계정과 연결돼 있어 uk_oauth_account_member_id_provider 때문에
        // completeLink가 반드시 실패한다. LINK_REQUIRED를 주면 비밀번호를 정확히 입력해도 409가 난다.
        given(oauthAccountRepository.existsByMemberMemberIdAndProvider(5L, OauthProvider.GOOGLE))
                .willReturn(true);

        assertThatThrownBy(() -> googleAuthService.authenticate(new GoogleLoginRequest(ID_TOKEN)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_OAUTH_ALREADY_LINKED);
    }

    // ---------- 3. onboarding 완료 ----------

    private GoogleSignupRequest signupRequest(String transitionToken) {
        return new GoogleSignupRequest(transitionToken, "googleuser1", "구글사용자",
                "Passw0rd!!", "Passw0rd!!", "010-0714-0001", LocalDate.of(1995, 7, 14), "MALE");
    }

    @Test
    void onboarding을_완료하면_회원과_연결이_생성되고_access_token을_발급한다() {
        givenAccessTokenIssued();
        String signupToken = transitionTokenProvider.issueSignupToken(OauthProvider.GOOGLE, IDENTITY);
        given(oauthAccountRepository.findByProviderAndProviderUserId(OauthProvider.GOOGLE, SUB))
                .willReturn(Optional.empty());
        Member created = memberWithId(9L, EMAIL, "JOINED");
        given(registrar.createMemberWithOauthAccount(any(), any())).willReturn(created);

        GoogleAuthResponse response = googleAuthService.completeSignup(signupRequest(signupToken));

        assertThat(response.status()).isEqualTo(GoogleAuthStatus.AUTHENTICATED);
        assertThat(response.login().member().memberId()).isEqualTo(9L);
        verify(registrar).createMemberWithOauthAccount(any(), any());
    }

    @Test
    void onboarding은_요청의_email이_아니라_transition_token의_claim을_사용한다() {
        givenAccessTokenIssued();
        String signupToken = transitionTokenProvider.issueSignupToken(OauthProvider.GOOGLE, IDENTITY);
        given(oauthAccountRepository.findByProviderAndProviderUserId(OauthProvider.GOOGLE, SUB))
                .willReturn(Optional.empty());
        given(registrar.createMemberWithOauthAccount(any(), any()))
                .willReturn(memberWithId(9L, EMAIL, "JOINED"));

        googleAuthService.completeSignup(signupRequest(signupToken));

        var claimsCaptor = org.mockito.ArgumentCaptor.forClass(TransitionTokenClaims.class);
        verify(registrar).createMemberWithOauthAccount(claimsCaptor.capture(), any());
        assertThat(claimsCaptor.getValue().email()).isEqualTo(EMAIL);
        assertThat(claimsCaptor.getValue().providerUserId()).isEqualTo(SUB);
    }

    @Test
    void 만료되거나_purpose가_다른_token으로는_onboarding할_수_없다() {
        String linkToken = transitionTokenProvider.issueLinkToken(OauthProvider.GOOGLE, IDENTITY, 5L);

        assertThatThrownBy(() -> googleAuthService.completeSignup(signupRequest(linkToken)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_TRANSITION_TOKEN_INVALID);
    }

    // ---------- 7. 재로그인 시 중복 회원 미생성 ----------

    @Test
    void 소비된_signup_token을_재사용하면_회원을_만들지도_access_token을_주지도_않는다() {
        // signup 경로는 비밀번호를 요구하지 않으므로, 여기서 access token을 재발급하면 transition token이
        // 만료 전까지 반복 사용 가능한 로그인 credential이 된다. 재시도는 Google ID token 재검증 경로로만 허용한다.
        String signupToken = transitionTokenProvider.issueSignupToken(OauthProvider.GOOGLE, IDENTITY);
        Member existing = memberWithId(9L, EMAIL, "JOINED");
        given(oauthAccountRepository.findByProviderAndProviderUserId(OauthProvider.GOOGLE, SUB))
                .willReturn(Optional.of(linkedAccount(existing, EMAIL)));

        assertThatThrownBy(() -> googleAuthService.completeSignup(signupRequest(signupToken)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_OAUTH_ALREADY_LINKED);

        verify(registrar, never()).createMemberWithOauthAccount(any(), any());
    }

    @Test
    void passwordConfirm_불일치는_멱등_경로에_도달하기_전에_거부된다() {
        String signupToken = transitionTokenProvider.issueSignupToken(OauthProvider.GOOGLE, IDENTITY);
        GoogleSignupRequest mismatched = new GoogleSignupRequest(signupToken, "googleuser1", "구글사용자",
                "Passw0rd!!", "틀린값!!", "010-0714-0001", LocalDate.of(1995, 7, 14), "MALE");

        assertThatThrownBy(() -> googleAuthService.completeSignup(mismatched))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);

        verify(registrar, never()).createMemberWithOauthAccount(any(), any());
    }

    @Test
    void 원인을_확정하지_못한_제약_위반은_409로_단정하지_않고_전파한다() {
        String signupToken = transitionTokenProvider.issueSignupToken(OauthProvider.GOOGLE, IDENTITY);
        given(oauthAccountRepository.findByProviderAndProviderUserId(OauthProvider.GOOGLE, SUB))
                .willReturn(Optional.empty());
        given(registrar.createMemberWithOauthAccount(any(), any()))
                .willThrow(new DataIntegrityViolationException("Data truncation: member.phone"));
        given(memberRepository.existsByLoginId("googleuser1")).willReturn(false);
        given(memberRepository.existsByEmail(EMAIL)).willReturn(false);

        assertThatThrownBy(() -> googleAuthService.completeSignup(signupRequest(signupToken)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------- 8. 동일 Google sub 동시 요청 ----------

    @Test
    void 동시_onboarding에서_제약_위반이_나면_먼저_생성된_회원으로_로그인시킨다() {
        givenAccessTokenIssued();
        String signupToken = transitionTokenProvider.issueSignupToken(OauthProvider.GOOGLE, IDENTITY);
        Member winner = memberWithId(9L, EMAIL, "JOINED");
        // 사전 조회에서는 없다가(경쟁), 위반 후 재조회에서는 존재한다.
        given(oauthAccountRepository.findByProviderAndProviderUserId(OauthProvider.GOOGLE, SUB))
                .willReturn(Optional.empty())
                .willReturn(Optional.of(linkedAccount(winner, EMAIL)));
        given(registrar.createMemberWithOauthAccount(any(), any()))
                .willThrow(new DataIntegrityViolationException("uk_oauth_account_provider_provider_user_id"));

        GoogleAuthResponse response = googleAuthService.completeSignup(signupRequest(signupToken));

        assertThat(response.status()).isEqualTo(GoogleAuthStatus.AUTHENTICATED);
        assertThat(response.login().member().memberId()).isEqualTo(9L);
    }

    @Test
    void 동시_onboarding_위반이_sub_충돌이_아니면_원인을_다시_판정한다() {
        String signupToken = transitionTokenProvider.issueSignupToken(OauthProvider.GOOGLE, IDENTITY);
        given(oauthAccountRepository.findByProviderAndProviderUserId(OauthProvider.GOOGLE, SUB))
                .willReturn(Optional.empty());
        given(registrar.createMemberWithOauthAccount(any(), any()))
                .willThrow(new DataIntegrityViolationException("uk_member_login_id"));
        given(memberRepository.existsByLoginId("googleuser1")).willReturn(true);

        assertThatThrownBy(() -> googleAuthService.completeSignup(signupRequest(signupToken)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_LOGIN_ID_DUPLICATED);
    }

    // ---------- 5, 6. 계정 연결 ----------

    @Test
    void 잘못된_비밀번호로는_계정을_연결할_수_없다() {
        String linkToken = transitionTokenProvider.issueLinkToken(OauthProvider.GOOGLE, IDENTITY, 5L);
        Member member = memberWithId(5L, EMAIL, "JOINED");
        given(memberRepository.findById(5L)).willReturn(Optional.of(member));
        given(passwordEncoder.matches(eq("wrong-password"), anyString())).willReturn(false);

        assertThatThrownBy(() -> googleAuthService.completeLink(
                new GoogleLinkRequest(linkToken, "wrong-password")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_LOGIN_FAILED);

        verify(registrar, never()).linkOauthAccount(any(), any());
    }

    @Test
    void 비밀번호_재인증에_성공하면_연결하고_access_token을_발급한다() {
        givenAccessTokenIssued();
        String linkToken = transitionTokenProvider.issueLinkToken(OauthProvider.GOOGLE, IDENTITY, 5L);
        Member member = memberWithId(5L, EMAIL, "JOINED");
        given(memberRepository.findById(5L)).willReturn(Optional.of(member));
        given(passwordEncoder.matches(eq("Passw0rd!!"), anyString())).willReturn(true);
        given(oauthAccountRepository.findByProviderAndProviderUserId(OauthProvider.GOOGLE, SUB))
                .willReturn(Optional.empty());
        given(oauthAccountRepository.existsByMemberMemberIdAndProvider(5L, OauthProvider.GOOGLE))
                .willReturn(false);

        GoogleAuthResponse response = googleAuthService.completeLink(
                new GoogleLinkRequest(linkToken, "Passw0rd!!"));

        assertThat(response.status()).isEqualTo(GoogleAuthStatus.AUTHENTICATED);
        assertThat(response.login().member().memberId()).isEqualTo(5L);
        verify(registrar).linkOauthAccount(eq(member), any());
    }

    @Test
    void 이미_다른_회원에_연결된_sub는_연결을_거부한다() {
        String linkToken = transitionTokenProvider.issueLinkToken(OauthProvider.GOOGLE, IDENTITY, 5L);
        Member member = memberWithId(5L, EMAIL, "JOINED");
        given(memberRepository.findById(5L)).willReturn(Optional.of(member));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);
        given(oauthAccountRepository.findByProviderAndProviderUserId(OauthProvider.GOOGLE, SUB))
                .willReturn(Optional.of(linkedAccount(memberWithId(99L, "other@example.com", "JOINED"), EMAIL)));

        assertThatThrownBy(() -> googleAuthService.completeLink(
                new GoogleLinkRequest(linkToken, "Passw0rd!!")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_OAUTH_ALREADY_LINKED);
    }

    @Test
    void 같은_회원에_이미_연결된_sub는_멱등하게_로그인시킨다() {
        givenAccessTokenIssued();
        String linkToken = transitionTokenProvider.issueLinkToken(OauthProvider.GOOGLE, IDENTITY, 5L);
        Member member = memberWithId(5L, EMAIL, "JOINED");
        given(memberRepository.findById(5L)).willReturn(Optional.of(member));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);
        given(oauthAccountRepository.findByProviderAndProviderUserId(OauthProvider.GOOGLE, SUB))
                .willReturn(Optional.of(linkedAccount(member, EMAIL)));

        GoogleAuthResponse response = googleAuthService.completeLink(
                new GoogleLinkRequest(linkToken, "Passw0rd!!"));

        assertThat(response.status()).isEqualTo(GoogleAuthStatus.AUTHENTICATED);
        verify(registrar, never()).linkOauthAccount(any(), any());
    }

    @Test
    void 이미_다른_Google_계정과_연결된_회원은_추가_연결을_거부한다() {
        String linkToken = transitionTokenProvider.issueLinkToken(OauthProvider.GOOGLE, IDENTITY, 5L);
        Member member = memberWithId(5L, EMAIL, "JOINED");
        given(memberRepository.findById(5L)).willReturn(Optional.of(member));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);
        given(oauthAccountRepository.findByProviderAndProviderUserId(OauthProvider.GOOGLE, SUB))
                .willReturn(Optional.empty());
        given(oauthAccountRepository.existsByMemberMemberIdAndProvider(5L, OauthProvider.GOOGLE))
                .willReturn(true);

        assertThatThrownBy(() -> googleAuthService.completeLink(
                new GoogleLinkRequest(linkToken, "Passw0rd!!")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_OAUTH_ALREADY_LINKED);
    }

    @Test
    void 동시_link에서_제약_위반이_나면_같은_관계일_때만_성공_처리한다() {
        givenAccessTokenIssued();
        String linkToken = transitionTokenProvider.issueLinkToken(OauthProvider.GOOGLE, IDENTITY, 5L);
        Member member = memberWithId(5L, EMAIL, "JOINED");
        given(memberRepository.findById(5L)).willReturn(Optional.of(member));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);
        given(oauthAccountRepository.findByProviderAndProviderUserId(OauthProvider.GOOGLE, SUB))
                .willReturn(Optional.empty())
                .willReturn(Optional.of(linkedAccount(member, EMAIL)));
        given(oauthAccountRepository.existsByMemberMemberIdAndProvider(5L, OauthProvider.GOOGLE))
                .willReturn(false);
        org.mockito.BDDMockito.willThrow(new DataIntegrityViolationException("uk_oauth_account"))
                .given(registrar).linkOauthAccount(any(), any());

        GoogleAuthResponse response = googleAuthService.completeLink(
                new GoogleLinkRequest(linkToken, "Passw0rd!!"));

        assertThat(response.status()).isEqualTo(GoogleAuthStatus.AUTHENTICATED);
    }

    @Test
    void 동시_link_위반이_다른_회원_관계면_거부한다() {
        String linkToken = transitionTokenProvider.issueLinkToken(OauthProvider.GOOGLE, IDENTITY, 5L);
        Member member = memberWithId(5L, EMAIL, "JOINED");
        given(memberRepository.findById(5L)).willReturn(Optional.of(member));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);
        given(oauthAccountRepository.findByProviderAndProviderUserId(OauthProvider.GOOGLE, SUB))
                .willReturn(Optional.empty())
                .willReturn(Optional.of(linkedAccount(memberWithId(99L, "other@example.com", "JOINED"), EMAIL)));
        given(oauthAccountRepository.existsByMemberMemberIdAndProvider(5L, OauthProvider.GOOGLE))
                .willReturn(false);
        org.mockito.BDDMockito.willThrow(new DataIntegrityViolationException("uk_oauth_account"))
                .given(registrar).linkOauthAccount(any(), any());

        assertThatThrownBy(() -> googleAuthService.completeLink(
                new GoogleLinkRequest(linkToken, "Passw0rd!!")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_OAUTH_ALREADY_LINKED);
    }

    @Test
    void 연결_대상_회원이_JOINED가_아니면_연결을_거부한다() {
        String linkToken = transitionTokenProvider.issueLinkToken(OauthProvider.GOOGLE, IDENTITY, 5L);
        given(memberRepository.findById(5L))
                .willReturn(Optional.of(memberWithId(5L, EMAIL, "SANCTIONED")));

        assertThatThrownBy(() -> googleAuthService.completeLink(
                new GoogleLinkRequest(linkToken, "Passw0rd!!")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_LOGIN_FAILED);
    }

    // ---------- feature flag ----------

    @Test
    void feature_flag가_꺼져_있으면_503으로_거부한다() {
        @SuppressWarnings("unchecked")
        ObjectProvider<GoogleIdTokenVerifier> emptyProvider = mock(ObjectProvider.class);
        GoogleAuthService disabled = new GoogleAuthService(
                new GoogleAuthProperties(false, "", 300),
                emptyProvider, transitionTokenProvider, registrar,
                oauthAccountRepository, memberRepository, passwordEncoder, jwtTokenProvider);

        assertThatThrownBy(() -> disabled.authenticate(new GoogleLoginRequest(ID_TOKEN)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_GOOGLE_DISABLED);
    }
}
