package com.example.Chungbuk.domain.camping.service;

import com.example.Chungbuk.domain.camping.constant.CampingSyncPauseReason;
import com.example.Chungbuk.domain.camping.dto.response.CampingSyncStatusResponse;
import com.example.Chungbuk.domain.camping.entity.CampingSyncState;
import com.example.Chungbuk.domain.camping.repository.CampingSyncStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CampingSyncStateService {

    private final CampingSyncStateRepository campingSyncStateRepository;

    @Transactional
    public CampingSyncState getCurrentState() {
        return campingSyncStateRepository.findById(CampingSyncState.SINGLETON_ID)
                .orElseGet(() -> campingSyncStateRepository.save(
                        CampingSyncState.createInitial()
                ));
    }

    @Transactional
    public CampingSyncState startSync() {
        CampingSyncState state = getCurrentState();
        state.markRunning();
        return campingSyncStateRepository.save(state);
    }

    @Transactional
    public CampingSyncState markWaiting(CampingSyncPauseReason reason, String message) {
        CampingSyncState state = getCurrentState();
        state.markWaiting(reason, message);
        return campingSyncStateRepository.save(state);
    }

    @Transactional
    public CampingSyncState markFailed(String message) {
        CampingSyncState state = getCurrentState();
        state.markFailed(message);
        return campingSyncStateRepository.save(state);
    }

    @Transactional
    public CampingSyncState markServerInterrupted(String message) {
        CampingSyncState state = getCurrentState();
        state.markServerInterrupted(message);
        return campingSyncStateRepository.save(state);
    }

    @Transactional
    public CampingSyncState completeSync(long savedCount) {
        CampingSyncState state = getCurrentState();
        state.completeSync(savedCount);
        return campingSyncStateRepository.save(state);
    }

    public CampingSyncStatusResponse getSyncStatus() {
        CampingSyncState state = getCurrentState();
        return CampingSyncStatusResponse.builder()
                .syncPhase(state.getPhase())
                .executionStatus(state.getExecutionStatus())
                .pauseReason(state.getPauseReason())
                .savedCount(state.getSavedCount())
                .lastStartedAt(state.getLastStartedAt())
                .lastCompletedAt(state.getLastCompletedAt())
                .lastErrorMessage(state.getLastErrorMessage())
                .build();
    }
}
