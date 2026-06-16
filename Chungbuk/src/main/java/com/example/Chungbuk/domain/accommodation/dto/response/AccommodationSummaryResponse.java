package com.example.Chungbuk.domain.accommodation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AccommodationSummaryResponse {

    private String id;

    private String cat1;
    private String cat2;
    private String cat3;

    private String title;
    private String region;
    private String category;

    private String address;
    private String imageUrl;
    private String tel;

    private String mapX;
    private String mapY;
}
