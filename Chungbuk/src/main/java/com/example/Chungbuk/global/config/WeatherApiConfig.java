package com.example.Chungbuk.global.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@PropertySource(
        value = "classpath:application-secret.properties",
        encoding = "UTF-8"
)
@EnableConfigurationProperties({
        WeatherApiProperties.class,
        OpenMeteoApiProperties.class
})
public class WeatherApiConfig {

    @Bean(name = "kmaRestTemplate")
    public RestTemplate kmaRestTemplate() {
        return createRestTemplate();
    }

    @Bean(name = "openMeteoRestTemplate")
    public RestTemplate openMeteoRestTemplate() {
        return createRestTemplate();
    }

    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(5_000);
        requestFactory.setReadTimeout(5_000);

        return new RestTemplate(requestFactory);
    }
}
