package com.example.Chungbuk.domain.camping.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CampingListResponse {

    private List<CampingSummaryResponse> items;
    private int page;
    private int size;
    private int totalCount;
}
