package com.demo.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 주문 서비스: 재고 확인(inventory) 후 결제(payment)를 호출해 주문을 완성한다.
 * 두 번의 아웃바운드 호출 모두 동일한 Trace ID 하에서 자식 Span 으로 이어진다.
 */
@RestController
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final WebClient webClient;
    private final String inventoryUrl;
    private final String paymentUrl;

    public OrderController(WebClient webClient,
                           @Value("${services.inventory.url}") String inventoryUrl,
                           @Value("${services.payment.url}") String paymentUrl) {
        this.webClient = webClient;
        this.inventoryUrl = inventoryUrl;
        this.paymentUrl = paymentUrl;
    }

    @PostMapping(value = "/api/orders", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> body) {
        String item = String.valueOf(body.getOrDefault("item", "unknown"));
        long orderId = ThreadLocalRandom.current().nextLong(100000, 999999);
        log.info("creating order orderId={} item={}", orderId, item);

        Map<String, Object> inventory = webClient.get()
                .uri(inventoryUrl + "/api/inventory/{item}", item)
                .retrieve()
                .bodyToMono(MAP_TYPE)
                .block();

        Map<String, Object> payment = webClient.post()
                .uri(paymentUrl + "/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("orderId", orderId, "item", item))
                .retrieve()
                .bodyToMono(MAP_TYPE)
                .block();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderId", orderId);
        result.put("item", item);
        result.put("inventory", inventory);
        result.put("payment", payment);
        result.put("status", "CONFIRMED");
        log.info("order confirmed orderId={}", orderId);
        return result;
    }
}
