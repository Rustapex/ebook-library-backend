package com.acorn.abc.common.security;

import com.acorn.abc.common.exception.ErrorCode;
import com.acorn.abc.common.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증 실패(토큰 없음/유효하지 않음/만료) 요청에 공통 에러 응답(401)을 내려준다.
 * SecurityFilterChain 단계에서 발생해 GlobalExceptionHandler가 처리할 수 없으므로 별도로 필요하다.
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        response.setStatus(ErrorCode.AUTH_TOKEN_EXPIRED.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(ErrorResponse.of(ErrorCode.AUTH_TOKEN_EXPIRED)));
    }
}
