package com.example.Chungbuk.domain.accommodation.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AccommodationListResponse {

    private List<AccommodationSummaryResponse> items;
    private int page;
    private int size;
    private int totalCount;
}
