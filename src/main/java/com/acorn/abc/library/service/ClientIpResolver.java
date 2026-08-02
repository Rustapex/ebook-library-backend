package com.acorn.abc.library.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// rate limit 우회 방지: 신뢰된 proxy를 통해 들어온 요청에서만 X-Forwarded-For를 인정한다(계획 5.5).
// library.rate-limit.trusted-proxies가 비어 있으면(기본값) 항상 실제 remote address를 사용한다.
// X-Forwarded-For는 각 proxy가 자신이 받은 client 주소를 오른쪽에 덧붙이는 방식으로 자라므로
// 실제 client IP를 구하려면 체인의 오른쪽(가장 가까운 hop)부터 신뢰된 proxy를 제거해나가야 한다.
@Component
public class ClientIpResolver {

    private static final int MAX_IP_LENGTH = 45;
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$");
    // "[0-9a-fA-F:]+" 같은 느슨한 패턴은 "a", "::::", "deadbeef"도 IPv6로 인정해 신뢰 proxy 환경에서
    // XFF 값을 조작해 rate limit을 우회할 수 있다. zone id(%eth0)/IPv4-embedded 형태는 지원하지 않되
    // 표준 8-group과 "::" 압축 표기는 정확히 검증하는 패턴을 사용한다.
    private static final Pattern IPV6_PATTERN = Pattern.compile(
            "^("
                    + "([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}"
                    + "|([0-9a-fA-F]{1,4}:){1,7}:"
                    + "|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}"
                    + "|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}"
                    + "|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}"
                    + "|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}"
                    + "|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}"
                    + "|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})"
                    + "|:((:[0-9a-fA-F]{1,4}){1,7}|:)"
                    + ")$");

    private final Set<String> trustedProxies;

    public ClientIpResolver(@Value("${library.rate-limit.trusted-proxies:}") String trustedProxiesRaw) {
        this.trustedProxies = trustedProxiesRaw == null || trustedProxiesRaw.isBlank()
                ? Set.of()
                : Arrays.stream(trustedProxiesRaw.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toSet());
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!trustedProxies.contains(remoteAddr)) {
            return remoteAddr;
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return remoteAddr;
        }

        String[] chain = forwardedFor.split(",");
        for (int i = chain.length - 1; i >= 0; i--) {
            String candidate = chain[i].trim();
            if (!isValidIp(candidate)) {
                // 형식이 깨진 hop이 있으면 체인을 신뢰하지 않고 실제 connection 주소로 fallback한다.
                return remoteAddr;
            }
            if (!trustedProxies.contains(candidate)) {
                return candidate;
            }
        }
        // 체인 전체가 신뢰된 proxy로만 구성된 경우(흔치 않음) 가장 왼쪽(최초 홉)을 사용한다.
        return chain[0].trim();
    }

    private boolean isValidIp(String candidate) {
        if (candidate.isEmpty() || candidate.length() > MAX_IP_LENGTH) {
            return false;
        }
        return IPV4_PATTERN.matcher(candidate).matches() || IPV6_PATTERN.matcher(candidate).matches();
    }
}
