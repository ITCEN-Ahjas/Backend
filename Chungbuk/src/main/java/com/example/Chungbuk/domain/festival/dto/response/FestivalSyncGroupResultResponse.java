package com.example.Chungbuk.domain.festival.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FestivalSyncGroupResultResponse {

    private String group;
    private String groupName;

    private String contentTypeId;
    private String category;

    private int targetCount;
    private int mainSyncedCount;
    private int detailSyncedCount;

    private int detailEmptyCount;
    private int skippedCount;
    private int failedCount;

    /*
     * 해당 그룹의 목록 API 호출 수
     *
     * 축제·공연·행사는 searchFestival2를 1회 목록 조회 후
     * 카테고리별로 나누므로, 공유 목록 호출 수는 FESTIVAL 그룹에 기록한다.
     */
    private int listApiCallCount;

    private int detailApiCallCount;

    private int tourApiCallCount;

    private boolean completed;
}