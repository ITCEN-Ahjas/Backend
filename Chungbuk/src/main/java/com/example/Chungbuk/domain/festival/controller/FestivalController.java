package com.example.Chungbuk.domain.festival.controller;

import com.example.Chungbuk.domain.festival.dto.response.ExperienceListResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalDetailResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalListResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalSyncResultResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalSyncStatusResponse;
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
@Tag(
        name = "Festival",
        description = "충북 축제·공연·행사·관광지·문화시설·레포츠 정보 API입니다."
)
public class FestivalController {

    private final FestivalService festivalService;
    private final FestivalSyncService festivalSyncService;

    @Operation(summary = "축제·공연·행사 목록 조회")
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

    @Operation(summary = "관광·체험 콘텐츠 목록 조회")
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

    @Operation(summary = "축제·체험 상세 조회")
    @GetMapping("/{contentId}")
    public FestivalDetailResponse getFestivalDetail(
            @PathVariable String contentId
    ) {
        return festivalService.getFestivalDetail(contentId);
    }

    @Operation(
            summary = "전체 콘텐츠 초기 적재",
            description = """
                    축제, 공연, 행사, 관광지, 문화시설, 레포츠 목록을 수집하고
                    상세 소개, 홈페이지, 유형별 주요 정보, 상세 이미지를 가능한 범위까지 저장합니다.

                    TourAPI 호출은 요청당 최대 900회로 제한됩니다.
                    호출 예산이 끝난 뒤 다시 실행하면 상세 미완료 콘텐츠를 이어서 처리합니다.
                    """
    )
    @PostMapping("/sync/bootstrap")
    public FestivalSyncResultResponse bootstrapFestivalContents(
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(defaultValue = "50") int maxPages,
            @RequestParam(defaultValue = "900") int maxApiCalls,
            @RequestParam(defaultValue = "true") boolean includeImages
    ) {
        return festivalSyncService.bootstrapFestivalContents(
                size,
                maxPages,
                maxApiCalls,
                includeImages
        );
    }

    @Operation(
            summary = "축제·체험 자동 갱신 실행",
            description = """
                    목록 데이터를 먼저 갱신한 뒤 신규, 수정, 상세 미완료 콘텐츠만 상세 API로 보강합니다.
                    레포츠 카드 정보는 장소 중심으로 처리하므로 카드 목적의 detailIntro2 반복 호출은 하지 않습니다.
                    """
    )
    @PostMapping("/sync/refresh")
    public FestivalSyncResultResponse refreshFestivalContents(
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(defaultValue = "50") int maxPages,
            @RequestParam(defaultValue = "900") int maxApiCalls,
            @RequestParam(defaultValue = "true") boolean includeImages
    ) {
        return festivalSyncService.refreshFestivalContents(
                size,
                maxPages,
                maxApiCalls,
                includeImages
        );
    }

    @Operation(
            summary = "콘텐츠 단건 상세 재동기화",
            description = "특정 콘텐츠의 상세 소개, 홈페이지, 유형별 주요 정보, 상세 이미지를 다시 저장합니다."
    )
    @PostMapping("/sync/{contentId}/detail")
    public FestivalSyncResultResponse syncFestivalContentDetail(
            @PathVariable String contentId,
            @RequestParam(defaultValue = "true") boolean includeImages
    ) {
        return festivalSyncService.syncFestivalContentDetail(
                contentId,
                includeImages
        );
    }

    @Operation(summary = "축제·체험 동기화 상태 조회")
    @GetMapping("/sync/status")
    public FestivalSyncStatusResponse getFestivalSyncStatus() {
        return festivalSyncService.getFestivalSyncStatus();
    }
}