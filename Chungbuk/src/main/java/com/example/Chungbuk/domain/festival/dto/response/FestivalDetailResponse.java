package com.example.Chungbuk.domain.festival.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class FestivalDetailResponse {

    private String id;
    private String title;
    private String region;
    private String category;
    private String status;
    private String startDate;
    private String endDate;
    private String address;
    private String imageUrl;
    private List<String> imageUrls;
    private String tel;
    private String homepage;
    private String overview;
    private String mapX;
    private String mapY;
    private String eventPlace;
    private String playTime;
    private String useTimeFestival;
    private String sponsor;
}