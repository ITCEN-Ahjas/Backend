package com.example.Chungbuk.domain.camping.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CampingSummaryResponse {

    private String contentId;
    private String facltNm;
    private String lineIntro;
    private String induty;
    private String lctCl;
    private String sigunguNm;
    private String addr1;
    private String mapX;
    private String mapY;
    private String manageSttus;
    private String firstImageUrl;
}
