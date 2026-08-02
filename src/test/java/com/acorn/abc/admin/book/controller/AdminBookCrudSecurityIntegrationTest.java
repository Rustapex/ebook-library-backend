package com.acorn.abc.admin.book.controller;

import com.acorn.abc.book.service.AdminBookService;
import com.acorn.abc.common.config.SecurityConfig;
import com.acorn.abc.common.response.PageResponse;
import com.acorn.abc.common.security.CustomAccessDeniedHandler;
import com.acorn.abc.common.security.CustomAuthenticationEntryPoint;
import com.acorn.abc.common.security.JwtTokenProvider;
import com.acorn.abc.member.domain.Member;
import com.acorn.abc.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /api/v1/admin/**는 SecurityConfig에서 hasRole("ADMIN")으로 보호된다(관리자 도서 API 구현계획 §5.1).
 * 이 경로가 그 규칙을 실제로 상속받는지 목록 GET 하나로 확인한다 — 규칙 자체가 prefix 단위라 다른
 * 엔드포인트에 반복하지 않는다(AdminCollectionSecurityIntegrationTest와 동일 관례).
 */
@WebMvcTest(AdminBookController.class)
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class,
        AdminBookCrudSecurityIntegrationTest.TestSecurityConfig.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:3000"
})
class AdminBookCrudSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AdminBookService adminBookService;

    // JwtAuthenticationFilter가 회원 상태(JOINED)를 조회하므로 슬라이스 컨텍스트에도 빈이 필요하다.
    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void getBooks_미인증이면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/books"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getBooks_USER_토큰이면_403을_반환한다() throws Exception {
        String token = jwtTokenProvider.generateToken(1L, "USER");
        Member member = joinedMember();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        mockMvc.perform(get("/api/v1/admin/books").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"));
    }

    @Test
    void getBooks_ADMIN_토큰이면_200을_반환한다() throws Exception {
        String token = jwtTokenProvider.generateToken(99L, "ADMIN");
        Member member = joinedMember();
        given(memberRepository.findById(99L)).willReturn(Optional.of(member));
        given(adminBookService.getAdminBooks(any(), any(), any(), any(), anyInt(), anyInt()))
                .willReturn(PageResponse.of(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/v1/admin/books").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private Member joinedMember() {
        Member member = mock(Member.class);
        given(member.getStatus()).willReturn("JOINED");
        return member;
    }

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        JwtTokenProvider jwtTokenProvider() {
            return new JwtTokenProvider(
                    "test-secret-key-test-secret-key-test-secret-key-1234567890",
                    3600
            );
        }
    }
}
