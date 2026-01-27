package com.project.rare_x_back.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.typesense.api.Client;
import org.typesense.resources.Node;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class TypesenseConfig {

    @Value("${typesense.host}")
    private String host;

    @Value("${typesense.port}")
    private String port;

    @Value("${typesense.protocol}")
    private String protocol;

    @Value("${typesense.api-key}")
    private String apiKey;

    @Bean
    public Client typesenseClient() {
        List<Node> nodes = new ArrayList<>();
        nodes.add(new Node(protocol, host, port));

        // 전체 경로 사용 (Spring Configuration과 충돌 방지)
        org.typesense.api.Configuration configuration = new org.typesense.api.Configuration(nodes, Duration.ofSeconds(5), apiKey);
        return new Client(configuration);
    }
}