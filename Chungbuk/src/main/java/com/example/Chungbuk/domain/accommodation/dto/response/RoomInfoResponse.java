package com.example.Chungbuk.domain.accommodation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RoomInfoResponse {

    private String roomTitle;
    private String roomSize;
    private String roomCount;
    private String baseCount;
    private String maxCount;
    private String offSeasonMinFee;
    private String offSeasonMaxFee;
    private String peakSeasonMinFee;
    private String peakSeasonMaxFee;
    private String roomImageUrl;
    private String bathFacility;
    private String internet;
    private String airCondition;
}
