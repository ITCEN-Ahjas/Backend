package com.example.Chungbuk.domain.place.dto.google.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GooglePlaceDetailResponse {

    private String id;
    private GooglePlacesTextSearchResponse.LocalizedText displayName;
    private String formattedAddress;
    private GooglePlacesTextSearchResponse.LatLng location;
    private String primaryType;
    private GooglePlacesTextSearchResponse.LocalizedText primaryTypeDisplayName;
    private List<String> types;
    private Double rating;
    private Integer userRatingCount;
    private List<GooglePlacesTextSearchResponse.Photo> photos;
    private String googleMapsUri;
    private String nationalPhoneNumber;
    private String internationalPhoneNumber;
    private String websiteUri;
    private OpeningHours regularOpeningHours;
    private GooglePlacesTextSearchResponse.LocalizedText editorialSummary;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpeningHours {

        private List<String> weekdayDescriptions;
    }
}
