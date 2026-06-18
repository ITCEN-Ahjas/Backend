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

    /*
     * 공공데이터포털 개발계정 일일 호출 한도 1,000회보다 여유를 둔
     * 축제·체험 동기화 전체 일일 호출 상한입니다.
     *
     * application.properties에 별도 설정이 없으면 900회를 사용합니다.
     */
    private int dailyCallLimit = 900;
}