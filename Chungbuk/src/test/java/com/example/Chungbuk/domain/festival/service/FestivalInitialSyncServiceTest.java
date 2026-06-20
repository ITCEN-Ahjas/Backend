package com.example.Chungbuk.domain.festival.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.Chungbuk.domain.festival.constant.FestivalInitialSyncPhase;
import com.example.Chungbuk.domain.festival.dto.response.FestivalInitializationResponse;
import com.example.Chungbuk.domain.festival.entity.FestivalInitialSyncState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FestivalInitialSyncServiceTest {

    @Mock
    private FestivalInitialSyncStateService
            festivalInitialSyncStateService;

    @Mock
    private FestivalInitialSyncRunGuard festivalInitialSyncRunGuard;

    @Mock
    private FestivalInitialSyncWorker festivalInitialSyncWorker;

    @InjectMocks
    private FestivalInitialSyncService festivalInitialSyncService;

    @Test
    void startsListSyncWhenDatabaseHasNotStartedInitialSync() {
        FestivalInitialSyncState notStarted =
                FestivalInitialSyncState.createInitial();

        FestivalInitialSyncState running =
                FestivalInitialSyncState.createInitial();

        running.markRunning(FestivalInitialSyncPhase.LIST_SYNCING);

        when(festivalInitialSyncStateService.getCurrentState())
                .thenReturn(notStarted);

        when(festivalInitialSyncRunGuard.tryStart())
                .thenReturn(true);

        when(festivalInitialSyncStateService.startPhase(
                FestivalInitialSyncPhase.LIST_SYNCING
        )).thenReturn(running);

        FestivalInitializationResponse response =
                festivalInitialSyncService.ensureInitialized();

        assertThat(response.isInitializationStarted()).isTrue();
        assertThat(response.isReady()).isFalse();

        assertThat(response.getInitialSyncPhase())
                .isEqualTo(FestivalInitialSyncPhase.LIST_SYNCING);

        verify(festivalInitialSyncWorker).runInitialSync(
                FestivalInitialSyncPhase.LIST_SYNCING
        );
    }

    @Test
    void doesNotStartTourApiSyncWhenInitialSyncIsReady() {
        FestivalInitialSyncState ready =
                FestivalInitialSyncState.createInitial();

        ready.updateStableProgress(
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

        when(festivalInitialSyncStateService.getCurrentState())
                .thenReturn(ready);

        FestivalInitializationResponse response =
                festivalInitialSyncService.ensureInitialized();

        assertThat(response.isInitializationStarted()).isFalse();
        assertThat(response.isReady()).isTrue();

        verify(festivalInitialSyncRunGuard, never()).tryStart();
        verify(festivalInitialSyncWorker, never()).runInitialSync(
                FestivalInitialSyncPhase.LIST_SYNCING
        );
    }

    @Test
    void doesNotStartDuplicateInitialSyncWhenAnotherRunExists() {
        FestivalInitialSyncState waiting =
                FestivalInitialSyncState.createInitial();

        waiting.markWaiting(
                com.example.Chungbuk.domain.festival.constant
                        .FestivalInitialSyncPauseReason
                        .DAILY_QUOTA_EXCEEDED,
                "일일 호출 한도 부족"
        );

        when(festivalInitialSyncStateService.getCurrentState())
                .thenReturn(waiting);

        when(festivalInitialSyncRunGuard.tryStart())
                .thenReturn(false);

        FestivalInitializationResponse response =
                festivalInitialSyncService.ensureInitialized();

        assertThat(response.isInitializationStarted()).isFalse();

        assertThat(response.isInitializationAlreadyRunning())
                .isTrue();

        verify(festivalInitialSyncWorker, never()).runInitialSync(
                FestivalInitialSyncPhase.LIST_SYNCING
        );
    }
}