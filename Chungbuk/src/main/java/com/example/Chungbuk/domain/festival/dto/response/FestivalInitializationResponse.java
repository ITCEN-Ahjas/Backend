package com.example.Chungbuk.domain.festival.dto.response;

import com.example.Chungbuk.domain.festival.constant.FestivalInitialSyncExecutionStatus;
import com.example.Chungbuk.domain.festival.constant.FestivalInitialSyncPauseReason;
import com.example.Chungbuk.domain.festival.constant.FestivalInitialSyncPhase;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FestivalInitializationResponse {

    private boolean initializationStarted;
    private boolean initializationAlreadyRunning;
    private boolean ready;

    private FestivalInitialSyncPhase initialSyncPhase;

    private FestivalInitialSyncExecutionStatus
            initialSyncExecutionStatus;

    private FestivalInitialSyncPauseReason initialSyncPauseReason;

    private boolean initialListCompleted;
    private boolean initialDetailCompleted;
    private boolean initialImageCompleted;

    private long activeContentCount;
    private long detailSyncedCount;
    private long imageSyncedCount;

    private LocalDateTime lastStartedAt;
    private LocalDateTime lastCompletedAt;

    private String message;
}