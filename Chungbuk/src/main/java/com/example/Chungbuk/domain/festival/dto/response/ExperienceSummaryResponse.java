package com.example.Chungbuk.domain.festival.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ExperienceSummaryResponse {

    private String id;
    private String title;
    private String region;
    private String category;
    private String address;
    private String imageUrl;
    private String tel;
    private String mapX;
    private String mapY;
    private String contentTypeId;
}