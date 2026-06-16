package com.example.Chungbuk.domain.festival.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ExperienceListResponse {

    private List<ExperienceSummaryResponse> items;
    private int page;
    private int size;
    private int totalCount;
}