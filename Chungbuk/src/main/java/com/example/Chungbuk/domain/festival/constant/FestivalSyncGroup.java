package com.example.Chungbuk.domain.festival.constant;

import java.util.List;

public enum FestivalSyncGroup {

    FESTIVAL(
            "FESTIVAL",
            "축제",
            "15",
            "축제"
    ),

    PERFORMANCE(
            "PERFORMANCE",
            "공연",
            "15",
            "공연"
    ),

    EVENT(
            "EVENT",
            "행사",
            "15",
            "행사"
    ),

    TOURIST_ATTRACTION(
            "TOURIST_ATTRACTION",
            "관광지",
            "12",
            "관광지"
    ),

    CULTURAL_FACILITY(
            "CULTURAL_FACILITY",
            "문화시설",
            "14",
            "문화시설"
    ),

    LEPORTS(
            "LEPORTS",
            "레포츠",
            "28",
            "레포츠"
    );

    private final String code;
    private final String displayName;
    private final String contentTypeId;
    private final String category;

    FestivalSyncGroup(
            String code,
            String displayName,
            String contentTypeId,
            String category
    ) {
        this.code = code;
        this.displayName = displayName;
        this.contentTypeId = contentTypeId;
        this.category = category;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getContentTypeId() {
        return contentTypeId;
    }

    public String getCategory() {
        return category;
    }

    public boolean isEventContent() {
        return "15".equals(contentTypeId);
    }

    public static List<FestivalSyncGroup> orderedGroups() {
        return List.of(
                FESTIVAL,
                PERFORMANCE,
                EVENT,
                TOURIST_ATTRACTION,
                CULTURAL_FACILITY,
                LEPORTS
        );
    }
}