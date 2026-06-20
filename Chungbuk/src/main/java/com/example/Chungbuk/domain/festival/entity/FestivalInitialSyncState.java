package com.example.Chungbuk.domain.festival.entity;

import com.example.Chungbuk.domain.festival.constant.FestivalInitialSyncExecutionStatus;
import com.example.Chungbuk.domain.festival.constant.FestivalInitialSyncPauseReason;
import com.example.Chungbuk.domain.festival.constant.FestivalInitialSyncPhase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "festival_initial_sync_states",
        indexes = {
                @Index(
                        name = "idx_festival_initial_sync_phase",
                        columnList = "sync_phase"
                ),
                @Index(
                        name = "idx_festival_initial_sync_status",
                        columnList = "execution_status"
                )
        }
)
public class FestivalInitialSyncState {

    public static final long SINGLETON_ID = 1L;

    @Id
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_phase", nullable = false, length = 30)
    private FestivalInitialSyncPhase phase;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_status", nullable = false, length = 30)
    private FestivalInitialSyncExecutionStatus executionStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "pause_reason", nullable = false, length = 50)
    private FestivalInitialSyncPauseReason pauseReason;

    @Column(name = "list_completed", nullable = false)
    private boolean listCompleted;

    @Column(name = "detail_completed", nullable = false)
    private boolean detailCompleted;

    @Column(name = "image_completed", nullable = false)
    private boolean imageCompleted;

    @Column(name = "active_content_count", nullable = false)
    private long activeContentCount;

    @Column(name = "detail_synced_count", nullable = false)
    private long detailSyncedCount;

    @Column(name = "image_synced_count", nullable = false)
    private long imageSyncedCount;

    @Column(name = "retry_scheduled_count", nullable = false)
    private long retryScheduledCount;

    @Column(name = "retry_failure_count", nullable = false)
    private long retryFailureCount;

    @Column(name = "last_started_at")
    private LocalDateTime lastStartedAt;

    @Column(name = "last_completed_at")
    private LocalDateTime lastCompletedAt;

    @Column(name = "last_error_message", length = 1000)
    private String lastErrorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public static FestivalInitialSyncState createInitial() {
        FestivalInitialSyncState state = new FestivalInitialSyncState();

        state.id = SINGLETON_ID;
        state.phase = FestivalInitialSyncPhase.NOT_STARTED;
        state.executionStatus = FestivalInitialSyncExecutionStatus.IDLE;
        state.pauseReason = FestivalInitialSyncPauseReason.NONE;

        state.listCompleted = false;
        state.detailCompleted = false;
        state.imageCompleted = false;

        state.activeContentCount = 0L;
        state.detailSyncedCount = 0L;
        state.imageSyncedCount = 0L;
        state.retryScheduledCount = 0L;
        state.retryFailureCount = 0L;

        return state;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (phase == null) {
            phase = FestivalInitialSyncPhase.NOT_STARTED;
        }

        if (executionStatus == null) {
            executionStatus = FestivalInitialSyncExecutionStatus.IDLE;
        }

        if (pauseReason == null) {
            pauseReason = FestivalInitialSyncPauseReason.NONE;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void updateStableProgress(
            FestivalInitialSyncPhase phase,
            boolean listCompleted,
            boolean detailCompleted,
            boolean imageCompleted,
            long activeContentCount,
            long detailSyncedCount,
            long imageSyncedCount,
            long retryScheduledCount,
            long retryFailureCount
    ) {
        this.phase = phase;
        this.executionStatus = FestivalInitialSyncExecutionStatus.IDLE;
        this.pauseReason = FestivalInitialSyncPauseReason.NONE;

        this.listCompleted = listCompleted;
        this.detailCompleted = detailCompleted;
        this.imageCompleted = imageCompleted;

        this.activeContentCount = activeContentCount;
        this.detailSyncedCount = detailSyncedCount;
        this.imageSyncedCount = imageSyncedCount;
        this.retryScheduledCount = retryScheduledCount;
        this.retryFailureCount = retryFailureCount;

        this.lastErrorMessage = null;

        if (phase == FestivalInitialSyncPhase.READY) {
            this.lastCompletedAt = LocalDateTime.now();
        }
    }

    public void updateMetrics(
            long activeContentCount,
            long detailSyncedCount,
            long imageSyncedCount,
            long retryScheduledCount,
            long retryFailureCount
    ) {
        this.activeContentCount = activeContentCount;
        this.detailSyncedCount = detailSyncedCount;
        this.imageSyncedCount = imageSyncedCount;
        this.retryScheduledCount = retryScheduledCount;
        this.retryFailureCount = retryFailureCount;
    }

    public void markRunning(FestivalInitialSyncPhase phase) {
        this.phase = phase;
        this.executionStatus = FestivalInitialSyncExecutionStatus.RUNNING;
        this.pauseReason = FestivalInitialSyncPauseReason.NONE;
        this.lastErrorMessage = null;
        this.lastStartedAt = LocalDateTime.now();
    }

    public void markWaiting(
            FestivalInitialSyncPauseReason pauseReason,
            String errorMessage
    ) {
        this.executionStatus = FestivalInitialSyncExecutionStatus.WAITING;
        this.pauseReason = pauseReason;
        this.lastErrorMessage = normalizeMessage(errorMessage);
    }

    public void markFailed(String errorMessage) {
        this.executionStatus = FestivalInitialSyncExecutionStatus.FAILED;
        this.pauseReason = FestivalInitialSyncPauseReason.TOUR_API_ERROR;
        this.lastErrorMessage = normalizeMessage(errorMessage);
    }

    public void markListCompleted() {
        this.listCompleted = true;
        this.detailCompleted = false;
        this.imageCompleted = false;
        this.phase = FestivalInitialSyncPhase.DETAIL_SYNCING;
        this.executionStatus = FestivalInitialSyncExecutionStatus.IDLE;
        this.pauseReason = FestivalInitialSyncPauseReason.NONE;
        this.lastErrorMessage = null;
    }

    public boolean isRunning() {
        return executionStatus == FestivalInitialSyncExecutionStatus.RUNNING;
    }

    public boolean isWaiting() {
        return executionStatus == FestivalInitialSyncExecutionStatus.WAITING;
    }

    public boolean isFailed() {
        return executionStatus == FestivalInitialSyncExecutionStatus.FAILED;
    }

    private String normalizeMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return "";
        }

        return errorMessage.length() > 1000
                ? errorMessage.substring(0, 1000)
                : errorMessage;
    }
}