package com.demo.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 결제 서비스: 결제를 처리하고 (선택) Claude 로 결제 코멘트를 생성한다.
 * Claude 호출은 별도 자식 Span(claude.messages)으로 추적된다.
 */
@RestController
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final ClaudeClient claudeClient;

    public PaymentController(ClaudeClient claudeClient) {
        this.claudeClient = claudeClient;
    }

    @PostMapping(value = "/api/payments", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> pay(@RequestBody Map<String, Object> body) {
        long orderId = asLong(body.get("orderId"));
        String item = String.valueOf(body.getOrDefault("item", "unknown"));
        long paymentId = ThreadLocalRandom.current().nextLong(100000, 999999);
        log.info("processing payment paymentId={} orderId={}", paymentId, orderId);

        Map<String, Object> summary = claudeClient.summarize(orderId, item);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paymentId", paymentId);
        result.put("orderId", orderId);
        result.put("status", "PAID");
        result.put("summary", summary);
        log.info("payment done paymentId={}", paymentId);
        return result;
    }

    private long asLong(Object o) {
        return o instanceof Number n ? n.longValue() : 0L;
    }
}
