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

    private long totalCount;

    private long festivalCount;
    private long touristSpotCount;
    private long cultureCount;
    private long leportsCount;

    private long detailSyncedCount;

    private LocalDateTime lastSyncedAt;

    private boolean empty;

    private Map<String, Long> contentTypeCounts;
    private Map<String, Long> categoryCounts;
}