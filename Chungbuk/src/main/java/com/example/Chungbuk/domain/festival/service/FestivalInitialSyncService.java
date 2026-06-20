package com.example.Chungbuk.domain.festival.service;

import com.example.Chungbuk.domain.festival.constant.FestivalInitialSyncPhase;
import com.example.Chungbuk.domain.festival.dto.response.FestivalInitializationResponse;
import com.example.Chungbuk.domain.festival.entity.FestivalInitialSyncState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FestivalInitialSyncService {

    private final FestivalInitialSyncStateService
            festivalInitialSyncStateService;

    private final FestivalInitialSyncRunGuard festivalInitialSyncRunGuard;

    private final FestivalInitialSyncWorker festivalInitialSyncWorker;

    public synchronized FestivalInitializationResponse ensureInitialized() {
        FestivalInitialSyncState state =
                festivalInitialSyncStateService.getCurrentState();

        if (state.isRunning()) {
            if (festivalInitialSyncRunGuard.isRunning()) {
                return toResponse(
                        state,
                        false,
                        true,
                        "초기 적재가 이미 진행 중입니다."
                );
            }

            state = festivalInitialSyncStateService
                    .markServerInterrupted(
                            "서버 재시작 또는 비정상 종료로 "
                                    + "초기 적재가 중단된 것으로 확인되었습니다."
                    );
        }

        if (state.getPhase() == FestivalInitialSyncPhase.READY) {
            return toResponse(
                    state,
                    false,
                    false,
                    "초기 적재가 완료되어 기존 DB 데이터를 사용합니다."
            );
        }

        if (!festivalInitialSyncRunGuard.tryStart()) {
            return toResponse(
                    state,
                    false,
                    true,
                    "초기 적재가 이미 진행 중입니다."
            );
        }

        FestivalInitialSyncPhase phase = resolveResumePhase(state);

        try {
            FestivalInitialSyncState runningState =
                    festivalInitialSyncStateService.startPhase(phase);

            festivalInitialSyncWorker.runInitialSync(phase);

            return toResponse(
                    runningState,
                    true,
                    false,
                    buildStartedMessage(phase)
            );
        } catch (RuntimeException e) {
            festivalInitialSyncRunGuard.finish();

            FestivalInitialSyncState failedState =
                    festivalInitialSyncStateService.markFailed(
                            "초기 적재 비동기 작업 시작에 실패했습니다: "
                                    + safeMessage(e)
                    );

            return toResponse(
                    failedState,
                    false,
                    false,
                    "초기 적재 시작에 실패했습니다."
            );
        }
    }

    private FestivalInitialSyncPhase resolveResumePhase(
            FestivalInitialSyncState state
    ) {
        if (state.getPhase() == FestivalInitialSyncPhase.NOT_STARTED) {
            return FestivalInitialSyncPhase.LIST_SYNCING;
        }

        if (state.getPhase() == FestivalInitialSyncPhase.LIST_SYNCING) {
            return FestivalInitialSyncPhase.LIST_SYNCING;
        }

        if (state.getPhase() == FestivalInitialSyncPhase.DETAIL_SYNCING) {
            return FestivalInitialSyncPhase.DETAIL_SYNCING;
        }

        if (state.getPhase() == FestivalInitialSyncPhase.IMAGE_SYNCING) {
            return FestivalInitialSyncPhase.IMAGE_SYNCING;
        }

        return FestivalInitialSyncPhase.LIST_SYNCING;
    }

    private FestivalInitializationResponse toResponse(
            FestivalInitialSyncState state,
            boolean initializationStarted,
            boolean initializationAlreadyRunning,
            String message
    ) {
        return FestivalInitializationResponse.builder()
                .initializationStarted(initializationStarted)
                .initializationAlreadyRunning(
                        initializationAlreadyRunning
                )
                .ready(state.getPhase() == FestivalInitialSyncPhase.READY)
                .initialSyncPhase(state.getPhase())
                .initialSyncExecutionStatus(
                        state.getExecutionStatus()
                )
                .initialSyncPauseReason(state.getPauseReason())
                .initialListCompleted(state.isListCompleted())
                .initialDetailCompleted(state.isDetailCompleted())
                .initialImageCompleted(state.isImageCompleted())
                .activeContentCount(state.getActiveContentCount())
                .detailSyncedCount(state.getDetailSyncedCount())
                .imageSyncedCount(state.getImageSyncedCount())
                .lastStartedAt(state.getLastStartedAt())
                .lastCompletedAt(state.getLastCompletedAt())
                .message(message)
                .build();
    }

    private String buildStartedMessage(
            FestivalInitialSyncPhase phase
    ) {
        return switch (phase) {
            case LIST_SYNCING ->
                    "축제·체험 목록 초기 적재를 백그라운드에서 시작했습니다.";
            case DETAIL_SYNCING ->
                    "미완료 상세 정보 초기 적재를 백그라운드에서 시작했습니다.";
            case IMAGE_SYNCING ->
                    "미완료 이미지 확인 작업을 백그라운드에서 시작했습니다.";
            default ->
                    "축제·체험 초기 적재를 백그라운드에서 시작했습니다.";
        };
    }

    private String safeMessage(Exception exception) {
        if (exception.getMessage() == null
                || exception.getMessage().isBlank()) {
            return exception.getClass().getSimpleName();
        }

        return exception.getMessage();
    }
}