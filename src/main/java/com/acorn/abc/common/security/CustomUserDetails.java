package com.acorn.abc.common.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * JWT 인증 후 SecurityContext에 저장되는 회원 정보.
 *
 * [사용법] 컨트롤러 파라미터에 @AuthenticationPrincipal을 선언하면 자동 주입된다.
 *
 * 예시:
 *
 *   @GetMapping("/me/rentals")
 *   public ResponseEntity<?> getMyRentals(
 *           @AuthenticationPrincipal CustomUserDetails userDetails) {
 *
 *       Long memberId = userDetails.getMemberId(); // 회원 ID
 *       String role   = userDetails.getRole();     // "USER" or "ADMIN"
 *       ...
 *   }
 *
 * [주의]
 * - 토큰이 없거나 만료된 요청은 SecurityConfig에서 자동으로 401 반환한다.
 * - 공개 API에 @AuthenticationPrincipal을 쓰면 비로그인 시 null이 들어온다.
 *   비로그인 허용이 필요한 경우: @AuthenticationPrincipal(expression = "#this == 'anonymousUser' ? null : #this")
 */
@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final Long memberId;
    private final String role;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return memberId.toString();
    }
}
