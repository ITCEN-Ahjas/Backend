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

    /*
     * 축제/체험 조회 API는 DB 조회 방식으로 전환되었으므로 Caffeine 캐시 대상에서 제외한다.
     *
     * 변경 전:
     * - festivalList
     * - experienceList
     * - festivalDetail
     *
     * 변경 후:
     * - 조회 API는 festival_contents 테이블을 직접 조회한다.
     * - TourAPI 호출은 동기화 API에서만 수행한다.
     * - 축제/체험 조회 캐시가 남아 있으면 DB 동기화 이후에도 이전 응답이 반환될 수 있으므로 제거한다.
     */
    public static final String ACCOMMODATION_LIST_CACHE = "accommodationList";
    public static final String ACCOMMODATION_DETAIL_CACHE = "accommodationDetail";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
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