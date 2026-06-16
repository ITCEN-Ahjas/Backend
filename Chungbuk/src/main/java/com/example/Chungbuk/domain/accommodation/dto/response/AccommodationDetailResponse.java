package com.example.Chungbuk.domain.accommodation.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AccommodationDetailResponse {

    private String id;

    private String cat1;
    private String cat2;
    private String cat3;

    private String title;
    private String region;
    private String category;

    private String address;
    private String imageUrl;
    private List<String> imageUrls;

    private String tel;
    private String homepage;

    private String overview;
    private String description;
    private String descriptionSource;

    private String mapX;
    private String mapY;

    private String checkInTime;
    private String checkOutTime;
    private String parking;
    private String cookingAvailable;
    private String roomCount;
    private String infoCenter;

    private List<RoomInfoResponse> rooms;
}
