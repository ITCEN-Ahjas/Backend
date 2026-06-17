package com.example.Chungbuk.domain.place.dto.google.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GooglePlacesTextSearchRequest {

    private String textQuery;
    private String includedType;
    private Boolean strictTypeFiltering;
    private String languageCode;
    private String regionCode;
    private Integer pageSize;
    private LocationRestriction locationRestriction;

    @Getter
    @Builder
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LocationRestriction {

        private Rectangle rectangle;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Rectangle {

        private LatLng low;
        private LatLng high;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class LatLng {

        private double latitude;
        private double longitude;
    }
}
