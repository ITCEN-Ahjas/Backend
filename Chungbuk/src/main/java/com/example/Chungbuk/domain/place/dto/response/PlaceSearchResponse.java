package com.example.Chungbuk.domain.place.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PlaceSearchResponse {

    private List<PlaceSummaryResponse> items;
    private int size;
    private String nextPageToken;
}
