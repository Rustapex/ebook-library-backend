package com.acorn.abc.library.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class ClientIpResolverTest {

    private HttpServletRequest requestOf(String remoteAddr, String forwardedFor) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        given(request.getRemoteAddr()).willReturn(remoteAddr);
        given(request.getHeader("X-Forwarded-For")).willReturn(forwardedFor);
        return request;
    }

    @Test
    void 신뢰되지_않은_remoteAddr는_XFF를_무시하고_그대로_사용한다() {
        ClientIpResolver resolver = new ClientIpResolver("198.51.100.5");
        HttpServletRequest request = requestOf("203.0.113.1", "9.9.9.9");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.1");
    }

    @Test
    void 신뢰된_proxy에서_들어오면_XFF의_오른쪽부터_신뢰된_hop을_제거해_실제_client를_찾는다() {
        ClientIpResolver resolver = new ClientIpResolver("198.51.100.5,198.51.100.6");
        HttpServletRequest request = requestOf("198.51.100.5", "203.0.113.9, 198.51.100.6");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.9");
    }

    @Test
    void 신뢰된_proxy인데_XFF가_없으면_remoteAddr를_사용한다() {
        ClientIpResolver resolver = new ClientIpResolver("198.51.100.5");
        HttpServletRequest request = requestOf("198.51.100.5", null);

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.5");
    }

    @Test
    void 체인에_형식이_잘못된_hop이_있으면_remoteAddr로_fallback한다() {
        ClientIpResolver resolver = new ClientIpResolver("198.51.100.5,198.51.100.6");
        HttpServletRequest request = requestOf("198.51.100.5", "not-an-ip, 198.51.100.6");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.5");
    }

    @Test
    void 지나치게_긴_hop_값은_거부한다() {
        ClientIpResolver resolver = new ClientIpResolver("198.51.100.5");
        String tooLong = "1".repeat(50);
        HttpServletRequest request = requestOf("198.51.100.5", tooLong);

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.5");
    }

    @Test
    void IPv6_형식도_허용한다() {
        ClientIpResolver resolver = new ClientIpResolver("198.51.100.5");
        HttpServletRequest request = requestOf("198.51.100.5", "2001:db8::1");

        assertThat(resolver.resolve(request)).isEqualTo("2001:db8::1");
    }

    @Test
    void IPv6_전체_표기와_loopback도_허용한다() {
        ClientIpResolver resolver = new ClientIpResolver("198.51.100.5");

        assertThat(new ClientIpResolver("198.51.100.5")
                .resolve(requestOf("198.51.100.5", "2001:0db8:0000:0000:0000:ff00:0042:8329")))
                .isEqualTo("2001:0db8:0000:0000:0000:ff00:0042:8329");
        assertThat(resolver.resolve(requestOf("198.51.100.5", "::1"))).isEqualTo("::1");
        assertThat(resolver.resolve(requestOf("198.51.100.5", "fe80::1"))).isEqualTo("fe80::1");
    }

    // 느슨한 "[0-9a-fA-F:]+" 패턴은 아래 문자열을 전부 IPv6로 잘못 인정했다. 신뢰 proxy 환경에서
    // 공격자가 XFF 값을 이런 임의 문자열로 바꿔가며 매 요청마다 다른 "IP"로 위장하면 IP별 rate
    // limit(분당 30회)을 사실상 무제한으로 우회할 수 있었다.
    @Test
    void 유효하지_않은_유사_IPv6_문자열은_거부하고_remoteAddr로_fallback한다() {
        ClientIpResolver resolver = new ClientIpResolver("198.51.100.5");

        assertThat(resolver.resolve(requestOf("198.51.100.5", "a"))).isEqualTo("198.51.100.5");
        assertThat(resolver.resolve(requestOf("198.51.100.5", "::::"))).isEqualTo("198.51.100.5");
        assertThat(resolver.resolve(requestOf("198.51.100.5", "deadbeef"))).isEqualTo("198.51.100.5");
        assertThat(resolver.resolve(requestOf("198.51.100.5", "gggg::1"))).isEqualTo("198.51.100.5");
    }

    @Test
    void trustedProxies가_비어있으면_항상_remoteAddr를_사용한다() {
        ClientIpResolver resolver = new ClientIpResolver("");
        HttpServletRequest request = requestOf("203.0.113.1", "9.9.9.9");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.1");
    }
}
