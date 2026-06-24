package com.example.Chungbuk.domain.festival.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.Chungbuk.domain.festival.constant.FestivalInitialSyncExecutionStatus;
import com.example.Chungbuk.domain.festival.constant.FestivalInitialSyncPauseReason;
import com.example.Chungbuk.domain.festival.constant.FestivalInitialSyncPhase;
import com.example.Chungbuk.domain.festival.entity.FestivalInitialSyncState;
import com.example.Chungbuk.domain.festival.repository.FestivalContentRepository;
import com.example.Chungbuk.domain.festival.repository.FestivalInitialSyncStateRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FestivalInitialSyncStateServiceTest {

    @Mock
    private FestivalInitialSyncStateRepository
            festivalInitialSyncStateRepository;

    @Mock
    private FestivalContentRepository festivalContentRepository;

    @InjectMocks
    private FestivalInitialSyncStateService festivalInitialSyncStateService;

    @Test
    void createsNotStartedStateWhenDatabaseIsEmpty() {
        when(festivalInitialSyncStateRepository.findById(1L))
                .thenReturn(Optional.empty());

        stubProgress(0L, 0L, 0L, 0L, 0L);

        when(festivalInitialSyncStateRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FestivalInitialSyncState state =
                festivalInitialSyncStateService.getCurrentState();

        assertThat(state.getPhase())
                .isEqualTo(FestivalInitialSyncPhase.NOT_STARTED);

        assertThat(state.getExecutionStatus())
                .isEqualTo(FestivalInitialSyncExecutionStatus.IDLE);

        assertThat(state.getPauseReason())
                .isEqualTo(FestivalInitialSyncPauseReason.NONE);

        assertThat(state.isListCompleted()).isFalse();
        assertThat(state.isDetailCompleted()).isFalse();
        assertThat(state.isImageCompleted()).isFalse();
    }

    @Test
    void createsReadyStateForExistingCompletedDatabase() {
        when(festivalInitialSyncStateRepository.findById(1L))
                .thenReturn(Optional.empty());

        stubProgress(854L, 854L, 854L, 0L, 0L);

        when(festivalInitialSyncStateRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FestivalInitialSyncState state =
                festivalInitialSyncStateService.getCurrentState();

        assertThat(state.getPhase())
                .isEqualTo(FestivalInitialSyncPhase.READY);

        assertThat(state.isListCompleted()).isTrue();
        assertThat(state.isDetailCompleted()).isTrue();
        assertThat(state.isImageCompleted()).isTrue();
    }

    @Test
    void keepsDetailSyncingStateWhenListIsCompletedButDetailsRemain() {
        FestivalInitialSyncState state =
                FestivalInitialSyncState.createInitial();

        state.markListCompleted();

        when(festivalInitialSyncStateRepository.findById(1L))
                .thenReturn(Optional.of(state));

        stubProgress(854L, 600L, 0L, 0L, 0L);

        when(festivalInitialSyncStateRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FestivalInitialSyncState result =
                festivalInitialSyncStateService.getCurrentState();

        assertThat(result.getPhase())
                .isEqualTo(FestivalInitialSyncPhase.DETAIL_SYNCING);

        assertThat(result.isListCompleted()).isTrue();
        assertThat(result.isDetailCompleted()).isFalse();
        assertThat(result.isImageCompleted()).isFalse();
    }

    @Test
    void convertsRunningStateToServerInterruptedWaitingState() {
        FestivalInitialSyncState state =
                FestivalInitialSyncState.createInitial();

        state.markRunning(FestivalInitialSyncPhase.IMAGE_SYNCING);

        when(festivalInitialSyncStateRepository.findById(1L))
                .thenReturn(Optional.of(state));

        when(festivalInitialSyncStateRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FestivalInitialSyncState result =
                festivalInitialSyncStateService.markServerInterrupted(
                        "서버 재시작으로 이미지 초기 적재가 중단되었습니다."
                );

        assertThat(result.getPhase())
                .isEqualTo(FestivalInitialSyncPhase.IMAGE_SYNCING);

        assertThat(result.getExecutionStatus())
                .isEqualTo(FestivalInitialSyncExecutionStatus.WAITING);

        assertThat(result.getPauseReason())
                .isEqualTo(
                        FestivalInitialSyncPauseReason.SERVER_INTERRUPTED
                );
    }

    private void stubProgress(
            long activeContentCount,
            long detailSyncedCount,
            long imageSyncedCount,
            long retryScheduledCount,
            long retryFailureCount
    ) {
        when(festivalContentRepository.countByActiveTrue())
                .thenReturn(activeContentCount);

        when(festivalContentRepository
                .countByActiveTrueAndDetailSourceUpdatedAtIsNotNull())
                .thenReturn(detailSyncedCount);

        when(festivalContentRepository
                .countByActiveTrueAndImageSyncCompletedTrue())
                .thenReturn(imageSyncedCount);

        when(festivalContentRepository
                .countByActiveTrueAndNextDetailRetryAtIsNotNull())
                .thenReturn(retryScheduledCount);

        when(festivalContentRepository
                .countByActiveTrueAndLastDetailFailureReasonIsNotNull())
                .thenReturn(retryFailureCount);
    }
}
