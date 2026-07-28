package com.demo.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * WebClient 아웃바운드 호출에 대한 method/url/status/elapsed 로깅 필터.
 * 여기서 나가는 요청에는 Micrometer Tracing 이 자동으로 traceparent 헤더를 심어
 * 다음 서비스로 Trace ID 가 전파된다.
 */
public final class HttpLoggingFilter {

    private static final Logger log = LoggerFactory.getLogger("http.client");

    private HttpLoggingFilter() {
    }

    public static ExchangeFilterFunction logExchange() {
        return (request, next) -> {
            long startNanos = System.nanoTime();
            return next.exchange(request)
                    .doOnNext(response -> {
                        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
                        log.info("outbound request",
                                kv("direction", "outbound"),
                                kv("method", request.method().name()),
                                kv("url", request.url().toString()),
                                kv("status", response.statusCode().value()),
                                kv("elapsedMs", elapsedMs));
                    });
        };
    }
}
