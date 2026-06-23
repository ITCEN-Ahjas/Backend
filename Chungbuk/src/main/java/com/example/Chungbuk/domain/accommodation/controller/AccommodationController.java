package com.example.Chungbuk.domain.accommodation.controller;

import com.example.Chungbuk.domain.accommodation.dto.response.AccommodationDetailResponse;
import com.example.Chungbuk.domain.accommodation.dto.response.AccommodationInitializationResponse;
import com.example.Chungbuk.domain.accommodation.dto.response.AccommodationListResponse;
import com.example.Chungbuk.domain.accommodation.dto.response.AccommodationSyncStatusResponse;
import com.example.Chungbuk.domain.accommodation.service.AccommodationInitialSyncService;
import com.example.Chungbuk.domain.accommodation.service.AccommodationService;
import com.example.Chungbuk.domain.accommodation.service.AccommodationSyncStateService;
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
@RequestMapping("/api/accommodations")
@RequiredArgsConstructor
@Tag(name = "Accommodation", description = "충북 숙박시설 정보 API")
public class AccommodationController {

    private final AccommodationService accommodationService;
    private final AccommodationInitialSyncService accommodationInitialSyncService;
    private final AccommodationSyncStateService accommodationSyncStateService;

    @Operation(summary = "숙박시설 목록 조회", description = "지역, 카테고리, 키워드로 충북 숙박시설 목록을 조회합니다.")
    @GetMapping
    public AccommodationListResponse getAccommodations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "전체") String category,
            @RequestParam(required = false) String keyword
    ) {
        return accommodationService.getAccommodations(page, size, region, category, keyword);
    }

    @Operation(summary = "숙박시설 목록 원본 응답 조회", description = "TourAPI 원본 JSON 응답을 그대로 반환합니다.")
    @GetMapping("/raw")
    public String getAccommodationRaw(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String region
    ) {
        return accommodationService.getAccommodationRaw(page, size, region);
    }

    @Operation(summary = "숙박시설 상세 조회", description = "contentId로 숙박시설 상세 정보와 객실 정보를 조회합니다.")
    @GetMapping("/{contentId}")
    public AccommodationDetailResponse getAccommodationDetail(
            @PathVariable String contentId
    ) {
        return accommodationService.getAccommodationDetail(contentId);
    }

    @Operation(summary = "숙소 배치 수집 시작", description = "TourAPI에서 충북 숙박시설 전체 데이터를 수집해 DB에 적재합니다. 백그라운드에서 실행됩니다.")
    @PostMapping("/sync/initialize")
    public AccommodationInitializationResponse initializeSync() {
        return accommodationInitialSyncService.ensureInitialized();
    }

    @Operation(summary = "숙소 배치 수집 상태 조회", description = "현재 배치 수집 진행 상태를 조회합니다.")
    @GetMapping("/sync/status")
    public AccommodationSyncStatusResponse getSyncStatus() {
        return accommodationSyncStateService.getSyncStatus();
    }
}
