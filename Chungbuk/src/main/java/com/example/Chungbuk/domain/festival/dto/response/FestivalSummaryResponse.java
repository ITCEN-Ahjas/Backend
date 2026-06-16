package com.example.Chungbuk.domain.festival.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FestivalSummaryResponse {

    private String id;

    private String contentTypeId;
    private String cat1;
    private String cat2;
    private String cat3;

    private String title;
    private String region;

    private String category;
    private String themeCategory;

    private String status;
    private String startDate;
    private String endDate;

    private String address;
    private String imageUrl;
    private String tel;

    private String mapX;
    private String mapY;

    private String timeLabel;
    private String timeValue;
    private String extraLabel;
    private String extraValue;
}