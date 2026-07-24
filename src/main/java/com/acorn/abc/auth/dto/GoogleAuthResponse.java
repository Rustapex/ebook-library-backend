package com.acorn.abc.auth.dto;

/**
 * Google 인증 endpoint의 성공 응답.
 *
 * <p>SIGNUP_REQUIRED/LINK_REQUIRED를 로그인 완료로 오인하지 않도록 access token과 transition token을
 * 서로 다른 필드에 담고, 두 필드가 동시에 채워지지 않게 한다.
 *
 * <ul>
 *   <li>{@code AUTHENTICATED}: {@code login}만 non-null</li>
 *   <li>{@code SIGNUP_REQUIRED}/{@code LINK_REQUIRED}: {@code transition}만 non-null</li>
 * </ul>
 */
public record GoogleAuthResponse(
        GoogleAuthStatus status,
        LoginResponse login,
        Transition transition
) {

    /**
     * 다음 절차에 필요한 단기 token과 화면 표시용 Google 프로필.
     *
     * <p>{@code email}/{@code name}은 표시 전용이다. 서버는 후속 요청에서 이 값을 신뢰하지 않고
     * transition token의 claim만 사용한다.
     */
    public record Transition(
            String transitionToken,
            long expiresIn,
            String email,
            String name
    ) {}

    public static GoogleAuthResponse authenticated(LoginResponse login) {
        return new GoogleAuthResponse(GoogleAuthStatus.AUTHENTICATED, login, null);
    }

    public static GoogleAuthResponse signupRequired(String transitionToken, long expiresIn,
                                                    String email, String name) {
        return new GoogleAuthResponse(GoogleAuthStatus.SIGNUP_REQUIRED, null,
                new Transition(transitionToken, expiresIn, email, name));
    }

    public static GoogleAuthResponse linkRequired(String transitionToken, long expiresIn,
                                                  String email, String name) {
        return new GoogleAuthResponse(GoogleAuthStatus.LINK_REQUIRED, null,
                new Transition(transitionToken, expiresIn, email, name));
    }
}
