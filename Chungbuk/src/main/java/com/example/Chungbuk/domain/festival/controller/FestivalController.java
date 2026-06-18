package com.example.Chungbuk.domain.festival.controller;

import com.example.Chungbuk.domain.festival.dto.response.ExperienceListResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalDetailResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalListResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalSyncResultResponse;
import com.example.Chungbuk.domain.festival.service.FestivalService;
import com.example.Chungbuk.domain.festival.service.FestivalSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/festivals")
@RequiredArgsConstructor
@Tag(name = "Festival", description = "충북 축제/체험 정보 API")
public class FestivalController {

    private final FestivalService festivalService;
    private final FestivalSyncService festivalSyncService;

    @Operation(summary = "축제 목록 조회", description = "기간, 지역, 카테고리, 키워드로 충북 축제 목록을 조회합니다.")
    @GetMapping
    public FestivalListResponse getFestivals(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String eventStartDate,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "전체") String category,
            @RequestParam(required = false) String keyword
    ) {
        return festivalService.getFestivals(
                page,
                size,
                eventStartDate,
                region,
                category,
                keyword
        );
    }

    @Operation(summary = "체험/관광 콘텐츠 목록 조회", description = "지역과 콘텐츠 유형, 카테고리, 키워드로 충북 체험·관광 콘텐츠 목록을 조회합니다.")
    @GetMapping("/experiences")
    public ExperienceListResponse getExperiences(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "전체") String contentTypeId,
            @RequestParam(defaultValue = "전체") String category,
            @RequestParam(required = false) String keyword
    ) {
        return festivalService.getExperiences(
                page,
                size,
                region,
                contentTypeId,
                category,
                keyword
        );
    }

    @Operation(summary = "축제 목록 원본 응답 조회", description = "TourAPI 원본 JSON 응답을 그대로 반환합니다.")
    @GetMapping("/raw")
    public String getFestivalRaw(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String eventStartDate,
            @RequestParam(required = false) String region
    ) {
        return festivalService.getFestivalRaw(
                page,
                size,
                eventStartDate,
                region
        );
    }

    @Operation(
            summary = "축제/체험 데이터 수동 동기화",
            description = "TourAPI 목록 데이터를 festival_contents 테이블에 저장하거나 갱신합니다. 기본값은 목록 데이터만 동기화합니다."
    )
    @PostMapping("/sync")
    public FestivalSyncResultResponse syncFestivalContents(
            @RequestParam(defaultValue = "1") int festivalPage,
            @RequestParam(defaultValue = "10") int festivalSize,
            @RequestParam(defaultValue = "1") int experiencePage,
            @RequestParam(defaultValue = "10") int experienceSize,
            @RequestParam(required = false) String eventStartDate,
            @RequestParam(defaultValue = "false") boolean includeDetail
    ) {
        return festivalSyncService.syncFestivalContents(
                festivalPage,
                festivalSize,
                experiencePage,
                experienceSize,
                eventStartDate,
                includeDetail
        );
    }

    @Operation(
            summary = "축제/체험 단건 상세 데이터 수동 동기화",
            description = "contentId 기준으로 TourAPI 상세 데이터를 조회하여 festival_contents 테이블에 저장하거나 갱신합니다."
    )
    @PostMapping("/sync/{contentId}/detail")
    public FestivalSyncResultResponse syncFestivalContentDetail(
            @PathVariable String contentId
    ) {
        return festivalSyncService.syncFestivalContentDetail(contentId);
    }

    @Operation(summary = "축제 상세 조회", description = "contentId로 축제 상세 정보를 조회합니다.")
    @GetMapping("/{contentId}")
    public FestivalDetailResponse getFestivalDetail(
            @PathVariable String contentId
    ) {
        return festivalService.getFestivalDetail(contentId);
    }
}