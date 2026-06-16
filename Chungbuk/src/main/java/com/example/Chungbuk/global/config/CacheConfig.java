package com.example.Chungbuk.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String FESTIVAL_LIST_CACHE = "festivalList";
    public static final String EXPERIENCE_LIST_CACHE = "experienceList";
    public static final String FESTIVAL_DETAIL_CACHE = "festivalDetail";
    public static final String ACCOMMODATION_LIST_CACHE = "accommodationList";
    public static final String ACCOMMODATION_DETAIL_CACHE = "accommodationDetail";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                FESTIVAL_LIST_CACHE,
                EXPERIENCE_LIST_CACHE,
                FESTIVAL_DETAIL_CACHE,
                ACCOMMODATION_LIST_CACHE,
                ACCOMMODATION_DETAIL_CACHE
        );

        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofHours(6))
                        .maximumSize(1000)
        );

        return cacheManager;
    }
}