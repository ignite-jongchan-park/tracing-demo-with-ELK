package com.demo.common;

import io.micrometer.observation.ObservationRegistry;
import io.netty.resolver.DefaultAddressResolverGroup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * 공용 WebClient 빈.
 *
 * 1) ObservationRegistry(Boot 관리, 추적 핸들러 포함)를 주입해 아웃바운드 호출마다
 *    client observation 이 생성되고 Micrometer Tracing 이 traceparent 헤더를 자동 주입한다.
 * 2) Reactor Netty 의 기본(native) DNS 리졸버는 Docker 서비스명(embedded DNS) 해석에
 *    실패(NXDOMAIN)하는 경우가 있어, JDK 리졸버(DefaultAddressResolverGroup)로 교체한다.
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(ObservationRegistry observationRegistry) {
        HttpClient httpClient = HttpClient.create()
                .resolver(DefaultAddressResolverGroup.INSTANCE);
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .observationRegistry(observationRegistry)
                .filter(HttpLoggingFilter.logExchange())
                .build();
    }
}
