package com.example.Chungbuk.domain.festival.scheduler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.Chungbuk.domain.festival.constant.FestivalInitialSyncPhase;
import com.example.Chungbuk.domain.festival.dto.response.FestivalInitializationResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalSyncResultResponse;
import com.example.Chungbuk.domain.festival.entity.FestivalInitialSyncState;
import com.example.Chungbuk.domain.festival.service.FestivalInitialSyncService;
import com.example.Chungbuk.domain.festival.service.FestivalInitialSyncStateService;
import com.example.Chungbuk.domain.festival.service.FestivalSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FestivalSyncSchedulerTest {

    @Mock
    private FestivalSyncService festivalSyncService;

    @Mock
    private FestivalInitialSyncStateService
            festivalInitialSyncStateService;

    @Mock
    private FestivalInitialSyncService festivalInitialSyncService;

    @InjectMocks
    private FestivalSyncScheduler festivalSyncScheduler;

    @Test
    void resumesInitialSyncInsteadOfRunningRefreshWhenNotReady() {
        FestivalInitialSyncState detailState =
                FestivalInitialSyncState.createInitial();

        detailState.markListCompleted();

        FestivalInitializationResponse response =
                FestivalInitializationResponse.builder()
                        .initializationStarted(true)
                        .initializationAlreadyRunning(false)
                        .ready(false)
                        .initialSyncPhase(
                                FestivalInitialSyncPhase.DETAIL_SYNCING
                        )
                        .initialSyncExecutionStatus(
                                detailState.getExecutionStatus()
                        )
                        .initialSyncPauseReason(
                                detailState.getPauseReason()
                        )
                        .message("상세 초기 적재를 시작했습니다.")
                        .build();

        when(festivalInitialSyncStateService.getCurrentState())
                .thenReturn(detailState);

        when(festivalInitialSyncService.ensureInitialized())
                .thenReturn(response);

        festivalSyncScheduler.refreshFestivalContentsDaily();

        verify(festivalInitialSyncService).ensureInitialized();
        verify(festivalSyncService, never()).refreshFestivalContents();
    }

    @Test
    void runsRegularRefreshOnlyAfterInitialSyncIsReady() {
        FestivalInitialSyncState readyState =
                FestivalInitialSyncState.createInitial();

        readyState.updateStableProgress(
                FestivalInitialSyncPhase.READY,
                true,
                true,
                true,
                851L,
                851L,
                851L,
                0L,
                0L
        );

        FestivalSyncResultResponse result =
                mock(FestivalSyncResultResponse.class);

        when(festivalInitialSyncStateService.getCurrentState())
                .thenReturn(readyState);

        when(festivalSyncService.refreshFestivalContents())
                .thenReturn(result);

        festivalSyncScheduler.refreshFestivalContentsDaily();

        verify(festivalSyncService).refreshFestivalContents();
        verify(festivalInitialSyncService, never()).ensureInitialized();
    }
}
