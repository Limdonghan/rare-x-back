package com.project.rare_x_back.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class JusoConfig {

    @Bean
    public WebClient jusoWebClient() {
        return WebClient.builder()
                .baseUrl("https://business.juso.go.kr")
                .build();
    }
}
