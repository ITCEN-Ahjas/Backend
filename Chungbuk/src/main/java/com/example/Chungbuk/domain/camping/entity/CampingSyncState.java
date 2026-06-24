package com.example.Chungbuk.domain.camping.entity;

import com.example.Chungbuk.domain.camping.constant.CampingSyncExecutionStatus;
import com.example.Chungbuk.domain.camping.constant.CampingSyncPauseReason;
import com.example.Chungbuk.domain.camping.constant.CampingSyncPhase;
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
@Table(name = "camping_sync_state")
public class CampingSyncState {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_phase", nullable = false, length = 30)
    private CampingSyncPhase phase;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_status", nullable = false, length = 30)
    private CampingSyncExecutionStatus executionStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "pause_reason", nullable = false, length = 50)
    private CampingSyncPauseReason pauseReason;

    @Column(name = "saved_count", nullable = false)
    private long savedCount;

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

    public static CampingSyncState createInitial() {
        CampingSyncState state = new CampingSyncState();
        state.id = SINGLETON_ID;
        state.phase = CampingSyncPhase.NOT_STARTED;
        state.executionStatus = CampingSyncExecutionStatus.IDLE;
        state.pauseReason = CampingSyncPauseReason.NONE;
        state.savedCount = 0L;
        return state;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (phase == null) phase = CampingSyncPhase.NOT_STARTED;
        if (executionStatus == null) executionStatus = CampingSyncExecutionStatus.IDLE;
        if (pauseReason == null) pauseReason = CampingSyncPauseReason.NONE;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void markRunning() {
        this.phase = CampingSyncPhase.LIST_SYNCING;
        this.executionStatus = CampingSyncExecutionStatus.RUNNING;
        this.pauseReason = CampingSyncPauseReason.NONE;
        this.lastErrorMessage = null;
        this.lastStartedAt = LocalDateTime.now();
    }

    public void markWaiting(CampingSyncPauseReason reason, String message) {
        this.executionStatus = CampingSyncExecutionStatus.WAITING;
        this.pauseReason = reason;
        this.lastErrorMessage = truncate(message);
    }

    public void markFailed(String message) {
        this.executionStatus = CampingSyncExecutionStatus.FAILED;
        this.pauseReason = CampingSyncPauseReason.API_ERROR;
        this.lastErrorMessage = truncate(message);
    }

    public void markServerInterrupted(String message) {
        this.executionStatus = CampingSyncExecutionStatus.WAITING;
        this.pauseReason = CampingSyncPauseReason.SERVER_INTERRUPTED;
        this.lastErrorMessage = truncate(message);
    }

    public void completeSync(long savedCount) {
        this.phase = CampingSyncPhase.READY;
        this.executionStatus = CampingSyncExecutionStatus.IDLE;
        this.pauseReason = CampingSyncPauseReason.NONE;
        this.savedCount = savedCount;
        this.lastErrorMessage = null;
        this.lastCompletedAt = LocalDateTime.now();
    }

    public boolean isRunning() {
        return executionStatus == CampingSyncExecutionStatus.RUNNING;
    }

    private String truncate(String message) {
        if (message == null || message.isBlank()) return "";
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
