package com.acorn.abc.auth.service;

import com.acorn.abc.auth.domain.OauthAccount;
import com.acorn.abc.auth.domain.OauthProvider;
import com.acorn.abc.auth.dto.GoogleAuthResponse;
import com.acorn.abc.auth.dto.GoogleAuthStatus;
import com.acorn.abc.auth.dto.GoogleLoginRequest;
import com.acorn.abc.auth.dto.GoogleSignupRequest;
import com.acorn.abc.auth.repository.OauthAccountRepository;
import com.acorn.abc.common.exception.BusinessException;
import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * GoogleAuthServiceTest는 registrar와 repository를 모두 mock하므로 이 기능의 핵심 설계 주장
 * ("제약 위반으로 rollback된 뒤 새 transaction에서 재조회해 충돌을 해소한다")을 검증하지 못한다.
 * DataIntegrityViolationException을 willThrow로 주입하기 때문에 catch 분기만 통과할 뿐,
 * 실제로 예외가 나는지 / 고아 row가 남지 않는지 / 재조회가 승자를 보는지는 확인되지 않는다.
 *
 * <p>이 클래스는 Spring이 관리하는 실제 bean과 실제 DB를 그대로 사용해 그 경계를 재현한다.
 * class 수준 @Transactional을 쓰지 않는다. 붙이면 검증 대상인 commit/rollback 경계가 사라진다.
 *
 * <p>Google ID token 검증만 mock으로 대체해 외부 network에 의존하지 않는다.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "app.auth.google.enabled=true",
        "app.auth.google.client-id=test-client-id.apps.googleusercontent.com"
})
class GoogleAuthPersistenceIntegrationTest {

    private static final String ID_TOKEN = "verified-google-id-token";

    @Autowired private GoogleAuthService googleAuthService;
    @Autowired private GoogleAccountRegistrar googleAccountRegistrar;
    @Autowired private OauthAccountRepository oauthAccountRepository;
    @Autowired private MemberRepository memberRepository;

    // enabled=true라 GoogleAuthConfig가 실제 verifier를 만들지만, 이 mock이 대체하므로 JWKS 호출은 없다.
    @MockitoBean private GoogleIdTokenVerifier googleIdTokenVerifier;

    private String subject;
    private String email;
    private String loginId;
    private final List<String> createdLoginIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        subject = "sub" + unique;
        email = "g" + unique + "@example.com";
        loginId = "g" + unique;
    }

    @AfterEach
    void cleanUp() {
        oauthAccountRepository.findByProviderAndProviderUserId(OauthProvider.GOOGLE, subject)
                .ifPresent(oauthAccountRepository::delete);
        for (String createdLoginId : createdLoginIds) {
            memberRepository.findByLoginId(createdLoginId).ifPresent(memberRepository::delete);
        }
        memberRepository.findByLoginId(loginId).ifPresent(memberRepository::delete);
        createdLoginIds.clear();
    }

    private TransitionTokenClaims signupClaims() {
        return signupClaims(email);
    }

    private TransitionTokenClaims signupClaims(String claimEmail) {
        return new TransitionTokenClaims(TransitionTokenPurpose.SIGNUP, OauthProvider.GOOGLE,
                subject, claimEmail, "구글사용자", null);
    }

    private GoogleSignupRequest signupRequest(String transitionToken, String requestedLoginId) {
        return new GoogleSignupRequest(transitionToken, requestedLoginId, "구글사용자",
                "Passw0rd!!", "Passw0rd!!", "010-0714-0001", LocalDate.of(1995, 7, 14), "MALE");
    }

    @Test
    void 제약_위반이_나면_회원이_commit되지_않고_새_조회가_승자_row를_본다() {
        String loserLoginId = loginId + "b";
        createdLoginIds.add(loginId);
        createdLoginIds.add(loserLoginId);

        Member winner = googleAccountRegistrar.createMemberWithOauthAccount(
                signupClaims(), signupRequest("unused", loginId));

        // 같은 sub로 두 번째 회원 생성 시도 = uk_oauth_account_provider_provider_user_id 위반.
        // email/loginId는 다르게 둔다. 같으면 registrar의 사전 검사에 먼저 걸려 DB 제약까지 가지 않는다.
        assertThatThrownBy(() -> googleAccountRegistrar.createMemberWithOauthAccount(
                signupClaims("loser-" + email), signupRequest("unused", loserLoginId)))
                .isInstanceOf(DataIntegrityViolationException.class);

        // registrar의 transaction이 rollback됐으므로 패자 member는 남지 않는다(고아 row 없음).
        assertThat(memberRepository.existsByLoginId(loserLoginId)).isFalse();

        // 새 transaction의 재조회가 승자를 본다. GoogleAuthService의 충돌 해소가 이 성질에 의존한다.
        OauthAccount resolved = oauthAccountRepository
                .findByProviderAndProviderUserId(OauthProvider.GOOGLE, subject)
                .orElseThrow();
        assertThat(resolved.getMember().getMemberId()).isEqualTo(winner.getMemberId());
    }

    @Test
    void 연결_조회는_transaction_밖에서도_member에_접근할_수_있다() {
        createdLoginIds.add(loginId);
        googleAccountRegistrar.createMemberWithOauthAccount(signupClaims(), signupRequest("unused", loginId));

        OauthAccount found = oauthAccountRepository
                .findByProviderAndProviderUserId(OauthProvider.GOOGLE, subject)
                .orElseThrow();

        // JOIN FETCH가 없으면 여기서 LazyInitializationException이 난다(open-in-view=false).
        assertThat(found.getMember().getStatus()).isEqualTo("JOINED");
        assertThat(found.getMember().getLoginId()).isEqualTo(loginId);
        assertThat(found.getMember().getRole()).isEqualTo("USER");
    }

    @Test
    void 연결된_회원의_Google_로그인이_detached_entity로도_access_token을_발급한다() {
        createdLoginIds.add(loginId);
        Member member = googleAccountRegistrar.createMemberWithOauthAccount(
                signupClaims(), signupRequest("unused", loginId));
        given(googleIdTokenVerifier.verify(ID_TOKEN))
                .willReturn(new GoogleIdentity(subject, email, "구글사용자"));

        GoogleAuthResponse response = googleAuthService.authenticate(new GoogleLoginRequest(ID_TOKEN));

        assertThat(response.status()).isEqualTo(GoogleAuthStatus.AUTHENTICATED);
        assertThat(response.login().accessToken()).isNotBlank();
        assertThat(response.login().member().memberId()).isEqualTo(member.getMemberId());
    }

    @Test
    void onboarding이_회원과_연결을_실제로_함께_생성한다() {
        createdLoginIds.add(loginId);
        given(googleIdTokenVerifier.verify(ID_TOKEN))
                .willReturn(new GoogleIdentity(subject, email, "구글사용자"));

        GoogleAuthResponse first = googleAuthService.authenticate(new GoogleLoginRequest(ID_TOKEN));
        assertThat(first.status()).isEqualTo(GoogleAuthStatus.SIGNUP_REQUIRED);

        GoogleAuthResponse completed = googleAuthService.completeSignup(
                signupRequest(first.transition().transitionToken(), loginId));

        assertThat(completed.status()).isEqualTo(GoogleAuthStatus.AUTHENTICATED);
        assertThat(memberRepository.findByLoginId(loginId)).isPresent();
        assertThat(oauthAccountRepository.findByProviderAndProviderUserId(OauthProvider.GOOGLE, subject))
                .isPresent();
    }

    @Test
    void Google이_name_claim을_주지_않아도_가입할_수_있다() {
        createdLoginIds.add(loginId);
        // profile scope 미승인 시 name claim이 없다. member.name은 NOT NULL이므로
        // token이 아니라 onboarding 입력에서 이름을 받는다.
        given(googleIdTokenVerifier.verify(ID_TOKEN))
                .willReturn(new GoogleIdentity(subject, email, null));

        GoogleAuthResponse first = googleAuthService.authenticate(new GoogleLoginRequest(ID_TOKEN));
        GoogleAuthResponse completed = googleAuthService.completeSignup(
                signupRequest(first.transition().transitionToken(), loginId));

        assertThat(completed.status()).isEqualTo(GoogleAuthStatus.AUTHENTICATED);
        assertThat(memberRepository.findByLoginId(loginId))
                .get().extracting(Member::getName).isEqualTo("구글사용자");
    }

    @Test
    void 이미_소비된_signup_token_재사용은_409로_거부된다() {
        createdLoginIds.add(loginId);
        given(googleIdTokenVerifier.verify(ID_TOKEN))
                .willReturn(new GoogleIdentity(subject, email, "구글사용자"));

        GoogleAuthResponse first = googleAuthService.authenticate(new GoogleLoginRequest(ID_TOKEN));
        String transitionToken = first.transition().transitionToken();
        googleAuthService.completeSignup(signupRequest(transitionToken, loginId));

        // 같은 transition token을 다시 쓰면 access token이 재발급되면 안 된다.
        // (signup 경로는 비밀번호를 요구하지 않아 재사용 비용이 0이다)
        assertThatThrownBy(() -> googleAuthService.completeSignup(signupRequest(transitionToken, loginId)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_OAUTH_ALREADY_LINKED);
    }

    @Test
    void 이미_Google에_연결된_회원의_email로는_LINK_REQUIRED가_아니라_409가_난다() {
        createdLoginIds.add(loginId);
        googleAccountRegistrar.createMemberWithOauthAccount(signupClaims(), signupRequest("unused", loginId));

        // 같은 email을 가진 다른 Google 계정(sub가 다름)이 로그인 시도.
        // 이 회원은 이미 GOOGLE 연결이 있어 uk_oauth_account_member_id_provider 때문에 연결이 불가능하다.
        // 상환할 수 없는 LINK token을 발급하지 않고 즉시 거부해야 한다.
        given(googleIdTokenVerifier.verify(ID_TOKEN))
                .willReturn(new GoogleIdentity(subject + "other", email, "다른사용자"));

        assertThatThrownBy(() -> googleAuthService.authenticate(new GoogleLoginRequest(ID_TOKEN)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_OAUTH_ALREADY_LINKED);
    }
}
