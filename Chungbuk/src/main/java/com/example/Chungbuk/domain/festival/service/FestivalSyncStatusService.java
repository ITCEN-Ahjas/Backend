package com.example.Chungbuk.domain.festival.service;

import com.example.Chungbuk.domain.festival.dto.response.FestivalSyncStatusResponse;
import com.example.Chungbuk.domain.festival.entity.FestivalInitialSyncState;
import com.example.Chungbuk.domain.festival.repository.FestivalContentRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FestivalSyncStatusService {

    private final FestivalContentRepository festivalContentRepository;

    private final FestivalInitialSyncStateService
            festivalInitialSyncStateService;

    @Transactional
    public FestivalSyncStatusResponse getFestivalSyncStatus() {
        FestivalInitialSyncState initialSyncState =
                festivalInitialSyncStateService.getCurrentState();

        long totalCount = festivalContentRepository.countByActiveTrue();

        return FestivalSyncStatusResponse.builder()
                .totalCount(totalCount)
                .festivalCount(
                        festivalContentRepository
                                .countByContentTypeIdAndActiveTrue("15")
                )
                .touristSpotCount(
                        festivalContentRepository
                                .countByContentTypeIdAndActiveTrue("12")
                )
                .cultureCount(
                        festivalContentRepository
                                .countByContentTypeIdAndActiveTrue("14")
                )
                .leportsCount(
                        festivalContentRepository
                                .countByContentTypeIdAndActiveTrue("28")
                )
                .detailSyncedCount(
                        festivalContentRepository
                                .countDetailSyncedContents()
                )
                .lastSyncedAt(
                        festivalContentRepository.findLatestSyncedAt()
                )
                .empty(totalCount == 0L)
                .contentTypeCounts(toCountMap(
                        festivalContentRepository
                                .countActiveContentsByContentTypeId()
                ))
                .categoryCounts(toCountMap(
                        festivalContentRepository
                                .countActiveContentsByCategory()
                ))
                .detailBaselineReadyCount(
                        festivalContentRepository
                                .countByActiveTrueAndDetailSourceUpdatedAtIsNotNull()
                )
                .imageSyncCompletedCount(
                        festivalContentRepository
                                .countByActiveTrueAndImageSyncCompletedTrue()
                )
                .imagePendingCount(
                        festivalContentRepository
                                .countByActiveTrueAndImageSyncCompletedFalse()
                )
                .imageStateUnknownCount(
                        festivalContentRepository
                                .countByActiveTrueAndImageSyncCompletedIsNull()
                )
                .imageAvailableCount(
                        festivalContentRepository
                                .countActiveContentsWithImages()
                )
                .imageUnavailableCount(
                        festivalContentRepository
                                .countActiveContentsWithoutImages()
                )
                .retryScheduledCount(
                        festivalContentRepository
                                .countByActiveTrueAndNextDetailRetryAtIsNotNull()
                )
                .retryFailureCount(
                        festivalContentRepository
                                .countByActiveTrueAndLastDetailFailureReasonIsNotNull()
                )
                .initialSyncPhase(initialSyncState.getPhase())
                .initialSyncExecutionStatus(
                        initialSyncState.getExecutionStatus()
                )
                .initialSyncPauseReason(
                        initialSyncState.getPauseReason()
                )
                .initialListCompleted(
                        initialSyncState.isListCompleted()
                )
                .initialDetailCompleted(
                        initialSyncState.isDetailCompleted()
                )
                .initialImageCompleted(
                        initialSyncState.isImageCompleted()
                )
                .initialSyncLastStartedAt(
                        initialSyncState.getLastStartedAt()
                )
                .initialSyncLastCompletedAt(
                        initialSyncState.getLastCompletedAt()
                )
                .initialSyncLastErrorMessage(
                        initialSyncState.getLastErrorMessage()
                )
                .build();
    }

    private Map<String, Long> toCountMap(List<Object[]> rows) {
        Map<String, Long> result = new LinkedHashMap<>();

        if (rows == null) {
            return result;
        }

        for (Object[] row : rows) {
            if (row == null || row.length < 2) {
                continue;
            }

            String key = row[0] == null
                    ? "UNKNOWN"
                    : String.valueOf(row[0]).trim();

            if (key.isBlank()) {
                key = "UNKNOWN";
            }

            long count = row[1] instanceof Number number
                    ? number.longValue()
                    : 0L;

            result.put(key, count);
        }

        return result;
    }
}