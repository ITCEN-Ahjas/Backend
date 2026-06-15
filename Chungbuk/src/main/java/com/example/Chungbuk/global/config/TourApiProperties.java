package com.example.Chungbuk.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tour-api")
public class TourApiProperties {

    private String baseUrl;
    private String serviceKey;
    private String mobileOs;
    private String mobileApp;
    private String responseType;
}