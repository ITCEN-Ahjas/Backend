package com.example.Chungbuk.domain.camping.dto.response;

import com.example.Chungbuk.domain.camping.constant.CampingSyncExecutionStatus;
import com.example.Chungbuk.domain.camping.constant.CampingSyncPauseReason;
import com.example.Chungbuk.domain.camping.constant.CampingSyncPhase;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CampingSyncStatusResponse {

    private CampingSyncPhase syncPhase;
    private CampingSyncExecutionStatus executionStatus;
    private CampingSyncPauseReason pauseReason;
    private long savedCount;
    private LocalDateTime lastStartedAt;
    private LocalDateTime lastCompletedAt;
    private String lastErrorMessage;
}
