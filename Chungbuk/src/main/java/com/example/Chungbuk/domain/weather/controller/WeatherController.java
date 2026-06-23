package com.example.Chungbuk.domain.weather.controller;

import com.example.Chungbuk.domain.weather.dto.request.OutfitRecommendationRequest;
import com.example.Chungbuk.domain.weather.dto.request.RegionWeatherRequest;
import com.example.Chungbuk.domain.weather.dto.response.RegionBatchOutfitRecommendationResponse;
import com.example.Chungbuk.domain.weather.dto.response.RegionOutfitRecommendationResponse;
import com.example.Chungbuk.domain.weather.dto.response.WeatherPageResponse;
import com.example.Chungbuk.domain.weather.dto.response.WeatherRegionsResponse;
import com.example.Chungbuk.domain.weather.service.OutfitRecommendationService;
import com.example.Chungbuk.domain.weather.service.WeatherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherService weatherService;

    private final OutfitRecommendationService
            outfitRecommendationService;

    public WeatherController(
            WeatherService weatherService,
            OutfitRecommendationService outfitRecommendationService
    ) {
        this.weatherService = weatherService;
        this.outfitRecommendationService =
                outfitRecommendationService;
    }

    @GetMapping("/regions")
    public WeatherRegionsResponse getSupportedRegions() {
        return weatherService.getSupportedRegions();
    }

    @GetMapping("/region")
    public WeatherPageResponse getRegionWeather(
            @RequestParam String region
    ) {
        RegionWeatherRequest request =
                new RegionWeatherRequest();

        request.setRegion(region);

        return weatherService.getRegionWeather(request);
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
}