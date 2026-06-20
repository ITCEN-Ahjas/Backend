package com.example.Chungbuk.domain.festival.service;

import com.example.Chungbuk.domain.festival.constant.FestivalInitialSyncExecutionStatus;
import com.example.Chungbuk.domain.festival.constant.FestivalInitialSyncPauseReason;
import com.example.Chungbuk.domain.festival.constant.FestivalInitialSyncPhase;
import com.example.Chungbuk.domain.festival.entity.FestivalInitialSyncState;
import com.example.Chungbuk.domain.festival.repository.FestivalContentRepository;
import com.example.Chungbuk.domain.festival.repository.FestivalInitialSyncStateRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FestivalInitialSyncStateService {

    private final FestivalInitialSyncStateRepository
            festivalInitialSyncStateRepository;

    private final FestivalContentRepository festivalContentRepository;

    public FestivalInitialSyncState getCurrentState() {
        Optional<FestivalInitialSyncState> existingState =
                festivalInitialSyncStateRepository.findById(
                        FestivalInitialSyncState.SINGLETON_ID
                );

        FestivalInitialSyncState state = existingState.orElseGet(
                FestivalInitialSyncState::createInitial
        );

        SyncProgressSnapshot snapshot = loadSnapshot();

        if (state.isRunning()
                || state.isWaiting()
                || state.isFailed()) {
            state.updateMetrics(
                    snapshot.activeContentCount(),
                    snapshot.detailSyncedCount(),
                    snapshot.imageSyncedCount(),
                    snapshot.retryScheduledCount(),
                    snapshot.retryFailureCount()
            );

            return festivalInitialSyncStateRepository.save(state);
        }

        reconcileStableState(
                state,
                snapshot,
                existingState.isEmpty()
        );

        return festivalInitialSyncStateRepository.save(state);
    }

    public FestivalInitialSyncState startPhase(
            FestivalInitialSyncPhase phase
    ) {
        FestivalInitialSyncState state = getCurrentState();

        state.markRunning(phase);

        return festivalInitialSyncStateRepository.save(state);
    }

    public FestivalInitialSyncState waitForDailyQuota(
            String errorMessage
    ) {
        FestivalInitialSyncState state = getCurrentState();

        state.markWaiting(
                FestivalInitialSyncPauseReason.DAILY_QUOTA_EXCEEDED,
                errorMessage
        );

        return festivalInitialSyncStateRepository.save(state);
    }

    public FestivalInitialSyncState waitForTourApiError(
            String errorMessage
    ) {
        FestivalInitialSyncState state = getCurrentState();

        state.markWaiting(
                FestivalInitialSyncPauseReason.TOUR_API_ERROR,
                errorMessage
        );

        return festivalInitialSyncStateRepository.save(state);
    }

    public FestivalInitialSyncState markServerInterrupted(
            String errorMessage
    ) {
        FestivalInitialSyncState state = getCurrentState();

        if (state.getExecutionStatus()
                != FestivalInitialSyncExecutionStatus.RUNNING) {
            return state;
        }

        state.markWaiting(
                FestivalInitialSyncPauseReason.SERVER_INTERRUPTED,
                errorMessage
        );

        return festivalInitialSyncStateRepository.save(state);
    }

    public FestivalInitialSyncState markFailed(String errorMessage) {
        FestivalInitialSyncState state = getCurrentState();

        state.markFailed(errorMessage);

        return festivalInitialSyncStateRepository.save(state);
    }

    private void reconcileStableState(
            FestivalInitialSyncState state,
            SyncProgressSnapshot snapshot,
            boolean createdFromExistingDatabase
    ) {
        if (snapshot.activeContentCount() == 0L) {
            state.updateProgress(
                    FestivalInitialSyncPhase.NOT_STARTED,
                    false,
                    false,
                    false,
                    0L,
                    0L,
                    0L,
                    snapshot.retryScheduledCount(),
                    snapshot.retryFailureCount()
            );

            return;
        }

        boolean listCompleted = state.isListCompleted();

        /*
         * 기존 DB에 이미 적재된 데이터를 처음 상태 테이블에 옮기는 경우에는
         * 목록 적재가 완료된 데이터로 간주한다.
         */
        if (createdFromExistingDatabase) {
            listCompleted = true;
        }

        boolean detailCompleted = listCompleted
                && snapshot.detailSyncedCount()
                >= snapshot.activeContentCount();

        boolean imageCompleted = detailCompleted
                && snapshot.imageSyncedCount()
                >= snapshot.activeContentCount()
                && snapshot.retryScheduledCount() == 0L
                && snapshot.retryFailureCount() == 0L;

        FestivalInitialSyncPhase phase = resolvePhase(
                listCompleted,
                detailCompleted,
                imageCompleted,
                snapshot.activeContentCount()
        );

        state.updateProgress(
                phase,
                listCompleted,
                detailCompleted,
                imageCompleted,
                snapshot.activeContentCount(),
                snapshot.detailSyncedCount(),
                snapshot.imageSyncedCount(),
                snapshot.retryScheduledCount(),
                snapshot.retryFailureCount()
        );
    }

    private FestivalInitialSyncPhase resolvePhase(
            boolean listCompleted,
            boolean detailCompleted,
            boolean imageCompleted,
            long activeContentCount
    ) {
        if (!listCompleted) {
            return activeContentCount == 0L
                    ? FestivalInitialSyncPhase.NOT_STARTED
                    : FestivalInitialSyncPhase.LIST_SYNCING;
        }

        if (!detailCompleted) {
            return FestivalInitialSyncPhase.DETAIL_SYNCING;
        }

        if (!imageCompleted) {
            return FestivalInitialSyncPhase.IMAGE_SYNCING;
        }

        return FestivalInitialSyncPhase.READY;
    }

    private SyncProgressSnapshot loadSnapshot() {
        return new SyncProgressSnapshot(
                festivalContentRepository.countByActiveTrue(),
                festivalContentRepository.countByActiveTrueAndDetailSourceUpdatedAtIsNotNull(),
                festivalContentRepository.countByActiveTrueAndImageSyncCompletedTrue(),
                festivalContentRepository
                        .countByActiveTrueAndNextDetailRetryAtIsNotNull(),
                festivalContentRepository
                        .countByActiveTrueAndLastDetailFailureReasonIsNotNull()
        );
    }

    private record SyncProgressSnapshot(
            long activeContentCount,
            long detailSyncedCount,
            long imageSyncedCount,
            long retryScheduledCount,
            long retryFailureCount
    ) {
    }
}