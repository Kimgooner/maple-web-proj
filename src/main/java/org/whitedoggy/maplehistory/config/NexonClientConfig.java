package org.whitedoggy.maplehistory.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

@Configuration
@EnableConfigurationProperties(NexonApiProperties.class)
public class NexonClientConfig {

    @Bean
    WebClient nexonWebClient(NexonApiProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(properties.requestTimeout());

        return WebClient.builder()
                .baseUrl(properties.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .defaultHeader("x-nxopen-api-key", properties.apiKey() == null ? "" : properties.apiKey())
                .build();
    }
}
