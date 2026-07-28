package com.demo.boot3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * /hello 를 호출하면 로그가 한 줄 찍힌다.
 * Boot 3 + (actuator + bridge-otel) 조합에서는 그 로그 앞에
 * [boot3-demo,<traceId>,<spanId>] 형태로 추적 ID 가 자동으로 붙는다.
 */
@RestController
public class HelloController {

    private static final Logger log = LoggerFactory.getLogger(HelloController.class);

    @GetMapping("/hello")
    public String hello() {
        log.info("hello 요청 처리 — 이 줄 앞에 [앱,traceId,spanId] 가 찍혀 있어야 합니다");
        return "hello from boot3";
    }
}
