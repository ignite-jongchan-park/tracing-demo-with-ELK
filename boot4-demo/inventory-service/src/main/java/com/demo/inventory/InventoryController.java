package com.demo.inventory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 재고 서비스: 체인의 말단(leaf) 중 하나. 외부 호출 없이 재고 수량만 반환한다.
 */
@RestController
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

    @GetMapping(value = "/api/inventory/{item}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getInventory(@PathVariable String item) {
        int stock = ThreadLocalRandom.current().nextInt(0, 100);
        boolean available = stock > 0;
        log.info("inventory checked item={} stock={} available={}", item, stock, available);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("item", item);
        result.put("stock", stock);
        result.put("available", available);
        return result;
    }
}
