package com.example.Chungbuk.domain.place.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlaceCategory {

    ALL("전체", "여행 명소", null),
    TOURIST_ATTRACTION("관광지", "관광지", "tourist_attraction"),
    RESTAURANT("음식점", "음식점", "restaurant"),
    SHOPPING("쇼핑", "쇼핑", "shopping_mall");

    private final String displayName;
    private final String searchTerm;
    private final String googleIncludedType;
}
