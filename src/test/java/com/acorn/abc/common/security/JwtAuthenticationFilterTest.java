package com.acorn.abc.common.security;

import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final Long MEMBER_ID = 1L;
    private static final String TOKEN = "valid-token";

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private MemberRepository memberRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_JOINED_회원이면_인증정보를_설정한다() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, memberRepository);
        given(jwtTokenProvider.validateToken(TOKEN)).willReturn(true);
        given(jwtTokenProvider.getMemberId(TOKEN)).willReturn(MEMBER_ID);
        given(jwtTokenProvider.getRole(TOKEN)).willReturn("USER");
        Member member = joinedMember("JOINED");
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        filter.doFilterInternal(requestWithToken(), new MockHttpServletResponse(), noopChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getMemberId())
                .isEqualTo(MEMBER_ID);
    }

    @Test
    void doFilterInternal_WITHDRAWN_회원이면_인증정보를_설정하지_않는다() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, memberRepository);
        given(jwtTokenProvider.validateToken(TOKEN)).willReturn(true);
        given(jwtTokenProvider.getMemberId(TOKEN)).willReturn(MEMBER_ID);
        Member member = joinedMember("WITHDRAWN");
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));

        filter.doFilterInternal(requestWithToken(), new MockHttpServletResponse(), noopChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_회원이_존재하지_않으면_인증정보를_설정하지_않는다() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, memberRepository);
        given(jwtTokenProvider.validateToken(TOKEN)).willReturn(true);
        given(jwtTokenProvider.getMemberId(TOKEN)).willReturn(MEMBER_ID);
        given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

        filter.doFilterInternal(requestWithToken(), new MockHttpServletResponse(), noopChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_토큰이_없으면_회원_조회_없이_다음_필터로_넘어간다() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, memberRepository);

        filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), noopChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        org.mockito.Mockito.verifyNoInteractions(memberRepository);
    }

    private Member joinedMember(String status) {
        Member member = mock(Member.class);
        given(member.getStatus()).willReturn(status);
        return member;
    }

    private MockHttpServletRequest requestWithToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + TOKEN);
        return request;
    }

    private FilterChain noopChain() {
        return (ServletRequest req, ServletResponse res) -> { };
    }
}
