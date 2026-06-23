package com.example.Chungbuk.domain.accommodation.service;

import com.example.Chungbuk.domain.accommodation.constant.AccommodationSyncPhase;
import com.example.Chungbuk.domain.accommodation.dto.response.AccommodationInitializationResponse;
import com.example.Chungbuk.domain.accommodation.entity.AccommodationSyncState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccommodationInitialSyncService {

    private final AccommodationSyncStateService accommodationSyncStateService;
    private final AccommodationInitialSyncRunGuard accommodationInitialSyncRunGuard;
    private final AccommodationInitialSyncWorker accommodationInitialSyncWorker;

    public synchronized AccommodationInitializationResponse ensureInitialized() {
        AccommodationSyncState state = accommodationSyncStateService.getCurrentState();

        if (state.isRunning()) {
            if (accommodationInitialSyncRunGuard.isRunning()) {
                return toResponse(state, false, true, "숙소 배치 수집이 이미 진행 중입니다.");
            }

            state = accommodationSyncStateService.markServerInterrupted(
                    "서버 재시작 또는 비정상 종료로 배치 수집이 중단된 것으로 확인되었습니다."
            );
        }

        if (state.getPhase() == AccommodationSyncPhase.READY) {
            return toResponse(state, false, false, "배치 수집이 이미 완료되어 DB 데이터를 사용합니다.");
        }

        if (!accommodationInitialSyncRunGuard.tryStart()) {
            return toResponse(state, false, true, "숙소 배치 수집이 이미 진행 중입니다.");
        }

        AccommodationSyncPhase phase = resolveStartPhase(state);

        try {
            AccommodationSyncState runningState = accommodationSyncStateService.startPhase(phase);
            accommodationInitialSyncWorker.runInitialSync(phase);

            return toResponse(runningState, true, false, "숙소 배치 수집을 백그라운드에서 시작했습니다.");
        } catch (RuntimeException e) {
            accommodationInitialSyncRunGuard.finish();
            AccommodationSyncState failedState = accommodationSyncStateService.markFailed(
                    "배치 수집 시작 실패: " + e.getMessage()
            );
            return toResponse(failedState, false, false, "배치 수집 시작에 실패했습니다.");
        }
    }

    private AccommodationSyncPhase resolveStartPhase(AccommodationSyncState state) {
        if (state.getPhase() == AccommodationSyncPhase.DETAIL_SYNCING) {
            return AccommodationSyncPhase.DETAIL_SYNCING;
        }
        return AccommodationSyncPhase.LIST_SYNCING;
    }

    private AccommodationInitializationResponse toResponse(
            AccommodationSyncState state,
            boolean started,
            boolean alreadyRunning,
            String message
    ) {
        return AccommodationInitializationResponse.builder()
                .syncStarted(started)
                .syncAlreadyRunning(alreadyRunning)
                .ready(state.getPhase() == AccommodationSyncPhase.READY)
                .syncPhase(state.getPhase())
                .executionStatus(state.getExecutionStatus())
                .pauseReason(state.getPauseReason())
                .listSavedCount(state.getListSavedCount())
                .detailSyncedCount(state.getDetailSyncedCount())
                .lastStartedAt(state.getLastStartedAt())
                .lastCompletedAt(state.getLastCompletedAt())
                .message(message)
                .build();
    }
}
