package com.example.Chungbuk.domain.accommodation.entity;

import com.example.Chungbuk.domain.accommodation.constant.AccommodationSyncExecutionStatus;
import com.example.Chungbuk.domain.accommodation.constant.AccommodationSyncPauseReason;
import com.example.Chungbuk.domain.accommodation.constant.AccommodationSyncPhase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
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
@Table(name = "accommodation_sync_state")
public class AccommodationSyncState {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_phase", nullable = false, length = 30)
    private AccommodationSyncPhase phase;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_status", nullable = false, length = 30)
    private AccommodationSyncExecutionStatus executionStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "pause_reason", nullable = false, length = 50)
    private AccommodationSyncPauseReason pauseReason;

    @Column(name = "list_saved_count", nullable = false)
    private long listSavedCount;

    @Column(name = "detail_synced_count", nullable = false)
    private long detailSyncedCount;

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

    public static AccommodationSyncState createInitial() {
        AccommodationSyncState state = new AccommodationSyncState();
        state.id = SINGLETON_ID;
        state.phase = AccommodationSyncPhase.NOT_STARTED;
        state.executionStatus = AccommodationSyncExecutionStatus.IDLE;
        state.pauseReason = AccommodationSyncPauseReason.NONE;
        state.listSavedCount = 0L;
        state.detailSyncedCount = 0L;
        return state;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (phase == null) {
            phase = AccommodationSyncPhase.NOT_STARTED;
        }
        if (executionStatus == null) {
            executionStatus = AccommodationSyncExecutionStatus.IDLE;
        }
        if (pauseReason == null) {
            pauseReason = AccommodationSyncPauseReason.NONE;
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void markRunning(AccommodationSyncPhase phase) {
        this.phase = phase;
        this.executionStatus = AccommodationSyncExecutionStatus.RUNNING;
        this.pauseReason = AccommodationSyncPauseReason.NONE;
        this.lastErrorMessage = null;
        this.lastStartedAt = LocalDateTime.now();
    }

    public void markWaiting(AccommodationSyncPauseReason reason, String message) {
        this.executionStatus = AccommodationSyncExecutionStatus.WAITING;
        this.pauseReason = reason;
        this.lastErrorMessage = truncate(message);
    }

    public void markFailed(String message) {
        this.executionStatus = AccommodationSyncExecutionStatus.FAILED;
        this.pauseReason = AccommodationSyncPauseReason.API_ERROR;
        this.lastErrorMessage = truncate(message);
    }

    public void markServerInterrupted(String message) {
        this.executionStatus = AccommodationSyncExecutionStatus.WAITING;
        this.pauseReason = AccommodationSyncPauseReason.SERVER_INTERRUPTED;
        this.lastErrorMessage = truncate(message);
    }

    public void completeListPhase(long listSavedCount) {
        this.phase = AccommodationSyncPhase.DETAIL_SYNCING;
        this.executionStatus = AccommodationSyncExecutionStatus.IDLE;
        this.pauseReason = AccommodationSyncPauseReason.NONE;
        this.listSavedCount = listSavedCount;
        this.lastErrorMessage = null;
    }

    public void completeAllSync(long detailSyncedCount) {
        this.phase = AccommodationSyncPhase.READY;
        this.executionStatus = AccommodationSyncExecutionStatus.IDLE;
        this.pauseReason = AccommodationSyncPauseReason.NONE;
        this.detailSyncedCount = detailSyncedCount;
        this.lastErrorMessage = null;
        this.lastCompletedAt = LocalDateTime.now();
    }

    public boolean isRunning() {
        return executionStatus == AccommodationSyncExecutionStatus.RUNNING;
    }

    private String truncate(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
