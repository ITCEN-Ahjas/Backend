package com.example.Chungbuk.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(AiRouteRecommendationProperties.class)
public class AiRouteRecommendationConfig {

    @Bean(name = "aiRouteRestTemplate")
    public RestTemplate aiRouteRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(5_000);
        requestFactory.setReadTimeout(20_000);

        return new RestTemplate(requestFactory);
    }
}
