package com.demo.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * 서버로 들어온 요청을 처리한 뒤 method/uri/status/elapsed 를 구조화 로그로 남긴다.
 * 서블릿 스레드에서 실행되므로 Micrometer Tracing 이 채워둔 traceId/spanId(MDC)가
 * 그대로 로그에 실린다 = 분산 추적의 핵심 증거.
 */
// 관측(추적) 필터보다 안쪽에서 실행되어야 finally 로깅 시점에도 traceId/spanId(MDC)가 살아있다.
// ServerHttpObservationFilter(HIGHEST_PRECEDENCE+1)가 span scope 를 열어둔 안쪽에 위치시킨다.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("http.access");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startNanos = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
            log.info("inbound request",
                    kv("direction", "inbound"),
                    kv("method", request.getMethod()),
                    kv("uri", request.getRequestURI()),
                    kv("query", request.getQueryString()),
                    kv("status", response.getStatus()),
                    kv("elapsedMs", elapsedMs));
        }
    }
}
