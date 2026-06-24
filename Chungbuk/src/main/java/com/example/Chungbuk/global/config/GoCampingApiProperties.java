package com.example.Chungbuk.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "go-camping")
public class GoCampingApiProperties {

    private String baseUrl;
    private String serviceKey;
}
