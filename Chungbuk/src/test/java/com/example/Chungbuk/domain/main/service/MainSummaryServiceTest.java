package com.example.Chungbuk.domain.main.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.Chungbuk.domain.accommodation.repository.AccommodationRepository;
import com.example.Chungbuk.domain.camping.repository.CampingRepository;
import com.example.Chungbuk.domain.festival.repository.FestivalContentRepository;
import com.example.Chungbuk.domain.main.dto.response.MainSummaryResponse;
import com.example.Chungbuk.domain.weather.dto.request.RegionWeatherRequest;
import com.example.Chungbuk.domain.weather.dto.response.CurrentWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.FeelsLikeWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.WeatherPageResponse;
import com.example.Chungbuk.domain.weather.service.WeatherService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MainSummaryServiceTest {

    private FestivalContentRepository festivalContentRepository;
    private AccommodationRepository accommodationRepository;
    private CampingRepository campingRepository;
    private WeatherService weatherService;
    private MainSummaryService mainSummaryService;

    @BeforeEach
    void setUp() {
        festivalContentRepository =
                mock(FestivalContentRepository.class);
        accommodationRepository =
                mock(AccommodationRepository.class);
        campingRepository = mock(CampingRepository.class);
        weatherService = mock(WeatherService.class);

        mainSummaryService = new MainSummaryService(
                festivalContentRepository,
                accommodationRepository,
                campingRepository,
                weatherService
        );
    }

    @Test
    void getMainSummaryReturnsCountsAndWeatherSummary() {
        when(festivalContentRepository
                .countByContentTypeIdAndActiveTrue("15"))
                .thenReturn(8L);
        when(festivalContentRepository.countByActiveTrue())
                .thenReturn(120L);
        when(campingRepository.count()).thenReturn(20L);
        when(accommodationRepository.count()).thenReturn(33L);
        when(weatherService.getRegionWeather(
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(
                weatherPage("Cheongju", 24.4, 26.2, 20),
                weatherPage("Chungju", 23.2, 24.1, 10)
        );

        MainSummaryResponse response =
                mainSummaryService.getMainSummary();

        assertEquals(8, response.todayStats().get(0).value());
        assertEquals(140, response.todayStats().get(1).value());
        assertEquals(33, response.todayStats().get(2).value());
        assertEquals("24°C", response.weather().temperature());
        assertEquals("26°C", response.weather().feelsLike());
        assertEquals("20%", response.weather().precipitationProbability());
        assertEquals("60%", response.weather().humidity());
        assertEquals(2, response.weather().regions().size());
        assertFalse(response.featureCards().isEmpty());

        ArgumentCaptor<RegionWeatherRequest> captor =
                ArgumentCaptor.forClass(RegionWeatherRequest.class);
        verify(weatherService, org.mockito.Mockito.times(2))
                .getRegionWeather(captor.capture());
        assertEquals("청주", captor.getAllValues().get(0).getRegion());
        assertEquals("충주", captor.getAllValues().get(1).getRegion());
    }

    @Test
    void getMainSummaryReturnsDefaultWeatherWhenWeatherServiceFails() {
        when(festivalContentRepository
                .countByContentTypeIdAndActiveTrue("15"))
                .thenReturn(8L);
        when(festivalContentRepository.countByActiveTrue())
                .thenReturn(120L);
        when(campingRepository.count()).thenReturn(20L);
        when(accommodationRepository.count()).thenReturn(33L);
        when(weatherService.getRegionWeather(
                org.mockito.ArgumentMatchers.any()
        )).thenThrow(new RuntimeException("weather api failure"));

        MainSummaryResponse response =
                mainSummaryService.getMainSummary();

        assertEquals("24°C", response.weather().temperature());
        assertEquals("26°C", response.weather().feelsLike());
        assertEquals("20%", response.weather().precipitationProbability());
        assertEquals("/clothing", response.weather().href());
        assertFalse(response.weather().regions().isEmpty());
    }

    private WeatherPageResponse weatherPage(
            String region,
            double temperature,
            double feelsLikeTemperature,
            int precipitationProbability
    ) {
        return new WeatherPageResponse(
                region,
                LocalDateTime.of(2026, 6, 28, 10, 0),
                new CurrentWeatherResponse(
                        region,
                        temperature,
                        60,
                        2.5,
                        "Normal",
                        "0mm",
                        "None",
                        precipitationProbability,
                        "Cloudy",
                        "Cloudy"
                ),
                new FeelsLikeWeatherResponse(
                        feelsLikeTemperature,
                        1.8,
                        "Feels like weather.",
                        "Summary",
                        "Detail",
                        List.of("temperature")
                )
        );
    }
}
