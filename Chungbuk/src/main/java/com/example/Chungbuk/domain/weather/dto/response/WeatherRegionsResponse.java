package com.example.Chungbuk.domain.weather.dto.response;

import java.util.List;

public class WeatherRegionsResponse {

    private final List<String> regions;

    public WeatherRegionsResponse(List<String> regions) {
        this.regions = List.copyOf(regions);
    }

    public List<String> getRegions() {
        return regions;
    }
}