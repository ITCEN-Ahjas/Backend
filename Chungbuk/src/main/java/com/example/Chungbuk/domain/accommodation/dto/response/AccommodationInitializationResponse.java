package com.example.Chungbuk.domain.accommodation.dto.response;

import com.example.Chungbuk.domain.accommodation.constant.AccommodationSyncExecutionStatus;
import com.example.Chungbuk.domain.accommodation.constant.AccommodationSyncPauseReason;
import com.example.Chungbuk.domain.accommodation.constant.AccommodationSyncPhase;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccommodationInitializationResponse {

    private boolean syncStarted;
    private boolean syncAlreadyRunning;
    private boolean ready;
    private AccommodationSyncPhase syncPhase;
    private AccommodationSyncExecutionStatus executionStatus;
    private AccommodationSyncPauseReason pauseReason;
    private long listSavedCount;
    private long detailSyncedCount;
    private LocalDateTime lastStartedAt;
    private LocalDateTime lastCompletedAt;
    private String message;
}
