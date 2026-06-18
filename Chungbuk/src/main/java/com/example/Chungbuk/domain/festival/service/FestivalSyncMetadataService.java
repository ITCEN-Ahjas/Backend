package com.example.Chungbuk.domain.festival.service;

import com.example.Chungbuk.domain.festival.dto.response.FestivalSyncMetadataInitializationResponse;
import com.example.Chungbuk.domain.festival.repository.FestivalContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FestivalSyncMetadataService {

    private final FestivalContentRepository festivalContentRepository;

    /*
     * 기존 854개 데이터를 기준으로 자동 refresh용 상태값만 채운다.
     *
     * TourAPI 목록·상세·이미지 API를 호출하지 않는다.
     * festival_contents 데이터를 새로 수집하거나 삭제하지 않는다.
     */
    @Transactional
    public FestivalSyncMetadataInitializationResponse
    initializeLegacySyncMetadata() {
        long targetCount = festivalContentRepository.countByActiveTrue();

        if (targetCount == 0) {
            return FestivalSyncMetadataInitializationResponse.builder()
                    .targetCount(0)
                    .detailBaselineInitializedCount(0)
                    .imageCompletedStateUpdatedCount(0)
                    .imagePendingStateUpdatedCount(0)
                    .detailBaselineReadyCount(0)
                    .imageCompletedCount(0)
                    .imagePendingCount(0)
                    .tourApiCallCount(0)
                    .message(
                            "활성 콘텐츠가 없어 동기화 상태값을 초기화하지 않았습니다."
                    )
                    .build();
        }

        int detailBaselineInitializedCount =
                festivalContentRepository
                        .initializeLegacyDetailSourceUpdatedAt();

        int imageCompletedStateUpdatedCount =
                festivalContentRepository
                        .initializeImageSyncCompletedTrue();

        int imagePendingStateUpdatedCount =
                festivalContentRepository
                        .initializeImageSyncCompletedFalse();

        long detailBaselineReadyCount = festivalContentRepository
                .countByActiveTrueAndDetailSourceUpdatedAtIsNotNull();

        long imageCompletedCount = festivalContentRepository
                .countByActiveTrueAndImageSyncCompletedTrue();

        long imagePendingCount = festivalContentRepository
                .countByActiveTrueAndImageSyncCompletedFalse();

        return FestivalSyncMetadataInitializationResponse.builder()
                .targetCount(targetCount)
                .detailBaselineInitializedCount(
                        detailBaselineInitializedCount
                )
                .imageCompletedStateUpdatedCount(
                        imageCompletedStateUpdatedCount
                )
                .imagePendingStateUpdatedCount(
                        imagePendingStateUpdatedCount
                )
                .detailBaselineReadyCount(detailBaselineReadyCount)
                .imageCompletedCount(imageCompletedCount)
                .imagePendingCount(imagePendingCount)
                .tourApiCallCount(0)
                .message(
                        "기존 DB 데이터를 기준으로 상세·이미지 자동 갱신 상태값을 "
                                + "초기화했습니다. TourAPI 호출은 발생하지 않았습니다."
                )
                .build();
    }
}