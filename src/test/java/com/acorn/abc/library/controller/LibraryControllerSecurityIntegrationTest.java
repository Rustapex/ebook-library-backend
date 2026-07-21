package com.acorn.abc.library.controller;

import com.acorn.abc.common.config.SecurityConfig;
import com.acorn.abc.common.security.CustomAccessDeniedHandler;
import com.acorn.abc.common.security.CustomAuthenticationEntryPoint;
import com.acorn.abc.common.security.JwtTokenProvider;
import com.acorn.abc.library.dto.LibrarySearchResponse;
import com.acorn.abc.library.service.ClientIpResolver;
import com.acorn.abc.library.service.LibraryRateLimiter;
import com.acorn.abc.library.service.PhysicalLibrarySearchService;
import jakarta.servlet.http.HttpServletRequest;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /api/v1/books/{bookId}/libraries는 SecurityConfig에서 permitAll이다(계획 2장 "보안").
 * 실제 SecurityConfig filter chain을 통과시켜 비회원 요청도 인증 없이 200을 받는지 확인한다
 * (AdminBookCrudSecurityIntegrationTest와 동일 관례, standalone MockMvc가 아닌 filter chain 적용).
 */
@WebMvcTest(LibraryController.class)
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class,
        LibraryControllerSecurityIntegrationTest.TestSecurityConfig.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:3000"
})
class LibraryControllerSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PhysicalLibrarySearchService physicalLibrarySearchService;
    @MockitoBean
    private LibraryRateLimiter libraryRateLimiter;
    @MockitoBean
    private ClientIpResolver clientIpResolver;

    @Test
    void 비회원도_인증_없이_200을_받는다() throws Exception {
        given(clientIpResolver.resolve(any(HttpServletRequest.class))).willReturn("203.0.113.1");
        given(libraryRateLimiter.tryConsume(anyString())).willReturn(true);
        given(physicalLibrarySearchService.search(1L, "11", null, 0, 20, null, null))
                .willReturn(new LibrarySearchResponse(1L, "제목", "http://cover", "11", "INFONARU", false,
                        0, 20, 0, 0, List.of(), List.of()));

        mockMvc.perform(get("/api/v1/books/1/libraries").param("region", "11"))
                .andExpect(status().isOk());
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
