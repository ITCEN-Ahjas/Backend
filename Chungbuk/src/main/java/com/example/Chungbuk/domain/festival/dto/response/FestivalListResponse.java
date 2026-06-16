package com.example.Chungbuk.domain.festival.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FestivalListResponse {

    private List<FestivalSummaryResponse> items;
    private int page;
    private int size;
    private int totalCount;
}