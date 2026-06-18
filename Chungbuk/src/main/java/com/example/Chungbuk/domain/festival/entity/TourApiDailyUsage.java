package com.example.Chungbuk.domain.festival.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "tour_api_daily_usages",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_tour_api_daily_usage_date",
                        columnNames = "usage_date"
                )
        },
        indexes = {
                @Index(
                        name = "idx_tour_api_daily_usage_date",
                        columnList = "usage_date"
                )
        }
)
public class TourApiDailyUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usage_date", nullable = false, unique = true)
    private LocalDate usageDate;

    @Column(name = "daily_call_limit", nullable = false)
    private int dailyCallLimit;

    @Column(name = "used_call_count", nullable = false)
    private int usedCallCount;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static TourApiDailyUsage create(
            LocalDate usageDate,
            int dailyCallLimit
    ) {
        return TourApiDailyUsage.builder()
                .usageDate(usageDate)
                .dailyCallLimit(normalizeDailyCallLimit(dailyCallLimit))
                .usedCallCount(0)
                .build();
    }

    public boolean canReserve(int callCount) {
        int normalizedCallCount = normalizeCallCount(callCount);

        return usedCallCount + normalizedCallCount <= dailyCallLimit;
    }

    public void reserve(int callCount) {
        int normalizedCallCount = normalizeCallCount(callCount);

        if (!canReserve(normalizedCallCount)) {
            throw new IllegalStateException(
                    "TourAPI 일일 호출 예산을 초과했습니다."
            );
        }

        usedCallCount += normalizedCallCount;
    }

    public int getRemainingCallCount() {
        return Math.max(dailyCallLimit - usedCallCount, 0);
    }

    public void updateDailyCallLimit(int configuredDailyCallLimit) {
        int normalizedDailyCallLimit = normalizeDailyCallLimit(
                configuredDailyCallLimit
        );

        /*
         * 이미 사용한 호출 수보다 작게 제한을 변경하면
         * 음수 잔여 호출량이 생길 수 있으므로 현재 사용량 이상으로 유지합니다.
         */
        this.dailyCallLimit = Math.max(
                normalizedDailyCallLimit,
                usedCallCount
        );
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (dailyCallLimit < 1) {
            dailyCallLimit = 1;
        }

        if (usedCallCount < 0) {
            usedCallCount = 0;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private static int normalizeDailyCallLimit(int dailyCallLimit) {
        return Math.max(dailyCallLimit, 1);
    }

    private int normalizeCallCount(int callCount) {
        return Math.max(callCount, 0);
    }
}