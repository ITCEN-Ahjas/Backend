package com.example.Chungbuk.domain.camping.scheduler;

import com.example.Chungbuk.domain.camping.constant.CampingSyncPhase;
import com.example.Chungbuk.domain.camping.dto.response.CampingInitializationResponse;
import com.example.Chungbuk.domain.camping.entity.CampingSyncState;
import com.example.Chungbuk.domain.camping.service.CampingInitialSyncService;
import com.example.Chungbuk.domain.camping.service.CampingSyncStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CampingSyncScheduler {

    private final CampingSyncStateService campingSyncStateService;
    private final CampingInitialSyncService campingInitialSyncService;

    @Scheduled(
            cron = "${camping.sync.scheduler.cron:0 0 4 * * *}",
            zone = "${camping.sync.scheduler.zone:Asia/Seoul}"
    )
    public void resumeCampingSyncDaily() {
        try {
            CampingSyncState state = campingSyncStateService.getCurrentState();

            if (state.getPhase() == CampingSyncPhase.READY) {
                return;
            }

            CampingInitializationResponse response =
                    campingInitialSyncService.ensureInitialized();

            log.info(
                    "캠핑장 배치 수집 새벽 재개 - phase={}, status={}, started={}, message={}",
                    response.getSyncPhase(),
                    response.getExecutionStatus(),
                    response.isSyncStarted(),
                    response.getMessage()
            );
        } catch (Exception e) {
            log.error("캠핑장 배치 수집 스케줄러 오류", e);
        }
    }
}
