package com.example.Chungbuk.domain.accommodation.scheduler;

import com.example.Chungbuk.domain.accommodation.constant.AccommodationSyncPhase;
import com.example.Chungbuk.domain.accommodation.dto.response.AccommodationInitializationResponse;
import com.example.Chungbuk.domain.accommodation.entity.AccommodationSyncState;
import com.example.Chungbuk.domain.accommodation.service.AccommodationInitialSyncService;
import com.example.Chungbuk.domain.accommodation.service.AccommodationSyncStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccommodationSyncScheduler {

    private final AccommodationSyncStateService accommodationSyncStateService;
    private final AccommodationInitialSyncService accommodationInitialSyncService;

    @Scheduled(
            cron = "${accommodation.sync.scheduler.cron:0 30 3 * * *}",
            zone = "${accommodation.sync.scheduler.zone:Asia/Seoul}"
    )
    public void resumeAccommodationSyncDaily() {
        try {
            AccommodationSyncState state = accommodationSyncStateService.getCurrentState();

            if (state.getPhase() == AccommodationSyncPhase.READY) {
                return;
            }

            AccommodationInitializationResponse response =
                    accommodationInitialSyncService.ensureInitialized();

            log.info(
                    "숙소 배치 수집 새벽 재개 - phase={}, status={}, started={}, message={}",
                    response.getSyncPhase(),
                    response.getExecutionStatus(),
                    response.isSyncStarted(),
                    response.getMessage()
            );
        } catch (Exception e) {
            log.error("숙소 배치 수집 스케줄러 오류", e);
        }
    }
}
