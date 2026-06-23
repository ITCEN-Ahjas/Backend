package com.example.Chungbuk.domain.camping.controller;

import com.example.Chungbuk.domain.camping.dto.response.CampingDetailResponse;
import com.example.Chungbuk.domain.camping.dto.response.CampingInitializationResponse;
import com.example.Chungbuk.domain.camping.dto.response.CampingListResponse;
import com.example.Chungbuk.domain.camping.dto.response.CampingSyncStatusResponse;
import com.example.Chungbuk.domain.camping.service.CampingInitialSyncService;
import com.example.Chungbuk.domain.camping.service.CampingService;
import com.example.Chungbuk.domain.camping.service.CampingSyncStateService;
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
@RequestMapping("/api/campings")
@RequiredArgsConstructor
@Tag(name = "Camping", description = "충북 캠핑장 정보 API")
public class CampingController {

    private final CampingService campingService;
    private final CampingInitialSyncService campingInitialSyncService;
    private final CampingSyncStateService campingSyncStateService;

    @Operation(summary = "캠핑장 목록 조회", description = "시군구, 캠핑유형, 키워드로 충북 캠핑장 목록을 조회합니다.")
    @GetMapping
    public CampingListResponse getCampings(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sigunguNm,
            @RequestParam(required = false) String induty,
            @RequestParam(required = false) String keyword
    ) {
        return campingService.getCampings(page, size, sigunguNm, induty, keyword);
    }

    @Operation(summary = "캠핑장 상세 조회", description = "contentId로 캠핑장 상세 정보를 조회합니다.")
    @GetMapping("/{contentId}")
    public CampingDetailResponse getCampingDetail(
            @PathVariable String contentId
    ) {
        return campingService.getCampingDetail(contentId);
    }

    @Operation(summary = "캠핑장 배치 수집 시작", description = "고캠핑 API에서 충북 캠핑장 데이터를 수집해 DB에 적재합니다. 백그라운드에서 실행됩니다.")
    @PostMapping("/sync/initialize")
    public CampingInitializationResponse initializeSync() {
        return campingInitialSyncService.ensureInitialized();
    }

    @Operation(summary = "캠핑장 배치 수집 상태 조회", description = "현재 배치 수집 진행 상태를 조회합니다.")
    @GetMapping("/sync/status")
    public CampingSyncStatusResponse getSyncStatus() {
        return campingSyncStateService.getSyncStatus();
    }
}
