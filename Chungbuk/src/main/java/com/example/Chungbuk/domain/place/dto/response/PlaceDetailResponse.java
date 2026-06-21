package com.example.Chungbuk.domain.place.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PlaceDetailResponse {

    private String placeId;
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    private String primaryType;
    private String primaryTypeName;
    private List<String> types;
    private Double rating;
    private Integer userRatingCount;
    private List<String> photoNames;
    private String photoName;
    private String googleMapsUri;
    private String nationalPhoneNumber;
    private String internationalPhoneNumber;
    private String websiteUri;
    private List<String> weekdayDescriptions;
    private String summary;
}
