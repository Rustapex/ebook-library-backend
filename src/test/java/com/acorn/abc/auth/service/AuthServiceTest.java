package com.acorn.abc.auth.service;

import com.acorn.abc.auth.dto.CheckLoginIdResponse;
import com.acorn.abc.common.security.JwtTokenProvider;
import com.acorn.abc.member.repository.MemberGradeRepository;
import com.acorn.abc.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * 로그인아이디 중복확인 API 구현계획(2026-07-15) §4.1 — checkLoginId 검증.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberGradeRepository memberGradeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    void checkLoginId_이미_존재하면_available_false를_반환한다() {
        given(memberRepository.existsByLoginId("member0714a")).willReturn(true);

        CheckLoginIdResponse response = authService.checkLoginId("member0714a");

        assertThat(response.loginId()).isEqualTo("member0714a");
        assertThat(response.available()).isFalse();
    }

    @Test
    void checkLoginId_존재하지_않으면_available_true를_반환한다() {
        given(memberRepository.existsByLoginId("newmember1")).willReturn(false);

        CheckLoginIdResponse response = authService.checkLoginId("newmember1");

        assertThat(response.loginId()).isEqualTo("newmember1");
        assertThat(response.available()).isTrue();
    }

    @Test
    void checkLoginId_요청받은_아이디를_그대로_repository에_전달한다() {
        given(memberRepository.existsByLoginId("checkThis1")).willReturn(false);

        authService.checkLoginId("checkThis1");

        verify(memberRepository).existsByLoginId(eq("checkThis1"));
    }
}
