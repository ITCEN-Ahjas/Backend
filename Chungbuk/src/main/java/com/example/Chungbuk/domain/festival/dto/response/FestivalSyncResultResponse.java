package com.example.Chungbuk.domain.festival.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FestivalSyncResultResponse {

    private int festivalSavedCount;
    private int experienceSavedCount;
    private int detailSyncedCount;

    private int insertedCount;
    private int updatedCount;
    private int deactivatedCount;

    private int tourApiCallCount;
    private int skippedDetailCount;

    private int detailEmptyCount;
    private int failedRequestCount;

    private int festivalPage;
    private int festivalSize;
    private int experiencePage;
    private int experienceSize;

    private int festivalStartPage;
    private int festivalEndPage;
    private int experienceStartPage;
    private int experienceEndPage;

    private int maxPages;
    private int detailLimit;

    private boolean includeDetail;
    private boolean automaticSync;
    private boolean deactivateMissing;
    private boolean sequential;

    private String eventStartDate;
    private String message;

    private List<FestivalSyncGroupResultResponse> groupResults;
}