package com.example.Chungbuk.domain.festival.controller;

import com.example.Chungbuk.domain.festival.dto.response.ExperienceListResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalDetailResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalInitializationResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalListResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalSyncMetadataInitializationResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalSyncResultResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalSyncStatusResponse;
import com.example.Chungbuk.domain.festival.service.FestivalInitialSyncService;
import com.example.Chungbuk.domain.festival.service.FestivalService;
import com.example.Chungbuk.domain.festival.service.FestivalSyncMetadataService;
import com.example.Chungbuk.domain.festival.service.FestivalSyncService;
import com.example.Chungbuk.domain.festival.service.FestivalSyncStatusService;
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

    private final FestivalSyncMetadataService
            festivalSyncMetadataService;

    private final FestivalSyncStatusService
            festivalSyncStatusService;

    private final FestivalInitialSyncService
            festivalInitialSyncService;

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

    @Operation(
            summary = "축제·체험 상세 조회",
            description = "존재하지 않거나 비활성화된 콘텐츠는 HTTP 404를 반환합니다."
    )
    @GetMapping("/{contentId}")
    public FestivalDetailResponse getFestivalDetail(
            @PathVariable String contentId
    ) {
        return festivalService.getFestivalDetail(contentId);
    }

    @Operation(
            summary = "홈페이지 최초 진입 초기 적재 확인",
            description = """
                    DB가 비어 있거나 초기 적재가 중단된 경우에만
                    현재 단계부터 초기 적재 작업을 백그라운드에서 시작합니다.

                    초기 적재 진행 중이거나 완료된 경우에는 TourAPI를
                    재호출하지 않고 현재 상태만 반환합니다.
                    """
    )
    @PostMapping("/sync/ensure-initialized")
    public FestivalInitializationResponse ensureInitialized() {
        return festivalInitialSyncService.ensureInitialized();
    }

    @Operation(
            summary = "기존 콘텐츠 동기화 상태값 초기화",
            description = """
                    현재 DB에 저장된 상세 정보와 이미지 데이터를 기준으로
                    detail_source_updated_at, image_sync_completed 상태값을 초기화합니다.

                    TourAPI 목록·상세·이미지 API를 호출하지 않으며,
                    기존 festival_contents 데이터는 삭제하거나 새로 수집하지 않습니다.
                    """
    )
    @PostMapping("/sync/metadata/initialize")
    public FestivalSyncMetadataInitializationResponse
    initializeLegacySyncMetadata() {
        return festivalSyncMetadataService
                .initializeLegacySyncMetadata();
    }

    @Operation(
            summary = "전체 콘텐츠 초기 적재",
            description = """
                    축제, 공연, 행사, 관광지, 문화시설, 레포츠 목록을 수집하고
                    상세 소개, 홈페이지, 유형별 주요 정보, 상세 이미지를 가능한 범위까지 저장합니다.

                    요청별 호출 수와 당일 TourAPI 호출 예산을 함께 확인합니다.
                    호출 예산이 부족하면 남은 상세 작업은 다음 실행으로 보류됩니다.
                    """
    )
    @PostMapping("/sync/bootstrap")
    public FestivalSyncResultResponse bootstrapFestivalContents(
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(defaultValue = "50") int maxPages,
            @RequestParam(defaultValue = "900") int maxApiCalls,
            @RequestParam(defaultValue = "true") boolean includeDetail
    ) {
        return festivalSyncService.bootstrapFestivalContents(
                size,
                maxPages,
                maxApiCalls,
                includeDetail
        );
    }

    @Operation(
            summary = "축제·체험 자동 갱신 실행",
            description = """
                    목록 데이터를 먼저 갱신한 뒤 신규, 수정, 상세 미완료,
                    이미지 미완료 콘텐츠만 상세 API로 보강합니다.

                    이미지가 원본에 없는 콘텐츠는 정상 완료로 처리하며,
                    실제 API 오류·타임아웃만 재시도 대상으로 관리합니다.
                    """
    )
    @PostMapping("/sync/refresh")
    public FestivalSyncResultResponse refreshFestivalContents(
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(defaultValue = "50") int maxPages,
            @RequestParam(defaultValue = "900") int maxApiCalls,
            @RequestParam(defaultValue = "true") boolean includeDetail
    ) {
        return festivalSyncService.refreshFestivalContents(
                size,
                maxPages,
                maxApiCalls,
                includeDetail
        );
    }

    @Operation(
            summary = "콘텐츠 단건 상세 재동기화",
            description = """
                    특정 콘텐츠의 상세 소개, 홈페이지, 유형별 주요 정보,
                    상세 이미지를 다시 저장합니다.

                    단건 상세 재동기화는 항상 상세·이미지를 함께 처리합니다.
                    """
    )
    @PostMapping("/sync/{contentId}/detail")
    public FestivalSyncResultResponse syncFestivalContentDetail(
            @PathVariable String contentId
    ) {
        return festivalSyncService.syncFestivalContentDetail(contentId);
    }

    @Operation(
            summary = "축제·체험 동기화 상태 조회",
            description = """
                    전체 콘텐츠 수, 초기 적재 단계, 상세 처리 상태,
                    이미지 실제 보유 수, 이미지 없음 수, 재시도 대기 수를 조회합니다.
                    """
    )
    @GetMapping("/sync/status")
    public FestivalSyncStatusResponse getFestivalSyncStatus() {
        return festivalSyncStatusService.getFestivalSyncStatus();
    }
}