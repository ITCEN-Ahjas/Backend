package com.example.Chungbuk.domain.festival.service;

import com.example.Chungbuk.domain.festival.entity.TourApiDailyUsage;
import com.example.Chungbuk.global.exception.TourApiQuotaExceededException;
import com.example.Chungbuk.domain.festival.repository.TourApiDailyUsageRepository;
import com.example.Chungbuk.global.config.TourApiProperties;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TourApiQuotaService {

    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final TourApiDailyUsageRepository tourApiDailyUsageRepository;
    private final TourApiProperties tourApiProperties;

    /*
     * 실제 TourAPI 요청 직전에 호출합니다.
     *
     * 요청 성공 여부와 관계없이 외부 API까지 요청이 나간다면
     * 호출량은 사용한 것으로 처리해야 합니다.
     */
    @Transactional
    public void reserveCallOrThrow() {
        boolean reserved = tryReserveCalls(1);

        if (!reserved) {
            QuotaSnapshot snapshot = getTodayQuotaSnapshot();

            throw new TourApiQuotaExceededException(
                    "오늘 TourAPI 호출 예산이 소진되었습니다. "
                            + "사용량="
                            + snapshot.usedCallCount()
                            + "/"
                            + snapshot.dailyCallLimit()
            );
        }
    }

    @Transactional
    public boolean tryReserveCalls(int callCount) {
        int normalizedCallCount = normalizeCallCount(callCount);

        if (normalizedCallCount == 0) {
            return true;
        }

        LocalDate today = getToday();
        int configuredDailyLimit = getConfiguredDailyCallLimit();

        TourApiDailyUsage usage = tourApiDailyUsageRepository
                .findByUsageDateForUpdate(today)
                .orElseGet(() -> tourApiDailyUsageRepository.saveAndFlush(
                        TourApiDailyUsage.create(
                                today,
                                configuredDailyLimit
                        )
                ));

        usage.updateDailyCallLimit(configuredDailyLimit);

        if (!usage.canReserve(normalizedCallCount)) {
            return false;
        }

        usage.reserve(normalizedCallCount);

        return true;
    }

    @Transactional(readOnly = true)
    public boolean hasAvailableCalls(int requiredCallCount) {
        int normalizedRequiredCallCount = normalizeCallCount(
                requiredCallCount
        );

        if (normalizedRequiredCallCount == 0) {
            return true;
        }

        LocalDate today = getToday();
        int configuredDailyLimit = getConfiguredDailyCallLimit();

        return tourApiDailyUsageRepository.findByUsageDate(today)
                .map(usage ->
                        usage.getRemainingCallCount()
                                >= normalizedRequiredCallCount
                )
                .orElse(configuredDailyLimit >= normalizedRequiredCallCount);
    }

    @Transactional(readOnly = true)
    public QuotaSnapshot getTodayQuotaSnapshot() {
        LocalDate today = getToday();
        int configuredDailyLimit = getConfiguredDailyCallLimit();

        return tourApiDailyUsageRepository.findByUsageDate(today)
                .map(usage -> new QuotaSnapshot(
                        usage.getUsageDate(),
                        usage.getDailyCallLimit(),
                        usage.getUsedCallCount(),
                        usage.getRemainingCallCount()
                ))
                .orElseGet(() -> new QuotaSnapshot(
                        today,
                        configuredDailyLimit,
                        0,
                        configuredDailyLimit
                ));
    }

    private LocalDate getToday() {
        return LocalDate.now(KOREA_ZONE_ID);
    }

    private int getConfiguredDailyCallLimit() {
        return Math.max(
                tourApiProperties.getDailyCallLimit(),
                1
        );
    }

    private int normalizeCallCount(int callCount) {
        return Math.max(callCount, 0);
    }

    public record QuotaSnapshot(
            LocalDate usageDate,
            int dailyCallLimit,
            int usedCallCount,
            int remainingCallCount
    ) {
    }
}