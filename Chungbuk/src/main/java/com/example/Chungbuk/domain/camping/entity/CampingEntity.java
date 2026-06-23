package com.example.Chungbuk.domain.camping.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "camping")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampingEntity {

    @Id
    @Column(name = "content_id")
    private String contentId;

    @Column(name = "faclt_nm", length = 300)
    private String facltNm;

    @Column(name = "line_intro", length = 500)
    private String lineIntro;

    @Lob
    @Column(name = "intro", columnDefinition = "TEXT")
    private String intro;

    @Column(name = "feature_nm", length = 500)
    private String featureNm;

    @Column(name = "induty", length = 200)
    private String induty;

    @Column(name = "lct_cl", length = 200)
    private String lctCl;

    @Column(name = "do_nm", length = 50)
    private String doNm;

    @Column(name = "sigungu_nm", length = 50)
    private String sigunguNm;

    @Column(name = "addr1", length = 500)
    private String addr1;

    @Column(name = "addr2", length = 300)
    private String addr2;

    @Column(name = "map_x", length = 50)
    private String mapX;

    @Column(name = "map_y", length = 50)
    private String mapY;

    @Column(name = "tel", length = 100)
    private String tel;

    @Column(name = "homepage", length = 1000)
    private String homepage;

    @Column(name = "resve_url", length = 1000)
    private String resveUrl;

    @Column(name = "resve_cl", length = 100)
    private String resveCl;

    @Column(name = "manage_sttus", length = 50)
    private String manageSttus;

    @Column(name = "gnrl_site_co")
    private Integer gnrlSiteCo;

    @Column(name = "auto_site_co")
    private Integer autoSiteCo;

    @Column(name = "glamp_site_co")
    private Integer glampSiteCo;

    @Column(name = "carav_site_co")
    private Integer caravSiteCo;

    @Column(name = "toilet", length = 100)
    private String toilet;

    @Column(name = "swrm_co", length = 100)
    private String swrmCo;

    @Column(name = "sbrs_cl", length = 500)
    private String sbrsCl;

    @Column(name = "posbl_fclty_cl", length = 500)
    private String posblFcltyCl;

    @Column(name = "thema_envrn_cl", length = 300)
    private String themaEnvrnCl;

    @Column(name = "animal_cmg_cl", length = 100)
    private String animalCmgCl;

    @Column(name = "first_image_url", length = 1000)
    private String firstImageUrl;
}
