package com.example.Chungbuk.domain.accommodation.service;

import com.example.Chungbuk.domain.accommodation.client.AccommodationApiClient;
import com.example.Chungbuk.domain.accommodation.dto.response.AccommodationDetailResponse;
import com.example.Chungbuk.domain.accommodation.dto.response.AccommodationListResponse;
import com.example.Chungbuk.domain.accommodation.dto.response.AccommodationSummaryResponse;
import com.example.Chungbuk.domain.accommodation.mapper.AccommodationMapper;
import com.example.Chungbuk.domain.accommodation.repository.AccommodationRepository;
import com.example.Chungbuk.domain.festival.constant.ChungbukRegion;
import com.example.Chungbuk.global.config.CacheConfig;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccommodationService {

    private final AccommodationApiClient accommodationApiClient;
    private final AccommodationMapper accommodationMapper;
    private final AccommodationRepository accommodationRepository;

    private static final int DEFAULT_PAGE = 1;

    @Cacheable(
            value = CacheConfig.ACCOMMODATION_LIST_CACHE,
            key = "'page=' + #page"
                    + " + ':size=' + #size"
                    + " + ':region=' + (#region == null ? '' : #region)"
                    + " + ':category=' + (#category == null ? '' : #category)"
                    + " + ':keyword=' + (#keyword == null ? '' : #keyword)"
    )
    public AccommodationListResponse getAccommodations(
            int page,
            int size,
            String region,
            String category,
            String keyword
    ) {
        int validPage = normalizePage(page);
        int validSize = normalizeSize(size);
        String sigunguCode = resolveSigunguCode(region);

        String rawJson = accommodationApiClient.getAccommodationListRaw(
                validPage,
                validSize,
                sigunguCode
        );

        AccommodationListResponse response = accommodationMapper.toAccommodationListResponse(
                rawJson,
                validPage,
                validSize
        );

        List<AccommodationSummaryResponse> filteredItems = response.getItems().stream()
                .filter(item -> matchesCategory(item.getCategory(), category))
                .filter(item -> matchesKeyword(item, keyword))
                .toList();

        return AccommodationListResponse.builder()
                .items(filteredItems)
                .page(validPage)
                .size(validSize)
                .totalCount(filteredItems.size())
                .build();
    }

    @Cacheable(
            value = CacheConfig.ACCOMMODATION_DETAIL_CACHE,
            key = "#contentId == null ? '' : #contentId"
    )
    @Transactional
    public AccommodationDetailResponse getAccommodationDetail(String contentId) {
        String validContentId = safe(contentId);

        return accommodationRepository.findById(validContentId)
                .map(accommodationMapper::toAccommodationDetailResponse)
                .orElseGet(() -> fetchAndPersistAccommodationDetail(validContentId));
    }

    private AccommodationDetailResponse fetchAndPersistAccommodationDetail(String contentId) {
        String detailCommonRawJson = accommodationApiClient.getAccommodationDetailCommonRaw(contentId);
        String detailIntroRawJson = accommodationApiClient.getAccommodationDetailIntroRaw(contentId);
        String detailImageRawJson = accommodationApiClient.getAccommodationDetailImageRaw(contentId);
        String roomInfoRawJson = accommodationApiClient.getAccommodationRoomInfoRaw(contentId);

        AccommodationDetailResponse response = accommodationMapper.toAccommodationDetailResponse(
                detailCommonRawJson,
                detailIntroRawJson,
                detailImageRawJson,
                roomInfoRawJson,
                contentId
        );

        if (hasText(response.getId()) && hasText(response.getTitle())) {
            accommodationRepository.save(accommodationMapper.toEntity(response));
        }

        return response;
    }

    public String getAccommodationRaw(int page, int size, String region) {
        int validPage = normalizePage(page);
        int validSize = normalizeSize(size);
        String sigunguCode = resolveSigunguCode(region);

        return accommodationApiClient.getAccommodationListRaw(
                validPage,
                validSize,
                sigunguCode
        );
    }

    private boolean matchesCategory(String itemCategory, String requestedCategory) {
        if (!hasText(requestedCategory) || "전체".equals(requestedCategory.trim())) {
            return true;
        }

        return requestedCategory.trim().equals(itemCategory);
    }

    private boolean matchesKeyword(AccommodationSummaryResponse item, String keyword) {
        if (!hasText(keyword)) {
            return true;
        }

        String normalizedKeyword = normalizeForSearch(keyword);

        String searchTarget = normalizeForSearch(
                item.getTitle() + " "
                        + item.getRegion() + " "
                        + item.getCategory() + " "
                        + item.getAddress()
        );

        return searchTarget.contains(normalizedKeyword);
    }

    private int normalizePage(int page) {
        if (page < 1) {
            return DEFAULT_PAGE;
        }

        return page;
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            return 10;
        }

        if (size > 100) {
            return 100;
        }

        return size;
    }

    private String resolveSigunguCode(String region) {
        return ChungbukRegion.findSigunguCodeByName(region);
    }

    private String normalizeForSearch(String value) {
        return safe(value)
                .replace(" ", "")
                .toLowerCase();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
