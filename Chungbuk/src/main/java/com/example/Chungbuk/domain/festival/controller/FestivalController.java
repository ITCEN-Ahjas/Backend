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
@Tag(
        name = "Festival",
        description = "충북 축제/체험 정보 API입니다. 조회 API는 DB 데이터를 기준으로 응답하며, 동기화 API는 TourAPI 데이터를 DB에 저장/갱신합니다."
)
public class FestivalController {

    private final FestivalService festivalService;
    private final FestivalSyncService festivalSyncService;

    @Operation(
            summary = "축제 목록 조회",
            description = """
                    DB에 저장된 충북 행사/공연/축제 목록을 조회합니다.
                    프론트엔드 요청마다 TourAPI를 직접 호출하지 않고, 수동 동기화로 저장된 festival_contents 데이터를 기준으로 응답합니다.
                    지역, 카테고리, 키워드, 행사 시작 기준일 조건을 사용할 수 있습니다.
                    """
    )
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

    @Operation(
            summary = "체험/관광 콘텐츠 목록 조회",
            description = """
                    DB에 저장된 충북 체험·관광 콘텐츠 목록을 조회합니다.
                    관광지, 문화시설, 레포츠 데이터를 대상으로 하며, 지역, 콘텐츠 유형, 카테고리, 키워드 조건을 사용할 수 있습니다.
                    프론트엔드 API 주소와 응답 구조는 유지하되 내부 조회 방식은 DB 조회를 사용합니다.
                    """
    )
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

    @Operation(
            summary = "TourAPI 축제 목록 원본 응답 조회",
            description = """
                    TourAPI 원본 JSON 응답을 확인하기 위한 개발/디버깅용 API입니다.
                    일반 프론트엔드 조회 흐름에서는 사용하지 않습니다.
                    실제 화면 조회 API는 DB에 저장된 festival_contents 데이터를 기준으로 응답합니다.
                    """
    )
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
            summary = "축제/체험 목록 데이터 수동 동기화",
            description = """
                    TourAPI 목록 데이터를 festival_contents 테이블에 저장하거나 갱신합니다.
                    기본값은 목록 데이터 중심 동기화이며, TourAPI 트래픽 제한을 고려하여 상세 데이터는 단건 상세 동기화 API로 보강하는 것을 권장합니다.
                    한 번의 요청에서 축제 1페이지와 체험/관광 1페이지만 동기화합니다.
                    """
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
            summary = "축제/체험 목록 데이터 범위 동기화",
            description = """
                    TourAPI 목록 데이터를 여러 페이지 범위로 반복 조회하여 festival_contents 테이블에 저장하거나 갱신합니다.
                    프론트엔드 첫 렌더링에서 호출하는 API가 아니라, 관리자 또는 개발자가 DB 데이터를 채우기 위해 사용하는 백엔드 데이터 관리용 API입니다.
                    기본값은 목록 데이터 중심 동기화이며, TourAPI 호출량을 줄이기 위해 includeDetail=false 사용을 권장합니다.
                    상세 데이터는 필요한 contentId만 단건 상세 동기화 API로 보강하는 것을 권장합니다.
                    
                    예시:
                    POST /api/festivals/sync/bulk?festivalStartPage=1&festivalEndPage=3&festivalSize=30&experienceStartPage=1&experienceEndPage=3&experienceSize=30&eventStartDate=20230101&includeDetail=false
                    """
    )
    @PostMapping("/sync/bulk")
    public FestivalSyncResultResponse syncFestivalContentsBulk(
            @RequestParam(defaultValue = "1") int festivalStartPage,
            @RequestParam(defaultValue = "1") int festivalEndPage,
            @RequestParam(defaultValue = "30") int festivalSize,
            @RequestParam(defaultValue = "1") int experienceStartPage,
            @RequestParam(defaultValue = "1") int experienceEndPage,
            @RequestParam(defaultValue = "30") int experienceSize,
            @RequestParam(required = false) String eventStartDate,
            @RequestParam(defaultValue = "false") boolean includeDetail
    ) {
        return festivalSyncService.syncFestivalContentsBulk(
                festivalStartPage,
                festivalEndPage,
                festivalSize,
                experienceStartPage,
                experienceEndPage,
                experienceSize,
                eventStartDate,
                includeDetail
        );
    }

    @Operation(
            summary = "축제/체험 단건 상세 데이터 수동 동기화",
            description = """
                    contentId 기준으로 TourAPI 상세 데이터를 조회하여 festival_contents 테이블에 저장하거나 갱신합니다.
                    목록 동기화 후 description, imageUrls, mainInfo 등 상세 정보가 필요한 콘텐츠를 보강할 때 사용합니다.
                    """
    )
    @PostMapping("/sync/{contentId}/detail")
    public FestivalSyncResultResponse syncFestivalContentDetail(
            @PathVariable String contentId
    ) {
        return festivalSyncService.syncFestivalContentDetail(contentId);
    }

    @Operation(
            summary = "축제/체험 상세 조회",
            description = """
                    contentId로 DB에 저장된 축제/체험 상세 정보를 조회합니다.
                    TourAPI를 실시간 호출하지 않고, 수동 동기화로 저장된 festival_contents 데이터를 기준으로 응답합니다.
                    """
    )
    @GetMapping("/{contentId}")
    public FestivalDetailResponse getFestivalDetail(
            @PathVariable String contentId
    ) {
        return festivalService.getFestivalDetail(contentId);
    }
}