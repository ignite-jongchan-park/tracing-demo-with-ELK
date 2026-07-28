package com.demo.payment;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * (선택) Claude API 호출을 하나의 자식 Span 으로 감싼다.
 * OpenTelemetry GenAI semantic conventions(gen_ai.*)를 Span 태그와 로그로 남겨
 * "LLM 호출도 분산 추적으로 관측 가능"함을 보여준다.
 *
 * Tracer 는 선택적으로 주입한다(ObjectProvider). 추적 모듈이 없어 Tracer 빈이 없을 때에도
 * 애플리케이션이 정상 기동하도록 하기 위함 — 이 경우 span 없이 mock/실호출만 수행한다.
 *
 * CLAUDE_API_KEY 가 비어 있으면 실제 호출 없이 mock 응답을 반환한다(키 없이도 데모 동작).
 */
@Component
public class ClaudeClient {

    private static final Logger log = LoggerFactory.getLogger("gen_ai");
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};
    private static final String CLAUDE_URL = "https://api.anthropic.com/v1/messages";

    private final WebClient webClient;
    private final Tracer tracer;   // 없을 수 있음(null) — 추적 모듈 미탑재 시
    private final String apiKey;
    private final String model;

    public ClaudeClient(WebClient webClient,
                        ObjectProvider<Tracer> tracerProvider,
                        @Value("${claude.api-key:}") String apiKey,
                        @Value("${claude.model:claude-haiku-4-5-20251001}") String model) {
        this.webClient = webClient;
        this.tracer = tracerProvider.getIfAvailable();
        this.apiKey = apiKey;
        this.model = model;
    }

    /** 결제 완료에 대한 한 줄 코멘트를 생성한다. 실패/무키 시 mock 으로 폴백. */
    public Map<String, Object> summarize(long orderId, String item) {
        Span span = (tracer != null) ? tracer.nextSpan().name("claude.messages").start() : null;
        Tracer.SpanInScope scope = (tracer != null && span != null) ? tracer.withSpan(span) : null;
        long startNanos = System.nanoTime();
        try {
            if (apiKey == null || apiKey.isBlank()) {
                return mock(span, startNanos, "no api key");
            }
            String prompt = "결제 완료 주문을 한 문장(20자 내외)으로 요약해줘. 주문번호 "
                    + orderId + ", 상품 " + item + ".";
            Map<String, Object> request = Map.of(
                    "model", model,
                    "max_tokens", 64,
                    "messages", List.of(Map.of("role", "user", "content", prompt)));

            Map<String, Object> response = webClient.post()
                    .uri(CLAUDE_URL)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(MAP_TYPE)
                    .block();

            String text = extractText(response);
            Map<String, Object> usage = asMap(response == null ? null : response.get("usage"));
            long inTok = asLong(usage.get("input_tokens"));
            long outTok = asLong(usage.get("output_tokens"));
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

            if (span != null) {
                span.tag("gen_ai.system", "anthropic");
                span.tag("gen_ai.request.model", model);
                span.tag("gen_ai.usage.input_tokens", String.valueOf(inTok));
                span.tag("gen_ai.usage.output_tokens", String.valueOf(outTok));
            }
            log.info("claude call ok",
                    kv("gen_ai.system", "anthropic"),
                    kv("gen_ai.request.model", model),
                    kv("gen_ai.usage.input_tokens", inTok),
                    kv("gen_ai.usage.output_tokens", outTok),
                    kv("elapsedMs", elapsedMs));
            return Map.of("note", text, "model", model, "mocked", false);
        } catch (Exception e) {
            log.warn("claude call failed, falling back to mock: {}", e.getMessage());
            return mock(span, startNanos, "error:" + e.getClass().getSimpleName());
        } finally {
            if (scope != null) {
                scope.close();
            }
            if (span != null) {
                span.end();
            }
        }
    }

    private Map<String, Object> mock(Span span, long startNanos, String reason) {
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        if (span != null) {
            span.tag("gen_ai.system", "mock");
            span.tag("gen_ai.request.model", "mock");
            span.tag("mock.reason", reason);
        }
        log.info("claude call mocked",
                kv("gen_ai.system", "mock"),
                kv("mock.reason", reason),
                kv("elapsedMs", elapsedMs));
        return Map.of("note", "결제가 정상 처리되었습니다.", "model", "mock", "mocked", true);
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> response) {
        if (response == null) {
            return "";
        }
        Object content = response.get("content");
        if (content instanceof List<?> list && !list.isEmpty()
                && list.get(0) instanceof Map<?, ?> first) {
            Object text = first.get("text");
            return text == null ? "" : text.toString();
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private long asLong(Object o) {
        return o instanceof Number n ? n.longValue() : 0L;
    }
}
