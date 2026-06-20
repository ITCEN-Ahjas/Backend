package com.example.Chungbuk.domain.festival.dto.response;

import com.example.Chungbuk.domain.festival.constant.FestivalInitialSyncExecutionStatus;
import com.example.Chungbuk.domain.festival.constant.FestivalInitialSyncPauseReason;
import com.example.Chungbuk.domain.festival.constant.FestivalInitialSyncPhase;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FestivalSyncStatusResponse {

    private long totalCount;

    private long festivalCount;
    private long touristSpotCount;
    private long cultureCount;
    private long leportsCount;

    private long detailSyncedCount;

    private LocalDateTime lastSyncedAt;

    private boolean empty;

    private Map<String, Long> contentTypeCounts;
    private Map<String, Long> categoryCounts;

    private long detailBaselineReadyCount;

    private long imageSyncCompletedCount;
    private long imagePendingCount;
    private long imageStateUnknownCount;

    private long imageAvailableCount;
    private long imageUnavailableCount;

    private long retryScheduledCount;
    private long retryFailureCount;

    /*
     * DB에 저장되는 초기 적재 진행 상태
     */
    private FestivalInitialSyncPhase initialSyncPhase;

    private FestivalInitialSyncExecutionStatus
            initialSyncExecutionStatus;

    private FestivalInitialSyncPauseReason initialSyncPauseReason;

    private boolean initialListCompleted;
    private boolean initialDetailCompleted;
    private boolean initialImageCompleted;

    private LocalDateTime initialSyncLastStartedAt;
    private LocalDateTime initialSyncLastCompletedAt;

    private String initialSyncLastErrorMessage;
}