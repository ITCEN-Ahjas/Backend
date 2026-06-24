package com.example.Chungbuk.domain.camping.service;

import com.example.Chungbuk.domain.camping.dto.response.CampingDetailResponse;
import com.example.Chungbuk.domain.camping.dto.response.CampingListResponse;
import com.example.Chungbuk.domain.camping.dto.response.CampingSummaryResponse;
import com.example.Chungbuk.domain.camping.entity.CampingEntity;
import com.example.Chungbuk.domain.camping.mapper.CampingMapper;
import com.example.Chungbuk.domain.camping.repository.CampingRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CampingService {

    private final CampingRepository campingRepository;
    private final CampingMapper campingMapper;

    public CampingListResponse getCampings(
            int page,
            int size,
            String sigunguNm,
            String induty,
            String keyword
    ) {
        int validPage = normalizePage(page);
        int validSize = normalizeSize(size);

        List<CampingEntity> all = campingRepository.findAll();

        List<CampingSummaryResponse> filtered = all.stream()
                .filter(e -> matchesSigungu(e.getSigunguNm(), sigunguNm))
                .filter(e -> matchesInduty(e.getInduty(), induty))
                .filter(e -> matchesKeyword(e, keyword))
                .map(e -> CampingSummaryResponse.builder()
                        .contentId(e.getContentId())
                        .facltNm(e.getFacltNm())
                        .lineIntro(e.getLineIntro())
                        .induty(e.getInduty())
                        .lctCl(e.getLctCl())
                        .sigunguNm(e.getSigunguNm())
                        .addr1(e.getAddr1())
                        .mapX(e.getMapX())
                        .mapY(e.getMapY())
                        .manageSttus(e.getManageSttus())
                        .firstImageUrl(e.getFirstImageUrl())
                        .build())
                .toList();

        int totalCount = filtered.size();
        int fromIndex = (validPage - 1) * validSize;
        int toIndex = Math.min(fromIndex + validSize, totalCount);

        List<CampingSummaryResponse> paged = fromIndex >= totalCount
                ? List.of()
                : filtered.subList(fromIndex, toIndex);

        return CampingListResponse.builder()
                .items(paged)
                .page(validPage)
                .size(validSize)
                .totalCount(totalCount)
                .build();
    }

    public CampingDetailResponse getCampingDetail(String contentId) {
        CampingEntity entity = campingRepository.findById(safe(contentId))
                .orElseThrow(() -> new IllegalArgumentException("캠핑장 정보를 찾을 수 없습니다. contentId=" + contentId));

        return campingMapper.toDetailResponse(entity);
    }

    private boolean matchesSigungu(String entityValue, String requested) {
        if (!hasText(requested)) return true;
        return safe(entityValue).contains(requested.trim());
    }

    private boolean matchesInduty(String entityValue, String requested) {
        if (!hasText(requested) || "전체".equals(requested.trim())) return true;
        return safe(entityValue).contains(requested.trim());
    }

    private boolean matchesKeyword(CampingEntity entity, String keyword) {
        if (!hasText(keyword)) return true;
        String normalized = keyword.replace(" ", "").toLowerCase();
        String target = (safe(entity.getFacltNm())
                + safe(entity.getSigunguNm())
                + safe(entity.getAddr1())
                + safe(entity.getInduty())
                + safe(entity.getLctCl())
                + safe(entity.getLineIntro()))
                .replace(" ", "").toLowerCase();
        return target.contains(normalized);
    }

    private int normalizePage(int page) {
        return page < 1 ? 1 : page;
    }

    private int normalizeSize(int size) {
        if (size < 1) return 10;
        if (size > 100) return 100;
        return size;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
