package com.acorn.abc.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    // 이 토큰이 "인증용 access token"임을 스스로 밝히게 한다. 다른 용도의 토큰(예: Google 신규가입/계정연결용
    // transition token)은 별도 key로 서명하므로 애초에 여기서 검증되지 않지만, key 분리는 다른 클래스의
    // 구현에 의존하는 성질이다. purpose를 함께 검사하면 향후 같은 key로 다른 토큰을 만드는 실수가
    // 곧바로 인증 우회가 되지 않는다. Google sub가 숫자 문자열이라 subject가 memberId로 해석될 수 있다.
    private static final String CLAIM_PURPOSE = "purpose";
    private static final String PURPOSE_ACCESS = "ACCESS";

    private final SecretKey signingKey;
    private final long expiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generateToken(Long memberId, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(memberId.toString())
                .claim(CLAIM_PURPOSE, PURPOSE_ACCESS)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration * 1000))
                .signWith(signingKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = getClaims(token);
            // subject가 숫자가 아니면 getMemberId가 필터 안에서 NumberFormatException을 던진다.
            // 그 예외는 DispatcherServlet 앞이라 GlobalExceptionHandler가 잡지 못해 기본 오류 페이지가
            // 노출된다. 인증 실패로 여기서 확정한다.
            Long.parseLong(claims.getSubject());
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getMemberId(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }

    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public long getExpiration() {
        return expiration;
    }

    private Claims getClaims(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // purpose가 있는데 ACCESS가 아니면 인증용 토큰이 아니다. purpose가 아예 없는 토큰은
        // 이 claim 도입 이전에 발급된 것이므로 그대로 허용한다(배포 시점에 로그인 세션이 끊기지 않게 한다).
        String purpose = claims.get(CLAIM_PURPOSE, String.class);
        if (purpose != null && !PURPOSE_ACCESS.equals(purpose)) {
            throw new JwtException("not an access token");
        }
        return claims;
    }
}
