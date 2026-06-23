package com.example.Chungbuk.domain.accommodation.service;

import com.example.Chungbuk.domain.accommodation.constant.AccommodationSyncPauseReason;
import com.example.Chungbuk.domain.accommodation.constant.AccommodationSyncPhase;
import com.example.Chungbuk.domain.accommodation.dto.response.AccommodationSyncStatusResponse;
import com.example.Chungbuk.domain.accommodation.entity.AccommodationSyncState;
import com.example.Chungbuk.domain.accommodation.repository.AccommodationSyncStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccommodationSyncStateService {

    private final AccommodationSyncStateRepository accommodationSyncStateRepository;

    @Transactional
    public AccommodationSyncState getCurrentState() {
        return accommodationSyncStateRepository.findById(AccommodationSyncState.SINGLETON_ID)
                .orElseGet(() -> accommodationSyncStateRepository.save(
                        AccommodationSyncState.createInitial()
                ));
    }

    @Transactional
    public AccommodationSyncState startPhase(AccommodationSyncPhase phase) {
        AccommodationSyncState state = getCurrentState();
        state.markRunning(phase);
        return accommodationSyncStateRepository.save(state);
    }

    @Transactional
    public AccommodationSyncState markWaiting(AccommodationSyncPauseReason reason, String message) {
        AccommodationSyncState state = getCurrentState();
        state.markWaiting(reason, message);
        return accommodationSyncStateRepository.save(state);
    }

    @Transactional
    public AccommodationSyncState markFailed(String message) {
        AccommodationSyncState state = getCurrentState();
        state.markFailed(message);
        return accommodationSyncStateRepository.save(state);
    }

    @Transactional
    public AccommodationSyncState markServerInterrupted(String message) {
        AccommodationSyncState state = getCurrentState();
        state.markServerInterrupted(message);
        return accommodationSyncStateRepository.save(state);
    }

    @Transactional
    public AccommodationSyncState completeListPhase(long listSavedCount) {
        AccommodationSyncState state = getCurrentState();
        state.completeListPhase(listSavedCount);
        return accommodationSyncStateRepository.save(state);
    }

    @Transactional
    public AccommodationSyncState completeAllSync(long detailSyncedCount) {
        AccommodationSyncState state = getCurrentState();
        state.completeAllSync(detailSyncedCount);
        return accommodationSyncStateRepository.save(state);
    }

    public AccommodationSyncStatusResponse getSyncStatus() {
        AccommodationSyncState state = getCurrentState();
        return AccommodationSyncStatusResponse.builder()
                .syncPhase(state.getPhase())
                .executionStatus(state.getExecutionStatus())
                .pauseReason(state.getPauseReason())
                .listSavedCount(state.getListSavedCount())
                .detailSyncedCount(state.getDetailSyncedCount())
                .lastStartedAt(state.getLastStartedAt())
                .lastCompletedAt(state.getLastCompletedAt())
                .lastErrorMessage(state.getLastErrorMessage())
                .build();
    }
}
