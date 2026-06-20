package com.example.Chungbuk.domain.festival.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.Chungbuk.domain.festival.constant.FestivalInitialSyncPhase;
import com.example.Chungbuk.domain.festival.dto.response.FestivalSyncResultResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FestivalInitialSyncWorkerTest {

    @Mock
    private FestivalSyncService festivalSyncService;

    @Mock
    private FestivalInitialSyncStateService
            festivalInitialSyncStateService;

    @Mock
    private FestivalInitialSyncRunGuard festivalInitialSyncRunGuard;

    @InjectMocks
    private FestivalInitialSyncWorker festivalInitialSyncWorker;

    @Test
    void completesListPhaseAfterSuccessfulBootstrap() {
        FestivalSyncResultResponse result =
                mock(FestivalSyncResultResponse.class);

        when(result.isDailyQuotaExceeded()).thenReturn(false);

        when(festivalSyncService.bootstrapFestivalContents(
                30,
                50,
                900,
                true
        )).thenReturn(result);

        festivalInitialSyncWorker.runInitialSync(
                FestivalInitialSyncPhase.LIST_SYNCING
        );

        verify(festivalInitialSyncStateService)
                .completeListPhaseAndReconcile();

        verify(festivalInitialSyncRunGuard).finish();
    }

    @Test
    void waitsForDailyQuotaWhenBootstrapUsesAllDailyCalls() {
        FestivalSyncResultResponse result =
                mock(FestivalSyncResultResponse.class);

        when(result.isDailyQuotaExceeded()).thenReturn(true);

        when(festivalSyncService.bootstrapFestivalContents(
                30,
                50,
                900,
                true
        )).thenReturn(result);

        festivalInitialSyncWorker.runInitialSync(
                FestivalInitialSyncPhase.LIST_SYNCING
        );

        verify(festivalInitialSyncStateService)
                .waitForDailyQuota(
                        "당일 TourAPI 호출 한도에 도달해 "
                                + "다음 자동 실행에서 초기 적재를 이어서 처리합니다."
                );

        verify(festivalInitialSyncRunGuard).finish();
    }

    @Test
    void resumesDetailPhaseWithRefresh() {
        FestivalSyncResultResponse result =
                mock(FestivalSyncResultResponse.class);

        when(result.isDailyQuotaExceeded()).thenReturn(false);

        when(festivalSyncService.refreshFestivalContents(
                30,
                50,
                900,
                true
        )).thenReturn(result);

        festivalInitialSyncWorker.runInitialSync(
                FestivalInitialSyncPhase.DETAIL_SYNCING
        );

        verify(festivalInitialSyncStateService)
                .completeCurrentPhaseAndReconcile();

        verify(festivalInitialSyncRunGuard).finish();
    }
}