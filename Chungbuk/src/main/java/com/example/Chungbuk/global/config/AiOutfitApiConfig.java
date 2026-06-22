package com.example.Chungbuk.global.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(AiOutfitApiProperties.class)
public class AiOutfitApiConfig {

    @Bean(name = "aiOutfitRestTemplate")
    public RestTemplate aiOutfitRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(5_000);
        requestFactory.setReadTimeout(20_000);

        return new RestTemplate(requestFactory);
    }
}