package com.example.Chungbuk.domain.festival.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FestivalDetailResponse {

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
    private List<String> imageUrls;

    private String tel;
    private String homepage;

    private String overview;
    private String description;
    private String descriptionSource;

    private String mapX;
    private String mapY;

    private String eventPlace;
    private String playTime;
    private String useTimeFestival;
    private String sponsor;

    private String timeLabel;
    private String timeValue;
    private String extraLabel;
    private String extraValue;

    private List<ContentInfoResponse> mainInfo;
}