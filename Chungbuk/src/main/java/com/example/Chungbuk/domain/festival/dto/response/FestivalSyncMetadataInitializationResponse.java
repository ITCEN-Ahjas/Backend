package com.example.Chungbuk.domain.festival.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FestivalSyncMetadataInitializationResponse {

    /*
     * 초기화 대상인 활성 콘텐츠 전체 수
     */
    private long targetCount;

    /*
     * 이번 실행에서 detail_source_updated_at을 새로 저장한 수
     */
    private int detailBaselineInitializedCount;

    /*
     * 이번 실행에서 이미지 완료 상태(true)로 변경한 수
     */
    private int imageCompletedStateUpdatedCount;

    /*
     * 이번 실행에서 이미지 확인 필요 상태(false)로 변경한 수
     */
    private int imagePendingStateUpdatedCount;

    /*
     * 초기화 실행 후의 최종 상태 집계
     */
    private long detailBaselineReadyCount;
    private long imageCompletedCount;
    private long imagePendingCount;

    /*
     * 이 API는 DB 값만 사용하므로 항상 0이어야 한다.
     */
    private int tourApiCallCount;

    private String message;
}