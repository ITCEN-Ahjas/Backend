package com.example.Chungbuk.domain.festival.dto.response;

import java.time.LocalDate;
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

    /*
     * 이번 동기화 실행에서 실제 TourAPI로 요청을 보낸 횟수입니다.
     *
     * 일일 호출 예산이 부족해서 요청 자체를 보내지 못한 경우는 포함하지 않습니다.
     */
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

    /*
     * 일일 TourAPI 예산 상태입니다.
     *
     * Scheduler, 수동 refresh, bootstrap, 단건 상세 동기화가
     * 동일한 날짜의 값을 공유합니다.
     */
    private LocalDate quotaUsageDate;
    private int dailyTourApiLimit;
    private int dailyTourApiUsedCount;
    private int dailyTourApiRemainingCount;
    private boolean dailyQuotaExceeded;

    private List<FestivalSyncGroupResultResponse> groupResults;
}