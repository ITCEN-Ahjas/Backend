package com.example.Chungbuk.domain.weather.service;

import com.example.Chungbuk.domain.weather.constant.ChungbukRegion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WeatherRegionServiceTest {

    private final WeatherRegionService weatherRegionService =
            new WeatherRegionService();

    @Test
    @DisplayName("청주 지역명을 기상청 격자 좌표 정보로 변환한다")
    void getRegion_returnsCheongjuRegion() {
        ChungbukRegion region = weatherRegionService.getRegion("청주");

        assertEquals("청주", region.getDisplayName());
        assertEquals(69, region.getNx());
        assertEquals(107, region.getNy());
    }

    @Test
    @DisplayName("앞뒤 공백이 있는 지역명도 정상 변환한다")
    void getRegion_trimsRegionName() {
        ChungbukRegion region = weatherRegionService.getRegion("  단양  ");

        assertEquals("단양", region.getDisplayName());
        assertEquals(84, region.getNx());
        assertEquals(115, region.getNy());
    }

    @Test
    @DisplayName("지원하지 않는 지역명은 예외 처리한다")
    void getRegion_throwsExceptionForUnsupportedRegion() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> weatherRegionService.getRegion("서울")
        );

        assertEquals(
                "지원하지 않는 충북 지역입니다: 서울",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("충북 지역 목록은 11개를 반환한다")
    void getAllRegionNames_returnsElevenRegions() {
        List<String> regionNames =
                weatherRegionService.getAllRegionNames();

        assertEquals(11, regionNames.size());
        assertEquals(
                List.of(
                        "청주", "충주", "제천", "보은", "옥천",
                        "영동", "증평", "진천", "괴산", "음성", "단양"
                ),
                regionNames
        );
    }
}