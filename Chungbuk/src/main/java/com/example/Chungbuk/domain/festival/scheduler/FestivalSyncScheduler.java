package com.example.Chungbuk.domain.festival.scheduler;

import com.example.Chungbuk.domain.festival.dto.response.FestivalSyncResultResponse;
import com.example.Chungbuk.domain.festival.service.FestivalSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FestivalSyncScheduler {

    private final FestivalSyncService festivalSyncService;

    private static final int SCHEDULED_SYNC_SIZE = 30;
    private static final int SCHEDULED_SYNC_MAX_PAGES = 20;
    private static final int SCHEDULED_SYNC_DETAIL_LIMIT = 30;
    private static final String SCHEDULED_SYNC_EVENT_START_DATE = "20230101";

    @Scheduled(
            cron = "${festival.sync.scheduler.cron:0 0 3 * * *}",
            zone = "${festival.sync.scheduler.zone:Asia/Seoul}"
    )
    public void syncFestivalContentsDaily() {
        try {
            FestivalSyncResultResponse result = festivalSyncService.syncFestivalContentsAll(
                    SCHEDULED_SYNC_SIZE,
                    SCHEDULED_SYNC_MAX_PAGES,
                    SCHEDULED_SYNC_EVENT_START_DATE,
                    true,
                    SCHEDULED_SYNC_DETAIL_LIMIT,
                    true
            );

            log.info(
                    "축제/체험 자동 동기화 완료 - totalSaved={}, festivalSaved={}, experienceSaved={}, detailSynced={}, deactivated={}, tourApiCallCount={}",
                    result.getFestivalSavedCount() + result.getExperienceSavedCount(),
                    result.getFestivalSavedCount(),
                    result.getExperienceSavedCount(),
                    result.getDetailSyncedCount(),
                    result.getDeactivatedCount(),
                    result.getTourApiCallCount()
            );
        } catch (Exception e) {
            log.error("축제/체험 자동 동기화 중 오류가 발생했습니다. 자동 비활성화 처리는 수행되지 않습니다.", e);
        }
    }
}