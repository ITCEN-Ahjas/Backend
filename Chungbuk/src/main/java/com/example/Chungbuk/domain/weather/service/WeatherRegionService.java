package com.example.Chungbuk.domain.weather.service;

import com.example.Chungbuk.domain.weather.constant.ChungbukRegion;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class WeatherRegionService {

    public ChungbukRegion getRegion(String regionName) {
        return ChungbukRegion.fromDisplayName(regionName);
    }

    public List<ChungbukRegion> getAllRegions() {
        return Arrays.asList(ChungbukRegion.values());
    }

    public List<String> getAllRegionNames() {
        return Arrays.stream(ChungbukRegion.values())
                .map(ChungbukRegion::getDisplayName)
                .toList();
    }
}