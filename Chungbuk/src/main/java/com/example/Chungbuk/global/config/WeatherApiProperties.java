package com.example.Chungbuk.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "weather.kma")
public class WeatherApiProperties {

    private String serviceKey;

    private String baseUrl =
            "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0";

    public String getServiceKey() {
        return serviceKey;
    }

    public void setServiceKey(String serviceKey) {
        this.serviceKey = serviceKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void validateServiceKey() {
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalStateException(
                    "기상청 API 인증키가 없습니다. "
                            + "application-secret.properties의 "
                            + "weather.kma.service-key 값을 확인하세요."
            );
        }
    }
}