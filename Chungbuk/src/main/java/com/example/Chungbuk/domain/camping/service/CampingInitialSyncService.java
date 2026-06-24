package com.example.Chungbuk.domain.camping.service;

import com.example.Chungbuk.domain.camping.constant.CampingSyncPhase;
import com.example.Chungbuk.domain.camping.dto.response.CampingInitializationResponse;
import com.example.Chungbuk.domain.camping.entity.CampingSyncState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CampingInitialSyncService {

    private final CampingSyncStateService campingSyncStateService;
    private final CampingInitialSyncRunGuard campingInitialSyncRunGuard;
    private final CampingInitialSyncWorker campingInitialSyncWorker;

    public synchronized CampingInitializationResponse ensureInitialized() {
        CampingSyncState state = campingSyncStateService.getCurrentState();

        if (state.isRunning()) {
            if (campingInitialSyncRunGuard.isRunning()) {
                return toResponse(state, false, true, "캠핑장 배치 수집이 이미 진행 중입니다.");
            }

            state = campingSyncStateService.markServerInterrupted(
                    "서버 재시작 또는 비정상 종료로 배치 수집이 중단된 것으로 확인되었습니다."
            );
        }

        if (state.getPhase() == CampingSyncPhase.READY) {
            return toResponse(state, false, false, "배치 수집이 이미 완료되어 DB 데이터를 사용합니다.");
        }

        if (!campingInitialSyncRunGuard.tryStart()) {
            return toResponse(state, false, true, "캠핑장 배치 수집이 이미 진행 중입니다.");
        }

        try {
            CampingSyncState runningState = campingSyncStateService.startSync();
            campingInitialSyncWorker.runSync();

            return toResponse(runningState, true, false, "캠핑장 배치 수집을 백그라운드에서 시작했습니다.");
        } catch (RuntimeException e) {
            campingInitialSyncRunGuard.finish();
            CampingSyncState failedState = campingSyncStateService.markFailed(
                    "배치 수집 시작 실패: " + e.getMessage()
            );
            return toResponse(failedState, false, false, "배치 수집 시작에 실패했습니다.");
        }
    }

    private CampingInitializationResponse toResponse(
            CampingSyncState state,
            boolean started,
            boolean alreadyRunning,
            String message
    ) {
        return CampingInitializationResponse.builder()
                .syncStarted(started)
                .syncAlreadyRunning(alreadyRunning)
                .ready(state.getPhase() == CampingSyncPhase.READY)
                .syncPhase(state.getPhase())
                .executionStatus(state.getExecutionStatus())
                .pauseReason(state.getPauseReason())
                .savedCount(state.getSavedCount())
                .lastStartedAt(state.getLastStartedAt())
                .lastCompletedAt(state.getLastCompletedAt())
                .message(message)
                .build();
    }
}
