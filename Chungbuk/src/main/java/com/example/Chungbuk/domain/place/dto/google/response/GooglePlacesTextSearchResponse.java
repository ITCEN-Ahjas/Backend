package com.example.Chungbuk.domain.place.dto.google.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GooglePlacesTextSearchResponse {

    private List<Place> places;
    private String nextPageToken;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Place {

        private String id;
        private LocalizedText displayName;
        private String formattedAddress;
        private LatLng location;
        private String primaryType;
        private LocalizedText primaryTypeDisplayName;
        private List<String> types;
        private Double rating;
        private Integer userRatingCount;
        private List<Photo> photos;
        private String googleMapsUri;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LocalizedText {

        private String text;
        private String languageCode;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LatLng {

        private double latitude;
        private double longitude;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Photo {

        private String name;
        private Integer widthPx;
        private Integer heightPx;
    }
}
