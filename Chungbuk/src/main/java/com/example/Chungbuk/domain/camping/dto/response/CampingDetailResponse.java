package com.example.Chungbuk.domain.camping.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CampingDetailResponse {

    private String contentId;
    private String facltNm;
    private String lineIntro;
    private String intro;
    private String featureNm;
    private String induty;
    private String lctCl;
    private String doNm;
    private String sigunguNm;
    private String addr1;
    private String addr2;
    private String mapX;
    private String mapY;
    private String tel;
    private String homepage;
    private String resveUrl;
    private String resveCl;
    private String manageSttus;
    private Integer gnrlSiteCo;
    private Integer autoSiteCo;
    private Integer glampSiteCo;
    private Integer caravSiteCo;
    private String toilet;
    private String swrmCo;
    private String sbrsCl;
    private String posblFcltyCl;
    private String themaEnvrnCl;
    private String animalCmgCl;
    private String firstImageUrl;
}
