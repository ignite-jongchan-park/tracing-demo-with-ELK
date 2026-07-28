package com.demo.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// scanBasePackages 로 com.demo.common 의 공용 빈(WebClient, 필터)까지 스캔.
@SpringBootApplication(scanBasePackages = "com.demo")
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
