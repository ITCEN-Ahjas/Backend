package com.example.Chungbuk.domain.festival.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FestivalSummaryResponse {

    private String id;
    private String title;
    private String region;
    private String category;
    private String status;
    private String startDate;
    private String endDate;
    private String address;
    private String imageUrl;
    private String tel;
    private String mapX;
    private String mapY;
}