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

        if (state.isRunning() || state.isWaiting() || state.isFailed()) {
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

    public FestivalInitialSyncState completeListPhaseAndReconcile() {
        FestivalInitialSyncState state = getRequiredState();

        state.markListCompleted();

        reconcileStableState(
                state,
                loadSnapshot(),
                false
        );

        return festivalInitialSyncStateRepository.save(state);
    }

    public FestivalInitialSyncState completeCurrentPhaseAndReconcile() {
        FestivalInitialSyncState state = getRequiredState();

        reconcileStableState(
                state,
                loadSnapshot(),
                false
        );

        return festivalInitialSyncStateRepository.save(state);
    }

    public FestivalInitialSyncState waitForDailyQuota(
            String errorMessage
    ) {
        FestivalInitialSyncState state = getRequiredState();

        state.markWaiting(
                FestivalInitialSyncPauseReason.DAILY_QUOTA_EXCEEDED,
                errorMessage
        );

        return festivalInitialSyncStateRepository.save(state);
    }

    public FestivalInitialSyncState waitForTourApiError(
            String errorMessage
    ) {
        FestivalInitialSyncState state = getRequiredState();

        state.markWaiting(
                FestivalInitialSyncPauseReason.TOUR_API_ERROR,
                errorMessage
        );

        return festivalInitialSyncStateRepository.save(state);
    }

    public FestivalInitialSyncState markServerInterrupted(
            String errorMessage
    ) {
        FestivalInitialSyncState state = getRequiredState();

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
        FestivalInitialSyncState state = getRequiredState();

        state.markFailed(errorMessage);

        return festivalInitialSyncStateRepository.save(state);
    }

    private FestivalInitialSyncState getRequiredState() {
        return festivalInitialSyncStateRepository.findById(
                        FestivalInitialSyncState.SINGLETON_ID
                )
                .orElseGet(FestivalInitialSyncState::createInitial);
    }

    private void reconcileStableState(
            FestivalInitialSyncState state,
            SyncProgressSnapshot snapshot,
            boolean createdFromExistingDatabase
    ) {
        if (snapshot.activeContentCount() == 0L) {
            state.updateStableProgress(
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

        boolean detailCompleted = snapshot.detailSyncedCount()
                >= snapshot.activeContentCount();

        boolean imageCompleted = detailCompleted
                && snapshot.imageSyncedCount()
                >= snapshot.activeContentCount()
                && snapshot.retryScheduledCount() == 0L
                && snapshot.retryFailureCount() == 0L;

        /*
         * 상태 테이블이 없던 기존 완료 DB는 READY로 초기화한다.
         * 단순히 데이터가 일부 존재한다고 목록 완료로 처리하지 않는다.
         */
        boolean legacyCompletedDatabase = createdFromExistingDatabase
                && detailCompleted
                && imageCompleted;

        boolean listCompleted = state.isListCompleted()
                || legacyCompletedDatabase;

        FestivalInitialSyncPhase phase = resolvePhase(
                listCompleted,
                detailCompleted,
                imageCompleted
        );

        state.updateStableProgress(
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
            boolean imageCompleted
    ) {
        if (!listCompleted) {
            return FestivalInitialSyncPhase.LIST_SYNCING;
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
                festivalContentRepository
                        .countByActiveTrueAndDetailSourceUpdatedAtIsNotNull(),
                festivalContentRepository
                        .countByActiveTrueAndImageSyncCompletedTrue(),
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