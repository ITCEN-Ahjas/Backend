package com.example.Chungbuk.domain.festival.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.Chungbuk.domain.festival.constant.FestivalInitialSyncPhase;
import com.example.Chungbuk.domain.festival.dto.response.FestivalSyncResultResponse;
import com.example.Chungbuk.domain.festival.entity.FestivalInitialSyncState;
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
    void waitsWhenListCollectionIsNotComplete() {
        FestivalSyncResultResponse result = mock(
                FestivalSyncResultResponse.class
        );

        when(result.isListSyncCompleted()).thenReturn(false);
        when(result.isDailyQuotaExceeded()).thenReturn(false);
        when(result.getSkippedDetailCount()).thenReturn(0);

        when(festivalSyncService.syncInitialLists(30, 50, 900))
                .thenReturn(result);

        festivalInitialSyncWorker.runInitialSync(
                FestivalInitialSyncPhase.LIST_SYNCING
        );

        verify(festivalInitialSyncStateService)
                .waitForTourApiError(anyString());

        verify(festivalInitialSyncStateService, never())
                .completeListPhaseAndReconcile();

        verify(festivalInitialSyncRunGuard).finish();
    }

    @Test
    void waitsForDailyQuotaWhenInitialDetailWorkIsSkipped() {
        FestivalSyncResultResponse result = mock(
                FestivalSyncResultResponse.class
        );

        when(result.isDailyQuotaExceeded()).thenReturn(true);

        FestivalInitialSyncState detailState =
                FestivalInitialSyncState.createInitial();

        detailState.markListCompleted();

        when(festivalSyncService.syncInitialDetails(1000, 900))
                .thenReturn(result);

        when(festivalInitialSyncStateService
                .completeCurrentPhaseAndReconcile())
                .thenReturn(detailState);

        festivalInitialSyncWorker.runInitialSync(
                FestivalInitialSyncPhase.DETAIL_SYNCING
        );

        verify(festivalInitialSyncStateService)
                .waitForDailyQuota(anyString());

        verify(festivalInitialSyncRunGuard).finish();
    }

    @Test
    void runsImagePhaseAfterDetailPhaseIsCompleted() {
        FestivalSyncResultResponse detailResult = mock(
                FestivalSyncResultResponse.class
        );

        when(detailResult.isDailyQuotaExceeded()).thenReturn(false);
        when(detailResult.getSkippedDetailCount()).thenReturn(0);

        FestivalSyncResultResponse imageResult = mock(
                FestivalSyncResultResponse.class
        );

        when(imageResult.isDailyQuotaExceeded()).thenReturn(false);
        when(imageResult.getSkippedDetailCount()).thenReturn(0);

        FestivalInitialSyncState imageState =
                FestivalInitialSyncState.createInitial();

        imageState.markListCompleted();
        imageState.markRunning(FestivalInitialSyncPhase.IMAGE_SYNCING);

        FestivalInitialSyncState readyState =
                FestivalInitialSyncState.createInitial();

        readyState.updateStableProgress(
                FestivalInitialSyncPhase.READY,
                true,
                true,
                true,
                10L,
                10L,
                10L,
                0L,
                0L
        );

        when(festivalSyncService.syncInitialDetails(1000, 900))
                .thenReturn(detailResult);

        when(festivalSyncService.syncInitialImages(1000, 900))
                .thenReturn(imageResult);

        when(festivalInitialSyncStateService
                .completeCurrentPhaseAndReconcile())
                .thenReturn(imageState, readyState);

        festivalInitialSyncWorker.runInitialSync(
                FestivalInitialSyncPhase.DETAIL_SYNCING
        );

        verify(festivalInitialSyncStateService)
                .startPhase(FestivalInitialSyncPhase.IMAGE_SYNCING);

        verify(festivalSyncService)
                .syncInitialImages(1000, 900);

        verify(festivalInitialSyncRunGuard).finish();
    }
}