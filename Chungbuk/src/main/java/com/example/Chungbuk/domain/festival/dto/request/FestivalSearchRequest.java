package com.example.Chungbuk.domain.festival.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FestivalSearchRequest {

    private String region;
    private String category;
    private String keyword;
    private String status;
    private Integer page;
    private Integer size;
}