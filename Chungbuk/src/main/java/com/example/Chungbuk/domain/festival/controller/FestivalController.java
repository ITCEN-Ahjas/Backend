package com.example.Chungbuk.domain.festival.controller;

import com.example.Chungbuk.domain.festival.dto.response.ExperienceListResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalDetailResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalListResponse;
import com.example.Chungbuk.domain.festival.service.FestivalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/festivals")
@RequiredArgsConstructor
@Tag(name = "Festival", description = "충북 축제/체험 정보 API")
public class FestivalController {

    private final FestivalService festivalService;

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

    @Operation(summary = "축제 상세 조회", description = "contentId로 축제 상세 정보를 조회합니다.")
    @GetMapping("/{contentId}")
    public FestivalDetailResponse getFestivalDetail(
            @PathVariable String contentId
    ) {
        return festivalService.getFestivalDetail(contentId);
    }
}