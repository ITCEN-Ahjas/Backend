package com.example.Chungbuk.domain.place.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PlaceSummaryResponse {

    private String placeId;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private String category;
    private String primaryType;
    private String primaryTypeName;
    private List<String> types;
    private Double rating;
    private Integer userRatingCount;
    private String photoName;
    private String googleMapsUri;
}
