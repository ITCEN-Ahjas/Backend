package com.example.Chungbuk.domain.festival.dto.response;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FestivalSyncStatusResponse {

    /*
     * 기존 상태 API 응답 필드
     */
    private long totalCount;
    private long festivalCount;
    private long touristSpotCount;
    private long cultureCount;
    private long leportsCount;

    /*
     * 상세 정보가 존재하거나 상세 처리 이력이 있는 콘텐츠 수
     */
    private long detailSyncedCount;

    private LocalDateTime lastSyncedAt;
    private boolean empty;

    private Map<String, Long> contentTypeCounts;
    private Map<String, Long> categoryCounts;

    /*
     * 자동 갱신 상태 상세 지표
     */
    private long detailBaselineReadyCount;

    /*
     * imageSyncCompleted = true
     * 이미지가 실제로 있거나, 원본에 이미지가 없음을 정상 확인한 콘텐츠 수
     */
    private long imageSyncCompletedCount;

    /*
     * imageSyncCompleted = false
     * 이미지 동기화 처리가 아직 끝나지 않은 콘텐츠 수
     */
    private long imagePendingCount;

    /*
     * imageSyncCompleted 값이 아직 없는 콘텐츠 수
     */
    private long imageStateUnknownCount;

    /*
     * 대표 이미지 또는 상세 이미지 목록이 실제로 존재하는 콘텐츠 수
     */
    private long imageAvailableCount;

    /*
     * 대표 이미지와 상세 이미지 목록이 모두 없는 콘텐츠 수
     */
    private long imageUnavailableCount;

    /*
     * 상세·이미지 요청 실패 후 재시도 시각이 설정된 콘텐츠 수
     */
    private long retryScheduledCount;

    /*
     * 마지막 상세 동기화 실패 사유가 남아 있는 콘텐츠 수
     */
    private long retryFailureCount;
}