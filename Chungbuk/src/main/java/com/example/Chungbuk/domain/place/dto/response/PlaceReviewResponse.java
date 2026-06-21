package com.example.Chungbuk.domain.place.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PlaceReviewResponse {

    private String reviewId;
    private String authorName;
    private String authorUri;
    private String authorPhotoUri;
    private Integer rating;
    private String text;
    private String originalText;
    private String languageCode;
    private String relativePublishTimeDescription;
    private String publishTime;
}
