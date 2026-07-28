package com.demo.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * 사용자 진입점. /api/checkout/{item} 하나로 전체 MSA 체인을 촉발한다.
 * gateway -> order-service -> {inventory-service, payment-service}
 */
@RestController
public class GatewayController {

    private static final Logger log = LoggerFactory.getLogger(GatewayController.class);

    private final WebClient webClient;
    private final String orderUrl;

    public GatewayController(WebClient webClient,
                             @Value("${services.order.url}") String orderUrl) {
        this.webClient = webClient;
        this.orderUrl = orderUrl;
    }

    @GetMapping(value = "/api/checkout/{item}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> checkout(@PathVariable String item) {
        log.info("checkout received for item={}", item);
        return webClient.post()
                .uri(orderUrl + "/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("item", item))
                .retrieve()
                .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
    }
}
