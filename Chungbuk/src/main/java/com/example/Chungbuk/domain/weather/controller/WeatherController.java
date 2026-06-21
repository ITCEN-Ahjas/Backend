package com.example.Chungbuk.domain.weather.controller;

import com.example.Chungbuk.domain.weather.dto.request.RegionWeatherRequest;
import com.example.Chungbuk.domain.weather.dto.response.WeatherPageResponse;
import com.example.Chungbuk.domain.weather.dto.response.WeatherRegionsResponse;
import com.example.Chungbuk.domain.weather.service.WeatherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/regions")
    public WeatherRegionsResponse getSupportedRegions() {
        return weatherService.getSupportedRegions();
    }

    @GetMapping("/region")
    public WeatherPageResponse getRegionWeather(
            @RequestParam String region
    ) {
        RegionWeatherRequest request = new RegionWeatherRequest();
        request.setRegion(region);

        return weatherService.getRegionWeather(request);
    }
}