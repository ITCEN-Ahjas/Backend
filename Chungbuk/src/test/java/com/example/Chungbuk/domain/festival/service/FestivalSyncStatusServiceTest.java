package com.example.Chungbuk.domain.festival.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.Chungbuk.domain.festival.constant.FestivalInitialSyncPhase;
import com.example.Chungbuk.domain.festival.dto.response.FestivalSyncStatusResponse;
import com.example.Chungbuk.domain.festival.entity.FestivalInitialSyncState;
import com.example.Chungbuk.domain.festival.repository.FestivalContentRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FestivalSyncStatusServiceTest {

    @Mock
    private FestivalContentRepository festivalContentRepository;

    @Mock
    private FestivalInitialSyncStateService festivalInitialSyncStateService;

    @InjectMocks
    private FestivalSyncStatusService festivalSyncStatusService;

    @Test
    void returnsDetailedSyncStatusMetrics() {
        LocalDateTime lastSyncedAt = LocalDateTime.of(
                2026,
                6,
                20,
                17,
                44,
                16
        );

        FestivalInitialSyncState initialSyncState =
                FestivalInitialSyncState.createInitial();

        initialSyncState.updateStableProgress(
                FestivalInitialSyncPhase.READY,
                true,
                true,
                true,
                851L,
                851L,
                851L,
                0L,
                0L
        );

        when(festivalInitialSyncStateService.getCurrentState())
                .thenReturn(initialSyncState);

        when(festivalContentRepository.countByActiveTrue())
                .thenReturn(851L);

        when(festivalContentRepository
                .countByContentTypeIdAndActiveTrue("15"))
                .thenReturn(8L);

        when(festivalContentRepository
                .countByContentTypeIdAndActiveTrue("12"))
                .thenReturn(576L);

        when(festivalContentRepository
                .countByContentTypeIdAndActiveTrue("14"))
                .thenReturn(63L);

        when(festivalContentRepository
                .countByContentTypeIdAndActiveTrue("28"))
                .thenReturn(204L);

        when(festivalContentRepository.countDetailSyncedContents())
                .thenReturn(851L);

        when(festivalContentRepository.findLatestSyncedAt())
                .thenReturn(lastSyncedAt);

        when(festivalContentRepository
                .countByActiveTrueAndDetailSourceUpdatedAtIsNotNull())
                .thenReturn(851L);

        when(festivalContentRepository
                .countByActiveTrueAndImageSyncCompletedTrue())
                .thenReturn(851L);

        when(festivalContentRepository
                .countByActiveTrueAndImageSyncCompletedFalse())
                .thenReturn(0L);

        when(festivalContentRepository
                .countByActiveTrueAndImageSyncCompletedIsNull())
                .thenReturn(0L);

        when(festivalContentRepository
                .countActiveContentsWithImages())
                .thenReturn(815L);

        when(festivalContentRepository
                .countActiveContentsWithoutImages())
                .thenReturn(36L);

        when(festivalContentRepository
                .countByActiveTrueAndNextDetailRetryAtIsNotNull())
                .thenReturn(0L);

        when(festivalContentRepository
                .countByActiveTrueAndLastDetailFailureReasonIsNotNull())
                .thenReturn(0L);

        when(festivalContentRepository
                .countActiveContentsByContentTypeId())
                .thenReturn(List.of(
                        new Object[]{"12", 576L},
                        new Object[]{"14", 63L},
                        new Object[]{"15", 8L},
                        new Object[]{"28", 204L}
                ));

        when(festivalContentRepository
                .countActiveContentsByCategory())
                .thenReturn(List.of(
                        new Object[]{"관광지", 576L},
                        new Object[]{"문화시설", 63L},
                        new Object[]{"축제", 6L},
                        new Object[]{"행사", 2L},
                        new Object[]{"레포츠", 204L}
                ));

        FestivalSyncStatusResponse response =
                festivalSyncStatusService.getFestivalSyncStatus();

        assertThat(response.getTotalCount()).isEqualTo(851L);
        assertThat(response.getDetailSyncedCount()).isEqualTo(851L);
        assertThat(response.getDetailBaselineReadyCount()).isEqualTo(851L);

        assertThat(response.getImageSyncCompletedCount())
                .isEqualTo(851L);

        assertThat(response.getImageAvailableCount()).isEqualTo(815L);
        assertThat(response.getImageUnavailableCount()).isEqualTo(36L);

        assertThat(response.getImagePendingCount()).isZero();
        assertThat(response.getImageStateUnknownCount()).isZero();
        assertThat(response.getRetryScheduledCount()).isZero();
        assertThat(response.getRetryFailureCount()).isZero();

        assertThat(response.getInitialSyncPhase())
                .isEqualTo(FestivalInitialSyncPhase.READY);

        assertThat(response.isInitialListCompleted()).isTrue();
        assertThat(response.isInitialDetailCompleted()).isTrue();
        assertThat(response.isInitialImageCompleted()).isTrue();

        assertThat(response.isEmpty()).isFalse();
        assertThat(response.getLastSyncedAt()).isEqualTo(lastSyncedAt);
    }
}