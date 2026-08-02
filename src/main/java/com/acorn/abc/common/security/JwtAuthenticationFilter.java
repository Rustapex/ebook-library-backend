package com.acorn.abc.common.security;

import com.acorn.abc.member.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // 회원 탈퇴/제재 토큰 차단(관리자 대여·결제·신고·회원탈퇴 API 구현계획 2026-07-22 §2.2)에서만 통과시키는 상태값.
    private static final String ACTIVE_MEMBER_STATUS = "JOINED";

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            Long memberId = jwtTokenProvider.getMemberId(token);
            String role = jwtTokenProvider.getRole(token);

            if (isActiveMember(memberId)) {
                CustomUserDetails userDetails = new CustomUserDetails(memberId, role);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    // WITHDRAWN/DEACTIVATED/SANCTIONED 회원의 기존 토큰은 다음 요청부터 인증되지 않은 요청으로 처리한다(§2.2).
    // 컨트롤러마다 상태 검사를 중복하지 않도록 이 필터 한 곳에서만 판단한다.
    private boolean isActiveMember(Long memberId) {
        return memberRepository.findById(memberId)
                .map(member -> ACTIVE_MEMBER_STATUS.equals(member.getStatus()))
                .orElse(false);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
