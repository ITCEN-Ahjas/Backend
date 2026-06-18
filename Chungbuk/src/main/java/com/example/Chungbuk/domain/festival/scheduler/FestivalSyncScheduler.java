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

    @Scheduled(
            cron = "${festival.sync.scheduler.cron:0 0 3 * * *}",
            zone = "${festival.sync.scheduler.zone:Asia/Seoul}"
    )
    public void refreshFestivalContentsDaily() {
        try {
            FestivalSyncResultResponse result =
                    festivalSyncService.refreshFestivalContents();

            log.info(
                    """
                    축제/체험 새벽 자동 refresh 완료
                    - festivalSavedCount={}
                    - experienceSavedCount={}
                    - detailSyncedCount={}
                    - deactivatedCount={}
                    - skippedDetailCount={}
                    - detailEmptyCount={}
                    - failedRequestCount={}
                    - tourApiCallCount={}
                    - message={}
                    """,
                    result.getFestivalSavedCount(),
                    result.getExperienceSavedCount(),
                    result.getDetailSyncedCount(),
                    result.getDeactivatedCount(),
                    result.getSkippedDetailCount(),
                    result.getDetailEmptyCount(),
                    result.getFailedRequestCount(),
                    result.getTourApiCallCount(),
                    result.getMessage()
            );
        } catch (Exception e) {
            log.error("축제/체험 새벽 자동 refresh 중 오류가 발생했습니다.", e);
        }
    }
}
