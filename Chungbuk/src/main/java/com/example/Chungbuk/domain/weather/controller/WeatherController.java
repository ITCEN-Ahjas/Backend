package com.example.Chungbuk.domain.weather.controller;

import com.example.Chungbuk.domain.weather.dto.request.OutfitRecommendationRequest;
import com.example.Chungbuk.domain.weather.dto.request.RegionWeatherRequest;
import com.example.Chungbuk.domain.weather.dto.response.RegionBatchOutfitRecommendationResponse;
import com.example.Chungbuk.domain.weather.dto.response.RegionOutfitRecommendationResponse;
import com.example.Chungbuk.domain.weather.dto.response.RegionTimeSlotOutfitRecommendationResponse;
import com.example.Chungbuk.domain.weather.dto.response.RegionTimeSlotWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.ResidenceCitySearchResponse;
import com.example.Chungbuk.domain.weather.dto.response.WeatherPageResponse;
import com.example.Chungbuk.domain.weather.dto.response.WeatherRegionsResponse;
import com.example.Chungbuk.domain.weather.service.OutfitRecommendationService;
import com.example.Chungbuk.domain.weather.service.ResidenceCityWeatherService;
import com.example.Chungbuk.domain.weather.service.WeatherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherService weatherService;

    private final OutfitRecommendationService
            outfitRecommendationService;

    private final ResidenceCityWeatherService
            residenceCityWeatherService;

    @Autowired
    public WeatherController(
            WeatherService weatherService,
            OutfitRecommendationService outfitRecommendationService,
            ResidenceCityWeatherService residenceCityWeatherService
    ) {
        this.weatherService = weatherService;
        this.outfitRecommendationService =
                outfitRecommendationService;
        this.residenceCityWeatherService =
                residenceCityWeatherService;
    }

    WeatherController(
            WeatherService weatherService,
            OutfitRecommendationService outfitRecommendationService
    ) {
        this(
                weatherService,
                outfitRecommendationService,
                null
        );
    }

    @GetMapping("/regions")
    public WeatherRegionsResponse getSupportedRegions() {
        return weatherService.getSupportedRegions();
    }

    @GetMapping("/region")
    public WeatherPageResponse getRegionWeather(
            @RequestParam String region
    ) {
        return weatherService.getRegionWeather(
                createRegionWeatherRequest(region)
        );
    }

    @GetMapping("/region/time-slots")
    public RegionTimeSlotWeatherResponse getRegionTimeSlotWeather(
            @RequestParam String region
    ) {
        return weatherService.getRegionTimeSlotWeather(
                createRegionWeatherRequest(region)
        );
    }

    @GetMapping("/residence-cities")
    public List<ResidenceCitySearchResponse> searchResidenceCities(
            @RequestParam String countryCode,
            @RequestParam String query
    ) {
        return residenceCityWeatherService.searchCities(
                countryCode,
                query
        );
    }

    @GetMapping("/outfit-recommendation")
    public RegionOutfitRecommendationResponse
    getOutfitRecommendation(
            @RequestParam String region,
            @RequestParam String travelStyle
    ) {
        OutfitRecommendationRequest request =
                new OutfitRecommendationRequest(
                        region,
                        travelStyle
                );

        return outfitRecommendationService.recommend(request);
    }

    @GetMapping("/outfit-recommendations")
    public RegionBatchOutfitRecommendationResponse
    getBatchOutfitRecommendations(
            @RequestParam String region
    ) {
        return outfitRecommendationService.recommendBatch(region);
    }

    @GetMapping("/outfit-recommendations/time-slots")
    public RegionTimeSlotOutfitRecommendationResponse
    getTimeSlotOutfitRecommendations(
            @RequestParam String region,
            @RequestParam(required = false) String residenceCity,
            @RequestParam(required = false) String residenceCountryCode
    ) {
        return outfitRecommendationService.recommendTimeSlots(
                region,
                residenceCity,
                residenceCountryCode
        );
    }

    private RegionWeatherRequest createRegionWeatherRequest(
            String region
    ) {
        RegionWeatherRequest request = new RegionWeatherRequest();
        request.setRegion(region);

        return request;
    }
}
